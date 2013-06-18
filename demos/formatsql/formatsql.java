package demos.formatsql;
/*
 * Date: 2010-11-9
 * Time: 9:38:43
 */


import gudusoft.gsqlparser.EDbVendor;
import gudusoft.gsqlparser.TGSqlParser;

import gudusoft.gsqlparser.pp.para.GFmtOptFactory;
import gudusoft.gsqlparser.pp.para.GFmtOpt;
import gudusoft.gsqlparser.pp.para.GOutputFmt;
import gudusoft.gsqlparser.pp.para.styleenums.TLinefeedsCommaOption;
import gudusoft.gsqlparser.pp.stmtformatter.FormatterFactory;

import java.io.File;


public class formatsql {

    public static void main(String args[])
     {

         if (args.length != 1){
             System.out.println("Usage: java formatsql sqlfile.sql");
             return;
         }
         File file=new File(args[0]);
         if (!file.exists()){
             System.out.println("File not exists:"+args[0]);
             return;
         }

        TGSqlParser sqlparser = new TGSqlParser(EDbVendor.dbvoracle);
         sqlparser.sqlfilename = args[0];
        //sqlparser.sqltext = "select col1, col2,sum(col3) from table1, table2 where col4 > col5 and col6= 1000";
//         TGSqlParser sqlparser = new TGSqlParser(EDbVendor.dbvpostgresql);
//         sqlparser.sqltext ="WITH upd AS (\n" +
//                 "  UPDATE employees SET sales_count = sales_count + 1 WHERE id =\n" +
//                 "    (SELECT sales_person FROM accounts WHERE name = 'Acme Corporation')\n" +
//                 "    RETURNING *\n" +
//                 ")\n" +
//                 "INSERT INTO employees_log SELECT *, current_timestamp FROM upd;";



        int ret = sqlparser.parse();
        if (ret == 0){
            GFmtOpt option = GFmtOptFactory.newInstance();
            //option.selectColumnlistComma =     TLinefeedsCommaOption.LfBeforeComma;
            // umcomment next line generate formatted sql in html
            //option.outputFmt =  GOutputFmt.ofhtml;
            String result = FormatterFactory.pp(sqlparser, option);
            System.out.println(result);
        }else{
            System.out.println(sqlparser.getErrormessage());
        }
     }

}