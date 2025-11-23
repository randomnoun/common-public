package com.randomnoun.common.jexl.sql;

/* (c) 2013 randomnoun. All Rights Reserved. This work is licensed under a
 * BSD Simplified License. (http://www.randomnoun.com/bsd-simplified.html)
 */

import java.text.*;
import java.util.*;

import org.apache.log4j.*;

import com.randomnoun.common.Text;
import com.randomnoun.common.jexl.DateSpan;
import com.randomnoun.common.jexl.EvalFallbackException;
import com.randomnoun.common.jexl.ast.*;
import com.randomnoun.common.jexl.eval.*;


/**
 * This class traverses a TopLevelExpression object and translates the AST graph
 * into a text representation that forms the 'WHERE' clause of an SQL query.
 *
 * <p>Only a subset of expressions are supported by the SQL Generator; an
 * EvalException is thrown if an invalid expression is supplied.
 *
 * <p>The context passed into this class contains:
 * <ul>
 *   <li>internal parameters that effect how the SQL is generated (such as the database vendor)
 *   <li>evaluatable functions (run at translation time)
 *   <li>translatable functions (converted into SQL at translation time)
 *   <li>database columns used in the SQL
 *   <li>variables (which are substituted at <i>translation</i> time into the value
 *     of that variable)
 *   <li>positional parameters (which are substituted at <i>execution</i> time into
 *     JDBC positional parameters)
 * </ul>
 *
 * <h3>Variables</h3>
 *
 * It's probably worth going into the difference between the two different types of variables.
 * If I have a variable that I know the value of at translation time; e.g. the variable
 * <code>messageVar</code> is <code>MT500</code>, I can generate standalone SQL that uses this variable:
 *
 * <pre style="code">
 *   // [1] parse the expression into an AST
 *   String exprString = "externalMessageType == messageVar";
 *   ExpressionParser parser = new ExpressionParser(new StringReader(exprString));
 *   TopLevelExpression expr = parser.TopLevelExpression();
 *
 *   // [2] set up an evaluation context to define variables
 *   EvalContext context = new EvalContext();
 *
 *   // [3] set up database columns used in the query
 *   //     note the first 'externalMessageType' in this line refers to the name
 *   //     of the column in exprString, and the second 'externalMessageType' refers
 *   //     to the name of the column in the table we're querying. It's clearer
 *   //     if these are set to the same value, but there's no other reason they have to.
 *   context.setVariable("externalMessageType", new SqlColumn("externalMessageType"));
 *
 *   // [4] set up variables used in the query
 *   context.setVariable("messageVar", "MT500");
 *
 *   // [5] generate the SQL
 *   SqlGenerator generator = new SqlGenerator();
 *   String result = (String) generator.visit(expr, context);
 * </pre>
 *
 * <p>The result here being <code>externalMessageType = 'MT500'</code>.
 *
 * <p>If, however, I won't know <code>messageVar</code> until execution time, I can still
 * generate the SQL using a PositionalParameter object:
 *
 * <pre style="code">
 *   // parse the expression and setup context as per [1], [2] and [3] above
 *
 *   // [4] set up an evaluation context to define variables
 *   context.setVariable("messageVar", new PositionalParameter("messageVar"));
 *
 *   // [5] generate the SQL as above
 *   SqlGenerator generator = new SqlGenerator();
 *   String result = (String) generator.visit(expr, context);
 * </pre>
 *
 * The result here being <code>externalMessageType = ?</code> instead. The value of the
 * '<code>?</code>' in the result is supplied
 * by the program at execution time, using standard JDBC positional parameters. But, you're
 * probably thinking, how do I know <i>which</i> positional parameter to use? After all,
 * I could have had more than one PositionalParameter's in the context, and they may
 * appear in any order or any number of times in the result String. To determine how to
 * supply these values to JDBC, the context variable VAR_PARAMETERS is set to a List of
 * Strings, each being the name of the positional parameter above. In the example above,
 * if we then executed
 *
 * <pre style="code">
 *   List parameList = (List) context.getVariable(SqlGenerator.VAR_PARAMETERS);
 * </pre>
 *
 * this will set paramList to a one-element list, containing the String "messageVar"
 * (corresponding to the single '<code>?</code>' in the result String). We can
 * iterate over this list to create the actual parameters to pass into JDBC when running
 * the query.
 *
 * <h3>Static dates</h3>
 * If a date is inserted into an SQL query, we need to know which database we're running
 * on, since each vendor seems to have their own way of representing fixed dates. This is
 * set using the VAR_DATABASE_TYPE variable in the ExpressionContext, which is set to
 * either DATABASE_ORACLE, DATABASE_DB2, DATABASE_SQLSERVER or DATABASE_MYSQL.
 *
 * <h3>Evaluatable functions</h3>
 * Any functions which implement EvalFunction are evaluated at translation time, (i.e.
 * are not translated into SQL). This is useful for things like nextWorkingDay(), which
 * need to execute before the SQL is passed through to the database.
 *
 * <h3>Translatable functions</h3>
 * Any functions which implement SqlFunction are translated into SQL and evaluated by
 * the database. This is useful for things like between() and like(), which are
 * converted into relational operators (&lt; and &gt;) and LIKE operators.
 *
 * <p>If a function implements both EvalFunction and SqlFunction, then this class
 * will always attempt to translate the function into SQL using the SqlFunction interface.
 *
 * <h3>Database columns</h3>
 * If a column is to be referenced from within the SQL, it must be set up in the EvalContext
 * as well (as per the 'externalMessageType' lines in the previous example).
 *
 * <h4>Column types</h4>
 * If not specified, the type of the column is VARCHAR, although this can be set
 * in the constructor:
 *
 * <pre style="code">
 *   SqlColumn arrivalDate = new SqlColumn("arrivalDate", SqlColumn.DATE);
 * </pre>
 *
 * <p>(All java.sql.Types.* constants are supported here).
 *
 * <h4>Column tables</h4>
 * <p>If our resultset JOINs many tables, and we wish to retrieve a value from a specific
 * table, that can also be placed in the SqlColumn constructor.
 *
 * <pre style="code">
 *   // e.g. SELECT tableA.id, tableB.id FROM tableA, tableB WHERE tableA.x = tableB.x
 *   SqlColumn tableA_id = new SqlColumn("id", "tableA");
 *   SqlColumn tableB_id = new SqlColumn("id", "tableB");
 * </pre>
 *
 * <!-- 
 * <h4>Money values</h4>
 * <p>If we wish to do monetary comparisons and the 'money value' spans two columns (currency and
 * amount), we can use the custom 'MONEYVALUE' type:
 *
 * <pre style="code">
 *   SqlColumn moneyColumn = new SqlColumn("amount", SqlColumn.MONEYVALUE);
 *   moneyColumn.setCurrencyCodeName("currency");
 * </pre>
 *
 * <p>this sets up a column which retrieves it's amount from the 'amount' value, and
 * the currency the 'currency' column of the table. MONEYVALUE columns can only be
 * compared against other MONEYVALUE columns or MoneyValue objects, e.g.:
 *
 * <pre style="code">
 *    String exprString = "amount &gt; toMoneyValue('USD', 10000)";
 *    ...
 *    context.setFunction("toMoneyValue", new EvalFunction.MoneyValueFunction() );
 *    context.setVariable("amount", moneyColumn);
 * </pre
 *
 * which will generate SQL of the form <code>currency = "USD" AND amount &gt; 10000</code>.
 * (MoneyValueFunction is an EvalFunction which returns a MoneyValue object).
 * -->
 *
 * <h4>FixedDate values</h4>
 * <p>Another custom data type is the 'fixed date', which is a date represented as a
 * string, in YYYYMMDD format. It is treated internally as identical to a VARCHAR
 * column, but allows operations on these columns to be aware that the information
 * being stored in it is a date.
 *
 * <p>Other references:
 * <ul>
 *   <li>DB2 date syntax: http://www-106.ibm.com/developerworks/db2/library/techarticle/0211yip/0211yip3.html
 *   <li>SQL date syntax: http://msdn.microsoft.com/library/en-us/tsqlref/ts_ca-co_2f3o.asp
 *   <li>Oracle date syntax: http://www-db.stanford.edu/~ullman/fcdb/oracle/or-time.html
 * </ul>
 *
 * 
 * @author knoxg
 */
