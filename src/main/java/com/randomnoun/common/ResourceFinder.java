package com.randomnoun.common;

/* (c) 2013 randomnoun. All Rights Reserved. This work is licensed under a
 * BSD Simplified License. (http://www.randomnoun.com/bsd-simplified.html)
 */

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;

import org.apache.log4j.Logger;

/** Find a resource recursively through all JARs, EARs, WARs, etc from
 * the current directory down.
 * 
 * <p>Command-line usage</p>
 *
 * <p>The following command-line arguments are recognised 
 *
<table>
<tr><th> -h -?     <td>displays this helptext
<tr><th> -f        <td>follow symlinks
<tr><th> -a        <td>show all resources found (i.e. do not use searchTerm)
<tr><th>
<tr><th colspan="2">Search criteria:
<tr><th> -i        <td>case-insensitive match
<tr><th> -sc       <td>if present, searchTerm matches within filename (default)
<tr><th> -ss       <td>if present, searchTerm matches start of filename
<tr><th> -se       <td>if present, searchTerm matches exact filename
<tr><th> -sr       <td>if present, searchTerm matches filename as a regular expression
<tr><th> -mf n     <td>max filesystem folder depth (0 = do not descend into subfolders)
<tr><th> -ma n     <td>max archive depth (0 = do not descend into archives)
<tr><th> -x        <td>if present, will attempt to recover if errors occur reading archives
                       (errors sent to stderr)
<tr><th>
<tr><th colspan="2">Action when resource found:
<tr><th> -v        <td>verbose; display filenames with file sizes and timestamps
<tr><th> -vv       <td>display MD5/SHA1 hashes of resources (NB: modifies display order)
<tr><th> -d n      <td>dump/display the contents of the n'th resource found
<tr><th> -d all    <td>dump the name and contents of all resources found
<tr><th> -d names  <td>dump just the names of all resources found (default)
<tr><th> -d n1,n2...<td>dump the name and contents of the n1'th, n2'nd etc... resources found
<tr><th> -dm n|all <td>as per -d, but performs manifest unmangling on resource (fixes linewraps)
<tr><th> -dj n|all <td>as per -d, but performs class decompiling (requires jad to be in PATH)
<tr><th> -c text   <td>search for text in contents of resource (uses UTF-8 encoding)
<tr><th> -ci text  <td>case-insensitive search for text in contents of resources
</table>

 * 
 * @TODO split CLI functionality into separate class
 * @TODO pass enough information to the callback classes to display somewhat sane progress bar 
 * @TODO fix -dj switch + handle inner classes
 * @TODO rewrite jad to deal with annotations and other 1.5+ crap
 * @TODO -cs switches to change search behaviour within content
 * 
 * @author knoxg
 * @version $Id$
 */
public class ResourceFinder {

	/** CVS revision identifier */
	public static final String _revision = "$Id$";

	/** Logger instance for this class */
	Logger logger = Logger.getLogger(ResourceFinder.class);

	/** Match type used in {@link #matches(String)}} comparisons that tests whether
	 * the last component of a resource name contains a specified string; e.g. 
	 * "abc/def.txt" will match against the searchTerm "ef" using this matchType.
	 */
	public final static int MATCHTYPE_CONTAINS = 0;
	
	/** Match type used in {@link #matches(String)}} comparisons that tests whether
	 * the last component of a resource name starts with a specified string; e.g. 
	 * "abc/def.txt" will match against the searchTerm "de" using this matchType.
	 */
	public final static int MATCHTYPE_STARTSWITH = 1;
	
	/** Match type used in {@link #matches(String)}} comparisons that tests whether
	 * the last component of a resource name is equal to a specified regular expression; e.g. 
	 * "abc/def.txt" will match against the searchTerm "def.txt" using this matchType.
	 */
	public final static int MATCHTYPE_EXACT = 2;

	/** Match type used in {@link #matches(String)}} comparisons that tests whether
	 * the last component of a resource name matches a specified regular expression; e.g. 
	 * "abc/def.txt" will match against the searchTerm ".*e.*" using this matchType.
	 */
	public final static int MATCHTYPE_REGEX = 3;

	/** Resource being searched for */
	private String searchTerm;
	
	/** If performing regex searches, the Pattern form of {@link #searchTerm} */
	private Pattern searchPattern;
	
	/** File or directory from which search begins. If this is null, {@link #startInputStream} must be non-null, and vice versa */
	private File startDirectory;
	
	/** ZipInputStream from which search begins.  If this is null, {@link #startDirectory} must be non-null, and vice versa*/ 
	private ZipInputStream startInputStream;
	
	/** A MATCHTYPE_* constant. */
	private int matchType;

	/** If true, performs a case-insensitive match */
	private boolean ignoreCase = false;
	
	/** If false, will prevent recursive search from following symbolic links */ 
	private boolean followSymlinks = false;

	/** If true, will invoke the ResourceFinderCallback for every file in every archive iterated over 
	 * (i.e. the {@link #searchTerm} will be ignored) */ 
	private boolean showAll = false;
	
	/** If true, will attempt to recover processing after reading an invalid ZIP entry */
	private boolean ignoreErrors = false;
	
	/** Maximum depth; -1 = no depth limit. See {@link #setMaxArchiveDepth(long)}. <i>(Not implemented)</i>*/
	private long maxArchiveDepth = -1;
	
	/** Maximum folder depth; -1 = no depth limit. See {@link #setMaxFolderDepth(long)}. */
	private long maxFolderDepth = -1;
	
	/** Current archive depth */
	private long currentArchiveDepth = -1;
	
	/** Current folder depth */
	private long currentFolderDepth = -1;
	
	/** Callback to be invoked on every resource that matches the search criteria */
	private ResourceFinderCallback callback;
	
	/** If set to true, allows the search to be aborted whilst it is in progress */
	private transient boolean abort = false;
	
	/** Regex to define which files will be opened via ZipInputStream. Will return true if the file ends with
	 * .zip, .sar, .jar, .war, .ear, .rar or .har. These are Java RARs (resource archives), not the other type
	 * of RAR. */
	private Pattern isArchivePattern = Pattern.compile(
		".*\\.([Zz][Ii][Pp]|" +
		"[SsJjWwEeRrHh][Aa][Rr])$");
	
