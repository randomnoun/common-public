package com.randomnoun.common.webapp;

/* (c) 2013-15 randomnoun. All Rights Reserved. This work is licensed under a
 * BSD Simplified License. (http://www.randomnoun.com/bsd-simplified.html)
 */

import java.beans.PropertyVetoException;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import com.mchange.v2.c3p0.ComboPooledDataSource;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.dao.DataAccessResourceFailureException;

import com.randomnoun.common.PropertyParser;
import com.randomnoun.common.Struct;
import com.randomnoun.common.Text;
import com.randomnoun.common.jexl.sql.SqlGenerator;
import com.randomnoun.common.security.SecurityAuthenticator;
import com.randomnoun.common.security.SecurityContext;
import com.randomnoun.common.security.SecurityLoader;
import com.randomnoun.common.security.User;
import com.randomnoun.common.security.impl.NullSecurityAuthenticatorImpl;
import com.randomnoun.common.security.impl.NullSecurityLoaderImpl;
import com.randomnoun.common.security.impl.SpringSecurityAuthenticatorImpl;
import com.randomnoun.common.security.impl.SpringSecurityLoaderImpl;


/** An attempt to standardise config objects across the handful of webapps in this
 * workspace.  
 * 
 * <p>Holds configuration data for this web application. Is used to look up 
 * resources for use by other application components, including:
 * 
 * <ul>
 * <li>database connections (JdbcTemplates)
 * <li>security contexts (for authentication / authorisation)
 * </ul>
 * 
 * @author Greg
 * 
 */
public abstract class AppConfigBase extends Properties {

	/** Generated serialVersionUID */
	private static final long serialVersionUID = -5375559262217993785L;

	/** Revision string for stacktraces */
    public static String _revision = "$Id$";

    /** Logger instance for this class */
    public static Logger logger = Logger.getLogger(AppConfigBase.class);
    
    /** Global instance */
    public static AppConfigBase instance = null;

    /** JdbcTemplates for this AppConfig */
    private Map<String, JdbcTemplate> jdbcTemplates = new HashMap<String, JdbcTemplate>();
    
    /** Datasource underlying the jdbcTemplates */
    private Map<String, DataSource> dataSources = new HashMap<String, DataSource>();
    
	/** Security context used for authentication and authorisation */
    protected SecurityContext securityContext;
    
	/** If set to non-null, indicates an exception that occurred during initialisation. */
	protected Throwable initialisationFailure = null;
    
	/** The name of the -D directive which, if present, specifies the folder containing the
	 * configuration file; e.g. "com.randomnoun.appName.configPath"
	 */
	public abstract String getSystemPropertyKeyConfigPath();

	/** The name of the file which configures this application; e.g. "/appName-web.properties" */
	public abstract String getConfigResourceLocation();
	
    /** Ensure that the database drivers are accessible, for simple and dbcp connection types 
     * 
     * @throws RuntimeExcption if the database drivers are not on the classpath of this webapp
     */
    protected void initDatabase() {
        logger.info("Initialising database...");
        try {
        	String connectionType = getProperty("database.connectionType");
        	if (connectionType==null || 
        	  connectionType.equals("simple") || 
        	  connectionType.equals("dbcp")) { 
        		Class.forName(getProperty("database.driver")).newInstance();
        	}
        } catch (Exception e) {
            initialisationFailure = e;
            throw new RuntimeException("Could not load database driver '" + getProperty("database.driver") + "'", e);
        }
    }
    
    /** Initialises log4j 
     */
    protected void initLogger() {
        System.out.println("Initialising log4j...");
        
        Properties props = new Properties();
        props.putAll(PropertyParser.restrict(this, "log4j", false));
    
        // replace ${xxxx}-style placeholders
        String key, value, varName, varValue;
        int startIdx, endIdx;
        for (Enumeration<Object> e = props.keys(); e.hasMoreElements(); ) {
        	key = (String) e.nextElement();
        	value = (String) props.get(key);
        	startIdx = 0;
        	startIdx = value.indexOf("${", startIdx);
        	while (startIdx > -1) {
        		endIdx = value.indexOf("}", startIdx+2);
        		if (endIdx>-1) {
        			varName = value.substring(startIdx+2, endIdx-startIdx-2);
        			varValue = (String) this.get(varName);
        			if (varValue == null) { varValue = ""; }
        			value = value.substring(0, startIdx) + varValue + value.substring(endIdx + 1);
        			startIdx = value.indexOf("${", startIdx); 
        		} else {
        			startIdx = -1;
        		}
        	}
        }
        
        PropertyConfigurator.configure(props);
        logger.debug("log4j initialised");
    }
    
