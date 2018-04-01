package com.randomnoun.common;

/* (c) 2013 randomnoun. All Rights Reserved. This work is licensed under a
 * BSD Simplified License. (http://www.randomnoun.com/bsd-simplified.html)
 */

import java.io.*;


/**
 * Utility class to copy streams synchronously and asynchronously.
 *
 * @version $Id$
 * @author knoxg
 */
public class StreamUtil {
    

    /**
     * Creates a new StreamUtils object.
     */
    public StreamUtil() {
    }

    /** Copies the data from an inputStream to an outputstream (used to mimic
     *  pipes).
     *
     *  @param id The id of this stream (used when debugging)
     *  @param input The stream to retrieve information from
     *  @param output The stream to send data to
     * @throws IOException
     */
    public static void copyStream(InputStream input, OutputStream output, int bufSize)
        throws IOException {
        int bytesRead;
        byte[] buffer = new byte[bufSize];

        while ((bytesRead = input.read(buffer)) != -1) {
            output.write(buffer, 0, bytesRead);
            output.flush();
        }
    }
    
    /** Copies the data from an inputStream to an outputstream 
    *
    *  @param input The stream to retrieve information from
    *  @param output The stream to send data to
    *  
    * @throws IOException
    */
    public static void copyStream(InputStream input, OutputStream output)
   		throws IOException 
    {
    	copyStream(input, output, 4096);
	}    

    /** Reads all available data from an InputStream, and returns it in a
     *  single byte array.
     *
     * @param input The stream to receive information from
     * @return A byte array containing the contents of the stream
     * @throws IOException
     */
    public static byte[] getByteArray(InputStream input)
        throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        copyStream(input, baos, 1024);

        return baos.toByteArray();
    }

    /** Returns a thread that, when started, will pipe all data from one
     *  inputstream to an outputstream. The thread will complete when the
     *  inputStream returns EOF.
     *
     *  @param id The id of this stream (used when debugging)
     *  @param input The stream to retrieve information from
     *  @param output The stream to send data to
     */
    public static Thread copyThread(InputStream input, OutputStream output, int bufSize) {
        // not terribly sure why variables accessed from within anonymous classes
        // need to be final, but hey. Hopefully this is just final within the
        // scope of this method call. Which would make sense.
        final InputStream f_input = input;
        final OutputStream f_output = output;
        final int f_bufSize = bufSize;

        return new Thread() {
            public void run() {
                try {
                    copyStream(f_input, f_output, f_bufSize);
                } catch (IOException e) {
                    // not much we can do about this, unfortunately.
                    // could wrap in a runtime exception, I guess.
                	// yeah, let's do that.
                    throw new RuntimeException(e);
                }
            }
        };
    }

    /** Scans this input stream until the text in 'searchText' is found. Returns
     * the number of bytes skipped if the search text was found, or -1 if the text
     * was not found.
     * 
     * @param input The input stream to scan
     * @param searchText The text we are searching for
     * 
     * @throws IOExcpetion if an IO Exception occurs reading the stream
     */    
    public static int indexOf(InputStream input, String searchText) throws IOException {
        int bytesRead = 0;
        int matched = 0;
        int ch;
        byte[] compare = searchText.getBytes();
        int    compareSize = compare.length;
        while (true) {
            ch = input.read();
            if (ch==-1) { return -1; }
            bytesRead++;
            if (ch==compare[matched]) {
                matched++;
                if (matched==compareSize) {
                    return bytesRead;
                }
            } else {
                matched = 0;
            }
        }
    }

    /** Reads this input stream until the text in 'searchText' is found. Returns
     * a String containing the data read, including the searchText. Returns null if the text
     * was not found.
     * 
     * @param input The input stream to scan
     * @param searchText The text we are searching for
     * 
     * @throws IOException if an IO Exception occurs reading the stream
     */    
    public static String readUntil(InputStream input, String searchText) throws IOException {
        //int bytesRead = 0;
        int matched = 0;
        int ch;
        byte[] compare = searchText.getBytes();
        int    compareSize = compare.length;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        while (true) {
            ch = input.read();
            if (ch==-1) { return null; }
            //bytesRead++;
            baos.write(ch);
            if (ch==compare[matched]) {
                matched++;
                if (matched==compareSize) {
                    return baos.toString();
                }
            } else {
                matched = 0;
            }
        }
    }
    
    /** Returns a thread that, when started, will pipe all data from one
     *  inputstream to an outputstream. The thread will complete when the
     *  inputStream returns EOF. The output stream will also be closed.
     *
     *  @param id The id of this stream (used when debugging)
     *  @param input The stream to retrieve information from
     *  @param output The stream to send data to
     */
    public static Thread copyAndCloseThread(InputStream input, OutputStream output, int bufSize) {
        // not terribly sure why variables accessed from within anonymous classes
        // need to be final, but hey. Hopefully this is just final within the
        // scope of this method call. Which would make sense.
        final InputStream f_input = input;
        final OutputStream f_output = output;
        final int f_bufSize = bufSize;

        return new Thread() {
            public void run() {
                try {
                    copyStream(f_input, f_output, f_bufSize);
                    f_output.close();
                } catch (IOException e) {
                    // not much we can do about this, unfortunately.
                    // could wrap in a runtime exception, I guess.
                	// yeah, let's do that.
                    throw new RuntimeException(e);
                }
            }
        };
    }
    
    
    
}
