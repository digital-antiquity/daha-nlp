package org.tdar.nlp.train;

import java.util.Comparator;

public class ReverseStringLengthComparator implements Comparator<String> {

    @Override
    public int compare(String o1, String o2) {
        if (o1.length() == o2.length()) {
            return o1.compareTo(o2);
        }
        if (o1.length() > o2.length()) {
            return -1;
        } else {
            return 1;
        }
    }
}
