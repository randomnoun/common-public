package com.randomnoun.common.db.enums;

public enum ConstraintTypeEnum {
	PRIMARY, FOREIGN, CHECK, UNIQUE;
	
	public static ConstraintTypeEnum fromDatabaseString(String s) {
		if (s.equals("PRIMARY KEY")) {
			return PRIMARY;
		} else if (s.equals("FOREIGN KEY")) {
			return FOREIGN;
		} else if (s.equals("CHECK")) {
			return CHECK; 
		} else if (s.equals("UNIQUE")) {
			return UNIQUE;
		} else {
			throw new IllegalArgumentException("No enum const for database string '" + s + "'");
		}
	}
}