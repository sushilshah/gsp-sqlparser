package demos.visitors;

import gudusoft.gsqlparser.EDbVendor;
import gudusoft.gsqlparser.EExpressionType;
import gudusoft.gsqlparser.TBaseType;
import gudusoft.gsqlparser.TGSqlParser;
import gudusoft.gsqlparser.TStatementList;
import gudusoft.gsqlparser.nodes.TAliasClause;
import gudusoft.gsqlparser.nodes.TCTE;
import gudusoft.gsqlparser.nodes.TCTEList;
import gudusoft.gsqlparser.nodes.TCaseExpression;
import gudusoft.gsqlparser.nodes.TColumnDefinition;
import gudusoft.gsqlparser.nodes.TColumnDefinitionList;
import gudusoft.gsqlparser.nodes.TConstant;
import gudusoft.gsqlparser.nodes.TConstraint;
import gudusoft.gsqlparser.nodes.TConstraintList;
import gudusoft.gsqlparser.nodes.TExpression;
import gudusoft.gsqlparser.nodes.TExpressionList;
import gudusoft.gsqlparser.nodes.TForUpdate;
import gudusoft.gsqlparser.nodes.TFunctionCall;
import gudusoft.gsqlparser.nodes.TGroupBy;
import gudusoft.gsqlparser.nodes.TGroupByItem;
import gudusoft.gsqlparser.nodes.TGroupByItemList;
import gudusoft.gsqlparser.nodes.TGroupingExpressionItem;
import gudusoft.gsqlparser.nodes.TGroupingExpressionItemList;
import gudusoft.gsqlparser.nodes.THierarchical;
import gudusoft.gsqlparser.nodes.TInExpr;
import gudusoft.gsqlparser.nodes.TJoin;
import gudusoft.gsqlparser.nodes.TJoinItem;
import gudusoft.gsqlparser.nodes.TJoinItemList;
import gudusoft.gsqlparser.nodes.TJoinList;
import gudusoft.gsqlparser.nodes.TMultiTarget;
import gudusoft.gsqlparser.nodes.TMultiTargetList;
import gudusoft.gsqlparser.nodes.TObjectName;
import gudusoft.gsqlparser.nodes.TObjectNameList;
import gudusoft.gsqlparser.nodes.TOrderBy;
import gudusoft.gsqlparser.nodes.TOrderByItem;
import gudusoft.gsqlparser.nodes.TOrderByItemList;
import gudusoft.gsqlparser.nodes.TParseTreeNode;
import gudusoft.gsqlparser.nodes.TParseTreeNodeList;
import gudusoft.gsqlparser.nodes.TParseTreeVisitor;
import gudusoft.gsqlparser.nodes.TResultColumn;
import gudusoft.gsqlparser.nodes.TResultColumnList;
import gudusoft.gsqlparser.nodes.TTable;
import gudusoft.gsqlparser.nodes.TTopClause;
import gudusoft.gsqlparser.nodes.TTypeAttribute;
import gudusoft.gsqlparser.nodes.TTypeAttributeList;
import gudusoft.gsqlparser.nodes.TTypeName;
import gudusoft.gsqlparser.nodes.TWhenClauseItem;
import gudusoft.gsqlparser.nodes.TWhenClauseItemList;
import gudusoft.gsqlparser.nodes.TWhereClause;
import gudusoft.gsqlparser.stmt.TAlterTableStatement;
import gudusoft.gsqlparser.stmt.TCreateIndexSqlStatement;
import gudusoft.gsqlparser.stmt.TCreateTableSqlStatement;
import gudusoft.gsqlparser.stmt.TCreateViewSqlStatement;
import gudusoft.gsqlparser.stmt.TDeleteSqlStatement;
import gudusoft.gsqlparser.stmt.TDropIndexSqlStatement;
import gudusoft.gsqlparser.stmt.TDropTableSqlStatement;
import gudusoft.gsqlparser.stmt.TDropViewSqlStatement;
import gudusoft.gsqlparser.stmt.TInsertSqlStatement;
import gudusoft.gsqlparser.stmt.TSelectSqlStatement;
import gudusoft.gsqlparser.stmt.TUpdateSqlStatement;
import gudusoft.gsqlparser.stmt.mssql.TMssqlCommit;
import gudusoft.gsqlparser.stmt.mssql.TMssqlRollback;
import gudusoft.gsqlparser.stmt.mssql.TMssqlSaveTran;
import gudusoft.gsqlparser.stmt.oracle.TExceptionClause;
import gudusoft.gsqlparser.stmt.oracle.TExceptionHandler;
import gudusoft.gsqlparser.stmt.oracle.TExceptionHandlerList;
import gudusoft.gsqlparser.stmt.oracle.TOracleStoredProcedureSqlStatement;
import gudusoft.gsqlparser.stmt.oracle.TPlsqlAssignStmt;
import gudusoft.gsqlparser.stmt.oracle.TPlsqlBasicStmt;
import gudusoft.gsqlparser.stmt.oracle.TPlsqlBlock;
import gudusoft.gsqlparser.stmt.oracle.TPlsqlCaseStmt;
import gudusoft.gsqlparser.stmt.oracle.TPlsqlCloseStmt;
import gudusoft.gsqlparser.stmt.oracle.TPlsqlCreateFunction;
import gudusoft.gsqlparser.stmt.oracle.TPlsqlCreatePackage;
import gudusoft.gsqlparser.stmt.oracle.TPlsqlCreateProcedure;
import gudusoft.gsqlparser.stmt.oracle.TPlsqlCreateTrigger;
import gudusoft.gsqlparser.stmt.oracle.TPlsqlCreateType;
import gudusoft.gsqlparser.stmt.oracle.TPlsqlCreateTypeBody;
import gudusoft.gsqlparser.stmt.oracle.TPlsqlCreateType_Placeholder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;

