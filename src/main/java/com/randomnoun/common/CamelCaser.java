package com.randomnoun.common;

/* (c) 2013 randomnoun. All Rights Reserved. This work is licensed under a
 * BSD Simplified License. (http://www.randomnoun.com/bsd-simplified.html)
 */

import java.util.*;


/**
 * This class is used to convert the Maps and Lists returned by Spring DAO into
 * camelCase versions, which can be used when dealing with some databases 
 * that return all column names in upper case. 
 * 
 * <p>Maps and Lists passed to this object will be modified in-place.
 *
 * <p>This object should be considered thread-safe, i.e. many threads may use a
 * single instance of this object at a time.
 *
 * @author knoxg
 */
public class CamelCaser {
    
    

    /** Internal map whose keys are ALLUPPERCASE text strings, and values are camelCase versions */
    private Map<String, String> namingMap;

    /**
     * Create a new map renamer. The list supplied to this constructor contains
     * a list of Strings that may be used as keys in the maps to be renamed. Each
     * String is the camelCase version to return.
     *
     * @param nameList
     */
    public CamelCaser(List<String> nameList) {
        this.populateNamingMap(nameList);
    }

    /**
     * Creates a new CamelCaser object
     *
     * @param camelCaseNames A list of camelcased names, separated by commas.
     *
     * @throws NullPointerException if there was a problem loading the resource property list,
     *   or the propertyKey does not exist in the resource.
     */
    public CamelCaser(String camelCaseNames) {
        List<String> nameList = new ArrayList<String>();
        StringTokenizer tokenizer = new StringTokenizer(camelCaseNames, ",");
        String token;

        while (tokenizer.hasMoreElements()) {
            token = tokenizer.nextToken();
            nameList.add(token.trim());
        }

        populateNamingMap(nameList);
    }

    /** Private method used to create the namingMap object */
    private void populateNamingMap(List<String> nameList) {
        // don't croak if we're passed a null list 
        if (nameList == null) {
            nameList = new ArrayList<String>(0);
        }

        namingMap = new HashMap<String, String>();

        String name;

        for (Iterator<String> i = nameList.iterator(); i.hasNext();) {
            name = (String) i.next();
            namingMap.put(name.toLowerCase(), name);
        }
    }

    /** Renames an individual string. If the camelCase version of the string is not
     *  known, then it is returned without modification.
     *
     * @param string The String to be renamed
     * @return The camelCase version of the String
     */
    public String renameString(String string) {
        String result = (String) namingMap.get(string.toLowerCase());

        if (result == null) {
            result = string;
        }

        return result;
    }

    /** Renames all the keys in the map. The map may itself contain Maps and Lists,
     * which will also be renamed.
     *
     * @param map The map to rename
     */
    @SuppressWarnings("unchecked")
	public void renameMap(Map<String, Object> map) {
        Object key;
        Object value;
        String newKey;


        // we iterator on this object rather than map.keySet().iterator(), 
        // since we are modifying the keys in the map
        // as we iterate. 
        List<String> keyList = new ArrayList<String>(map.keySet());

        for (Iterator<String> i = keyList.iterator(); i.hasNext();) {
            key = i.next();
            value = map.get(key);

            if (key instanceof String) {
                newKey = renameString((String) key);
                map.remove(key);
                map.put(newKey, value);
            }

            // recurse through this structure
            if (value instanceof Map) {
                renameMap((Map<String, Object>) value);
            } else if (value instanceof List) {
                renameList((List<?>) value);
            }
        }
    }

    /** Renames all the objects in a list. Any Maps or Lists contained with the List
     * will be recursed into.
     *
     * @param map The list containing maps to rename.
     */
    @SuppressWarnings("unchecked")
	public void renameList(List<? extends Object> list) {
        Object obj;

        for (Iterator<? extends Object> i = list.iterator(); i.hasNext();) {
            obj = i.next();

            // recurse through this structure
            if (obj instanceof Map) {
                renameMap((Map<String, Object>) obj);
            } else if (obj instanceof List) {
                renameList((List<?>) obj);
            }
        }
    }
}
