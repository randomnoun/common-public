package com.randomnoun.common;

/* (c) 2013 randomnoun. All Rights Reserved. This work is licensed under a
 * BSD Simplified License. (http://www.randomnoun.com/bsd-simplified.html)
 */

import java.util.*;

import org.apache.log4j.Logger;

/**
 * Most-recently used cache class. 
 * 
 * <p>You probably want to use something in the guava Collections library these days instead.
 * 
 * <p>This class can be used to implement serverside data caches. The constructor
 * takes three parameters: maximum size, expiry time (in milliseconds) and a 
 * 'RetrievalCallback' object (defined as an interface in this class). 
 * 
 * <p>As items are put into this object, they are timestamped; if a key is applied
 * to retrieve an object that has 'expired', then this object will return null
 * (if no callback has been supplied), or will execute the callback to refresh
 * the value. A callback can (optionally) be supplied with every get() invocation
 * so that this object knows precisely how to generate the requested object. (This
 * works since it is usually computationally cheaper to generate the Callback object
 * than to generate the result of the callback, which generally needs to fetch
 * information from a database).
 * 
 * <p>There are two possible expiry mechanisms: either an item is expired from the
 * time it is entered into the cache, or expired from the time it is last retrieved
 * from the cache. The former makes sense for 'dynamic' data that may change over time
 * (e.g. rowcounts); the latter makes sense for 'static' data that is just used to
 * cache large objects that don't need to be in memory at all times 
 * (e.g. message definitions). By default this object will operate as per 'static'
 * rules; the other method can be selected by calling the setDynamicCaching() after
 * construction.
 *
 * <p><b>Implementation notes</b>
 *    
 * <p>It may be possible to extend this class to use WeakReferences, so that it can
 * shrink in size as memory constraints within the VM become increased. This is 
 * unlikely to be useful in practice, however, since this is probably an indication
 * of a memory leak or over-utilised server. It may make more sense to just reduce the
 * maximum size in these cases. Performance tuning would be required to know 
 * for sure.
 * 
 * <p>Also note that if an object exceeds it's expiryTime, it is *not* automatically
 * removed from the cache. This would involve having an additional Timer thread
 * running through these objects and scanning for expired objects, which is unlikely
 * to be useful in practice. An additional method could be implemented to trigger these
 * cleanups if this becomes necessary.
 * 
 * @author knoxg
 * 
 *
 */

