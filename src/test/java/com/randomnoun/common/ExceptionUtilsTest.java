package com.randomnoun.common;

/* (c) 2013 randomnoun. All Rights Reserved. This work is licensed under a
 * BSD Simplified License. (http://www.randomnoun.com/bsd-simplified.html)
 */

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import junit.framework.TestCase;

/** Test class for Exception Utils
 * 
 * @author knoxg
 * @blog http://www.randomnoun.com/wp/2012/12/17/marginally-better-stack-traces/
 * @version $Id$
 */
public class ExceptionUtilsTest extends TestCase {
    /** A revision marker to be used in exception stack traces. */
    public static final String _revision = "$Id$";

	public void testGetStackTrace() {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		Exception e = null;
		try {
            Junk.a();
        } catch(HighLevelException hle) {
            e = hle;
        }
		e.printStackTrace(new PrintStream(baos));
		// check that our getStackTrace() method matches java's printStackTrace() method
		assertEquals(baos.toString(), ExceptionUtils.getStackTrace(e));
		
		try {
            Junk.b();
        } catch(MidLevelException mle) {
            e = mle;
        }
		baos.reset(); e.printStackTrace(new PrintStream(baos));
		assertEquals(baos.toString(), ExceptionUtils.getStackTrace(e));
		
		try {
            Junk.d();
        } catch(LowLevelException lle) {
            e = lle;
        }
		baos.reset(); e.printStackTrace(new PrintStream(baos));
		assertEquals(baos.toString(), ExceptionUtils.getStackTrace(e));

		try {
            Junk.e();
        } catch(LowLevelException lle) {
            e = lle;
        }
		baos.reset(); e.printStackTrace(new PrintStream(baos));
		assertEquals(baos.toString(), ExceptionUtils.getStackTrace(e));

	}
	
