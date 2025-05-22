package com.randomnoun.common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

/**
 * The ErrorList class is a a fairly generic class for containing validation errors for
 * input forms, similar to the struts ActionErrors class. 
 * 
 * <p>Each error within this class can contain a 'short' and 'long' description, 
 * where the short description is normally two-word categorisation of the error 
 * and the longer description describes what has happened and how to fix the problem; 
 * (e.g. shortText="Missing field", longText="The field 'id' is mandatory. Please enter a value for this field."). 
 * 
 * <p>In the UI, the short description is normally rendered in bold before the long
 * description. 
 * 
 * <p>An individual error also may contain a severity, 
 * and list of fields that it applies to (defined by a comma-separate string of field names)
 * An error without a severity supplied is assumed to be {@link #SEVERITY_INVALID}, 
 * and an error without any fields supplied is assumed to be associated with the entire form, 
 * rather than a specific set of fields.
 * 
 * <p>Errors are inserted into an ErrorList by using one of the addError methods:
 * <ul>
 * <li> {@link #addError(String, String)} - add an invalid form error
 * <li> {@link #addError(String, String, int)} - add an form error with a specific severity
 * <li> {@link #addError(String, String, String)} - add an invalid field error  
 * <li> {@link #addError(String, String, String, int)} - add an invalid field error with a specific severity 
 * </ul>
 * 
 * @author knoxg
 */
public class ErrorList implements List<ErrorList.ErrorData>, Serializable {

	/** Generated serialVersionUID */
	private static final long serialVersionUID = 4246736116021572339L;
	
	// uses an ArrayList by default, but by calling makeThreadsafe(), will wrap the backing array 
	// in a Collections.synchronizedList()
	List<ErrorList.ErrorData> delegate;
	boolean threadsafe = false;
	
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

    
    /** ErrorInfo inner class - contains information related to
     *  a single error
     */
    public static class ErrorData extends HashMap<String, Object>
    {
        /** Generated serialVersionUID */
		private static final long serialVersionUID = -176737615399095851L;

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
            put("severity", Integer.valueOf(severity));
        }

        @Override
		public int hashCode() {
			return super.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			return super.equals(obj);
		}

		/** Retrieves the type for this error
         *  @return   the type for this error
         */
        public String getShortText()
        {
            return (String) get("shortText");
        }

        /** Retrieves the description for this error
         *  @return   the description for this error
         */
        public String getLongText()
        {
            return (String) get("longText");
        }

        /** Retrieves a comma-separated list of fields that caused this error
         *  @return   a comma-separated list of fields that caused this error
         */
        public String getField()
        {
            return (String) get("field");
        }

        /** Retrieves the severity of this error
         *  @return   the severity of this error
         */
        public int getSeverity()
        {
            return ((Integer) get("severity")).intValue();
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
        delegate = new ArrayList<>();
    }
    
    
    /** Convert the backing store for this ErrorList to a Collections.synchronizedList, suitable for use in multithreaded applications.
     * Note that iterations on this collection must still be synchronised.
     */
    public synchronized void makeThreadsafe() {
    	if (!threadsafe) {
    		delegate = Collections.synchronizedList(delegate);
    		threadsafe = true;
    	}
    }
    
    /** Removes any duplicate errors in this ErrorList */
    public void removeDuplicates() {
        Set<ErrorList.ErrorData> uniqueData = new HashSet<>();
        synchronized(delegate) {
	        for (Iterator<ErrorList.ErrorData> i = this.iterator(); i.hasNext(); ) {
	        	ErrorList.ErrorData ed = i.next();
	            if (uniqueData.contains(ed)) {
	            	i.remove();
	            } else {
	                uniqueData.add(ed);
	            }
	        }
        }
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
        delegate.add(new ErrorData(shortText, longText, errorField, severity));
    }

    /**
     * As per {@link #addError(String, String, String, int)}, with a default
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
        delegate.clear();
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
     * Returns true if an error has occured on the field
     * passed as a parameter to this method
     *
     * @return true if an error occured, false if not.
     */
    public boolean hasErrorOn(String field)
    {
        ErrorData errorInfo;

        synchronized(delegate) {
	        for (Iterator<ErrorData> i = delegate.iterator(); i.hasNext(); ) {
	            errorInfo = i.next();
	            if (errorInfo.getField()!=null) {
		            String[] errorFields = errorInfo.getField().split(",");
		            for (int j = 0; j < errorFields.length; j++) {
		                if (errorFields[j].equals(field)) {
		                    return true;
		                }
		            }
	            }
	        }
        }
        return false;
    }

    /**
     * Returns true if there are any errors in this object
     *
     * @return True if the number of errors &gt; 0
     */
    public boolean hasErrors()
    {
        return size() > 0;
    }

    /**
     * Returns true if there are any errors of the specified severity or higher
     * Note that our severities aren't in increasing severity order any more so this method is now deprecated
     *
     * @param severity a SEVERITY_* constant
     *
     * @return True if the number of errors at the specified severity or higher &gt; 0
     * @deprecated
     */
    public boolean hasErrors(int severity)
    {
    	synchronized(delegate) {
	        for (Iterator<ErrorData> i = this.iterator(); i.hasNext(); ){
	            ErrorData errorData = (ErrorData) i.next();
	            if (errorData.getSeverity() >= severity ) { 
	                return true;
	            }
	        }
    	}
        return false;
    }


