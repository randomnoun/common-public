package com.randomnoun.common;

/* (c) 2013 randomnoun. All Rights Reserved. This work is licensed under a
 * BSD Simplified License. (http://www.randomnoun.com/bsd-simplified.html)
 */

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;



import org.apache.commons.beanutils.PropertyUtils;

import com.randomnoun.common.Struct;
import com.randomnoun.common.Text;

/**
 * Encoder/decoder of JavaBeans and 'structured' maps and lists. A structured
 * map or list is any Map or List that satisfies certain conventions, listed below.
 *
 * <p>A structured map is any implementation of the {@link java.util.Map}
 * interface, in which every key is a {@link String}, and every value is either a
 * primitive wrapper type ({@link String}, {@link Long},
 * {@link Integer}, etc...), a structured map or a structured list.
 * A structured list is any implementation of the {@link java.util.List} interface,
 * of which every value is a primitive wrapper type, a structured map or a structured list.
 *
 * <p>In this way, arbitrarily complex objects can be created and passed between
 * Struts code and the business layer, or Struts code and the JSP layer,
 * without resorting to the creation of application-specific
 * datatypes. These datatypes are also used by the Spring JdbcTemplate framework to
 * return values from a database, so it is useful to have some generic functions
 * that operate on them.
 *
 * 
 * @author knoxg
 */
public class Struct {
    
    


    /** A ConcurrentReaderHashMap that maps Class objects to Maps, each of which
     *  maps getter method names (e.g. getabcd) to Method objects. Method names should be
     *  lower-cased when elements are added or looked up in this map, to provide
     *  case-insensitivity.
     */
    private static Map<Class<?>, Map<String,Method>> gettersCache;

    /** A ConcurrentReaderHashMap that maps Class objects to Maps, each of which
     *  maps setter method names (e.g. setabcd) to Method objects. Method names should be
     *  lower-cased when elements are added or looked up in this map, to provide
     *  case-insensitivity.
     */
    private static Map<Class<?>, Map<String,Method>> settersCache;

    /** Serialise Date objects using the Microsoft convention for Dates.*/
	public static final String DATE_FORMAT_MICROSOFT = "microsoft";
	
	/** Serialise Date objects as milliseconds since the epoch */
	public static final String DATE_FORMAT_NUMERIC = "numeric";

	/* which you'd probably mark as a method annotation instead */
	/** This class can be serialised as a JSON value by calling it's toString() method */ 
	public static interface ToStringReturnsJson { }
	
	/** This class can be serialised as a JSON value by calling it's toJson() method */
	public static interface ToJson { 
		public String toJson(); 
	}
	/** This class can be serialised as a JSON value by calling it's toJson(String) method. 
     * Multiple json formats are supported by supplying a jsonFormat string; e.g. 'simple'. 
     * Passing null or an empty string should be equivalent to calling toJson() if the class also implements he Struct.ToJson interface.
     */
	public static interface ToJsonFormat { 
		public String toJson(String jsonFormat); 
	}
    
    /** A Comparator which performs comparisons between two rows in a structured list (used
     *  in sorting). This comparator is equivalent to an 'ORDER BY' on a single field
     *  returned by the Spring JdbcTemplate method.
     */
    public static class StructuredListComparator
        implements Comparator {
        /** The key field to sort on */
        private String fieldName;

        /** Create a new StructuredListComparator object, which will sort on the
         *  supplied field.
         *
         * @param fieldName The name of the field to sort on
         */
        public StructuredListComparator(String fieldName) {
            this.fieldName = fieldName;
        }

        /** Compare two structured list elements
         *
         * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
         */
        public int compare(Object a, Object b)
            throws IllegalArgumentException {
            if (!(a instanceof Map)) {
                throw new IllegalArgumentException("List must be composed of Maps");
            }
            if (!(b instanceof Map)) {
                throw new IllegalArgumentException("List must be composed of Maps");
            }

            Map mapA = (Map) a;
            Map mapB = (Map) b;
            if (!mapA.containsKey(fieldName)) {
                throw new IllegalArgumentException("keyField '" + fieldName + "' not found in Map");
            }
            if (!mapB.containsKey(fieldName)) {
                throw new IllegalArgumentException("keyField '" + fieldName + "' not found in Map");
            }

            Object objectA = mapA.get(fieldName);
            if (!(objectA instanceof Comparable)) {
                throw new IllegalArgumentException("keyField '" + fieldName + "' element must implement Comparable");
            }
            return ((Comparable) objectA).compareTo(mapB.get(fieldName));
        }
    }

    /** A comparator that can deal with nulls (unlike the one in TreeSet) */
    public static class ListContainingNullComparator
        implements Comparator {
        public int compare(Object o1, Object o2) {
            if (o1 == null && o2 == null) {
                return 0;
            } else if (o1 == null) {
                return -1;
            } else if (o2 == null) {
                return 1;
            }
            return ((Comparable) o1).compareTo(o2);
        }
    }

    /** A Comparator which performs comparisons between two rows in a structured list (used
     *  in sorting), keyed on a case-insensitive String value. This comparator is similar to an 
     * 'ORDER BY' on a single field returned by the Spring JdbcTemplate method.
     */
    public static class StructuredListComparatorIgnoreCase
        implements Comparator {

        /** The key field to sort on */
        private String fieldName;

        /** Create a new StructuredListComparatorIgnoreCase object, which will sort on the
         *  supplied field.
         *
         * @param fieldName The name of the field to sort on
         */
        public StructuredListComparatorIgnoreCase(String fieldName) {
            this.fieldName = fieldName;
        }

        /** Compare two structured list elements
         *
         * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
         */
        public int compare(Object a, Object b)
            throws IllegalArgumentException {
            if (!(a instanceof Map)) {
                throw new IllegalArgumentException("List must be composed of Maps");
            }
            if (!(b instanceof Map)) {
                throw new IllegalArgumentException("List must be composed of Maps");
            }

            Map mapA = (Map) a;
            Map mapB = (Map) b;
            if (!mapA.containsKey(fieldName)) {
                throw new IllegalArgumentException("keyField '" + fieldName + "' not found in Map");
            }
            if (!mapB.containsKey(fieldName)) {
                throw new IllegalArgumentException("keyField '" + fieldName + "' not found in Map");
            }

            String stringA = (String) mapA.get(fieldName);
            return stringA.compareToIgnoreCase((String) mapB.get(fieldName));
        }
    }


    /** Sorts a structured list on the supplied key field
     *
     * @param list The list to search.
     * @param keyField The name of the key field.
     *
     * @throws NullPointerException if list or keyField is set to null.
     * @throws IllegalStateException if the list is not composed of Maps
     */
    static public void sortStructuredList(List list, String keyField) {
        if (list == null) { throw new NullPointerException("Cannot search null list"); }
        if (keyField == null) { throw new NullPointerException("Cannot search for null keyField"); }

        Comparator comparator = new StructuredListComparator(keyField);
        Collections.sort(list, comparator);
    }

    /** Sorts a structured list on the supplied key field; the key value is sorted
     * case-insensitively (it must be of type String or a ClassCastException will occur).
     *
     * @param list The list to search.
     * @param keyField The name of the key field.
     *
     * @throws NullPointerException if list or keyField is set to null.
     * @throws IllegalStateException if the list is not composed of Maps
     */
    static public void sortStructuredListIgnoreCase(List list, String keyField) {
        if (list == null) { throw new NullPointerException("Cannot search null list"); }
        if (keyField == null) { throw new NullPointerException("Cannot search for null keyField"); }

        Comparator comparator = new StructuredListComparatorIgnoreCase(keyField);
        Collections.sort(list, comparator);
    }


