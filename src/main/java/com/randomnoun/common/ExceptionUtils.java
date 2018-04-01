package com.randomnoun.common;

/* (c) 2013 randomnoun. All Rights Reserved. This work is licensed under a
 * BSD Simplified License. (http://www.randomnoun.com/bsd-simplified.html)
 */

import java.io.*;
import java.lang.reflect.*;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.log4j.Logger;

/**
 * Exception utilities class.
 *
 * <p>This class contains static utility methods for handling and manipulating exceptions;
 * the only one you're likely to call being 
 * {@link #getStackTraceWithRevisions(Throwable, ClassLoader, int, String)},
 * which extracts CVS revision information from classes to produce an annotated (and highlighted)
 * stack trace.
 * 
 * <p>When passing in {@link java.lang.ClassLoader}s to these methods, you may want to try one of 
 * <ul>
 * <li>this.getClass().getClassLoader()
 * <li>Thread.currentThread().getContextClassLoader()
 * </ul>
 * 
 * @blog http://www.randomnoun.com/wp/2012/12/17/marginally-better-stack-traces/
 * @version $Id$
 * @author knoxg
 */
public class ExceptionUtils {
    

    /** Perform no stack trace element highlighting */
    public static final int HIGHLIGHT_NONE = 0;
    
    /** Allow stack trace elements to be highlighted, as text */
    public static final int HIGHLIGHT_TEXT = 1;
    
    /** Allow stack trace elements to be highlighted, as bold HTML */
    public static final int HIGHLIGHT_HTML = 2;

    /** Allow stack trace elements to be highlighted, with span'ed CSS elements */
    public static final int HIGHLIGHT_HTML_WITH_CSS = 3;

    /** The number of characters of the git revision to include in non-CSS stack traces (from the left side of the String) */
    public static final int NUM_CHARACTERS_GIT_REVISION = 8;
    
    static Logger logger = Logger.getLogger(ExceptionUtils.class);
    
    /**
     * Private constructor to prevent instantiation of this class
     */
    private ExceptionUtils() {
    }

    /**
     * Converts an exception's stack trace to a string. If the exception passed
     * to this function is null, returns the empty string.
     *
     * @param e exception
     * 
     * @return string representation of the exception's stack trace
     */
    public static String getStackTrace(Throwable e) {
        if (e == null) {
            return "";
        }
        StringWriter writer = new StringWriter();
        e.printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }

