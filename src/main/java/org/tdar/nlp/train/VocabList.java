package org.tdar.nlp.train;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.jena.atlas.logging.Log;
import org.tdar.nlp.Utils;

public class VocabList {

    private SortedSet<String> list = new TreeSet<>(new ReverseStringLengthComparator());

    public VocabList(FileInputStream fileInputStream) throws IOException {

        String types = IOUtils.toString(fileInputStream,Charset.defaultCharset());

        list.addAll(Arrays.asList(types.split("[\r\n]")));
        list.removeAll(Collections.singleton(""));
        
        List<String> toAdd = new ArrayList<>();
        // handle "period" which can be both cases
        List<String> toRemove = new ArrayList<>();
        for (String entry : list) {
            if (entry.trim().startsWith("#")) {
                toRemove.add(entry);
            }
            String t_ = Utils.replaceSmartQuotes(entry);
            if (!entry.equals(t_)) {
                toAdd.add(t_);
            }
            if (t_.toLowerCase().contains("period")) {
                toAdd.add(StringUtils.replace(t_, "Period", "period"));
            }
            toAdd.add(t_.replaceAll("\\s\\w\\.\\s", " "));
        }
        list.addAll(toAdd);
        list.removeAll(toRemove);

        toAdd.clear();
        for (String entry : list) {
            if (entry.contains(" ")) {
                toAdd.add(entry.replace(" ", "_"));
                toAdd.add(entry.replace("-", "_"));
            }
        }
    }
    
    

    public SortedSet<String> getList() {
        return list;
    }


}
