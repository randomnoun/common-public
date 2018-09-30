package com.randomnoun.common.security;

/* (c) 2013 randomnoun. All Rights Reserved. This work is licensed under a
 * BSD Simplified License. (http://www.randomnoun.com/bsd-simplified.html)
 */

import java.io.*;

import com.randomnoun.common.security.ResourceCriteria;
import com.randomnoun.common.security.User;

/**
 * A class encapsulating permission information. A 'permission' allows a user (or role)
 * to perform an 'activity' on a 'resource'. Roles, activities and resources are
 * all specified as Strings, a user is specified by a User object (identified by
 * username and customerId).
 * 
 * <p>A resource may also have a criteria supplied; e.g. a user may have a view/account
 * permission, but only accounts with a certain monetary value.
 *
 * <p>Two constructors are provided, one for users and one for roles. Note that there
 * are no 'setter' methods in this class; permissions may only be altered through
 * creating new ones.
 * 
 * @author knoxg
 */
public class Permission implements Serializable
{
     
    /** generated serialVersionUID */
	private static final long serialVersionUID = -3718647050573844606L;

	/** The role for this permission. A permission may apply to a role or a user but not both. */
    private String role;

    /** The user for this permission. A permission may apply to a role or a user but not both. */
    private User user;

    /** The activity for this permission. */
    private String activity;

    /** The resource for this permission. */
    private String resource;

    /** The resourceCriteria for this permission. */
    private ResourceCriteria resourceCriteria;

    /** Create a new role-based permission.
     *
     * @param role  the name of this role this permission applies to
     * @param activity  the name of the activity we are permitting
     * @param resource  the resource we are permitting access to
     * @param resourceCriteria   a criteria which limits the types of resources that this
     *   permission applies to
     */
    public Permission(String role, String activity, String resource,
        ResourceCriteria resourceCriteria)
    {
        if (role==null) { throw new NullPointerException("null role"); }
        if (activity==null) { throw new NullPointerException("null activity"); }
        if (resource==null) { throw new NullPointerException("null resource"); }

        this.role = role;
        this.user = null;
        this.activity = activity;
        this.resource = resource;
        this.resourceCriteria = resourceCriteria;
    }

    /** Create a new role-based permission.
     *
     * @param user  the user this permission applies to
     * @param activity  the name of the activity we are permitting
     * @param resource  the resource we are permitting access to
     * @param resourceCriteria   a criteria which limits the types of resources that this
     *   permission applies to
     */
    public Permission(User user, String activity, String resource,
        ResourceCriteria resourceCriteria)
    {
        if (user==null) { throw new NullPointerException("null user"); }
        if (activity==null) { throw new NullPointerException("null activity"); }
        if (resource==null) { throw new NullPointerException("null resource"); }
        
        this.user = user;
        this.role = null;
        this.activity = activity;
        this.resource = resource;
        this.resourceCriteria = resourceCriteria;
    }
    
	/** Create a permission that is not assigned to either a user or role
	 * 
	 * @param activity the name of the activity we are permitting
	 * @param resource the resource we are permitting acess to
	 */
    public Permission(String activity, String resource) {
		if (activity==null) { throw new NullPointerException("null activity"); }
		if (resource==null) { throw new NullPointerException("null resource"); }
		this.activity = activity;
		this.resource = resource;
    }
    
    /** Create a permission that is not assigned to either a user or role
	 * 
	 * @param permission a permission in 'activity.resource' format
	 */
    public Permission(String permission) {
		if (permission==null) { throw new NullPointerException("null permission"); }
		int pos = permission.indexOf('.');
		if (pos==-1) { throw new IllegalArgumentException("permission must be in 'activity.resource' format"); }
		this.activity = permission.substring(0,pos);
		this.resource = permission.substring(pos+1);
    }
    
    /** Create a permission that is not assigned to either a user or role, with a resource criteria
	 * 
	 * @param permission a permission in 'activity.resource' format
	 */
    public Permission(String permission, ResourceCriteria resourceCriteria) {
		if (permission==null) { throw new NullPointerException("null permission"); }
		int pos = permission.indexOf('.');
		if (pos==-1) { throw new IllegalArgumentException("permission must be in 'activity.resource' format"); }
		this.activity = permission.substring(0,pos);
		this.resource = permission.substring(pos+1);
		this.resourceCriteria = resourceCriteria;
    }
    

    /** Returns true if this permission is user-based (as opposed to role-based).
     *
     * @return true if this permission is user-based (as opposed to role-based)
     * 
     * @throws IllegalStateException if this permission is not assigned to a user or role
     */
    boolean isUserPermission()
    {
    	if (user!=null) { 
    		return true;
    	} else if (role!=null) {
    		return false;
    	} else {
    		throw new IllegalStateException("Permission is not assigned to user or role");
    	}
    }

    /** Returns true if this permission is role-based (as opposed to user-based).
     *
     * @return true if this permission is role-based (as opposed to user-based)
     * 
     * @throws IllegalStateException if this permission is not assigned to a user or role
     */
    boolean isRolePermission()
    {
		if (role!=null) { 
			return true;
		} else if (user!=null) {
			return false;
		} else {
			throw new IllegalStateException("Permission is not assigned to user or role");
		}
    }

    /** Returns the role this permission applies to, or null if it is a user-based role.
     *
     * @return the role this permission applies to, or null if it is a user-based role
     */
    public String getRole()
    {
        return role;
    }

    /** Returns the user this permission applies to, or null if it is a user-based role.
     *
     * @return the user this permission applies to, or null if it is a user-based role
     */
    public User getUser()
    {
        return user;
    }

    /** Returns the activity this permission applies to.
     *
     * @return the activity this permission applies to
     */
    public String getActivity()
    {
        return activity;
    }

    /** Returns the resource this permission applies to.
     *
     * @return the resource this permission applies to
     */
    public String getResource()
    {
        return resource;
    }

    /** Returns the resourceCriteria that applies to this permission.
     *
     * @return the resourceCriteria that applies to this permission
     */
    public ResourceCriteria getResourceCriteria()
    {
        return resourceCriteria;
    }
}
