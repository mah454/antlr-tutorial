grammar MapGrammer;

import Common;

program
    : clauses* EOF
    ;

clauses
    : MAP '->' assignment
    ;

assignment
    : path '=' expression
    ;

expression
    : expression '+' expression                # concatExpr        // string concatenation
    | expression mathOperation expression      # mathExpr          // math operation
    | '(' expression ')'                       # parenExpr
    | NUMBER                                   # numberExpr
    | STRING                                   # stringExpr
    | NULL                                     # nullExpr
    | path                                     # pathExpr
    ;

path
    : pathSegment ('.' pathSegment)*
    ;

pathSegment
    : IDENT
    | IDENT '[' ']'
    | IDENT '[' NUMBER ']'
    | IDENT '[' statement ']'
    | '[' ']' '.' IDENT
    | '[' statement ']' '.' IDENT
    ;

statement
    : statement OR statement
    | statement AND statement
    | '(' statement ')'
    | comparisonExpr
    | NUMBER
    ;

comparisonExpr
    : stmtValue comparator stmtValue
    ;

stmtValue
    : STRING
    | NUMBER
    | NULL
    | path
    ;

comparator
    : '==' | '!=' | '>' | '>=' | '<' | '<=' | '~' | '!~'
    ;

mathOperation
    : '+' | '-' | '*' | '/'
    ;

MAP    : 'map';