package ir.moke;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;
import ir.moke.antlr4.FilterGrammerBaseVisitor;
import ir.moke.antlr4.FilterGrammerParser;

import java.util.ArrayList;
import java.util.List;

public class FilterEvalVisitor extends FilterGrammerBaseVisitor<Void> {
    private final ObjectMapper mapper = new ObjectMapper();
    private final JsonNode data;

    public FilterEvalVisitor(JsonNode data) {
        this.data = data;
    }

    @Override
    public Void visitProgram(FilterGrammerParser.ProgramContext ctx) {
        for (var stmt : ctx.statement()) {
            visit(stmt);
        }
        return null;
    }

    @Override
    public Void visitStatement(FilterGrammerParser.StatementContext ctx) {
        String op = ctx.operation().getText();
        if ("filter".equals(op)) {
            applyFilter(ctx.expression());
        }
        return null;
    }

    /**
     * Apply a filter expression to the data
     */
    private void applyFilter(FilterGrammerParser.ExpressionContext expr) {

        // root باید Object باشد
        if (!data.isObject()) {
            throw new IllegalStateException("Root must be an object");
        }

        // resolve first path segment manually: node[]
        JsonNode nodeArray = data.get("node");

        if (nodeArray == null || !nodeArray.isArray()) {
            return;
        }

        ArrayNode result = mapper.createArrayNode();

        for (JsonNode item : nodeArray) {
            if (evalExpression(expr, item)) {
                result.add(item);
            }
        }

        ((ObjectNode) data).set("node", result);
    }

    /**
     * Evaluate any expression (logical or comparison)
     */

    private boolean evalExpression(FilterGrammerParser.ExpressionContext expr, JsonNode ctx) {
        // Parenthesized expression
        if (expr.expression().size() == 1) {
            return evalExpression(expr.expression(0), ctx);
        }

        // Logical expressions
        if (expr.AND() != null) {
            return evalExpression(expr.expression(0), ctx)
                    && evalExpression(expr.expression(1), ctx);
        }
        if (expr.OR() != null) {
            return evalExpression(expr.expression(0), ctx)
                    || evalExpression(expr.expression(1), ctx);
        }

        // Comparison
        return evalComparison(expr.comparison(), ctx);
    }

    /**
     * Evaluate a comparison
     */
    private boolean evalComparison(FilterGrammerParser.ComparisonContext cmp, JsonNode ctx) {
        JsonNode left = resolveValue(cmp.valueExpr(0), ctx);
        JsonNode right = resolveValue(cmp.valueExpr(1), ctx);

        String op = cmp.comparator().getText();

        if (left.isArray()) {
            for (JsonNode item : left) {
                if (compare(item, right, op)) {
                    return true;
                }
            }
            return false;
        }

        if ("~".equals(op) && left.isArray()) {
            if (left.isArray()) {
                for (JsonNode item : left) {
                    if (item.equals(right)) {
                        return true;
                    }
                }
                return false;
            }
        }

        // Numeric comparison
        if (left.isNumber() && right.isNumber()) {
            double l = left.asDouble();
            double r = right.asDouble();
            return switch (op) {
                case "==" -> l == r;
                case "!=" -> l != r;
                case ">" -> l > r;
                case "<" -> l < r;
                case ">=" -> l >= r;
                case "<=" -> l <= r;
                case "~" -> left.asText().contains(right.asText());
                default -> false;
            };
        }

        // String comparison
        String lText = left.asText();
        String rText = right.asText();
        return switch (op) {
            case "==" -> lText.equals(rText);
            case "!=" -> !lText.equals(rText);
            case ">" -> lText.compareTo(rText) > 0;
            case "<" -> lText.compareTo(rText) < 0;
            case ">=" -> lText.compareTo(rText) >= 0;
            case "<=" -> lText.compareTo(rText) <= 0;
            case "~" -> lText.contains(rText);
            default -> false;
        };
    }

    private boolean compare(JsonNode left, JsonNode right, String op) {
        if (left.isNumber() && right.isNumber()) {
            double l = left.asDouble();
            double r = right.asDouble();
            return switch (op) {
                case "==" -> l == r;
                case "!=" -> l != r;
                case ">" -> l > r;
                case "<" -> l < r;
                case ">=" -> l >= r;
                case "<=" -> l <= r;
                default -> false;
            };
        }

        String lText = left.asText();
        String rText = right.asText();
        return switch (op) {
            case "==" -> lText.equals(rText);
            case "!=" -> !lText.equals(rText);
            case "~" -> lText.contains(rText);
            default -> false;
        };
    }


    /**
     * Resolve a value expression to a JsonNode
     */
    private JsonNode resolveValue(FilterGrammerParser.ValueExprContext expr, JsonNode ctx) {
        if (expr.NUMBER() != null) {
            return new IntNode(Integer.parseInt(expr.NUMBER().getText()));
        }
        if (expr.STRING() != null) {
            return new TextNode(stripQuotes(expr.STRING().getText()));
        }
        if (expr.NULL() != null) {
            return NullNode.instance;
        }
        if (expr.path() != null) {
            List<JsonNode> values = resolvePath(expr.path(), ctx);

            if (values.size() == 1) {
                return values.get(0);
            }

            ArrayNode arr = mapper.createArrayNode();
            values.forEach(arr::add);
            return arr;
        }
        return NullNode.instance;
    }

    /**
     * Resolve a nested path like profile.contact.city
     */
    private List<JsonNode> resolvePath(FilterGrammerParser.PathContext path, JsonNode ctx) {
        List<JsonNode> currentNodes = List.of(ctx);

        for (var segment : path.pathSegment()) {
            List<JsonNode> nextNodes = new ArrayList<>();
            String field = segment.IDENT().getText();
            boolean isArray = segment.getChildCount() > 1; // has []

            for (JsonNode node : currentNodes) {
                if (!node.has(field)) continue;

                JsonNode value = node.get(field);

                if (isArray && value.isArray()) {
                    value.forEach(nextNodes::add);
                } else {
                    nextNodes.add(value);
                }
            }

            currentNodes = nextNodes;
        }

        return currentNodes;
    }


    /**
     * Remove quotes from a string literal
     */
    private String stripQuotes(String s) {
        return s.substring(1, s.length() - 1);
    }
}