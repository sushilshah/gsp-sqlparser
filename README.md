gsp-sqlparser
=============

gsp-sqlparser


downloaded from this site:
http://www.sqlparser.com/




1. In order to compile and run this Java demo, make sure JDK must be installed (JDK version >= 1.5)
you can always download the latest version of JDK here:
http://www.oracle.com/technetwork/java/javase/downloads/index.html

after download this JDK, then install it.

2. download the latest version of General SQL Parser Java version here:
http://www.sqlparser.com/download.php

3. unzip the downloaded general sql parser libary file to c:\tmp, 
you will find demos under c:\tmp\gsp_java_trial\dist\demos

Or you can download more demos here: http://www.dpriver.com/blog/list-of-demos-illustrate-how-to-use-general-sql-parser/


4. set java environment before compile and run demos, replace java home directory with correct value
  4.1 open a dos command window, now you at c:\tmp\gsp_java_trial\dist>
	4.2 set java_home="C:\Program Files\Java\jdk1.5.0_09";
	4.3 set path=%path%;%java_home%\bin;
	4.4 set classpath=%classpath%;%java_home%\jre\lib\rt.jar;./gsp.jar

5. we compile and run checksyntax demo under demos\checksyntax
	5.1 javac demos\checksyntax\checksyntax.java
	5.2 create a simple sql file test.sql under c:\tmp\gsp_java_trial\dist with following sample sql:
		select count(*) from tab
	5.2 java  demos.checksyntax.checksyntax test.sql
	5.3 after select a SQL dialect, you will see a message: Check syntax ok!
	
6. you can compile other demos under c:\tmp\gsp_java_trial\dist\demos