	public void testGetStackTraceWithRevisionsNoHighlight() {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		Exception e = null;
		String highlightedStackTrace = null;
		String expectedRegex = null;
		Pattern expectedPattern = null;

		try {
            Junk.a();
        } catch(HighLevelException hle) {
            e = hle;
        }
		e.printStackTrace(new PrintStream(baos));
		highlightedStackTrace = ExceptionUtils.getStackTraceWithRevisions(e, 
			this.getClass().getClassLoader(), ExceptionUtils.HIGHLIGHT_NONE, "com.randomnoun.");
		// check that our getStackTrace() method matches java's printStackTrace() method
		
		/* 
		  
		This is what we expect when run in the Eclipse UI
		
		We need to generalise this a bit to take into consideration different test runners,
		changes in line numbers and changes in version numbers. 
		 
		String expected = 
			"com.randomnoun.common.HighLevelException: com.randomnoun.common.MidLevelException: <middle>\n" + 
			"    at com.randomnoun.common.Junk.a(Junk.java:24, ver 1.12)\n" + 
			"    at com.randomnoun.common.ExceptionUtilsTest.testGetStackTraceWithRevisionsNoHighlight(ExceptionUtilsTest.java:55, ver 1.12)\n" + 
			"    at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)\n" + 
			"    at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:57)\n" + 
			"    at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)\n" + 
			"    at java.lang.reflect.Method.invoke(Method.java:601)\n" + 
			"    at junit.framework.TestCase.runTest(TestCase.java:154)\n" + 
			"    at junit.framework.TestCase.runBare(TestCase.java:127)\n" + 
			"    at junit.framework.TestResult$1.protect(TestResult.java:106)\n" + 
			"    at junit.framework.TestResult.runProtected(TestResult.java:124)\n" + 
			"    at junit.framework.TestResult.run(TestResult.java:109)\n" + 
			"    at junit.framework.TestCase.run(TestCase.java:118)\n" + 
			"    at junit.framework.TestSuite.runTest(TestSuite.java:208)\n" + 
			"    at junit.framework.TestSuite.run(TestSuite.java:203)\n" + 
			"    at org.eclipse.jdt.internal.junit.runner.junit3.JUnit3TestReference.run(JUnit3TestReference.java:130)\n" + 
			"    at org.eclipse.jdt.internal.junit.runner.TestExecution.run(TestExecution.java:38)\n" + 
			"    at org.eclipse.jdt.internal.junit.runner.RemoteTestRunner.runTests(RemoteTestRunner.java:467)\n" + 
			"    at org.eclipse.jdt.internal.junit.runner.RemoteTestRunner.runTests(RemoteTestRunner.java:683)\n" + 
			"    at org.eclipse.jdt.internal.junit.runner.RemoteTestRunner.run(RemoteTestRunner.java:390)\n" + 
			"    at org.eclipse.jdt.internal.junit.runner.RemoteTestRunner.main(RemoteTestRunner.java:197)\n" + 
			"Caused by: com.randomnoun.common.MidLevelException: <middle>\n" + 
			"    at com.randomnoun.common.Junk.c(Junk.java:36, ver 1.12)\n" + 
			"    at com.randomnoun.common.Junk.b(Junk.java:29, ver 1.12)\n" + 
			"    at com.randomnoun.common.Junk.a(Junk.java:22, ver 1.12)\n" + 
			"    ... 19 more\n" + 
			"Caused by: com.randomnoun.common.LowLevelException\n" + 
			"    at com.randomnoun.common.Junk.e(Junk.java:45, ver 1.12)\n" + 
			"    at com.randomnoun.common.Junk.d(Junk.java:41, ver 1.12)\n" + 
			"    at com.randomnoun.common.Junk.c(Junk.java:34, ver 1.12)\n" + 
			"    ... 21 more";				
		*/
		
		expectedRegex =  
			"com.randomnoun.common.HighLevelException: com.randomnoun.common.MidLevelException: <middle>\n" + 
			"    at com.randomnoun.common.Junk.a(Junk.java:[0-9]+, ver [0-9.]+)\n" + 
			"    at com.randomnoun.common.ExceptionUtilsTest.testGetStackTraceWithRevisionsNoHighlight(ExceptionUtilsTest.java:[0-9]+, ver [0-9.]+)\n" +
			".*" +
			"    at junit.framework.TestCase.run(TestCase.java:[0-9]+)\n" +
			".*" +
			"Caused by: com.randomnoun.common.MidLevelException: <middle>\n" +  
			"    at com.randomnoun.common.Junk.c(Junk.java:[0-9]+, ver [0-9.]+)\n" + 
			"    at com.randomnoun.common.Junk.b(Junk.java:[0-9]+, ver [0-9.]+)\n" + 
			"    at com.randomnoun.common.Junk.a(Junk.java:[0-9]+, ver [0-9.]+)\n" + 
			"\t... [0-9]+ more\n" + 
			"Caused by: com.randomnoun.common.LowLevelException\n" + 
			"    at com.randomnoun.common.Junk.e(Junk.java:[0-9]+, ver [0-9.]+)\n" + 
			"    at com.randomnoun.common.Junk.d(Junk.java:[0-9]+, ver [0-9.]+)\n" + 
			"    at com.randomnoun.common.Junk.c(Junk.java:[0-9]+, ver [0-9.]+)\n" + 
			"\t... [0-9]+ more\n" ;
		expectedRegex = expectedRegex.replaceAll("\\(",  "\\\\("); // regex-escape grouping operators 
		expectedRegex = expectedRegex.replaceAll("\\)",  "\\\\)");
		expectedPattern = Pattern.compile(expectedRegex, Pattern.DOTALL);
		assertTrue("Incorrect stacktrace:\n" + highlightedStackTrace, expectedPattern.matcher(highlightedStackTrace).find());
		
		
		try {
            Junk.b();
        } catch(MidLevelException mle) {
            e = mle;
        }
		baos.reset(); e.printStackTrace(new PrintStream(baos));
		highlightedStackTrace = ExceptionUtils.getStackTraceWithRevisions(e, 
				this.getClass().getClassLoader(), ExceptionUtils.HIGHLIGHT_NONE, "com.randomnoun.");
		expectedRegex = 
			"com.randomnoun.common.MidLevelException: <middle>\n" + 
			"    at com.randomnoun.common.Junk.c(Junk.java:[0-9]+, ver [0-9.]+)\n" + 
			"    at com.randomnoun.common.Junk.b(Junk.java:[0-9]+, ver [0-9.]+)\n" +
			"    at com.randomnoun.common.ExceptionUtilsTest.testGetStackTraceWithRevisionsNoHighlight(ExceptionUtilsTest.java:[0-9]+, ver [0-9.]+)\n" +
			".*" +
			"    at junit.framework.TestCase.run(TestCase.java:118)\n" + 
			".*" + 
			"Caused by: com.randomnoun.common.LowLevelException\n" + 
			"    at com.randomnoun.common.Junk.e(Junk.java:[0-9]+, ver [0-9.]+)\n" + 
			"    at com.randomnoun.common.Junk.d(Junk.java:[0-9]+, ver [0-9.]+)\n" + 
			"    at com.randomnoun.common.Junk.c(Junk.java:[0-9]+, ver [0-9.]+)\n" + 
			"\t... [0-9]+ more\n";
		expectedRegex = expectedRegex.replaceAll("\\(",  "\\\\("); // regex-escape grouping operators 
		expectedRegex = expectedRegex.replaceAll("\\)",  "\\\\)");
		expectedPattern = Pattern.compile(expectedRegex, Pattern.DOTALL);
		assertTrue("Incorrect stacktrace:\n" + highlightedStackTrace, expectedPattern.matcher(highlightedStackTrace).find());
		
		
		try {
            Junk.d();
        } catch(LowLevelException lle) {
            e = lle;
        }
		baos.reset(); e.printStackTrace(new PrintStream(baos));
		highlightedStackTrace = ExceptionUtils.getStackTraceWithRevisions(e, 
				this.getClass().getClassLoader(), ExceptionUtils.HIGHLIGHT_NONE, "com.randomnoun.");
		expectedRegex = 
			"com.randomnoun.common.LowLevelException\n" + 
			"    at com.randomnoun.common.Junk.e(Junk.java:[0-9]+, ver [0-9.]+)\n" + 
			"    at com.randomnoun.common.Junk.d(Junk.java:[0-9]+, ver [0-9.]+)\n" + 
			"    at com.randomnoun.common.ExceptionUtilsTest.testGetStackTraceWithRevisionsNoHighlight(ExceptionUtilsTest.java:[0-9]+, ver [0-9.]+)\n" + 
			".*";
		expectedRegex = expectedRegex.replaceAll("\\(",  "\\\\("); // regex-escape grouping operators 
		expectedRegex = expectedRegex.replaceAll("\\)",  "\\\\)");
		expectedPattern = Pattern.compile(expectedRegex, Pattern.DOTALL);
		assertTrue("Incorrect stacktrace:\n" + highlightedStackTrace, expectedPattern.matcher(highlightedStackTrace).find());

		try {
            Junk.e();
        } catch(LowLevelException lle) {
            e = lle;
        }
		baos.reset(); e.printStackTrace(new PrintStream(baos));
		highlightedStackTrace = ExceptionUtils.getStackTraceWithRevisions(e, 
				this.getClass().getClassLoader(), ExceptionUtils.HIGHLIGHT_NONE, "com.randomnoun.");
		expectedRegex = 
			"com.randomnoun.common.LowLevelException\n" + 
			"    at com.randomnoun.common.Junk.e(Junk.java:[0-9]+, ver [0-9.]+)\n" + 
			"    at com.randomnoun.common.ExceptionUtilsTest.testGetStackTraceWithRevisionsNoHighlight(ExceptionUtilsTest.java:[0-9]+, ver [0-9.]+)\n" + 
			".*";
		expectedRegex = expectedRegex.replaceAll("\\(",  "\\\\("); // regex-escape grouping operators 
		expectedRegex = expectedRegex.replaceAll("\\)",  "\\\\)");
		expectedPattern = Pattern.compile(expectedRegex, Pattern.DOTALL);
		assertTrue("Incorrect stacktrace:\n" + highlightedStackTrace, expectedPattern.matcher(highlightedStackTrace).find());

	}
	