    /** Set all fields in object 'obj' using values in request. Each request parameter
     *  is checked against the object to see if a bean setter method exists for that
     *  parameter name. If it does, then it is invoked, using the parameter value
     *  as the setter argument. This method uses the
     * {@link Struct#setValue(Object, String, Object, boolean, boolean, boolean)} method
     *  to dynamically create map and list elements as required if the object being
     *  set implements the Map interface.
     *
     *  <p>e.g. if passed a Map object, and the HttpServletRequest has a parameter named
     *  "<tt>table[12].id</tt>" with the value "<tt>1234</tt>", then:
     *  <ul><li>a List object named "table" is created
     *  in the Map,
     *  <li>the list is increased to allow at least 12 elements,
     *  <li> that element is set to a new Map object ...
     *  <li> ... which contains the String key "id", which is assigned the  value "1234".
     *  </ul>
     *  <p>This processing allows developers to pass arbitrarily complex structures through
     *  from HttpServletRequest name/value pairs.
     *
     *  <p>This method operates on both structured objects (Maps/Lists) or 
     *  data-transfer objects (DTOs). For example, if passed a bean-like Object which had a getTable() method that
     *  returned a List, then this would be used to perform the processing above. (If the
     *  List returned a Map at index 12, then an 'id' key would be created as before; if
     *  the List returned another Object at index 12, then the setId() method would be
     *  called instead; if the list returned null at index 12, then a Map would be created as before).
     *
     *  <p>NB: If an object is not a Map, and does not have the appropriate setter() method,
     *  then this function will *not* raise an exception. This is intentional behaviour -
     *  (use {@link #setValue(Object, String, Object, boolean, boolean, boolean)} if you
     *  need more fine-grained control.
     *
     *  <p>You can limit the setters that are invoked by using the
     *  {@see #setFromRequest(Object, HttpServletRequest, String[])} form of this method.
     *
     * @param obj The object being set.
     * @param request The request containing the source data
     *
     * @throws RuntimeException if an invocation target exception occurred whilst
     *   calling the object's set() methods.
     */
    public static void setFromRequest(Object obj, HttpServletRequest request) {
        setFromRequest(obj, request, null);
    }

    /** Set all fields in object 'obj' using values in request. Only the request
     *  parameter names passed in through the 'fields' argument will be used
     *  to populate the object. If the named parameter does not exist in the request,
     *  then the setter for that parameter is not invoked.
     *
     *  <p>See {@see #setFromRequest(Object, HttpServletRequest)} for more information
     *  on how this method operates.
     *
     * @param obj The object being set.
     * @param request The request containing the source data
     * @param fields An array of strings, denoting the fields that are sourced from
     *   the request. All other parameters in the request are ignored.
     *
     * @throws RuntimeException if an invocation target exception occurred whilst
     *   calling the object's set() methods.
     */
    public static void setFromRequest(Object obj, HttpServletRequest request, String[] fields) {
        Iterator paramListIter;

        if (fields == null) {
            paramListIter = request.getParameterMap().keySet().iterator();
        } else {
            paramListIter = Arrays.asList(fields).iterator();
        }

        while (paramListIter.hasNext()) {
            String parameter = (String) paramListIter.next();
            if (parameter.endsWith("[]")) {
            	parameter = parameter.substring(0, parameter.length()-2);
            	String values[] = request.getParameterValues(parameter);
            	if (values!=null) {
	            	for (int i=0; i<values.length; i++) {
	            		values[i] = Text.replaceString(values[i], "\015\012", "\n");
	            	}
            	}
            	setValue(obj, parameter, values, true, true, true);
            } else {
	            String value = request.getParameter(parameter);
	            
	            // convert CR-LFs into java newlines 
	            value = Text.replaceString(value, "\015\012", "\n");
	
	            // The null check below is commented out because we need to be able to pass
	            // null values through in order to set booleans values for checkboxes
	            // that are unchecked (these are represented in HTTP by missing (null) parameters)
	
	            //if (value != null) { 
	            setValue(obj, parameter, value, true, true, true);
	            //}
            }
        }
    }


    /** Similar to the {@link #setFromRequest(Object, HttpServletRequest)} method, but
     *  uses a Map instead of a request.
     * 
     * @param obj The object being set.
     * @param map The Map containing the source data
     * @param ignoreMissingSetter passed to {@link #setValue(Object, String, Object, boolean, boolean, boolean)} 
     * @param convertStrings passed to {@link #setValue(Object, String, Object, boolean, boolean, boolean)}
     * @param createMissingElements passed to {@link #setValue(Object, String, Object, boolean, boolean, boolean)}
     */
    public static void setFromMap(Object obj, Map map, 
      boolean ignoreMissingSetter, boolean convertStrings, boolean createMissingElements) 
    {
        setFromMap(obj, map, ignoreMissingSetter, convertStrings, createMissingElements, null); 
    }

    /** Similar to the {@link #setFromRequest(Object, HttpServletRequest, String[])} method, but
     *  uses a Map instead of a request.
     * 
     * @param obj The object being set.
     * @param map The Map containing the source data
     * @param ignoreMissingSetter passed to {@link #setValue(Object, String, Object, boolean, boolean, boolean)} 
     * @param convertStrings passed to {@link #setValue(Object, String, Object, boolean, boolean, boolean)}
     * @param createMissingElements passed to {@link #setValue(Object, String, Object, boolean, boolean, boolean)}
     * @param fields An array of strings, denoting the fields that are sourced from
     *   the request. All other parameters in the request are ignored.
     */
    public static void setFromMap(Object obj, Map map, 
      boolean ignoreMissingSetter, boolean convertStrings, boolean createMissingElements, 
      String[] fields) 
    {
        if (fields == null) {
            for (Iterator i = map.entrySet().iterator(); i.hasNext(); ) {
                Map.Entry entry = (Map.Entry) i.next();
                setValue(obj, (String) entry.getKey(), entry.getValue(), ignoreMissingSetter, convertStrings, createMissingElements);
            }
        } else {
            for (int i = 0; i<fields.length; i++) {
                setValue(obj, fields[i], map.get(fields[i]), ignoreMissingSetter, convertStrings, createMissingElements);
            }
        }
    }

    /** Similar to the {@link #setFromRequest(Object, HttpServletRequest, String[])} method, but
     *  uses a Map instead of a request.
     * 
     * @param targetObj The object being set.
     * @param sourceObj The Map containing the source data
     * @param ignoreMissingSetter passed to {@link #setValue(Object, String, Object, boolean, boolean, boolean)} 
     * @param convertStrings passed to {@link #setValue(Object, String, Object, boolean, boolean, boolean)}
     * @param createMissingElements passed to {@link #setValue(Object, String, Object, boolean, boolean, boolean)}
     * @param fields An array of strings, denoting the fields that are sourced from
     *   the request. All other parameters in the request are ignored.
     */
    public static void setFromObject(Object targetObj, Object sourceObj, 
      boolean ignoreMissingSetter, boolean convertStrings, boolean createMissingElements, 
      String[] fields) 
    {
    	// probably something in bean-utils for all of this
        if (fields == null) {
        	/*
            for (Iterator i = sourceObj.entrySet().iterator(); i.hasNext(); ) {
                Map.Entry entry = (Map.Entry) i.next();
                setValue(targetObj, (String) entry.getKey(), entry.getValue(), ignoreMissingSetter, convertStrings, createMissingElements);
            }
            */
        	throw new UnsupportedOperationException("null field list not supported");
        } else {
            for (int i = 0; i<fields.length; i++) {
                setValue(targetObj, fields[i], getValue(sourceObj, fields[i]), ignoreMissingSetter, convertStrings, createMissingElements);
            }
        }
    }
    
    

    /** Return a getter Method for a class. 
     *
     * <p>(Getters are cached by this class)
     *
     * @param clazz The class we are retrieving the getter Method for
     * @param propertyName The name of the property on this class; e.g. "asd" will return the
     *   method "getAsd()"
     * 
     * @return the getter Method for the supplied class.
     */
    private static Method getGetterMethod(Class<?> clazz, String propertyName) {
        Map<String,Method> getters = (Map<String,Method>) gettersCache.get(clazz);

        if (getters == null) {
            getters = new HashMap<String,Method>();

            Method[] methods = clazz.getMethods();
            Method method;

            // get a list of setters this object contains
            for (int i = 0; i < methods.length; i++) {
                method = methods[i];

                if (method.getName().startsWith("get") && method.getParameterTypes().length == 1) {
                    String property = method.getName().substring(3);
                    getters.put(property.toLowerCase(), method);
                }
            }
            gettersCache.put(clazz, getters);
        }

        return (Method) getters.get(propertyName.toLowerCase());
    }

