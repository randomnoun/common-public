package com.randomnoun.common.security;

/* (c) 2013 randomnoun. All Rights Reserved. This work is licensed under a
 * BSD Simplified License. (http://www.randomnoun.com/bsd-simplified.html)
 */

import java.io.IOException;
import java.util.*;

import com.randomnoun.common.security.User;

/** Security loader object.
 *
 * <p>An instance of this class is responsible for serialising/deserialising
 * security information to a persistent data store. The SecurityContext object
 * will invoke methods in this class as appropriate. Initialisation properties
 * can be passed to the SecurityLoader using a properties map in the SecurityContext
 * constructor.
 *
 * 
 * @author knoxg
 */
public interface SecurityLoader
{
    /**
     * Initialise this security loader. This method will be invoked by the SecurityContext
     * object on initialisation
     *
     * @param properties Initialisation properties for this loader.
     */
    public void initialise(Map<String, Object> properties);


    // the loadAll() methods here were for the security editor UI, which I imagine
    // I'm not going to implement again in the next few years. Could just remove it.
    
    /**
     * Return a structured list of application specific Permission objects which is used to 
     * preload the SecurityContext rolePermission cache. Each permission returned must be a 
     * role-based Permission.
     */
    public List<Permission> loadAllRolePermissions()
        throws IOException;

    /**
     * Return List of Permission objects associated with a particular role
     */    
    public List<Permission> loadRolePermissions(String role) 
     throws IOException;


    /**
     * Return a list of Permission objects 
     * associated with the roles possessed by a particular user.
     * 
     * Equivalent to appending the results of calling 
     * {@link #loadRolePermissions(String)} for each role that a user
     * is in.  
     */         
    public List<Permission> loadUserRolePermissions(User user) throws IOException;
    
            
    /**
     * Returns the list of permissions assigned to this user.
     */
    public List<Permission> loadUserPermissions(User user) throws IOException;

	/**
	 * Returns the list of roles assigned to this user.
	 */
	public List<String> loadUserRoles(User user) throws IOException;


	/** Load a user. Will not load any role or permission data for that user.
	 * 
	 * <p>Will probably throw an IOException if the user doesn't exist.
	 * 
	 * @return a User object.
	 */
	public User loadUser(long userId) throws IOException;
	
    /**
     * Return a list of User objects representing all users contained in this
     * security context. Permission information relating to that user is not
     * populated unless the 'populatePermission' parameter is set to true.
     *
     * <p>The information returned by this function may be cached, depending
     * on the initialisation properties of the security context.
     *
     * @return A List of Users.
     */
    public List<User> loadAllUsers()
        throws IOException;

    /**
     * Return a List of all resources in this security context, identified
     * by String.
     *
     * <p>The information returned by this function may be cached, depending
     * on the initialisation properties of the security context.
     *
     * @return A List of resources.
     */
    public List<String> loadAllResources()
        throws IOException;

    /**
     * Return a List of all activities in this security context for a given
     * resource, identified by String.
     *
     * <p>The information returned by this function may be cached, depending
     * on the initialisation properties of the security context.
     *
     * @param resourceName The resource we wish to retrieve activities for
     *
     * @return A List of activities.
     */
    public List<String> loadAllActivities(String resourceName)
        throws IOException;

    /**
     * Return a List of all roles in this security context, identified
     * by String.
     *
     * <p>The information returned by this function may be cached, depending
     * on the initialisation properties of the security context.
     *
     * @return A List of Roles, just the name.
     */
    public List<String> loadAllRoles()
        throws IOException;

	/**
	 * Return a List of all permissions in this security context, as Permission objects.
	 * (User, role and criteria fields will be left blank in these objects).
	 *
	 * <p>The information returned by this function may be cached, depending
	 * on the initialisation properties of the security context.
	 *
	 * @return A List of Permission objects available to this application
	 */
	public List<Permission> loadAllPermissions()
		throws IOException;



    /**
     * Return a List of all roles in this security context. Each role is returned
     * as a Map containing (by default) the keys roleId, roleName, description
     *
     * @return A List of Roles, in Map format.
     */
    public List<Map<String, Object>> loadAllRoleDetails()
        throws IOException;
        
    /**
     * Return a List of all users in this security context. Each user is returned
     * as a Map containing (by default) the keys userId, name
     *
     * @return A List of Users, in Map format.
     */
    public List<Map<String, Object>> loadAllUserDetails()
        throws IOException;    

	/** Informs any delegate security contexts to reset themselves. */
	public void resetSecurityContext() throws IOException;
	
    /** Persists the role and permission information recorded for this user to
     * the database. Existing role and permission information in the database will be
     * removed.
     *
     * @param user The user to persist
     * @param permissions The permissions for this user
     */
    public void saveUserRolesAndPermissions(User user, List<String> roles, List<Permission> userPermissions)
        throws IOException;

    /** Persists the permission information for this role to
     * the database. Existing permission information in the database will be
     * removed.
     *
     * @param user The role to persist
     * @param permissions The permissions for this role
     */
    public void saveRolePermissions(String role, List<Permission> rolePermissions)
        throws IOException;
                  
}
