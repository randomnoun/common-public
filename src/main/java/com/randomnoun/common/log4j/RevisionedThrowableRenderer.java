package com.randomnoun.common.log4j;

import org.apache.log4j.spi.ThrowableRenderer;

import com.randomnoun.common.ExceptionUtils;

/** Based on the DefaultThrowableRenderer in log4j 1.x 
 * 
 * <p>This ThrowableRenderer is used to include revision markers and highlights in stack traces produced by log4j.
 * 
 * <p>Assign it to the LogManager via:
 * 
 * <pre>
 * 		LoggerRepository repo = LogManager.getLoggerRepository();
		if (repo instanceof ThrowableRendererSupport) {
            ((ThrowableRendererSupport) repo).setThrowableRenderer(new RevisionedThrowableRenderer());
        }
 * </pre>
 */
public class RevisionedThrowableRenderer implements ThrowableRenderer {
	
	private static String highlightPrefix = "com.randomnoun.";
	
    public RevisionedThrowableRenderer(String highlightPrefix) {
    	// NB: this is setting a static field, not an instance field
    	RevisionedThrowableRenderer.highlightPrefix = highlightPrefix;
    }

    /** {@inheritDoc} */
    public String[] doRender(final Throwable throwable) {
        return render(throwable);
    }

    /**
     * Render throwable using Throwable.printStackTrace.
     * @param throwable throwable, may not be null.
     * @return string representation.
     */
    public static String[] render(final Throwable throwable) {
    	String s = ExceptionUtils.getStackTraceWithRevisions(throwable, 
    		RevisionedThrowableRenderer.class.getClassLoader(),
    		ExceptionUtils.HIGHLIGHT_TEXT,
    		highlightPrefix);
    	// could just return 's' here, but the DefaultThrowableRenderer returns each line separately
    	return s.split("\n"); 
    	
    	
    	// DefaultThrowableRenderer:
    	/*
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        try {
            throwable.printStackTrace(pw);
        } catch(RuntimeException ex) {
        }
        pw.flush();
        LineNumberReader reader = new LineNumberReader(new StringReader(sw.toString()));
        ArrayList lines = new ArrayList();
        try {
          String line = reader.readLine();
          while(line != null) {
            lines.add(line);
            line = reader.readLine();
          }
        } catch(IOException ex) {
            if (ex instanceof InterruptedIOException) {
                Thread.currentThread().interrupt();
            }
            lines.add(ex.toString());
        }
        String[] tempRep = new String[lines.size()];
        lines.toArray(tempRep);
        return tempRep;
        */
    }

}
