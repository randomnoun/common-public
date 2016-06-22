package com.randomnoun.common.dao.db;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;








//import com.jacob.com.Variant;
import com.randomnoun.common.spring.StringRowMapper;
import com.randomnoun.common.to.db.DatabaseTO;
import com.randomnoun.common.to.db.DatabaseTO.ConstraintColumnTO;
import com.randomnoun.common.to.db.DatabaseTO.ConstraintTO;
import com.randomnoun.common.to.db.DatabaseTO.ConstraintType;
import com.randomnoun.common.to.db.DatabaseTO.DatabaseType;
import com.randomnoun.common.to.db.DatabaseTO.SchemaTO;
import com.randomnoun.common.to.db.DatabaseTO.TableColumnTO;
import com.randomnoun.common.to.db.DatabaseTO.TableTO;
import com.randomnoun.common.to.db.DatabaseTO.TriggerTO;

/** Class to load DatabaseTOs */
public class DatabaseDAO {

	Logger logger = Logger.getLogger(DatabaseDAO.class);
	
	// only required for online DB metadata
	// private DataSource ds;
	private JdbcTemplate jt;
	
	DatabaseType dbType;
	
	public DatabaseDAO(JdbcTemplate jt, DatabaseType dbType) {
		this.jt = jt;
		this.dbType = dbType;
	}

	public SchemaTO getSchema(DatabaseTO d, String schemaName, boolean getDetails) {
		if (schemaName==null) {
			switch (d.getDatabaseType()) {
				case ORACLE:
					throw new UnsupportedOperationException("Not supported for this database type");
				
				case MYSQL:
					schemaName = (String) jt.queryForObject("SELECT DATABASE();", java.lang.String.class);
					break;
			
				case SQLSERVER:
					throw new UnsupportedOperationException("Not supported for this database type");
					
				default:
					throw new IllegalStateException("Unknown database type " + d.getDatabaseType());
			}
		}
		SchemaTO s = d.getSchemas().get(d.upper(schemaName));
		if (s == null) {
			s = new SchemaTO(d, schemaName);
			if (getDetails) {
				List<String> tableNames = getTableNames(s);
				for (String t : tableNames) { 
					getTable(s, t); // unused return value; adds to schemaTO 
				}  
				
				List<String> triggerNames = getTriggerNames(s);
				for (String t : triggerNames) { 
					getTrigger(s, t); // unused return value; adds to schemaTO 
				}
			}
			d.getSchemas().put(d.upper(schemaName), s); 
		}
		return s;
	}

	
	// @TODO subclass the different DatabaseType code here later
	public ConstraintTO getConstraint(TableTO table, ConstraintType type, String name) {
		ConstraintTO c = new ConstraintTO();
		c.setTable(table);
		c.setType(type);
		c.setName(name);
		c.setColumns(new LinkedHashMap<String, ConstraintColumnTO>());
		final ConstraintTO constraintRef = c;

		logger.debug("Loading metadata for constraint '" + name + "'");
		// JdbcTemplate jt = new JdbcTemplate(table.getSchema().getDatasource());
		List columnList = null;
		switch (table.getSchema().getDatabase().getDatabaseType()) {
			case ORACLE:
				throw new UnsupportedOperationException("Not implemented for this database type");
				//break;
				
			case MYSQL:
				columnList = jt.query(
					"SELECT constraint_catalog, constraint_schema, constraint_name, " +
					"  table_catalog, table_schema, table_name, " + 
					"  column_name, ordinal_position, " +
					"  referenced_table_name, referenced_column_name " +
				    " FROM information_schema.key_column_usage" +
				    " WHERE constraint_name = '" + name + "' " +
				    " ORDER BY ordinal_position ",
					new RowMapper() {
						public Object mapRow(ResultSet rs, int rowCount) throws SQLException {
							return new ConstraintColumnTO(constraintRef, 
								constraintRef.getTable().getSchema().getDatabase().upper(rs.getString("COLUMN_NAME")),
								rs.getLong("ORDINAL_POSITION"),
								constraintRef.getTable().getSchema().getDatabase().upper(rs.getString("REFERENCED_TABLE_NAME")),
								constraintRef.getTable().getSchema().getDatabase().upper(rs.getString("REFERENCED_COLUMN_NAME"))
							);
						}
					});
				break;

			case SQLSERVER:
				columnList = jt.query(
					"SELECT constraint_catalog, constraint_schema, constraint_name, " +
					"  table_catalog, table_schema, table_name, " + 
					"  column_name, ordinal_position " +
				    " FROM information_schema.key_column_usage" +
				    " WHERE constraint_name = '" + name + "' " +
				    " ORDER BY ordinal_position ",
					new RowMapper() {
						public Object mapRow(ResultSet rs, int rowCount) throws SQLException {
							return new ConstraintColumnTO(constraintRef, 
								constraintRef.getTable().getSchema().getDatabase().upper(rs.getString("COLUMN_NAME")),
								rs.getLong("ORDINAL_POSITION"));
						}
					});
				break;
				
			default:
				throw new IllegalStateException("Unknown database type " + table.getSchema().getDatabase().getDatabaseType());
		}
		if (columnList.size() == 0 && type != ConstraintType.CHECK) {
			throw new IllegalStateException("No column metadata found for table '" + name + "'");
		}
		for (Iterator i = columnList.iterator(); i.hasNext(); ) {
			ConstraintColumnTO column = (ConstraintColumnTO) i.next();
			c.getColumns().put(column.getName(), column);
		}
		return c;
	}
	
