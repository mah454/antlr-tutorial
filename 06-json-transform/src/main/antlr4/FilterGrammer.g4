grammar FilterGrammer;

import Common;

program
    : clauses* EOF
    ;

clauses
    : FILTER '->' expressions
    ;

expressions
    : expressions OR expressions
    | expressions AND expressions
    | '(' expressions ')'
    | statement
    ;

statement
    : stmtValue comparator stmtValue
    ;

stmtValue
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
    : '='      // equal ignore case
    | '=='     // exact case
    | '!='     // not equal
    | '>'      // greater than
    | '>='     // greater equal
    | '<'      // less than
    | '<='     // less equal
    | '~'      // contain
    | '!~'     // not contain
    ;

FILTER : 'filter';
