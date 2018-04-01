package com.randomnoun.common.db.to;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TableTO {
	public String name;
	public SchemaTO schema;
	public Map<String, TableColumnTO> columns;
	public Map<String, ConstraintTO> constraints;

	public TableTO(SchemaTO schema, String tableName) {
		this.schema = schema;
		this.name = tableName;
		this.columns = new LinkedHashMap<String, TableColumnTO>();
		this.constraints = new LinkedHashMap<String, ConstraintTO>();

	}
	
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
	
}