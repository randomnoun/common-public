package com.randomnoun.common.spring;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.CallableStatementCallback;
import org.springframework.jdbc.core.CallableStatementCreator;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ParameterizedPreparedStatementSetter;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.StatementCallback;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.jdbc.support.SQLExceptionTranslator;
import org.springframework.jdbc.support.rowset.SqlRowSet;

import com.randomnoun.common.db.SqlWithArguments;

/** A JdbcTemplate that can take SqlWithArguments objects as parameters.
 * 
 * <p>This provides a bit of syntactic sugar so that you can write
 * 
 * <pre>jt.update(sqlWithArgs);</pre>
 * 
 * instead of 
 *  
 * <pre>jt.update(sqlWithArgs.getSql(), sqlWithArgs.getArgs(), sqlWithArgs.getArgTypes());</pre>
 * 
 * <p>All JdbcTemplate methods that take <code>String sql, Object[] args, int[] argTypes</code>
 * parameters have an additional SqlWithArguments equivalent.
 * 
 * <p>This class will also recognise SelectFromResultSetExtractors and SelectFromRowMappers and will
 * add the appropriate SELECTs and FROMs to the SQL before it is executed.
 * 
 * @author knoxg
 */
public class JdbcTemplateWithArguments extends JdbcTemplate {

	JdbcTemplate jt;
	public JdbcTemplateWithArguments(JdbcTemplate jt) {
		if (jt instanceof JdbcTemplateWithArguments) {
			throw new IllegalStateException("Cannot wrap a JdbcTemplateWithArguments with a JdbcTemplateWithArguments"); 
		}
		this.jt = jt;
	}

	/** Adds 'SELECT' and 'FROM' clauses to the supplied SQL if the caller has also supplied a SelectFromResultSetExtractor */ 
	private <T> String getSelectFromSql(String sql, ResultSetExtractor<T> rse) {
		if (rse instanceof SelectFromResultSetExtractor) {
			SelectFromResultSetExtractor<T> sfrse = (SelectFromResultSetExtractor<T>) rse;
			sql = "SELECT " + sfrse.getSelect() + " FROM " + sfrse.getFrom() + " " + sql;
		}
		return sql;
	}
	
