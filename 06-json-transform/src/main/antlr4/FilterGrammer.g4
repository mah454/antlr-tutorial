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
    | arrayFilter
    ;

arrayFilter
    : path '[' expressions ']'
    | path '[' NUMBER ']'
    | '[' NUMBER ']'
    ;

statement
    : stmtValue comparator stmtValue
    ;

stmtValue
    : STRING
    | NUMBER
    | NULL
    | path
    | '@'
    ;

path
    : pathSegment ('.' pathSegment)*
    ;

pathSegment
    : IDENT ('[' ']')?
    | ('[' ']').IDENT
    | IDENT '[' NUMBER ']'
    | '[' NUMBER ']' IDENT
    ;

comparator
    : '='      // equal ignore case
    | '=='     // exact case
    | '!='     // not equal ignore case
    | '!=='    // not exact equal
    | '>'      // greater than
    | '>='     // greater equal
    | '<'      // less than
    | '<='     // less equal
    | '~'      // contain
    | '!~'     // not contain
    ;

FILTER : 'filter';
