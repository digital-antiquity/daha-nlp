package org.tdar.nlp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class WordOverlapAnalyzer {

    private Set<String> input = new HashSet<>();
    
    public WordOverlapAnalyzer(Set<String> input) {
        this.input = input;
    }
    
    public Map<String, List<String>> analyze(Set<String> terms) {
        Map<String, List<String>> combine = new HashMap<>();
        for (String term : terms) {
            Set<String> seen = new HashSet<>();
            for (String phrase : input) {
                if (phrase.contains(term)) {
                    seen.add(phrase);
                }
            }
            if (seen.size() == 1) {
                String key = seen.iterator().next();
                List<String> orDefault = combine.getOrDefault(key, new ArrayList<String>());
                orDefault.add(term);
                combine.putIfAbsent(key, orDefault);
            }
        }
        return combine;
    }
}