/**
 *  2010-8-9, subquery in expr was visited, in_expr, function call.
 */

public class toXml  {

    public static void main(String args[]) throws IOException
    {
        long t;
        t = System.currentTimeMillis();

        if (args.length != 1){
            System.out.println("Usage: java toXml sqlfile.sql");
            return;
        }
        File file=new File(args[0]);
        if (!file.exists()){
            System.out.println("File not exists:"+args[0]);
            return;
        }

        EDbVendor dbVendor = EDbVendor.dbvoracle;
        String msg = "Please select SQL dialect: 1: SQL Server, 2: Oralce, 3: MySQL, 4: DB2, 5: PostGRESQL, 6: Teradta, default is 2: Oracle";
        System.out.println(msg);

        BufferedReader br=new   BufferedReader(new InputStreamReader(System.in));
        try{
            int db = Integer.parseInt(br.readLine());
            if (db == 1){
                dbVendor = EDbVendor.dbvmssql;
            }else if(db == 2){
                dbVendor = EDbVendor.dbvoracle;
            }else if(db == 3){
                dbVendor = EDbVendor.dbvmysql;
            }else if(db == 4){
                dbVendor = EDbVendor.dbvdb2;
            }else if(db == 5){
                dbVendor = EDbVendor.dbvpostgresql;
            }else if(db == 6){
                dbVendor = EDbVendor.dbvteradata;
            }
        }catch(IOException i) {
        }catch (NumberFormatException numberFormatException){
        }

        System.out.println("Selected SQL dialect: "+dbVendor.toString());

        TGSqlParser sqlparser = new TGSqlParser(dbVendor);

        sqlparser.sqlfilename  = args[0];

        int ret = sqlparser.parse();
        if (ret == 0){
            xmlVisitor xv = new xmlVisitor();
            sqlparser.sqlstatements.accept(xv);
            String  testFile = args[0]+".xml";
            write(testFile,xv.getXml());
            System.out.println(testFile+" was generated!");
        }else{
            System.out.println(sqlparser.getErrormessage());
        }

        System.out.println("Time Escaped: "+ (System.currentTimeMillis() - t) );
    }

  static public  void write(String fFileName, String FIXED_TEXT) throws IOException  {
      Writer out = new OutputStreamWriter(new FileOutputStream(fFileName));
      try {
        out.write(FIXED_TEXT);
      }
      finally {
        out.close();
      }
    }

}

class xmlVisitor extends TParseTreeVisitor {

    public final static String crlf = "\r\n";
    StringBuilder sb;
    public xmlVisitor(){
        sb = new StringBuilder(1024);
    }


    void appendEndTag(String tagName){
        sb.append(String.format("</%s>"+crlf,tagName));
    }