	public void testGetStackTraceWithRevisionsText() {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		Exception e = null;
		String highlightedStackTrace = null;
		String expectedRegex = null;
		Pattern expectedPattern = null;
		
		try {
            Junk.a();
        } catch(HighLevelException hle) {
            e = hle;
        }
		e.printStackTrace(new PrintStream(baos));
		highlightedStackTrace = ExceptionUtils.getStackTraceWithRevisions(e, 
			this.getClass().getClassLoader(), ExceptionUtils.HIGHLIGHT_TEXT, "com.randomnoun.");
		
		expectedRegex =  
			"com.randomnoun.common.HighLevelException: com.randomnoun.common.MidLevelException: <middle>\n" + 
			" => at com.randomnoun.common.Junk.a(Junk.java:[0-9]+, ver [0-9.]+)\n" + 
			" => at com.randomnoun.common.ExceptionUtilsTest.testGetStackTraceWithRevisionsText(ExceptionUtilsTest.java:[0-9]+, ver [0-9.]+)\n" +
			".*" +
			"    at junit.framework.TestCase.run(TestCase.java:[0-9]+)\n" +
			".*" +
			"Caused by: com.randomnoun.common.MidLevelException: <middle>\n" +  
			" => at com.randomnoun.common.Junk.c(Junk.java:[0-9]+, ver [0-9.]+)\n" + 
			" => at com.randomnoun.common.Junk.b(Junk.java:[0-9]+, ver [0-9.]+)\n" + 
			" => at com.randomnoun.common.Junk.a(Junk.java:[0-9]+, ver [0-9.]+)\n" + 
			"\t... [0-9]+ more\n" + 
			"Caused by: com.randomnoun.common.LowLevelException\n" + 
			" => at com.randomnoun.common.Junk.e(Junk.java:[0-9]+, ver [0-9.]+)\n" + 
			" => at com.randomnoun.common.Junk.d(Junk.java:[0-9]+, ver [0-9.]+)\n" + 
			" => at com.randomnoun.common.Junk.c(Junk.java:[0-9]+, ver [0-9.]+)\n" + 
			"\t... [0-9]+ more\n" ;
		expectedRegex = expectedRegex.replaceAll("\\(",  "\\\\("); // regex-escape grouping operators 
		expectedRegex = expectedRegex.replaceAll("\\)",  "\\\\)");
		expectedPattern = Pattern.compile(expectedRegex, Pattern.DOTALL);
		assertTrue("Incorrect stacktrace:\n" + highlightedStackTrace, expectedPattern.matcher(highlightedStackTrace).find());
		
		
		try {
            Junk.b();
        } catch(MidLevelException mle) {
            e = mle;
        }
		baos.reset(); e.printStackTrace(new PrintStream(baos));
		highlightedStackTrace = ExceptionUtils.getStackTraceWithRevisions(e, 
				this.getClass().getClassLoader(), ExceptionUtils.HIGHLIGHT_TEXT, "com.randomnoun.");
		expectedRegex = 
			"com.randomnoun.common.MidLevelException: <middle>\n" + 
			" => at com.randomnoun.common.Junk.c(Junk.java:[0-9]+, ver [0-9.]+)\n" + 
			" => at com.randomnoun.common.Junk.b(Junk.java:[0-9]+, ver [0-9.]+)\n" +
			" => at com.randomnoun.common.ExceptionUtilsTest.testGetStackTraceWithRevisionsText(ExceptionUtilsTest.java:[0-9]+, ver [0-9.]+)\n" +
			".*" +
			"    at junit.framework.TestCase.run(TestCase.java:118)\n" + 
			".*" + 
			"Caused by: com.randomnoun.common.LowLevelException\n" + 
			" => at com.randomnoun.common.Junk.e(Junk.java:[0-9]+, ver [0-9.]+)\n" + 
			" => at com.randomnoun.common.Junk.d(Junk.java:[0-9]+, ver [0-9.]+)\n" + 
			" => at com.randomnoun.common.Junk.c(Junk.java:[0-9]+, ver [0-9.]+)\n" + 
			"\t... [0-9]+ more\n";
		expectedRegex = expectedRegex.replaceAll("\\(",  "\\\\("); // regex-escape grouping operators 
		expectedRegex = expectedRegex.replaceAll("\\)",  "\\\\)");
		expectedPattern = Pattern.compile(expectedRegex, Pattern.DOTALL);
		assertTrue("Incorrect stacktrace:\n" + highlightedStackTrace, expectedPattern.matcher(highlightedStackTrace).find());
		
		
		try {
            Junk.d();
        } catch(LowLevelException lle) {
            e = lle;
        }
		baos.reset(); e.printStackTrace(new PrintStream(baos));
		highlightedStackTrace = ExceptionUtils.getStackTraceWithRevisions(e, 
				this.getClass().getClassLoader(), ExceptionUtils.HIGHLIGHT_TEXT, "com.randomnoun.");
		expectedRegex = 
			"com.randomnoun.common.LowLevelException\n" + 
			" => at com.randomnoun.common.Junk.e(Junk.java:[0-9]+, ver [0-9.]+)\n" + 
			" => at com.randomnoun.common.Junk.d(Junk.java:[0-9]+, ver [0-9.]+)\n" + 
			" => at com.randomnoun.common.ExceptionUtilsTest.testGetStackTraceWithRevisionsText(ExceptionUtilsTest.java:[0-9]+, ver [0-9.]+)\n" + 
			".*";
		expectedRegex = expectedRegex.replaceAll("\\(",  "\\\\("); // regex-escape grouping operators 
		expectedRegex = expectedRegex.replaceAll("\\)",  "\\\\)");
		expectedPattern = Pattern.compile(expectedRegex, Pattern.DOTALL);
		assertTrue("Incorrect stacktrace:\n" + highlightedStackTrace, expectedPattern.matcher(highlightedStackTrace).find());

		try {
            Junk.e();
        } catch(LowLevelException lle) {
            e = lle;
        }
		baos.reset(); e.printStackTrace(new PrintStream(baos));
		highlightedStackTrace = ExceptionUtils.getStackTraceWithRevisions(e, 
			this.getClass().getClassLoader(), ExceptionUtils.HIGHLIGHT_TEXT, "com.randomnoun.");
		expectedRegex = 
			"com.randomnoun.common.LowLevelException\n" + 
			" => at com.randomnoun.common.Junk.e(Junk.java:[0-9]+, ver [0-9.]+)\n" + 
			" => at com.randomnoun.common.ExceptionUtilsTest.testGetStackTraceWithRevisionsText(ExceptionUtilsTest.java:[0-9]+, ver [0-9.]+)\n" + 
			".*";
		expectedRegex = expectedRegex.replaceAll("\\(",  "\\\\("); // regex-escape grouping operators 
		expectedRegex = expectedRegex.replaceAll("\\)",  "\\\\)");
		expectedPattern = Pattern.compile(expectedRegex, Pattern.DOTALL);
		assertTrue("Incorrect stacktrace:\n" + highlightedStackTrace, expectedPattern.matcher(highlightedStackTrace).find());

	}
	
