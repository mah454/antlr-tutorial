package ir.moke.visitor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.TextNode;
import ir.moke.antlr4.FilterGrammerBaseVisitor;
import ir.moke.antlr4.FilterGrammerParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class FilterEvalVisitor extends FilterGrammerBaseVisitor<Void> {
    private final ObjectMapper mapper = new ObjectMapper();
    private final JsonNode data;

    public FilterEvalVisitor(JsonNode data) {
        this.data = data;
    }

    @Override
    public Void visitProgram(FilterGrammerParser.ProgramContext ctx) {
        for (var clause : ctx.clauses()) {
            visit(clause);
        }
        return null;
    }


    @Override
    public Void visitClauses(FilterGrammerParser.ClausesContext ctx) {
        visit(ctx.expressions());
        return null;
    }

    @Override
    public Void visitExpressions(FilterGrammerParser.ExpressionsContext ctx) {
        if (data.isObject()) throw new IllegalArgumentException("Json node should be array");

        ArrayNode arr = (ArrayNode) data;
        if (arr.isEmpty()) return null;

        ArrayNode arrayNode = mapper.createArrayNode();

        for (JsonNode item : arr) {
            boolean isTrue = evalExpression(ctx, item);
            if (isTrue) arrayNode.add(item);
        }

        ((ArrayNode) data).removeAll();
        ((ArrayNode) data).addAll(arrayNode);
        return null;
    }

    private boolean evalExpression(FilterGrammerParser.ExpressionsContext ctx, JsonNode jsonNode) {
        if (ctx.expressions().size() == 1) {
            return evalExpression(ctx.expressions(0), jsonNode);
        } else if (ctx.AND() != null) {
            return evalExpression(ctx.expressions(0), jsonNode) && evalExpression(ctx.expressions(1), jsonNode);
        } else if (ctx.OR() != null) {
            return evalExpression(ctx.expressions(0), jsonNode) || evalExpression(ctx.expressions(1), jsonNode);
        } else if (ctx.arrayFilter() != null) {
            return evalArrayFilter(ctx.arrayFilter(), jsonNode);
        } else {
            return evalComparison(ctx.statement(), jsonNode);
        }
    }

    private boolean evalArrayFilter(FilterGrammerParser.ArrayFilterContext ctx, JsonNode jsonNode) {
        FilterGrammerParser.PathContext pathContext = ctx.path();
        List<JsonNode> targetNodes;

        if (pathContext == null) {
            targetNodes = List.of(data);
        } else {
            targetNodes = resolvePath(pathContext, jsonNode);
        }

        if (ctx.NUMBER() != null) {
            for (JsonNode arrNode : targetNodes) {
                if (!arrNode.isArray()) continue;
                int index = Integer.parseInt(ctx.NUMBER().getText());
                ArrayNode resultArr = mapper.createArrayNode();
                JsonNode indexNode = arrNode.get(index);
                if (indexNode != null) resultArr.add(indexNode);
                ((ArrayNode) arrNode).removeAll();
                ((ArrayNode) arrNode).addAll(resultArr);
            }
        } else {
            for (JsonNode arrNode : targetNodes) {
                if (!arrNode.isArray()) continue;
                ArrayNode resultArr = mapper.createArrayNode();
                for (JsonNode node : arrNode) {
                    FilterGrammerParser.ExpressionsContext expressionsContext = ctx.expressions();
                    boolean evaluated = evalExpression(expressionsContext, node);
                    if (evaluated) resultArr.add(node);
                }
                ((ArrayNode) arrNode).removeAll();
                ((ArrayNode) arrNode).addAll(resultArr);
            }
        }

        return true;
    }

    private boolean evalComparison(FilterGrammerParser.StatementContext ctx, JsonNode jsonNode) {
        if (ctx == null) return false;
        JsonNode leftNode = readValue(ctx.stmtValue(0), jsonNode);
        JsonNode rightNode = readValue(ctx.stmtValue(1), jsonNode);
        String comparator = ctx.comparator().getText();


        if (leftNode.isArray() && !rightNode.isArray()) {
            for (JsonNode item : leftNode) {
                if (compare(item, rightNode, comparator)) return true;
            }
            return false;
        }

        if (!leftNode.isArray() && rightNode.isArray()) {
            for (JsonNode item : rightNode) {
                if (compare(leftNode, item, comparator)) return true;
            }
            return false;
        }

        if (leftNode.isArray() && rightNode.isArray()) {
            for (JsonNode ln : leftNode) {
                for (JsonNode rn : rightNode) {
                    if (compare(ln, rn, comparator)) return true;
                }
            }
            return false;
        }

        return compare(leftNode, rightNode, comparator);
    }

    private boolean compare(JsonNode left, JsonNode right, String comparator) {
        if (left.isNumber() && right.isNumber()) {
            return checkNumeric(left, right, comparator);
        }
        return checkString(left, right, comparator);
    }

    private boolean checkString(JsonNode leftNode, JsonNode rightNode, String comparator) {
        String lText = leftNode.textValue();
        String rText = rightNode.textValue();
        return switch (comparator) {
            case "=" -> lText.equalsIgnoreCase(rText);
            case "==" -> Objects.equals(lText, rText);
            case "!=" -> !Objects.equals(lText.toLowerCase(), rText.toLowerCase());
            case "!==" -> !Objects.equals(lText, rText);
            case ">" -> lText.compareTo(rText) > 0;
            case ">=" -> lText.compareTo(rText) >= 0;
            case "<" -> lText.compareTo(rText) < 0;
            case "<=" -> lText.compareTo(rText) <= 0;
            case "~" -> lText.toLowerCase().contains(rText.toLowerCase());
            case "!~" -> !lText.toLowerCase().contains(rText.toLowerCase());
            default -> false;
        };
    }

    private boolean checkNumeric(JsonNode leftNode, JsonNode rightNode, String comparator) {
        double l = leftNode.doubleValue();
        double r = rightNode.doubleValue();
        return switch (comparator) {
            case "=", "==" -> l == r;
            case "!=", "!==" -> l != r;
            case ">" -> l > r;
            case ">=" -> l >= r;
            case "<" -> l < r;
            case "<=" -> l <= r;
            case "~" -> leftNode.textValue().contains(rightNode.textValue());
            case "!~" -> !leftNode.textValue().contains(rightNode.textValue());
            default -> false;
        };
    }

    private JsonNode readValue(FilterGrammerParser.StmtValueContext ctx, JsonNode node) {
        if (ctx.NUMBER() != null) return new IntNode(Integer.parseInt(ctx.NUMBER().getText()));
        if (ctx.STRING() != null) return new TextNode(stripQuotes(ctx.STRING().getText()));
        if (ctx.NULL() != null) return NullNode.getInstance();
        if (ctx.path() != null) {
            List<JsonNode> jsonNodes = resolvePath(ctx.path(), node);
            if (jsonNodes.size() == 1) {
                return jsonNodes.getFirst();
            } else {
                ArrayNode arrayNode = mapper.createArrayNode();
                jsonNodes.forEach(arrayNode::add);
                return arrayNode;
            }
        }
        return NullNode.getInstance();
    }

    private List<JsonNode> resolvePath(FilterGrammerParser.PathContext ctx, JsonNode root) {
        if (ctx == null || ctx.pathSegment().isEmpty()) {
            return List.of(root);
        }

        List<JsonNode> current = List.of(root);

        for (FilterGrammerParser.PathSegmentContext segment : ctx.pathSegment()) {
            List<JsonNode> next = new ArrayList<>();

            for (JsonNode node : current) {
                applySegment(node, segment, next);
            }

            current = next;
            if (current.isEmpty()) break;
        }

        return current;
    }

    private void applySegment(
            JsonNode node,
            FilterGrammerParser.PathSegmentContext segment,
            List<JsonNode> output
    ) {
        // If node is array → apply segment to each element
        if (node.isArray()) {
            for (JsonNode item : node) {
                applySegment(item, segment, output);
            }
            return;
        }

        if (!node.isObject()) return;

        String field = segment.IDENT() != null
                ? segment.IDENT().getText()
                : null;

        if (field == null || !node.has(field)) return;

        JsonNode value = node.get(field);

        // IDENT[]
        if (isArrayExpansion(segment)) {
            if (value.isArray()) {
                value.forEach(output::add);
            }
            return;
        }

        // IDENT[NUMBER]
        Integer index = extractIndex(segment);
        if (index != null) {
            if (value.isArray() && index < value.size()) {
                output.add(value.get(index));
            }
            return;
        }

        // IDENT
        output.add(value);
    }

    private boolean isArrayExpansion(FilterGrammerParser.PathSegmentContext ctx) {
        return ctx.getText().endsWith("[]");
    }

    private Integer extractIndex(FilterGrammerParser.PathSegmentContext ctx) {
        if (ctx.NUMBER() == null) return null;
        return Integer.parseInt(ctx.NUMBER().getText());
    }

    /**
     * remove double quotes
     *
     */
    private String stripQuotes(String s) {
        return s.substring(1, s.length() - 1);
    }
}