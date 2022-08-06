package com.randomnoun.common.webapp.taglib;

/* (c) 2013 randomnoun. All Rights Reserved. This work is licensed under a
 * BSD Simplified License. (http://www.randomnoun.com/bsd-simplified.html)
 */

import java.io.*;
import java.util.*;

import javax.servlet.jsp.*;
import javax.servlet.jsp.tagext.*;

import com.randomnoun.common.Struct;
import com.randomnoun.common.Text;

/**
 * Extends a javascript variable with a server-side resource or request attribute.
 * The variable may contain any amount of maps or lists, which will be
 * converted into the javascript equivalent.
 *
 * <p>Attributes defined for this tag (in common.tld) are:
 * <ul>
 * <li>baseName - the name of the javascript variable to extend
 * <li>name - the name of the object within base (may contain sub-object names separated by '.')
 * <li>value - the value of the javascript variable.
 * <li>key - if defined, the value of this field within the value object is used to extend the base object.
 *     if key is supplied the object is replaced, not merged
 * </ul>
 *
 * <p>Both name and value may contain EL-style expressions.
 *
 * @author  knoxg
 * 
 */
public class ExtendJavascriptVarTag
    extends BodyTagSupport
{
    /** Generated serialVersionUID */
	private static final long serialVersionUID = -7010835090281695598L;

	
    /** Base javscript object name */
    private String baseName;
    
    private String key;

    /** The javscript variable name */
    private String name;
    
    // * * The string entered in the value attribute of this tag */
    //private String valueString;
    
    /** The calculated value of the java object to embed */ 
    private Object value;

    /** The method in which dates are serialised to JSON */
    private String jsonFormat;
    
    
    /** Sets the name of the javascript object to modify
     * 
     * @param baseName the name of the javascript object to modify
     */
    public void setBaseName(String baseName)
    {
        this.baseName = baseName;
    }

    /**
     * Gets the name of the javascript object to modify
     *
     * @return the name of the javascript object to modify
     */
    public String getBaseName()
    {
        return baseName;
    }

    /** Sets the name of the field within the object to modify
     * 
     * @param key the name of the field within the object to modify
     */
    public void setKey(String key)
    {
        this.key = key;
    }

    /**
     * Gets the name of the field within the object to modify
     *
     * @return the name of the field within the object to modify
     */
    public String getKey()
    {
        return key;
    }

    /** Sets the name of the javascript variable to generate
     * 
     * @param name the name of the javascript variable to generate
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * Gets the name of the javascript variable to generate
     *
     * @return the name of the javascript variable to generate
     */
    public String getName()
    {
        return name;
    }

    /**
     * Sets the object to convert into javascript 
     *
     * @param value the object to convert into javascript 
     */
    public void setValue(Object value)
    {
        this.value = value;
    }

    /**
     * Returns the object to convert into javascript 
     *
     * @return the object to convert into javascript 
     */
    public Object getValue()
    {
        return value;
    }

    /** Sets the JSON format (e.g. method in which dates are serialised to JSON)
     * 
     * @param name the JSON format 
     */
    public void setJsonFormat(String jsonFormat)
    {
        this.jsonFormat = jsonFormat;
    }

    /**
     * Gets the JSON format (e.g. method in which dates are serialised to JSON)
     *
     * @return the JSON format
     */
    public String getJsonFormat()
    {
        return jsonFormat;
    }

    /** doStart tag handler required to fulfill the Tag interface defined in the
     * <a href="http://java.sun.com/products/jsp/">JSP specification</a>.
     *
     * This tag is always empty, and therefore must always
     * return BodyTag.SKIP_BODY
     *
     * @return BodyTag.SKIP_BODY
     */
    @SuppressWarnings("rawtypes")
	public int doStartTag()
        throws javax.servlet.jsp.JspException
    {
        try {
        	JspWriter out = pageContext.getOut();
        	
        	// ns = name.split('.');
            // for (var i=0; i<ns.length; i++) { var n=ns[i]; if (!base.hasOwnProperty(n)) { obj[n]={}; } obj = obj[n]; };
            // for (var i in obj) { if (obj.hasOwnProperty(i)) { base[i] = obj[i]; } };
            
            out.print("(function(b,obj) {"); // b=base
            out.print("function e(b,n){if (!b.hasOwnProperty(n)){b[n]={};}};\n");
            //String[] names = name.split("\\.");
            //for (String n : names) {
            //	out.print("n=\"" + Text.escapeJavascript(n) + "\";e(b,n);b=b[n];");
            //};
            out.print("var ns=\"" + Text.escapeJavascript(name) + "\".split('.');");
            out.print("for (var i in ns) { e(b,ns[i]);b=b[ns[i]]; }; ");
            if (!Text.isBlank(key)) {
            	out.print("b[obj[\"" + Text.escapeJavascript(key) + "\"]]={}; b=b[obj[\"" + Text.escapeJavascript(key) + "\"]]; ");
            }
            out.print("for (var i in obj) { if (obj.hasOwnProperty(i)) { b[i] = obj[i]; }}; ");
            out.print("})(" + baseName + ", "); 
        	
	        if (value == null) {
	        	out.append("null");
	        } else if (value instanceof String) {
	        	out.append("\"");
	        	out.append(Text.escapeJavascript((String) value));
	        	out.append("\"");
	        } else if (value instanceof List) {
	        	// out.append(Struct.structuredListToJson((List) value, jsonFormat));
	        	Struct.structuredListToJson(out, (List) value, jsonFormat);
	        } else if (value instanceof Map) {
	        	// out.append(Struct.structuredMapToJson((Map) value, jsonFormat));
	        	Struct.structuredMapToJson(out, (Map) value, jsonFormat);
	        } else if (value instanceof Number) {
	        	out.append(String.valueOf(value));
	        } else if (value instanceof Boolean) {
	        	out.append(String.valueOf(value));
	        } else if (value instanceof java.util.Date) {
            	// MS-compatible JSON encoding of Dates:
            	// see http://weblogs.asp.net/bleroy/archive/2008/01/18/dates-and-json.aspx
                // javascript += "\"\\/Date(" + ((java.util.Date)value).getTime() +  ")\\/\"";
	        	out.append(Struct.toDate((java.util.Date) value, jsonFormat));
	        } else if (value instanceof Struct.WriteJsonFormat) {
	        	((Struct.WriteJsonFormat) value).writeJsonFormat(out, jsonFormat); 
	        } else if (value instanceof Struct.ToJsonFormat) {
	        	out.append(((Struct.ToJsonFormat) value).toJson(jsonFormat));
	        } else if (value instanceof Struct.ToJson) {
	        	out.append(((Struct.ToJson) value).toJson());
	        } else {
	        	throw new RuntimeException("Cannot translate Java object " + value.getClass().getName() + " to javascript variable");
	        }
	        out.append(");");
	        
	        // out.print(javascript);
		} catch (IOException ex) {
			// ignore these errors, since they can occur when the user hits 'stop' in their browser
		} catch (Throwable t) {
			// WAS does not log exceptions that occur within tag libraries; log and rethrow
			t.printStackTrace();
			throw (JspException) new JspException("Exception occurred in ExtendJavascriptVarTag").initCause(t);
		}

        return BodyTag.SKIP_BODY; // this tag always has an empty body.
    }

    /** doEnd tag handler required to fulfill the Tag interface defined in the
     * <a href="http://java.sun.com/products/jsp/">JSP specification</a>.
     *
     * <p>This method does nothing, and always returns BodyTag.EVAL_PAGE
     *
     * @return BodyTag.EVAL_PAGE
     */
    public int doEndTag()
        throws javax.servlet.jsp.JspException
    {
        name = null;
        value = null;
        jsonFormat = null;

        return BodyTag.EVAL_PAGE;
    }
    

}
