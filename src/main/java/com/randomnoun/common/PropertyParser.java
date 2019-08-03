package com.randomnoun.common;

/* (c) 2013 randomnoun. All Rights Reserved. This work is licensed under a
 * BSD Simplified License. (http://www.randomnoun.com/bsd-simplified.html)
 */

import java.io.*;
import java.text.ParseException;
import java.util.*;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import com.randomnoun.common.Struct;
import com.randomnoun.common.PropertyParser;



/**
 * The PropertyParser class parses a property definition text file into
 * a Properties object.
 *
 * <p>This parser differs from the standard Properties parser in that
 * sections can be marked off for particular regions (e.g. properties
 * that only take effect in development, or when run on certain machines).
 * The current region
 * is specified by the <tt>com.randomnoun.common.mode</tt> system property
 * set on the VM commandline. If this system property is not set,
 * the region defaults to the value "<tt>localhost-dev-unknown</tt>".
 * An alternate constructor exists if you
 * wish to specify the region manually.
 *
 * <p>Environments are specified in '<i>machine-release-subsystem</i>'
 * format, with each segment set as follows:
 * <attributes>
 * machine - the hostname of the system (in lowercase)
 * release - the development phase of the system (either set to <tt>dev</tt>,
 *   <tt>xpt</tt>, <tt>prd</tt> for development, acceptance or production
 * subsystem - the subsystem that this VM represents. 
 * </attributes>
 *
 * <p>Properties are specified in the standard "propertyName=propertyValue" method.
 * Whitespace is removed from either side of the '=' character. New-lines
 * can be specified in the property value by using the escape sequence '\n'.
 * Lines can be continued over a single line by placing the character '\' at
 * at the end of the line to be continued; e.g.
 *
 * <pre style="code">
 *   property1=value1
 *   property2=this is a very long value for property2, which spans \
 *             over a single line.
 * </pre>
 *
 * <p>Properties that are specific to a particular region should be surrounded
 * by the lines "STARTENVIRONMENT environmentMask" and "ENDENVIRONMENT environmentMask".
 * Individual properties can be defined for a region by prefixing the line
 * with "ENV environmentMask"; e.g.
 *
 * <pre style="code">
 *   property1=all regions
 *
 *   STARTENVIRONMENT *-xpt-*
 *     property2=this property only set in acceptance region
 *     property3=same for this property
 *   ENDENVIRONMENT
 *
 *   STARTENVIRONMENT dtp11523-dev-*
 *     property2=these properties only set in the development region
 *     property3=running on the host dtp11523
 *   ENDENVIRONMENT
 *
 *   ENV *-prd-* property4=this property only visible in production
 * </pre>
 *
 * <p>As shown above, the '<tt>*</tt>' character can be used to specify a property
 * across multiple regions. The keywords 'STARTENVIRONMENT', 'ENDENVIRONMENT' and
 * 'ENV' are case-insensitive
 * 
 * <p>You can now also specify environments based on the values of previously-defined
 * properties; this allows a simple <tt>#ifdef</tt> style facility. There are 
 * two types of syntax, which use regex matching or simple string matching; e.g.
 * 
 * <pre style="code">
 *   enable.fileAct=true
 *   compound.property=123-456-789
 * 
 *   STARTENVIRONMENT enable.fileAct = true
 *     # these properties are only set when enable.fileAct is set to true
 *   ENDENVIRONMENT
 *   STARTENVIRONMENT compound.property =~ *-789
 *     # these properties are only set when compound.property ends with -789
 *   ENDENVIRONMENT
 * </pre>
 * 
 * <p>Property files can contain any number of blank lines, or comments (lines
 * starting with the '#' character), which will be ignored by the parser.
 *
 * <p>Any occurences of the string "<tt>\n</tt>" in a property value will be
 * replaced by a newline character.
 *
 * <p>A property make also contain a List, rather than a string, by including the
 * index of the list item in square brackets in the property key; e.g.
 *
 * <pre style="code">
 *   listname[1]=first element
 *   listname[3]=third element
 * </pre>
 *
 * <p>This implementation returns an ArrayList of Strings for these types of declarations.
 * Undeclared array elements that appear before the last index will return null.
 *
 * <p>If you want to create a list, but the index of the elements is unknown (they are 
 * dependant on other properties, for example), then you can use a "*" to denote the
 * next available list index, or leave it empty to denote the last used list index; e.g.
 * 
 * <pre style="code">
 * testList[0].a=a-value 0
 * testList[0].b=b-value 0
 * testList[0].c=a-value 0
 *  
 * testList[1].a=a-value 1
 * testList[1].b=b-value 1
 * testList[1].c=a-value 1
 *  
 * testList[*].a=a-value 2
 * testList[].b=b-value 2
 * testList[].c=a-value 2
 * </pre>
 * 
 * will generate a three-element list, each of which contains a map with three key/value pairs.
 *
 * <p>Properties that appear multiple times will take the value of the last-specified
 * value.
 *
 *
 * @author  knoxg
 * 
 */
