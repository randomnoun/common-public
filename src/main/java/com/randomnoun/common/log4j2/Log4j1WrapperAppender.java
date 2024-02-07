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

import java.io.IOException;
import java.io.StringReader;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Layout;
import org.apache.log4j.bridge.LogEventAdapter; // need to use our own adapter
import org.apache.log4j.config.PropertySetter;
import org.apache.log4j.helpers.OptionConverter;
import org.apache.log4j.spi.Filter;
// import org.apache.log4j.Appender;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.LoaderUtil;

/**
 * Binds a Log4j 1.x Appender to Log4j 2.
 */
@Plugin(name = Log4j1WrapperAppender.PLUGIN_NAME, category = Core.CATEGORY_NAME, elementType = Appender.ELEMENT_TYPE, printObject = true)
public class Log4j1WrapperAppender extends AbstractAppender {

	// the log4j1 appender
	org.apache.log4j.Appender log4j1Appender;
	
	public static final String PLUGIN_NAME = "Log4j1Wrapper";
	
	/**
     * Builds HttpAppender instances.
     * @param <B> The type to build
     */
    public static class Log4j1WrapperBuilder<B extends Log4j1WrapperBuilder<B>> extends AbstractAppender.Builder<B>
            implements org.apache.logging.log4j.core.util.Builder<Log4j1WrapperAppender> {


    	// so even though this attribute is an object, somewhere in the bowels of this fucking
    	// builder constructor strategy pattern it's converting it into a string.
    	
        @PluginBuilderAttribute
        private org.apache.log4j.Appender log4j1Appender = null;

        @Override
        public Log4j1WrapperAppender build() {
        	Log4j1WrapperAppender lw = new Log4j1WrapperAppender(getName(), log4j1Appender);
        	return lw;
        }

        public org.apache.log4j.Appender getLog4j1Appender() {
            return log4j1Appender;
        }

        public B setLog4j1Appender(final org.apache.log4j.Appender log4j1Appender) {
            this.log4j1Appender = log4j1Appender;
            return asBuilder();
        }
    }
    
    /**
     * @return a builder for a MemoryAppender.
     */
    @PluginBuilderFactory
    public static <B extends Log4j1WrapperBuilder<B>> B newBuilder() {
        return new Log4j1WrapperBuilder<B>().asBuilder();
    }
    

    
    // well log4j2 went all-in on the annotations, didn't they
    
	@PluginFactory
	public static Log4j1WrapperAppender createAppender(
		@PluginAttribute(value = "name", defaultString = "null") final String name,
		@PluginAttribute(value = "className", defaultString = "null") final String className,
		@PluginAttribute(value = "properties", defaultString = "null") final String propertiesString )
	{
		Log4j1WrapperAppender lw =  new Log4j1WrapperAppender(name, className, propertiesString ); 
		return lw;
	}

	public static Log4j1WrapperAppender createAppender(final String name, final org.apache.log4j.Appender log4j1Appender) 
	{
		Log4j1WrapperAppender lw =  new Log4j1WrapperAppender(name, log4j1Appender); 
		return lw;
	}

	
	/*
    protected Log4j1WrapperAppender(final String name, final Filter filter, final Layout<? extends Serializable> layout,
        final boolean ignoreExceptions, final Property[] properties) {
        super(name, filter, layout, ignoreExceptions, properties);
    }
    */
	
	private Log4j1WrapperAppender(final String name, final org.apache.log4j.Appender log4j1Appender) {
		super(name, null, null, true, Property.EMPTY_ARRAY);
		this.log4j1Appender = log4j1Appender;
	}

	private Log4j1WrapperAppender(final String name, final String className, final String propertiesString) {
		super(name, null, null, true, Property.EMPTY_ARRAY);

		// org.apache.log4j.Appender log4j1Appender
		Properties properties = new Properties();
		try {
			properties.load(new StringReader(propertiesString));
		} catch (IOException e) {
			throw new IllegalStateException("IOException in StringReader");
		}
		
        final String prefix = "log4j.appender." + name;
        final String layoutPrefix = "log4j.appender." + name + ".layout";
        final String filterPrefix = "log4j.appender." + name + ".filter.";
        this.log4j1Appender = buildLog4j1Appender(name, className, prefix, layoutPrefix, filterPrefix, properties);
	}
	

    @Override
    public void append(LogEvent event) {
    	log4j1Appender.doAppend(new LogEventAdapter(event));
    }

    @Override
    public void stop() {
    	log4j1Appender.close();
    }

    public org.apache.log4j.Appender getLog4j1Appender() {
        return log4j1Appender;
    }
    

