package com.randomnoun.common.db.enums;

public enum DatabaseTypeEnum {
	/** A Jet (MSAccess) database accessed via the jdbc:odbc bridge */
	JET,
	/** A Jet (MSAccess) database accessed via the JACOB DAO library */
	JET_DAO,
	/** Oracle */
	ORACLE,
	/** MySQL */
	MYSQL,
	/** Microsoft SQL Server */
	SQLSERVER,
	/** Firebird */
	FIREBIRD,
	/** A generic database accessed via the JDBC metadata functions */
	GENERIC
	
}