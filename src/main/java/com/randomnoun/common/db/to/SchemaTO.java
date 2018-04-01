package com.randomnoun.common.db.to;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Container class for per-owner (schema) data */
public class SchemaTO {
	public DatabaseTO database;
	
	public String name;
	public Map<String, TableTO> tables;
	public Map<String, TriggerTO> triggers;
	
	/*
	public List indexes;
	public List views;
	public List synonyms;
	public List sequences;
	public List clusters;
	//public UserTypeTO userType;
	//public SourceTypeTO sourceType;
	//public SecurityTO security;
	 */
	
	public SchemaTO(DatabaseTO database, String schemaName) {
		this.database = database;
		this.name = schemaName;
		tables = new HashMap<String, TableTO>();
		triggers = new HashMap<String, TriggerTO>();
	}
	
	public TableTO getTable(String tableName) {
		TableTO table = tables.get(database.upper(tableName));
		if (table == null) { 
			table = new TableTO(this, database.upper(tableName)); 
			tables.put(database.upper(tableName), table); 
		}
		return table;
	}
	
	public String getName() { return name; }
	public DatabaseTO getDatabase() { return database; }

	public List<String> getTableNames() {
		return new ArrayList<String>(tables.keySet());
	}

	public List<String> getTriggerNames() {
		return new ArrayList<String>(triggers.keySet());
	}
}