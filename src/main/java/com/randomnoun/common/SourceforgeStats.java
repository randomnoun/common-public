package com.randomnoun.common;

/* (c) 2013 randomnoun. All Rights Reserved. This work is licensed under a
 * Creative Commons Attribution 3.0 Unported License. (http://creativecommons.org/licenses/by/3.0/)
 */

/* (c) 2013 randomnoun. All Rights Reserved. This work is licensed under a 
 * <a rel="license" href="http://creativecommons.org/licenses/by/3.0/">Creative Commons Attribution 3.0 Unported License</a>.
 */

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

/** Retrieve sourceforge statistics and store them in a database. 
 *
 * <p>The following MySQL creation DDL should be run beforehand 
 * (assuming a schema named `stats` has already been created)
 * 
 * <pre>
 * CREATE TABLE `stats`.`daily` (
  `id` INTEGER UNSIGNED NOT NULL AUTO_INCREMENT,
  `projectId` INTEGER UNSIGNED NOT NULL,
  `date` TIMESTAMP NOT NULL,
  `statType` INTEGER UNSIGNED NOT NULL,
  `value` INTEGER NOT NULL,
  PRIMARY KEY (`id`),
  INDEX `IDX_DAILY_PROJECT`(`projectId`),
  INDEX `IDX_DAILY_DATE`(`date`),
  UNIQUE INDEX `IDX_DAILY_PROJECT_DATE_TYPE`(`projectId`, `date`, `statType`);
)
ENGINE = InnoDB;
 * < /pre>
 *
 * <h2>Limitations</h2>
 * 
 * <p>Does not store operating system or geo (country) statistics for file download stats
 * <p>Does not store per-file stats (only per-project)
 * <p>{@link #getFileDownloadStats(String, String, String)} and 
 *   {@link #getScmStats(String, ScmType, String, String)} methods currently assume each
 *   result set returned by sourceforge provides one statistic per day (i.e. start and 
 *   end dates do not span more than one calendar month)
 * <p>The last CVS stat in the current month may not have the normal 00:00:00 time,
 *   associated with it, which may cause primary key violations on subsequent updates.
 *   This might be resolved by running the SQL 
 *   <code>DELETE FROM `stats`.`daily` WHERE date_format(`date`, '%H:%i') <> '00:00'</code>
 * 
 * @author knoxg
 * @blog http://www.randomnoun.com/wp/?p=5
 * @version $Id$
 * */
public class SourceforgeStats {

	/** Logger instance for this class */
	Logger logger = Logger.getLogger(SourceforgeStats.class);

    /** A revision marker to be used in exception stack traces. */
    public static final String _revision = "$Id$";
	
	/** Types of source control management that SourceForge supports */
	public enum ScmType { CVS, SVN, THE_OTHER_ONES };
	
	/** Types of statistics that we will transfer. */
	public enum StatType { 
		FILE_DOWNLOAD(100), 
		CVS_ANON_READ(200), CVS_DEV_READ(201), CVS_WRITE(202),
	    SVN_READ(300), SVN_WRITE(301), SVN_WRITE_FILE(302);
		
		long databaseValue;
		private StatType(long databaseValue) {
			this.databaseValue = databaseValue;
		}
		private long toDatabaseValue() { return databaseValue; }
		
	}
	
	/** A class to store an individual statistic value */
	public static class Stat {
		StatType statType;
		Date startDateRange, endDateRange;
		long value;
		public Stat(StatType statType, Date startDateRange, Date endDateRange, long value) {
			this.statType = statType;
			this.startDateRange = startDateRange;
			this.endDateRange = endDateRange;
			this.value = value;
		}
	}
	