public class PropertyParser {

    /** Logger instance for this class */
    public static final Logger logger = Logger.getLogger(PropertyParser.class.getName());

    /** display each token as it is read */
    static private final boolean verbose = false;

    /** file to parse */
    private LineNumberReader lineReader;

    /** line to parse */
    private StringTokenizer thisline;

    /** environment processing enabled */
    private boolean inEnvironment = false;

    /** currently within correct environment */
    private boolean correctEnvironment = true;

    /** current environment string */
    private String environmentID = "";

    /** the properties object we will populate from this InputStream */
    private Properties properties = null;

    /**
     * Create a new Parser object. Note that parsing does not begin until the
     * Parse method is called on this object.
     *
     * @param reader        The source of the site definition text.
     * @param environmentID     The environment ID in which to parse this text.
     */
    public PropertyParser(Reader reader, String environmentID) {
        lineReader = new LineNumberReader(reader);
        this.environmentID = environmentID;
    }

    /**
     * Create a new Parser object. Note that parsing does not begin until the
     * Parse method is called on this object. Assumes a DEV environment
     *
     * @param reader The source of the site definition text.
     */
    public PropertyParser(Reader reader) {
        this(reader, System.getProperty("com.randomnoun.common.mode", "localhost-dev-unknown"));
    }

    /**
     * Generates a Properties object from the input stream.
     *
     * @return A valid Properties object.
     *
     * @throws IOException
     * @throws ParseException
     */
    public Properties parse()
        throws ParseException, IOException {
        String line = ""; // current line
        String token = ""; // current token

        // ensure that all class fields have been reset
        properties = new Properties();

        line = lineReader.readLine();

        while (line != null) {
            line = line.trim();

            // line-continuation (a line ending with '\' is appended with the next)
            // could prove hazardous if we get a \ on the last line
            while (line != null && line.endsWith("\\")) {
                line = line.substring(0, line.length() - 1);

                try {
                    line = line + lineReader.readLine().trim();
                } catch (NullPointerException npe) {
                    // end of file reached; ignore
                }
            }

            // true = don't return delimiters
            thisline = new StringTokenizer(line, " =\t\n\r", true);

            if (thisline.hasMoreTokens()) {
                token = parseToken("keyword");

                // are we in the correct environment ?
                if (correctEnvironment || token.toLowerCase().equals("endenvironment")) {
                    parseLine(token);
                }
            }

            line = lineReader.readLine();
        }

        return properties;
    }

