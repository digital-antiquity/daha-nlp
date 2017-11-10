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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Notes:
 * * can we identify names in citations eg (Kent 2103) and organize or weight them differently?
 * * should we weight nested names for instititutions?
 * * should we weight occurrence when handling nested people?
 * 
 * @author abrin
 *
 */
public class NLPHelper {
    
    private final Logger log = LogManager.getLogger(getClass());

    private static final String PUNCTUATION = "\\.;\\(\\)\\[\\]\\?\\-\\_\\,\\^\\&°£»\\|\\*\\/’\"\'«";
    private static final String PERSON = "person";
    public static final int SKIP_PHRASES_LONGER_THAN = 5;
    public static String[] stopWords = { "investigation", "catalog", "and", "or", "appendix", "submitted", "expection", "feature","figures", "table","page", "figure", "below", "collection", "indeterminate","unknown", "not", "comments", "available", "count", "miles", "feet","acres","inches", "photo","zone" ,"Miscellaneous"};
    private List<String> boostValues = new ArrayList<>();
    private List<String> skipPreviousTerms = new ArrayList<>();
    public static final String NUMERIC_PUNCTUATION = "^[\\d\\s/" + PUNCTUATION + "]+$";
    public static final int MIN_TERM_LENGTH = 3;
    public static final boolean REMOVE_HTML_TERMS = true;
    private Map<String, TermWrapper> ocur = new HashMap<>();
    private String type;
    private String regexBoost = null;
    private int minPercentOneLetter = 50;
    private int minPercentNumberWords = 50;
    private int minPercentNumbers = 50;
    private int maxPercentNonAscii = 50;
    private int minTermLength = MIN_TERM_LENGTH;

    public NLPHelper(String type) {
        this.type = type;
    }

    boolean stringValid(String key) {
        if ((StringUtils.contains(key, "=\"") || StringUtils.contains(key, "=\'")) && REMOVE_HTML_TERMS) {
            return false;
        }

        if (key.matches(NUMERIC_PUNCTUATION)) {
            return false;
        }

        if (containsStopWord(key)) {
            return false;
        }
        int percentOneLetter = percentOneLetter(key);
        if (percentOneLetter > 74) {
            return false;
        }

        if (percentOneLetter > getMinPercentOneLetter()) {
            return false;
        }

        if (percentNumberWords(key) > getMinPercentNumberWords()) {
            return false;
        }

        if (percentNumericCharacters(key) > getMinPercentNumbers()) {
            return false;
        }

        if (percentNonAsciiCharacters(key) < getMaxPercentNonAscii()) {
            return false;
        }

        if (unmatchedChars(key)) {
            return false;
        }

        if (StringUtils.length(key) < getMinTermLength()) {
            return false;
        }
        return true;
    }

    private int percentNonAsciiCharacters(String key) {
        int numOk = 0;
        int total = key.length();
        for (char c : key.toCharArray()) {
            int ascii = (int) c;
            if (ascii > 64 && ascii < 91 || ascii > 96 && ascii < 123 || ascii == 32) {
                numOk++;
            }
        }
        return toPercent(numOk, total);
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
            return toPercent(total, multiWord.size());
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
        // log.debug("total: "+ totalCount + " num:" + numCount);
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
        String match = ".*\\b("+StringUtils.join(stopWords,"|")+")\\b.*".toLowerCase();
        if (val.toLowerCase().matches(match)) {
                return true;
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
        return toPercent(letterCount, words.length);
    }

    /**
     * Cleans text: Strips newlines and tags
     * 
     * @param key
     * @return
     */
    public static String cleanString(String key) {
        key = key.replaceAll("[\r\n]", "");
        key = key.replaceAll("\\s+", " ").trim();
        key = key.replaceAll("['’]s", "");
        if (key.indexOf("<") > -1) {
            key = key.substring(0, key.indexOf("<"));
        }
        if (key.indexOf(">") > -1) {
            key = key.substring(0, key.indexOf(">"));
        }
        key = key.trim().replaceAll("^[" + PUNCTUATION + "]\\s", "");
        key = key.trim().replaceAll("\\s[" + PUNCTUATION + "]$", "");
        return key.trim();
    }

    public String getType() {
        return type;
    }

    public int getMinTermLength() {
        return minTermLength;
    }

    public void setMinTermLength(int minTermLength) {
        this.minTermLength = minTermLength;
    }

    public int getMaxPercentNonAscii() {
        return maxPercentNonAscii;
    }

    public void setMaxPercentNonAscii(int maxPercentNonAscii) {
        this.maxPercentNonAscii = maxPercentNonAscii;
    }

    public int getMinPercentNumbers() {
        return minPercentNumbers;
    }

    public void setMinPercentNumbers(int minPercentNumbers) {
        this.minPercentNumbers = minPercentNumbers;
    }

    public int getMinPercentNumberWords() {
        return minPercentNumberWords;
    }

    public void setMinPercentNumberWords(int minPercentNumberWords) {
        this.minPercentNumberWords = minPercentNumberWords;
    }

    public int getMinPercentOneLetter() {
        return minPercentOneLetter;
    }

    public void setMinPercentOneLetter(int minPercentOneLetter) {
        this.minPercentOneLetter = minPercentOneLetter;
    }

    public List<String> getBoostValues() {
        return boostValues;
    }

    public void setBoostValues(List<String> boostValues) {
        this.boostValues = boostValues;
    }

    public String getRegexBoost() {
        return regexBoost;
    }

    public void setRegexBoost(String regexBoost) {
        this.regexBoost = regexBoost;
    }

    public List<String> getSkipPreviousTerms() {
        return skipPreviousTerms;
    }

    public void setSkipPreviousTerms(List<String> skipPreviousTerms) {
        this.skipPreviousTerms = skipPreviousTerms;
    }

}
