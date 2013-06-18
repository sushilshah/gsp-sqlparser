package demos.columnInWhereClause;

import gudusoft.gsqlparser.EDbVendor;
import gudusoft.gsqlparser.TGSqlParser;


public class ColumnInWhereClause
{

	public static void main(String[] args){
		 TGSqlParser sqlparser = new TGSqlParser(EDbVendor.dbvmssql);
         sqlparser.sqltext = "Select firstname, lastname, age from Clients where State = \"CA\" and  City = \"Hollywood\"";
         int i = sqlparser.parse( );
         if (i == 0)
         {
             WhereCondition w = new WhereCondition(sqlparser.sqlstatements.get( 0 ).getWhereClause( ).getCondition( ));
             w.printColumn();
         }
         else
             System.out.println(sqlparser.getErrormessage( ));
	}
}
