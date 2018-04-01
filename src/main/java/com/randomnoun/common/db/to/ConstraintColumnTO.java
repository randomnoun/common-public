package com.randomnoun.common.db.to;

public class ConstraintColumnTO {
	ConstraintTO constraint;
	public String name;
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