package org.tdar.nlp;

public class Utils {
    public static final String PUNCTUATION = "\\.;\\(\\)\\[\\]\\?\\-\\_\\,\\^\\&°£»\\|\\*\\/’\"\'«";

    public static int toPercent(int numCount, int totalCount) {
        return (int) (((float) numCount / (float) totalCount) * 100);
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
        return firstLetter.matches("["+PUNCTUATION+"]");
    }

}
