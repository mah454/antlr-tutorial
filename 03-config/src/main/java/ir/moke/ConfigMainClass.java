package ir.moke;

import ir.moke.antlr4.ConfigLexer;
import ir.moke.antlr4.ConfigParser;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.Map;

public class ConfigMainClass {
    public static void main(String[] args) {
        // Example input
        String inputText = """
                host = 127.0.0.1
                port = 8080
                username = admin
                password = adminpass
                """;

        // Step 1: Create a CharStream from input
        CharStream input = CharStreams.fromString(inputText);

        // Step 2: Create a lexer
        ConfigLexer lexer = new ConfigLexer(input);

        // Step 3: Tokenize input
        CommonTokenStream tokens = new CommonTokenStream(lexer);

        // Step 4: Create a parser
        ConfigParser parser = new ConfigParser(tokens);

        // Step 5: Parse starting from the rule "r"
        ParseTree tree = parser.config();

        // Print the parse tree
        System.out.println(tree.toStringTree(parser));

        // Visitor
        EvalVisitor visitor = new EvalVisitor();
        Map<String, String> configMap = visitor.visit(tree);
        System.out.println("Result = " + configMap);
    }
}
