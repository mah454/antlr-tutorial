package ir.moke.visitor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import ir.moke.antlr4.MapGrammerBaseVisitor;
import ir.moke.antlr4.MapGrammerParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MapEvalVisitor extends MapGrammerBaseVisitor<JsonNode> {
    private JsonNode currentRoot;
    private final JsonNode data;

    public MapEvalVisitor(JsonNode data) {
        this.data = data;
    }

    /* ================= PROGRAM ================= */

    @Override
    public JsonNode visitProgram(MapGrammerParser.ProgramContext ctx) {
        ctx.clauses().forEach(this::visit);
        return null;
    }

    @Override
    public JsonNode visitClauses(MapGrammerParser.ClausesContext ctx) {
        return visit(ctx.assignment());
    }

    @Override
    public JsonNode visitAssignment(MapGrammerParser.AssignmentContext ctx) {
        if (data.isArray()) {
            data.forEach(n -> applyAssignment(ctx, n));
        } else {
            applyAssignment(ctx, data);
        }
        return null;
    }

    private void applyAssignment(MapGrammerParser.AssignmentContext ctx, JsonNode root) {
        this.currentRoot = root; // ✅ CRITICAL

        List<ObjectNode> targets = resolveTargetNodes(ctx.path(), root);
        String field = lastSegmentName(ctx.path());

        JsonNode value = visit(ctx.expression());

        for (ObjectNode target : targets) {
            if (value.isNull()) {
                target.remove(field);
            } else {
                target.set(field, value.deepCopy());
            }
        }
    }

    private String lastSegmentName(MapGrammerParser.PathContext ctx) {
        return ctx.pathSegment(ctx.pathSegment().size() - 1)
                .IDENT()
                .getText();
    }

    /* ================= EXPRESSIONS ================= */

    @Override
    public JsonNode visitNullExpr(MapGrammerParser.NullExprContext ctx) {
        return NullNode.getInstance();
    }

    @Override
    public JsonNode visitStringExpr(MapGrammerParser.StringExprContext ctx) {
        return TextNode.valueOf(stripQuotes(ctx.STRING().getText()));
    }

    @Override
    public JsonNode visitNumberExpr(MapGrammerParser.NumberExprContext ctx) {
        return DoubleNode.valueOf(Double.parseDouble(ctx.NUMBER().getText()));
    }

    @Override
    public JsonNode visitPathExpr(MapGrammerParser.PathExprContext ctx) {
        return resolvePath(ctx.path(), currentRoot);
    }

    @Override
    public JsonNode visitParenExpr(MapGrammerParser.ParenExprContext ctx) {
        return visit(ctx.expression());
    }

    @Override
    public JsonNode visitConcatExpr(MapGrammerParser.ConcatExprContext ctx) {
        return TextNode.valueOf(visit(ctx.expression(0)).asText() + visit(ctx.expression(1)).asText()
        );
    }

    @Override
    public JsonNode visitMathExpr(MapGrammerParser.MathExprContext ctx) {
        JsonNode l = visit(ctx.expression(0));
        JsonNode r = visit(ctx.expression(1));

        double lv = l.isNumber() ? l.asDouble() : 0;
        double rv = r.isNumber() ? r.asDouble() : 0;

        return switch (ctx.mathOperation().getText()) {
            case "+" -> DoubleNode.valueOf(lv + rv);
            case "-" -> DoubleNode.valueOf(lv - rv);
            case "*" -> DoubleNode.valueOf(lv * rv);
            case "/" -> DoubleNode.valueOf(lv / rv);
            default -> NullNode.getInstance();
        };
    }

    /* ================= FILTER ================= */

    private boolean evalFilter(MapGrammerParser.StatementContext ctx, JsonNode current) {

        JsonNode left = evalStmtValue(ctx.stmtValue(0), current);
        JsonNode right = evalStmtValue(ctx.stmtValue(1), current);

        if (left.isNull() || right.isNull()) return false;

        return switch (ctx.comparator().getText()) {
            case "==" -> left.equals(right);
            case "!=" -> !left.equals(right);
            case ">" -> left.asDouble() > right.asDouble();
            case ">=" -> left.asDouble() >= right.asDouble();
            case "<" -> left.asDouble() < right.asDouble();
            case "<=" -> left.asDouble() <= right.asDouble();
            case "~" -> left.asText().contains(right.asText());
            case "!~" -> !left.asText().contains(right.asText());
            default -> false;
        };
    }

    private JsonNode evalStmtValue(MapGrammerParser.StmtValueContext ctx, JsonNode current) {

        if (ctx.STRING() != null)
            return TextNode.valueOf(stripQuotes(ctx.STRING().getText()));

        if (ctx.NUMBER() != null)
            return DoubleNode.valueOf(Double.parseDouble(ctx.NUMBER().getText()));

        if (ctx.NULL() != null)
            return NullNode.getInstance();

        if (ctx.path() != null)
            return resolvePath(ctx.path(), current);

        return NullNode.getInstance();
    }

    /* ================= PATH ================= */

    private JsonNode resolvePath(MapGrammerParser.PathContext ctx, JsonNode root) {

        JsonNode current = root;

        for (var seg : ctx.pathSegment()) {
            if (!current.isObject()) return NullNode.instance;

            current = current.get(seg.IDENT().getText());
            if (current == null) return NullNode.instance;
        }
        return current;
    }

    /* ================= TARGET RESOLUTION ================= */

    private List<ObjectNode> resolveTargetNodes(MapGrammerParser.PathContext ctx, JsonNode root) {

        List<JsonNode> current = List.of(root);

        for (int i = 0; i < ctx.pathSegment().size() - 1; i++) {
            MapGrammerParser.PathSegmentContext seg = ctx.pathSegment(i);
            List<JsonNode> next = new ArrayList<>();

            for (JsonNode node : current) {
                if (!node.isObject()) continue;

                JsonNode child = node.get(seg.IDENT().getText());
                if (child == null) continue;

                // address[NUMBER]
                if (seg.NUMBER() != null && child.isArray()) {
                    int idx = Integer.parseInt(seg.NUMBER().getText());
                    if (idx >= 0 && idx < child.size()) {
                        next.add(child.get(idx));
                    }
                }
                // address[ filter ]
                else if (seg.statement() != null && child.isArray()) {
                    for (JsonNode e : child) {
                        if (e.isObject() && evalFilter(seg.statement(), e)) {
                            next.add(e);
                        }
                    }
                }
                // normal object
                else {
                    next.add(child);
                }
            }
            current = next;
        }

        return current.stream()
                .filter(Objects::nonNull)
                .filter(JsonNode::isObject)
                .map(n -> (ObjectNode) n)
                .toList();
    }

    /* ================= UTIL ================= */

    private String stripQuotes(String s) {
        return s.substring(1, s.length() - 1);
    }
}