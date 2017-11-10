package org.tdar.nlp;

import java.util.HashMap;
import java.util.Map;

public class Page {
    public static final String ALL = "ALL";
    private Integer pageNumber;
    private Map<String, Map<String,TermWrapper>> data = new HashMap<>();

    public Page(Integer pageNumber) {
        this.pageNumber = pageNumber;
    }

    public Integer getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(Integer pageNumber) {
        this.pageNumber = pageNumber;
    }

    public boolean appendOcurrenceMap(String name, int pos, double probability, NLPHelper helper) {
        String key = helper.cleanString(name);

        if (!helper.stringValid(key)) {
            return false;
        }
        Map<String, TermWrapper> map = data.getOrDefault(helper.getType(), new HashMap<String,TermWrapper>());
        
        map.put(key, map.getOrDefault(key, new TermWrapper()).increment(probability));
        data.put(key, map);
        return true;
    }

    Map<String, Integer> totals;
    public Map<String, Integer> getTotalReferences() {
        if (totals != null) {
            return totals;
        }
        totals = new HashMap<>();
        totals.put(ALL,0);

        for (String key : data.keySet()) {
            int total = 0;
            Map<String, TermWrapper> entries = data.get(key);
            for (String entry : entries.keySet()) {
                total += entries.get(entry).getOccur();
            }
            totals.put(key, total);
            totals.put(ALL,totals.getOrDefault(ALL, 0) + total);
        }
        return totals;
    }

}
