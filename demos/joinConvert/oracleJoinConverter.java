
package demos.joinConvert;

/*
 * Date: 11-12-1
 */

import gudusoft.gsqlparser.EDbVendor;
import gudusoft.gsqlparser.EExpressionType;
import gudusoft.gsqlparser.ESqlStatementType;
import gudusoft.gsqlparser.TGSqlParser;
import gudusoft.gsqlparser.nodes.IExpressionVisitor;
import gudusoft.gsqlparser.nodes.TExpression;
import gudusoft.gsqlparser.nodes.TParseTreeNode;
import gudusoft.gsqlparser.nodes.TTable;
import gudusoft.gsqlparser.stmt.TSelectSqlStatement;

import java.util.ArrayList;

public class oracleJoinConverter
{

	enum jointype {
		inner, left, right
	};

	class FromClause
	{

		TTable joinTable;
		String joinClause;
		String condition;
	}

	class JoinCondition
	{

		public String lefttable, righttable, leftcolumn, rightcolumn;
		public jointype jt;
		public Boolean used;
		public TExpression lexpr, rexpr, expr;
	}

	class getJoinConditionVisitor implements IExpressionVisitor
	{

		Boolean isFirstExpr = true;
		ArrayList<JoinCondition> jrs = new ArrayList<JoinCondition>( );

		public ArrayList<JoinCondition> getJrs( )
		{
			return jrs;
		}

		boolean is_compare_condition( EExpressionType t )
		{
			return ( ( t == EExpressionType.simple_comparison_t )
					|| ( t == EExpressionType.group_comparison_t ) || ( t == EExpressionType.in_t ) );
		}

		private void analyzeJoinCondition( TExpression expr,
				TExpression parent_expr )
		{
			TExpression slexpr, srexpr, lc_expr = expr;

			if ( is_compare_condition( lc_expr.getExpressionType( ) ) )
			{
				slexpr = lc_expr.getLeftOperand( );
				srexpr = lc_expr.getRightOperand( );

				if ( slexpr.isOracleOuterJoin( ) || srexpr.isOracleOuterJoin( ) )
				{
					JoinCondition jr = new JoinCondition( );
					jr.used = false;
					jr.lexpr = slexpr;
					jr.rexpr = srexpr;
					jr.expr = expr;
					if ( slexpr.isOracleOuterJoin( ) )
					{
						// If the plus is on the left, the join type is right
						// out join.
						jr.jt = jointype.right;
						// remove (+)
						slexpr.getEndToken( ).setString( "" );
					}
					if ( srexpr.isOracleOuterJoin( ) )
					{
						// If the plus is on the right, the join type is left
						// out join.
						jr.jt = jointype.left;
						srexpr.getEndToken( ).setString( "" );
					}
					if ( ( slexpr.getExpressionType( ) == EExpressionType.simple_constant_t ) )
					{
						jr.lefttable = null;
						jr.righttable = getExpressionTable( srexpr );
					}
					else if ( srexpr.getExpressionType( ) == EExpressionType.simple_constant_t )
					{
						jr.righttable = null;
						jr.lefttable = getExpressionTable( slexpr );
					}
					else
					{
						jr.lefttable = getExpressionTable( slexpr );
						jr.righttable = getExpressionTable( srexpr );
					}
					jrs.add( jr );
					// System.out.printf( "join condition: %s\n", expr.toString(
					// ) );
				}
				else if ( ( slexpr.getExpressionType( ) == EExpressionType.simple_object_name_t )
						&& ( !slexpr.toString( ).startsWith( ":" ) )
						&& ( !slexpr.toString( ).startsWith( "?" ) )
						&& ( srexpr.getExpressionType( ) == EExpressionType.simple_object_name_t )
						&& ( !srexpr.toString( ).startsWith( ":" ) )
						&& ( !srexpr.toString( ).startsWith( "?" ) ) )
				{
					JoinCondition jr = new JoinCondition( );
					jr.used = false;
					jr.lexpr = slexpr;
					jr.rexpr = srexpr;
					jr.expr = expr;
					jr.jt = jointype.inner;
					jr.lefttable = getExpressionTable( slexpr );
					jr.righttable = getExpressionTable( srexpr );
					jrs.add( jr );
					// System.out.printf(
					// "join condition: %s, %s:%d, %s:%d, %s\n",
					// expr.toString( ),
					// slexpr.toString( ),
					// slexpr.getExpressionType( ),
					// srexpr.toString( ),
					// srexpr.getExpressionType( ),
					// srexpr.getObjectOperand( ).getObjectType( ) );
				}
				else
				{
					// not a join condition
				}

			}

		}

