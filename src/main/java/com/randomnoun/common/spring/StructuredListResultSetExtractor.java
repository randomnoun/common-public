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
import com.randomnoun.common.spring.ClobRowMapper;

// this could probably delegate to StructuredMapCallbackHandlerResultSetExtractor these days
// 

/**
 * This class returns structured object graphs composed of lists/maps.
 * Think of it as a poor man's JDO, using Maps and Lists rather than concrete
 * classes, which allows us to omit the POJO generation step.
 *
 * <p>This ResultReader is to be used in the JdbcTemplate class to retrieve structured
 * representations of queries covering more than one table. In a simple case, say a
 * cowGroup contains many cows, and we wish to return the cowGroup ID and name as
 * well as the cow ID and name in the one query; typically, this would be done
 * through the following hypothetical SQL:
 *
 * <pre class="code">
 * SELECT cowGroupId, cowGroupName, cowId, cowName
 * FROM cowGroups, cows
 * WHERE cows.cowGroupId = cowGroups.cowGroupId
 * </pre>
 *
 * Normally, Spring would return this as a List containing a map containing four
 * keys (cowGroupId, cowGroupName, cowId and cowName), containing the values of
 * each column returned by the RDBMS; e.g. the code fragment
 *
 * <pre class="code">
             List result1 = jt.queryForList(sql);
             System.out.println(Text.structuredListToString("result1", result1));
             </pre>
             (with the SQL above) would produce this output (a three-element list):
 * <pre class="code">
              result1 = [
                0 = {
                  COWGROUPID => 1
                  COWGROUPNAME => 'ROLLINGFIELD7'
                  COWID => 1000
                  COWNAME => 'DAISY'
                }
                1 = {
                  COWGROUPID => 2
                  COWGROUPNAME => 'SLAUGHTERHOUSE5'
                  COWID => 1001
                  COWNAME => 'BUTTERCUP'
                }
                2 = {
                  COWGROUPID => 2
                  COWGROUPNAME => 'SLAUGHTERHOUSE5'
                  COWID => 1002
                  COWNAME => 'STEVE'
                }
              ]
              </pre>
 *
 * note how both BUTTERCUP and STEVE belong to the SLAUGHTERHOUSE5 cowGroup, but
 * DAISY is within it's own cowGroup. If we wanted to express this relationship
 * by having a list of cows within each cowgroup, we would have to scan through
 * this list manually, and filter each cow into it's parent cowGroup.
 *
 * <p>This object automates this process; e.g. the same structure above, when
 * processed using the following code snippet:
 *
 * <pre class="code">
              String mapping = "cowGroupId, cowGroupName, " +
                "cowId AS cows.cowId, cowName AS cows.cowName";
              List result2 = jt.query(sql, new StructuredResultReader(mapping));
              System.out.println(Text.structuredListToString("result2", result2));
             </pre>
 * ... would produce this output (a two-element list) ...
             <pre style="code">
              result2 = [
                0 = {
                  cowGroupName => 'ROLLINGFIELD7'
                  cowGroupId => 2
                  cows = [
                    0 = {
                      cowId => 1000
                      cowName => 'DAISY'
                    }
                  ]
                }
                1 = {
                  cowGroupName => 'SLAUGHTERHOUSE5'
                  cowGroupId => 1
                  cows = [
                    0 = {
                      cowId => 1001
                      cowName => 'BUTTERCUP'
                    }
                    1 = {
                      cowId => 1002
                      cowName => 'STEVE'
                    }
                  ]
                }
              ]
              </pre>
 *
 * which may be more suitable for processing. This structure can also be used within
 * JSTL, e.g.
 *
 * <pre class="code">
 *   &lt;c:out value="${result2[0].cows[1].cowName}"/>
 * </pre>
 *
 * would generate the text "STEVE".
 *
 * <p>This class can generate an arbitrary number of levels within the returned structure
 * by specifying more groupings in the mapping text. This class also performs
 * camelCasing of returned columns (e.g. to return 'cowName' rather than 'COWNAME').
 *
 * <p>The guts of the interface lies in the mapping string passed to the StructuredResultReader
 * constructor. The syntax of this mapping string is a list of column names, separated by
 * commas; a column name may also have an "AS" definition, which defines how it
 * is to be represented in the returned structure.
 *
 * <i>e.g.</i> assuming a department contains many people, this mapping will group each
 * person within their department:
 *
 * <pre class="code">
 * departmentName, personName AS people.name, personId AS people.id
 * </pre>
 *
 * which will return a List of Maps, containing two keys, 'departmentName' and 'people'.
 * The people value will be a List of Maps, containing two keys, 'name' and 'id'.
 *
 * <p>If each person now has a number of addresses, we could perform a two-level grouping,
 * using the following mapping:
 *
 * <pre class="code">
 * department, personName AS people.name, personId AS people.id,
 * addressType AS people.addresses.type
 * </pre>
 *
 * <p>Each people value will now be a List of Maps containing three keys, 'name', 'id' and
 * 'addresses'; the 'addresses' value will be a List of Maps with only one key, 'type'.
 *
 * <p>Hope that all makes sense.
 *
 * <p>Implementation note: The resultset returned by the SQL must be sorted by
 * the top-level group, then by the second-level group, and so on, for this code to
 * work as expected.
 *
 * 
 * @author knoxg
 */
