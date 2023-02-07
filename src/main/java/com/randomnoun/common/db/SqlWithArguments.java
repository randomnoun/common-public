package com.randomnoun.common.db;

import java.util.Arrays;
import java.util.Objects;

/** A container class for SQL with positional placeholders, and the arguments to be substituted 
 * into those placeholders, to be used in a JdbcTemplate.
 * 
 * @author knoxg
 */
public class SqlWithArguments {
	
	private String sql;
	private Object[] args;
	private int[] argTypes;
	
	/** Create a new SqlWithArguments object.
	 * 
	 * @param sql the SQL, containing '?' positional placeholders
	 * @param args the arguments to be substituted into those placeholders
	 * @param argTypes the SQL datatypes of the arguments (as java.sql.Types constants)   
	 */
	public SqlWithArguments(String sql, Object[] args, int[] argTypes) {
		this.sql = sql;
		this.args = args;
		this.argTypes = argTypes;
	}

	/** Return the SQL, containing '?' positional placeholders
	 * 
	 * @return the SQL, containing '?' positional placeholders
	 */
	public String getSql() {
		return sql;
	}

	
	/** Return the arguments to be substituted into the SQL placeholders
	 * 
	 * @return the arguments to be substituted into the SQL placeholders
	 */
	public Object[] getArgs() {
		return args;
	}
	
	/** Return the SQL datatypes of the arguments (as java.sql.Types constants)
	 *  
	 * @return the SQL datatypes of the arguments (as java.sql.Types constants)
	 */
	public int[] getArgTypes() {
		return argTypes;
		
	}

	// setters
	
	public void setSql(String sql) {
		this.sql = sql;
	}

	public void setArgs(Object[] args) {
		this.args = args;
	}

	public void setArgTypes(int[] argTypes) {
		this.argTypes = argTypes;
	}
	

    @Override
    public int hashCode() {
        return Objects.hash(sql, Arrays.hashCode(args), Arrays.hashCode(argTypes));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { 
        	return true; 
        } else if (o == null || getClass() != o.getClass()) { 
        	return false;
        } else {
        	SqlWithArguments that = (SqlWithArguments) o;
        	return Objects.equals(sql, that.sql) &&
    			Arrays.equals(args, that.args) &&
    			Arrays.equals(argTypes, that.argTypes);
        }
    }
	
}
