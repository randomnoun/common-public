package com.randomnoun.common.db.history;

/* (c) 2013 randomnoun. All Rights Reserved. This work is licensed under a
 * BSD Simplified License. (http://www.randomnoun.com/bsd-simplified.html)
 */

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import javax.script.Compilable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleScriptContext;
import javax.sql.DataSource;

import org.apache.log4j.Logger;

import com.randomnoun.common.Text;
import com.randomnoun.common.db.DatabaseReader;
import com.randomnoun.common.db.dao.MysqlDatabaseReader;
import com.randomnoun.common.db.to.SchemaTO;
import com.randomnoun.common.jessop.JessopCompiledScript;

/** This class will generate history table and triggers. 
 * 
 * If you get these error messages:
 * <pre>
 * Access denied; you need the SUPER privilege for this operation
 * </pre>
 * 
 * then try this:
 * <pre>
 * GRANT ALL PRIVILEGES ON ON schema_name.* TO 'ON schema_name'@'%' WITH GRANT OPTION;
 * </pre>
 * 
 * I've got a sqlserver version of this somewhere. Good luck finding that again.
 * 
 * <p>This class is similar to the old HistoryTableGenerator, but uses templates instead.
 * 
 * @TODO add other db types
 * @TODO add stored procs to roll back table(s) to a given point in time
 * @TODO add history partitions, if mysql supports it 
 **/
public class HistoryTableGenerator {

	Logger logger = Logger.getLogger(HistoryTableGenerator.class);
		
	Logger scriptLogger = Logger.getLogger("com.randomnoun.common.db.HistoryTableGenerator2.script");
	
	private DataSource ds;
	
	private String jessopScript;
	private String jessopScriptFilename;
	private String schemaName; 
	private Map<String, Object> options = new HashMap<String, Object>();
	
	public HistoryTableGenerator(DataSource ds) {
		this.ds = ds;
	}

	/** Set the options for the history generator.
	 * 
	 * <p>History options are specific to the generator being used, but typical options are:
	 * 
	 * <ul>
	 * <li>undoEnabledTableNames - List<String>
     * <li>dropTables - Boolean
	 * <li>existingDataUserActionId - Boolean
	 * <li>alwaysRecreateTriggers - Boolean
	 * <li>alwaysRecreateStoredProcedures - Boolean
	 * <li>includeUpdateBitFields - Boolean 
	 * <li>includeCurrentUser - Boolean
	 * </ul>
	 * 
	 * <p>Refer to the source code of the generator as to which options are supported
	 * ( e.g. src/main/resources/common/db/mysql/mysql-historyTable-2.sql.jessop )
	 * 
	 * @param options
	 */
	public void setOptions(Map<String, Object> options) {
		this.options = options;
	}

	
	/** When the logger in this class is set to log at DEBUG level, then this method returns the
	 * jessop script transpiled to whichever language it's supposed to be in.
	 * 
	 * @param engine
	 * @param jessopSource
	 * @return
	 * @throws ScriptException
	 */
	private String getSource(ScriptEngine engine, String jessopSource) throws ScriptException {
		Compilable compilable = (Compilable) engine;
		JessopCompiledScript compiledScript = (JessopCompiledScript) compilable.compile(jessopSource);
		return compiledScript.getSource();
	}
	
	/** Returns the SQL that will create history tables, triggers and stored procedures.
	 * 
	 * <p>To break this String back into individual SQL statements, use {@link com.randomnoun.common.db.SqlParser}
	 * 
	 * @return SQL that will create history tables, triggers and stored procedures 
	 * 
	 * @throws ScriptException
	 */
	public String generateHistoryTableSql() throws ScriptException {
   
		DatabaseReader dr = new MysqlDatabaseReader(ds);
		SchemaTO schema = dr.getSchema(schemaName);
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintWriter pw = new PrintWriter(baos);
		
		ScriptEngine engine = new ScriptEngineManager().getEngineByName("jessop");
		if (engine==null) { throw new IllegalStateException("Missing engine 'jessop'"); }
		engine.put(ScriptEngine.FILENAME, jessopScriptFilename);
		
		ScriptContext sc = new SimpleScriptContext();
		sc.setWriter(pw);
		sc.setAttribute("schema", schema, ScriptContext.ENGINE_SCOPE);
		
		sc.setAttribute("logger", scriptLogger, ScriptContext.ENGINE_SCOPE);
		sc.setAttribute("options", options, ScriptContext.ENGINE_SCOPE);

		logger.info("Start eval");
		jessopScript = Text.replaceString(jessopScript, "\r", ""); // jessop has issues with \r\n EOLs
		if (logger.isDebugEnabled()) { logger.debug(getSource(engine, jessopScript)); }
		
		
		// table loop is within the generator now
		engine.eval(jessopScript, sc);
		
		String sql = baos.toString();
		return sql;
	}

	public String getJessopScript() {
		return jessopScript;
	}

	public void setJessopScript(String jessopScript) {
		this.jessopScript = jessopScript;
	}

	public String getJessopScriptFilename() {
		return jessopScriptFilename;
	}

	public void setJessopScriptFilename(String jessopScriptFilename) {
		this.jessopScriptFilename = jessopScriptFilename;
	}

	public String getSchemaName() {
		return schemaName;
	}

	public void setSchemaName(String schemaName) {
		this.schemaName = schemaName;
	}

	public Map<String, Object> getOptions() {
		return options;
	}


	
	
}