	/** Call this method within a {@link ResourceFinderCallback} to stop looking for resources */
	public void abort() { this.abort = true; }
	
	/** Return the file or directory from which search begins. If this returns null, try {@link #getStartInputStream()} */
	public File getStartDirectory() { return startDirectory; }
	
	/** Return the ZipInputStream from which search begins.  If this returns null, try {@link #getStartDirectory()} */ 
	public InputStream getStartInputStream() { return startInputStream; }
	
	/** Tests a resource name against the search criteria specified in this object
	 * 
	 * @param resourceName the last component of a resource name
	 * 
	 * @return true if the resource passes the search criteria, false otherwise
	 */
	public boolean matches(String resourceName) {
		if (resourceName==null) { throw new NullPointerException("null string"); }
		if (ignoreCase) {
			switch(matchType) {
				case MATCHTYPE_EXACT: return searchTerm.equalsIgnoreCase(resourceName);
				case MATCHTYPE_REGEX: return searchPattern.matcher(resourceName).find();
				case MATCHTYPE_STARTSWITH: return resourceName.toUpperCase().startsWith(searchTerm);
				case MATCHTYPE_CONTAINS: return resourceName.toUpperCase().contains(searchTerm);
				default: throw new IllegalStateException("Illegal matchType '" + matchType + "'");
			}
		} else {
			switch(matchType) {
				case MATCHTYPE_EXACT: return searchTerm.equals(resourceName);
				case MATCHTYPE_REGEX: return searchPattern.matcher(resourceName).find();
				case MATCHTYPE_STARTSWITH: return resourceName.startsWith(searchTerm);
				case MATCHTYPE_CONTAINS: return resourceName.contains(searchTerm);
				default: throw new IllegalStateException("Illegal matchType '" + matchType + "'");
			}
		}
	}
	

	/** An {@link java.io.InputStream} wrapper which updates an internal md5/sha1 digest
	 * as the stream is being read.
	 */
	public static class HashGeneratingInputStream extends InputStream {
		InputStream is;
		MessageDigest algorithm1, algorithm2;
		
		public HashGeneratingInputStream(InputStream is) {
			this.is = is;
			try {
				algorithm1 = MessageDigest.getInstance("MD5");
				algorithm2 = MessageDigest.getInstance("SHA-1");
			} catch (NoSuchAlgorithmException nsae) {
				throw (IllegalStateException) new IllegalStateException(
					"Invalid crypto config").initCause(nsae);
			}
    		algorithm1.reset();
    		algorithm2.reset();

		}
		
		@Override
		public int read() throws IOException {
			int result = is.read();
			if (result != -1) {
				algorithm1.update((byte) result);
				algorithm2.update((byte) result);
			}
			return result;
		}
		public int available() throws IOException {
			return is.available();
		}
		public void close() {
			// ignored;
		}
		public void mark(int readlimit) {
			is.mark(readlimit);
		}
        public boolean markSupported() {
        	return is.markSupported();
        }
        public int read(byte[] b)  throws IOException {
        	int result = is.read(b);
        	if (result!=-1) {
        		algorithm1.update(b, 0, result);
        		algorithm2.update(b, 0, result);
        	}
        	return result;
        }
        public int read(byte[] b, int off, int len)  throws IOException {
        	int result = is.read(b, off, len);
        	if (result!=-1) {
        		algorithm1.update(b, off, result);
        		algorithm2.update(b, off, result);
        	}
			return result;
        }
        public void reset() throws IOException {
        	is.reset();
        }
        public long skip(long n) throws IOException {
        	return is.skip(n);
        }
        /** Returns the MD5 digest of all input that has been read by this InputStream so far,
         * in a hexadecimal String form */
        public String getMd5() {
    		byte messageDigest[] = algorithm1.digest();
    		//System.err.println("md5 messageDigest is " + messageDigest.length + " bytes");
    		StringBuffer hexString = new StringBuffer();
    		for (int i=0;i<messageDigest.length;i++) {
    			hexString.append(Integer.toString( ( messageDigest[i] & 0xff ) + 0x100, 16).substring( 1 ));
    		}
    		return hexString.toString();
        }
        /** Returns the SHA1 digest of all input that has been read by this InputStream so far,
         * in a hexadecimal String form */
		public String getSha1() {
    		byte messageDigest[] = algorithm2.digest();
    		//System.err.println("sha1 messageDigest is " + messageDigest.length + " bytes");
    		StringBuffer hexString = new StringBuffer();
    		for (int i=0;i<messageDigest.length;i++) {
    			hexString.append(Integer.toString( ( messageDigest[i] & 0xff ) + 0x100, 16).substring( 1 ));
    		}
    		return hexString.toString();
        }
	}
	

	public static class ResourceFinderCallbackResult {
		boolean abort = false;
		InputStream replaceInputStream = null;
	}
	
	/** A callback interface used when resources are found using this object
	 * 
	 */
	public interface ResourceFinderCallback {

		/** This method is only invoked for archive resources, before that archive has been read or
		 * recursed into. Both archives and standard files will be passed to the 
		 * {@link #postProcess(String, long, long, InputStream)} method.  
		 * 
		 * @param resourceName full resource name
		 * @param filesize the size of the (uncompressed) resource, or -1 if this is unknown
		 *   (some archives do not store this information) 
		 * @param timestamp the timestamp of the resource 
		 * @param inputStream an inputStream which can be used to retrieve the contents
		 *   of the resource
		 *   
		 * @return a ResourceFinderCallbackResult which can be used to abort the search or 
		 *   modify/wrap the inputStream being searched.
		 * 
		 * @throws IOException
		 */
		public ResourceFinderCallbackResult preProcess(String resourceName, long filesize, long timestamp, 
			InputStream inputStream) throws IOException;

		/** This method is invoked for each resource that matches the search criteria
		 * specified in the containing {@link ResourceFinder} class. This method
		 * is not responsible for closing the supplied inputStream.
		 *  
		 * @param resourceName full resource name
		 * @param filesize the size of the (uncompressed) resource, or -1 if this is unknown
		 *   (some archives do not store this information) 
		 * @param timestamp the timestamp of the resource 
		 * @param inputStream an inputStream which can be used to retrieve the contents
		 *   of the resource
		 * 
		 * @return a ResourceFinderCallbackResult which can be used to abort the search
		 * 
		 * @throws IOException if an operation on the <tt>inputStream</tt> fails 
		 */
		public ResourceFinderCallbackResult postProcess(String resourceName, long filesize, long timestamp, 
			InputStream inputStream, ResourceFinderCallbackResult preProcessResult) throws IOException;
		
	}

