package com.randomnoun.common;

/* (c) 2013 randomnoun. All Rights Reserved. This work is licensed under a
 * BSD Simplified License. (http://www.randomnoun.com/bsd-simplified.html)
 */

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Collator;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.randomnoun.common.Struct;

/** Text utility functions
 *
 * when i blog up this class, don't forget to put the phrase 'textual healing' somewhere in the article.
 * because that'd be GOLD.
 *
 * @author knoxg
 * @version $Id$
 */
public class Text {
    /** A revision marker to be used in exception stack traces. */
    public static final String _revision = "$Id$";

    /** Used to prevent massive debug dumps. See {@link #getDisplayString(String, String)} */
    private static final int MAX_STRING_OUTPUT_CHARS = 300;

	/** Left-justification constant for use in the {@link #pad(String, int, int)} method */
	public static final int JUSTIFICATION_LEFT = 0;
	
	/** Center-justification constant for use in the {@link #pad(String, int, int)} method */
	public static final int JUSTIFICATION_CENTER = 1;

	/** Right-justification constant for use in the {@link #pad(String, int, int)} method */
	public static final int JUSTIFICATION_RIGHT = 2;
	

    /** Returns true if the supplied string is null or the empty string, false otherwise
     *
     * @param text The string to test
     * @return true if the supplied string is null or the empty string, false otherwise
     */
    public static boolean isBlank(String text) {
        return (text == null || text.equals(""));
    }

    /** Returns true if the supplied string is non-null and only contains numeric characters
     *
     * @param text The string to test
     * @return true if the supplied string is non-null and only contains numeric characters
     */
    public static boolean isNumeric(String text) {
        if (text == null) {
            return false;
        }
        char ch;
        for (int i = 0; i < text.length(); i++) {
            ch = text.charAt(i);
            if (ch < '0' || ch > '9') {
                return false;
            }
        }
        return true;
    }

    /** Returns true if the supplied string is non-null and only contains numeric characters
    * or a single decimal point. The value can have a leading negative ('-') symbol.
    * 
    * @param text The string to test
    * @return true if the supplied string is non-null and only contains numeric characters,
    *   which may contain a '.' character in there somewhere.
    */
   public static boolean isNumericDecimal(String text) {
       if (text == null) {
           return false;
       }
       boolean seenPoint = false; // existential quandary there for you
       char ch;
       for (int i = 0; i < text.length(); i++) {
           ch = text.charAt(i);
           if (ch=='.') {
        	   if (seenPoint) { return false; }
        	   seenPoint = true;
           } else if (ch == '-' && i == 0) {
        	   // leading negative symbol OK
           } else if (ch < '0' || ch > '9') {
               return false;
           }
       }
       return true;
   }


    /** Ensures that a string returned from a browser (on any platform) conforms
     * to unix line-EOF conventions. Any instances of consecutive CRs (<tt>0xD</tt>) 
     * and LFs (<tt>0xA</tt>) in a string will be reduced to a series of CRs (the number of CRs will be the
     * maximum number of CRs or LFs found in a row).  
     * 
     * @param input the input string
     * 
     * @return the canonicalised string, as described above
     */
    public static String reduceNewlines(String input) {
    	StringBuilder sb = new StringBuilder();
    	int len = input.length();
    	int crCount = 0;
		int lfCount = 0;
		boolean insertNewline = false;
		char ch;
    	for (int i=0; i<len; i++) {
    		ch = input.charAt(i);
    		if (ch == (char) 0xA) {
    			lfCount ++; insertNewline = true;
    		} else if (ch == (char) 0xD) {
    			crCount ++; insertNewline = true;
    		} else if (insertNewline) {
				for (int j=0; j<Math.max(lfCount, crCount); j++) {
					sb.append((char) 0xA);
				}
				insertNewline = false; lfCount=0; crCount=0;
				sb.append(ch);
    		} else {
    			sb.append(ch);
    		}
    	}
    	if (insertNewline) {
			for (int j=0; j<Math.max(lfCount, crCount); j++) {
				sb.append((char) 0xA);
			}
    	}
    	
    	return sb.toString();
    }


    /**
     * Returns the HTML-escaped form of a string. The <tt>&amp;</tt>,
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
        StringBuilder sb = new StringBuilder(string.length());

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
                    // interestingly, &quote; (with the e) works fine for HTML display,
                    // but not inside hidden field values
                    sb.append("&quot;");
                    break;
                default:
                    sb.append(c);
            }
        }

        return sb.toString();
    }

    /**
     * Returns the unescaped version of a string. 
     * Converts &amp;amp; &amp;lt; and &amp;gt; entities to their
     * characters
     * 
     * @TODO all those other ones
     *
     * @param string The string to be unescaped.
     * @return The unescaped version of the string.
     */
    private String unescapeHtml(String string)
    {
        string = string.replaceAll("&amp;", "&");
        string = string.replaceAll("&lt;", "<");
        string = string.replaceAll("&gt;", ">");
        return string;
    }    
    
    /**
     * Returns a regex-escaped form of a string. That is, the pattern 
     * returned by this method, if compiled into a regex, will match
     * the supplied string exactly. 
     *
     * @param string the string to convert
     *
     * @return the HTML-escaped form of the string
     */
    static public String escapeRegex(String string) {
        if (string == null) {
            return "";
        }

        char c;
        StringBuilder sb = new StringBuilder(string.length());

        for (int i = 0; i < string.length(); i++) {
            c = string.charAt(i);

            switch (c) {
                case '.':
                case '+': // intentional fall-through
                case '?': // intentional fall-through
                case '\\': // intentional fall-through
                case '{': // intentional fall-through
                case '}': // intentional fall-through
                case '[': // intentional fall-through
                case ']': // intentional fall-through
                case '^': // intentional fall-through
                case '$': // intentional fall-through
                case '(': // intentional fall-through
                case '|': // intentional fall-through
                case ')': // intentional fall-through
                	sb.append("\\");
                    sb.append(c);
                    break;
                default:
                    sb.append(c);
            }
        }

        return sb.toString();
    }
    
    
    /**
     * Returns the csv-escaped form of a string. A csv-escaped string is
     * used when writing to a CSV (comma-separated-value) file. It ensures
     * that commas included within a string are quoted. We use the Microsoft-Excel
     * quoting rules, so that our CSV files can be imported into that. These rules
     * (derived from experimentation) are:
     *
     * <ul>
     * <li>Strings without commas (,) inverted commas ("), or newlines (\n) are returned as-is.
     * <li>Otherwise, the string is surrounded by inverted commas, and any
     *   inverted commas within the string are doubled-up (i.e. '"' becomes '""').
     * </ul>
     *
     * <p>Embedded newlines are inserted as-is, as per Excel. This will require
     * some care whilst parsing if we want to be able to read these files.
     *
     * @param string the string to convert
     *
     * @return the csv-escaped form of the string
     */
    static public String escapeCsv(String string) {
        if (string == null) {
            return "";
        }

        if (string.indexOf(',') == -1 && string.indexOf('"') == -1 && string.indexOf('\n') == -1) {
            return string;
        }

        string = Text.replaceString(string, "\"", "\"\"");
        string = "\"" + string + "\"";

        return string;
    }

