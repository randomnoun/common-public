package com.randomnoun.common.db.to;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Container class for per-owner (schema) data */
public class SchemaTO {
	private DatabaseTO database;
	
	private String name;
	private Map<String, TableTO> tableMap;
	private Map<String, TriggerTO> triggerMap;
	
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
		tableMap = new HashMap<String, TableTO>();
		triggerMap = new HashMap<String, TriggerTO>();
	}
	
	public TableTO getTable(String tableName) {
		TableTO table = tableMap.get(database.upper(tableName));
		if (table == null) { 
			table = new TableTO(this, database.upper(tableName)); 
			tableMap.put(database.upper(tableName), table); 
		}
		return table;
	}
	
	public String getName() { return name; }
	public DatabaseTO getDatabase() { return database; }

	public List<String> getTableNames() {
		return new ArrayList<String>(tableMap.keySet());
	}

	public List<String> getTriggerNames() {
		return new ArrayList<String>(triggerMap.keySet());
	}

	public Map<String, TableTO> getTableMap() {
		return tableMap;
	}

	public void setTableMap(Map<String, TableTO> tableMap) {
		this.tableMap = tableMap;
	}

	public Map<String, TriggerTO> getTriggerMap() {
		return triggerMap;
	}

	public void setTriggerMap(Map<String, TriggerTO> triggerMap) {
		this.triggerMap = triggerMap;
	}

	public void setDatabase(DatabaseTO database) {
		this.database = database;
	}

	public void setName(String name) {
		this.name = name;
	}
}