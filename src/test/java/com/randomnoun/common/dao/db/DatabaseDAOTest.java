package com.randomnoun.common.dao.db;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import com.randomnoun.common.to.db.DatabaseTO;
import com.randomnoun.common.to.db.DatabaseTO.DatabaseType;
import com.randomnoun.common.to.db.DatabaseTO.SchemaTO;

import junit.framework.TestCase;

/**
 * 
 * 
 * @author knoxg
 */
public class DatabaseDAOTest extends TestCase {

	private DataSource ds;
	private JdbcTemplate jt;

	Logger logger = Logger.getLogger(DatabaseDAOTest.class);
	
	public void init() throws ClassNotFoundException, SQLException {
		//if (true) { return; } // skip tests for now
		
		Class.forName("com.mysql.jdbc.Driver");
		String connString = "jdbc:mysql://localhost:3306/datatype-dev";
        String username = "datatype-dev";
		String password = "datatype-dev";
		Connection conn = DriverManager.getConnection(connString, username, password);
		ds = new SingleConnectionDataSource(conn, false);
		jt = new JdbcTemplate(ds);

		Properties lp = new Properties();
		lp.put("log4j.rootCategory", "DEBUG, CONSOLE");
		lp.put("log4j.appender.CONSOLE", "org.apache.log4j.ConsoleAppender");
		lp.put("log4j.appender.CONSOLE.layout", "org.apache.log4j.PatternLayout");
		lp.put("log4j.appender.CONSOLE.layout.ConversionPattern", "[DatabaseDAOTest] %d{ABSOLUTE} %-5p %c - %m %n");

		/*
		 * lp.put("log4j.appender.FILE", "com.randomnoun.common.log4j.CustomRollingFileAppender");
		 * lp.put("log4j.appender.FILE.File", "c:\\datebaseDAOTest.log");
		 * lp.put("log4j.appender.FILE.MaxBackupIndex", "100");
		 * lp.put("log4j.appender.FILE.layout", "org.apache.log4j.PatternLayout");
		 * lp.put("log4j.appender.FILE.layout.ConversionPattern","%d{dd/MM/yy HH:mm:ss.SSS} %-5p %c - %m %n");
		 */

		lp.put("log4j.logger.org.springframework", "INFO");	
		PropertyConfigurator.configure(lp);
	}
	
	private void dumpSchema(SchemaTO s) {
		logger.info("=== Schema " + s);
		
	}
	
	public void testDbMetadata() throws SQLException, ClassNotFoundException {
		init();
		DatabaseDAO dbDAO = new DatabaseDAO(jt, DatabaseType.MYSQL);
		DatabaseTO db = dbDAO.getDatabaseTO();
		logger.info("Product: " + db.getProductName());
		logger.info("Product version: " + db.getProductVersion());
		logger.info("Database version: " + db.getMajorVersion() + "." + db.getMinorVersion());
		for (String schemaName : db.getSchemas().keySet()) {
			logger.info("Schema " + schemaName);
			SchemaTO s = db.getSchemas().get(schemaName);
			dumpSchema(s);
		}
	}
	
