package com.randomnoun.common.to.db;

/* (c) 2013 randomnoun. All Rights Reserved. This work is licensed under a
 * BSD Simplified License. (http://www.randomnoun.com/bsd-simplified.html)
 */

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

/** Container class for database metadata.
 * 
 * <p>Contains methods for populating metadata from the data dictionaries of
 * Oracle and MySQL, for the purposes of whatever it is that I'm doing at the time.
 *
 *  
 *  NB: does not set default values for oracle
 *  
 * @version $Id$
 */
public class DatabaseTO {
	/** A revision marker to be used in exception stack traces. */
    public static final String _revision = "$Id$";


	static Logger logger = Logger.getLogger(DatabaseTO.class);
	
	boolean caseInsensitive = false;
	private DatabaseType dbType;
	private String jetDatabaseFilename;
	Map<String, SchemaTO> schemas = null; // I'm not calling these schemata
	
	// as reported by Connection to this database
	int majorVersion;
	int minorVersion;
	String productName;
	String productVersion;
	
	
	public static enum DatabaseType {
		/** A Jet (MSAccess) database accessed via the jdbc:odbc bridge */
		JET,
		/** A Jet (MSAccess) database accessed via the JACOB DAO library */
		JET_DAO,
		/** Oracle */
		ORACLE,
		/** MySQL */
		MYSQL,
		/** Microsoft SQL Server */
		SQLSERVER,
		/** Firebird */
		FIREBIRD
		
	}
	
	public static enum ConstraintType {
		PRIMARY, FOREIGN, CHECK, UNIQUE;
		
		public static ConstraintType fromDatabaseString(String s) {
			if (s.equals("PRIMARY KEY")) {
				return PRIMARY;
			} else if (s.equals("FOREIGN KEY")) {
				return FOREIGN;
			} else if (s.equals("CHECK")) {
				return CHECK; 
			} else if (s.equals("UNIQUE")) {
				return UNIQUE;
			} else {
				throw new IllegalArgumentException("No enum const for database string '" + s + "'");
			}
		}
	}
	
	public static class ConstraintTO {
		public Map<String, ConstraintColumnTO> getColumns() {
			return columns;
		}

		public void setColumns(Map<String, ConstraintColumnTO> columns) {
			this.columns = columns;
		}

		public void setName(String name) {
			this.name = name;
		}

		public void setTable(TableTO table) {
			this.table = table;
		}

		public void setType(ConstraintType type) {
			this.type = type;
		}

		String name;
		TableTO table;
		ConstraintType type;
		Map<String, ConstraintColumnTO> columns;
		
		public ConstraintTO() { }
		
		public TableTO getTable() { return table; }
		public String getName() { return name; }
		public ConstraintType getType() { return type; }
		
		public List<ConstraintColumnTO> getConstraintColumns() {
			return new ArrayList<ConstraintColumnTO>(columns.values());
		}
		
		public List<String> getConstraintColumnNames() {
			return new ArrayList<String>(columns.keySet());
		}
	}
	
	public static class ConstraintColumnTO {
		ConstraintTO constraint;
		String name;
		String refTableName;
		String refColumnName;
		long columnId;
		
		public ConstraintColumnTO(ConstraintTO constraint, String name, long columnId) {
			this.constraint = constraint;
			this.name = name;
			this.columnId = columnId;
		}
		public ConstraintColumnTO(ConstraintTO constraint, String name, long columnId, String refTableName, String refColumnName) {
			this.constraint = constraint;
			this.name = name;
			this.columnId = columnId;
			this.refTableName = refTableName;
			this.refColumnName = refColumnName;
		}
		public ConstraintTO getConstraint() { return constraint; }
		public String getName() { return name; }
		public long getColumnId() { return columnId; }

		public TableColumnTO getTableColumn() {
			return constraint.table.getTableColumn(name);
		}

	}
	
	public static class TableTO {
		public Map<String, TableColumnTO> getColumns() {
			return columns;
		}


		public void setColumns(Map<String, TableColumnTO> columns) {
			this.columns = columns;
		}


		public Map<String, ConstraintTO> getConstraints() {
			return constraints;
		}


		public void setConstraints(Map<String, ConstraintTO> constraints) {
			this.constraints = constraints;
		}


		public void setName(String name) {
			this.name = name;
		}


		public void setSchema(SchemaTO schema) {
			this.schema = schema;
		}

		String name;
		SchemaTO schema;
		Map<String, TableColumnTO> columns;
		Map<String, ConstraintTO> constraints;