	public void testGetStackTraceWithRevisionsHtml() {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		Exception e = null;
		String highlightedStackTrace = null;
		String expectedRegex = null;
		Pattern expectedPattern = null;
		
		try {
            Junk.a();
        } catch(HighLevelException hle) {
            e = hle;
        }
		e.printStackTrace(new PrintStream(baos));
		highlightedStackTrace = ExceptionUtils.getStackTraceWithRevisions(e, 
			this.getClass().getClassLoader(), ExceptionUtils.HIGHLIGHT_HTML, "com.randomnoun.");
		
		expectedRegex =  
			"com.randomnoun.common.HighLevelException: com.randomnoun.common.MidLevelException: &lt;middle&gt;\n" + 
			"    at <b>com.randomnoun.common.Junk.a(Junk.java:[0-9]+, ver [0-9.]+)</b>\n" + 
			"    at <b>com.randomnoun.common.ExceptionUtilsTest.testGetStackTraceWithRevisionsHtml(ExceptionUtilsTest.java:[0-9]+, ver [0-9.]+)</b>\n" +
			".*" +
			"    at junit.framework.TestCase.run(TestCase.java:[0-9]+)\n" +
			".*" +
			"Caused by: com.randomnoun.common.MidLevelException: &lt;middle&gt;\n" +  
			"    at <b>com.randomnoun.common.Junk.c(Junk.java:[0-9]+, ver [0-9.]+)</b>\n" + 
			"    at <b>com.randomnoun.common.Junk.b(Junk.java:[0-9]+, ver [0-9.]+)</b>\n" + 
			"    at <b>com.randomnoun.common.Junk.a(Junk.java:[0-9]+, ver [0-9.]+)</b>\n" + 
			"\t... [0-9]+ more\n" + 
			"Caused by: com.randomnoun.common.LowLevelException\n" + 
			"    at <b>com.randomnoun.common.Junk.e(Junk.java:[0-9]+, ver [0-9.]+)</b>\n" + 
			"    at <b>com.randomnoun.common.Junk.d(Junk.java:[0-9]+, ver [0-9.]+)</b>\n" + 
			"    at <b>com.randomnoun.common.Junk.c(Junk.java:[0-9]+, ver [0-9.]+)</b>\n" + 
			"\t... [0-9]+ more\n" ;
		expectedRegex = expectedRegex.replaceAll("\\(",  "\\\\("); // regex-escape grouping operators 
		expectedRegex = expectedRegex.replaceAll("\\)",  "\\\\)");
		expectedPattern = Pattern.compile(expectedRegex, Pattern.DOTALL);
		assertTrue("Incorrect stacktrace:\n" + highlightedStackTrace, expectedPattern.matcher(highlightedStackTrace).find());
		
		
		try {
            Junk.b();
        } catch(MidLevelException mle) {
            e = mle;
        }
		baos.reset(); e.printStackTrace(new PrintStream(baos));
		highlightedStackTrace = ExceptionUtils.getStackTraceWithRevisions(e, 
				this.getClass().getClassLoader(), ExceptionUtils.HIGHLIGHT_HTML, "com.randomnoun.");
		expectedRegex = 
			"com.randomnoun.common.MidLevelException: &lt;middle&gt;\n" + 
			"    at <b>com.randomnoun.common.Junk.c(Junk.java:[0-9]+, ver [0-9.]+)</b>\n" + 
			"    at <b>com.randomnoun.common.Junk.b(Junk.java:[0-9]+, ver [0-9.]+)</b>\n" +
			"    at <b>com.randomnoun.common.ExceptionUtilsTest.testGetStackTraceWithRevisionsHtml(ExceptionUtilsTest.java:[0-9]+, ver [0-9.]+)</b>\n" +
			".*" +
			"    at junit.framework.TestCase.run(TestCase.java:118)\n" + 
			".*" + 
			"Caused by: com.randomnoun.common.LowLevelException\n" + 
			"    at <b>com.randomnoun.common.Junk.e(Junk.java:[0-9]+, ver [0-9.]+)</b>\n" + 
			"    at <b>com.randomnoun.common.Junk.d(Junk.java:[0-9]+, ver [0-9.]+)</b>\n" + 
			"    at <b>com.randomnoun.common.Junk.c(Junk.java:[0-9]+, ver [0-9.]+)</b>\n" + 
			"\t... [0-9]+ more\n";
		expectedRegex = expectedRegex.replaceAll("\\(",  "\\\\("); // regex-escape grouping operators 
		expectedRegex = expectedRegex.replaceAll("\\)",  "\\\\)");
		expectedPattern = Pattern.compile(expectedRegex, Pattern.DOTALL);
		assertTrue("Incorrect stacktrace:\n" + highlightedStackTrace, expectedPattern.matcher(highlightedStackTrace).find());
		
		
		try {
            Junk.d();
        } catch(LowLevelException lle) {
            e = lle;
        }
		baos.reset(); e.printStackTrace(new PrintStream(baos));
		highlightedStackTrace = ExceptionUtils.getStackTraceWithRevisions(e, 
				this.getClass().getClassLoader(), ExceptionUtils.HIGHLIGHT_HTML, "com.randomnoun.");
		expectedRegex = 
			"com.randomnoun.common.LowLevelException\n" + 
			"    at <b>com.randomnoun.common.Junk.e(Junk.java:[0-9]+, ver [0-9.]+)</b>\n" + 
			"    at <b>com.randomnoun.common.Junk.d(Junk.java:[0-9]+, ver [0-9.]+)</b>\n" + 
			"    at <b>com.randomnoun.common.ExceptionUtilsTest.testGetStackTraceWithRevisionsHtml(ExceptionUtilsTest.java:[0-9]+, ver [0-9.]+)</b>\n" + 
			".*";
		expectedRegex = expectedRegex.replaceAll("\\(",  "\\\\("); // regex-escape grouping operators 
		expectedRegex = expectedRegex.replaceAll("\\)",  "\\\\)");
		expectedPattern = Pattern.compile(expectedRegex, Pattern.DOTALL);
		assertTrue("Incorrect stacktrace:\n" + highlightedStackTrace, expectedPattern.matcher(highlightedStackTrace).find());

		try {
            Junk.e();
        } catch(LowLevelException lle) {
            e = lle;
        }
		baos.reset(); e.printStackTrace(new PrintStream(baos));
		highlightedStackTrace = ExceptionUtils.getStackTraceWithRevisions(e, 
			this.getClass().getClassLoader(), ExceptionUtils.HIGHLIGHT_HTML, "com.randomnoun.");
		expectedRegex = 
			"com.randomnoun.common.LowLevelException\n" + 
			"    at <b>com.randomnoun.common.Junk.e(Junk.java:[0-9]+, ver [0-9.]+)</b>\n" + 
			"    at <b>com.randomnoun.common.ExceptionUtilsTest.testGetStackTraceWithRevisionsHtml(ExceptionUtilsTest.java:[0-9]+, ver [0-9.]+)</b>\n" + 
			".*";
		expectedRegex = expectedRegex.replaceAll("\\(",  "\\\\("); // regex-escape grouping operators 
		expectedRegex = expectedRegex.replaceAll("\\)",  "\\\\)");
		expectedPattern = Pattern.compile(expectedRegex, Pattern.DOTALL);
		assertTrue("Incorrect stacktrace:\n" + highlightedStackTrace, expectedPattern.matcher(highlightedStackTrace).find());

	}
	
