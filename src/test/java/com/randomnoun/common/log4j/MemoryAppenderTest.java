package com.randomnoun.common.log4j;

/* (c) 2013 randomnoun. All Rights Reserved. This work is licensed under a
 * BSD Simplified License. (http://www.randomnoun.com/bsd-simplified.html)
 */

import java.util.List;
import java.util.Properties;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.spi.LoggingEvent;

import com.randomnoun.common.log4j.MemoryAppenderTest;

import junit.framework.TestCase;

/**
 * Unit test for Log4jCliConfigurationTest
 * 
 * @blog http://www.randomnoun.com/wp/2013/01/13/logging/
 * 
 * @author knoxg
 */
public class MemoryAppenderTest
	extends TestCase
{

	/** Reset log4j before each test
	 */
	protected void setUp() {
		LogManager.resetConfiguration();
	}
	
	/**
	 */
	protected void tearDown()
	{
	}
	
	public void testMemoryAppenderViaProperties() {

		Properties props = new Properties();
		props.put("log4j.rootCategory", "INFO, MEMORY");
		props.put("log4j.appender.MEMORY", "com.randomnoun.common.log4j.MemoryAppender");
		// layouts have no effect on MemoryAppenders
		//props.put("log4j.appender.MEMORY.layout", "org.apache.log4j.PatternLayout");
		//props.put("log4j.appender.MEMORY.layout.ConversionPattern", "%d{dd/MM/yy HH:mm:ss.SSS} %-5p %c - %m%n");
		props.put("log4j.appender.MEMORY.MaximumLogSize", 100);
		PropertyConfigurator.configure(props);
		
		Logger logger = Logger.getLogger("testLogger");
		MemoryAppender memoryAppender = (MemoryAppender) Logger.getRootLogger().getAppender("MEMORY");
		List<LoggingEvent> loggingEvents;

		long start = System.currentTimeMillis();
		logger.info("info message");
		long end = System.currentTimeMillis();
		
		loggingEvents = memoryAppender.getLoggingEvents();
		assertTrue("info message in memoryAppender", loggingEvents.size()==1);
		assertEquals("info message", loggingEvents.get(0).getMessage());
		assertEquals("info message", loggingEvents.get(0).getRenderedMessage());
		assertNotNull(loggingEvents.get(0).getLoggerName());
		assertEquals(Level.INFO, loggingEvents.get(0).getLevel());
		// NB: timestamp field requires log4j 1.2.15
		assertTrue("timestamp of loggingEvent >= start", loggingEvents.get(0).getTimeStamp() >= start);
		assertTrue("timestamp of loggingEvent <= end", loggingEvents.get(0).getTimeStamp() <= end);
		
		logger.debug("debug message");
		loggingEvents = memoryAppender.getLoggingEvents();
		assertTrue("debug message suppressed from memoryAppender", loggingEvents.size()==1);
		
	}

	public void testMemoryAppenderViaObjectModel() {
		Logger logger = Logger.getLogger("testLogger");
		Logger.getRootLogger().setLevel(Level.INFO);
		MemoryAppender memoryAppender = new MemoryAppender();
		logger.removeAllAppenders();
		logger.addAppender(memoryAppender);
		
		List<LoggingEvent> loggingEvents;

		long start = System.currentTimeMillis();
		logger.info("info message");
		long end = System.currentTimeMillis();
		
		loggingEvents = memoryAppender.getLoggingEvents();
		assertTrue("info message in memoryAppender", loggingEvents.size()==1);
		assertEquals("info message", loggingEvents.get(0).getMessage());
		assertEquals("info message", loggingEvents.get(0).getRenderedMessage());
		assertNotNull(loggingEvents.get(0).getLoggerName());
		assertEquals(Level.INFO, loggingEvents.get(0).getLevel());
		// NB: timestamp field requires log4j 1.2.15
		assertTrue("timestamp of loggingEvent >= start", loggingEvents.get(0).getTimeStamp() >= start);
		assertTrue("timestamp of loggingEvent <= end", loggingEvents.get(0).getTimeStamp() <= end);
		
		logger.debug("debug message");
		loggingEvents = memoryAppender.getLoggingEvents();
		assertTrue("debug message suppressed from memoryAppender", loggingEvents.size()==1);
	}

	public void testLogWindowSize() {

		// create a MemoryLogger capable of holding 10 entries
		
		Properties props = new Properties();
		props.put("log4j.rootCategory", "INFO, MEMORY");
		props.put("log4j.appender.MEMORY", "com.randomnoun.common.log4j.MemoryAppender");
		props.put("log4j.appender.MEMORY.MaximumLogSize", "10");
		PropertyConfigurator.configure(props);
		
		Logger logger = Logger.getLogger("testLogger");
		MemoryAppender memoryAppender = (MemoryAppender) Logger.getRootLogger().getAppender("MEMORY");
		List<LoggingEvent> loggingEvents;

		// write five messages. 
		// the 0th message in MemoryAppender should be the most recent (i.e. message #5)
		for (int i=1; i<=5; i++) {
			logger.info("message number " + i);
		}
		loggingEvents = memoryAppender.getLoggingEvents();
		assertTrue("5 info messages in memoryAppender", loggingEvents.size()==5);
		for (int i=0; i<5; i++) {
			assertEquals("message number " + (5 - i), loggingEvents.get(i).getRenderedMessage());
		}
		
		// write another five messages
		for (int i=6; i<=10; i++) {
			logger.info("message number " + i);
		}
		loggingEvents = memoryAppender.getLoggingEvents();
		assertTrue("10 info messages in memoryAppender", loggingEvents.size()==10);
		for (int i=0; i<10; i++) {
			assertEquals("message number " + (10 - i), loggingEvents.get(i).getRenderedMessage());
		}
		
		// write another five messages (memoryAppender should just contain messages 6-15)
		for (int i=11; i<=15; i++) {
			logger.info("message number " + i);
		}
		loggingEvents = memoryAppender.getLoggingEvents();
		//System.out.println(loggingEvents.size());
		assertTrue("10 info messages in memoryAppender", loggingEvents.size()==10);
		for (int i=0; i<10; i++) {
			assertEquals("message number " + (15 - i), loggingEvents.get(i).getRenderedMessage());
		}
		
	}


}
