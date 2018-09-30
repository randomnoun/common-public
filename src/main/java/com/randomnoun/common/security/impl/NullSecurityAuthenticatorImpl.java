package com.randomnoun.common.security.impl;

/* (c) 2013 randomnoun. All Rights Reserved. This work is licensed under a
 * BSD Simplified License. (http://www.randomnoun.com/bsd-simplified.html)
 */

import java.io.IOException;
import java.util.Map;

import com.randomnoun.common.security.SecurityAuthenticator;
import com.randomnoun.common.security.User;

public class NullSecurityAuthenticatorImpl implements SecurityAuthenticator {

	public void initialise(Map<String, Object> properties) {
		// TODO Auto-generated method stub
		
	}

	// everybody wins !
	public boolean authenticate(User user, String password) throws IOException {
		return true;
	}

}
