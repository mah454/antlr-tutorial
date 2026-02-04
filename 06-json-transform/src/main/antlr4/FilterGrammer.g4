grammar FilterGrammer;

import Common;

program
    : statement* EOF
    ;

statement
    : FILTER '->' expression
    ;

expression
    : expression OR expression
    | expression AND expression
    | '(' expression ')'
    | comparison
    ;

comparison
    : valueExpr comparator valueExpr
    ;

valueExpr
    : STRING
    | NUMBER
    | NULL
    | path
    ;

path
    : pathSegment ('.' pathSegment)*
    ;

pathSegment
    : IDENT ('[' ']')?
    | ('[' ']').IDENT
    ;

comparator
    : '==' | '!=' | '>' | '>=' | '<' | '<=' | '~' | '!~'
    ;

FILTER : 'filter';
