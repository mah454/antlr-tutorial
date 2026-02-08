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
    | IDENT '[' ']'                        // address[]
    | IDENT '[' NUMBER ']'                 // address[2]
    | IDENT '[' statement ']'             // address[state == "X"]
    | '[' ']' '.' IDENT
    | '[' statement ']' '.' IDENT
    ;

statement
    : stmtValue comparator stmtValue
    | NUMBER
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