	/** Class which defines a callback which sends names and resource hashes to System.out.
	 */
	public static class HashingResourceFinderCallback implements ResourceFinderCallback {
		
		int resourceIndex = 0;
		boolean showHashes = false;
	
		public HashingResourceFinderCallback() {
		}
		
		public ResourceFinderCallbackResult preProcess(String resourceName, long filesize, long timestamp, InputStream inputStream) throws IOException {
			ResourceFinderCallbackResult rfcbr = new ResourceFinderCallbackResult();
			rfcbr.replaceInputStream = new HashGeneratingInputStream(inputStream);
			return rfcbr;
		}
		
		public ResourceFinderCallbackResult postProcess(String resourceName, long filesize, long timestamp, InputStream inputStream, ResourceFinderCallbackResult preProcessResult) throws IOException {
			ResourceFinderCallbackResult rfcbr = new ResourceFinderCallbackResult();
			try {
				SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
				String md5, sha1;
				if (inputStream instanceof HashGeneratingInputStream) {
					HashGeneratingInputStream hgis = (HashGeneratingInputStream) inputStream;
					// pump the rest of the bits through this stream
					byte[] buffer = new byte[4096];
					while (inputStream.read(buffer) != -1) { /* nothing */ }
					md5 = hgis.getMd5();
					sha1 = hgis.getSha1();
				} else {
					MessageDigest algorithm1, algorithm2;
					try {
						algorithm1 = MessageDigest.getInstance("MD5");
						algorithm2 = MessageDigest.getInstance("SHA1");
					} catch (NoSuchAlgorithmException nsae) {
						throw (IllegalStateException) new IllegalStateException(
							"Invalid crypto config").initCause(nsae);
					}
		    		algorithm1.reset();
		    		algorithm2.reset();
					byte[] buffer = new byte[4096];
					int bytesRead;
					while ((bytesRead = inputStream.read(buffer)) != -1) {
						algorithm1.update(buffer, 0, bytesRead);
						algorithm2.update(buffer, 0, bytesRead);
			        }
		    		byte messageDigest1[] = algorithm1.digest();
		    		byte messageDigest2[] = algorithm2.digest();
		    		StringBuffer hexString1 = new StringBuffer();
		    		StringBuffer hexString2 = new StringBuffer();
		    		for (int i=0; i<messageDigest1.length; i++) {
		    			hexString1.append(Integer.toString( ( messageDigest1[i] & 0xff ) + 0x100, 16).substring( 1 ));
		    		}
		    		for (int i=0; i<messageDigest2.length; i++) {
		    			hexString2.append(Integer.toString( ( messageDigest2[i] & 0xff ) + 0x100, 16).substring( 1 ));
		    		}
		    		md5 = hexString1.toString();
		    		sha1 = hexString2.toString();
				}
	    		System.out.println("[" + resourceIndex + "] " + resourceName + " " + (filesize==-1 ? "(unknown)" : String.valueOf(filesize)) + 
	    			" " + sdf.format(new Date(timestamp)) + " " + md5 + " " + sha1 );
			} catch (IllegalArgumentException iae) {
				// not sure if this is needed any more
				throw new IOException("IllegalArgumentException processing ZipInputStream", iae);
			}
			resourceIndex++;
			return rfcbr;
		}
	}

	
	/** Class which defines a callback which sends names and resources to System.out.
	 * 
	 * <p>This class uses '#' as a separator between the filesystem and files contained
	 * within archives; e.g. test.jar#abc.txt refers to abc.txt in test.jar.
	 * 
	 * <p>For comparison, Tangosol seems to use '!', includes a leading slash and 
	 * includes a protocol-like identifier at the beginning
	 * (e.g. jar:file:test.jar!/abc.txt). If a constructor is supplied which only provides
	 * a ZipInputStream (i.e. no filename is available), then resources will be returned 
	 * starting with the '#' character.
	 * 
	 */
	public static class DisplayResourceFinderCallback implements ResourceFinderCallback {
		
		public final static int DUMP_NAMES = -1;
		public final static int DUMP_NAMES_AND_RESOURCES = -2;
		public final static int DUMP_RESOURCES = 0;
	
		int dumpType = 0;
		List<Integer> dumpResourceNumbers;
		int maxDumpResourceNumber = -1;
		int resourceIndex = 0;
		boolean verbose = false;
		boolean manifests = false;
		boolean decompile = false;
		String searchContents = null;
		boolean searchContentsIgnoreCase = false;

		// with all the trimmings
		public DisplayResourceFinderCallback(int dumpType, List<Integer> dumpResourceNumbers, boolean verbose, boolean manifests, boolean decompile, String searchContents, boolean searchContentsIgnoreCase) {
			// System.out.println("2 searchContents is " + searchContents);
			this.dumpResourceNumbers = dumpResourceNumbers;
			this.dumpType = dumpType;
			this.verbose = verbose;
			this.manifests = manifests;
			this.decompile = decompile;
			this.searchContents = searchContents;
			this.searchContentsIgnoreCase = searchContentsIgnoreCase;
			if (dumpResourceNumbers != null) {
				for (Integer drn : dumpResourceNumbers) {
					maxDumpResourceNumber = Math.max(maxDumpResourceNumber, drn.intValue());
				}
			}
		}
		