    protected void initSecurityContext() {
    	
    	if (securityContext==null) {
			logger.info("Initialising security context...");
			if (!"false".equals(getProperty("auth.enableSecurityContext"))) {
				Map<String, Object> securityProperties  = new HashMap<String, Object>();
				// no customerIds, applicationIds, auditUsernames or table suffixes ! Yay !
				String dbVendor = (String) this.get("database.vendor");
				if (Text.isBlank(dbVendor)) { dbVendor = SqlGenerator.DATABASE_MYSQL; }
				
				securityProperties.put(SpringSecurityLoaderImpl.INIT_DATABASE_VENDOR, dbVendor);
				securityProperties.put(SpringSecurityLoaderImpl.INIT_JDBCTEMPLATE, getJdbcTemplate());
				
				securityProperties.put(SecurityContext.INIT_CASE_INSENSITIVE, this.get("securityContext.caseInsensitive"));
				securityProperties.put(SecurityContext.INIT_USER_CACHE_SIZE, this.get("securityContext.userCacheSize"));
				securityProperties.put(SecurityContext.INIT_USER_CACHE_EXPIRY, this.get("securityContext.userCacheExpiry"));
				
				SecurityLoader securityLoader = new SpringSecurityLoaderImpl();
				SecurityAuthenticator securityAuthenticator = new SpringSecurityAuthenticatorImpl();
				
				securityContext = new SecurityContext(securityProperties, securityLoader, securityAuthenticator);
			} else {
				Map<String, Object> securityProperties  = new HashMap<String, Object>();
				SecurityLoader securityLoader = new NullSecurityLoaderImpl();
				SecurityAuthenticator securityAuthenticator = new NullSecurityAuthenticatorImpl();
				securityContext = new SecurityContext(securityProperties, securityLoader, securityAuthenticator);
			}
    	}
    }
    
    /** Sets the hostname application property. Should be called before {@link #loadProperties()},
     * which may have ENVIRONMENT settings that rely on the hostname.
     */
    protected void initHostname() {
        String hostname;
        try {
            java.net.InetAddress localMachine = java.net.InetAddress.getLocalHost();
            hostname = localMachine.getHostName();
        } catch(java.net.UnknownHostException uhe) {
            hostname = "localhost";
        }
        this.put("hostname", hostname);
    }

    /** Returns a bundle to be used for internationalisation. The bundle will initially be
     * searched for using the standard ResourceBundle.getBundle(name) method (i.e. in the
     * server's classpath); and, if this fails, will return 
     * ResourceBundle("resources.i18n." + name); which will search the i18n tree of the
     * eomail project. 
     * 
     * @param i18nBundleName The bundle name to search for (e.g. "user")
     * @param user The user for whom we are retrieving i18ned messages for (the locale of this
     *   user will be used to determine which resource bundle to use)
     * @return The ResourceBundle requested.
     */    
    public ResourceBundle getBundle(String i18nBundleName, User user) {
        if (i18nBundleName.startsWith("resources")) {
            return ResourceBundle.getBundle(i18nBundleName, user.getLocale());    
        } else {
            return ResourceBundle.getBundle("resources.i18n." + i18nBundleName, user.getLocale());
        }
        
    }

    
    /** Load the configuration from both the classpath and the
     * filesystem. Properties defined on the filesystem will override any contained within
     * the web application. 
     * 
     * <p>Multiple environments can be specified in properties files using the 
     * STARTENVIRONMENT/ENDENVIRONMENT tags. The tags use the machine's hostname as an
     * environment specifier. 
     */
    protected void loadProperties() {
    	String configPath = System.getProperty(getSystemPropertyKeyConfigPath());
    	if (configPath==null) { configPath = "."; }
        File configFile = new File(configPath + getConfigResourceLocation());
        try {
            // load from resources
            System.out.println("Loading properties file for environment '" + this.getProperty("hostname") + "'");
            InputStream is = this.getClass().getClassLoader().getResourceAsStream(getConfigResourceLocation());
            if (is==null && getConfigResourceLocation().charAt(0)=='/') {
            	// command-line support
            	is = this.getClass().getClassLoader().getResourceAsStream(getConfigResourceLocation().substring(1));
            }
            if (is==null) {
                System.out.println("Could not find internal property file");
            } else {
                PropertyParser parser = new PropertyParser(new InputStreamReader(is), this.getProperty("hostname"));
                Properties props = parser.parse();
                is.close();
                this.putAll(props);
            }
            
            // then load from file (if it exists)
            if (configFile.exists()) {
                System.out.println("Loading properties file from '" + configFile.getCanonicalPath() + "'");
                is = new BufferedInputStream(new FileInputStream(configFile));
                PropertyParser parser = new PropertyParser(new InputStreamReader(is), this.getProperty("hostname"));
                Properties props = parser.parse();
                is.close();
                this.putAll(props);
            } else {
                System.out.println("Properties file not found at '" + configFile.getCanonicalPath() + "' ... using defaults");
                
            }
        } catch (Exception e) {
            this.initialisationFailure = e;
            throw new RuntimeException("Could not load " + getConfigResourceLocation(), e);
        }
    }
    
    
    /** Return a jdbcTemplate object that can be used to query the database 
     * 
     * @return a jdbcTemplate to the database
     */
    public JdbcTemplate getJdbcTemplate() {
    	return getJdbcTemplate(null);
    }
    