	/** Adds 'SELECT' and 'FROM' clauses to the supplied SQL if the caller has also supplied a SelectFromRowMapper */
	private <T> String getSelectFromSql(String sql, RowMapper<T> rowMapper) {
		if (rowMapper instanceof SelectFromRowMapper) {
			SelectFromRowMapper<T> sfRowMapper = (SelectFromRowMapper<T>) rowMapper;
			sql = "SELECT " + sfRowMapper.getSelect() + " FROM " + sfRowMapper.getFrom() + " " + sql;
		}
		return sql;
	}

	
	public void setDataSource(DataSource dataSource) {
		jt.setDataSource(dataSource);
	}
	public int hashCode() {
		return jt.hashCode();
	}
	public DataSource getDataSource() {
		return jt.getDataSource();
	}
	public void setDatabaseProductName(String dbName) {
		jt.setDatabaseProductName(dbName);
	}
	public void setExceptionTranslator(SQLExceptionTranslator exceptionTranslator) {
		jt.setExceptionTranslator(exceptionTranslator);
	}
	public SQLExceptionTranslator getExceptionTranslator() {
		return jt.getExceptionTranslator();
	}
	public boolean equals(Object obj) {
		return jt.equals(obj);
	}
	public void setLazyInit(boolean lazyInit) {
		jt.setLazyInit(lazyInit);
	}
	public boolean isLazyInit() {
		return jt.isLazyInit();
	}
	public void afterPropertiesSet() {
		jt.afterPropertiesSet();
	}
	public void setIgnoreWarnings(boolean ignoreWarnings) {
		jt.setIgnoreWarnings(ignoreWarnings);
	}
	public boolean isIgnoreWarnings() {
		return jt.isIgnoreWarnings();
	}
	public void setFetchSize(int fetchSize) {
		jt.setFetchSize(fetchSize);
	}
	public int getFetchSize() {
		return jt.getFetchSize();
	}
	public void setMaxRows(int maxRows) {
		jt.setMaxRows(maxRows);
	}
	public String toString() {
		return jt.toString();
	}
	public int getMaxRows() {
		return jt.getMaxRows();
	}
	public void setQueryTimeout(int queryTimeout) {
		jt.setQueryTimeout(queryTimeout);
	}
	public int getQueryTimeout() {
		return jt.getQueryTimeout();
	}
	public void setSkipResultsProcessing(boolean skipResultsProcessing) {
		jt.setSkipResultsProcessing(skipResultsProcessing);
	}
	public boolean isSkipResultsProcessing() {
		return jt.isSkipResultsProcessing();
	}
	public void setSkipUndeclaredResults(boolean skipUndeclaredResults) {
		jt.setSkipUndeclaredResults(skipUndeclaredResults);
	}
	public boolean isSkipUndeclaredResults() {
		return jt.isSkipUndeclaredResults();
	}
	public void setResultsMapCaseInsensitive(boolean resultsMapCaseInsensitive) {
		jt.setResultsMapCaseInsensitive(resultsMapCaseInsensitive);
	}
	public boolean isResultsMapCaseInsensitive() {
		return jt.isResultsMapCaseInsensitive();
	}
	public <T> T execute(ConnectionCallback<T> action) throws DataAccessException {
		return jt.execute(action);
	}
	public <T> T execute(StatementCallback<T> action) throws DataAccessException {
		return jt.execute(action);
	}
	public void execute(String sql) throws DataAccessException {
		jt.execute(sql);
	}
	public <T> T query(String sql, ResultSetExtractor<T> rse) throws DataAccessException {
		sql = getSelectFromSql(sql, rse);
		return jt.query(sql, rse);
	}
	public void query(String sql, RowCallbackHandler rch) throws DataAccessException {
		jt.query(sql, rch);
	}
	public <T> List<T> query(String sql, RowMapper<T> rowMapper) throws DataAccessException {
		sql = getSelectFromSql(sql, rowMapper);
		return jt.query(sql, rowMapper);
	}
	public Map<String, Object> queryForMap(String sql) throws DataAccessException {
		return jt.queryForMap(sql);
	}
	public <T> T queryForObject(String sql, RowMapper<T> rowMapper) throws DataAccessException {
		sql = getSelectFromSql(sql, rowMapper);
		return jt.queryForObject(sql, rowMapper);
	}
	public <T> T queryForObject(String sql, Class<T> requiredType) throws DataAccessException {
		return jt.queryForObject(sql, requiredType);
	}
	public <T> List<T> queryForList(String sql, Class<T> elementType) throws DataAccessException {
		return jt.queryForList(sql, elementType);
	}
	public List<Map<String, Object>> queryForList(String sql) throws DataAccessException {
		return jt.queryForList(sql);
	}
	public SqlRowSet queryForRowSet(String sql) throws DataAccessException {
		return jt.queryForRowSet(sql);
	}
	public int update(String sql) throws DataAccessException {
		return jt.update(sql);
	}
	public int[] batchUpdate(String... sql) throws DataAccessException {
		return jt.batchUpdate(sql);
	}
	public <T> T execute(PreparedStatementCreator psc, PreparedStatementCallback<T> action) throws DataAccessException {
		return jt.execute(psc, action);
	}
	public <T> T execute(String sql, PreparedStatementCallback<T> action) throws DataAccessException {
		return jt.execute(sql, action);
	}
	
	public <T> T query(PreparedStatementCreator psc, PreparedStatementSetter pss, ResultSetExtractor<T> rse)
			throws DataAccessException {
		return jt.query(psc, pss, rse);
	}
	public <T> T query(PreparedStatementCreator psc, ResultSetExtractor<T> rse) throws DataAccessException {
		return jt.query(psc, rse);
	}
	public <T> T query(String sql, PreparedStatementSetter pss, ResultSetExtractor<T> rse) throws DataAccessException {
		sql = getSelectFromSql(sql, rse);
		return jt.query(sql, pss, rse);
	}
	