    /**
     * Converts an exception's stack trace to a string. Each stack trace element
     * is annotated to include the CVS revision Id, if it contains a public static
     * String element containing this information. 
     * 
     * <p>Stack trace elements whose classes begin with the specified highlightPrefix
     * are also marked, to allow easier debugging. Highlights used can be text
     * (which will insert the string "=>" before relevent stack trace elements), or 
     * HTML (which will render the stack trace element between &lt;b&gt; and &lt;/b&gt; tags.
     * 
     * <p>If HTML highlighting is enabled, then the exception message is also HTML-escaped.
     *
     * @param e exception
     * @param loader ClassLoader used to read stack trace element revision information
     * @param highlight One of the HIGHLIGHT_* constants in this class
     * @param highlightPrefix A prefix used to determine which stack trace elements are
     *   rendered as being 'important'. (e.g. "<tt>com.randomnoun.common.</tt>"). Multiple
     *   prefixes can be specified, if separated by commas.   
     * 
     * @return string representation of the exception's stack trace
     */
    public static String getStackTraceWithRevisions(Throwable e, ClassLoader loader, int highlight, String highlightPrefix) {
        if (e == null) {
            return "(null)";
        }
        StringBuffer sb = new StringBuffer();
        String additionalMessageSource = null;
        String additionalMessage = null; // because some Exceptions are <strike>retarded</strike> of questionable quality

        // was replacing 'e' here with e.getTarget() if e was a bsh.TargetError, 
        // but should probably just tack this onto the .getCause() chain stead
		
        try {
	        if (e.getClass().getName().equals("com.spotify.docker.client.exceptions.DockerRequestException")) {
	        	Method m;
				m = e.getClass().getMethod("message");
				additionalMessageSource = "DockerRequestException.message()=";
				additionalMessage = (String) m.invoke(e);
				if (additionalMessage!=null) { additionalMessage = additionalMessage.trim(); }
	        }
		} catch (SecurityException e1) {
			// ignore - just use original exception
		} catch (NoSuchMethodException e1) {
			// ignore - just use original exception
		} catch (IllegalArgumentException e1) {
			// ignore - just use original exception
		} catch (IllegalAccessException e1) {
			// ignore - just use original exception
		} catch (InvocationTargetException e1) {
			// ignore - just use original exception
		}
        
        String s = e.getClass().getName();
        String message = e.getLocalizedMessage();
        if (highlight==HIGHLIGHT_HTML || highlight==HIGHLIGHT_HTML_WITH_CSS) {
        	sb.append(escapeHtml((message != null) ? (s + ": " + message) : s ));
        } else {
        	sb.append((message != null) ? (s + ": " + message) : s );
        }
        
        if (additionalMessage!=null) {
        	sb.append('\n');
            if (highlight==HIGHLIGHT_HTML || highlight==HIGHLIGHT_HTML_WITH_CSS) {
            	sb.append(escapeHtml(additionalMessageSource));
            	sb.append(escapeHtml(additionalMessage));
            } else {
            	sb.append(additionalMessageSource);
            	sb.append(additionalMessage);
            }
        }
        sb.append('\n');
        
        // dump the stack trace for the top-level exception
        StackTraceElement[] trace = null;
        trace = e.getStackTrace();
        
        Map<String, JarMetadata> jarMetadataCache = new HashMap<String, JarMetadata>();
        
        for (int i=0; i < trace.length; i++) {
            sb.append(getStackTraceElementWithRevision(jarMetadataCache, trace[i], loader, highlight, highlightPrefix) + "\n");
        }
        Throwable cause = getCause(e);
        if (cause != null) {
            sb.append(getStackTraceWithRevisionsAsCause(jarMetadataCache, cause, trace, loader, highlight, highlightPrefix));
        }
        return sb.toString();
    }

    /** Returns the 'Caused by...' exception text for a chained exception, performing the
     * same stack trace element reduction as performed by the built-in {@link java.lang.Throwable#printStackTrace()}
     * class.
     * 
     * <p>Note that the notion of 'Suppressed' exceptions introduced in Java 7 is not 
     * supported by this implementation.
     *  
     * @param e the cause of the original exception
     * @param causedTrace the original exception trace
     * @param loader ClassLoader used to read stack trace element revision information
     * @param highlight One of the HIGHLIGHT_* constants in this class
     * @param highlightPrefix A prefix used to determine which stack trace elements are
     *   rendered as being 'important'. (e.g. "<tt>com.randomnoun.common.</tt>"). Multiple
     *   prefixes can be specified, if separated by commas.
     *   
     * @return the 'caused by' component of a stack trace
     */
    private static String getStackTraceWithRevisionsAsCause(Map<String, JarMetadata> jarMetadataCache, Throwable e, StackTraceElement[] causedTrace, ClassLoader loader, int highlight, String highlightPrefix) {
        
        StringBuffer sb = new StringBuffer();
        
        // Compute number of frames in common between this and caused
        StackTraceElement[] trace = e.getStackTrace();
        int m = trace.length-1;
        int n = causedTrace.length-1;
        while (m >= 0 && n >=0 && trace[m].equals(causedTrace[n])) {
            m--; n--;
        }
        int framesInCommon = trace.length - 1 - m;

        String s = e.getClass().getName();
        String message = e.getLocalizedMessage();
        if (message==null) {  // org.json.simple.parser.ParseException doesn't set message 
        	message = e.toString(); 
        	if (s.equals(message)) { message = null; }
        } 
        sb.append("Caused by: ");
        if (highlight==HIGHLIGHT_HTML || highlight==HIGHLIGHT_HTML_WITH_CSS) {
        	sb.append(escapeHtml((message != null) ? (s + ": " + message) : s ));
        } else {
        	sb.append((message != null) ? (s + ": " + message) : s );
        }
        sb.append("\n");
        
        for (int i=0; i <= m; i++) {
            sb.append(getStackTraceElementWithRevision(jarMetadataCache, trace[i], loader, highlight, highlightPrefix) + "\n");
        }
        if (framesInCommon != 0)
            sb.append("\t... " + framesInCommon + " more\n");

        // Recurse if we have a cause
        Throwable ourCause = getCause(e);
        if (ourCause != null) {
            sb.append(getStackTraceWithRevisionsAsCause(jarMetadataCache, ourCause, trace, loader, highlight, highlightPrefix));
        }
        return sb.toString();
    }
    
