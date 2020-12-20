package com.randomnoun.common.db.to;

public class ConstraintColumnTO {
	
	private ConstraintTO constraint;
	private String name;
	private String refTableName;
	private String refColumnName;
	private long position; // ordinal position
	
	public ConstraintColumnTO(ConstraintTO constraint, String name, long columnId) {
		this.constraint = constraint;
		this.name = name;
		this.position = columnId;
	}
	public ConstraintColumnTO(ConstraintTO constraint, String name, long columnId, String refTableName, String refColumnName) {
		this.constraint = constraint;
		this.name = name;
		this.position = columnId;
		this.refTableName = refTableName;
		this.refColumnName = refColumnName;
	}
	public ConstraintTO getConstraint() { return constraint; }
	public String getName() { return name; }
	public long getPosition() { return position; }

	public TableColumnTO getTableColumn() {
		return constraint.getTable().getTableColumn(name);
	}
	public String getRefTableName() {
		return refTableName;
	}
	public void setRefTableName(String refTableName) {
		this.refTableName = refTableName;
	}
	public String getRefColumnName() {
		return refColumnName;
	}
	public void setRefColumnName(String refColumnName) {
		this.refColumnName = refColumnName;
	}
	public void setConstraint(ConstraintTO constraint) {
		this.constraint = constraint;
	}
	public void setName(String name) {
		this.name = name;
	}
	public void setPosition(long columnId) {
		this.position = columnId;
	}

}