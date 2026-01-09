// FilterGrammer.g4
grammar FilterGrammer;

import Common;  // import lexer tokens

program
    : statement* EOF
    ;

statement
    : operation '->' expression
    ;

operation
    : FILTER
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
    | interpolation
    ;

interpolation
    : '${' path '}' (('+' | '-') NUMBER)?
    ;

path
    : IDENT ('.' IDENT)*
    ;

comparator
    : '==' | '!=' | '>' | '>=' | '<' | '<='
    ;

/* Lexer rules specific to this grammar */
FILTER : 'filter';
