package com.randomnoun.common;

import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.assertEquals;

public class TextTest {
    @Test
    public void testCsvEscape(){
        HashMap<String, String> inputOutput = new HashMap<String, String>();
        inputOutput.put("=CMD()", "\"'=CMD()\"");
        inputOutput.put("=12345", "\"'=12345\"");
        inputOutput.put("abc,def","\"abc,def\"");
        inputOutput.put("=DDE(\"cmd\";\"/C calc\";\"__DdeLink_60_870516294\")", "\"'=DDE(\"\"cmd\"\";\"\"/C calc\"\";\"\"__DdeLink_60_870516294\"\")\"");
        inputOutput.put("=cmd|' /C calc'!A0", "\"'=cmd\\|' /C calc'!A0\"");
        inputOutput.put("+cmd|' /C calc'!A0", "\"'+cmd\\|' /C calc'!A0\"");
        inputOutput.put("-cmd|' /C calc'!A0", "\"'-cmd\\|' /C calc'!A0\"");
        for(String in : inputOutput.keySet()){
            String out =Text.escapeCsv(in);
            assertEquals(inputOutput.get(in), out);
        }
    }
}
