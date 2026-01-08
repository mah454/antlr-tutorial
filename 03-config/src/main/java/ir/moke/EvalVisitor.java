package ir.moke;

import ir.moke.antlr4.ConfigBaseVisitor;
import ir.moke.antlr4.ConfigParser;

import java.util.HashMap;
import java.util.Map;

public class EvalVisitor extends ConfigBaseVisitor<Map<String, String>> {

    private final Map<String, String> config = new HashMap<>();

    @Override
    public Map<String, String> visitConfig(ConfigParser.ConfigContext ctx) {
        visitChildren(ctx);
        return config;
    }

    @Override
    public Map<String, String> visitPair(ConfigParser.PairContext ctx) {
        String key = ctx.STRING().getText();
        String value = ctx.value().getText();
        config.put(key, value);
        return config;
    }
}