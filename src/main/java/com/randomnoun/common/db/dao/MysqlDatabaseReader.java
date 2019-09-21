package com.randomnoun.common.db.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.springframework.jdbc.core.RowMapper;

import com.randomnoun.common.db.DatabaseReader;
import com.randomnoun.common.db.enums.ConstraintType;
import com.randomnoun.common.db.enums.DatabaseType;
import com.randomnoun.common.db.to.ConstraintColumnTO;
import com.randomnoun.common.db.to.ConstraintTO;
import com.randomnoun.common.db.to.MysqlTableColumnTO;
import com.randomnoun.common.db.to.SchemaTO;
import com.randomnoun.common.db.to.TableColumnTO;
import com.randomnoun.common.db.to.TableTO;
import com.randomnoun.common.db.to.TriggerTO;
import com.randomnoun.common.spring.StringRowMapper;

//all the stuff that's specific to mysql should go in here
public class MysqlDatabaseReader extends DatabaseReader {

	Logger logger = Logger.getLogger(MysqlDatabaseReader.class);
	
	public MysqlDatabaseReader(DataSource dataSource) {
		super(dataSource);
		this.db.dbType = DatabaseType.MYSQL;
	}

	@Override
	public SchemaTO readSchema(String schemaName) {
		SchemaTO schema = new SchemaTO(db, schemaName);
		// TODO Auto-generated method stub
		List<String> tableList = null;
		List<String> triggerList = null;
		
		tableList = jt.query(
			"SELECT TABLE_NAME "+
			" FROM INFORMATION_SCHEMA.TABLES " +
			" WHERE TABLE_SCHEMA='" + schema.name + "'",
			new StringRowMapper() );
		//tableList = schema.database.upper(tableList);
		for (String n : tableList) {
			TableTO t = readTable(schema, n);
			schema.tables.put(n, t);
			
		}

		triggerList = jt.query(
			"SELECT TRIGGER_NAME "+
			" FROM INFORMATION_SCHEMA.TRIGGERS " +
			" WHERE TRIGGER_SCHEMA='" + schema.name + "'",  // c.f. EVENT_OBJECT_TRIGGER
			new StringRowMapper() );
		triggerList = schema.database.upper(triggerList);
		for (String n : triggerList) {
			TriggerTO t = readTrigger(schema, n);
			schema.triggers.put(n, t);
		}
		
		return schema;
	}
	
	private TriggerTO readTrigger(SchemaTO schema, String triggerName) {
		final TriggerTO t = new TriggerTO(schema, triggerName);
		return t;
	}

	// probably more efficient to read all tables at once, but this is what the current code does
	@SuppressWarnings("unchecked")
	private TableTO readTable(SchemaTO s, String tableName) {
		final TableTO t = new TableTO(s, tableName);
		List<? extends TableColumnTO> columnList = null;
		List<String> constraintNames = null;
		
		columnList = jt.query(
			// TABLE_CATALOG always NULL, TABLE_SCHEMA
			"SELECT TABLE_NAME, COLUMN_NAME, ORDINAL_POSITION, COLUMN_DEFAULT, IS_NULLABLE, "+
			"  DATA_TYPE, CHARACTER_MAXIMUM_LENGTH, CHARACTER_OCTET_LENGTH, " +
			"  NUMERIC_PRECISION, NUMERIC_SCALE, CHARACTER_SET_NAME, " +
			"  COLLATION_NAME, COLUMN_TYPE, COLUMN_KEY, EXTRA, PRIVILEGES, " +
			"  COLUMN_COMMENT "+
			" FROM INFORMATION_SCHEMA.COLUMNS " +
			" WHERE " +
			" TABLE_SCHEMA = '" + t.schema.name + "' AND " +
			(t.schema.database.caseInsensitive ? 
				" UPPER(TABLE_NAME) = '" + t.name + "' " :
				" TABLE_NAME = '" + t.name + "' ") +		
			"	ORDER BY TABLE_NAME, ORDINAL_POSITION ", 
			new RowMapper<MysqlTableColumnTO>() {
				public MysqlTableColumnTO mapRow(ResultSet rs, int rowCount) throws SQLException {
					MysqlTableColumnTO mtc = new MysqlTableColumnTO(t, 
						t.schema.database.upper(rs.getString("COLUMN_NAME")),
						rs.getLong("ORDINAL_POSITION"),
						false, // name == "id" ?
						rs.getString("DATA_TYPE"),
						rs.getObject("CHARACTER_MAXIMUM_LENGTH") == null ? -1 : rs.getLong("CHARACTER_MAXIMUM_LENGTH"),
						rs.getObject("NUMERIC_PRECISION") == null ? -1 : rs.getLong("NUMERIC_PRECISION"),
						rs.getObject("NUMERIC_SCALE") == null ? -1 : rs.getLong("NUMERIC_SCALE"),
						rs.getString("IS_NULLABLE").startsWith("Y") ? true : false,
						rs.getString("COLUMN_DEFAULT"),
						rs.getString("COLUMN_COMMENT"));
					// String ct = rs.getString("COLUMN_TYPE");
					// logger.info("type for " + rs.getString("COLUMN_NAME") + " was " + ct);
					// e.g. "int(10) unsigned", "enum('value1','value2','value3')"					
					mtc.setColumnType(rs.getString("COLUMN_TYPE"));
					return mtc;
				}
			});
		for (TableColumnTO c : columnList) {
			t.columns.put(c.getName(), c);
		}
	
		constraintNames = jt.query(
			"SELECT CONSTRAINT_NAME "+
			" FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS " +
			" WHERE TABLE_SCHEMA='" + t.schema.name + "'" +
			" AND TABLE_NAME='" + t.name + "'",
			new StringRowMapper() );
		//constraintList = t.schema.database.upper(constraintList);
		for (String c : constraintNames) {
			ConstraintTO constraint = getConstraint(t, c);
			t.constraints.put(c, constraint);
			if (constraint.getType() == ConstraintType.PRIMARY) {
				for (String cn : constraint.getConstraintColumnNames()) {
					t.columns.get(cn).setPrimaryKey(true);
				}
			}
		}
		return t;
	}
	