	public TableTO getTable(SchemaTO schema, String tableName) {
		final TableTO t = new TableTO();
		t.setSchema(schema);
		t.setName(tableName);
		t.setColumns(new LinkedHashMap<String, TableColumnTO>());
		t.setConstraints(new LinkedHashMap<String, ConstraintTO>());

		// columns = new HashMap<String, TableColumnVO>();
		// populate it from the db
		logger.debug("Loading metadata for table '" + tableName + "'");
		// JdbcTemplate jt = new JdbcTemplate(schema.ds);
		List columnList = null;
		switch (schema.getDatabase().getDatabaseType()) {
			case ORACLE:
				columnList = jt.query(
					"SELECT ALL_TABLES.OWNER, ALL_TABLES.TABLE_NAME, ALL_TAB_COLUMNS.COLUMN_NAME, " +
					" DATA_TYPE, DATA_TYPE_MOD, DATA_LENGTH, DATA_PRECISION, DATA_SCALE, NULLABLE, " +
					" COMMENTS, COLUMN_ID " +
					"	FROM (ALL_TABLES INNER JOIN ALL_TAB_COLUMNS " + 
					"	  ON (ALL_TABLES.TABLE_NAME = ALL_TAB_COLUMNS.TABLE_NAME)) " +
					"	    LEFT JOIN ALL_COL_COMMENTS " +
					"	  ON (ALL_TAB_COLUMNS.TABLE_NAME = ALL_COL_COMMENTS.TABLE_NAME AND " + 
					"	         ALL_TAB_COLUMNS.COLUMN_NAME = ALL_COL_COMMENTS.COLUMN_NAME) " +
					"	WHERE " +
					
					/*
					"   ALL_TABLES.TABLESPACE_NAME IN ( " +
					"	  'A','List','of','tablespace' " +
					" 	) " + 
					"	AND ALL_TABLES.OWNER IN ('a','list','of','users') " +
					*/

					// hopefully not necessary (won't generate duplicate columns if 2 tables with same name in 2 different schemas )
					// it hides some tables which are necessary for column resolution, so leaving it out for the time being
					/* "   OWNER = '" + schema.name + "' " + "   AND */    
					
					" TABLE_NAME = '" + t.getName() + "' " +
					"	ORDER BY ALL_TAB_COLUMNS.TABLE_NAME, ALL_TAB_COLUMNS.COLUMN_ID", 
					new RowMapper() {
						public Object mapRow(ResultSet rs, int rowCount) throws SQLException {
							TableColumnTO col = new TableColumnTO();
							col.setTable(t);
							col.setName(t.getSchema().getDatabase().upper(rs.getString("COLUMN_NAME")));
							col.setColumnId(rs.getLong("COLUMN_ID"));
							col.setDataType(rs.getString("DATA_TYPE"));
							col.setDataTypeLength(rs.getLong("DATA_LENGTH")); if (rs.wasNull()) { col.setDataTypeLength(null); }
							col.setDataTypePrecision(rs.getLong("DATA_PRECISION")); if (rs.wasNull()) { col.setDataTypePrecision(null); }
							col.setDataScale(rs.getLong("DATA_SCALE")); if (rs.wasNull()) { col.setDataScale(null); }
							col.setNullable(rs.getString("NULLABLE").equals("Y") ? true : false);
							col.setDefaultValue(null);
							col.setComments(rs.getString("COMMENTS"));
							return col;
						}
					});
				break;
				
			case MYSQL:
				columnList = jt.query(
					// TABLE_CATALOG always NULL, TABLE_SCHEMA
					"SELECT TABLE_NAME, COLUMN_NAME, ORDINAL_POSITION, COLUMN_DEFAULT, IS_NULLABLE, "+
					"  DATA_TYPE, CHARACTER_MAXIMUM_LENGTH, CHARACTER_OCTET_LENGTH, " +
					"  NUMERIC_PRECISION, NUMERIC_SCALE, CHARACTER_SET_NAME, " +
					"  COLLATION_NAME, COLUMN_TYPE, COLUMN_KEY, EXTRA, PRIVILEGES, " +
					"  COLUMN_COMMENT "+
					" FROM INFORMATION_SCHEMA.COLUMNS " +
					" WHERE " +
					" TABLE_SCHEMA = '" + schema.getName() + "' AND " +
					(t.getSchema().getDatabase().isCaseInsensitive() ? 
						" UPPER(TABLE_NAME) = '" + t.getName() + "' " :
						" TABLE_NAME = '" + t.getName() + "' ") +		
					"	ORDER BY TABLE_NAME, ORDINAL_POSITION ", 
					new RowMapper() {
						public Object mapRow(ResultSet rs, int rowCount) throws SQLException {
							TableColumnTO col = new TableColumnTO();
							col.setTable(t);
							col.setName(t.getSchema().getDatabase().upper(rs.getString("COLUMN_NAME")));
							col.setColumnId(rs.getLong("ORDINAL_POSITION")); // I guess
							col.setDataType(rs.getString("DATA_TYPE"));
							col.setDataTypeLength(rs.getLong("CHARACTER_DATA_LENGTH")); if (rs.wasNull()) { col.setDataTypeLength(null); }
							col.setDataTypePrecision(rs.getLong("NUMERIC_PRECISION")); if (rs.wasNull()) { col.setDataTypePrecision(null); }
							col.setDataScale(rs.getLong("NUMERIC_SCALE")); if (rs.wasNull()) { col.setDataScale(null); }
							col.setNullable(rs.getString("IS_NULLABLE").startsWith("Y") ? true : false);
							col.setDefaultValue(rs.getString("COLUMN_DEFAULT"));
							col.setComments(rs.getString("COLUMN_COMMENT"));
							// @TODO PRIVILEGES, EXTRA, COLUMN_KEY, COLUMN_TYPE (not DATA_TYPE?)
							// COLLATION_NAME, CHARACTER_SET_NAME
							return col;
						}
					});
				break;

			case SQLSERVER:
				columnList = jt.query(
					"SELECT table_catalog, table_schema, table_name, column_name, " +
					"  ordinal_position, column_default, is_nullable, data_type, " +
					"  character_maximum_length, character_octet_length, numeric_precision, " +
					"  numeric_precision_radix, numeric_scale, datetime_precision, " +
					"  character_set_catalog, character_set_schema, character_set_name, " +
					"  collation_catalog, collation_schema, collation_name, domain_catalog, " +
					"  domain_schema, domain_name " +
				    " FROM information_schema.columns" +
				    " WHERE table_name = '" + t.getName() + "' " +
				    " ORDER BY table_name, ordinal_position ",
					new RowMapper() {
						public Object mapRow(ResultSet rs, int rowCount) throws SQLException {
							TableColumnTO col = new TableColumnTO();
							col.setTable(t);
							col.setName(t.getSchema().getDatabase().upper(rs.getString("COLUMN_NAME")));
							col.setColumnId(rs.getLong("ORDINAL_POSITION")); // I guess
							col.setDataType(rs.getString("DATA_TYPE"));
							col.setDataTypeLength(rs.getLong("CHARACTER_MAXIMUM_LENGTH")); if (rs.wasNull()) { col.setDataTypeLength(null); }
							col.setDataTypePrecision(rs.getLong("NUMERIC_PRECISION")); if (rs.wasNull()) { col.setDataTypePrecision(null); }
							col.setDataScale(rs.getLong("NUMERIC_SCALE")); if (rs.wasNull()) { col.setDataScale(null); }
							col.setNullable(rs.getString("IS_NULLABLE").startsWith("Y") ? true : false);
							col.setDefaultValue(rs.getString("COLUMN_DEFAULT"));
							col.setComments(null); // ?
							// @TODO CHARACTER_OCTET_LENGTH, NUMERIC_PRECISION_RADIX, DATETIME_PRECISION
							// COLLATION_CATALOG, COLLATION_SCHEMA, COLLATION_NAME, 
							// CHARACTER_SET_CATALOG, CHARACTER_SET_SCHEMA, CHARACTER_SET_NAME,
							// DOMAIN_CATALOG, DOMAIN_SCHEMA, DOMAIN_NAME
							return col;
						}
					});
				break;

			case JET:
				// access 2007 see stackoverflow 11548697, or not 
				// access 2000 ?
				// throw new UnsupportedOperationException("Appears access doesn't allow this sort of thing");
				// maybe fallback to Java metadata
				
				// looks suspiciously like the SQLServer code, so I assume none of this works
				columnList = jt.query(
					"SELECT table_catalog, table_schema, table_name, column_name, " +
					"  ordinal_position, column_default, is_nullable, data_type, " +
					"  character_maximum_length, character_octet_length, numeric_precision, " +
					"  numeric_precision_radix, numeric_scale, datetime_precision, " +
					"  character_set_catalog, character_set_schema, character_set_name, " +
					"  collation_catalog, collation_schema, collation_name, domain_catalog, " +
					"  domain_schema, domain_name " +
				    " FROM information_schema.columns" +
				    " WHERE table_name = '" + t.getName() + "' " +
				    " ORDER BY table_name, ordinal_position ",
					new RowMapper() {
						public Object mapRow(ResultSet rs, int rowCount) throws SQLException {
							return new TableColumnTO(t, 
								t.getSchema().getDatabase().upper(rs.getString("COLUMN_NAME")),
								rs.getLong("ORDINAL_POSITION"),
								rs.getString("DATA_TYPE"),
								rs.getObject("CHARACTER_MAXIMUM_LENGTH") == null ? -1 : rs.getLong("CHARACTER_MAXIMUM_LENGTH"),
								rs.getObject("NUMERIC_PRECISION") == null ? -1 : rs.getLong("NUMERIC_PRECISION"),
								rs.getObject("NUMERIC_SCALE") == null ? -1 : rs.getLong("NUMERIC_SCALE"),
								rs.getString("IS_NULLABLE").startsWith("Y") ? true : false,
								rs.getString("COLUMN_DEFAULT"),
								null);
						}
					});
				break;

			default:
				throw new IllegalStateException("Unknown database type " + schema.getDatabase().getDatabaseType());
		}
		if (columnList.size() == 0) {
			throw new IllegalStateException("No column metadata found for table '" + t.getName() + "'");
		}
		for (Iterator i = columnList.iterator(); i.hasNext(); ) {
			TableColumnTO column = (TableColumnTO) i.next();
			t.getColumns().put(column.getName(), column);
		}
		return t;
	}

