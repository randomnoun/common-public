package com.randomnoun.common.security.impl;

/* (c) 2013 randomnoun. All Rights Reserved. This work is licensed under a
 * BSD Simplified License. (http://www.randomnoun.com/bsd-simplified.html)
 */

import java.io.IOException;
import java.util.*;

import org.apache.log4j.Logger;
import org.springframework.jdbc.core.*;

import com.randomnoun.common.Text;
import com.randomnoun.common.security.Permission;
import com.randomnoun.common.security.SecurityAuthenticator;
import com.randomnoun.common.security.User;
import com.randomnoun.common.security.impl.SpringSecurityAuthenticatorImpl;

// ok. the USERID column in the table is actually a string. 
// presumably the actual User.userId is sourced from some other column.

// this implementation appears to favour querying on the USERID String,
// so bear that in mind as well. It also doesn't populate the actual User.userId,
// so don't use that at all.

/**
 * See SpringSecurityLoaderImpl. Maybe.
 *
 * 
 * @author knoxg
 */
public class SpringSecurityAuthenticatorImpl
	implements SecurityAuthenticator
{
	
	/** Logger for this class */
	public static final Logger logger = Logger.getLogger(SpringSecurityAuthenticatorImpl.class);

	/** Properties used in this loader */
	private Map<String, Object> properties = null;
    
	public static String INIT_TABLENAME = "tableName";
	public static String INIT_USERNAME_COLUMN = "usernameColumn";
	public static String INIT_PASSWORD_COLUMN = "passwordColumn";
	
	/** Odd. */
	public static String INIT_JDBCTEMPLATE = "jdbcTemplate";
	
	String tableName;
	String usernameColumn;
	String passwordColumn;
	
	/** Initialise this loader.
	 *
	 * @see com.randomnoun.common.security.SecurityLoader#initialise(java.util.Map)
	 */
	public void initialise(Map<String, Object> properties)
	{
		this.properties = properties;
		if (properties.get(INIT_JDBCTEMPLATE)==null) { throw new NullPointerException("null INIT_JDBCTEMPLATE"); }
        
		tableName = Text.strDefault((String) properties.get(INIT_TABLENAME), "users");
		usernameColumn = Text.strDefault((String) properties.get(INIT_USERNAME_COLUMN), "userId");
		passwordColumn = Text.strDefault((String) properties.get(INIT_PASSWORD_COLUMN), "password");
	}

	/** Authenticates a user. 
	 * 
	 */
	public boolean authenticate(User user, String password)
	{
		// check that the password is not empty
		if (password == null || password.equals("")) {	
			return false;
		}
		JdbcTemplate jt = (JdbcTemplate)properties.get("jdbcTemplate");
		String sql = 
		  "SELECT " + passwordColumn  +
		  " FROM " + tableName +  
		  " WHERE " + usernameColumn + " = ?";
		List<Object> sqlParams = new ArrayList<Object>(2);
		sqlParams.add(user.getUsername());
		boolean authenticated = false;  
		try {           
			// TODO hash this, then perform the comparison in a stored procedure, 
			// then move it all to LDAP or OpenID
			String databasePassword = (String) jt.queryForObject(sql, sqlParams.toArray(), String.class);
			authenticated = password.equals(databasePassword);
		} catch (Exception ex) {
			logger.error("Error authenticating user", ex);
			authenticated = false;   
		}
		return authenticated;
	}
		
	
	/** Resets the security context. 
	 * 
	 * <p>This security context holds no state, so this method does nothing.
	 */
	public void resetSecurityContext() {
		// no action necessary
	}

	public void saveUserRolesAndPermissions(User user, List<String> roles, List<Permission> userPermissions) throws IOException 
	{
		throw new UnsupportedOperationException("not implemented");
	}

	public void saveRolePermissions(String role, List<Permission> rolePermissions)
		throws IOException 
	{
		throw new UnsupportedOperationException("not implemented");
	}

}

