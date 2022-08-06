package com.randomnoun.common.db.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;

import com.randomnoun.common.db.DatabaseReader;
import com.randomnoun.common.db.enums.ConstraintTypeEnum;
import com.randomnoun.common.db.enums.DatabaseTypeEnum;
import com.randomnoun.common.db.to.ConstraintColumnTO;
import com.randomnoun.common.db.to.ConstraintTO;
import com.randomnoun.common.db.to.MysqlTableColumnTO;
import com.randomnoun.common.db.to.RoutineTO;
import com.randomnoun.common.db.to.SchemaTO;
import com.randomnoun.common.db.to.TableTO;
import com.randomnoun.common.db.to.TriggerTO;
import com.randomnoun.common.spring.StringRowMapper;

//all the stuff that's specific to mysql should go in here
public class MysqlDatabaseReader extends DatabaseReader {

	Logger logger = Logger.getLogger(MysqlDatabaseReader.class);
	
	public MysqlDatabaseReader(DataSource dataSource) {
		super(dataSource);
		this.db.setDbType(DatabaseTypeEnum.MYSQL);
	}

	@Override
	public SchemaTO readSchema(String schemaName) {
		SchemaTO schema = new SchemaTO(db, schemaName);
		// TODO Auto-generated method stub
		List<String> tableList = null;
		
		// this became outrageously slow in mysql 8
		tableList = jt.query(
			"SELECT table_name "+
			" FROM information_schema.tables " +
			" WHERE table_schema='" + schema.getName() + "'",
			new StringRowMapper() );
		//tableList = schema.database.upper(tableList);
		for (String n : tableList) {
			logger.debug("Table " + n);
			// TableTO t = readTable(schema, n);
			TableTO t = new TableTO(schema, n);
			schema.getTableMap().put(n, t);
		}
		
		readTables(schema);
		readTriggers(schema);
		readRoutines(schema);
		return schema;
	}
	
	private void readTriggers(final SchemaTO s) {
		// final TriggerTO t = new TriggerTO(schema, triggerName);
		// return t;
		
		jt.query(
			"SELECT T.trigger_catalog, T.trigger_schema, T.trigger_name,\n" + 
			" T.event_manipulation,\n" + 
			" T.event_object_catalog, T.event_object_schema, T.event_object_table,\n" + 
			" T.action_order,\n" + 
			" T.action_condition,\n" + // null 
			" T.action_statement,\n" + 
			" T.action_orientation,\n" + // ROW 
			" T.action_timing,\n" + // AFTER 
			" T.action_reference_old_table, T.action_reference_new_table,\n" + // null, null 
			" T.action_reference_old_row, T.action_reference_new_row,\n" + // OLD, NEW
			" T.created,\n" + 
			" T.sql_mode,\n" + // IGNORE_SPACE,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION 
			" T.definer,\n" + // root@localhost
			" T.character_set_client,\n" + // utf8mb4 
			" T.collation_connection,\n" + // utf8mb4_unicode_ci 
			" T.database_collation  \n" + // utf8mb4_0900_ai_ci 
			"FROM INFORMATION_SCHEMA.TRIGGERS T\n" +
			"WHERE T.trigger_schema = '" + s.getName() + "'\n" + // TODO escape 
			" ORDER BY T.event_object_schema, T.event_object_table, T.event_manipulation, T.action_order",
			new ResultSetExtractor<Object>() {
				@Override
				public Object extractData(ResultSet rs) throws SQLException, DataAccessException {
					TableTO t = null;
					String lastTableName = null;
					
					while (rs.next()) {
						String tableName = rs.getString("event_object_table");
						if (t == null || !tableName.equals(lastTableName)) {
							t = s.getTable(tableName);
							lastTableName = tableName;
						}
						
						String triggerName = rs.getString("trigger_name");
						TriggerTO trigger = new TriggerTO(t, t.getSchema().getDatabase().upper(triggerName));
						trigger.setEventManipulation(rs.getString("event_manipulation"));
						trigger.setActionOrder(rs.getLong("action_order"));
						trigger.setActionStatement(rs.getString("action_statement"));
						trigger.setActionTiming(rs.getString("action_timing"));
						trigger.setSqlMode(rs.getString("sql_mode"));
						trigger.setDefiner(rs.getString("definer"));
						s.getTriggerMap().put(trigger.getName(), trigger); // needs to be unique across the schema I guess.
					}
					return null;
				}
		});
	}

