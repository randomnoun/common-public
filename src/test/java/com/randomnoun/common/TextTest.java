package com.randomnoun.common;

import com.randomnoun.common.Text;

import junit.framework.TestCase;


/**
 * 
 * @author knoxg
 */
public class TextTest
    extends TestCase
{
    /**
     * Constructor for TextTest.
     * @param arg0
     */
    public TextTest(String name)
    {
        super(name);
    }

    /**
     * DOCUMENT ME!
     */
    public void testIsBlank()
    {
        assertTrue("isBlank(null)", Text.isBlank(null));
        assertTrue("isBlank('')", Text.isBlank(""));
        assertFalse("isBlank('abc')", Text.isBlank("abc"));
    }

    public void testEscapeHtml()
    {
        assertEquals("escapeHtml(null)", Text.escapeHtml(null), "");
        assertEquals("escapeHtml('')", Text.escapeHtml(""), "");

        assertEquals("escapeHtml(<)", "&lt;", Text.escapeHtml("<"));
        assertEquals("escapeHtml(>)", "&gt;", Text.escapeHtml(">"));
        assertEquals("escapeHtml(&)", "&amp;", Text.escapeHtml("&"));
        assertEquals("escapeHtml(mixed)", "ab&lt;&amp;&gt;cd", Text.escapeHtml("ab<&>cd"));

        assertEquals("escapeHtml(\u0007)", "&#xfffd;", Text.escapeHtml("\u0007")); // i see you have the machine that goes ping
        assertEquals("escapeHtml(\u1234)", "&#x1234;", Text.escapeHtml("\u1234"));
    }

    public void testEscapeCss()
    {
        assertEquals("escapeCss(null)", Text.escapeCss(null), "");
        assertEquals("escapeCss('')", Text.escapeCss(""), "");

        assertEquals("escapeCss(<)", "\\3c ", Text.escapeCss("<"));
        assertEquals("escapeCss(>)", "\\3e ", Text.escapeCss(">"));
        assertEquals("escapeCss(&)", "\\26 ", Text.escapeCss("&"));
        assertEquals("escapeCss(mixed)", "ab\\3c \\26 \\3e cd", Text.escapeCss("ab<&>cd"));

        assertEquals("escapeCss(\u0007)", "\\7 ", Text.escapeCss("\u0007")); // ESAPI lets this through; may want to check later
        assertEquals("escapeCss(\u1234)", "\\1234 ", Text.escapeCss("\u1234"));
    }

    
    /**
     * DOCUMENT ME!
     */
    public void testGetDisplayString()
    {
        String a;

        try
        {
            a = Text.getDisplayString(null, null);
            fail("Text.getDisplayString(null, null) should throw NullPointerException");
        }
        catch (NullPointerException ne)
        {
            // ok  
        }

        try
        {
            a = Text.getDisplayString(null, "abc");
            fail("Text.getDisplayString(null, 'abc') should throw NullPointerException");
        }
        catch (NullPointerException ne)
        {
            // ok  
        }

        assertEquals("getDisplayString('abc', null)", "(null)",
            Text.getDisplayString("abc", null));
        assertEquals("getDisplayString('password', null)", "********",
            Text.getDisplayString("password", null));
        assertEquals("getDisplayString('pAsSwOrD', null)", "(null)",
            Text.getDisplayString("pAsSwOrD", null));
        assertEquals("getDisplayString('credentials', null)", "********",
            Text.getDisplayString("credentials", null));

        assertEquals("getDisplayString('abc', 'def')", "def",
            Text.getDisplayString("abc", "def"));
        assertEquals("getDisplayString('password', 'def')", "********",
            Text.getDisplayString("password", "def"));
        assertEquals("getDisplayString('pAsSwOrD', 'def')", "def",
            Text.getDisplayString("pAsSwOrD", "def"));
        assertEquals("getDisplayString('credentials', 'def')", "********",
            Text.getDisplayString("credentials", "def"));

        a = "";

        for (int i = 0; i < 200; i++)
        {
            a = a + "1234567890";
        }

        ;
        assertEquals("getDisplayString('abc', longstring)",
            a.substring(0, 300) + "... (1700 more characters truncated)",
            Text.getDisplayString("abc", a));
    }

    /**
     * DOCUMENT ME!
     */
    public void testStrDefault()
    {
        assertNull("strDefault(null, null)", Text.strDefault(null, null));
        assertEquals("strDefault(null, 'abc')", "abc", Text.strDefault(null, "abc"));
        assertEquals("strDefault(null, '')", "", Text.strDefault(null, ""));

        assertEquals("strDefault('def', null)", "def", Text.strDefault("def", null));
        assertEquals("strDefault('def', 'abc')", "def", Text.strDefault("def", "abc"));
        assertEquals("strDefault('def', '')", "def", Text.strDefault("def", ""));

        assertEquals("strDefault('', null)", "", Text.strDefault("", null));
        assertEquals("strDefault('', 'abc')", "", Text.strDefault("", "abc"));
        assertEquals("strDefault('', '')", "", Text.strDefault("", ""));
    }

    /**
     * DOCUMENT ME!
     */
    public void testReplaceString()
    {
        String a;

        /*
           try {
             a = Text.replaceString(null, null, null);
             fail("replaceString(null, null, null) should throw NullPointerException");
           } catch (NullPointerException ne) {
             // ok
           }
           try {
             a = Text.replaceString("abc", null, null);
             fail("replaceString('abc', null, null) should throw NullPointerException");
           } catch (NullPointerException ne) {
             // ok
           }
    
           try {
             a = Text.replaceString("abc", "def", null);
             fail("replaceString('abc', 'def', null) should throw NullPointerException");
           } catch (NullPointerException ne) {
             // ok
           }
         */
        // single char replacement        
        assertEquals("replaceString('', 'b', 'x')", "", Text.replaceString("", "b", "x"));
        assertEquals("replaceString('abc', 'b', 'x')", "axc",
            Text.replaceString("abc", "b", "x"));
        assertEquals("replaceString('abc', 'd', 'x')", "abc",
            Text.replaceString("abc", "d", "x"));
        assertEquals("replaceString('abc', 'bc', 'x')", "ax",
            Text.replaceString("abc", "bc", "x"));
        assertEquals("replaceString('abababa', 'b', 'x')", "axaxaxa",
            Text.replaceString("abababa", "b", "x"));
        assertEquals("replaceString('abababa', 'bab', 'x')", "axaba",
            Text.replaceString("abababa", "bab", "x"));

        // multiple char replacement
        assertEquals("replaceString('', 'b', 'fish')", "",
            Text.replaceString("", "b", "x"));
        assertEquals("replaceString('abc', 'b', 'fish')", "afishc",
            Text.replaceString("abc", "b", "fish"));
        assertEquals("replaceString('abc', 'd', 'fish')", "abc",
            Text.replaceString("abc", "d", "fish"));
        assertEquals("replaceString('abc', 'bc', 'fish')", "afish",
            Text.replaceString("abc", "bc", "fish"));
        assertEquals("replaceString('abababa', 'b', 'fish')", "afishafishafisha",
            Text.replaceString("abababa", "b", "fish"));
        assertEquals("replaceString('abababa', 'bab', 'fish')", "afishaba",
            Text.replaceString("abababa", "bab", "fish"));

        // replace with text already in original string
        assertEquals("replaceString('', 'b', 'a')", "", Text.replaceString("", "b", "a"));
        assertEquals("replaceString('abc', 'b', 'a')", "aac",
            Text.replaceString("abc", "b", "a"));
        assertEquals("replaceString('abc', 'd', 'a')", "abc",
            Text.replaceString("abc", "d", "a"));
        assertEquals("replaceString('abc', 'bc', 'a')", "aa",
            Text.replaceString("abc", "bc", "a"));
        assertEquals("replaceString('abababa', 'b', 'a')", "aaaaaaa",
            Text.replaceString("abababa", "b", "a"));
        assertEquals("replaceString('abababa', 'bab', 'a')", "aaaba",
            Text.replaceString("abababa", "bab", "a"));
    }

    /**
     * DOCUMENT ME!
     */
    public void testGetFileContents()
    {
    }

    /**
     * DOCUMENT ME!
     */
    public void testIndent()
    {
    }

    /**
     * DOCUMENT ME!
     */
    public void testStructuredListToString()
    {
    }

    /**
     * DOCUMENT ME!
     */
    public void testStructuredMapToString()
    {
    }

    /*
     * Test for void setFromRequest(Object, HttpServletRequest)
     */
    public void testSetFromRequestObjectHttpServletRequest()
    {
    }

    /*
     * Test for void setFromRequest(Object, HttpServletRequest, String[])
     */
    public void testSetFromRequestObjectHttpServletRequestStringArray()
    {
    }

    /**
     * DOCUMENT ME!
     */
    public void testGetValue()
    {
    }
    
    public void testReduceNewlines() 
    {
    	String dosString = "this\r\nis a string\r\nwith DOS EOL conventions\r\n";
		String unixString = "this\nis a string\nwith unix EOL conventions\n";
		String macString = "this\ris a string\rwith mac EOL conventions\r";
		String oddString = "this\r\n\nis a string\r\rwith mixed EOL conventions\n\n\n\r\n\r";
		
		assertEquals("this\nis a string\nwith DOS EOL conventions\n", Text.reduceNewlines(dosString));
		assertEquals("this\nis a string\nwith unix EOL conventions\n", Text.reduceNewlines(unixString));
		assertEquals("this\nis a string\nwith mac EOL conventions\n", Text.reduceNewlines(macString));
		assertEquals("this\n\nis a string\n\nwith mixed EOL conventions\n\n\n\n", Text.reduceNewlines(oddString));
    }
    
    public void testEscapeJavascript() {
    	String a = "lineWith\nNewlines";
    	String b = "lineWith\r\nNewlines";
    	String c = "lineWith'Quote";
    	String d = "lineWith</scrIpT>scriptTag";
    	
    	assertEquals("lineWith\\nNewlines", Text.escapeJavascript(a));
    	assertEquals("lineWith\\nNewlines", Text.escapeJavascript2(b));
    	assertEquals("lineWith\\u0027Quote", Text.escapeJavascript(c));
    	assertEquals("lineWith\\u003C/scrIpT>scriptTag", Text.escapeJavascript(d));
    }
    
    public void testParseCodecOutput() {
    	// @TODO
    }
    public void testCsvEscape(){
    	
    	assertEquals("\"'=CMD()\"", Text.escapeCsv("=CMD()"));
    	assertEquals("\"'=12345\"", Text.escapeCsv("=12345"));
    	assertEquals("\"abc,def\"", Text.escapeCsv("abc,def"));
    	assertEquals("\"'=DDE(\"\"cmd\"\";\"\"/C calc\"\";\"\"__DdeLink_60_870516294\"\")\"", Text.escapeCsv("=DDE(\"cmd\";\"/C calc\";\"__DdeLink_60_870516294\")"));
    	assertEquals("\"'=cmd|' /C calc'!A0\"", Text.escapeCsv("=cmd|' /C calc'!A0"));
    	assertEquals("\"'+cmd|' /C calc'!A0\"", Text.escapeCsv("+cmd|' /C calc'!A0"));
    	assertEquals("\"'-cmd|' /C calc'!A0\"", Text.escapeCsv("-cmd|' /C calc'!A0"));

    	// numbers shouldn't be escaped
    	assertEquals("123.456", Text.escapeCsv("123.456"));
    	assertEquals("123", Text.escapeCsv("123"));
    	assertEquals("-123.456", Text.escapeCsv("-123.456"));
    	assertEquals("-123", Text.escapeCsv("-123"));
    	
    	// and with E notation
    	assertEquals("123.456E+5", Text.escapeCsv("123.456E+5"));
    	assertEquals("123E+5", Text.escapeCsv("123E+5"));
    	assertEquals("-123.456E+5", Text.escapeCsv("-123.456E+5"));
    	assertEquals("-123E+5", Text.escapeCsv("-123E+5"));
    }
    
    public void testPad() {
    	String source = "1234";
    	assertEquals("1234      ", Text.pad(source, 10, Text.JUSTIFICATION_LEFT));
		assertEquals("      1234", Text.pad(source, 10, Text.JUSTIFICATION_RIGHT));
		assertEquals("   1234   ", Text.pad(source, 10, Text.JUSTIFICATION_CENTER));
    }
    
    
}
