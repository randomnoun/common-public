package com.randomnoun.common.db.to;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.randomnoun.common.db.enums.ConstraintTypeEnum;

public class ConstraintTO {
	
	private String name;
	private TableTO table;
	private ConstraintTypeEnum constraintType;
	private Map<String, ConstraintColumnTO> constraintColumnMap;
	
	public ConstraintTO(TableTO table, ConstraintTypeEnum constraintType, String name) {
		this.table = table;
		this.constraintType = constraintType;
		this.name = name;
		this.constraintColumnMap = new LinkedHashMap<String, ConstraintColumnTO>();
		// final ConstraintTO constraintRef = this;

		//DatabaseReader.logger.debug("Loading metadata for constraint '" + name + "'");
		//JdbcTemplate jt = new JdbcTemplate(table.schema.ds);
		
	}
	public TableTO getTable() { return table; }
	public String getName() { return name; }
	public ConstraintTypeEnum getConstraintType() { return constraintType; }
	
	public List<ConstraintColumnTO> getConstraintColumns() {
		return new ArrayList<ConstraintColumnTO>(constraintColumnMap.values());
	}
	
	public List<String> getConstraintColumnNames() {
		return new ArrayList<String>(constraintColumnMap.keySet());
	}
	
	public Map<String, ConstraintColumnTO> getConstraintColumnMap() {
		return constraintColumnMap;
	}
	public void setConstraintColumnMap(Map<String, ConstraintColumnTO> constraintColumnMap) {
		this.constraintColumnMap = constraintColumnMap;
	}
	public void setName(String name) {
		this.name = name;
	}
	public void setTable(TableTO table) {
		this.table = table;
	}
	public void setType(ConstraintTypeEnum type) {
		this.constraintType = type;
	}
}