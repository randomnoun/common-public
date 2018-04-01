package com.randomnoun.common;

/* (c) 2013 randomnoun. All Rights Reserved. This work is licensed under a
 * BSD Simplified License. (http://www.randomnoun.com/bsd-simplified.html)
 */

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.lang.Process;

/** Utility class for running processes, including timeouts, remote
 * execution, and slightly better exceptions that include more information
 * about process failure. 
 *  
 * @version $Id$
 */
public class ProcessUtil {
    
    


	/// maximum output; if >0, removes passwords & limits to this amount
	private static int maxOutputChars = 8000;
	
	public static int NO_MAX_OUTPUT_CHARS = 0;
	
	public static String cygPathToWindows(String cygPath) {
		throw new UnsupportedOperationException("not implemented");
	}

	public static void setMaxOutputChars(int maxOutputChars) {
		ProcessUtil.maxOutputChars = maxOutputChars;
	}
	
	public static String windowsPathToCygwin(String winPath) {
		winPath = winPath.replace('\\', '/');
		if (winPath.length() > 2 && winPath.charAt(1) == ':') {
			return "/cygdrive/" + Character.toLowerCase(winPath.charAt(0)) + "/" +
			  winPath.substring(2);
		} else if (winPath.length() > 2 && winPath.startsWith("//")) {
			return winPath;
		} else {
			return winPath;
		}
	}
	
	/** Encapsulates an error from executing a command through System.exec()
	 * 
	 */
	public static class ProcessException extends Exception {
		
		private String command;
		private String hostname;
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
		public ProcessException(String command, String hostname, String exitCause, int exitCode, String stdout, String stderr) {
			super("Error executing '" + command + "'" + (hostname==null ? "" : " on '" + hostname + "'") + ", cause='" + exitCause + "'");
			this.hostname = hostname;
			this.command = command;
			this.exitCode = exitCode;
			this.exitCause = exitCause;
			this.stdout = stdout;
			this.stderr = stderr;
		}
		public String getStdout() { return stdout; }
		public String getStderr() { return stderr; }
		public String getCommand() { return command; }
		public String getHostname() { return hostname; }
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
	
	// @TODO refactor this with the following method, and the method after that :)
	public static String sshExec(String remoteUser, String hostname, String command, long timeout) throws ProcessException {
		Process process;
		try {
			process = Runtime.getRuntime().exec(new String[] {
					"ssh", "-T", "-o", "StrictHostKeyChecking=no", remoteUser + "@" + hostname, command });
		} catch (IOException ioe) {
			throw (ProcessException) new ProcessException(command, hostname, "IOException", 0, "", "").initCause(ioe);
		}
		InputStream stdout = process.getInputStream();
		InputStream stderr = process.getErrorStream();
		ByteArrayOutputStream stdoutByteArrayStream = new ByteArrayOutputStream();
		ByteArrayOutputStream stderrByteArrayStream = new ByteArrayOutputStream();
		Thread copyThread = StreamUtil.copyThread(stdout, stdoutByteArrayStream, 1024);
		Thread copyErrorThread = StreamUtil.copyThread(stderr, stderrByteArrayStream, 1024);
		copyThread.start();
		copyErrorThread.start();
		int exitCode = -1;
		boolean throwException = false;
		long interval = 100;
		String cause = "";
		
		try {
			// exitCode = process.waitFor();
			// if (exitCode != 0) { throwException = true; }
			// copyThread.join(); // wait for copy thread to complete
			long	timeWaiting = 0;
			boolean	processFinished = false;
			
			while (timeWaiting < timeout && !processFinished) {
				processFinished = true;
				Thread.sleep(interval);
				try {
					exitCode = process.exitValue();
				} catch (IllegalThreadStateException e) {
					processFinished = false;
				}
				timeWaiting += interval;
			}
			
			if (processFinished) {
				// wait for  copy threads to complete
				copyThread.join();
				copyErrorThread.join();  
				if (exitCode != 0) {
					cause = "non-0 exitCode";
					throwException = true;
				}
			} else {
				cause = "timeout";
				throwException = true;
				process.destroy();
				copyErrorThread.join();
				copyThread.join();
			}			
			
		} catch (InterruptedException ie) {
			cause = "InterruptedException";
			throwException = true;
			process.destroy();
			copyErrorThread.stop();
			copyThread.stop();
		}
		
		if (throwException) {
			throw new ProcessException(
				command, hostname, cause, exitCode,  
				stdoutByteArrayStream.toString(), stderrByteArrayStream.toString());
		}
		return stdoutByteArrayStream.toString();
	}
	
	
	// @TODO refactor this with the following method
	public static String sshExec(String remoteUser, String hostname, String command) throws ProcessException {
		Process process;
		try {
			process = Runtime.getRuntime().exec(new String[] {
					"ssh", "-T", "-o", "StrictHostKeyChecking=no", remoteUser + "@" + hostname, command });
		} catch (IOException ioe) {
			throw (ProcessException) new ProcessException(command, hostname, "IOException", 0, "", "").initCause(ioe);
		}
		InputStream stdout = process.getInputStream();
		InputStream stderr = process.getErrorStream();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
		Thread copyThread = StreamUtil.copyThread(stdout, baos, 1024);
		Thread copyErrorThread = StreamUtil.copyThread(stderr, baos2, 1024);
		copyThread.start();
		copyErrorThread.start();
		int exitCode = 0;
		boolean throwException = false;
		String cause = "";
		
		try {
			exitCode = process.waitFor(); 
			if (exitCode != 0) { throwException = true; cause = "non-0 exitCode"; }
			copyThread.join(); 
			copyErrorThread.join();
		} catch (InterruptedException ie) {
			throwException = true;
			cause = "InterruptedException";
			process.destroy();
			copyErrorThread.stop();
			copyThread.stop();
		}
		if (throwException) {
			throw new ProcessException(
				command, hostname, cause, exitCode,  
				baos.toString(), baos2.toString());
		}
		return baos.toString();
	}

