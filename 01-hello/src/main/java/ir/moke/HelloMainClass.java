package ir.moke;

import ir.moke.antlr4.HelloLexer;
import ir.moke.antlr4.HelloParser;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

public class HelloMainClass {
    public static void main(String[] args) {
// Example input
        String inputText = "hello world";

        // Step 1: Create a CharStream from input
        CharStream input = CharStreams.fromString(inputText);

        // Step 2: Create a lexer
        HelloLexer lexer = new HelloLexer(input);

        // Step 3: Tokenize input
        CommonTokenStream tokens = new CommonTokenStream(lexer);

        // Step 4: Create a parser
        HelloParser parser = new HelloParser(tokens);

        // Step 5: Parse starting from the rule "r"
        ParseTree tree = parser.r();

        // Print the parse tree
        System.out.println(tree.toStringTree(parser));

        // Visitor
        EvalVisitor visitor = new EvalVisitor();
        String result = visitor.visit(tree);
        System.out.println("Result = " + result);
    }
}