	@SuppressWarnings("unchecked")
	public ConstraintTO getConstraint(TableTO t, String constraintName) {
		//ConstraintTO constraint = constraints.get(schema.database.upper(constraintName));
		//if (constraint == null) {
			
			String typeString = (String) jt.queryForObject(
				"SELECT CONSTRAINT_TYPE "+
				" FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS " +
				" WHERE TABLE_SCHEMA='" + t.schema.name + "' " +
				" AND TABLE_NAME='" + t.name + "' " +
				" AND CONSTRAINT_NAME='" + constraintName + "'", String.class);
			ConstraintType type = ConstraintType.fromDatabaseString(typeString);
			final ConstraintTO constraint = new ConstraintTO(t, type, t.schema.database.upper(constraintName));
			
			/* so this used to work */
			List<ConstraintColumnTO> columnList = jt.query(
				"SELECT constraint_catalog, constraint_schema, constraint_name, " +
				"  table_catalog, table_schema, table_name, " + 
				"  column_name, ordinal_position, " + // position_in_unique_constraint,
				// referenced_table_schema
				"  referenced_table_name, referenced_column_name " + 
			    " FROM information_schema.key_column_usage" +
			    " WHERE constraint_name = '" + constraintName + "' " + // and catalog and schema
			    " AND table_schema = '" + t.schema.name + "' " + 
			    " AND table_name = '" + t.getName() + "' " +
			    " ORDER BY ordinal_position ",
				new RowMapper<ConstraintColumnTO>() {
					public ConstraintColumnTO mapRow(ResultSet rs, int rowCount) throws SQLException {
						return new ConstraintColumnTO(constraint, 
								constraint.table.schema.database.upper(rs.getString("COLUMN_NAME")),
							rs.getLong("ORDINAL_POSITION"),
							constraint.table.schema.database.upper(rs.getString("REFERENCED_TABLE_NAME")),
							constraint.table.schema.database.upper(rs.getString("REFERENCED_COLUMN_NAME"))
						);
					}
				});
			if (columnList.size() == 0 && type != ConstraintType.CHECK) {
				throw new IllegalStateException("No column metadata found for table '" + t.name + "' constraint '" + constraintName + "'");
			}
			for (Iterator i = columnList.iterator(); i.hasNext(); ) {
				ConstraintColumnTO column = (ConstraintColumnTO) i.next();
				constraint.columns.put(column.name, column);
			}
			

			
			
			return constraint;
			// constraints.put(schema.database.upper(constraintName), constraint); 
		//}
		//return constraint;
	}
	
	
	
	
	

}
