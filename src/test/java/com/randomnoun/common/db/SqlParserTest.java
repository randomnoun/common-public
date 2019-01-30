package com.randomnoun.common.db;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.ParseException;

import org.junit.Before;
import org.junit.Test;

import com.randomnoun.common.Struct;

public class SqlParserTest {

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testParseStatements() throws IOException, ParseException {
		SqlParser p = new SqlParser();
		String s1 = "statement1; statement2; statement3; /* a comment */ statement /* inside */ 4;";
		String[] result = p.parseStatements(new ByteArrayInputStream(s1.getBytes()), true).toArray(new String[]{});
		String[] expected = new String[] { 
			"statement1", 
			"statement2", 
			"statement3", 
			"/* a comment */ statement /* inside */ 4" };
		assertArrayEquals(expected,  result);
		
		result = p.parseStatements(new ByteArrayInputStream(s1.getBytes()), false).toArray(new String[]{});
		expected = new String[] { 
			"statement1", 
			"statement2", 
			"statement3", 
			"statement  4" };
		assertArrayEquals(expected,  result);
		// System.out.println(Struct.structuredListToString("result",p.parseStatements(new ByteArrayInputStream(s1.getBytes()),false)));
		
		
		String s2 = "--things\n" +
			"a multi-line -- more things\n" +
			"statement with -- yet more things\n" +
			"eol comments \"-- not this one /* or this */ of course--\" embedded within -- things?\n" +
			"it;";
		result = p.parseStatements(new ByteArrayInputStream(s2.getBytes()), true).toArray(new String[]{});
		expected = new String[] { 
			"-- things", 
			"-- more things", 
			"-- yet more things", 
			"-- things?", 
			"a multi-line statement with eol comments \"-- not this one /* or this */ of course--\" embedded within it" };
		assertArrayEquals(expected,  result);

		result = p.parseStatements(new ByteArrayInputStream(s2.getBytes()), false).toArray(new String[]{});
		expected = new String[] { 
			"a multi-line statement with eol comments \"-- not this one /* or this */ of course--\" embedded within it" };
		assertArrayEquals(expected,  result);

		
		String s3 = "a statement; followed by a statement not terminated with a semi-colon";
		result = p.parseStatements(new ByteArrayInputStream(s3.getBytes()), true).toArray(new String[]{});
		expected = new String[] { 
			"a statement",
			"followed by a statement not terminated with a semi-colon" };
		assertArrayEquals(expected,  result);
		
		
		try {
			String s4 = "a statement; followed by a statement with an unclosed \"quoted string";
			System.out.println(Struct.structuredListToString("result",p.parseStatements(new ByteArrayInputStream(s4.getBytes()), true)));
			fail("Expected ParseException");
		} catch (ParseException pe) {
			// fine
		}

		try {
			String s5 = "a statement; followed by a statement with an unclosed /* comment";
			System.out.println(Struct.structuredListToString("result",p.parseStatements(new ByteArrayInputStream(s5.getBytes()), true)));
			fail("Expected ParseException");
		} catch (ParseException pe) {
			// fine
		}
		
		String s6 = "ending with semi-colon; delimiter $$; and; now $$ things are delimited by dollars $$ delimiter ;$$ back to semicolon; hopefully\n";
		result = p.parseStatements(new ByteArrayInputStream(s6.getBytes()), true).toArray(new String[]{});
		expected = new String[] { 
			"ending with semi-colon",
			"and; now",
			"things are delimited by dollars",
			"back to semicolon",
			"hopefully"
		};
		assertArrayEquals(expected,  result);
		
		
		// the 'delimiter' command can also end with newlines without it's own delimiter
		String s7 = "ending with semi-colon; delimiter $$\n" +
		  "and; now $$ things are delimited by dollars $$ delimiter ;\n" +
		  "back to semicolon; hopefully\n";
		result = p.parseStatements(new ByteArrayInputStream(s7.getBytes()), true).toArray(new String[]{});
		expected = new String[] { 
			"ending with semi-colon",
			"and; now",
			"things are delimited by dollars",
			"back to semicolon",
			"hopefully"
		};
		assertArrayEquals(expected,  result);
		
		String s8 = "statement1; a='escaped quotes (\\' and \\\") in single quotes'; statement3;";
		result = p.parseStatements(new ByteArrayInputStream(s8.getBytes()), true).toArray(new String[]{});
		expected = new String[] { 
			"statement1", 
			"a='escaped quotes (\\' and \\\") in single quotes'", 
			"statement3"};
		assertArrayEquals(expected,  result);

		String s9 = "statement1; a=\"escaped quotes (\\' and \\\") in double quotes\"; statement3;";
		result = p.parseStatements(new ByteArrayInputStream(s9.getBytes()), true).toArray(new String[]{});
		expected = new String[] { 
			"statement1", 
			"a=\"escaped quotes (\\' and \\\") in double quotes\"", 
			"statement3"};
		assertArrayEquals(expected,  result);

		
	}

	@Test
	public void testConsumeStatements() {
		//fail("Not yet implemented");
	}

}
