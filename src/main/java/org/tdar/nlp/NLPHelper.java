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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

public class NLPHelper {

    public static void printInOccurrenceOrder(String key2, Map<String, Integer> ocur) {
        Map<Integer, List<String>> reverse = new HashMap<Integer, List<String>>();
        int avg = 0;
        Map<String, Integer> singles = new TreeMap<String, Integer>(String.CASE_INSENSITIVE_ORDER);
        Map<String, Integer> multiWord = new TreeMap<String, Integer>(String.CASE_INSENSITIVE_ORDER);

        // split into cleaned single and multi-word phrase
        for (Entry<String, Integer> entry : ocur.entrySet()) {
            Integer value = entry.getValue();
            String key = entry.getKey();
            key = cleanString(key);
            avg += value;
            if (key.contains(" ")) {
                multiWord.put(key, value);
            } else {
                singles.put(key, value);
            }
        }
        avg = avg / ocur.size();

        // for each single word phrase, try and fit it into a multi-word and combine
        Set<String> toRemove = new HashSet<String>();
        Iterator<Entry<String, Integer>> iterator = singles.entrySet().iterator();
        while (iterator.hasNext()) {
            Entry<String, Integer> entry = iterator.next();
            String txt = entry.getKey();
            for (String key : multiWord.keySet()) {
                // note this will inappropriately boost one last name match over another, eg. Adam Smith, and Steve Smith, with Smith
                if (stripClean(key).contains(stripClean(txt))) {
                    toRemove.add(txt);
                    multiWord.put(key, multiWord.get(key) + entry.getValue());
                }
            }
        }
        for (String key : toRemove) {
            singles.remove(key);
        }

        multiWord.putAll(singles);
        toRemove.clear();
        Set<String> set2 = new HashSet<>(multiWord.keySet());
        for (String key : multiWord.keySet()) {
            String cl = stripClean(key);
            String init = "";
            String init2 = "";
            // -------------------------- 1 -- 2 ------- 3 -------- 4
            Pattern p = Pattern.compile("(\\w)(\\w*)\\s(\\w\\.)\\s(\\w+)");
            Matcher matcher = p.matcher(cl);
            if (matcher.matches()) {
                // First Last (w/o initial)
                init = matcher.group(1) + matcher.group(2) + " " + matcher.group(4);
                // F. Last
                init = matcher.group(1) + " " + matcher.group(4);
            }
            for (String keyi : set2) {
                if (keyi.equals(key) || toRemove.contains(keyi)) {
                    continue;
                }
                int dist = StringUtils.getLevenshteinDistance(cl, stripClean(keyi));
                if (keyi.equals(init) || keyi.equals(init2) || dist < 2 && keyi.length() > 5) {
                    toRemove.add(keyi);
                    multiWord.put(key, multiWord.get(key) + multiWord.get(keyi));
                }
            }
        }

        for (String key : toRemove) {
            multiWord.remove(key);
        }

        // reverse the hash by count
        for (Entry<String, Integer> entry : multiWord.entrySet()) {
            int value = entry.getValue();
            String key = entry.getKey();
            value += 10 * StringUtils.countMatches(key, " ");
            List<String> results = reverse.get(value);
            if (results == null) {
                results = new ArrayList<String>();
            }
            results.add(key);
            reverse.put(value, results);
        }

        // output
        List<Integer> list = new ArrayList<Integer>(reverse.keySet());
        Collections.sort(list);
        Collections.reverse(list);
        for (Integer key : list) {
            for (String val : reverse.get(key)) {
                String header = key2;
                if (StringUtils.isNotBlank(key2)) {
                    header += " ";
                }
                if (key > avg) {
                    System.out.println(header + key + " | " + val);
                } else {
                    // System.out.println(header + "| " + val);
                }
            }
        }
    }

    private static String stripClean(String key) {
        String mat = key.toLowerCase().replaceAll("[^\\w\\s]", "");
        // // remove initials Richard P Morris --> Richard Morris
        // mat = mat.replace("\\s\\w\\s", " ");
        // // remove initials |R Morris --> Morris
        // mat = mat.replace("^\\w\\s", " ");
        // // remove initials Morris i --> Morris
        // mat = mat.replace("\\s\\w$", " ");
        return mat;
    }

    public static String cleanString(String key) {
        key = key.replaceAll("[\r\n]", "");
        key = key.replaceAll("\\s+", " ").trim();
        return key;
    }

}
