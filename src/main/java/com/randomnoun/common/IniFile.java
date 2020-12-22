package com.randomnoun.common;

/* (c) 2013 randomnoun. All Rights Reserved. This work is licensed under a
 * BSD Simplified License. (http://www.randomnoun.com/bsd-simplified.html)
 */

import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.InputStreamReader;
import java.text.ParseException;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;


/** Windows .ini file parser
 * 
 * Properties without explicit groups are recorded with a null group
 * 
 * 
 */
public class IniFile {


	/** Map of property groups to property name-value pairs */
	private Map<String, Map<String, String>> properties;
	
	public IniFile() {
		properties = new HashMap<String, Map<String, String>>();
	
	}
	
	public void load(File file) throws IOException, ParseException {
		InputStream is = new FileInputStream(file);
		load(is);
	}
	
	public void load(InputStream is) throws IOException, ParseException {
		LineNumberReader lnr = new LineNumberReader(new InputStreamReader(is));
		try {
			String line = lnr.readLine();
			String group = null;
			while(line!=null) {
				// @TODO handle newlines 
				line = line.trim();
				if (line.startsWith(";")) {
					// comment; ignore
				} else if (line.startsWith("[")) {
					if (line.endsWith("]")) {
						group = line.substring(1, line.length()-1);
					} else {
						throw new ParseException("Line " + lnr.getLineNumber() + ": invalid .ini group declaration", 0);
					}
				} else if (line.contains("=")) {
					int pos = line.indexOf("=");
					String name = line.substring(0, pos);
					String value = line.substring(pos+1);
					Map<String, String> propMap = properties.get(group);
					if (propMap==null) {
						propMap = new HashMap<String, String>();
						properties.put(group, propMap);
					}
					propMap.put(name, value);
				}
				
				line=lnr.readLine();
			}
		} finally {
			try {
				is.close();	
			} catch (IOException ioe) {
				// ignore
			}
		}
	}
	
	public String get(String group, String key) {
		Map<String, String> propMap = properties.get(group);
		if (propMap==null) { return null; }
		return propMap.get(key);
	}
	
	public Set<String> getGroups() {
		return properties.keySet();
	}
	
	public Set<String> getGroupKeys(String group) {
		Map<String, String> propMap = properties.get(group);
		if (propMap==null) { return null; }
		return propMap.keySet();
	}
	
}
