package com.randomnoun.common.log4j2;

/* (c) 2013 randomnoun. All Rights Reserved. This work is licensed under a
 * BSD Simplified License. (http://www.randomnoun.com/bsd-simplified.html)
 */

import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
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
public class MemoryAppenderTest
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
	
	public void testMemoryAppenderViaProperties_Log2Logger() {

		Properties props = new Properties();
		props.put("log4j.rootCategory", "INFO, MEMORY");
		props.put("log4j.appender.MEMORY", "com.randomnoun.common.log4j2.MemoryAppender");
		props.put("log4j.appender.MEMORY.MaximumLogSize", 100);
		
		// layouts have no effect on MemoryAppenders
		//props.put("log4j.appender.MEMORY.layout", "org.apache.log4j.PatternLayout");
		//props.put("log4j.appender.MEMORY.layout.ConversionPattern", "%d{dd/MM/yy HH:mm:ss.SSS} %-5p %c - %m%n");
		// PropertyConfigurator.configure(props);
		
		Log4j2CliConfiguration lcc = new Log4j2CliConfiguration();
		lcc.init("[MemoryAppenderTest]", props);
		
		Log4j1ConfigurationParser lcp = new Log4j1ConfigurationParser();
		ConfigurationBuilder<?> builder = lcp.buildConfigurationBuilder(props);
		LoggerContext ctx = Configurator.initialize(builder.build());

		org.apache.logging.log4j.Logger logger = LogManager.getLogger("testLogger");
		MemoryAppender memoryAppender = ctx.getConfiguration().getAppender("MEMORY");
		
		List<LogEvent> logEvents;

		long start = System.currentTimeMillis();
		logger.info("info message");
		long end = System.currentTimeMillis();
		
		logEvents = memoryAppender.getLogEvents();
		assertTrue("info message in memoryAppender", logEvents.size()==1);
		assertEquals("info message", logEvents.get(0).getMessage().getFormattedMessage());
		// assertEquals("info message", logEvents.get(0).getRenderedMessage());
		assertNotNull(logEvents.get(0).getLoggerName());
		assertEquals(Level.INFO, logEvents.get(0).getLevel());
		// NB: timestamp field requires log4j 1.2.15
		assertTrue("timestamp of loggingEvent >= start", logEvents.get(0).getTimeMillis() >= start);
		assertTrue("timestamp of loggingEvent <= end", logEvents.get(0).getTimeMillis() <= end);
		
		logger.debug("debug message");
		logEvents = memoryAppender.getLogEvents();
		assertTrue("debug message suppressed from memoryAppender", logEvents.size()==1);
		
	}

	// see https://logging.apache.org/log4j/2.x/manual/customconfig.html#AddingToCurrent
	
	public void testMemoryAppenderViaObjectModel() {
		Logger logger = Logger.getLogger("testLogger");
		
		
		LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        Configuration config = ctx.getConfiguration();
		config.getRootLogger().setLevel(Level.INFO);
		
		MemoryAppender memoryAppender = MemoryAppender.createAppender("Memory");
		memoryAppender.start();
        config.addAppender(memoryAppender);
		
        AppenderRef ref = AppenderRef.createAppenderRef("MEMORY", null, null);
        AppenderRef[] refs = new AppenderRef[] {ref};
        LoggerConfig loggerConfig = LoggerConfig.createLogger(false, Level.INFO, "testLogger", "true", refs, null, config, null );
        loggerConfig.addAppender(memoryAppender, null, null);
        config.addLogger("testLogger", loggerConfig);
        ctx.updateLoggers();
        
		List<LogEvent> logEvents;

		long start = System.currentTimeMillis();
		logger.info("info message");
		long end = System.currentTimeMillis();
		
		logEvents = memoryAppender.getLogEvents();
		assertTrue("info message in memoryAppender", logEvents.size()==1);
		assertEquals("info message", logEvents.get(0).getMessage().toString());
		assertEquals("info message", logEvents.get(0).getMessage().getFormattedMessage());
		assertNotNull(logEvents.get(0).getLoggerName());
		assertEquals(Level.INFO, logEvents.get(0).getLevel());
		assertTrue("timestamp of loggingEvent >= start", logEvents.get(0).getTimeMillis() >= start);
		assertTrue("timestamp of loggingEvent <= end", logEvents.get(0).getTimeMillis() <= end);
		
		logger.debug("debug message");
		logEvents = memoryAppender.getLogEvents();
		assertTrue("debug message suppressed from memoryAppender", logEvents.size()==1);
	}
	

	
	public void testLogWindowSize() {

		// create a MemoryLogger capable of holding 10 entries
		
		Properties props = new Properties();
		props.put("log4j.rootCategory", "INFO, MEMORY");
		props.put("log4j.appender.MEMORY", "com.randomnoun.common.log4j2.MemoryAppender");
		props.put("log4j.appender.MEMORY.MaximumLogSize", "10");
		// PropertyConfigurator.configure(props);
		Log4j2CliConfiguration lcc = new Log4j2CliConfiguration();
		lcc.init("[MemoryAppenderTest]", props);
		
		Log4j1ConfigurationParser lcp = new Log4j1ConfigurationParser();
		ConfigurationBuilder<?> builder = lcp.buildConfigurationBuilder(props);
		LoggerContext ctx = Configurator.initialize(builder.build());

		Logger logger = Logger.getLogger("testLogger");
		// MemoryAppender memoryAppender = (MemoryAppender) Logger.getRootLogger().getAppender("MEMORY");
		MemoryAppender memoryAppender = ctx.getConfiguration().getAppender("MEMORY");
		
		List<LogEvent> logEvents;

		// write five messages. 
		// the 0th message in MemoryAppender should be the most recent (i.e. message #5)
		for (int i=1; i<=5; i++) {
			logger.info("message number " + i);
		}
		logEvents = memoryAppender.getLogEvents();
		assertTrue("5 info messages in memoryAppender", logEvents.size()==5);
		for (int i=0; i<5; i++) {
			assertEquals("message number " + (5 - i), logEvents.get(i).getMessage().getFormattedMessage());
		}
		
		// write another five messages
		for (int i=6; i<=10; i++) {
			logger.info("message number " + i);
		}
		logEvents = memoryAppender.getLogEvents();
		assertTrue("10 info messages in memoryAppender", logEvents.size()==10);
		for (int i=0; i<10; i++) {
			assertEquals("message number " + (10 - i), logEvents.get(i).getMessage().getFormattedMessage());
		}
		
		// write another five messages (memoryAppender should just contain messages 6-15)
		for (int i=11; i<=15; i++) {
			logger.info("message number " + i);
		}
		logEvents = memoryAppender.getLogEvents();
		//System.out.println(loggingEvents.size());
		assertTrue("10 info messages in memoryAppender", logEvents.size()==10);
		for (int i=0; i<10; i++) {
			assertEquals("message number " + (15 - i), logEvents.get(i).getMessage().getFormattedMessage());
		}
		
	}


}
