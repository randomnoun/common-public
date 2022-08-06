package com.randomnoun.common.jexl;

/* (c) 2013 randomnoun. All Rights Reserved. This work is licensed under a
 * BSD Simplified License. (http://www.randomnoun.com/bsd-simplified.html)
 */

import java.io.Serializable;
import java.text.*;
import java.util.*;

import com.randomnoun.common.Text;

/**
 * Data object representing a point in time, or a 24-hour day period.
 * 
 * <p>You should probably use something in joda or the java 8 time package these days
 *
 * <p>DateSpans may be <i>fixed</i>, which refer to dates that do not necessarily have timezones
 * (e.g. a valueDate, represented with the fixed date "2004-11-07" is always the 7th of November,
 * regardless of where in the world this may be interpreted). Note that
 * Date objects are stored internally as milliseconds since the Unix epoch UTC, and therefore
 * do *not* qualify as 'fixed' dates. Anything that isn't fixed I refer to as 'timezoned'
 * in the documentation here (because they only make sense when interpretted in a
 * specific timezone).
 *
 * <p>Examples of DateSpans:
 * <pre>
 * new DateSpan("2004-11-07")          - fixed day span       (representing November 7, 2004, 24hr period))
 * new DateSpan("2004-11-07T20:00:00") - fixed timestamp      (representing November 7, 2004, 8:00pm)
 * new DateSpan(
 *   DateUtils.getStartOfDay(new Date(),
 *   TimeZone.getDefault()), true)     - timezoned day span   (representing today in local time (24hr period))
 * new DateSpan(new Date())            - timezoned timestamp  (representing right now in local time)
 * </pre>
 *
 * <p>Note that only yyyy-MM-dd or yyyy-MM-dd'T'HH:mm:ss (without timezone) string formats are permitted;
 * this is more strict than that allowed by ISO8901, so I do not use that class here.
 *
 * <p>Note that timestamps are only precise down to 1 second.
 *
 * <p>'fixed' times may also be assigned default timezones, which can be used when comparing
 * timezoned values with fixed DateSpans; e.g. the RUNDATE for Market Center "New York" may be
 * specified as "2004-11-07" (and compared with valueDates with that same fixed date),
 * but when compared against a SystemArrivalTime, needs to be interpretted in a time zone (in
 * this case, TimeZone.getTimezone("America/New_York")). This can be specified at construction
 * time, e.g.
 * <pre style="code">
 *   new DateSpan("2004-11-07", TimeZone.getTimezone("America/New_York"))
 * </pre>
 *
 * <p>Note that if a timezone is not supplied for a 'fixed' time, then any call to {@link #getStartAsDate()}
 * or {@link #getEndAsDate()} will return an IllegalStateException.
 *
 * <p>This class only supports the 'fixed day span', 'timezoned day span' or 'timezoned timestamp'
 * examples above. Could extend this later on to 'fixed timestamp' values. 
 * Similarly, this class only supports 24hour spans, but could support more arbitrary durations 
 * if this was needed further down the track.
 *
 * <p>Only years between 1000 and 9999 will be handled correctly by this class.
 * 
 * @author knoxg
 */