    void appendStartTag(String tagName){
        sb.append(String.format("<%s>"+crlf,tagName));
    }

    String getTagName(TParseTreeNode node){
        return node.getClass().getSimpleName();
    }


    void appendStartTag(TParseTreeNode node){

        if (node instanceof TStatementList){
          appendStartTagWithCount(node,( (TStatementList)node).size() );
        }
        else if (node instanceof TParseTreeNodeList){
          appendStartTagWithCount(node,( (TParseTreeNodeList)node).size() );  
        }
        else{
        sb.append(String.format("<%s>"+crlf,getTagName(node)));
        }
    }

    void appendStartTagWithIntProperty(TParseTreeNode node,String propertyName, int propertyValue){
        sb.append(String.format("<%s "+propertyName+"='%d'>"+crlf,getTagName(node),propertyValue));
    }
    
    void appendStartTagWithIntProperty(TParseTreeNode node,String propertyName, EExpressionType propertyValue){
        sb.append(String.format("<%s "+propertyName+"='%s'>"+crlf,getTagName(node),propertyValue.name( )));
    }

    void appendStartTagWithIntProperty(TParseTreeNode node,String propertyName, String propertyValue){
        sb.append(String.format("<%s "+propertyName+"='%s'>"+crlf,getTagName(node),propertyValue));
    }

    void appendStartTagWithIntProperty(TParseTreeNode node,String propertyName, int propertyValue,String propertyName2, String propertyValue2){
        sb.append(String.format("<%s "+propertyName+"='%d' "+propertyName2+"='%s'"+">"+crlf,getTagName(node),propertyValue,propertyValue2));
    }

    void appendStartTagWithIntProperty(TParseTreeNode node,String propertyName, String propertyValue,String propertyName2, String propertyValue2){
        sb.append(String.format("<%s "+propertyName+"='%s' "+propertyName2+"='%s'"+">"+crlf,getTagName(node),propertyValue,propertyValue2));
    }

    void appendEndTag(TParseTreeNode node){
        sb.append(String.format("</%s>"+crlf,getTagName(node)));   
    }

    void appendStartTagWithCount(TParseTreeNode node, int count){
        appendStartTagWithIntProperty(node,"size",count);
    }

    public void preVisit(TConstant node){
        appendStartTag(node);
        sb.append(node.toString());
    }
    public void postVisit(TConstant node){
        appendEndTag(node);
    }

    public void preVisit(TTopClause node){
        appendStartTag(node);
        if (node.getExpr() != null){
            node.getExpr().accept(this);
        }
        if (node.getSubquery() != null){
            node.getSubquery().accept(this);
        }
    }
    
    public void postVisit(TTopClause node){
        appendEndTag(node);
    }


    public void preVisit(TSelectSqlStatement node){
        sb.append(String.format("<%s setOperator='%d'>"+crlf,getTagName(node),node.getSetOperator()) );

    }

    public void postVisit(TSelectSqlStatement node){
        appendEndTag(node);
    }

    public void preVisit(TResultColumnList node){
        appendStartTag(node);
    }

    public void postVisit(TResultColumnList node){
        appendEndTag(node);
    }

    public void preVisit(TResultColumn node){
        appendStartTag(node);
        node.getExpr().accept(this);
        if (node.getAliasClause() != null){
            node.getAliasClause().accept(this);
        }
    }

    public void postVisit(TResultColumn node){
        appendEndTag(node);
    }