		public boolean exprVisit( TParseTreeNode pNode, boolean isLeafNode )
		{
			TExpression expr = (TExpression) pNode;
			// System.out.printf("expr visited: %s\n",expr.toString());
			analyzeJoinCondition( expr, expr );
			return true;

		}

	}

	private String ErrorMessage = "";

	public String getErrorMessage( )
	{
		return ErrorMessage;
	}

	private int ErrorNo;

	private String query;

	public oracleJoinConverter( String sql )
	{
		this.query = sql;
	}

	public String getQuery( )
	{
		// remove blank line from query
		return this.query.replaceAll( "(?m)^[ \t]*\r?\n", "" );
	}

	public int convert( )
	{

		TGSqlParser sqlparser = new TGSqlParser( EDbVendor.dbvoracle );
		sqlparser.sqltext = this.query;
		ErrorNo = sqlparser.parse( );
		if ( ErrorNo != 0 )
		{
			ErrorMessage = sqlparser.getErrormessage( );
			return ErrorNo;
		}
		if ( sqlparser.sqlstatements.get( 0 ).sqlstatementtype != ESqlStatementType.sstselect )
			return 0;
		TSelectSqlStatement select = (TSelectSqlStatement) sqlparser.sqlstatements.get( 0 );
		analyzeSelect( select );
		this.query = select.toString( );
		return ErrorNo;
	}

	private boolean isNameOfTable( TTable table, String name )
	{
		return ( name == null ) ? false : table.getName( )
				.equalsIgnoreCase( name );
	}

	private boolean isAliasOfTable( TTable table, String alias )
	{
		if ( table.getAliasClause( ) == null )
		{
			return false;
		}
		else
			return ( alias == null ) ? false : table.getAliasClause( )
					.toString( )
					.equalsIgnoreCase( alias );
	}

	private boolean isNameOrAliasOfTable( TTable table, String str )
	{
		return isAliasOfTable( table, str ) || isNameOfTable( table, str );
	}

	private boolean areTableJoined( TTable lefttable, TTable righttable,
			ArrayList<JoinCondition> jrs )
	{

		boolean ret = false;

		for ( int i = 0; i < jrs.size( ); i++ )
		{
			JoinCondition jc = jrs.get( i );
			if ( jc.used )
			{
				continue;
			}
			ret = isNameOrAliasOfTable( lefttable, jc.lefttable )
					&& isNameOrAliasOfTable( righttable, jc.righttable );
			if ( ret )
				break;
			ret = isNameOrAliasOfTable( lefttable, jc.righttable )
					&& isNameOrAliasOfTable( righttable, jc.lefttable );
			if ( ret )
				break;
		}

		return ret;
	}

	private String getJoinType( ArrayList<JoinCondition> jrs )
	{
		String str = "inner join";
		for ( int i = 0; i < jrs.size( ); i++ )
		{
			if ( jrs.get( i ).jt == jointype.left )
			{
				str = "left outer join";
				break;
			}
			else if ( jrs.get( i ).jt == jointype.right )
			{
				str = "right outer join";
				break;
			}
		}

		return str;
	}

