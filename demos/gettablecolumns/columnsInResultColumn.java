package demos.gettablecolumns;

/*
 * Date: 11-4-19
 * ref : http://www.dpriver.com/blog/list-of-demos-illustrate-how-to-use-general-sql-parser/get-referenced-table-column-in-a-select-list-item/
 */

import demos.columnInClause;
import gudusoft.gsqlparser.EDbVendor;
import gudusoft.gsqlparser.TCustomSqlStatement;
import gudusoft.gsqlparser.TGSqlParser;
import gudusoft.gsqlparser.nodes.*;
import gudusoft.gsqlparser.stmt.TSelectSqlStatement;

public class columnsInResultColumn {

    public static void main(String args[])
     {
         TGSqlParser sqlparser = new TGSqlParser(EDbVendor.dbvoracle);
         sqlparser.sqltext  = "select sal.income + sal.bonus * emp.age + 5 as real_sal,\n" +
                 "       emp.name as title \n" +
                 "from employee emp, salary sal \n" +
                 "where emp.id=sal.eid";
         int ret = sqlparser.parse();
         if (ret == 0){
             TSelectSqlStatement select = (TSelectSqlStatement)sqlparser.sqlstatements.get(0);
             TResultColumnList columns = select.getResultColumnList();

             for(int i = 0; i < columns.size();i++){
                 printColumns(columns.getResultColumn(i),select);
             }

         }else{
             System.out.println(sqlparser.getErrormessage());
         }

     }

     static void printColumns(TResultColumn cl,TCustomSqlStatement sqlStatement){

         if(cl.getAliasClause() != null){
             System.out.println("\nResult column:" + cl.getAliasClause().toString());
         }else{
            System.out.println("\nResult column:" + cl.getExpr().toString());
         }

         new columnInClause().printColumns(cl.getExpr(),sqlStatement);
     }
}

