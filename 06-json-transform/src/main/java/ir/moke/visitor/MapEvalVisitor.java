package ir.moke.visitor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import ir.moke.antlr4.MapGrammerBaseVisitor;
import ir.moke.antlr4.MapGrammerParser;

public class MapEvalVisitor extends MapGrammerBaseVisitor<Void> {

    private final ArrayNode data;

    public MapEvalVisitor(ArrayNode data) {
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
        String fieldName = ctx.path().IDENT(ctx.path().IDENT().size() - 1).getText();

        for (JsonNode node : data) {
            ObjectNode target = resolveScope(node, ctx.path());
            if (target == null) continue;

            JsonNode value = evalExpr(ctx.expr(), node);
            target.set(fieldName, value);
        }
        return null;
    }

    /**
     * Evaluate expression recursively based on subclass
     */
    private JsonNode evalExpr(MapGrammerParser.ExprContext ctx, JsonNode jsonCtx) {
        if (ctx instanceof MapGrammerParser.NumberExprContext numberCtx) {
            return new DoubleNode(Double.parseDouble(numberCtx.NUMBER().getText()));
        }

        if (ctx instanceof MapGrammerParser.StringExprContext stringCtx) {
            return new TextNode(stripQuotes(stringCtx.STRING().getText()));
        }

        if (ctx instanceof MapGrammerParser.NullExprContext) {
            return NullNode.instance;
        }

        if (ctx instanceof MapGrammerParser.PathExprContext pathCtx) {
            return resolvePath(pathCtx.path(), jsonCtx);
        }

        if (ctx instanceof MapGrammerParser.ParenExprContext parenCtx) {
            return evalExpr(parenCtx.expr(), jsonCtx);
        }

        if (ctx instanceof MapGrammerParser.MulDivExprContext mdCtx) {
            JsonNode left = evalExpr(mdCtx.expr(0), jsonCtx);
            JsonNode right = evalExpr(mdCtx.expr(1), jsonCtx);
            double l = left.isNumber() ? left.asDouble() : 0;
            double r = right.isNumber() ? right.asDouble() : 0;
            String op = mdCtx.getChild(1).getText();
            return switch (op) {
                case "*" -> new DoubleNode(l * r);
                case "/" -> new DoubleNode(l / r);
                default -> NullNode.instance;
            };
        }

        if (ctx instanceof MapGrammerParser.AddSubExprContext asCtx) {
            JsonNode left = evalExpr(asCtx.expr(0), jsonCtx);
            JsonNode right = evalExpr(asCtx.expr(1), jsonCtx);
            String op = asCtx.getChild(1).getText();

            if (op.equals("+") && (left.isTextual() || right.isTextual())) {
                return new TextNode(left.asText() + right.asText());
            }

            double l = left.isNumber() ? left.asDouble() : 0;
            double r = right.isNumber() ? right.asDouble() : 0;

            return switch (op) {
                case "+" -> new DoubleNode(l + r);
                case "-" -> new DoubleNode(l - r);
                default -> NullNode.instance;
            };
        }

        if (ctx instanceof MapGrammerParser.ConcatExprContext concatCtx) {
            JsonNode left = evalExpr(concatCtx.expr(0), jsonCtx);
            JsonNode right = evalExpr(concatCtx.expr(1), jsonCtx);
            return new TextNode(left.asText() + right.asText());
        }

        return NullNode.instance;
    }

    /**
     * Resolve a path to value relative to a node
     */
    private JsonNode resolvePath(MapGrammerParser.PathContext path, JsonNode ctx) {
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
     * Resolve target ObjectNode for assignment
     */
    private ObjectNode resolveScope(JsonNode root, MapGrammerParser.PathContext path) {
        if (path.IDENT().size() == 1) return (ObjectNode) root;

        JsonNode current = root;
        for (int i = 0; i < path.IDENT().size() - 1; i++) {
            current = current.get(path.IDENT(i).getText());
            if (current == null || !current.isObject()) return null;
        }
        return (ObjectNode) current;
    }

    private String stripQuotes(String s) {
        return s.substring(1, s.length() - 1);
    }
}