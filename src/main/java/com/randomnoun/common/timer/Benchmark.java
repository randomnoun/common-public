package com.randomnoun.common.timer;

/* (c) 2013 randomnoun. All Rights Reserved. This work is licensed under a
 * BSD Simplified License. (http://www.randomnoun.com/bsd-simplified.html)
 */

import java.io.*;
import java.text.*;
import java.util.*;

import com.randomnoun.common.Text;

/**
 * An object that performs time benchmarking.
 *
 * <p>This object contains a reference to all benchmark instances
 * that are currently executing in the VM, referenced by <code>benchId</code>.
 * It is encouraged that each individual request has a unique benchId, although
 * this is not required unless you need to use the {@link #getBenchmark} method.
 *
 * (if multiple instances are created using the same <code>benchId</code>,
 * only the most recent instance will be retrievable via getBenchmark()).
 *
 * <p>Multiple checkpoints can be set during the course of a benchmark, which
 * will be written to disk after completion.
 *
 * <p>Output is generated only after the benchmark has completed. All output is
 * buffered, and references to any open benchmark files are kept within this
 * object in order to minimise the amount of overhead that benchmarking will
 * impose.
 *
 * <p>This class does not expire benchmarks, and therefore may cause memory leaks
 * if benchmarks are not closed correctly.
 *
 * <p>Output is of the form:
 * <pre style="code">
 *   benchId1,begin,timestamp,pointID1,timestamp1,pointID1,timestamp2,[...],end,timestampn,duration
 *   benchId2,begin,timestamp,pointID1,timestamp1,pointID1,timestamp2,[...],end,timestampn,duration
 *   :
 *   :
 * </pre>
 *
 * <p>where <i>benchId</i> is the benchId indentifying this benchmark, and
 * <i>pointIDn</i> are the individual checkpoint identifiers. Each line finishes
 * with the text ",duration," followed by the duration of the entire benchmark, in
 * milliseconds. Timestamps are displayed in the format defined by the
 * {@link #setDateFormat(String)} method.
 *
 * <p>There is no current way of following a benchmark through the EJB boundary layer;
 * to do so would currently involve overriding the User object, which I'm not terribly keen to do.
 * It is possible to set up a new Benchmark on the EJB side, and track performance
 * independently over there. It should then be a simple matter to heuristically match up
 * individual HTTP requests/struts actions and the EJB methods that they invoke
 * (under normal load conditions). It's important, however, that two Benchmark engines in the
 * same VM do not write to the same file, as they will corrupt each other's output.
 *
 * @author knoxg
 * 
 */
public class Benchmark {

    /** A Comparator which performs comparisons between two Benchmarks.
     */
    public static class BenchmarkComparator implements Comparator<Benchmark> {        

    	/** Create a new BenchmarkComparator object */
        public BenchmarkComparator() {
        }

        /** Compare two structured list elements
         *
         * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
         */
        public int compare(Benchmark a, Benchmark b) throws IllegalArgumentException {
            Benchmark benchmarkA = (Benchmark) a;
            Benchmark benchmarkB = (Benchmark) b;

            return benchmarkA.startTime < benchmarkB.startTime ? -1 : 
              (benchmarkA.startTime > benchmarkB.startTime ? 1 : 0);
        }
    }

    /** A class containing the checkpoints for a single benchmark */
    public class CheckpointList {
        /** List of Checkpoints, containing the checkpoints for a single
         *  benchmark */
        List<Checkpoint> list = new ArrayList<Checkpoint>();

        /** Return an iterator for the checkpoint list */
        public Iterator<Checkpoint> iterator() {
            return list.iterator();
        }

        /** Retrieve an individual checkpoint */
        public Checkpoint get(int n) {
            return (Checkpoint) list.get(n);
        }

        /** Adds a checkpoint to this set. The checkpoint is stamped using
         *  the current system time. */
        public void addCheckPoint(String id) {
            list.add(new Checkpoint(id));
        }

        /** Adds an annotation to this set. */
        public void addAnnotation(String annotation) {
            list.add(new Checkpoint(annotation, -1));
        }
        
        
        /** Returns a string version of this set. Timestamps are returned
         *  using the format specified by the Benchmark class */
        public String toString() {
            int i;
            int size = list.size();
            StringBuffer sb = new StringBuffer();
            Checkpoint checkpoint;
            SimpleDateFormat dateFormat;

            dateFormat = new SimpleDateFormat(Benchmark.dateFormatText);

            for (i = 0; i < size; i++) {
                checkpoint = (Checkpoint) list.get(i);
                if (checkpoint.getTimestamp()==-1) {
	                sb.append(',');
	                sb.append(Text.escapeCsv("\u0087" + checkpoint.id)); // ASCII 135, Hex 87
                } else {
	                sb.append(',');
	                sb.append(Text.escapeCsv(dateFormat.format(checkpoint.getDate())));
	                sb.append(',');
	                sb.append(Text.escapeCsv(checkpoint.id));
                }
            }

            if (list.size() > 0) {
                sb.append(",t,");
                checkpoint = (Checkpoint) list.get(list.size() - 1);
                sb.append(HiResTimer.getElapsedMillis(((Checkpoint) list.get(0)).timestamp, checkpoint.timestamp));
            }

            return sb.toString();
        }
    }

    /** A single benchmark point. May be either a begin, end, or checkpoint.
     * 
     * A checkpoint with a timestamp of -1 is an annotation.
     */
    public class Checkpoint {
        /** The string ID for this benchmark point */
        String id;

        /** The time at which this point was created to the benchmark */
        long timestamp;

        public Checkpoint(String id) {
            this.id = id;
            this.timestamp = HiResTimer.getTimestamp();
        }

