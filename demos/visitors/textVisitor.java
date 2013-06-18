package demos.visitors;

import gudusoft.gsqlparser.EDbVendor;
import gudusoft.gsqlparser.TCustomSqlStatement;
import gudusoft.gsqlparser.TGSqlParser;
import gudusoft.gsqlparser.TStatementList;
import gudusoft.gsqlparser.nodes.TParseTreeNode;
import gudusoft.gsqlparser.nodes.TParseTreeVisitor;
import gudusoft.gsqlparser.stmt.*;
import gudusoft.gsqlparser.stmt.oracle.TExceptionClause;
import gudusoft.gsqlparser.stmt.oracle.TExceptionHandler;
import gudusoft.gsqlparser.stmt.TCreateSequenceStmt;
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
import gudusoft.gsqlparser.stmt.oracle.TPlsqlCursorDeclStmt;
import gudusoft.gsqlparser.stmt.oracle.TPlsqlDummyStmt;
import gudusoft.gsqlparser.stmt.oracle.TPlsqlElsifStmt;
import gudusoft.gsqlparser.stmt.oracle.TPlsqlExecImmeStmt;
import gudusoft.gsqlparser.stmt.oracle.TPlsqlExitStmt;
import gudusoft.gsqlparser.stmt.oracle.TPlsqlFetchStmt;
import gudusoft.gsqlparser.stmt.oracle.TPlsqlForallStmt;
import gudusoft.gsqlparser.stmt.oracle.TPlsqlGotoStmt;
import gudusoft.gsqlparser.stmt.oracle.TPlsqlIfStmt;
import gudusoft.gsqlparser.stmt.oracle.TPlsqlLoopStmt;
import gudusoft.gsqlparser.stmt.oracle.TPlsqlNullStmt;
import gudusoft.gsqlparser.stmt.oracle.TPlsqlOpenStmt;
import gudusoft.gsqlparser.stmt.oracle.TPlsqlOpenforStmt;
import gudusoft.gsqlparser.stmt.oracle.TPlsqlPipeRowStmt;
import gudusoft.gsqlparser.stmt.oracle.TPlsqlPragmaDeclStmt;
import gudusoft.gsqlparser.stmt.oracle.TPlsqlProcedureSpecStmt;
import gudusoft.gsqlparser.stmt.oracle.TPlsqlRaiseStmt;
import gudusoft.gsqlparser.stmt.oracle.TPlsqlRecordTypeDefStmt;
import gudusoft.gsqlparser.stmt.oracle.TPlsqlReturnStmt;
import gudusoft.gsqlparser.stmt.oracle.TPlsqlSqlStmt;
import gudusoft.gsqlparser.stmt.oracle.TPlsqlSubProgram;
import gudusoft.gsqlparser.stmt.oracle.TPlsqlTableTypeDefStmt;
import gudusoft.gsqlparser.stmt.oracle.TPlsqlVarDeclStmt;
import gudusoft.gsqlparser.stmt.oracle.TPlsqlVarrayTypeDefStmt;
import gudusoft.gsqlparser.stmt.oracle.TSqlplusCmdStatement;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class textVisitor {

    public static void main(String args[])
    {
        long t;
        t = System.currentTimeMillis();

        if (args.length != 1){
            System.out.println("Usage: java textVisitor sqlfile.sql");
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
            TtextVisitor tv = new TtextVisitor();
           for(int i=0;i<sqlparser.sqlstatements.size();i++){
               sqlparser.sqlstatements.get(i).accept(tv);
           }
        }else{
            System.out.println(sqlparser.getErrormessage());
        }

        System.out.println("Time Escaped: "+ (System.currentTimeMillis() - t) );
    }

}

class TtextVisitor extends TParseTreeVisitor{
    void show(TCustomSqlStatement stmt){
        System.out.println("sql type: "+stmt.sqlstatementtype);
        //System.out.println("sql text: "+stmt.toString());
    }

