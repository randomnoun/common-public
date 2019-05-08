package com.randomnoun.common.spring;

import org.springframework.jdbc.core.ResultSetExtractor;

/** A SelectFromResultSetExtractor is pretty much the same as a ResultSetExtractor, except 
 * it knows what columns and tables to query (i.e. the 'SELECT' and 'FROM' clauses of the SQL), 
 * so the caller only needs to supply WHERE and ORDER BY SQL clauses
 * 
 * @author knoxg
 */
public interface SelectFromResultSetExtractor<T> extends ResultSetExtractor<T> {

	public String getSelect();
	
	public String getFrom();
	
	// this might be a good place to add pagination constraints as well, once we add that to the API

	
}