		private ResourceFinderCallbackResult dump(String resourceName, long filesize, long timestamp, InputStream inputStream) throws IOException {
			ResourceFinderCallbackResult rfcr = new ResourceFinderCallbackResult(); 
			if ((dumpType == DUMP_NAMES_AND_RESOURCES || dumpType == DUMP_NAMES) &&
				(dumpResourceNumbers==null || dumpResourceNumbers.contains(new Integer(resourceIndex)))
			   )
			{
				if (verbose) {
					SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
					System.out.println("[" + resourceIndex + "] " + resourceName + " " + (filesize==-1 ? "(unknown)" : String.valueOf(filesize)) + " " + sdf.format(new Date(timestamp)));
				} else if (searchContents == null) {
					System.out.println("[" + resourceIndex + "] " + resourceName);
				} else if (searchContents != null) {
					int pos = -1;
					// @TODO this assumes we never search for strings containing newlines
					LineNumberReader lnr = new LineNumberReader(new InputStreamReader(inputStream));
					if (searchContentsIgnoreCase) {
						searchContents = searchContents.toLowerCase();
						String line = lnr.readLine();
						if (line!=null) {
							pos = line.toString().toLowerCase().indexOf(searchContents.toLowerCase());
							while (line!=null && pos==-1) {
								line = lnr.readLine();
								pos = line==null ? -1 : line.toString().toLowerCase().indexOf(searchContents.toLowerCase());
							}
						}
					} else {
						String line = lnr.readLine();
						if (line!=null) {
							pos = line.toString().indexOf(searchContents);
							while (line!=null && pos==-1) {
								line = lnr.readLine();
								pos = line==null ? -1 :  line.toString().indexOf(searchContents);
							}
						}
					}
					if (pos!=-1) {
						System.out.print("[" + resourceIndex + "] [line " + lnr.getLineNumber() + ", col " + pos + "] " + resourceName);
					}
				}
			}  
			if ((dumpType == DUMP_NAMES_AND_RESOURCES || dumpType == DUMP_RESOURCES) &&
				(dumpResourceNumbers==null || dumpResourceNumbers.contains(new Integer(resourceIndex)))
				) {
				// InputStream is = ResourceFinder.getResourceStream(resourceName);
				if (manifests) {
					BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
					String line = br.readLine();
					while (line != null) {
						int len = line.length();
						while (line.length() > 0 && line.charAt(0) == ' ') {
							line = line.substring(1);
						}
						System.out.print(line);
						if (len != 70) { System.out.println(); }
						line = br.readLine();
					}
				} else if (decompile) {
					// hopefully this is deleted when the VM exits
					// @TODO: we need to grab all inner classes for this class as well
					throw new UnsupportedOperationException("Decompilation is disabled until I bring 'ProcessUtil' into this package. jad hasn't worked in years, anyway.");
					/*
					File tmpFile = File.createTempFile("resourceFinder", ".class");
					FileOutputStream fos = new FileOutputStream(tmpFile); 
					StreamUtil.copyStream(inputStream, fos, 1024);
					fos.close();
					try {
						String result = ProcessUtil.exec(new String[] { "jad", "-lnc", "-p", tmpFile.getCanonicalPath() });
						System.out.println(result);
					} catch (ProcessUtil.ProcessException pe) {
						throw (IOException) new IOException("Problem executing jad").initCause(pe);
					}
					*/
					
				} else {
					StreamUtil.copyStream(inputStream, System.out, 1024);
				}
				if (resourceIndex == maxDumpResourceNumber) {
					// don't bother continuing this search if we're found the last resource being searched for
					rfcr.abort = true;
				}

				// we don't insert an additional newline when dumping the contents of just one file
				// so that stdout redirection still does something useful
				if (dumpType == DUMP_NAMES_AND_RESOURCES && 
					(dumpResourceNumbers==null || dumpResourceNumbers.size()>1)) {
					System.out.println();
				}
			}
			resourceIndex++;
			return rfcr;
		}

		public ResourceFinderCallbackResult preProcess(String resourceName, long filesize, long timestamp, InputStream inputStream) throws IOException {
			return dump(resourceName, filesize, timestamp, inputStream);
		}
		
		// won't be needing this one
		public ResourceFinderCallbackResult postProcess(String resourceName, long filesize, long timestamp, InputStream inputStream, ResourceFinderCallbackResult preProcessResult) throws IOException {
			if (preProcessResult==null) { return dump(resourceName, filesize, timestamp, inputStream); }
			return null;
		}
		
		
	}
	
	/** Creates a new resource finder object
	 * 
	 * @param searchTerm resource being searched for
	 * @param matchType a MATCHTYPE_* constant denoting how the searchTerm is to be used to match against resource names
	 * @param ignoreCase if true, will perform a case insensitive search 
	 * @param startDirectory directory from which search begins
	 * @param callback callback to be invoked on every resource that matches the search criteria
	 * 
	 * @throws IOException if the start directory is invalid
	 */
	public ResourceFinder(String searchTerm, int matchType, boolean ignoreCase, File startDirectory, ResourceFinderCallback callback) throws IOException {
		init(searchTerm, matchType, ignoreCase, callback);
		this.startDirectory = startDirectory.getCanonicalFile(); // for symlink test

	}
	
	/** Common code to both the File and ZipInputStream constructors */
	private void init(String searchTerm, int matchType, boolean ignoreCase, 
		ResourceFinderCallback callback) 
	{
		this.matchType = matchType;
		this.ignoreCase = ignoreCase;
		this.showAll = false;
		this.ignoreErrors = false;
		this.callback = callback;

		// @TODO clean this up a bit
		if (ignoreCase) {
			switch (matchType) {
				case MATCHTYPE_EXACT: break;
				case MATCHTYPE_REGEX: searchPattern = Pattern.compile(searchTerm, Pattern.CASE_INSENSITIVE); break;
				case MATCHTYPE_STARTSWITH: searchTerm = searchTerm.toUpperCase(); break;
				case MATCHTYPE_CONTAINS: searchTerm = searchTerm.toUpperCase(); break;
				default: throw new IllegalStateException("Illegal matchType '" + matchType + "'");
			}
		} else {
			switch (matchType) {
				case MATCHTYPE_EXACT: break;
				case MATCHTYPE_REGEX: searchPattern = Pattern.compile(searchTerm); break;
				case MATCHTYPE_STARTSWITH: break;
				case MATCHTYPE_CONTAINS: break;
				default: throw new IllegalStateException("Illegal matchType '" + matchType + "'");
			}
		}
		this.searchTerm = searchTerm;
		
	}

	/** Sets whether to follow symbolic links during filesystem scans. By default symlinks will not be followed.
	 * 
	 * @see #getFollowSymlinks()
	 * 
	 * @param followSymlinks true if symbolic links should be followed during filesystem scans, false otherwise 
	 */
	public void setFollowSymLinks(boolean followSymlinks) {
		this.followSymlinks = followSymlinks;
	}
	
