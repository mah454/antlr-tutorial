package ir.moke;

import ir.moke.antlr4.ClauseLexer;
import ir.moke.antlr4.ClauseParser;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

public class ClauseMainClass {
    public static void main(String[] args) {
        // Example input
        String inputText = "age > 25 or (name = ali or name=Hossein)";

        // Step 1: Create a CharStream from input
        CharStream input = CharStreams.fromString(inputText);

        // Step 2: Create a lexer
        ClauseLexer lexer = new ClauseLexer(input);

        // Step 3: Tokenize input
        CommonTokenStream tokens = new CommonTokenStream(lexer);

        // Step 4: Create a parser
        ClauseParser parser = new ClauseParser(tokens);

        // Step 5: Parse starting from the rule "r"
        ParseTree tree = parser.expression();

        // Print the parse tree
        System.out.println(tree.toStringTree(parser));

        // Visitor
        EvalVisitor visitor = new EvalVisitor();
        String visit = visitor.visit(tree);
        System.out.println("Result = " + visit);
    }
}
