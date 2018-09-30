package com.randomnoun.common.security;

/* (c) 2013 randomnoun. All Rights Reserved. This work is licensed under a
 * BSD Simplified License. (http://www.randomnoun.com/bsd-simplified.html)
 */

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.randomnoun.common.MRUCache;
import com.randomnoun.common.security.Permission;
import com.randomnoun.common.security.ResourceCriteria;
import com.randomnoun.common.security.SecurityContext;
import com.randomnoun.common.security.SecurityLoader;
import com.randomnoun.common.security.User;

/**
 * This class manages users, roles, resources and permissions for an application.
 * 
 * <p>Most of the code that adds/deletes/maintains these objects has been removed, that is
 * now the responsibility of the security implementation code. 
 * 
 * <p>Methods to read user and permission data are delegated to the SecurityLoader, and
 * methods that authenticate users are delegated to the SecurityAuthenticator.
 * 
 * <p>This class now mostly acts as a cache for user and role data, and can perform
 * simple and complex permission checks for users against resources.
 * 
 * <p>The following properties can be passed to the SecurityContext during construction;
 * property keys are defined as static public final Strings in this class.
 *
 * <attributes>
 *   INIT_CASE_INSENSITIVE - make the security cache case-insensitive, typically when interfacing with 
 *     Active Directory. Defaults to false.
 *   INIT_USER_CACHE_SIZE - maximum size of user cache
 *   INIT_USER_CACHE_EXPIRY - expiry time of users from the user cache (in milliseconds). If this
 *     property is not set, user caching is disabled.
 * </attributes>
 *
 * <p>Additional properties may also be required based on the SecurityLoader implementation used.
 * 
 * @author knoxg
 *
 * @see com.randomnoun.common.security.SecurityLoader
 */
public class SecurityContext {
    
    // /** The logger for this class */
    // private static Logger logger = Logger.getLogger(SecurityContext.class.getName());

    /** SecurityContext properties */
    private Map<String, Object> properties = null;

    /** An initialisation property key. See the class documentation for details. */
    public static final String INIT_USER_CACHE_SIZE = "securityContext.userCacheSize";

    /** An initialisation property key. See the class documentation for details. */
    public static final String INIT_USER_CACHE_EXPIRY = "securityContext.userCacheExpiry";

    /** An initialisation property key. See the class documentation for details. */
    public static final String INIT_USERNAME_MASK = "securityContext.usernameMask";
    
    /** An initialisation property key. See the class documentation for details. */
	public static final String INIT_CASE_INSENSITIVE = "securityContext.caseInsensitive";

    /** Maps rolenames to maps of permission names (in the form 'activity.resource')
     * to Permission objects (possibly containing ResourceCriteria objects). 
     * 
	 * If the security context is case-insensitive, then role names are lower-cased.
     */
    private Map<String, Map<String, Permission>> rolePermissionCache = null;

    /** Maps usernames to maps of permission names (in the form 'activity.resource')
     * to Permission objects (possibly containing ResourceCriteria objects). 
     *  
     * If the security context is case-insensitive, then usernames are lower-cased. */
    private Map<User, Map<String, Permission>> userPermissionCache = null;
    
    /** Maps user objects to list of roles. 
     * 
     * @TODO convert to HashSet ?
     */
    private Map<User, List<String>> userRoleCache = null;

    /** Maps userIds to Users. 
     */
    private Map<Long, User> userCache = null;
    
    /** This security loader is used to retrieve information from a persistant data
     *  store for this context */
    private SecurityLoader securityLoader = null;

    
    /** The authenticator, if you want to actually check passwords */
    private SecurityAuthenticator securityAuthenticator = null;

    // should probably use guava caches for all of this now
    
