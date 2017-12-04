package org.tdar.nlp;

import static org.junit.Assert.*;

import org.junit.Test;

public class UtilsTest {

    @Test
    public void testQuotes() {
        String txt = " “Middle Sacaton 1” or <START:culture> “Late Snaketown <END> . ”";
        String txt_ = Utils.replaceSmartQuotes(txt);
        System.out.println(txt_);
        assertEquals(txt_,"\"Middle Sacaton 1\" or <START:culture> \"Late Snaketown <END> . \"");
    }
}
