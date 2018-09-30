package com.randomnoun.common.jexl.sql;

/* (c) 2013 randomnoun. All Rights Reserved. This work is licensed under a
 * BSD Simplified License. (http://www.randomnoun.com/bsd-simplified.html)
 */


/**
 * An object representing a variable that can be passed in as a positional parameter.
 *
 * <p>See SqlGenerator for a description of how this class is intended to be used.
 *
 * 
 * @author knoxg
 */
public class PositionalParameter
{
    /** The name of this positional parameter. */
    private String name;

    /** Create a new Positional Parameter
     *
     * @param name A name for this positional parameter, which is returned
     *   in the VAR_PARAMETERS list when the parameter is referenced in generated SQL.
     */
    public PositionalParameter(String name)
    {
        this.name = name;
    }

    /** Retrieves the name of this positional parameter */
    public String getName()
    {
        return name;
    }
}