    /** Given a csv-encoded string (as produced by the rules in {@see #escapeCsv},
     *  produces a List of Strings which represent the individual values in the string.
     *  Note that this method is *not* equivalent to calling <tt>Arrays.asList(astring.split(","))</tt>.
     *
     * <p>Setting the whitespaceSensitive parameter to false allows leading and trailing
     * whitespace in *non-quoted* values to be removed, e.g. if the input string <tt>text</tt> is:
     *
     * <pre class="code">
     * abc,def,  ghi, j k ,"lmn"," op "," q,r","""hello""", "another"
     * </pre>
     *
     * then <tt>parseCsv(text, <b>false</b>)</tt> will return the strings:
     * <pre class="code">
     * abc
     * def
     * ghi
     * j k
     * lmn
     *  op        <i>(this String has one leading space, and a trailing space after 'p')</i>
     *  q,r       <i>(this String has one leading space)</i>
     * "hello"
     * another
     * </pre>
     *
     * and <tt>parseCsv(text, <b>true</b>)</tt> would throw a ParseException (since the
     * final element is a quoted value, but begins with a space).
     *
     * If the <tt>, "another"</tt> text is removed, however, then
     * <tt>parseCsv(text, true)</tt> would return the following:
     *
     * and <tt>parseCsv(text, true)</tt> will return the string
     * <pre>
     * abc
     * def
     *   ghi      <i>(this String has two leading spaces)</i>
     *  j k       <i>(this String has one leading space and a trailing space after the 'k' character)</i>
     * lmn
     *  op        <i>(this String has one leading space, and a trailing space after 'p')</i>
     *  q,r       <i>(this String has one leading space)</i>
     * "hello"
     * </pre>
     *
     * <p>Most applications would want to use the 'whiteSpaceSensitive=false' form of this function, since
     * (a) less chance of a ParseException, and (b) it's what an end-user would normally
     * expect. This can be performed by calling the {@link #parseCsv(String)} method.
     *
     * <p>Whitespace is determined by using the <tt>Character.isSpaceChar()</tt> method,
     * which is Unicode-aware.
     *
     * @param text   The CSV-encoded string to parse
     * @param trim   If set to true, will trim leading and trailing whitespace in *non-quoted* values.
     *
     * @return a List of Strings. The returned List is guaranteed to always contain at least one element.
     *
     * @throws NullPointerException if the text passed to this method is null
     * @throws ParseException if a quoted value contains leading whitespace before the
     *  opening quote, or after the trailing quote.
     * @throws ParseException if a quoted value has a start quote, but no end quote, or
     *   if a value has additional text after a quoted value (before the next comma or EOL).
     */
    static public List<String> parseCsv(String text, boolean whitespaceSensitive)
        throws ParseException {
        if (text == null) {
            throw new NullPointerException("null text");
        }

        // parse state: 
        //   0=searching for new value (at start of line or after comma) 
        //   1=consuming non-quoted values
        //   2=consuming quoted value
        //   3=consumed first quote within a quoted value (may be termining quote or a "" sequence)
        //   4=consuming whitespace up to next comma/EOL (after quoted value, not whitespaceSensitive)
        int parseState = 0;
        int length = text.length();
        String element;
        List<String> result = new ArrayList<String>();
        char ch;
        StringBuilder buffer = new StringBuilder();

        for (int pos = 0; pos < length; pos++) {
            ch = text.charAt(pos);

            // System.out.println("pos " + pos + ", state=" + parseState + ", nextchar=" + ch + ", buf=" + buffer);
            switch (parseState) {
                case 0:
                    if (Character.isSpaceChar(ch)) {
                        if (whitespaceSensitive) {
                            buffer.append(ch);
                            parseState = 1;
                        } else {
                            // ignore
                        }
                    } else if (ch == '"') {
                        parseState = 2;
                    } else if (ch == ',') {
                    	result.add(""); // add an empty element; state remains unchanged
                    } else {
                        buffer.append(ch);
                        parseState = 1;
                    }
                    break;
                case 1:
                    if (ch == ',') {
                        element = buffer.toString();
                        if (!whitespaceSensitive) {
                            element = element.trim();
                        }
                        result.add(element);
                        buffer.setLength(0);
                        parseState = 0;
                    } else {
                        buffer.append(ch);
                    }
                    break;
                case 2:
                    if (ch == '"') {
                        parseState = 3;
                    } else {
                        buffer.append(ch);
                    }
                    break;
                case 3:
                    if (ch == '"') {
                        buffer.append('"');
                        parseState = 2;
                    } else if (ch == ',') {
                        result.add(buffer.toString());
                        buffer.setLength(0);
                        parseState = 0;
                    } else if (Character.isSpaceChar(ch)) {
                        if (whitespaceSensitive) {
                            throw new ParseException("Cannot have trailing whitespace after close quote character", pos);
                        }
                        parseState = 4;
                    } else {
                        throw new ParseException("Cannot have trailing data after close quote character", pos);
                    }
                    break;
                case 4:
                    if (Character.isSpaceChar(ch)) {
                        // consume and ignore
                    } else if (ch == ',') {
                        result.add(buffer.toString());
                        buffer.setLength(0);
                        parseState = 0;
                    } else {
                        throw new ParseException("Cannot have trailing data after close quote character", pos);
                    }
                default:
                    throw new IllegalStateException("Illegal state '" + parseState + "' in parseCsv");
            }
        }

        // if state is 2, we are in the middle of a quoted value
        if (parseState == 2) {
            throw new ParseException("Missing endquote in csv text", length);
        }

        // otherwise we still need to add what's left in the buffer into the result list
        element = buffer.toString();
        if (parseState == 1 && !whitespaceSensitive) {
            element = element.trim();
        }
        result.add(element);
        return result;
    }

    /**
     * Equivalent to <tt>parseCsv(text, false);</tt> (i.e. whitespace-insensitive parsing).
     * Refer to the documentation for that method for more details.
     *
     * @see #parseCsv(String, boolean)
     *
     * @param text he CSV-encoded string to parse
     * 
     * @return a List of Strings. The returned List is guaranteed to always contain at least one element.
     *
     * @throws NullPointerException if the text passed to this method is null.
     * @throws ParseException see {@see #parseCsv(String, boolean)} for details.
     */
    static public List<String> parseCsv(String text)
        throws ParseException {
        return Text.parseCsv(text, false);
    }

    /** Returns a java-escaped string. Replaces '"' with '\"'.
     *
     * <p>Since this is predominantly used in the query builder, I am not worrying about
     * unicode sequences (SWIFT is ASCII) or newlines (although this may be necessary later)
     * for multiline textboxes
     *
     * @return The java-escaped version of the string
     */
    public static String escapeJava(String string) {
        return Text.replaceString(string, "\"", "\\\"");
    }

    /** Returns a javascript string. The characters <tt>'</tt>,
     * <tt>"</tt> and <tt>\</tt> are converted into their Unicode equivalents,
     *
     * <p>Non-printable characters are converted into unicode equivalents
     **
     * <p>Newlines are now replaced with "\n" 
     *
     * @return The java-escaped version of the string
     */
    public static String escapeJavascript(String string) {
        // backslashes are always escaped
        //string = Text.replaceString(string, "\\", "\\u005C");
        //string = Text.replaceString(string, "\"", "\\u0022");
        //string = Text.replaceString(string, "'", "\\u0027");
		//string = Text.replaceString(string, "\n", "\\n");
        
    	StringBuilder sb = new StringBuilder(string.length());
		for (int i = 0; i<string.length(); i++) {
			char ch = string.charAt(i);
			if (ch=='\n') {
			   sb.append("\\n");	
			} else if (ch=='\\' || ch=='"' || ch=='\'' || ch<32 || ch>126) {
				String hex = Integer.toString(ch, 16);
				sb.append("\\u" + "0000".substring(0, 4-hex.length()) + hex);
			} else {
				sb.append(ch);
			}
		}
        return sb.toString();
    }


