package org.tdar.nlp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

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
                //if exact matches, ignore
                if (StringUtils.equals(term, phrase)) {
                    continue;
                }

                // if we contain, track it
                if (contains(phrase,term)) {
                    seen.add(phrase);
                }
            }
            
            // if we only have one overlap between the sets, then we combine
            if (seen.size() == 1) {
                String key = seen.iterator().next();
                List<String> orDefault = combine.getOrDefault(key, new ArrayList<String>());
                orDefault.add(term);
                combine.putIfAbsent(key, orDefault);
            }
            
            // if we have more than one match, let's see if we're nested
            if (seen.size() > 1) {
                boolean nested = true;
                Iterator<String> iter = seen.iterator();
                String longest = iter.next();
                while (iter.hasNext()) {
                    String next= iter.next();
                    if (longest.length() > next.length()) {
                        if (!contains(longest,next)) {
                            // failure
                            nested = false;
                            break;
                        }
                    } else {
                        if (!contains(next, longest)) {
                            // failure
                            nested = false;
                            break;
                        }
                        longest = next;
                    }
                }

                if (nested) {
                    List<String> orDefault = combine.getOrDefault(longest, new ArrayList<String>());
                    orDefault.add(term);
                    combine.putIfAbsent(longest, orDefault);
                    
                    System.out.println("::" + longest + " --> " + term + " --> " + seen);
                }
            }
        }
        return combine;
    }

    public static boolean contains(String next, String longest) {
        if (StringUtils.startsWith(next, longest + " ") || StringUtils.endsWith(next, " " + longest) || next.contains( " " + longest + " ")){
            return true;
        }
        return false;
    }
}
