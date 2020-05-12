package com.randomnoun.common.jexl.eval.function;

/* (c) 2013-2018 randomnoun. All Rights Reserved. This work is licensed under a
 * BSD Simplified License. (http://www.randomnoun.com/bsd-simplified.html)
 */

import java.util.List;

import com.randomnoun.common.jexl.eval.EvalContext;
import com.randomnoun.common.jexl.eval.EvalException;
import com.randomnoun.common.jexl.eval.EvalFunction;

/** Defines a like() function.
 *
 * <p>like() takes a two parameters, the string being matched and the SQL LIKE pattern we
 * are matching on. The LIKE pattern uses the "%" character to represent any sequence of
 * characters, and "_" to indicate a single character. This class automatically deals
 * with conflicts with the Java regex parser, so the pattern may contain any other
 * Unicode or regular expression characters, which will be treated like any other
 * character.
 *
 * <p>The result of the function is of type Boolean.
 */
public class LikeFunction
    implements EvalFunction
{
    /** Implements the function as per the class description. */
    public Object evaluate(String functionName, EvalContext context, List<Object> arguments)
        throws EvalException
    {
        if (arguments.size() != 2) { throw new EvalException(functionName + "() must contain two parameters"); }
        if (!(arguments.get(0) instanceof String)) {
            throw new EvalException(functionName + "() parameter 1 must be a string type");
        }
        if (!(arguments.get(1) instanceof String)) {
            throw new EvalException(functionName + "() parameter 2 must be a string type");
        }

        String arg0 = (String)arguments.get(0);
        String arg1 = (String)arguments.get(1);
        if (arg0 == null) {
            throw new EvalException(functionName + "() first parameter cannot be null");
        }
        if (arg1 == null) {
            throw new EvalException(functionName + "() second parameter cannot be null");
        }

        // convert arg1 into a regex; first escape any chars that may confuse the
        // regex engine (see java.util.regex.Pattern class to see how this list was derived)
        String specialChars = "\\[]^$?*+{}|().";
        for (int i = 0; i < specialChars.length(); i++) {
            arg1 = arg1.replaceAll("\\" + specialChars.charAt(i),
                    "\\\\" + "\\" + specialChars.charAt(i));
        }
        arg1 = arg1.replaceAll("%", ".*");
        arg1 = arg1.replaceAll("_", ".");
        return Boolean.valueOf(arg0.matches(arg1));
    }
}