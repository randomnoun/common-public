package com.randomnoun.common.security;

/* (c) 2013 randomnoun. All Rights Reserved. This work is licensed under a
 * BSD Simplified License. (http://www.randomnoun.com/bsd-simplified.html)
 */

import java.io.IOException;
import java.util.*;

import com.randomnoun.common.security.User;



/** Security authenticator object.
 *
 * <p>Just the authentication, none of the authorisation.
 *
 * 
 * @author knoxg
 */
public interface SecurityAuthenticator
{
    /**
     * Initialise this security loader. This method will be invoked by the SecurityContext
     * object on initialisation
     *
     * @param properties Initialisation properties for this loader.
     */
    public void initialise(Map<String, Object> properties);

    /**
     * Returns true if the supplied password authenticates the supplied user,
     * false otherwise.
     *
     * @param username The username to authenticate
     * @param password The password used to authenticate the user
     *
     * @return true if the username/password combination is valid.
     * @throws IOException
     */
    public boolean authenticate(User user, String password)
        throws IOException;
     
}
