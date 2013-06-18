
package demos.columnImpact;

import gudusoft.gsqlparser.EDbVendor;
import gudusoft.gsqlparser.EExpressionType;
import gudusoft.gsqlparser.EFunctionType;
import gudusoft.gsqlparser.ETableSource;
import gudusoft.gsqlparser.TCustomSqlStatement;
import gudusoft.gsqlparser.TGSqlParser;
import gudusoft.gsqlparser.TSourceToken;
import gudusoft.gsqlparser.nodes.IExpressionVisitor;
import gudusoft.gsqlparser.nodes.TCTE;
import gudusoft.gsqlparser.nodes.TCaseExpression;
import gudusoft.gsqlparser.nodes.TExpression;
import gudusoft.gsqlparser.nodes.TFunctionCall;
import gudusoft.gsqlparser.nodes.TGroupByItem;
import gudusoft.gsqlparser.nodes.TInExpr;
import gudusoft.gsqlparser.nodes.TJoin;
import gudusoft.gsqlparser.nodes.TJoinItem;
import gudusoft.gsqlparser.nodes.TOrderBy;
import gudusoft.gsqlparser.nodes.TOrderByItem;
import gudusoft.gsqlparser.nodes.TOrderByItemList;
import gudusoft.gsqlparser.nodes.TParseTreeNode;
import gudusoft.gsqlparser.nodes.TParseTreeNodeList;
import gudusoft.gsqlparser.nodes.TResultColumn;
import gudusoft.gsqlparser.nodes.TTable;
import gudusoft.gsqlparser.nodes.TTableList;
import gudusoft.gsqlparser.nodes.TTrimArgument;
import gudusoft.gsqlparser.nodes.TWhenClauseItem;
import gudusoft.gsqlparser.nodes.TWhenClauseItemList;
import gudusoft.gsqlparser.stmt.TSelectSqlStatement;
import gudusoft.gsqlparser.stmt.TUpdateSqlStatement;

