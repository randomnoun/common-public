package com.randomnoun.common.webapp.taglib;

/* (c) 2013 randomnoun. All Rights Reserved. This work is licensed under a
 * BSD Simplified License. (http://www.randomnoun.com/bsd-simplified.html)
 */

import java.io.*;
import java.text.*;
import java.util.*;

import javax.servlet.jsp.*;

import org.apache.log4j.Logger;

import com.randomnoun.common.Struct;
import com.randomnoun.common.ErrorList;
import com.randomnoun.common.Text;

/**
 * Render a dynamic HTML SELECT tag using information from a structured list.
 * 
 * <p>This class is intended to be used in a JSP 2.0 container, i.e. EL expressions
 * will have already been evaluated by the container before they reach this class.
 *
 * <p>Attributes defined for this tag (in common.tld) are:
 * <attributes>
 * name - the NAME element of the generated SELECT tag.
 * value - the current value of the SELECT tag.
 * data - a reference to a structured list containing both display and value columns,
 *   or a list of Strings. An empty list can be forced by setting an empty data attribute.
 * valueColumn - the name of the column in the list to use to populate OPTION values
 * displayColumn - the name of the column in the list to use to populate OPTION display text
 *   (will default to valueColumn if left blank)
 * firstOption - specifies a display value for the first option for a list; this
 *   option corresponds to a null value. Used to generate options like
 *   '(please select...)' at the top of a select box.
 * bundle - the resource bundle used to internationalise display column text
 * bundleFormat - a MessageFormat used to convert a value into a bundle key
 * </attributes>
 *
 * Only one of 'value' and 'valueFromRequest' can be set.
 *
 * <p>Additional attributes that are passed directly through to HTML:
 * See {@link StandardHtmlTag} for a list of additional attributes which
 * will be directly generated as HTML.
 *
 * 
 *
 */