	public List<String> getTableNames(SchemaTO s) {
		List tableList = null;
		switch (s.getDatabase().getDatabaseType()) {
			case ORACLE:
				throw new UnsupportedOperationException("Not supported for this database type");
			
			case MYSQL:
				tableList = jt.query(
					"SELECT TABLE_NAME "+
					" FROM INFORMATION_SCHEMA.TABLES " +
					" WHERE TABLE_SCHEMA='" + s.getName() + "'",
					new StringRowMapper() );
				break;

			case SQLSERVER:
				tableList = jt.query(
					"SELECT TABLE_NAME "+
					" FROM INFORMATION_SCHEMA.TABLES " +
					" WHERE TABLE_SCHEMA='" + s.getName() + "'",
					new StringRowMapper() );
				break;

			case FIREBIRD: 
				throw new UnsupportedOperationException("Read this and then code something up: http://stackoverflow.com/questions/10945384/firebird-sql-statement-to-get-the-table-schema");

			case JET:
				// see http://stackoverflow.com/questions/2629211/can-we-list-all-tables-in-msaccess-database-using-sql
				// returns tables (1), linked odbc tables (4) and linked access tables (6)
				tableList = jt.query(
					"SELECT name "+
					" FROM MSysObjects " +
					" WHERE type IN (1,4,6)",
					new StringRowMapper() );
				break;
				
				
			case JET_DAO:
				// should probably be DAO or ADOing this, but...
				/* so much for that idea 
				tableList = new ArrayList<String>();
				Workspace workspace = new Workspace(database.jetDatabaseFilename);
				Databases databases = workspace.getDatabases();
				Database daoDatabase = databases.getItem(new Variant(0));
				// Database daoDatabase = new Database(database.jetDatabaseFilename);
				TableDefs tableDefs = daoDatabase.getTableDefs();
				for (int i=1; i<=tableDefs.getCount(); i++) {
					TableDef t = tableDefs.getItem(new Variant(i));
					tableList.add(t.getName());
				}
				// @TODO: probably needs to be in a try/catch
				daoDatabase.close();
				*/
				throw new IllegalStateException("Unimplemented database type " + s.getDatabase().getDatabaseType());
				//break;
				
			default:
				throw new IllegalStateException("Unknown database type " + s.getDatabase().getDatabaseType());
		}
		return s.getDatabase().upper(tableList);
	}
	
