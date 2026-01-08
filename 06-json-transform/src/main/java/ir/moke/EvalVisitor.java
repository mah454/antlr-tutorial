package ir.moke;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;
import ir.moke.antlr4.JsonTransformBaseVisitor;
import ir.moke.antlr4.JsonTransformParser;

public class EvalVisitor extends JsonTransformBaseVisitor<Void> {
    private final ObjectMapper mapper = new ObjectMapper();
    private ArrayNode data;

    public EvalVisitor(ArrayNode data) {
        this.data = data;
    }


    @Override
    public Void visitProgram(JsonTransformParser.ProgramContext ctx) {
        for (var stmt : ctx.statement()) {
            visit(stmt);
        }
        return null;
    }

    @Override
    public Void visitStatement(JsonTransformParser.StatementContext ctx) {
        String op = ctx.operation().getText();

        switch (op) {
            case "filter" -> applyFilter(ctx.expression());
            case "map" -> applyMap(ctx.operation().path(), ctx.expression());
            case "mute" -> applyMute(ctx.operation().path(), ctx.expression());
            case "append" -> applyAppend(ctx.operation().path(), ctx.expression());
            case "rename" -> applyRename(ctx.operation().path(), ctx.expression());
            case "replace" -> applyReplace(ctx.operation().path(), ctx.expression());
        }
        return null;
    }

    private void applyFilter(JsonTransformParser.ExpressionContext expr) {
        ArrayNode result = mapper.createArrayNode();

        for (JsonNode node : data) {
            if (evalCondition(expr, node)) {
                result.add(node);
            }
        }

        data.removeAll();
        data.addAll(result);
    }

    private boolean evalCondition(JsonTransformParser.ExpressionContext expr, JsonNode ctx) {
        if (expr.logicalExpr() != null) {
            return evalLogical(expr.logicalExpr(), ctx);
        }
        return false;
    }

    private boolean evalLogical(JsonTransformParser.LogicalExprContext expr, JsonNode ctx) {
        if (expr.AND() != null) {
            return evalLogical(expr.logicalExpr(0), ctx)
                    && evalLogical(expr.logicalExpr(1), ctx);
        }
        if (expr.OR() != null) {
            return evalLogical(expr.logicalExpr(0), ctx)
                    || evalLogical(expr.logicalExpr(1), ctx);
        }
        return evalComparison(expr.comparison(), ctx);
    }

    private boolean evalComparison(JsonTransformParser.ComparisonContext cmp, JsonNode ctx) {
        JsonNode left = resolveValue(cmp.valueExpr(0), ctx);
        JsonNode right = resolveValue(cmp.valueExpr(1), ctx);

        String op = cmp.comparator().getText();

        // If both are numeric
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

        // For strings
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

    private JsonNode resolvePath(JsonTransformParser.PathContext path, JsonNode ctx) {
        JsonNode current = ctx;
        for (var id : path.IDENT()) {
            current = current.get(id.getText());
            if (current == null) return NullNode.instance;
        }
        return current;
    }

    private JsonNode resolveValue(JsonTransformParser.ValueExprContext expr, JsonNode ctx) {
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
        if (expr.arithmeticExpr() != null) {
            return evalArithmetic(expr.arithmeticExpr(), ctx);
        }
        return NullNode.instance;
    }

    private String stripQuotes(String s) {
        return s.substring(1, s.length() - 1);
    }

    private void applyAssignment(JsonTransformParser.AssignmentContext assign, ObjectNode target) {
        if (target == null) return;

        String field = assign.path().IDENT(assign.path().IDENT().size() - 1).getText();
        JsonNode value = resolveValue(assign.valueExpr(), target);

        target.set(field, value);
    }

    private void applyMap(JsonTransformParser.PathContext scope, JsonTransformParser.ExpressionContext expr) {
        for (JsonNode node : data) {
            ObjectNode target = resolveScope(node, scope);
            applyAssignment(expr.assignment(), target);
        }
    }

    private JsonNode evalArithmetic(
            JsonTransformParser.ArithmeticExprContext expr,
            JsonNode ctx
    ) {
        if (expr == null) {
            return NullNode.instance;
        }

        // NUMBER
        if (expr.NUMBER() != null) {
            return new IntNode(Integer.parseInt(expr.NUMBER().getText()));
        }

        // PATH (e.g. profile.age)
        if (expr.path() != null) {
            return resolvePath(expr.path(), ctx);
        }

        // BINARY OPERATION
        if (expr.op != null) {
            JsonNode left = evalArithmetic(expr.arithmeticExpr(0), ctx);
            JsonNode right = evalArithmetic(expr.arithmeticExpr(1), ctx);

            int l = left.asInt();
            int r = right.asInt();

            return switch (expr.op.getText()) {
                case "+" -> new IntNode(l + r);
                case "-" -> new IntNode(l - r);
                case "*" -> new IntNode(l * r);
                case "/" -> new IntNode(l / r);
                default -> NullNode.instance;
            };
        }
        return NullNode.instance;
    }

    private void applyMute(JsonTransformParser.PathContext scope, JsonTransformParser.ExpressionContext expr) {
        for (JsonNode node : data) {
            ObjectNode target = resolveScope(node, scope);
            applyAssignment(expr.assignment(), target);
        }
    }

    private ObjectNode resolveScope(JsonNode root, JsonTransformParser.PathContext scope) {
        if (scope == null) {
            return (ObjectNode) root;
        }

        JsonNode current = root;
        for (var id : scope.IDENT()) {
            current = current.get(id.getText());
            if (current == null || !current.isObject()) {
                return null;
            }
        }
        return (ObjectNode) current;
    }

    private void applyAppend(
            JsonTransformParser.PathContext scope,
            JsonTransformParser.ExpressionContext expr
    ) {
        for (JsonNode node : data) {
            ObjectNode target = resolveScope(node, scope);
            if (target == null) continue;

            JsonTransformParser.AssignmentContext assign = expr.assignment();
            String field = assign.path()
                    .IDENT(assign.path().IDENT().size() - 1)
                    .getText();

            if (!target.has(field)) {
                JsonNode value = resolveValue(assign.valueExpr(), target);
                target.set(field, value);
            }
        }
    }

    private void applyRename(
            JsonTransformParser.PathContext scope,
            JsonTransformParser.ExpressionContext expr
    ) {
        for (JsonNode node : data) {
            ObjectNode target = resolveScope(node, scope);
            if (target == null) continue;

            JsonTransformParser.AssignmentContext assign = expr.assignment();

            String newKey = assign.path()
                    .IDENT(assign.path().IDENT().size() - 1)
                    .getText();

            JsonNode oldValue = resolveValue(assign.valueExpr(), target);
            String oldKey = assign.valueExpr().path().IDENT(
                    assign.valueExpr().path().IDENT().size() - 1
            ).getText();

            target.set(newKey, oldValue);
            target.remove(oldKey);
        }
    }

    private void applyReplace(
            JsonTransformParser.PathContext scope,
            JsonTransformParser.ExpressionContext expr
    ) {
        for (JsonNode node : data) {
            ObjectNode target = resolveScope(node, scope);
            if (target == null) continue;

            JsonTransformParser.AssignmentContext assign = expr.assignment();

            String field = assign.path()
                    .IDENT(assign.path().IDENT().size() - 1)
                    .getText();

            JsonNode value = resolveValue(assign.valueExpr(), target);
            target.set(field, value);
        }
    }

}