	/** Request file download statistics from SourceForge
	 *   
	 * @param project project unix name
	 * @param start start date, in yyyy-MM-dd format
	 * @param end end date, in yyyy-MM-dd format
	 * @return
	 * @throws IOException
	 */
	public List<Stat> getFileDownloadStats(String project, String start, String end) throws IOException {
		List<Stat> stats = new ArrayList<Stat>();
		HttpClient client = new HttpClient();
		// individual files have their own stats, I'm just grabbing whole-of-project stats 
		String url = "http://sourceforge.net/projects/" + project + "/files/stats/json?start_date=" + start + "&end_date=" + end;
		logger.debug("Retrieving url '" + url + "'");
		GetMethod gm = new GetMethod(url);
		client.executeMethod(gm);
		InputStream is = gm.getResponseBodyAsStream();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd"); // it's in UTC, but let's ignore that
		JSONTokener tokener = new JSONTokener(new InputStreamReader(is));
		
		try {
			JSONObject resultJs = new JSONObject(tokener);
			logger.debug(resultJs.toString());
			JSONArray downloadsJs = resultJs.getJSONArray("downloads");
			for (int i=0; i<downloadsJs.length(); i++) {
				JSONArray downloadJs = (JSONArray) downloadsJs.get(i);
				Date date = sdf.parse(downloadJs.getString(0));
				long value = downloadJs.getLong(1);
				Stat stat = new Stat(StatType.FILE_DOWNLOAD, date, date, value);
				stats.add(stat);
			}
		} catch (JSONException e) {
			throw new IOException("Error parsing JSON", e);
		} catch (ParseException e) {
			throw new IOException("Error parsing date", e);
		}
		logger.debug("returning " + stats.size() + " stats");
		return stats;
	}
	
	/** Perform multiple file download statistic requests from SourceForge using
	 * a given calendar range. One request will be made per month.
	 * 
	 * @param project project unix name
	 * @param startCal start date
	 * @param endCal end date
	 * @return
	 * @throws IOException
	 */
	public List<Stat> getFileDownloadStats(String project, Calendar startCal, Calendar endCal) throws IOException {
		List<Stat> stats = new ArrayList<Stat>();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		Calendar startMonth = (Calendar) startCal.clone();
		while (!startMonth.after(endCal)) {
			Calendar endMonth = (Calendar) startMonth.clone(); endMonth.add(Calendar.MONTH, 1); endMonth.add(Calendar.DAY_OF_YEAR, -1);
			stats.addAll(getFileDownloadStats(project, sdf.format(startMonth.getTime()), sdf.format(endMonth.getTime()) ));
			startMonth.add(Calendar.MONTH, 1);
		}
		return stats;
	}
	
