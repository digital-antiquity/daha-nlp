package org.tdar.nlp.document;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.collections.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tdar.nlp.nlp.NLPHelper;
import org.tdar.nlp.nlp.ResultAnalyzer;
import org.tdar.nlp.result.ResultDocument;
import org.tdar.nlp.result.Section;
import org.tdar.nlp.result.SectionType;

public class NlpDocument {
    private static final String SITECODES2 = "siteCodes";

    private final Logger log = LogManager.getLogger(getClass());

    private List<NlpPage> pages = new ArrayList<>();
    private List<NlpPage> frontmatter = new ArrayList<>();
    private List<NlpPage> tableOfContents = new ArrayList<>();
    private List<NlpPage> bibliography = new ArrayList<>();
    private List<NlpPage> body = new ArrayList<>();
    private int totalBib = 0;
    private int totalToc = 0;
    private List<NLPHelper> helpers = new ArrayList<>();

    private ResultDocument resultDocument;

    private String filename;

    public NlpDocument(String filename) {
        this.setFilename(filename);
    }

    public List<NlpPage> getPages() {
        return pages;
    }

    public List<NlpPage> getFrontmatter() {
        return frontmatter;
    }

    public List<NlpPage> getBibliography() {
        return bibliography;
    }

    public List<NlpPage> getBody() {
        return body;
    }

    public void generateTableOfContents() {
        int start = -1;
        int end = -1;
        boolean seenLowEnd = false;
        if (CollectionUtils.isEmpty(pages) || pages.size()  < 3) {
            return;
        }
        
        // for normal documents with a TOC, the expectation is that the toc will be heavily weighted in the front
        // for a compendium of tables or FORMS the average may be quite even across all pages, so above average
        // is less useful unless we BOOST the average higher
        int avg = (totalToc / pages.size()) * 5;
        // arbitrary guess
        if (avg == 0) {
            avg = 5;
        }
        for (int i = 0; i < pages.size() / 2; i++) {
            NlpPage page = pages.get(i);
            log.trace("page: " + i + " start: " + start + " end: " + end + " avg: " + avg + " rank: " + page.getTocRank());
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

        // treat page 1 & 2 as "frontmatter"
        if (start == -1) {
            start = 2;
        }
        if (start > 20) {
            return;
        }
        if (start > 0) {
            for (int i = 0; i < start; i++) {
                NlpPage page = pages.get(i);
                frontmatter.add(page);
                body.remove(page);
            }
        }
        if (end > start) {
            for (int i = start; i <= end; i++) {
                NlpPage page = pages.get(i);
                tableOfContents.add(page);
                body.remove(page);
            }
        }
    }

    public void addPage(NlpPage page) {
        pages.add(page);
        totalToc += page.getTocRank();
        totalBib += page.getTotalReferences().get(NlpPage.ALL);
    }

    public void generateBibliography() {
        if (CollectionUtils.isEmpty(pages)) {
            return;
        }
        int avg = totalBib / pages.size();
        int bib = -1;
        int below = 0;
        int bibPoint = pages.size() - pages.size() / 4;
        int declaredBib = -1;
        if (bibPoint != pages.size()) {
            for (int i = bibPoint - 1; i < pages.size(); i++) {
                Integer num = pages.get(i).getTotalReferences().get(NlpPage.ALL);
                if (num > avg && bib == -1) {
                    bib = i;
                }

                // if we go down again, count it
                if (num < avg) {
                    below++;
                }

                // if below > 2, then reset
                if (below > 2 && pages.size() - i > 10) {
                    below = 0;
                    bib = -1;
                }
                
                if (pages.get(i).isBibliography()) {
                    bib = i;
                    // get "earliest" declared bib
                    if (declaredBib == -1) {
                        declaredBib = i;
                    }
                }
            }
        }
        
        // if the declared bib is < actual bib, or declared bib has been found; then use that
        if (bib == -1 && declaredBib > 0 || declaredBib < bib) {
            bib = declaredBib;
        }
        
        log.debug("BibStart:" + bib + " avg:" + avg);
        if (bib == -1) {
            return;
        }
        for (int i = bib; i < pages.size(); i++) {
            NlpPage page = pages.get(i);
            body.remove(page);
            bibliography.add(page);
        }
    }

    public List<NlpPage> getTableOfContents() {
        return tableOfContents;
    }

    public void analyze() {
        body.addAll(pages);
        generateTableOfContents();
        generateBibliography();

    }

    public ResultDocument printResults() {
        resultDocument = new ResultDocument(this);
        printResults(getFrontmatter(), SectionType.FRONT);
        printResults(getTableOfContents(), SectionType.TOC);
        printResults(getBody(), SectionType.BODY);
        printResults(getBibliography(), SectionType.BIBLIOGRPAHY);
        return resultDocument;
    }

    private void printResults(List<NlpPage> _pages, SectionType type) {
        log.debug(":::::::::::::::: " + type + " (" + _pages.size() + ") ::::::::::::::::");
        Map<String, Integer> siteCodes = new HashMap<>();
        boolean runSiteCodes = false;

        if (CollectionUtils.isNotEmpty(_pages)) {
            Section section = new Section(type);
            section.setStartPage(_pages.get(0).getPageNumber());
            section.setEndPage(_pages.get(_pages.size() - 1).getPageNumber());
            if (section.getType() == SectionType.FRONT) {
                for (NlpPage page :_pages) {
                    section.addSentences(page.getSentences());
                }
            }
            resultDocument.getSections().add(section);
            for (NLPHelper helper : helpers) {
                ResultAnalyzer result = new ResultAnalyzer(helper);
                log.trace(helper.getType());
                if (type == SectionType.BODY) {
                    result.setMinProbability(.8);
                }
                if (siteCodes.isEmpty()) {
                    runSiteCodes = true;
                }

                for (NlpPage page : _pages) {
                    result.addPage(page);
                    if (runSiteCodes) {
                        for (Entry<String, Integer> entry : page.getSiteCodes().entrySet()) {
                            Integer integer = siteCodes.getOrDefault(entry.getKey(), 0);
                            siteCodes.put(entry.getKey(), integer + entry.getValue());
                        }
                    }
                }
                runSiteCodes = false;
                result.printOccurrence(section);
            }
            printSiteCodes(siteCodes, section);
        }
    }

    private void printSiteCodes(Map<String, Integer> siteCodes, Section section) {
        Map<Integer, List<String>> rev = new HashMap<>();
        Map<String, Integer> results = section.getResults().getOrDefault(SITECODES2, new HashMap<String, Integer>());

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
            for (String v : rev.get(key)) {
                results.put(v, key);
            }
            maxSiteCodeNum--;
            if (maxSiteCodeNum < 0) {
                break;
            }
        }
        section.getResults().put(SITECODES2, results);

    }

    public List<NLPHelper> getHelpers() {
        return helpers;
    }

    public void setHelpers(List<NLPHelper> helpers) {
        this.helpers = helpers;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }
}
