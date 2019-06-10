package com.randomnoun.common;

/* (c) 2013 randomnoun. All Rights Reserved. This work is licensed under a
 * BSD Simplified License. (http://www.randomnoun.com/bsd-simplified.html)
 */

import java.text.ParseException;
import java.util.*;

import javax.servlet.http.HttpServletRequest;

/**
 * The ErrorList class is a a fairly generic class for containing validation errors for
 * input forms, similar to the struts ActionErrors class. Each error within this class can
 * contain a 'short' and 'long' description, where the short description is normally a 
 * categorisation of the error and the longer description describes what has happened and
 * how to fix the problem; (e.g. shortText="Missing field", 
 * longText="The field 'id' is mandatory. Please enter a value for this field."). The short
 * description is normally rendered by <tt>errorHeader.jsp</tt> in bold before the long
 * description. An individual error also may contain a list of fields that it applies to
 * (e.g. a 'mandatory inclusive' error may apply to several fields at once), and a
 * severity (normally set to {@link #SEVERITY_INVALID}. Errors are displayed by the
 * <tt>errorHeader.jsp</tt> JSP, which is typically included at the top of any page
 * that contains an input form. An error without a severity supplied is assumed to be 
 * of SEVERITY_INVALID, and an error without any fields supplied is assumed to be
 * associated with the entire form, rather than a specific set of fields.
 * 
 * <p>Errors are inserted into an ErrorList by using one of the addError methods:
 * <ul>
 * <li> {@link #addError(String, String)} - add an invalid form error
 * <li> {@link #addError(String, String, int)} - add an form error with a specific severity
 * <li> {@link #addError(String, String, String)} - add an invalid field error  
 * <li> {@link #addError(String, String, String, int)} - add an invalid field error with a specific severity 
 * </ul>
 * 
 * <p>Field-level errors are detected by the <tt>&lt;mm:input&gt;</tt> and <tt>&lt;mm:select&gt;</tt>
 * tag libraries to alert the user that the fields are in error.
 * 
 * <p>Code that uses an ErrorList to perform validation may 'attach' an object to the
 * errorList instance to perform standard validations and to localise error messages. This
 * information can be used by other helper classes to extract field names and data from
 * complex objects. In common, methods to perform standard validation are contained 
 * in the {@link com.randomnoun.common.struts.ActionBase} class (which can be
 * used as a superclass of any struts action). 
 * 
 * <p>EJBs that wish to pass information back to the presentation layer should use
 * ValidationExceptions (which take an ErrorList as an argument).
 *
 * @author knoxg
 * @version <tt>$Id$</tt>
 */
