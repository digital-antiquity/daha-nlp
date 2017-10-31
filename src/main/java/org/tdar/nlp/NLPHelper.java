package org.tdar.nlp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

/**
 * Notes: 
 *  * can we identify names in citations eg (Kent 2103) and organize or weight them differently?
 *  * should we weight nested names for instititutions?
 *  * should we weight occurrence when handling nested people?   
 * @author abrin
 *
 */
public class NLPHelper {
    private static final String PERSON = "person";
    public static final int SKIP_PHRASES_LONGER_THAN = 5;
    public static String[] stopWords = { "investigation", "catalog", " and ", " or ", "appendix", "submitted", "expection" };
    public static final String NUMERIC_PUNCTUATION = "^[\\d\\s/\\.;\\(\\)\\[\\]\\?\\-\\_\\,]+$";
    public static final int MIN_TERM_LENGTH = 3;
    public static final boolean REMOVE_HTML_TERMS = true;
    private Map<String, TermWrapper> ocur = new HashMap<>();
    private String type;

    public NLPHelper(String type) {
        this.type = type;
    }

    
    
    
    private boolean stringValid(String key) {
        if ((StringUtils.contains(key, "=\"") || StringUtils.contains(key, "=\'")) && REMOVE_HTML_TERMS) {
            return false;
        }

        if (key.matches(NUMERIC_PUNCTUATION)) {
            return false;
        }

        if (containsStopWord(key) && PERSON.equals(type)) {
            return false;
        }
        int percentOneLetter = percentOneLetter(key);
        if (percentOneLetter > 74 && PERSON.equals(type)) {
            return false;
        }

        if (percentOneLetter > 50) {
            return false;
        }

        if (percentNumberWords(key) > 50) {
            return false;
        }

        if (percentNumericCharacters(key) > 50) {
            return false;
        }

        if (unmatchedChars(key)) {
            return false;
        }
        
        if (StringUtils.length(key) < MIN_TERM_LENGTH) {
            return false;
        }
        return true;
    }

    public static boolean unmatchedChars(String key) {
        int openP = StringUtils.countMatches(key, "(");
        int closedP = StringUtils.countMatches(key, ")");
        int openB = StringUtils.countMatches(key, "[");
        int closedB = StringUtils.countMatches(key, "]");
        int quote = StringUtils.countMatches(key, "\"");

        if (openP != closedP) {
            return true;
        }

        if (openB != closedB) {
            return true;
        }
        
        if (quote % 2 != 0) {
            return true;
        }

        return false;
    }

    public void appendOcurrenceMap(String name, int pos) {
        String key = cleanString(name);

        if (!stringValid(key)) {
            return;
        }
        ocur.put(key, ocur.getOrDefault(key, new TermWrapper()).increment());
    }

    public void printInOccurrenceOrder() {
        int avg = 0;
        Map<String, TermWrapper> multiWord = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        // split into cleaned single and multi-word phrase
        for (Entry<String, TermWrapper> entry : ocur.entrySet()) {
            TermWrapper value = entry.getValue();
            String key = entry.getKey();
            key = cleanString(key);
            avg += value.getOccur();
            int numSpaces = StringUtils.countMatches(key, " ");
            if (numSpaces < SKIP_PHRASES_LONGER_THAN) {
                multiWord.put(key, value);
            }
        }
        if (ocur.size() > 0) {
            avg = avg / ocur.size();
        }

        WordOverlapAnalyzer multi = new WordOverlapAnalyzer(multiWord.keySet());
        Map<String, List<String>> analyze = multi.analyze(multiWord.keySet());
        for (Entry<String,List<String>> entry : analyze.entrySet()) {
            TermWrapper keyWrapper = multiWord.get(entry.getKey());
            for (String val : entry.getValue()) {
                TermWrapper vw = multiWord.get(val);
                vw.setTerm(val);
                keyWrapper.combine(vw);
                multiWord.remove(val);
            }
        }
        
        if (PERSON.equals(type)) {
            PersonalNameOverwrapAnalyzer overlap = new PersonalNameOverwrapAnalyzer();
            overlap.combineMultiWordOverlap(multiWord);
        }

        
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
                        System.out.println(header + key + " | " + val);
                    }
                } else {
                    // System.out.println(header + "| " + val);
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

            List<String> results = reverse.getOrDefault(weightedOccurrence, new ArrayList<String>());
            total += weightedOccurrence;
            results.add(key);
            reverse.put(weightedOccurrence, results);
        }
        if (multiWord.size() > 0) {
            return total / multiWord.size();
        }
        return 0;
    }

  
    /**
     * What % of the term is made up of numbers
     * 
     * @param header
     * @return
     */
    public static int percentNumberWords(String header) {
        String[] words = header.split(" ");
        int letterCount = 0;
        for (String w : words) {
            if (w.matches("^[\\d\\.]*$")) {
                letterCount++;
            }
        }
        return toPercent(letterCount, words.length);
    }

    /**
     * Find the % of characters in a phrase that are numbers (eg. 42 1/22/27 V)
     * 
     * @param key
     * @return
     */
    public static int percentNumericCharacters(String key) {
        int numCount = 0;
        int totalCount = 0;
        for (char c : key.toCharArray()) {
            if (Character.isDigit(c)) {
                numCount++;
                totalCount++;
                continue;
            }
            if (Character.isAlphabetic(c)) {
                totalCount++;
            }
        }
        // System.out.println("total: "+ totalCount + " num:" + numCount);
        return toPercent(numCount, totalCount);
    }

    private static int toPercent(int numCount, int totalCount) {
        return (int) (((float) numCount / (float) totalCount) * 100);
    }

    /**
     * Does the string contain a word to ignore
     * 
     * @param val
     * @return
     */
    public static boolean containsStopWord(String val) {
        for (String term : stopWords) {
            if (StringUtils.containsIgnoreCase(val, term)) {
                return true;
            }
        }
        return false;
    }

    /**
     * return the % of 1 ltter words in the name
     * 
     * @param header
     * @return
     */
    public static int percentOneLetter(String header) {
        String[] words = header.split(" ");
        int letterCount = 0;
        for (String w : words) {
            if (w.length() == 1) {
                letterCount++;
            }
        }
        return toPercent(letterCount,words.length);
    }


    /**
     * Cleans text: Strips newlines and tags
     * 
     * @param key
     * @return
     */
    public String cleanString(String key) {
        key = key.replaceAll("[\r\n]", "");
        key = key.replaceAll("\\s+", " ").trim();
        key = key.replaceAll("['’]s", "");
        if (key.indexOf("<") > -1) {
            key = key.substring(0, key.indexOf("<"));
        }
        if (key.indexOf(">") > -1) {
            key = key.substring(0, key.indexOf(">"));
        }
        return key.trim();
    }

    public String getType() {
        return type;
    }

}