	public List<String> getTriggerNames(SchemaTO s) {
		List triggerList = null;
		switch (s.getDatabase().getDatabaseType()) {
			case ORACLE:
				throw new UnsupportedOperationException("Not supported for this database type");
			
			case MYSQL:
				triggerList = jt.query(
					"SELECT TRIGGER_NAME "+
					" FROM INFORMATION_SCHEMA.TRIGGERS " +
					" WHERE TRIGGER_SCHEMA='" + s.getName() + "'",  // c.f. EVENT_OBJECT_TRIGGER
					new StringRowMapper() );
				break;
		
			case SQLSERVER:
				// from http://msdn.microsoft.com/en-us/magazine/cc163442.aspx
				triggerList = jt.query(
				"SELECT so_tr.name AS TriggerName " +
				/*
				" , so_tbl.name AS TableName, " +
				" t.TABLE_SCHEMA AS TableSchema " +
				*/
				" FROM sysobjects so_tr " +
				" INNER JOIN sysobjects so_tbl ON so_tr.parent_obj = so_tbl.id " +
				" INNER JOIN INFORMATION_SCHEMA.TABLES t ON t.TABLE_NAME = so_tbl.name " +
				" WHERE so_tr.type = 'TR' " +
				" AND t.TABLE_SCHEMA = '" + s.getName() + "'",
				//" ORDER BY so_tbl.name ASC, so_tr.name ASC",
				new StringRowMapper() );
				break;
				
			default:
				throw new IllegalStateException("Unknown database type " + s.getDatabase().getDatabaseType());
		}
		return s.getDatabase().upper(triggerList);
	}
	