    public void preVisit(TExpression node){
        appendStartTagWithIntProperty(node,"type",node.getExpressionType());
        switch(node.getExpressionType()){
            case simple_object_name_t:
                node.getObjectOperand().accept(this);
                break;
            case simple_constant_t:
                node.getConstantOperand().accept(this);
                break;
            case function_t:
                node.getFunctionCall().accept(this);
                break;
            case cursor_t:
                node.getSubQuery().accept(this);
                break;
            case subquery_t:
                node.getSubQuery().accept(this);
                break;
            case exists_t:
                node.getSubQuery().accept(this);
                break;
            case case_t:
                node.getCaseExpression().accept(this);
                break;
            case simple_comparison_t:
                appendStartTag("comparisonOperator");
                sb.append(node.getComparisonOperator().toString());
                appendEndTag("comparisonOperator");

                if (node.getSubQuery() != null){
                    node.getExprList().accept(this);
                    node.getSubQuery().accept(this);
                }else{
                    node.getLeftOperand().accept(this);
                    node.getRightOperand().accept(this);
                }
                break;
            case group_comparison_t:
                appendStartTag("comparisonOperator");
                sb.append(node.getComparisonOperator().toString().replace(">","&#62;").replace("<","&#60;"));
                appendEndTag("comparisonOperator");

                appendStartTag("comparsionSomeAnyAll");
                sb.append(node.getQuantifier().toString().replace(">","&#62;").replace("<","&#60;"));
                appendEndTag("comparsionSomeAnyAll");

                if (node.getExprList() != null){
                  node.getExprList().accept(this);
                }else{
                    node.getLeftOperand().accept(this);
                }

                node.getRightOperand().accept(this);
                break;
            case in_t:
                if (node.getExprList() != null){
                  node.getExprList().accept(this);
                }else{
                    node.getLeftOperand().accept(this);
                }

                node.getRightOperand( ).accept(this);
                break;
            case pattern_matching_t:
                node.getLeftOperand().accept(this);
                node.getRightOperand().accept(this);
                if (node.getLikeEscapeOperand() != null){
                    node.getLikeEscapeOperand().accept(this);
                }
                break;
            case between_t:
                node.getBetweenOperand().accept(this);
                node.getLeftOperand().accept(this);
                node.getRightOperand().accept(this);
                break;
            default:
                if (node.getLeftOperand() != null){
                node.getLeftOperand().accept(this);
                }

                if (node.getRightOperand() != null){
                node.getRightOperand().accept(this);
                }
                
                //sb.append(node.toString().replace(">","&#62;").replace("<","&#60;"));
                break;
        }
    }

    public void postVisit(TExpression node){
        appendEndTag(node);
    }

    public void preVisit(TAliasClause node){
        appendStartTag(node);
        sb.append(node.toString());
    }


    public void preVisit(TInExpr node){
        appendStartTag(node);
        if (node.getSubQuery() != null){
            node.getSubQuery().accept(this);
        }else if(node.getGroupingExpressionItemList() != null){
            node.getGroupingExpressionItemList().accept(this);
        }else{
        sb.append(node.toString());
        }
    }

    public void postVisit(TInExpr node){
        appendEndTag(node);
    }

    public void preVisit(TExpressionList node){
        appendStartTag(node);
    }

    public void postVisit(TExpressionList node){
        appendEndTag(node);
    }

    public void preVisit(TGroupingExpressionItem node){
        appendStartTag(node);
        if (node.getExpr() != null){
            node.getExpr().accept(this);
        }else if (node.getExprList() != null){
            node.getExprList().accept(this);
        }
    }

    public void postVisit(TGroupingExpressionItem node){
        appendEndTag(node);
    }

    public void preVisit(TGroupingExpressionItemList node){
        appendStartTag(node);
    }

    public void postVisit(TGroupingExpressionItemList node){
        appendEndTag(node);
    }

    public void postVisit(TAliasClause node){
        appendEndTag(node);
    }

    public void preVisit(TJoin node){
        appendStartTagWithIntProperty(node,"type",node.getKind());
        if (node.getAliasClause() != null){
            node.getAliasClause().accept(this);
        }

        if(node.getKind() == TBaseType.join_source_fake){
            node.getTable().accept(this);
        }else if(node.getKind() == TBaseType.join_source_table){
            node.getTable().accept(this);
        }else if(node.getKind() == TBaseType.join_source_join){
            node.getJoin().accept(this);
        }

    }

    public void postVisit(TJoin node){
        appendEndTag(node);
    }
    
    public void preVisit(TJoinList node){
        appendStartTag(node);
    }
    public void postVisit(TJoinList node){
        appendEndTag(node);
    }
    public void preVisit(TJoinItem node){
        appendStartTagWithIntProperty(node,"jointype",node.getJoinType().toString());
        if (node.getKind() == TBaseType.join_source_table){
            node.getTable().accept(this);
        }else if (node.getKind() == TBaseType.join_source_join){
            node.getJoin().accept(this);
        }

        if (node.getOnCondition() != null){
            node.getOnCondition().accept(this);
        }

        if (node.getUsingColumns() != null){
            node.getUsingColumns().accept(this);
        }
    }
    public void postVisit(TJoinItem node){
        appendEndTag(node);
    }
    public void preVisit(TJoinItemList node){
        appendStartTag(node);
    }
    public void postVisit(TJoinItemList node){
        appendEndTag(node);
    }

