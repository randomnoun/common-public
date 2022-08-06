package com.randomnoun.common.email;

/* (c) 2013 randomnoun. All Rights Reserved. This work is licensed under a
 * BSD Simplified License. (http://www.randomnoun.com/bsd-simplified.html)
 */
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.randomnoun.common.StreamUtil;

import jakarta.activation.DataHandler;
import jakarta.activation.DataSource;
import jakarta.activation.FileDataSource;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;

/**
 * Provides a simple, one-class wrapper around the Java Mail API. Use the
 * {@link #emailTo(String, String, String, String, String, String, String)} 
 * method to send a mail in a single method call, or 
 * {@link #emailToNoEx(String, String, String, String, String, String, String)} 
 * to send an email ignoring exceptions.
 *
 * <p>SMTP authentication is supported.
 * 
 * <p>S/MIME encryption isn't supported, but would be relatively easy to add;
 *
 * <p>Use the (somewhat more complex) {@see #emailAttachmentTo(Map)} method to send emails
 * with attachments sourced from the file system, byte arrays or the classpath,
 * or to modify the email headers.
 *
 * @author knoxg
 */
public class EmailWrapper {
    
    public static Logger logger = Logger.getLogger(EmailWrapper.class);

    private static boolean isBlank(String string) {
        return (string==null || "".equals(string));
    }

    /**
     * A static method which provides the facility to easily send off an
     * email using the JavaMail API. Mail-generated exceptions are sent to
     * logger.error.
     *
     * @param to A comma-separated list of recipients
     * @param from The address to place in the From: field of the email
     * @param subject The subject text
     * @param msgText The message text
     */
    public static void emailToNoEx(String to, String from, String host, String subject, 
      String bodyText, String username, String password) {
        try {
            emailTo(to, from, host, subject, bodyText, username, password);
        } catch (MessagingException me) {
            logger.error("A messaging exception occurred whilst sending an email", me);
        }
    }

    /**
     * A simpler version of emailTo. 
     *
     * @param to A comma-separated list of recipients
     * @param from The address to place in the From: field of the email
     * @param host The SMTP host to use
     * @param subject The subject text
     * @param bodyText The message text
     * @param username The SMTP user to send from (null if not authenticated)
     * @param password The SMTP password to use (null if not authenticated)
     *
     */
    public static void emailTo(String to, String from, String host, String subject, String bodyText,
        String username, String password)
        throws MessagingException {
        Properties props;

        props = new Properties();
        props.put("to", to);
        props.put("from", from);
        props.put("host", host);
        props.put("subject", subject);
        props.put("bodyText", bodyText);
        if (username!=null) { props.put("username", username); } 
        if (password!=null) { props.put("password", password); } 
        emailAttachmentTo(props);
    }

    /**
     * A simpler version of emailTo, with HTML. 
     *
     * @param to A comma-separated list of recipients
     * @param from The address to place in the From: field of the email
     * @param host The SMTP host to use
     * @param subject The subject text
     * @param bodyText The message text
     * @param bodyHtml The message text, in HTML format
     * @param username The SMTP user to send from (null if not authenticated)
     * @param password The SMTP password to use (null if not authenticated)
     *
     */
    public static void emailTo(String to, String from, String host, String subject, String bodyText,
        String bodyHtml, String username, String password)
        throws MessagingException {
        Properties props;

        props = new Properties();
        props.put("to", to);
        props.put("from", from);
        props.put("host", host);
        props.put("subject", subject);
        props.put("bodyText", bodyText);
        props.put("bodyHtml", bodyHtml);
        
        if (username!=null) { props.put("username", username); } 
        if (password!=null) { props.put("password", password); } 
        emailAttachmentTo(props);
    }
    
    
    /** Return an array of InternetAddress objects from a comma-separated list,
     *  and a header list. The headerList object passed to the
     *  {@link #emailAttachmentTo(java.util.Map)} object is passed into this
     *  object; if a header exists in this list with the name _headerName_ passed
     *  to this method, then the email addresses contained within it will also
     *  be appended to the returned array.
     *
     *  @param stringList A comma-separated list of names which will be appended
     *    to the array
     *  @param headerName The header name that will be used to search for
     *    more email addresses
     *  @param headerList A structured list, describing a list of custom
     *    headers to be added to an email, as passed to emailAttachmentTo()
     *
     *  @return an InternetAddress[] structure, suitable for use in various
     *    JavaMail API calls
     */
    private static InternetAddress[] getAddressList(String stringList, String headerName, List<Map<String, Object>> headerList)
        throws MessagingException {
        String[] addresses;
        List<InternetAddress> addressList = new ArrayList<>();
        int i;

        // add addresses in 'stringList' to addressList array
        if (!isBlank(stringList)) {
            addresses = stringList.split(",");

            for (i = 0; i < addresses.length; i++) {
                addressList.add(new InternetAddress(addresses[i]));
            }
        }

        // do the same for any headers matching the headerName supplied
        if (!isBlank(headerName) && (headerList != null)) {
            for (Iterator<Map<String, Object>> j = headerList.iterator(); j.hasNext();) {
                Map<String, Object> map = (Map<String, Object>) j.next();

                if (headerName.equals(map.get("name"))) {
                    addresses = ((String) map.get("value")).split(",");

                    for (i = 0; i < addresses.length; i++) {
                        addressList.add(new InternetAddress(addresses[i]));
                    }
                }
            }
        }

        return (InternetAddress[]) addressList.toArray(new InternetAddress[] {  });
    }