    /** Return a setter method for a class. Note that if a class supplies multiple setters (with
     * one parameter each), then one will be arbitrarily chosen. So don't have multiple setters ! 
     *
     * <p>(Setters are cached by this class)
     *
     * @param clazz The class we are retrieving the setter Method for
     * @param propertyName The name of the property on this class; e.g. "asd" will return the
     *   first "setAsd(...)" found on this class with one parameter.
     * 
     * @return The setter Method for the supplied class
     */
    private static Method getSetterMethod(Class<?> clazz, String propertyName) {
        Map<String,Method> setters = (Map<String, Method>) settersCache.get(clazz);

        if (setters == null) {
            setters = new HashMap<String,Method>();

            Method[] methods = clazz.getMethods();
            Method method;

            // get a list of setters this object contains
            for (int i = 0; i < methods.length; i++) {
                method = methods[i];

                if (method.getName().startsWith("set") && method.getParameterTypes().length == 1) {
                    String property = method.getName().substring(3);
                    setters.put(property.toLowerCase(), method);
                }
            }
            settersCache.put(clazz, setters);
        }

        return (Method) setters.get(propertyName.toLowerCase());
    }

    /** Call setter method, converting from String */
    private static void callSetterMethodWithString(Object target, Method setter, String value) {
        Class clazz = setter.getParameterTypes()[0];
        Object realValue;

        if (clazz.equals(String.class)) {
            realValue = value;
        } else if (clazz.equals(boolean.class)) {
            realValue = Boolean.valueOf(value);
        } else if (clazz.equals(long.class)) {
        	realValue = value.equals("") ? new Long(0) : new Long(value);
        } else if (clazz.equals(int.class)) {
        	realValue = value.equals("") ? new Integer(0) : new Integer(value);
        } else if (clazz.equals(double.class)) {
        	realValue = value.equals("") ? new Double(0) : new Double(value);
        } else if (clazz.equals(float.class)) {
        	realValue = value.equals("") ? new Float(0) : new Float(value);
        } else if (clazz.equals(short.class)) {
        	realValue = value.equals("") ? new Short((short) 0) : new Short(value);
        // missing char.class here

        } else if (clazz.equals(Long.class)) {
        	realValue = value.equals("") ? null : new Long(value);
        } else if (clazz.equals(Integer.class)) {
        	realValue = value.equals("") ? null : new Integer(value);
        } else if (clazz.equals(Double.class)) {
        	realValue = value.equals("") ? null : new Double(value);
        } else if (clazz.equals(Float.class)) {
        	realValue = value.equals("") ? null : new Float(value);
        } else if (clazz.equals(Short.class)) {
        	realValue = value.equals("") ? null : new Short(value);
        // missing Character.class here
        
        
        } else {
            throw new IllegalArgumentException("Cannot convert string value to " + "'" + clazz.getName() + "' required for setter method '" + setter.getName() + "'");
        }

        callSetterMethod(target, setter, realValue);
    }

    /** Call getter method */
    private static Object callGetterMethod(Object target, Method getter) {
        Object[] methodArgs = new Object[0];

        try {
            return getter.invoke(target, methodArgs);
        } catch (InvocationTargetException ite) {
            // if (ite instanceof RuntimeException) { throw ite; }
            throw (IllegalArgumentException) new IllegalArgumentException("Exception calling method '" + getter.getName() + "' on object '" + target.getClass().getName() + "': " + ite.getMessage()).initCause(ite);
        } catch (IllegalAccessException iae) {
            throw (IllegalArgumentException) new IllegalArgumentException("Exception calling method '" + getter.getName() + "' on object '" + target.getClass().getName() + "': " + iae.getMessage()).initCause(iae);
        }
    }

    /** Checks whether the an instance of the value class can be assigned to a variable
     * of 'variableClass' class via a
     * {@see java.lang.reflect.Method#invoke(java.lang.Object, java.lang.Object[])}
     * method. This is different to the normal
     * {@see java.lang.Class#isAssignableFrom(java.lang.Class)} method since it
     * also takes primitive to wrapper conversions into account.
     *
     * @param variableClass The class representing the variable we are assigning to
     *   (e.g. the 'x' in 'x = y')
     * @param value The class representing the value we are assigning to the variable
     *   (e.g. the 'y' in 'x = y')
     *
     * @return true if the assignment is compatible, false otherwise.
     */
    private static boolean isAssignmentSafe(Class variableClass, Class value) {
        // check for instance or wrapper type
        return variableClass.isAssignableFrom(value) || 
          (variableClass.equals(boolean.class) && value.equals(Boolean.class)) || 
          (variableClass.equals(char.class) && value.equals(Character.class)) || 
          (variableClass.equals(byte.class) && value.equals(Byte.class)) || 
          (variableClass.equals(short.class) && value.equals(Short.class)) || 
          (variableClass.equals(int.class) && value.equals(Integer.class)) || 
          (variableClass.equals(long.class) && value.equals(Long.class)) || 
          (variableClass.equals(float.class) && value.equals(Float.class)) || 
          (variableClass.equals(double.class) && value.equals(Double.class));
    }

    /** Call setter method */
    private static void callSetterMethod(Object target, Method setter, Object value) {

        Object[] methodArgs = new Object[1];
        methodArgs[0] = value;
        
        if (value!=null) {
	        Class setterParamClass = setter.getParameterTypes()[0];
	        Class valueClass = value.getClass();
	        
	        // special case for assigning BigDecimals to longs
			if (setterParamClass.equals(Long.class) && valueClass.equals(BigDecimal.class)) {
				methodArgs[0] = new Long(((BigDecimal)value).longValue()); 
			} else if (setterParamClass.equals(long.class) && valueClass.equals(BigDecimal.class)) {
	        	methodArgs[0] = new Long(((BigDecimal)value).longValue()); 
	        } else {
		        if (!isAssignmentSafe(setterParamClass, value.getClass())) {
		            throw new IllegalArgumentException("Exception calling method '" + setter.getName() + 
	  				  "' on object '" + target.getClass().getName() + "': value object of type '" + 
	  				  valueClass.getName() + "' is not assignment-compatible with setter parameter '" + 
	  				  setterParamClass.getName() + "'");
		        }
	        }
        }

        try {
            setter.invoke(target, methodArgs);
        } catch (InvocationTargetException ite) {
            // if (ite instanceof RuntimeException) { throw ite; }
            throw (IllegalArgumentException) new IllegalArgumentException("Exception calling method '" + setter.getName() + "' on object '" + target.getClass().getName() + "': " + ite.getMessage()).initCause(ite);
        } catch (IllegalAccessException iae) {
            throw (IllegalArgumentException) new IllegalArgumentException("Exception calling method '" + setter.getName() + "' on object '" + target.getClass().getName() + "': " + iae.getMessage()).initCause(iae);
        } catch (Exception e) {
            // if (ite instanceof RuntimeException) { throw ite; }
            throw (IllegalArgumentException) new IllegalArgumentException("Exception calling method '" + setter.getName() + "' on object '" + target.getClass().getName() + "': " + e.getMessage()).initCause(e);
        }
    }


    /** Return a single value from a structured map. Returns null if the
     *  value does not exist, or if an error occurred retrieving it.
     *
     * @param map The structure map
     * @param key The key of the value we wish to retrieve (e.g. "abc.def[12].ghi")
     *
     * @result The value
     */
    static public Object getValue(Object object, String key) {
        // @TODO (low priority) make this function consistent with setValue()
        // the PropertyUtils object below is taken from Jakarta's commons-beanutils.jar 
        // package, which has similar, but not identical semantics. 
        // The provided functionality should hopefully be a superset of that
        // provided by this class anyway, so it may not be important. 
        // (beanutils property retrieval allows curly-brace "{"/"}"-style syntax
        // which we don't allow here).

        try {
            return PropertyUtils.getProperty(object, key);
        } catch (Exception e) {
            return null;
        }
    }

