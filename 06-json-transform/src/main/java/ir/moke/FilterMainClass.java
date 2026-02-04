package ir.moke;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import ir.moke.antlr4.FilterGrammerLexer;
import ir.moke.antlr4.FilterGrammerParser;
import ir.moke.visitor.FilterEvalVisitor;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

public class FilterMainClass {
    private static final String jsonData = """
            [
              {
                "username": "aaa",
                "password": "1234",
                "profile": {
                  "name": "Ali",
                  "family": "Mohammadi",
                  "age": 45,
                  "account" : 9287311,
                  "contact": {
                    "city": "Tehran",
                    "zip": 12345
                  }
                }
              },
              {
                "username": "bbb",
                "password": "1234",
                "profile": {
                  "name": "Mahdi",
                  "family": "Sheikh Hosseini",
                  "age": 21,
                  "account" : 12456789794,
                  "contact": {
                    "city": "Pardis",
                    "zip": 99999
                  }
                }
              },
              {
                "username": "ccc",
                "password": "1234",
                "profile": {
                  "name": "Hossein",
                  "family": "Javadi",
                  "age": 33,
                  "account" : 11111111111111,
                  "contact": {
                    "city": "Eghlid",
                    "zip": 4444
                  }
                }
              }
            ]
            """;

    public static void main(String[] args) throws JsonProcessingException {
        // Example input
        String inputText = "filter -> profile.age == 45 or (profile.contact.city == \"Eghlid\" or profile.contact.city == \"Pardis\")";
//        String inputText = "filter -> profile.age == 45 or profile.age == 33";
        System.out.println(inputText);
        // Step 1: Create a CharStream from input
        CharStream input = CharStreams.fromString(inputText);

        // Step 2: Create a lexer
        FilterGrammerLexer lexer = new FilterGrammerLexer(input);

        // Step 3: Tokenize input
        CommonTokenStream tokens = new CommonTokenStream(lexer);

        // Step 4: Create a parser
        FilterGrammerParser parser = new FilterGrammerParser(tokens);

        FilterGrammerParser.ProgramContext program = parser.program();

        // Print the parse tree
        System.out.println(program.toStringTree(parser));

        // json to ArrayNode Object
        ArrayNode json = (ArrayNode) new ObjectMapper().readTree(jsonData);

        // Run visitor
        FilterEvalVisitor visitor = new FilterEvalVisitor(json);
        visitor.visit(program);


        // Visitor
        System.out.println(new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(json));
    }
}
