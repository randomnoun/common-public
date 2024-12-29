package com.randomnoun.common.io;

import java.io.IOException;
import java.io.OutputStream;

/**
 * An outputstream which mimics the 'tee' unix utility. 
 * It writes to two outputStreams at the same time
 */
public class TeeOutputStream extends OutputStream {

	/** The OutputStream */
	protected OutputStream out;
	
    /** The second OutputStream to write to. */
    protected OutputStream branch;

    /**
     * Constructs a TeeOutputStream.
     *
     * @param out    the main OutputStream
     * @param branch the second OutputStream
     */
    public TeeOutputStream(final OutputStream out, final OutputStream branch) {
        this.out = out;
        this.branch = branch;
    }

    /**
     * Writes the bytes to both streams.
     *
     * @param b the bytes to write
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public synchronized void write(final byte[] b) throws IOException {
        out.write(b);
        branch.write(b);
    }

    /**
     * Writes the specified bytes to both streams.
     *
     * @param b   the bytes to write
     * @param off The start offset
     * @param len The number of bytes to write
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public synchronized void write(final byte[] b, final int off, final int len) throws IOException {
        out.write(b, off, len);
        branch.write(b, off, len);
    }

    /**
     * Writes a byte to both streams.
     *
     * @param b the byte to write
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public synchronized void write(final int b) throws IOException {
        out.write(b);
        branch.write(b);
    }

    /**
     * Flushes both streams.
     *
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public void flush() throws IOException {
        out.flush();
        branch.flush();
    }

    /**
     * Closes both output streams.
     * 
     * <p>
     * If closing the main output stream throws an exception, attempt to close the branch output stream.
     * </p>
     *
     * <p>
     * If closing the main and branch output streams both throw exceptions, which exceptions is thrown by this method is
     * currently unspecified and subject to change.
     * </p>
     *
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public void close() throws IOException {
        try {
            out.close();
        } finally {
            branch.close();
        }
    }

}
