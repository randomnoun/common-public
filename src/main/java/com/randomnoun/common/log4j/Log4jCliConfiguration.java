package com.randomnoun.common.log4j;

/* (c) 2013 randomnoun. All Rights Reserved. This work is licensed under a
 * BSD Simplified License. (http://www.randomnoun.com/bsd-simplified.html)
 */

import java.util.Properties;

import org.apache.log4j.LogManager;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.spi.LoggerRepository;
import org.apache.log4j.spi.ThrowableRendererSupport;

/** Configures log4j for command-line interface programs.
 * 
 * <p>By default, everything's sent to stdout, using the following log4j initialisation properties:
 * 
 * <pre>
log4j.rootCategory=DEBUG, CONSOLE
log4j.appender.CONSOLE=org.apache.log4j.ConsoleAppender
log4j.appender.CONSOLE.layout=org.apache.log4j.PatternLayout
log4j.appender.CONSOLE.layout.ConversionPattern=%d{ABSOLUTE} %-5p %c - %m %n

log4j.logger.org.springframework=INFO

# log4j.appender.FILE=com.randomnoun.common.log4j.CustomRollingFileAppender
# log4j.appender.FILE.File=c:\\another.log
# log4j.appender.FILE.MaxBackupIndex=100
# log4j.appender.FILE.layout=org.apache.log4j.PatternLayout
# log4j.appender.FILE.layout.ConversionPattern=%d{dd/MM/yy HH:mm:ss.SSS} %-5p %c - %m %n
   </pre>
 * 
 * <p>with an optional line prefix before the <code>%d{ABSOLUTE}</code> in the ConversionPattern.
 * 
 * @see http://logging.apache.org/log4j/1.2/manual.html
 * 
 * @TODO Support XML-based configurators
 * 
 * @blog http://www.randomnoun.com/wp/2013/01/13/logging/
 * @author knoxg
 * @version $Id$
 */
public class Log4jCliConfiguration {
    
    

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
		
		Properties props = new Properties();
		props.put("log4j.rootCategory", "DEBUG, CONSOLE");
		props.put("log4j.appender.CONSOLE", "org.apache.log4j.ConsoleAppender");
		props.put("log4j.appender.CONSOLE.layout", "org.apache.log4j.PatternLayout");
		props.put("log4j.appender.CONSOLE.layout.ConversionPattern", logFormatPrefix + "%d{ABSOLUTE} %-5p %c - %m%n");
		props.put("log4j.logger.org.springframework", "INFO"); // since Spring is a bit too verbose for my liking at DEBUG level
		if (override!=null) { 
			props.putAll(override);
		}
		String highlightPrefix = props.getProperty("log4j.revisionedThrowableRenderer.highlightPrefix");
		if (highlightPrefix != null) {
			LoggerRepository repo = LogManager.getLoggerRepository();
			if (repo instanceof ThrowableRendererSupport) {
	            // if null, log4j will use a DefaultThrowableRenderer
	            // ThrowableRenderer renderer = ((ThrowableRendererSupport) repo).getThrowableRenderer();
	            ((ThrowableRendererSupport) repo).setThrowableRenderer(new RevisionedThrowableRenderer(highlightPrefix));
	        }
		}
		
		PropertyConfigurator.configure(props);
	}
}


