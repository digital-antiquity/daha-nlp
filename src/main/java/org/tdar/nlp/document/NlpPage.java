package org.tdar.nlp.document;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tdar.nlp.TermWrapper;
import org.tdar.nlp.nlp.SpanWrapper;
import org.tdar.utils.SiteCodeExtractor;

import opennlp.tools.util.Span;

public class NlpPage {
    public static final String ALL = "ALL";
    private Integer pageNumber;
    private Map<String, Map<String, TermWrapper>> data = new HashMap<>();
    private int tocRank = 0;
    private Map<String, Integer> siteCodes = new HashMap<>();
    private List<String> tocAnchors = Arrays.asList("chapter", "table", "figure", "appendix", "list of", "chapter o", "chapter t", "chapter 1", "chapter 2");

    private final Logger log = LogManager.getLogger(getClass());

    public NlpPage(Integer pageNumber) {
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

    private boolean appendOcurrenceMap(SpanWrapper sw) {
        //String key, int pos, double probability, String type) {
        Map<String, TermWrapper> map = data.getOrDefault(sw.getType(), new HashMap<String, TermWrapper>());
        TermWrapper value = new TermWrapper(sw.getText());
        value.setPos(sw.getPos());
        map.put(sw.getText(), map.getOrDefault(sw.getText(), value).increment(sw.getProb()));
        data.put(sw.getType(), map);
        return true;
    }

    Map<String, Integer> totals;
    private List<SpanWrapper> spans = new ArrayList<>();

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
        reconcileSpans();

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
            Integer integer = getSiteCodes().getOrDefault(code, 0);

            integer = integer + 1;
            getSiteCodes().put(code, integer);
        }
    }

    public Map<String, Integer> getSiteCodes() {
        return siteCodes;
    }

    public void setSiteCodes(Map<String, Integer> siteCodes) {
        this.siteCodes = siteCodes;
    }

    public void addSpan(Span span, String text, int pos, String type) {
        this.spans.add(new SpanWrapper(span, text, pos, type));
    }

    @Override
    public String toString() {
        return String.format("page: %s tokens: %s toc: %s", pageNumber, totals.get(ALL), tocRank);
    }

    
    public void reconcileSpans() {

        // sort tokens by start, then end, to be able to compare overlaps
        Collections.sort(spans, new Comparator<SpanWrapper>() {

            @Override
            public int compare(SpanWrapper o1_, SpanWrapper o2_) {
                Span o1 = o1_.getSpan();
                Span o2 = o2_.getSpan();

                // prefer earlier start
                if (o1.getStart() < o2.getStart()) {
                    return -1;
                }

                // if start and end the same, prefer later end (we want the tag that's the longest)
                if (o1.getStart() == o2.getStart()) {
                    if (o1.getEnd() > o2.getEnd()) {
                        return -1;
                    }
                }
                
                if (o1.getStart() == o2.getStart() && o1.getEnd() == o2.getEnd()) {
                    return 0;
                }

                return 1;
            }
        });
        SpanWrapper current = null;
        List<SpanWrapper> toRemove = new ArrayList<>();
        for (SpanWrapper wrap : spans) {
            log.trace(wrap);
            if (current !=  null && (current.getSpan().contains(wrap.getSpan()) || current.getSpan().intersects(wrap.getSpan()))) {
                log.trace("removing: {} {} {}", wrap.getText(), wrap.getSpan().getProb(), current.getSpan().getProb());
                if (StringUtils.equals(current.getType() , wrap.getType())) {
                    current.addProb(wrap.getSpan().getProb());
                }
                toRemove.add(wrap);
            } else {
                current = wrap;
            }
        }
        log.trace("   spans: {}", spans);
        log.trace("toRemove: {}", toRemove);
        spans.removeAll(toRemove);
        log.trace("    done: {}", spans);
        for (SpanWrapper sw : spans) {
            appendOcurrenceMap(sw);
        }

        spans.clear();
    }
}
