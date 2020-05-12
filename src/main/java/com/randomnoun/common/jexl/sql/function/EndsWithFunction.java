package com.randomnoun.common.jexl.sql.function;

/* (c) 2013-2018 randomnoun. All Rights Reserved. This work is licensed under a
 * BSD Simplified License. (http://www.randomnoun.com/bsd-simplified.html)
 */

import java.util.List;

import com.randomnoun.common.jexl.eval.EvalContext;
import com.randomnoun.common.jexl.eval.EvalException;
import com.randomnoun.common.jexl.sql.SqlColumn;
import com.randomnoun.common.jexl.sql.SqlFunction;
import com.randomnoun.common.jexl.sql.SqlGenerator;
import com.randomnoun.common.jexl.sql.SqlText;

/** 'EndsWith' function. Returns true if the data in the specified SqlColumn (in the first
 *  argument) ends with the text in the second argument.
 */
public class EndsWithFunction extends com.randomnoun.common.jexl.eval.function.EndsWithFunction  implements SqlFunction {
	/** @inheritdoc */
	public String toSql(String functionName, EvalContext evalContext, List<Object> arguments) {
		if (arguments.size() != 2) { throw new EvalException(functionName + "() must contain two parameters"); }
		if (!(arguments.get(0) instanceof SqlColumn)) { throw new EvalException(functionName + "() parameter 1 must be an SQL column"); }
		if (!(arguments.get(1) instanceof String || arguments.get(1) instanceof SqlText)) { throw new EvalException(functionName + "() parameter 2 must be a string type"); }

		SqlColumn arg0 = (SqlColumn) arguments.get(0);
		String likePattern = "";

		if (arguments.get(1) instanceof String) {
			likePattern = "%" + SqlGenerator.escapeLikeLiteral(evalContext, (String) arguments.get(1));
		} else {
			// could conceivably concat result with '%'                    
			throw new EvalException("Cannot translate " + functionName + "(a,b) where 'b' is a dynamically-generated value");
		}

		Object arg1 = arguments.get(1);

		if (arg0 == null) { throw new EvalException(functionName + "() first parameter cannot be null"); }
		if (arg1 == null) { throw new EvalException(functionName + "() second parameter cannot be null"); }
		return "(" + arg0.getFullName() + " LIKE " + SqlGenerator.toSql(evalContext, likePattern) + ")";
	}
}