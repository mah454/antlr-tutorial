grammar Sql;

sql         : selectStmt EOF ;
selectStmt  : SELECT columnList FROM tableName (WHERE condition)? ;
columnList  : columnName (',' columnName)* ;
condition   : columnName comparator value ;
comparator  : '=' | '>' | '<' | '>=' | '<=' | '!=' ;

tableName   : ID ;
columnName  : ID | '*' ;
value       : STRING | INT ;

SELECT  : 'SELECT' ;
FROM    : 'FROM' ;
WHERE   : 'WHERE' ;

ID      : [a-zA-Z_][a-zA-Z0-9_]* ;
INT     : [0-9]+ ;
STRING  : '\'' [a-zA-Z0-9_ ]* '\'' ; // simple quoted strings
WS      : [ \t\r\n]+ -> skip ;
