package com.randomnoun.common.jexl.eval;

/* (c) 2013 randomnoun. All Rights Reserved. This work is licensed under a
 * BSD Simplified License. (http://www.randomnoun.com/bsd-simplified.html)
 */

import java.util.*;

import org.apache.log4j.Logger;


/**
 * This class encapsulates a function, which is intended to be placed in an
 * EvalContext (using EvalContext.setFunctions()). Any
 * expression evaluated in that context will then be able to invoke developer-defined
 * functions.
 *
 * <pre>
    Map&lt;String, EvalFunction&gt; functions = new HashMap();
    functions.put("length", new LengthFunction());
    functions.put("like", new LikeFunction());
    EvalContext evalContext = new EvalContext();
    evalContext.setFunctions(functions);
 * </pre>
 *
 * Some functions can also generate SQL, see
 * {@link com.randomnoun.common.jexl.sql.SqlFunction}.
 *
 * 
 * @author knoxg
 */
public interface EvalFunction
{
	/** Logger for this class */
	public static Logger logger = Logger.getLogger(EvalFunction.class);

    /**
     * This is the main entry point for classes that implement this interface.
     * The evaluate method performs the evaluation of the function that this class
     * implements (and is supplied the functionName, so that the same class may
     * evaluate multiple functions). 
     *
     * @param functionName The name of the function to be evaluated (can be disregarded
     *   if this class only performs a single function)
     * @param context The context in which the function is being executed (could hold
     *   things like local variable values, current line number, etc...)
     * @param arguments The arguments passed to this function
     *
     * @return The result of the evaluation
     *
     * @throws EvalException An exception occurred during evaluation
     */
    public Object evaluate(String functionName, EvalContext context, List<Object> arguments)
        throws EvalException;
}
