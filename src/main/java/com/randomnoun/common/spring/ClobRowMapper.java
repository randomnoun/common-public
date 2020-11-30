package com.randomnoun.common.spring;

/* (c) 2013 randomnoun. All Rights Reserved. This work is licensed under a
 * BSD Simplified License. (http://www.randomnoun.com/bsd-simplified.html)
 */

import java.io.*;
import java.sql.*;
import java.util.*;

import javax.sql.*;

import org.apache.log4j.Logger;
import org.springframework.jdbc.core.*;
import org.springframework.jdbc.support.DatabaseMetaDataCallback;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.MetaDataAccessException;
import org.springframework.jdbc.support.lob.DefaultLobHandler;
import org.springframework.jdbc.support.lob.LobHandler;

import com.randomnoun.common.StreamUtil;
import com.randomnoun.common.spring.ClobRowMapper;

/**
 * This class is intended to replace the standard RowMapper provided by Spring
 * to allow CLOBs to be treated as String objects transparently (i.e. the Map
 * which represents a row returned by this class will not contain any
 * oracle.sql.CLOB objects).
 *
 * <p>LobHandlers aren't required in recent Oracle drivers, so this entire class is 
 * probably obsolete these days.
 *
 * 
 * @author knoxg
 */
public class ClobRowMapper implements RowMapper<Map<String, Object>>
{
    
    /** Logger for this class */
    public final Logger logger = Logger.getLogger(ClobRowMapper.class);

    /** Oracle Large OBject handler. */
    final LobHandler lobHandler; 

    public String detectDatabase(DataSource ds) {
        logger.debug("Looking up default SQLErrorCodes for DataSource");
        String dbName = "unknown";
        
        try {
            @SuppressWarnings("unchecked")
			Map<String, Object> dbmdInfo = (Map<String, Object>) JdbcUtils.extractDatabaseMetaData(ds, new DatabaseMetaDataCallback() {
                public Object processMetaData(DatabaseMetaData dbmd) throws SQLException {
                    Map<String, Object> info = new HashMap<String, Object>(2);
                    if (dbmd != null) {
                        info.put("DatabaseProductName", dbmd.getDatabaseProductName());
                        info.put("DriverVersion", dbmd.getDriverVersion());
                    }
                    return info;
                }
            });
            if (dbmdInfo != null) {
                
                // should always be the case outside of test environments
                dbName = (String) dbmdInfo.get("DatabaseProductName");
                String driverVersion = (String) dbmdInfo.get("DriverVersion");
                logger.debug("Found dbName='" + dbName + "', driverVerson='" + driverVersion + "'");

                if (dbName != null && dbName.startsWith("DB2")) {
                    dbName = "DB2";
                }
                /* 
                if (dbName != null) {
                    this.dataSourceProductName.put(ds, dbName);
                    logger.info("Database Product Name is " + dbName);
                    logger.info("Driver Version is " + driverVersion);
                    SQLErrorCodes sec = (SQLErrorCodes) this.rdbmsErrorCodes.get(dbName);
                    if (sec != null) {
                        return sec;
                    }
                    logger.info("Error Codes for " + dbName + " not found");
                }
                */
            }
        }
        catch (MetaDataAccessException ex) {
            logger.warn("Error while getting database metadata", ex);
        }

        // fallback is to return an empty ErrorCodes instance
        logger.debug("Returning dbName='" + dbName + "'");
        return dbName;
    }
            
            
    /**
     * Creates a new ClobRowMapper object.
     */
    public ClobRowMapper(JdbcTemplate jt)
    {
    	lobHandler = new DefaultLobHandler();
    	/*
        DataSource ds = jt.getDataSource();
        try {
            if (detectDatabase(ds).equals("DB2")) {
            	lobHandler = new DefaultLobHandler();
            } else {
                // lobHandler = new OracleLobHandler();
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not instantiate lobHandler");
        }
        */
    }

    /** Map rows to a disconnected HashMap representation */
    public Map<String, Object> mapRow(ResultSet rs, int rowNum)
        throws SQLException
    {
        Map<String, Object> row = new HashMap<String, Object>();
        String key;
        Object value;

        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();

        for (int i = 1; i <= columnCount; i++) {
			// key.toUpperCase required for SqlServer; DB2 & Oracle will automatically
			// do this anyway
            key = metaData.getColumnLabel(i).toUpperCase();  
            value = rs.getObject(i);
            if (value != null && value.getClass().getName().equals("oracle.sql.CLOB")) {
                value = lobHandler.getClobAsString(rs, i);
            } else if (value instanceof java.sql.Clob) {
                Clob clob = (Clob) value;
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                try {
                    StreamUtil.copyStream(clob.getAsciiStream(), baos, 1024);
                } catch (IOException ioe) {
                    throw (SQLException) new SQLException("IO error transferring CLOB").initCause(ioe);
                }
                
                value = baos.toString();
            }
            row.put(key, value);
        }

        return row;
    }
}
