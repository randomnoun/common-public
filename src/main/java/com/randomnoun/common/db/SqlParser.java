package com.randomnoun.common.db;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** A class to read an InputStream containing SQL statements (e.g. a MySQL input file) and split it into 
 * individual SQL statements.
 * 
 * <p>Some MySQL command-line directives (e.g. 'DELIMITER') are processed whilst parsing. 
 */
public class SqlParser {
	
	private static Pattern MYSQL_DELIMITER_COMMAND = Pattern.compile("delimiter\\s+(.*)$", Pattern.CASE_INSENSITIVE);
	
	// could have a ParseState that contains things like the current delimiter, or current database
	// (for 'use' commands), which can be modified by the InputStream
	
	/** Convert an InputStream of SQL statements into a List of individual
	 * statements. Statements are delimited by ";" strings that occur outside of strings or comments.
	 * The delimiter string is not included in the returned list of statements.
	 * 
	 * <p>The delimiter string may be changed using the 'delimiter str' command. 
	 * This command may end with a newline instead of a delimiter.
	 * 
	 * <p>Comments may also be returned in the result.
	 * <p>Comments are defined by '-- to-end-of-line' or '/* within slash-star star-slash *&#8288;/' syntax.
	 * Comments that are created with '--' and that occur within a statement are returned before that statement
	 * has finished parsing.
	 * 
	 * <p>NB: Does not handle escape sequences found within double or single quotes
	 * 
	 * @param includeComments include comment strings in result
	 * 
	 * @throws IOException 
	 * @throws ParseException unclosed /*-style comment or single/double-quoted string
	 */
	public List<String> parseStatements(InputStream is, boolean includeComments) 
		throws IOException, ParseException 
	{
		final List<String> allSql = new ArrayList<String>();
		consumeStatements(is, includeComments, new Consumer<String>() {
			@Override
			public void consume(String s) {
				allSql.add(s);
			}
		});
		return allSql;
	}

	public static interface Consumer<T> {
		public void consume(T s);
	}
	
	/** convert a text file of SQL statements into a List of individual
	 * statements. Comments may also be returned in the result.
	 * 
	 * <p>NB: Does not handle escape sequences found within double or single quotes
	 * 
	 * @param includeComments include comment strings in result
	 * 
	 * @throws IOException 
	 * @throws ParseException unclosed /*-style comment or single/double-quoted string
	 */
	public void consumeStatements(InputStream is, boolean includeComments, Consumer<String> consumer) 
		throws IOException, ParseException 
	{
		// @TODO some error handling
		// List allSql = new ArrayList<String>();
		
		int state = 0; // command parsing [to states 0, 1, 2, 4, 7]
		// 1 // parsed double quote, in double quotes [to states 0, 1]
		// 2 // parsed - [to states 0, 3]
		// 3 // parsed -- [ to states 0, 3]
		// 4 // parsed /  [ to states 0, 5]
		// 5 // parsed /* [ to states 6, 5]
		// 6 // parsed * from state 5  [ to states 0, 5]
		// 7 // parse single quote, in single quotes [to states 0, 1]
		// 8 // parsed delimiter character [to states 0, 7]

		String s = ""; // current statement
		String c = ""; // current comment
		String delimiter = ";"; // default delimiter
		int intch = is.read();
		int delimIdx = 0; // number of delimiter characters read
		while (intch!=-1) {
			char ch = (char) intch;
			
			if (state==0) {
				if (ch == delimiter.charAt(delimIdx)) {
					delimIdx++;
					if (delimiter.length()==delimIdx) {
						s = s.trim(); delimIdx = 0; // was s.trim() + ";"
						// could check for all commands at http://dev.mysql.com/doc/refman/5.7/en/mysql-commands.html
						// but let's just implement 'DELIMITER' for now
						Matcher m = MYSQL_DELIMITER_COMMAND.matcher(s);
						if (m.matches()) {
							delimiter = m.group(1); // set the new delimiter
						} else {
							consumer.consume(s); 
						}
						s="";
					} else {
						state = 0; 
					}
				} else {
					if (delimIdx>0) {
						// could push these back onto the inputStream in case the delimiter startsWith a " or -, but that seems a bit fiddly
						s = s + delimiter.substring(0, delimIdx); delimIdx = 0; 
					}
					switch(ch) {
						case '"' : state = 1; s = s + ch; break;
						case '-' : state = 2; break;
						case '/' : state = 4; break;
						case '\'' : state = 7; s = s + ch; break;
						case '\r' :
						case '\n' :
							// if this is a delimiter command, process it
							Matcher m = MYSQL_DELIMITER_COMMAND.matcher(s.trim());
							if (m.matches()) {
								delimiter = m.group(1); // set the new delimiter
								s = ""; delimIdx = 0;
							} else {
								s = s + ch;
							}
							break;
						default: s = s + ch;
					}
				}
				
			} else if (state==1) {
				switch(ch) {
					case '"' : state = 0; s = s + ch; break;
					default: s = s + ch;
				}
			} else if (state==2) {
				switch(ch) {
					case '-' : state = 3; break;
					default: state = 0; s = s + "-" + ch;
				}
			} else if (state==3) {
				switch(ch) {
					case '\r' : 
					case '\n' : 
						state = 0; 
						if (includeComments) { consumer.consume("-- " + c.trim()); } 
						c=""; break;
					default :
						c = c + ch;
				}
			} else if (state==4) {
				switch(ch) {
					case '*' : state = 5; break;
					default: state = 0; s = s + "/" + ch;
				}
			} else if (state==5) {
				switch(ch) {
					case '*' : state = 6; break;
					default: c = c + ch;
				}
			} else if (state==6) {
				switch(ch) {
					case '/' : 
						state = 0; 
						if (includeComments) { s += ("/* " + c.trim() + " */"); } // was consumer.consume(...) 
						c=""; break;
					default: c = c + "*" + ch;
				}
			} else if (state==7) {
				switch(ch) {
					case '\'' : state = 0; s = s + ch; break;
					default: s = s + ch;
				}
			} 
			
			intch = is.read();
		}
		
		if (state==5) {
			// unclosed /*-style comment
			// ignore for the time being
			throw new ParseException("Unclosed /*-style comment before EOF", -1);
		} else if (state==1) {
			// unclosed quoted string
			// ignore for the time being
			throw new ParseException("Unclosed double quoted string before EOF", -1);
		} else if (state==7) {
			// unclosed quoted string
			// ignore for the time being
			throw new ParseException("Unclosed single quoted string before EOF", -1);
		} else if (state==0) {
			if (!s.trim().equals("")) {
				// unterminated statement at end of InputStream; add to list
				consumer.consume(s.trim());
			}
		}
		
		// return allSql;
	}



}