    void showNode(TParseTreeNode node){
        System.out.println("node type: "+node.getNodeType());
    }
    public void preVisit(TSelectSqlStatement stmt){
        show(stmt);
    }

    public void postVisit(TStatementList node){
        System.out.println("stmt list: "+node.size());
    }
    public void postVisit(TPlsqlBlock node){show(node);}
    public void postVisit(TExceptionClause node){showNode(node);}
    public void postVisit(TExceptionHandler node){showNode(node);}
    public void postVisit(TCreateSequenceStmt node){show(node);}
    public void postVisit(TPlsqlAssignStmt node){show(node);}
    public void postVisit(TPlsqlBasicStmt node){show(node);}
    public void postVisit(TPlsqlCaseStmt node){show(node);}
    public void postVisit(TPlsqlCloseStmt node){show(node);}
    public void postVisit(TPlsqlCreateFunction node){show(node);}
    public void postVisit(TPlsqlCreatePackage node){show(node);}
    public void postVisit(TPlsqlCreateProcedure node){show(node);}
    public void postVisit(TPlsqlCreateTrigger node){show(node);}
    public void postVisit(TPlsqlCreateType node){show(node);}
    public void postVisit(TPlsqlCreateType_Placeholder node){show(node);}
    public void postVisit(TPlsqlCreateTypeBody node){show(node);}
    public void visit(TPlsqlCursorDeclStmt node){show(node);}
    public void visit(TPlsqlDummyStmt node){show(node);}
    public void visit(TPlsqlElsifStmt node){show(node);}
    public void visit(TPlsqlExecImmeStmt node){show(node);}
    public void visit(TPlsqlExitStmt node){show(node);}
    public void visit(TPlsqlFetchStmt node){show(node);}
    public void visit(TPlsqlForallStmt node){show(node);}
    public void visit(TPlsqlGotoStmt node){show(node);}
    public void visit(TPlsqlIfStmt node){show(node);}
    public void visit(TPlsqlLoopStmt node){show(node);}
    public void visit(TPlsqlNullStmt node){show(node);}
    public void visit(TPlsqlOpenforStmt node){show(node);}
    public void visit(TPlsqlOpenStmt node){show(node);}
    public void visit(TPlsqlPipeRowStmt node){show(node);}
    public void visit(TPlsqlPragmaDeclStmt node){show(node);}
    public void visit(TPlsqlProcedureSpecStmt node){show(node);}
    public void visit(TPlsqlRaiseStmt node){show(node);}
    public void visit(TPlsqlRecordTypeDefStmt node){show(node);}
    public void visit(TPlsqlReturnStmt node){show(node);}
    public void visit(TPlsqlSqlStmt node){show(node);}
    public void visit(TPlsqlSubProgram node){show(node);}
    public void visit(TPlsqlTableTypeDefStmt node){show(node);}
    public void visit(TPlsqlVarDeclStmt node){show(node);}
    public void visit(TPlsqlVarrayTypeDefStmt node){show(node);}
    public void visit(TSqlplusCmdStatement node){show(node);}

    public void postVisit(TAlterTableStatement stmt){show(stmt);}
    public void postVisit(TCreateIndexSqlStatement stmt){show(stmt);}
    public void postVisit(TCreateTableSqlStatement stmt){show(stmt);}
    public void postVisit(TCreateViewSqlStatement stmt){show(stmt);}
    public void postVisit(TDeleteSqlStatement stmt){show(stmt);}
    public void postVisit(TDropIndexSqlStatement stmt){show(stmt);}
    public void postVisit(TDropTableSqlStatement stmt){show(stmt);}
    public void postVisit(TDropViewSqlStatement stmt){show(stmt);}
    public void postVisit(TInsertSqlStatement stmt){show(stmt);}
    public void postVisit(TMergeSqlStatement stmt){show(stmt);}
    public void postVisit(TUpdateSqlStatement stmt){show(stmt);}
    public void postVisit(TUnknownSqlStatement stmt){show(stmt);}


}

