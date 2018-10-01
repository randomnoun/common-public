package com.randomnoun.common;

/* (c) 2013 randomnoun. All Rights Reserved. This work is licensed under a
 * BSD Simplified License. (http://www.randomnoun.com/bsd-simplified.html)
 */

import java.util.*;


/**
 * Can probably do all this with ThreadLocals these days.
 * 
 * <p>Manages a set of hashmaps used by EJB/servlet containers to hold <b>thread-global</b> data.
 * 
 * <p>For example, calling <code>ThreadContext.get("asd")</code> from two different threads may
 * return two different values.
 *
 * <p>In addition, each thread is supplied with a stack context which allows multiple EJBs
 * to run in the same thread (e.g. if one EJB invokes another local EJB which then
 * executes within the same thread). To ensure separation of data, each EJB
 * should invoke {@link #ThreadContext.push()} to create its own context as soon as
 * it is invoked, and call {@link #ThreadContext.pop()} before it terminates to
 * maintain the per-thread data stack. (i.e. the invoked method, not the method caller is
 * responsible for maintaining data integrity).
 *
 * <p>This class has all the methods of {@link java.util.Map}, although these are
 * now static. A real thread-specific java.util.Map object can be obtained by
 * calling the {@link #getMap() getMap} method. Using this object rather than
 * calling static methods on ThreadContext directly may improve performance. The
 * map instance returned by getMap() is <b>not</b> synchronized, since it is
 * assumed that only one thread is accessing it's own context at the same time.
 *
 * <p>Each EJB (or thread) wishing to use this data structure should clean up after
 * itself by invoking ThreadContext.clear() before passing control back to the
 * EJB container (or terminating). Every thread MUST be removed using .pop(), otherwise
 * memory leaks will occur.
 *
 * <p>Implementation note: this class relies on different thread's returning unique
 * strings for their Thread.getName() method. It will break if this is not the
 * case in a particular VM implementation. 
 *
 * <p><b>NB:</b> Don't use this method to pass state between EJBs. 
 * Where an EJB invokes a second EJB, it should be
 * assumed that the invoked EJB exists on another VM. Since this data
 * structure will not be populated correctly on the second VM, it should not
 * be relied upon to pass state information between EJBs.
 *
 * @author knoxg
 * 
 */
public class ThreadContext
{
    public static String _revision = "$Id$";

    /** Backing map of threads-IDs to thread-specific Lists, containing maps */
    private static Map<String, List<Map<Object, Object>>> globalMap;

    /**
     * Creates a new ThreadContext object.
     *
     * @throws Exception DOCUMENT ME!
     */
    public ThreadContext()
        throws Exception
    {
        /** VM singleton - cannot be instantiated */
        throw new Exception("Cannot instantiate ThreadContext");
    }

    /** Create a new context for this thread.
     *
     *  @returns The global map for the newly created context.
     **/
    public static Map<Object, Object> push()
    {
        List<Map<Object,Object>> list;
        Map<Object, Object> map;
        String currentThreadName = Thread.currentThread().getName();

        synchronized (globalMap) {
            list = (List<Map<Object,Object>>) globalMap.get(currentThreadName);

            if (list == null) {
                list = new ArrayList<Map<Object,Object>>();
                globalMap.put(currentThreadName, list);
            }

            map = new HashMap<Object, Object>();
            list.add(map);
        }

        return map;
    }

    /** Remove the most newly-created context for this thread.
     *
     *  @throws IllegalStateException This exception is thrown if no context
     *    exists for this thread.
     */
    public static void pop()
    {
        // NB: Lists are never removed from the global hashmap, as we presume
        // that threads are reused by the server container. This may prevent
        // this class from being used in a more general-purpose (non-EJB-container)
        // solution.
        List<Map<Object,Object>> list;
        String currentThreadName = Thread.currentThread().getName();

        synchronized (globalMap)
        {
            list = globalMap.get(currentThreadName);

            if (list == null) {
                throw new IllegalStateException("No context exists for this thread.");
            }

            if (list.size() == 0) {
                throw new IllegalStateException("No context exists for this thread.");
            }

            list.remove(list.size() - 1);

            if (list.size() == 0) {
                globalMap.remove(currentThreadName);
            }
        }
    }

    /** Returns a java.util.Map object which contains all thread-specific
     * data. Using the returned object from this method repeatedly
     * from within a single calling method will be more efficient than calling
     * the static methods of this class.
     *
     * @return The map for this thread.
     * @throws IllegalStateException This exception is thrown if a per-thread
     *   context has not yet been created by calling push().
     */
    public static Map<Object, Object> getMap()
    {
        List<Map<Object,Object>> list;
        String currentThreadName = Thread.currentThread().getName();

        list = globalMap.get(currentThreadName);

        if (list == null) {
            list = new ArrayList<Map<Object,Object>>();
            globalMap.put(currentThreadName, list);
        }

        // create an exception if one does not exist
        if (list.size() == 0)         {
            throw new IllegalStateException(
                "ThreadContext has not yet been created (push() must be invoked before getMap())");
        }

        return list.get(list.size() - 1);
    }

