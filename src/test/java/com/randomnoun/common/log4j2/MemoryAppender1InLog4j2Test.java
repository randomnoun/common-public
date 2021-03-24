package com.randomnoun.common.log4j2;

/* (c) 2013 randomnoun. All Rights Reserved. This work is licensed under a
 * BSD Simplified License. (http://www.randomnoun.com/bsd-simplified.html)
 */

import java.util.List;
import java.util.Properties;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;

import com.randomnoun.common.log4j.MemoryAppender;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;

import junit.framework.TestCase;

/**
 * Unit test for the log4j2 MemoryAppender 
 * 
 * @blog http://www.randomnoun.com/wp/2013/01/13/logging/
 * 
 * @author knoxg
 */
public class MemoryAppender1InLog4j2Test
	extends TestCase
{

	/** Reset log4j before each test
	 */
	protected void setUp() {
		LogManager.shutdown();
	}
	
	/**
	 */
	protected void tearDown()
	{
	}
	
	
	public void testNothing() {
	
	}
	
	/* this fails as log4j2 doesn't preserve the log4j1 timestamps. will move this to a separate project later
	public void testMemoryAppenderViaProperties_Log1Logger() {

		Properties props = new Properties();
		props.put("log4j.rootCategory", "INFO, MEMORY");
		props.put("log4j.appender.MEMORY", "com.randomnoun.common.log4j.MemoryAppender");
		// layouts have no effect on MemoryAppenders
		//props.put("log4j.appender.MEMORY.layout", "org.apache.log4j.PatternLayout");
		//props.put("log4j.appender.MEMORY.layout.ConversionPattern", "%d{dd/MM/yy HH:mm:ss.SSS} %-5p %c - %m%n");
		
		props.put("log4j.appender.MEMORY.MaximumLogSize", "100");
		
		// PropertyConfigurator.configure(props);
		
		Log4j1ConfigurationParser lcp = new Log4j1ConfigurationParser();
		ConfigurationBuilder<?> builder = lcp.buildConfigurationBuilder(props);
		LoggerContext ctx = Configurator.initialize(builder.build());

		Logger logger = Logger.getLogger("testLogger");
		// MemoryAppender memoryAppender = (MemoryAppender) Logger.getRootLogger().getAppender("MEMORY");
		// this will be some kind of adapter now, no doubt
		Log4j1WrapperAppender wrapperAppender = ctx.getConfiguration().getAppender("MEMORY");
		MemoryAppender memoryAppender = (MemoryAppender) wrapperAppender.getLog4j1Appender();

		List<LoggingEvent> logEvents;

		long start = System.currentTimeMillis();
		logger.info("info message");
		long end = System.currentTimeMillis();
		
		logEvents = memoryAppender.getLoggingEvents();
		assertTrue("info message in memoryAppender", logEvents.size()==1);
		assertEquals("info message", logEvents.get(0).getRenderedMessage());
		// assertEquals("info message", logEvents.get(0).getRenderedMessage());
		assertNotNull(logEvents.get(0).getLoggerName());
		assertEquals(Level.INFO, logEvents.get(0).getLevel());
		// NB: timestamp field requires log4j 1.2.15
		assertTrue("timestamp of loggingEvent >= start", logEvents.get(0).getTimeStamp() >= start);
		assertTrue("timestamp of loggingEvent <= end", logEvents.get(0).getTimeStamp() <= end);
		
		logger.debug("debug message");
		logEvents = memoryAppender.getLoggingEvents();
		assertTrue("debug message suppressed from memoryAppender", logEvents.size()==1);
		
	}

	// see https://logging.apache.org/log4j/2.x/manual/customconfig.html#AddingToCurrent
	
	public void testMemoryAppenderViaObjectModel() {
		Logger logger = Logger.getLogger("testLogger");
		
		
		LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        Configuration config = ctx.getConfiguration();
        
		config.getRootLogger().setLevel(org.apache.logging.log4j.Level.INFO);
		
		// one of those adapter thingies
		MemoryAppender log4j1MemoryAppender = new MemoryAppender(1000);
		// Log4j1WrapperAppender memoryAppender = Log4j1WrapperAppender.createAppender("Memory", log4j1MemoryAppender);  
		
		Log4j1WrapperAppender memoryAppender = Log4j1WrapperAppender.createAppender("Memory", log4j1MemoryAppender);  
		
		// MemoryAppender memoryAppender = MemoryAppender.createAppender("Memory");
		// memoryAppender.start();
        // config.addAppender(memoryAppender);
        
		
        AppenderRef ref = AppenderRef.createAppenderRef("MEMORY", null, null);
        AppenderRef[] refs = new AppenderRef[] {ref};
        LoggerConfig loggerConfig = LoggerConfig.createLogger(false, org.apache.logging.log4j.Level.INFO, "testLogger", "true", refs, null, config, null );
        loggerConfig.addAppender(memoryAppender, null, null);
        config.addLogger("testLogger", loggerConfig);
        ctx.updateLoggers();
        
		List<LoggingEvent> logEvents;

		long start = System.currentTimeMillis();
		logger.info("info message");
		long end = System.currentTimeMillis();
		
		logEvents = log4j1MemoryAppender.getLoggingEvents();
		assertTrue("info message in memoryAppender", logEvents.size()==1);
		assertEquals("info message", logEvents.get(0).getMessage().toString());
		assertEquals("info message", logEvents.get(0).getRenderedMessage());
		assertNotNull(logEvents.get(0).getLoggerName());
		assertEquals(Level.INFO, logEvents.get(0).getLevel());
		
		// the LoggingEvent class provided in the log4j-1.2-api jar 
		// has a 'final' implementation of getTimeStamp() which returns 0, 
		// so the LogEventAdapter class also returns zero.
		// I mean, why would anyone want to know when a log message was generated
		// Surprised they didn't have a final getMessage() that returned null as well,
		// which would make logging all that more informative.
		
		// anyway, I've created my own LogEvent class that actually stores the TimeStamp, this will need to be 
		// include ahead of log4j-1.2-api to take effect.
		System.out.println(logEvents.get(0).getTimeStamp()); // was zero
		System.out.println(start);
		assertTrue("timestamp of loggingEvent >= start", logEvents.get(0).getTimeStamp() >= start);
		assertTrue("timestamp of loggingEvent <= end", logEvents.get(0).getTimeStamp() <= end);
		
		
		logger.debug("debug message");
		logEvents = log4j1MemoryAppender.getLoggingEvents();
		assertTrue("debug message suppressed from memoryAppender", logEvents.size()==1);
	}
	

	
	public void testLogWindowSize() {

		// create a MemoryLogger capable of holding 10 entries
		
		Properties props = new Properties();
		props.put("log4j.rootCategory", "INFO, MEMORY");
		props.put("log4j.appender.MEMORY", "com.randomnoun.common.log4j.MemoryAppender");
		props.put("log4j.appender.MEMORY.MaximumLogSize", "10");
		// PropertyConfigurator.configure(props);
		

		Log4j1ConfigurationParser lcp = new Log4j1ConfigurationParser();
		ConfigurationBuilder<?> builder = lcp.buildConfigurationBuilder(props);
		LoggerContext ctx = Configurator.initialize(builder.build());

		Logger logger = Logger.getLogger("testLogger");
		Log4j1WrapperAppender wrapperAppender = ctx.getConfiguration().getAppender("MEMORY");
		MemoryAppender memoryAppender = (MemoryAppender) wrapperAppender.getLog4j1Appender();
		
		List<LoggingEvent> logEvents;

		// write five messages. 
		// the 0th message in MemoryAppender should be the most recent (i.e. message #5)
		for (int i=1; i<=5; i++) {
			logger.info("message number " + i);
		}
		logEvents = memoryAppender.getLoggingEvents();
		assertTrue("5 info messages in memoryAppender", logEvents.size()==5);
		for (int i=0; i<5; i++) {
			assertEquals("message number " + (5 - i), logEvents.get(i).getRenderedMessage());
		}
		
		// write another five messages
		for (int i=6; i<=10; i++) {
			logger.info("message number " + i);
		}
		logEvents = memoryAppender.getLoggingEvents();
		assertTrue("10 info messages in memoryAppender", logEvents.size()==10);
		for (int i=0; i<10; i++) {
			assertEquals("message number " + (10 - i), logEvents.get(i).getRenderedMessage());
		}
		
		// write another five messages (memoryAppender should just contain messages 6-15)
		for (int i=11; i<=15; i++) {
			logger.info("message number " + i);
		}
		logEvents = memoryAppender.getLoggingEvents();
		//System.out.println(loggingEvents.size());
		assertTrue("10 info messages in memoryAppender", logEvents.size()==10);
		for (int i=0; i<10; i++) {
			assertEquals("message number " + (15 - i), logEvents.get(i).getRenderedMessage());
		}
		
	}
	*/


}
