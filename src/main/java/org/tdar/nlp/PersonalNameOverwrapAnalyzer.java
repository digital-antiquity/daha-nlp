package org.tdar.nlp;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PersonalNameOverwrapAnalyzer {

    /**
     * Deal with cases like A. Brin A Brin and Adam Brin, and try and combine overlapping names, it's not exact and has issues like Smith, A. Smith, and B.
     * Smith...
     * 
     * @param type
     * @param multiWord
     */
    public void combineMultiWordOverlap( Map<String, TermWrapper> multiWord) {
        List<String> keySet = new ArrayList<String>(multiWord.keySet());
        Set<String> set2 = new TreeSet<>(new StringLengthComparator());
        set2.addAll(keySet);
        Set<String> toRemove = new HashSet<>();

        /**
         * Match Shortened Versions of Names
         */
        for (String key : keySet) {
            String cl = stripClean(key).toLowerCase();
            String init = "";
            String init2 = "";
            String init3 = "";
            // -------------------------- 1 -- 2 ------- 3 -------- 4
            Pattern p = Pattern.compile("(\\w)(\\w*)\\.?\\s(\\w\\.?)\\s([\\w]+)");
            Matcher matcher = p.matcher(cl);
            if (matcher.matches()) {
                // First Last (w/o initial)
                init = matcher.group(1) + matcher.group(2) + " " + matcher.group(4);
                // F. Last
                init2 = matcher.group(1) + " " + matcher.group(4);
            }

            // -------------------------- 1 -- 2 ------- 3
            Pattern p2 = Pattern.compile("(\\w)(\\w*)\\.?\\s(\\w+)");
            Matcher matcher2 = p2.matcher(cl);
            if (matcher2.matches()) {
                // F. Last
                init3 = matcher2.group(1) + " " + matcher2.group(3);
            }
            for (String keyi : set2) {
                if (keyi.equals(key) || toRemove.contains(keyi)) {
                    continue;
                }
                String clean = stripClean(keyi).toLowerCase();
                if (clean.equalsIgnoreCase(init) || clean.equalsIgnoreCase(init2) || clean.equalsIgnoreCase(init3)) {
                    toRemove.add(keyi);
                    TermWrapper termWrapper = multiWord.get(keyi);
                    termWrapper.setTerm(keyi);
                    multiWord.get(key).combine(termWrapper);
                }
            }
        }

        for (String key : toRemove) {
            multiWord.remove(key);
        }
    }


    /**
     * Remove punctuation and force to lowercase
     * 
     * @param key
     * @return
     */
    private String stripClean(String key) {
        String mat = key.toLowerCase().replaceAll("[^\\w\\s]", "");
        return mat;
    }
}
