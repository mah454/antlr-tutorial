package ir.moke;

import ir.moke.antlr4.SqlBaseVisitor;
import ir.moke.antlr4.SqlParser;

public class EvalVisitor extends SqlBaseVisitor<String> {

    private String query ;
    @Override
    public String visitSql(SqlParser.SqlContext ctx) {
        visitChildren(ctx);
        return query;
    }

    @Override
    public String visitSelectStmt(SqlParser.SelectStmtContext ctx) {
        String tableName = ctx.tableName().getText();
        String columnList = ctx.columnList().getText();
        query = "SELECT " + columnList + " FROM " + tableName;
        if (ctx.condition() != null) {
            String condition = ctx.condition().getText();
            query += " WHERE " + condition;
        }
        return query;
    }
}