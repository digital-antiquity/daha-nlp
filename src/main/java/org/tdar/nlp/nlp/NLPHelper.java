package org.tdar.nlp.nlp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tdar.nlp.Utils;

import opennlp.tools.namefind.TokenNameFinderModel;

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

    public static String[] stopWords = { "investigation", "catalog", "and", "or", "appendix", "submitted", "expection", "feature", "figures", "table", "page",
            "figure", "below", "collection", "indeterminate", "unknown", "not", "comments", "available", "count", "miles", "feet", "acres", "inches", "photo",
            "zone", "Miscellaneous" };
    private String regexBoost = null;
    private List<String> boostValues = new ArrayList<>();
    private List<String> skipPreviousTerms = new ArrayList<>();
    public static final String NUMERIC_PUNCTUATION = "^[\\d\\s/" + Utils.PUNCTUATION + "]+$";
    public static final int MIN_TERM_LENGTH = 3;
    public static final boolean REMOVE_HTML_TERMS = true;

    private String type;
    private int minPercentOneLetter = 50;
    private int minPercentNumberWords = 50;
    private int minPercentNumbers = 50;
    private int maxPercentNonAscii = 50;
    private int minTermLength = MIN_TERM_LENGTH;

    private List<TokenNameFinderModel> models;

    private List<String> ignoreTerms = new ArrayList<>();;

    public NLPHelper(String type, TokenNameFinderModel... models) {
        this.type = type;
        this.models = Arrays.asList(models);
    }

    public boolean stringValid(String key) {
        if ((StringUtils.contains(key, "=\"") || StringUtils.contains(key, "=\'")) && REMOVE_HTML_TERMS) {
            return false;
        }

        // see http://citeseerx.ist.psu.edu/viewdoc/download?doi=10.1.1.81.8901&rep=rep1&type=pdf for more ideas
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

        if (Utils.percentNumericCharacters(key) > getMinPercentNumbers()) {
            return false;
        }

        if (percentNonAsciiCharacters(key) < getMaxPercentNonAscii()) {
            return false;
        }

        for (String part : key.split(" ")) {
            for (String term : ignoreTerms) {
                if (part.equalsIgnoreCase(term.toLowerCase())) {
                    return false;
                }
            }
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
        return Utils.toPercent(numOk, total);
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
        return Utils.toPercent(letterCount, words.length);
    }

    /**
     * Does the string contain a word to ignore
     * 
     * @param val
     * @return
     */
    public static boolean containsStopWord(String val) {
        String match = ".*\\b(" + StringUtils.join(stopWords, "|") + ")\\b.*".toLowerCase();
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
        return Utils.toPercent(letterCount, words.length);
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

    public List<TokenNameFinderModel> getModels() {
        return models;
    }

    public void setModels(List<TokenNameFinderModel> models) {
        this.models = models;
    }

    public void setTermIgnore(List<String> asList) {
        this.ignoreTerms = asList;
        // TODO Auto-generated method stub

    }

}