	public void testGetStackTraceWithRevisionsTextInnerClass() {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		Exception e = null;
		String highlightedStackTrace = null;
		String expectedRegex = null;
		Pattern expectedPattern = null;
		
		try {
            Junk.InnerClassB1.f();
        } catch(LowLevelException mle) {
            e = mle;
        }
		baos.reset(); e.printStackTrace(new PrintStream(baos));
		assertEquals(baos.toString(), ExceptionUtils.getStackTrace(e));
		
		highlightedStackTrace = ExceptionUtils.getStackTraceWithRevisions(e, 
				this.getClass().getClassLoader(), ExceptionUtils.HIGHLIGHT_TEXT, "com.randomnoun.");
		expectedRegex = 
			"com.randomnoun.common.LowLevelException\n" + 
			" => at com.randomnoun.common.Junk\\$InnerClassB1\\$InnerClassB2.g(Junk.java:[0-9]+, ver [0-9.]+)\n" + 
			" => at com.randomnoun.common.Junk\\$InnerClassB1.f(Junk.java:[0-9]+, ver [0-9.]+)\n" + 
			" => at com.randomnoun.common.ExceptionUtilsTest.testGetStackTraceWithRevisionsTextInnerClass(ExceptionUtilsTest.java:[0-9]+, ver [0-9.]+)\n" + 
			".*" +
			"    at junit.framework.TestCase.run(TestCase.java:118)\n" + 
			".*";
		expectedRegex = expectedRegex.replaceAll("\\(",  "\\\\("); // regex-escape grouping operators 
		expectedRegex = expectedRegex.replaceAll("\\)",  "\\\\)");
		expectedPattern = Pattern.compile(expectedRegex, Pattern.DOTALL);
		assertTrue("Incorrect stacktrace:\n" + highlightedStackTrace, expectedPattern.matcher(highlightedStackTrace).find());

		
		try {
            Junk.InnerClassB1.InnerClassB2.g();
        } catch(LowLevelException mle) {
            e = mle;
        }
		baos.reset(); e.printStackTrace(new PrintStream(baos));
		highlightedStackTrace = ExceptionUtils.getStackTraceWithRevisions(e, 
				this.getClass().getClassLoader(), ExceptionUtils.HIGHLIGHT_TEXT, "com.randomnoun.");
		expectedRegex = 
				"com.randomnoun.common.LowLevelException\n" + 
				" => at com.randomnoun.common.Junk\\$InnerClassB1\\$InnerClassB2.g(Junk.java:[0-9]+, ver [0-9.]+)\n" + 
				" => at com.randomnoun.common.ExceptionUtilsTest.testGetStackTraceWithRevisionsTextInnerClass(ExceptionUtilsTest.java:[0-9]+, ver [0-9.]+)\n" + 
				".*" +
				"    at junit.framework.TestCase.run(TestCase.java:118)\n" + 
				".*";
		expectedRegex = expectedRegex.replaceAll("\\(",  "\\\\("); // regex-escape grouping operators 
		expectedRegex = expectedRegex.replaceAll("\\)",  "\\\\)");
		expectedPattern = Pattern.compile(expectedRegex, Pattern.DOTALL);
		assertTrue("Incorrect stacktrace:\n" + highlightedStackTrace, expectedPattern.matcher(highlightedStackTrace).find());

	}