	/** Returns whether symbolic links will be followed during filesystem scans
	 *
	 * @see #setFollowSymLinks(boolean)
	 *  
	 * @return whether symbolic links will be followed during filesystem scans
	 */
	public boolean getFollowSymlinks() {
		return followSymlinks;
	}
	
	/** Sets whether the ResourceFinderCallback should be invoked for every file in every archive iterated over 
	 * (i.e. to ignore the {@link #searchTerm} ). By default this flag is set to false.
	 * 
	 * @see #getShowAll()
	 * 
	 * @param showAll if true, will invoke the ResourceFinderCallback for every file in every archive iterated over 
	 */
	public void setShowAll(boolean showAll) {
		this.showAll = showAll;
	}
	
	/** Returns whether the ResourceFinderCallback will be invoked for every file in every archive iterated over
	 * 
	 * @see #setShowAll(boolean)
	 * 
	 * @return true if the ResourceFinderCallback will be invoked for every file in every archive iterated over, false otherwise
	 */
	public boolean getShowAll() {
		return showAll;
	}
	
	/** Sets whether to ignore (some) exceptions encountered whilst processing ZipInputStreams. 
	 * 
	 * <p>Only EOFExceptions, ZipExceptions, IllegalArgumentExceptions and the push-back buffer 
	 * IOException will be ignored if this flag is set. Ignored exceptions will still be logged.
	 * 
	 * <p>By default, this flag is set to false.
	 *
	 * @see #getIgnoreErrors()
	 * 
	 * @param ignoreErrors true to ignore exceptions as described above, false otherwise
	 */
	public void setIgnoreErrors(boolean ignoreErrors) {
		this.ignoreErrors = ignoreErrors;
	}
	
	/** Returns true if exceptions will be ignored whilst processing ZipInputStreams
	 * 
	 * @see #setIgnoreErrors(boolean)
	 * 
	 * @return true if exceptions will be ignored whilst processing ZipInputStreams
	 */
	public boolean getIgnoreErrors() {
		return ignoreErrors;
	}

	
	/** Creates a new resource finder object
	 * 
	 * @param searchTerm resource being searched for
	 * @param matchType a MATCHTYPE_* constant denoting how the searchTerm is to be used to match against resource names
	 * @param ignoreCase if true, will perform a case insensitive search 
	 * @param startDirectory directory from which search begins
	 * @param callback callback to be invoked on every resource that matches the search criteria 
	 * 
	 * @throws IOException if the start directory is invalid
	 */
	public ResourceFinder(String searchTerm, int matchType, boolean ignoreCase, ZipInputStream startInputStream, ResourceFinderCallback callback) throws IOException {
		init(searchTerm, matchType, ignoreCase, callback);
		this.startInputStream = startInputStream;
	}	
	
	/** Searches and returns a list of resources matching the criteria defined
	 * in the constructor
	 * 
	 * @TODO the list returned by this object probably isn't accurate.
	 * 
	 * @return a List of Strings, in the syntax defined by the class javadoc
	 * 
	 * @throws IOException
	 */
	public void find() throws IOException {
		this.currentArchiveDepth = -1; // yet to enumerate initial folder / stream
		if (startInputStream != null) {
			//List<String> result = new ArrayList<String>();
			findResourceInZip(startInputStream, "#");
			return;
			
		} else if (startDirectory.isFile()) {
			//List<String> result = new ArrayList<String>();
			File file = startDirectory;
			String name = file.getName();
			if (!followSymlinks && isLink(file )) {
				// ignore symlinks
				// System.out.println("shazbot");
			} else if (matches(name)) {
				FileInputStream fis = new FileInputStream(file);
				callback.postProcess(name, file.length(), file.lastModified(), fis, null);
				fis.close();
				// perhaps make this another switch
				/*
				if (isArchive(name)) {
					ZipInputStream zis = new ZipInputStream(new FileInputStream(file));
					result.addAll(findResourceInZip(zis,  name + "#"));
				}
				*/
			} else if (isArchive(name)) {
				if (showAll) {
					FileInputStream fis = new FileInputStream(file);
					callback.postProcess(name, file.length(), file.lastModified(), fis, null);
					fis.close();
				}
				ZipInputStream zis = new ZipInputStream(new FileInputStream(file));
				zis.close();
			} else {
				if (showAll) {
					FileInputStream fis = new FileInputStream(file);
					callback.postProcess(name, file.length(), file.lastModified(), fis, null);
					fis.close();
				}
			}
			return;
		} else {
			findResourceInFolder(startDirectory, "");
		}
	}

	/** Returns true if the filename will be treated as an archive
	 * 
	 * @param name a filename
	 * 
	 * @return true if the file is an archive, false otherwise
	 */
	public boolean isArchive(String name) {
		return isArchivePattern.matcher(name).matches();
	}

	/** Determines whether a file is a symbolic link. 
	 * (Copied from http://www.idiom.com/~zilla/Xfiles/javasymlinks.html)
	 *
	 *  @param file file to test
	 *  
	 *  @return true if the file is a symbolic link, false otherwise
	 */
	public static boolean isLink(File file) throws IOException {
		try {
	        if (!file.exists()) {
	    	    return true;
	        } else {
			    String cnnpath = file.getCanonicalPath();
			    String abspath = file.getAbsolutePath();
			    return !abspath.equals(cnnpath);
			}
	    } catch(IOException ex) {
	        //System.err.println(ex);
	        return true;
	    }
	}