public class StructuredListResultSetExtractor
    implements ResultSetExtractor<Object> {
    
    
    
    /** Logger instance for this class */
    private static Logger logger = Logger.getLogger(StructuredListResultSetExtractor.class);  
    
    /** Contains mappings from source column names to (structured) target column names */
    Map<String, String> columnMapping;

    /** List to save results in */
    private final List<Map<String, Object>> results;
    private List<String> levels;

    /** Row mapper */
    private final RowMapper<Map<String, Object>> rowMapper;
    private Map<String, Object> lastResultRow = null;

    /** The counter used to count rows */
    private int rowNum = 0;

    /**
     * Create a new RowMapperResultReader.
     * @param rowMapper the RowMapper which creates an object for each row
     */
    public StructuredListResultSetExtractor(JdbcTemplate jt, String mappings) {
        this(new ClobRowMapper(jt), mappings, 0);
    }

    /**
     * Create a new RowMapperResultReader.
     * @param rowMapper the RowMapper which creates an object for each row
     * @param rowsExpected the number of expected rows
     * (just used for optimized collection handling)
     */
    public StructuredListResultSetExtractor(RowMapper<Map<String, Object>> rowMapper, String mappings, int rowsExpected) {
        
        if (mappings == null) { throw new NullPointerException("mappings cannot be null"); }

        // Use the more efficient collection if we know how many rows to expect:
        // ArrayList in case of a known row count, LinkedList if unknown
        this.results = (rowsExpected > 0) ? new ArrayList<Map<String, Object>>(rowsExpected) : new LinkedList<Map<String, Object>>();
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
	 * @return a List of structured Maps, as described in the class javadoc
	 */
    public Object extractData(ResultSet rs) throws SQLException, DataAccessException 
	{
		// int rowNum = 0;
		while (rs.next()) {
			// results.add(this.rowMapper.mapRow(rs, rowNum++));
			processRow(rs);
		}
		return results;
	}
    
    
    /**
     * Used by the ResultReader interface to return the results read by this class
     * 
     * @see org.springframework.jdbc.core.ResultReader#getResults()
     *
    public List getResults() {
        return results;
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

                List<Map<String,Object>> containerList = results;
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
        for (Iterator<Map.Entry<String, String>> i = columnMapping.entrySet().iterator(); i.hasNext(); ) {
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

                /* if (!containerMap.containsKey(component)) {
                   containerMap.put(component, new HashMap());
                   } */

                // System.out.println("Setting component '" + component + "' in " + 
                //   Codec.structuredMapToString("containerMap", containerMap));
                if (component.equals(levels.get(level))) {
                    level++;
                    containerList = (List<Map<String, Object>>) containerMap.get(component);

                    if (containerList == null) {
                        containerList = new ArrayList<Map<String,Object>>();
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

        createList.add(createRow);
        lastResultRow = (Map<String, Object>) results.get(results.size() - 1);
        /*
        if (logger.isDebugEnabled()) {
            logger.debug("processRow complete; added row " + Struct.structuredMapToString("createRow", createRow));
            logger.debug("resultSet now = " + Struct.structuredListToString("results", results));
        }
        */
    }


}
