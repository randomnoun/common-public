package com.randomnoun.common.db.to;

import org.apache.log4j.Logger;

/** Holds type information for a table column. 
 */
public class MysqlTableColumnTO extends TableColumnTO {
	
	Logger logger = Logger.getLogger(MysqlTableColumnTO.class);

	private String columnType;
	
	public MysqlTableColumnTO(TableTO table, String name, long columnId, boolean isPrimaryKey, String dataType,
		long dataTypeLength, long dataTypePrecision, long dataScale, 
		boolean nullable, String defaultValue, String comments) 
	{
		super(table, name, columnId, isPrimaryKey, dataType, dataTypeLength, dataTypePrecision, dataScale,
				nullable, defaultValue, comments);
	}

	public String getColumnType() {
		return columnType;
	}

	public void setColumnType(String columnType) {
		this.columnType = columnType;
	}
	
	
}