    /** Returns a named jdbcTemplate object that can be used to query the database
     *  
     * @param connectionName The name of the connection
     * 
     * @return a jdbcTemplate to the database
     */
    public synchronized JdbcTemplate getJdbcTemplate(String connectionName) {
    	String prefix = connectionName == null ? "database." : "databases." + connectionName + ".";
    	
    	JdbcTemplate jt = jdbcTemplates.get(connectionName);
    	if (jt==null) {
        	String connectionType = getProperty(prefix + "connectionType");
        	if (connectionType==null) { connectionType = "simple"; }
    		String driver = getProperty(prefix + "driver");
	        String url = getProperty(prefix + "url");
	        String username = getProperty(prefix + "username");
	        String password = getProperty(prefix + "password");
	        logger.info("Retrieving " + connectionType + " connection for '" + username + "'/'" + password + "' @ '" + url + "'");
	        DataSource ds = null;
        	if (!Text.isBlank(driver)) { 
	        	try {
	        		Class.forName(driver); 
	        	} catch (ClassNotFoundException e) {
					logger.error("Error loading class '" + driver + "' for database '" + connectionName + "'", e);
				} 
	        }
	        if (connectionType.equals("simple")) {
	        	if (url==null) { throw new NullPointerException(prefix + "url has not been initialised"); }
		        Connection connection;
		        try {
		            if (Text.isBlank(username)) {
		                connection = DriverManager.getConnection (url);
		            } else {
		                connection = DriverManager.getConnection (url, username, password);
		            }
		        } catch (SQLException sqle) {
		            throw new DataAccessResourceFailureException("Could not open connection to database", sqle); 
		        }
		        ds = new SingleConnectionDataSource(connection, false);
		        
	        } else if (connectionType.equals("dbcp")) {
	        	if (url==null) { throw new NullPointerException(prefix + "url has not been initialised"); }
	        	BasicDataSource bds = new BasicDataSource();
	        	bds.setDriverClassName(driver);
	        	Map<? extends Object, ? extends Object> dbcpProps = PropertyParser.restrict(this, prefix + "dbcp", true);
	        	Struct.setFromMap(bds, dbcpProps, false, true, false);
	        	bds.setUrl(url);
	        	if (!Text.isBlank(username)) {
	        		bds.setUsername(username);
	        		bds.setPassword(password);
	        	}
	        	ds = bds;
	        	
	        } else if (connectionType.equals("c3p0")) {
	        	if (url==null) { throw new NullPointerException(prefix + "url has not been initialised"); }
	        	ComboPooledDataSource  cpds = new ComboPooledDataSource ();
	        	try {
					cpds.setDriverClass(driver);
				} catch (PropertyVetoException e) {
					throw new IllegalStateException("Could not set driverClass '" + driver + "' for c3p0 datasource", e);
				}
	        	@SuppressWarnings("unchecked")
				Map<String,String> c3poProps = (Map<String, String>) PropertyParser.restrict(this, prefix + "c3p0", true);
	        	// remove extensions from props
	        	for (Iterator<Map.Entry<String,String>> i = c3poProps.entrySet().iterator(); i.hasNext(); ) { 
	        		Map.Entry<String,String> e=i.next(); 
	        		if (e.getKey().startsWith("extensions.")) { i.remove(); }
	        	}
	        	Struct.setFromMap(cpds, c3poProps, false, true, false);
	        	
	        	// set extensions separately
	        	@SuppressWarnings("unchecked")
				Map<String,String> c3poExtensionProps = (Map<String, String>) PropertyParser.restrict(this, prefix + "c3p0.extensions", true);
	        	cpds.setExtensions(c3poExtensionProps);
	        	cpds.setJdbcUrl(url);
	        	if (!Text.isBlank(username)) {
	        		cpds.setUser(username);
	        		cpds.setPassword(password);
	        	}
	        	ds = cpds;
	        	
	        } else if (connectionType.equals("jndi")) {
	        	String jndiName = getProperty(prefix + "jndiName");
				try {
					InitialContext ctx = new InitialContext();
					Context envContext  = (Context) ctx.lookup("java:/comp/env");
					ds = (DataSource) envContext.lookup(jndiName);
					// could fallback to global datasource if comp/env is not here
				} catch (NamingException e) {
					throw new IllegalStateException("Could not retrieve datasource '" + jndiName + "' from JNDI", e);
				}
	        } else {
	        	throw new IllegalStateException("Unknown " + prefix + "connectionType property '" + connectionType + "')");
	        }
	        
	        jt = new JdbcTemplate(ds);
	        dataSources.put(connectionName, ds);
	        jdbcTemplates.put(connectionName, jt);
    	}
    	return jt;
    }
    
    
	/** Return the security context for this application
	 * 
	 * @return the security context for this application
	 */
    public SecurityContext getSecurityContext() {
    	if (securityContext==null) {
    		throw new IllegalStateException("Security context not initialised");
    	}
    	return securityContext;
    }
    

