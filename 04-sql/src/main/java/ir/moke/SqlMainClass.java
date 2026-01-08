package ir.moke;

import ir.moke.antlr4.SqlLexer;
import ir.moke.antlr4.SqlParser;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

public class SqlMainClass {
    public static void main(String[] args) {
        // Example input
//        String inputText = "SELECT name, age FROM users WHERE age > 18";
        String inputText = "SELECT * FROM users WHERE age > 18 and (name='Mahdi' or name='javad')";

        // Step 1: Create a CharStream from input
        CharStream input = CharStreams.fromString(inputText);

        // Step 2: Create a lexer
        SqlLexer lexer = new SqlLexer(input);

        // Step 3: Tokenize input
        CommonTokenStream tokens = new CommonTokenStream(lexer);

        // Step 4: Create a parser
        SqlParser parser = new SqlParser(tokens);

        // Step 5: Parse starting from the rule "r"
        ParseTree tree = parser.sql();

        // Print the parse tree
        System.out.println(tree.toStringTree(parser));

        // Visitor
        EvalVisitor visitor = new EvalVisitor();
        String visit = visitor.visit(tree);
        System.out.println("Result = " + visit);
    }
}
