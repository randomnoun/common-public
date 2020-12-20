package com.randomnoun.common.db.to;

public class RoutineParameterTO {

	RoutineTO routine;
	String name;
	long position;
	String mode; // IN, OUT, INOUT
	
	// this should all go into a DataTypeTO probably
	
	private String dataType;  // function only
	private Long dataTypeLength; // character length
	// private Long dataTypeByteLength;
	private Long dataTypeNumericPrecision;
	private Long dataTypeNumericScale;
	private Long dataTypeDateTimePrecision;
	private String charsetName;
	private String collationName;

	
	
	public RoutineTO getRoutine() {
		return routine;
	}
	public void setRoutine(RoutineTO routine) {
		this.routine = routine;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public long getPosition() {
		return position;
	}
	public void setPosition(long position) {
		this.position = position;
	}
	public String getMode() {
		return mode;
	}
	public void setMode(String mode) {
		this.mode = mode;
	}
	public String getDataType() {
		return dataType;
	}
	public void setDataType(String dataType) {
		this.dataType = dataType;
	}
	public Long getDataTypeLength() {
		return dataTypeLength;
	}
	public void setDataTypeLength(Long dataTypeLength) {
		this.dataTypeLength = dataTypeLength;
	}
	public Long getDataTypeNumericPrecision() {
		return dataTypeNumericPrecision;
	}
	public void setDataTypeNumericPrecision(Long dataTypeNumericPrecision) {
		this.dataTypeNumericPrecision = dataTypeNumericPrecision;
	}
	public Long getDataTypeNumericScale() {
		return dataTypeNumericScale;
	}
	public void setDataTypeNumericScale(Long dataTypeNumericScale) {
		this.dataTypeNumericScale = dataTypeNumericScale;
	}
	public Long getDataTypeDateTimePrecision() {
		return dataTypeDateTimePrecision;
	}
	public void setDataTypeDateTimePrecision(Long dataTypeDateTimePrecision) {
		this.dataTypeDateTimePrecision = dataTypeDateTimePrecision;
	}
	public String getCharsetName() {
		return charsetName;
	}
	public void setCharsetName(String charsetName) {
		this.charsetName = charsetName;
	}
	public String getCollationName() {
		return collationName;
	}
	public void setCollationName(String collationName) {
		this.collationName = collationName;
	}
	
	
}
