package ir.moke;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ir.moke.antlr4.MapGrammerLexer;
import ir.moke.antlr4.MapGrammerParser;
import ir.moke.visitor.MapEvalVisitor;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

public class MapMainClass {
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
//        String input = "map -> profile.name = \"jafar\"";
//        String input = "map -> profile.age = profile.age * 2";
//        String input = "map -> profile.name = null";
        String input = "map -> profile.contact.fullName = profile.name + \" \" + profile.family";

        CharStream inputStream = CharStreams.fromString(input);
        MapGrammerLexer lexer = new MapGrammerLexer(inputStream);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        MapGrammerParser parser = new MapGrammerParser(tokens);

        MapGrammerParser.ProgramContext program = parser.program();
        JsonNode json = new ObjectMapper().readTree(jsonData);

        MapEvalVisitor visitor = new MapEvalVisitor(json);
        visitor.visit(program);

        System.out.println(new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(json));
    }
}
