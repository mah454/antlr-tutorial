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
        ctx.clauses().forEach(this::visit);
        return null;
    }

    @Override
    public Void visitClauses(FilterGrammerParser.ClausesContext ctx) {
        visit(ctx.expressions());
        return null;
    }

    @Override
    public Void visitExpressions(FilterGrammerParser.ExpressionsContext ctx) {
        if (!data.isArray()) throw new IllegalArgumentException("Json node should be array");
        ArrayNode arr = (ArrayNode) data;
        if (arr.isEmpty()) return null;

        ArrayNode filtered = mapper.createArrayNode();
        for (JsonNode item : arr) {
            if (evalExpression(ctx, item)) filtered.add(item);
        }

        arr.removeAll();
        arr.addAll(filtered);
        return null;
    }

    private boolean evalExpression(FilterGrammerParser.ExpressionsContext ctx, JsonNode node) {
        if (ctx.statement() != null) return evalComparison(ctx.statement(), node);
        if (ctx.arrayFilter() != null) return evalArrayFilter(ctx.arrayFilter(), node);

        if (ctx.AND() != null) return evalExpression(ctx.expressions(0), node) && evalExpression(ctx.expressions(1), node);
        if (ctx.OR() != null) return evalExpression(ctx.expressions(0), node) || evalExpression(ctx.expressions(1), node);

        if (ctx.expressions().size() == 1) return evalExpression(ctx.expressions(0), node);

        return false;
    }

    private boolean evalArrayFilter(FilterGrammerParser.ArrayFilterContext ctx, JsonNode node) {
        List<JsonNode> targets = ctx.path() == null ? List.of(node) : resolvePath(ctx.path(), node);

        if (ctx.NUMBER() != null) {
            int index = Integer.parseInt(ctx.NUMBER().getText());
            for (JsonNode target : targets) {
                if (target.isArray()) {
                    ArrayNode filtered = mapper.createArrayNode();
                    JsonNode indexedNode = target.get(index);
                    if (indexedNode != null) filtered.add(indexedNode);
                    ((ArrayNode) target).removeAll();
                    ((ArrayNode) target).addAll(filtered);
                }
            }
        } else {
            for (JsonNode target : targets) {
                if (!target.isArray()) continue;
                ArrayNode filtered = mapper.createArrayNode();
                for (JsonNode item : target) {
                    if (evalExpression(ctx.expressions(), item)) filtered.add(item);
                }
                ((ArrayNode) target).removeAll();
                ((ArrayNode) target).addAll(filtered);
            }
        }

        return true;
    }

    private boolean evalComparison(FilterGrammerParser.StatementContext ctx, JsonNode node) {
        if (ctx == null) return false;
        JsonNode left = readValue(ctx.stmtValue(0), node);
        JsonNode right = readValue(ctx.stmtValue(1), node);
        String cmp = ctx.comparator().getText();

        if (left.isArray() || right.isArray()) {
            for (JsonNode l : left.isArray() ? left : List.of(left)) {
                for (JsonNode r : right.isArray() ? right : List.of(right)) {
                    if (compare(l, r, cmp)) return true;
                }
            }
            return false;
        }

        return compare(left, right, cmp);
    }

    private boolean compare(JsonNode left, JsonNode right, String cmp) {
        if (left.isNumber() && right.isNumber()) return checkNumeric(left.doubleValue(), right.doubleValue(), cmp);
        return checkString(left.textValue(), right.textValue(), cmp);
    }

    private boolean checkString(String l, String r, String cmp) {
        return switch (cmp) {
            case "=" -> l.equalsIgnoreCase(r);
            case "==" -> Objects.equals(l, r);
            case "!=" -> !l.equalsIgnoreCase(r);
            case "!==" -> !Objects.equals(l, r);
            case ">" -> l.compareTo(r) > 0;
            case ">=" -> l.compareTo(r) >= 0;
            case "<" -> l.compareTo(r) < 0;
            case "<=" -> l.compareTo(r) <= 0;
            case "~" -> l.toLowerCase().contains(r.toLowerCase());
            case "!~" -> !l.toLowerCase().contains(r.toLowerCase());
            default -> false;
        };
    }

    private boolean checkNumeric(double l, double r, String cmp) {
        return switch (cmp) {
            case "=", "==" -> l == r;
            case "!=", "!==" -> l != r;
            case ">" -> l > r;
            case ">=" -> l >= r;
            case "<" -> l < r;
            case "<=" -> l <= r;
            case "~", "!~" -> throw new IllegalArgumentException("Numeric contains operators not supported");
            default -> false;
        };
    }

    private JsonNode readValue(FilterGrammerParser.StmtValueContext ctx, JsonNode node) {
        if ("@".equals(ctx.getText())) return node;
        if (ctx.NUMBER() != null) return new IntNode(Integer.parseInt(ctx.NUMBER().getText()));
        if (ctx.STRING() != null) return new TextNode(stripQuotes(ctx.STRING().getText()));
        if (ctx.NULL() != null) return NullNode.getInstance();
        if (ctx.path() != null) {
            List<JsonNode> nodes = resolvePath(ctx.path(), node);
            if (nodes.size() == 1) return nodes.getFirst();
            ArrayNode arr = mapper.createArrayNode();
            nodes.forEach(arr::add);
            return arr;
        }
        return NullNode.getInstance();
    }

    private List<JsonNode> resolvePath(FilterGrammerParser.PathContext ctx, JsonNode root) {
        List<JsonNode> current = List.of(root);
        for (var segment : ctx.pathSegment()) {
            List<JsonNode> next = new ArrayList<>();
            for (JsonNode node : current) applySegment(node, segment, next);
            current = next;
            if (current.isEmpty()) break;
        }
        return current;
    }

    private void applySegment(JsonNode node, FilterGrammerParser.PathSegmentContext segment, List<JsonNode> output) {
        if (node.isArray()) {
            node.forEach(n -> applySegment(n, segment, output));
            return;
        }
        if (!node.isObject()) return;

        String field = segment.IDENT() != null ? segment.IDENT().getText() : null;
        if (field == null || !node.has(field)) return;

        JsonNode value = node.get(field);
        if (isArrayExpansion(segment)) {
            value.forEach(output::add);
        } else if (extractIndex(segment) != null) {
            Integer idx = extractIndex(segment);
            if (idx != null && value.isArray() && idx < value.size()) output.add(value.get(idx));
        } else output.add(value);
    }

    private boolean isArrayExpansion(FilterGrammerParser.PathSegmentContext ctx) {
        return ctx.getText().endsWith("[]");
    }

    private Integer extractIndex(FilterGrammerParser.PathSegmentContext ctx) {
        return ctx.NUMBER() != null ? Integer.parseInt(ctx.NUMBER().getText()) : null;
    }

    private String stripQuotes(String s) {
        return s.substring(1, s.length() - 1);
    }
}