    /**
     * Parses a single line of the site definition file.
     *
     * @param token The token that was at the beginning of this line
     *
     * @throws ParseException
     */
    @SuppressWarnings("unchecked")
	private void parseLine(String token)
        throws ParseException {
        String lowerCaseToken;
        String nextToken = null;
        String value = null;

        // System.out.println("P " + token + "(" + line + ")");
        lowerCaseToken = token.toLowerCase();

        if (token.startsWith("#")) {
            // comment
            // call the appropriate method depending on the keyword...
        } else if (lowerCaseToken.equals("startenvironment")) {
            parseStartEnvironment();
        } else if (lowerCaseToken.equals("endenvironment")) {
            parseEndEnvironment();
        } else if (lowerCaseToken.equals("env")) {
            parseEnv();
		} else if (lowerCaseToken.equals("includeresource")) {
			parseIncludeResource();
        } else {
            // see if this is a key=value property assignment
            try {
                nextToken = parseToken("token");
            } catch (ParseException pe) {
                newParseException("Unknown keyword '" + token + "', '=' expected");
            }

            if (nextToken.equals("=")) {
                // must be in a key=value assignment
                value = null;

                try {
                    value = parseTokenToEOL("property value");
                } catch (ParseException pe) {
                }

                if (value == null) {
                    value = "";
                }

                // check if this is an array element	
                int lb = token.indexOf('[');
                int rb = token.indexOf(']');
                

                if (lb != -1 || rb != -1) {
                    if (lb == -1 || rb == -1) {
                        newParseException("Invalid list property key '" + token + "'");
                    }
                    List<Object> list = null;
                    String keyPart = token.substring(0, lb);
                    String listPart = token.substring(lb);
                    try {
                        list = (List<Object>) properties.get(keyPart);
                    } catch (ClassCastException cce) {
                        newParseException("Cannot create list '" + token + "', this property already exists");
                    }
                    if (list == null) {
                        list = new ArrayList<Object>();
                        properties.put(keyPart, list);
                    }
                    // if list index is "*" then create a new list index; if it's "-", then use the last index
                    String index = token.substring(lb+1, rb);
                    if (index.equals("*")) {
                    	index = String.valueOf(list.size()); // 0-based list, so next index is set to the size
                    	listPart = "[" + index + "]" + listPart.substring(listPart.indexOf("]")+1);
                    } else if (index.equals("")) {
                    	index = String.valueOf(list.size()-1);
						listPart = "[" + index + "]" + listPart.substring(listPart.indexOf("]")+1);
                    }
                    
                    Struct.setValue(list, listPart, value, true, true, true);
                } else {
                    // no index supplied, just set the property	
                    properties.setProperty(token, value);
                }
            } else {
                newParseException("Unknown token '" + nextToken + "', '=' expected");
            }
        }
    }

    /**
     * Creates and throws a new parse exception, which includes the current parse
     * position within the Reader.
     *
     * @param s The additional text to be included in this exception
     *
     * @throws ParseException The parse exception requested
     */
    private void newParseException(String s)
        throws ParseException {
        throw new ParseException("line " + lineReader.getLineNumber() + ": " + s, 0);
    }

    /**
     * Returns the next token.
     *
     * @param what The type of token we are expecting. This string is only used
     *   in any parseExceptions which are thrown.
     *
     * @return The next token on the line.
     *
     * @throws ParseException A parsing exception has occurred.
     */
    private String parseToken(String what)
        throws ParseException {
        String result = null;

        // keep grabbing tokens, until we hit something that isn't considered whitespace
        while (result == null || result.equals(" ") || result.equals("\r") || result.equals("\n") || result.equals("\t")) {
            if (!thisline.hasMoreTokens()) {
                newParseException("Expecting " + what);
            }

            result = thisline.nextToken(" =\t\n\r");
        }

        // convert \n's to newlines.
        result = result.replaceAll("\\\\n", "\n");

        if (verbose) {
            logger.debug("parsed token: '" + result + "'");
        }

        return result;
    }

    /**
     * Grabs every token until the end of line, and returns it as a string. If the
     * debugging variable 'verbose' is set to true, then each token is sent
     * to System.out as it is read. Enclosing single or double-quotes are removed.
     *
     * @param what The type of token we are expecting. This text is used
     *   in any parseExceptions which are thrown.
     *
     * @return The remaining text.
     *
     * @throws ParseException
     */
    private String parseTokenToEOL(String what)
        throws ParseException {
        String token;

        if (!thisline.hasMoreTokens()) {
            newParseException("Expecting " + what);
        }

        token = thisline.nextToken("\n").trim();
        token = token.replaceAll("\\\\n", "\n");

        if (verbose) {
            System.out.println("parsed token: " + token);
        }

        return token;
    }