	/** Returns a list of resources within the supplied folder, subfolders,
	 * and archives contained within these folders
	 * 
	 * @param folder the folder to search from
	 * @param prefix a prefix which is included in any results returned by this
	 *   method
	 * 
	 * @return a List of Strings, in the syntax defined in the class javadoc
	 * 
	 * @throws IOException 
	 */
	public void findResourceInFolder(File folder, String prefix) throws IOException  {
		if (maxArchiveDepth!=-1 && currentArchiveDepth>=maxArchiveDepth) {
			return;
		}
		// System.err.println("findResourceInFolder(" + prefix + "):" + currentArchiveDepth);
		currentArchiveDepth++;
		
		File[] folderContents = folder.listFiles();
		if (folderContents != null) {
			for (File file : folderContents) {
	
				// don't think any of these are going to work if we're calculating hashes as well.
				// maybe it will. who knows.
				
				String name = file.getName();
				// System.out.println("Filetest '" + name + "' against '" + resourceName + "' (fs=" + followSymlinks + ")");
				if (!followSymlinks && isLink(file)) {
					// ignore symlinks
					// System.out.println("shazbot");
				} else if (file.isDirectory() && (maxFolderDepth==-1 || currentFolderDepth+1 <= maxFolderDepth)) {
					currentFolderDepth++;
					findResourceInFolder(file, prefix + name + "/");
					currentFolderDepth--;
					
				} else if (matches(name)) {
					FileInputStream fis = new FileInputStream(file);
					callback.postProcess(prefix + name, file.length(), file.lastModified(), fis, null);
					fis.close();
					// perhaps make this another switch
					if (isArchive(name)) {
						fis = new FileInputStream(file);
						ZipInputStream zis = new ZipInputStream(fis);
						findResourceInZip(zis, prefix + name + "#");
						fis.close();
					}
				} else if (isArchive(name)) {
					if (showAll) {
						FileInputStream fis = new FileInputStream(file);
						callback.postProcess(prefix + name, file.length(), file.lastModified(), fis, null);
						fis.close();
					}
					FileInputStream fis = new FileInputStream(file);
					ZipInputStream zis = new ZipInputStream(fis);
					findResourceInZip(zis, prefix + name + "#");
					fis.close();
				} else {
					if (showAll) {
						FileInputStream fis = new FileInputStream(file);
						callback.postProcess(prefix + name, file.length(), file.lastModified(), fis, null);
						fis.close();
					}
				}
				if (abort) { break; }
			}
		}

		currentArchiveDepth--;
	}

	/** If ignoreErrors is true, send a message to stderr with the
	 * exception message, otherwise throw an encapsulated ZipException
	 * 
	 * @param message message describing exception
	 * @param e cause of the exception
	 */
	public void ignorableException(String message, Exception e) throws ZipException {
		if (ignoreErrors) {
			logger.error(message, e);
		} else {
			throw (ZipException) new ZipException(message).initCause(e);
		}
	}
	
	/** Returns a list of resources within the supplied archive, 
	 * and archives contained within this archive
	 * 
	 * @param zipInputStream the archive to search
	 * @param prefix a prefix which is included in any results returned by this
	 *   method. By convention, this prefix should end with a '#' to separate it
	 *   from resources found within the resource.
	 * 
	 * @return a List of Strings, in the syntax defined in the class javadoc
	 * 
	 * @throws IOException 
	 */

	public void findResourceInZip(ZipInputStream zipInputStream, String prefix) throws IOException {
		if (maxArchiveDepth!=-1 && currentArchiveDepth>=maxArchiveDepth) {
			return;
		}
		// System.err.println("findResourceInZip(" + prefix + "):" + currentArchiveDepth);
		currentArchiveDepth++;
		
		// System.out.println("Searching in " + prefix);
		ZipEntry zipEntry = null;
		try {
			zipEntry = zipInputStream.getNextEntry();
		} catch (EOFException oefe) {
			ignorableException("Error retrieving first entry in zip '" + prefix.substring(0, prefix.length()-1) + "'", oefe);
			currentArchiveDepth--;
			return;
		} catch (ZipException ze) {
			ignorableException("Error retrieving first entry in zip '" + prefix.substring(0, prefix.length()-1) + "'", ze);
			currentArchiveDepth--;
			return;
		} catch (IllegalArgumentException iae) {
			// can occur in ZipInputStream.getUTF8String
			ignorableException("Error retrieving first entry in zip '" + prefix.substring(0, prefix.length()-1) + "'", iae);
			currentArchiveDepth--;
			return;
		}
		while (zipEntry != null) {
			String name = zipEntry.getName();
			String shortName = name;

			// on unix, it's possible to get directory entries (trailing '/'s) within ZIPs; on windows this doesn't seem to happen
			while (shortName.endsWith("/")) { shortName = shortName.substring(0, shortName.length() - 1); }
			while (shortName.endsWith("\\")) { shortName = shortName.substring(0, shortName.length() - 1); }
			if (shortName.indexOf('/')!=-1) { shortName = shortName.substring(shortName.lastIndexOf('/') + 1); }
			if (shortName.indexOf('\\')!=-1) { shortName = shortName.substring(shortName.lastIndexOf('\\') + 1); }

			// may need to do case-sensitive match
			/* commenting this out temporarily

			if (showVersions && name.equalsIgnoreCase("META-INF/MANIFEST.MF")) {
				// treat this as a property file. Which is wrong, because it's got insane line breaks
				// but good enough for retrieving version data
				
				Properties props = new Properties();
				props.load(zipInputStream);
				if (props.getProperty("Specification-Version")!=null) {
					// there's also an Implementation-Version, but this appears to be the same
					// maven2 doesn't write these entries. perhaps.
					// @TODO something
				}
			}
			*/
			
			InputStream inputStreamToProcess = zipInputStream;
			
			// might just be easier to add .reset() to ZipInputStream
			ResourceFinderCallbackResult rfcbResult = null;
			if (isArchive(name)) {
				try {
					if (matches(shortName) || showAll) {
						rfcbResult = callback.preProcess(prefix + name, zipEntry.getSize(), zipEntry.getTime(), inputStreamToProcess);
						if (rfcbResult!=null && rfcbResult.replaceInputStream!=null) {
							inputStreamToProcess = rfcbResult.replaceInputStream;
						}
						if (rfcbResult!=null && rfcbResult.abort) {
							this.abort = true; break;
						}
					}
					ZipInputStream zis = new ZipInputStream(inputStreamToProcess);
					findResourceInZip(zis, prefix + name + "#");
					zipInputStream.closeEntry();
				} catch (EOFException eofe) {
					// can trigger "java.io.EOFException: Unexpected end of ZLIB input stream" errors
					ignorableException("Error reading zip '" + prefix + name + "'", eofe);
				} catch (ZipException ze) {
					ignorableException("Error reading zip '" + prefix + name + "'", ze);
				}
			}
			if (matches(shortName) || showAll) {
				rfcbResult = callback.postProcess(prefix + name, zipEntry.getSize(), zipEntry.getTime(), inputStreamToProcess, rfcbResult);
				if (rfcbResult!=null && rfcbResult.abort) {
					this.abort = true; break;
				}
			}
			
			try {
				zipEntry = zipInputStream.getNextEntry();
			} catch (EOFException oefe) {
				ignorableException("Error retrieving next entry in zip '" + prefix.substring(0, prefix.length()-1) + "'; after '"+ name + "'", oefe);
				break;
			} catch (ZipException ze) {
				ignorableException("Error retrieving next entry in zip '" + prefix.substring(0, prefix.length()-1) + "'; after '"+ name + "'", ze);
				break;
			} catch (IllegalArgumentException iae) {
				// can occur in ZipInputStream.getUTF8String
				ignorableException("Error retrieving next entry in zip '" + prefix.substring(0, prefix.length()-1) + "'; after '"+ name + "'", iae);
				break;
			} catch (IOException ioe) {
				// may occur after dodgy CRCs:
				// invalid entry CRC (expected 0xab633fa2 but got 0xc30a2df7)
				// Exception in thread "main" java.io.IOException: Push back buffer is full
				if (ioe.getMessage().contains("Push back buffer")) {
					ignorableException("Error retrieving next entry in zip '" + prefix.substring(0, prefix.length()-1) + "'; after '"+ name + "'", ioe);
					break;
				} else {
					currentArchiveDepth--;
					throw ioe;
				}
			}
			if (abort) { break ; }
		}
		currentArchiveDepth--;
	}