    /** Sets a single value in a structured object. The object may be composed
     * of javabean-style objects, Map/List classes, or a combination of the two.
     * 
     * <p>There is a possible denial-of-service attack that can be used against
     * this method which you should probably be aware of, but can't do anything about:
     * if a large list index is supplied, e.g. "<tt>abc[1293921]</tt>", then a list will be
     * generated with this many elements in it. There are workarounds for this, 
     * but none are really satisfactory.
     * 
     * <p>It should also be noted that even if an exception is thrown within this
     * method, the data structure underneath it may still have been modified, e.g.
     * setting "<tt>abc.def.ghi</tt>" may successfully construct 'def' but later
     * fail on 'ghi'. 
     * 
     * @param map The structure map or list
     * @param key The key of the value we wish to set (e.g. "abc.def[12].ghi"). Note
     *   that map keys are assumed; i.e. they are not enclosed in 'curly braces'.
     * @param value The value to set this to
     * @param ignoreMissingSetter If we are asked to set a value in an object which
     *   does not have an appropriate setter, don't raise an exception (just does
     *   nothing instead).
     * @param convertStrings If we are asked to set a string value in an object
     *   which has a setter, but of a different type (e.g. setCustomerId(Long)),
     *   performs conversions to that type automatically. (Useful when setting
     *   values sourced from a HttpServletRequest, which will always be of String type).
     * @param createMissingElements If we are asked to set a value in a Map or List
     *   that does not exist, will create the required Map keys or extend the List
     *   so that the value can be set.
     *
     * @throws IllegalArgumentException if
     * <ul><li>An null map or list is navigated down
     * <li>A non-List object is invoked with the '[]' operator
     * <li>A non-Map object is invoked with the '.' operator, or the object
     *   has no getter/setter method with the appropriate name
     * <li>An empty mapped property is supplied e.g. bob..something
     * <li>An invalid key was supplied (e.g. 'bob[123' -- no closing square bracket)
     * </ul>
     * @throws NumberFormatException if an array index cannot be converted to
     *   an integer via {@see java.lang.Integer#parseInt(java.lang.String)}.
     */
    static public void setValue(Object object, String key, Object value, boolean ignoreMissingSetter, boolean convertStrings, boolean createMissingElements)
        throws NumberFormatException 
    {
        if (object == null) { throw new NullPointerException("null object"); }
        if (key == null) { throw new NullPointerException("null key"); }

        // parse state: 
        //   0=searching for new value (at start of line) 
        //   1=consuming list index (after '[')
        //   2=consuming map index (after start of line or after '.')
        //   3=search for new value (after ']')
        Object ref = object; // reference into object

        int parseState = 0;
        int length = key.length();
        String element;
        int elementInt = -1;
        List lastList = null;
        String parentName = null;
        Object parentRef = null;
        
        if (object instanceof List) { lastList = (List) object; }

        char ch;
        StringBuffer buffer = new StringBuffer();

        for (int pos = 0; pos < length; pos++) {
            ch = key.charAt(pos);

            // System.out.println("pos " + pos + ", state=" + parseState + ", nextchar=" + ch + ", buf=" + buffer);
            switch (parseState) {
                
                case 0: 
                    if (ch == '[') {
                        buffer.setLength(0);
                        parseState = 1;
                    } else { // could check for legal map index characters here
                        buffer.setLength(0);
                        buffer.append(ch);
                        parseState = 2;
                    }
                    break;
                    
                case 3:
                    if (ch == '[') {
                        buffer.setLength(0);
                        parseState = 1;
                    } else if (ch == '.') {
                        buffer.setLength(0);
                        parseState = 2;
                    } else {
                        throw new IllegalArgumentException("Expecting '[' or '.' after ']' in key '" + key + "'; found '" + ch + "'");
                    }
                    break;
                
                case 1:
                    if (ch >= '0' && ch <= '9') {
                        buffer.append(ch);
                    } else if (ch == ']') {
                        element = buffer.toString();
                        elementInt = Integer.parseInt(element);

                        if (ref == null) {
                            // create list if parent was a map
                            if ((parentRef instanceof Map) && createMissingElements) {
                                ref = new ArrayList();
                                ((Map) parentRef).put(parentName, ref);
                            } else if ((parentRef instanceof List) && createMissingElements) {
                                ref = new ArrayList();
                                ((List) parentRef).set(Integer.parseInt(parentName), ref);
                            } else {
                                throw new IllegalArgumentException("Could not retrieve indexed property " + element + " in key '" + key + "' from null object");
                            }
                        }

                        if (ref instanceof List) {
                            lastList = (List) ref;

                            // XXX: denial of service attacks possible here
                            // (large list number may exceed available memory)
                            if (lastList.size() <= elementInt) {
                                setListElement(lastList, elementInt, null);
                            }

                            parentName = element;
                            parentRef = ref;
                            ref = lastList.get(elementInt);
                        } else {
                            // could attempt to reflect on a get(int)-style method,
                            // but this seems like overkill; anything that has List-like
                            // semantics should implement the List interface.
                            throw new IllegalArgumentException("Could not retrieve indexed property " + element + " in key '" + key + "' from object of type '" + ref.getClass().getName() + "'");
                        }

                        parseState = 3;
                    } else {
                        throw new IllegalArgumentException("Illegal character '" + ch + "'" + " found in list index");
                    }
                    break;
                
                case 2:
                    if (ch != '.' && ch != '[') {
                        buffer.append(ch);
                    } else {
                        // navigate map
                        element = buffer.toString();

                        if (ref == null) {
                            if ((parentRef instanceof List) && createMissingElements) {
                                ref = new HashMap();
                                ((List) parentRef).set(Integer.parseInt(parentName), ref);
                            } else if ((parentRef instanceof Map) && createMissingElements) {
                                ref = new HashMap();
                                ((Map) parentRef).put(parentName, ref);
                            } else {
                                throw new IllegalArgumentException("Could not retrieve mapped property '" + element + "' in key '" + key + "' from null object");
                            }
                        }

                        if (element.equals("")) {
                            throw new IllegalArgumentException("Could not retrieve empty mapped property '" + element + "' in key '" + key + "'");
                        } else if (ref instanceof Map) {
                            parentName = element;
                            parentRef = ref;
                            ref = ((Map) ref).get(element);
                        } else {
                            // look for getter method
                            Method getter = getGetterMethod(ref.getClass(), element);

                            if (getter == null) {
                                throw new IllegalArgumentException("Could not retrieve mapped property '" + element + "' in key '" + key + "' for class '" + ref.getClass().getName() + "': no getter method found");
                            }

                            parentName = null; // can't dynamically create these
                            parentRef = null;
                            ref = callGetterMethod(ref, getter);
                        }

                        buffer.setLength(0);

                        if (ch == '[') {
                            parseState = 1;
                        } else if (ch == '.') {
                            parseState = 2;
                        } else {
                            throw new IllegalStateException("Unexpected character '" + ch + "' parsing key '" + key + "'");
                        }
                    }
                    break;
                
                default:
                    throw new IllegalStateException("Unexpected state " + parseState + " parsing key '" + key + "'");
            }
        }

        if (parseState == 0) {
            throw new IllegalStateException("Unexpected error after parsing key '" + key + "'");
        } else if (parseState == 1) {
            throw new IllegalStateException("Missing ']' in key '" + key + "'");
        } else if (parseState == 2) {
            // set mapped property
            element = buffer.toString();

            if (ref == null) {
                if ((parentRef instanceof Map) && createMissingElements) {
                    ref = new HashMap();
                    ((Map) parentRef).put(parentName, ref);
                } else if ((parentRef instanceof List) && createMissingElements) {
                    ref = new HashMap();
                    ((List) parentRef).set(Integer.parseInt(parentName), ref);
                } else {
                    throw new IllegalArgumentException("Could not set mapped property '" + element + "' in key '" + key + "' for null object");
                }
            }

            if (element.equals("")) {
                throw new IllegalArgumentException("Could not set empty mapped property '" + element + "' in key '" + key + "'");
            } else if (ref instanceof Map) {
                ((Map) ref).put(element, value);
            } else {
                Method setter = getSetterMethod(ref.getClass(), element);

                if (setter == null) {
                    // System.out.println("Missing setter '" + element + "'");
                    if (!ignoreMissingSetter) {
                        throw new IllegalArgumentException("Could not set mapped property '" + element + "' in key '" + key + "' for class '" + ref.getClass().getName() + "': no setter method found");
                    }
                } else {
                    // System.out.println("Setting '" + element + "' with value " + value);
                    try {
	                    if (convertStrings && ((value == null) || (value instanceof String))) {
	                        if (value==null) { value = ""; }
	                        callSetterMethodWithString(ref, setter, (String) value);
	                    } else {
	                        callSetterMethod(ref, setter, value);
	                    }
                    } catch (Exception e) {
                    	throw (IllegalArgumentException) new IllegalArgumentException("Could not set field '" + key + "' with value '" + value + "'").initCause(e);
                    }
                }
            }
        } else if (parseState == 3) {
            // set list property
            // ref currently points to the current list element; we want to set the 
            // value of the last list we saw, contained in lastList.
            ref = lastList;

            if (ref == null) {
                throw new IllegalArgumentException("Could not set indexed property '" + elementInt + "' in key '" + key + "' for null object");
            }

            if (ref instanceof List) {
                ((List) ref).set(elementInt, value);
            } else {
                throw new IllegalArgumentException("Could not set indexed property " + elementInt + " in key '" + key + "' in object of type '" + ref.getClass().getName() + "'");
            }
        }
    }

