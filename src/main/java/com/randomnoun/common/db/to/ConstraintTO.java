package com.randomnoun.common.db.to;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.randomnoun.common.db.enums.ConstraintType;

public class ConstraintTO {
	String name;
	public TableTO table;
	ConstraintType type;
	public Map<String, ConstraintColumnTO> columns;
	
	public ConstraintTO(TableTO table, ConstraintType type, String name) {
		this.table = table;
		this.type = type;
		this.name = name;
		this.columns = new LinkedHashMap<String, ConstraintColumnTO>();
		// final ConstraintTO constraintRef = this;

		//DatabaseReader.logger.debug("Loading metadata for constraint '" + name + "'");
		//JdbcTemplate jt = new JdbcTemplate(table.schema.ds);
		
	}
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