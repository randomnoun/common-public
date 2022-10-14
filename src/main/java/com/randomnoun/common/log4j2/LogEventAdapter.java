/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */
package com.randomnoun.common.log4j2;

import org.apache.log4j.Category;
import org.apache.log4j.Level;
import org.apache.log4j.spi.LocationInfo;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.ThrowableInformation;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.spi.StandardLevel;

// a modified form of the LogEventAdapter in log4j2, which uses the log4j 1 LoggingEvent class

/**
 * Converts a Log4j 2 LogEvent into the components needed by a Log4j 1.x LoggingEvent.
 * This class requires Log4j 2.
 */
public class LogEventAdapter extends LoggingEvent {

    /** generated serialVersionUID */
	private static final long serialVersionUID = -1455395715714529922L;

	public LogEventAdapter(LogEvent event) {
    	super(event.getLoggerFqcn(), Category.getInstance(event.getLoggerName()), 
    			event.getTimeMillis(), getLevel(event), event.getMessage(), event.getThreadName(), 
    			event.getThrown() == null ? null : new ThrowableInformation(event.getThrown()),
    			null, new LocationInfo(event.getThrown(), event.getLoggerFqcn()), event.getContextData().toMap()); // ?
        // this.event = event;
    }

    /**
     * Return the level of this event. Use this form instead of directly
     * accessing the <code>level</code> field.
     */
    public static Level getLevel(LogEvent event) {
        switch (StandardLevel.getStandardLevel(event.getLevel().intLevel())) {
            case TRACE:
                return Level.TRACE;
            case DEBUG:
                return Level.DEBUG;
            case INFO:
                return Level.INFO;
            case WARN:
                return Level.WARN;
            case FATAL:
                return Level.FATAL;
            case OFF:
                return Level.OFF;
            case ALL:
                return Level.ALL;
            default:
                return Level.ERROR;
        }
    }
}
