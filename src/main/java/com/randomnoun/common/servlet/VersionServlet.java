package com.randomnoun.common.servlet;

/* (c) 2013 randomnoun. All Rights Reserved. This work is licensed under a
 * BSD Simplified License. (http://www.randomnoun.com/bsd-simplified.html)
 */

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.json.JSONObject;

/**
 * Reads the contents of the build.properties file in the 
 * current application and sends it to the browser in a 
 * variety of forms. Probably JSON for starters.
 *
 * <p>The properties file must be located on the classpath at the
 * location "/build.properties".
 * 
 * <p>For this to work, the application needs to have been built
 * through maven (and possibly bamboo), and there should be a 
 * <tt>build.properties</tt> file in the application being created, 
 * with the contents:
 * 
 * <pre>
 *  bamboo.buildKey=${bambooBuildKey}
	bamboo.buildNumber=${bambooBuildNumber}
	bamboo.buildPlanName=${bambooBuildPlanName}
	bamboo.buildTimeStamp=${bambooBuildTimeStamp}
	bamboo.buildForceCleanCheckout=${bambooForceCleanCheckout}
	bamboo.custom.cvs.last.update.time=${bambooCustomCvsLastUpdateTime}
	bamboo.custom.cvs.last.update.time.label=${bambooCustomCvsLastUpdateTimeLabel}
	
	maven.pom.name=${pom.name}
    maven.pom.version=${pom.version}
    maven.pom.groupId=${pom.groupId}
    maven.pom.artifactId=${pom.artifactId}
   </pre>
 *
 * and the bamboo plan should have the following in the bamboo plan's
 * stage's job's task's maven goal specification:
 * 
 * <pre>
 *  "-DbambooBuildKey=${bamboo.buildKey}" 
    "-DbambooBuildNumber=${bamboo.buildNumber}" 
    "-DbambooBuildPlanName=${bamboo.buildPlanName}" 
    "-DbambooBuildTimeStamp=${bamboo.buildTimeStamp}" 
    "-DbambooBuildForceCleanCheckout=${bamboo.buildForceCleanCheckout}" 
    "-DbambooCustomCvsLastUpdateTime=${bamboo.custom.cvs.last.update.time}" 
    "-DbambooCustomCvsLastUpdateTimeLabel=${bamboo.custom.cvs.last.update.time.label}" 
    -DuniqueVersion=false 
 * </pre>
 * 
 * @author knoxg
 * @blog http://www.randomnoun.com/wp/2013/09/24/webapp-versions-v1-0/
 * @version $Id$
 */
@SuppressWarnings("serial")
public class VersionServlet extends HttpServlet {
    
	/** A revision marker to be used in exception stack traces. */
    public static final String _revision = "$Id$";

	/** Logger for this class */
    public static final Logger logger = Logger.getLogger(VersionServlet.class);

    
	/** Post method; just defers to get
	 * 
	 * @see javax.servlet.http.HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}

	/** If a property has not been populated through the maven resource filter
	 * mechanism, then remove it from the Properties object.
	 *   
	 * @param props Properties object
	 * @param key key to check
	 * @param value default value
	 */
	private void removeDefaultProperty(Properties props, String key, String value) {
		if (("${" + value + "}").equals(props.get(key))) {
			props.remove(key);
		}
	}
	
	/** See class documentation
	 * 
	 * @see javax.servlet.http.HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException 
    {
    	InputStream is = VersionServlet.class.getClassLoader().getResourceAsStream("/build.properties");
    	Properties props = new Properties();
    	if (is==null) {
    		props.put("error", "Missing build.properties");
    	} else {
	    	props.load(is);
	    	is.close();
    	}
    	
    	removeDefaultProperty(props, "bamboo.buildKey", "bambooBuildKey");
    	removeDefaultProperty(props, "bamboo.buildNumber", "bambooBuildNumber");
    	removeDefaultProperty(props, "bamboo.buildPlanName", "bambooBuildPlanName");
    	removeDefaultProperty(props, "bamboo.buildTimeStamp", "bambooBuildTimeStamp");
    	removeDefaultProperty(props, "bamboo.buildForceCleanCheckout", "bambooForceCleanCheckout");
    	removeDefaultProperty(props, "bamboo.custom.cvs.last.update.time", "bambooCustomCvsLastUpdateTime");
    	removeDefaultProperty(props, "bamboo.custom.cvs.last.update.time.label", "bambooCustomCvsLastUpdateTimeLabel");
    	
    	response.setHeader("Content-Type", "application/json");
    	response.setStatus(HttpServletResponse.SC_OK);
    	response.getWriter().println(new JSONObject(props).toString());
    }
}