	public static String exec(String[] command) throws ProcessException {
		return sshExec(null, null, Text.join(command, " "), -1, null, null);
	}
	
	public static String exec(String[] command, String[] env) throws ProcessException {
		return sshExec(null, null, Text.join(command, " "), -1, null, env);
	}

	public static String exec(String[] command, long timeout) throws ProcessException {
		return sshExec(null, null, Text.join(command, " "), timeout, null, null);
	}
	
	public static String exec(String[] command, long timeout, InputStream stdin) throws ProcessException {
		return sshExec(null, null, Text.join(command, " "), timeout, stdin, null);
	}	

	public static String sshExec(String remoteUser, String hostname, String command, long timeout, InputStream stdin) throws ProcessException {
		return sshExec(remoteUser, hostname, command, timeout, stdin, null);
	}
	
	
	// so Java 1.7 now has this thing called a ProcessBuilder, which
	// should hopefully make all of this much, much simpler.
	// Actually, no. No it doesn't.
	
	// see https://docs.oracle.com/javase/7/docs/api/java/lang/ProcessBuilder.html
	private static String sshExec(String remoteUser, String hostname, String command, long timeout, InputStream stdin, String[] env) throws ProcessException {
		Process process;
		try {
			if (hostname == null) {
				if (env==null) {
					process = Runtime.getRuntime().exec(command);
				} else {
					process = Runtime.getRuntime().exec(command, env);
				}
			} else {
				if (env != null) {
					throw new IllegalArgumentException("Cannot pass environment to remote process");
				}
				process = Runtime.getRuntime().exec(new String[] {
						"ssh", "-T", "-o", "StrictHostKeyChecking=no", remoteUser + "@" + hostname, command }); 
			}
		} catch (IOException ioe) {
			throw (ProcessException) new ProcessException(command, hostname, "IOException", 0, "", "").initCause(ioe);
		}
		InputStream stdout = process.getInputStream();
		InputStream stderr = process.getErrorStream();
		OutputStream processStdin = process.getOutputStream();
		ByteArrayOutputStream stdoutByteArrayStream = new ByteArrayOutputStream();
		ByteArrayOutputStream stderrByteArrayStream = new ByteArrayOutputStream();
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
			// yes, I've tried putting a timeout in here
			// exitCode = process.waitFor();  // @TODO something with this
			// if (exitCode != 0) { throwException = true; }
			// copyThread.join(); // wait for copy thread to complete
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
				command, hostname, cause, exitCode,  
				stdoutByteArrayStream.toString(), stderrByteArrayStream.toString());
		}
		return stdoutByteArrayStream.toString();
	}	
	
	
	
	
}