public class SelectTag
    extends StandardHtmlTag
{
    
    

    /** Logger for this class */
    private final static Logger logger = Logger.getLogger(SelectTag.class);

    /** The name of the SELECT tag. */
    private String name;

    /** The current value of the SELECT tag. */
    private String value;

    /** The value of the 'data' attribute */
    private String dataString;

    /** A reference to a structured list (evaluated from dataString). */
    private List data;

    /** The value of the 'bundle' attribute */
    private String bundleString;

    /** The value of the 'bundlePattern' attribute */
    private String bundleFormat;

    /** A reference to a resource bundle (evaluated from bundleString). */
    private ResourceBundle bundle;

    /** The name of the column in the list to use to populate OPTION values */
    private String valueColumn;

    /** The name of the column in the list to use to populate OPTION display text */
    private String displayColumn;

    /** Some display text for a first OPTION of the SELECT tag */
    private String firstOption;
    
    /** If non-null, and the displayValue or value to be displayed is a Date, will format the 
     * value with a SimpleDateFormatter object with this pattern
     */
    private String formatDate;
    
    /* ***********************************************************************
     * Tag Library attribute methods
     */

    /** Sets the name of the SELECT tag.
     *  @param value the name of the SELECT tag.
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /** Retrieves the name of the SELECT tag.
     *  @return   the name of the SELECT tag.
     */
    public String getName()
    {
        return name;
    }

    /** Sets the current value of the SELECT tag.
     *  @param value the current value of the SELECT tag.
     */
    public void setValue(String value)
    {
        this.value = value;
    }

    /** Retrieves the current value of the SELECT tag.
     *  @return   the current value of the SELECT tag.
     */
    public String getValue()
    {
        return value;
    }

    /** Sets the structured list used to populate the SELECT tag.
     *  @param value the structured list used to populate the SELECT tag.
     */
    public void setData(Object data)
    {
        this.data = (List) data;
    }

    /** Retrieves the structured list used to populate the SELECT tag.
     *  @return   the structured list used to populate the SELECT tag.
     */
    public Object getData()
    {
        return dataString;
    }

    /** Sets the resource bundle used to internationalise display column text
     *  @param value the resource bundle used to internationalise display column text
     */
    public void setBundle(String bundleString)
    {
        this.bundleString = bundleString;
    }

    /** Retrieves the resource bundle used to internationalise display column text
     *  @return   the resource bundle used to internationalise display column text
     */
    public String getBundle()
    {
        return bundleString;
    }

    /** Sets how the resource bundle will be used to retrieve i18n text
     *  @param value how the resource bundle will be used to retrieve i18n text
     */
    public void setBundleFormat(String bundleFormat)
    {
        this.bundleFormat = bundleFormat;
    }

    /** Retrieves how the resource bundle will be used to retrieve i18n text
     *  @return   how the resource bundle will be used to retrieve i18n text
     */
    public String bundlePattern()
    {
        return bundleFormat;
    }

    /** Sets the column name in the 'data' structured list used to represent
     *    OPTION values in the generated SELECT HTML.
     *  @param value the column name in the 'data' structured list used to represent
     *    OPTION values in the generated SELECT HTML.
     */
    public void setValueColumn(String valueColumn)
    {
        this.valueColumn = valueColumn;
    }

    /** Retrieves the column name in the 'data' structured list used to represent
     *    OPTION values in the generated SELECT HTML.
     *  @return   the column name in the 'data' structured list used to represent
     *    OPTION values in the generated SELECT HTML.
     */
    public String getValueColumn()
    {
        return valueColumn;
    }

    /** Sets the column name in the 'data' structured list used to represent
     *    OPTION values in the generated SELECT HTML.
     *  @param value the column name in the 'data' structured list used to represent
     *    OPTION values in the generated SELECT HTML.
     */
    public void setDisplayColumn(String displayColumn)
    {
        this.displayColumn = displayColumn;
    }

    /** Retrieves the column name in the 'data' structured list used to represent
     *    OPTION display text in the generated SELECT HTML.
     *  @return   the column name in the 'data' structured list used to represent
     *    OPTION display text in the generated SELECT HTML.
     */
    public String getDisplayColumn()
    {
        return displayColumn;
    }

    /** Sets the display text for a first OPTION of the SELECT tag.
     *  @param value the display text for a first OPTION of the SELECT tag.
     */
    public void setFirstOption(String firstOption)
    {
        this.firstOption = firstOption;
    }

    /** Retrieves the display text for a first OPTION of the SELECT tag.
     *  @return   the display text for a first OPTION of the SELECT tag.
     */
    public String getFirstOption()
    {
        return firstOption;
    }
    
    /** Sets the date pattern to use to format Date objects
     *  @param value the date pattern to use to format Date objects
     */
    public void setFormatDate(String formatDate)
    {
        this.formatDate = formatDate;
    }

    /** Retrieves the date pattern to use to format Date objects
     *  @return   the date pattern to use to format Date objects
     */
    public String getFormatDate()
    {
        return formatDate;
    }    

    /** Sets the name of the multiple attribute.
     *  @param value the name of the multiple attribute.
     */
    public void setMultiple(String multiple)
    {
    	this.attributes.put("multiple", multiple);
    }

    /** Retrieves the name of the multiple attribute.
     *  @return   the name of the multiple attribute.
     */
    public String getMultiple()
    {
    	return (String) attributes.get("multiple");
    }

    
    
    

    /* ***********************************************************************
     * End of tag library attribute methods
     */

    /** Start custom taglibrary processing. Sets internal private variables based on
     *  HttpSession values and defaults, and emits the generating HTML. The body
     *  of this tag will not be evaluated, as declared in the the .tld file.
     *
     *  @todo escape HTML attributes and values
     *
     *  @return This method always returns TagSupport.SKIP_BODY, as required by the
     *   JSP Taglib specification for empty tags
     */
    public int doStartTag()
        throws JspException
    {
        try {
        	SimpleDateFormat sdf = null;
            evaluateAttributes();
            if (formatDate!=null) {
            	sdf = new SimpleDateFormat(formatDate);
            }
            JspWriter out = pageContext.getOut();

            // use value column for display if not explicitly set
            if (displayColumn == null) {
                displayColumn = valueColumn;
            }

            // remove quotes from name
            if (name.startsWith("\"")) {
                name = name.substring(1);
            }
            if (name.endsWith("\"")) {
                name = name.substring(0, name.length() - 1);
            }

            // determine styling based on whether this field is in error
            ErrorList errors = null;
            try {
                errors = (ErrorList)pageContext.getAttribute("errors",
                  PageContext.REQUEST_SCOPE);
            } catch (ClassCastException cce) {
                // just ignore these.
            }

            String fieldStyle;
            boolean hasError = false;
            fieldStyle = "inputField";
            if (errors != null && errors.hasErrorOn(name)) {
                fieldStyle = "inputFieldError";
                hasError = true;
            }
            if (attributes.containsKey("class")) {
                attributes.put("class", fieldStyle + " " + attributes.get("class"));
            } else {
                attributes.put("class", fieldStyle);
            }

            // generate the HTML
            if (hasError) {
                out.print("<span class=\"inputFieldError\">");
            }
            out.print("<select name=\"" + name + "\" id=\"" + name + "\" " + this.getAttributeString() + ">");
            if (firstOption != null) {
                out.println("<option value=\"\">" + Text.escapeHtml(firstOption) +
                    "</option>\n");
            }

            // generate select options
            if (data != null) {
                for (Iterator i = data.iterator(); i.hasNext();) {
                    Object obj = i.next();
                    Object displayObj = null;
                    Object valueObj = null;
                    String valueText = "";
                    String displayText = null;

                    // could generalise the first two cases here
                    if (obj instanceof Map) {
                        Map row = (Map) obj;
                        if (row.get(displayColumn) != null) {
                        	displayObj = row.get(displayColumn);
                        	if (displayObj instanceof Date && sdf!=null) {
                            	displayText = sdf.format((Date) displayObj);
                            } else {
                            	displayText = displayObj.toString();
                            }
                        }
                        
                        valueObj = row.get(valueColumn);
                        if (valueObj == null) {
                            throw new IllegalStateException(
                                "Null value encountered in SelectTag data '" + dataString + "'");
                        }
                        if (valueObj instanceof Date && sdf!=null) {
                        	valueText = sdf.format((Date) valueObj);
                        } else {
                        	valueText = valueObj.toString();
                        }
                        if (displayText == null) {
                            displayText = valueText;
                        }
                    } else if (displayColumn!=null && valueColumn!=null) {
                    	valueObj = Struct.getValue(obj, valueColumn);
                    	displayObj = (Struct.getValue(obj, displayColumn));
                        if (valueObj == null) {
                            throw new IllegalStateException(
                                "Null value encountered in SelectTag data '" + dataString + "'");
                        }
                    	if (valueObj instanceof Date && sdf!=null) {
                        	valueText = sdf.format((Date) valueObj);
                        } else {
                        	valueText = valueObj.toString();
                        }
                    	if (displayObj instanceof Date && sdf!=null) {
                        	displayText = sdf.format((Date) displayObj);
                        } else {
                        	displayText = displayObj.toString();
                        }
                	} else {
                    	if (obj instanceof Date && sdf!=null) {
                        	valueText = sdf.format((Date) obj);
                        } else {
                        	valueText = obj.toString();
                        }
                        displayText = valueText;
                    }

                    if (bundle != null) {
                        String bundleKey = displayText;
                        if (bundleFormat != null) {
                            bundleKey = MessageFormat.format(bundleFormat,
                                    new Object[] { displayText });
                        }

                        try {
                            displayText = bundle.getString(bundleKey);
                        } catch (MissingResourceException mre) {
                            // just use the bundle key in this case
                            // displayText = "???" + bundleKey + "???";
                            displayText = bundleKey;
                        }
                    }

                    out.print("<option value=\"" + Text.escapeHtml(valueText) + "\"" +
                        (valueText.equals(value) ? " selected" : "") + ">" +
                        Text.escapeHtml(displayText) + "</option>\n");
                }
            }

            out.print("</select>\n");

            if (hasError) {
                out.print("</span>");
            }
        } catch (IOException ex) {
            // ignore these errors, since they can occur when the user hits 'stop' in their browser
		} catch (Throwable t) {
			// WAS does not log exceptions that occur within tag libraries; log and rethrow
			t.printStackTrace();
			throw (JspException) new JspException("Exception occurred in SelectTag").initCause(t);
		}

        // selectTag element contents are not evaluated 
        return SKIP_BODY;
    }

    /** End of custom tag library processing. This method performs no work.
     *
     *  @return This tag always returns TagSupport.SKIP_BODY .
     **/
    public int doEndTag()
    {
        // reset attributes
        id = null;
        name = null;
        value = null;
        dataString = null;
        data = null;
        valueColumn = null;
        displayColumn = null;
        bundle = null;
        bundleFormat = null;
        formatDate = null;
        clearAttributes();

        return SKIP_BODY;
    }
    
	/** Return a localised list for use in a mm:select tag. This function returns a list of 
	 * Maps, each element of which is a single option in the select tag. The map contains
	 * two attributes: 'text' and 'value', containing the internationalised and 
	 * non-internationalised versions of each option. Note that some select boxes are
	 * not internationalised (e.g. 'selectTop').   
	 * 
	 * @param bundle The bundle to retrieve localisation information form.
	 * @param prefix A prefix used to return information from the bundle
	 * @param options A comma-separated list of option values
	 * 
	 * @return A List as documented above
	 */  
	public static List getSelectOptions(ResourceBundle bundle, String prefix, String options) {
		List optionsList;
		try {
			optionsList = Text.parseCsv(options);
		} catch (ParseException pe) {
			throw new IllegalArgumentException("Invalid options list '" + options + "'");
		}
		List list = new ArrayList();
		for (Iterator i = optionsList.iterator(); i.hasNext(); ) {
			String option = (String) i.next();
			Map map = new HashMap();
			map.put("value", option);
			if (bundle!=null) {
				map.put("text", bundle.getString(prefix + option));  // localisation
			} else {
				map.put("text", option);  // no localisation                
			}
			list.add(map);
		}
		return list;
	}
    
    
}