	/** Calls {@link JdbcTemplate#query(String, Object[], ResultSetExtractor)}
	 * @see JdbcTemplate#query(String, Object[], ResultSetExtractor)
	 */ 
	public <T> T query(SqlWithArguments sqlWithArgs, ResultSetExtractor<T> rse)
			throws DataAccessException {
		String sql = getSelectFromSql(sqlWithArgs.getSql(), rse);
		return jt.query(sql, sqlWithArgs.getArgs(), sqlWithArgs.getArgTypes(), rse);
	}
	public <T> T query(String sql, Object[] args, int[] argTypes, ResultSetExtractor<T> rse)
			throws DataAccessException {
		sql = getSelectFromSql(sql, rse);
		return jt.query(sql, args, argTypes, rse);
	}
	public <T> T query(String sql, Object[] args, ResultSetExtractor<T> rse) throws DataAccessException {
		sql = getSelectFromSql(sql, rse);
		return jt.query(sql, args, rse);
	}
	public <T> T query(String sql, ResultSetExtractor<T> rse, Object... args) throws DataAccessException {
		sql = getSelectFromSql(sql, rse);
		return jt.query(sql, rse, args);
	}
	public void query(PreparedStatementCreator psc, RowCallbackHandler rch) throws DataAccessException {
		jt.query(psc, rch);
	}
	public void query(String sql, PreparedStatementSetter pss, RowCallbackHandler rch) throws DataAccessException {
		jt.query(sql, pss, rch);
	}
	/** Calls {@link JdbcTemplate#query(String, Object[], int[], RowCallbackHandler)}
	 * @see JdbcTemplate#query(String, Object[], int[], RowCallbackHandler)
	 */ 
	public void query(SqlWithArguments sqlWithArgs, RowCallbackHandler rch) throws DataAccessException {
		jt.query(sqlWithArgs.getSql(), sqlWithArgs.getArgs(), sqlWithArgs.getArgTypes(), rch);
	}
	public void query(String sql, Object[] args, int[] argTypes, RowCallbackHandler rch) throws DataAccessException {
		jt.query(sql, args, argTypes, rch);
	}
	public void query(String sql, Object[] args, RowCallbackHandler rch) throws DataAccessException {
		jt.query(sql, args, rch);
	}
	public void query(String sql, RowCallbackHandler rch, Object... args) throws DataAccessException {
		jt.query(sql, rch, args);
	}
	public <T> List<T> query(PreparedStatementCreator psc, RowMapper<T> rowMapper) throws DataAccessException {
		return jt.query(psc, rowMapper);
	}
	public <T> List<T> query(String sql, PreparedStatementSetter pss, RowMapper<T> rowMapper)
			throws DataAccessException {
		sql = getSelectFromSql(sql, rowMapper);
		return jt.query(sql, pss, rowMapper);
	}
	