public class DateSpan
    implements Serializable
{
    /** Generated serialVersionUID */
	private static final long serialVersionUID = 4189707923685011603L;

	/** Contains the string representation of a 'fixed' timestamp or duration (or null if this is a timezoned DateSpan) */
    private String fixedTime;

    /** Contains a 'timezoned' Date, or for fixed values, contains the Date of 'fixedTime' in the defaultTimeZone */
    private Date timezonedTime;

    /** Is true if this DateSpan represents a 24hour period, or false for a 1 second instant. */
    boolean isDay;

    /** For 'fixed' DateSpans, specifies a default timezone to interpret this in (if we need to) */
    private TimeZone defaultTimezone;
    
    /** A single timestamp instant, without timezone. Equivalent to DateSpan(Date, false, null) */
    public DateSpan(Date timestamp) {
        this(timestamp, false, null);
    }

    /** Create a 'timezoneless' date. A defaultTimezone may be supplied, which will be used
     *  when converting to Date objects.
     *
     * @param dateSpan A date in yyyy-MM-dd or yyyy-MM-dd'T'HH:mm:ss format (the former
     *    specifies a day period, the latter specifies a single instant
     * @param defaultTimezone A default timezone to use when converting to Date objects.
     *    This parameter may be null.
     *
     * @throws NullPointerException if the dateSpan passed to this method is null
     **/
    public DateSpan(String dateSpan, TimeZone defaultTimezone)
    {
        // ensure this is a date we can accept
        if (dateSpan == null) {
            throw new NullPointerException("null dateSpan");
        }

        DateFormat timestampFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        DateFormat dayFormat = new SimpleDateFormat("yyyy-MM-dd");
        TimeZone timezone;

        if (defaultTimezone == null) {
            timezone = TimeZone.getTimeZone("UTC"); // a simple, known timezone.
        } else {
            timezone = defaultTimezone;
        }

        // we want dates accepted here to be exactly right
        timestampFormat.setLenient(false);
        dayFormat.setLenient(false);
        timestampFormat.setTimeZone(timezone);
        dayFormat.setTimeZone(timezone);

        Date date;

        try {
            date = timestampFormat.parse(dateSpan);

            // parse succeeded, must be a timestamp
            this.isDay = false;
            this.fixedTime = dateSpan;
            this.timezonedTime = date;
            this.defaultTimezone = defaultTimezone;
        } catch (ParseException pe) {
            try {
            	// set timestamp component for this date to 00:00:00 in this timezone
            	date = timestampFormat.parse(dateSpan + "T00:00:00");
                //date = dayFormat.parse(dateSpan);
                
                // parse succeeded, must be a day span
                this.isDay = true;
                this.fixedTime = dateSpan;
                this.timezonedTime = date;
                this.defaultTimezone = defaultTimezone;
            } catch (ParseException pe2) {
                throw new IllegalArgumentException("dateSpan '" + dateSpan +
                    "' could not be parsed as a timestamp or day value");
            }
        }
    }

    /** Specify a 24-hour period, starting at point (e.g. "today" spans 24 hours).
     *
     * @param dateSpanStart the time of this DateSpan (or the beginning, if a period)
     * @param isDay set this to true if this DateSpan represents a 24 hour day, false otherwise
     * @param defaultTimezone the timezone to interpret this date in, if we need to convert
     *   it to string form.
     *
     * @throws NullPointerException if the dateSpanStart parameter passed to this method is null
     */
    public DateSpan(Date dateSpanStart, boolean isDay, TimeZone defaultTimezone)
    {
        if (dateSpanStart == null) {
            throw new NullPointerException("null dateSpanStart");
        }

        this.timezonedTime = dateSpanStart;
        this.isDay = isDay;
        this.defaultTimezone = defaultTimezone;
    }

    /** Returns true for a  'fixed' DateSpan. See the class javadocs for details.
     *
     * @return true for a 'fixed' DateSpan
     */
    public boolean isFixed()
    {
        return (this.fixedTime != null);
    }

    /** Returns true for a 24hour period. See the class javadocs for details
     *
     * @return true if this DateSpan represents a 24 hour period, false otherwise
     */
    public boolean isDay()
    {
        return this.isDay;
    }

    /** Gets the 'fixed' representation of this DateSpan, if the DateSpan is fixed. This
     * may return a day string (yyyy-MM-dd) or a day/time (yyyy-MM-dd'T'HH:mm:ss).
     *
     * @return the 'fixed' representation of this DateSpan, if the DateSpan is fixed
     *
     * @throws IllegalStateException if this DateSpan is not fixed.
     */
    public String getFixed()
    {
        if (!isFixed()) {
            throw new IllegalStateException(
                "getFixed() only allowed on fixed DateSpan objects");
        }

        return fixedTime;
    }

    /** Returns the start of this DateSpan in yyyyMMdd format (presumably for comparing
     *  against the value date column). If this is a timezoned DateSpan and no default
     * timezone has been set, raises an IllegalStateException
     *
     * @return  the start of this DateSpan in yyyyMMdd format
     *
     * @throws IllegalStateException if no defaultTimezone has been specified for a timezoned DateSpan
     */
    public String getStartAsYyyymmdd()
    {
        if (isFixed()) {
            // yyyy-MM-ddTHH:mm:ss
            // 0123456789012345678
            return fixedTime.substring(0, 4) + fixedTime.substring(5, 7) +
            fixedTime.substring(8, 10);
        } else {
            if (defaultTimezone == null) {
                throw new IllegalStateException("Default timezone must be set");
            }
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
            sdf.setTimeZone(defaultTimezone);
            return sdf.format(timezonedTime);
        }
    }

    /** Returns the start of this DateSpan as a Date (milliseconds from UTC epoch).
     * If this is a fixed DateSpan and no default timezone has been set, raises
     * an IllegalStateException
     *
     * @return  the start of this DateSpan as a Date
     *
     * @throws IllegalStateException if no defaultTimezone has been specified for a fixed DateSpan
     */
    public Date getStartAsDate()
    {
        if (isFixed()) {
            if (defaultTimezone == null) {
                throw new IllegalStateException("Default timezone must be set");
            }
            return timezonedTime;
        } else {
            return timezonedTime;
        }
    }

    /** Returns the end of this DateSpan in yyyyMMdd format (presumably for comparing
     *  against the value date column). If this is a timezoned DateSpan and no default
     * timezone has been set, raises an IllegalStateException
     *
     * @return  the end of this DateSpan in yyyyMMdd format
     *
     * @throws IllegalStateException if this DateSpan represents a point in time (not a duration),
     *   or if no defaultTimezone has been specified for a timezoned DateSpan
     */
    public String getEndAsYyyymmdd()
    {
        if (!isDay) {
            throw new IllegalStateException(
                "getEndAsString() only supported for time periods, not timestamps");
        }

        if (isFixed()) {
            Calendar cal = new GregorianCalendar(Integer.parseInt(fixedTime.substring(0, 4)),
                    Integer.parseInt(fixedTime.substring(5, 7)),
                    Integer.parseInt(fixedTime.substring(8, 10)));

            cal.add(Calendar.DAY_OF_YEAR, 1);

            int day = cal.get(Calendar.DAY_OF_MONTH);
            int month = cal.get(Calendar.MONTH) + 1;
            int year = cal.get(Calendar.YEAR);

            return ((day < 10 ? "0" : "") + day) + ((month < 10 ? "0" : "") + month) +
            year;
        } else {
            if (defaultTimezone == null) {
                throw new IllegalStateException("Default timezone must be set");
            }

            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
            sdf.setTimeZone(defaultTimezone);
            return sdf.format(new Date(timezonedTime.getTime() + 86399999)); // 86400000ms in 1 day
        }
    }

    /** Returns the end of this DateSpan as a Date (milliseconds from UTC epoch).
     * If this is a fixed DateSpan and no default timezone has been set, raises
     * an IllegalStateException
     *
     * @return the end of this DateSpan as a Date
     *
     * @throws IllegalStateException if no defaultTimezone has been specified for a fixed DateSpan
     */
    public Date getEndAsDate()
    {
        if (isFixed()) {
            if (defaultTimezone == null) {
                throw new IllegalStateException("Default timezone must be set");
            }
            return new Date(timezonedTime.getTime() + 86399999); // 86400000ms in 1 day
        } else {
            return new Date(timezonedTime.getTime() + 86399999); // 86400000ms in 1 day
        }
    }

    /** A string representation of this object, useful for debugging.
     *
     * @return a string representation of this object, useful for debugging.
     */
    public String toString()
    {
        if (isFixed()) {
            return "(\"" + fixedTime + "\", isDay=" + isDay + ", tz=" +
            (defaultTimezone == null ? "(null)" : defaultTimezone.getID()) +
            ")";
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'+00:00'");
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            return "(t=" + sdf.format(timezonedTime) + ", isDay=" + isDay + ", tz=" +
            (defaultTimezone == null ? "(null)" : defaultTimezone.getID()) +
            ")";
        }
    }
    
    /** Inverse of toString(); required for {@link com.randomnoun.common.Text#parseStructOutput(String)} 
     * to do it's thing.
     * 
     * @param string The text representation of this object (as returned by .toString())
     * 
     * @return an instance of a DateSpan
     */
    public static DateSpan valueOf(String text) throws ParseException {
        if (!(text.startsWith("(") && text.endsWith(")"))) {
            throw new ParseException("DateSpan representation must begin and end with brackets", 0);
        }
        text = text.substring(1, text.length()-1);
        List<String> list = Text.parseCsv(text);
        if (list.size()!=3) { throw new ParseException("Expected 3 elements in DateSpan representation", 0); }
        String time = (String) list.get(0);
        String isDay = (String) list.get(1);
        String tz = (String) list.get(2);
        TimeZone timezone = null; 
        if (!isDay.startsWith("isDay=")) { throw new ParseException("Expected 'isDay=' in second element", 0); }
        if (!tz.startsWith("tz=")) { throw new ParseException("Expected 'tz=' in third element", 0); }
        if (!tz.equals("(null)")) { timezone = TimeZone.getTimeZone(tz.substring(3)); }
          
        if (time.startsWith("t=")) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'+00:00'");
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date = sdf.parse(time.substring(2));
            return new DateSpan(date, isDay.substring(6).equals("true"), timezone);
                 
        } else {
            return new DateSpan(time, timezone);
            
        }
    }
    

    /** Returns true if the object passed to this method as an equivalent dateSpan instance
     *  to the current object. (Used in unit testing only) 
     * 
     *  @param other the object we are comparing agianst
     * 
     *  @return true if the other object is identical to this one, false otherwise
     */
    public boolean equals(Object obj) {
        if (!(obj instanceof DateSpan)) { return false; }
        DateSpan other = (DateSpan) obj;
        if ( ((fixedTime == null && other.fixedTime==null) ||
              (fixedTime != null && fixedTime.equals(other.fixedTime))) &&
             ((timezonedTime == null && other.timezonedTime==null) ||
              (timezonedTime != null && timezonedTime.equals(other.timezonedTime))) &&
             ((defaultTimezone == null && other.defaultTimezone==null) ||
              (defaultTimezone != null && defaultTimezone.equals(other.defaultTimezone))) &&
             isDay == other.isDay ) {
            return true;
        } else {
            return false;
        }
    }


}
