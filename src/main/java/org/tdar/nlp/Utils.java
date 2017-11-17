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
}
