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

    private void applyAssignment(MapGrammerParser.AssignmentContext ctx, JsonNode root) {
        List<ObjectNode> targets = resolveTargetNodes(ctx.path(), root);
        String fieldName = lastSegmentName(ctx.path());

        for (ObjectNode target : targets) {
            JsonNode value = evalExpr(ctx.expr(), root);

            if (value.isNull()) {
                target.remove(fieldName);
            } else {
                target.set(fieldName, value.deepCopy());
            }
        }
    }

    private String lastSegmentName(MapGrammerParser.PathContext ctx) {
        MapGrammerParser.PathSegmentContext seg = ctx.pathSegment(ctx.pathSegment().size() - 1);
        return seg.IDENT().getText();
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
            JsonNode leftNode = evalExpr(concatCtx.expr(0), rootNode);
            JsonNode rightNode = evalExpr(concatCtx.expr(1), rootNode);
            String l = leftNode.isNull() ? "" : leftNode.asText();
            String r = rightNode.isNull() ? "" : rightNode.asText();
            if (l == null && r == null) return NullNode.getInstance();
            return new TextNode((l + r).trim());
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

    private List<ObjectNode> resolveTargetNodes(MapGrammerParser.PathContext ctx, JsonNode root) {
        List<JsonNode> current = List.of(root);

        for (int i = 0; i < ctx.pathSegment().size() - 1; i++) {
            MapGrammerParser.PathSegmentContext seg = ctx.pathSegment(i);
            List<JsonNode> next = new ArrayList<>();

            for (JsonNode node : current) {
                String field = seg.IDENT().getText();
                JsonNode child = node.get(field);

                if (child == null) continue;

                // address[]
                if (seg.NUMBER() == null && seg.getText().endsWith("[]") && child.isArray()) {
                    child.forEach(next::add);
                }
                // address[2]
                else if (seg.NUMBER() != null && child.isArray()) {
                    next.add(child.get(Integer.parseInt(seg.NUMBER().getText())));
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


    /**
     * remove double quotes
     *
     */
    private String stripQuotes(String s) {
        return s.substring(1, s.length() - 1);
    }
}