    public void preVisit(TTable node){
        appendStartTagWithIntProperty(node,"type",node.getTableType().toString());
        //sb.append(node.toString());
        sb.append(node.toString().replace(">","&#62;").replace("<","&#60;"));
        if (node.getAliasClause() != null){
            node.getAliasClause().accept(this);
        }
    }
    public void postVisit(TTable node){
        appendEndTag(node);
    }

    public void preVisit(TObjectName node){
        appendStartTagWithIntProperty(node,"type",node.getObjectType());
        sb.append(node.toString());
    }
    public void postVisit(TObjectName node){
        appendEndTag(node);
    }
    public void preVisit(TObjectNameList node){
        appendStartTag(node);
    }
    public void postVisit(TObjectNameList node){
        appendEndTag(node);
    }

    public void preVisit(TWhereClause node){
        appendStartTag(node);
        node.getCondition().accept(this);
    }
    public void postVisit(TWhereClause node){
        appendEndTag(node);
    }

    public void preVisit(THierarchical node){
        appendStartTag(node);
        if (node.getConnectByClause() != null){
            appendStartTag("connect_by_clause");
            node.getConnectByClause().accept(this);
            appendEndTag("connect_by_clause");
        }
        
        if (node.getStartWithClause() != null){
            appendStartTag("start_with_clause");
            node.getStartWithClause().accept(this);
            appendEndTag("start_with_clause");
        }

    }
    public void postVisit(THierarchical node){
        appendEndTag(node);
    }

    public void preVisit(TGroupBy node){
        appendStartTag(node);
    }
    public void postVisit(TGroupBy node){
        if (node.getHavingClause() != null){
            appendStartTag("haveing_clause");
            node.getHavingClause().accept(this);
            appendEndTag("haveing_clause");
        }
        appendEndTag(node);
    }
    public void preVisit(TGroupByItem node){
        appendStartTag(node);
        sb.append(node.toString());
    }
    public void postVisit(TGroupByItem node){
        appendEndTag(node);
    }
    public void preVisit(TGroupByItemList node){
        appendStartTag(node);
    }
    public void postVisit(TGroupByItemList node){
        appendEndTag(node);
    }

    public void preVisit(TOrderBy node){
        appendStartTag(node);
    }
    public void postVisit(TOrderBy node){
        appendEndTag(node);
    }
    public void preVisit(TOrderByItem node){
        appendStartTag(node);
        sb.append(node.toString()) ;
    }
    public void postVisit(TOrderByItem node){
        appendEndTag(node);
    }
    public void preVisit(TOrderByItemList node){
        appendStartTag(node);
    }
    public void postVisit(TOrderByItemList node){
        appendEndTag(node);
    }

    public void preVisit(TForUpdate node){
        appendStartTag(node);
        node.getColumnRefs().accept(this);
    }
    public void postVisit(TForUpdate node){
        appendEndTag(node);
    }

    public void preVisit(TStatementList node){
        appendStartTag(node);
        //appendStartTagWithIntProperty(node,"size",node.size());
    }
    public void postVisit(TStatementList node){
        appendEndTag(node);
    }

    void doDeclare_Body_Exception(TOracleStoredProcedureSqlStatement node){
       
        if (node.getDeclareStatements() != null){
            appendStartTag("declare");
            node.getDeclareStatements().accept(this);
            appendEndTag("declare");
        }

        if (node.getBodyStatements() != null){
            appendStartTag("body");
            node.getBodyStatements().accept(this);
            appendEndTag("body");
        }

        if (node.getExceptionClause() != null){
            node.getExceptionClause().accept(this);
        }

    }
    public void preVisit(TPlsqlCreatePackage node){
        appendStartTag(node);
        doDeclare_Body_Exception(node);
    }
    public void postVisit(TPlsqlCreatePackage node){
        appendEndTag(node);
    }

