package demos.expressionTraverser;
/*
 * Date: 2010-11-3
 * Time: 10:38:15
 */

import gudusoft.gsqlparser.EDbVendor;
import gudusoft.gsqlparser.TGSqlParser;
import gudusoft.gsqlparser.nodes.IExpressionVisitor;
import gudusoft.gsqlparser.nodes.TExpression;
import gudusoft.gsqlparser.nodes.TParseTreeNode;
import gudusoft.gsqlparser.stmt.TSelectSqlStatement;

public class expressionTraverser  {

    public static void main(String args[])
     {

        TGSqlParser sqlparser = new TGSqlParser(EDbVendor.dbvoracle);

         sqlparser.sqltext = "select col1, col2,sum(col3) from table1, table2 where col4 > col5 and col6= 1000 or c1 = 1 and not sal";

        int ret = sqlparser.parse();
        if (ret == 0){
            TSelectSqlStatement select = (TSelectSqlStatement)sqlparser.sqlstatements.get(0);
            TExpression expr = select.getWhereClause().getCondition();

            System.out.println("pre order");
            expr.preOrderTraverse(new exprVisitor());

            System.out.println("\nin order");
            expr.inOrderTraverse(new exprVisitor());

            System.out.println("\npost order");
            expr.postOrderTraverse(new exprVisitor());
            expr.postOrderTraverse(new exprVisitor());
        }else{
            System.out.println(sqlparser.getErrormessage());
        }
     }

}

class exprVisitor implements IExpressionVisitor {

    public boolean exprVisit(TParseTreeNode pNode,boolean isLeafNode){
        String sign = "";
        if (isLeafNode){
            sign ="*";
        }
         System.out.println(sign+pNode.getClass().toString()+" "+ pNode.toString());
        return true;
    };
}
