package com.randomnoun.common.email;

/* (c) 2013 randomnoun. All Rights Reserved. This work is licensed under a
 * BSD Simplified License. (http://www.randomnoun.com/bsd-simplified.html)
 */

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

public class CustomMimeMessage extends MimeMessage {

	private String client;
	private String suffix;
	private static int id = 0;
	
	/** Create a MimeMessage. Equivalent to mavax.mail.MimeMessage(Session),
	 * but allows a different Message-ID to be set.
	 * 
	 * Normal Message-IDs look like this:
	 * <pre>
	 * &lt;28405330.11271144052625.JavaMail.knoxg@filament&gt;
     *                          -client-.----suffix----
	 * </pre>
	 * but this class allows different values for client and suffix above.
	 * 
	 * @param session
	 * @param client
	 * @param suffix
	 */
	public CustomMimeMessage(Session session, String client, String suffix) {
		super(session);
		this.client = client;
		this.suffix = suffix;
	}

	/**
     * Update the Message-ID header.  This method is called
     * by the <code>updateHeaders</code> and allows a subclass
     * to override only the algorithm for choosing a Message-ID.
     *
     * @since		JavaMail 1.4
     */
    protected void updateMessageID() throws MessagingException {
    	// setHeader("Message-ID", "<" + UniqueValue.getUniqueMessageIDValue(session) + ">");
    	setHeader("Message-ID", "<" + getUniqueMessageIDValue(session) + ">");
    }		
	
    public String getUniqueMessageIDValue(Session session) {
    	// String suffix = null;

    	/*InternetAddress addr = InternetAddress.getLocalAddress(ssn);
    	if (addr != null)
    	    suffix = addr.getAddress();
    	else {
    	    suffix = "javamailuser@localhost"; // worst-case default
    	}
    	*/

    	StringBuffer s = new StringBuffer();

    	// Unique string is <hashcode>.<id>.<currentTime>.JavaMail.<suffix>
    	s.append(s.hashCode()).append('.').append(getUniqueId()).append('.').
    	  append(System.currentTimeMillis()).append('.').
    	  // append("JavaMail.").
    	  append(client).
    	  append(suffix);
    	return s.toString();

    }
    
    private static synchronized int getUniqueId() {
    	return id++;
    }

    
}