    public void preVisit(TPlsqlCreateProcedure node){
        appendStartTag(node);
        doDeclare_Body_Exception(node);
    }
    public void postVisit(TPlsqlCreateProcedure node){
        appendEndTag(node);
    }

    public void preVisit(TPlsqlCreateFunction node){
        appendStartTag(node);
        doDeclare_Body_Exception(node);
    }
    public void postVisit(TPlsqlCreateFunction node){
        appendEndTag(node);
    }
    public void preVisit(TPlsqlBlock node){
        appendStartTag(node);
        doDeclare_Body_Exception(node);
    }
    public void postVisit(TPlsqlBlock node){
        appendEndTag(node);
    }

    public void preVisit(TExceptionClause node){
        appendStartTag(node);
    }
    public void postVisit(TExceptionClause node){
        appendEndTag(node);
    }
    public void preVisit(TExceptionHandler node){
        appendStartTag(node);
    }
    public void postVisit(TExceptionHandler node){
        appendEndTag(node);
    }
    public void preVisit(TExceptionHandlerList node){
        appendStartTag(node);
    }
    public void postVisit(TExceptionHandlerList node){
        appendEndTag(node);
    }
    public void preVisit(TAlterTableStatement stmt){
        appendStartTagWithIntProperty(stmt,"name",stmt.getTableName().toString());
    }
    public void postVisit(TAlterTableStatement stmt){
        appendEndTag(stmt);
    }

    public void preVisit(TTypeName node){
       appendStartTagWithIntProperty(node,"type",node.getType());
       sb.append(node.toString());
    }
    public void postVisit(TTypeName node){
        appendEndTag(node);
    }

    public void preVisit(TColumnDefinition node){
        appendStartTagWithIntProperty(node,"null",(node.isNull())? 1:0,"name",node.getColumnName().toString());
        if (node.getDatatype() != null){
            node.getDatatype().accept(this);
        }
        if (node.getDefaultExpression() != null){
            node.getDefaultExpression().accept(this);
        }
        if (node.getConstraints() != null){
            node.getConstraints().accept(this);
        }
    }
    public void postVisit(TColumnDefinition node){
        appendEndTag(node);
    }
    public void preVisit(TColumnDefinitionList node){
        appendStartTag(node);
    }
    public void postVisit(TColumnDefinitionList node){
        appendEndTag(node);
    }

    public void preVisit(TConstraint node){
        appendStartTagWithIntProperty(node,"type",node.getConstraint_type().toString(),"name",(node.getConstraintName() != null) ? node.getConstraintName().toString():"");
        switch(node.getConstraint_type()){
            case notnull:
                break;
            case unique:
                if (node.getColumnList() != null){
                    node.getColumnList().accept(this);
                }
                break;
            case check:
                node.getCheckCondition().accept(this);
                break;
            case primary_key:
                if (node.getColumnList() != null){
                    node.getColumnList().accept(this);
                }
                break;
            case foreign_key:
                if (node.getColumnList() != null){
                    node.getColumnList().accept(this);
                }
                if (node.getReferencedObject() != null){
                    node.getReferencedObject().accept(this);
                }
                if (node.getReferencedColumnList() != null){
                    node.getReferencedColumnList().accept(this);
                }
                break;
            case reference:
                node.getReferencedObject().accept(this);
                node.getReferencedColumnList().accept(this);
                break;
            default:
                break;
        }
    }
    public void postVisit(TConstraint node){
        appendEndTag(node);
    }
    public void preVisit(TConstraintList node){
        appendStartTag(node);
    }
    public void postVisit(TConstraintList node){
        appendEndTag(node);
    }

    public void preVisit(TCreateViewSqlStatement stmt){
        appendStartTagWithIntProperty(stmt,"name",stmt.getViewName().toString());
        stmt.getSubquery().accept(this);
    }
    public void postVisit(TCreateViewSqlStatement stmt){
        appendEndTag(stmt);
    }

    public void preVisit(TCreateIndexSqlStatement stmt){
        appendStartTagWithIntProperty(stmt,"name",stmt.getIndexName().toString());
    }
    public void postVisit(TCreateIndexSqlStatement stmt){
        appendEndTag(stmt);
    }
    public void preVisit(TCreateTableSqlStatement stmt){
       appendStartTagWithIntProperty(stmt,"name",stmt.getTargetTable().toString());
    }
    public void postVisit(TCreateTableSqlStatement stmt){
        appendEndTag(stmt);
    }

