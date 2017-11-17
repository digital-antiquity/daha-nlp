package org.tdar.nlp.result;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect
public class Section implements Serializable {

    private static final long serialVersionUID = -4827733043559740169L;
    private SectionType type;
    private int startPage;
    private int endPage;
    private Map<String, Map<String, Integer>> results = new HashMap<>();

    public Section(SectionType type) {
        this.type = type;
    }

    public SectionType getType() {
        return type;
    }

    public void setType(SectionType type) {
        this.type = type;
    }

    public int getStartPage() {
        return startPage;
    }

    public void setStartPage(int startPage) {
        this.startPage = startPage;
    }

    public int getEndPage() {
        return endPage;
    }

    public void setEndPage(int endPage) {
        this.endPage = endPage;
    }

    public Map<String, Map<String, Integer>> getResults() {
        return results;
    }

    public void setResults(Map<String, Map<String, Integer>> results) {
        this.results = results;
    }

}
