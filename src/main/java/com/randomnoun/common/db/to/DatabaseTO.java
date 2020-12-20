package com.randomnoun.common.db.to;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.randomnoun.common.db.enums.DatabaseTypeEnum;

/** The thing returned by the DatabaseReader.
 * 
 * 
 * @author knoxg
 */
public class DatabaseTO {
	private boolean caseInsensitive = false;
	private DatabaseTypeEnum databaseType;
	private Map<String, SchemaTO> schemaMap = null;
	
	public DatabaseTypeEnum getDatabaseType() { return databaseType; }
	
	public void setCaseInsensitive() {
		this.caseInsensitive = true;
	}
	
	public String upper(String s) {
		return caseInsensitive ? s.toUpperCase() : s;
	}
	
	public List<String> upper(List<String> l) {
		if (caseInsensitive) {
			List<String> r = new ArrayList<String>();
			for (String s : l) {
				r.add(s.toUpperCase());
			}
			return r;
		} else {
			return l;
		}
	}

	public boolean isCaseInsensitive() {
		return caseInsensitive;
	}

	public void setCaseInsensitive(boolean caseInsensitive) {
		this.caseInsensitive = caseInsensitive;
	}

	public DatabaseTypeEnum getDbType() {
		return databaseType;
	}

	public void setDbType(DatabaseTypeEnum dbType) {
		this.databaseType = dbType;
	}

	public Map<String, SchemaTO> getSchemaMap() {
		return schemaMap;
	}

	public void setSchemaMap(Map<String, SchemaTO> schemaMap) {
		this.schemaMap = schemaMap;
	}

	public void setDatabaseType(DatabaseTypeEnum databaseType) {
		this.databaseType = databaseType;
	}

	
}
