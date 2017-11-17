package org.tdar.nlp;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.tdar.nlp.nlp.NLPHelper;

public class NumericCountTest {

    @Test
    public void test() {
        String key = "42 1/22/27 V";
        int per = NLPHelper.percentNumericCharacters(key);
        System.out.println(per);
        assertEquals(87, per);
        
        key = "122 12/21/27 HI NE";
        per = NLPHelper.percentNumericCharacters(key);
        System.out.println(per);
    }
    
    
}
