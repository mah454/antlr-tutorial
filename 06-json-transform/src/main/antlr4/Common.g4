lexer grammar Common;

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