	public void testAddRecord() throws ClassNotFoundException, SQLException {
		init();
		// lngId is an auto_increment field
		long now = System.currentTimeMillis();
		
		// this book looks fascinating
		// https://books.google.com.au/books?id=a8W8fKQYiogC&lpg=PA40&ots=ok_NBw7-CL&dq=mysql%20year%20datatype%20jdbc&pg=PP1#v=onepage&q=mysql%20year%20datatype%20jdbc&f=false
		
		jt.update("INSERT INTO tblAllType(sbitBit, snbitBit, " + 
			" uintTinyInt, sintTinyInt, unintTinyInt, snintTinyInt, " + 
			" sblnBoolean, snblnBoolean, " + 
			" uintSmallInt, sintSmallInt, unintSmallInt, snintSmallInt, " + 
			" uintMediumInt, sintMediumInt, unintMediumInt, snintMediumInt, " + 
			" uintInteger, sintInteger, unintInteger, snintInteger, " + 
			" uintBigInt, sintBigInt, unintBigInt, snintBigInt, " + 
			" udecDecimal10_0, sdecDecimal10_0, undecDecimal10_0, sndecDecimal10_0, " + 
			" ufltFloat, sfltFloat, unfltFloat, snfltFloat, " + 
			" udblDouble, sdblDouble, undblDouble, sndblDouble, " + 
			" dtmDate, ndtmDate, dtmDateTime, ndtmDateTime, " + 
			" dtmTimestamp, ndtmTimestamp, dtmTime, ndtmTime, " +
			" dtmYear, ndtmYear, " + 
			" txtChar10, ntxtChar10, txtVarchar10, ntxtVarchar10, " + 
			" binBinary10, nbinBinary10, binVarBinary10, nbinVarBinary10, " + 
			" binTinyBlob, nbinTinyBlob, txtTinyText, ntxtTinyText, " + 
			" binBlob10, nbinBlob10, txtText10, ntxtText10, " + 
			" binMediumBlob, nbinMediumBlob, txtMediumText, ntxtMediumText, " + 
			" binLongBlob, nbinLongBlob, txtLongText, ntxtLongText, " + 
			" enmEnum, nenmEnum, setSet, nsetSet) " +
			" VALUES ( ?, ?," +
			" ?, ?, ?, ?," +
			" ?, ?," +
			" ?, ?, ?, ?," +
			" ?, ?, ?, ?," +
			" ?, ?, ?, ?," +
			" ?, ?, ?, ?," +
			" ?, ?, ?, ?," +
			" ?, ?, ?, ?," +
			" ?, ?, ?, ?," +
			" ?, ?, ?, ?," +
			" ?, ?, ?, ?, " +
			" ?, ?," +
			" ?, ?, ?, ?," +
			" ?, ?, ?, ?," +
			" ?, ?, ?, ?," +
			" ?, ?, ?, ?," +
			" ?, ?, ?, ?," +
			" ?, ?, ?, ?," +
			" ?, ?, ?, ?)",
			// these types are from column 3 of http://dev.mysql.com/doc/connector-j/en/connector-j-reference-type-conversions.html
			new Object[] { /* bit */ true, true,
				/* tinyint */ (int) 1, (int) 1, (int) 1, (int) 1,  // tinyint(1)=boolean if tinyInt1IsBit property enabled(default); integer otherwise
				/* boolean */ true, true, // synonym for tinyint(1); see above
				/* smallint */ (int) 1, (int) 1, (int) 1, (int) 1,
				/* mediumint */ (int) 1, (int) 1, (int) 1, (int) 1,  // unsigned were long with C/J 3.1 and earlier
				/* integer */ (int) 1, (int) 1, (long) 1, (long) 1,  // unsigned are long
				/* bigint */ (long) 1, (long) 1, new BigInteger("1"), new BigInteger("1"), // unsigned are BigInteger
				/* decimal */ new BigDecimal("1"), new BigDecimal("1"), new BigDecimal("1"), new BigDecimal("1"),
				/* float */ (float) 1, (float) 1, (float) 1, (float) 1,
				/* double */ (double) 1, (double) 1, (double) 1, (double) 1, 
				/* date, datetime */ new java.sql.Date(now), new java.sql.Date(now), new Timestamp(now), new Timestamp(now),
				/* timestamp, time */ new Timestamp(now), new Timestamp(now), new Time(now), new Time(now),
				// NB: get 'Data truncated for column 'dtmYear' at row 1' errors if supplying a java.sql.Date or java.util.Date (Types.DATE) for year:
				/* year */ (short) 2015, (short) 2015,  // year=java.sql.Date if yearIsDate property enabled(default); short otherwise
				/* char, varchar */ "char", "char", "varchar", "varchar", // if character set is BINARY, then byte[]
				/* binary, varbinary */ new byte[] { 1 }, new byte[] { 1 }, new byte[] { 1 }, new byte[] { 1 }, 
				/* tinyblob, tinytext */ new byte[] { 1 }, new byte[] { 1 }, "tinytext", "tinytext", 
				/* blob, text*/ new byte[] { 1 }, new byte[] { 1 }, "tinytext", "tinytext",
				/* mediumblob, mediumtext */ new byte[] { 1 }, new byte[] { 1 }, "tinytext", "tinytext",
				/* longblob, longtext */ new byte[] { 1 }, new byte[] { 1 }, "tinytext", "tinytext",
				/* enum, set */ "value1", "value1", "value1", "value1"
		    },
			// these types are from column 2 of http://dev.mysql.com/doc/connector-j/en/connector-j-reference-type-conversions.html 
			new int[] { Types.BIT, Types.BIT, 
				Types.TINYINT, Types.TINYINT, Types.TINYINT, Types.TINYINT,
				Types.TINYINT, Types.TINYINT,
				Types.SMALLINT, Types.SMALLINT, Types.SMALLINT, Types.SMALLINT, 
				Types.INTEGER, Types.INTEGER, Types.INTEGER, Types.INTEGER, // states MEDIUMINT (not in Types) in Connector-J reference 
				Types.INTEGER, Types.INTEGER, Types.INTEGER, Types.INTEGER,
				Types.BIGINT, Types.BIGINT, Types.BIGINT, Types.BIGINT, 
				Types.DECIMAL, Types.DECIMAL, Types.DECIMAL, Types.DECIMAL, 
				Types.FLOAT, Types.FLOAT, Types.FLOAT, Types.FLOAT, 
				Types.DOUBLE, Types.DOUBLE, Types.DOUBLE, Types.DOUBLE, 
				Types.DATE, Types.DATE, Types.DATE, Types.DATE, 
				Types.TIMESTAMP, Types.TIMESTAMP, Types.TIME, Types.TIME, 
				Types.NUMERIC, Types.NUMERIC, // states YEAR (not in Types), tried DATE, but get 'Data truncated for column 'dtmYear' at row 1' errors
				Types.CHAR, Types.CHAR, Types.VARCHAR, Types.VARCHAR,
				Types.BINARY, Types.BINARY, Types.VARBINARY, Types.VARBINARY,
				Types.BLOB, Types.BLOB, Types.VARCHAR, Types.VARCHAR, // states TINYBLOB
				Types.BLOB, Types.BLOB, Types.VARCHAR, Types.VARCHAR,
				Types.BLOB, Types.BLOB, Types.VARCHAR, Types.VARCHAR, // states MEDIUMBLOB
				Types.BLOB, Types.BLOB, Types.VARCHAR, Types.VARCHAR, // states LONGBLOB. NB: Not LONGBINARY, LONGVARCHAR 
				Types.CHAR, Types.CHAR, Types.CHAR, Types.CHAR });
	}
	
}
