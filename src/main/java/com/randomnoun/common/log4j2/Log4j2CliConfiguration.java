package com.randomnoun.common.log4j2;

import java.util.Properties;

import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;

/** Configures log4j for command-line interface programs.
 * 
 * <p>This class configured the log4j2 (two!) framework, using log4j1 (one!) properties.
 * 
 * <p>It uses a modified Log4j1ConfigurationParser (from the log4j2 framework) to do this, 
 * which maps some well-known log4j1 appenders to log4j2 appenders, and does a pretty ham-fisted job of everything else.
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
 * @blog http://www.randomnoun.com/wp/2013/01/13/logging/
 * @author knoxg
 * 
 */
public class Log4j2CliConfiguration {
    
	/** Create a new Log4jCli2Configuration instance */
	public Log4j2CliConfiguration() {
	}
	
	/** Initialise log4j.
	 *
	 * <p>The properties file supplied is in log4j format.
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
		
		// log4j1 class names - the Log4j1ConfigurationParser recognises a limited set of these
		// and converts them into the log4j2 equivalents. 
		// Unknown appenders are ignored, which is obviously what people want to happen.
		props.put("log4j.appender.CONSOLE", "org.apache.log4j.ConsoleAppender");
		props.put("log4j.appender.CONSOLE.layout", "org.apache.log4j.PatternLayout");

		// log4j2 class names - the Log4j1ConfigurationParser doesn't recognise these
		// props.put("log4j.appender.CONSOLE", "org.apache.logging.log4j.core.appender.ConsoleAppender");
		// props.put("log4j.appender.CONSOLE.layout", "org.apache.logging.log4j.core.layout.PatternLayout");
		props.put("log4j.appender.CONSOLE.layout.ConversionPattern", logFormatPrefix + "%d{ABSOLUTE} %-5p %c - %m%n");
		props.put("log4j.logger.org.springframework", "INFO"); // since Spring is a bit too verbose for my liking at DEBUG level
		if (override!=null) { 
			props.putAll(override);
		}

		/*
		// to use the bundled Log41ConfigurationParser, had to do this:
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			props.store(baos,  null);
			ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
			
			// in log4j-1.2.jar 
			// Log4j1ConfigurationParser lcp = new Log4j1ConfigurationParser();
			
			Log4j1ConfigurationParser lcp = new Log4j1ConfigurationParser();
			ConfigurationBuilder<?> builder = lcp.buildConfigurationBuilder(bais);
		} catch (IOException e) {
			throw new IllegalStateException("IOException in log4j initiialisation", e);
		}
		*/

		// our Log4j1ConfigurationParser can take a Properties object instead
		Log4j1ConfigurationParser lcp = new Log4j1ConfigurationParser();
		ConfigurationBuilder<?> builder = lcp.buildConfigurationBuilder(props);
		
		// this needs to happen before Configurator.initialize()
		// LogManager.shutdown();
		Configuration config = builder.build();

		// so this doesn't work, because the (ctx.getState() == LifeCycle.State.INITIALIZED)) condition
		// in Log4jContextFactory.getContext() prevents the configuration from taking effect 
		/*LoggerContext ctx; =*/ // Configurator.initialize(config);
		
		// so you need to do this,
		// which is not what https://logging.apache.org/log4j/2.x/manual/customconfig.html 
		// fucking tells you to do. This took about two hours to figure out.
		/*LoggerContext ctx; =*/  Configurator.reconfigure(config);

		
		// incidentally, did you know that every time you create a Logger, it's introspecting the stack for no good reason ?
		// *slow hand clap*, log4j.
		
		
		
		// log4j doesn't have ThrowableRenderers, it has ExtendedStackTraceElements instead
		// which I can't find a working example of anywhere
		/*
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
        */
	}
}