	// probably more efficient to read all tables at once, but this is what the current code does
	private void readTables(final SchemaTO s) {
		jt.query(
			// TABLE_CATALOG always NULL, TABLE_SCHEMA
			"SELECT TABLE_NAME, COLUMN_NAME, ORDINAL_POSITION, COLUMN_DEFAULT, IS_NULLABLE, "+
			"  DATA_TYPE, CHARACTER_MAXIMUM_LENGTH, CHARACTER_OCTET_LENGTH, " +
			"  NUMERIC_PRECISION, NUMERIC_SCALE, CHARACTER_SET_NAME, " +
			"  COLLATION_NAME, COLUMN_TYPE, COLUMN_KEY, EXTRA, PRIVILEGES, " +
			"  COLUMN_COMMENT "+
			" FROM INFORMATION_SCHEMA.COLUMNS " +
			" WHERE " +
			" TABLE_SCHEMA = '" + s.getName() + "' " + // @TODO escape 
			"	ORDER BY TABLE_NAME, ORDINAL_POSITION ", 
			new ResultSetExtractor<Object>() {
				@Override
				public Object extractData(ResultSet rs) throws SQLException, DataAccessException {
					TableTO t = null;
					String lastTableName = null;
					while (rs.next()) {
						String tableName = rs.getString("TABLE_NAME");
						if (t == null || !tableName.equals(lastTableName)) {
							t = s.getTable(tableName);
							lastTableName = tableName;
						}
						MysqlTableColumnTO mtc = new MysqlTableColumnTO(t, 
							t.getSchema().getDatabase().upper(rs.getString("COLUMN_NAME")),
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
						t.getTableColumnMap().put(mtc.getName(), mtc);
					}
					return null;
				}
			});
		
		jt.query(
			"SELECT TC.constraint_catalog, TC.constraint_schema, TC.constraint_name,\n" + 
			" TC.table_schema, TC.table_name,\n" + 
			" TC.constraint_type, \n" + // TC.enforced in mysql 8 
			" KCU.column_name, KCU.ordinal_position, \n" + // -- KCU.position_in_unique_constraint = null 
			" KCU.referenced_table_schema, KCU.referenced_table_name, KCU.referenced_column_name\n" + 
			"FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS TC\n" + 
			" LEFT JOIN information_schema.KEY_COLUMN_USAGE KCU\n" + 
			" ON (TC.constraint_catalog = KCU.constraint_catalog\n" + 
			" AND TC.constraint_schema = KCU.constraint_schema\n" + 
			" AND TC.constraint_name = KCU.constraint_name\n" + 
			" AND TC.table_schema = KCU.table_schema\n" + 
			" AND TC.table_name = KCU.table_name)\n" + 
			"WHERE TC.table_schema = '" + s.getName() + "'\n" + // @TODO escape 
			" ORDER BY TC.table_schema, TC.table_name, TC.constraint_name, KCU.ordinal_position;\n",
			new ResultSetExtractor<Object>() {
				@Override
				public Object extractData(ResultSet rs) throws SQLException, DataAccessException {
					TableTO t = null;
					ConstraintTO c = null;
					String lastTableName = null;
					String lastConstraintName = null;
					
					while (rs.next()) {
						String tableName = s.getDatabase().upper(rs.getString("TABLE_NAME"));
						if (t == null || !tableName.equals(lastTableName)) {
							t = s.getTable(tableName);
							lastTableName = tableName;
							lastConstraintName = null;
							c = null;
						}
						
						String constraintName = s.getDatabase().upper(rs.getString("CONSTRAINT_NAME"));
						if (c == null || !constraintName.equals(lastConstraintName)) {
							String typeString = rs.getString("CONSTRAINT_TYPE");
							ConstraintTypeEnum type = ConstraintTypeEnum.fromDatabaseString(typeString);
							c = new ConstraintTO(t, type, t.getSchema().getDatabase().upper(constraintName));
							t.getConstraintMap().put(constraintName, c);
							lastConstraintName = constraintName;
						}

						ConstraintColumnTO cc = new ConstraintColumnTO(c,
							c.getTable().getSchema().getDatabase().upper(rs.getString("COLUMN_NAME")),
							rs.getLong("ORDINAL_POSITION"),
							c.getTable().getSchema().getDatabase().upper(rs.getString("REFERENCED_TABLE_NAME")),
							c.getTable().getSchema().getDatabase().upper(rs.getString("REFERENCED_COLUMN_NAME")));
						
						c.getConstraintColumnMap().put(cc.getName(), cc);
						if (c.getConstraintType() == ConstraintTypeEnum.PRIMARY) {
							if (t.getTableColumnMap().get(cc.getName()) == null) {
								logger.error("Could not find column '" + cc.getName() + "' in table '" + t.getName() + "', tableName='" + tableName + "', constraintName = '" + constraintName + "'");
							} else {
								t.getTableColumnMap().get(cc.getName()).setPrimaryKey(true);
							}
						}
					}
					return null;
				}
			});
	}
	
	// probably more efficient to read all tables at once, but this is what the current code does
	private void readRoutines(final SchemaTO s) {
		jt.query(
			// TABLE_CATALOG always NULL, TABLE_SCHEMA
			"SELECT routine_catalog, routine_schema, routine_name, routine_type, "+
			"  data_type, character_maximum_length, character_octet_length, " +
			"  numeric_precision, numeric_scale, character_set_name, " +
			"  collation_name, " + // dtd_identifier, routine_body
			"  routine_definition,  " + // external_language, parameter_style 
			"  is_deterministic, sql_data_access, sql_path, " +
			"  security_type, sql_mode, routine_comment, " + // created, last_altered
			"  definer " + // , character_set_client, collation_connection, database_collation
			" FROM INFORMATION_SCHEMA.routines " +
			" WHERE " +
			" routine_schema = '" + s.getName() + "' " + // @TODO escape 
			"	ORDER BY routine_name ", 
			new ResultSetExtractor<Object>() {
				@Override
				public Object extractData(ResultSet rs) throws SQLException, DataAccessException {
					while (rs.next()) {
						String routineName = rs.getString("routine_name");
						RoutineTO r = new RoutineTO(s, routineName);
						r.setType(rs.getString("routine_type"));
						r.setDataType("data_type");
						r.setDataTypeLength(rs.getLong("character_maximum_length")); if (rs.wasNull()) { r.setDataTypeLength(null); }
						r.setDataTypeNumericPrecision(rs.getLong("numeric_precision")); if (rs.wasNull()) { r.setDataTypeNumericPrecision(null); }
						r.setDataTypeNumericScale(rs.getLong("numeric_scale")); if (rs.wasNull()) { r.setDataTypeNumericScale(null); }
						r.setCharsetName(rs.getString("character_set_name"));
						r.setCollationName(rs.getString("collation_name"));
						r.setDefinition(rs.getString("routine_definition"));
						r.setDeterministic("YES".equals(rs.getString("is_deterministic")));
						r.setSecurityType(rs.getString("security_type"));
						r.setSqlMode(rs.getString("sql_mode"));
						r.setComment(rs.getString("routine_comment"));
						r.setDefiner(rs.getString("definer"));
						
						/*
						RoutineParameterTO mtc = new RoutineParameterTO(t, 
							t.getSchema().getDatabase().upper(rs.getString("COLUMN_NAME")),
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
						t.getTableColumnMap().put(mtc.getName(), mtc);
						*/
					}
					return null;
				}
			}
		);

	}
	

}