    /** Returns a single stack trace element as a String, with highlighting
     * 
     * @param jarMetadataCache jar-level metadata cached during the traversal of the stackTraceElements.
     * @param ste the StackTraceElement
     * @param loader ClassLoader used to read stack trace element revision information
     * @param highlight One of the HIGHLIGHT_* constants in this class
     * @param highlightPrefix A prefix used to determine which stack trace elements are
     *   rendered as being 'important'. (e.g. "<tt>com.randomnoun.common.</tt>"). Multiple
     *   prefixes can be specified, separated by commas.
     *   
     * @return the stack trace element as a String, with highlighting
     */
    private static String getStackTraceElementWithRevision(Map<String, JarMetadata> jarMetadataCache, 
    	StackTraceElement ste, ClassLoader loader,
        int highlight, String highlightPrefix) 
    {
        // s should be something like:
        // javax.servlet.http.HttpServlet.service(HttpServlet.java:740) or
    	// java.net.Socket.<init>(Socket.java:425), which should be HTML escaped
        String s;
        if (highlightPrefix==null || highlight==HIGHLIGHT_NONE) {
        	s = "    at " + ste.toString();
        } else {
	        boolean isHighlighted = (highlight!=HIGHLIGHT_NONE && isHighlighted(ste.getClassName(), highlightPrefix));
	        if (isHighlighted) {
	        	if (highlight==HIGHLIGHT_HTML) {
		            s = "    at <b>" + escapeHtml(ste.toString()) + "</b>";  
		        } else if (isHighlighted && highlight==HIGHLIGHT_HTML_WITH_CSS) {
			        s = "    at <span class=\"stackTrace-highlight\">" + escapeHtml(ste.toString()) + "</span>";
		        } else if (isHighlighted && highlight==HIGHLIGHT_TEXT) {
		            s = " => at " + ste.toString();
		        } else { 
		            throw new IllegalArgumentException("Unknown highlight " + highlight);
		        }
	        } else { // !isHighlighted
	        	if (highlight==HIGHLIGHT_HTML || highlight==HIGHLIGHT_HTML_WITH_CSS) {
	        		s = "    at " + escapeHtml(ste.toString());
	        	} else {
	        		s = "    at " + ste.toString();
	        	}
	        } 
        }
        
        int openBracketPos = s.lastIndexOf("(");
        int closeBracketPos = s.lastIndexOf(")");
        if (openBracketPos!=-1 && closeBracketPos!=-1) {
            try {
                // add maven, git and cvs revision info
                String className = ste.getClassName();
                JarMetadata jarMetadata = getJarMetadata(jarMetadataCache, loader, className);
                String revision = getClassRevision(loader, className);
                if (revision != null) {
                	if (highlight==HIGHLIGHT_HTML_WITH_CSS) {
                		s = s.substring(0, closeBracketPos) + "<span class=\"stackTrace-revision\">" + revision + "</span> " + s.substring(closeBracketPos);
                	} else {
                		s = s.substring(0, closeBracketPos) + ", " + revision + s.substring(closeBracketPos);
                	}
                }
                if (jarMetadata != null) {
                	if (jarMetadata.groupId!=null && jarMetadata.artifactId!=null) {
                		if (highlight==HIGHLIGHT_HTML_WITH_CSS) {
                			s = s.substring(0, openBracketPos + 1) + "<span class=\"stackTrace-maven\">" + jarMetadata.groupId + ":" + jarMetadata.artifactId + ":" + jarMetadata.version + "</span> " + s.substring(openBracketPos + 1);
                		} else {
                			s = s.substring(0, openBracketPos + 1) + jarMetadata.groupId + ":" + jarMetadata.artifactId + ":" + jarMetadata.version + ", " + s.substring(openBracketPos + 1);
                		}
                	}
                	if (jarMetadata.gitRevision!=null) {
                		if (highlight==HIGHLIGHT_HTML_WITH_CSS) {
                			s = s.substring(0, openBracketPos + 1) + "<span class=\"stackTrace-git\">" + jarMetadata.gitRevision + "</span> " + s.substring(openBracketPos + 1);
                		} else {
                			s = s.substring(0, openBracketPos + 1) + jarMetadata.gitRevision.substring(0, NUM_CHARACTERS_GIT_REVISION) + ", " + s.substring(openBracketPos + 1);
                		}
                	}
                }
            } catch (Exception e2) {
            	// logger.error(e2);
            } catch (NoClassDefFoundError ncdfe) {
            }
        }
        return s;
    }
    
