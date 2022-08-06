package com.randomnoun.common.jexl.eval;

/* (c) 2013 randomnoun. All Rights Reserved. This work is licensed under a
 * BSD Simplified License. (http://www.randomnoun.com/bsd-simplified.html)
 */
import java.util.HashMap;
import java.util.Map;

/**
 * Provides variables and functions for an expression to be evaluated with
 *
 * 
 * @author knoxg
 */
public class EvalContext
{
    /** Contains a map of variable names to variable values */
    private Map<String, Object> variables = new HashMap<String, Object>();

    /** Contains a map of function names to EvalFunction values */
    private Map<String, EvalFunction> functions = new HashMap<String, EvalFunction>();

    /** Sets all variables accessable to an expression. */
    public void setVariables(Map<String, Object> variables)
    {
        this.variables = variables;
    }

    /** Set a specific variable binding */
    public void setVariable(String name, Object value)
    {
        variables.put(name, value);
    }

    /** Removes a specific variable binding */
    public void unsetVariable(String name)
    {
        variables.remove(name);
    }

    /** Returns the value of a variable (used in the Evaluator class). */
    public Object getVariable(String varName)
    {
        return variables.get(varName);
    }

    /** Returns true if the variable exists. */
    public boolean hasVariable(String varName)
    {
        return variables.containsKey(varName);
    }

    /** Sets all variables accessible to an expression. */
    public void setFunctions(Map<String, EvalFunction> functions)
    {
        this.functions = functions;
    }

    /** Set a specific function */
    public void setFunction(String name, EvalFunction function)
    {
        functions.put(name, function);
    }

    /** Returns the value of a variable (used in the Evaluator class). */
    public Object getFunction(String function)
    {
        return functions.get(function);
    }

    /** Returns a component of a compound variable (which must currently be a Map object).
     * The component name is the key of the map. 
     * e.g. if a Map variable called 'x' contains a key 'y' with the value
     * 'z', then the following code could be used to retrieve this value:
     *
     * <pre>
     *   x = evalContext.getVariable("x")
     *   value = evalContext.getVariableComponent(x, "x", "y")  // evaluates to 'z'
     * </pre>
     *
     * <p>Note that this is only useful in the Evaluator to retrieve compound variables,
     * outside the evaluator you should be just be evaluating the value "x.y" directly.
     *
     * @param varObj the compound variable value that is being evaluated (as returned
     *   by the getVariable() method)
     * @param baseName the name of the variable
     * @param componentName the name of the component in varObj to return.
     *
     * */
    public Object getVariableComponent(Object varObj, String baseName,
        String componentName)
        throws EvalException
    {
        if (varObj == null) {
            throw new EvalException("Can not retrieve component '" + componentName +
                "' from '" + baseName + "'; base is null");
        }

        if (varObj instanceof Map) {
            return ((Map<?, ?>) varObj).get(componentName);
        } else {
            throw new EvalException("Can not retrieve component '" + componentName +
                "' from '" + baseName + "'; base is of type '" +
                varObj.getClass().getName() + "'");
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param varObj DOCUMENT ME!
     * @param baseName DOCUMENT ME!
     * @param componentName DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public boolean hasVariableComponent(Object varObj, String baseName,
        String componentName)
    {
        if (varObj == null) {
            throw new EvalException("Can not retrieve component '" + componentName +
                "' from '" + baseName + "'; base is null");
        }

        if (varObj instanceof Map) {
            return ((Map<?, ?>) varObj).containsKey(componentName);
        } else {
            throw new EvalException("Can not retrieve component '" + componentName +
                "' from '" + baseName + "'; base is of type '" +
                varObj.getClass().getName() + "'");
        }
    }

}