    public void preVisit(TDropIndexSqlStatement stmt){
        appendStartTagWithIntProperty(stmt,"name",stmt.getIndexName().toString());
    }
    public void postVisit(TDropIndexSqlStatement stmt){
        appendEndTag(stmt);
    }

    public void preVisit(TDropTableSqlStatement stmt){
        appendStartTagWithIntProperty(stmt,"name",stmt.getTableName().toString());
    }
    public void postVisit(TDropTableSqlStatement stmt){
        appendEndTag(stmt);
    }

    public void preVisit(TDropViewSqlStatement stmt){
        appendStartTagWithIntProperty(stmt,"name",stmt.getViewName().toString());
    }
    public void postVisit(TDropViewSqlStatement stmt){
        appendEndTag(stmt);
    }

    public void preVisit(TDeleteSqlStatement stmt){
        appendStartTag(stmt);
    }
    public void postVisit(TDeleteSqlStatement stmt){
        appendEndTag(stmt);
    }

    public void preVisit(TUpdateSqlStatement stmt){
        appendStartTag(stmt);
    }
    public void postVisit(TUpdateSqlStatement stmt){
        appendEndTag(stmt);
    }

    public void preVisit(TFunctionCall node){
        appendStartTagWithIntProperty(node,"type",node.getFunctionType().toString(),"name",node.getFunctionName().toString());
        if (node.getArgs() != null){
            appendStartTag("args");
            node.getArgs().accept(this);
            appendEndTag("args");
        }
        //sb.append(node.toString().replace(">","&#62;").replace("<","&#60;"));
    }
    public void postVisit(TFunctionCall node){
        appendEndTag(node);
    }

    public void preVisit(TInsertSqlStatement stmt){
        appendStartTag(stmt);
    }
    public void postVisit(TInsertSqlStatement stmt){
        appendEndTag(stmt);
    }

    public void preVisit(TMultiTarget node){
        appendStartTag(node);
        if (node.getColumnList() != null){
            node.getColumnList().accept(this);
        }

        if (node.getSubQuery() != null){
            node.getSubQuery().accept(this);
        }
    }
    public void postVisit(TMultiTarget node){
        appendEndTag(node);
    }
    
    public void preVisit(TMultiTargetList node){
        appendStartTag(node);
    }
    public void postVisit(TMultiTargetList node){
        appendEndTag(node);
    }

    public void preVisit(TCTE node){
        appendStartTagWithIntProperty(node,"name",node.getTableName().toString());
        if (node.getColumnList() != null){
            node.getColumnList().accept(this);
        }
        node.getSubquery().accept(this);
    }
    public void postVisit(TCTE node){
        appendEndTag(node);
    }

    public void preVisit(TCTEList node){
        appendStartTag(node);
    }
    public void postVisit(TCTEList node){
        appendEndTag(node);
    }

    public void preVisit(TPlsqlAssignStmt node){
        appendStartTag(node);
        outputNodeData(node);
    }
    public void postVisit(TPlsqlAssignStmt node){
        appendEndTag(node);
    }

    public void preVisit(TPlsqlBasicStmt node){
        appendStartTag(node);
        outputNodeData(node);
    }
    public void postVisit(TPlsqlBasicStmt node){
        appendEndTag(node);
    }

    public void preVisit(TPlsqlCaseStmt node){
        appendStartTag(node);
        node.getCaseExpr().accept(this);
    }
    public void postVisit(TPlsqlCaseStmt node){
        appendEndTag(node);
    }

    public void preVisit(TCaseExpression node){
        appendStartTag(node);
        if (node.getInput_expr() != null){
            appendStartTag("input_expr");
            node.getInput_expr().accept(this);
            appendEndTag("input_expr");
        }

        node.getWhenClauseItemList().accept(this);

        if (node.getElse_expr() != null){
            appendStartTag("else_expr");
            node.getElse_expr().accept(this);
            appendEndTag("else_expr");
        }

        if (node.getElse_statement_list().size() > 0){
            node.getElse_statement_list().accept(this);
        }
    }
    public void postVisit(TCaseExpression node){
        appendEndTag(node);
    }

