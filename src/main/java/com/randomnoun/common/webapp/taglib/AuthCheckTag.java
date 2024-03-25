package com.randomnoun.common.webapp.taglib;

/* (c) 2013 randomnoun. All Rights Reserved. This work is licensed under a
 * BSD Simplified License. (http://www.randomnoun.com/bsd-simplified.html)
 */

import java.io.*;

import javax.servlet.http.*;
import javax.servlet.jsp.*;
import javax.servlet.jsp.tagext.*;

import org.apache.log4j.Logger;


/**
 * Custom JSP tag which is used to ensure that the page was generated from within struts.
 * This is used to prevent users from typing in JSP URLs directly, bypassing
 * our security mechanisms.
 *
 * @author  knoxg
 * 
 */
public class AuthCheckTag
    extends BodyTagSupport
{

	/** Generated serialVersionUID */
    private static final long serialVersionUID = -6531003975186503397L;

	/** Logger instance for this class */
    private static Logger logger = Logger.getLogger(AuthCheckTag.class);

    /** This variable is set true if the request comes from struts */
    private boolean isStruts;

    /** doStart tag handler required to fulfill the Tag interface defined in the
     * <a href="http://java.sun.com/products/jsp/">JSP specification</a>.
     *
     * <p>This method determines whether this request has come via the
     * struts framework, by checking the value of the 'isStrutsRequest'
     * request attribute. This is set on every request by the
     * {@link com.randomnoun.common.webapp.struts.RequestProcessor}
     * class. This tag is always empty, and therefore must always
     * return BodyTag.SKIP_BODY
     *
     * @return BodyTag.SKIP_BODY
     */
    public int doStartTag()
        throws javax.servlet.jsp.JspException
    {
        isStruts = false;

        String isStrutsString = (String) pageContext.getRequest().getAttribute("isStrutsRequest");

        if ("true".equals(isStrutsString)) {
            isStruts = true;
        }

        return BodyTag.SKIP_BODY; // this tag always has an empty body.
    }

    /** doEnd tag handler required to fulfill the Tag interface defined in the
     * <a href="http://java.sun.com/products/jsp/">JSP specification</a>.
     *
     * <p>This method enforces the presence of the 'isStrutsRequest' request
     * attribute. If the attribute is present, then processing continues
     * normally, otherwise an error message is given to the user, and the
     * requested URL is sent to the logger of this class.
     *
     * <p>Note that the output of this tag is *not* internationalised.
     *
     * @return BodyTag.SKIP_BODY or BodyTag.SKIP_PAGE
     */
    public int doEndTag()
        throws javax.servlet.jsp.JspException
    {
		try {
	        if (isStruts) {
	            return BodyTag.EVAL_PAGE;
	        } else {
                HttpServletRequest httpRequest = (HttpServletRequest)pageContext.getRequest();
                logger.info("Attempt to access JSP directly via URL: '" + httpRequest.getRequestURL() + "'");
                try {
                    // try to take back anything buffered to be sent to the client
                    pageContext.getOut().clear();
                } catch (IOException ioe) {
                    // swallow this exception - it's not that important if content has already been sent
                }
                pageContext.getOut().println("<html><body>");
                pageContext.getOut().println("<p>Direct access to JSPs is denied by policy</p>");
                pageContext.getOut().println("</body></html>");
            } 
            return BodyTag.SKIP_PAGE;
		} catch (IOException ioe) {
			// may be caused by end-user hitting 'stop' button in browser; ignore
			return BodyTag.SKIP_PAGE;
		} catch (Throwable t) {
			 // log and rethrow
			 t.printStackTrace();
			 throw (JspException) new JspException("Exception occurred in AuthCheckTag").initCause(t);
		}    
	}
}
