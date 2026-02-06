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
    : expr '+' expr              # concatExpr        // string concatenation
    | expr mathOperation expr    # mathExpr          // math operation
    | '(' expr ')'               # parenExpr
    | NUMBER                     # numberExpr
    | STRING                     # stringExpr
    | NULL                       # nullExpr
    | path                       # pathExpr
    ;

path
    : pathSegment ('.' pathSegment)*
    ;

pathSegment
    : IDENT ('[' ']')?
    | IDENT ('[' NUMBER ']')?
    | ('[' ']').IDENT
    | ('[' NUMBER ']').IDENT
    ;

mathOperation
    : '+' | '-' | '*' | '/'
    ;

MAP    : 'map';