    /** This class is invoked by the MRUCache to recalculate values in the
     * user permission cache that have expired, or where not in the cache to begin with.
     */
    private static class UserPermissionCallback
        implements MRUCache.RetrievalCallback {

    	SecurityLoader loader;

        /** Creates a new callback used to populate the
         *  user cache
         */
        public UserPermissionCallback(SecurityLoader loader) {
            this.loader = loader;
        }

        /** this method is only called if the required value is not in the cache,
         * or the value in the cache has expired.
         *
         * @param key The username of the User to return
         */
        public Object get(Object key) {
            if (key==null) { throw new NullPointerException("null key"); }
            if (!(key instanceof User)) {
            	throw new IllegalArgumentException("Expected user as User, found " + key.getClass().getName());
            }

            User user = (User) key;
			
			Map<String, Permission> result = new HashMap<String, Permission>();
            try {
                List<Permission> permissions = loader.loadUserPermissions(user);
                for (Iterator<Permission> i = permissions.iterator(); i.hasNext(); ) {
                	Permission perm = (Permission) i.next();
                	if (result.containsKey(perm.getActivity() + "." + perm.getResource())) {
                		throw new IllegalStateException("User '" + key + "' contains two versions of permission '" + 
                		  perm.getActivity() + "." + perm.getResource() + "'; please check the database");
                	}
                	result.put(perm.getActivity() + "." + perm.getResource(), perm);
                }
            } catch (IOException ioe) {
                throw new RuntimeException("IOException reading user permissions", ioe);
            }
            return result;
        }
    }

	/** This class is invoked by the MRUCache to recalculate values in the
	 * user permission cache that have expired, or where not in the cache to begin with.
	 */
	private static class RolePermissionCallback
		implements MRUCache.RetrievalCallback {

		SecurityLoader loader;

		/** Creates a new rowcount callback used to populate the
		 *  user cache
		 */
		public RolePermissionCallback(SecurityLoader loader) {
			this.loader = loader;
		}

		/** this method is only called if the required value is not in the cache,
		 * or the value in the cache has expired.
		 *
		 * @param key The username of the User to return
		 */
		public Object get(Object key) {
			if (key==null) { throw new NullPointerException("null key"); }
			if (!(key instanceof String)) {
				throw new IllegalArgumentException("Expected roleName as string, found " + key.getClass().getName());
			}
			String rolename = (String) key;
			Map<String, Permission> result = new HashMap<String, Permission>();
			try {
				List<Permission> permissions = loader.loadRolePermissions(rolename);
				for (Iterator<Permission> i = permissions.iterator(); i.hasNext(); ) {
					Permission perm = (Permission) i.next();
					if (result.containsKey(perm.getActivity() + "." + perm.getResource())) {
						throw new IllegalStateException("Role '" + key + "' contains two versions of permission '" + 
						  perm.getActivity() + "." + perm.getResource() + "'; please check the database");
					}
					result.put(perm.getActivity() + "." + perm.getResource(), perm);
				}
			} catch (IOException ioe) {
				throw new RuntimeException("IOException reading role permissions", ioe);
			}
			return result;
		}
	}


	/** This class is invoked by the MRUCache to recalculate values in the
	 * user role cache that have expired, or where not in the cache to begin with.
	 */
	private static class UserRoleCallback
		implements MRUCache.RetrievalCallback {

		SecurityLoader loader;

		/** Creates a new rowcount callback used to populate the
		 *  user cache
		 */
		public UserRoleCallback(SecurityLoader loader) {
			this.loader = loader;
		}

		/** this method is only called if the required value is not in the cache,
		 * or the value in the cache has expired.
		 *
		 * @param key The username of the User to return
		 */
		public Object get(Object key) {
			if (key==null) { throw new NullPointerException("null key"); }
			if (!(key instanceof User)) {
				throw new IllegalArgumentException("Expected user as User, found " + key.getClass().getName());
			}
			
			try {
				// @TODO convert to HashSet ?
				return loader.loadUserRoles((User) key);
			} catch (IOException ioe) {
				throw new RuntimeException("IOException reading user permissions", ioe);
			}
		}
	}