    /** Returns a javascript string. The characters <tt>'</tt>,
     * <tt>"</tt> and <tt>\</tt> are converted into their Unicode equivalents,
     *
     * <p>Non-printable characters are converted into unicode equivalents
     *
     * @deprecated use {@link #escapeJavascript(String)} instead
     * 
     * @return The java-escaped version of the string
     */
    public static String escapeJavascript2(String string) {
    	// this method only exists for backwards-compatability
        string = reduceNewlines(string);  // canonicalise CRLFs
    	return escapeJavascript(string);
    }

    
    /** Unescapes a java-escaped string. Replaces '\"' with '"',
     * '\\u0022' with '"', '\\u0027' with ''', '\\u005C' with '\'.
     *
     * <p>Since this is predominantly used in the query builder, I am not worrying about
     * unicode sequences (SWIFT is ASCII) or newlines (although this may be necessary later)
     * for multiline textboxes
     *
     * @return The java-escaped version of the string
     */
    public static String unescapeJava(String string) {
        string = Text.replaceString(string, "\\\"", "\"");
        string = Text.replaceString(string, "\\u0022", "\"");
        string = Text.replaceString(string, "\\u0027", "'");
        string = Text.replaceString(string, "\\u005C", "\\");
        return string;
    }

    // this should probably go into Text at some stage
    /** Returns a python string, escaped so that it can be enclosed in a single-quoted string. 
     * 
     * <p>The characters <tt>'</tt>,
     * <tt>"</tt> and <tt>\</tt> are converted into their Unicode equivalents,
     *
     * <p>Non-printable characters are converted into unicode equivalents
     *
     * @return The python-escaped version of the string
     */
    public static String escapePython(String string) {
    	// pretty much the same as Text.escapeJavascript2(), without the reduceNewLines, which probably shouldn't be there anyway
    	string = Text.replaceString(string, "\\", "\\u005C");
        string = Text.replaceString(string, "\"", "\\u0022");
        string = Text.replaceString(string, "'", "\\u0027");
		string = Text.replaceString(string, "\n", "\\n");
		StringBuilder sb = new StringBuilder(string.length());
		for (int i = 0; i<string.length(); i++) {
			char ch = string.charAt(i);
			if (ch>=32 && ch<=126) {
				sb.append(ch);
			} else {
				String hex = Integer.toString(ch, 16);
				sb.append("\\u" + "0000".substring(0, 4-hex.length()) + hex);
			}
		}
        return sb.toString();
        // return string;
    }


    
    /** Returns the given string; but will truncate it to MAX_STRING_OUTPUT_CHARS.
     *  If it exceeds this length, a message is appended expressing how many
     *  characters were truncated. Strings with the key of 'exception' are
     *  not truncated (in order to display full stack traces when these occur).
     *  Any keys that contain the text 'password', 'Password', 'credential' or
     *  'Credential' will be returned as eight asterisks.
     *
     * <p>This method is used in the debug JSP when dumping properties to the user,
     *  in order to prevent inordinately verbose output.
     *
     *  @param key The key of the string we wish to display
     *  @param string The string value
     *  @return A (possibly truncated) version of this string
     */
    public static String getDisplayString(String key, String string) {
        return getDisplayString(key, string, MAX_STRING_OUTPUT_CHARS);
    }

    /** Returns the given string; but will truncate it to MAX_STRING_OUTPUT_CHARS.
     *  If it exceeds this length, a message is appended expressing how many
     *  characters were truncated. Strings with the key of 'exception' are
     *  not truncated (in order to display full stack traces when these occur).
     *  Any keys that contain the text 'password', 'Password', 'credential' or
     *  'Credential' will be returned as eight asterisks.
     *
     * <p>This method is used in the debug JSP when dumping properties to the user,
     *  in order to prevent inordinately verbose output.
     *
     *  @param key The key of the string we wish to display
     *  @param string The string value
     *  @param maxChars The maximum number of characters to display
     *  
     *  @return A (possibly truncated) version of this string
     */
    public static String getDisplayString(String key, String string, int maxChars) {
        if (string == null) {
            string = "(null)";
        }

        if ("exception".equals(key)) {
            return string;
        }

        if (key.indexOf("password") >= 0 || key.indexOf("Password") >= 0 || key.indexOf("credential") >= 0 || key.indexOf("Credential") >= 0) {
            return "********";
        }

        if (string.length() <= maxChars) {
            return string;
        } else {
            return string.substring(0, maxChars) + "... (" + (string.length() - maxChars) + " more characters truncated)";
        }
    }

    /** Utility function to return a default if the supplied string is null.
     *  Shorthand for <tt>(strText==null) ? strDefaultText : strText;</tt>
     *
     * @return strText is strText is not null, otherwise strDefaultText
     */
    public static String strDefault(String strText, String strDefaultText) {
        return (strText == null) ? strDefaultText : strText;
    }

    /** Return a string composed of a series of strings, separated with the specified delimiter
     *
     * @param elements The array of elements to join
     * @return delimiter The delimiter to join each string with
     *
     * @throws NullPointerException if elements or delimiter is null
     *
     * @see #join(List, String)
     */
    public static String join(String[] elements, String delimiter) {
    	return joinWithLast(elements, false, delimiter, delimiter);
    }

    /** Return a string composed of a series of strings, separated with the specified delimiter
     *
     * @param elements A Collection or Iterable of the elements to join
     * @return delimiter The delimiter to join each string with
     *
     * @throws NullPointerException if elements or delimiter is null
     *
     * @see #join(String[], String)
     */
    public static String join(Iterable elements, String delimiter) {
    	return joinWithLast(elements, false, delimiter, delimiter);
    }
    
    /** Return a string composed of a series of strings, separated with the specified delimiter.
    * Each element is contained in single quotes. The final delimeter can be set to a different
    * value, to produce text in the form <tt>"'a', 'b' or 'c'"</tt> or <tt>"'a', 'b' and 'c'"</tt>. 
    *
    * <p>There is no special handling of values containing quotes; see {@link #escapeCsv(String)} 
    *
    * @param elements The array of elements to join
    * @param isQuoted If true, each element is surrounded by single quotes
    * @param delimiter The delimiter to join each string with
    * @param lastDelimiter The delimiter to join the second-last and last elements
    *
    * @throws NullPointerException if elements or delimiter is null
    *
    * @see #join(List, String)
    */
   public static String joinWithLast(String[] elements, boolean isQuoted, String delimiter, String lastDelimiter) {
   	   StringBuilder sb = new StringBuilder();
       if (elements == null) {
           throw new NullPointerException("null elements");
       }
       if (delimiter == null) {
           throw new NullPointerException("null delimiter");
       }
       if (lastDelimiter == null) {
           throw new NullPointerException("null lastDelimiter");
       }
       int len = elements.length;
       if (len == 0) {
           return "";
       }

       for (int i = 0; i < len - 1; i++) {
    	   if (isQuoted) { sb.append("'"); }
           sb.append(elements[i]);
           if (isQuoted) { sb.append("'"); }
           if (i == len - 2) { sb.append(lastDelimiter); } else { sb.append(delimiter); }
       }
       sb.append(elements[len - 1]);
       return sb.toString();
   }

