package org.tdar.nlp.result;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.tdar.nlp.document.NlpDocument;
import org.tdar.nlp.document.NlpPage;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect
public class ResultDocument implements Serializable {

    private static final long serialVersionUID = -2774763526575249633L;
    private String filename;
    private String title;
    private List<Section> sections = new ArrayList<>();
    private List<Page> pages = new ArrayList<>();

    public ResultDocument(NlpDocument nlpDocument) {
        for (NlpPage page : nlpDocument.getPages()) {
            pages.add(new Page(page));
        }
        this.filename = nlpDocument.getFilename();
//        this.title = 
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<Section> getSections() {
        return sections;
    }

    public void setSections(List<Section> sections) {
        this.sections = sections;
    }

    public List<Page> getPages() {
        return pages;
    }

    public void setPages(List<Page> pages) {
        this.pages = pages;
    }

}
