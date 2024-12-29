package com.randomnoun.common;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.text.ParseException;
import java.util.Properties;

import com.randomnoun.common.PropertyParser.PropertiesWithComments;

import junit.framework.TestCase;

public class PropertyParserTest
    extends TestCase
{
	public PropertyParserTest(String name)
    {
        super(name);
    }

    public void testSimpleProps() throws ParseException, IOException {
    	String in = 
    		"a=b\n" +
    		"# regular comment with an = in it\n" +
    		"c=d";
    			
    	Reader r = new StringReader(in);
    	PropertyParser pp = new PropertyParser(r);
    	Properties p = pp.parse();
    	
        assertEquals(2, p.size());
        assertEquals("b", p.getProperty("a"));
        assertEquals("d", p.getProperty("c"));
        
        assertEquals(0, ((PropertiesWithComments) p).getComments().size());
    }

    public void testPropsWithComments() throws ParseException, IOException {
    	String in =
    		"## comment for a\n" +
    		"a=b\n" +
    		"## comment for c\n" +
    		"# that extends over multiple lines\n" +
    		"c=d\n" +
    		"e=f";
    			
    	Reader r = new StringReader(in);
    	PropertyParser pp = new PropertyParser(r);
    	PropertiesWithComments p = pp.parse();

        assertEquals(3, p.size());
        assertEquals("b", p.getProperty("a"));
        assertEquals("d", p.getProperty("c"));
        assertEquals("f", p.getProperty("e"));

        assertEquals("comment for a", p.getComment("a"));
        assertEquals("comment for c\nthat extends over multiple lines", p.getComment("c"));
        assertEquals(null, p.getComment("e"));
        assertEquals(2, p.getComments().size());

    }

}