	/** This class is invoked by the MRUCache to load Users. It delegates to the Loader.
	 */
	private static class UserCallback
		implements MRUCache.RetrievalCallback {

		SecurityLoader loader;

		/** Creates a new rowcount callback used to populate the
		 *  user cache
		 */
		public UserCallback(SecurityLoader loader) {
			this.loader = loader;
		}

		/** this method is only called if the required value is not in the cache,
		 * or the value in the cache has expired.
		 *
		 * @param key The username of the User to return
		 */
		public Object get(Object key) {
			if (key==null) { throw new NullPointerException("null key"); }
			if (!(key instanceof Number)) {
				throw new IllegalArgumentException("Expected numeric userId, found " + key.getClass().getName());
			}
			
			try {
				// @TODO convert to HashSet ?
				return loader.loadUser(((Number) key).longValue());
			} catch (IOException ioe) {
				throw new RuntimeException("IOException reading user", ioe);
			}
		}
	}


    /**
     * Creates a new SecurityContext object.
     *
     * @param properties Initialisation properties for this SecurityContext, its
     *   SecurityLoader, and SecurityAuthenticator
     *
     * @throws IllegalStateException if the context is configured to preload,
     *   and it fails to do so.
     */
    public SecurityContext(Map<String, Object> properties, SecurityLoader securityLoader, 
    		SecurityAuthenticator securityAuthenticator) {
        this.properties = properties;
        this.securityLoader = securityLoader;
        this.securityLoader.initialise(properties);
        this.securityAuthenticator = securityAuthenticator;
        this.securityAuthenticator.initialise(properties);
		resetSecurityContext();
    }
    

	/** Retrieve a list of permissions for this user, as Permission objects.
	 * 
	 * @param user
	 * @return
	 * @throws IOException
	 */
	public List<Permission> getUserPermissions(User user) throws IOException {
		// @TODO should get this User out of our userCache, keyed by id
		
		List<Permission> permissions = new ArrayList<Permission>();
		Map<String, Permission> cachedPermissions = userPermissionCache.get(user);
		for (Iterator<Permission> i = cachedPermissions.values().iterator(); i.hasNext(); ) {
			permissions.add(i.next());
		}
		return permissions;
	}


    /** Returns a list of Permission objects that apply to the specified rolename.
     *
     * @param roleName the role name
     * @return A List of Permission objects that apply to that role
     */
    public List<Permission> getRolePermissions(String roleName) {
        // retrieve from cache
        List<Permission> result = new ArrayList<Permission>();
        if (roleName==null) {
        	throw new NullPointerException("null roleName");
        } else {
        	Map<String, Permission> rolePermissions = rolePermissionCache.get(roleName);
        	if (rolePermissions == null) {
        		throw new IllegalArgumentException("Unknown role '" + roleName + "'");
        	}
        	for (Iterator<Permission> i = rolePermissions.values().iterator(); i.hasNext(); ) {
        		result.add(i.next());
        	}
        }
        return result;
    }

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
    public List<User> getAllUsers() throws IOException {
        return securityLoader.loadAllUsers();
    }

    /**
     * Return a List of all resources in this security context, identified
     * by String.
     *
     * <p>The information returned by this function may be cached, depending
     * on the initialisation properties of the security context.
     *
     * @return A List of resources
     */
    public List<String> getAllResources() throws IOException {
        return securityLoader.loadAllResources();
    }

	/**
	 * Return a List of all Permissions in this security context.
	 *
	 * <p>The information returned by this function may be cached, depending
	 * on the initialisation properties of the security context.
	 *
	 * @return A List of resources
	 */
	public List<Permission> getAllPermissions() throws IOException {
		return securityLoader.loadAllPermissions();
	}


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
     *
     * @throws SecurityException
     */
    public List<String> getAllActivities(String resourceName) throws IOException {
        return securityLoader.loadAllActivities(resourceName);
    }

    /**
     * Return a List of roles in this security context for the User, identified
     * by String.
     *
     * <p>The information returned by this function may be cached, depending
     * on the initialisation properties of the security context.
     *
     * @return A List of roles.
     */
    public List<String> getAllRoles()
        throws IOException {
        return securityLoader.loadAllRoles();
    }

