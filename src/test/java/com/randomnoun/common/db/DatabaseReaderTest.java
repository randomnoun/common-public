package com.randomnoun.common.db;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import javax.sql.DataSource;

import junit.framework.TestCase;

import org.apache.log4j.Logger;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import com.randomnoun.common.db.enums.DatabaseType;
import com.randomnoun.common.db.to.TableTO;
import com.randomnoun.common.db.dao.MysqlDatabaseReader;
import com.randomnoun.common.db.to.ConstraintTO;
import com.randomnoun.common.db.to.SchemaTO;
import com.randomnoun.common.db.to.TableColumnTO;
import com.randomnoun.common.log4j.Log4jCliConfiguration;

public class DatabaseReaderTest extends TestCase {
	
	Logger logger = Logger.getLogger(DatabaseReaderTest.class);
	
	private DataSource ds;
	
	public void setUp() throws ClassNotFoundException, SQLException, UnknownHostException {
		// if (true) { return; } // skip tests for now
		String hostname = InetAddress.getLocalHost().getHostName();
		System.out.println("Running on " + hostname);
		if (!(hostname.equals("halogen") || hostname.equals("bnedev11"))) { return; } // for now
		
		//Class.forName("com.mysql.jdbc.Driver");
		//String connString = "jdbc:mysql://bnetst04.dev.randomnoun/syra-tst";
		//String username = "syra-tst";
		//String password = "syra-tst";
		
		// can't get column metadata through this method
		//String connString = "jdbc:odbc:syra"; // this isn't going to work for jet, is it. no. no it isn't. not today anyway.
		//String connString  = "jdbc:odbc:Driver={Microsoft Access Driver (*.mdb, *.accdb)};DBQ=C:\\public\\syra.mdb";
		//String username = null;
		//String password = null;
		
        Log4jCliConfiguration lcc = new Log4jCliConfiguration();
        Properties props = new Properties();
        lcc.init("[DatabaseReaderTest]", props);
		
		logger.info("setUp()");
	}
	
	public void dumpSchema(SchemaTO s) {
		// something
		for (TableTO t : s.tables.values()) {
			logger.info("Table " + t.name);
			for (TableColumnTO tc : t.columns.values()) {
				logger.info("  Column " + tc.getName());
			}
			for (ConstraintTO c : t.constraints.values()) {
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
	
	
	public void testMysql() throws SQLException {

		// String connString  = "jdbc:mysql://localhost/jacobi-web-int?zeroDateTimeBehavior=convertToNull&autoReconnect=true&useSSL=false";
		String connString  = "jdbc:mysql://mysql.dev.randomnoun/common?zeroDateTimeBehavior=convertToNull&autoReconnect=true&useSSL=false";
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
		String connString  = "jdbc:oracle:thin:@farquin:1521:MLDEV1";
		String username = "florg";
		String password = "badger";
		Connection conn = DriverManager.getConnection(connString, username, password);
		ds = new SingleConnectionDataSource(conn, false);
		DatabaseReader dr = new OracleDatabaseReader(ds);
		SchemaTO s = dr.readSchema(null);
		dumpSchema(s);
	} 

	public void testSqlServer() throws SQLException, ClassNotFoundException {
		Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
		String connString = "jdbc:sqlserver://SERVER";
		String username = "randomnoun_BIBTest_ro"; // think the ro user is rw anyway
		String password = "abc123";
		
		Connection conn = DriverManager.getConnection(connString, username, password);
		ds = new SingleConnectionDataSource(conn, false);
		DatabaseReader dr = new SqlServerDatabaseReader(ds);
		SchemaTO s = dr.readSchema(null);
		dumpSchema(s);
	}
	*/
	
	// db2 etc
	

}
