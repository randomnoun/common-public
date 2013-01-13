package com.randomnoun.common.log4j;

/* (c) 2013 randomnoun. All Rights Reserved. This work is licensed under a
 * Creative Commons Attribution 3.0 Unported License. (http://creativecommons.org/licenses/by/3.0/)
 */

import java.util.Properties;

import org.apache.log4j.PropertyConfigurator;

/** Configures log4j for command-line interface programs.
 * 
 * <p>By default, everything's sent to stdout, using the following log4j initilisation properties:
 * 
 * <pre>
log4j.rootCategory=DEBUG, CONSOLE
log4j.appender.CONSOLE=org.apache.log4j.ConsoleAppender
log4j.appender.CONSOLE.layout=org.apache.log4j.PatternLayout
log4j.appender.CONSOLE.layout.ConversionPattern=%d{ABSOLUTE} %-5p %c - %m %n

log4j.logger.org.springframework=INFO

# log4j.appender.FILE=com.randomnoun.common.log4j.CustomRollingFileAppender
# log4j.appender.FILE.File=c:\\eomail.log
# log4j.appender.FILE.MaxBackupIndex=100
# log4j.appender.FILE.layout=org.apache.log4j.PatternLayout
# log4j.appender.FILE.layout.ConversionPattern=%d{dd/MM/yy HH:mm:ss.SSS} %-5p %c - %m %n
   </pre>
 * 
 * <p>with an optional line prefix before the <code>%d{ABSOLUTE}</code> in the ConversionPattern.
 * 
 * @see http://logging.apache.org/log4j/1.2/manual.html
 * 
 * <p>TODO: Support XML-based configurators
 * 
 * @blog http://www.randomnoun.com/wp/2013/01/13/logging/
 * @author knoxg
 * @version $Id$
 */
public class Log4jCliConfiguration {
    /** A revision marker to be used in exception stack traces. */
    public static final String _revision = "$Id$";

	/** Create a new Log4jCliConfiguration instance */
	public Log4jCliConfiguration() {
	}
	
	/** Initialise log4j.
	 * 
	 * @param logFormatPrefix a string prefixed to each log. Useful for program identifiers;
	 *   e.g. "[programName] "
	 * @param override if non-null, additional log4j properties. Any properties contained in 
	 *   this object will override the defaults.
	 * 
	 */
	public void init(String logFormatPrefix, Properties override) {
		
		if (logFormatPrefix==null) { 
			logFormatPrefix = ""; 
		} else {
			logFormatPrefix += " ";
		}
		
		Properties lp = new Properties();
		lp.put("log4j.rootCategory", "DEBUG, CONSOLE");
		lp.put("log4j.appender.CONSOLE", "org.apache.log4j.ConsoleAppender");
		lp.put("log4j.appender.CONSOLE.layout", "org.apache.log4j.PatternLayout");
		lp.put("log4j.appender.CONSOLE.layout.ConversionPattern", logFormatPrefix + "%d{ABSOLUTE} %-5p %c - %m%n");
		
		lp.put("log4j.logger.org.springframework", "INFO"); // since Spring is a bit too verbose for my liking at DEBUG level
		
		/*
		lp.put("log4j.appender.FILE", "com.randomnoun.common.log4j.CustomRollingFileAppender");
		lp.put("log4j.appender.FILE.File", "c:\\eomail.log");
		lp.put("log4j.appender.FILE.MaxBackupIndex", "100");
		lp.put("log4j.appender.FILE.layout", "org.apache.log4j.PatternLayout");
		lp.put("log4j.appender.FILE.layout.ConversionPattern", "%d{dd/MM/yy HH:mm:ss.SSS} %-5p %c - %m %n");
		*/
		
		
		if (override!=null) { lp.putAll(override); }
		PropertyConfigurator.configure(lp);
	}
}