    /** Sets the element at a particular list index to a particular object.
     * This differs from the standard List.set() method inasmuch as the list
     * is allowed to grow in the case where index>=List.size(). If the list
     * needs to grow to accomodate the new element, then null objects are
     * appended until the list is large enough.
     *
     * @param list   The list to modify
     * @param index  The position within the list we wish to set to this object
     * @param object The object to be placed into the list.
     */
    public static void setListElement(List list, int index, Object object) {
        int size;

        if (list == null) {
            throw new NullPointerException("list parameter must not be null");
        }

        if (index < 0) {
            throw new IndexOutOfBoundsException("index parameter must be >= 0");
        }

        size = list.size();

        while (index >= size) {
            list.add(null);
            size = size + 1;
        }

        list.set(index, object);
    }

    /** As per {@link #getStructuredListItem(List, String, Object)}, with a 
     * numeric key.
     * 
     * @param list The list to search.
     * @param keyField The name of the key field.
     * @param key The key value to search for
     *
     * @return The requested element in the list, or null if the element cannot be found.
     *
     * @throws NullPointerException if list or keyField is set to null.
     * @throws IllegalStateException if the list is not composed of Maps
     */
    static public Map getStructuredListItem(List list, String keyField, long longValue) {
        if (list == null) { throw new NullPointerException("Cannot search null list"); }
        if (keyField == null) { throw new NullPointerException("Cannot search for null keyField"); }

        Map row;
        Object foundKey;
        for (Iterator i = list.iterator(); i.hasNext();) {
            try {
                row = (Map) i.next();
            } catch (ClassCastException cce) {
                throw (IllegalStateException) new IllegalStateException("List must be composed of Maps").initCause(cce);
            }
            foundKey = row.get(keyField);
            if (foundKey != null) {
                if (!(foundKey instanceof Number)) {
                    throw (IllegalStateException) new IllegalStateException("Key is not numeric (found '" + foundKey.getClass().getName() + "' instead)");
                }
                if (((Number) foundKey).longValue() == longValue) {
                    return row;
                }
            }
        }
        return null;
    }

    /** As per {@link #getStructuredListObject(List, String, Object)}, with a 
     * numeric key.
     *
     * @param list The list to search.
     * @param keyField The name of the key field.
     * @param key The key value to search for
     *
     * @return The requested element in the list, or null if the element cannot be found.
     *
     * @throws NullPointerException if list or keyField is set to null.
     * @throws IllegalStateException if the list is not composed of Maps
     */
    static public Object getStructuredListObject(List list, String keyField, long longValue) {
        if (list == null) { throw new NullPointerException("Cannot search null list"); }
        if (keyField == null) { throw new NullPointerException("Cannot search for null keyField"); }
        Object row;
        Object foundKey;
        for (Iterator i = list.iterator(); i.hasNext();) {
            row = i.next();
            foundKey = Struct.getValue(row, keyField); 
            if (foundKey != null) {
                if (!(foundKey instanceof Number)) {
                    throw (IllegalStateException) new IllegalStateException("Key is not numeric (found '" + foundKey.getClass().getName() + "' instead)");
                }
                if (((Number) foundKey).longValue() == longValue) {
                    return row;
                }
            }
        }
        return null;
    }

    
    /** Searches a structured list for a particular row. The list is presumed to be a List of Maps,
     *  each of which contains a key field. Lists are searched sequentially until the desired row is
     *  found. It is permitted to search on null key values. If the row cannot be found, null is
     *  returned.
     *
     *  <p>For example, in the list:
     *  <pre>
     *  list = [
     *    0: { systemId = "abc", systemTimezone = "dalby" }
     *    1: { systemId = "def", systemTimezone = "london" }
     *    2: { systemId = "ghi", systemTimezone = "new york" }
     *  ]
     *  </pre>
     *
     *  <p><tt>getStructuredListItem(list, "systemId", "def")</tt> would return a reference to the
     *  second element in the list, i.e. the Map:
     *
     *  <pre>
     *  { systemId = "def", systemTimezone = "london" }
     *  </pre>
     *
     * @param list The list to search.
     * @param keyField The name of the key field.
     * @param key The key value to search for
     *
     * @return The requested element in the list, or null if the element cannot be found.
     *
     * @throws NullPointerException if list or keyField is set to null.
     * @throws IllegalArgumentException if the list is not composed of Maps
     */
    static public Map getStructuredListItem(List list, String keyField, Object key) {
        if (list == null) { throw new NullPointerException("Cannot search null list"); }
        if (keyField == null) { throw new NullPointerException("Cannot search for null keyField"); }
        Map row;
        Object foundKey;
        for (Iterator i = list.iterator(); i.hasNext();) {
            // could handle generic objects via Codec.getValue(row, keyField); instead
            try {
                row = (Map) i.next();
            } catch (ClassCastException cce) {
                throw (IllegalArgumentException) new IllegalArgumentException("List must be composed of Maps").initCause(cce);
            }
            foundKey = row.get(keyField);  
            if (foundKey == null && key==null) {
                return row;
            } else {
                if (foundKey!=null && foundKey.equals(key)) {
                    return row;
                }
            }
        }
        return null;
    }
    
	// fun fun fun
	 /** As per {@link Struct#getStructuredListItem(List, String, long)}, with a 
    * compound key.
    * 
    * @param list The list to search.
    * @param keyField The name of the key field.
    * @param longValue The key value to search for
    * @param keyField2 The name of the second key field.
    * @param longValue2 The second key value to search for
    *
    * @return The requested element in the list, or null if the element cannot be found.
    *
    * @throws NullPointerException if list or keyField is set to null.
    * @throws IllegalStateException if the list is not composed of Maps
    */
   static public Map getStructuredListItem2(List list, String keyField, long longValue, String keyField2, long longValue2) {
       if (list == null) { throw new NullPointerException("Cannot search null list"); }
       if (keyField == null) { throw new NullPointerException("Cannot search for null keyField"); }
       if (keyField2 == null) { throw new NullPointerException("Cannot search for null keyField2"); }

       Map row;
       Object foundKey, foundKey2;
       for (Iterator i = list.iterator(); i.hasNext();) {
           try {
               row = (Map) i.next();
           } catch (ClassCastException cce) {
               throw (IllegalStateException) new IllegalStateException("List must be composed of Maps").initCause(cce);
           }
           foundKey = row.get(keyField);
           if (foundKey != null) {
               if (!(foundKey instanceof Number)) {
                   throw (IllegalStateException) new IllegalStateException("Key is not numeric (found '" + foundKey.getClass().getName() + "' instead)");
               }
               if (((Number) foundKey).longValue() == longValue) {
               	
               	foundKey2 = row.get(keyField2);
               	if (foundKey2 != null) {
                       if (!(foundKey2 instanceof Number)) {
                           throw (IllegalStateException) new IllegalStateException("Key2 is not numeric (found '" + foundKey2.getClass().getName() + "' instead)");
                       }
               	}
                   if (((Number) foundKey2).longValue() == longValue2) {
                   	return row;
                   }
               }
           }
       }
       return null;
   }
   
   


    /** Searches a structured list for a particular row. The list may
     * be composed of arbitrary objects.
     *
     * @param list The list to search.
     * @param keyField The name of the key field.
     * @param key The key value to search for
     *
     * @return The requested element in the list, or null if the element cannot be found.
     *
     * @throws NullPointerException if list or keyField is set to null.
     * @throws IllegalArgumentException if the list is not composed of Maps
     */