        public Checkpoint(String id, long timestamp) {
            this.id = id;
            this.timestamp = timestamp;
        }

        public String getId() {
            return id;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public Date getDate() {
            // return date;
            return new Date(startTime + HiResTimer.getElapsedMillis(startHiresTimestamp, timestamp));
        }
    }

    /** Hashtable mapping filenames to java.io.Writer objects */
    private static Hashtable<String, Writer> writers = null;

    /** Hashtable mapping benchmark IDs to Benchmark objects */
    private static Hashtable<String, Benchmark> benchmarks = null;

    /** date format for timestamps. The default date format is
     *  the SimpleDateFormat <tt>"yyyy-dd-MM HH:mm:ss.SSS"</tt>. Note that
     *  better-than-millisecond precision is unavailable using the
     *  standard Java-supplied timer functions. */
    private static String dateFormatText = null;

    /** the set of checkpoints for this benchmark instance */
    protected CheckpointList checkpointList = null;

    /** the string ID for this benchmark */
    private String benchId = null;

    /** Start time, in milliseconds */
    protected long startTime;

    /** HiRes Timer token for start of benchmark */
    protected long startHiresTimestamp;

    /** True if benchmark is active, false if the benchmark has completed. */
    protected boolean active;

    /** the file where the results of this benchmark will be written */
    private String filename = null;

    /** Returns a PrintWriter that can be written to in order to
     *  commit a benchmark to disk. */
    private static PrintWriter open(String filename)
        throws IOException {
        PrintWriter out;

        // use existing Writer if it is already open
        if (writers.get(filename) != null) {
            return (PrintWriter) writers.get(filename);
        }

        // 'true' flag indicates files will be appended to
        out = new PrintWriter(new BufferedWriter(new FileWriter(filename, true)));
        writers.put(filename, out);

        return out;
    }

    /** Instantiates a new benchmark. Automatically creates a 'begin' checkpoint
     *  for this new instance. */
    public Benchmark(String benchId, String filename) {
        startTime = System.currentTimeMillis();
        startHiresTimestamp = HiResTimer.getTimestamp();

        checkpointList = new CheckpointList();
        benchmarks.put(benchId, this);
        checkpointList.addCheckPoint("begin");
        this.benchId = benchId;
        this.filename = filename;
        this.active = true;
    }

    /** Alters the ID for a benchmark that is already running. */
    public void setBenchId(String benchId) {
        benchmarks.remove(this.benchId);
        this.benchId = benchId;
        benchmarks.put(benchId, this);
    }

    /** Performs checkpoint. */
    public void checkpoint(String pointID) {
        checkpointList.addCheckPoint(pointID);
    }
    
    /** Creates a benchmark annotation */
    public void annotate(String annotation) {
        checkpointList.addAnnotation(annotation);
    }


    /** Returns a string representation of this benchmark */
    public String toString() {
        return benchId + checkpointList.toString();
    }

    /** Completes benchmark and writes to disk */
    public void end()
        throws IOException {
        PrintWriter printWriter;

        checkpointList.addCheckPoint("end");
        if (filename != null) {
            printWriter = (PrintWriter) writers.get(filename);
            if (printWriter == null) {
                printWriter = open(filename);
            }
            printWriter.println(this.toString());
            printWriter.flush();
        }
        active = false;
        benchmarks.remove(benchId);
    }

    /** Returns whether this benchmark is currently active */
    public boolean isActive() {
        return active;
    }


    /** Cancel this benchmark */
    public void cancel() {
        benchmarks.remove(benchId);
    }

    /** Returns all checkpoints stored in this benchmark */
    public CheckpointList getCheckpointList() {
        return checkpointList;
    }

    /** Returns the benchmark ID */
    public String getId() {
        return benchId;
    }

    /** Returns the benchmark with the supplied ID */
    public static Benchmark getBenchmark(String benchId) {
        return (Benchmark) benchmarks.get(benchId);
    }

    /** Sets the date format for all output of this class. The date format
     * is represented as a string, rather than a DateFormat object, since
     * most SimpleDateFormats are not thread-safe. */
    public static void setDateFormat(String dateFormatText) {
        Benchmark.dateFormatText = dateFormatText;
    }

    /** Flushes all writers within this benchmark object (commits any
     *  unwritten data to disk). Files are still kept open. */
    public static void flushWriters()
        throws IOException {
        Enumeration<Writer> e;

        for (e = writers.elements(); e.hasMoreElements();) {
            ((Writer) e.nextElement()).flush();
        }
    }

    /** Closes all writers within this benchmark object. Files will be
     *  automatically reopened for any running benchmarks.  */
    public static void closeWriters()
        throws IOException {
        Enumeration<Writer> e;

        for (e = writers.elements(); e.hasMoreElements();) {
            ((Writer) e.nextElement()).close();
        }

        // remove references to old writers
        writers = new Hashtable<String, Writer>();
    }
    
    public String toJson() {
    	String result = "[";
		   Iterator<Checkpoint> i = getCheckpointList().iterator();
		   // Benchmark.Checkpoint cpStart = i.hasNext() ? (Benchmark.Checkpoint) i.next() : null;
		   while (i.hasNext()) {
			   Benchmark.Checkpoint cpNext = (Benchmark.Checkpoint) i.next();
			   result += "{\"id\":\"" + Text.escapeJavascript(cpNext.getId()) + "\",\"timestamp\":" + cpNext.getTimestamp() + "}";
			   if (i.hasNext()) { result += ","; }
		   }
		   result += "]";
		   return result;
    }
    

    static {
        writers = new Hashtable<String, Writer>();
        benchmarks = new Hashtable<String, Benchmark>();

        // default date format
        dateFormatText = "yyyy-MM-dd HH:mm:ss.SSS";
    }
}
