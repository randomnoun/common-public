package com.randomnoun.common.security.impl;

/* (c) 2013 randomnoun. All Rights Reserved. This work is licensed under a
 * BSD Simplified License. (http://www.randomnoun.com/bsd-simplified.html)
 */

import java.io.IOException;
import java.util.*;

import org.apache.log4j.Logger;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.*;

import com.randomnoun.common.CamelCaser;
import com.randomnoun.common.Struct;
import com.randomnoun.common.Text;
import com.randomnoun.common.jexl.sql.SqlGenerator;
import com.randomnoun.common.security.Permission;
import com.randomnoun.common.security.SecurityContext;
import com.randomnoun.common.security.SecurityLoader;
import com.randomnoun.common.security.User;
import com.randomnoun.common.security.impl.ResourceCriteriaImpl;
import com.randomnoun.common.security.impl.SpringSecurityLoaderImpl;
import com.randomnoun.common.spring.StringRowMapper;


/**
 * An implementation of the {@link com.randomnoun.common.security.SecurityLoader}
 * class, using the Spring framework to populate the SecurityContext from a JDBC
 * datasource.
 * 
 * <p>This security context used to optionally take a customerId (used to partition
 * users across separate SaaS contexts), or an applicationId (used to partition
 * permissions across separate software products). If not defined, then these 
 * columns did not need to be present in the database. This functionality
 * has been deprecated.
 * 
 * <p>I think we had String userids at one point as well, so look out for that.
 *
 * <p>This class has a number of initialisation properties that are specific
 * to this class (in addition to those initialisation properties that are
 * set by the SecurityContext itself):
 *
 * <ul>
 * <li> {@link #INIT_JDBCTEMPLATE} - The Spring JdbcTemplate class used to retrieve
 *   information from a database.
 * <li> {@link #INIT_DATABASE_VENDOR} - Set to one of the SqlGenerator.DATABASE_* constants,
 *   which specifies what syntax of SQL to use (DB2, Oracle or SqlServer).
 * </ul>
 * 
 * @author knoxg
 */
