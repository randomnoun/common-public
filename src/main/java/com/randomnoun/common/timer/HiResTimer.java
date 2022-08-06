package com.randomnoun.common.timer;

/* (c) 2013 randomnoun. All Rights Reserved. This work is licensed under a
 * BSD Simplified License. (http://www.randomnoun.com/bsd-simplified.html)
 */

import org.apache.log4j.Logger;

import com.randomnoun.common.timer.HiResTimer;

/**
 * This is some increasingly ancient code to provide microsecond timer resolution in Java 
 * using JNI on Windows.
 * 
 * <p>Java now provides this natively via System.nanoTime(), so all of this can and should be thrown out
 * some day. 
 * 
 * <p>Historical background: Java previously supplied only millisecond
 * resolution, and on Windows this was only around 10-20 millisecond precision;
 * approx 16ms (Win3.1), 55ms (ME/98/95) or 10ms (others)). Other platforms (Linux/Solaris)
 * appear to provide real millisecond resolution.
 *
 * <p>This class uses JNI to access the Windows QueryPerformanceCounter API. It
 * requires the HiResTimer.dll to be located in the VM's java.library.path. In order to
 * work in WSAD, it will also examine the contents of the 'workspace.root' System
 * property, and attempt to reference it from within the developer's build tree.
 *
 * <p>If the native library cannot be loaded, this class falls back to Java's
 * standard timer implementation (System.currentTimeMillis).
 *
 * <p>Note that the Websphere reloading ClassLoader cannot deal with JNI,
 * so this class must exist in an external classpath or JAR which is referenced from
 * the system classpath. The Ant build task 'buildStartupJar' in ext/build.xml
 * can compile the JNI and create this startup JAR.
 *
 * <p>This file was based on the discussion at
 * <a href="http://www.fawcette.com/archives/premier/mgznarch/javapro/2001/08aug01/km0108/km0108-1.asp">
 * http://www.fawcette.com/archives/premier/mgznarch/javapro/2001/08aug01/km0108/km0108-1.asp</a>
 *
 * <p>Some instructions on building JNI in a cygwin/gcc environment was found at
 * <a href="http://www.inonit.com/cygwin/jni/helloWorld/">
 * http://www.inonit.com/cygwin/jni/helloWorld/</a>
 * 
 * <p>Documentation on Windows APIs can be found in Windows 2003 Core API download, found at
 * <a href="http://www.microsoft.com/msdownload/platformsdk/sdkupdate/">
 * http://www.microsoft.com/msdownload/platformsdk/sdkupdate/</a>
 *
 * @author  knoxg
 * 
 */
public class HiResTimer
{
    
    
 
    /** Logger instance for this class */
    private static Logger logger = Logger.getLogger(HiResTimer.class);
    
    /** Set to true if the HiResTimer DLL can be found in the java library path */
    private static boolean foundLibrary = false;

    /** A HiResTimer instance; used since I want to be able to provide these methods
     *  as static, which I found difficult in JNI directly. 
     */
    private static HiResTimer instance = new HiResTimer();


    private native boolean native_isHighResTimerAvailable();

    /**
     * JNI method to return the frequency of the system clock (ticks per second)
     *
     * @return the frequency of the system clock (ticks per second)
     */
    private native long native_getFrequency();

    /**
     * JNI method to return the current timestamp (in frequency units)
     *
     * @return the current timestamp (in frequency units)
     */
    private native long native_getTimestamp();

    /**
     * Return the frequency of the available timer.
     *
     * @return The granularity of the timer, measured in ticks per second.
     */
    public static long getFrequency()
    {
        if (foundLibrary) {
            return instance.native_getFrequency();
        } else {
            // System ostensibly supplies us with 1ms precision (although it doesn't, really)
            return 1000;
        }
        
    }

    /**
     * Returns the current value of the timer, as measured in the frequency supplied
     * by {@link #getFrequency()}. The starting time for returned values is undefined
     * (i.e. there is no fixed point for timestamp = 0); this value can only be used
     * for relative timing only, or must be correlated with System.currentTimeMillis().
     *
     * @return The current value of the timer.
     */
    public static long getTimestamp()
    {
        if (foundLibrary) {
            return instance.native_getTimestamp();
        } else {
            return System.currentTimeMillis();
        }
    }

    /** Returns true if we can use a natively-supplied timer, false otherwise
     *
     * @return true if we can use a natively-supplied timer, false otherwise
     */
    public static boolean isNativeTimerAvailable()
    {
        return foundLibrary;
    }

