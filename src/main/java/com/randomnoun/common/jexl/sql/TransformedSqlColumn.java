package com.randomnoun.common.jexl.sql;

import com.randomnoun.common.jexl.EvalFallbackException;

/** An SQL column which represents a transformed value ( the value in the expression is not the stored value ).
 * 
 * <p>Examples of TransformedSqlColumns:
 * <ul>
 * <li>enum values ( 1-char in database exposed as enum strings)
 * <li>yes/no values ( 'Y'/'N' in database exposed as booleans )
 * <li>api-prefixed values ( 1234 in database exposed as "obj-1234" )
 * </ul>
 * 
 * <p>If the transformation is reversible, we can convert some expressions to use the raw column 
 * (which can leverage indexes on that column).
 * 
 */
public abstract class TransformedSqlColumn extends SqlColumn {

	public TransformedSqlColumn(String name, String table, int dataType) {
		super(name, table, dataType);
	}

	/** If true, we can reverse transformations for equals and not-equals comparisons */
	abstract boolean reversableEquals();     // if we can convert col == "someValue" to a simplified form
	
	/** If true, we can reverse transformations for comparison operations; we can only do this if transformations preserve ordering */
	abstract boolean reversableComparison(); // if we can convert col > "someValue" to a simplified form
	
	/** The source column */
	abstract SqlColumn getSourceSqlColumn();    // name of column to use for reverse comparisons
	
	/** Reverse the transformation 
	 * 
	 * @param value literal values in expression
	 * 
	 * @value the raw value stored in the database
	 */
	abstract Object reverseLiteral(Object value) throws EvalFallbackException;
	
}