   /** Return a string composed of a series of strings, separated with the specified delimiter
    *
    * <p>There is no special handling of values containing quotes; see {@link #escapeCsv(String)} 
    *
    * @param elements A Collection or Iterable containing the elements to join
    * @param isQuoted If true, each element is surrounded by single quotes
    * @param delimiter The delimiter to join each string with
    * @param lastDelimiter The delimiter to join the second-last and last elements
    *
    * @throws NullPointerException if elements or delimiter is null
    *
    * @see #join(String[], String)
    */
   public static String joinWithLast(Iterable<?> elements, boolean isQuoted, String delimiter, String lastDelimiter) {
   	StringBuilder sb = new StringBuilder();
       if (elements == null) {
           throw new NullPointerException("null elements");
       }
       if (delimiter == null) {
           throw new NullPointerException("null delimiter");
       }
       if (lastDelimiter == null) {
           throw new NullPointerException("null lastDelimiter");
       }
       Iterator<?> i = elements.iterator();
       if (!i.hasNext()) { return ""; } 
       
       Object thisEl = i.next();
       while (i.hasNext()) {
    	   Object nextEl = i.next();
    	   if (isQuoted) { sb.append("'"); }
           sb.append(thisEl);
           if (isQuoted) { sb.append("'"); }
           if (i.hasNext()) {
               sb.append(delimiter);
           } else {
        	   sb.append(lastDelimiter);
           }
           thisEl = nextEl;
       }
       if (isQuoted) { sb.append("'"); }
       sb.append(thisEl);
       if (isQuoted) { sb.append("'"); }
       
       return sb.toString();
   }
    
    
    

    /*
     * efficient search & replace ... stolen from Usenet:
     * http://groups.google.co.uk/groups?hl=en&lr=&selm=memo.19990629182431.344B%40none.crap
     */

    /**
     * An efficient search & replace routine. Replaces all instances of
     * searchString within str with replaceString.
     *
     * @param str The string to search
     * @param searchString The string to search for
     * @param replaceString The string to replace it with
     *
     */
    public static String replaceString(String originalString, String searchString, String replaceString) {
        if (replaceString == null) {
            return originalString;
        }

        if (searchString == null) {
            return originalString;
        }

        if (originalString == null) {
            return null;
        }

        int loc = originalString.indexOf(searchString);

        if (loc == -1) {
            return originalString;
        }

        char[] src = originalString.toCharArray();
        int n = searchString.length();
        int m = originalString.length();
        StringBuilder buf = new StringBuilder(m + replaceString.length() - n);
        int start = 0;

        do {
            if (loc > start) {
                buf.append(src, start, loc - start);
            }

            buf.append(replaceString);
            start = loc + n;
            loc = originalString.indexOf(searchString, start);
        } while (loc > 0);

        if (start < m) {
            buf.append(src, start, m - start);
        }

        return buf.toString();
    }

    /**
     * Reads a file, and returns its contents in a String
     *
     * @param filename The file to read
     *
     * @return The contents of the string,
     *
     * @throws IOException A problem occurred whilst attempting to read the string
     */
    public static String getFileContents(String filename)
        throws IOException {
        File file = new File(filename);
        FileInputStream fis = new FileInputStream(file);
        byte[] data = new byte[(int) file.length()];
        int len = fis.read(data);
        fis.close();
        if (len < file.length()) {
            /* this should never happen -- file has changed underneath us */
            throw new IOException("Buffer read != size of file");
        }

        return new String(data);
    }

    /**
     * Reads a file, and returns its contents in a String. Identical to calling
     * <code>getFileContents(projectFile.getCanonicalPath())</code>.
     *
     * @param file The file to read
     *
     * @return The contents of the string,
     * @throws IOException 
     *
     * @throws IOException A problem occurred whilst attempting to read the string
     */
	public static String getFileContents(File file) throws IOException {
		return getFileContents(file.getCanonicalPath());
	}
    
    
    /**
     * Prefixes every lines supplied with a given indent. e.g.
     * <tt>indent("\t", "abcd\nefgh")</tt> would return "\tabcd\n\tefgh". If the
     * string ends in a newline, then the return value also ends with a newline.
     *
     * @param indentString   The characters to indent with. Usually spaces or tabs,
     *   but could be something like a timestamp.
     * @param originalString The string to indent.
     * @return The originalString, with every line (as separated by the newline
     *   character) prefixed with indentString.
     */
    static public String indent(String indentString, String originalString) {
        String allButLastChar;
        if (originalString == null || indentString == null) {
            throw new NullPointerException();
        }
        if (originalString.equals("")) {
            return indentString;
        }
        allButLastChar = originalString.substring(0, originalString.length() - 1);
        return indentString + replaceString(allButLastChar, "\n", "\n" + indentString) + originalString.substring(originalString.length() - 1);
    }
    
    /** Ensure that a string is padded with spaces so that it meets the 
     * required length. If the input string exceeds this length, this it 
     * is returned unchanged
     * 
     * @param inputString the string to pad
     * @param length the desired length
     * @param justification a JUSTIFICATION_* constant defining whether left or 
     *   right justification is required.
     * 
     * @return a padded string. 
     */
    static public String pad(String inputString, int length, int justification) {
    	// @TODO not terribly efficient, but who cares
    	switch (justification) {
    		case JUSTIFICATION_LEFT:
    			while (inputString.length() < length) { 
    				inputString = inputString + " ";
    			}
    			break;

			case JUSTIFICATION_RIGHT:
				while (inputString.length() < length) { 
					inputString = " " + inputString;
				}
				break;
    		 	
			case JUSTIFICATION_CENTER:
				while (inputString.length() < length) { 
					inputString = inputString + " ";
					if (inputString.length() < length) {
						inputString = " " + inputString;
					}
				}
				break;
    	}
    	return inputString;
    }

    /** Given a period-separated list of components (e.g. variable references ("a.b.c") or classnames),
     *  returns the last component. For example,
     *  getLastComponent("com.randomnoun.common.util.Text") will return "Text".
     *
     *  <p>If component is null, this function returns null.
     *  <p>If component contains no periods, this function returns the original string.
     *
     *  @param component The string to retrieve the last component from
     */
    static public String getLastComponent(String string) {
        if (string == null) {
            return null;
        }
        if (string.indexOf('.') == -1) {
            return string;
        }
        return string.substring(string.lastIndexOf('.') + 1);
    }

    /** Escape this supplied string so it can represent a 'name' or 'value' component
     * on a HTTP queryString. This generally involves escaping special characters into %xx
     * form. Note that this only works for US-ASCII data.
     *
     */
    public static String escapeQueryString(String unescapedQueryString) {
        // default encoding
        byte[] data = encodeUrl(allowed_within_query, unescapedQueryString.getBytes());

        try {
            return new String(data, "US-ASCII");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("encodeQueryString() requires ASCII support");
        }
    }

    /**
     * Encodes an array of bytes into an array of URL safe 7-bit
     * characters. Unsafe characters are escaped.
     *
     * @param urlsafe bitset of characters deemed URL safe
     * @param bytes array of bytes to convert to URL safe characters
     * @return array of bytes containing URL safe characters
     */
    private static final byte[] encodeUrl(BitSet urlsafe, byte[] bytes) {
        if (bytes == null) {
            return null;
        }

        if (urlsafe == null) {
            throw new NullPointerException("null urlsafe");
        }

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        for (int i = 0; i < bytes.length; i++) {
            int b = bytes[i];

            if (b < 0) {
                b = 256 + b;
            }

            if (urlsafe.get(b)) {
                if (b == ' ') {
                    b = '+';
                }

                buffer.write(b);
            } else {
                buffer.write('%');

                char hex1 = Character.toUpperCase(Character.forDigit((b >> 4) & 0xF, 16));
                char hex2 = Character.toUpperCase(Character.forDigit(b & 0xF, 16));

                buffer.write(hex1);
                buffer.write(hex2);
            }
        }

        return buffer.toByteArray();
    }

    /**
     * Encodes a string into Base64 format.
     * No blanks or line breaks are inserted.
     * @param s  a String to be encoded.
     * @return   A String with the Base64 encoded data.
     */
    public static String encodeBase64(String s) {
        return new String(encodeBase64(s.getBytes()));
    }