    /**
     * Processes an ENV rule
     *
     * @throws ParseException
     */
    private void parseEnv()
        throws ParseException {
        String selectedenvironmentID;
        String token;

        selectedenvironmentID = parseToken("Environment ID");

        // @TODO this should really use the same rules as parseStartEnvironment
        if (selectedenvironmentID.equals(environmentID)) {
            token = parseToken("keyword").toLowerCase();
            parseLine(token);
        }
    }

	private void parseIncludeResource() throws ParseException {
		// include another properties file in here. This has a very
		// good chance of creating a infinite loop if one property file
		// includes another one, which in turn includes the first one. 
		// - this will manifest itself in some kind of OutOfMemoryException
		// or a StackOverflowException
		
		String resourceName = parseTokenToEOL("resource name");
		logger.debug("Including property resource '" + resourceName + "'");

		// load properties from classpath (in .EAR)
		InputStream inputStream = PropertyParser.class.getClassLoader().getResourceAsStream(resourceName);
		if (inputStream==null) {
			throw new ParseException("Could not find included resource '" + resourceName + "'", 0);
		}
		PropertyParser propertyParser = new PropertyParser(new InputStreamReader(inputStream), environmentID);
		Properties includedProperties = new Properties();
		try {
			includedProperties = propertyParser.parse();
		} catch (Exception e) {
			throw (ParseException) new ParseException("Could not load included resource '" +
				resourceName + "'", 0).initCause(e);
		}
		
		// should merge lists/maps, rather than replacing them.
		// (maybe this should be configurable ?)
		// @TODO code below only merges lists
		for (Iterator<Entry<Object, Object>> i = includedProperties.entrySet().iterator(); i.hasNext(); ) {
			Map.Entry<Object, Object> entry = i.next();
			if (entry.getValue() instanceof List) {
				Object existingObj = properties.get(entry.getKey());
				if (existingObj==null || !(existingObj instanceof List)) {
					properties.put(entry.getKey(), entry.getValue());
				} else {
					@SuppressWarnings("unchecked")
					List<Object> existingList = (List<Object>) existingObj;
					@SuppressWarnings("unchecked")
					List<Object> listValue = (List<Object>) entry.getValue();
					for (int j=0; j<listValue.size(); j++) {
						if (listValue.get(j)!=null) {
							Struct.setListElement(existingList, j, listValue.get(j));
						}
					}
				}
			} else {
				properties.put(entry.getKey(), entry.getValue());
			}
		}
		// properties.putAll(includedProperties);
	}

    /** Begins per-environment parsing rules.
     *  @throws ParseException
     */
    private void parseStartEnvironment()
        throws ParseException {
        if (inEnvironment) {
            newParseException("attempted to nest environment areas");
        }
        String propertyName = "environmentId";
        String propertyMatch = null;
        String propertyValue = null;
        String envSpec = parseTokenToEOL("Environment specification");
        if (envSpec.indexOf("=~")!=-1) {
        	propertyName = envSpec.substring(0, envSpec.indexOf("=~")).trim();
        	propertyMatch = envSpec.substring(envSpec.indexOf("=~")+2).trim();
			propertyMatch = propertyMatch.replaceAll("\\*", ".*");
        	propertyValue = propertyName.equals("environmentId") ? environmentID : properties.getProperty(propertyName);
        	if (propertyValue==null) { propertyValue = ""; }
			correctEnvironment = propertyValue.matches(propertyMatch);	
        } else if (envSpec.indexOf("=")!=-1) {
			propertyName = envSpec.substring(0, envSpec.indexOf("=")).trim();
			propertyMatch = envSpec.substring(envSpec.indexOf("=")+1).trim();
			propertyValue = propertyName.equals("environmentId") ? environmentID : properties.getProperty(propertyName);
			if (propertyValue==null) { propertyValue = ""; }
			correctEnvironment = propertyValue.equals(propertyMatch);	
        } else {
        	// not on 'xxx=xxx' form, default to old behaviour (match on envId)
			propertyMatch = envSpec.replaceAll("\\*", ".*");
			correctEnvironment = environmentID.toLowerCase().matches(propertyMatch.toLowerCase());
        }
        
        // test for "property=value" style environments
        //selectedenvironmentID = selectedenvironmentID.replaceAll("\\*", ".*");
        //correctEnvironment = environmentID.matches(selectedenvironmentID);
        inEnvironment = true;
    }