public class SqlGenerator
    extends Evaluator {

    /** Internal variable to contain database type.
     *  (internal variables start with "." since they are not valid identifiers,
     *  and therefore cannot be referenced from within Expressions). */
    public static final String VAR_DATABASE_TYPE = ".databaseType";

    /** Internal variable used to contain positional parameter names. */
    public static final String VAR_PARAMETERS = ".parameters";
    
    /** Internal variable to configure empty string comparison handling.
     * 
     * When set to Boolean.TRUE, a comparison with the String constant "" will also 
     * include NULL values. See {{@link #comparisonOpToSql(String, EvalContext, Object, Object)}
     */
    public static final String VAR_EMPTY_STRING_COMPARISON_INCLUDES_NULL = ".emptyStringComparisonIncludesNull";
    

    /** Oracle VAR_DATABASE_TYPE value */
    public static final String DATABASE_ORACLE = "Oracle";

    /** DB2 VAR_DATABASE_TYPE value */
    public static final String DATABASE_DB2 = "DB2";

    /** SQL Server VAR_DATABASE_TYPE value */
    public static final String DATABASE_SQLSERVER = "SqlServer";

	/** MySQL VAR_DATABASE_TYPE value */
	public static final String DATABASE_MYSQL = "MySQL";

	/** Jet (MSAccess) VAR_DATABASE_TYPE value */
	public static final String DATABASE_JET = "JET";

	
    // these are non-printable to reduce the chance of conflicts within strings.

    /** Used to internally delimit positional paramaeters in generated SQL */
    private static final String POS_MARKER_LEFT = "\uF0000";

    /** Used to internally delimit positional paramaeters in generated SQL */
    private static final String POS_MARKER_RIGHT = "\uF0001";

    /** Logger instance for this class */
    Logger logger = Logger.getLogger(SqlGenerator.class);

    /** Generates code for comparisons of MoneyValue and Date types. Other data types
     * fall back to "(" + lhs + " " + sqlOp + " " + rhs + ")".
     *
     * @param sqlOp The operation we wish to insert into the SQL.
     *   (NB: this is *NOT* the operation in the .javacc syntax; e.g.
     *    use "=" as an sqlOp, not "==" ).
     * @param newSqlOp A modified SQL operator to use when dealing with dates, in
     *   certain conditions. See the wiki for details.
     * @param lhs The left hand side of the operation
     * @param rhs The right hand side of the operation
     * @return The SQL for this comparison
     */
    public static SqlText comparisonOpToSql(String sqlOp, EvalContext evalContext, Object lhs, Object rhs) {
        // Date date = null;
        // boolean startOfDay = false;

        SqlColumn sqlColumn = (lhs instanceof SqlColumn) ? (SqlColumn) lhs : null;
        int dataType = (sqlColumn == null) ? -1 : sqlColumn.getDataType();
        DateSpan dateSpan;

        // Expression formats
        // DateFormat isoDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        // DateFormat isoDateTimeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

        if (rhs instanceof Date) {
            throw new EvalException("Date objects not supported (use DateSpan instead)");
        }

        // manipulate the conditional operator        
        switch (dataType) {
            case -1:
                // lhs is not an SqlColumn; treat normally
                break;

            /*
            case SqlColumn.MONEYVALUE:
                if (!(rhs instanceof MoneyValue)) {
                    throw new EvalException("The lhs of this comparison is a MONEYVALUE SqlColumn; " + 
                      "rhs can only be a MoneyValue literal (found " + rhs.getClass().getName() + "')");
                }
                MoneyValue money = (MoneyValue) rhs;
                return new SqlText("((" + sqlColumn.getFullCurrencyCodeName() + " = '" + 
                  money.getCurrency().getCurrencyCode() + "') AND (" + 
                  sqlColumn.getFullName() + " " + sqlOp + " " + money.getAmount() + "))");
			*/
                
            case SqlColumn.TIMEVALUE: // used to represent ConditionTO.DATATYPE_DATETIME columns
                if (!(rhs instanceof DateSpan)) {
                    throw new EvalException("The lhs of this comparison is a TIMEVALUE SqlColumn; " + 
                      "rhs can only be a DateSpan literal (found " + rhs.getClass().getName() + "')");
                }
                dateSpan = (DateSpan) rhs;
                if (dateSpan.isDay()) {
                    if (sqlOp.equals("=")) {
                        return new SqlText("(" + toSql(evalContext, lhs) + " >= " + toSql(evalContext, dateSpan.getStartAsDate()) + 
                          " AND " + toSql(evalContext, lhs) + " <= " + toSql(evalContext, dateSpan.getEndAsDate()) + ")");
                    } else if (sqlOp.equals("<>")) {
                        return new SqlText("(" + toSql(evalContext, lhs) + " < " + toSql(evalContext, dateSpan.getStartAsDate()) + 
                          " OR " + toSql(evalContext, lhs) + " > " + toSql(evalContext, dateSpan.getEndAsDate()) + ")");
                    } else if (sqlOp.equals(">")) {
                        rhs = dateSpan.getEndAsDate();
                    } else if (sqlOp.equals(">=")) {
                        rhs = dateSpan.getStartAsDate();
                    } else if (sqlOp.equals("<")) {
                        rhs = dateSpan.getStartAsDate();
                    } else if (sqlOp.equals("<=")) {
                        rhs = dateSpan.getEndAsDate();
                    } else {
                        throw new EvalException("Unknown SQL operator '" + sqlOp + "'");
                    }
                } else {
                    rhs = dateSpan.getStartAsDate();
                }
                break;

            case SqlColumn.FIXEDDATEVALUE: // used to represent ConditionTO.DATATYPE_FIXEDDATE columns
                if (!(rhs instanceof DateSpan)) {
                    throw new EvalException("The lhs of this comparison is a FIXEDDATEVALUE SqlColumn; " + "can only compare against a DateSpan literal");
                }
                dateSpan = (DateSpan) rhs;
                if (!dateSpan.isDay()) {
                    throw new EvalException("The lhs of this comparison is a FIXEDDATEVALUE SqlColumn; " + "DateSpan cannot contain time components");
                }

                // just do a string comparison
                rhs = dateSpan.getStartAsYyyymmdd();

                break;
            default:
            // any other data types are compared normally, as below.
        }

        if (rhs instanceof SqlColumn) {
            // query builder doesn't allow these types of expressions, and throwing an exception just keeps the code simpler.
            throw new EvalException("rhs SqlColumn not implemented");
        }

        boolean emptyStringIncludesNull = Boolean.TRUE.equals(evalContext.getVariable(VAR_EMPTY_STRING_COMPARISON_INCLUDES_NULL)); 
        
        // default action: just compare the lhs to the rhs
        SqlText result = new SqlText("(" + toSql(evalContext, lhs) + " " + sqlOp + " " + toSql(evalContext, rhs) + ")");

        if (emptyStringIncludesNull) {
            // empty/null checks against strings
        	if ("=".equals(sqlOp) && "".equals(rhs)) {
	            result = new SqlText("(" + result + " OR (" + toSql(evalContext, lhs) + " IS NULL))");
	        } else if ("<>".equals(sqlOp) && "".equals(rhs)) {
	            result = new SqlText("(" + result + " OR (NOT " + toSql(evalContext, lhs) + " IS NULL))");
	        } else if ("<>".equals(sqlOp) && !"".equals(rhs)) {
	            result = new SqlText("(" + result + " OR (" + toSql(evalContext, lhs) + " IS NULL))");
	        } 
        }

        return result;
    }

    /**
     * Return the database type from this context
     *
     * @param evalContext The context passed to the .visit() method of this class
     * @return a DATABASE_* constant
     *
     * @throws EvalException if database type is not set, or is invalid
     */
    public static String getDatabaseType(EvalContext evalContext) {
        String databaseType = (String) evalContext.getVariable(VAR_DATABASE_TYPE);

        if (databaseType == null) {
            throw new EvalException("Database type must be set to translate this expression");
        }

        if (databaseType.equals(DATABASE_DB2) || 
          databaseType.equals(DATABASE_ORACLE) || 
          databaseType.equals(DATABASE_SQLSERVER) ||
          databaseType.equals(DATABASE_JET) ||
          databaseType.equals(DATABASE_MYSQL)) {
            return databaseType;
        }

        throw new EvalException("Unknown database type '" + databaseType + "'");
    }


    /** Convert an evaluated node object into it's SQL representation.
     *  The code must have evaluated to a literal String or Number by the
     *  time this method is called, or an EvalException is thrown.
     *
     * @param obj The object to convert to SQL
     * @return The SQL representation of this object
     */
    public static SqlText toSql(EvalContext evalContext, Object obj) {
        if (obj == null) {
            // ?
            throw new EvalException("Null values not supported");

        } else if (obj instanceof SqlColumn) {
            return new SqlText(((SqlColumn) obj).getFullName());

        } else if (obj instanceof SqlText) {
            return (SqlText) obj;

        } else if (obj instanceof String) {
            return new SqlText(escapeStringLiteral(evalContext, (String) obj));

        } else if (obj instanceof Number) {
            // NB: this will probably fail when processing E+ exponent notation 
            // in numbers, even though it is actually allowed in expression.jj.
            // This shouldn't occur in practice, however. 
            return new SqlText(obj.toString());

        } else if (obj instanceof Date) {
            String databaseType = getDatabaseType(evalContext);
            Date date;
            SimpleDateFormat sdf;

            if (databaseType.equals(DATABASE_ORACLE)) {
                date = (Date) obj;
                // @TODO (low priority) we may need to set timezone for sdf below to the database timezone, but we'll just assume the server tz = database tz
                sdf = new SimpleDateFormat("yyyy/MM/dd:HH:mm:ss");
                return new SqlText("TO_DATE('" + sdf.format(date) + "', 'yyyy/mm/dd:HH24:mi:ss')");

            } else if (databaseType.equals(DATABASE_DB2)) {
                date = (Date) obj;
                sdf = new SimpleDateFormat("yyyy-MM-dd-HH.mm.ss");
                return new SqlText("TIMESTAMP('" + sdf.format(date) + "')");

            } else if (databaseType.equals(DATABASE_SQLSERVER)) {
                date = (Date) obj;
                sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                return new SqlText("CONVERT(datetime, '" + sdf.format(date) + "', 20)");

            } else if (databaseType.equals(DATABASE_MYSQL)) {
                date = (Date) obj;
                sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                return new SqlText("'" + sdf.format(date) + "'"); // weird

            } else if (databaseType.equals(DATABASE_JET)) {
                date = (Date) obj;
                sdf = new SimpleDateFormat("#dd/MM/yyyy HH:mm:ss#"); // american date ordering in JET SQL. ODBC might reverse this though. let's see.
                return new SqlText("'" + sdf.format(date) + "'"); // weird

                
            } else {
                throw new EvalException("Do not know how to express date constants in database type '" + databaseType + "'");
            }

        } else if (obj instanceof Boolean) {
            // ?
            throw new EvalException("Boolean values not supported");

        } else {
            throw new EvalException("Cannot convert class '" + obj.getClass().getName() + "' to SQL");
        }
    }

    /**
     * Escape a string literal. (in oracle, this surrounds a value with quotes and
     * replaces quotes with doubled-up quote characters).
     *
     * @param evalContext
     * @param string
     * @return
     */
    public static String escapeStringLiteral(EvalContext evalContext, String string) {
        String databaseType = getDatabaseType(evalContext);

        if (databaseType.equals(DATABASE_ORACLE)) {
            string = "'" + Text.replaceString(string, "'", "''") + "'";
        } else if (databaseType.equals(DATABASE_DB2)) {
            string = "'" + Text.replaceString(string, "'", "''") + "'";
        } else if (databaseType.equals(DATABASE_SQLSERVER)) {
            string = "'" + Text.replaceString(string, "'", "''") + "'";
        } else if (databaseType.equals(DATABASE_MYSQL)) {
        	// mysql string escaping rules: https://dev.mysql.com/doc/refman/8.0/en/string-literals.html
        	
        	// we're not going to escape backslashes in here as the user may have intended to already escape
        	// some characters in LIKE expressions; we don't want to convert "\% starts with a percentage sign"
        	// to "\\% starts with a percentage sign". 
        	// That also means the user is responsible for converting newlines to "\n" etc
        	// (nb: you can actually have newlines in string literals in mysql)
        	
        	// also note \% is treated by mysql as "\%" outside of a like expression
        	// and       \_ is treated by mysql as "\_" outside of a like expression
        	// but most other escapes, and unknown escapes are converted to single characters; e.g. 
        	//           \x is treated by mysql as "x" outside of a like expression
        	// but again this is up to the user to grok.
        	
        	// if I start using this class across databases again may want to come up with some 
        	// crossvendor escaping rules which will have a myriad of tiny bugs in it 
        	
            string = "'" + Text.replaceString(string, "'", "\\'") + "'";
        } else if (databaseType.equals(DATABASE_JET)) {
            string = "'" + Text.replaceString(string, "'", "\\'") + "'";

        } else {
            throw new EvalException("Do not know how to escape string literals in database type '" + databaseType + "'");
        }

        return string;
    }

    /**
     * Escape the text used in a LIKE function, so that % and _ characters have no
     * special meaning. Note that an escaped like literal still needs to be passed
     * through escapeStringLiteral in order to surround it with quotes/escape quotes, etc...
     *
     * <p>(This isn't done in this method because we want to be able to modify the LIKE
     * value that the user enters e.g. for the startsWith() function)
     *
     * @param evalContext The evaluation context (containing the database type)
     * @param string The string to be escaped

     * @return The escaped string
     */
    public static String escapeLikeLiteral(EvalContext evalContext, String string) {
        String databaseType = getDatabaseType(evalContext);

		// only the Oracle rules below have been verified; should probably check
		// the rest
        if (databaseType.equals(DATABASE_ORACLE)) {
            string = Text.replaceString(string, "%", "\\%");  // % becomes \%
            string = Text.replaceString(string, "_", "\\_");  // _ becomes \_
        } else if (databaseType.equals(DATABASE_SQLSERVER)) {
			string = Text.replaceString(string, "%", "\\%");  // % becomes \%
			string = Text.replaceString(string, "_", "\\_");  // _ becomes \_
        } else if (databaseType.equals(DATABASE_DB2)) {
			string = Text.replaceString(string, "%", "\\%");  // % becomes \%
			string = Text.replaceString(string, "_", "\\_");  // _ becomes \_
        } else if (databaseType.equals(DATABASE_MYSQL)) {
			string = Text.replaceString(string, "%", "\\%");  // % becomes \%
			string = Text.replaceString(string, "_", "\\_");  // _ becomes \_
        } else if (databaseType.equals(DATABASE_JET)) {
			string = Text.replaceString(string, "%", "\\%");  // % becomes \% . Possibly '*' in JET.
			string = Text.replaceString(string, "_", "\\_");  // _ becomes \_
			
        } else {
            throw new EvalException("Do not know how to escape string literals in database type '" + databaseType + "'");
        }

        return string;
    }

    /** Convert a TopLevelExpression node to it's SQL representation. The SQL is returned
     * as a String. If any positional parameters have been referenced inside the SQL,
     * then the context variable VAR_PARAMETERS is set, containing an ordered list of Strings,
     * containing the positional parameter names.
     *
     * <p>The PRE text in these javadocs correspond to the javacc expansion of
     * this node; e.g. in expression.jj, the rule for TopLevelExpression
     * is
     *
     * <pre>
     * void TopLevelExpression():
     * {}
     * {
     *   Expression() <EOF>
     * }
     * </pre>
     *
     * <p>jtb then creates two fields in the TopLevelExpression object, "expression" (containing
     * the Expression() node) and "nodeToken" (containing the EOF token). This is documented
     * in the form below (this is included for all jtb-generated visit methods):
     *
     * <p>See the class javadocs for detailed instructions on how to use this method.
     *
     * <PRE>
     * expression -> Expression()
     * nodeToken -> &lt;EOF&gt;
     * </PRE>
     */
    public Object visit(TopLevelExpression n, EvalContext context) {
        EvalContext evalContext = (EvalContext) context;
        String sql = n.expression.accept(this, context).toString();

        // replace {positionalNames} with '?' markers and remember what we've hit so far ... 
        int pos = sql.indexOf(POS_MARKER_LEFT);
        int pos2;
        List<String> paramList = new ArrayList<>();

        while (pos != -1) {
            pos2 = sql.indexOf(POS_MARKER_RIGHT, pos + POS_MARKER_LEFT.length());

            if (pos2 == -1) {
                throw new EvalException("Internal error (unclosed positional parameter)");
            }

            paramList.add(sql.substring(pos + POS_MARKER_LEFT.length(), pos2));
            sql = sql.substring(0, pos) + "?" + sql.substring(pos2 + POS_MARKER_RIGHT.length());
            pos = sql.indexOf(POS_MARKER_LEFT);
        }

        if (paramList.size() > 0) {
            evalContext.setVariable(VAR_PARAMETERS, paramList);
        } else {
            evalContext.unsetVariable(VAR_PARAMETERS);
        }

        return sql.toString();
    }

    /** Generate the SQL for an Expression node.
     *
     * <PRE>
     * conditionalAndExpression -> ConditionalAndExpression()
     * nodeListOptional -> ( "||" ConditionalAndExpression() )*
     * </PRE>
     */
    public Object visit(Expression n, EvalContext context) {
        NodeSequence seq;
        Object lhs;
        Object rhs;
        EvalContext evalContext = (EvalContext) context;

        lhs = n.conditionalAndExpression.accept(this, context);

        for (Enumeration<?> e = n.nodeListOptional.elements(); e.hasMoreElements();) {
            seq = (NodeSequence) e.nextElement();
            rhs = seq.elementAt(1).accept(this, context);
            lhs = new SqlText("(" + toSql(evalContext, lhs) + " OR " + toSql(evalContext, rhs) + ")");
        }

        return lhs;
    }

    /** Generate the SQL for a ConditionalAndExpression node.
     *
     * <PRE>
     * equalityExpression -> EqualityExpression()
     * nodeListOptional -> ( "&&" EqualityExpression() )*
     * </PRE>
     */
    public Object visit(ConditionalAndExpression n, EvalContext context) {
        NodeSequence seq;
        Object lhs;
        Object rhs;
        EvalContext evalContext = (EvalContext) context;

        lhs = n.equalityExpression.accept(this, context);

        for (Enumeration<?> e = n.nodeListOptional.elements(); e.hasMoreElements();) {
            seq = (NodeSequence) e.nextElement();
            rhs = seq.elementAt(1).accept(this, context);
            lhs = new SqlText("(" + toSql(evalContext, lhs) + " AND " + toSql(evalContext, rhs) + ")");
        }

        return lhs;
    }

    /** Generate the SQL for a EqualityExpression node.
     *
     * <PRE>
     * relationalExpression -> RelationalExpression()
     * nodeListOptional -> ( ( "==" | "!=" ) RelationalExpression() )*
     * </PRE>
     */
    public Object visit(EqualityExpression n, EvalContext context) {
        NodeSequence seq;
        Object lhs;
        Object rhs;
        String[] ops = { "==", "!=" };
        EvalContext evalContext = (EvalContext) context;

        lhs = n.relationalExpression.accept(this, context);

        for (Enumeration<?> e = n.nodeListOptional.elements(); e.hasMoreElements();) {
            seq = (NodeSequence) e.nextElement();
            rhs = seq.elementAt(1).accept(this, context);

            int which = ((NodeChoice) seq.elementAt(0)).which;

            switch (which) {
                // this requires a special-case on null rhs, but 
                // leaving this as it won't occur in my app at the moment. (I 
                // use the isNull() functions for this sort of thing)
                case 0:
                    lhs = comparisonOpToSql("=", evalContext, lhs, rhs);
                    break;
                case 1:
                    lhs = comparisonOpToSql("<>", evalContext, lhs, rhs);
                    break;
                default:
                    throw new EvalException("Internal error (EqualityExpression)");
            }
        }

        return lhs;
    }

    /** Generate the SQL for a RelationalExpression node.
     *
     * <PRE>
     * additiveExpression -> AdditiveExpression()
     * nodeListOptional -> ( ( "&lt;" | "&gt;" | "&lt;=" | "&gt;=" ) AdditiveExpression() )*
     * </PRE>
     */
    public Object visit(RelationalExpression n, EvalContext context) {
        NodeSequence seq;
        Object lhs;
        Object rhs;
        String[] ops = { "<", ">", "<=", ">=" };
        EvalContext evalContext = (EvalContext) context;

        lhs = n.additiveExpression.accept(this, context);

        for (Enumeration<?> e = n.nodeListOptional.elements(); e.hasMoreElements();) {
            seq = (NodeSequence) e.nextElement();
            rhs = seq.elementAt(1).accept(this, context);

            int which = ((NodeChoice) seq.elementAt(0)).which;

            // logger.debug("Running op '" + ops[which] + "' on lhs:" + lhs + ", rhs:" + rhs);
            // note comparisons ops here may be modified.
            switch (which) {
                case 0:
                    lhs = comparisonOpToSql("<", evalContext, lhs, rhs);
                    break;
                case 1:
                    lhs = comparisonOpToSql(">", evalContext, lhs, rhs);
                    break;
                case 2:
                    lhs = comparisonOpToSql("<=", evalContext, lhs, rhs);
                    break;
                case 3:
                    lhs = comparisonOpToSql(">=", evalContext, lhs, rhs);
                    break;
                default:
                    throw new EvalException("Internal error (RelationalExpression)");
            }
        }

        return lhs;
    }

    /** Generate the SQL for a AdditiveExpression node.
     *
     * <PRE>
     * multiplicativeExpression -> MultiplicativeExpression()
     * nodeListOptional -> ( ( "+" | "-" ) MultiplicativeExpression() )*
     * </PRE>
     */
    public Object visit(AdditiveExpression n, EvalContext context) {
        NodeSequence seq;
        Object lhs;
        Object rhs;
        String[] ops = { "+", "-" };
        EvalContext evalContext = (EvalContext) context;

        lhs = n.multiplicativeExpression.accept(this, context);

        for (Enumeration<?> e = n.nodeListOptional.elements(); e.hasMoreElements();) {
            seq = (NodeSequence) e.nextElement();
            rhs = seq.elementAt(1).accept(this, context);

            int which = ((NodeChoice) seq.elementAt(0)).which;

            // logger.debug("Running op '" + ops[which] + "' on lhs:" + lhs + ", rhs:" + rhs);
            switch (which) {
                case 0:
                    // NB: does not support String addition, only numeric addition
                    lhs = new SqlText("(" + toSql(evalContext, lhs) + " + " + toSql(evalContext, rhs) + ")");
                    break;
                case 1:
                    lhs = new SqlText("(" + toSql(evalContext, lhs) + " - " + toSql(evalContext, rhs) + ")");
                    break;
                default:
                    throw new EvalException("Internal error (AdditiveExpression)");
            }
        }

        return lhs;
    }

    /** Generate the SQL for a MultiplicativeExpression node.
     *
     * <PRE>
     * unaryExpression -> UnaryExpression()
     * nodeListOptional -> ( ( "*" | "/" | "%" ) UnaryExpression() )*
     * </PRE>
     */
    public Object visit(MultiplicativeExpression n, EvalContext context) {
        NodeSequence seq;
        Object lhs;
        Object rhs;
        String[] ops = { "*", "/", "%" };

        lhs = n.unaryExpression.accept(this, context);

        EvalContext evalContext = (EvalContext) context;

        for (Enumeration<?> e = n.nodeListOptional.elements(); e.hasMoreElements();) {
            seq = (NodeSequence) e.nextElement();
            rhs = seq.elementAt(1).accept(this, context);

            int which = ((NodeChoice) seq.elementAt(0)).which;

            // logger.debug("Running op '" + ops[which] + "' on lhs:" + lhs + ", rhs:" + rhs);
            switch (which) {
                case 0:
                    lhs = new SqlText("(" + toSql(evalContext, lhs) + " * " + toSql(evalContext, rhs) + ")");
                    break;
                case 1:
                    lhs = new SqlText("(" + toSql(evalContext, lhs) + " / " + toSql(evalContext, rhs) + ")");
                    break;
                case 2:
                    lhs = new SqlText("(" + toSql(evalContext, lhs) + " % " + toSql(evalContext, rhs) + ")");
                    break;
                default:
                    throw new EvalException("Internal error (MultiplicativeExpression)");
            }
        }

        return lhs;
    }

    /** Generate the SQL for a UnaryExpression node.
     *
     * <PRE>
     * nodeChoice -> ( "~" | "!" | "-" ) UnaryExpression()
     *       | PrimaryExpression()
     * </PRE>
     */
    public Object visit(UnaryExpression n, EvalContext context) {
        NodeSequence seq;
        Object lhs;
        EvalContext evalContext = (EvalContext) context;

        if (n.nodeChoice.which == 0) {
            seq = (NodeSequence) n.nodeChoice.choice;
            lhs = seq.elementAt(1).accept(this, context);

            int which = ((NodeChoice) seq.elementAt(0)).which;

            // String op = ((NodeToken) ((NodeChoice) nl.nodes.get(0)).choice).tokenImage;
            switch (which) {
                case 0:
                    // long rhs =  makeNumeric ( ((Node) nl.nodes.get(0)).accept(this, context) ); 
                    // return new Long(~rhs); 
                    return new SqlText("(~" + toSql(evalContext, lhs) + ")");
                case 1:
                    return new SqlText("(NOT " + toSql(evalContext, lhs) + ")");
                case 2:
                    return new SqlText("(- " + toSql(evalContext, lhs) + ")");
                default:
                    throw new EvalException("Internal error (UnaryExpression)");
            }
        } else {
            return n.nodeChoice.choice.accept(this, context);
        }
    }

    /** Generate the SQL for a PrimaryExpression node.
     *
     * <PRE>
     * nodeChoice -> FunctionCall()
     *       | Name()
     *       | Literal()
     *       | "(" Expression() ")"
     * </PRE>
     */
    public Object visit(PrimaryExpression n, EvalContext context) {
        NodeSequence seq;
        int which = n.nodeChoice.which;

        switch (which) {
            case 0:
            case 1:
            case 2:
                return n.nodeChoice.choice.accept(this, context);
            case 3:
                seq = (NodeSequence) n.nodeChoice.choice;
                Object obj = seq.elementAt(1).accept(this, context);

                return obj;
            default:
                throw new EvalException("Internal parser error (PrimaryExpression)");
        }
    }

    /** Generate the SQL for a Name node.
     *
     * <PRE>
     * nodeToken -> &lt;IDENTIFIER&gt;
     * nodeListOptional -> ( "." &lt;IDENTIFIER&gt; )*
     * </PRE>
     */
    public Object visit(Name n, EvalContext context) {
        EvalContext evalContext = (EvalContext) context;
        String varComponentName;
        String varBaseName = n.nodeToken.tokenImage;

        // logger.debug("Fetching var '" + varBaseName + "'");
        if (!evalContext.hasVariable(varBaseName)) {
            throw new EvalException("Unknown variable '" + varBaseName + "'");
        }

        Object value = evalContext.getVariable(varBaseName);
        NodeSequence seq;

        /*
           if (value == null) {
                logger.debug(" = null");
           } else {
                logger.debug(" = " + value.getClass().getName() + " with value '" + value + "'");
           }
         */
        for (Enumeration<?> e = n.nodeListOptional.elements(); e.hasMoreElements();) {
            seq = (NodeSequence) e.nextElement();
            varComponentName = ((NodeToken) seq.elementAt(1)).tokenImage;

            // logger.debug("Fetching component '" + varComponentName + "' from var '" + varBaseName + "'");
            if (evalContext.hasVariableComponent(value, varBaseName, varComponentName)) {
                throw new EvalException("Unknown variable component '" + varComponentName + "' in variable '" + varBaseName + "'");
            }

            value = evalContext.getVariableComponent(value, varBaseName, varComponentName);
            varBaseName = varBaseName + "." + varComponentName;
        }




        // insert positional parameters into the generated SQL as 
        //    {paramName}
        //  ... these will get substituted with '?' just before 
        // visit(TopLevelExpression) returns the result to the caller.
        if (value instanceof PositionalParameter) {
            PositionalParameter posParameter = (PositionalParameter) value;

            return new SqlText(POS_MARKER_LEFT + posParameter.getName() + POS_MARKER_RIGHT);
        }


        // otherwise just get the value itself (this will be converted into SqlText
        // by other visit() methods)
        return value;
    }

    /** Generate the SQL for a FunctionCall node.
     *
     * <PRE>
     * nodeToken -> &lt;IDENTIFIER&gt;
     * arguments -> Arguments()
     * </PRE>
     */
    public Object visit(FunctionCall n, EvalContext context) {
        List argumentList = (List) n.arguments.accept(this, context);
        String functionName = n.nodeToken.tokenImage;
        EvalContext evalContext = (EvalContext) context;
        Object function = evalContext.getFunction(functionName);
        Evaluator evaluator = new Evaluator();

        if (function == null) {
            throw new EvalException("Unknown function '" + functionName + "'");

        } else if (function instanceof SqlFunction) {
            SqlFunction sqlFunction = (SqlFunction) function;
            try {
                return new SqlText(sqlFunction.toSql(functionName, evalContext, argumentList));
            } catch (EvalFallbackException ee) {
                // I think this only occurs in replaced promptable values
                
                logger.info("evaluating fallback for '" + functionName + "'");
                argumentList = (List) evaluator.visit((Arguments) n.arguments, context);
                return ((EvalFunction) sqlFunction).evaluate(functionName, evalContext, argumentList);
            }

        } else if (function instanceof EvalFunction) {
            EvalFunction evalFunction = (EvalFunction) function;
            // evaluate the argument list (pass in the context to allow TZ_* references)
            argumentList = (List) evaluator.visit((Arguments) n.arguments, context);
            return evalFunction.evaluate(functionName, evalContext, argumentList);

        }

        throw new EvalException("Cannot translate or evaluate function '" + function + "'");
    }

    /** Generate the SQL for an Arguments node
     *
     * <PRE>
     * nodeToken -> "("
     * nodeOptional -> [ ArgumentList() ]
     * nodeToken1 -> ")"
     * </PRE>
     */
    public Object visit(Arguments n, EvalContext context) {
        if (n.nodeOptional.present()) {
            return n.nodeOptional.accept(this, context);
        } else {
            return new ArrayList<>(0);
        }
    }

    /** Generate the SQL for a ArgumentList node.
     *
     *  <p>(Since an ArgumentList is fed directly into the EvalFunction to generate SQL,
     *  we just return a List of arguments)
     *
     * <PRE>
     * expression -> Expression()
     * nodeListOptional -> ( "," Expression() )*
     * </PRE>
     */
    public Object visit(ArgumentList n, EvalContext context) {
        NodeSequence seq;
        List<Object> arguments = new ArrayList<>();

        arguments.add(n.expression.accept(this, context));

        for (Enumeration<?> e = n.nodeListOptional.elements(); e.hasMoreElements();) {
            seq = (NodeSequence) e.nextElement();
            arguments.add(seq.elementAt(1).accept(this, context));
        }

        return arguments;
    }

    /** Generate the SQL for a Literal node.
     *
     * <PRE>
     * nodeChoice -> &lt;INTEGER_LITERAL&gt;
     *       | &lt;FLOATING_POINT_LITERAL&gt;
     *       | &lt;CHARACTER_LITERAL&gt;
     *       | &lt;STRING_LITERAL&gt;
     *       | BooleanLiteral()
     *       | NullLiteral()
     * </PRE>
     *
     */
    public Object visit(Literal n, EvalContext context) {
        String token = null;

        if (n.nodeChoice.choice instanceof NodeToken) {
            token = ((NodeToken) n.nodeChoice.choice).tokenImage;
        }

        switch (n.nodeChoice.which) {
            case 0:
                return Long.valueOf(token);
            case 1:
                return Double.valueOf(token);
            case 2:
                return Character.valueOf(token.charAt(1));
            case 3:
                return Text.unescapeJava(token.substring(1, token.length() - 1));
        }

        // must be 'true', 'false', or 'null'
        return n.nodeChoice.accept(this, context);
    }

    /** Generate the SQL for a BooleanLiteral node.
     *
     * <PRE>
     * nodeChoice -> "true"
     *       | "false"
     * </PRE>
     */
    public Object visit(BooleanLiteral n, EvalContext context) {
        if (n.nodeChoice.which == 0) {
            return Boolean.valueOf(true);
        } else {
            return Boolean.valueOf(false);
        }
    }

    /** Generate the SQL for a NullLiteral node.
     *
     * <PRE>
     * nodeToken -> "null"
     * </PRE>
     */
    public Object visit(NullLiteral n, EvalContext context) {
        return null;
    }

    /** This is never executed (we do not evaluate tokens) */
    public Object visit(NodeToken n, EvalContext context) {
        return n.tokenImage;
    }
}