	/** Determines whether a user 'may have' a permission on the application. 
	 * See the hasPermission method in SecurityContext for more details.
	 *
	 * @param user The user we are checking permissions for. 
	 * @param permission The permission we are checking.
	 * @return 'true' if the user may be authorised to perform the permission,
	 *   false otherwise
	 */
	public boolean hasPermission(User user, String permission)
	{
		if ("false".equals(getProperty("auth.enableSecurityContext"))) { return true; }
		SecurityContext securityContext = getSecurityContext();
		return securityContext.hasPermission(user, permission);
	}

	/** Determines whether a user 'may have' a permission on the application. 
	 * See the hasPermission method in SecurityContext for more details. If the user
	 * does not have the permission, then a SecurityException is thrown
	 *
	 * @see SecurityContext#hasPermission(com.randomnoun.common.security.User, String)
	 *
	 * @param user The user we are checking permissions for. 
	 * @param permission The permission we are checking.
	 * 
	 * @throws SecurityException if the user does not have the supplied permission 
	 */
	public void checkPermission(User user, String permission) throws SecurityException {
		if (!hasPermission(user,  permission)) {
			throw new SecurityException("User does not have '" + permission + "' permission");
		}
    }

	/** Determines whether a user has a permission on the application. 
	 * See the hasPermission method in SecurityContext for more details.
	 *
	 * @param user The user we are checking permissions for.
	 * @param permission The permission we are checking.
	 * @param resourceContext Fine-grained resource context
	 * @return 'true' if the user is authorised to perform the permission,
	 *   false otherwise
	 */
	public boolean hasPermission(User user, String permission, Map<String, Object> resourceContext)
	{
		if ("false".equals(getProperty("auth.enableSecurityContext"))) { return true; }
		SecurityContext securityContext = getSecurityContext();
		return securityContext.hasPermission(user, permission, resourceContext);
	}


	/** Provides direct access to the datasource for this application. Most applications should use the 
	 * {@link #getJdbcTemplate()} method instead.
	 * 
	 * @return the dataSource underlying the jdbcTemplate to the database
	 */
	public DataSource getDataSource() {
		getJdbcTemplate(); // make sure default dataSource is initialised
		return dataSources.get(null);
	}

	/** Provides direct access to a named datasource for this application. Most applications should use the 
	 * {@link #getJdbcTemplate(String)} method instead.
	 * 
	 * @param connectionName the name of the connection
	 * 
	 * @return the dataSource underlying the named jdbcTemplate to the database 
	 */
	public DataSource getDataSource(String connectionName) {
		getJdbcTemplate(connectionName); // make sure default dataSource is initialised
		return dataSources.get(connectionName);
	}

}
