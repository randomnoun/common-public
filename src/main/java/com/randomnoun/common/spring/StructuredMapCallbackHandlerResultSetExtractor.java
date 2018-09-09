package com.randomnoun.common.spring;

/* (c) 2013 randomnoun. All Rights Reserved. This work is licensed under a
 * BSD Simplified License. (http://www.randomnoun.com/bsd-simplified.html)
 */

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.*;
import org.apache.log4j.Logger;

import com.randomnoun.common.Struct;

/** A bit like a StructuredListResultSetExtractor, but executes a callback on each object, rather than returning List of them.
 *
 * @see StructuredListResultSetExtractor
 *
 * 
 * @author knoxg
 */
public class StructuredMapCallbackHandlerResultSetExtractor 
    implements ResultSetExtractor<Object> {
    
    
    
    /** Logger instance for this class */
    private static Logger logger = Logger.getLogger(StructuredMapCallbackHandlerResultSetExtractor.class);  
    
    /** Contains mappings from source column names to (structured) target column names */
    Map<String, String> columnMapping;

    /** List to save results in */
    private final List<Map<String, Object>> results;
    private List<String> levels;

    /** Row mapper */
    private final RowMapper<Map<String, Object>> rowMapper;
    private Map<String, Object> lastResultRow = null;
    
    private StructuredMapCallbackHandler smch;

    public static interface StructuredMapCallbackHandler {
    	public void processMap(Map<String, Object> row);
    }
    
    /** The counter used to count rows */
    private int rowNum = 0;

    /**
     * Create a new RowMapperResultReader.
     * 
     * @param jt jdbcTemplate for some reason
     */
    public StructuredMapCallbackHandlerResultSetExtractor(JdbcTemplate jt, String mappings, StructuredMapCallbackHandler smch) {
        this(new ColumnMapRowMapper(), mappings, smch);
    }
    

    /**
     * Create a new RowMapperResultReader.
     * 
     * @param rowMapper the RowMapper which creates an object for each row
     */
    private StructuredMapCallbackHandlerResultSetExtractor(RowMapper<Map<String, Object>> rowMapper, String mappings, StructuredMapCallbackHandler smch) {
        this.smch = smch;
        if (mappings == null) { throw new NullPointerException("mappings cannot be null"); }

        // Use the more efficient collection if we know how many rows to expect:
        // ArrayList in case of a known row count, LinkedList if unknown
        this.results = new ArrayList<Map<String, Object>>();
        this.rowMapper = rowMapper;
        this.columnMapping = new HashMap<String, String>();
        this.levels = new ArrayList<String>(3); // we're not going to go higher than this too often

        StringTokenizer st = new StringTokenizer(mappings, ",");
        StringTokenizer st2;
        StringTokenizer st3;
        String column = null;
        String columnTarget = null;
        String token;
        String mapping;

        while (st.hasMoreTokens()) {
            mapping = st.nextToken().trim();

            if (mapping.indexOf(' ') == -1) {
                column = mapping;
                columnTarget = mapping;
            } else {
                // parse state (note that this uses a StringTokenizer, 
                // rather than a character-based parser)
                // 
                // 0 = start of parse
                // 1 = consumed column name
                // 2 = consumed 'as'
                // 3 = consumed mapping
                int state = 0; // 0=initial, 1=got column name, 2=got 'as', 3=got mapping
                st2 = new StringTokenizer(mapping, " ");
                while (st2.hasMoreTokens()) {
                    token = st2.nextToken().trim();
                    if (token.equals("")) { continue; }

                    if (state == 0) {
                        column = token;
                        state = 1;
                    } else if (state == 1) {
                        if (!token.equalsIgnoreCase("as")) {
                            throw new IllegalArgumentException("Invalid mapping '" + mapping + "'; expected AS");
                        }
                        state = 2;
                    } else if (state == 2) {
                        columnTarget = token;
                        state = 3;
                    } else if (state == 3) {
                        throw new IllegalArgumentException("Invalid mapping '" + mapping + "'; too many tokens");
                    }
                }
            }

            // check target for levels
            int levelIdx = 0;
            st3 = new StringTokenizer(columnTarget, ".");
            if (st3.hasMoreTokens()) {
                String level = st3.nextToken();

                while (st3.hasMoreTokens()) {
                    if (levelIdx < levels.size()) {
                        if (!levels.get(levelIdx).equals(level)) {
                            throw new IllegalArgumentException("Multiple lists in mapping at level " + levelIdx + ": '" + levels.get(levelIdx) + "' and '" + level + "'");
                        }
                    } else {
                        levels.add(level);
                        // System.out.println("Levels now: " + levels);
                    }
                    level = st3.nextToken();
                    levelIdx++;
                }
            }
            columnMapping.put(column.toUpperCase(), columnTarget);
        }
    }

	/** Required to support ResultSetExtractor interface
	 * 
	 * @param rs resultSet to process
	 * 
	 * @return null
	 */
    public Object extractData(ResultSet rs) throws SQLException, DataAccessException 
	{
		while (rs.next()) {
			processRow(rs);
		}
		if (results.size()>0) { smch.processMap(results.get(0)); }
		return null;
	}
    
    
    /** Used by the ResultReader interface to return the results read by this class
     * 
     * @see org.springframework.jdbc.core.ResultReader#getResults()
     *
    public List getResults() {
    	return null;
    }
    */

    /**
     * Used by the ResultReader interface to process a single row from the database.
     * 
     * <p>The row is read and matched against the 'levels' specified in the 
     * object constructor. As values change, tree branches are created in the returned
     * structured List. 
     * 
     * @see org.springframework.jdbc.core.RowCallbackHandler#processRow(java.sql.ResultSet)
     */
    @SuppressWarnings("unchecked")
	public void processRow(ResultSet rs)
        throws SQLException {
        Map<String, Object> row = (Map<String, Object>) rowMapper.mapRow(rs, this.rowNum++); // ClobRowMapper always returns a Map.
		// System.out.println("Processing row " + Struct.structuredMapToString("row", row));
        // logger.debug("row is " + Struct.structuredMapToJson(row));
        
        int createLevel = 0;
        List<Map<String, Object>> createList = results;
        Map<String, Object> createRow = new HashMap<String, Object>();
        String createPrefix = "";

        // determine highest level that we can create at
        if (lastResultRow != null) {
            // System.out.println("lastResultRow processing");
            createLevel = levels.size() + 1;

            // find lowest level that has a different value
            for (Iterator<Map.Entry<String, String>> i = columnMapping.entrySet().iterator(); i.hasNext();) {
                Map.Entry<String, String> entry = (Map.Entry<String, String>) i.next();
                String column = (String) entry.getKey();
                String columnTarget = (String) entry.getValue();

                List<Map<String,Object>> containerList = results; // maybe
                Map<String, Object> containerMap = lastResultRow;

                String component;
                int pos = columnTarget.indexOf('.');
                int level = 0;

                while (pos != -1) {
                    component = columnTarget.substring(0, pos);
                    columnTarget = columnTarget.substring(pos + 1);

                    if (!containerMap.containsKey(component)) {
                        throw new IllegalStateException("Missing field '" + component + "' in " + Struct.structuredMapToString("containerMap", containerMap) + "; last result row is " + Struct.structuredMapToString("lastResultRow", lastResultRow));
                    }

                    if (component.equals(levels.get(level))) {
                        level++;
                        containerList = (List<Map<String,Object>>) containerMap.get(component);
                        containerMap = (Map<String, Object>) containerList.get(containerList.size() - 1);
                        if (containerMap==null) {
                        	logger.error("null containerMap");
                        }
                    } else {
                        containerMap = (Map<String, Object>) containerMap.get(component);
                        if (containerMap==null) {
                        	logger.error("null containerMap");
                        }

                    }

                    pos = columnTarget.indexOf('.');
                }

                Object thisValue = row.get(column);
                Object lastValue = containerMap.get(columnTarget);
                // System.out.println("Comparing thisValue '" + thisValue + "' to lastValue '" + lastValue + "'");

                if ((thisValue == null && lastValue != null) || (thisValue != null && !thisValue.equals(lastValue))) {
                    // values are different; create row
                    if (createLevel > level) {
                        createList = containerList;

                        // System.out.println("Reducing level to '" + level + "' because of " +
                        //   "column '" + columnTarget + "' differing (" + thisValue + " instead of previous: " + lastValue);
                        createLevel = level;
                    }
                }
            }
        }

        if (createLevel > levels.size()) {
            // rows are completely identical -- don't add it to the list
            return;
        }

        for (int i = 0; i < createLevel; i++) {
            createPrefix = createPrefix + levels.get(i) + ".";
        }

        // generate 'createRow'
        for (Iterator<Map.Entry<String, String>> i = columnMapping.entrySet().iterator(); i.hasNext();) {
            Map.Entry<String, String> entry = i.next();
            String column = (String) entry.getKey();
            String columnTarget = (String) entry.getValue();
            if (!columnTarget.startsWith(createPrefix)) {
                continue;
            }
            Object value = row.get(column); 

            //logger.debug("About to add column '" + columnTarget + "' from rs column '" + column + "'; createPrefix = '" + createPrefix + "' with value '" + value + "'");
            
            columnTarget = columnTarget.substring(createPrefix.length());
            //logger.debug("  columnTarget '" + columnTarget + "'");

            List<Map<String, Object>> containerList = createList;
            Map<String, Object> containerMap = createRow;
            
            int level = createLevel;  // ** was 0 ?
            String component;
            int pos = columnTarget.indexOf('.');

            while (pos != -1) {
                component = columnTarget.substring(0, pos);
                columnTarget = columnTarget.substring(pos + 1);

                if (component.equals(levels.get(level))) {
                    level++;
                    containerList = (List<Map<String, Object>>) containerMap.get(component);

                    if (containerList == null) {
                        containerList = new ArrayList<Map<String, Object>>();
                        containerMap.put(component, containerList);
                        containerList.add(new HashMap<String, Object>());
                    }

                    containerMap = (Map<String, Object>) containerList.get(containerList.size() - 1);
                    if (containerMap==null) {
                    	logger.error("C null containerMap");
                    }

                } else {
                    containerMap = (Map<String, Object>) containerMap.get(component);
                    if (containerMap==null) {
                    	logger.error("D null containerMap");
                    }

                }

                pos = columnTarget.indexOf('.');
            }

            containerMap.put(columnTarget, value);
        }

        if (createList == results) {
        	if (results.size()>0) { smch.processMap(results.get(0)); } 
        	results.clear(); // @TODO don't use a list for this, since we only ever hold a single object. Keeps it vaguely similar to StructuredResultReader a.k.a. StructuredListResultSetExtractor though.
        } else {
        	// smch.processSomethingElsePerhaps();
        }
        
        createList.add(createRow);
        lastResultRow = results.get(results.size() - 1);
        
    }

}