		public TableTO() { }
		
		
		public TableColumnTO getTableColumn(String name) {
			TableColumnTO column = columns.get(name);
			if (column==null) { throw new IllegalArgumentException("Column '" + name + "' not found"); }
			return column;
		};
		
		public List<TableColumnTO> getTableColumns() {
			return new ArrayList<TableColumnTO>(columns.values());
		}
		
		public List<String> getTableColumnNames() {
			return new ArrayList<String>(columns.keySet());
		}
		public String getName() { return name; }
		public SchemaTO getSchema() { return schema; }
		
		public ConstraintTO getConstraint(String constraintName) {
			ConstraintTO constraint = constraints.get(schema.database.upper(constraintName));
			if (constraint == null) {
				throw new UnsupportedOperationException("use DAO to construct constraints");
				// @TODO load on demand ?
				/*
				String typeString = (String) schema.jt.queryForObject(
					"SELECT CONSTRAINT_TYPE "+
					" FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS " +
					" WHERE TABLE_SCHEMA='" + schema.name + "' " +
					" AND TABLE_NAME='" + name + "' " +
					" AND CONSTRAINT_NAME='" + constraintName + "'", String.class);
				ConstraintType type = ConstraintType.fromDatabaseString(typeString);
				constraint = new ConstraintTO(this, type, schema.database.upper(constraintName)); 
				constraints.put(schema.database.upper(constraintName), constraint);
				*/ 
			}
			return constraint;
		}
		
	}
	
	/** Holds type information for a table column. 
	 */
	public static class TableColumnTO {
		TableTO table;
		String name;
		long columnId;
		String dataType;
		Long dataTypeLength;
		Long dataTypePrecision;
		Long dataScale;
		boolean nullable;
		String defaultValue;
		String comments;
		
		public TableColumnTO(TableTO table, String name, long columnId, String dataType,
			long dataTypeLength, long dataTypePrecision, long dataScale, 
			boolean nullable, String defaultValue, String comments) 
		{
			logger.debug("Datatype for '" + table.name + "." + name + "' is " + dataType + " (" + dataTypeLength + ", " + dataTypePrecision + ", " + dataScale + ")");  
			this.table = table;
			this.name = name;
			this.columnId = columnId;
			this.dataType = dataType;
			this.dataTypeLength = dataTypeLength;
			this.dataTypePrecision = dataTypePrecision;
			this.dataScale = dataScale;
			this.nullable = nullable;
			this.defaultValue = defaultValue;
			this.comments = comments;
		}
		public TableColumnTO() {
			// TODO Auto-generated constructor stub
		}
		public TableTO getTable() { return table; }
		public String getName() { return name; }
		public long   getColumnId() { return columnId; }
		public String getDataType() { return dataType; }
		public boolean getNullable() { return nullable; }
		public String getComments() { return comments; }

		public String getDefaultValue() {
			return defaultValue;
		}
		public void setDefaultValue(String defaultValue) {
			this.defaultValue = defaultValue;
		}
		public void setTable(TableTO table) {
			this.table = table;
		}
		public void setName(String name) {
			this.name = name;
		}
		public void setColumnId(long columnId) {
			this.columnId = columnId;
		}
		public void setDataType(String dataType) {
			this.dataType = dataType;
		}
		public void setNullable(boolean nullable) {
			this.nullable = nullable;
		}
		public void setComments(String comments) {
			this.comments = comments;
		}
		public Long getDataTypeLength() {
			return dataTypeLength;
		}
		public void setDataTypeLength(Long dataTypeLength) {
			this.dataTypeLength = dataTypeLength;
		}
		public Long getDataTypePrecision() {
			return dataTypePrecision;
		}
		public void setDataTypePrecision(Long dataTypePrecision) {
			this.dataTypePrecision = dataTypePrecision;
		}
		public Long getDataScale() {
			return dataScale;
		}
		public void setDataScale(Long dataScale) {
			this.dataScale = dataScale;
		}

		// @TODO need better tests here
		// only call these if dataType = NUMBER
		/*
		public boolean isLong() {
			if (table.schema.database.dbType==DatabaseTO.DatabaseType.ORACLE) {
				if (!this.getDataType().toUpperCase().equals("NUMBER")) { 
					throw new IllegalStateException("Cannot invoke on non-NUMBER datatype (found '" + this.getDataType() + "'");
				};
				return this.dataScale <= 0 ;
			} else if (table.schema.database.dbType==DatabaseTO.DatabaseType.MYSQL) {
				return !this.getDataType().equals("double"); // untested
			} else {
				throw new UnsupportedOperationException("Not supported for this database type");
			}
		}
		public boolean isDouble() {
			if (table.schema.database.dbType==DatabaseTO.DatabaseType.ORACLE) {
				if (!this.getDataType().toUpperCase().equals("NUMBER")) { 
					throw new IllegalStateException("Cannot invoke on non-NUMBER datatype (found '" + this.getDataType() + "'");
				};
				// this is interesting, no ?
				return this.dataScale > 0 && !getName().toUpperCase().contains("AMOUNT"); 
			} else if (table.schema.database.dbType==DatabaseTO.DatabaseType.MYSQL) {
				return this.getDataType().equals("double"); 
			} else {
				throw new UnsupportedOperationException("Not supported for this database type");
			}
		}
		*/