    /** Returns true if a ThreadContext has been set up for the current Thread,
     * (through a push() call), false otherwise.
     *
     * @return true if a ThreadContext exists, false otherwise
     */
    public static boolean contextExists()
    {
        List<Map<Object,Object>> list;
        String currentThreadName = Thread.currentThread().getName();

        list = globalMap.get(currentThreadName);

        return (list != null) && (list.size() > 0);
    }

    // ***************************************************************************************
    //
    // All methods below this line mirror the methods within java.util.Map, but are static,
    // and operate on the current thread context.
    //

    /** Clears the current EJB's map. This method only clears out the existing thread context,
     *  it does not pop the current context off the stack
     */
    public static void clear()
    {
        getMap().clear();
    }

    /** As per {@link java.util.Map#containsKey(java.lang.Object)} for the current thread's context
     * 
     * @return true if this map contains a mapping for the specified key
     *
     * @throws IllegalStateException This exception is thrown if a per-thread
     *   context has not yet been created by calling push().
     */
    public static boolean containsKey(Object key)
    {
        return getMap().containsKey(key);
    }

    /** As per {@link java.util.Map#containsValue(java.lang.Object)} for the current thread's context
     *  
     * @return true if this map maps one or more keys to the specified value. 
     *
     * @throws IllegalStateException This exception is thrown if a per-thread
     *   context has not yet been created by calling push().
     */
    public static boolean containsValue(Object value)
    {
        return getMap().containsValue(value);
    }

    /**
     * As per {@link java.util.Map#entrySet()} for the current thread's context
     *
     * @return a set view of the mappings contained in this map.
     *
     * @throws IllegalStateException This exception is thrown if a per-thread
     *   context has not yet been created by calling push().
     */
    public static Set<Map.Entry<Object, Object>> entrySet()
    {
        return getMap().entrySet();
    }

    /**
     * As per {@link java.util.Map#get(java.lang.Object)} for the current thread's context
     *
     * @param key key whose associated value is to be returned. 

     *
     * @return the value to which this map maps the specified key, or null if the map contains 
     *   no mapping for this key
     *
     * @throws IllegalStateException This exception is thrown if a per-thread
     *   context has not yet been created by calling push().
     */
    public static Object get(Object key)
    {
        return getMap().get(key);
    }

    /**
     * As per {@link java.util.Map#isEmpty(java.lang.Object)} for the current thread's context
     *
     * @return true if this map contains no key-value mappings
     *
     * @throws IllegalStateException This exception is thrown if a per-thread
     *   context has not yet been created by calling push().
     */
    public static boolean isEmpty()
    {
        return getMap().isEmpty();
    }

    /**
     * As per {@link java.util.Map#keySet(java.lang.Object)} for the current thread's context
     *
     * @return a set view of the keys contained in this map
     *
     * @throws IllegalStateException This exception is thrown if a per-thread
     *   context has not yet been created by calling push().
     */
    public static Set<Object> keySet()
    {
        return getMap().keySet();
    }

    /**
     * As per {@link java.util.Map#put(java.lang.Object, java.lang.Object)} for the current thread's 
     * context
     *
     * @param key key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     *
     * @return previous value associated with specified key, or null if there was no mapping for key. A null return can also indicate that the map previously associated null 
     *   with the specified key, if the implementation supports null values
     *
     * @throws IllegalStateException This exception is thrown if a per-thread
     *   context has not yet been created by calling push().
     */
    public static Object put(Object key, Object value)
    {
        return getMap().put(key, value);
    }

    /**
     * As per {@link java.util.Map#putAll(java.util.Map)} for the current thread's context
     *
     * @param t Mappings to be stored in this map
     *
     * @throws IllegalStateException This exception is thrown if a per-thread
     *   context has not yet been created by calling push().
     */
    public static void putAll(Map<Object, Object> t)
    {
        getMap().putAll(t);
    }

    /**
     * As per {@link java.util.Map#remove(java.lang.Object)} for the current thread's context
     *
     * @param key key whose mapping is to be removed from the map
     *
     * @return previous value associated with specified key, or null if there was no mapping for key
     *
     * @throws IllegalStateException This exception is thrown if a per-thread
     *   context has not yet been created by calling push().
     */
    public static Object remove(Object key)
    {
        return getMap().remove(key);
    }

    /**
     * As per {@link java.util.Map#size()} for the current thread's context
     *
     * @return the number of key-value mappings in this map
     *
     * @throws IllegalStateException This exception is thrown if a per-thread
     *   context has not yet been created by calling push().
     */
    public static int size()
    {
        return getMap().size();
    }

    /**
     * As per {@link java.util.Map#values()} for the current thread's context
     *
     * @return a collection view of the values contained in this map
     *
     * @throws IllegalStateException This exception is thrown if a per-thread
     *   context has not yet been created by calling push().
     */
    public static Collection<Object> values()
    {
        return getMap().values();
    }

    static
    {
        // set up the globalMap containing all ThreadContexts
        globalMap = Collections.synchronizedMap(new HashMap<String, List<Map<Object, Object>>>());
    }
}
