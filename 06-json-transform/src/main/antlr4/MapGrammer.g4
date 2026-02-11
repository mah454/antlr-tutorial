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
    : IDENT arraySelector*
    | arraySelector
    ;

arraySelector
    : '[' ']'
    | '[' NUMBER ']'
    | '[' statement ']'
    ;

statement
    : stmtValue comparator stmtValue
    | statement OR statement
    | statement AND statement
    | '(' statement ')'
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
