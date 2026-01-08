package ir.moke;

import ir.moke.antlr4.HelloBaseVisitor;
import ir.moke.antlr4.HelloParser;

public class EvalVisitor extends HelloBaseVisitor<String> {
    @Override
    public String visitR(HelloParser.RContext ctx) {
        String name = ctx.ID().getText();
        return "Hello, " + name + " !";
    }
}
