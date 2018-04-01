package com.randomnoun.common.db.to;


// @TODO enum this
public class TriggerTO {
	SchemaTO schema; // this is how mysql stores them. Might make more sense
	               // to make this a table reference
	String name;
	String eventManipulation; // INSERT/UPDATE/DELETE
	// String eventObjectCatalog; // NULL in mysql
	String eventObjectSchema;  // is this ever != schema.name ?
	String eventObjectTable;
	//long actionOrder;          // always 0 
	//String actionCondition;    // always null
	String actionStatement;
	String actionTiming; // BEFORE/AFTER
	String sqlMode;
	String definer;
	
	public TriggerTO(SchemaTO schema, String name) {
		this.schema = schema;
		this.name = name;
	}
}