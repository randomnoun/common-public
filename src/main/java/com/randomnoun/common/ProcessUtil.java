package com.randomnoun.common;

/* (c) 2013 randomnoun. All Rights Reserved. This work is licensed under a
 * BSD Simplified License. (http://www.randomnoun.com/bsd-simplified.html)
 */

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.File;

import java.lang.Process;

/** Utility class for running processes, including timeouts, and slightly better exceptions that include more information
 * about process failure. 
 * 
 */
public class ProcessUtil {
    
	/// maximum output; if >0, removes passwords & limits to this amount
	private int maxOutputChars = 8000;
	
	public static int NO_MAX_OUTPUT_CHARS = 0;
	
	public void setMaxOutputChars(int maxOutputChars) {
		this.maxOutputChars = maxOutputChars;
	}
	
	/** Encapsulates an error from executing a command through System.exec()
	 * 
	 */
	public class ProcessException extends Exception {
		
		/** Generated serialVersionUID */
		private static final long serialVersionUID = -6301630237335589674L;
		
		private String command;
		private int exitCode;
		private String stdout;
		private String stderr;
		private String exitCause;

		/** Create a new executable exception
		 * 
		 * @param command Command being executed
		 * @param hostname The host the command was being executed on
		 * @param exitCode The exit code of the program
		 * @param stdout The standard output of the program
		 * @param stderr The error output of the program
		 */
		public ProcessException(String command, String exitCause, int exitCode, String stdout, String stderr) {
			super("Error executing '" + command + "'" + ", cause='" + exitCause + "'");
			
			this.command = command;
			this.exitCode = exitCode;
			this.exitCause = exitCause;
			this.stdout = stdout;
			this.stderr = stderr;
		}
		public String getStdout() { return stdout; }
		public String getStderr() { return stderr; }
		public String getCommand() { return command; }
		public int getExitCode() { return exitCode; }
		public String getExitCause() { return exitCause; }
		public String getMessage() {
			// trim stdout/stderr
			if (maxOutputChars>0) { stdout = Text.getDisplayString("stdout", stdout, 8000); }
			if (maxOutputChars>0) { stderr = Text.getDisplayString("stderr", stderr, 8000); }
			return super.getMessage() +
			  "; " + 
			  (stdout==null ? "" : ", stdout='" + stdout + "'\n") +
		      (stderr==null ? "" : ", stderr='" + stderr + "'\n") +
		      "exitCode=" + exitCode;
		}
	}
	
	

	public String exec(String[] command) throws ProcessException {
		return exec(Text.join(command, " "), -1, null, null, null);
	}
	
	public String exec(String[] command, String[] env) throws ProcessException {
		return exec(Text.join(command, " "), -1, null, env, null);
	}

	@SuppressWarnings("deprecation")
	public String exec(String command, long timeout, InputStream stdin, String[] env, File dir) throws ProcessException {
		Process process;
		try {
			process = Runtime.getRuntime().exec(command, env, dir);
		} catch (IOException ioe) {
			throw (ProcessException) new ProcessException(command, "IOException", 0, "", "").initCause(ioe);
		}
		InputStream stdout = process.getInputStream();
		InputStream stderr = process.getErrorStream();
		OutputStream processStdin = process.getOutputStream();
		ByteArrayOutputStream stdoutByteArrayStream = new ByteArrayOutputStream();
		ByteArrayOutputStream stderrByteArrayStream = new ByteArrayOutputStream();
		
		// could probably use nio these days to remove the .stop()s below
		Thread copyStdoutThread = StreamUtil.copyThread(stdout, stdoutByteArrayStream, 1024);
		Thread copyStderrThread = StreamUtil.copyThread(stderr, stderrByteArrayStream, 1024);
		Thread copyStdinThread = null;
		if (stdin!=null) { 
			copyStdinThread = StreamUtil.copyAndCloseThread(stdin, processStdin, 1024);
			copyStdinThread.start();
		}
		
		copyStdoutThread.start();
		copyStderrThread.start();
		int exitCode = -1;
		boolean throwException = false;
		long interval = 100;
		String cause = "";
		
		try {
			// this didn't work for some reason, but maybe it does now
			// exitCode = process.waitFor();
			
			long	timeWaiting = 0;
			boolean	processFinished = false;
			
			while ((timeout == -1 || timeWaiting < timeout) && !processFinished) {
				processFinished = true;
				Thread.sleep(interval);  // caught below
				
				try {
					exitCode = process.exitValue();
				} catch (IllegalThreadStateException e) {
					// process hasn't finished yet
					processFinished = false;
				}
				timeWaiting += interval;
			}
			
			if (processFinished) {
				// wait for  copy threads to complete
				copyStdoutThread.join();
				copyStderrThread.join();  
				if (copyStdinThread!=null) {
					copyStdinThread.stop();   // can't do much about stdin
				}
				if (exitCode != 0) {
					cause = "non-0 exitCode";
					throwException = true;
				}
			} else {
				cause = "timeout";
				throwException = true;
				process.destroy(); 
				if (copyStdinThread!=null) {
					copyStdinThread.stop();
				}
				copyStderrThread.stop(); // .join() here hangs. should probably set a flag here instead
				copyStdoutThread.stop(); // maybe stop these ?
			}			
			
		} catch (InterruptedException ie) {
			// should probably clean things up here.
			cause = "InterruptedException";
			throwException = true;
			process.destroy();
			if (copyStdinThread!=null) {
				copyStdinThread.stop();
			}
			copyStderrThread.stop();
			copyStdoutThread.stop(); // maybe allow these to continue to completion ?			
		}
		
		if (throwException) {
			throw new ProcessException(
				command, cause, exitCode,  
				stdoutByteArrayStream.toString(), stderrByteArrayStream.toString());
		}
		return stdoutByteArrayStream.toString();
	}	
	
	
	
	
}
