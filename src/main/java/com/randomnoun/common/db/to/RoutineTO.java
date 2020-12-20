package com.randomnoun.common.db.to;

import java.util.Map;

public class RoutineTO {
	
	private SchemaTO schema;
	private String name;
	private String type; // PROCEDURE, FUNCTION
	private String dataType;  // function only
	private Long dataTypeLength; // character length
	// private Long dataTypeByteLength;
	private Long dataTypeNumericPrecision;
	private Long dataTypeNumericScale;
	private Long dataTypeDateTimePrecision;
	private String charsetName;
	private String collationName;
	
	// private String dtdIdentifier; // functions only, is the dataType + extra bits
	// private String bodyLanguage; // always 'SQL'
	private String definer;
	
	private String comment;
	private String definition;
	private boolean deterministic;
	private String dataAccess;  //  The value is one of CONTAINS SQL, NO SQL, READS SQL DATA, or MODIFIES SQL DATA.
	private String securityType; // The routine SQL SECURITY characteristic. The value is one of DEFINER or INVOKER.
	
	private String sqlMode; // eg IGNORE_SPACE,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION
	
	private Map<String, RoutineParameterTO> routineParameterMap;
	
	public RoutineTO(SchemaTO schema, String name) {
		this.schema = schema;
		this.name = name;
	}

	public SchemaTO getSchema() {
		return schema;
	}

	public void setSchema(SchemaTO schema) {
		this.schema = schema;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getDataType() {
		return dataType;
	}

	public void setDataType(String dataType) {
		this.dataType = dataType;
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

	public String getDefiner() {
		return definer;
	}

	public void setDefiner(String definer) {
		this.definer = definer;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public String getDefinition() {
		return definition;
	}

	public void setDefinition(String definition) {
		this.definition = definition;
	}

	public boolean isDeterministic() {
		return deterministic;
	}

	public void setDeterministic(boolean deterministic) {
		this.deterministic = deterministic;
	}

	public String getDataAccess() {
		return dataAccess;
	}

	public void setDataAccess(String dataAccess) {
		this.dataAccess = dataAccess;
	}

	public String getSecurityType() {
		return securityType;
	}

	public void setSecurityType(String securityType) {
		this.securityType = securityType;
	}

	public String getSqlMode() {
		return sqlMode;
	}

	public void setSqlMode(String sqlMode) {
		this.sqlMode = sqlMode;
	}

	public Map<String, RoutineParameterTO> getRoutineParameterMap() {
		return routineParameterMap;
	}

	public void setRoutineParameterMap(Map<String, RoutineParameterTO> routineParameterMap) {
		this.routineParameterMap = routineParameterMap;
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

}