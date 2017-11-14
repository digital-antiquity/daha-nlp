package org.tdar.nlp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class NlpDocument {
    private final Logger log = LogManager.getLogger(getClass());

    List<Page> pages = new ArrayList<>();
    List<Page> frontmatter = new ArrayList<>();
    private List<Page> tableOfContents = new ArrayList<>();
    List<Page> bibliography = new ArrayList<>();
    List<Page> body = new ArrayList<>();
    int totalBib = 0;
    int totalToc = 0;
    private List<NLPHelper> helpers = new ArrayList<>();

    private List<Page> getPages() {
        return pages;
    }

    public List<Page> getFrontmatter() {
        return frontmatter;
    }

    public List<Page> getBibliography() {
        return bibliography;
    }

    public List<Page> getBody() {
        return body;
    }

    public void generateTableOfContents() {
        int start = -1;
        int end = -1;
        boolean seenLowEnd = false;
        int avg = totalToc / pages.size() / 3;
        for (int i = 0; i < pages.size() / 2 ; i++) {
            Page page = pages.get(i);
            log.trace("page: " + i + " start: " + start + " end: " + end + " avg: "+ avg + " rank: " + page.getTocRank());
            if (start < 0 && page.getTocRank() > avg) {
                start = i;
            }
            if (start >= 0 && page.getTocRank() > avg && seenLowEnd == false) {
                end = i;
            }
            if (end > 0 && page.getTocRank() < avg) {
                seenLowEnd = true;
            }
        }
        log.debug("toc start: " + start + " toc end:" + end + " avg:" + avg + " totalToc:" + totalToc);

        if (start > 0) {
            for (int i = 0; i < start; i++) {
                Page page = pages.get(i);
                frontmatter.add(page);
                body.remove(page);
            }
        }
        if (end > start) {
            for (int i = start; i <= end; i++) {
                Page page = pages.get(i);
                tableOfContents.add(page);
                body.remove(page);
            }
        }
    }

    public void addPage(Page page) {
        pages.add(page);
        totalToc += page.getTocRank();
        totalBib += page.getTotalReferences().get(Page.ALL);
    }

    public void generateBibliography() {
        int avg = totalBib / pages.size();
        int bib = -1;
        int bibPoint = pages.size() - pages.size() / 4;
        if (bibPoint != pages.size()) {
            for (int i = bibPoint; i >= 0; i--) {
                if (pages.get(i).getTotalReferences().get(Page.ALL) > avg) {
                    bib = i;
                } else if (bib > 0) {
                    break;
                }
            }
        }
        log.debug("BibStart:" + bib);

        for (int i = bib; i < pages.size(); i++) {
            Page page = pages.get(i);
            body.remove(page);
            bibliography.add(page);
        }
    }

    public List<Page> getTableOfContents() {
        return tableOfContents;
    }

    public void analyze() {
        body.addAll(pages);
        generateTableOfContents();
        generateBibliography();

    }

    public void printResults() {
        printResults(getFrontmatter(), SectionType.FRONT);
        printResults(getBody(), SectionType.BODY);
    }

    private void printResults(List<Page> _pages, SectionType type) {
        log.debug(":::::::::::::::: " + type + " ("+ _pages.size() +") ::::::::::::::::");
        Map<String, Integer> siteCodes = new HashMap<>();
        boolean runSiteCodes = false;
        for (NLPHelper helper : helpers) {
            ResultAnalyzer result = new ResultAnalyzer(helper);
            if (type == SectionType.BODY) {
                result.setMinProbability(.9);
            }
            if (siteCodes.isEmpty()) {
                runSiteCodes = true;
            }
            for (Page page : _pages) {
                result.addPage(page);
                if (runSiteCodes) {
                    for (Entry<String, Integer> entry : page.getSiteCodes().entrySet()) {
                        Integer integer = siteCodes.getOrDefault(entry.getKey(), 0);
                        siteCodes.put(entry.getKey(), integer + entry.getValue());
                    }
                }
            }
            runSiteCodes = false;
            result.printOccurrence();
        }
        printSiteCodes(siteCodes);
    }

    private void printSiteCodes(Map<String, Integer> siteCodes) {
        Map<Integer, List<String>> rev = new HashMap<>();
        for (Entry<String, Integer> entry : siteCodes.entrySet()) {
            List<String> key = rev.get(entry.getValue());
            if (key == null) {
                key = new ArrayList<>();
            }
            key.add(entry.getKey());
            rev.put(entry.getValue(), key);
        }

        List<Integer> keys = new ArrayList<>(rev.keySet());
        Collections.sort(keys, Collections.reverseOrder());
        int maxSiteCodeNum = 10;
        for (Integer key : keys) {
            log.debug("site code " + key + " |" + rev.get(key));
            maxSiteCodeNum--;
            if (maxSiteCodeNum < 0) {
                break;
            }
        }
    }

    public List<NLPHelper> getHelpers() {
        return helpers;
    }

    public void setHelpers(List<NLPHelper> helpers) {
        this.helpers = helpers;
    }
}
