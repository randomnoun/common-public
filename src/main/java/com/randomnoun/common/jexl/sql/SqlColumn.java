package com.randomnoun.common.jexl.sql;

/* (c) 2013 randomnoun. All Rights Reserved. This work is licensed under a
 * BSD Simplified License. (http://www.randomnoun.com/bsd-simplified.html)
 */

import java.sql.Types;


/**
 * Objects of this type are used in the evaluation context passed into an SqlGenerator
 * to define a database column used in a TopLevelExpression.
 *
 * <p>See the SqlGenerator class for detailed documentation.
 *
 * 
 * @author knoxg
 */
public class SqlColumn
{

    /** These constants are copied directly from the java.sql.Types class;
     *  note that we also add the new types MONEYVALUE, DATEVALUE, TIMEVALUE, FIXEDDATEVALUE and FIXEDTIMEVALUE */
    public static final int ARRAY = Types.ARRAY;
    public static final int BIGINT = Types.BIGINT;
    public static final int BINARY = Types.BINARY;
    public static final int BIT = Types.BIT;
    public static final int BLOB = Types.BLOB;
    public static final int BOOLEAN = Types.BOOLEAN;
    public static final int CHAR = Types.CHAR;
    public static final int CLOB = Types.CLOB;
    public static final int DATALINK = Types.DATALINK;
    public static final int DATE = Types.DATE;
    public static final int DECIMAL = Types.DECIMAL;
    public static final int DISTINCT = Types.DISTINCT;
    public static final int DOUBLE = Types.DOUBLE;
    public static final int FLOAT = Types.FLOAT;
    public static final int INTEGER = Types.INTEGER;
    public static final int JAVA_OBJECT = Types.JAVA_OBJECT;
    public static final int LONGVARBINARY = Types.LONGVARBINARY;
    public static final int LONGVARCHAR = Types.LONGVARCHAR;
    public static final int NULL = Types.NULL;
    public static final int NUMERIC = Types.NUMERIC;
    public static final int OTHER = Types.OTHER;
    public static final int REAL = Types.REAL;
    public static final int REF = Types.REF;
    public static final int SMALLINT = Types.SMALLINT;
    public static final int STRUCT = Types.STRUCT;
    public static final int TIME = Types.TIME;
    public static final int TIMESTAMP = Types.TIMESTAMP;
    public static final int TINYINT = Types.TINYINT;
    public static final int VARBINARY = Types.VARBINARY;
    public static final int VARCHAR = Types.VARCHAR;

    /** The range 5000-6000 is currently unused in java.sql.Types;
     * (and doesn't look like it will ever be used), so I'm going to use
     * this for custom types. Anything in here is treated specially in SqlGenerator
     * when converting to SQL.
     *
     */
    public static final int MONEYVALUE = 5000; // perform currency matching
    public static final int DATEVALUE = 5001; // perform conditional date ranges
    public static final int TIMEVALUE = 5002; // perform conditional date ranges
    public static final int FIXEDDATEVALUE = 5003; // as per DATEVALUE, stored as String (no TZ)
    public static final int FIXEDTIMEVALUE = 5004; // as per TIMEVALUE, stored as String (no TZ)

    /** The name of this database column */
    private String name = null;

    /** If this object describes a MONEYVALUE column, this field contains the name of the
     *  currency code column (this.name contains the amount). */
    private String currencyCodeName = null;

    /** The name of the table this fields belongs to (or null if not specifying tables) */
    private String table = null;

    /** The type of this column. Corresponds to one of the public static final int constants
     *  defined in this class. */
    private int dataType = VARCHAR;

    /** Create a new column, of type VARCHAR
     *
     * @param name     The name of the column in the database
     */
    public SqlColumn(String name)
    {
        this.name = name;
    }

    /** Create a new column, with the supplied datatype
     *
     * @param name     The name of the column in the database
     * @param dataType The datatype of the column. Corresponds to one of the public static final
     *   int constants defined in this class.
     */
    public SqlColumn(String name, int dataType)
    {
        this.name = name;
        this.dataType = dataType;
    }

    /** Create a new column, in a specific table, of type VARCHAR
     *
     * @param name     The name of the column in the database
     * @param table    The name of the table in the database this column is in
     */
    public SqlColumn(String name, String table)
    {
        this.name = name;
        this.table = table;
    }

    /** Create a new column, in a specific table, with the supplied datatype
     *
     * @param name     The name of the column in the database
     * @param table    The name of the table in the database this column is in
     * @param dataType The datatype of the column. Corresponds to one of the public static final
     *   int constants defined in this class.
     */
    public SqlColumn(String name, String table, int dataType)
    {
        this.name = name;
        this.table = table;
        this.dataType = dataType;
    }

    /** Sets the column name that contains the currency code, for a MONEYVALUE SqlColumn.
     * If a table has been specified for this column, then the currency is <b>always</b>
     * sourced from the same table.
     *
     * @param currencyCodeName the column name that contains the currency code
     *
     * @throws IllegalStateException if this method is called for a non-MONEYVALUE SqlColumn
     */
    public void setCurrencyCodeName(String currencyCodeName)
    {
        if (this.dataType != MONEYVALUE)
        {
            throw new IllegalStateException(
                "A currencyColumnName may only be set for a MONEYVALUE type");
        }

        this.currencyCodeName = currencyCodeName;
    }

    /** Retrieves the name of this SqlColumn, as set by the constructor */
    public String getName()
    {
        return name;
    }

    /** Retrieves the table of this SqlColumn, as set by the constructor */
    public String getTable()
    {
        return table;
    }

    /** Retrieves the currency column of this SqlColumn, as supplied by setCurrencyCodeName */
    public String getCurrencyCodeName()
    {
        return currencyCodeName;
    }

    /** Retrieves the data type of this SqlColumn */
    public int getDataType()
    {
        return dataType;
    }

    /** Either returns the name of this column, or table + "." + name, if a table has been set */
    public String getFullName()
    {
        if (table == null)
        {
            return name;
        }
        else
        {
            return table + "." + name;
        }
    }

    /** Either returns the name of the currency column, or table + "." + currency name, if a table
     *  has been set */
    public String getFullCurrencyCodeName()
    {
        if (table == null)
        {
            return currencyCodeName;
        }
        else
        {
            return table + "." + currencyCodeName;
        }
    }

    /** Retrieve a string representation of this column */
    public String toString()
    {
        return getFullName();
    }
}
