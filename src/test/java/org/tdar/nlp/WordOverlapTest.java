package org.tdar.nlp;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.junit.Test;

public class WordOverlapTest {

    @Test
    public void test() {
        WordOverlapAnalyzer analyer = new WordOverlapAnalyzer(new HashSet<>(Arrays.asList("Isa Chandra Moskawitz", "Terry Hope Romero", "Terry George")));
        Map<String, List<String>> analyze = analyer.analyze(new HashSet<>(Arrays.asList("Moskawitz", "Isa Chandra", "Terry")));
        System.out.println(analyze);
        assertTrue(analyze.get("Isa Chandra Moskawitz").contains("Isa Chandra"));
        assertTrue(analyze.get("Isa Chandra Moskawitz").contains("Moskawitz"));
        assertTrue(analyze.keySet().size() == 1);
    }
    
    

}