public class SpringSecurityLoaderImpl
	implements SecurityLoader
{
	
	/** Logger for this class */
	public static final Logger logger = Logger.getLogger(SpringSecurityLoaderImpl.class);

	/** Properties used in this loader */
	private Map<String, Object> properties = null;
    
	/** Used as parameter to {@link #convertPermissionList(List, int)} method */
	private static final int PERMISSION_USER = 1;
	/** Used as parameter to {@link #convertPermissionList(List, int)} method */
	private static final int PERMISSION_ROLE = 2;
	/** Used as parameter to {@link #convertPermissionList(List, int)} method */
	private static final int PERMISSION_NONE = 3;
    
	/** Initialisation property key to set JdbcTemplate. */
	public final static String INIT_JDBCTEMPLATE = "jdbcTemplate";

	/* * Initialisation property key to set the username mask. */
	// public final static String INIT_USERNAME_MASK = "usernameMask";

	/** Database vendor for generated SQL. Should be one of the SqlGenerator.DB_* constants */
	public final static String INIT_DATABASE_VENDOR = "databaseVendor";
	
	/** Boolean object version of SecurityContext.INIT_CASE_INSENSITIVE string; set to
	 * false if missing */
	public final static String INIT_CASE_INSENSITIVE_OBJ = "caseInsensitiveObj";
    
	/** Column renamer for the shouty ROLETABLE table */
	public final CamelCaser roleCamelCaser = new CamelCaser("roleId,roleName,description");
    
	/** Column renamer for the USERS table */
	public final CamelCaser userCamelCaser = new CamelCaser("userId,name");
    
	/** Column renamer for the PERMISSION table */
	public final CamelCaser permissionCamelCaser = new CamelCaser("roleName,userId,activityName,resourceName,resourceCriteria");
    
	/** Column renamer for the SECURITYAUDITS table */    
	public final CamelCaser auditCamelCaser = new CamelCaser("userId,auditTime,auditDescription,resourceCriteria,authorised,authoriserId,authoriseTime");
    

	// we used to have staging tables for changes to the security model, which then
	// had to be approved before they were transferred to the real security model. Remember that ? 
	// 'four-eyes' security anyone ? 
	
	/** Return the name of the USERS table that this security loader will retrieve data from */
	private String userTable() {
		return "users";
	}
    
	/** Return the name of the USERROLE table that this security loader will retrieve data from */
	private String userRoleTable() {
		return "userRole";
	}

	/** Return the name of the ROLETABLE table that this security loader will retrieve data from */
	private String roleTable() {
		return "roleTable";
	}

	/** Return the name of the SECURITYTABLE table that this security loader will retrieve data from */
	private String securityTable() {
		return "securityTable";
	}

	/*
	private String roleTableSequence() {
		return "SEQ_ROLETABLE";
	}
	
    
	private boolean isAuditEnabled() {
		Boolean enabled = (Boolean) properties.get(INIT_AUDIT_ENABLED);
		if (enabled==null) {
			return Boolean.TRUE;
		}
		return enabled;
	}
	
	/ * * Return the username to be used to audit any security operations performed by this security loader  * /
	private String auditUsername() {
		String auditUser = (String) properties.get(INIT_AUDIT_USERNAME);
		if (auditUser==null) {
			throw new IllegalStateException("Audit user required for all mutable security context operations");            
		}
		return auditUser;
	}
	*/

	/** Retrieve the database vendor from the security loaders properties map.
	 * @return the database vendor */
	private String getDatabaseVendor() {
		return (String) properties.get(INIT_DATABASE_VENDOR);
	}
	
	
	/** Returns true if this security context is case-insensitive, false otherwise
	 * 
	 * @return true if this security context is case-insensitive, false otherwise
	 */
	private boolean getCaseInsensitive() {
		// @TODO: cache this
		return ((Boolean) properties.get(INIT_CASE_INSENSITIVE_OBJ)).booleanValue();
	}
	
	/** Return the sql for a string comparison for a username field, taking
	 * case sensitivity into account.
	 * 
	 * @param lhs expression to lowercase (if required) 
	 *
	 * @return
	 */
	private String lowerSql(String lhs) {
		if (getCaseInsensitive()) {
			String vendor = getDatabaseVendor();
			if (vendor.equals(SqlGenerator.DATABASE_DB2)) {
				return "LOWER(" + lhs + ")";
			} else if (vendor.equals(SqlGenerator.DATABASE_ORACLE)) {
				return "LOWER(" + lhs + ")";
			} else if (vendor.equals(SqlGenerator.DATABASE_SQLSERVER)) {
				return "LOWER(" + lhs + ")";
			} else if (vendor.equals(SqlGenerator.DATABASE_MYSQL)) {
				return "LOWER(" + lhs + ")";
			} else {
				throw new IllegalStateException("Case-insensitivity security contexts" +
				  " not supported for database type '" + vendor + "'");
			}
		} else {
			return lhs;
		}
	}
	
	/** Converts a value to lowercase, but only if case insensitivity is required */
	private String lower(String value) {
		if (getCaseInsensitive()) {
			return value.toLowerCase();
		} else {
			return value;
		}
	}
	
	/** Converts a long into an object suitable to be passed to the database
	 * as a roleId or permissionId. 
	 * 
	 * @param var object to convert
	 * 
	 * @return a Long representation of the object (or Integer for JET DBs)
	 */
	private Object toSequenceType(long var) {
		String vendor = getDatabaseVendor();
		if (vendor.equals(SqlGenerator.DATABASE_JET)) {
			return new Integer((int) var);
		} else {
			return new Long(var);
		}
	}
    
	/** Initialise this loader.
	 *
	 * <p>The properties Map passed into this method must contain the following
	 * attributes.
	 *
	 * <attributes>
	 *   jdbcTemplate - a JdbcTemplate object connected to a datasource
	 *   databaseVendor - one of the SqlGenerator.DATABASE_* constants, defining what 
	 *     syntax of SQL to generate from this class.
	 * </attributes>
	 *
	 * @see com.randomnoun.common.security.SecurityLoader#initialise(java.util.Map)
	 */
	public void initialise(Map<String, Object> properties)
	{
		this.properties = properties;
		//String tableSuffix = (String) properties.get(INIT_TABLE_SUFFIX);
		//if (tableSuffix == null) { throw new NullPointerException("null INIT_TABLE_SUFFIX"); }
		if (properties.get(INIT_JDBCTEMPLATE)==null) { throw new NullPointerException("null INIT_JDBCTEMPLATE"); }
		if (properties.get(INIT_DATABASE_VENDOR)==null) { throw new NullPointerException("null INIT_DATABASE_VENDOR"); }
		String vendor = (String) properties.get(INIT_DATABASE_VENDOR);
		if (!(vendor.equals(SqlGenerator.DATABASE_DB2) ||
		  vendor.equals(SqlGenerator.DATABASE_ORACLE) ||
		  vendor.equals(SqlGenerator.DATABASE_SQLSERVER) ||
		  vendor.equals(SqlGenerator.DATABASE_MYSQL) ||
		  vendor.equals(SqlGenerator.DATABASE_JET))) {
			throw new IllegalArgumentException("Invalid INIT_DATABASE_VENDOR property '" + vendor + "'");
		}

		// '' should only be allowed when staging contexts are disabled
		/*
		if (!(tableSuffix.equals("_WORK") || tableSuffix.equals("_LIVE") ||
			tableSuffix.equals("")))
		{
			throw new IllegalArgumentException("INIT_TABLE_SUFFIX must be set to '', '_WORK' or '_LIVE'");
		}
		*/
        
		String caseInsensitive = (String) properties.get(SecurityContext.INIT_CASE_INSENSITIVE);
		if (Text.isBlank(caseInsensitive)) {
			caseInsensitive = "false";
		}
		properties.put(INIT_CASE_INSENSITIVE_OBJ, Boolean.valueOf(caseInsensitive));
		
		/*
		it's still OK to call read-only methods on the staging security context without an audit user  
         
		if (tableSuffix.equals("_WORK") && properties.get(INIT_AUDIT_USERNAME)==null) {
			throw new IllegalArgumentException(
			  "Must include audit username when operating on staging security context");
		}*/
        
	}

	/** Load all role permissions.
	 *
	 * {@inheritDoc}
	 *
	 * @return A List of Permission objects for all roles in the current application. Does
	 * not return Permissions that are not explicitly associated with a role in the security table.
	 *
	 * @throws IOException if an error occured loading from the database. This IOException
	 *   will always contain a spring DataAccessException which can be accessed in its .getCause()
	 *   method.
	 *
	 * @see com.randomnoun.common.security.SecurityLoader#loadAllRolePermissions()
	 */
	public List<Permission> loadAllRolePermissions()
		throws IOException
	{
		JdbcTemplate jt = (JdbcTemplate)properties.get("jdbcTemplate");
		String loadAllRolePermissionsSql = 
		  "SELECT roleName, activityName, resourceName, resourceCriteria " +
		  "FROM " + roleTable() + ", " + securityTable() + ", permission, resources " + 
		  "WHERE " +
		  " (" + securityTable() + ".roleId = " + roleTable() + ".roleId) " +
		  " AND " + "(" + securityTable() + ".permissionId = permission.permissionId) " +
		  " AND " + "(permission.resourceId = resources.resourceId)";
		List<Object> sqlParams = new ArrayList<Object>(2);  
		List<Map<String, Object>> list = jt.query(loadAllRolePermissionsSql,
		  sqlParams.toArray(),
		  new ColumnMapRowMapper());
		if (logger.isDebugEnabled()) {
			logger.debug(Struct.structuredListToString("loadAllRolePermissions", list));
		}
		permissionCamelCaser.renameList(list);
		return convertPermissionList(list, PERMISSION_ROLE);
	}

	/** Retrieve per-user permission objects.
	 *
	 * {@inheritDoc}
	 *
	 * @throws IOException if an error occured loading from the database. This IOException
	 *   will always contain a spring DataAccessException which can be accessed in its .getCause()
	 *   method.
	 *
	 * @see com.randomnoun.common.security.SecurityLoader#loadUserPermission()
	 */
	public List<Permission> loadUserPermissions(User user)
		throws IOException
	{
		JdbcTemplate jt = (JdbcTemplate) properties.get("jdbcTemplate");
        
		String sqlPermissions = 
		  "SELECT activityName, resourceName, resourceCriteria " +
		  "FROM " + securityTable() + ", permission, resources " + 
		  "WHERE " +
		  " " + securityTable() + ".roleId IS NULL " +
		  " AND (" + lowerSql(securityTable() + ".userId") + " = ?) " +
		  " AND (" + securityTable() + ".permissionId = permission.permissionId) " +
		  " AND (permission.resourceId = resources.resourceId) ORDER BY resourceName, activityName";
		List<Object> sqlParams = new ArrayList<Object>(2);  
		sqlParams.add(lower(user.getUsername()));
		List<Map<String, Object>> list = jt.query(sqlPermissions, sqlParams.toArray(), new ColumnMapRowMapper());
		
		if (logger.isDebugEnabled()) {
		  logger.debug(Struct.structuredListToString("loadAllUserPermissions [2]", list));
		}
		permissionCamelCaser.renameList(list);
		return convertPermissionList(list, PERMISSION_USER);                
	}
    

	/** Retrieve a list of roles applied to a particular user
	 *  for the current application context.
	 *
	 * @return List of roles, represented as Strings
	 *
	 * @see com.randomnoun.common.security.SecurityLoader#loadRolesForUser()
	 */
	public List<String> loadUserRoles(User user)
	{
		JdbcTemplate jt = (JdbcTemplate)properties.get("jdbcTemplate");
		String sql = "SELECT roleName " + 
		  " FROM " + roleTable() + "," + userRoleTable() + 		  
		  " WHERE " +
		  roleTable() + ".roleId = " + userRoleTable() + ".roleId " +
		  " AND " + lowerSql(userRoleTable() + ".userId") + " = ?";
		logger.debug("loadRolesForUser: " + sql + "; " + user.getUsername());
		List<Object> sqlParams = new ArrayList<Object>(3);
		sqlParams.add(lower(user.getUsername()));		  
		List<String> list = jt.query(sql, sqlParams.toArray(), new StringRowMapper());
		return list;
	}


	/**
	 * Return List of Permission associated with a particular role
	 * 
	 * @param role
	 * @return A List of Permission objects that apply to that role
	 */    
	public List<Permission> loadRolePermissions(String role)       
	{
		JdbcTemplate jt = (JdbcTemplate)properties.get("jdbcTemplate");
		long roleId = findRoleIdByName(role);
		String sql = 
		  "SELECT roleName, activityName, resourceName, resourceCriteria " +
		  "FROM " + roleTable() + ", " + securityTable() + ", permission, resources " + 
		  "WHERE " + 
		  " (" + securityTable() + ".roleId = ?) " +
		  " AND (" + roleTable() + ".roleId = " + securityTable() + ".roleId) " +
		  " AND (" + securityTable() + ".permissionId = permission.permissionId) " +
		  " AND (permission.resourceId = resources.resourceId) " +
		  " ORDER BY resourceName, activityName";
		List<Object> sqlParams = new ArrayList<Object>(2);
		sqlParams.add(toSequenceType(roleId));
		List<Map<String, Object>> list = jt.query(sql, sqlParams.toArray(), new ColumnMapRowMapper());
		permissionCamelCaser.renameList(list);
		return convertPermissionList(list, PERMISSION_ROLE);
	}

	/**
	 * Return List of Permissions containing permissions contained for all roles
	 * for a particular user.
	 * 
	 * <p>Note that if a user contains multiple roles that have permissions that apply to
	 * the same activity/resource combinations, then that will be reflected
	 * in the returned list. 
	 * 
	 * @param userid Name of user we are interested in.
	 * @return A List of Maps 
	 */
	public List<Permission> loadUserRolePermissions(User user)       
	{                    
		JdbcTemplate jt = (JdbcTemplate) properties.get("jdbcTemplate");
		String sql ="SELECT activityName, resourceName, resourceCriteria " +
		  "FROM " + securityTable() + ", " + roleTable() + ", permission, resources " + 
		  "WHERE (" + securityTable() + ".roleId IN (" +
			"SELECT " + userRoleTable() + ".roleId " +
			"FROM " + userRoleTable() + ", " + roleTable() + 
			" WHERE " + userRoleTable() + ".roleId = " + roleTable() + ".roleId " +
			" AND " + lowerSql("userId") + " = ?)) " +
			" AND (" + securityTable() + ".permissionId = permission.permissionId) " +
			" AND (permission.resourceId = resources.resourceId)";
		List<Object> sqlParams = new ArrayList<Object>(2);
		sqlParams.add(user.getUsername());
		List<Map<String, Object>> list = jt.query(sql, sqlParams.toArray(), new ColumnMapRowMapper());
                
		// take out duplicates
		// Can't use DISTINCT on queries that contain CLOBs. Goddamn oracle.
		ArrayList<Map<String, Object>> list2 = new ArrayList<Map<String, Object>>();
		for (int x = 0; x < list.size(); x++) {
			Map<String, Object> row = list.get(x);
			if (!list2.contains(row)) {
				list2.add(row);   
			}
		}
		permissionCamelCaser.renameList(list2);
		return convertPermissionList(list2, PERMISSION_NONE); // role perm here ?
		          
	}


	/**
	 * Return a List of all Permissions available to this application
	 * 
	 * @return a List of Permissions available to this application
	 */    
	public List<Permission> loadAllPermissions()       
	{
		JdbcTemplate jt = (JdbcTemplate)properties.get("jdbcTemplate");
		String sql =
		  "SELECT activityName, resourceName" +
		  " FROM permission, resources " + 
		  " WHERE " +
		  " permission.resourceId = resources.resourceId " +
		  "ORDER BY resourceName, activityName";
		List<Object> sqlParams = new ArrayList<Object>(2);
		List<Map<String, Object>> list = jt.query(sql, sqlParams.toArray(), new ColumnMapRowMapper());
		permissionCamelCaser.renameList(list);
		return convertPermissionList(list, PERMISSION_NONE);
	}
    


	/** Private method to convert a list of PERMISSION rows (as returned by Spring)
	 *  into Permission Objects.
	 *
	 * <p>On reflection, probably should have used a Spring RowMapper for this. Oh well. It works.
	 *
	 * @param list  The list to convert
	 * @return  A List of Permission objects
	 */
	private List<Permission> convertPermissionList(List<Map<String, Object>> list, int permissionType)
	{
		String resourceType;
		String expressionString;
		Permission permission;
		List<Permission> result = new ArrayList<Permission>(list.size());
        
		for (Iterator<Map<String, Object>> i = list.iterator(); i.hasNext(); ) {
			Map<String, Object> map = i.next();
			resourceType = (String) map.get("resourceName");
			expressionString = (String) map.get("resourceCriteria");
			ResourceCriteriaImpl resourceCriteriaImpl = null;
			if (expressionString != null && !expressionString.equals("")) {
				try {
					resourceCriteriaImpl = new ResourceCriteriaImpl(expressionString);
				} catch (Exception ce) {
					throw new DataIntegrityViolationException(
						"Invalid criteria found in SECURITY.RESOURCECRITERIA: '" +
						expressionString + "'", ce);
				}
			}
			
			if (permissionType == PERMISSION_ROLE) {
				permission = new Permission((String)map.get("roleName"),
				  (String)map.get("activityName"), resourceType, resourceCriteriaImpl);
			} else if (permissionType == PERMISSION_USER) {
				User user = new User();
				user.setUsername((String) map.get("userId"));
				permission = new Permission( user,
				  (String)map.get("activityName"), resourceType, resourceCriteriaImpl);
			} else if (permissionType == PERMISSION_NONE) {
				permission = new Permission((String)map.get("activityName"), resourceType);
				
			} else {
				throw new IllegalArgumentException("Unknown permission type '" + permissionType + "'");
			}
			result.add(permission);
		}
		return result;
	}

	// if you want to do this, subclass it
	public User loadUser(long userId) throws IOException {
		throw new IOException("loadUser() not implemented");
		
		/*
		// tempted to go through all these methods and change userId to an actual number rather than a varchar.
		// sounds fair to me.
		
		JdbcTemplate jt = (JdbcTemplate) properties.get("jdbcTemplate");
		String sql = 
		  "SELECT " + userTable() + ".userId " + // so userId's a string is it ? terrific.
		  " FROM " + userTable();
		List<String> list = jt.query(sql, new StringRowMapper());
		List<User> result = new ArrayList<User>(list.size());
		for (Iterator i = list.iterator(); i.hasNext(); ) {
			String username = (String) i.next();
			User user = new User();
			user.setUsername(username);  // this is horrible, but it's consistent with what's in here at the moment.
			result.add(user);
		}
		return result;
		*/
		
	}
	
	/** Retrieve a list of users in the current application context.
	 *
	 * {@inheritDoc}
	 *
	 * @throws IOException if an error occured loading from the database. This IOException
	 *   will always contain a spring DataAccessException which can be accessed via
	 *   {@link java.io.Throwable#getCause} method.
	 *
	 * @see com.randomnoun.common.security.SecurityLoader#loadAllUsers()
	 */
	public List<User> loadAllUsers()
		throws IOException
	{
		JdbcTemplate jt = (JdbcTemplate)properties.get("jdbcTemplate");
		String sql = 
		  "SELECT " + userTable() + ".userId " + 
		  " FROM " + userTable();
		List<String> list = jt.query(sql, new StringRowMapper());
		List<User> result = new ArrayList<User>(list.size());
		for (Iterator<String> i = list.iterator(); i.hasNext(); ) {
			String username = (String) i.next();
			User user = new User();
			user.setUsername(username);
			result.add(user);
		}
		return result;
	}

	/** Retrieve a list of all the resources under security for the current application context.
	 *
	 * @return List of maps, each map represents resource
	 *
	 * @see com.randomnoun.common.security.SecurityLoader#loadAllResources()
	 */
	public List<String> loadAllResources() {
		JdbcTemplate jt = (JdbcTemplate)properties.get("jdbcTemplate");
		String sql =
		  "SELECT resourceName " + 
		  "FROM resources ";
		List<Object> sqlParams = new ArrayList<Object>(2);
		List<String> list = jt.query(sql, sqlParams.toArray(), new StringRowMapper());
		return list;
	}


	/** Retrieve a list of all the activities that can be applied to resource
	 *  for the current application context.
	 *
	 * @return List of maps, each map represents an activity.
	 *
	 * @see com.randomnoun.common.security.SecurityLoader#loadAllActivities()
	 */
	public List<String> loadAllActivities(String resourceName)
	{
		JdbcTemplate jt = (JdbcTemplate)properties.get("jdbcTemplate");
		String sql =
		  "SELECT activityName " + 
		  "FROM permission, resources " +
		  "WHERE " +
		  " resourceName = ? " +
		  " AND permission.resourceId = resources.resourceId ";
		List<Object> sqlParams = new ArrayList<Object>(3);
		sqlParams.add(resourceName);
		List<String> list = jt.query(sql, sqlParams.toArray(), new StringRowMapper());
		return list;
	}

	/** Retrieve a list of all the roles that can be applied to a user
	 *  for the current application context.
	 *
	 * @return List of maps, each map represents a role.
	 *
	 * @see com.randomnoun.common.security.SecurityLoader#loadAllRoles()
	 */
	public List<String> loadAllRoles()
	{
		JdbcTemplate jt = (JdbcTemplate)properties.get("jdbcTemplate");
		String sql = "SELECT roleName " +
		  " FROM " + roleTable();
		List<Object> sqlParams = new ArrayList<Object>(2);
		List<String> list = jt.query(sql, sqlParams.toArray(), new StringRowMapper());
		return list;
	}


	/**
	 * Returns List of maps, where each map represents the details of a particular role.
	 */
	public List<Map<String, Object>> loadAllRoleDetails()
	{
		logger.debug("SpringSecurityLoaderImpl.loadAllRoleDetails(): 1. Entering");
		JdbcTemplate jt = (JdbcTemplate) properties.get("jdbcTemplate");
		String sql = 
		  "SELECT roleId, roleName, description" +
		  " FROM " + roleTable(); 
		List<Object> sqlParams = new ArrayList<Object>(2);
		List<Map<String,Object>> list = jt.queryForList(sql, sqlParams.toArray());
		roleCamelCaser.renameList(list);
		return list;
	}
    
	/**
	 * Returns List of maps, where each map represents the details of a particular user.
	 */
	public List<Map<String, Object>> loadAllUserDetails()
	{
		JdbcTemplate jt = (JdbcTemplate) properties.get("jdbcTemplate");
		String sql = "SELECT userId, name " +
		  " FROM " + userTable(); 
		List<Object> sqlParams = new ArrayList<Object>(1);
		List<Map<String,Object>> list = jt.queryForList(sql, sqlParams.toArray());
		userCamelCaser.renameList(list);
		return list;
	}

	/**
	 * Returns the ROLEID of a role, given its name
	 *
	 * @param role Name of the role to find
	 *
	 */
	private long findRoleIdByName(String role)        
	{
		try {
			JdbcTemplate jt = (JdbcTemplate) properties.get("jdbcTemplate");
			logger.debug("findRoleIdByName('" + role + "') called");
			String tablename = roleTable();
			String sql =
			  "SELECT roleId " +
			  "FROM " + tablename + 
			  " WHERE roleName = ? ";
			List<Object> sqlParams = new ArrayList<Object>(3);
			sqlParams.add(role);
			long key = jt.queryForObject(sql, sqlParams.toArray(), Long.class);
			logger.debug("findRoleIdByName('" + role + "') returning " + key);
			return key;
		} catch (org.springframework.dao.IncorrectResultSizeDataAccessException irsdae) {
			throw (IllegalArgumentException) new IllegalArgumentException(
			  "Could not find role '" + role + "' in " + roleTable()).initCause(irsdae);
		}
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