    /** Returns true if the provided className matches the highlightPrefix pattern supplied, 
     * false otherwise
     *  
     * @param className The name of a class (i.e. the class contained in a stack trace element)
     * @param highlightPrefix A prefix used to determine which stack trace elements are
     *   rendered as being 'important'. (e.g. "<tt>com.randomnoun.common.</tt>"). Multiple
     *   prefixes can be specified, separated by commas.
     *   
     * @return true if the provided className matches the highlightPrefix pattern supplied, 
     *   false otherwise
     */
    private static boolean isHighlighted(String className, String highlightPrefix) {
    	if (highlightPrefix.contains(",")) {
    		String[] prefixes = highlightPrefix.split(",");
    		boolean highlighted = false; 
    		for (int i=0; i<prefixes.length && !highlighted; i++) {
    			highlighted = className.startsWith(prefixes[i]);
    		}
    		return highlighted;
    	} else {
    		return className.startsWith(highlightPrefix);
    	}
    }

    /** Return the number of elements in the current thread's stack trace (i.e. the height
     * of the call stack). 
     * 
     * @return the height of the call stack 
     */
    public static int getStackDepth() {
        Throwable t = new Throwable();
        return t.getStackTrace().length;
    }
    
    /** Jar-level metadata is cached for the duration of a single ExceptionUtils method call */
    private static class JarMetadata {
    	String gitRevision;
    	String groupId;
    	String artifactId;
    	String version;
    }
    
    
    /** Returns the JAR metadata (including the git revision and mvn groupId:artifactId co-ordinates) of the JAR containing a class. 
     * 
     * @param loader The classLoader to use
     * @param className The class we wish to retrieve
     * 
     * @return A JarMetadata class, or null
     * 
     * @throws ClassNotFoundException
     * @throws SecurityException
     * @throws NoSuchFieldException
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     */    
    private static JarMetadata getJarMetadata(Map<String, JarMetadata> jarMetadataCache, ClassLoader loader, String className) 
        	throws ClassNotFoundException, SecurityException, NoSuchFieldException, 
        	IllegalArgumentException, IllegalAccessException 
    {
        if (className.indexOf('$')!=-1) {
            className = className.substring(0, className.indexOf('$'));
        }
        
        Class clazz = Class.forName(className, true, loader);
        
        // this is stored in the build.properties file, which we'll have to get from the JAR containing the class
        // see http://stackoverflow.com/questions/1983839/determine-which-jar-file-a-class-is-from
        int revisionMethod = 0;
        
        String uri = clazz.getResource('/' + clazz.getName().replace('.', '/') + ".class").toString();
        if (uri.startsWith("file:")) {
        	// this happens in eclipse; e.g. 
        	//   uri=file:/C:/Users/knoxg/workspace-4.4.1-sts-3.6.2/.metadata/.plugins/org.eclipse.wst.server.core/tmp0/wtpwebapps/jacobi-web/WEB-INF/classes/com/jacobistrategies/web/struts/Struts1Shim.class
        	// use the current class loader's build.properties if available
        	revisionMethod = 1;
        } else if (uri.startsWith("jar:file:")) {
        	revisionMethod = 2;
        } else {
            int idx = uri.indexOf(':');
            String protocol = idx == -1 ? "(unknown)" : uri.substring(0, idx);
            // logger.warn("unknown protocol " + protocol + " in classpath uri '" + uri + "'");
            revisionMethod = -1;
        }

        JarMetadata md = null;
        if (revisionMethod == 1) {
        	// get the revision from the supplied class loader's build.properties, if there is one
        	md = new JarMetadata();
        	InputStream is = loader.getResourceAsStream("/build.properties");
        	if (is==null) {
        		// logger.warn("missing build.properties");
        		return null;
        	} else {
        		Properties props = new Properties();
        		try {
        			props.load(is);
        			md.gitRevision = props.getProperty("git.buildNumber");
        			if ("${buildNumber}".equals(md.gitRevision)) { md.gitRevision = null; }
	        	} catch (IOException ioe) {
	 	        	// ignore;
	 	        }
        	}
        	
        } else if (revisionMethod == 2) {
        	// get the revision from the containing JAR
        	
	        int idx = uri.indexOf('!');
	        //As far as I know, the if statement below can't ever trigger, so it's more of a sanity check thing.
	        if (idx == -1) {
	        	// logger.warn("unparseable classpath uri '" + uri + "'");
	        	return null;
	        }
	        String fileName;
	        try {
	            fileName = URLDecoder.decode(uri.substring("jar:file:".length(), idx), Charset.defaultCharset().name());
	        } catch (UnsupportedEncodingException e) {
	            throw new IllegalStateException("default charset doesn't exist. Your VM is borked.");
	        }
	        
        	// use the cache if it exists
        	if (jarMetadataCache!=null) {
        		md = jarMetadataCache.get(fileName);
        	}
        	if (md==null) {
        		md = new JarMetadata();
        		jarMetadataCache.put(fileName, md);
        		File f = new File(fileName); // .getAbsolutePath();
		        
		        // get the build.properties from this JAR
		        // logger.debug("Getting build.properties for " + className + " from " + f.getPath());
		        try {
			        ZipInputStream zis = new ZipInputStream(new FileInputStream(f));
			        try {
				        ZipEntry ze = zis.getNextEntry();
				        while (ze!=null && (md.gitRevision==null || md.version==null)) {
				        	// logger.debug(ze.getName());
				        	if (ze.getName().equals("build.properties") || ze.getName().equals("/build.properties")) {
				        		// logger.debug(ze.getName());
				        		Properties props = new Properties();
				        		props.load(zis);
				        		md.gitRevision = props.getProperty("git.buildNumber");
				        	}
				        	if (ze.getName().endsWith("/pom.properties")) { 
				        		// e.g. META-INF/maven/edu.ucla.cs.compilers/jtb/pom.xml, pom.properties
				        		// should probably check full path to prevent false positives
				        		Properties props = new Properties();
				        		props.load(zis);
				        		md.groupId = props.getProperty("groupId");
				        		md.artifactId = props.getProperty("artifactId");
				        		md.version = props.getProperty("version");
				        	}
				        	ze = zis.getNextEntry();
				        }
			        } finally {
			        	zis.close();
			        }
		        } catch (IOException ioe) {
		        	// ignore;
		        }
        	}
        }
        return md;
    }
    
