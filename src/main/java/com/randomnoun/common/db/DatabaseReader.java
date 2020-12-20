package com.randomnoun.common.db;

/* (c) 2013 randomnoun. All Rights Reserved. This work is licensed under a
 * BSD Simplified License. (http://www.randomnoun.com/bsd-simplified.html)
 */

import java.util.HashMap;

import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;

import com.randomnoun.common.db.to.SchemaTO;
import com.randomnoun.common.db.to.TableColumnTO;
import com.randomnoun.common.db.to.TableTO;

/** Container class for database metadata.
 * 
 * <p>Rewrite of DatabaseTO to be marginally more structured
 * 
 * <p>Contains methods for populating metadata from the data dictionaries of
 * Oracle and MySQL, for the purposes of whatever it is that I'm doing at the time.
 *
 *  
 *  NB: does not set default values for oracle
 *  
 */
public abstract class DatabaseReader {

	protected com.randomnoun.common.db.to.DatabaseTO db;
    
	// only required for online DB metadata
	public DataSource ds;
	public JdbcTemplate jt;
	
	/* these aren't used yet
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
	*/
	
	/** Database metadata container. Will load reference data on demand */
	// @TODO look up correct Reader instance and use that 
	public DatabaseReader(DataSource dataSource /*, DatabaseType dbType*/) {
		this.ds = dataSource;
		this.jt = new JdbcTemplate(dataSource);
		this.db = new com.randomnoun.common.db.to.DatabaseTO();
		db.setDatabaseType(null);
		db.setSchemaMap(new HashMap<String, SchemaTO>());
	}
	
	
	// this only works for MYSQL. so that's odd.
	public SchemaTO getSchema(String schemaName) {
		
		// default schema name
		if (schemaName==null) {
			switch (db.getDatabaseType()) {
				case ORACLE:
					throw new UnsupportedOperationException("Not supported for this database type");
				
				case MYSQL:
					schemaName = (String) jt.queryForObject("SELECT DATABASE();", java.lang.String.class);
					break;
			
				case SQLSERVER:
					throw new UnsupportedOperationException("Not supported for this database type");
					
				default:
					throw new IllegalStateException("Unknown database type " + db.getDatabaseType());
			}
		}
		
		SchemaTO schema = db.getSchemaMap().get(db.upper(schemaName));
		if (schema == null) {
			schema = readSchema(db.upper(schemaName));
			db.getSchemaMap().put(db.upper(schemaName), schema); 
		}
		return schema;
	}
	
	abstract public SchemaTO readSchema(String schemaName);  // SchemaTO schema
	
	
	// @TODO pass in current schema/table for context resolution
	public TableColumnTO getColumn(com.randomnoun.common.db.to.DatabaseTO db, String identifier) {
		String schemaName = null;
		String tableName = null;
		String columnName = null;
		String[] bits = identifier.split("\\.");
		if (bits.length > 3) { throw new IllegalArgumentException("Too many components in column identifier '" + identifier + "'"); }
		if (bits.length < 1) { throw new IllegalArgumentException("Too few components in column identifier '" + identifier + "'"); }
		if (bits.length > 2) { schemaName = bits[bits.length - 3]; }
		if (bits.length > 1) { tableName = bits[bits.length - 2]; }
		columnName = bits[bits.length - 1];
		SchemaTO schema = db.getSchemaMap().get(schemaName); 
		TableTO table = schema.getTable(tableName);
		return table.getTableColumn(columnName);
	}
	
}