    /**
     * Return a List of all roles in this security context, identified
     * by String.
     *
     * <p>The information returned by this function may be cached, depending
     * on the initialisation properties of the security context.
     *
     * @return A List of roles.
     */
    public List<String> getUserRoles(User user)
        throws IOException 
    {
        List<String> cachedRoles = userRoleCache.get(user);
        if (cachedRoles==null) {
        	throw new IllegalStateException("Unknown user '" + user + "'");
        }
        return cachedRoles;
    }

	/** 
	 * Returns a detailed list of roles from the security context. Each role
	 * is defined as a Map with the following keys:
	 * 
	 * <attributes>
	 * roleId - the numeric id for the role
	 * roleName - the name of the role for
	 * system - (Number) set to 1 if this role is read-only, 0 otherwise
	 * description - a description for the role
	 * </attributes>
	 * 
	 * @return a list of roles, as described above
	 * 
	 * @throws IOException
	 */
    public List<Map<String, Object>> getAllRoleDetails()
        throws IOException {
        return securityLoader.loadAllRoleDetails();
    }

	/** 
	 * Returns a detailed list of users from the security context. Each user
	 * is defined as a Map with the following keys:
	 * 
	 * <attributes>
	 * userId - the login name for the user
	 * name - the full name of the user
	 * system - (Number) set to 1 if this role is read-only, 0 otherwise
	 * </attributes>
	 * 
	 * @return a list of users, as described above
	 * 
	 * @throws IOException
	 */
    public List<Map<String, Object>> getAllUserDetails()
        throws IOException {
        return securityLoader.loadAllUserDetails();
    }

  


    /** Returns true if a user is allowed to perform the permission supplied. The permission
     *  is expressed in 'activity.resourceType' format, e.g. 'update.message'. No expression
     *  context is supplied; this method will not evaluate any conditional resource
     *  restrictions. This is useful in cases where the full resource context is not known,
     *  for example when a message is first created by a user.
     *
     *  <p>In this case, the 'create.message' permission can be checked using this method
     *  before the user starts entering information, and 'create.message' can be
     *  checked with an expression context after the header fields have been populated.
     *
     * <p>If a permission is supplied that is not known by the application, this
     * method will return false.
     *
     * @param user The user we are determining
     * @param permission The permission we are testing for. Permissions are expressed in
     *   'activity.resourceType' format.
     * @return true if the permission is allowed, false is the permission is denied.
     *
     * @throws NullPointerException if either parameter to this method is null
     * @throws IllegalArgumentException if the permission supplied is formatted incorrectly.
     */
    public boolean hasPermission(User user, String permission) {
        return hasPermission(user, permission, null);
    }

    /**
     * Returns true if a user is allowed to perform the permission supplied, with
     * given resource context. If a permission is assigned to both the user
     * and the role, then the user permission is evaluated first.
     *
     * @param user The user we are determining
     * @param permission The permission we are testing for. Permissions are expressed in
     *   'activity.resourceType' format.
     * @param context The resource context used to evaluate against the resource expression
     *
     * @return true if the permission is allowed, false is the permission is denied.
     *
     * @throws NullPointerException if either parameter to this method is null
     * @throws IllegalArgumentException if the permission supplied is formatted incorrectly.
     */
    public boolean hasPermission(User user, String permission, Map<String, Object> context) {
    	// @TODO should get this User out of our userCache, keyed by id
        if (permission == null) { throw new NullPointerException("Null permission"); }
        if (user == null) { throw new NullPointerException("Null user"); }

        int pos = permission.indexOf('.');
        if (pos == -1) {
            throw new IllegalArgumentException("Illegal permission value '" + permission + "'");
        }

		//  try per-user permissions...
		Map<String, Permission> userPermissions = userPermissionCache.get(user);
		if (userPermissions == null) {
			// @TODO - load from DB ? should be handled by MRUCache
			throw new IllegalStateException("Unknown user '" + user.getUsername() + "'");
		} else {
			Permission userPermission = (Permission) userPermissions.get(permission);
			if (userPermission!=null) {
				if (context == null) {
					return true;
				} else {
					ResourceCriteria criteria = userPermission.getResourceCriteria();
					if (criteria == null || criteria.evaluate(context)) {
						return true;
					}
				}
			}
		}
		
		// then try per-role permissions ...
		List<String> roles = userRoleCache.get(user);
		if (roles == null) {
			// @TODO - load from DB ? should be handled by MRUCache
			throw new IllegalStateException("Unknown user '" + user.getUsername() + "'");
		} else {
			for (Iterator<String> i = roles.iterator(); i.hasNext(); ) {
				String rolename = (String) i.next();
				Map<String, Permission> rolePermissions = rolePermissionCache.get(rolename);
				if (rolePermissions == null) {
					// @TODO - load from DB ? should be handled by MRUCache
					throw new IllegalStateException("Unknown role '" + rolename + "'");
				} else {
					// this is a strange data structure
					Permission rolePermission = (Permission) rolePermissions.get(permission);
					if (rolePermission!=null) {
						if (context == null) {
							return true;
						} else {
							ResourceCriteria criteria = rolePermission.getResourceCriteria();
							if (criteria == null || criteria.evaluate(context)) {
								return true;
							}
						}	
					}
				}
			}
		}

        return false;
    }

