package com.randomnoun.common.jexl.eval.function;

/* (c) 2013-2018 randomnoun. All Rights Reserved. This work is licensed under a
 * BSD Simplified License. (http://www.randomnoun.com/bsd-simplified.html)
 */

import java.util.List;

import com.randomnoun.common.jexl.eval.EvalContext;
import com.randomnoun.common.jexl.eval.EvalException;
import com.randomnoun.common.jexl.eval.EvalFunction;
import com.randomnoun.common.jexl.sql.SqlColumn;

/** Defines a startsWith() function.
 *
 * Returns true if the first value starts with the second value.
 *
 * <p>The result of the function is of type Boolean.
 */
public class StartsWithFunction
    implements EvalFunction
{
    /** Implements the function as per the class description. */
    public Object evaluate(String functionName, EvalContext context, List<Object> arguments)
        throws EvalException
    {
        String arg0;
        if (arguments.size() != 2) { throw new EvalException(functionName + "() must contain two parameters"); }
        if (arguments.get(0) instanceof String) {
            arg0 = (String) arguments.get(0);
        } else if (arguments.get(0) instanceof SqlColumn) {
            arg0 = ((SqlColumn) arguments.get(0)).getName();
        } else {
            throw new EvalException(functionName + "() parameter 1 must be a string or SqlColumn type; found " + arguments.get(0).getClass().getName());
        }
        if (!(arguments.get(1) instanceof String)) {
            throw new EvalException(functionName + "() parameter 2 must be a string type; found " + arguments.get(1).getClass().getName());
        }
                    
        String arg1 = (String)arguments.get(1);
        if (arg0 == null) {
            throw new EvalException(functionName + "() first parameter cannot be null");
        }
        if (arg1 == null) {
            throw new EvalException(functionName + "() second parameter cannot be null");
        }

        return Boolean.valueOf(arg0.startsWith(arg1));
    }
}