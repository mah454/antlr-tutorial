package ir.moke;

import ir.moke.antlr4.ExprLexer;
import ir.moke.antlr4.ExprParser;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

public class ExprMainClass {
    public static void main(String[] args) {
        // Example input
//        String inputText = "3+4*5\n(1+2)*3\n";
        String inputText = "3+2\n";

        // Step 1: Create a CharStream from input
        CharStream input = CharStreams.fromString(inputText);

        // Step 2: Create a lexer
        ExprLexer lexer = new ExprLexer(input);

        // Step 3: Tokenize input
        CommonTokenStream tokens = new CommonTokenStream(lexer);

        // Step 4: Create a parser
        ExprParser parser = new ExprParser(tokens);

        // Step 5: Parse starting from the rule "r"
        ParseTree tree = parser.prog();

        // Print the parse tree
        System.out.println(tree.toStringTree(parser));

        // Visitor
        EvalVisitor visitor = new EvalVisitor();
        int result = visitor.visit(tree);
        System.out.println("Result = " + result);
    }
}
