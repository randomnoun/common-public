package com.randomnoun.common.jexl;

/** An SqlFunction can throw this to indicate that it should be evaluated into an SQL literal, 
 * rather than being translated into an SQL expression.
 * 
 * @author knoxg
 */
public class EvalFallbackException extends Exception {
	/** generated serialVersionUID */
	private static final long serialVersionUID = -1826807890464491396L;

	public EvalFallbackException() {
		super();
	}
}