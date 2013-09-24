package com.randomnoun.common;

/* (c) 2013 randomnoun. All Rights Reserved. This work is licensed under a
 * BSD Simplified License. (http://www.randomnoun.com/bsd-simplified.html)
 */

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Properties;

import javax.sql.DataSource;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import com.randomnoun.common.SourceforgeStats.ScmType;
import com.randomnoun.common.SourceforgeStats.Stat;
import com.randomnoun.common.log4j.Log4jCliConfiguration;

/** Test the SourceforgeStats retrieval class.
 * 
 * <p>File download statistics are retrieved from Jan 2011 until Sep 2012
 * 
 * <P>Source control statistics are retrieved from Jan 2012 until Sep 2012
 * 
 * @author knoxg
 * @blog http://www.randomnoun.com/wp/2012/09/23/sourceforge-omphaloskepsis/
 * @version $Id$
 *
 */
public class SourceforgeStatsTest extends TestCase {

	/** Enable this to actually run the tests 
	 * (disabled since we don't want this to run during normal maven builds) */
	private static boolean ENABLED = false;
	
	// ***** update these constants for your database / project settings
	
	private static String JDBC_CONNECTION_STRING = 
	  "jdbc:mysql://filament/stats?zeroDateTimeBehavior=convertToNull&autoReconnect=true";
	private static String JDBC_USERNAME = "stats";
	private static String JDBC_PASSWORD = "stats";
	
	// the indexes into this array correspond to `daily`.`projectId` values in MySQL
	private static String PROJECT_NAMES[] = { "p7spy", "jvix", "packetmap", "timetube" };
	
	// arrays containing which projects have each type of statistic 
	private static String DOWNLOAD_STATS[] = { "p7spy", "jvix", "packetmap", "timetube" };
	private static String CVS_STATS[] = { "jvix" };
	private static String SVN_STATS[] = { "packetmap", "timetube" };

    // *****

	// test fixture instance variables
	Calendar start2011, start2012, until2012;
	List<String> projectList;
	JdbcTemplate jt;
	
	
	public void setUp() {
		if (!ENABLED) { return; }
		Log4jCliConfiguration lcc = new Log4jCliConfiguration();
		Properties props = new Properties();
		props.put("log4j.rootCategory", "INFO, CONSOLE"); // default to INFO threshold, send to CONSOLE logger
		props.put("log4j.logger.com.randomnoun.common.SourceforgeStats", "DEBUG"); // DEBUG threshold for this class
		lcc.init("[SourceforgeStats]", props);

		start2011 = new GregorianCalendar(); start2011.clear();
		start2012 = new GregorianCalendar(); start2012.clear();
		until2012 = new GregorianCalendar(); until2012.clear();
		
		start2011.set(2011, 0, 1, 0, 0, 0); // Jan 2011 (0-based month)
		start2012.set(2012, 0, 1, 0, 0, 0); // Jan 2012
		until2012.set(2012, 8, 1, 0, 0, 0); // Sep 2012

		projectList = Arrays.asList(PROJECT_NAMES);
		
		
		try {
			Connection conn = DriverManager.getConnection(
			  JDBC_CONNECTION_STRING, JDBC_USERNAME, JDBC_PASSWORD);
			DataSource ds = new SingleConnectionDataSource(conn, true);
			jt = new JdbcTemplate(ds);
		} catch (SQLException sqle) {
			throw new RuntimeException(sqle);
		}
	}
	
	public void testGetFileDownloadStats() throws IOException {
		if (!ENABLED) { return; }
		SourceforgeStats sfs = new SourceforgeStats();
		for (String projectName : DOWNLOAD_STATS) {
			List<Stat> stats = sfs.getFileDownloadStats(projectName, start2011, until2012); 
			sfs.updateDatabase(jt, projectList.indexOf(projectName), stats);
		}
	}

	public void testGetCvsStats() throws IOException {
		if (!ENABLED) { return; }
		SourceforgeStats sfs = new SourceforgeStats();
		for (String projectName : CVS_STATS) {
			List<Stat> stats = sfs.getScmStats(projectName, ScmType.CVS, start2012, until2012); 
			sfs.updateDatabase(jt, projectList.indexOf(projectName), stats);
		}
	}
	
	
	public void testGetSvnStats() throws IOException {
		if (!ENABLED) { return; }
		SourceforgeStats sfs = new SourceforgeStats();
		for (String projectName : SVN_STATS) {
			List<Stat> stats = sfs.getScmStats(projectName, ScmType.SVN, start2012, until2012); 
			sfs.updateDatabase(jt, projectList.indexOf(projectName), stats);
		}
	}
	
	public static void main(String[] args) {
		junit.textui.TestRunner.run(new TestSuite(SourceforgeStatsTest.class));
	}   
	
}