	/** Request source control management statistics from SourceForge
	 *   
	 * @param project project unix name
	 * @param scmType the type of source control system in use for this project
	 * @param start start date, in yyyy-MM-dd format
	 * @param end end date, in yyyy-MM-dd format
	 * @return
	 * @throws IOException
	 */
	public List<Stat> getScmStats(String project, ScmType scmType, String start, String end) throws IOException {
		List<Stat> stats = new ArrayList<Stat>();
		HttpClient client = new HttpClient();
		String scmParam;
		String[] jsonKeys;
		StatType[] statTypes;
		if (scmType==ScmType.CVS) { 
			scmParam="CVSRepository";
			jsonKeys=new String[] { "write", "anon_read", "dev_read" };
			statTypes=new StatType[] { StatType.CVS_WRITE, StatType.CVS_ANON_READ, StatType.CVS_DEV_READ };
		} else if (scmType==ScmType.SVN){
			scmParam="SVNRepository";
			jsonKeys=new String[] { "write_txn", "read_txn", "write_files" };
			statTypes=new StatType[] { StatType.SVN_WRITE, StatType.SVN_READ, StatType.SVN_WRITE_FILE };
		} else { 
			throw new IllegalArgumentException("Invalid scmType '" + scmType + "'"); 
		}
		
		// e.g. https://sourceforge.net/projects/jvix/stats/scm?repo=CVSRepository&dates=2012-01-01+to+2012-01-31
		//   which performs an XHR request for 
		// https://sourceforge.net/projects/jvix/stats/scm_data?repo=CVSRepository&begin=2012-01-01&end=2012-01-31
		String url = "http://sourceforge.net/projects/" + project + "/stats/scm_data?repo=" + scmParam + "&begin=" + start + "&end=" + end; 
		logger.debug("Retrieving url '" + url + "'");
		GetMethod gm = new GetMethod(url);
		client.executeMethod(gm);
		String jsonText = gm.getResponseBodyAsString();
		//InputStream is = gm.getResponseBodyAsStream();
		JSONTokener tokener = new JSONTokener(jsonText);
		
		try {
			JSONObject resultJs = new JSONObject(tokener);
			logger.debug(resultJs.toString());
			
			JSONObject dataJs = resultJs.getJSONObject("data");
			for (int k=0; k<jsonKeys.length; k++) {
				String jsonKey = jsonKeys[k];
				StatType statType = statTypes[k];
				JSONArray writeJs = dataJs.getJSONArray(jsonKey);
				for (int i=0; i<writeJs.length(); i++) {
					JSONArray recordJs = (JSONArray) writeJs.get(i);
					Date date = new Date((long) recordJs.getDouble(0));
					//logger.debug("date " + date); // NB: date should be displayed in UTC
					long value = recordJs.getLong(1);
					Stat stat = new Stat(statType, date, date, value);
					stats.add(stat);
				}
			}
			
			
		} catch (JSONException e) {
			throw new IOException("Error parsing JSON in '" + jsonText + "'", e);
		} catch (NumberFormatException e) {
			throw new IOException("Error parsing date", e);
		}
		logger.debug("returning " + stats.size() + " stats");
		return stats;
		
		
	}
	
	/** Perform multiple source control management statistic requests from SourceForge 
	 * using a given calendar range. One request will be made per month.
	 * 
	 * @param project project unix name
	 * @param scmType the type of source control system in use for this project 
	 * @param startCal start date
	 * @param endCal end date
	 * @return
	 * @throws IOException
	 */
	public List<Stat> getScmStats(String project, ScmType scmType, Calendar startCal, Calendar endCal) throws IOException {
		
		List<Stat> stats = new ArrayList<Stat>();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		Calendar startMonth = (Calendar) startCal.clone();
		while (!startMonth.after(endCal)) {
			Calendar endMonth = (Calendar) startMonth.clone(); endMonth.add(Calendar.MONTH, 1); endMonth.add(Calendar.DAY_OF_YEAR, -1);
			stats.addAll(getScmStats(project, scmType, sdf.format(startMonth.getTime()), sdf.format(endMonth.getTime()) ));
			startMonth.add(Calendar.MONTH, 1);
		}
		return stats;
	}

	/** Insert the supplied statistics into the database, or update it if
	 * it already exists. 
	 * 
	 * @param jt JdbcTemplate containing connection to the database
	 * @param projectId the project ID 
	 * @param stats the statistics
	 */
	public void updateDatabase(JdbcTemplate jt, long projectId, List<Stat> stats) {
		logger.debug("Storing " + stats.size() + " records in database...");
		for (int i=0; i<stats.size(); i++) {
			Stat stat = stats.get(i);
			try {
				jt.update("INSERT INTO daily(projectId, date, statType, value) VALUES (?, ?, ?, ?)",
				  new Object[] { projectId, new java.sql.Date(stat.startDateRange.getTime()), stat.statType.toDatabaseValue(), stat.value });
			} catch (DataIntegrityViolationException dive) {
				// if this statistic exists, update it instead
				try {
					jt.update("UPDATE daily SET value = ? WHERE projectId = ? AND date = ? AND statType = ?",
					  new Object[] { stat.value, projectId, new java.sql.Date(stat.startDateRange.getTime()), stat.statType.toDatabaseValue() });
				} catch (DataIntegrityViolationException dive2) {
					logger.error("DataIntegrityViolationException on UPDATE", dive2);
				}
			}
		}
	}

}
