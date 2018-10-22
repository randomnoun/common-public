package com.randomnoun.common.spring;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;

/** A ResultSetExtractor that uses a RowMapper to return a List of values from a ResultSet, but
 * will not include nulls in the returned List. This could be used to return
 * structured objects, where some records are used to modify existing results rather
 * than returning new objects. 
 *  
 * @author knoxg
 *
 * @param <T> The type of object that the supplied RowMapper will return
 */
public class SkipNullResultSetExtractor<T> implements ResultSetExtractor<List<T>> {

	RowMapper<T> rowMapper;
	
	public SkipNullResultSetExtractor(RowMapper<T> rowMapper) {
		this.rowMapper = rowMapper;
	}
	
	@Override
	public List<T> extractData(ResultSet rs) throws SQLException, DataAccessException {
		List<T> results = new ArrayList<T>();
        int rowNum = 0;
        while (rs.next()) {
        	T rowObj = (T) rowMapper.mapRow(rs, rowNum++);
            if (rowObj!=null) { results.add(rowObj); }
        }
        return results;
	}


	
}