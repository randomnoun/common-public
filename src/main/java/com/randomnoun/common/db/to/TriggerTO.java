package com.randomnoun.common.db.to;


public class TriggerTO {
	private TableTO table; 
	
	private String name;
	private String eventManipulation; // INSERT/UPDATE/DELETE
	private long   actionOrder;          // always 0 
	//private String actionCondition;    // always null
	private String actionStatement;
	private String actionTiming; // BEFORE/AFTER
	private String sqlMode;
	private String definer;
	
	public TriggerTO(TableTO table, String name) {
		this.table = table;
		this.name = name;
	}

	public TableTO getTable() {
		return table;
	}

	public void setTable(TableTO table) {
		this.table = table;
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

	public long getActionOrder() {
		return actionOrder;
	}

	public void setActionOrder(long actionOrder) {
		this.actionOrder = actionOrder;
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