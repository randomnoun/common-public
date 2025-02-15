package com.randomnoun.common;

/* (c) 2013 randomnoun. All Rights Reserved. This work is licensed under a
 * BSD Simplified License. (http://www.randomnoun.com/bsd-simplified.html)
 */
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Collator;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Text utility functions
 *
 * @author knoxg
 */
public class Text {
    
    /** Used to prevent massive debug dumps. See {@link #getDisplayString(String, String)} */
    private static final int MAX_STRING_OUTPUT_CHARS = 300;

	/** Left-justification constant for use in the {@link #pad(String, int, int)} method */
	public static final int JUSTIFICATION_LEFT = 0;
	
	/** Center-justification constant for use in the {@link #pad(String, int, int)} method */
	public static final int JUSTIFICATION_CENTER = 1;

	/** Right-justification constant for use in the {@link #pad(String, int, int)} method */
	public static final int JUSTIFICATION_RIGHT = 2;
	
	public static Pattern scriptPattern = Pattern.compile("<(/script)", Pattern.CASE_INSENSITIVE);

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
       int len = text.length();
       for (int i = 0; i < len; i++) {
           ch = text.charAt(i);
           if (ch=='.') {
        	   if (seenPoint) { return false; }
        	   seenPoint = true;
           } else if (ch == '-' && i == 0) {
        	   // leading negative symbol OK
        	   if (len == 1) {
        		   // but not if it's the only character in the string
        		   return false;
        	   }
           } else if (ch < '0' || ch > '9') {
               return false;
           }
       }
       return true;
   }

     /** Returns true if the supplied string is non-null and only contains numeric characters
     * or a single decimal point. The value can have a leading negative ('-') symbol.
     * 
     * This version allows exponents ("E+nn" or "E-nn") to the end of the value.
     * 
     * @param text The string to test
     * @return true if the supplied string is non-null and only contains numeric characters,
     *  which may contain a '.' character in there somewhere.
     */
    public static boolean isNumericDecimalExp(String text) {
        if (text == null) {
          return false;
        }
	    boolean seenPoint = false; // existential quandary there for you
	    int expPos = -1;           // position of the 'E' character
	    char ch;
	    for (int i = 0; i < text.length(); i++) {
	    	ch = text.charAt(i);
	    	if (ch=='E') {
	    		if (expPos != -1) { return false; }
	    		expPos = i;
	    	} else if (ch=='.' && expPos == -1) {
            	if (seenPoint) { return false; }
            	seenPoint = true;
	    	} else if ((ch == '+' || ch == '-') && i == expPos + 1) {
	    		// + or - directly after 'E' OK
            } else if (ch == '-' && i == 0) {
            	// leading negative symbol OK
            } else if (ch < '0' || ch > '9') {
            	return false;
            }
	    }
	    return true;
    }


    /** Ensures that a string returned from a browser (on any platform) conforms
     * to unix line-EOF conventions. Any instances of consecutive CRs (<code>0xD</code>) 
     * and LFs (<code>0xA</code>) in a string will be reduced to a series of CRs (the number of CRs will be the
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
     * Returns the HTML-escaped form of a string. The <code>&amp;</code>,
     * <code>&lt;</code>, <code>&gt;</code>, and <code>"</code> characters are converted to
     * <code>&amp;amp;</code>, <code>&amp;lt;</code>, <code>&amp;gt;</code>, and
     * <code>&amp;quot;</code> respectively.
     * 
     * <p>Characters in the unicode control code blocks ( apart from \t, \n and \r ) are converted to &amp;xfffd;
     * <p>Characters outside of the ASCII printable range are converted into &amp;xnnnn; form
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
        String hex;
        StringBuilder sb = new StringBuilder(string.length());
        for (int i = 0; i < string.length(); i++) {
            c = string.charAt(i);
            // check for illegal characters
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
                	// 'illegal characters' according to ESAPI. 7f to 9f are control characters in unicode 
            		if ( ( c <= 0x1f && c != '\t' && c != '\n' && c != '\r' ) || ( c >= 0x7f && c <= 0x9f ) ) {
            			sb.append("&#xfffd;"); // REPLACEMENT_HEX in ESAPI's HtmlEntityCodec
            		} else if ( c > 0x1f && c <= 0x7f ) {
            			// safe printable
            			sb.append(c);
            		} else {
            			// ESAPI didn't have the else block above, which was causing it escape everything 
            			hex = getHexForNonAlphanumeric(c);
            			sb.append("&#x" + hex + ";");
            		}
                	
            }
        }

        return sb.toString();
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
     * <li>A value that starts with any of "=", "@", "+" or "-" has a leading single apostrophe added
     *   to prevent the value being evaluated in Excel. The leading quote is visible to the user when the
     *   csv is opened, which may mean that it will have to be removed when roundtripping data.
     *   This may complicate things if the user actually wants a leading single quote in their CSV value.   
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

        boolean quoted = false;
        // from https://www.contextis.com/en/blog/comma-separated-vulnerabilities
        // prefix cells that start with ‘=’ , '@', '+' or '-' with an apostrophe 
        // This will ensure that the cell isn’t interpreted as a formula, and as a bonus in Microsoft Excel the apostrophe itself will not be displayed.
        if (string.startsWith("=") || 
          string.startsWith("@")) {
            // prefix the string with an a single quote char to escape it
            string = "'" + string;
            quoted = true; // not sure need to quote here, but doesn't hurt
        } else if ((string.startsWith("+") || string.startsWith("-")) && 
        	(string.length() == 1 || !Text.isNumericDecimalExp(string))) {
        	// numbers can legitimately start with '+' or '-' but anything else should be escaped
        	string = "'" + string;
            quoted = true; 
        }

        
        if (string.indexOf(',') == -1 && string.indexOf('"') == -1 && string.indexOf('\n') == -1 && !quoted) {
        	return string;
        }
        string = Text.replaceString(string, "\"", "\"\"");
        string = "\"" + string + "\"";

        return string;
    }

    /** Given a csv-encoded string (as produced by the rules in {@link #escapeCsv(String)},
     *  produces a List of Strings which represent the individual values in the string.
     *  Note that this method is *not* equivalent to calling <code>Arrays.asList(astring.split(","))</code>.
     *
     * <p>Setting the whitespaceSensitive parameter to false allows leading and trailing
     * whitespace in *non-quoted* values to be removed, e.g. if the input string <code>text</code> is:
     *
     * <pre class="code">
     * abc,def,  ghi, j k ,"lmn"," op "," q,r","""hello""", "another"
     * </pre>
     *
     * then <code>parseCsv(text, <b>false</b>)</code> will return the strings:
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
     * and <code>parseCsv(text, <b>true</b>)</code> would throw a ParseException (since the
     * final element is a quoted value, but begins with a space).
     *
     * If the <code>, "another"</code> text is removed, however, then
     * <code>parseCsv(text, true)</code> would return the following:
     *
     * and <code>parseCsv(text, true)</code> will return the string
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
     * <p>Whitespace is determined by using the <code>Character.isSpaceChar()</code> method,
     * which is Unicode-aware.
     *
     * @param text   The CSV-encoded string to parse
     * @param whitespaceSensitive   If set to true, will trim leading and trailing whitespace in *non-quoted* values.
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
                    break;
                    
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
    
    @FunctionalInterface
    public interface CsvLineReader { // doesn't extend Supplier<T> as it throws exceptions
    	/** Returns the next logical line in the CSV ( quoted values can contain newlines )  
    	 * 
    	 * @return
    	 * @throws ParseException
    	 * @throws IOException
    	 */
        List<String> readLine() throws ParseException, IOException;
    }
    
    // same as parseCsv(String, whitespaceSensitive) but can handle newlines in quotes by supplying a Reader
    // the returned object will return a List<String> or null if EOF is reached
    // ParseExceptions are wrapped in something, probably
    static public CsvLineReader parseCsv(Reader r, boolean whitespaceSensitive) {
        if (r == null) {
            throw new NullPointerException("null reader");
        }
    	return new CsvLineReader() {
    		// eof if we actually read eof or encouner a parse exception ( cannot recover )
    		boolean isAtStart = true; // for backwards compatibility with Text.parseCsv(""), first readLine() is never null
    		boolean isEOF = false;
			@Override
			public List<String> readLine() throws ParseException, IOException {
				if (isEOF) { return null; }
				
				// parse state: 
		        //   0=searching for new value (at start of line or after comma) 
		        //   1=consuming non-quoted values
		        //   2=consuming quoted value
		        //   3=consumed first quote within a quoted value (may be termining quote or a "" sequence)
		        //   4=consuming whitespace up to next comma/EOL (after quoted value, not whitespaceSensitive)
		        int parseState = 0;
		        // int length = text.length();
		        String element;
		        List<String> result = new ArrayList<String>();
		        char ch;
		        StringBuilder buffer = new StringBuilder();
		        int intChar = r.read();
		        int pos = 1;
		        if (intChar == -1 && !isAtStart) {
		        	isEOF = true;
		        	return null;
		        }

		        // @TODO better CRLF handling
		        isAtStart = false;
		        while (intChar != -1) {
		            ch = (char) intChar;

		            // System.out.println("pos " + pos + ", state=" + parseState + ", nextchar=" + ch + ", buf=" + buffer);
		            switch (parseState) {
		                case 0:
		                	if (ch == '\n') {
		                		// return result so far
		                		element = buffer.toString();
		        		        result.add(buffer.toString());
		                		return result;
		                	} else if (Character.isSpaceChar(ch)) {
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
		                	if (ch == '\n') {
		                		// return result so far
		                		element = buffer.toString();
		        		        if (!whitespaceSensitive) {
		        		            element = element.trim();
		        		        }
		        		        result.add(buffer.toString());
		                		return result;
		                	} else if (ch == ',') {
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
		                	if (ch == '\n') {
		                        result.add(buffer.toString());
		                        buffer.setLength(0);
		                        parseState = 0;
		                        return result;
		                	} else if (ch == '"') {
		                        buffer.append('"');
		                        parseState = 2;
		                    } else if (ch == ',') {
		                        result.add(buffer.toString());
		                        buffer.setLength(0);
		                        parseState = 0;
		                    } else if (Character.isSpaceChar(ch)) {
		                        if (whitespaceSensitive) {
		                        	isEOF = true;
		                            throw new ParseException("Cannot have trailing whitespace after close quote character", pos);
		                        }
		                        parseState = 4;
		                    } else {
		                    	isEOF = true;
		                        throw new ParseException("Cannot have trailing data after close quote character", pos);
		                    }
		                    break;
		                case 4:
		                	if (ch == '\n') {
		                		// return result so far
		                		result.add(buffer.toString());
		                		return result;
		                	} else if (Character.isSpaceChar(ch)) {
		                        // consume and ignore
		                    } else if (ch == ',') {
		                        result.add(buffer.toString());
		                        buffer.setLength(0);
		                        parseState = 0;
		                    } else {
		                    	isEOF = true;
		                        throw new ParseException("Cannot have trailing data after close quote character", pos);
		                    }
		                    break;
		                    
		                default:
		                    throw new IllegalStateException("Illegal state '" + parseState + "' in parseCsv");
		            }
		            
			        intChar = r.read();
			        pos++;
		        }
		        isEOF = true;

		        // if state is 2, we are in the middle of a quoted value
		        if (parseState == 2) {
		            throw new ParseException("Missing endquote in csv text", pos);
		        }

		        // otherwise we still need to add what's left in the buffer into the result list
		        element = buffer.toString();
		        if (parseState == 1 && !whitespaceSensitive) {
		            element = element.trim();
		        }
		        result.add(element);
		        return result;
			}
    	};
    }

    /**
     * Equivalent to <code>parseCsv(text, false);</code> (i.e. whitespace-insensitive parsing).
     * Refer to the documentation for that method for more details.
     *
     * @see #parseCsv(String, boolean)
     *
     * @param text he CSV-encoded string to parse
     * 
     * @return a List of Strings. The returned List is guaranteed to always contain at least one element.
     *
     * @throws NullPointerException if the text passed to this method is null.
     * @throws ParseException see {@link #parseCsv(String, boolean)} for details.
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

    /** Returns a javascript string. The characters <code>'</code>,
     * <code>"</code> and <code>\</code> are converted into their Unicode equivalents,
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
		return scriptPattern.matcher(sb.toString()).replaceAll("\\\\u003C$1");
        // return sb.toString();
    }


    /** Returns a javascript string. The characters <code>'</code>,
     * <code>"</code> and <code>\</code> are converted into their Unicode equivalents,
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

    /** Returns a python string, escaped so that it can be enclosed in a single-quoted string. 
     * 
     * <p>The characters <code>'</code>,
     * <code>"</code> and <code>\</code> are converted into their Unicode equivalents,
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
    
    /** Escape a filename or path component. 
     * Characters that typically have special meanings in paths (":", "/", "\") are escaped with a preceding "\" character.
     * 
     * Does not escape glob characters ( "*" or "?" ). 
     * Do not use this method to escape a full file path; when escaping a file path, escape each path component separately and then join 
     * the components with "/" characters ( see {@link #createEscapedPath(String[])} ). 
     * 
     * @param string the filename or path component to escape
     * 
     * @return the escaped form of the filename (or path component)
     */
    // Does not escape DOS special filenames ( "NUL", "CON", "LPT1" etc ). Remember those ? Of course you do.
    public static String escapePathComponent(String string) {
    	string = Text.replaceString(string, "\\", "\\\\");
    	string = Text.replaceString(string, "/", "\\/");
    	string = Text.replaceString(string, ":", "\\:");
    	return string;
    }
    
    /** Unescape a filename or path component. 
     * The escape sequences "\\" , "\:" and "\/" are converted to "\", ":" and "/" respectively.
     * All other escape sequences will raise an IllegalArgumentException 
     *  
     * <p>See {@link #splitEscapedPath(String)} to split an escaped path into components. 
     *  
     * @param pathComponent the filename or path component to unescape
     * 
     * @return the unescaped form of the filename or path component
     * 
     * @throws IllegalArgumentException if an unexpected escape is encountered, or the escape is unclosed
     */
    public static String unescapePathComponent(String pathComponent) {
    	if (pathComponent == null) {
            return null;
        }
        char c;
        boolean inEscape = false;
        StringBuilder sb = new StringBuilder(pathComponent.length());
        for (int i = 0; i < pathComponent.length(); i++) {
            c = pathComponent.charAt(i);
            if (inEscape) {
                switch (c) {
                	case '\\': 
                    case '/': // intentional fall-through
                    case ':': // intentional fall-through
                    	sb.append(c);
                        break;
                    default:
                    	throw new IllegalArgumentException("Unexpected escape '\\" + c + "' in filename");
                }
                inEscape = false;
            } else {
                switch (c) {
	                case '\\': 
	                	inEscape = true;
	                	break;
	                default:
	                	sb.append(c);
                }
            }
        }
        if (inEscape) {
        	throw new IllegalArgumentException("Unclosed escape in filename");
        }
        return sb.toString();
    }

    // need to escape the \ in a regex ( \\ ) in a String ( \\\\ )
    private static Pattern splitPathPattern = Pattern.compile("(?<!\\\\)/"); 
    
	/** Split a path, but allow forward slashes in path components if they're escaped by a preceding '\' character.
     * Individual path components returned by this method will be unescaped.
     *
     * <pre>
     * splitPath(null) = NPE
     * splitPath("") = [ "" ]
     * splitPath("abc") = [ "abc" ]
     * splitPath("abc/def/ghi") = [ "abc", "def", "ghi" ]
     * splitPath("abc\\/def/ghi") = [ "abc/def", "ghi" ]
     * </pre>
     * 
     * <p>Opposite of {@link #createEscapedPath(String[])}
     */
    public static String[] splitEscapedPath(String escapedPath) {
    	String[] result = splitPathPattern.split(escapedPath);
    	for (int i=0; i<result.length; i++) {
    		result[i] = Text.unescapePathComponent(result[i]);
    	}
    	return result;
    }
    
    /** Escapes the components of a path String, returning an escaped full path String.
     * Each path component is escaped with {@link #escapePathComponent(String)} and then joined using '/' characters.
     * 
     * <p>Opposite of {@link #splitEscapedPath(String)}.
     * 
     * @param pathComponents the filename components
     * @return an escaped path
     */
    public static String createEscapedPath(String[] pathComponents) {
    	String result = null;
    	if (pathComponents.length == 0) { 
    		throw new IllegalArgumentException("empty pathComponents"); 
    	}
    	for (String c : pathComponents) {
    		if (c==null) { 
    			throw new NullPointerException("null pathComponent"); 
    		}
    		if (result == null) {
    			result = escapePathComponent(c);
    		} else {
    			result = result + "/" + escapePathComponent(c); 
    		}
    	}
    	return result;
    }
    
    // escapeCss from ESAPI 2.0.1
    private static final String[] esapi_hex = new String[256];
	static {
		for ( char c = 0; c < 0xFF; c++ ) {
			if ( c >= 0x30 && c <= 0x39 || c >= 0x41 && c <= 0x5A || c >= 0x61 && c <= 0x7A ) {
				esapi_hex[c] = null;
			} else {
				esapi_hex[c] = toHex(c).intern();
			}
		}
	}
	private static String toHex(char c) {
		return Integer.toHexString(c);
	}
	private static String getHexForNonAlphanumeric(char c) {
		if(c<0xFF) {return esapi_hex[c]; }
		return toHex(c);
	}
    private static String encodeCssCharacter(Character c) {
		String hex = getHexForNonAlphanumeric(c);
		if ( hex == null ) { return "" + c; }
        return "\\" + hex + " ";
    }

    /**
     * Returns the CSS-escaped form of a string. 
     * 
     * <p>Characters outside of the printable ASCII range are converted to \nnnn form
     *
     * @param input the string to convert
     *
     * @return the HTML-escaped form of the string
     */
    public static String escapeCss(String input) {
    	if (input == null) { return ""; }
    	StringBuilder sb = new StringBuilder();
		for (int i = 0; i < input.length(); i++) {
			char c = input.charAt(i);
			sb.append(encodeCssCharacter(c));
		}
		return sb.toString();    	
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
     *  Shorthand for <code>(strText==null) ? strDefaultText : strText;</code>
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
     */
    public static String join(Iterable<?> elements, String delimiter) {
    	return joinWithLast(elements, false, delimiter, delimiter);
    }
    
    /** Return a string composed of a series of strings, separated with the specified delimiter.
    * Each element is contained in single quotes. The final delimeter can be set to a different
    * value, to produce text in the form <code>"'a', 'b' or 'c'"</code> or <code>"'a', 'b' and 'c'"</code>. 
    *
    * <p>There is no special handling of values containing quotes; see {@link #escapeCsv(String)} 
    *
    * @param elements The array of elements to join
    * @param isQuoted If true, each element is surrounded by single quotes
    * @param delimiter The delimiter to join each string with
    * @param lastDelimiter The delimiter to join the second-last and last elements
    *
    * @throws NullPointerException if elements or delimiter is null
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
       if (isQuoted) { sb.append("'"); }
       sb.append(elements[len - 1]);
       if (isQuoted) { sb.append("'"); }
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
     * An efficient search &amp; replace routine. Replaces all instances of
     * searchString within str with replaceString.
     *
     * @param originalString The string to search
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
     * <code>indent("\t", "abcd\nefgh")</code> would return "\tabcd\n\tefgh". If the
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
     *  @param string The string to retrieve the last component from
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
        unreserved.or(alphanum);
        unreserved.or(mark);
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
        
    }
    
    

    /**
     * Returns a comparator that compares contained numbers based on their numeric values and compares other parts
     * using the current locale's order rules.
     * <p>For example in German locale this will be a comparator that handles umlauts correctly and ignores
     * upper/lower case differences.</p>
     *
     * @return <p>A string comparator that uses the current locale's order rules and handles embedded numbers
     *         correctly.</p>
     */
    public static Comparator<String> getNaturalComparator() {
        final Collator collator = Collator.getInstance();
        return new Comparator<String>() {
            public int compare(String o1, String o2) {
                return compareNatural(collator, o1, o2);
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
    
    /** Returns a string composed of the supplied text, repeated 0 or more times 
     * 
     * @param text text to repeat
     * @param count number of repetitions
     * 
     * @return the repeated text
     */
    static public String repeat(String text, int count) {
    	StringBuffer sb = new StringBuffer();
    	for (int i=0; i<count; i++) {
    		sb.append(text);
    	}
    	return sb.toString();
    }
    
    
	/** Perform ${xxxx}-style substitution of placeholders in text. Placeholders without 
	 * values will be left as-is.
	 * 
	 * <p>For example, gives the set of variables:
	 * <ul>
	 * <li>abc = def
	 * </ul>
	 * 
	 * <p>then the result of <code>substituteParameters("xxxx${abc}yyyy${def}zzzz")</code>
	 * will be "xxxxdefyyyy${def}zzzz"
	 * 
	 * <p><code>$</code> followed by any other character will be left as-is. 
	 * 
	 * @param variables a set of variable names and values, used in the substitution 
	 * @param text the text to be substituted.
	 * 
	 * @return text, with placeholders replaced with values in the variables parameter
	 */
	public static String substitutePlaceholders(Map<?, ?> variables, String text) {
		// escaped version of (\$\{.*?\}|[^$]+|\$.)
		Pattern p = Pattern.compile("(\\$\\{.*?\\}|[^$]+|\\$)"); // modified regex
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
