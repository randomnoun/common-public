package com.randomnoun.common.security;

/* (c) 2013 randomnoun. All Rights Reserved. This work is licensed under a
 * BSD Simplified License. (http://www.randomnoun.com/bsd-simplified.html)
 */

import java.io.*;
import java.util.Locale;

import com.randomnoun.common.security.User;

/**
 * A class encapsulating information to define an end-user. 
 *
 * @author knoxg
 */
public class User
    implements Serializable
{
    
    /** generated serialVersionUID */
	private static final long serialVersionUID = 3633994518788000533L;

	/** The username for this user; e.g. knoxg . */
    private String username;
    
    /** The customerId for this user. */
    private long customerId;
    
    /** The userId for this user. */
    private long userId = -1;

    /** The locale for this user. */
    private Locale locale; // used for language translation

    // default public constructor
    public User() {
    
    }
    
    /** Sets the username that partly identifies this user. (Within a realm, both username
     *   and customerId must be used to uniquely identify a user).
     *   
     *  @param value the username for this user
     */
    public void setUsername(String username)
    {
        this.username = username;
    }

    /** Returns the username partly identifying this user. (Within a realm, both username
     *   and customerId must be used to uniquely identify a user).
     *   
     *  @return the username for this user.
     */
    public String getUsername()
    {
        return username;
    }

    /** Sets the customerId that partly identifies this user. (Both username
     *   and customerId must be used to uniquely identify a user).
     *  @param value the customerId for this user
     */
    public void setCustomerId(long customerId)
    {
        this.customerId = customerId;
    }

    /** Returns the customerId partly identifying this user. (Both username
     *   and customerId must be used to uniquely identify a user).
     *  @return the customerId for this user.
     */
    public long getCustomerId()
    {
        return customerId;
    }

    /** Sets the locale for this user. The locale is used to localise text
     *  that will be sent to the user.
     *  @param value the locale for this user
     */
    public void setLocale(Locale locale)
    {
        this.locale = locale;
    }

    /** Returns the locale for this user.
     *  @return the locale for this user.
     */
    public Locale getLocale()
    {
        return locale;
    }

    /** Sets the userId for this user 
     * 
     * @param userId the userId for this user
     */
    public void setUserId(long userId)
    {
    	this.userId = userId;
    }

    /** Returns the userId for this user 
     * @return the userId the userId for this user
     */
    public long getUserId()
    {
    	return userId;
    }


    /** Returns a string representation of this user (used for debugging only).
     *  This representation includes the username, customerId, roles and permissions
     *  stored for this user
     *
     *  @return a string representation of this user
     */
    public String toString()
    {
        return "username='" + username + "', customerId=" + customerId;
    }

    /** Two users are considered identical if their customerIds and usernames are identical
     *
     * @param  object the other object used in the comparison
     * @return true if the users are considered identical
     */
    public boolean equals(Object object)
    {
        if (object == null)
        {
            return false;
        }

        if (!(object instanceof User))
        {
            return false;
        }

        User otherUser = (User)object;

        return ((this.customerId == otherUser.getCustomerId()) &&
        (this.username.equals(otherUser.getUsername())));
    }

    /** Must override hasCode() if we override equals().
     *
     * @return a hash which will return identical values for identical users
     *   (as determined by .equals())
     */
    public int hashCode()
    {
    	// hmm. surprised that this worked in the past.
        // return System.identityHashCode(username + "\u0000" + customerId);
    	return new String(username + "\u0000" + customerId).hashCode();
    }
}