		public boolean isBigDecimal() {
			if (table.schema.database.dbType==DatabaseTO.DatabaseType.ORACLE) {
				if (!this.getDataType().toUpperCase().equals("NUMBER")) { 
					throw new IllegalStateException("Cannot invoke on non-NUMBER datatype (found '" + this.getDataType() + "'");
				};
				return this.dataScale > 0 && getName().toUpperCase().contains("AMOUNT") ; 
			} else if (table.schema.database.dbType==DatabaseTO.DatabaseType.MYSQL) {
				return false; // untested
			} else {
				throw new UnsupportedOperationException("Not supported for this database type");
			}
		}
		public String getTypeString() {
			if (dataScale!=-1 && dataTypePrecision==-1) {
				throw new IllegalStateException("datatype scale set without having precision set");
			}
			if (dataTypeLength!=-1 && dataTypePrecision!=-1) {
				throw new IllegalStateException("both datatype length and precision set");
			}
			if (dataTypeLength!=-1) {
				return dataType + "(" + dataTypeLength + ")";
			} else if (dataTypePrecision!=-1) {
				if (dataScale!=-1) {
					// @TODO is this the wrong way round ?
					return dataType + "(" + dataTypePrecision + "," + dataScale + ")";
				} else {
					return dataType + "(" + dataTypePrecision + ")";
				}
			} else {
				return dataType;
			}
		}
		
		
	}
	
	public static class SourceTypeTO {
		public SchemaTO schema;
		public List packages;
		public List packageBodies;
		public List procedures;
		public List functions;
		public List triggers;
	}
	
	public static class UserTypeTO {
		public SchemaTO schema;
		public List arrayTypes;
		public List objectTypes;
		public List tableTypes;
		public List xmlSchema;
	}
	
	public static class SecurityTO {
		public SchemaTO schema;
		public List users;
		public List roles;
		public List profiles;
	}
	
	public static class StorageTO {
		public SchemaTO schema;
		public Map<String, TableTO> tablespaces;
		public List datafiles;
		public List rollbackSegments;
		public List redoLogGroups;
		public List archiveLogs;
	}
	
	public static class DistributedTO {
		public SchemaTO schema;
		public List inDoubtTransactions;
		public List databaseLinks;
		public List streams;
		public List advancedQueues;
		public List advancedReplication;
		
	}
	
	// @TODO enum this
	public static class TriggerTO {
		SchemaTO schema; // this is how mysql stores them. Might make more sense
		               // to make this a table reference
		String name;
		String eventManipulation; // INSERT/UPDATE/DELETE
		// String eventObjectCatalog; // NULL in mysql. Now it's 'def'
		String eventObjectSchema;  // is this ever != schema.name ?
		String eventObjectTable;
		//long actionOrder;          // always 0 
		//String actionCondition;    // always null
		String actionStatement;
		String actionTiming; // BEFORE/AFTER
		String sqlMode;
		String definer;
		
		public SchemaTO getSchema() {
			return schema;
		}
		public void setSchema(SchemaTO schema) {
			this.schema = schema;
		}
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public String getEventManipulation() {
			return eventManipulation;
		}
		public void setEventManipulation(String eventManipulation) {
			this.eventManipulation = eventManipulation;
		}
		public String getEventObjectSchema() {
			return eventObjectSchema;
		}
		public void setEventObjectSchema(String eventObjectSchema) {
			this.eventObjectSchema = eventObjectSchema;
		}
		public String getEventObjectTable() {
			return eventObjectTable;
		}
		public void setEventObjectTable(String eventObjectTable) {
			this.eventObjectTable = eventObjectTable;
		}
		public String getActionStatement() {
			return actionStatement;
		}
		public void setActionStatement(String actionStatement) {
			this.actionStatement = actionStatement;
		}
		public String getActionTiming() {
			return actionTiming;
		}
		public void setActionTiming(String actionTiming) {
			this.actionTiming = actionTiming;
		}
		public String getSqlMode() {
			return sqlMode;
		}
		public void setSqlMode(String sqlMode) {
			this.sqlMode = sqlMode;
		}
		public String getDefiner() {
			return definer;
		}
		public void setDefiner(String definer) {
			this.definer = definer;
		}
	}
	