    public void preVisit(TWhenClauseItemList node){
        appendStartTag(node);
    }
    public void postVisit(TWhenClauseItemList node){
        appendEndTag(node);
    }

    public void preVisit(TWhenClauseItem node){
        appendStartTag(node);
        node.getComparison_expr().accept(this);
        if (node.getReturn_expr() != null){
            node.getReturn_expr().accept(this);
        }else if (node.getStatement_list().size() >0){
            node.getStatement_list().accept(this);
        }
    }
    public void postVisit(TWhenClauseItem node){
        appendEndTag(node);
    }


    public void preVisit(TPlsqlCloseStmt node){
        appendStartTag(node);
    }

    public void postVisit(TPlsqlCloseStmt node){
        appendEndTag(node);
    }

    public void preVisit(TPlsqlCreateTrigger node){
        appendStartTagWithIntProperty(node,"name",node.getTriggerName().toString());
        appendStartTag("event_clause");
        sb.append(node.getEventClause().toString());
        appendEndTag("event_clause");
        if (node.getWhenCondition() != null){
            node.getWhenCondition().accept(this);
        }
        appendStartTag("body");
        node.getTriggerBody().accept(this);
        appendEndTag("body");
    }

    public void postVisit(TPlsqlCreateTrigger node){
        appendEndTag(node);
    }

    public void preVisit(TPlsqlCreateType node){
        appendStartTagWithIntProperty(node,"type",node.getKind(),"name",node.getTypeName().toString());
        if (node.getAttributes() != null){
            node.getAttributes().accept(this);
        }
    }

    public void postVisit(TPlsqlCreateType node){
        appendEndTag(node);
    }


    public void preVisit(TTypeAttribute node){
        appendStartTag(node);
        node.getAttributeName().accept(this);
        node.getDatatype().accept(this);
    }

    public void postVisit(TTypeAttribute node){
        appendEndTag(node);
    }

    public void preVisit(TTypeAttributeList node){
        appendStartTag(node);
    }

    public void postVisit(TTypeAttributeList node){
        appendEndTag(node);
    }

    public void preVisit(TPlsqlCreateType_Placeholder node){
        appendStartTagWithIntProperty(node,"type",node.getKind());
        switch(node.getKind()){
            case TBaseType.kind_create_varray:
                node.getVarrayStatement().accept(this);
                break;
            case TBaseType.kind_create_nested_table:
                node.getNestedTableStatement().accept(this);
                break;
            default:
                node.getObjectStatement().accept(this);
                break;
        }
    }

    public void postVisit(TPlsqlCreateType_Placeholder node){
        appendEndTag(node);
    }

    public void preVisit(TPlsqlCreateTypeBody node){
        appendStartTagWithIntProperty(node,"name",node.getTypeName().toString());
        node.getBodyStatements().accept(this);
    }

    public void postVisit(TPlsqlCreateTypeBody node){
        appendEndTag(node);
    }

    void outputNodeData(TParseTreeNode node){
        sb.append(node.toString());
    }
    
    public void preVisit(TMssqlCommit node){
        if (node.getTransactionName() != null){
         appendStartTagWithIntProperty(node,"transactionName",node.getTransactionName().toString());   
        }else{
        appendStartTag(node);
        }
        sb.append(node.toString());
    }

    public void postVisit(TMssqlCommit node){
        appendEndTag(node);
    }


    public void preVisit(TMssqlRollback node){
        if (node.getTransactionName() != null){
         appendStartTagWithIntProperty(node,"transactionName",node.getTransactionName().toString());
        }else{
        appendStartTag(node);
        }
        sb.append(node.toString());
    }

    public void postVisit(TMssqlRollback node){
        appendEndTag(node);
    }

    public void preVisit(TMssqlSaveTran node){
        if (node.getTransactionName() != null){
         appendStartTagWithIntProperty(node,"transactionName",node.getTransactionName().toString());
        }else{
        appendStartTag(node);
        }
        sb.append(node.toString());
    }

    public void postVisit(TMssqlSaveTran node){
        appendEndTag(node);
    }

    public String getXml(){
        return "<?xml-stylesheet type=\"text/xsl\" href=\"tree-view.xsl\"?>"+crlf
                +"<sqlscript>"+crlf
                +sb.toString()
                +"</sqlscript>";
    }
}
