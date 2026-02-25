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
    : arrayIndexOrAll ('.' pathSegment)*        // allow paths starting with [] or [NUMBER]
    | pathSegment ('.' pathSegment)*            // normal path
    ;

pathSegment
    : IDENT                                     // simple field
    | IDENT '[' NUMBER ']'                      // field[index]
    | IDENT '[' ']'                             // field[]
    | arrayIndexOrAll                           // nested array access directly
    ;

arrayIndexOrAll
    : '[' ']'                                   // all elements
    | '[' NUMBER ']'                            // element by index
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
