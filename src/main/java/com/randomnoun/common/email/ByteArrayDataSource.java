package com.randomnoun.common.email;

/* (c) 2013 randomnoun. All Rights Reserved. This work is licensed under a
 * BSD Simplified License. (http://www.randomnoun.com/bsd-simplified.html)
 */
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import jakarta.activation.DataSource;


/** A DataSource generated from an array of bytes
 *  Used to send arbitrary data through the JavaMail API
 *
 * @author knoxg
 */
public class ByteArrayDataSource
    implements DataSource
{
    private byte[] data;
    private String name;
    private String contentType;

    /** Creates a DataSource from an array of bytes
     * @param data byte[] Array of bytes to convert into a DataSource
     * @param name String Name of the DataSource (ex: filename)
     */
    public ByteArrayDataSource(byte[] data, String name, String contentType)
    {
        this.data = data;
        this.name = name;
        this.contentType = contentType; // e.g. "application/octet-stream";
    }

    /** Returns the name of the DataSource
     * @returns String Name of the DataSource
     */
    public String getName()
    {
        return name;
    }

    /** Returns the content type of the DataSource
     * @returns String Content type of the DataSource
     */
    public String getContentType()
    {
        return contentType;
    }

    /** Returns an InputStream from the DataSource
     * @returns InputStream Array of bytes converted into an InputStream
     */
    public InputStream getInputStream()
        throws IOException
    {
        return new ByteArrayInputStream(data);
    }

    /** Returns an OutputStream from the DataSource
     * @returns OutputStream Array of bytes converted into an OutputStream
     */
    public OutputStream getOutputStream()
        throws IOException
    {
        OutputStream out = new ByteArrayOutputStream();

        out.write(data);

        return out;
    }
}
