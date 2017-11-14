package org.tdar.nlp;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.tdar.utils.SiteCodeExtractor;

public class Page {
    public static final String ALL = "ALL";
    private Integer pageNumber;
    private Map<String, Map<String, TermWrapper>> data = new HashMap<>();
    private int tocRank = 0;
    private List<String> tocAnchors = Arrays.asList("chapter", "table", "figure", "appendix", "list of","chapter o", "chapter t", "chapter 1","chapter 2");
    private Map<String, Integer> siteCodes = new HashMap<>();

    
    public Page(Integer pageNumber) {
        this.pageNumber = pageNumber;
    }

    public Integer getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(Integer pageNumber) {
        this.pageNumber = pageNumber;
    }

    public Map<String, Map<String, TermWrapper>> getReferences() {
        return data;
    }

    public boolean appendOcurrenceMap(String name, int pos, double probability, NLPHelper helper) {
        String key = Utils.cleanString(name);

        if (!helper.stringValid(key)) {
            return false;
        }
        String type = helper.getType();
        Map<String, TermWrapper> map = data.getOrDefault(type, new HashMap<String, TermWrapper>());

        map.put(key, map.getOrDefault(key, new TermWrapper()).increment(probability));
        data.put(type, map);
        return true;
    }

    Map<String, Integer> totals;

    public Map<String, Integer> getTotalReferences() {
        if (totals != null) {
            return totals;
        }
        totals = new HashMap<>();
        totals.put(ALL, 0);

        for (String key : data.keySet()) {
            int total = 0;
            Map<String, TermWrapper> entries = data.get(key);
            for (String entry : entries.keySet()) {
                total += entries.get(entry).getOccur();
            }
            totals.put(key, total);
            totals.put(ALL, totals.getOrDefault(ALL, 0) + total);
        }
        return totals;
    }

    public void addSentence(String sentence) {
        addTocRank(sentence);

    }

    private void addTocRank(String sentence) {
        String lc = sentence.toLowerCase();
        int chapter = StringUtils.countMatches(lc, "chapter");
        int tables = StringUtils.countMatches(lc, "table");
        tables = +StringUtils.countMatches(lc, "table of contents") * 10;
        int figure = StringUtils.countMatches(lc, "figure");
        int listOf = StringUtils.countMatches(lc, "list of ") * 5;
        chapter = StringUtils.countMatches(lc, "chapter one");
        chapter += StringUtils.countMatches(lc, "chapter 1");
        chapter += StringUtils.countMatches(lc, "chapter t");
        chapter += StringUtils.countMatches(lc, "chapter 2");
        int dot = StringUtils.countMatches(lc, "....");
        dot += StringUtils.countMatches(lc, ". . . .");
        dot += StringUtils.countMatches(lc, " - - - - -");
        dot += StringUtils.countMatches(lc, "-----");
        dot += StringUtils.countMatches(lc, "______");
        setTocRank(getTocRank() + chapter + tables + figure + listOf + dot);
    }

    public int getTocRank() {
        return tocRank;
    }

    public void setTocRank(int tocRank) {
        this.tocRank = tocRank;
    }

    public void extractSiteCodes(String sentence) {
        for (String code : SiteCodeExtractor.extractSiteCodeTokens(sentence, false)) {
            Integer integer = getSiteCodes().get(code);
            if (integer == null) {
                integer = 1;
            } else {
                integer = integer + 1;
            }
            getSiteCodes().put(code, integer);
        }
    }

    public Map<String, Integer> getSiteCodes() {
        return siteCodes;
    }

    public void setSiteCodes(Map<String, Integer> siteCodes) {
        this.siteCodes = siteCodes;
    }
}
