package org.tdar.nlp.result;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.tdar.nlp.TermWrapper;
import org.tdar.nlp.document.NlpPage;

public class Page implements Serializable {

    private static final long serialVersionUID = -2463082072595100301L;
    private Integer pageNumber;
    private Map<String, Map<String, TermWrapper>> data = new HashMap<>();
    private int tocRank = 0;
    private Map<String, Integer> siteCodes = new HashMap<>();
    private Map<String, Integer> totalTokens;

    public Page(NlpPage page) {
        this.pageNumber = page.getPageNumber();
        this.data = page.getReferences();
        this.tocRank = page.getTocRank();
        this.totalTokens = page.getTotalReferences();
        this.siteCodes = page.getSiteCodes();
    }

    public Integer getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(Integer pageNumber) {
        this.pageNumber = pageNumber;
    }

    public Map<String, Map<String, TermWrapper>> getData() {
        return data;
    }

    public void setData(Map<String, Map<String, TermWrapper>> data) {
        this.data = data;
    }

    public int getTocRank() {
        return tocRank;
    }

    public void setTocRank(int tocRank) {
        this.tocRank = tocRank;
    }

    public Map<String, Integer> getSiteCodes() {
        return siteCodes;
    }

    public void setSiteCodes(Map<String, Integer> siteCodes) {
        this.siteCodes = siteCodes;
    }

    public Map<String, Integer> getTotalTokens() {
        return totalTokens;
    }

    public void setTotalTokens(Map<String, Integer> totalTokens) {
        this.totalTokens = totalTokens;
    }

}