	public TriggerTO getTrigger(SchemaTO schema, String triggerName) {
		final TriggerTO t = new TriggerTO();
		t.setSchema(schema);
		t.setName(triggerName);
		

		// columns = new HashMap<String, TableColumnVO>();
		// populate it from the db
		logger.debug("Loading metadata for table '" + tableName + "'");
		// JdbcTemplate jt = new JdbcTemplate(schema.ds);
		List columnList = null;
		switch (schema.getDatabase().getDatabaseType()) {
			case ORACLE:
				throw new UnsupportedOperationException("Not supported for this database type");
				
			case MYSQL:
				columnList = jt.query(
					// TABLE_CATALOG always NULL, TABLE_SCHEMA
					"SELECT EVENT_MANIPULATION, EVENT_OBJECT_CATALOG, EVENT_OBJECT_SCHEMA, EVENT_OBJECT_TABLE, " +
					" ACTION_ORDER, ACTION_CONDITION, ACTION_STATEMENT, ACTION_ORIENTATION, ACTION_TIMING, " +
					" ACTION_REFERENCE_OLD_TABLE, ACTION_REFERENCE_NEW_TABLE, " +
					" ACTION_REFERENCE_OLD_ROW, ACTION_REFERENCE_NEW_ROW, " +
					" CREATED, SQL_MODE, DEFINER, CHARACTER_SET_CLIENT, COLLATION_CONNECTION, DATABASE_COLLATION " +
					" FROM INFORMATION_SCHEMA.TRIGGERS " +
					" WHERE " +
					" TRIGGER_SCHEMA = '" + schema.getName() + "' AND " +
					(t.getSchema().getDatabase().isCaseInsensitive() ? 
						" UPPER(TRIGGER_NAME) = '" + t.getName() + "' " :
						" TRIGGER_NAME = '" + t.getName() + "' ") +		
					"	ORDER BY TRIGGER_NAME, ACTION_ORDER ", 
					new RowMapper() {
						public Object mapRow(ResultSet rs, int rowCount) throws SQLException {
							
							
							return new TriggerTO(s, 
								t.getSchema().getDatabase().upper(rs.getString("TRIGGER_NAME")),
								rs.getLong("ORDINAL_POSITION"),
								rs.getString("DATA_TYPE"),
								rs.getObject("CHARACTER_MAXIMUM_LENGTH") == null ? -1 : rs.getLong("CHARACTER_MAXIMUM_LENGTH"),
								rs.getObject("NUMERIC_PRECISION") == null ? -1 : rs.getLong("NUMERIC_PRECISION"),
								rs.getObject("NUMERIC_SCALE") == null ? -1 : rs.getLong("NUMERIC_SCALE"),
								rs.getString("IS_NULLABLE").startsWith("Y") ? true : false,
								rs.getString("COLUMN_DEFAULT"),
								rs.getString("COLUMN_COMMENT"));
						}
					});
				break;

			case SQLSERVER:
				throw new UnsupportedOperationException("Not supported for this database type");

			case JET:
				throw new UnsupportedOperationException("Not supported for this database type");

			default:
				throw new IllegalStateException("Unknown database type " + schema.getDatabase().getDatabaseType());
		}
		if (columnList.size() == 0) {
			throw new IllegalStateException("No column metadata found for table '" + t.getName() + "'");
		}
		for (Iterator i = columnList.iterator(); i.hasNext(); ) {
			TableColumnTO column = (TableColumnTO) i.next();
			t.getColumns().put(column.getName(), column);
		}
		return t;
	}

	
	
