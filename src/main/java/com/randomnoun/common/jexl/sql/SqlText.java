package com.randomnoun.common.jexl.sql;

/* (c) 2013 randomnoun. All Rights Reserved. This work is licensed under a
 * BSD Simplified License. (http://www.randomnoun.com/bsd-simplified.html)
 */


/** Class to mark a String as generated 'SQL' (as opposed to
 *  a String literal).
 *
 *  <p>As the SqlGenerator traverses nodes in the AST graph, the visit() methods
 *  return values which are either POJOs (Integers, Strings, Longs, etc...) which
 *  are <i>evaluated</i> from the AST, or SQL text, which is basically an
 *  opaque string which is generated from what we've seen so far in the AST;
 *  (e.g. "abc &gt; 123 AND def LIKE 'GHI'"). As soon as a POJO is converted into
 *  it's SQL form, it is wrappered in this object, so that other visit() nodes
 *  don't accidentally try to evaluate it.
 *
 *  @author knoxg
 *  
 */
public class SqlText
{

    /** An SQL text 'snippet' */
    private String sql;

    /** Create an SQL snippet */
    public SqlText(String sql)
    {
        this.sql = sql;
    }

    /** Retrieve the SQL snippet stored in this object. */
    public String getSqlText()
    {
        return sql;
    }

    /** We supply a .toString() method to make debugging easier */
    public String toString()
    {
        return sql;
    }
}
