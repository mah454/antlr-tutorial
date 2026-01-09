grammar MapGrammer;

import Common;

// program = series of map statements
program
    : statement* EOF
    ;

// a statement is: map -> assignment
statement
    : MAP '->' assignment
    ;

// assignment: path = expression
assignment
    : path '=' expr
    ;

// expressions with operator precedence
expr
    : expr '+' expr           # concatExpr        // string concatenation
    | expr '*' expr           # mulDivExpr        // multiplication
    | expr '/' expr           # mulDivExpr
    | expr '-' expr           # addSubExpr
    | expr '+' expr           # addSubExpr
    | '(' expr ')'            # parenExpr
    | NUMBER                  # numberExpr
    | STRING                  # stringExpr
    | NULL                    # nullExpr
    | path                    # pathExpr
    ;

// path = nested identifiers
path
    : IDENT ('.' IDENT)*
    ;

/* Lexer rules */
MAP    : 'map';