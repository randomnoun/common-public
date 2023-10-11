package com.randomnoun.common.spring;

import org.springframework.jdbc.core.RowMapper;

/** A SelectFromRowMapper is pretty much the same as a RowMapper, except 
 * it knows what columns and tables to query (i.e. the 'SELECT' and 'FROM' clauses of the SQL), 
 * so the caller only needs to supply WHERE and ORDER BY SQL clauses
 * 
 * @author knoxg
 */
public interface SelectFromRowMapper<T> extends RowMapper<T> {

	// cte can be used to set up common table expressions ('WITH' statements) before the SELECT 
	
	default public String getCte() { return null; }
	
	public String getSelect();
	
	public String getFrom();
	
	// this might be a good place to add pagination constraints as well, once we add that to the API
	
}