    /**
     * Encodes a byte array into Base64 format.
     * No blanks or line breaks are inserted.
     * @param in  an array containing the data bytes to be encoded.
     * @return    A character array with the Base64 encoded data.
     */
    public static char[] encodeBase64(byte[] in) {
        int iLen = in.length;
        int oDataLen = (iLen * 4 + 2) / 3; // output length without padding
        int oLen = ((iLen + 2) / 3) * 4; // output length including padding
        char[] out = new char[oLen];
        int ip = 0;
        int op = 0;

        while (ip < iLen) {
            int i0 = in[ip++] & 0xff;
            int i1 = ip < iLen ? in[ip++] & 0xff : 0;
            int i2 = ip < iLen ? in[ip++] & 0xff : 0;
            int o0 = i0 >>> 2;
            int o1 = ((i0 & 3) << 4) | (i1 >>> 4);
            int o2 = ((i1 & 0xf) << 2) | (i2 >>> 6);
            int o3 = i2 & 0x3F;
            out[op++] = map1[o0];
            out[op++] = map1[o1];
            out[op] = op < oDataLen ? map1[o2] : '=';
            op++;
            out[op] = op < oDataLen ? map1[o3] : '=';
            op++;
        }
        return out;
    }

	/** Used by {@link #parseData(String) to parse dates generated in Codec output.
	 * (These dates are generated using the standard Java .toString() method, which
	 * probably changes depending on the VM's locale, which I'm going to ignore for 
	 * the time being).
	 */
	static class DateParser {
		
		/** Parse a date generated by Date.toString() into a Date object
		 * 
		 * @param dateString a string representation of a date
		 * @return a Date representation of a date
		 */
		public static Date valueOf(String dateString) {
			SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd hh:mm:ss z yyyy");
			try {
				return sdf.parse(dateString);
			} catch (ParseException pe) {
				throw (IllegalArgumentException) new IllegalArgumentException("Invalid date '" + dateString + "'").initCause(pe);
			}
		}
	}

    // ---------------------- Generous characters for each component validation
    // -- not much of this is used in this class, so I should shorten these definitions, 
    // but you never know, I might use it later, so it's here for the time being.
    // 
    // compiled from
    //  org.apache.commons.httpclient.util.URIUtil
    //  org.apache.commons.codec.net.URLCodec
    //  org.apache.commons.httpclient.util.EncodingUtil
    //  org.apache.commons.httpclient.URI
    //
    // trust me... just calling escapeQueryString() is *so* much easier.
    private static final BitSet percent = new BitSet(256); // escape % as %25
    private static final BitSet digit = new BitSet(256); // 0-9
    private static final BitSet alpha = new BitSet(256); // lowalpha | upalpha
    private static final BitSet alphanum = new BitSet(256); // alpha | digit
    private static final BitSet hex = new BitSet(256); // digit | a-f | A-F
    private static final BitSet escaped = new BitSet(256); // "%" hex hex
    private static final BitSet mark = new BitSet(256); // -_.!~*'()
    private static final BitSet unreserved = new BitSet(256);

    // alphanum | mark (URI allowed, no purpose)
    private static final BitSet reserved = new BitSet(256); // ;/?:"&=+$,
    private static final BitSet uric = new BitSet(256);

    // reserved | unreserved | escaped
    private static final BitSet allowed_query = new BitSet(256); // uric - %
    private static final BitSet allowed_within_query = new BitSet(256);

    /** Mapping table from 6-bit nibble to Base64 characters */
    private static char[] map1 = new char[64];

    private static Map knownTypes = new HashMap();

