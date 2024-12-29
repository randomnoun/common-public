package com.randomnoun.common.db.to;

import org.apache.log4j.Logger;

/** Holds type information for a table column. 
 */
public class TableColumnTO {
	
	Logger logger = Logger.getLogger(TableColumnTO.class);
	
	private TableTO table;
	private String name;
	private long columnId;
	private boolean isPrimaryKey;
	private String dataType;
	private long dataTypeLength;
	private long dataTypePrecision;
	private long dataScale;
	private boolean nullable;
	private String defaultValue;
	private String comments;
	
	public TableColumnTO(TableTO table, String name, long columnId, boolean isPrimaryKey, String dataType,
		long dataTypeLength, long dataTypePrecision, long dataScale, 
		boolean nullable, String defaultValue, String comments) 
	{
		logger.debug("Datatype for '" + table.getName() + "." + name + "' is " + (isPrimaryKey ? "PK " : "") + dataType + " (" + dataTypeLength + ", " + dataTypePrecision + ", " + dataScale + ")");  
		this.table = table;
		this.name = name;
		this.columnId = columnId;
		this.isPrimaryKey = isPrimaryKey;
		this.dataType = dataType;
		this.dataTypeLength = dataTypeLength;
		this.dataTypePrecision = dataTypePrecision;
		this.dataScale = dataScale;
		this.nullable = nullable;
		this.defaultValue = defaultValue;
		this.comments = comments;
	}
	public TableTO getTable() { return table; }
	public String getName() { return name; }
	public long   getColumnId() { return columnId; }
	public boolean isPrimaryKey() { return isPrimaryKey; }
	public void setPrimaryKey(boolean b) {
		this.isPrimaryKey = b;
	}
	public String getDataType() { return dataType; }
	public long   getDataTypeLength() { return dataTypeLength; }
	public long   getDataTypePrecision() { return dataTypePrecision; }
	public long   getDataScale() { return dataScale; }
	public boolean getNullable() { return nullable; }
	public String getDefaultValue() { return defaultValue; }
	public String getComments() { return comments; }
	
	
	public String getTypeString() {
		if (dataScale!=-1 && dataTypePrecision==-1) {
			throw new IllegalStateException("datatype scale set without having precision set");
		}
		if (dataTypeLength!=-1 && dataTypePrecision!=-1) {
			throw new IllegalStateException("both datatype length and precision set");
		}
		if (dataTypeLength!=-1) {
			return dataType + "(" + dataTypeLength + ")";
		} else if (dataTypePrecision!=-1) {
			if (dataScale!=-1) {
				// @TODO is this the wrong way round ?
				return dataType + "(" + dataTypePrecision + "," + dataScale + ")";
			} else {
				return dataType + "(" + dataTypePrecision + ")";
			}
		} else {
			return dataType;
		}
	}
	
	
	
	
}