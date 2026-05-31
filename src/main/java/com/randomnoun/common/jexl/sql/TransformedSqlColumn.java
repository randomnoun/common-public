package com.randomnoun.common.jexl.sql;

/** An SQL column which represents a transformed value ( the value in the expression is not the stored value ).
 * 
 * <p>Examples of TransformedSqlColumns:
 * <ul>
 * <li>enum values ( 1-char in database exposed as enum strings )
 * <li>yes/no values ( 'Y'/'N' in database exposed as booleans )
 * <li>api-prefixed values ( 1234 in database exposed as "obj-1234" )
 * </ul>
 * 
 * <p>If the transformation is reversible, we can convert some expressions to use the raw column 
 * which can then leverage indexes on that column.
 * 
 */
public abstract class TransformedSqlColumn extends SqlColumn {

	public TransformedSqlColumn(String name, String table, int dataType) {
		super(name, table, dataType);
	}

	/** If true, we can perform null checks on the source column instead of the transformed value */
	public abstract boolean isNullPreserved();        // if nulls are preserved through transformation
	
	/** If true, we can reverse transformations for equals and not-equals comparisons */
	public abstract boolean isReversableEquals();     // if we can convert col == "someValue" to a simplified form
	
	/** If true, we can reverse transformations for comparison operations; we can only do this if transformations preserve ordering */
	public abstract boolean isReversableComparison(); // if we can convert col > "someValue" to a simplified form
	
	/** The source column */
	public abstract SqlColumn getSourceSqlColumn();    // name of column to use for reverse comparisons
	
	/** Reverse the transformation, i.e. convert a user-supplied value back into a database value.
	 * The user-supplied value must be a literal (String, Boolean, Long etc), not an expression. 
	 * 
	 * @param value literal value in expression
	 * 
	 * @value the raw value stored in the database
	 */
	public abstract Object reverseTransformLiteral(Object value) throws CannotReverseTransformationException;
	
}
