package com.randomnoun.common.servlet;

/* (c) 2013-2016 randomnoun. All Rights Reserved. This work is licensed under a
 * BSD Simplified License. (http://www.randomnoun.com/bsd-simplified.html)
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

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
 * <code>build.properties</code> file in the application being created, 
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
 * <p>This script will also look for a build.properties file in /etc/build.properties; 
 * this should exist on docker containers built by bamboo. If this exists, it is also 
 * included in the JSON output under the "/etc/build.properties" key.
 * 
 * 
 * @author knoxg
 * @see <a href="http://www.randomnoun.com/wp/2013/09/24/webapp-versions-v1-0/">http://www.randomnoun.com/wp/2013/09/24/webapp-versions-v1-0/</a>
 * 
 */
public class VersionServlet extends HttpServlet {
    
	/** Generated serialVersionUID */
	private static final long serialVersionUID = -6978469440912690523L;
	
	/** Logger for this class */
    public static final Logger logger = Logger.getLogger(VersionServlet.class);

    
	/** Post method; just defers to get
	 * 
	 * @see jakarta.servlet.http.HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
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
	
	/** Removes build properties that were not set during the build process
	 *  (i.e. are still set to '${xxx}' placeholders).
	 *   
	 * @param props Properties object
	 */
	private void removeDefaultProperties(Properties props) {
		// projects not built in bamboo won't set these
    	removeDefaultProperty(props, "bamboo.buildKey", "bambooBuildKey");
    	removeDefaultProperty(props, "bamboo.buildNumber", "bambooBuildNumber");
    	removeDefaultProperty(props, "bamboo.buildPlanName", "bambooBuildPlanName");
    	removeDefaultProperty(props, "bamboo.buildTimeStamp", "bambooBuildTimeStamp");
    	removeDefaultProperty(props, "bamboo.buildForceCleanCheckout", "bambooForceCleanCheckout");
    	// bamboo.custom.cvs.last.update.time == "${bambooCustomCvsLastUpdateTime}" 
    	// if there is no -Dbamboo.custom.cvs.last.update.time property on mvn build cmdline
    	removeDefaultProperty(props, "bamboo.custom.cvs.last.update.time", "bambooCustomCvsLastUpdateTime");
    	removeDefaultProperty(props, "bamboo.custom.cvs.last.update.time.label", "bambooCustomCvsLastUpdateTimeLabel");
        // bamboo.custom.cvs.last.update.time == "${bamboo.custom.cvs.last.update.time}"
    	// if there IS a -Dbamboo.custom.cvs.last.update.time property on mvn build cmdline
    	// but it's not set by bamboo (because it's now a git project, but the mvn cmdline hasn't been changed)  
    	removeDefaultProperty(props, "bamboo.custom.cvs.last.update.time", "bamboo.custom.cvs.last.update.time");
    	removeDefaultProperty(props, "bamboo.custom.cvs.last.update.time.label", "bamboo.custom.cvs.last.update.time.label");
    	
    	
    	// bamboo git projects have some extra properties we could include here; 
    	//   bamboo.repository.git.branch
    	//   bamboo.repository.git.repositoryUrl
    	// see https://confluence.atlassian.com/bamboo/bamboo-variables-289277087.html
    	
    	// projects in cvs repositories won't set this
    	removeDefaultProperty(props, "git.buildNumber", "buildNumber");
	}
	
	/** See class documentation
	 * 
	 * @see jakarta.servlet.http.HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
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
    	
    	removeDefaultProperties(props);
    	File etcPropsFile = new File("/etc/build.properties");
    	if (etcPropsFile.exists()) {
    		Properties etcProps = new Properties();
    		is = new FileInputStream(etcPropsFile);
    		etcProps.load(is);
    		is.close();
    		removeDefaultProperties(etcProps);
    		props.put("/etc/build.properties", etcProps);
    	}
    	
    	response.setHeader("Content-Type", "application/json");
    	response.setStatus(HttpServletResponse.SC_OK);
    	response.getWriter().println(new JSONObject(props).toString());
    }
}
