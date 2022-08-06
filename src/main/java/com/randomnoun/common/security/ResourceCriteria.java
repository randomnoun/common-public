package com.randomnoun.common.security;

/* (c) 2013 randomnoun. All Rights Reserved. This work is licensed under a
 * BSD Simplified License. (http://www.randomnoun.com/bsd-simplified.html)
 */

import java.io.*;
import java.util.*;

/**
 * A criteria than can be used to evaluate fine-grained resources. (e.g.
 * an 'account' resource where account.balance &gt; 1000). This class should be
 * subclassed by security implementations that wish to perform access control
 * over fine-grained resources.
 *
 * <p>It is the responsibility of this class to translate its criteriaString into
 * a form that can return a true or false value when supplied a
 * criteria context (in this case, an instance of the account). 
 * This class provides a default implementation of the
 * {@link #evaluate(Map)} method, which currently always returns true.
 * 
 * @author knoxg
 */
public abstract class ResourceCriteria
    implements Serializable
{
    
    /** generated serialVersionUID */
	private static final long serialVersionUID = 7387179933750418124L;
	
	/** The string used to construct this ResourceCriteria object */
    private String criteriaString;

    /** Create a ResourceCriteria object */
    public ResourceCriteria(String criteriaString)
    {
        this.criteriaString = criteriaString;
    }

    /** Returns the string used to construct this ResourceCriteria object.
     *
     * @return the string used to construct this ResourceCriteria object.
     */
    public String getCriteriaString()
    {
        return criteriaString;
    }

    /** Returns a string representing this ResourceCriteria object.
     *  This method Should be overridden by subclasses.
     *
     *  @return  a string representing this ResourceCriteria object
     */
    public String toString()
    {
        return criteriaString;
    }

    /**
     * Returns true if this resourceCriteria identifies a resource with the
     * supplied criteriaContext. The context used is resource-specific, but
     * is always expressed as a name/value Map. e.g. for a message resource,
     * the context may include a the headers of that message, and the values
     * for those headers. For a system property resource, the context may
     * just be the name of that system property.
     *
     * @param criteriaContext The context used to identify the resource
     * @return true if this ResourceCriteria matches the supplied context,
     *   false if it does not.
     */
    public boolean evaluate(Map<String, Object> criteriaContext)
    {
        return true;
        
    }
}
