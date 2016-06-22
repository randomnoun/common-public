package com.randomnoun.common.spring;

/* (c) 2013 randomnoun. All Rights Reserved. This work is licensed under a
 * BSD Simplified License. (http://www.randomnoun.com/bsd-simplified.html)
 */

import java.io.*;
import java.sql.*;
import java.util.*;


import org.springframework.dao.TypeMismatchDataAccessException;
import org.springframework.jdbc.core.*;
import org.springframework.jdbc.support.lob.LobHandler;
import org.springframework.jdbc.support.lob.OracleLobHandler;

import com.randomnoun.common.StreamUtil;



/**
 * This class is intended to replace the standard RowMapper provided by Spring
 * to return a List of String objects (rather than a List of Maps). The
 * queries executed by this rowMapper must therefore always evaluate to
 * a single String or CLOB column (the CLOB will be mapped to a String
 * if required).
 *
 * <p>This class can be used as in the following code fragment:
 *
 * <pre style="code">
                JdbcTemplate jt = EjbConfig.getEjbConfig().getJdbcTemplate();
                List stringList = jt.query(
                    "SELECT DISTINCT roleName FROM userRole " +
                    "WHERE customerId = ? ",
                    new Object[] { new Long(customerId) },
                    new RowMapperResultReader(new StringRowMapper() ));
 * </pre>
 *
 *
 * @version $Id$
 * @author knoxg
 */
public class StringRowMapper
    implements RowMapper
{
    /** A revision marker to be used in exception stack traces. */
    public static final String _revision = "$Id$";

    /** Oracle Large OBject handler. */
    final LobHandler lobHandler; // could make this static if it's threadsafe

    /**
     * Creates a new ClobRowMapper object.
     */
    public StringRowMapper()
    {
        try {
            lobHandler = new OracleLobHandler();
        } catch (Exception e) {
            throw new RuntimeException("Could not instantiate lobHandler");
        }
    }

    /**
     * Returns a single object representing this row.
     *
     * @param resultSet The resultset to map
     * @param rowNumber The current row number
     *
     * @return a single object representing this row.
     *
     * @throws TypeMismatchDataAccessException if the first column of the restset is not
     *   a String or CLOB.
     */
    public Object mapRow(ResultSet resultSet, int rowNumber)
        throws SQLException
    {
        Map row = new HashMap();
        String key;
        Object value;

        value = resultSet.getObject(1);

        if (value == null) {
            // just keep it as a null
        } else if (value.getClass().getName().equals("oracle.sql.CLOB")) {
        	// need to do text comparison rather than instanceof to get around classloading issues
            value = lobHandler.getClobAsString(resultSet, 1);
        } else if (value instanceof java.sql.Clob) {
			Clob clob = (Clob) value;
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try {
				StreamUtil.copyStream(clob.getAsciiStream(), baos, 1024);
			} catch (IOException ioe) {
				throw (SQLException) new SQLException("IO error transferring CLOB").initCause(ioe);
			}
			value = baos.toString();
        } else if (!(value instanceof String)) {
            throw new TypeMismatchDataAccessException(
                "Expecting String/CLOB in column 1 of query (found " +
                value.getClass().getName() + ")");
        }

        return (String)value;
    }
}
