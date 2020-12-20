package com.randomnoun.common.db.to;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TableTO {
	private String name;
	private SchemaTO schema;
	private Map<String, TableColumnTO> tableColumnMap;
	private Map<String, ConstraintTO> constraintMap;
	private Map<String, TriggerTO> triggerMap;

	public TableTO(SchemaTO schema, String tableName) {
		this.schema = schema;
		this.name = tableName;
		this.tableColumnMap = new LinkedHashMap<String, TableColumnTO>();
		this.constraintMap = new LinkedHashMap<String, ConstraintTO>();
		this.triggerMap = new LinkedHashMap<String, TriggerTO>();

	}
	
	public TableColumnTO getTableColumn(String name) {
		TableColumnTO column = tableColumnMap.get(name);
		if (column==null) { throw new IllegalArgumentException("Column '" + name + "' not found"); }
		return column;
	};
	
	public List<TableColumnTO> getTableColumns() {
		return new ArrayList<TableColumnTO>(tableColumnMap.values());
	}
	
	public List<String> getTableColumnNames() {
		return new ArrayList<String>(tableColumnMap.keySet());
	}
	public String getName() { return name; }
	public SchemaTO getSchema() { return schema; }

	public Map<String, TableColumnTO> getTableColumnMap() {
		return tableColumnMap;
	}

	public void setTableColumnMap(Map<String, TableColumnTO> tableColumnMap) {
		this.tableColumnMap = tableColumnMap;
	}

	public Map<String, ConstraintTO> getConstraintMap() {
		return constraintMap;
	}

	public void setConstraintMap(Map<String, ConstraintTO> constraintMap) {
		this.constraintMap = constraintMap;
	}

	public Map<String, TriggerTO> getTriggerMap() {
		return triggerMap;
	}

	public void setTriggerMap(Map<String, TriggerTO> triggerMap) {
		this.triggerMap = triggerMap;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setSchema(SchemaTO schema) {
		this.schema = schema;
	}
	
}