    // NB: www-form-encoding appears to be alpha | numeric | -_.* ( + space) 
    static {
        percent.set('%');

        for (int i = '0'; i <= '9'; i++) {
            digit.set(i);
        }

        for (int i = 'a'; i <= 'z'; i++) {
            alpha.set(i);
        }

        for (int i = 'A'; i <= 'Z'; i++) {
            alpha.set(i);
        }

        alphanum.or(alpha);
        alphanum.or(digit);
        hex.or(digit);

        for (int i = 'a'; i <= 'f'; i++) {
            hex.set(i);
        }

        for (int i = 'A'; i <= 'F'; i++) {
            hex.set(i);
        }

        escaped.or(percent);
        escaped.or(hex);
        mark.set('-');
        mark.set('_');
        mark.set('.');
        mark.set('!');
        mark.set('~');
        mark.set('*');
        mark.set('\'');
        mark.set('(');
        mark.set(')');
        reserved.set(';');
        reserved.set('/');
        reserved.set('?');
        reserved.set(':');
        reserved.set('@');
        reserved.set('&');
        reserved.set('=');
        reserved.set('+');
        reserved.set('$');
        reserved.set(',');
        uric.or(reserved);
        uric.or(unreserved);
        uric.or(escaped);
        allowed_query.or(uric);
        allowed_query.clear('%');
        allowed_within_query.or(allowed_query);
        allowed_within_query.andNot(reserved);


        // excluded 'reserved'                       
        // create map1 array
        int i = 0;
        for (char c = 'A'; c <= 'Z'; c++) {
            map1[i++] = c;
        }
        for (char c = 'a'; c <= 'z'; c++) {
            map1[i++] = c;
        }
        for (char c = '0'; c <= '9'; c++) {
            map1[i++] = c;
        }
        map1[i++] = '+';
        map1[i++] = '/';
        
        // these types use a static valueOf() method or single String constructor 
        // to return a type in parseData();
        knownTypes.put("Boolean", "java.lang.Boolean");
        knownTypes.put("Character", "java.lang.Character");
        knownTypes.put("Byte", "java.lang.Byte");
        knownTypes.put("Short", "java.lang.Short");
        knownTypes.put("Integer", "java.lang.Integer");
        knownTypes.put("Long", "java.lang.Long");
        knownTypes.put("Float", "java.lang.Float");
        knownTypes.put("Double", "java.lang.Double");
        knownTypes.put("BigDecimal", "java.math.BigDecimal");
        knownTypes.put("DateSpan", "com.randomnoun.common.jexl.eval.DateSpan");
        knownTypes.put("Timestamp", "java.sql.Timestamp");
		knownTypes.put("Date", "com.randomnoun.common.Text$DateParser");
            
    }
    
    
    /** Heinously inefficent method of parsing an unstructured list, map or set into
     * an object, which is returned by this method. Basically coded up so I can
     * cut &amp; paste state from the debug JSP into text files for testing.
     * 
     * <p>There are a few limits on round-tripping data via this method; specifically,
     * numbers that don't have a type identifier will be returned as Long, and custom 
     * datatypes that do not implement a .valueOf() method may not be able to 
     * be constructed from a text description.
     * 
     * <p>Comments can be added using '#' notation (these cannot occur in the 
     * middle of a structure, however). 
     * 
     * @param text The text to parse. See 
     *   {@link com.randomnoun.common.Struct#structuredListToString(String, List)},
     *   {@link com.randomnoun.common.Struct#structuredMapToString(String, Map)} and
     *   {@link com.randomnoun.common.Struct#structuredSetToString(String, Set)} for
     *   information on the required input format.
     *
     * @return an object constructed from the text description. 
     */
    public static Map parseStructOutput(String text) throws ParseException {
        LineNumberReader lineReader = new LineNumberReader(new StringReader(text));
        String line;
        String token;
        Map topLevelMap = new HashMap();
        
        Stack<Object> objectStack = new Stack<Object>();
        objectStack.push(topLevelMap);
        
        int parseState = 0; 
        // line-level parse state: 
        //   0=creating top-level object 
        //   1=constructing map   
        //   2=constructing list
        //   3=constructing set
        
        try {
            line = lineReader.readLine();
            while (line!=null) {            
                int length = line.length();
                int indent = 0; // could verify indent against stack size
                // System.out.println("ps=" + parseState + "; line ='" + line + "'");
                
                while (indent<length && line.charAt(indent)==' ') { indent++; }
                int tokenEnd = indent + 1;
                while (tokenEnd<length && line.charAt(tokenEnd)!=' ' && line.charAt(tokenEnd)!=':') { tokenEnd++; }
                if (tokenEnd<length) {
                    token = line.substring(indent, tokenEnd);
                } else {
                    token = line.substring(indent);
                }
                // System.out.println("Token = '" + token + "'");
                int listIndex = -1;
                if (parseState == 2 && !token.equals("")) {
                    try {
                        listIndex = Integer.parseInt(token);
                    } catch (NumberFormatException nfe) {
                        // throw new ParseException("Line " + lnr.getLineNumber() + ": Could not parse integer at start of line", lnr.getLineNumber());
                        listIndex = -1;    
                    }
                }
                
                if (token.equals("") || token.startsWith("#")) {
                    // ignore blank lines or comments
                    
                } else if (token.equals("}")) {
                    // close map
                    if (parseState != 1) { throw new ParseException("Line " + lineReader.getLineNumber() + ": Found '}' whilst not constructing map", 0 ); }
                    objectStack.pop();
                    parseState = getNewParseState(objectStack);

                } else if (token.equals("]")) {
                    // close array
                    if (parseState != 2) { throw new ParseException("Line " + lineReader.getLineNumber() + ": Found ']' whilst not constructing list", 0 ); }
                    objectStack.pop();
					parseState = getNewParseState(objectStack);

				} else if (token.equals(")")) {
					// close set
					if (parseState != 3) { throw new ParseException("Line " + lineReader.getLineNumber() + ": Found ')' whilst not constructing set", 0 ); }
					objectStack.pop();
					parseState = getNewParseState(objectStack);
                    
                } else {
                    // line at this point could be 
                    //    token = { 
                    //    token = [
                    //    token = ( 
                    //    token => data 
                    //    token: data      (where token is an int)
                    //    data             (set element)
                    
                    String rest = line.substring(tokenEnd);  
                    // System.out.println("rest = '" + rest + "'");          
                    if (rest.startsWith(": ")) {
                        if (parseState != 2) { throw new ParseException("Line " + lineReader.getLineNumber() + ": Found nnn: xxx whilst not constructing list", 0); }
                        Struct.setListElement(((List) objectStack.peek()), listIndex, parseData(rest.substring(2)));
        
                    } else if (rest.startsWith(" => ")) {
                        if (parseState != 1) { throw new ParseException("Line " + lineReader.getLineNumber() + ": Found xxx => xxx whilst not constructing map", 0); }
                        ((Map) objectStack.peek()).put(token, parseData(rest.substring(4)));
                    
                    } else if (rest.startsWith(" = [")) {
                        List newList = new ArrayList();
                        if (parseState==0 || parseState==1) {
                            ((Map) objectStack.peek()).put(token, newList);
                        } else if (parseState==2) {
                            // should verify list index
                            Struct.setListElement(((List) objectStack.peek()), listIndex, newList);
                        } else if (parseState==3) {
							((Set) objectStack.peek()).add(newList);
						} else {
							throw new IllegalStateException("Unknown state '" + parseState + "'");
						}
                        objectStack.push(newList);
                        parseState = 2;
        
                    } else if (rest.startsWith(" = {")) {
                        Map newMap = new HashMap();
                        if (parseState==0 || parseState==1) {
                            ((Map) objectStack.peek()).put(token, newMap);
                        } else if (parseState==2) {
                            // should verify list index
                            Struct.setListElement(((List) objectStack.peek()), listIndex, newMap);
                        } else if (parseState==3) {
							((Set) objectStack.peek()).add(newMap);
						} else {
							throw new IllegalStateException("Unknown state '" + parseState + "'");
						}                        
						objectStack.push(newMap);
                        parseState = 1;

					} else if (rest.startsWith(" = (")) {
						Set newSet = new HashSet();
						if (parseState==0 || parseState==1) {
							((Map) objectStack.peek()).put(token, newSet);
						} else if (parseState==2) {
							// should verify list index
							Struct.setListElement(((List) objectStack.peek()), listIndex, newSet);
						} else if (parseState==3) {
							((Set) objectStack.peek()).add(newSet);
						} else {
							throw new IllegalStateException("Unknown state '" + parseState + "'");
						}
						objectStack.push(newSet);
						parseState = 3;
                        
                    } else {
						if (parseState != 3) { throw new ParseException("Line " + lineReader.getLineNumber() + ": Found data whilst not constructing map ('" + line + "')", 0); }
						((Set) objectStack.peek()).add(parseData(line.trim()));
                    }
                }
                
                // get next line
                line = lineReader.readLine();
            }

        } catch (IOException ioe) {
            // this can't happen !
            throw (IllegalStateException) new IllegalStateException("IOException reading String").initCause(ioe);
        }

        if (objectStack.size()>1) {
            throw new ParseException("Unclosed } or ] in string representation (stack size = " + objectStack.size() + ")", 0);
        }
        if (parseState!=0) {
            throw new ParseException("{} or [] mismatch in string representation (parseState = " + parseState + ")", 0);
        }
        
        return topLevelMap;
    }
    
    /** Private method used by {@link #parseStructOutput(String)} to determine the parse
     * state for the next item on the parse stack.
     * 
     * @param stack current parse stack; elements are pushed onto this as parsing takes
     *   place; the class of the top element of this stack will determine the next parse
     *   state.
     * 
     * @return a parse state identifier; see {@link #parseStructOutput(String)} for 
     *   details.
     * 
     * @throws IllegalStateException if the next element on the stack is not a Set, List or Map
     */
    private static int getNewParseState(Stack objectStack) {
    	int parseState;
		if (objectStack.size() > 1) {
			if (objectStack.peek() instanceof Set) {
				parseState = 3;
			} else if (objectStack.peek() instanceof List) {
				parseState = 2;
			} else if (objectStack.peek() instanceof Map) {
				parseState = 1;
			} else {
				throw new IllegalStateException("Invalid object '" + objectStack.peek().getClass().getName() + "' found on parse stack");
			}
		} else {
			parseState = 0;
		}
		return parseState;
    }

    /** Parse the rhs of a Struct declaration.
     *  
     * <p>This method takes, as input, the output from a 
     * {@link Struct#structuredListToString(String, List)} or
     * {@link Struct#structuredMapToString(String, Map)} function, and attempts to
     * reconstruct the original Map or List. This is useful when reading text
     * representations of complex objects from disk for use in test cases.
     * 
     * <p>See the Codec class methods for descriptions of what types of syntax are
     * accepted by this method.
     * 
     * @param text The string representation
     * 
     * @return The original class or method
     */    
    private static Object parseData(String text) throws ParseException {
        if (text.startsWith("'") && text.endsWith("'")) {
            return text.substring(1, text.length()-1);
        } else if (text.equals("null")) {
            return null;
        } else if (text.startsWith("(")) {
            int pos = text.indexOf(')');
            if (pos==-1) { throw new ParseException("Unclosed parenthesis in data value '" + text + "'", 0); }
            String type = text.substring(1, pos);
            String rest = text.substring(pos + 2);
            
            if (knownTypes.containsKey(type)) {
                try {
                    Class clazz = Class.forName((String) knownTypes.get(type));
                    
                    // if class implements valueOf, then use this
                    Method parseMethod;
                    try {
                        parseMethod = clazz.getMethod("valueOf", new Class[] {String.class});
                    } catch (NoSuchMethodException nsme) {
                        parseMethod = null;
                    }
                    
                    if (parseMethod!=null) {
                        return parseMethod.invoke(null, new Object[] { rest } );
                    }
                    
                    // otherwise use a String constructor
                    Constructor cons = clazz.getConstructor(new Class[] {String.class});
                    return cons.newInstance(new Object[] { rest });
                } catch (Exception e) {
                    throw (ParseException) new ParseException("Could not instantiate class " + knownTypes.get(type) + " for data value '" + rest + "'", 0).initCause(e);
                }
            } else {
                // could attempt to use a String constructor here, but 
                // throwing an exception is probably safer
                throw new ParseException("Unknown type '" + type + "' for data value '" + text + "'", 0);
            }
        }
        
        
        
        throw new ParseException("Expected string constant of the form 'xxx' or typed value whilst parsing value '" + text + "'", 0);
        
    }


