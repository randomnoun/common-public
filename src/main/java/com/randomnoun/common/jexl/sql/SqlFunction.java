package com.randomnoun.common.jexl.sql;

import java.util.*;

import com.randomnoun.common.jexl.EvalFallbackException;
import com.randomnoun.common.jexl.eval.*;

/**
 * A function which can be translated into SQL. These functions should be
 * set in an SqlGenerator's context to allow expressions to use those functions.
 *
 * <p>This class defines a number of 'standard' functions, which can be used;
 * they subclass the functions in EvalFunction of the same name, so that they can work
 * in both the Evaluator and SqlGenerator AST visitors.
 *
 * 
 * @author knoxg
 */
public interface SqlFunction {

	/** All SQL functions must implement this method. It returns the SQL required for
	 *  the database to evaluate the function. evalContext can be used to get the
	 *  database type, if required.
	 *
	 * @param functionName The name of the function being translated
	 * @param evalContext  The evaluation context of the SqlGenerator doing the translation
	 * @param arguments    The arguments to the function
	 *
	 * @return the SQL representation of the function
	 * @throws EvalException If an exception occured during translation (e.g. missing arguments, arguments of wrong type, invalid arguments).
	 * @throws EvalFallbackException If this expression should be evaluated at runtime instead of being translated  
	 */
	public abstract String toSql(String functionName, EvalContext evalContext, List<Object> arguments)
		throws EvalException, EvalFallbackException;
}