    /** Returns a string describing the CVS revision number of a class. 
     * 
     * <p>The class is initialised as part of this process, which may cause a number of 
     * exceptions to be thrown. 
     * 
     * @param loader The classLoader to use
     * @param className The class we wish to retrieve
     * 
     * @return The CVS revision string, in the form "ver 1.234"
     * 
     * @throws ClassNotFoundException
     * @throws SecurityException
     * @throws NoSuchFieldException
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     */    
    private static String getClassRevision(ClassLoader loader, String className) 
        	throws ClassNotFoundException, SecurityException, NoSuchFieldException, 
        	IllegalArgumentException, IllegalAccessException 
    {
        if (className.indexOf('$')!=-1) {
            className = className.substring(0, className.indexOf('$'));
        }
        
        // if this is in a CVS repository, each class has it's own _revision public final static String variable
        Class clazz = Class.forName(className, true, loader);
        Field field = null;
        try {
        	field = clazz.getField("_revision");
        } catch (NoSuchFieldException nsfe) {
        	
        }
        String classRevision = null;
        if (field!=null) {
	        classRevision = (String) field.get(null);
	    
	        // remove rest of $Id$ text 
	        int pos = classRevision.indexOf(",v ");
	        if (pos != -1) {
	            classRevision = classRevision.substring(pos + 3);
	            pos = classRevision.indexOf(' ');
	            classRevision = "ver " + classRevision.substring(0, pos);
	        }
        }
        return classRevision;
    }


