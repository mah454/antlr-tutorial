package ir.moke.visitor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private static final ObjectMapper mapper = new ObjectMapper();
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
        this.currentRoot = root; // CRITICAL

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

        /*
         * OR
         */
        if (ctx.OR() != null) {
            return evalFilter(ctx.statement(0), current)
                    || evalFilter(ctx.statement(1), current);
        }

        /*
         * AND
         */
        if (ctx.AND() != null) {
            return evalFilter(ctx.statement(0), current)
                    && evalFilter(ctx.statement(1), current);
        }

        /*
         * Parentheses
         */
        if (ctx.statement().size() == 1) {
            return evalFilter(ctx.statement(0), current);
        }

        /*
         * NUMBER → truthy check
         */
        if (ctx.NUMBER() != null) {
            return Double.parseDouble(ctx.NUMBER().getText()) != 0;
        }

        /*
         * Comparison
         */
        if (ctx.comparisonExpr() != null) {
            return evalComparison(ctx.comparisonExpr(), current);
        }

        return false;
    }

    private boolean evalComparison(MapGrammerParser.ComparisonExprContext ctx, JsonNode node) {
        if (ctx == null) return false;
        JsonNode left = readValue(ctx.stmtValue(0), node);
        JsonNode right = readValue(ctx.stmtValue(1), node);
        if (left == null || right == null) return false;
        String cmp = ctx.comparator().getText();

        if (left.isArray() || right.isArray()) {
            for (JsonNode l : left.isArray() ? left : List.of(left)) {
                for (JsonNode r : right.isArray() ? right : List.of(right)) {
                    if (compare(l, r, cmp)) return true;
                }
            }
            return false;
        }

        return compare(left, right, cmp);
    }

    private boolean compare(JsonNode left, JsonNode right, String cmp) {
        if (left.isNumber() && right.isNumber()) return checkNumeric(left.doubleValue(), right.doubleValue(), cmp);
        return checkString(left.textValue(), right.textValue(), cmp);
    }

    private boolean checkString(String l, String r, String cmp) {
        return switch (cmp) {
            case "=" -> l.equalsIgnoreCase(r);
            case "==" -> Objects.equals(l, r);
            case "!=" -> !l.equalsIgnoreCase(r);
            case "!==" -> !Objects.equals(l, r);
            case ">" -> l.compareTo(r) > 0;
            case ">=" -> l.compareTo(r) >= 0;
            case "<" -> l.compareTo(r) < 0;
            case "<=" -> l.compareTo(r) <= 0;
            case "~" -> l.toLowerCase().contains(r.toLowerCase());
            case "!~" -> !l.toLowerCase().contains(r.toLowerCase());
            default -> false;
        };
    }

    private boolean checkNumeric(double l, double r, String cmp) {
        return switch (cmp) {
            case "=", "==" -> l == r;
            case "!=", "!==" -> l != r;
            case ">" -> l > r;
            case ">=" -> l >= r;
            case "<" -> l < r;
            case "<=" -> l <= r;
            case "~", "!~" -> throw new IllegalArgumentException("Numeric contains operators not supported");
            default -> false;
        };
    }

    private JsonNode readValue(MapGrammerParser.StmtValueContext ctx, JsonNode node) {
        if ("@".equals(ctx.getText())) return node;
        if (ctx.STRING() != null) return TextNode.valueOf(stripQuotes(ctx.STRING().getText()));
        if (ctx.NUMBER() != null) return DoubleNode.valueOf(Double.parseDouble(ctx.NUMBER().getText()));
        if (ctx.NULL() != null) return NullNode.getInstance();
        if (ctx.path() != null) return resolvePath(ctx.path(), node);
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

                /*
                 * ROOT ARRAY EXPANSION → []
                 */
                if (seg.getText().equals("[]")) {
                    if (node.isArray()) {
                        node.forEach(next::add);
                    }
                    continue;
                }

                if (!node.isObject()) continue;

                JsonNode child = node.get(seg.IDENT().getText());
                if (child == null) continue;

                /*
                 * address[]
                 */
                if (seg.getText().endsWith("[]")) {
                    if (child.isArray()) {
                        child.forEach(next::add);
                    }
                }

                /*
                 * address[NUMBER]
                 */
                else if (seg.NUMBER() != null && child.isArray()) {
                    int idx = Integer.parseInt(seg.NUMBER().getText());
                    if (idx >= 0 && idx < child.size()) {
                        next.add(child.get(idx));
                    }
                }

                /*
                 * address[filter]
                 */
                else if (seg.statement() != null && child.isArray()) {
                    for (JsonNode e : child) {
                        if (e.isObject() && evalFilter(seg.statement(), e)) {
                            next.add(e);
                        }
                    }
                }

                /*
                 * NORMAL OBJECT
                 */
                else {
                    next.add(child);
                }
            }

            current = next;
        }

        return current.stream()
                .filter(JsonNode::isObject)
                .map(n -> (ObjectNode) n)
                .toList();
    }

    /* ================= UTIL ================= */

    private String stripQuotes(String s) {
        return s.substring(1, s.length() - 1);
    }
}