	/** Calls {@link JdbcTemplate#query(String, Object[], RowMapper)}
	 * @see JdbcTemplate#query(String, Object[], RowMapper) */ 
	public <T> List<T> query(SqlWithArguments sqlWithArgs, RowMapper<T> rowMapper)
			throws DataAccessException {
		String sql = getSelectFromSql(sqlWithArgs.getSql(), rowMapper);
		return jt.query(sql, sqlWithArgs.getArgs(), sqlWithArgs.getArgTypes(), rowMapper);
	}
	public <T> List<T> query(String sql, Object[] args, int[] argTypes, RowMapper<T> rowMapper)
			throws DataAccessException {
		sql = getSelectFromSql(sql, rowMapper);
		return jt.query(sql, args, argTypes, rowMapper);
	}
	public <T> List<T> query(String sql, Object[] args, RowMapper<T> rowMapper) throws DataAccessException {
		sql = getSelectFromSql(sql, rowMapper);
		return jt.query(sql, args, rowMapper);
	}
	public <T> List<T> query(String sql, RowMapper<T> rowMapper, Object... args) throws DataAccessException {
		sql = getSelectFromSql(sql, rowMapper);
		return jt.query(sql, rowMapper, args);
	}
	public <T> T queryForObject(String sql, Object[] args, int[] argTypes, RowMapper<T> rowMapper)
			throws DataAccessException {
		sql = getSelectFromSql(sql, rowMapper);
		return jt.queryForObject(sql, args, argTypes, rowMapper);
	}
	public <T> T queryForObject(String sql, Object[] args, RowMapper<T> rowMapper) throws DataAccessException {
		sql = getSelectFromSql(sql, rowMapper);
		return jt.queryForObject(sql, args, rowMapper);
	}
	public <T> T queryForObject(String sql, RowMapper<T> rowMapper, Object... args) throws DataAccessException {
		sql = getSelectFromSql(sql, rowMapper);
		return jt.queryForObject(sql, rowMapper, args);
	}
	/** Calls {@link JdbcTemplate#queryForObject(String, Object[], int[], Class)}
	 * @see JdbcTemplate#queryForObject(String, Object[], int[], Class) 
	 */ 
	public <T> T queryForObject(SqlWithArguments sqlWithArgs, Class<T> requiredType)
			throws DataAccessException {
		return jt.queryForObject(sqlWithArgs.getSql(), sqlWithArgs.getArgs(), sqlWithArgs.getArgTypes(), requiredType);
	}
	public <T> T queryForObject(String sql, Object[] args, int[] argTypes, Class<T> requiredType)
			throws DataAccessException {
		return jt.queryForObject(sql, args, argTypes, requiredType);
	}
	public <T> T queryForObject(String sql, Object[] args, Class<T> requiredType) throws DataAccessException {
		return jt.queryForObject(sql, args, requiredType);
	}
	public <T> T queryForObject(String sql, Class<T> requiredType, Object... args) throws DataAccessException {
		return jt.queryForObject(sql, requiredType, args);
	}
	/** Calls {@link JdbcTemplate#queryForMap(String, Object[], int[])}
	 * @see JdbcTemplate#queryForMap(String, Object[], int[]) 
	 */ 
	public Map<String, Object> queryForMap(SqlWithArguments sqlWithArgs) throws DataAccessException {
		return jt.queryForMap(sqlWithArgs.getSql(), sqlWithArgs.getArgs(), sqlWithArgs.getArgTypes());
	}
	public Map<String, Object> queryForMap(String sql, Object[] args, int[] argTypes) throws DataAccessException {
		return jt.queryForMap(sql, args, argTypes);
	}
	public Map<String, Object> queryForMap(String sql, Object... args) throws DataAccessException {
		return jt.queryForMap(sql, args);
	}
	/** Calls {@link JdbcTemplate#queryForList(String, Object[], int[], Class)}
	 * @see JdbcTemplate#queryForList(String, Object[], int[], Class)) 
	 */ 
	public <T> List<T> queryForList(SqlWithArguments sqlWithArgs, Class<T> elementType)
			throws DataAccessException {
		return jt.queryForList(sqlWithArgs.getSql(), sqlWithArgs.getArgs(), sqlWithArgs.getArgTypes(), elementType);
	}
	public <T> List<T> queryForList(String sql, Object[] args, int[] argTypes, Class<T> elementType)
			throws DataAccessException {
		return jt.queryForList(sql, args, argTypes, elementType);
	}
	public <T> List<T> queryForList(String sql, Object[] args, Class<T> elementType) throws DataAccessException {
		return jt.queryForList(sql, args, elementType);
	}
	public <T> List<T> queryForList(String sql, Class<T> elementType, Object... args) throws DataAccessException {
		return jt.queryForList(sql, elementType, args);
	}
	/** Calls {@link JdbcTemplate#queryForList(String, Object[], int[])}
	 * @see JdbcTemplate#queryForList(String, Object[], int[])}
	 */ 
	public List<Map<String, Object>> queryForList(SqlWithArguments sqlWithArgs)
			throws DataAccessException {
		return jt.queryForList(sqlWithArgs.getSql(), sqlWithArgs.getArgs(), sqlWithArgs.getArgTypes());
	}
	public List<Map<String, Object>> queryForList(String sql, Object[] args, int[] argTypes)
			throws DataAccessException {
		return jt.queryForList(sql, args, argTypes);
	}
	public List<Map<String, Object>> queryForList(String sql, Object... args) throws DataAccessException {
		return jt.queryForList(sql, args);
	}
	/** Calls {@link JdbcTemplate#queryForRowSet(String, Object[], int[])}
	 * @see JdbcTemplate#queryForRowSet(String, Object[], int[]) 
	 */ 
	public SqlRowSet queryForRowSet(SqlWithArguments sqlWithArgs) throws DataAccessException {
		return jt.queryForRowSet(sqlWithArgs.getSql(), sqlWithArgs.getArgs(), sqlWithArgs.getArgTypes());
	}
	public SqlRowSet queryForRowSet(String sql, Object[] args, int[] argTypes) throws DataAccessException {
		return jt.queryForRowSet(sql, args, argTypes);
	}
	public SqlRowSet queryForRowSet(String sql, Object... args) throws DataAccessException {
		return jt.queryForRowSet(sql, args);
	}
	public int update(PreparedStatementCreator psc) throws DataAccessException {
		return jt.update(psc);
	}
	public int update(PreparedStatementCreator psc, KeyHolder generatedKeyHolder) throws DataAccessException {
		return jt.update(psc, generatedKeyHolder);
	}
	public int update(String sql, PreparedStatementSetter pss) throws DataAccessException {
		return jt.update(sql, pss);
	}
	/** Calls {@link JdbcTemplate#update(String, Object[], int[])}
	 * @see JdbcTemplate#update(String, Object[], int[]) 
	 */ 
	public int update(SqlWithArguments sqlWithArgs) throws DataAccessException {
		return jt.update(sqlWithArgs.getSql(), sqlWithArgs.getArgs(), sqlWithArgs.getArgTypes());
	}
	public int update(String sql, Object[] args, int[] argTypes) throws DataAccessException {
		return jt.update(sql, args, argTypes);
	}
	public int update(String sql, Object... args) throws DataAccessException {
		return jt.update(sql, args);
	}
	public int[] batchUpdate(String sql, BatchPreparedStatementSetter pss) throws DataAccessException {
		return jt.batchUpdate(sql, pss);
	}
	public int[] batchUpdate(String sql, List<Object[]> batchArgs) throws DataAccessException {
		return jt.batchUpdate(sql, batchArgs);
	}
	/* could supply a list of SqlWithArguments and assume the sql & types are identical for all, but 
	 * probably safer just to leave this 
	public int[] batchUpdate(List<SqlWithArguments> batchSqlWithArgs) throws DataAccessException {
		return jt.batchUpdate(sql, batchArgs, argTypes);
	}
	*/
	public int[] batchUpdate(String sql, List<Object[]> batchArgs, int[] argTypes) throws DataAccessException {
		return jt.batchUpdate(sql, batchArgs, argTypes);
	}
	public <T> int[][] batchUpdate(String sql, Collection<T> batchArgs, int batchSize,
			ParameterizedPreparedStatementSetter<T> pss) throws DataAccessException {
		return jt.batchUpdate(sql, batchArgs, batchSize, pss);
	}
	public <T> T execute(CallableStatementCreator csc, CallableStatementCallback<T> action) throws DataAccessException {
		return jt.execute(csc, action);
	}
	public <T> T execute(String callString, CallableStatementCallback<T> action) throws DataAccessException {
		return jt.execute(callString, action);
	}
	public Map<String, Object> call(CallableStatementCreator csc, List<SqlParameter> declaredParameters)
			throws DataAccessException {
		return jt.call(csc, declaredParameters);
	}
	
}
