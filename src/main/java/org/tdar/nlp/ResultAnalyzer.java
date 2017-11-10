package org.tdar.nlp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ResultAnalyzer {

    public static final int SKIP_PHRASES_LONGER_THAN = 5;
    private static final String PERSON = "person";
    private final Logger log = LogManager.getLogger(getClass());
    private String regexBoost = null;
    private List<String> boostValues = new ArrayList<>();

    private Map<String, TermWrapper> ocur = new HashMap<>();
    private String type;
    
    public ResultAnalyzer(NLPHelper helper) {
        this.type = helper.getType();
        this.boostValues = helper.getBoostValues();
        this.regexBoost = helper.getRegexBoost();
    }
    
    public void addPage(Page page) {
        Map<String, TermWrapper> map = page.getReferences().get(type);
        for (String key : map.keySet()) {
            TermWrapper wrap = ocur.get(key);
            TermWrapper wrap_ = map.get(key);
            
            if (wrap == null) {
                wrap = wrap_;
            } else {
                wrap.combine(wrap_);
            }
            ocur.put(key, wrap);
        }
    }

    public void printOccurrence() {
        int avg = 0;
        Map<String, TermWrapper> multiWord = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        // split into cleaned single and multi-word phrase
        for (Entry<String, TermWrapper> entry : ocur.entrySet()) {
            TermWrapper value = entry.getValue();
            String key = entry.getKey();
            key = Utils.cleanString(key);
            avg += value.getOccur();
            int numSpaces = StringUtils.countMatches(key, " ");
            if (numSpaces < SKIP_PHRASES_LONGER_THAN) {
                multiWord.put(key, value);
            } else {
                // log.debug("\t--" + key);
            }
        }
        if (ocur.size() > 0) {
            avg = avg / ocur.size();
        }

//        WordOverlapAnalyzer multi = new WordOverlapAnalyzer(multiWord.keySet());
//        Map<String, List<String>> analyze = multi.analyze(multiWord.keySet());
//        for (Entry<String, List<String>> entry : analyze.entrySet()) {
//            TermWrapper keyWrapper = multiWord.get(entry.getKey());
//            for (String val : entry.getValue()) {
//                TermWrapper vw = multiWord.get(val);
//                vw.setTerm(val);
//                keyWrapper.combine(vw);
//                multiWord.remove(val);
//                // log.debug("\t--" + val + " --> " + keyWrapper.getTerm());
//            }
//        }

//        if (PERSON.equals(type)) {
//            PersonalNameOverwrapAnalyzer overlap = new PersonalNameOverwrapAnalyzer();
//            overlap.combineMultiWordOverlap(multiWord);
//        }

        Map<Integer, List<String>> reverse = new HashMap<>();
        int weightedAvg = sortByOccurrence(multiWord, reverse);

        printResults(type, avg, reverse);
    }

    private void printResults(String type, int avg, Map<Integer, List<String>> reverse) {
        // output
        List<Integer> list = new ArrayList<Integer>(reverse.keySet());
        Collections.sort(list);
        Collections.reverse(list);
        for (Integer key : list) {
            for (String val : reverse.get(key)) {
                String header = type;
                if (StringUtils.isNotBlank(type)) {
                    header += " ";
                }
                if (key > avg) {
                    if (type.equalsIgnoreCase(PERSON) && Pattern.compile("\\d").matcher(val).find()) {
                    } else {
                        log.debug(header + key + " | " + val);
                    }
                } else {
                    // log.debug(header + "| " + val);
                }
            }
        }
    }

    /**
     * Flip a hash by the # of ocurrences as opposed to the term
     * 
     * @param multiWord
     * @return
     */
    private int sortByOccurrence(Map<String, TermWrapper> multiWord, Map<Integer, List<String>> reverse) {

        // reverse the hash by count
        int total = 0;
        for (Entry<String, TermWrapper> entry : multiWord.entrySet()) {
            TermWrapper value = entry.getValue();
            String key = entry.getKey();
            Integer weightedOccurrence = value.getWeightedOccurrence();
            for (String boost : boostValues) {
                if (StringUtils.containsIgnoreCase(key, boost)) {
                    weightedOccurrence += 200;
                }
                if (StringUtils.equalsIgnoreCase(key, boost)) {
                    weightedOccurrence -=1000;
                }
            }
            if (regexBoost != null && key.matches(regexBoost)) {
                weightedOccurrence += 200;
            }
            List<String> results = reverse.getOrDefault(weightedOccurrence, new ArrayList<String>());
            total += weightedOccurrence;
            results.add(key);
            reverse.put(weightedOccurrence, results);
        }
        if (multiWord.size() > 0) {
            return Utils.toPercent(total, multiWord.size());
        }
        return 0;
    }

}
