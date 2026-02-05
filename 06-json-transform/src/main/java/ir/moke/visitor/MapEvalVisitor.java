package ir.moke.visitor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import ir.moke.antlr4.MapGrammerBaseVisitor;
import ir.moke.antlr4.MapGrammerParser;

public class MapEvalVisitor extends MapGrammerBaseVisitor<Void> {

    private final JsonNode data;

    public MapEvalVisitor(JsonNode data) {
        this.data = data;
    }

    @Override
    public Void visitProgram(MapGrammerParser.ProgramContext ctx) {
        for (var stmt : ctx.statement()) {
            visit(stmt);
        }
        return null;
    }

    @Override
    public Void visitStatement(MapGrammerParser.StatementContext ctx) {
        visit(ctx.assignment());
        return null;
    }

    @Override
    public Void visitAssignment(MapGrammerParser.AssignmentContext ctx) {
        if (data.isObject()) {
            applyAssignment(ctx, data);
        } else if (data.isArray()) {
            for (JsonNode item : data) {
                applyAssignment(ctx, item);
            }
        }
        return null;
    }

    private void applyAssignment(MapGrammerParser.AssignmentContext ctx, JsonNode node) {
        String fieldName = ctx.path().pathSegment(ctx.path().pathSegment().size() - 1).getText();
        ObjectNode target = resolveTargetNode(ctx.path(), node);
        if (target == null) return;

        JsonNode value = evalExpr(ctx.expr(), node);
        target.set(fieldName, value);
    }

    private JsonNode evalExpr(MapGrammerParser.ExprContext ctx, JsonNode rootNode) {
        if (ctx instanceof MapGrammerParser.NullExprContext) {
            return NullNode.getInstance();
        } else if (ctx instanceof MapGrammerParser.StringExprContext strCtx) {
            return new TextNode(stripQuotes(strCtx.STRING().getText()));
        } else if (ctx instanceof MapGrammerParser.NumberExprContext numCtx) {
            return new DoubleNode(Double.parseDouble(numCtx.NUMBER().getText()));
        } else if (ctx instanceof MapGrammerParser.PathExprContext pathCtx) {
            return resolvePath(pathCtx.path(), rootNode);
        } else if (ctx instanceof MapGrammerParser.ParenExprContext parentCtx) {
            return evalExpr(parentCtx.expr(), rootNode);
        } else if (ctx instanceof MapGrammerParser.MathExprContext mathCtx) {
            JsonNode leftNode = evalExpr(mathCtx.expr(0), rootNode);
            JsonNode rightNode = evalExpr(mathCtx.expr(1), rootNode);
            String op = mathCtx.mathOperation().getText();

            if (op.equals("+") && leftNode.isTextual() && rightNode.isTextual()) {
                return new TextNode(leftNode.asText() + rightNode.asText());
            }

            double l = leftNode.isNumber() ? leftNode.asDouble() : 0;
            double r = rightNode.isNumber() ? rightNode.asDouble() : 0;

            return switch (op) {
                case "+" -> new DoubleNode(l + r);
                case "-" -> new DoubleNode(l - r);
                case "*" -> new DoubleNode(l * r);
                case "/" -> new DoubleNode(l / r);
                default -> NullNode.getInstance();
            };
        } else if (ctx instanceof MapGrammerParser.ConcatExprContext concatCtx) {
            String l = evalExpr(concatCtx.expr(0),rootNode).asText();
            String r = evalExpr(concatCtx.expr(1),rootNode).asText();
            return new TextNode(l + r);
        }
        return NullNode.getInstance();
    }

    private JsonNode resolvePath(MapGrammerParser.PathContext ctx, JsonNode rootNode) {
        JsonNode current = rootNode;
        for (var id : ctx.pathSegment()) {
            if (current.has(id.getText())) {
                current = current.get(id.getText());
            } else {
                return NullNode.instance;
            }
        }
        return current;
    }

    private ObjectNode resolveTargetNode(MapGrammerParser.PathContext ctx, JsonNode rootNode) {
        if (ctx.pathSegment().size() == 1) return (ObjectNode) rootNode;

        JsonNode currentNode = rootNode;
        for (int i = 0; i < ctx.pathSegment().size() - 1; i++) {
            String field = ctx.pathSegment(i).getText();
            currentNode = currentNode.get(field);
            if (currentNode == null) return null;
        }

        return (ObjectNode) currentNode;
    }

    /**
     * remove double quotes
     *
     */
    private String stripQuotes(String s) {
        return s.substring(1, s.length() - 1);
    }
}