package com.randomnoun.common;

/* (c) 2013 randomnoun. All Rights Reserved. This work is licensed under a
 * BSD Simplified License. (http://www.randomnoun.com/bsd-simplified.html)
 */

import java.util.HashMap;
import java.util.Map;

/** Case-preserving hashmap that allows case-insensitive lookups.
 * 
 * @author knoxg
 * 
 * 
 */
public class HashMapIgnoreCase<K, V> extends HashMap<String, V> {
    
	/** generated  serialVersionUID */
	private static final long serialVersionUID = -6469387933015736361L;
	
	/** an extra map of lower-case keys to case-sensitive keys */
	private Map<String, String> lowercase = new HashMap<String, String>();
	
	/**
	 * Retrieve a value from the cache.
	 *
	 * @param key key whose associated value is to be returned
	 *
	 * @return the value to which this map maps the specified key, or
	 *   null if the map contains no mapping for this key.
	 */
	@Override
	public V get(Object key)
	{
		// System.out.println("getting " + lowercase.get(key.toLowerCase()));
		return super.get(lowercase.get(((String)key).toLowerCase()));
	}
	
	/**
	 * Associates the specified value with the specified key in this map.
	 * If the map previously contained a mapping for this key, the old
	 * value is replaced.
	 *
	 * @param key key with which the specified value is to be associated.
	 * @param value value to be associated with the specified key.
	 * 
	 * @return previous value associated with specified key, or <code>null</code>
	 *	       if there was no mapping for key.  A <code>null</code> return can
	 *	       also indicate that the HashMap previously associated
	 *	       <code>null</code> with the specified key.
	 *
	 * @throws IllegalArgumentException if the key parameter is not a String
	 */
	@Override
	public V put(String key, V value) {
		if (!(key instanceof String)) {
			throw new IllegalArgumentException("HashMapIgnoreCase keys must be Strings");
		}
		
		String lowercaseKey = ((String)key).toLowerCase();
		V previousEntry = null;
		
		// remove any existing entry with the same case-insensitive value
		if (lowercase.containsKey(lowercaseKey)) {
			//System.out.println("Removing " + lowercase.get(lowercaseKey));
			previousEntry = super.remove(lowercase.get(lowercaseKey));
		}
		lowercase.put(lowercaseKey, key);
		//System.out.println("putting " + key + "/" + value);
		super.put(key, value);
		return previousEntry;
	}
	
	/** Returns true if this map contains a mapping for the specified key. 
	 * More formally, returns true if and only if this map contains at 
	 * a mapping for a key <code>k</code> such that 
	 * (<code>key==null ? k==null : key.equals(k)</code>). (There can be at 
	 * most one such mapping.)
	 *  
	 * @param key key whose presence in this map is to be tested. 
	 * 
	 * @return true if this map contains a mapping for the specified key. 
	 */
	@Override
	public boolean containsKey(Object key) {
		if (!(key instanceof String)) {
			return false;
		}
		String lowercaseKey = ((String)key).toLowerCase();
		return lowercase.containsKey(lowercaseKey);
	}
	

	/**
	 * Removes the mapping for this key from this map if present.
	 *
	 * @param  key key whose mapping is to be removed from the map.
	 * @return previous value associated with specified key, or <code>null</code>
	 *	       if there was no mapping for key.  A <code>null</code> return can
	 *	       also indicate that the map previously associated <code>null</code>
	 *	       with the specified key.
	 */
	@Override
	public V remove(Object key) {
		V previousEntry;
		String lowercaseKey = ((String) key).toLowerCase();
		previousEntry = super.remove(lowercase.get(lowercaseKey));
		lowercase.remove(lowercaseKey);
		return previousEntry;
	}
	

}
