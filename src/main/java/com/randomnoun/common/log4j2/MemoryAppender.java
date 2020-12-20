package com.randomnoun.common.log4j2;

import java.io.Writer;
import java.net.URL;

/* (c) 2013 randomnoun. All Rights Reserved. This work is licensed under a
 * BSD Simplified License. (http://www.randomnoun.com/bsd-simplified.html)
 */

import java.util.*;

import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.StringLayout;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.appender.HttpAppender;
import org.apache.logging.log4j.core.appender.HttpManager;
import org.apache.logging.log4j.core.appender.HttpURLConnectionManager;
import org.apache.logging.log4j.core.appender.ManagerFactory;
import org.apache.logging.log4j.core.appender.NullAppender;
import org.apache.logging.log4j.core.appender.WriterAppender;
import org.apache.logging.log4j.core.appender.WriterManager;
import org.apache.logging.log4j.core.appender.WriterAppender.Builder;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.net.ssl.SslConfiguration;
import org.apache.logging.log4j.core.util.CloseShieldWriter;


/**
 * Log4j2 appender to capture logging events in an in-memory List. 
 *
 * <p>Maybe log4j2 already has one. That'd be nice.
 * 
 * <p>Apparently not.
 *
 * <p>The code in this class is based on the WriterAppender class
 * in the log4j source code.
 * 
 * <p>This appender can be configured using the property "maximumLogSize" 
 * which limits the number of logging events captured by this class (old events
 * are popped off the list when the list becomes full).
 * 
 * <p>Use the {@link #getLoggingEvents()} to return the List of events written
 * to this class. This list is a <b>copy</b> of the list contained within this class,
 * so it can safely be iterated over even if logging events are still
 * occurring in an application.
 * 
 * <p>Example log4j configuration:
 * <pre class="code">
 * log4j.rootLogger=DEBUG, MEMORY
 * 
 * log4j.appender.MEMORY=com.randomnoun.common.log4j.MemoryAppender
 * log4j.appender.MEMORY.MaximumLogSize=1000
 * </pre>
 * 
 * You can then obtain the list of events via the code:
 * <pre>
 * MemoryAppender memoryAppender = (MemoryAppender) Logger.getRootLogger().getAppender("MEMORY");
 * List events = memoryAppender.getEvents();
 * </pre>
 *
 * @blog http://www.randomnoun.com/wp/2013/01/13/logging/
 * 
 * @author knoxg
 */
@Plugin(name = MemoryAppender.PLUGIN_NAME, category = Core.CATEGORY_NAME, elementType = Appender.ELEMENT_TYPE, printObject = true)
public class MemoryAppender extends AbstractAppender {

	public static final String PLUGIN_NAME = "Memory";
	
    public final static int DEFAULT_LOG_SIZE = 1000;
    private int maximumLogSize = DEFAULT_LOG_SIZE;
    private LinkedList<LogEvent> logEvents;

	
    /**
     * Builds HttpAppender instances.
     * @param <B> The type to build
     */
    // this builder appears to create another builder, which some would consider pretty strange
    public static class MemoryAppenderBuilder<B extends MemoryAppenderBuilder<B>> 
    	extends AbstractAppender.Builder<B>
        implements org.apache.logging.log4j.core.util.Builder<MemoryAppender> {


        @PluginBuilderAttribute
        private int maximumLogSize = 1000;

        @Override
        public MemoryAppender build() {
        	MemoryAppender ma = new MemoryAppender(getName());
        	ma.setMaximumLogSize(maximumLogSize);
        	return ma;
        }

        public int getMaximumLogSize() {
            return maximumLogSize;
        }

        public B setMaximumLogSize(final int maximumLogSize) {
            this.maximumLogSize = maximumLogSize;
            return asBuilder();
        }
    }

    /**
     * @return a builder for a MemoryAppender.
     */
    @PluginBuilderFactory
    public static <B extends MemoryAppenderBuilder<B>> B newBuilder() {
        return new MemoryAppenderBuilder<B>().asBuilder();
    }
    
    // well log4j2 went all-in on the annotations, didn't they
    
	@PluginFactory
	public static MemoryAppender createAppender(
		@PluginAttribute(value = "name", defaultString = "null") final String name) 
	{
		return new MemoryAppender(name);
	}

	private MemoryAppender(final String name) {
		super(name, null, null, true, Property.EMPTY_ARRAY);
		logEvents = new LinkedList<LogEvent>();
	}
	
    /** Set the maximum log size */
    public void setMaximumLogSize(int logSize)
    {
        this.maximumLogSize = logSize;
    }

    /** Return the maximum log size */
    public int getMaximumLogSize()
    {
        return maximumLogSize;
    }
    

	@Override
	public void append(final LogEvent event) {
		synchronized(logEvents) {
	        if (logEvents.size() >= maximumLogSize) {
	        	logEvents.removeLast();
	        }
	        logEvents.addFirst(event);
        }
	}
    
	/** Returns a list of logging events captured by this appender. (This list 
     *  is cloned in order to prevent ConcurrentModificationExceptions occuring
     *  whilst iterating through it) */
    public List<LogEvent> getLogEvents()
    {
        return new ArrayList<LogEvent>(logEvents);
    }
    
}
