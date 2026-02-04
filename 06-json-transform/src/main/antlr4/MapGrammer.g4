grammar MapGrammer;

import Common;

program
    : statement* EOF
    ;

statement
    : MAP '->' assignment
    ;

assignment
    : path '=' expr
    ;

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

path
    : pathSegment ('.' pathSegment)*
    ;

pathSegment
    : IDENT ('[' ']')?
    | ('[' ']').IDENT
    ;

MAP    : 'map';