    /** Versatile extendable email wrapper method.
     *
     * This method takes a structured map as input, representing the
     * message to be sent. It supports multiple to, from, cc, bcc, and replyto
     * fields. It supports custom headers. It supports attachments from
     * files on the filesystem, classpath resources, and passed in directly.
     *
     * The following attributes are accepted by this method
     *
     * <attributes>
     * to - To addresses, comma-separated. Will also include any
     *      custom headers supplied with the headername of 'to'
     * from - From address
     * subject - The subject of the email
     * bodyText - The body text of the email
     * bodyHtml - The body text of the email, in HTML format
     * username - If not null, the username to authenticate to the SMTP server
     * password - If not null, the password with which to authenticate to the SMTP 
     * cc - CC addresses, comma-separated. Will also include any
     *                   custom headers supplied with the headername of 'cc'
     * bcc - BCC addresses, comma-separated. Will also include any
     *                   custom headers supplied with the headername of 'bcc'
     * replyTo - Reply-To addresses, comma-separated. Will also include any
     *                   custom headers supplied with the headername of 'replyTo'
     * client - email client to use in generated MessageID (defaults to "JavaMail")
     * suffix - suffix to use in generated MessageID (defaults to "username@host")
     *          (both client & suffix must be specified together)
     * sessionProperties - A map containing additional JavaMail session properties
     * headers - A structured list of custom headers
     * <attributes>
     *   name - The header name
     *   value - The header value
     * </attributes>
     *
     * attachFiles   Attachments to this email, sourced from the filesystem
     * <attributes>
     *   filename - The file on the local filesystem which contains the  data to send
     *   attachFilename - The name of the file visible in the email
     *   contentType - The content-type to assign to this file. Will default to
     *                   <tt>application/octet-stream</tt>
     * </attributes>
     *
     * attachResources Attachments to this email, sourced from the classLoader
     * <attributes>
     *   resource - The resource name
     *   attachFilename - The name of the file visible in the email
     *   contentType - The content-type to assign to this file. Will default to
     *                   <tt>application/octet-stream</tt>
     *   classLoader - Any class which will indicate which class loader to
     *                   use to find this resource, or a ClassLoader instance.
     *                   If missing, will default to the class loader of the
     *                   EmailWrapper class.
     * </attributes>
     *
     * attachData    Attachments to this email, passed in directly
     * <attributes>
     *   data - The data comprising the attachment. Can be either a
     *                   byte array, or a string. If the object is of any other
     *                   type, then it is converted to a string using it's
     *                   .toString() method.
     *   attachFilename - The name of the file visible in the email
     *   contentType - The content-type to assign to this file. Will default
     *                   to <tt>application/octet-stream</tt>
     * </attributes>
     * </attributes>
     *
     * This method returns no values
     *
     * @throws MessagingException An error occurred sending the email
     */
    @SuppressWarnings("unchecked")
	public static void emailAttachmentTo(Map<Object, Object> params) // Map<Object, Object> so it can accept Properties objects
        throws MessagingException {
        logger.debug("Inside emailAttachmentTo method with params");

        // get parameters out of map
        String to = (String) params.get("to");
        String from = (String) params.get("from");
        String cc = (String) params.get("cc");
        String bcc = (String) params.get("bcc");
        String replyTo = (String) params.get("replyTo");
        String bodyText = (String) params.get("bodyText");
        String bodyHtml = (String) params.get("bodyHtml");
        String subject = (String) params.get("subject");
        String host = (String) params.get("host");
        String username = (String) params.get("username");
        String password = (String) params.get("password");
        String client = (String) params.get("client");
        String suffix = (String) params.get("suffix");
        
        List<Map<String, Object>> attachFiles = (List<Map<String, Object>>) params.get("attachFiles"); // list of filenames to retrieve from disk
        List<Map<String, Object>> attachResources = (List<Map<String, Object>>) params.get("attachResources"); // list of resources to retrieve from classpath
        List<Map<String, Object>> attachData = (List<Map<String, Object>>) params.get("attachData"); // list of attachment data
        List<Map<String, Object>> headers = (List<Map<String, Object>>) params.get("headers");
        Map<String, Object> sessionProperties = (Map<String, Object>) params.get("sessionProperties");
        
        boolean isMultipart = false;
        boolean isAltContent = false;  // true if both text and html

        // validate / set defaults
        if (isBlank(to)) { throw new IllegalArgumentException("Empty 'to' address"); }
        if (isBlank(host)) { host = "127.0.0.1"; }

        // set up mail session
        Properties props = new Properties();
        props.put("mail.smtp.host", host);
        if (username != null) { props.put("mail.smtp.user", username); }
        if (sessionProperties != null) {
        	props.putAll(sessionProperties);
        }
        
        Session session = Session.getInstance(props, null);
        Message msg;
        if (isBlank(client) || isBlank(suffix)) {
        	msg = new MimeMessage(session);
        } else {
        	msg = new CustomMimeMessage(session, client, suffix);
        }

        if (!isBlank(from)) {
            msg.setFrom(new InternetAddress(from));
        }
        if (!isBlank(subject)) {
            msg.setSubject(subject);
        }

        msg.setSentDate(new Date());
        
        if (bodyHtml!=null && bodyText!=null) { 
        	isMultipart = true; 
        	isAltContent = true;
        }

        // add receipient information
        InternetAddress[] toArray = getAddressList(to, "to", headers);
        if (toArray.length > 0) { msg.setRecipients(Message.RecipientType.TO, toArray); }

        InternetAddress[] ccArray = getAddressList(cc, "cc", headers);
        if (ccArray.length > 0) { msg.setRecipients(Message.RecipientType.CC, ccArray); }

        InternetAddress[] bccArray = getAddressList(bcc, "bcc", headers);
        if (bccArray.length > 0) { msg.setRecipients(Message.RecipientType.BCC, bccArray); }

        InternetAddress[] replyToArray = getAddressList(replyTo, "replyTo", headers);
        if (replyToArray.length > 0) { msg.setReplyTo(replyToArray); }

        // add other headers
        if (headers != null) {
            for (Iterator<Map<String, Object>> i = headers.iterator(); i.hasNext();) {
            	Map<String, Object> map = i.next();
                String headerName = (String) map.get("name");

                if (headerName != null && !headerName.equals("to") && !headerName.equals("cc") && !headerName.equals("bcc")) {
                    msg.addHeader(headerName, (String) map.get("value"));
                }
            }
        }

        // create multipart message. Beware of non us-ascii charsets.
        MimeMultipart multiPart;
        if (isAltContent) {
        	multiPart = new MimeMultipart("alternative");
        	MimeBodyPart bodyPart = new MimeBodyPart();
        	bodyPart.setText(bodyText /*, "us-ascii", "plain" */);
        	multiPart.addBodyPart(bodyPart);
        	bodyPart = new MimeBodyPart();
        	bodyPart.setText(bodyHtml, "us-ascii", "html" );  /* creates text/html Content-type */
        	multiPart.addBodyPart(bodyPart);
        } else {
        	multiPart = new MimeMultipart("mixed");
        	MimeBodyPart bodyPart = new MimeBodyPart();
        	if (bodyText!=null) {
        		bodyPart.setText(bodyText /*, "us-ascii", "text/plain" */);
        	} else if (bodyHtml!=null) {
        		bodyPart.setText(bodyHtml, "us-ascii", "html");
        	}
            multiPart.addBodyPart(bodyPart);
        }

        // create attachments from files on disk
        if (attachFiles != null) {
            for (Iterator<Map<String, Object>> i = attachFiles.iterator(); i.hasNext();) {
                Map<String, Object> map = i.next();
                String filename = (String) map.get("filename");
                String attachFilename = (String) map.get("attachFilename");
                DataSource dataSource = new FileDataSource(filename);
                MimeBodyPart attachment = new MimeBodyPart();

                attachment.setDataHandler(new DataHandler(dataSource));
                attachment.setFileName(attachFilename); //attachFile);
                multiPart.addBodyPart(attachment);
                isMultipart = true;
            }
        }

        // create attachments from a list of classpath resources
        try {
            if (attachResources != null) {
                for (Iterator<Map<String, Object>> i = attachResources.iterator(); i.hasNext();) {
                	Map<String, Object> map = i.next();
                    String resource = (String) map.get("resource");
                    String attachFilename = (String) map.get("attachFilename");
                    Object classLoaderObject = (Object) map.get("classloader");
                    String contentType = (String) map.get("contentType");

                    if (isBlank(contentType)) {
                        contentType = "application/octet-stream";
                    }

                    ClassLoader classLoader;

                    if (classLoaderObject == null) {
                        classLoaderObject = EmailWrapper.class;
                    }

                    if (classLoaderObject instanceof Class) {
                        classLoader = ((Class<?>) classLoaderObject).getClassLoader();
                    } else if (classLoaderObject instanceof ClassLoader) {
                        classLoader = (ClassLoader) classLoaderObject;
                    } else {
                        classLoader = classLoaderObject.getClass().getClassLoader();
                    }

                    InputStream inputStream = classLoader.getResourceAsStream(resource);
                    byte[] attachmentData = StreamUtil.getByteArray(inputStream);
                    DataSource dataSource = new ByteArrayDataSource(attachmentData, attachFilename, contentType);
                    MimeBodyPart attachment = new MimeBodyPart();

                    attachment.setDataHandler(new DataHandler(dataSource));
                    attachment.setFileName(attachFilename);
                    multiPart.addBodyPart(attachment);
                    isMultipart = true;
                }
            }
        } catch (IOException ioe) {
            throw new MessagingException("Error reading resource", ioe);
        }

        // create attachments from data passed in to this method
        if (attachData != null) {
            for (Iterator<Map<String, Object>> i = attachData.iterator(); i.hasNext();) {
            	Map<String, Object> map = i.next();
                String attachFilename = (String) map.get("attachFilename");
                Object data = map.get("data");
                String contentType = (String) map.get("contentType");
                String contentId = (String) map.get("contentId");
                String disposition = (String) map.get("disposition");
                if (isBlank(contentType)) {
                    contentType = "application/octet-stream";
                }
                

                byte[] attachmentData;

                if (data instanceof byte[]) {
                    attachmentData = (byte[]) data;
                } else if (data instanceof String) {
                    attachmentData = ((String) data).getBytes();
                } else {
                    attachmentData = data.toString().getBytes();
                }

                DataSource dataSource = new ByteArrayDataSource(attachmentData, attachFilename, contentType);
                MimeBodyPart attachment = new MimeBodyPart();

                attachment.setDataHandler(new DataHandler(dataSource));
                attachment.setFileName(attachFilename);
                multiPart.addBodyPart(attachment);
                if (contentId!=null) {
                	attachment.addHeader("Content-ID", contentId);
                }
                if ("inline".equals(disposition)) {
                	attachment.setDisposition(Part.INLINE);
                } else {
                	attachment.setDisposition(Part.ATTACHMENT);
                }
                
                
                isMultipart = true;
            }
        }

        // only make this a multi-part message if we have attachments
        if (isMultipart) {
        	
            msg.setContent(multiPart);
        } else {
        	if (bodyText!=null) {
        		msg.setText(bodyText);
        	} else if (bodyHtml!=null) {
        		throw new UnsupportedOperationException("HTML text supplied without plain text. Test this before using.");
        	}
        }
        
        Transport tr = session.getTransport("smtp");
        if (username!=null && password!=null) {
            tr.connect(host, username, password);
        } else {
            tr.connect();
        }
        msg.saveChanges(); 
        tr.sendMessage(msg, msg.getAllRecipients());
        tr.close();
    }
}
