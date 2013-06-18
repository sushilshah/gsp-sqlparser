package demos.gettablecolumns;

import gudusoft.gsqlparser.*;

import java.io.*;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;

/**
 * The logic to find all tables and columns in SQL script is quite simple like this:
 *<p>1. Iterate all statements via {@link TGSqlParser#sqlstatements} and {@link gudusoft.gsqlparser.TCustomSqlStatement#getStatements()}
 *<p>2. Once a statement was found, get all tables belong to this statement via {@link TCustomSqlStatement#tables}
 *<p>3. Once a table was found, get all columns belong to this table via {@link gudusoft.gsqlparser.nodes.TTable#getObjectNameReferences()} 
 */

class metaDB implements IMetaDatabase {

    String columns[][] = {
        {"dbo","subselect3table1","f1"},
        {"dbo","subselect3table2","s3t1a1"}
    };

    public boolean checkColumn(String schema, String table, String column){
       boolean bSchema,bTable,bColumn,bRet = false;
        for (int i=0; i<columns.length;i++){
            if (schema == null){
                bSchema = true;
            }else{
                bSchema = columns[i][0].equalsIgnoreCase(schema);
            }

            if (!bSchema) continue;

            bTable = columns[i][1].equalsIgnoreCase(table);
            if (!bTable) continue;

            bColumn = columns[i][2].equalsIgnoreCase(column);
            if (!bColumn) continue;

            bRet =true;
            break;

        }

        return bRet;
    }

}

class getObject{

    private String sqlfile="";
    private String sqltext = "";

    public void setSqltext(String sqltext) {
        this.sqltext = sqltext;
    }

    public void setSqlfile(String sqlfile) {
        this.sqlfile = sqlfile;
    }


    String[] foundTables = new String[10000];
    String[] foundColumns = new String[10000];
    int foundTableCount = 0;
    int foundColumnsCount = 0;

    StringBuffer stringBuffer;

    EDbVendor dbvendor;
    getObject(EDbVendor db){
        this.dbvendor = db;
        stringBuffer = new StringBuffer(1024);
      }


    String run(){

        TGSqlParser sqlparser = new TGSqlParser(this.dbvendor);

            foundColumnsCount = 0;
            foundTableCount = 0;
            if (sqlfile.length() > 0){
            sqlparser.sqlfilename = sqlfile;
            }else{
                sqlparser.sqltext = sqltext;
            }

            // if you need a callback function to help determine
            // table and column relationship, set it here!
           //sqlparser.setMetaDatabase(new metaDB());

            int ret = sqlparser.parse();
            if (ret == 0){

               TCustomSqlStatement stmt = null;
               for (int i=0;i<sqlparser.sqlstatements.size();i++){
                   analyzeStmt(sqlparser.sqlstatements.get(i));
               }

                String[] foundTables2 = new String[foundTableCount];
                for(int k1=0;k1<foundTableCount;k1++){
                        foundTables[k1] = foundTables[k1].toLowerCase();
                }
                System.arraycopy(foundTables,0,foundTables2,0,foundTableCount);
                Set set= new HashSet(Arrays.asList(foundTables2));
                Object[] foundTables3 = set.toArray();
                Arrays.sort(foundTables3);

                String[] foundColumns2 = new String[foundColumnsCount];
                for(int k1=0;k1<foundColumnsCount;k1++){
                        foundColumns[k1] = foundColumns[k1].toLowerCase();
                }
                System.arraycopy(foundColumns,0,foundColumns2,0,foundColumnsCount);
                //System.out.println("before sort:"+foundColumnsCount);

                Set set2= new HashSet(Arrays.asList(foundColumns2));
                Object[] foundColumns3 = set2.toArray();
                Arrays.sort(foundColumns3);
                //System.out.println("after sort:"+foundColumns3.length);


                //System.out.println("Tables:");
                stringBuffer.append("Tables:\n");
                for(int j=0;j<foundTables3.length;j++){
                    //System.out.println(foundTables3[j]);
                    stringBuffer.append(foundTables3[j]+"\n");
                }

                //System.out.println("\nColumns:");
                stringBuffer.append("Columns:\n");
                for(int j=0;j<foundColumns3.length;j++){
                    //System.out.println(foundColumns3[j]);
                    stringBuffer.append(foundColumns3[j]+"\n");
                }

            }else{
                //System.out.println(sqlparser.getErrormessage());
                stringBuffer.append(sqlparser.getErrormessage()+"\n");
            }

         return stringBuffer.toString();
        }


    protected void analyzeStmt(TCustomSqlStatement stmt){
        for(int i=0;i<stmt.tables.size();i++){
            if (stmt.tables.getTable(i).isBaseTable())
            {
                if ( (stmt.dbvendor == EDbVendor.dbvmssql)
                        &&( (stmt.tables.getTable(i).getFullName().equalsIgnoreCase("deleted"))
                            ||(stmt.tables.getTable(i).getFullName().equalsIgnoreCase("inserted"))
                           )
                  ){
                    continue;
                }

              foundTables[foundTableCount] = stmt.tables.getTable(i).getFullName();
              foundTableCount++;
              for (int j=0;j<stmt.tables.getTable(i).getObjectNameReferences().size();j++){
                foundColumns[foundColumnsCount] = stmt.tables.getTable(i).getFullName()+"."+stmt.tables.getTable(i).getObjectNameReferences().getObjectName(j).getColumnNameOnly();
                foundColumns[foundColumnsCount] += "(table determined:"+stmt.tables.getTable(i).getObjectNameReferences().getObjectName(j).isTableDetermined()+")";
                  foundColumnsCount++;
              }
            }
            //System.out.println(stmt.tables.getTable(i).getFullName());
        }

        for (int i=0;i<stmt.getStatements().size();i++){
           analyzeStmt(stmt.getStatements().get(i));
        }
    }

}

public class gettablecolumns {

    public String run(EDbVendor dbVendor ,String sqltext){
        getObject g = new getObject(dbVendor);

        g.setSqltext(sqltext);
        return g.run();
    }

    public static void main(String args[])
     {
       long t = System.currentTimeMillis();

       if (args.length != 1){
           System.out.println("Usage: java gettablecolumns sqlfile.sql");
           return;
       }
       File file=new File(args[0]);
       if (!file.exists()){
           System.out.println("File not exists:"+args[0]);
           return;
       }

     EDbVendor dbVendor = EDbVendor.dbvoracle;
     String msg = "Please select SQL dialect: 1: SQL Server, 2: Oralce, 3: MySQL, 4: DB2, 5: PostGRESQL, 6: Teradta, 7: Sybase, default is 2: Oracle";
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
         }else if(db == 7){
             dbVendor = EDbVendor.dbvsybase;
         }
     }catch(IOException i) {
     }catch (NumberFormatException numberFormatException){
     }

     System.out.println("Selected SQL dialect: "+dbVendor.toString());

     getObject g = new getObject(dbVendor);

     g.setSqlfile(args[0]);
     System.out.println(g.run());

    // System.out.println("Time Escaped: "+ (System.currentTimeMillis() - t) );
     }

}