	/** Container class for per-owner (schema) data */
	public static class SchemaTO {
		DatabaseTO database;
		//private DataSource ds;
		//private JdbcTemplate jt;
		
		String name;
		public Map<String, TableTO> tables;
		public Map<String, TriggerTO> triggers;
		
		// not yet implemented
		//public List indexes;
		//public List views;
		//public List synonyms;
		//public List sequences;
		//public List clusters;
		//public UserTypeTO userType;
		//public SourceTypeTO sourceType;
		//public SecurityTO security;
		
		public SchemaTO(DatabaseTO database, String schemaName) {
			this.database = database;
			//this.ds = database.ds;
			//if (!database.dbType.equals(DatabaseType.JET_DAO)) {
			//	this.jt = new JdbcTemplate(ds);
			//}
			this.name = schemaName;
			tables = new HashMap<String, TableTO>();
		}
		
		public TableTO getTable(String tableName) {
			TableTO table = tables.get(database.upper(tableName));
			if (table == null) {
				throw new UnsupportedOperationException("use DAO to construct TableTOs");
				/*
				table = new TableTO(this, database.upper(tableName)); 
				tables.put(database.upper(tableName), table);
				*/ 
			}
			return table;
		}
		
		public String getName() { return name; }
		public DatabaseTO getDatabase() { return database; }
		public Map<String, TableTO> getTables() { return tables; }
		public Map<String, TriggerTO> getTriggers() { return triggers; }

		
	}
	
	/** Database metadata container. Will load reference data on demand */
	public DatabaseTO(DatabaseType dbType) {
		//this.ds = dataSource;
		//this.jt = new JdbcTemplate(dataSource);
		this.dbType = dbType;
		schemas = new HashMap<String, SchemaTO>();
	}
	
	/** Alternate constructor that uses ADO to interrogate a Jet (MSAccess)
	 * database. Way to go, Microsoft.
	 * 
	 * @param jetDatabaseLocation
	 */
	public DatabaseTO(String jetDatabaseFilename) {
		this.dbType = DatabaseType.JET_DAO;
		this.jetDatabaseFilename = jetDatabaseFilename;
		schemas = new HashMap<String, SchemaTO>();
	}
	
	public void setCaseInsensitive() {
		this.caseInsensitive = true;
	}
	
	public String upper(String s) {
		return caseInsensitive ? s.toUpperCase() : s;
	}
	
	public List<String> upper(List<String> l) {
		if (caseInsensitive) {
			List<String> r = new ArrayList<String>();
			for (String s : l) {
				r.add(s.toUpperCase());
			}
			return r;
		} else {
			return l;
		}
	}
	
	
	// @TODO pass in current schema/table for context resolution
	// probably need a set of tables here, throw exception if in >1
	public TableColumnTO getColumn(DatabaseTO d, SchemaTO defaultSchema, TableTO defaultTable, String identifier) {
		String schemaName = null;
		String tableName = null;
		String columnName = null;
		String[] bits = identifier.split("\\.");
		if (bits.length > 3) { throw new IllegalArgumentException("Too many components in column identifier '" + identifier + "'"); }
		if (bits.length < 1) { throw new IllegalArgumentException("Too few components in column identifier '" + identifier + "'"); }
		if (bits.length > 2) { schemaName = bits[bits.length - 3]; }
		if (bits.length > 1) { tableName = bits[bits.length - 2]; }
		columnName = bits[bits.length - 1];
		SchemaTO schema = d.getSchemas().get(schemaName); 
		TableTO table = schema.getTable(tableName);
		return table.getTableColumn(columnName);
	}
	
	public DatabaseType getDatabaseType() {
		return dbType;
	}
	public boolean isCaseInsensitive() {
		return caseInsensitive;
	}
	public Map<String, SchemaTO> getSchemas() {
		return schemas;
	}
    public int getMajorVersion() {
		return majorVersion;
	}

	public void setMajorVersion(int majorVersion) {
		this.majorVersion = majorVersion;
	}

	public int getMinorVersion() {
		return minorVersion;
	}

	public void setMinorVersion(int minorVersion) {
		this.minorVersion = minorVersion;
	}

	public String getProductName() {
		return productName;
	}

	public void setProductName(String productName) {
		this.productName = productName;
	}

	public String getProductVersion() {
		return productVersion;
	}

	public void setProductVersion(String productVersion) {
		this.productVersion = productVersion;
	}
	
}
