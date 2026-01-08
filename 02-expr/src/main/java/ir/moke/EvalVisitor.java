package ir.moke;

import ir.moke.antlr4.ExprBaseVisitor;
import ir.moke.antlr4.ExprParser;

public class EvalVisitor extends ExprBaseVisitor<Integer> {
    @Override
    public Integer visitProg(ExprParser.ProgContext ctx) {
        int result = 0;
        for (ExprParser.ExprContext e : ctx.expr()) {
            result = visit(e);
        }
        return result;
    }

    @Override
    public Integer visitExpr(ExprParser.ExprContext ctx) {
        if (ctx.INT() != null) {
            return Integer.valueOf(ctx.INT().getText());
        }

        if (ctx.getChildCount() == 3) {
            if (ctx.getChild(0).getText().equals("(")) {
                return visit(ctx.expr(0));
            }
        }

        Integer left = visit(ctx.expr(0));
        Integer right = visit(ctx.expr(1));
        String op = ctx.getChild(1).getText();
        return switch (op) {
            case "*" -> left * right;
            case "/" -> left / right;
            case "+" -> left + right;
            case "-" -> left - right;
            default -> throw new RuntimeException("Unknown op: " + op);
        };
    }
}
