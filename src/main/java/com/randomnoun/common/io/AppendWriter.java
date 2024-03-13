package com.randomnoun.common.io;

import java.io.IOException;
import java.io.Serializable;
import java.io.Writer;


/** A Writer that has the same append() methods as StringBuilder.
 *
 * <p>This class exists mainly so that I can cut and paste the existing StringBuilder code, but use
 * a Writer backing implementation instead.
 * 
 * <p>Note that these methods may now throw IOExceptions.
 */
public class AppendWriter extends Writer implements Serializable {

    private static final long serialVersionUID = -146927496096066153L;
    private final Writer w;

    /**
     * Constructs a new {@link StringBuilder} instance with default capacity.
     */
    public AppendWriter(Writer w) {
        this.w = w;
    }

    // these methods extend the Writer abstract class
    
    /**
     * Appends a single character to this Writer.
     *
     * @param value The character to append
     * @return This writer instance
     * @throws IOException 
     */
    @Override
    public Writer append(final char value) throws IOException {
        w.write((int) value);
        return this;
    }

    /**
     * Appends a character sequence to this Writer.
     *
     * @param value The character to append
     * @return This writer instance
     */
    @Override
    public Writer append(final CharSequence value) throws IOException {
    	if (value == null) {
    		w.write("null");
    	} else {
    		w.write(value.toString());
    	}
        return this;
    }


    @Override
    public void close() throws IOException {
        w.close();
    }

    @Override
    public void flush() throws IOException {
        w.flush();
    }

    @Override
    public void write(final String value) throws IOException {
    	w.write(value);
    }

    @Override
    public void write(final char[] value, final int offset, final int length) throws IOException {
    	w.write(value, offset, length);
    }
    
    // these methods are in StringBuilder
    
    public AppendWriter append(Object obj) throws IOException {
        return append(String.valueOf(obj));
    }

    public AppendWriter append(String str) throws IOException {
        w.write(str == null ? "null" : str);
        return this;
    }

    public AppendWriter append(StringBuffer sb) throws IOException {
        w.write(sb.toString());
        return this;
    }

    public AppendWriter append(char[] str) throws IOException {
        w.write(str);
        return this;
    }

    public AppendWriter append(char[] str, int offset, int len) throws IOException {
    	w.write(str, offset, len);
        return this;
    }

    public AppendWriter append(boolean b) throws IOException {
    	w.write(b ? "true" : "false");
        return this;
    }

    public AppendWriter append(int i) throws IOException {
    	w.write(String.valueOf(i));
        return this;
    }

    public AppendWriter append(long lng) throws IOException {
        w.write(String.valueOf(lng));
        return this;
    }

    public AppendWriter append(float f) throws IOException {
        w.write(String.valueOf(f));
        return this;
    }

    public AppendWriter append(double d) throws IOException {
        w.write(String.valueOf(d));
        return this;
    }

}