    /**
     * Returns true if the Windows HAL (Hardware Abstraction Layer) supports
     * a high-resolution performance counter. Note that even if this returns false,
     * we can fall back to a native counter, which is still more precise than
     * the one that Java supplies.
     *
     * @return True if high resolution performance is available, false if not.
     */
    public static boolean isHiResTimerAvailable()
    {
        // NB: short-cut boolean logic
        return foundLibrary && instance.native_isHighResTimerAvailable();
    }

    /** Returns the elapsed time between two timestamps, as a integer number of
     *  milliseconds.
     *
     * @return the elapsed time between two timestamps, as a integer number of
     *  milliseconds.
     */
    public static long getElapsedMillis(long timestamp1, long timestamp2)
    {
        return ((timestamp2 - timestamp1) * 1000) / getFrequency();
    }

    /** Returns the elapsed time between two timestamps, as a integer number of
     *  microseconds.
     *
     * @return the elapsed time between two timestamps, as a integer number of
     *  microseconds.
     */
    public static long getElapsedNanos(long timestamp1, long timestamp2)
    {
        return ((timestamp2 - timestamp1) * 1000000) / getFrequency();
    }

    /**
     * Test program.
     *
     * @param args Command-line options
     *
     * @throws InterruptedException if the Thread.sleep() call was interrupted
     */
    public static void main(String[] args)
        throws InterruptedException
    {
        System.out.println("Native timer: " +
            (HiResTimer.isNativeTimerAvailable() ? "available" : "unavailable"));
        System.out.println("Hi-res timer: " +
            (HiResTimer.isHiResTimerAvailable() ? "available" : "unavailable"));

        java.text.NumberFormat nf = new java.text.DecimalFormat("0.#########");

        long freq = HiResTimer.getFrequency();

        System.out.println("Timer has frequency of approx " + freq + " ticks/sec");

        // this isn't really the granularity, you know. It's just the *maximum* precision 
        // we can ever expect to get out of the timer. Whether the timer actually reaches
        // this theoretical maximum is a completely different matter.
        System.out.println("Timer has granularity of approx " +
            nf.format(1 / (double)freq) + " seconds.");

        long dStart = HiResTimer.getTimestamp();

        Thread.sleep(1000);

        long dEnd = HiResTimer.getTimestamp();

        System.out.println("Thread.sleep() test=" +
            HiResTimer.getElapsedMillis(dStart, dEnd) +
            " milliseconds (should be around 1000); = " +
            HiResTimer.getElapsedNanos(dStart, dEnd) + " microseconds");

        dStart = HiResTimer.getTimestamp();
        Thread.sleep(2000);
        dEnd = HiResTimer.getTimestamp();
        System.out.println("Thread.sleep() test=" +
            HiResTimer.getElapsedMillis(dStart, dEnd) +
            " milliseconds (should be around 2000); = " +
            HiResTimer.getElapsedNanos(dStart, dEnd) + " microseconds");
    }

    static
    {
        try
        {
            // use the java.library.path variable first
            System.loadLibrary("HiResTimer");
            foundLibrary = true;
        }
        catch (UnsatisfiedLinkError ule)
        {
            // exception intentionally ignored
        }

        if (!foundLibrary)
        {
            // when run in WSAD, workspace.root is something like
            //   C:/Documents and Settings/knoxg/My Documents/IBM/wsappdev51/workspace/Servers/QueryBuilderSvr.wsc
            // ... so we can try looking in here
            String workspaceRoot = System.getProperty("workspace.root");
            
            // NB, in eclipse wtp.deploy is set to something like
            //   C:\Documents and Settings\knoxg\workspace-3.5sr2\.metadata\.plugins\org.eclipse.wst.server.core\tmp0\wtpwebapps
            // so could try something relative to that as well

            if (workspaceRoot != null)
            {
                String location = workspaceRoot +
                    "/../../common/ext/bootstrap/HiResTimer.dll";

                try {
                    System.load(location);
                    foundLibrary = true;
                } catch (UnsatisfiedLinkError ule) {
                    logger.debug(
                        "Could not load HiResTimer from java.library.path or '" +
                        location + "'");

                    // ule.printStackTrace();
                    // NB: we still can't use a DLL even if it's loaded in another ClassLoader
                    // exception intentionally ignored
                }
            }
        }

        if (foundLibrary) {
            HiResTimer timer = new HiResTimer();
            logger.info("Native timer library available: high resolution timer " +
                (timer.native_isHighResTimerAvailable() ? "available" : "unavailable"));
        } else {
        	logger.info("Native timer library unavailable");
        }
    }
}
