package org.tdar.nlp;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.Test;

public class WordCleanupTest {

public final String text = "\n" + 
        "Sample Collection ............................................................................................................................................................... 7\n" + 
        "Geologic B a c k g ro u n d .................................................................................................................................................................  9"
        + "M odel s\n"  +
        "1.30 M iddle Sacaton 1 time segment, bowl interiors and ja r exteriors ...............................................................................  432\n" + 
        "1.31 M iddle Sacaton 1 time segment, bowl interiors and ja r exteriors ...............................................................................  433\n" + 
        ""
        + "a d a m t e s t\n" + 
        "Development o f the Geologic Resource M o d e ls .................................................................................................................  10\n";

    private String[] dictionary = new String[] {"sample","collection", "geologic", "development", "background", "resource" , "development" ,"exteriors","time", "of", "a", "i", "the", "models","and", "middle","jar"};
    String term = "";
    int start = -1;
    int end = -1;

    @Test
    public void test() {
        String[] split = text.split("(\b|\\s|\r|\n|\\W|\\d)");
        for (int i=0; i< split.length; i++) {
            String find = split[i].trim().toLowerCase();
            if (StringUtils.isBlank(find)) {
                if (inDictionary(term)) {
                    System.out.println(term);
                }
                reset(term);
                continue;
            }

            boolean inDictionaryLookAhead = inDictionaryLookAhead(split, i+1 );
            boolean termInDic = inDictionary(term);
            if (inDictionary(find) || find.length() > 8) {
                if (termInDic) {
                    System.out.println(term);
                }

                if ((StringUtils.equals("a", find ) || StringUtils.equals(find , "i"))) {
                    if (inDictionaryLookAhead) {
                        reset(find);
                    } else {
                        if (termInDic) {
                            reset(term);
                        } else {
                            System.out.println("\t" + term + "["+find+"]");
                            term += find;
                            start = i;
                        }
                    }
                } else {
                    reset(find);
                }
            } else {
                if (inDictionary(term + find) && inDictionaryLookAhead && inDictionaryLookAhead(split, i+2)) {
                    reset(find+term);
                } else if (termInDic && !StringUtils.equals("a", term)){
                    reset(term);
                } else if (term.length() > 8) {
                    reset("");
                } else {
                    System.out.println("\t" + term + "["+find+"]");
                    term += find;
                    start = i;
                }
            }
        }
    }

    private boolean inDictionaryLookAhead(String[] split, int i) {
        if ( i >= split.length) {
            return false;
        }
        return inDictionary(split[i]);
    }

    private void reset(String phrase) {
        term = "";
        start = -1;
        end = -1;
        if (StringUtils.isNotBlank(phrase)) {
        System.out.println(phrase);
        }
    }

    private boolean inDictionary(String find) {
        return ArrayUtils.contains(dictionary, find);
    }

}