    /**
     * Completes per-environment parsing rules.
     *
     * @throws ParseException
     */
    private void parseEndEnvironment()
        throws ParseException {
        if (!inEnvironment) {
            newParseException("'endenvironment' without matching 'startenvironment'");
        }

        inEnvironment = false;
        correctEnvironment = true;
    }

    /**
     * Returns a subset of a Properties object. The subset is determined by only
     * returning those key/value pairs whose keys begin with a set prefix. e.g.
     * if 'a' contains the properties
     *
     * <attributes>
     *   customer.1.name=fish
     *   customer.1.description=Patagonian toothfish
     *   customer.2.name=hunter
     *   customer.2.description=Patagonian toothfish hunter
     *   customer.11.name=spear
     *   customer.11.description=Patagonian toothfish hunter's spear
     * </attributes>
     *
     * <p>then calling <code>restrict(a, "customer.1", false)</code> will return:
     *
     * <attributes>
     *   customer.1.name=fish
     *   customer.1.description=Patagonian toothfish
     * </attributes>
     *
     * <p>setting the 'removePrefix' to true will remove the initial prefix from
     * the returned property list; <code>restrict(a, "customer.1", true)</code> in
     * this case will then return:
     *
     * <attributes>
     *   name=fish
     *   description=Patagonian toothfish
     * </attributes>
     *
     * <p>Note that the prefix passed in to this method has no trailing period,
     * but each property must contain that period (to prevent <tt>customer.11</tt>
     * from being returned in the example above).
     *
     * <p>If 'properties' is set to null, then this method will return null.
     * If 'prefix' is set to null, then this method will return the entire property list.
     *
     * @param properties The initial property list that we wish to restrict
     * @param prefix     The prefix used to restrict the property list
     * @param removePrefix  If set to true, the result keys will be stripped of the initial prefix text
     *
     * @return A restricted property list.
     */
    public static Map<? extends Object, ? extends Object> restrict(Map<Object, Object> properties, String prefix, boolean removePrefix) {
        if (properties == null) {
            return null;
        }

        if (prefix == null) {
            return properties;
        }

        Properties result = new Properties();

        // this could possibly break existing, yet weird code
        // if (!prefix.endsWith(".")) { prefix = prefix + "."; };
        prefix = prefix + ".";
        
        Map.Entry<Object, Object> entry;

        for (Iterator<Entry<Object, Object>> i = properties.entrySet().iterator(); i.hasNext();) {
            entry = i.next();

            String key = (String) entry.getKey();

            if (key.startsWith(prefix)) {
                if (removePrefix) {
                    key = key.substring(prefix.length());
                }

                result.put(key, entry.getValue());
            }
        }

        return result;
    }

    /**
     * Method used to test the parser from the command line. The file to parse is
     * specified on the command line; if missing, then it uses 'test.properties'
     * as the default.
     *
     * @param args
     *
     * @throws IOException
     * @throws ParseException
     */
    public static void main(String[] args)
        throws IOException, ParseException {
        String filename = "test.properties";
        PropertyParser propertyParser;

        if (args.length != 1) {
            System.out.println("Reading from '" + filename + "' by default...");
        } else {
            filename = args[0];
        }

        try {
            propertyParser = new PropertyParser(new FileReader(filename));
            propertyParser.parse();
            System.out.println("Parse OK");
        } catch (ParseException pe) {
            System.out.println("Caught ParseException: " + pe);
            pe.printStackTrace();
        }
    }
}
