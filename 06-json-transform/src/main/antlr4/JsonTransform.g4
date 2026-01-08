grammar JsonTransform;

/* =======================
   Parser Rules
   ======================= */

program
    : statement* EOF
    ;

statement
    : operation '->' expression
    ;

operation
    : FILTER
    | MAP
    | MUTE
    | APPEND path?
    | RENAME path?
    | REPLACE path?
    | MAP path
    ;

expression
    : assignment
    | logicalExpr
    ;

assignment
    : path '=' valueExpr
    ;

logicalExpr
    : logicalExpr OR logicalExpr
    | logicalExpr AND logicalExpr
    | comparison
    ;

comparison
    : valueExpr comparator valueExpr
    ;

valueExpr
    : arithmeticExpr
    | STRING
    | NUMBER
    | NULL
    | path
    | interpolation
    ;

arithmeticExpr
    : arithmeticExpr op=('*' | '/') arithmeticExpr
    | arithmeticExpr op=('+' | '-') arithmeticExpr
    | NUMBER
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

/* =======================
   Lexer Rules
   ======================= */

FILTER   : 'filter';
MAP      : 'map';
MUTE     : 'mute';
APPEND   : 'append';
RENAME   : 'rename';
REPLACE  : 'replace';

AND      : 'and';
OR       : 'or';

NULL     : 'null';

IDENT
    : [a-zA-Z_][a-zA-Z_0-9]*
    ;

NUMBER
    : [0-9]+
    ;

STRING
    : '"' (~["\\] | '\\' .)* '"'
    ;

WS
    : [ \t\r\n]+ -> skip
    ;