	private ArrayList<JoinCondition> getJoinCondition( TTable lefttable,
			TTable righttable, ArrayList<JoinCondition> jrs )
	{
		ArrayList<JoinCondition> lcjrs = new ArrayList<JoinCondition>( );
		for ( int i = 0; i < jrs.size( ); i++ )
		{
			JoinCondition jc = jrs.get( i );
			if ( jc.used )
			{
				continue;
			}

			if ( isNameOrAliasOfTable( lefttable, jc.lefttable )
					&& isNameOrAliasOfTable( righttable, jc.righttable ) )
			{
				lcjrs.add( jc );
				jc.used = true;
			}
			else if ( isNameOrAliasOfTable( lefttable, jc.righttable )
					&& isNameOrAliasOfTable( righttable, jc.lefttable ) )
			{
				lcjrs.add( jc );
				jc.used = true;
			}
			else if ( ( jc.lefttable == null )
					&& ( isNameOrAliasOfTable( lefttable, jc.righttable ) || isNameOrAliasOfTable( righttable,
							jc.righttable ) ) )
			{
				// 'Y' = righttable.c1(+)
				lcjrs.add( jc );
				jc.used = true;
			}
			else if ( ( jc.righttable == null )
					&& ( isNameOrAliasOfTable( lefttable, jc.lefttable ) || isNameOrAliasOfTable( righttable,
							jc.lefttable ) ) )
			{
				// lefttable.c1(+) = 'Y'
				lcjrs.add( jc );
				jc.used = true;
			}
		}
		return lcjrs;
	}

	private void analyzeSelect( TSelectSqlStatement select )
	{
		if ( !select.isCombinedQuery( ) )
		{
			if ( select.tables.size( ) == 1 )
				return;

			if ( select.getWhereClause( ) == null )
			{
				if ( select.tables.size( ) > 1 )
				{
					// cross join
					String str = select.tables.getTable( 0 )
							.getFullNameWithAliasString( );
					for ( int i = 1; i < select.tables.size( ); i++ )
					{
						str = str
								+ "\ncross join "
								+ select.tables.getTable( i )
										.getFullNameWithAliasString( );
					}

					for ( int k = select.joins.size( ) - 1; k > 0; k-- )
					{
						select.joins.removeJoin( k );
					}
					select.joins.getJoin( 0 ).setString( str );
				}
			}
			else
			{

				getJoinConditionVisitor v = new getJoinConditionVisitor( );

				// get join conditions
				select.getWhereClause( ).getCondition( ).preOrderTraverse( v );
				ArrayList<JoinCondition> jrs = v.getJrs( );

				// Console.WriteLine(jrs.Count);
				boolean tableUsed[] = new boolean[select.tables.size( )];
				for ( int i = 0; i < select.tables.size( ); i++ )
				{
					tableUsed[i] = false;
				}

				// make first table to be the left most joined table
				String fromclause = select.tables.getTable( 0 )
						.getFullNameWithAliasString( );

				tableUsed[0] = true;
				boolean foundTableJoined;
				ArrayList<FromClause> fromClauses = new ArrayList<FromClause>( );
				for ( ;; )
				{
					foundTableJoined = false;

					for ( int i = 0; i < select.tables.size( ); i++ )
					{
						TTable lcTable1 = select.tables.getTable( i );

						TTable leftTable = null, rightTable = null;
						for ( int j = i + 1; j < select.tables.size( ); j++ )
						{
							TTable lcTable2 = select.tables.getTable( j );
							if ( areTableJoined( lcTable1, lcTable2, jrs ) )
							{
								if ( tableUsed[i] && ( !tableUsed[j] ) )
								{
									leftTable = lcTable1;
									rightTable = lcTable2;
								}
								else if ( ( !tableUsed[i] ) && tableUsed[j] )
								{
									leftTable = lcTable2;
									rightTable = lcTable1;
								}

								if ( ( leftTable != null )
										&& ( rightTable != null ) )
								{
									ArrayList<JoinCondition> lcjrs = getJoinCondition( leftTable,
											rightTable,
											jrs );
									FromClause fc = new FromClause( );
									fc.joinTable = rightTable;
									fc.joinClause = getJoinType( lcjrs );
									String condition = "";
									for ( int k = 0; k < lcjrs.size( ); k++ )
									{
										condition += lcjrs.get( k ).expr.toString( );
										if ( k != lcjrs.size( ) - 1 )
										{
											condition += " and ";
										}
										TExpression lc_expr = lcjrs.get( k ).expr;
										lc_expr.remove( );
									}
									fc.condition = condition;

									fromClauses.add( fc );
									tableUsed[i] = true;
									tableUsed[j] = true;

									foundTableJoined = true;
								}
							}
						}
					}

					if ( !foundTableJoined )
					{
						break;
					}
				}

				// are all join conditions used?
				for ( int i = 0; i < jrs.size( ); i++ )
				{
					JoinCondition jc = jrs.get( i );
					if ( !jc.used )
					{
						for ( int j = fromClauses.size( ) - 1; j >= 0; j-- )
						{
							if ( isNameOrAliasOfTable( fromClauses.get( j ).joinTable,
									jc.lefttable )
									|| isNameOrAliasOfTable( fromClauses.get( j ).joinTable,
											jc.righttable ) )
							{
								fromClauses.get( j ).condition += " and "
										+ jc.expr.toString( );
								jc.used = true;
								jc.expr.remove( );
								break;
							}
						}
					}
				}

				for ( int i = 0; i < select.tables.size( ); i++ )
				{
					if ( !tableUsed[i] )
					{
						ErrorNo++;
						ErrorMessage += String.format( "%sError %d, Message: %s",
								System.getProperty( "line.separator" ),
								ErrorNo,
								"This table has no join condition: "
										+ select.tables.getTable( i )
												.getFullName( ) );
					}
				}

				// link all join clause
				for ( int i = 0; i < fromClauses.size( ); i++ )
				{
					FromClause fc = fromClauses.get( i );
					// fromclause += System.getProperty("line.separator") +
					// fc.joinClause
					// +" "+fc.joinTable.getFullNameWithAliasString()+" on "+fc.condition;
					fromclause += "\n"
							+ fc.joinClause
							+ " "
							+ fc.joinTable.getFullNameWithAliasString( )
							+ " on "
							+ fc.condition;
				}

				for ( int k = select.joins.size( ) - 1; k > 0; k-- )
				{
					select.joins.removeJoin( k );
				}

				select.joins.getJoin( 0 ).setString( fromclause );

				if ( ( select.getWhereClause( ).getCondition( ).getStartToken( ) == null )
						|| ( select.getWhereClause( )
								.getCondition( )
								.toString( )
								.trim( )
								.length( ) == 0 ) )
				{
					// no where condition, remove WHERE keyword
					select.getWhereClause( ).setString( " " );

				}
			}
		}
		else
		{
			analyzeSelect( select.getLeftStmt( ) );
			analyzeSelect( select.getRightStmt( ) );
		}
	}

