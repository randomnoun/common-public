package com.randomnoun.common.security.impl;

/* (c) 2013 randomnoun. All Rights Reserved. This work is licensed under a
 * BSD Simplified License. (http://www.randomnoun.com/bsd-simplified.html)
 */

import java.io.IOException;
import java.util.*;

import org.apache.log4j.Logger;

import com.randomnoun.common.security.Permission;
import com.randomnoun.common.security.SecurityLoader;
import com.randomnoun.common.security.User;
import com.randomnoun.common.security.impl.NullSecurityLoaderImpl;

/**
 * An implementation of the {@link com.randomnoun.common.security.SecurityLoader}
 * class, that does absolutely nothing.
 * 
 * <p>All methods do nothing or return empty lists, 
 * and searches for user or role IDs return -1.
 * 
 * <attributes>
 * </attributes>
 * 
 * 
 * @author knoxg
 */ 
public class NullSecurityLoaderImpl implements SecurityLoader
{
    
	/** Logger for this class */
	public static final Logger logger = Logger.getLogger(NullSecurityLoaderImpl.class);

	/** Initialise this loader.
	 *
	 * @see com.randomnoun.common.security.SecurityLoader#initialise(java.util.Map)
	 */
	public void initialise(Map<String, Object> properties) {
	}

	public List<Permission> loadAllRolePermissions() throws IOException {
		return Collections.emptyList();
	}

	public List<Permission> loadRolePermissions(String role) throws IOException {
		return Collections.emptyList();
	}

	public List<Permission> loadUserRolePermissions(User user) throws IOException {
		return Collections.emptyList();	}

	public List<Permission> loadUserPermissions(User user) throws IOException {
		return Collections.emptyList();	}

	public List<String> loadUserRoles(User user) throws IOException {
		return Collections.emptyList();	
	}

	public User loadUser(long userId) {
		return null;
	}
	
	public List<User> loadAllUsers() throws IOException  {
		return Collections.emptyList();	
	}

	public List<String> loadAllResources() throws IOException {
		return null;
	}

	public List<String> loadAllActivities(String resourceName) throws IOException {
		return Collections.emptyList();
	}

	public List<String> loadAllRoles() throws IOException {
		return Collections.emptyList();
	}

	public List<Permission> loadAllPermissions() throws IOException {
		return Collections.emptyList();
	}

	public List<Map<String, Object>> loadAllRoleDetails() throws IOException {
		return Collections.emptyList();
	}

	public List<Map<String, Object>> loadAllUserDetails() throws IOException {
		return Collections.emptyList();
	}

	public void resetSecurityContext() throws IOException {
		
	}

	public void saveUserRolesAndPermissions(User user, List<String> roles, List<Permission> userPermissions)
		throws IOException {
		throw new UnsupportedOperationException("not implemented");
	}

	public void saveRolePermissions(String role, List<Permission> rolePermissions)
			throws IOException {
		throw new UnsupportedOperationException("not implemented");
	}

	
}

