package com.randomnoun.common.jexl.sql;

/** The TransformedSqlColumn.reverseLiteral() method can throw a CannotReverseLiteralException to indicate that
 * it cannot reverse a transformation on the supplied literal. 
 * 
 * @author knoxg
 */
public class CannotReverseTransformationException extends Exception {
	/** generated serialVersionUID */
	private static final long serialVersionUID = -400067560958864984L;

	public CannotReverseTransformationException() {
		super();
	}
}