    static public Object getStructuredListObject(List list, String keyField, Object key) {
        if (list == null) { throw new NullPointerException("Cannot search null list"); }
        if (keyField == null) { throw new NullPointerException("Cannot search for null keyField"); }
        Object row;
        Object foundKey;
        for (Iterator i = list.iterator(); i.hasNext();) {
            // could handle generic objects via Codec.getValue(row, keyField); instead
            row = i.next();
            foundKey = Struct.getValue(row, keyField);  
            if (foundKey == null && key==null) {
                return row;
            } else {
                if (foundKey!=null && foundKey.equals(key)) {
                    return row;
                }
            }
        }
        return null;
    }

    
    /* It would be nice if this was implemented :) 
    public List filterStructuredList(List list, TopLevelExpression expression) {
        return null;
    }
    */

    /** Searches a structured list for a particular column. The list is presumed to be a List of Maps,
     *  each of which contains a particular key. The value of this key is retrieved from each
     *  Map, and added to a new List, which is then returned to the user. If the
     *  value of that key is null for a particular Map, then the null value is added to the
     *  returned list.
     *
     *  <p>For example, in the list:
     *  <pre>
     *  list = [
     *    0: { systemId = "abc", systemTimezone = "dalby" }
     *    1: { systemId = "def", systemTimezone = "london" }
     *    2: { systemId = "ghi", systemTimezone = "new york" }
     *    3: null
     *    4: { systemId = "jkl", systemTimezone = "melbourne" }
     *  ]
     *  </pre>
     *
     *  <p><tt>getStructuredListColumn(list, "systemId")</tt> would return a new List with
     *  three elements, i.e.
     *
     *  <pre>
     *  list = [
     *    0: "abc"
     *    1: "def"
     *    2: "ghi"
     *    3: null
     *    4: "jkl"
     *  ]
     *  </pre>
     *
     * @param list The list to search.
     * @param columnName The key of the entry in each map to retrieve
     *
     * @return a List of Objects
     *
     * @throws NullPointerException if list or columnName is set to null.
     */
    public static List getStructuredListColumn(List list, String columnName) {
        if (list == null) {
            throw new NullPointerException("list must not be empty");
        }

        if (columnName == null) {
            throw new NullPointerException("columnName must not be empty");
        }

        if (list.size() == 0) {
            return Collections.EMPTY_LIST;
        }

        ArrayList result = new ArrayList(list.size());
        Iterator it = list.iterator();

        while (it.hasNext()) {
        	Object obj = it.next();
        	if (obj instanceof Map) {
                Map map = (Map) obj;
	            if (map==null) {
	            	result.add(null);
	            } else {
		            Object o = map.get(columnName);
		            result.add(o);
	            }
        	} else {
        		Object o = getValue(obj, columnName);
        		result.add(o);
        	}
        }

        return result;
    }


    /** This method returns a human-readable version of a structured list.
     * The output of this list looks similar to the following:
     *
     * <pre style="code">
     *   topLevelName = [
     *     0: 'stringValue'
     *     1: null
     *     2: (WeirdObjectClass) "toString() output of weirdObject"
     *     3 = {
     *         mapElement => ...,
     *         ...
     *       }
     *     4 = [
     *         0: listElement
     *         ...
     *       ]
     *     5 = (
     *         setElement
     *     )
     *     ...
     *   ]
     * </pre>
     *
     * Strings are represented as their own values in quotes; null values
     * are represented with the text <tt>null</tt>, and structured maps
     * and structured lists contained within this list are recursed into.
     *
     * @param topLevelName The name to assign to the list in the first row of output
     * @param list         The list we wish to represent as a string
     * @return             A human-readable version of a structured list
     */
    static public String structuredListToString(String topLevelName, List list) {
        String s;
        Object value;
        int index = 0;

		if (list==null) {
			return topLevelName + " = (List) null\n";
		}

        s = topLevelName + " = [\n";

        for (Iterator i = list.iterator(); i.hasNext();) {
            value = i.next();

            if (value == null) {
                s = s + "  " + index + ": null\n";
            } else if (value instanceof String) {
                s = s + "  " + index + ": '" + Text.getDisplayString("", (String) value) + "'\n";
			} else if (value.getClass().isArray()) {
				List wrapper = Arrays.asList((Object[]) value);
				s = s + "  " + index + ": (" + value.getClass() + ") " + Text.indent("  ", structuredListToString(String.valueOf(index), wrapper));
            } else if (value instanceof Map) {
                s = s + Text.indent("  ", structuredMapToString(String.valueOf(index), (Map) value));
            } else if (value instanceof List) {
                s = s + Text.indent("  ", structuredListToString(String.valueOf(index), (List) value));
			} else if (value instanceof Set) {
				s = s + Text.indent("  ", structuredSetToString(String.valueOf(index), (Set) value));
            } else {
                s = s + "  " + index + ": (" + Text.getLastComponent(value.getClass().getName()) + ") " + Text.getDisplayString("", value.toString()) + "\n";
            }

            index = index + 1;
        }

        s = s + "]\n";

        return s;
    }

	/** This method returns a human-readable version of a structured set.
	 * The output of this set looks similar to the following:
	 *
	 * <pre style="code">
	 *   topLevelName = (
	 *     0: 'stringValue'
	 *     1: null
	 *     2: (WeirdObjectClass) "toString() output of weirdObject"
	 *     3 = {
	 *         mapElement => ...,
	 *         ...
	 *       }
	 *     4 = [
	 *         0: listElement
	 *         ...
	 *       ]
	 *     5 = (
	 *         setElement
	 *       )
	 *     ...
	 *   )
	 * </pre>
	 *
	 * Strings are represented as their own values in quotes; null values
	 * are represented with the text <tt>null</tt>, and structured maps
	 * and structured lists contained within this list are recursed into.
	 *
	 * @param topLevelName The name to assign to the list in the first row of output
	 * @param list         The list we wish to represent as a string
	 * @return             A human-readable version of a structured list
	 */
	static public String structuredSetToString(String topLevelName, Set set) {
		String s;
		Object value;
		int index = 0;

		if (set==null) {
			return topLevelName + " = (Set) null\n";
		}

		s = topLevelName + " = (\n";

		for (Iterator i = set.iterator(); i.hasNext();) {
			value = i.next();

			if (value == null) {
				s = s + "  " + index + ": null\n";
			} else if (value instanceof String) {
				s = s + Text.getDisplayString("", (String) value) + "'\n";
			} else if (value.getClass().isArray()) {
				List wrapper = Arrays.asList((Object[]) value);
				s = s + Text.indent("  ", structuredListToString(String.valueOf(index), wrapper));
			} else if (value instanceof Map) {
				s = s + Text.indent("  ", structuredMapToString(String.valueOf(index), (Map) value));
			} else if (value instanceof List) {
				s = s + Text.indent("  ", structuredListToString(String.valueOf(index), (List) value));
			} else if (value instanceof Set) {
				s = s + Text.indent("  ", structuredSetToString(String.valueOf(index), (Set) value));
			} else {
				s = s + "  (" + Text.getLastComponent(value.getClass().getName()) + ") " + Text.getDisplayString("", value.toString()) + "\n";
			}

			index = index + 1;
		}

		s = s + ")\n";

		return s;
	}