    /**
     * Returns a comparator that compares contained numbers based on their numeric values and compares other parts
     * using the current locale's order rules.
     * <p>For example in German locale this will be a comparator that handles umlauts correctly and ignores
     * upper/lower case differences.</p>
     *
     * @return <p>A string comparator that uses the current locale's order rules and handles embedded numbers
     *         correctly.</p>
     * @see #getNaturalComparator(java.text.Collator)
     */
    public static Comparator getNaturalComparator() {
        final Collator collator = Collator.getInstance();
        return new Comparator() {
            public int compare(Object o1, Object o2) {
                return compareNatural(collator, (String) o1, (String) o2);
            }
        };
    }
    

    /**
     * <p>Compares two strings using the current locale's rules and comparing contained numbers based on their numeric
     * values.</p>
     * <p>This is probably the best default comparison to use.</p>
     * <p>If you know that the texts to be compared are in a certain language that differs from the default locale's
     * langage, then get a collator for the desired locale ({@link java.text.Collator#getInstance(java.util.Locale)})
     * and pass it to {@link #compareNatural(java.text.Collator, String, String)}</p>
     *
     * @param s first string
     * @param t second string
     * @return zero iff <code>s</code> and <code>t</code> are equal,
     *         a value less than zero iff <code>s</code> lexicographically precedes <code>t</code>
     *         and a value larger than zero iff <code>s</code> lexicographically follows <code>t</code>
     */
    public static int compareNatural(Collator collator, String s, String t) {
        return compareNatural(s, t, false, collator);
    }


    /** Natural compare operation. Stolen from 
     * http://www.eekboom.com/java/compareNatural/src/com/eekboom/utils/Strings.java
     * (source file is under BSD license). 
     * 
     * @param s             first string
     * @param t             second string
     * @param caseSensitive treat characters differing in case only as equal - will be ignored if a collator is given
     * @param collator      used to compare subwords that aren't numbers - if null, characters will be compared
     *                      individually based on their Unicode value
     * @return zero iff <code>s</code> and <code>t</code> are equal,
     *         a value less than zero iff <code>s</code> lexicographically precedes <code>t</code>
     *         and a value larger than zero iff <code>s</code> lexicographically follows <code>t</code>
     */
    private static int compareNatural(String s, String t, boolean caseSensitive, Collator collator) {
        int sIndex = 0;
        int tIndex = 0;

        int sLength = s.length();
        int tLength = t.length();

        while(true) {
            // both character indices are after a subword (or at zero)

            // Check if one string is at end
            if(sIndex == sLength && tIndex == tLength) {
                return 0;
            }
            if(sIndex == sLength) {
                return -1;
            }
            if(tIndex == tLength) {
                return 1;
            }

            // Compare sub word
            char sChar = s.charAt(sIndex);
            char tChar = t.charAt(tIndex);

            boolean sCharIsDigit = Character.isDigit(sChar);
            boolean tCharIsDigit = Character.isDigit(tChar);

            if(sCharIsDigit && tCharIsDigit) {
                // Compare numbers

                // skip leading 0s
                int sLeadingZeroCount = 0;
                while(sChar == '0') {
                    ++sLeadingZeroCount;
                    ++sIndex;
                    if(sIndex == sLength) {
                        break;
                    }
                    sChar = s.charAt(sIndex);
                }
                int tLeadingZeroCount = 0;
                while(tChar == '0') {
                    ++tLeadingZeroCount;
                    ++tIndex;
                    if(tIndex == tLength) {
                        break;
                    }
                    tChar = t.charAt(tIndex);
                }
                boolean sAllZero = sIndex == sLength || !Character.isDigit(sChar);
                boolean tAllZero = tIndex == tLength || !Character.isDigit(tChar);
                if(sAllZero && tAllZero) {
                    continue;
                }
                if(sAllZero && !tAllZero) {
                    return -1;
                }
                if(tAllZero) {
                    return 1;
                }

                int diff = 0;
                do {
                    if(diff == 0) {
                        diff = sChar - tChar;
                    }
                    ++sIndex;
                    ++tIndex;
                    if(sIndex == sLength && tIndex == tLength) {
                        return diff != 0 ? diff : sLeadingZeroCount - tLeadingZeroCount;
                    }
                    if(sIndex == sLength) {
                        if(diff == 0) {
                            return -1;
                        }
                        return Character.isDigit(t.charAt(tIndex)) ? -1 : diff;
                    }
                    if(tIndex == tLength) {
                        if(diff == 0) {
                            return 1;
                        }
                        return Character.isDigit(s.charAt(sIndex)) ? 1 : diff;
                    }
                    sChar = s.charAt(sIndex);
                    tChar = t.charAt(tIndex);
                    sCharIsDigit = Character.isDigit(sChar);
                    tCharIsDigit = Character.isDigit(tChar);
                    if(!sCharIsDigit && !tCharIsDigit) {
                        // both number sub words have the same length
                        if(diff != 0) {
                            return diff;
                        }
                        break;
                    }
                    if(!sCharIsDigit) {
                        return -1;
                    }
                    if(!tCharIsDigit) {
                        return 1;
                    }
                } while(true);
            }
            else {
                // Compare words
                if(collator != null) {
                    // To use the collator the whole subwords have to be compared - character-by-character comparision
                    // is not possible. So find the two subwords first
                    int aw = sIndex;
                    int bw = tIndex;
                    do {
                        ++sIndex;
                    } while(sIndex < sLength && !Character.isDigit(s.charAt(sIndex)));
                    do {
                        ++tIndex;
                    } while(tIndex < tLength && !Character.isDigit(t.charAt(tIndex)));

                    String as = s.substring(aw, sIndex);
                    String bs = t.substring(bw, tIndex);
                    int subwordResult = collator.compare(as, bs);
                    if(subwordResult != 0) {
                        return subwordResult;
                    }
                }
                else {
                    // No collator specified. All characters should be ascii only. Compare character-by-character.
                    do {
                        if(sChar != tChar) {
                            if(caseSensitive) {
                                return sChar - tChar;
                            }
                            sChar = Character.toUpperCase(sChar);
                            tChar = Character.toUpperCase(tChar);
                            if(sChar != tChar) {
                                sChar = Character.toLowerCase(sChar);
                                tChar = Character.toLowerCase(tChar);
                                if(sChar != tChar) {
                                    return sChar - tChar;
                                }
                            }
                        }
                        ++sIndex;
                        ++tIndex;
                        if(sIndex == sLength && tIndex == tLength) {
                            return 0;
                        }
                        if(sIndex == sLength) {
                            return -1;
                        }
                        if(tIndex == tLength) {
                            return 1;
                        }
                        sChar = s.charAt(sIndex);
                        tChar = t.charAt(tIndex);
                        sCharIsDigit = Character.isDigit(sChar);
                        tCharIsDigit = Character.isDigit(tChar);
                    } while(!sCharIsDigit && !tCharIsDigit);
                }
            }
        }
    }