	public List<String> getSchemaNames(DatabaseTO db) {
		List schemaList = null;
		switch (db.getDatabaseType()) {
			case ORACLE:
				throw new UnsupportedOperationException("Not supported for this database type");
			
			case MYSQL:
				// CATALOG_NAME (always 'def'), SCHEMA_NAME, DEFAULT_CHARACTER_SET_NAME, DEFAULT_COLLATION_NAME, SQL_PATH (always null)
				// also: 'SHOW DATABASES'
				schemaList = jt.query(
					"SELECT SCHEMA_NAME "+
					" FROM INFORMATION_SCHEMA.SCHEMATA ",
					new StringRowMapper() );
				break;

			case SQLSERVER:
				throw new UnsupportedOperationException("Not supported for this database type");
				
			default:
				throw new IllegalStateException("Unknown database type " + db.getDatabaseType());
		}
		return db.upper(schemaList);
		
	}
	
	public List<String> getConstraintNames(SchemaTO s) {
		List constraintList = null;
		switch (s.getDatabase().getDatabaseType()) {
			case ORACLE:
				throw new UnsupportedOperationException("Not supported for this database type");
			
			case MYSQL:
				constraintList = jt.query(
					"SELECT CONSTRAINT_NAME "+
					" FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS " +
					" WHERE TABLE_SCHEMA='" + s.getName() + "'" +
					" AND TABLE_NAME='" + s.getName() + "'",
					new StringRowMapper() );
				break;

			case SQLSERVER:
				constraintList = jt.query(
					"SELECT CONSTRAINT_NAME "+
					" FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS " +
					" WHERE TABLE_SCHEMA='" + s.getName() + "'" +
					" AND TABLE_NAME='" + s.getName() + "'",
					new StringRowMapper() );
				break;
				
			default:
				throw new IllegalStateException("Unknown database type " + s.getDatabase().getDatabaseType());
		}
		return s.getDatabase().upper(constraintList);
		
	}