	/** Returns a resource as an inputstream
	 * 
	 * @param resourceName a resource name, as defined by the class javadoc
	 * 
	 * @return the resource as an InputStream  
	 * 
	 * @throws FileNotFoundException the resource could not be found
	 * @throws IOException the resource could not be read
	 */
	public static InputStream getResourceStream(String resourceName) throws IOException {
		int pos = resourceName.indexOf("#");
		if (pos == -1) {
			return new FileInputStream(resourceName);
		} else {
			String filename = resourceName.substring(0, pos);
			String component = resourceName.substring(pos + 1);
			ZipInputStream zis = new ZipInputStream(new FileInputStream(filename));
			return getResourceComponent(zis, component, resourceName);
		}
	}
	
	/** Private method to recursively search within an archive for a file
	 * 
	 * @param zipInputStream input stream to search
	 * @param component resource name fragment, separated by '#' characters
	 * @param fullResource full resource name (only used in exception messages)  
	 * 
	 * @return the input stream 
	 * 
	 * @throws IOException the input stream cannot be read
	 */
	public static InputStream getResourceComponent(ZipInputStream zipInputStream, String component, String fullResource) throws IOException {
		int pos = component.indexOf("#");
		String filename = component;
		String subComponent = null;
		if (pos != -1) {
			filename = component.substring(0, pos);
			subComponent = component.substring(pos + 1);
		}
		ZipEntry zipEntry = zipInputStream.getNextEntry();
		while (zipEntry != null) {
			if (zipEntry.getName().equals(filename)) {
				if (subComponent == null) {
					return zipInputStream;
				} else {
					return getResourceComponent(new ZipInputStream(zipInputStream), subComponent, fullResource); 
				}
			}
			zipEntry = zipInputStream.getNextEntry();
		}
		throw new FileNotFoundException("Could not find component '" + filename + "' in '" + fullResource + "'");
	}
	
	/** Sets the maximum number of times I'm going to recursively enter a 
	 * JAR/EAR/WAR/whatever.
	 * 
	 * <ul>
	 * <li>0 = none; i.e. will just perform a directory scan.
	 * <li>1..n = will search up to n directories/archives deep 
	 * <li>-1 = infinite; i.e. will not perform depth checking 
	 * </ul>
	 * 
	 * @param depth maximum depth (-1=no limit, 0=will not recursive into JARs/WARs etc..)
	 */
	public void setMaxArchiveDepth(long maxArchiveDepth) {
		this.maxArchiveDepth = maxArchiveDepth;
	}

	/** Sets the maximum folder depth to descend into the filesystem structure.
	 * 
	 * <p>This will not limit folder depth within archives, only folder depth within the filesystem 
	 * 
	 * <p>This setting has no effect if using the InputStream constructor.
	 * 
	 * <ul>
	 * <li>0 = none; i.e. will just scan within the top-level folder.
	 * <li>1..n = will search up to n folders deep 
	 * <li>-1 = infinite; i.e. will not perform folder depth checking 
	 * </ul>
	 * 
	 * @param depth maximum depth (-1=no limit, 0=will not recurse into folders)
	 */
	public void setMaxFolderDepth(long maxFolderDepth) {
		this.maxFolderDepth = maxFolderDepth;
	}

	
	public static String usage() {
		return 
		  "Usage: \n" +
		  "  java " + ResourceFinder.class.getName() + " [options] searchTerm\n" +
		  "or\n" +
		  "  java " + ResourceFinder.class.getName() + " [options] -a\n" +
		  "where [options] are:\n" +
		  " -h -?     displays this helptext\n" +
		  " -f        follow symlinks\n" + 
		  " -a        show all resources found (i.e. do not use searchTerm)\n" +
		  "\n" +
		  "Search criteria:\n" +
		  " -i        case-insensitive match\n" +
		  " -sc       if present, searchTerm matches within filename (default)\n" +
		  " -ss       if present, searchTerm matches start of filename\n" +
		  " -se       if present, searchTerm matches exact filename\n" +
		  " -sr       if present, searchTerm matches filename as a regular expression\n" +
		  " -mf n     max filesystem folder depth (0 = do not descend into subfolders)\n" +
		  " -ma n     max archive depth (0 = do not descend into archives)\n" +
		  " -x        if present, will attempt to recover if errors occur reading archives\n"+
		  "             (errors sent to stderr)\n" +
		  "\n" +
		  "Action when resource found:\n" +
		  " -v        verbose; display filenames with file sizes and timestamps\n" +
		  " -vv       display MD5/SHA1 hashes of resources (NB: modifies display order)\n" +
		  " -d n      dump/display the contents of the n'th resource found\n" +
		  " -d all    dump the name and contents of all resources found\n" +
		  " -d names  dump just the names of all resources found (default)\n" +
		  " -d n1,n2... dump the name and contents of the n1'th, n2'nd etc... resources found\n" +
		  " -dm n|all as per -d, but performs manifest unmangling on resource (fixes linewraps)\n" +
		  " -dj n|all as per -d, but performs class decompiling (requires jad to be in PATH)\n" +
		  " -c text   search for text in contents of resource (uses UTF-8 encoding)\n" +
		  " -ci text  case-insensitive search for text in contents of resources\n" +
		  "\n" +
		  "* A maximum of one -d switch should be present\n" +
		  "* The -d and -c switches are mutually exclusive\n";
	}
	
