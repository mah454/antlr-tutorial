package ir.moke;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import ir.moke.antlr4.FilterGrammerLexer;
import ir.moke.antlr4.FilterGrammerParser;
import ir.moke.antlr4.MapGrammerLexer;
import ir.moke.antlr4.MapGrammerParser;
import ir.moke.visitor.FilterEvalVisitor;
import ir.moke.visitor.MapEvalVisitor;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import java.util.List;
import java.util.Objects;

public class JsonStream {
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final String MAP_SIGNATURE = "map -> ";
    private static final String FILTER_SIGNATURE = "filter -> ";
    private String jsonData;
    private JsonNode node;

    static {
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        DefaultPrettyPrinter prettyPrinter = new DefaultPrettyPrinter();
        prettyPrinter.indentArraysWith(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE);
        mapper.writer(prettyPrinter);
        mapper.setDefaultPrettyPrinter(prettyPrinter);
    }

    private JsonStream(String jsonData) {
        try {
            this.jsonData = jsonData;
            this.node = mapper.readTree(jsonData);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private JsonStream(JsonNode node) {
        this.node = node;
        this.jsonData = node.toString();
    }

    public static JsonStream of(String jsonData) {
        return new JsonStream(Objects.requireNonNull(jsonData));
    }

    public static JsonStream of(JsonNode node) {
        return new JsonStream(Objects.requireNonNull(node));
    }

    public JsonStream filter(String filterClause) {
        try {
            if (filterClause.contains("->")) {
                String action = filterClause.split("->", 2)[0];
                if (action.isEmpty() || !action.trim().equalsIgnoreCase("filter")) {
                    throw new IllegalStateException("filter clause should started with : 'filter ->'");
                }
            } else {
                filterClause = FILTER_SIGNATURE + filterClause.trim();
            }

            /* Start filtering */
            CharStream input = CharStreams.fromString(filterClause);

            // Step 2: Create a lexer
            FilterGrammerLexer lexer = new FilterGrammerLexer(input);

            // Step 3: Tokenize input
            CommonTokenStream tokens = new CommonTokenStream(lexer);

            // Step 4: Create a parser
            FilterGrammerParser parser = new FilterGrammerParser(tokens);
            FilterGrammerParser.ProgramContext program = parser.program();


            // json to ArrayNode Object
            node = new ObjectMapper().readTree(jsonData);

            // Run visitor
            FilterEvalVisitor visitor = new FilterEvalVisitor(node);
            visitor.visit(program);

            this.jsonData = node.toString();
            return this;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public JsonStream map(String mapClause) {
        try {
            if (mapClause.contains("->")) {
                String action = mapClause.split("->", 2)[0];
                if (action.isEmpty() || !action.trim().equalsIgnoreCase("map")) {
                    throw new IllegalStateException("map clause should started with : 'map ->'");
                }
            } else {
                mapClause = MAP_SIGNATURE + mapClause.trim();
            }

            /* Start mapping */
            CharStream inputStream = CharStreams.fromString(mapClause);

            // Step 1: Create a lexer
            MapGrammerLexer lexer = new MapGrammerLexer(inputStream);

            // Step 2: Tokenize input
            CommonTokenStream tokens = new CommonTokenStream(lexer);

            // Step 3: Create a parser
            MapGrammerParser parser = new MapGrammerParser(tokens);
            MapGrammerParser.ProgramContext program = parser.program();

            // Step 4: parse jsonData
            node = new ObjectMapper().readTree(jsonData);

            // Step 5: Run visitor
            MapEvalVisitor visitor = new MapEvalVisitor(node);
            visitor.visit(program);

            this.jsonData = node.toString();
            return this;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public JsonStream apply(List<String> clauses) {
        for (String clause : clauses) {
            if (clause == null || clause.isEmpty()) continue;
            if (clause.toLowerCase().startsWith("filter")) {
                this.node = filter(clause).toJsonNode();
            } else if (clause.toLowerCase().startsWith("map")) {
                this.node = map(clause).toJsonNode();
            } else {
                throw new IllegalStateException("clause should start with 'map|filter'");
            }
        }

        return this;
    }

    public JsonStream apply(String clause) {
        if (clause.toLowerCase().startsWith("filter")) {
            this.node = filter(clause).toJsonNode();
        } else if (clause.toLowerCase().startsWith("map")) {
            this.node = map(clause).toJsonNode();
        } else {
            throw new IllegalStateException("clause should start with 'map|filter'");
        }
        return this;
    }

    public JsonNode toJsonNode() {
        return node;
    }

    public void prettyPrint() {
        System.out.println(prettyString());
    }

    public String prettyString() {
        try {
            return mapper.writeValueAsString(node);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public void print() {
        System.out.println(node.toString());
    }

    @Override
    public String toString() {
        return node.toString();
    }
}
