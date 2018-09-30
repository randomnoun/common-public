package com.randomnoun.common.jexl;

/* (c) 2013 randomnoun. All Rights Reserved. This work is licensed under a
 * BSD Simplified License. (http://www.randomnoun.com/bsd-simplified.html)
 */
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.StringReader;

import com.randomnoun.common.jexl.ast.TopLevelExpression;
import com.randomnoun.common.jexl.parser.ExpressionParser;
import com.randomnoun.common.jexl.parser.ParseException;
import com.randomnoun.common.jexl.parser.TokenMgrError;
import com.randomnoun.common.jexl.visitor.TreeDumper;

/**
 * Couple of methods to convert things to and from TopLevelExpressions.
 *
 * 
 * @author knoxg
 */
public class ExpressionUtil
{

    /** Convert Java expression String to a TopLevelExpression */
    public static TopLevelExpression stringToExpression(String expressionString)
        throws java.text.ParseException
    {
        StringReader reader = new StringReader(expressionString);
        ExpressionParser parser = new ExpressionParser(reader);
        TopLevelExpression root = null;

        try {
            root = parser.TopLevelExpression();
        } catch (ParseException pe) {
            throw new java.text.ParseException(pe.getMessage(), -1);
        } catch (TokenMgrError tme) {
            throw new java.text.ParseException(tme.getMessage(), -1);
        }

        return root;
    }

    /** Convert TopLevelExpression to a Java expression String */
    public static String expressionToString(TopLevelExpression expression) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintWriter out = new PrintWriter(baos);
        TreeDumper dumper = new TreeDumper(out);
        dumper.visit(expression);
        out.flush();
        return baos.toString();
    }

}