    /** This method returns a human-readable version of a structured map.
     * The output of this list looks similar to the following:
     *
     * <pre style="code">
     *   topLevelName = {
     *     apples => 'stringValue'
     *     rhinocerouseses => null
     *     weirdObjectValue => (WeirdObjectClass) "toString() output of weirdObject"
     *     mapValue = {
     *         mapElement => ...
     *         ...
     *       }
     *     listValue = [
     *         0: listElement
     *         ...
     *       ]
     *     setValue = (
     *         setElement
     *       )
     *     ...
     *   ]
     * </pre>
     *
     * Keys within the map are sorted alphabetically before being enumerated.
     *
     * Strings are represented as their own values in quotes; null values
     * are represented with the text <tt>null</tt>, and structured maps
     * and structured lists contained within this list are recursed into.
     *
     * @param topLevelName The name to assign to the list in the first row of output
     * @param map          The map we wish to represent as a string
     * @return             A human-readable version of a structured map
     */
    static public String structuredMapToString(String topLevelName, Map map) {
        String s;
        String key;
        Object value;

		if (map==null) {
			return topLevelName + " = (Map) null\n";
		}

		// sort map output by key name
        List list = new ArrayList(map.keySet());
        Collections.sort(list, new ListContainingNullComparator());

        s = topLevelName + " = {\n";

        for (Iterator i = list.iterator(); i.hasNext();) {
            key = (String) i.next();
            value = map.get(key);

            if (value == null) {
                s = s + "  " + ((key == null) ? "null" : key) + " => null\n";
            } else if (value instanceof String) {
                s = s + "  " + ((key == null) ? "null" : key) + " => '" + Text.getDisplayString(key, (String) value) + "'\n";
			} else if (value.getClass().isArray()) {
				//List wrapper = Arrays.asList((Object[]) value);
				//s = s + Text.indent("  ", structuredListToString(key, wrapper));
				if (value instanceof Object[]) {
	            	List wrapper = Arrays.asList((Object[])value);
	            	s = s + Text.indent("  ", structuredListToString(key, wrapper));
            	} else if (value instanceof double[]) {
            		// @TODO other primitive array types
            		// @TODO convert directly probably
            		// @XXX this displays primitive double types as (Double), not (double)
            		double[] daSrc = (double[]) value;
            		Double[] daTgt = new Double[daSrc.length];
            		for (int j=0; j<daSrc.length; j++) { daTgt[j]=daSrc[j]; }
            		List arrayList = Arrays.asList((Object[])daTgt);
            		s = s + Text.indent("  ", structuredListToString(key, arrayList));
            	} else if (value instanceof int[]) {
            		int[] daSrc = (int[]) value;
            		Integer[] daTgt = new Integer[daSrc.length];
            		for (int j=0; j<daSrc.length; j++) { daTgt[j]=daSrc[j]; }
            		List arrayList = Arrays.asList((Object[])daTgt);
            		s = s + Text.indent("  ", structuredListToString(key, arrayList));

            	} else {
            		throw new UnsupportedOperationException("Cannot convert primitive array to String");
            	}
				
            } else if (value instanceof Map) {
                s = s + Text.indent("  ", structuredMapToString(key, (Map) value));
            } else if (value instanceof List) {
                s = s + Text.indent("  ", structuredListToString(key, (List) value));
			} else if (value instanceof Set) {
				s = s + Text.indent("  ", structuredSetToString(key, (Set) value));
            } else {
                s = s + "  " + ((key == null) ? "null" : key) + " => (" + Text.getLastComponent(value.getClass().getName()) + ") " + Text.getDisplayString(key, value.toString()) + "\n";
            }
        }

        s = s + "}\n";

        return s;
    }


	/** This method returns a human-readable version of an array object that contains
	 * structured lists/maps/sets. Bear in mind that the array object itself isn't 
	 * considered 'structured' by the definitions at the top of this class, or
	 * at least, it won't until this is added to the other structured parsers/readers.
	 *
	 * <pre style="code">
	 *   topLevelName[] = [
	 *     0: 'stringValue'
	 *     1: null
	 *     2: (WeirdObjectClass) "toString() output of weirdObject"
	 *     3 = {
	 *         mapElement => ...,
	 *         ...
	 *       }
	 *     4 = [
	 *         0: listElement
	 *         ...
	 *       ]
	 *     5 = (
	 *         setElement
	 *     )
	 *     ...
	 *   ]
	 * </pre>
	 *
	 * Strings are represented as their own values in quotes; null values
	 * are represented with the text <tt>null</tt>, and structured maps
	 * and structured lists contained within this list are recursed into.
	 *
	 * @param topLevelName The name to assign to the list in the first row of output
	 * @param list         The list we wish to represent as a string
	 * @return             A human-readable version of a structured list
	 */
	static public String arrayToString(String topLevelName, Object list) {
		if (list==null) {
			return topLevelName + "[] = (Array) null\n";
		}
		if (!list.getClass().isArray()) {
			throw new IllegalArgumentException("object must be array");
		}
		List wrapper = Arrays.asList((Object[]) list);
		return structuredListToString(topLevelName + "[]", wrapper); 
	}

	/**
     * Converts a java List into javascript
     *
     * @param list the list to convert into javascript
     *
     * @return the javascript version of this list.
     */
    public static String structuredListToJson(List list, String jsonFormat)
    {
        StringBuilder s = new StringBuilder(list.size() * 2);
        structuredListToJson(s, list, jsonFormat);
        return s.toString();
    }
    
    private static void structuredListToJson(StringBuilder s, List list, String jsonFormat) {
        Object value;
        int index = 0;
        s.append('[');
        for (Iterator i = list.iterator(); i.hasNext(); ) {
            value = i.next();
            if (value == null) {
                s.append("null");
            } else if (value instanceof String) {
                s.append('\"').append(Text.escapeJavascript((String) value)).append('\"'); 
            } else if (value instanceof ToJsonFormat) {
            	s.append(((ToJsonFormat)value).toJson(jsonFormat));
            } else if (value instanceof ToJson) {
            	s.append(((ToJson)value).toJson());
            } else if (value instanceof ToStringReturnsJson) {
            	s.append(value.toString());
            } else if (value instanceof Map) {
                structuredMapToJson(s, (Map) value, jsonFormat); // @TODO pass in stringbuffer to pvt method
            } else if (value instanceof List) {
                structuredListToJson(s, (List)value, jsonFormat);
            } else if (value instanceof Number) {
                s.append(value);
            } else if (value instanceof Boolean) {
                s.append(value);
            } else if (value instanceof java.util.Date) {
            	// MS-compatible JSON encoding of Dates:
            	// see http://weblogs.asp.net/bleroy/archive/2008/01/18/dates-and-json.aspx
                s.append(toDate((java.util.Date)value, jsonFormat));
            } else if (value.getClass().isArray()) {
           	  	if (value instanceof Object[]) {
           	  		List arrayList = Arrays.asList((Object[])value);
           	  		structuredListToJson(s, (List)arrayList, jsonFormat);
           	  	} else if (value instanceof double[]) {
	              	// @TODO other primitive array types
	           		// @TODO convert directly probably
	           		double[] daSrc = (double[]) value;
	           		Double[] daTgt = new Double[daSrc.length];
	           		for (int j=0; j<daSrc.length; j++) { daTgt[j]=daSrc[j]; }
	           		List arrayList = Arrays.asList((Object[])daTgt);
	           		structuredListToJson(s, (List) arrayList, jsonFormat);
           	  	} else if (value instanceof int[]) {
	           		int[] daSrc = (int[]) value;
	           		Integer[] daTgt = new Integer[daSrc.length];
	           		for (int j=0; j<daSrc.length; j++) { daTgt[j]=daSrc[j]; }
	           		List arrayList = Arrays.asList((Object[])daTgt);
	           		structuredListToJson(s, (List) arrayList, jsonFormat);
           	  	} else {
           	  		throw new UnsupportedOperationException("Cannot convert primitive array to JSON");
           	  	}
            } else {
                throw new RuntimeException("Cannot translate Java object " +
                    value.getClass().getName() + " to javascript value");
            }
            index = index + 1;
            if (i.hasNext()) {
                s.append(',');
            }
        }
        s.append("]\n");
        // return s.toString();
    }

   /** Converts a java map into javascript  
    *
    * @param map the Map to convert into javascript
    *
    * @return a javascript version of this Map
    */
   public static String structuredMapToJson(Map map, String jsonFormat) {
	   StringBuilder sb = new StringBuilder();
	   structuredMapToJson(sb, map, jsonFormat);
	   return sb.toString();
   }
   
