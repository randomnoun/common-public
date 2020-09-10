package com.randomnoun.common;

import junit.framework.TestCase;

/**
 * 
 * @author knoxg
 */
public class HashMapIgnoreCaseTest
    extends TestCase
{
    /**
     * Constructor for HashMapIgnoreCaseTest.
     * 
     * @param name test name
     */
    public HashMapIgnoreCaseTest(String name) {
        super(name);
    }

    /**
     * Test the map methods :)
     */
    public void testMap() {
		HashMapIgnoreCase<String, String> map = new HashMapIgnoreCase<>();
    	map.put("abc", "def");
    	map.put("AbC", "ghi");
    	assertEquals(1, map.size());
    	assertEquals(true, map.containsKey("abc"));
		assertEquals(true, map.containsKey("ABc"));
		assertEquals(false, map.containsKey("ghi"));
		
    	assertEquals("ghi", map.get("abc"));
		assertEquals("ghi", map.get("AbC"));
		assertEquals("ghi", map.get("ABC"));
		assertEquals(null, map.get("ghi"));
		
		assertEquals("ghi", map.remove("ABC"));
		assertEquals(0, map.size());
    }
    
	public static void main(String[] args) {
		junit.textui.TestRunner.run(new HashMapIgnoreCaseTest("testMap"));
	}    
}
