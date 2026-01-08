package ir.moke;

import ir.moke.antlr4.ClauseBaseVisitor;
import ir.moke.antlr4.ClauseParser;

import java.util.ArrayList;
import java.util.List;

public class EvalVisitor extends ClauseBaseVisitor<String> {

    @Override
    public String visitExpression(ClauseParser.ExpressionContext ctx) {
        return visit(ctx.orExpr());
    }

    @Override
    public String visitOrExpr(ClauseParser.OrExprContext ctx) {
        if (ctx.OR().isEmpty()) {
            // Single AND expression
            return visit(ctx.andExpr(0));
        } else {
            // Multiple OR expressions
            List<String> orParts = new ArrayList<>();
            for (ClauseParser.AndExprContext andCtx : ctx.andExpr()) {
                orParts.add(visit(andCtx));
            }
            return "{ $and: [ %s ] }".formatted(String.join(", ", orParts)).trim();
        }
    }

    @Override
    public String visitAndExpr(ClauseParser.AndExprContext ctx) {
        if (ctx.AND().isEmpty()) {
            return visit(ctx.primary(0));
        } else {
            List<String> andParts = new ArrayList<>();
            for (ClauseParser.PrimaryContext pCtx : ctx.primary()) {
                andParts.add(visit(pCtx));
            }
            return "{ $and: [ %s ] }".formatted(String.join(", ", andParts)).trim();
        }
    }

    @Override
    public String visitPrimary(ClauseParser.PrimaryContext ctx) {
        if (ctx.comparison() != null) {
            return visit(ctx.comparison());
        } else if (ctx.orExpr() != null) {
            return visit(ctx.orExpr());
        }
        return "";
    }

    @Override
    public String visitComparison(ClauseParser.ComparisonContext ctx) {
        String field = ctx.IDENTIFIER().getText();
        String op = ctx.operator().getText();
        String value = visit(ctx.operand());

        String mongoOp = switch (op) {
            case "!=", "ne" -> "$ne";
            case ">", "gt" -> "$gt";
            case ">=", "gte" -> "$gte";
            case "<", "lt" -> "$lt";
            case "<=", "lte" -> "$lte";
            case "~", "reg", "!~", "regi" -> "$regex";

            default -> "$eq";
        };

        if (op.equals("regi") || op.equals("!~")) {
            return """
                    { "%s" : { %s : %s , $options: "i" } }
                    """.formatted(field, mongoOp, value).trim();
        } else {
            return """
                    { "%s" : { %s : %s } }
                    """.formatted(field, mongoOp, value).trim();
        }
    }

    @Override
    public String visitOperand(ClauseParser.OperandContext ctx) {
        if (ctx.STRING() != null) {
            String text = ctx.STRING().getText();
            // If the value is quoted, keep it as string
            if (text.startsWith("\"") && text.endsWith("\"")) {
                return text;
            } else {
                // It's unquoted identifier, treat as string for Mongo
                return "\"" + text + "\"";
            }
        } else if (ctx.NUMBER() != null) {
            return ctx.NUMBER().getText();
        } else if (ctx.IDENTIFIER() != null) {
            return "\"" + ctx.IDENTIFIER().getText() + "\"";
        }
        return "";
    }
}
