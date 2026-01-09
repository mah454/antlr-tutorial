package ir.moke;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.TextNode;
import ir.moke.antlr4.FilterGrammerBaseVisitor;
import ir.moke.antlr4.FilterGrammerParser;

public class FilterEvalVisitor extends FilterGrammerBaseVisitor<Void> {
    private final ObjectMapper mapper = new ObjectMapper();
    private final ArrayNode data;

    public FilterEvalVisitor(ArrayNode data) {
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
        ArrayNode result = mapper.createArrayNode();

        for (JsonNode node : data) {
            if (evalExpression(expr, node)) {
                result.add(node);
            }
        }

        data.removeAll();
        data.addAll(result);
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
            return resolvePath(expr.path(), ctx);
        }
        return NullNode.instance;
    }

    /**
     * Resolve a nested path like profile.contact.city
     */
    private JsonNode resolvePath(FilterGrammerParser.PathContext path, JsonNode ctx) {
        JsonNode current = ctx;
        for (var id : path.IDENT()) {
            if (current.has(id.getText())) {
                current = current.get(id.getText());
            } else {
                return NullNode.instance;
            }
        }
        return current;
    }

    /**
     * Remove quotes from a string literal
     */
    private String stripQuotes(String s) {
        return s.substring(1, s.length() - 1);
    }
}