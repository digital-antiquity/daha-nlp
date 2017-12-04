package org.tdar.nlp.train;

import java.util.HashSet;
import java.util.Set;

public class SentenceResult {

    private Set<String> tags = new HashSet<>();
    private String sentence;
    private String taggedSentence;
    public Set<String> getTags() {
        return tags;
    }
    public void setTags(Set<String> tags) {
        this.tags = tags;
    }
    public String getSentence() {
        return sentence;
    }
    public void setSentence(String sentence) {
        this.sentence = sentence;
    }
    public String getTaggedSentence() {
        return taggedSentence;
    }
    public void setTaggedSentence(String taggedSentence) {
        this.taggedSentence = taggedSentence;
    }
    
    
}
