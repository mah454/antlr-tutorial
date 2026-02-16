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
        applyAssignment(ctx, data);
        return null;
    }

    private void applyAssignment(MapGrammerParser.AssignmentContext ctx, JsonNode root) {

        List<ObjectNode> targets = resolveAssignmentTargets(ctx.path(), root);

        if (targets.isEmpty()) return;

        String field = lastSegmentName(ctx.path());
        JsonNode previousRoot = this.currentRoot;

        for (ObjectNode target : targets) {

            this.currentRoot = target;

            JsonNode value = visit(ctx.expression());
            if (value == null) value = NullNode.getInstance();

            if (value.isNull()) {
                target.remove(field);
            } else {
                target.set(field, value.deepCopy());
            }
        }

        this.currentRoot = previousRoot;
    }

    private String lastSegmentName(MapGrammerParser.PathContext ctx) {
        if (ctx.pathSegment().isEmpty())
            throw new IllegalArgumentException("Assignment path must contain at least one segment.");
        MapGrammerParser.PathSegmentContext last = ctx.pathSegment(ctx.pathSegment().size() - 1);
        if (last.IDENT() == null || !last.arraySelector().isEmpty())
            throw new IllegalArgumentException("Assignment target must end with a plain field name.");
        return last.IDENT().getText();
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
        JsonNode left = visit(ctx.expression(0));
        JsonNode right = visit(ctx.expression(1));
        return TextNode.valueOf(left.asText() + right.asText());
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

    /* ================= FILTER SUPPORT ================= */

    private boolean evalFilter(MapGrammerParser.StatementContext ctx, JsonNode current) {

        if (ctx == null) return false;

        if (ctx.comparator() != null && ctx.stmtValue().size() == 2) {

            JsonNode left = evalStmtValue(ctx.stmtValue(0), current);
            JsonNode right = evalStmtValue(ctx.stmtValue(1), current);

            if (left == null || right == null || left.isNull() || right.isNull()) {
                return false;
            }

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

        if (ctx.statement().size() == 1 && ctx.getChildCount() == 3) {
            return evalFilter(ctx.statement(0), current);
        }

        if (ctx.statement().size() == 2) {
            String op = ctx.getChild(1).getText().toUpperCase();
            return switch (op) {
                case "OR", "||" -> evalFilter(ctx.statement(0), current) || evalFilter(ctx.statement(1), current);
                case "AND", "&&" -> evalFilter(ctx.statement(0), current) && evalFilter(ctx.statement(1), current);
                default -> false;
            };
        }

        return false;
    }

    private JsonNode evalStmtValue(MapGrammerParser.StmtValueContext ctx, JsonNode current) {
        if (ctx.STRING() != null) return TextNode.valueOf(stripQuotes(ctx.STRING().getText()));
        if (ctx.NUMBER() != null) return DoubleNode.valueOf(Double.parseDouble(ctx.NUMBER().getText()));
        if (ctx.NULL() != null) return NullNode.getInstance();
        if (ctx.path() != null) return resolvePath(ctx.path(), current);
        return NullNode.getInstance();
    }

    /* ================= PATH RESOLUTION ================= */

    private JsonNode resolvePath(MapGrammerParser.PathContext ctx, JsonNode root) {

        if (ctx == null || root == null || root.isNull()) return NullNode.getInstance();
        List<JsonNode> nodes = new ArrayList<>();
        nodes.add(root);

        for (MapGrammerParser.PathSegmentContext segment : ctx.pathSegment()) {
            nodes = traverseSegment(nodes, segment);
            if (nodes.isEmpty()) return NullNode.getInstance();
        }

        JsonNode result = nodes.getFirst();
        return result == null ? NullNode.getInstance() : result;
    }

    private List<ObjectNode> resolveAssignmentTargets(MapGrammerParser.PathContext ctx, JsonNode root) {

        if (ctx.pathSegment().isEmpty()) return List.of();

        List<JsonNode> nodes = new ArrayList<>();
        nodes.add(root);

        int lastIndex = ctx.pathSegment().size() - 1;

        for (int i = 0; i < lastIndex; i++) {
            nodes = traverseSegment(nodes, ctx.pathSegment(i));
            if (nodes.isEmpty()) break;
        }

        List<ObjectNode> targets = new ArrayList<>();
        for (JsonNode node : nodes) {
            if (node != null && node.isObject()) targets.add((ObjectNode) node);
        }

        return targets;
    }

    private List<JsonNode> traverseSegment(List<JsonNode> inputs, MapGrammerParser.PathSegmentContext segment) {

        List<JsonNode> current = new ArrayList<>();

        if (segment.IDENT() != null) {
            String field = segment.IDENT().getText();
            for (JsonNode node : inputs) {
                collectByField(node, field, current);
            }
        } else {
            current.addAll(inputs);
        }

        if (!segment.arraySelector().isEmpty()) {
            for (MapGrammerParser.ArraySelectorContext selector : segment.arraySelector()) {
                current = applyArraySelector(current, selector);
                if (current.isEmpty()) break;
            }
        }

        return current;
    }

    private void collectByField(JsonNode node, String field, List<JsonNode> collector) {

        if (node == null || node.isNull()) return;
        if (node.isObject()) {
            JsonNode child = node.get(field);
            if (child != null) collector.add(child);
        } else if (node.isArray()) {
            for (JsonNode element : node) {
                collectByField(element, field, collector);
            }
        }
    }

    private List<JsonNode> applyArraySelector(List<JsonNode> nodes, MapGrammerParser.ArraySelectorContext selector) {

        List<JsonNode> result = new ArrayList<>();
        for (JsonNode node : nodes) {
            if (node == null || node.isNull()) continue;
            if (selector.statement() != null) {
                if (node.isArray()) {
                    for (JsonNode element : node) {
                        if (element.isObject() && evalFilter(selector.statement(), element)) result.add(element);
                    }
                } else if (node.isObject() && evalFilter(selector.statement(), node)) {
                    result.add(node);
                }
                continue;
            }

            if (selector.NUMBER() != null) {
                if (node.isArray()) {
                    int idx = Integer.parseInt(selector.NUMBER().getText());
                    if (idx >= 0 && idx < node.size()) result.add(node.get(idx));
                }
                continue;
            }

            if (node.isArray()) {
                node.forEach(result::add);
            } else if (node.isObject()) {
                result.add(node);
            }
        }

        return result;
    }

    /* ================= UTILITIES ================= */

    private String stripQuotes(String s) {
        if (s == null || s.length() < 2) return "";
        return s.substring(1, s.length() - 1);
    }
}