    private org.apache.log4j.Appender buildLog4j1Appender(final String appenderName, final String className, final String prefix,
            final String layoutPrefix, final String filterPrefix, final Properties props) {
    	org.apache.log4j.Appender appender = newInstanceOf(className, "Appender");
        if (appender == null) {
            return null;
        }
        appender.setName(appenderName);
        appender.setLayout(parseLayout(layoutPrefix, appenderName, props));
        final String errorHandlerPrefix = prefix + ".errorhandler";
        // String errorHandlerClass = OptionConverter.findAndSubst(errorHandlerPrefix, props);
        String errorHandlerClass = props.getProperty(errorHandlerPrefix);
        if (errorHandlerClass != null) {
        	org.apache.log4j.spi.ErrorHandler eh = parseErrorHandler(props, errorHandlerPrefix, errorHandlerClass, appender);
            if (eh != null) {
                appender.setErrorHandler(eh);
            }
        }
        parseAppenderFilters(props, filterPrefix, appenderName);
        String[] keys = new String[] {
                layoutPrefix,
        };
        addProperties(appender, keys, props, prefix);
        return appender;
    }
    
    private <T> T newInstanceOf(String className, String type) {
        try {
            return LoaderUtil.newInstanceOf(className);
        } catch (Exception ex) {
        	StatusLogger.getLogger().warn("Log4j1WrapperAppender: Unable to create " + type + " " + className + " due to " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
            return null;
        }
    }
    
    public Layout parseLayout(String layoutPrefix, String appenderName, Properties props) {
        // String layoutClass = OptionConverter.findAndSubst(layoutPrefix, props);
    	String layoutClass = props.getProperty(layoutPrefix);
        if (layoutClass == null) {
            return null;
        }
        Layout layout = buildLayout(layoutPrefix, layoutClass, appenderName, props);
        return layout;
    }

    private Layout buildLayout(String layoutPrefix, String className, String appenderName, Properties props) {
        Layout layout = newInstanceOf(className, "Layout");
        if (layout == null) {
            return null;
        }
        PropertySetter.setProperties(layout, props, layoutPrefix + ".");
        return layout;
    }

    public org.apache.log4j.spi.ErrorHandler parseErrorHandler(final Properties props, final String errorHandlerPrefix,
            final String errorHandlerClass, final  org.apache.log4j.Appender appender) {
    	org.apache.log4j.spi.ErrorHandler eh = newInstanceOf(errorHandlerClass, "ErrorHandler");
        final String[] keys = new String[] {
                errorHandlerPrefix + ".root-ref",
                errorHandlerPrefix + ".logger-ref",
                errorHandlerPrefix + ".appender-ref"
        };
        addProperties(eh, keys, props, errorHandlerPrefix);
        return eh;
    }

    public void addProperties(final Object obj, final String[] keys, final Properties props, final String prefix) {
        final Properties edited = new Properties();
        for (String name : props.stringPropertyNames()) {
        	boolean filter = false;
        	if (name.startsWith(prefix)) {
        		filter = true;
        		for (String key : keys) {
                    if (name.equals(key)) {
                        filter = false;
                    }
                }
        	} else {
        		filter = false;
        	}
        	if (filter) {
        		edited.put(name, props.getProperty(name));
        	}
        }
        PropertySetter.setProperties(obj, edited, prefix + ".");
    }
    
    public Filter parseAppenderFilters(Properties props, String filterPrefix, String appenderName) {
        // extract filters and filter options from props into a hashtable mapping
        // the property name defining the filter class to a list of pre-parsed
        // name-value pairs associated to that filter

    	// so instead of List<NameValue>, I'm usnig a properties object 
        int fIdx = filterPrefix.length();
        SortedMap<String, Properties> filters = new TreeMap<String, Properties>();
        Enumeration e = props.keys();
        String name = "";
        while (e.hasMoreElements()) {
            String key = (String) e.nextElement();
            if (key.startsWith(filterPrefix)) {
                int dotIdx = key.indexOf('.', fIdx);
                String filterKey = key;
                if (dotIdx != -1) {
                    filterKey = key.substring(0, dotIdx);
                    name = key.substring(dotIdx + 1);
                }
                // Properties filterOpts = filters.computeIfAbsent(filterKey, k -> new ArrayList<>());
                Properties filterOpts = filters.get(filterKey);
                if (filterOpts == null) {
                	filterOpts = new Properties();
                	filters.put(filterKey,  filterOpts);
                }
                
                if (dotIdx != -1) {
                    String value = OptionConverter.findAndSubst(key, props);
                    filterOpts.put(name, value);
                }
            }
        }

        Filter head = null;
        Filter next = null;
        for (Map.Entry<String, Properties> entry : filters.entrySet()) {
            String clazz = props.getProperty(entry.getKey());
            Filter filter = null;
            if (clazz != null) {
                filter = buildFilter(clazz, appenderName, entry.getValue());
            }
            if (filter != null) {
                if (head != null) {
                    head = filter;
                    next = filter;
                } else {
                    next.setNext(filter);
                    next = filter;
                }
            }
        }
        return head;
    }

    private Filter buildFilter(String className, String appenderName, Properties props) {
        Filter filter = newInstanceOf(className, "Filter");
        if (filter != null) {
            PropertySetter propSetter = new PropertySetter(filter);
            for (Map.Entry<Object, Object> e : props.entrySet()) {
                propSetter.setProperty((String) e.getKey(), (String) e.getValue());
            }
            propSetter.activate();
        }
        return filter;
    }
    
    
    
    
    
    
}