public class ErrorList extends ArrayList<ErrorList.ErrorData>
    implements java.io.Serializable
{
    
    

    /** Severity level indicating 'not an error' (e.g. informational only). Used to indicate successful operations */
    public static final int SEVERITY_OK = 0; // Not an error

    /** invalid user-supplied data; end-user can fix situation */
    public static final int SEVERITY_INVALID = 1;

    /** invalid system-supplied data; end-user cannot fix situation */
    public static final int SEVERITY_ERROR = 2;

    /** internal error (e.g. EJB/LDAP connection failure) */
    public static final int SEVERITY_FATAL = 3;

    /** unrecoverable internal error (can't think of anything here, but the world ending could be one */
    public static final int SEVERITY_PANIC = 4;

    // these were created after the 5 above, which is why they have higher ID numbers
    // at a later stage will renumber these so that
    // OK < INFO < WARNING < INVALID < ERROR < FATAL < PANIC
    
    /** information message; can be used in addition to SEVERITY_OK for additional text */
    public static final int SEVERITY_INFO = 5;

    /** possibly incorrect user-supplied data; operation still succeeds but may return incorrect results */
    public static final int SEVERITY_WARNING = 6;

    
    // the thing we're validating
    private transient Object attachedObject;

    // contains error messages
    private transient ResourceBundle attachedBundle;
    private transient String bundleFormat;
    private transient String attachedFieldFormat;
    private transient Locale locale;

    /** ErrorInfo inner class - contains information related to
     *  a single error
     */
    public static class ErrorData
        extends HashMap
    {
        /**
         * Creates a new ErrorInfo object.
         *
         * @param shortText a short, descriptive string for this error
         * @param longText  a more lengthy description of this error
         * @param errorField    comma-separated list of field names that caused
         *                    this error
         * @param severity    the severity of this error
         */
        public ErrorData(String shortText, String longText, String errorField,
            int severity)
        {
            super();
            put("shortText", shortText);
            put("longText", longText);
            put("field", errorField);
            put("severity", new Integer(severity));
        }

        /** Retrieves the type for this error
         *  @return   the type for this error
         */
        public String getShortText()
        {
            return (String)get("shortText");
        }

        /** Retrieves the description for this error
         *  @return   the description for this error
         */
        public String getLongText()
        {
            return (String)get("longText");
        }

        /** Retrieves the description for this error, with newlines converted to <br/>s. 
         *   When displaying with &lt;c:out&gt;, set the escapeXml attribute to false
         *   
         *  @return   the description for this error
         */
        public String getLongTextWithNewlines()
        {
        	String longText = (String) get("longText");
        	longText=Text.escapeHtml(longText);
        	longText=Text.replaceString(longText, "\n", "<br/>");
            return longText;
        }

        
        /** Retrieves a comma-separated list of fields that caused this error
         *  @return   a comma-separated list of fields that caused this error
         */
        public String getField()
        {
            return (String)get("field");
        }

        /** Retrieves the severity of this error
         *  @return   the severity of this error
         */
        public int getSeverity()
        {
            return ((Integer)get("severity")).intValue();
        }

        /** Retrieves a string representation of this error
         *  @return   a string representation of this error
         */
        public String toString()
        {
            return "{shortText='" + getShortText() + "', longText='" + getLongText() +
              "', fields='" + getField() + "', severity=" + getSeverity() + "}";
        }
    }

    /**
     * Create a new, empty ErrorData object.
     */
    public ErrorList()
    {
        super();
    }

    /**
     * Adds an error. This is the "real" addError() method; all others delegate to this
     * one.
     *
     * @param errorField  a comma-separated list of field names that caused this error
     * @param shortText a short string conveying the error type (e.g. 'Missing field')
     * @param longText  a longer string describing the nature of the error and how to resolve it
     *   (e.g. 'The field 'id' is mandatory. Please enter a value for this field.')
     * @param severity    the severity of this error (one of the SEVERITY_* constants of this class).
     *
     * @see SEVERITY_INVALID
     */
    public void addError(String errorField, String shortText, String longText,
        int severity)
    {
        // if we have an attachedFieldFormat, then format each field to this format
        if (attachedFieldFormat != null && errorField != null) {
            try {
                List fields = Text.parseCsv(errorField);
                errorField = "";
                for (Iterator i = fields.iterator(); i.hasNext();) {
                    String field = (String)i.next();
                    errorField = errorField + getFieldName(field) +
                        (i.hasNext() ? "," : "");
                }
            } catch (ParseException pe) {
                throw (IllegalArgumentException)new IllegalArgumentException(
                    "Invalid errorField list '" + errorField + "'").initCause(pe);
            }
        }
        super.add(new ErrorData(shortText, longText, errorField, severity));
    }

    /**
     * As per {@link #addError(String, int, String, String, int)}, with a default
     * severity of {@link #SEVERITY_ERROR}.
     *
     * @param errorField  a comma-separated list of field names that caused this error
     * @param shortText a short string conveying the error type (e.g. 'Missing field')
     * @param longText  a longer string describing the nature of the error and how to resolve it
     *   (e.g. 'The field 'id' is mandatory. Please enter a value for this field.')
     */
    public void addError(String errorField, String shortText, String longText)
    {
        addError(errorField, shortText, longText, SEVERITY_ERROR);
    }

    /**
     * Adds an error that isn't associated with any particular field
     *
     * @param shortText a short string conveying the error type (e.g. 'Missing field')
     * @param longText  a longer string describing the nature of the error and how to resolve it
     *   (e.g. 'The field 'id' is mandatory. Please enter a value for this field.')
     * @param severity  the severity of this error (one of the SEVERITY_* constants of this class)
     *
     */
    public void addError(String shortText, String longText, int severity)
    {
        addError("", shortText, longText, severity);
    }

    /**
     * Adds an error that isn't associated with any particular field, with a default
     * severity of {@link #SEVERITY_ERROR}.
     *
     * @param shortText a short string conveying the error type (e.g. 'Missing field')
     * @param longText  a longer string describing the nature of the error and how to resolve it
     *   (e.g. 'The field 'id' is mandatory. Please enter a value for this field.')
     */
    public void addError(String shortText, String longText)
    {
        addError("", shortText, longText, SEVERITY_ERROR);
    }

    /**
     * Resets all errors contained within this object
     */
    public void clearErrors()
    {
        super.clear();
    }

    /**
     * Appends the errors contained within another ErrorData into this one.
     *
     * @param errorList
     *
     * @return <b>true</b> if there were any additional errors to embed and at least
     *   one of the errors had a severity of SEVERITY_ERROR or higher (worse),
     *   <b>false</b> otherwise.
     */
    public boolean addErrors(ErrorList errorList)
    {
        if (errorList == null) {
            return false;
        }
        if (errorList.size() == 0) {
            return false;
        }
        int maxSeverity = Integer.MIN_VALUE;
        int severity;

        for (int i = 0; i < errorList.size(); i++) {
            severity = errorList.getSeverityAt(i);
            add(errorList.get(i));
            if (severity > maxSeverity) {
                maxSeverity = severity;
            }
        }

        if (maxSeverity >= SEVERITY_ERROR) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Returns true if an error has occured on the CGI field
     * passed as a parameter to this method
     *
     * @return true if an error occured, false if not.
     */
    public boolean hasErrorOn(String field)
    {
        ErrorData errorInfo;
        boolean hasError = false;

        for (Iterator i = super.iterator(); i.hasNext(); ) {
            errorInfo = (ErrorData)i.next();
            if (errorInfo.getField()!=null) {
	            String[] errorFields = errorInfo.getField().split(",");
	            for (int j = 0; j < errorFields.length; j++) {
	                if (errorFields[j].equals(field)) {
	                    return true;
	                }
	            }
            }
        }
        return false;
    }

    /**
     * Returns true if there are any errors in this object
     *
     * @return True if the number of errors > 0
     */
    public boolean hasErrors()
    {
        return size() > 0;
    }

    /**
     * Returns true if there are any errors of the specified severity or higher
     *
     * @param severity a SEVERITY_* constant
     *
     * @return True if the number of errors at the specified severity or higher > 0
     */
    public boolean hasErrors(int severity)
    {
        for (Iterator i = this.iterator(); i.hasNext(); ){
            ErrorData errorData = (ErrorData) i.next();
            if (errorData.getSeverity() >= severity ) { 
                return true;
            }
        }
        return false;
    }


    /**
     * Returns the maximum severity of all current errors.
     *
     * @return the severity ranking of the most severe error, or -1 if
     *   there are no errors contained within this ErrorData object.
     */
    public int maxErrorSeverity()
    {
        int maxSeverity = -1;
        int curSeverity;

        for (int i = 0; i < size(); i++) {
            curSeverity = getSeverityAt(i);
            if (curSeverity > maxSeverity) {
                maxSeverity = curSeverity;
            }
        }
        return maxSeverity;
    }

    /**
     * Returns the number of errors in this object
     *
     * @return The number of errors in this object
     */
    public int size() {
        return super.size();
    }

    /**
     * Same as {@link #size()}. Only here to allow us to access the object in JSTL
     *
     * @return The number of errors in this object
     */
    public int getSize() {
        return size();
    }

    /**
     * Returns the shortText string of the pos'th error
     */
    public String getShortTextAt(int pos) {
        return ((ErrorData)super.get(pos)).getShortText();
    }

    /**
     * Returns the longText of the pos'th error
     */
    public String getLongTextAt(int pos) {
        return ((ErrorData)super.get(pos)).getLongText();
    }

    /**
     * Returns the field of the pos'th error
     */
    public String getFieldAt(int pos) {
        return ((ErrorData)super.get(pos)).getField();
    }

    /**
     * Returns the severity of the pos'th error
     */
    public int getSeverityAt(int pos) {
        return ((ErrorData)super.get(pos)).getSeverity();
    }

    /**
     * Return all the errors within this object in a single string.
     * Suitable for inclusion within email alarms, logs etc...
     *
     * @return Newline-separated list of errors
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        ErrorData errorData;
        for (Iterator i = super.iterator(); i.hasNext(); ) {
            errorData = (ErrorData)i.next();

            sb.append("#");
            sb.append(errorData.getShortText() + " - ");
            sb.append(errorData.getLongText());
            if (!Text.isBlank(errorData.getField())) {
            	sb.append(" [" + errorData.getField() + "]\n");
            }
        }
        return sb.toString();
    }

    /** Attaches an object to be validated to this ErrorList.
     *
     * @param attachedObject a POJO, a HttpServletRequest, or a Map
     *   (e.g. request.getParameterMap()). It identifies  where the validated data
     *   is coming from.
     * @param attachedBundle a bundle where the field names for this object are
     *   to be retrieved from.
     * @param bundleFormat a MessageFormat used to to retrieve field names from
     *   attachedBundle. The "{0}" placeholder in this String is replaced with
     *   the name of the field being validated.
     */

    // first parameter of this method can be 
    // second method 
    public void setValidatedObject(Object attachedObject, String attachedFieldFormat,
        ResourceBundle attachedBundle, String bundleFormat, Locale locale)
    {
        if (attachedObject==null) { throw new NullPointerException("null attachedObject"); }
        if (attachedFieldFormat==null) { throw new NullPointerException("null fieldFormat"); }
        if (attachedBundle==null) { throw new NullPointerException("null attachedBundle"); } 
        if (bundleFormat==null) { throw new NullPointerException("null bundleFormat"); }
        if (locale==null) { throw new NullPointerException("null locale"); }

        this.attachedObject = attachedObject;
        this.attachedFieldFormat = attachedFieldFormat;
        this.attachedBundle = attachedBundle;
        this.bundleFormat = bundleFormat;
        this.locale = locale;
    }

    public void resetValidatedObject()
    {
        this.attachedObject = null;
        this.attachedFieldFormat = null;
        this.attachedBundle = null;
        this.bundleFormat = null;
    }

    /**
     * The object containing the data being validated
     *
     * @return the object containing the data being validated
     */
    public Object getObject()
    {
        return attachedObject;
    }
    
    /**
     * The locale in which validation messages will be localised
     *  
     * @return The locale in which validation messages will be localised
     */
    public Locale getLocale()
    {
        return locale;
    }

    /** Returns the value of a field from the attached object
     * 
     * @param name field name
     * 
     * @return field value
     */
    public String getFieldValue(String name) {
    	// use reflection
        if (attachedObject instanceof HttpServletRequest) {
            return ((HttpServletRequest)attachedObject).getParameter(name);
        }

        Object obj = Struct.getValue(attachedObject, name);
        if (obj == null) {
            return null;
        } else if (obj instanceof String[]) {
            return ((String[])obj)[0];
        } else if (obj instanceof String) {
            return (String)obj;
        } else {
            throw new IllegalStateException("Cannot retrieve non-string value '" + name +
                "' (found class " + obj.getClass().getName() + ")");
        }
    }

    /**
     * Returns the bundle used for localising validation messages 
     *
     * @return the bundle used for localising validation messages
     */
    public ResourceBundle getBundle()
    {
        return attachedBundle;
    }

    /**
     * Returns a localised form of the supplied field name, to be used within
     * generic validation messages
     *
     * @param field field name
     *
     * @return localised form of field name
     */
    public String getLocalisedFieldName(String field)
    {
        return attachedBundle.getString(Text.replaceString(bundleFormat, "{0}", field));
    }

    public String getFieldName(String field)
    {
        if (attachedFieldFormat == null) {
            return field;
        } else {
            return Text.replaceString(attachedFieldFormat, "{0}", field);
        }
    }

    /** Removes validation metadata from this object 
     * (i.e. attached objects, bundles, bundleFormats)
     *  
     */
    public void unattach()
    {
        this.attachedObject = null;
        this.attachedBundle = null;
        this.bundleFormat = null;
    }

    
    /** Return the JSON representation of this object
     *  
     * @return the JSON representation of this object
     */
    public String toJSON() {
        StringBuffer sb = new StringBuffer();
        ErrorList.ErrorData errorData;
        sb.append("[");
        for (int i=0; i<this.size(); i++) {
        	errorData = (ErrorData) get(i);
        	if (i>0) { sb.append(","); }
        	sb.append(
        	  "{\"shortText\":\"" + Text.escapeJavascript2(errorData.getShortText()) + "\"," +
        	  "\"longText\":\"" + Text.escapeJavascript2(errorData.getLongText()) + "\"," +
        	  "\"fields\":\"" + Text.escapeJavascript2(errorData.getField()) + "\"," +
        	  "\"severity\":" + errorData.getSeverity() + "}");
        }
        sb.append("]");
        return sb.toString();
    }

}
