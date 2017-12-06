package org.tdar.nlp;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.geotools.xml.styling.sldComplexType;

public class Utils {
    public static final String PUNCTUATION = "\\.;\\(\\)\\[\\]\\?\\-\\_\\,\\^\\&°£»\\|\\*\\/’\"\'«";

    public static int toPercent(int numCount, int totalCount) {
        return (int) (((float) numCount / (float) totalCount) * 100);
    }

    public static String replaceSmartQuotes(String str) {
        // See http://www.microsoft.com/typography/unicode/1252.htm
        String str_ = str.replaceAll("[\u0091\u0092\u2018\u2019]", "\'");
        str_ = str_.replaceAll("[\u0093\u0094\u201c\u201d]", "\"");
        return str_;
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
        return Utils.toPercent(numCount, totalCount);
    }

    public static boolean isPunctuation(String firstLetter) {
        return firstLetter.matches("[" + PUNCTUATION + "]");
    }

    public static boolean isTablePart(String key) {
        for (String part : StringUtils.split(key.toLowerCase())) {
            // if we wanted to, we could add 'type' here to check for : ,"ne","sw","nw" w/o location
            boolean containsTableTerm = ArrayUtils.contains(new String[] { "context", "total", "type", "class", "sequence", "comments","yes","no" }, part);
            if (containsTableTerm) {
                return true;
            }
        }
        return false;
    }

    public static boolean lastWordOneLetter(String key) {
        if (StringUtils.isBlank(key) || !key.contains(" ")) {
            return false;
        }
        String[] split = key.split(" ");

        if (split[split.length - 1].length() == 1) {
            return true;
        }
        return false;
    }

}