	/** Command-line interface to this class
	 * 
	 * @param args arguments
	 * 
	 * @throws IOException
	 */
	public static void main(String args[]) throws IOException {
		String searchTerm;
		String searchContents = null;
		String dumpResource = "";
		int     dumpType = DisplayResourceFinderCallback.DUMP_NAMES;
		List<Integer> dumpResourceList = null;
		int     argIndex = 0;
		int     matchType = MATCHTYPE_CONTAINS;
		long    maxArchiveDepth = -1;
		long    maxFolderDepth = -1;
		boolean followSymlinks = false;
		boolean verbose = false;
		boolean showHashes = false;
		boolean showAll = false;
		boolean manifests = false;
		boolean decompile = false;
		boolean ignoreCase = false;
		boolean ignoreErrors = false;
		boolean searchContentsIgnoreCase = false;
		
		if (args.length < 1) { 
			System.out.println(usage());
			throw new IllegalArgumentException("Expected resource search term or options");
		}
		while (argIndex < args.length && args[argIndex].startsWith("-")) {
			if (args[argIndex].startsWith("-d")) {
				if (args[argIndex].equals("-dm")) { manifests = true; }
				if (args[argIndex].equals("-dj")) { decompile = true; }
				
			    dumpResource = args[argIndex + 1];
			    if (dumpResource.equals("all")) {
			    	dumpType = DisplayResourceFinderCallback.DUMP_NAMES_AND_RESOURCES;
			    } else if (dumpResource.equals("names")) {
			    	dumpType = DisplayResourceFinderCallback.DUMP_NAMES;
			    } else {
			    	dumpResourceList = new ArrayList<Integer>();
			    	String[] resources = dumpResource.split(",");
			    	for (String resource : resources) {
			    		try {
			    			dumpResourceList.add(new Integer(resource));
			    		} catch (NumberFormatException nfe) {
					    	// @TODO if it's not a number, then could use it as a resource id
					    	throw new IllegalArgumentException("Expected numeric resource id (found '" + dumpResource + "')");
					    }
			    	}
			    	if (dumpResourceList.size() == 1) {
			    		dumpType = DisplayResourceFinderCallback.DUMP_RESOURCES;
			    	} else {
			    		dumpType = DisplayResourceFinderCallback.DUMP_NAMES_AND_RESOURCES;
			    	}
			    }
			    // 1.6 method args = Arrays.copyOfRange(args, 2, args.length);
			    argIndex += 2;
			    
			} else if (args[argIndex].equals("-mf")) {
				maxFolderDepth = Long.parseLong(args[argIndex + 1]);
				argIndex += 2;
		    } else if (args[argIndex].equals("-ma")) {
				maxArchiveDepth = Long.parseLong(args[argIndex + 1]);
				argIndex += 2;
			} else if (args[argIndex].equals("-v")) {
			    verbose = true;
			    argIndex ++;
			} else if (args[argIndex].equals("-vv")) {
				verbose = true;
			    showHashes = true;
			    argIndex ++;
			} else if (args[argIndex].equals("-f")) {
			    followSymlinks = true;
			    argIndex ++;
			} else if (args[argIndex].equals("-a")) {
			    showAll = true;
			    argIndex ++;
			} else if (args[argIndex].equals("-sc")) {
			    matchType = MATCHTYPE_CONTAINS;
			    argIndex ++;
			} else if (args[argIndex].equals("-ss")) {
			    matchType = MATCHTYPE_STARTSWITH;
			    argIndex ++;
			} else if (args[argIndex].equals("-sr")) {
			    matchType = MATCHTYPE_REGEX;
			    argIndex ++;
			} else if (args[argIndex].equals("-se")) {
			    matchType = MATCHTYPE_EXACT;
			    argIndex ++;
			} else if (args[argIndex].equals("-c")) {
			    searchContents = args[argIndex + 1];
			    // System.out.println("1 searchContents is " + searchContents);
			    argIndex += 2;		
			} else if (args[argIndex].equals("-ci")) {
			    searchContents = args[argIndex + 1];
			    searchContentsIgnoreCase = true;
			    // System.out.println("1 searchContents is " + searchContents);
			    argIndex += 2;		
			} else if (args[argIndex].equals("-i")) {
			    ignoreCase = true;
			    argIndex ++;
			} else if (args[argIndex].equals("-x")) {
			    ignoreErrors = true;
			    argIndex ++;
			} else if (args[argIndex].equals("-h") || args[argIndex].equals("-?")) {
				System.out.println(usage());
				System.exit(0);
			} else {
				System.out.println(usage());
				throw new IllegalArgumentException("Unknown switch '" + args[argIndex] + "' supplied");
			}
		}

		if (showAll) {
			searchTerm = "maguffin";
		} else {
			if (args.length < argIndex + 1) {
				System.out.println(usage());
				throw new IllegalArgumentException("Expected resource search term");
			}
			searchTerm = args[argIndex++];
		}
		
		ResourceFinderCallback callback;
		if (showHashes) {
			callback = new HashingResourceFinderCallback();
		} else {
			callback = new DisplayResourceFinderCallback(dumpType, dumpResourceList, verbose, manifests, decompile, 
			  searchContents, searchContentsIgnoreCase);
		}
		
		ResourceFinder resourceFinder = new ResourceFinder(searchTerm, matchType, ignoreCase, new File("."), callback);
		resourceFinder.setFollowSymLinks(followSymlinks);
		resourceFinder.setShowAll(showAll);
		resourceFinder.setIgnoreErrors(ignoreErrors);
		if (maxArchiveDepth != -1) { resourceFinder.setMaxArchiveDepth(maxArchiveDepth); }
		if (maxFolderDepth != -1) { resourceFinder.setMaxFolderDepth(maxFolderDepth); }
		 
		resourceFinder.find();
		
		
	}
}