	/**
	 * Returns the Permission object for a specific user/permission combination, or null
	 * if this permission is not granted. This method will not search the user's 
	 * role-based permissions. 
	 *
	 * @param user The user we are determining
	 * @param permission The permission we are testing for. Permissions are expressed in
	 *   'activity.resourceType' format.
	 *
	 * @return a permission object. 
	 *
	 * @throws NullPointerException if either parameter to this method is null
	 * @throws IllegalArgumentException if the permission supplied is formatted incorrectly.
	 */
	public Permission getPermission(User user, String permission) {
		// @TODO should get this User out of our userCache, keyed by id
		if (permission == null) { throw new NullPointerException("Null permission"); }
		if (user == null) { throw new NullPointerException("Null user"); }

		int pos = permission.indexOf('.');
		if (pos == -1) {
			throw new IllegalArgumentException("Illegal permission value '" + permission + "'");
		}

		//  try per-user permissions...
		Map<String, Permission> userPermissions = userPermissionCache.get(user);
		if (userPermissions == null) {
			// @TODO - load from DB ? should be handled by MRUCache
			throw new IllegalStateException("Unknown user '" + user.getUsername() + "'");
		} else {
			Permission userPermission = (Permission) userPermissions.get(permission);
			if (userPermission != null) {
				return userPermission;
			}
		}

		return null;
	}
    
    
    /**
     * Returns a list of all Permission objects assigned to a user and all the 
     * roles that the user is a member of. This allows multiple permission conditions
     * to be applied to a user, one for each role.
     *
     * @param user The user we are determining
     * @param permission The permission we are testing for. Permissions are expressed in
     *   'activity.resourceType' format.
     *
     * @return a List of Permission objects, or an empty list if the user 
     *   (and none of their roles) contains this permission
     *
     * @throws NullPointerException if either parameter to this method is null
     * @throws IllegalArgumentException if the permission supplied is formatted incorrectly.
     */
    public List<Permission> getPermissions(User user, String permission) {
    	// @TODO should get this User out of our userCache, keyed by id
        if (permission == null) { throw new NullPointerException("Null permission"); }
        if (user == null) { throw new NullPointerException("Null user"); }
        List<Permission> result = new ArrayList<Permission>();

        int pos = permission.indexOf('.');
        if (pos == -1) {
            throw new IllegalArgumentException("Illegal permission value '" + permission + "'");
        }

		//  try per-user permissions...
		Map<String, Permission> userPermissions = userPermissionCache.get(user);
		if (userPermissions == null) {
			// @TODO - load from DB ? should be handled by MRUCache
			throw new IllegalStateException("Unknown user '" + user.getUsername() + "'");
		} else {
			Permission userPermission = (Permission) userPermissions.get(permission);
			if (userPermission != null) {
				result.add(userPermission);
			}
		}
		
		// then try per-role permissions ...
		List<String> roles = userRoleCache.get(user);
		if (roles == null) {
			throw new IllegalStateException("Unknown user '" + user.getUsername() + "'");
		} else {
			for (Iterator<String> i = roles.iterator(); i.hasNext(); ) {
				String rolename = (String) i.next();
				Map<String, Permission> rolePermissions = rolePermissionCache.get(rolename);
				if (rolePermissions == null) {
					throw new IllegalStateException("Unknown role '" + rolename + "'");
				} else {
					Permission rolePermission = (Permission) rolePermissions.get(permission);
					if (rolePermission!=null) {
						result.add(rolePermission);
					}
				}
			}
		}
		return result;
    }
    


