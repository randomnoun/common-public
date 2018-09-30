package com.randomnoun.common.jexl.eval;

/* (c) 2013 randomnoun. All Rights Reserved. This work is licensed under a
 * BSD Simplified License. (http://www.randomnoun.com/bsd-simplified.html)
 */


/**
 * A class that encapsulates a runtime exception occuring during
 * evaluation of an expression.
 *
 * <p>This class extends RuntimeException because the visit() classes in ObjectVisitor 
 * do not have any 'throws' clauses. This should probably be a checked exception.
 * 
 * @author knoxg
 */
public class EvalException
    extends RuntimeException
{
    /** Generated serialVersionUID */
	private static final long serialVersionUID = -3581223769526972975L;

	/**
     * Creates a new EvalException object.
     *
     * @param message exception message
     */
    public EvalException(String message)
    {
        super(message);
    }
}
