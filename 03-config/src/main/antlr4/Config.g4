grammar Config;

config: (pair NEWLINE)* pair? ;        // allow trailing line or last pair without newline
pair  : STRING '=' value ;             // one key=value pair
value : INT | BOOL | STRING ;          // value can be int, bool, string, or identifier

INT     : [0-9]+ ;                     // integers
BOOL    : 'true' | 'false' ;           // booleans
STRING  : [a-zA-Z0-9_.]+ ;             // simple string with dots allowed
NEWLINE : [\r\n]+ ;                    // line breaks
WS      : [ \t]+ -> skip ;             // skip spaces