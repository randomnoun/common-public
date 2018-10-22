package com.randomnoun.common.spring;

/* (c) 2013 randomnoun. All Rights Reserved. This work is licensed under a
 * BSD Simplified License. (http://www.randomnoun.com/bsd-simplified.html)
 */

import java.sql.*;

import org.springframework.dao.TypeMismatchDataAccessException;
import org.springframework.jdbc.core.*;


/**
 * This class is intended to replace the standard RowMapper provided by Spring
 * to return a List of Long objects (rather than a List of Maps). The
 * queries executed by this rowMapper must therefore always evaluate to
 * a single numeric column 
 *
 * <p>This class can be used as in the following code fragment:
 *
 * <pre style="code">
                List longList = jt.query(
                    "SELECT DISTINCT lngId FROM tblCave " +
                    "WHERE txtSecretPassword = ? ",
                    new Object[] { "open sesame" },
                    new RowMapperResultReader(new LongRowMapper() ));
 * </pre>
 *
 *
 * 
 * @author knoxg
 */
public class LongRowMapper
    implements RowMapper<Long>
{
    
    

    /**
     * Creates a new ClobRowMapper object.
     */
    public LongRowMapper()
    {
    }

    /**
     * Returns a single Long representing this row.
     *
     * @param resultSet The resultset to map
     * @param rowNumber The current row number
     *
     * @return a single object representing this row.
     *
     * @throws TypeMismatchDataAccessException if the first column of the resultset is not
     *   numeric.
     */
    public Long mapRow(ResultSet resultSet, int rowNumber)
        throws SQLException
    {
        Object value;

        value = resultSet.getObject(1);

        if (value == null) {
            // just keep it as a null
        	return null;
        } else if (value instanceof Number) {
            return ((Number) value).longValue();
        } else {
            throw new TypeMismatchDataAccessException(
              "Expecting numeric value in column 1 of query (found " +
              value.getClass().getName() + ")");
        }
    }
}
