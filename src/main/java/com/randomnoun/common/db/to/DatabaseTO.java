package com.randomnoun.common.db.to;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.randomnoun.common.db.enums.DatabaseType;

/** The thing returned by the DatabaseReader.
 * 
 * 
 * @author knoxg
 */
public class DatabaseTO {
	public DatabaseType dbType;
	public boolean caseInsensitive = false;
	public Map<String, SchemaTO> schemas = null;
	
	public DatabaseType getDatabaseType() { return dbType; }
	
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

	
}
