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
 * Sets a javascript variable from a server-side resource or request attribute.
 * The variable may contain any amount of maps or lists, which will be
 * converted into the javascript equivalent.
 *
 * <p>Attributes defined for this tag (in common.tld) are:
 * <attributes>
 *   name - the name of the javascript variable to define
 *   value - the value of the javascript variable.
 * </attributes>
 *
 * <p>Both name and value may contain EL-style expressions.
 *
 * <p><i>e.g.</i> the following JSP snippet (taken from an early version of
 * messageList.jsp) retrieves the 'columns' List from
 * a request attribute, and sets it to a javascript variable of the same name:
 *
             <pre class="code">
             var columns = new Array();
             &lt;c:forEach var="i" varStatus="rowStatus" items="${columns}" >
               columns[&lt;c:out value='${rowStatus.index}'/>] = {
                 visible: &lt;c:out value='${i.visible}'/>,
                 name: "&lt;c:out value='${i.name}'/>",
                 width: &lt;c:out value='${i.width}'/> };
             &lt;/c:forEach>
             </pre>
 *
 * <p>... which produces output of the form:
 *
             <pre class="code">
             columns[0] = {
               visible: true,
               name: "externalMessageType",
               width: 91 };
             columns[1] = {
               visible: true,
               name: "queue",
               width: 89 };
             ...
             </pre>
 *
 * <p>This JSP snippet can be reproduced with the following tag:
 *
             <pre class="code">
             &lt;mm:setJavascriptVar name="columns" value="${columns}" />
             </pre>
 *
 * <p>... which produces the slightly more terse, but functionally equivalent:
 *
             <pre class="code">
             var columns = [{name: "externalMessageType",visible: true,width: 91}
               ,{name: "queue",visible: true,width: 89}
               ...
             ]
             ;
             </pre>
 *
 *  This tag also translates arbitrary levels of maps and lists within objects passed
 *  through to Javascript,
 *
 * <p><i>e.g.</i> this code sets a javascript variable 'x' to the value
 * of the 'y' request attribute
 *
            <pre class="code">
            &lt;mm:setJavascriptVar name="x" value="${y}" />
            </pre>
 *
 * <p>For a reasonably complex 'y', this would generate the following
 * HTML-embedded Javascript:
 *
            <pre class="code">
            var x = ['list-string-element-1', 'list-string-element2',
                     (key1: value1), (key2:value2),
                      1234, 4321 ]
            </pre>
 *
 * <p>The example above shows how string, map and numeric elements are
 * represented within a list object.
 *
 * @author  knoxg
 * 
 */
public class SetJavascriptVarTag
    extends BodyTagSupport
{
    /** Generated serialVersionUID */
	private static final long serialVersionUID = -7010835090281695598L;

    /** The javscript name */
    private String name;
    
    // * * The string entered in the value attribute of this tag */
    //private String valueString;
    
    /** The calculated value of the java object to embed */ 
    private Object value;

    /** The method in which dates are serialised to JSON */
    private String jsonFormat;
    
    
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

    /** Backwards-compatibility for old taglibs
     * @see #setJsonFormat(String)
     */
    public void setDateFormat(String dateFormat) {
        this.jsonFormat = dateFormat;
    }

    /** Backwards-compatibility for old taglibs 
     * @see #getJsonFormat()
     */
    public String getDateFormat() {
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
	        String javascript;
	        javascript = (name.indexOf(".")==-1 ? "var " : "") + name + " = ";
	        if (value == null) {
	            javascript += "null";
	        } else if (value instanceof String) {
	            javascript += "\"" + Text.escapeJavascript((String) value) + "\"";
	        } else if (value instanceof List) {
	            javascript += Struct.structuredListToJson((List) value, jsonFormat);
	        } else if (value instanceof Map) {
	            javascript += Struct.structuredMapToJson((Map) value, jsonFormat);
	        } else if (value instanceof Number) {
	            javascript += value;
	        } else if (value instanceof Boolean) {
	        	javascript += value;
	        } else if (value instanceof java.util.Date) {
            	// MS-compatible JSON encoding of Dates:
            	// see http://weblogs.asp.net/bleroy/archive/2008/01/18/dates-and-json.aspx
                // javascript += "\"\\/Date(" + ((java.util.Date)value).getTime() +  ")\\/\"";
	        	javascript += Struct.toDate((java.util.Date) value, jsonFormat);
	        } else if (value instanceof Struct.ToJson) {
	        	javascript += ((Struct.ToJson) value).toJson();
	        } else {
	        	throw new RuntimeException("Cannot translate Java object " +
	                value.getClass().getName() + " to javascript variable");
	        }

	        javascript += ";";
	        
	        JspWriter out = pageContext.getOut();
	        out.print(javascript);
		} catch (IOException ex) {
			// ignore these errors, since they can occur when the user hits 'stop' in their browser
		} catch (Throwable t) {
			// WAS does not log exceptions that occur within tag libraries; log and rethrow
			t.printStackTrace();
			throw (JspException) new JspException("Exception occurred in SetJavascriptVarTag").initCause(t);
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
