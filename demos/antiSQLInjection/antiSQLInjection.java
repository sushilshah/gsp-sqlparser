package demos.antiSQLInjection;


import gudusoft.gsqlparser.EDbVendor;

public class antiSQLInjection  {

    public static void main(String args[])
     {

         String sqltext = "Select ID from TABLE1 where ID IN (select ID from TABLE1 where ID > 50032323 or 1=1)";
         TAntiSQLInjection anti = new TAntiSQLInjection(EDbVendor.dbvoracle);
         if (anti.isInjected(sqltext)){
            System.out.println("SQL injected found:");
            for(int i=0;i<anti.getSqlInjections().size();i++){
                System.out.println("type: "+anti.getSqlInjections().get(i).getType()+", description: "+ anti.getSqlInjections().get(i).getDescription());
            }
         }else {
             System.out.println("Not injected");
         }

     }

}