// this probably isn't as efficient as it could be (it contains a mruList AND a 
// HashMap of expiryTimes, which I'm sure is overkill. To do this properly
// would involve extending Map.Entry to include a timestamp value, however,
// which would require a fair amount of re-engineering. (would need to subclass 
// Map.Entry, and use "this.putAll()" to insert subclasses into this instance. 
// you'd still need a way of keeping a sorted list of timestamped entries anyway,
// now that I think about it (to choose the next expiry candidate), so this may not 
// be so bad after all. 
public class MRUCache
    extends HashMap
{
    
    
    
    /** Logger instance for this class */
    public static Logger logger = Logger.getLogger(MRUCache.class);
    
    /** The callback interface. Caches should provide one of these classes to 
     * perform any time-consuming tasks required to generate values in this cache.
     * If the time taken to generate a value isn't time-consuming, then perhaps
     * you shouldn't be using a cache to store them.
     */
    public static interface RetrievalCallback {
        
        /** Dynamically generates the value of an entity in a cache
         * 
         * @param key The key used to reference the cached object
         * 
         * @return The newly generated (or updated) cached object
         */ 
        public Object get(Object key);
    }
    
    /** If set to true, expires data from the time it was <i>entered</i> into the cache,
     * rather than the time if was <i>fetched</i> from the cache. Suitable when we 
     * wish to apply a strict expiry time for dynamically-updating data, e.g.
     * rowcounts.
     */
    private boolean dynamicCaching = false;

    /** An ordered list of the objects in this cache. Objects at position 0 on the list
     *  have been the most recently accessed (i.e. objects will be expired from the
     * end of this list). */
    private LinkedList mruList;

    /** The maximum number of elements that this cache can hold. If &lt;= 0, then the
     * size of the map is unlimited. */
    private int cacheSize;

    /** A callback which can be used to populate entries in the cache that are unknown,
     *  or have expired. */
    private RetrievalCallback callbackInstance;

    /** A Map whose keys are elements in this cache, and who's entries are the last time
     *  that entry was 'touched' (i.e. added to or retrieved from the cache) */
    private Map lastUpdatedTimes;

    /** The amount of time an entry can remain valid, measured from when the element
     *  is first added to the cache. If expiryTime is set to &lt;= 0, then items never expire. */
    private int expiryTime;

    /**
     * Creates a new MRUCache object.
     *
     * @param cacheSize  The maximum number of elements that this cache can hold. A value &lt;=0 means 
     *    that the size of this map is not limited.
     * @param expiryTime The amount of time (in ms) an entry can remain valid, measured from when the element
     *    is first added to the cache (or last retrieved if {@link #setDynamicCaching()} is 
     *    in effect. A value &lt;=0 means that items do not have an
     *    expiry period.
     * @param callback   A callback which can be used to populate entries in the cache that are unknown,
     *    or have expired. This parameter can be set to null <b>if and only if</b> the
     *    two-parameter {@link #get(Object, RetrievalCallback)} method is used to retrieve
     *    elements from the map.
     */
    public MRUCache(int cacheSize, int expiryTime, RetrievalCallback callback)
    {
        mruList = new LinkedList();
        lastUpdatedTimes = new HashMap();
        this.cacheSize = cacheSize;
        this.callbackInstance = callback;
        this.expiryTime = expiryTime;
    }

    /**
     * Retrieve an element from the cache. If a callback was defined in the cache
     * constructor, then this will be used to refresh
     * values in the cache if they are missing, or have expired.
     *
     * @param key key whose associated value is to be returned
     *
     * @return the value to which this map maps the specified key, or
     *   null if the map contains no mapping for this key.
     */
    public synchronized Object get(Object key)
    {
        return get(key, callbackInstance);
    }

    /**
     * Retrieves an element from the cache. The supplied callback will be used to
     * refresh the value in the cache if it is missing, or has expired. If the
     * requested element is missing and the callback is set to null, then
     * null is returned.
     *
     * @param key key whose associated value is to be returned
     * @param callback a callback used to populate the cache if necessary
     *
     * @return the value to which this map maps the specified key, or
     *   null if the map contains no mapping for this key.
     */
    public synchronized Object get(Object key, RetrievalCallback callback)
    {
        long now = System.currentTimeMillis();

        if (!this.containsKey(key))
        {
            if (callback == null) {
                return null;
            } else {
                // cache is missing key; retrieve from callback and update access times
                synchronized (this) {
                    if (cacheSize > 0 && mruList.size() >= cacheSize) {
                        this.remove(mruList.getLast());
                        mruList.removeLast();
                    }

                    super.put(key, callback.get(key));
                    lastUpdatedTimes.put(key, new Long(now));
                    mruList.addFirst(key);
                }
            }
            
        } else {
            
            // cache contains key
            if (expiryTime > 0) {
                
                // expire this element if it's old
                Long lastUpdatedTime = (Long)lastUpdatedTimes.get(key);
                if (lastUpdatedTime==null) {
                	logger.warn("MRUCache contains key '" + key + "' with no lastUpdatedTime set");
                }
                if (lastUpdatedTime == null || (lastUpdatedTime.longValue() + expiryTime < now)) {
                    synchronized (this) {
                        if (callback == null) {
                            // no callback, we just remove it
                            super.remove(key);
                            lastUpdatedTimes.remove(key);
                            mruList.remove(key);

                            return null;
                        } else {
                            super.put(key, callback.get(key));
                            lastUpdatedTimes.put(key, new Long(now));
                        }
                    }
                }
            }

            // we have an element and it has been retrieved, move it to the
            // top of the mruList. (we may not necessarily want to do this;
            // this should be a constructor preference). There are pros & cons 
            // to this approach:
            // pros: if we just got an element, we probably don't want it expiring out
            //   of the cache
            // cons: this moves 'stale' objects to the top of the mruList
            //   stack; this means that when we remove elements from mruList
            //   to fit in more elements, we may be expiring older elements.
            // this should probably be a decision made by the developer, rather than
            // this code, but I'd like to keep the interface simple.
            if (!dynamicCaching) {
                synchronized (this) {
                    mruList.remove(key);
                    mruList.addFirst(key);
                }
            }
        }

        return super.get(key);
    }

    /**
     * Returns the object if it is in the cache and has not expired.
     * This method will return null if the the key is not in cache, or
     * if it is in the cache but has expired.
     *
     * @param key key whose associated value is to be returned
     *
     * @return the value to which this map maps the specified key, or
     *   null if the map contains no mapping for this key.
     */
    public synchronized Object getNoCallback(Object key) {
        if (!this.containsKey(key)) {
            return null;
        }

        if (expiryTime > 0) {
            if (((Long)lastUpdatedTimes.get(key)).longValue() + expiryTime < 
              System.currentTimeMillis()) 
            {
                return null;
            }
        }

        return super.get(key);
    }

    /**
     * Add an element into the cache.
     *
     * @param key  key with which the specified value is to be associated.
     * @param value value to be associated with the specified key.
     * 
     * @return the previous value of this map entry, if one exists, otherwise null
     */
    public synchronized Object put(Object key, Object value) {
    	
        synchronized (this) {
			Object lastValue = super.put(key, value);
            lastUpdatedTimes.put(key, new Long(System.currentTimeMillis()));
            return lastValue;
        }
    }
    
    /** Enforces dynamic caching rules. When called, data will be expired from the time it 
     * is <i>entered</i> into the cache, rather than the time if is <i>fetched</i> from 
     * the cache. Suitable when we wish to apply a strict expiry time for 
     * dynamically-updating data, e.g. rowcounts.
     */
    public void setDynamicCaching() {
        dynamicCaching = true;
    }
}