	// taken from the W3C Jigsaw server sourcecode; class org.w3c.jigsaw.http.Request#unescape(String)
	/**
	 * Unescape a HTTP escaped string
	 * @param s The string to be unescaped
	 * @return the unescaped string.
	 */
	public static String unescapeQueryString (String s) {
		StringBuilder sbuf = new StringBuilder() ;
		int len  = s.length() ;
		int ch = -1 ;
		for (int i = 0 ; i < len ; i++) {
			switch (ch = s.charAt(i)) {
				case '%':
					if (i < len - 2) {
						// @TODO check to see how illegal escapes are treated
						// e.g. "%nothex"
						ch = s.charAt (++i) ;
						int hb = (Character.isDigit ((char) ch) 
							  ? ch - '0'
							  : 10+Character.toLowerCase ((char) ch)-'a') & 0xF ;
						ch = s.charAt (++i) ;
						int lb = (Character.isDigit ((char) ch)
							  ? ch - '0'
							  : 10+Character.toLowerCase ((char) ch)-'a') & 0xF ;
						sbuf.append ((char) ((hb << 4) | lb)) ;
					} else {
						sbuf.append ('%');  // hit EOL, just leave as is
					}
					break ;
				case '+':
					sbuf.append (' ') ;
					break ;
				default:
					sbuf.append ((char) ch) ;
			}
		}
		return sbuf.toString() ;
	}
	
	/** Returns the largest common prefix between two other strings; e.g. 
	 * getCommonPrefix("abcsomething", "abcsometharg") would be "abcsometh".
	 * 
	 * @param string1 String number one
	 * @param string2 String number two
	 * 
	 * @return the large common prefix between the two strings
	 * 
	 * @throws NullPointerException is string1 or string2 is null
	 */
	public static String getCommonPrefix(String string1, String string2) {
		if (string1==null) { throw new NullPointerException("null string1"); }
		if (string2==null) { throw new NullPointerException("null string2"); }
		int c = 0;
		int maxLen = Math.min(string1.length(), string2.length());		
		
		while (c < maxLen && string1.charAt(c)==string2.charAt(c)) {
			c++;
		}
		return string1.substring(0, c);
	}

	/** Uppercases the first character of a string.
     * 
     * @param text text to modify
     * 
     * @return the supplied text, with the first character converted to uppercase.
     */
    static public String toFirstUpper(String text) {
    	return Character.toUpperCase(text.charAt(0)) + text.substring(1); 
    }


	/** Lowercases the first character of a string.
     * 
     * @param text text to modify
     * 
     * @return the supplied text, with the first character converted to lowercase.
     */
    static public String toFirstLower(String text) {
    	return Character.toLowerCase(text.charAt(0)) + text.substring(1); 
    }

	

    
    /** Number of character edits between two strings; taken from  
	 * http://www.merriampark.com/ldjava.htm. There's a version in commongs-lang,
	 * apparently, but according to the comments on that page, it uses O(n^2) memory,
	 * which can't be good.
	 * 
	 * @param s string 1
	 * @param t string 2
	 *  
	 * @return the smallest number of edits required to convert s into t 
	 */
	public static int getLevenshteinDistance (String s, String t) {
		  if (s == null || t == null) {
		    throw new IllegalArgumentException("Strings must not be null");
		  }
				
		  /*
		    The difference between this impl. and the previous is that, rather 
		     than creating and retaining a matrix of size s.length()+1 by t.length()+1, 
		     we maintain two single-dimensional arrays of length s.length()+1.  The first, d,
		     is the 'current working' distance array that maintains the newest distance cost
		     counts as we iterate through the characters of String s.  Each time we increment
		     the index of String t we are comparing, d is copied to p, the second int[].  Doing so
		     allows us to retain the previous cost counts as required by the algorithm (taking 
		     the minimum of the cost count to the left, up one, and diagonally up and to the left
		     of the current cost count being calculated).  (Note that the arrays aren't really 
		     copied anymore, just switched...this is clearly much better than cloning an array 
		     or doing a System.arraycopy() each time  through the outer loop.)

		     Effectively, the difference between the two implementations is this one does not 
		     cause an out of memory condition when calculating the LD over two very large strings.  		
		  */		
				
		  int n = s.length(); // length of s
		  int m = t.length(); // length of t
				
		  if (n == 0) {
		    return m;
		  } else if (m == 0) {
		    return n;
		  }

		  int p[] = new int[n+1]; //'previous' cost array, horizontally
		  int d[] = new int[n+1]; // cost array, horizontally
		  int _d[]; //placeholder to assist in swapping p and d

		  // indexes into strings s and t
		  int i; // iterates through s
		  int j; // iterates through t

		  char t_j; // jth character of t

		  int cost; // cost

		  for (i = 0; i<=n; i++) {
		     p[i] = i;
		  }
				
		  for (j = 1; j<=m; j++) {
		     t_j = t.charAt(j-1);
		     d[0] = j;
				
		     for (i=1; i<=n; i++) {
		        cost = s.charAt(i-1)==t_j ? 0 : 1;
		        // minimum of cell to the left+1, to the top+1, diagonally left and up +cost				
		        d[i] = Math.min(Math.min(d[i-1]+1, p[i]+1),  p[i-1]+cost);  
		     }

		     // copy current distance counts to 'previous row' distance counts
		     _d = p;
		     p = d;
		     d = _d;
		  } 
				
		  // our last action in the above loop was to switch d and p, so p now 
		  // actually has the most recent cost counts
		  return p[n];
	}
    
	/** Return the md5 hash of a string
     * 
     * @param text text to hash
     * 
     * @return a hex-encoded version of the MD5 hash
     * 
     * @throws IllegalStateException if the java installation in use doesn't know 
     *   about MD5
     */
    static public String getMD5(String text) {
    	try{
    		MessageDigest algorithm = MessageDigest.getInstance("MD5");
    		algorithm.reset();
    		// algorithm.update(defaultBytes);
    		algorithm.update(text.getBytes());
    		byte messageDigest[] = algorithm.digest();
    	            
    		StringBuilder hexString = new StringBuilder();
    		for (int i=0;i<messageDigest.length;i++) {
    			hexString.append(Integer.toHexString(0xFF & messageDigest[i]));
    		}
    		return hexString.toString();
    	} catch (NoSuchAlgorithmException nsae) {
    		throw (IllegalStateException) new IllegalStateException("Unknown algorithm 'MD5'").initCause(nsae);
    	}
    }
    
	/** Perform ${xxxx}-style substitution of placeholders in text. Placeholders without 
	 * values will be left as-is.
	 * 
	 * <p>For example, gives the set of variables:
	 * <attributes>
	 * abc - def
	 * </attributes>
	 * 
	 * <p>then the result of <tt>substituteParameters("xxxx${abc}yyyy${def}zzzz")</tt>
	 * will be "xxxxdefyyyy${def}zzzz"
	 * 
	 * <p><tt>$</tt> followed by any other character will be left as-is. 
	 * 
	 * @param variables a set of variable names and values, used in the substitution 
	 * @param text the text to be substituted.
	 * 
	 * @return text, with placeholders replaced with values in the variables parameter
	 */
	public static String substitutePlaceholders(Map variables, String text) {
		// escaped version of (\$\{.*?\}|[^$]+|\$.)
		Pattern p = Pattern.compile("(\\$\\{.*?\\}|[^$]+|\\$.)");
		Matcher m = p.matcher(text);
		String result = "";
		while (m.find()) {
			String token = m.group(1);
			if (token.startsWith("${") && token.endsWith("}")) {
				Object value = variables.get(token.substring(2, token.length()-1));
				if (value == null) {
					result = result + token;
				} else {
					result = result + value.toString();
				}
			} else {
				result = result + token;
			}
		}
		return result;
	}

	
}