	public com.randomnoun.common.to.db.DatabaseTO getDatabaseTO(boolean getDetails) throws SQLException {
		// return a DatabaseTO containing all schemas, tables, columns, triggers, and stored procs 
		DatabaseTO db = new DatabaseTO(this.dbType);
		Connection c = jt.getDataSource().getConnection();
		DatabaseMetaData dmd = c.getMetaData();
		// see https://books.google.com.au/books?id=a8W8fKQYiogC&pg=PA40&lpg=PA40&dq=mysql+year+datatype+jdbc&source=bl&ots=ok_NBw7-CL&sig=M2GF2I-BI3AhQN930qypRvNTqrQ&hl=en&sa=X&ei=RHIUVcTpCM_h8AXuvYGwCA&ved=0CEgQ6AEwBw#v=onepage&q=mysql%20year%20datatype%20jdbc&f=false
		// extract everything out of this dmd and put it in the dbTO
		try { db.setMajorVersion(dmd.getDatabaseMajorVersion()); } catch (Exception e) { }
		try { db.setMinorVersion(dmd.getDatabaseMinorVersion()); } catch (Exception e) { }
		try { db.setProductName(dmd.getDatabaseProductName()); } catch (Exception e) { }
		try { db.setProductVersion(dmd.getDatabaseProductVersion()); } catch (Exception e) { }
		
		// populate schemas
		if (getDetails) {
			List<String> schemaNames = getSchemaNames(db);
			for (String schemaName : schemaNames) {
				SchemaTO s = getSchema(db, schemaName, true);
			}
		}
		
		return db;
	}


	
}
