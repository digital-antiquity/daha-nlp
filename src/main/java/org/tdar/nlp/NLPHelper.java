package org.tdar.nlp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

public class NLPHelper {
    private static final int SKIP_PHRASES_LONGER_THAN = 5;
    private static String[] stopWords = { "investigation", "catalog", " and ", " or ", "appendix", "submitted", "expection" };
    private static final String NUMERIC_PUNCTUATION = "^[\\d\\s/\\.;\\(\\)\\[\\]\\?\\-\\_\\,]+$";
    private static final int MIN_TERM_LENGTH = 3;
    private static final boolean REMOVE_HTML_TERMS = true;
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

        if (containsStopWord(key) && "person".equals(type)) {
            return false;
        }
        if (percentOneLetter(key) > 74 && "person".equals(type)) {
            return false;
        }
        if (percentNumber(key) > 50) {
            return false;
        }

        if (StringUtils.length(key) > MIN_TERM_LENGTH) {
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
        Map<String, TermWrapper> singles = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        Map<String, TermWrapper> multiWord = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        // split into cleaned single and multi-word phrase
        for (Entry<String, TermWrapper> entry : ocur.entrySet()) {
            TermWrapper value = entry.getValue();
            String key = entry.getKey();
            key = cleanString(key);
            avg += value.getOccur();
            int numSpaces = StringUtils.countMatches(key, " ");
            if (numSpaces > 0) {
                // Skip lots of words
                if (numSpaces < SKIP_PHRASES_LONGER_THAN) {
                    multiWord.put(key, value);
                }
            } else {
                singles.put(key, value);
            }
        }
        if (ocur.size() > 0) {
            avg = avg / ocur.size();
        }

        stripOverlappingSingleMultiWord(type, singles, multiWord);

        multiWord.putAll(singles);

        combineMultiWordOverlap(type, multiWord);

        Map<Integer, List<String>> reverse = sortByOccurrence(multiWord);

        printResults(type, avg, reverse);
    }

    private Set<String> stripOverlappingSingleMultiWord(String type, Map<String, TermWrapper> singles, Map<String, TermWrapper> multiWord) {
        Set<String> toRemove = new HashSet<String>();
        // only combine multi-> single words if we're dealing with people
        if (type.equalsIgnoreCase("Person")) {
            // for each single word phrase, try and fit it into a multi-word and combine
            Iterator<Entry<String, TermWrapper>> iterator = singles.entrySet().iterator();
            while (iterator.hasNext()) {
                Entry<String, TermWrapper> entry = iterator.next();
                String txt = entry.getKey();
                for (String key : multiWord.keySet()) {
                    // note this will inappropriately boost one last name match over another, eg. Adam Smith, and Steve Smith, with Smith
                    String parent = stripClean(key);
                    if (parent.toLowerCase().endsWith(" " + txt.toLowerCase())) {
                        toRemove.add(txt);
                        entry.getValue().setTerm(entry.getKey());
                        multiWord.get(key).combine(entry.getValue());
                    }
                }
            }
            for (String key : toRemove) {
                singles.remove(key);
            }
        }
        return toRemove;
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
                    if (type.equalsIgnoreCase("person") && Pattern.compile("\\d").matcher(val).find()) {
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
    private Map<Integer, List<String>> sortByOccurrence(Map<String, TermWrapper> multiWord) {
        Map<Integer, List<String>> reverse = new HashMap<>();
        // reverse the hash by count
        for (Entry<String, TermWrapper> entry : multiWord.entrySet()) {
            TermWrapper value = entry.getValue();
            String key = entry.getKey();
            Integer weightedOccurrence = value.getWeightedOccurrence();

            List<String> results = reverse.getOrDefault(weightedOccurrence, new ArrayList<String>());
            results.add(key);
            reverse.put(weightedOccurrence, results);
        }
        return reverse;
    }

    /**
     * Deal with cases like A. Brin A Brin and Adam Brin, and try and combine overlapping names, it's not exact and has issues like Smith, A. Smith, and B.
     * Smith...
     * 
     * @param type
     * @param multiWord
     */
    private void combineMultiWordOverlap(String type, Map<String, TermWrapper> multiWord) {
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
            if (type.equalsIgnoreCase("person")) {
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
     * What % of the term is made up of numbers
     * 
     * @param header
     * @return
     */
    private static int percentNumber(String header) {
        String[] words = header.split(" ");
        int letterCount = 0;
        for (String w : words) {
            if (w.matches("^[\\d\\.]*$")) {
                letterCount++;
            }
        }
        return (int) (((float) letterCount / (float) words.length) * 100);
    }

    /**
     * Does the string contain a word to ignore
     * 
     * @param val
     * @return
     */
    private static boolean containsStopWord(String val) {
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
    private static int percentOneLetter(String header) {
        String[] words = header.split(" ");
        int letterCount = 0;
        for (String w : words) {
            if (w.length() == 1) {
                letterCount++;
            }
        }
        return (int) (((float) letterCount / (float) words.length) * 100);
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
