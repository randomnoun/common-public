package com.randomnoun.common.log4j2;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.Charset;

import org.apache.logging.log4j.Logger;

/** An outputstream that writes to a log4j Logger.
 * 
 * Lines are terminated by CRLF or LF. 
 *
 * @author knoxg
 */
public class LoggingOutputStream extends OutputStream {
	Logger logger;
	StringBuffer line = new StringBuffer();
	PipedOutputStream pos;
	PipedInputStream pis;
	InputStreamReader isr;
	boolean wasCr; // last character was a CR (to convert CRLFs)
	
	public LoggingOutputStream(Logger logger, Charset charset) throws IOException {
		this.logger = logger;
		this.pos = new PipedOutputStream();
		this.pis = new PipedInputStream(pos);
		this.isr = new InputStreamReader(pis, charset);
	}
	public LoggingOutputStream(Logger logger, String charsetName) throws IOException {
		this(logger, Charset.forName(charsetName));
	}
	public LoggingOutputStream(Logger logger) throws IOException {
		this(logger, Charset.defaultCharset());
	}
	
	@Override
	public void write(int b) throws IOException {
		pos.write(b);
		int ch = isr.read();
		if (ch == -1) {
			// EOF
		} else if (ch == 13) { // \r
			logger.info(line); line.setLength(0);
			wasCr = true;
		} else if (ch == 10) { // \n
			if (!wasCr) {
				logger.info(line); line.setLength(0);
			}
			wasCr = false;
		} else { 
			line.append((char) b);
			wasCr = false;
		}
	}
}