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
        } else {
            return evalComparison(ctx.statement(), jsonNode);
        }
    }

    private boolean evalComparison(FilterGrammerParser.StatementContext ctx, JsonNode jsonNode) {
        JsonNode leftNode = readValue(ctx.stmtValue(0), jsonNode);
        JsonNode rightNode = readValue(ctx.stmtValue(1), jsonNode);
        String comparator = ctx.comparator().getText();

        if (leftNode.isArray()) {
            for (JsonNode item : leftNode) {
                if (item.isNumber()) {
                    return checkNumeric(leftNode, rightNode, comparator);
                } else {
                    return checkString(leftNode, rightNode, comparator);
                }
            }
        } else if ("~".equals(comparator) && leftNode.isArray()) {
            for (JsonNode item : leftNode) {
                return item.equals(rightNode);
            }
        } else {
            if (leftNode.isNumber()) {
                return checkNumeric(leftNode, rightNode, comparator);
            } else {
                return checkString(leftNode, rightNode, comparator);
            }
        }

        return false;
    }

    private boolean checkString(JsonNode leftNode, JsonNode rightNode, String comparator) {
        String lText = leftNode.textValue();
        String rText = rightNode.textValue();
        return switch (comparator) {
            case "=" -> lText.equalsIgnoreCase(rText);
            case "==" -> Objects.equals(lText, rText);
            case "!=" -> !Objects.equals(lText, rText);
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
            case "!=" -> l != r;
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

    private List<JsonNode> resolvePath(FilterGrammerParser.PathContext ctx, JsonNode jsonNode) {
        List<JsonNode> currentNodes = List.of(jsonNode);

        for (FilterGrammerParser.PathSegmentContext segment : ctx.pathSegment()) {
            String field = segment.IDENT().getText();
            List<JsonNode> foundedItems = new ArrayList<>();
            boolean isArray = segment.getChildCount() > 1; // has []
            for (JsonNode node : currentNodes) {
                if (!node.has(field)) continue;
                JsonNode value = node.get(field);
                if (isArray && value.isArray()) {
                    value.forEach(foundedItems::add);
                }
                foundedItems.add(value);
            }
            currentNodes = foundedItems;
        }

        return currentNodes;
    }

    /**
     * remove double quotes
     *
     */
    private String stripQuotes(String s) {
        return s.substring(1, s.length() - 1);
    }
}