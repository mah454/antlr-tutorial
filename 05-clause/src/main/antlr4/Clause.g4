grammar Clause;

/*
 * Parser rules
 */

expression
    : orExpr EOF
    ;

orExpr
    : andExpr (OR andExpr)*
    ;

andExpr
    : primary (AND primary)*
    ;

primary
    : '(' orExpr ')'
    | comparison
    ;

comparison
    : IDENTIFIER operator operand
    ;

operand
    : IDENTIFIER
    | STRING
    | NUMBER
    ;

operator
    : EQ
    | NE
    | GT
    | LT
    | GTE
    | LTE
    | REG
    | REGI
    ;

/*
 * Lexer rules
 */

// logical operators
AND : ('and' | 'AND');
OR  : ('or'  | 'OR');

// comparison operators (word-based)
EQ  : 'eq' | '=';
NE  : 'ne' | '!=';
GT  : 'gt' | '>';
LT  : 'lt' | '<';
GTE : 'gte' | '>=';
LTE : 'lte' | '<=';
REG : 'reg' | '~';
REGI : 'regi' | '!~';

// literals
IDENTIFIER
    : [a-zA-Z_][a-zA-Z0-9_]*
    ;

NUMBER
    : [0-9]+
    ;

STRING
    : '"' (~["\\] | '\\' .)* '"'
    | [a-zA-Z_][a-zA-Z0-9_]*   // allows unquoted Tehran
    ;

// whitespace
WS
    : [ \t\r\n]+ -> skip
    ;
