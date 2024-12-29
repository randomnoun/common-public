package com.randomnoun.common.db;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import com.randomnoun.common.db.dao.MysqlDatabaseReader;
import com.randomnoun.common.db.to.ConstraintTO;
import com.randomnoun.common.db.to.SchemaTO;
import com.randomnoun.common.db.to.TableColumnTO;
import com.randomnoun.common.db.to.TableTO;

import junit.framework.TestCase;

public class DatabaseReaderTest extends TestCase {
	
	Logger logger = Logger.getLogger(DatabaseReaderTest.class);
	
	/** Tests will only run if build is executing on these hosts */
	public static List<String> TEST_HOSTS = Arrays.asList(new String[] { "halogen", "bnedev11", "yttrium" }); 
	
	private DataSource ds;
	
	public void setUp() throws ClassNotFoundException, SQLException, UnknownHostException {
		// if (true) { return; } // skip tests for now
		String hostname = InetAddress.getLocalHost().getHostName();
		System.out.println("Running on " + hostname);
		if (!TEST_HOSTS.contains(hostname)) { return; }
		LogManager.shutdown(); // ok so if I do this I need to reconstruct the Logger instances, which is annoying
		logger.info("setUp()");
		logger.fatal("For christ sake");
	}
	
	public void dumpSchema(SchemaTO s) {
		// something
		for (TableTO t : s.getTableMap().values()) {
			logger.info("Table " + t.getName());
			for (TableColumnTO tc : t.getTableColumnMap().values()) {
				logger.info("  Column " + tc.getName());
			}
			for (ConstraintTO c : t.getConstraintMap().values()) {
				logger.info("  Constraint " + c.getName());
			}
		}
		
		
	}
	
	/*
	public void testJet() throws SQLException {
		String connString  = "jdbc:odbc:Driver={Microsoft Access Driver (*.mdb, *.accdb)};" +
		  "DBQ=C:\\data\\work\\vmaint\\vmaint-mdb\\vmaint.mdb";
		String username = null;
		String password = null;
		Connection conn = DriverManager.getConnection(connString, username, password);
		ds = new SingleConnectionDataSource(conn, false);
		DatabaseReader dr = new JetDatabaseReader(ds);
		SchemaTO s = dr.readSchema(null);
		dumpSchema(s);
	}
	*/
	
	
	public void testMysql() throws SQLException, UnknownHostException {

		String hostname = InetAddress.getLocalHost().getHostName();
		if (hostname == null || !TEST_HOSTS.contains(hostname)) { return; }

		String connString  = "jdbc:mysql://localhost/common?zeroDateTimeBehavior=convertToNull&autoReconnect=true&useSSL=false";
		String username = "common";
		String password = "common";
		
		Connection conn = DriverManager.getConnection(connString, username, password);
		ds = new SingleConnectionDataSource(conn, false);
		DatabaseReader dr = new MysqlDatabaseReader(ds);
		SchemaTO s = dr.getSchema(null);
		dumpSchema(s);
	} 

	/*
	public void testOracle() throws SQLException {
		String connString  = "jdbc:oracle:thin:@test:1521:DEV";
		String username = "test";
		String password = "test";
		Connection conn = DriverManager.getConnection(connString, username, password);
		ds = new SingleConnectionDataSource(conn, false);
		DatabaseReader dr = new OracleDatabaseReader(ds);
		SchemaTO s = dr.readSchema(null);
		dumpSchema(s);
	} 

	public void testSqlServer() throws SQLException, ClassNotFoundException {
		Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
		String connString = "jdbc:sqlserver://SERVER";
		String username = "test"; 
		String password = "test";
		
		Connection conn = DriverManager.getConnection(connString, username, password);
		ds = new SingleConnectionDataSource(conn, false);
		DatabaseReader dr = new SqlServerDatabaseReader(ds);
		SchemaTO s = dr.readSchema(null);
		dumpSchema(s);
	}
	*/
	
	// db2 etc
	

}
