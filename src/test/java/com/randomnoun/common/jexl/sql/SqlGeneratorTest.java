package com.randomnoun.common.jexl.sql;

import static org.junit.Assert.*;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.randomnoun.common.jexl.ast.TopLevelExpression;
import com.randomnoun.common.jexl.eval.EvalContext;
import com.randomnoun.common.jexl.eval.EvalFunction;
import com.randomnoun.common.jexl.parser.ExpressionParser;
import com.randomnoun.common.jexl.parser.ParseException;
import com.randomnoun.common.jexl.parser.TokenMgrError;

public class SqlGeneratorTest {

	@Before
	public void setUp() throws Exception {
	}

	
	/** Return an EvalContext containing the column and function definitions required
	    *  when converting a TopLevelExpression object into SQL
	    *
	    * @return the requested EvalContext
	    */
	   private EvalContext getSqlGeneratorContext()
	   {
	       // set up variables
	       EvalContext evalContext = new EvalContext();
	       evalContext.setVariable(SqlGenerator.VAR_DATABASE_TYPE, "MySQL");

	       // set up functions
	       Map<String, EvalFunction> functions = new HashMap<String, EvalFunction>();
	       evalContext.setFunctions(functions);

	       return evalContext;
	   }
	   
	   /** Convert Java expression String to a TopLevelExpression */
	    public TopLevelExpression stringToExpression(String expressionString)
	        throws java.text.ParseException
	    {
	        StringReader reader = new StringReader(expressionString);
	        ExpressionParser parser = new ExpressionParser(reader);
	        TopLevelExpression root = null;

	        try {
	        	root = parser.TopLevelExpression();
	        } catch (ParseException pe) {
	            throw new java.text.ParseException(pe.getMessage(), -1);
	        } catch (TokenMgrError tme) {
	            throw new java.text.ParseException(tme.getMessage(), -1);
	        }

	        return root;
	    }
	    
	    
	   
   /** Convert TopLevelExpression to SQL WHERE clause against resource tables
    *
    * @param expression An expression to convert
    * @param paramsList a container for positional parameters to be used in the generated query
    * @param evalContextMap a map containing runtime values used by functions (e.g. 'nextWorkingDay'), or user-supplied data
    *
    * @throws NullPointerException a null resource was passed to this method
    * @throws IllegalArgumentException an invalid resource was passed to this method
    **/
    public String expressionToSql(TopLevelExpression expression,
        List<String> paramsList, Map<String, Object> evalContextMap)
    {
        // set up evalContext for this resource
        EvalContext evalContext = getSqlGeneratorContext();

        // pass all supplied dates into eval Context
        if (evalContextMap != null) {
	 		for (Iterator<Map.Entry<String, Object>> i = evalContextMap.entrySet().iterator(); i.hasNext(); ) {
 				Map.Entry<String, Object> entry = i.next(); 
				String key = entry.getKey();
				evalContext.setVariable(key, entry.getValue());
			}
        }

        SqlGenerator sqlGenerator = new SqlGenerator();
        String sqlText = (String) sqlGenerator.visit(expression, evalContext);
        @SuppressWarnings("unchecked")
        List<String> params = (List<String>) evalContext.getVariable(SqlGenerator.VAR_PARAMETERS);
        if (params != null && paramsList != null) {
            paramsList.addAll(params);
        }

        return sqlText;
   }
	
	@Test
	public void test() throws java.text.ParseException {
		// some sql generator tests
		Map<String, Object> vars = new HashMap<>(); // input vars
		vars.put("a", "something");
		vars.put("id", new SqlColumn("lngId", "SOMETABLE", SqlColumn.NUMERIC));
		vars.put("val", new SqlColumn("lngVal", "SOMETABLE", SqlColumn.NUMERIC));
		vars.put("textVal", new SqlColumn("txtVal", "SOMETABLE", SqlColumn.VARCHAR));
		
		vars.put("userId", new PositionalParameter("userId"));
		
		List<String> paramsList = new ArrayList<>(); // output params 
		String result = expressionToSql(stringToExpression("a == 1"), paramsList, vars);
		assertEquals("('something' = 1)", result);
		System.out.println(result);
		
		result = expressionToSql(stringToExpression("id == 1"), paramsList, vars);
		System.out.println(result);
		assertEquals("(SOMETABLE.lngId = 1)", result);
		
		result = expressionToSql(stringToExpression("id == 1 && val > 2"), paramsList, vars);
		System.out.println(result);
		assertEquals("((SOMETABLE.lngId = 1) AND (SOMETABLE.lngVal > 2))", result);
		
		result = expressionToSql(stringToExpression("(id == 1 && val > 2) || textVal != \"USD\""), paramsList, vars);
		System.out.println(result);
		assertEquals("(((SOMETABLE.lngId = 1) AND (SOMETABLE.lngVal > 2)) OR (SOMETABLE.txtVal <> 'USD'))", result);
		
		result = expressionToSql(stringToExpression("id == userId"), paramsList, vars);
		System.out.println(result);
		assertEquals("(SOMETABLE.lngId = ?)", result);
		
		// @TODO some function tests
	}

}