   // private methods that uses StringBuilders, which is hopefully a bit more efficient
   // than using Strings
   private static void structuredMapToJson(StringBuilder sb, Map map, String jsonFormat) {
       Map.Entry entry;
       // String s;
       Object key;
       String keyJson;
       Object value;
       List list = new ArrayList(map.keySet());

       Collections.sort(list, new ListComparator());
       boolean isFirst = true;

       sb.append("{");

       for (Iterator i = list.iterator(); i.hasNext();) {
           key = (Object) i.next();
           if (key instanceof String) {
        	   keyJson = "\"" + key + "\"";
           } else if (key instanceof Number) {
        	   keyJson = "\"" + String.valueOf(key) + "\""; // coerce numeric keys to strings
           } else {
        	   throw new IllegalArgumentException("Cannot convert key type " + key.getClass().getName() + " to javascript value");
           }
           value = map.get(key);
           if (key == null || key.equals("")) {
               continue; // don't allow null or empty keys, 
           } else if (value == null) {
               continue; // don't bother transferring null values to javascript
           } else if (value instanceof String) {
        	   if (!isFirst) { sb.append(","); }
        	   sb.append(keyJson);
        	   sb.append( ": \"");
        	   sb.append(Text.escapeJavascript((String) value));
        	   sb.append("\"");
           } else if (value instanceof ToJsonFormat) {
        	   if (!isFirst) { sb.append(","); }
        	   sb.append(keyJson);
        	   sb.append(": ");
        	   sb.append(((ToJsonFormat)value).toJson(jsonFormat));
           } else if (value instanceof ToJson) {
        	   if (!isFirst) { sb.append(","); }
        	   sb.append(keyJson);
        	   sb.append(": ");
        	   sb.append(((ToJson)value).toJson());
           } else if (value instanceof ToStringReturnsJson) {
        	   if (!isFirst) { sb.append(","); }
        	   sb.append(keyJson);
        	   sb.append(": ");
        	   sb.append(value.toString());
           } else if (value instanceof Map) {
        	   if (!isFirst) { sb.append(","); }
               sb.append(keyJson);
               sb.append(": ");
               structuredMapToJson(sb, (Map)value, jsonFormat);
           } else if (value instanceof List) {
        	   if (!isFirst) { sb.append(","); }
        	   sb.append(keyJson);
        	   sb.append(": ");
        	   structuredListToJson(sb, (List)value, jsonFormat);
           } else if (value instanceof Number) {
        	   if (!isFirst) { sb.append(","); }
               sb.append(keyJson);
               sb.append(": ");
               sb.append(value);
           } else if (value instanceof Boolean) {
        	   if (!isFirst) { sb.append(","); }
        	   sb.append(keyJson);
        	   sb.append(": ");
        	   sb.append(value);
           } else if (value instanceof java.util.Date) {
           	// MS-compatible JSON encoding of Dates:
           	// see http://weblogs.asp.net/bleroy/archive/2008/01/18/dates-and-json.aspx
        	   if (!isFirst) { sb.append(","); }
               sb.append(keyJson);
               sb.append(": ");
               sb.append(toDate((java.util.Date)value, jsonFormat));
           } else if (value.getClass().isArray()) {
        	   
           	  if (value instanceof Object[]) {
	              List arrayList = Arrays.asList((Object[])value);
	              if (!isFirst) { sb.append(","); }
	              sb.append(keyJson);
	              sb.append(": ");
	              structuredListToJson(sb, (List)arrayList, jsonFormat);
           	  } else if (value instanceof double[]) {
              	  // @TODO other primitive array types
            	  // @TODO convert directly probably
           		  double[] daSrc = (double[]) value;
           		  Double[] daTgt = new Double[daSrc.length];
           		  for (int j=0; j<daSrc.length; j++) { daTgt[j]=daSrc[j]; }
           		  List arrayList = Arrays.asList((Object[])daTgt);
           		  if (!isFirst) { sb.append(","); }
           		  sb.append(keyJson);
           		  sb.append(": ");
           		  structuredListToJson(sb, (List)arrayList, jsonFormat);
           	  } else if (value instanceof int[]) {
              	  // @TODO other primitive array types
            	  // @TODO convert directly probably
           		  int[] daSrc = (int[]) value;
           		  Integer[] daTgt = new Integer[daSrc.length];
           		  for (int j=0; j<daSrc.length; j++) { daTgt[j]=daSrc[j]; }
           		  List arrayList = Arrays.asList((Object[])daTgt);
           		  if (!isFirst) { sb.append(","); }
           		  sb.append(keyJson);
           		  sb.append(": ");
           		  structuredListToJson(sb, (List)arrayList, jsonFormat);
           	  } else {
           		  throw new UnsupportedOperationException("Cannot convert primitive array to JSON");
           	  }
           } else {
               throw new RuntimeException("Cannot translate Java object " + value.getClass().getName() + " to javascript value");
           }
           isFirst = false;
       }

       sb.append("}\n");
       // return s;
   }
	
	
    /**
     * Converts a java List into javascript
     *
     * @param list the list to convert into javascript
     *
     * @return the javascript version of this list.
     */
    static public String structuredListToJson(List list)
    {
    	return structuredListToJson(list, "microsoft");
    }
    
    /** Convert a date object to it's JSON representation.
     * 
     * <p>As there's no real standard for this, a type format is used to define what kind of Dates your 
     * going to get on the Json side.
     * 
     * @see see http://weblogs.asp.net/bleroy/archive/2008/01/18/dates-and-json.aspx
     * 
     * @param d a Date object
     * @param jsonFormat either "microsoft" or "numeric"
     * 
     * @return A date in json representation.
     */
    static public String toDate(Date d, String jsonFormat) {
	   	if (jsonFormat==null || jsonFormat.equals("microsoft")) {
	   		return "\"\\/Date(" + d.getTime() +  ")\\/\"";	
	   	} else {
	   		return String.valueOf(d.getTime());
	   	}
    }


	/** A comparator that allows elements within lists to be sorted, allowing
	 * nulls (in order to sort map keys)
	 */
    static class ListComparator
        implements Comparator
    {
        public int compare(Object o1, Object o2) {
            if (o1 == null && o2 == null) {
                return 0;
            } else if (o1 == null) {
                return -1;
            } else if (o2 == null) {
                return 1;
            }
            return ((Comparable)o1).compareTo(o2);
        }
    }

    /** Converts a java map into javascript  
     *
     * @param map the Map to convert into javascript
     *
     * @return a javascript version of this Map
     */
    static public String structuredMapToJson(Map map)
    {
    	return structuredMapToJson(map, DATE_FORMAT_MICROSOFT);
    }
    
    /** Create a structured Map out of key / value pairs. Keys must be Strings.
     * 
     * <p>So the value of <code>Struct.newStructuredMap("thing", 1L, "otherThing", "value");</code> is a map
     * with two entries:
     * <ul><li>an entry with key "thing" and value new Long(1), and 
     * <li>an entry  with key "otherThing" and value "value".
     * </ul> 
     * 
     * @param keyValuePairs a list of key / value pairs 
     * 
     * @return a Map as described above
     * 
     * @throws ClassCastException if any of the supplied keys is not a String
     */
    // looking forward to when statically defined Maps become a Java language construct
    static public Map<String, Object> newStructuredMap(Object... keyValuePairs) {
    	if (keyValuePairs.length%2==1) { throw new IllegalArgumentException("keyValuePairs must have even number of elements"); }
    	Map<String, Object> map = new HashMap();
    	for (int i=0; i<keyValuePairs.length; i+=2) {
    		String key = (String) keyValuePairs[i];
    		map.put(key, keyValuePairs[i + 1]);
    	}
    	return map;
    }

	/** Rename a structured column, as returned from a Spring JdbcTemplate query. This method will iterate
	 * throw all rows of a table, replacing any srcColumnName keys with destColumnName
	 * 
	 * @param rows the table being modified
	 * @param srcColumnName the name of the row key (column) being replaced
	 * @param destColumnName the new name of the row key (column)
	 */
	public static void renameStructuredListColumn(List<Map<String, Object>> rows, String srcColumnName, String destColumnName) {
		if (srcColumnName == null) { throw new NullPointerException("null srcColumnName"); }
		if (destColumnName == null) { throw new NullPointerException("null destColumnName"); }
		if (srcColumnName.equals(destColumnName)) { return; }
		
		for (int i=0; i<rows.size(); i++) {
			Map<String,Object> row = rows.get(i);
			if (row.containsKey(srcColumnName)) {
				row.put(destColumnName, row.get(srcColumnName));
				row.remove(srcColumnName);
			}
		}
	}

	/** Add a structured column containing a constant value. This method will iterate through all 
	 * rows of a table, adding a new newColumnName key with the supplied value.
	 * 
	 * @param rows the table being modified
	 * @param newColumnName the name of the row key (column) being added
	 * @param value the value to add to the row
	 */
	public static void addStructuredListColumn(List<Map<String, Object>> rows, String newColumnName, Object value) {
		for (int i=0; i<rows.size(); i++) {
			Map<String,Object> row = rows.get(i);
			row.put(newColumnName, value);
		}
	}

    static {
        // NB: these used to be ConcurrentReaderHashMaps, but I didn't want to
        // bring concurrent.jar into the classpath.
        gettersCache = new ConcurrentHashMap();
        settersCache = new ConcurrentHashMap();
    }
}