    /**
     * Returns the maximum severity of all current errors.
     * Note that our severities aren't in increasing severity order any more so this method is now deprecated
     *
     * @return the severity ranking of the most severe error, or -1 if
     *   there are no errors contained within this ErrorData object.
     * @deprecated  
     */
    public int maxErrorSeverity()
    {
        int maxSeverity = -1;
        int curSeverity;

        synchronized(delegate) {
	        for (int i = 0; i < size(); i++) {
	            curSeverity = getSeverityAt(i);
	            if (curSeverity > maxSeverity) {
	                maxSeverity = curSeverity;
	            }
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
        return delegate.size();
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
        return ((ErrorData) delegate.get(pos)).getShortText();
    }

    /**
     * Returns the longText of the pos'th error
     */
    public String getLongTextAt(int pos) {
        return ((ErrorData) delegate.get(pos)).getLongText();
    }

    /**
     * Returns the field of the pos'th error
     */
    public String getFieldAt(int pos) {
        return ((ErrorData) delegate.get(pos)).getField();
    }

    /**
     * Returns the severity of the pos'th error
     */
    public int getSeverityAt(int pos) {
        return ((ErrorData) delegate.get(pos)).getSeverity();
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
        synchronized(delegate) {
	        for (Iterator<ErrorData> i = delegate.iterator(); i.hasNext(); ) {
	            errorData = (ErrorData)i.next();
	
	            sb.append("#");
	            sb.append(errorData.getShortText() + " - ");
	            sb.append(errorData.getLongText());
	            if (!Text.isBlank(errorData.getField())) {
	            	sb.append(" [" + errorData.getField() + "]\n");
	            }
	        }
        }
        return sb.toString();
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
        	  "{\"shortText\":\"" + Text.escapeJavascript(errorData.getShortText()) + "\"," +
        	  "\"longText\":\"" + Text.escapeJavascript(errorData.getLongText()) + "\"," +
        	  "\"fields\":\"" + Text.escapeJavascript(errorData.getField()) + "\"," +
        	  "\"severity\":" + errorData.getSeverity() + "}");
        }
        sb.append("]");
        return sb.toString();
    }

    /** Delegate methods */
    

    public void forEach(Consumer<? super ErrorData> action) {
		delegate.forEach(action);
	}

	public boolean isEmpty() {
		return delegate.isEmpty();
	}

	public boolean contains(Object o) {
		return delegate.contains(o);
	}

	public Iterator<ErrorData> iterator() {
		return delegate.iterator();
	}

	public Object[] toArray() {
		return delegate.toArray();
	}

	public <T> T[] toArray(T[] a) {
		return delegate.toArray(a);
	}

	public boolean add(ErrorData e) {
		return delegate.add(e);
	}

	public boolean remove(Object o) {
		return delegate.remove(o);
	}

	public boolean containsAll(Collection<?> c) {
		return delegate.containsAll(c);
	}

	public boolean addAll(Collection<? extends ErrorData> c) {
		return delegate.addAll(c);
	}

	public boolean addAll(int index, Collection<? extends ErrorData> c) {
		return delegate.addAll(index, c);
	}

	public boolean removeAll(Collection<?> c) {
		return delegate.removeAll(c);
	}

	public boolean retainAll(Collection<?> c) {
		return delegate.retainAll(c);
	}

	public void replaceAll(UnaryOperator<ErrorData> operator) {
		delegate.replaceAll(operator);
	}

	public boolean removeIf(Predicate<? super ErrorData> filter) {
		return delegate.removeIf(filter);
	}

	public void sort(Comparator<? super ErrorData> c) {
		delegate.sort(c);
	}

	public void clear() {
		delegate.clear();
	}

	public boolean equals(Object o) {
		return delegate.equals(o);
	}

	public int hashCode() {
		return delegate.hashCode();
	}

	public ErrorData get(int index) {
		return delegate.get(index);
	}

	public ErrorData set(int index, ErrorData element) {
		return delegate.set(index, element);
	}

	public void add(int index, ErrorData element) {
		delegate.add(index, element);
	}

	public Stream<ErrorData> stream() {
		return delegate.stream();
	}

	public ErrorData remove(int index) {
		return delegate.remove(index);
	}

	public Stream<ErrorData> parallelStream() {
		return delegate.parallelStream();
	}

	public int indexOf(Object o) {
		return delegate.indexOf(o);
	}

	public int lastIndexOf(Object o) {
		return delegate.lastIndexOf(o);
	}

	public ListIterator<ErrorData> listIterator() {
		return delegate.listIterator();
	}

	public ListIterator<ErrorData> listIterator(int index) {
		return delegate.listIterator(index);
	}

	public List<ErrorData> subList(int fromIndex, int toIndex) {
		return delegate.subList(fromIndex, toIndex);
	}

	public Spliterator<ErrorData> spliterator() {
		return delegate.spliterator();
	}
    
    
}