    /** An alternate implementation of {@link #getClassRevision(ClassLoader, String)},
     * which searches the raw bytecode of the class, rather than using Java reflection.
     * May be a bit more robust.
     * 
     * @TODO implement this
     * 
     * @param loader Classloader to use
     * @param className Class to load
     * 
     * @return The CVS revision string, in the form "ver 1.234".
     * 
     * @throws ClassNotFoundException
     * @throws SecurityException
     * @throws NoSuchFieldException
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     */
    public static String getClassRevision2(ClassLoader loader, String className) 
    	throws ClassNotFoundException, SecurityException, NoSuchFieldException, 
    	IllegalArgumentException, IllegalAccessException 
    {
        if (className.indexOf('$')!=-1) {
            className = className.substring(0, className.indexOf('$'));
        }
        String file = className.replace('.',  '/') + ".class";
        InputStream is = loader.getResourceAsStream(file);
        
        // read up to '$Id:' text
        
        throw new UnsupportedOperationException("Not implemented");
    }
    
    
    
    /** Returns a list of all the messages contained within a exception (following the causal
     * chain of the supplied exception). This may be more useful to and end-user, since it
     * should not contain any references to Java class names.
     * 
     * @param throwable an exception
     * 
     * @return a List of Strings
     */
    public static List<String> getStackTraceSummary(Throwable throwable) {
    	List<String> result = new ArrayList<String>();
    	while (throwable!=null) {
    		result.add(throwable.getMessage());
    		throwable = getCause(throwable);
    		// I think some RemoteExceptions have non-standard caused-by chains as well...
    		if (throwable==null) {
    			if (throwable instanceof java.sql.SQLException) {
    				throwable = ((java.sql.SQLException) throwable).getNextException();
    			} 
    		}
    	}
    	return result;
    }

    /** Returns the cause of an exception, or null if not known.
     * If the exception is a a <code>bsh.TargetError</code>, then the cause is determined
     * by calling the <code>getTarget()</code> method, otherwise this method will
     * return the same value returned by the standard Exception 
     * <code>getCause()</code> method.
     * 
     * @param e the cause of an exception, or null if not known.
     * 
     * @return the cause of an exception, or null if not known.
     */
    private static Throwable getCause(Throwable e) {
    	Throwable cause = null;
    	if (e.getClass().getName().equals("bsh.TargetError")) {
            try {
    	        if (e.getClass().getName().equals("bsh.TargetError")) {
    	        	Method m;
    				m = e.getClass().getMethod("getTarget");
    				cause = (Throwable) m.invoke(e);
    	        }
    		} catch (SecurityException e1) {
    			// ignore - just use original exception
    		} catch (NoSuchMethodException e1) {
    			// ignore - just use original exception
    		} catch (IllegalArgumentException e1) {
    			// ignore - just use original exception
    		} catch (IllegalAccessException e1) {
    			// ignore - just use original exception
    		} catch (InvocationTargetException e1) {
    			// ignore - just use original exception
    		}
    	} else {
    		cause = e.getCause();
    	}
    	return cause;
    }

    /**
     * Returns the HTML-escaped form of a string. Any <tt>&amp;</tt>,
     * <tt>&lt;</tt>, <tt>&gt;</tt>, and <tt>"</tt> characters are converted to
     * <tt>&amp;amp;</tt>, <tt>&amp;lt;<tt>, <tt>&amp;gt;<tt>, and
     * <tt>&amp;quot;</tt> respectively.
     *
     * @param string the string to convert
     *
     * @return the HTML-escaped form of the string
     */
    static public String escapeHtml(String string) {
        if (string == null) {
            return "";
        }

        char c;
        StringBuffer sb = new StringBuffer(string.length());

        for (int i = 0; i < string.length(); i++) {
            c = string.charAt(i);

            switch (c) {
                case '&':
                    sb.append("&amp;");
                    break;
                case '<':
                    sb.append("&lt;");
                    break;
                case '>':
                    sb.append("&gt;");
                    break;
                case '\"':
                    sb.append("&quot;");
                    break;
                default:
                    sb.append(c);
            }
        }

        return sb.toString();
    }
    
}
