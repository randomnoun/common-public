package com.randomnoun.common.security.impl;

/* (c) 2013 randomnoun. All Rights Reserved. This work is licensed under a
 * BSD Simplified License. (http://www.randomnoun.com/bsd-simplified.html)
 */

import java.io.StringReader;
import java.util.*;

import com.randomnoun.common.jexl.ast.TopLevelExpression;
import com.randomnoun.common.jexl.eval.EvalContext;
import com.randomnoun.common.jexl.eval.EvalFunction;
import com.randomnoun.common.jexl.eval.Evaluator;
import com.randomnoun.common.jexl.parser.ExpressionParser;
import com.randomnoun.common.jexl.parser.ParseException;
import com.randomnoun.common.jexl.parser.TokenMgrError;
/*
import com.randomnoun.common.jexl.sql.function.AggregateFunction;
import com.randomnoun.common.jexl.sql.function.BetweenFunction;
import com.randomnoun.common.jexl.sql.function.EndsWithFunction;
import com.randomnoun.common.jexl.sql.function.IsInFunction;
import com.randomnoun.common.jexl.sql.function.IsNullFunction;
import com.randomnoun.common.jexl.sql.function.LikeFunction;
import com.randomnoun.common.jexl.sql.function.PromptFunction;
import com.randomnoun.common.jexl.sql.function.StartsWithFunction;
*/
import com.randomnoun.common.security.ResourceCriteria;

/**
 * Implements the ResourceCriteria interface, using JEXL strings to represent
 * {@link com.randomnoun.common.jexl.ast.TopLevelExpression} objects,
 * which are then used to determine matches a given criteria context.
 *
 * 
 * @author knoxg
 */
public class ResourceCriteriaImpl extends ResourceCriteria
{
    
    /** generated serialVerisonUID */
	private static final long serialVersionUID = -8307206866854674839L;
	
	/** The expression used to evaluate a criteria context. */
    private TopLevelExpression expression;

    /**
     * Construct a new ResourceCriteriaImpl object. The criteria is parsed using
     * an EditableTranslator to convert it into a EditableCriteria object,
     * and from there into an Expression object.
     *
     * @param criteriaString The JEX expression used to define this criteria.
     *
     * @throws CriteriaException if the underlying EditableTranslator throws
     *   an exception during initialisation, deserialisation or conversion.
     */
    public ResourceCriteriaImpl(String criteriaString)
    {
        super(criteriaString);

        if (criteriaString != null && !"".equals(criteriaString)) {
            try {
                expression = stringToExpression(criteriaString);
            } catch (java.text.ParseException pe) {
                throw new RuntimeException(
                    "Illegal expression found in security table: '" + criteriaString +
                    "', Error: " + pe.getMessage());
            }
        }
    }
    
    /** Convert Java expression String to a TopLevelExpression */
    public static TopLevelExpression stringToExpression(String expressionString)
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
    

    /**
     * Evaluates the supplied criteria context against the criteria expression
     * stored in this object.
     *
     * {@inheritdoc}
     *
     * @param criteriaContext {@inheritdoc}
     *
     * @return {@inheritdoc}
     */
    public boolean evaluate(Map<String, Object> criteriaContext)
    {
        EvalContext evalContext = new EvalContext();

        Map<String, EvalFunction> functions = new HashMap<String, EvalFunction>();
        /*
        functions.put("all", new AggregateFunction("all", "AND", false, false));
        functions.put("anyTrue", new AggregateFunction("anyTrue", "OR", false, false));        
        functions.put("anyFalse", new AggregateFunction("anyFalse", "OR", false, true));
        functions.put("none", new AggregateFunction("none", "AND", true, false));
        functions.put("like", new LikeFunction());
        functions.put("startsWith", new StartsWithFunction());
        functions.put("endsWith", new EndsWithFunction());
        functions.put("prompt", new PromptFunction());
        functions.put("between", new BetweenFunction());
        functions.put("isNull", new IsNullFunction());
        functions.put("isIn", new IsInFunction());
        */
        evalContext.setFunctions(functions);

        // put security-specific resources, functions, etc... in here
        evalContext.setVariables(criteriaContext);

        if (expression == null) {
            // always return true for null expressions
            return true;
        }
        
        Evaluator evaluator = new Evaluator();
        Object result = evaluator.visit(expression, evalContext);
        return ((Boolean) result).booleanValue();
        // return ExpressionUtils.evaluateBooleanExpression(expression, evalContext);
    }
}