import java.awt.Point;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class ColumnImpact
{

	enum ClauseType {
		connectby, groupby, join, orderby, select, startwith, undefine, where
	}

	class columnsInExpr implements IExpressionVisitor
	{

		private List<TColumn> columns;
		private TExpression expr;
		private ColumnImpact impact;
		private int level;
		private TCustomSqlStatement stmt;

		public columnsInExpr( ColumnImpact impact, TExpression expr,
				List<TColumn> columns, TCustomSqlStatement stmt, int level )
		{
			this.stmt = stmt;
			this.impact = impact;
			this.expr = expr;
			this.columns = columns;
			this.level = level;
		}

		private void addColumnToList( TParseTreeNodeList list,
				TCustomSqlStatement stmt )
		{
			if ( list != null )
			{
				for ( int i = 0; i < list.size( ); i++ )
				{
					List<TExpression> exprList = new ArrayList<TExpression>( );
					Object element = list.getElement( i );

					if ( element instanceof TGroupByItem )
					{
						exprList.add( ( (TGroupByItem) element ).getExpr( ) );
					}
					if ( element instanceof TOrderByItem )
					{
						exprList.add( ( (TOrderByItem) element ).getSortKey( ) );
					}
					else if ( element instanceof TExpression )
					{
						exprList.add( (TExpression) element );
					}
					else if ( element instanceof TWhenClauseItem )
					{
						exprList.add( ( (TWhenClauseItem) element ).getComparison_expr( ) );
						exprList.add( ( (TWhenClauseItem) element ).getReturn_expr( ) );
					}

					for ( TExpression expr : exprList )
					{
						expr.inOrderTraverse( this );
					}
				}
			}
		}

		public boolean exprVisit( TParseTreeNode pNode, boolean isLeafNode )
		{
			TExpression lcexpr = (TExpression) pNode;
			if ( lcexpr.getExpressionType( ) == EExpressionType.simple_object_name_t )
			{
				columns.add( impact.attrToColumn( lcexpr, stmt ) );
			}
			else if ( lcexpr.getExpressionType( ) == EExpressionType.between_t )
			{
				columns.add( impact.attrToColumn( lcexpr.getBetweenOperand( ),
						stmt ) );
			}
			else if ( lcexpr.getExpressionType( ) == EExpressionType.function_t )
			{
				TFunctionCall func = (TFunctionCall) lcexpr.getFunctionCall( );
				if ( func.getFunctionType( ) == EFunctionType.trim_t )
				{
					TTrimArgument args = func.getTrimArgument( );
					TExpression expr = args.getStringExpression( );
					if ( expr != null && !expr.toString( ).trim( ).equals( "*" ) )
					{
						expr.inOrderTraverse( this );
					}
					expr = args.getTrimCharacter( );
					if ( expr != null && !expr.toString( ).trim( ).equals( "*" ) )
					{
						expr.inOrderTraverse( this );
					}
				}
				else if ( func.getFunctionType( ) == EFunctionType.cast_t )
				{
					TExpression expr = func.getExpr1( );
					if ( expr != null
							&& !expr.toString( ).trim( ).equals( "*" )
							|| func.getFunctionType( ) == EFunctionType.extract_t )
					{
						expr.inOrderTraverse( this );
					}
				}
				else if ( func.getFunctionType( ) == EFunctionType.convert_t )
				{
					TExpression expr = func.getExpr1( );
					if ( expr != null && !expr.toString( ).trim( ).equals( "*" ) )
					{
						expr.inOrderTraverse( this );
					}
					expr = func.getExpr2( );
					if ( expr != null && !expr.toString( ).trim( ).equals( "*" ) )
					{
						expr.inOrderTraverse( this );
					}
				}
				else if ( func.getFunctionType( ) == EFunctionType.contains_t
						|| func.getFunctionType( ) == EFunctionType.freetext_t )
				{
					TExpression expr = func.getExpr1( );
					if ( expr != null && !expr.toString( ).trim( ).equals( "*" ) )
					{
						expr.inOrderTraverse( this );
					}
					TInExpr inExpr = func.getInExpr( );
					if ( inExpr.getExprList( ) != null )
					{
						for ( int k = 0; k < inExpr.getExprList( ).size( ); k++ )
						{
							expr = inExpr.getExprList( ).getExpression( k );
							if ( expr.toString( ).trim( ).equals( "*" ) )
								continue;
							expr.inOrderTraverse( this );
						}
						if ( expr != null
								&& !expr.toString( ).trim( ).equals( "*" ) )
						{
							expr.inOrderTraverse( this );
						}
					}
					expr = inExpr.getFunc_expr( );
					if ( expr != null && !expr.toString( ).trim( ).equals( "*" ) )
					{
						expr.inOrderTraverse( this );
					}
				}
				else if ( func.getFunctionType( ) == EFunctionType.extractxml_t )
				{
					TExpression expr = func.getXMLType_Instance( );
					if ( expr != null && !expr.toString( ).trim( ).equals( "*" ) )
					{
						expr.inOrderTraverse( this );
					}
					expr = func.getXPath_String( );
					if ( expr != null && !expr.toString( ).trim( ).equals( "*" ) )
					{
						expr.inOrderTraverse( this );
					}
					expr = func.getNamespace_String( );
					if ( expr != null && !expr.toString( ).trim( ).equals( "*" ) )
					{
						expr.inOrderTraverse( this );
					}
				}

				if ( func.getFunctionType( ) == EFunctionType.rank_t )
				{
					TOrderByItemList orderByList = func.getOrderByList( );
					for ( int k = 0; k < orderByList.size( ); k++ )
					{
						TExpression expr = orderByList.getOrderByItem( k )
								.getSortKey( );
						if ( expr.toString( ).trim( ).equals( "*" ) )
							continue;
						expr.inOrderTraverse( this );
					}
				}
				else if ( func.getArgs( ) != null )
				{
					for ( int k = 0; k < func.getArgs( ).size( ); k++ )
					{
						TExpression expr = func.getArgs( ).getExpression( k );
						if ( expr.toString( ).trim( ).equals( "*" ) )
							continue;
						expr.inOrderTraverse( this );
					}
				}
				if ( func.getAnalyticFunction( ) != null )
				{
					TParseTreeNodeList list = func.getAnalyticFunction( )
							.getPartitionBy_ExprList( );
					addColumnToList( list, stmt );

					if ( func.getAnalyticFunction( ).getOrderBy( ) != null )
					{
						list = func.getAnalyticFunction( )
								.getOrderBy( )
								.getItems( );
						addColumnToList( list, stmt );
					}
				}

			}
			else if ( lcexpr.getExpressionType( ) == EExpressionType.subquery_t )
			{

				for ( int i = 0; i < ( (TSelectSqlStatement) lcexpr.getSubQuery( ) ).getResultColumnList( )
						.size( ); i++ )
				{
					impact.linkFieldToTables( null,
							( (TSelectSqlStatement) lcexpr.getSubQuery( ) ).getResultColumnList( )
									.getResultColumn( i ),
							(TSelectSqlStatement) lcexpr.getSubQuery( ),
							level + 1 );
				}

			}
			else if ( lcexpr.getExpressionType( ) == EExpressionType.case_t )
			{
				TCaseExpression expr = lcexpr.getCaseExpression( );
				TExpression conditionExpr = expr.getInput_expr( );
				if ( conditionExpr != null )
				{
					conditionExpr.inOrderTraverse( this );
				}
				TExpression defaultExpr = expr.getElse_expr( );
				if ( defaultExpr != null )
				{
					defaultExpr.inOrderTraverse( this );
				}
				TWhenClauseItemList list = expr.getWhenClauseItemList( );
				addColumnToList( list, stmt );
			}
			return true;
		}

		public void searchColumn( )
		{
			this.expr.inOrderTraverse( this );
		}
	}

	public static interface IMetaDatabaseFilter
	{

		boolean metaDatabaseTableColumn( String tableOwner, String tableName,
				String columnName );
	}

	static class MetaDatabaseFilter implements IMetaDatabaseFilter
	{

		public boolean metaDatabaseTableColumn( String tableOwner,
				String tableName, String columnName )
		{
			if ( "other_table".equalsIgnoreCase( tableName ) )
			{
				if ( "c1".equalsIgnoreCase( columnName ) )
					return true;
				else
					return false;
			}
			if ( "some_table".equalsIgnoreCase( tableName ) )
			{
				if ( "c1".equalsIgnoreCase( columnName ) )
					return false;
				else
					return true;
			}
			return true;
		}
	}

	class Table
	{

		public String prefixName;
		public String tableAlias;
		public String tableName;
	}

	class TAlias
	{

		public String alias;
		public String column;
		public Point location;
	}

	class TColumn
	{

		public String columnName;
		public String columnPrex;
		public String orignColumn;
		public Point location;
		public List<String> tableNames = new ArrayList<String>( );

		public String getFullName( String tableName )
		{
			if ( tableName != null )
			{
				return tableName + "." + columnName;
			}
			else
			{
				return columnName;
			}
		}

		public String getOrigName( )
		{
			if ( columnPrex != null )
			{
				return columnPrex + "." + columnName;
			}
			else
			{
				return columnName;
			}
		}

	}

	class TResultEntry
	{

		public ClauseType clause;

		public String targetColumn;
		public TTable targetTable;
		public Point location;

		public TResultEntry( TTable table, String column, ClauseType clause,
				Point location )
		{
			this.targetTable = table;
			this.targetColumn = column;
			this.clause = clause;
			this.location = location;
		}
	}

	class TSourceColumn
	{

		public List<ClauseType> clauses = new ArrayList<ClauseType>( );
		public String name;
		public String tableName;
		public String tableOwner;
		public Map<ClauseType, List<Point>> locations = new HashMap<ClauseType, List<Point>>( );
	}

	public static void main( String[] args )
	{
		if ( args.length == 0 )
		{
			System.out.println( "Usage: java ColumnImpact scriptfile [/d]/[/s [/xml] [/c]] [/o <output file path>] [/t <database type>]" );
			System.out.println( "/s: Option, display the analysis result simply." );
			System.out.println( "/c: Option, display the analysis result simply in column level." );
			System.out.println( "/d: Option, display the analysis result in detail." );
			System.out.println( "/xml: Option, export the analysis results to XML format, it's valid only if /s is specified" );
			System.out.println( "/o: Option, write the output stream to the specified file." );
			System.out.println( "/t: Option, set the database type. Support oracle, mysql, mssql and db2, the default type is oracle" );
			// Console.Read();
			return;
		}

		List<String> argList = Arrays.asList( args );

		boolean simply = argList.indexOf( "/s" ) != -1;

		boolean isXML = simply && argList.indexOf( "/xml" ) != -1;

		boolean isColumnLevel = simply && argList.indexOf( "/c" ) != -1;

		String outputFile = null;

		int index = argList.indexOf( "/o" );

		if ( index != -1 && args.length > index + 1 )
		{
			outputFile = args[index + 1];
		}

		FileOutputStream writer = null;
		if ( outputFile != null )
		{
			try
			{
				writer = new FileOutputStream( outputFile );
				System.setOut( new PrintStream( writer ) );
			}
			catch ( FileNotFoundException e )
			{
				e.printStackTrace( );
			}
		}

		EDbVendor vendor = EDbVendor.dbvoracle;

		index = argList.indexOf( "/t" );

		if ( index != -1 && args.length > index + 1 )
		{
			if ( args[index + 1].equalsIgnoreCase( "mssql" ) )
			{
				vendor = EDbVendor.dbvmssql;
			}
			else if ( args[index + 1].equalsIgnoreCase( "db2" ) )
			{
				vendor = EDbVendor.dbvdb2;
			}
			else if ( args[index + 1].equalsIgnoreCase( "mysql" ) )
			{
				vendor = EDbVendor.dbvmysql;
			}
			else if ( args[index + 1].equalsIgnoreCase( "mssql" ) )
			{
				vendor = EDbVendor.dbvmssql;
			}
		}

		ColumnImpact impact = new ColumnImpact( new File( args[0] ),
				vendor,
				simply,
				isXML,
				isColumnLevel,
				null );

		System.out.print( impact.getImpactResult( ) );

		if ( !simply )
		{
			System.out.println( "\r\nYou can add /s directive to display the analysis result in a simple format." );
		}

		try
		{
			// if ( outputFile == null )
			// {
			// System.in.read( );
			// }
			// else
			{
				if ( writer != null )
				{
					writer.close( );
				}
			}
		}
		catch ( IOException e )
		{
			e.printStackTrace( );
		}

	} // main
	/* store the relations of alias to column */
	private List<TAlias> aliases = new ArrayList<TAlias>( );
	private StringBuffer buffer = new StringBuffer( );
	private HashMap<String, TCustomSqlStatement> cteMap = new HashMap<String, TCustomSqlStatement>( );
	private HashMap<TCustomSqlStatement, ClauseType> currentClauseMap = new HashMap<TCustomSqlStatement, ClauseType>( );
	private String currentSource = null;
	/* store the dependency relations */
	private HashMap<String, List<TResultEntry>> dependMap = new HashMap<String, List<TResultEntry>>( );
	private IMetaDatabaseFilter filter;
	private boolean isXML = false;
	private boolean isColumnLevel = false;
	private boolean simply = false;
	private int columnNumber = 0;
	private TCustomSqlStatement subquery = null;

	public ColumnImpact( File file, EDbVendor dbVendor, Boolean simply,
			Boolean isXML )
	{
		this.simply = simply;
		this.isXML = isXML;
		TGSqlParser sqlparser = new TGSqlParser( dbVendor );
		sqlparser.sqlfilename = file.getAbsolutePath( );
		impactSQL( sqlparser );
	}

	public ColumnImpact( File file, EDbVendor dbVendor, Boolean simply,
			Boolean isXML, IMetaDatabaseFilter filter )
	{
		this.simply = simply;
		this.isXML = isXML;
		this.filter = filter;
		TGSqlParser sqlparser = new TGSqlParser( dbVendor );
		sqlparser.sqlfilename = file.getAbsolutePath( );
		impactSQL( sqlparser );
	}

	public ColumnImpact( String sql, EDbVendor dbVendor, Boolean simply,
			Boolean isXML )
	{
		this.simply = simply;
		this.isXML = isXML;
		TGSqlParser sqlparser = new TGSqlParser( dbVendor );
		sqlparser.sqltext = sql;
		impactSQL( sqlparser );
	}

	public ColumnImpact( String sql, EDbVendor dbVendor, Boolean simply,
			Boolean isXML, IMetaDatabaseFilter filter )
	{
		this.simply = simply;
		this.isXML = isXML;
		this.filter = filter;
		TGSqlParser sqlparser = new TGSqlParser( dbVendor );
		sqlparser.sqltext = sql;
		impactSQL( sqlparser );
	}

	public ColumnImpact( File file, EDbVendor dbVendor, boolean simply,
			boolean isXML, boolean isColumnLevel, IMetaDatabaseFilter filter )
	{
		this.simply = simply;
		this.isXML = isXML;
		this.isColumnLevel = isColumnLevel;
		this.filter = filter;
		TGSqlParser sqlparser = new TGSqlParser( dbVendor );
		sqlparser.sqlfilename = file.getAbsolutePath( );;
		impactSQL( sqlparser );
	}

	public ColumnImpact( String sql, EDbVendor dbVendor, Boolean simply,
			Boolean isXML, boolean isColumnLevel, IMetaDatabaseFilter filter )
	{
		this.simply = simply;
		this.isXML = isXML;
		this.isColumnLevel = isColumnLevel;
		this.filter = filter;
		TGSqlParser sqlparser = new TGSqlParser( dbVendor );
		sqlparser.sqltext = sql;
		impactSQL( sqlparser );
	}

	private TColumn attrToColumn( TExpression attr, TCustomSqlStatement stmt )
	{
		TColumn column = new TColumn( );
		column.columnName = attr.getEndToken( ).toString( );
		column.location = new Point( (int) attr.getEndToken( ).lineNo,
				(int) attr.getEndToken( ).columnNo );

		if ( attr.toString( ).indexOf( "." ) > 0 )
		{
			column.columnPrex = attr.toString( ).substring( 0,
					attr.toString( ).indexOf( "." ) );

			String tableName = column.columnPrex;
			if ( !column.tableNames.contains( tableName ) )
			{
				column.tableNames.add( tableName );
			}
		}
		else
		{
			TTableList tables = stmt.tables;
			for ( int i = 0; i < tables.size( ); i++ )
			{
				TTable lztable = tables.getTable( i );
				Table table = TLzTaleToTable( lztable );
				if ( !column.tableNames.contains( table.tableName ) )
				{
					column.tableNames.add( table.tableName );
				}
			}
		}

		column.orignColumn = column.columnName;

		return column;
	}

	private String buildString( String string, int level )
	{
		StringBuffer buffer = new StringBuffer( );
		for ( int i = 0; i < level; i++ )
		{
			buffer.append( string );
		}
		return buffer.toString( );
	}

	private TCustomSqlStatement containClasuse(
			HashMap<TCustomSqlStatement, ClauseType> currentClauseMap,
			TCustomSqlStatement select )
	{
		if ( currentClauseMap.containsKey( select ) )
			return select;
		else if ( select.getParentStmt( ) instanceof TCustomSqlStatement )
		{
			return containClasuse( currentClauseMap,
					(TCustomSqlStatement) select.getParentStmt( ) );
		}
		else
			return null;
	}

	private List<TColumn> exprToColumn( TExpression expr,
			TCustomSqlStatement stmt, int level )
	{
		List<TColumn> columns = new ArrayList<TColumn>( );

		columnsInExpr c = new columnsInExpr( this, expr, columns, stmt, level );
		c.searchColumn( );

		return columns;
	}

	private boolean findColumnInSubQuery( TSelectSqlStatement select,
			String columnName, int level, Point originLocation )
	{
		boolean ret = false;
		if ( select.getSetOperator( ) != TSelectSqlStatement.setOperator_none )
		{
			boolean left = findColumnInSubQuery( select.getLeftStmt( ),
					columnName,
					level,
					originLocation );
			boolean right = findColumnInSubQuery( select.getRightStmt( ),
					columnName,
					level,
					originLocation );
			ret = left && right;
		}
		else
		{
			// check colum name in select list of subquery
			TResultColumn columnField = null;
			if ( !"*".equals( columnName ) )
			{
				for ( int i = 0; i < select.getResultColumnList( ).size( ); i++ )
				{
					TResultColumn field = select.getResultColumnList( )
							.getResultColumn( i );
					if ( field.getAliasClause( ) != null )
					{
						if ( field.getAliasClause( )
								.toString( )
								.equalsIgnoreCase( columnName ) )
						{
							columnField = field;
							break;
						}
					}
					else
					{
						if ( field.getExpr( ).getExpressionType( ) == EExpressionType.simple_object_name_t )
						{
							TColumn column = attrToColumn( field.getExpr( ),
									select );
							if ( columnName.equalsIgnoreCase( column.columnName ) )
							{
								columnField = field;
								break;
							}
						}
					}
				}
			}
			for ( int i = 0; i < select.getResultColumnList( ).size( ); i++ )
			{
				TResultColumn field = select.getResultColumnList( )
						.getResultColumn( i );
				if ( columnField != null && !field.equals( columnField ) )
				{
					continue;
				}
				if ( field.getAliasClause( ) != null )
				{
					ret = "*".equals( columnName )
							|| field.getAliasClause( )
									.toString( )
									.equalsIgnoreCase( columnName );
					if ( ret )
					{
						// let's check where this column come from?
						if ( !simply )
						{
							buffer.append( buildString( " ", level )
									+ "--> "
									+ field.getAliasClause( ).toString( )
									+ "(alias)\r\n" );
						}
						linkFieldToTables( null, field, select, level );
					}
				}
				else
				{
					if ( field.getExpr( ).getExpressionType( ) == EExpressionType.simple_object_name_t )
					{
						TColumn column = attrToColumn( field.getExpr( ), select );
						ret = "*".equals( columnName )
								|| columnName.equalsIgnoreCase( column.columnName );
						if ( ret || "*".equals( column.columnName ) )
						{
							findColumnInTables( column,
									select,
									level,
									ret == false ? columnName : null,
									originLocation );
							findColumnsFromClauses( select, level + 1 );
						}
					}
				}

				if ( ret && !"*".equals( columnName ) )
					break;
			}
		}
		return ret;
	} // findColumnInSubQuery

	private boolean findColumnInTables( TColumn column, String tableName,
			TCustomSqlStatement select, int level )
	{
		return findColumnInTables( column,
				tableName,
				select,
				level,
				ClauseType.undefine );
	}

	private boolean findColumnInTables( TColumn column, String tableName,
			TCustomSqlStatement select, int level, ClauseType clause )
	{
		boolean ret = false;
		TTableList tables = select.tables;

		if ( tables.size( ) == 1 )
		{
			TTable lzTable = tables.getTable( 0 );
			// buffer.AppendLine(lzTable.AsText);
			if ( ( lzTable.getTableType( ) == ETableSource.objectname )
					&& ( tableName == null
							|| ( tableName != null
									&& lzTable.getAliasClause( ) == null && lzTable.getName( )
									.equalsIgnoreCase( tableName ) ) || ( tableName != null
							&& lzTable.getAliasClause( ) != null && lzTable.getAliasClause( )
							.toString( )
							.equalsIgnoreCase( tableName ) ) ) )
			{
				ret = true;

				if ( !simply )
				{
					buffer.append( buildString( " ", level )
							+ "--> "
							+ lzTable.getName( )
							+ "."
							+ column.columnName
							+ "\r\n" );
				}
				if ( cteMap.containsKey( lzTable.getName( ) ) )
				{
					if ( !simply )
					{
						buffer.append( buildString( " ", level )
								+ "--> WITH CTE\r\n" );
					}
					ret = findColumnInSubQuery( (TSelectSqlStatement) cteMap.get( lzTable.getName( ) ),
							column.columnName,
							level,
							column.location );
				}
				else
				{
					if ( currentSource != null
							&& dependMap.containsKey( currentSource ) )
					{
						TCustomSqlStatement stmt = containClasuse( currentClauseMap,
								select );
						if ( stmt != null )
						{
							dependMap.get( currentSource )
									.add( new TResultEntry( lzTable,
											column.columnName,
											(ClauseType) currentClauseMap.get( stmt ),
											column.location ) );
						}
						else if ( select instanceof TSelectSqlStatement )
						{
							if ( ClauseType.undefine.equals( clause ) )
								dependMap.get( currentSource )
										.add( new TResultEntry( lzTable,
												column.columnName,
												ClauseType.select,
												column.location ) );
							else
								dependMap.get( currentSource )
										.add( new TResultEntry( lzTable,
												column.columnName,
												clause,
												column.location ) );
						}
						else
						{
							dependMap.get( currentSource )
									.add( new TResultEntry( lzTable,
											column.columnName,
											ClauseType.undefine,
											column.location ) );
						}
					}
				}
			}
			else if ( select.getParentStmt( ) instanceof TSelectSqlStatement )
			{
				subquery = select;
				ret = findColumnInTables( column,
						tableName,
						select.getParentStmt( ),
						level,
						clause );
				subquery = null;
			}
		}

		if ( ret )
			return ret;

		for ( int x = 0; x < tables.size( ); x++ )
		{
			TTable lztable = tables.getTable( x );
			switch ( lztable.getTableType( ) )
			{
				case objectname :
					Table table = TLzTaleToTable( lztable );
					String alias = table.tableAlias;
					if ( alias != null )
						alias = alias.trim( );
					if ( ( tableName != null )
							&& ( ( tableName.equalsIgnoreCase( alias ) || tableName.equalsIgnoreCase( table.tableName ) ) ) )
					{
						if ( !simply )
						{
							buffer.append( buildString( " ", level )
									+ "--> "
									+ table.tableName
									+ "."
									+ column.columnName
									+ "\r\n" );
						}
						if ( cteMap.containsKey( lztable.getName( ) ) )
						{
							if ( !simply )
							{
								buffer.append( buildString( " ", level )
										+ "--> WITH CTE\r\n" );
							}
							ret = findColumnInSubQuery( (TSelectSqlStatement) cteMap.get( lztable.getName( ) ),
									column.columnName,
									level,
									column.location );
						}
						else
						{
							if ( dependMap.containsKey( currentSource ) )
							{
								String columnName = column.orignColumn;
								if ( "*".equals( columnName ) )
									columnName = column.columnName;
								if ( currentClauseMap.containsKey( select ) )
								{
									dependMap.get( currentSource )
											.add( new TResultEntry( lztable,
													columnName,
													(ClauseType) currentClauseMap.get( select ),
													column.location ) );
								}
								else if ( select instanceof TSelectSqlStatement )
								{
									dependMap.get( currentSource )
											.add( new TResultEntry( lztable,
													columnName,
													ClauseType.select,
													column.location ) );
								}
								else
								{
									dependMap.get( currentSource )
											.add( new TResultEntry( lztable,
													columnName,
													ClauseType.undefine,
													column.location ) );
								}
							}
							ret = true;
						}
					}
					break;
				case subquery :
					for ( int i = 0; i < column.tableNames.size( ); i++ )
					{
						String name = column.tableNames.get( i );
						TSelectSqlStatement selectStat = (TSelectSqlStatement) lztable.getSubquery( );

						if ( selectStat == subquery )
							continue;

						if ( name == null )
						{
							ret = findColumnInSubQuery( selectStat,
									column.columnName,
									level,
									column.location );
							break;
						}

						if ( lztable.getAliasClause( ) != null
								&& lztable.getAliasClause( )
										.getAliasName( )
										.toString( )
										.equalsIgnoreCase( name ) )
						{
							ret = findColumnInSubQuery( selectStat,
									column.columnName,
									level,
									column.location );
							break;
						}

						boolean flag = false;
						for ( int j = 0; j < selectStat.tables.size( ); j++ )
						{
							if ( selectStat.tables.getTable( j )
									.getAliasClause( ) != null )
							{
								if ( selectStat.tables.getTable( j )
										.getAliasClause( )
										.getAliasName( )
										.toString( )
										.equalsIgnoreCase( name ) )
								{
									ret = findColumnInSubQuery( selectStat,
											column.columnName,
											level,
											column.location );
									flag = true;
									break;
								}
							}
							else
							{
								if ( selectStat.tables.getTable( j )
										.getTableName( )
										.toString( )
										.equalsIgnoreCase( name ) )
								{
									ret = findColumnInSubQuery( selectStat,
											column.columnName,
											level,
											column.location );
									flag = true;
									break;
								}
							}
						}
						if ( flag )
							break;
					}
					break;
				default :
					break;
			}
			if ( ret )
				break;
		}

		if ( !ret && select.getParentStmt( ) instanceof TSelectSqlStatement )
		{
			subquery = select;
			ret = findColumnInTables( column,
					tableName,
					select.getParentStmt( ),
					level,
					clause );
			subquery = null;
		}

		return ret;
	}

	private boolean findColumnInTables( TColumn column,
			TCustomSqlStatement select, int level, String columnName,
			Point originLocation )
	{
		boolean ret = false;
		for ( String tableName : column.tableNames )
		{
			if ( columnName != null && filter != null )
			{
				int dotIndex = tableName.lastIndexOf( "." );
				String tableOwner = null;
				String tableRealName = null;
				if ( dotIndex >= 0 )
				{
					tableOwner = tableName.substring( 0, dotIndex );
					tableRealName = tableName.replace( tableOwner + ".", "" );
				}
				else
				{
					tableRealName = tableName;
				}
				if ( filter.metaDatabaseTableColumn( tableOwner,
						tableRealName,
						columnName ) )
				{
					column.columnName = columnName;
					if ( originLocation != null )
						column.location = originLocation;
					// column.orignColumn = "*";
					ret |= findColumnInTables( column, tableName, select, level );
				}
				else
					ret |= false;
			}
			else
				ret |= findColumnInTables( column, tableName, select, level );
		}
		return ret;
	}

	private void findColumnsFromClauses( TCustomSqlStatement select, int level )
	{
		currentClauseMap.put( select, ClauseType.undefine );
		HashMap<TExpression, ClauseType> clauseTable = new LinkedHashMap<TExpression, ClauseType>( );
		if ( select instanceof TSelectSqlStatement )
		{

			TSelectSqlStatement statement = (TSelectSqlStatement) select;

			if ( statement.getOrderbyClause( ) != null )
			{
				TOrderBy sortList = statement.getOrderbyClause( );
				for ( int i = 0; i < sortList.getItems( ).size( ); i++ )
				{
					TOrderByItem orderBy = sortList.getItems( )
							.getOrderByItem( i );
					TExpression expr = orderBy.getSortKey( );
					clauseTable.put( expr, ClauseType.orderby );
				}
			}

			if ( statement.getWhereClause( ) != null )
			{
				clauseTable.put( statement.getWhereClause( ).getCondition( ),
						ClauseType.where );
			}
			if ( statement.getHierarchicalClause( ) != null
					&& statement.getHierarchicalClause( ).getConnectByClause( ) != null )
			{
				clauseTable.put( statement.getHierarchicalClause( )
						.getConnectByClause( ), ClauseType.connectby );
			}
			if ( statement.getHierarchicalClause( ) != null
					&& statement.getHierarchicalClause( ).getStartWithClause( ) != null )
			{
				clauseTable.put( statement.getHierarchicalClause( )
						.getStartWithClause( ), ClauseType.startwith );
			}
			if ( statement.joins != null )
			{
				for ( int i = 0; i < statement.joins.size( ); i++ )
				{
					TJoin join = statement.joins.getJoin( i );
					if ( join.getJoinItems( ) != null )
					{
						for ( int j = 0; j < join.getJoinItems( ).size( ); j++ )
						{
							TJoinItem joinItem = join.getJoinItems( )
									.getJoinItem( j );
							TExpression expr = joinItem.getOnCondition( );
							if ( expr != null )
								clauseTable.put( expr, ClauseType.join );
						}
					}
				}
			}
		}
		else if ( select instanceof TUpdateSqlStatement )
		{
			TUpdateSqlStatement statement = (TUpdateSqlStatement) select;
			if ( statement.getOrderByClause( ) != null )
			{
				TOrderByItemList sortList = statement.getOrderByClause( )
						.getItems( );
				for ( int i = 0; i < sortList.size( ); i++ )
				{
					TOrderByItem orderBy = sortList.getOrderByItem( i );
					TExpression expr = orderBy.getSortKey( );
					clauseTable.put( expr, ClauseType.orderby );
				}
			}
			if ( statement.getWhereClause( ) != null )
			{
				clauseTable.put( statement.getWhereClause( ).getCondition( ),
						ClauseType.where );
			}

			if ( statement.joins != null )
			{
				for ( int i = 0; i < statement.joins.size( ); i++ )
				{
					TJoin join = statement.joins.getJoin( i );
					if ( join.getJoinItems( ) != null )
					{
						for ( int j = 0; j < join.getJoinItems( ).size( ); j++ )
						{
							TJoinItem joinItem = join.getJoinItems( )
									.getJoinItem( j );
							TExpression expr = joinItem.getOnCondition( );
							if ( expr != null )
								clauseTable.put( expr, ClauseType.join );
						}
					}
				}
			}
		}

		for ( TExpression expr : clauseTable.keySet( ) )
		{
			currentClauseMap.put( select, clauseTable.get( expr ) );

			if ( !simply )
			{
				switch ( (ClauseType) currentClauseMap.get( select ) )
				{
					case where :
						buffer.append( buildString( " ", level )
								+ "--> Where Clause\r\n" );
						break;
					case connectby :
						buffer.append( buildString( " ", level )
								+ "--> Connect By Clause\r\n" );
						break;
					case startwith :
						buffer.append( buildString( " ", level )
								+ "--> Start With Clause\r\n" );
						break;
					case orderby :
						buffer.append( buildString( " ", level )
								+ "--> Order By Clause\r\n" );
						break;
					case join :
						buffer.append( buildString( " ", level )
								+ "--> Join\r\n" );
						break;
				}

			}

			List<TColumn> columns = exprToColumn( expr, select, level );
			for ( TColumn column1 : columns )
			{
				for ( String tableName : column1.tableNames )
				{
					if ( !simply )
					{

						switch ( (ClauseType) currentClauseMap.get( select ) )
						{
							case where :
								buffer.append( buildString( " ", level + 1 )
										+ "--> "
										+ column1.getFullName( tableName )
										+ "(Where)\r\n" );
								break;
							case connectby :
								buffer.append( buildString( " ", level + 1 )
										+ "--> "
										+ column1.getFullName( tableName )
										+ "(Connect By)\r\n" );
								break;
							case startwith :
								buffer.append( buildString( " ", level + 1 )
										+ "--> "
										+ column1.getFullName( tableName )
										+ "(Start With)\r\n" );
								break;
							case orderby :
								buffer.append( buildString( " ", level + 1 )
										+ "--> "
										+ column1.getFullName( tableName )
										+ "(Order By)\r\n" );
								break;
							case join :
								buffer.append( buildString( " ", level + 1 )
										+ "--> "
										+ column1.getFullName( tableName )
										+ "(Join)\r\n" );
								break;
						}

					}
					findColumnInTables( column1, tableName, select, level + 2 );
				}

			}
		}
		currentClauseMap.remove( select );

		// check order by clause
		findColumnsFromGroupBy( select, level );
	}

	private void findColumnsFromGroupBy( TCustomSqlStatement select, int level )
	{
		if ( select instanceof TSelectSqlStatement
				&& ( (TSelectSqlStatement) select ).getGroupByClause( ) != null )
		{
			for ( int j = 0; j < ( (TSelectSqlStatement) select ).getGroupByClause( )
					.getItems( )
					.size( ); j++ )
			{
				TGroupByItem i = ( (TSelectSqlStatement) select ).getGroupByClause( )
						.getItems( )
						.getGroupByItem( j );
				List<TColumn> columns1 = exprToColumn( i.getExpr( ),
						select,
						level );

				TColumn column1 = columns1.get( 0 );
				for ( String tableName : column1.tableNames )
				{
					if ( !simply )
					{
						buffer.append( buildString( " ", level )
								+ "--> "
								+ column1.getFullName( tableName )
								+ "(group by)\r\n" );
					}
					findColumnInTables( column1,
							tableName,
							select,
							level + 1,
							ClauseType.groupby );
				}

			}

		}
	}

	private void findColumnsFromList( TCustomSqlStatement select, int level,
			TParseTreeNodeList list )
	{
		if ( list == null )
			return;

		for ( int i = 0; i < list.size( ); i++ )
		{
			Object element = list.getElement( i );
			TExpression lcexpr = null;
			if ( element instanceof TGroupByItem )
			{
				lcexpr = ( (TGroupByItem) element ).getExpr( );
			}
			else if ( element instanceof TOrderByItem )
			{
				lcexpr = ( (TOrderByItem) element ).getSortKey( );
			}
			else if ( element instanceof TExpression )
			{
				lcexpr = (TExpression) element;
			}

			if ( lcexpr != null )
			{
				List<TColumn> columns = exprToColumn( lcexpr, select, level );
				for ( TColumn column1 : columns )
				{
					findColumnInTables( column1, select, level + 1, null, null );
					findColumnsFromClauses( select, level + 2 );
				}
			}
		}
	}

	public String getImpactResult( )
	{
		return buffer.toString( );
	}

	private void impactSQL( TGSqlParser sqlparser )
	{
		int ret = sqlparser.parse( );

		if ( ret != 0 )
		{
			buffer.append( sqlparser.getErrormessage( ) + "\r\n" );
		}
		else
		{
			Document doc = null;
			Element columnImpactResult = null;
			if ( simply && isXML )
			{
				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance( );
				try
				{
					DocumentBuilder builder = factory.newDocumentBuilder( );
					doc = builder.newDocument( );
					doc.setXmlVersion( "1.0" );
					columnImpactResult = doc.createElement( "columnImpactResult" );
					doc.appendChild( columnImpactResult );
				}
				catch ( ParserConfigurationException e )
				{
					e.printStackTrace( );
				}
			}

			for ( int k = 0; k < sqlparser.sqlstatements.size( ); k++ )
			{
				if ( sqlparser.sqlstatements.get( k ) instanceof TCustomSqlStatement )
				{
					dependMap.clear( );
					aliases.clear( );
					currentSource = null;
					cteMap.clear( );
					currentClauseMap.clear( );

					TCustomSqlStatement select = (TCustomSqlStatement) sqlparser.sqlstatements.get( k );
					if ( select.getCteList( ) != null
							&& select.getCteList( ).size( ) > 0 )
					{
						for ( int i = 0; i < select.getCteList( ).size( ); i++ )
						{
							TCTE expression = (TCTE) select.getCteList( )
									.getCTE( i );
							cteMap.put( expression.getTableName( ).toString( ),
									expression.getSubquery( ) );
						}
					}

					columnNumber = 0;
					impactSqlFromStatement( select );

					if ( simply )
					{
						if ( !isXML )
						{
							for ( TAlias alias : aliases )
							{

								buffer.append( alias.alias + " depends on: " );

								List<String> collections = new ArrayList<String>( );

								if ( dependMap.containsKey( alias.alias ) )
								{
									List<TResultEntry> results = (List<TResultEntry>) dependMap.get( alias.alias );
									for ( TResultEntry result : results )
									{
										String column = null;
										if ( isColumnLevel
												&& result.clause != ClauseType.select )
											continue;
										if ( "*".equals( result.targetColumn ) )
										{
											column = result.targetTable.getFullName( )
													.toLowerCase( );
										}
										else
										{
											column = ( result.targetTable.getFullName( )
													+ "." + result.targetColumn ).toLowerCase( );
										}
										if ( !collections.contains( column ) )
											collections.add( column );
									}
								}

								List<String> list = new ArrayList<String>( collections );
								for ( int i = 0; i < list.size( ); i++ )
								{
									if ( i < collections.size( ) - 1 )
										buffer.append( list.get( i ) + ", " );
									else
										buffer.append( list.get( i ) );
								}

								buffer.append( "\r\n" );

							}
						}
						else
						{

							for ( TAlias alias : aliases )
							{
								Element targetColumn = doc.createElement( "targetColumn" );
								targetColumn.setAttribute( "name", alias.column );
								targetColumn.setAttribute( "coordinate",
										alias.location.x
												+ ","
												+ alias.location.y );
								if ( !alias.alias.equals( alias.column ) )
									targetColumn.setAttribute( "alias",
											alias.alias );
								columnImpactResult.appendChild( targetColumn );

								HashMap<String, TSourceColumn> collections = new LinkedHashMap<String, TSourceColumn>( );

								if ( dependMap.containsKey( alias.alias ) )
								{
									List<TResultEntry> results = (List<TResultEntry>) dependMap.get( alias.alias );
									for ( TResultEntry result : results )
									{
										if ( isColumnLevel
												&& result.clause != ClauseType.select )
											continue;
										String key = null;
										if ( "*".equals( result.targetColumn ) )
										{
											key = result.targetTable.getFullName( )
													.toLowerCase( );
										}
										else
										{
											key = ( result.targetTable.getFullName( )
													.toLowerCase( )
													+ "." + result.targetColumn ).toLowerCase( );
										}

										TSourceColumn sourceColumn = null;
										if ( collections.containsKey( key ) )
										{
											sourceColumn = (TSourceColumn) collections.get( key );
											if ( !sourceColumn.clauses.contains( result.clause ) )
											{
												sourceColumn.clauses.add( result.clause );
											}

											if ( result.location != null )
											{
												if ( !sourceColumn.locations.containsKey( result.clause ) )
													sourceColumn.locations.put( result.clause,
															new ArrayList<Point>( ) );
												List<Point> ys = sourceColumn.locations.get( result.clause );
												if ( !ys.contains( result.location ) )
													ys.add( result.location );
											}

										}
										else
										{
											sourceColumn = new TSourceColumn( );
											collections.put( key, sourceColumn );
											sourceColumn.tableOwner = result.targetTable.getTableName( )
													.getSchemaString( );
											sourceColumn.tableName = result.targetTable.getName( );
											if ( !"*".equals( result.targetColumn ) )
											{
												sourceColumn.name = result.targetColumn;
											}
											if ( !sourceColumn.clauses.contains( result.clause ) )
											{
												sourceColumn.clauses.add( result.clause );
											}
											if ( result.location != null )
											{
												if ( !sourceColumn.locations.containsKey( result.clause ) )
													sourceColumn.locations.put( result.clause,
															new ArrayList<Point>( ) );
												List<Point> ys = sourceColumn.locations.get( result.clause );
												if ( !ys.contains( result.location ) )
													ys.add( result.location );
											}
										}
									}

									Iterator<String> iter = collections.keySet( )
											.iterator( );

									while ( iter.hasNext( ) )
									{
										TSourceColumn sourceColumn = (TSourceColumn) collections.get( iter.next( ) );
										if ( sourceColumn.clauses.size( ) > 0 )
										{
											for ( int j = 0; j < sourceColumn.clauses.size( ); j++ )
											{
												ClauseType clause = sourceColumn.clauses.get( j );
												Element element = doc.createElement( "sourceColumn" );
												if ( sourceColumn.tableOwner != null )
												{
													element.setAttribute( "tableOwner",
															sourceColumn.tableOwner );
												}
												if ( sourceColumn.tableName != null )
												{
													element.setAttribute( "tableName",
															sourceColumn.tableName );
												}
												if ( sourceColumn.name != null )
												{
													element.setAttribute( "name",
															sourceColumn.name );
												}
												{
													StringBuilder buffer = new StringBuilder( );
													switch ( clause )
													{
														case where :
															buffer.append( "where" );
															break;
														case connectby :
															buffer.append( "connect by" );
															break;
														case startwith :
															buffer.append( "start with" );
															break;
														case orderby :
															buffer.append( "order by" );
															break;
														case join :
															buffer.append( "join" );
															break;
														case select :
															buffer.append( "select" );
															break;
														case groupby :
															buffer.append( "group by" );
															break;
													}
													if ( buffer.toString( )
															.length( ) != 0 )
														element.setAttribute( "clause",
																buffer.toString( ) );
												}
												{
													StringBuilder buffer = new StringBuilder( );
													buildLocationString( sourceColumn,
															clause,
															buffer );
													if ( buffer.toString( )
															.length( ) != 0 )
														element.setAttribute( "coordinate",
																buffer.toString( ) );
												}
												targetColumn.appendChild( element );
											}
										}
									}
								}
							}
						}
					}
				}
			}
			if ( doc != null )
			{
				try
				{
					StringWriter sw = new StringWriter( );
					com.sun.org.apache.xml.internal.serialize.OutputFormat format = new com.sun.org.apache.xml.internal.serialize.OutputFormat( doc );
					format.setIndenting( true );
					format.setIndent( 2 );
					format.setLineWidth( 0 );
					Writer output = new BufferedWriter( sw );
					com.sun.org.apache.xml.internal.serialize.XMLSerializer serializer = new com.sun.org.apache.xml.internal.serialize.XMLSerializer( output,
							format );
					serializer.serialize( doc );
					buffer.append( sw.toString( ).trim( ) );
				}
				catch ( IOException e )
				{
					e.printStackTrace( );
				}
			}
		}
	}

	private void buildLocationString( TSourceColumn sourceColumn,
			ClauseType clauseType, StringBuilder locationBuffer )
	{
		List<Point> ys = sourceColumn.locations.get( clauseType );
		if ( ys != null )
		{
			for ( int z = 0; z < ys.size( ); z++ )
			{
				locationBuffer.append( ys.get( z ).x + "," + ys.get( z ).y );
				if ( z < ys.size( ) - 1 )
					locationBuffer.append( ";" );
			}
		}
	}

	private void impactSqlFromStatement( TCustomSqlStatement select )
	{
		if ( select instanceof TSelectSqlStatement )
		{
			TSelectSqlStatement stmt = (TSelectSqlStatement) select;
			if ( stmt.getSetOperator( ) != TSelectSqlStatement.setOperator_none )
			{
				impactSqlFromStatement( stmt.getLeftStmt( ) );
				impactSqlFromStatement( stmt.getRightStmt( ) );
			}
			else
			{
				for ( int i = 0; i < select.getResultColumnList( ).size( ); i++ )
				{
					linkFieldToTables( null, select.getResultColumnList( )
							.getResultColumn( i ), select, 0 );
				}
			}
		}
		else if ( select instanceof gudusoft.gsqlparser.stmt.TInsertSqlStatement
				&& ( (gudusoft.gsqlparser.stmt.TInsertSqlStatement) select ).getSubQuery( ) != null )
		{
			impactSqlFromStatement( ( (gudusoft.gsqlparser.stmt.TInsertSqlStatement) select ).getSubQuery( ) );
		}
		else
		{
			if ( select.getResultColumnList( ) != null )
			{
				for ( int i = 0; i < select.getResultColumnList( ).size( ); i++ )
				{
					linkFieldToTables( null, select.getResultColumnList( )
							.getResultColumn( i ), select, 0 );
				}
			}
		}
	}

	private boolean isPseudocolumn( String column )
	{
		if ( column == null )
			return false;
		if ( "rownum".equalsIgnoreCase( column.trim( ) ) )
			return true;
		else if ( "rowid".equalsIgnoreCase( column.trim( ) ) )
			return true;
		else if ( "nextval".equalsIgnoreCase( column.trim( ) ) )
			return true;
		else if ( "sysdate".equalsIgnoreCase( column.trim( ) ) )
			return true;
		return false;
	}

	private boolean linkFieldToTables( TAlias parentAlias, TResultColumn field,
			TCustomSqlStatement select, int level )
	{
		boolean ret = false;
		// all items in select list was represented by a TLzField Objects
		switch ( field.getExpr( ).getExpressionType( ) )
		{
			case simple_object_name_t :
				TColumn column = attrToColumn( field.getExpr( ), select );
				boolean isPseudocolumn = select.dbvendor == EDbVendor.dbvoracle
						&& this.isPseudocolumn( column.columnName );
				if ( level == 0 || parentAlias != null )
				{
					TAlias alias = null;
					if ( parentAlias != null )
					{
						alias = parentAlias;
					}
					else
					{
						alias = new TAlias( );
						alias.column = field.toString( );
						alias.alias = field.toString( );
						alias.location = new Point( (int) field.getStartToken( ).lineNo,
								(int) field.getStartToken( ).columnNo );
						if ( field.getAliasClause( ) != null )
						{
							alias.alias = field.getAliasClause( ).toString( );
							alias.column = field.toString( );
							TSourceToken startToken = field.getAliasClause( )
									.getAliasName( )
									.getStartToken( );
							alias.location = new Point( (int) startToken.lineNo,
									(int) startToken.columnNo );
						}
						aliases.add( alias );
					}
					currentSource = alias.alias;
					if ( !dependMap.containsKey( currentSource ) )
						dependMap.put( currentSource,
								new ArrayList<TResultEntry>( ) );

					if ( !simply && parentAlias == null )
					{
						if ( !alias.alias.equalsIgnoreCase( column.getOrigName( ) ) )
						{
							buffer.append( "\r\nSearch "
									+ alias.alias
									+ ( level == 0 ? ( " <<column_"
											+ ( ++columnNumber ) + ">>" ) : "" )
									+ "\r\n" );
							buffer.append( "--> "
									+ column.getOrigName( )
									+ ( !isPseudocolumn
											&& column.tableNames.size( ) > 1 ? ( " <<GUESS>>" )
											: "" )
									+ "\r\n" );
						}
						else
						{
							buffer.append( "\r\nSearch "
									+ column.getOrigName( )
									+ ( level == 0 ? ( " <<column_"
											+ ( ++columnNumber ) + ">>" ) : "" )
									+ ( !isPseudocolumn
											&& column.tableNames.size( ) > 1 ? ( " <<GUESS>>" )
											: "" )
									+ "\r\n" );
							level -= 1;
						}
					}

				}
				if ( isPseudocolumn )
				{
					break;
				}
				ret = findColumnInTables( column, select, level + 1, null, null );
				findColumnsFromClauses( select, level + 2 );
				break;
			case subquery_t :
				TAlias alias1 = new TAlias( );
				alias1.column = field.toString( );
				alias1.alias = field.toString( );
				alias1.location = new Point( (int) field.getStartToken( ).lineNo,
						(int) field.getStartToken( ).columnNo );
				if ( field.getAliasClause( ) != null )
				{
					alias1.alias = field.getAliasClause( ).toString( );
					TSourceToken startToken = field.getAliasClause( )
							.getAliasName( )
							.getStartToken( );
					alias1.column = field.toString( );
					alias1.location = new Point( (int) startToken.lineNo,
							(int) startToken.columnNo );
				}

				if ( level == 0 )
				{
					aliases.add( alias1 );
					if ( !simply )
					{
						buffer.append( "\r\nSearch "
								+ alias1.alias
								+ ( level == 0 ? ( " <<column_"
										+ ( ++columnNumber ) + ">>" ) : "" )
								+ "\r\n" );
						// buffer.append( "--> "
						// + field.getExpr( ).getSubQuery( )
						// + "\r\n" );
					}
				}
				TSelectSqlStatement stmt = (TSelectSqlStatement) field.getExpr( )
						.getSubQuery( );
				List<TSelectSqlStatement> stmtList = new ArrayList<TSelectSqlStatement>( );
				getSelectSqlStatements( stmt, stmtList );
				for ( int i = 0; i < stmtList.size( ); i++ )
				{
					linkFieldToTables( alias1, stmtList.get( i )
							.getResultColumnList( )
							.getResultColumn( 0 ), stmtList.get( i ), level - 1 );
				}
				break;
			default :
				if ( level == 0 )
				{
					TAlias alias = new TAlias( );

					if ( select instanceof TUpdateSqlStatement )
					{
						TExpression expression = field.getExpr( )
								.getLeftOperand( );
						alias.column = expression.toString( );
						alias.alias = alias.column;
						alias.location = new Point( (int) expression.getStartToken( ).lineNo,
								(int) expression.getStartToken( ).columnNo );
					}
					else
					{
						alias.column = field.toString( );
						alias.alias = alias.column;
						alias.location = new Point( (int) field.getStartToken( ).lineNo,
								(int) field.getStartToken( ).columnNo );

					}
					if ( field.getAliasClause( ) != null )
					{
						alias.alias = field.getAliasClause( ).toString( );
						alias.column = field.toString( );
						TSourceToken startToken = field.getAliasClause( )
								.getAliasName( )
								.getStartToken( );
						alias.location = new Point( (int) startToken.lineNo,
								(int) startToken.columnNo );
					}
					aliases.add( alias );
					if ( !simply )
					{
						buffer.append( "\r\n"
								+ "Search "
								+ alias.alias
								+ ( level == 0 ? ( " <<column_"
										+ ( ++columnNumber ) + ">>" ) : "" )
								+ "\r\n" );
					}

					currentSource = alias.alias;
					if ( !dependMap.containsKey( currentSource ) )
						dependMap.put( currentSource,
								new ArrayList<TResultEntry>( ) );
				}

				List<TColumn> columns = exprToColumn( field.getExpr( ),
						select,
						level );
				if ( select instanceof TUpdateSqlStatement )
				{
					while ( columns.size( ) > 1 )
					{
						columns.remove( columns.size( ) - 1 );
					}
				}
				if ( !simply )
				{
					for ( TColumn column1 : columns )
					{
						if ( level == 0 )
						{
							buffer.append( buildString( " ", level )
									+ "--> "
									+ column1.getOrigName( )
									+ "\r\n" );
						}
					}
				}

				for ( TColumn column1 : columns )
				{
					if ( level == 0 )
					{
						if ( !simply )
						{
							buffer.append( "\r\n"
									+ "Search "
									+ column1.getOrigName( )
									+ "\r\n" );
						}
					}
					if ( !( select instanceof TUpdateSqlStatement ) )
					{
						findColumnInTables( column1,
								select,
								level + 1,
								null,
								null );
					}
					findColumnsFromClauses( select, level + 2 );
				}

				if ( field.getExpr( ).getExpressionType( ) == EExpressionType.function_t )
				{
					TFunctionCall func = (TFunctionCall) field.getExpr( )
							.getFunctionCall( );
					// buffer.AppendLine("function name {0}",
					// func.funcname.AsText);
					if ( func.getFunctionName( )
							.toString( )
							.equalsIgnoreCase( "count" )
							|| func.getFunctionName( )
									.toString( )
									.equalsIgnoreCase( "sum" )
							|| func.getFunctionName( )
									.toString( )
									.equalsIgnoreCase( "row_number" ) )
					{
						if ( !simply )
						{
							buffer.append( buildString( " ", level + 1 )
									+ "--> aggregate function "
									+ func.toString( )
									+ "\r\n" );
							for ( int i = 0; i < select.tables.size( ); i++ )
							{
								if ( select.tables.getTable( i ).getSubquery( ) == null )
								{
									buffer.append( buildString( " ", level + 1 )
											+ "--> table "
											+ select.tables.getTable( i )
													.getFullNameWithAliasString( )
											+ "\r\n" );
								}
								else
								{
									buffer.append( buildString( " ", level + 1 )
											+ "--> table "
											+ select.tables.getTable( i )
													.toString( )
											+ ( select.tables.getTable( i )
													.getAliasClause( ) != null ? ( " " + select.tables.getTable( i )
													.getAliasClause( )
													.toString( ) )
													: "" )
											+ "\r\n" );
								}
							}
						}
						// check column in function arguments
						int argCount = 0;
						if ( func.getArgs( ) != null )
						{
							for ( int k = 0; k < func.getArgs( ).size( ); k++ )
							{
								TExpression expr = func.getArgs( )
										.getExpression( k );
								if ( expr.toString( ).trim( ).equals( "*" ) )
									continue;
								List<TColumn> columns1 = exprToColumn( expr,
										select,
										level + 1 );
								for ( TColumn column1 : columns1 )
								{
									findColumnInTables( column1,
											select,
											level + 1,
											null,
											null );
									findColumnsFromClauses( select, level + 2 );
								}
								argCount++;
							}
						}

						if ( argCount == 0 )
						{

							Point point = new Point( (int) func.getEndToken( ).lineNo,
									(int) func.getEndToken( ).columnNo );
							if ( func.getArgs( ) != null
									&& func.getArgs( ).size( ) > 0 )
							{
								for ( int k = 0; k < func.getArgs( ).size( ); k++ )
								{
									TExpression expr = func.getArgs( )
											.getExpression( k );
									if ( expr.toString( ).trim( ).equals( "*" ) )
									{
										point = new Point( (int) expr.getStartToken( ).lineNo,
												(int) expr.getStartToken( ).columnNo );
										break;
									}
								}
							}
							if ( dependMap.containsKey( currentSource ) )
							{

								if ( currentClauseMap.containsKey( select ) )
								{
									dependMap.get( currentSource )
											.add( new TResultEntry( select.tables.getTable( 0 ),
													"*",
													(ClauseType) currentClauseMap.get( select ),
													point ) );
								}
								else if ( select instanceof TSelectSqlStatement )
								{
									dependMap.get( currentSource )
											.add( new TResultEntry( select.tables.getTable( 0 ),
													"*",
													ClauseType.select,
													point ) );
								}
								else
								{
									dependMap.get( currentSource )
											.add( new TResultEntry( select.tables.getTable( 0 ),
													"*",
													ClauseType.undefine,
													point ) );
								}
							}
						}

						if ( func.getAnalyticFunction( ) != null )
						{
							TParseTreeNodeList list = func.getAnalyticFunction( )
									.getPartitionBy_ExprList( );
							findColumnsFromList( select, level + 1, list );

							if ( func.getAnalyticFunction( ).getOrderBy( ) != null )
							{
								list = func.getAnalyticFunction( )
										.getOrderBy( )
										.getItems( );
								findColumnsFromList( select, level + 1, list );
							}
						}

						findColumnsFromClauses( select, level + 2 );

					}
				}
				break;
		}

		return ret;
	}

	private void getSelectSqlStatements( TSelectSqlStatement select,
			List<TSelectSqlStatement> stmtList )
	{
		if ( select.getSetOperator( ) != TSelectSqlStatement.setOperator_none )
		{
			getSelectSqlStatements( select.getLeftStmt( ), stmtList );
			getSelectSqlStatements( select.getRightStmt( ), stmtList );
		}
		else
		{
			stmtList.add( select );
		}
	}

	private Table TLzTaleToTable( TTable lztable )
	{
		Table table = new Table( );
		if ( lztable.getTableName( ) != null )
		{
			table.tableName = lztable.getName( );
			if ( lztable.getTableName( ).toString( ).indexOf( "." ) > 0 )
			{
				table.prefixName = lztable.getTableName( )
						.toString( )
						.substring( 0, lztable.getFullName( ).indexOf( '.' ) );
			}
		}

		if ( lztable.getAliasClause( ) != null )
		{
			table.tableAlias = lztable.getAliasClause( ).toString( );
		}
		return table;
	}

}
