package com.randomnoun.common.log4j;

/* (c) 2013 randomnoun. All Rights Reserved. This work is licensed under a
 * BSD Simplified License. (http://www.randomnoun.com/bsd-simplified.html)
 */

import java.util.*;

import org.apache.log4j.*;
import org.apache.log4j.spi.LoggingEvent;

/**
 * Log4j appender to capture logging events in an in-memory List. 
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
public class MemoryAppender
    extends AppenderSkeleton
{
    
    
    
    public final static long DEFAULT_LOG_SIZE = 1000;
    private long maximumLogSize = DEFAULT_LOG_SIZE;
    private LinkedList<LoggingEvent> loggingEvents;

    /** Create a new MemoryAppender object */
    public MemoryAppender()
    {
        // this should be a LinkList since we use this data structure as a queue
        loggingEvents = new LinkedList<LoggingEvent>();
    }

    /** Create a new MemoryAppender with the specified log size
     * 
     * @param logSize The maximum number of logging events to store in this class 
     */ 
    public MemoryAppender(int logSize)
    {
        this.maximumLogSize = logSize;
        // this should be a LinkList since we use this data structure as a queue
        loggingEvents = new LinkedList<LoggingEvent>();
    }

    /** Immediate flush is always set to true, regardless of how
     *  this logger is configured. 
     *
     * @param value ignored
     */
    public void setImmediateFlush(boolean value)
    {
        // this method does nothing
    }

    /** Returns value of the <b>ImmediateFlush</b> option. */
    public boolean getImmediateFlush()
    {
        return true;
    }

    /** Set the maximum log size */
    public void setMaximumLogSize(long logSize)
    {
        this.maximumLogSize = logSize;
    }

    /** Return the maximum log size */
    public long getMaximumLogSize()
    {
        return maximumLogSize;
    }

    /** This method does nothing. 
     */
    public void activateOptions()
    {
    }

    /**
       This method is called by the {@link AppenderSkeleton#doAppend}
       method.
       <p>If the output stream exists and is writable then write a log
       statement to the output stream. Otherwise, write a single warning
       message to <code>System.err</code>.
       <p>The format of the output will depend on this appender's
       layout.
     */
    public void append(LoggingEvent event)
    {
        // Reminder: the nesting of calls is:
        //
        //    doAppend()
        //      - check threshold
        //      - filter
        //      - append();
        //        - checkEntryConditions();
        //        - subAppend();
        if (!checkEntryConditions())
        {
            return;
        }

        subAppend(event);
    }

    /**
       This method determines if there is a sense in attempting to append.
       <p>It checks whether there is a set output target and also if
       there is a set layout. If these checks fail, then the boolean
       value <code>false</code> is returned. 
       
       <p>This method always returns true; which I guess means we can't have
       thresholds set on our MemoryAppender.
     */
    protected boolean checkEntryConditions()
    {
        /*
           if (this.layout == null)
           {
               errorHandler.error("No layout set for the appender named [" + name + "].");
               return false;
           } */
        return true;
    }

    /** Close this appender instance. The event log list is cleared by this method. 
     * This method is identical to calling clear()
     */
    public synchronized void close()
    {
        loggingEvents.clear();
    }
    
    /** Clear all events from this appender. */
    public synchronized void clear() 
    {
		synchronized(loggingEvents) {
			loggingEvents.clear();
		}
    }

    /** Actual appending occurs here. */
    protected void subAppend(LoggingEvent event)
    {
        // this.qw.write(this.layout.format(event));
        synchronized(loggingEvents) {
	        if (loggingEvents.size() >= maximumLogSize)
	        {
	            loggingEvents.removeLast();
	        }
	
	        loggingEvents.addFirst(event);
        }
    }

    /**
       The MemoryAppender does not require a layout. Hence, this method returns
       <code>false</code>.
     */
    public boolean requiresLayout()
    {
        return false;
    }

    /** Returns a list of logging events captured by this appender. (This list 
     *  is cloned in order to prevent ConcurrentModificationExceptions occuring
     *  whilst iterating through it) */
    public List<LoggingEvent> getLoggingEvents()
    {
        return new ArrayList<LoggingEvent>(loggingEvents);
    }
}