    /** Returns a string representation of this security context.
     *
     * @return a string representation of this security context.
     */
    public String toString() {
        return super.toString() + (rolePermissionCache == null ? " - uninitialised" : ": " + rolePermissionCache.toString());
    }

    /** Clear all caches and re-initialises this security context (as defined 
     * in this instance's initial initialisation properties). 
     * This method also resets this security context's loader.
     */
    @SuppressWarnings("unchecked")
	public void resetSecurityContext() {
        // logger.debug("Security context properties: " + properties.toString());
    	
		int cacheSize = Integer.MAX_VALUE;
		int cacheExpiry = Integer.MAX_VALUE;
        if (properties.get(INIT_USER_CACHE_SIZE) != null && properties.get(INIT_USER_CACHE_EXPIRY) != null) {
            cacheSize = Integer.parseInt((String) properties.get(INIT_USER_CACHE_SIZE));
            cacheExpiry = Integer.parseInt((String) properties.get(INIT_USER_CACHE_EXPIRY));
        }

        UserPermissionCallback userPermissionCallback = new UserPermissionCallback(this.securityLoader);
        userPermissionCache = new MRUCache(cacheSize, cacheExpiry, userPermissionCallback );

		UserRoleCallback userRoleCallback = new UserRoleCallback (this.securityLoader);
		userRoleCache = new MRUCache(cacheSize, cacheExpiry, userRoleCallback );

		RolePermissionCallback rolePermissionCallback = new RolePermissionCallback (this.securityLoader);
		rolePermissionCache = new MRUCache(cacheSize, cacheExpiry, rolePermissionCallback );

		UserCallback userCallback = new UserCallback (this.securityLoader);
		userCache = new MRUCache(cacheSize, cacheExpiry, userCallback );

		try {
			securityLoader.resetSecurityContext();	
		} catch (IOException ioe) {
			throw (IllegalArgumentException) new IllegalArgumentException(
			  "Cannot initialise security Context").initCause(ioe);
		}
    }

    /**
     * Authenticate the supplied username and password with the authentication provider.
     * Returns true if the username/password combination is valid, false otherwise
     *
     * <p>Some authentication providers may require more complex handshakes (e.g. TFA authentication)
     * which are currently suported by setting flags in a subclassed User object. 
     * Possible mangling the password parameter as well. See the securityAuthenticator
     * documentation for details.
     *
     * <p>The User object passed to this method may not have a valid userId assigned to it (this 
     * may be set by the authentication provider).
     *
     * @param user user 
     * @param password password
     *
     * @return true if the username/password combination is valid, false otherwise
     *
     * @throws IOException an exception occurred accessing the authentication provider.
     */
    public boolean authenticate(User user, String password)
        throws IOException {
    	// @TODO should get this User out of our userCache, keyed by id
        return securityAuthenticator.authenticate(user, password);
    }

    /** Returns a User, given their userId
     * 
     * <p>This method will not load role or permissions data for the user. 
     * 
     * @param userId
     * @return
     */
    public User getUser(long userId) {
    	// this cache has different User objects than the User objects in the role/user permission caches, 
		User user = (User) userCache.get(userId);
		return user;
    }

    
	// why are these methods public ?
    public List<Permission> loadRolePermissions(String role)
        throws IOException {
        return securityLoader.loadRolePermissions(role);
    }

    public List<Permission> loadUserRolePermissions(User user)
        throws IOException {
        return securityLoader.loadUserRolePermissions(user);
    }


}