	public void testGetStackDepth() {
		// not the sort of thing you can test, really
	}

	public void testGetStackTraceSummary() {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		Exception e = null;
		List<String> summary = null;
		List<String> expectedSummary = null;

		try {
            Junk.a();
        } catch(HighLevelException hle) {
            e = hle;
        }
		e.printStackTrace(new PrintStream(baos));
		summary = ExceptionUtils.getStackTraceSummary(e);
		expectedSummary = new ArrayList<String>();
		expectedSummary.add("com.randomnoun.common.MidLevelException: <middle>"); 
		expectedSummary.add("<middle>");
		expectedSummary.add(null);
		assertEquals(expectedSummary, summary);
		
		
		try {
            Junk.b();
        } catch(MidLevelException mle) {
            e = mle;
        }
		baos.reset(); e.printStackTrace(new PrintStream(baos));
		e.printStackTrace(new PrintStream(baos));
		summary = ExceptionUtils.getStackTraceSummary(e);
		expectedSummary = new ArrayList<String>();
		expectedSummary.add("<middle>");
		expectedSummary.add(null);
		assertEquals(expectedSummary, summary);
		
		try {
            Junk.d();
        } catch(LowLevelException lle) {
            e = lle;
        }
		baos.reset(); e.printStackTrace(new PrintStream(baos));
		summary = ExceptionUtils.getStackTraceSummary(e);
		expectedSummary = new ArrayList<String>();
		expectedSummary.add(null);
		assertEquals(expectedSummary, summary);

		try {
            Junk.e();
        } catch(LowLevelException lle) {
            e = lle;
        }
		baos.reset(); e.printStackTrace(new PrintStream(baos));
		summary = ExceptionUtils.getStackTraceSummary(e);
		expectedSummary = new ArrayList<String>();
		expectedSummary.add(null);
		assertEquals(expectedSummary, summary);

	}

}
