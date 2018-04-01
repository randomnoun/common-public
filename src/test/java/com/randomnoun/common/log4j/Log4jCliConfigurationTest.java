package com.randomnoun.common.log4j;

/* (c) 2013 randomnoun. All Rights Reserved. This work is licensed under a
 * BSD Simplified License. (http://www.randomnoun.com/bsd-simplified.html)
 */

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.randomnoun.common.log4j.Log4jCliConfigurationTest;

import junit.framework.TestCase;

/**
 * Unit test for Log4jCliConfigurationTest
 *
 * @blog http://www.randomnoun.com/wp/2013/01/13/logging/
 * 
 * @author knoxg
 */
public class Log4jCliConfigurationTest
	extends TestCase
{

	ByteArrayOutputStream baos;
	PrintStream stdout; 
	PrintStream out;
	
	/** Redirect System.out to something we can inspect
	 */
	protected void setUp() {
		baos = new ByteArrayOutputStream();
		out = new PrintStream(baos);
		stdout = System.out;
		System.setOut(out);
	}
	
	/**
	 */
	protected void tearDown()
	{
		// reset System.out back to stdout
		System.setOut(stdout);
	}
	
	public void testNoLogPrefix() {
		// CRLFs on windows
		String lineSeparator = System.getProperty("line.separator");		
		String regex, line;
		
		Log4jCliConfiguration lcc = new Log4jCliConfiguration();
		lcc.init(null, null);
		Logger logger = Logger.getLogger("testLogger");
		
		logger.info("info message");
		regex = "^[0-9]{2}:[0-9]{2}:[0-9]{2},[0-9]{3} INFO  testLogger - info message$";
		line = baos.toString().split(lineSeparator)[0];
		// should look something like "06:52:08,156 INFO  testLogger - info message"
		assertTrue("String '" + line + "' does not match regex '" + regex + "'", line.matches(regex));
		
		logger.debug("debug message");
		regex = "^[0-9]{2}:[0-9]{2}:[0-9]{2},[0-9]{3} DEBUG testLogger - debug message$";
		line = baos.toString().split(lineSeparator)[1];
		assertTrue("String '" + line + "' does not match regex '" + regex + "'", line.matches(regex));
	}

	public void testWithPrefix() {
		// CRLFs on windows
		String lineSeparator = System.getProperty("line.separator");		
		String regex, line;
		
		Log4jCliConfiguration lcc = new Log4jCliConfiguration();
		lcc.init("[Log4jCliConfigurationTest]", null);
		Logger logger = Logger.getLogger("testLogger");
		
		logger.info("info message");
		regex = "^\\[Log4jCliConfigurationTest\\] [0-9]{2}:[0-9]{2}:[0-9]{2},[0-9]{3} INFO  testLogger - info message$";
		line = baos.toString().split(lineSeparator)[0];
		// should look something like "06:52:08,156 INFO  testLogger - info message"
		assertTrue("String '" + line + "' does not match regex '" + regex + "'", line.matches(regex));
		
		logger.debug("debug message");
		regex = "^\\[Log4jCliConfigurationTest\\] [0-9]{2}:[0-9]{2}:[0-9]{2},[0-9]{3} DEBUG testLogger - debug message$";
		line = baos.toString().split(lineSeparator)[1];
		assertTrue("String '" + line + "' does not match regex '" + regex + "'", line.matches(regex));
	}
	
	public void testPropertiesOverride() {
		// CRLFs on windows
		String lineSeparator = System.getProperty("line.separator");		
		String regex, line;
		
		Log4jCliConfiguration lcc = new Log4jCliConfiguration();
		Properties props = new Properties();
		props.put("log4j.rootCategory", "INFO, CONSOLE");  // set default threshold to 'INFO' level
		lcc.init("[Log4jCliConfigurationTest]", props);
		Logger logger = Logger.getLogger("testLogger");
		
		logger.info("info message");
		regex = "^\\[Log4jCliConfigurationTest\\] [0-9]{2}:[0-9]{2}:[0-9]{2},[0-9]{3} INFO  testLogger - info message$";
		line = baos.toString().split(lineSeparator)[0];
		// should look something like "06:52:08,156 INFO  testLogger - info message"
		assertTrue("String '" + line + "' does not match regex '" + regex + "'", line.matches(regex));
		
		logger.debug("debug message");
		assertTrue("debug message should have been suppressed", 
			baos.toString().split(lineSeparator).length==1);
	}

}