	private String getExpressionTable( TExpression expr )
	{
		if ( expr.getObjectOperand( ) != null )
			return expr.getObjectOperand( ).getObjectString( );
		else if ( expr.getLeftOperand( ) != null
				&& expr.getLeftOperand( ).getObjectOperand( ) != null )
			return expr.getLeftOperand( ).getObjectOperand( ).getObjectString( );
		else if ( expr.getRightOperand( ) != null
				&& expr.getRightOperand( ).getObjectOperand( ) != null )
			return expr.getRightOperand( )
					.getObjectOperand( )
					.getObjectString( );
		else
			return null;
	}

	public static void main( String args[] )
	{
		// String sqltext = "SELECT e.employee_id,\n" +
		// "       e.last_name,\n" +
		// "       e.department_id\n" +
		// "FROM   employees e,\n" +
		// "       departments d\n" ;

		String sqltext = "SELECT e.employee_id,\n"
				+ "       e.last_name,\n"
				+ "       e.department_id\n"
				+ "FROM   employees e,\n"
				+ "       departments d\n"
				+ "WHERE  e.department_id = d.department_id";

		sqltext = "SELECT m.*, \n"
				+ "       altname.last_name  last_name_student, \n"
				+ "       altname.first_name first_name_student, \n"
				+ "       ccu.date_joined, \n"
				+ "       ccu.last_login, \n"
				+ "       ccu.photo_id, \n"
				+ "       ccu.last_updated \n"
				+ "FROM   summit.mstr m, \n"
				+ "       summit.alt_name altname, \n"
				+ "       smmtccon.ccn_user ccu \n"
				+ "WHERE  m.id =?\n"
				+ "       AND m.id = altname.id(+) \n"
				+ "       AND m.id = ccu.id(+) \n"
				+ "       AND altname.grad_name_ind(+) = '*'";

		// sqltext = "SELECT * \n" +
		// "FROM   summit.mstr m, \n" +
		// "       summit.alt_name altname, \n" +
		// "       smmtccon.ccn_user ccu \n" +
		// //"       uhelp.deg_coll deg \n" +
		// "WHERE  m.id = ? \n" +
		// "       AND m.id = altname.id(+) \n" +
		// "       AND m.id = ccu.id(+) \n" +
		// "       AND 'N' = ccu.admin(+) \n" +
		// "       AND altname.grad_name_ind(+) = '*'";

		// sqltext = "SELECT ppp.project_name proj_name, \n" +
		// "       pr.role_title    user_role \n" +
		// "FROM   jboss_admin.portal_application_location pal, \n" +
		// "       jboss_admin.portal_application pa, \n" +
		// "       jboss_admin.portal_user_app_location_role pualr, \n" +
		// "       jboss_admin.portal_location pl, \n" +
		// "       jboss_admin.portal_role pr, \n" +
		// "       jboss_admin.portal_pep_project ppp, \n" +
		// "       jboss_admin.portal_user pu \n" +
		// "WHERE  (pal.application_location_id = pualr.application_location_id \n"
		// +
		// "         AND pu.jbp_uid = pualr.jbp_uid \n" +
		// "         AND pu.username = 'USERID') \n" +
		// "       AND pal.uidr_uid = pl.uidr_uid \n" +
		// "       AND pal.application_id = pa.application_id \n" +
		// "       AND pal.application_id = pr.application_id \n" +
		// "       AND pualr.role_id = pr.role_id \n" +
		// "       AND pualr.project_id = ppp.project_id \n" +
		// "       AND pa.application_id = 'APPID' ";

		sqltext = "SELECT * \n"
				+ "FROM   smmtccon.ccn_menu menu, \n"
				+ "       smmtccon.ccn_page paget \n"
				+ "WHERE  ( menu.page_id = paget.page_id(+) ) \n"
				+ "       AND ( NOT enabled = 'N' ) \n"
				+ "       AND ( ( :parent_menu_id IS NULL \n"
				+ "               AND menu.parent_menu_id IS NULL ) \n"
				+ "              OR ( menu.parent_menu_id = :parent_menu_id ) ) \n"
				+ "ORDER  BY item_seq;";

		sqltext = "select *\n"
				+ "from  ods_trf_pnb_stuf_lijst_adrsrt2 lst\n"
				+ "		, ods_stg_pnb_stuf_pers_adr pas\n"
				+ "		, ods_stg_pnb_stuf_pers_nat nat\n"
				+ "		, ods_stg_pnb_stuf_adr adr\n"
				+ "		, ods_stg_pnb_stuf_np prs\n"
				+ "where \n"
				+ "		pas.soort_adres = lst.soort_adres\n"
				+ "	and prs.id = nat.prs_id(+)\n"
				+ "	and adr.id = pas.adr_id\n"
				+ "	and prs.id = pas.prs_id\n"
				+ "  and lst.persoonssoort = 'PERSOON'\n"
				+ "  and pas.einddatumrelatie is null  ";

		sqltext = "select *\n"
				+ "		from  ods_trf_pnb_stuf_lijst_adrsrt2 lst\n"
				+ "				, ods_stg_pnb_stuf_np prs\n"
				+ "				, ods_stg_pnb_stuf_pers_adr pas\n"
				+ "				, ods_stg_pnb_stuf_pers_nat nat\n"
				+ "		 		, ods_stg_pnb_stuf_adr adr\n"
				+ "		 where \n"
				+ "				pas.soort_adres = lst.soort_adres\n"
				+ "			and prs.id(+) = nat.prs_id\n"
				+ "			and adr.id = pas.adr_id\n"
				+ "			and prs.id = pas.prs_id\n"
				+ "		 and lst.persoonssoort = 'PERSOON'\n"
				+ "		  and pas.einddatumrelatie is null";

		// sqltext = "SELECT ppp.project_name proj_name, \n"
		// + "       pr.role_title    user_role \n"
		// + "FROM   jboss_admin.portal_application_location pal, \n"
		// + "       jboss_admin.portal_application pa, \n"
		// + "       jboss_admin.portal_user_app_location_role pualr, \n"
		// + "       jboss_admin.portal_location pl, \n"
		// + "       jboss_admin.portal_role pr, \n"
		// + "       jboss_admin.portal_pep_project ppp, \n"
		// + "       jboss_admin.portal_user pu \n"
		// +
		// "WHERE  (pal.application_location_id = pualr.application_location_id \n"
		// + "         AND pu.jbp_uid = pualr.jbp_uid \n"
		// + "         AND pu.username = 'USERID') \n"
		// + "       AND pal.uidr_uid = pl.uidr_uid \n"
		// + "       AND pal.application_id = pa.application_id \n"
		// + "       AND pal.application_id = pr.application_id \n"
		// + "       AND pualr.role_id = pr.role_id \n"
		// + "       AND pualr.project_id = ppp.project_id \n"
		// + "       AND pa.application_id = 'APPID'";
		//
		// sqltext = "select *\n"
		// + "from  ods_trf_pnb_stuf_lijst_adrsrt2 lst\n"
		// + "		, ods_stg_pnb_stuf_np prs\n"
		// + "		, ods_stg_pnb_stuf_pers_adr pas\n"
		// + "		, ods_stg_pnb_stuf_pers_nat nat\n"
		// + "		, ods_stg_pnb_stuf_adr adr\n"
		// + "where \n"
		// + "		pas.soort_adres = lst.soort_adres\n"
		// + "	and prs.id = nat.prs_id(+)\n"
		// + "	and adr.id = pas.adr_id\n"
		// + "	and prs.id = pas.prs_id\n"
		// + "  and lst.persoonssoort = 'PERSOON'\n"
		// + "   and pas.einddatumrelatie is null";

		// sqltext = "select *\n"
		// + "from  ods_trf_pnb_stuf_lijst_adrsrt2 lst,\n"
		// + "       ods_stg_pnb_stuf_np prs,\n"
		// + "       ods_stg_pnb_stuf_pers_adr pas,\n"
		// + "       ods_stg_pnb_stuf_pers_nat nat,\n"
		// + "       ods_stg_pnb_stuf_adr adr\n"
		// + "where  pas.soort_adres = lst.soort_adres\n"
		// + "       and prs.id(+) = nat.prs_id\n"
		// + "       and adr.id = pas.adr_id\n"
		// + "       and prs.id = pas.prs_id\n"
		// + "       and lst.persoonssoort = 'PERSOON'\n"
		// + "       and pas.einddatumrelatie is null\n";

		sqltext = "SELECT e.employee_id,\n"
				+ "       e.last_name,\n"
				+ "       e.department_id\n"
				+ "FROM   employees e,\n"
				+ "       departments d\n"
				+ "WHERE  e.department_id = d.department_id(+)";

		sqltext = "SELECT e.employee_id,\n"
				+ "       e.last_name,\n"
				+ "       e.department_id\n"
				+ "FROM   employees e,\n"
				+ "       departments d\n"
				+ "WHERE  e.department_id(+) = d.department_id";

		System.out.println( "SQL with Oracle propriety joins" );
		System.out.println( sqltext );
		oracleJoinConverter converter = new oracleJoinConverter( sqltext );
		if ( converter.convert( ) != 0 )
		{
			System.out.println( converter.getErrorMessage( ) );
		}
		else
		{
			System.out.println( "\nSQL in ANSI joins" );
			System.out.println( converter.getQuery( ) );
		}
	}
}