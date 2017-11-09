package org.tdar.nlp;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.HashCodeBuilder;

public class TermWrapper {

    private String term;
    private List<TermWrapper> alternates = new ArrayList<>();
    private int pos;
    private int occur = 1;
    private List<Double> probability = new ArrayList<>();
    public String getTerm() {
        return term;
    }

    public void setTerm(String term) {
        this.term = term;
    }

    public int getPos() {
        return pos;
    }

    public void setPos(int pos) {
        this.pos = pos;
    }

    public int getOccur() {
        return occur;
    }

    public void setOccur(int occur) {
        this.occur = occur;
    }

    @Override
    public boolean equals(Object obj) {
        return Objects.equals(term, ((TermWrapper) obj).getTerm());
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(term);
    }

    public List<TermWrapper> getAlternates() {
        return alternates;
    }

    public void setAlternates(List<TermWrapper> alternates) {
        this.alternates = alternates;
    }

    public TermWrapper increment(double probability) {
        occur++;
        this.probability.add(probability);
        return this;
    }

    public TermWrapper combine(TermWrapper value) {
        occur += value.getOccur();
        alternates.add(value);
        alternates.addAll(value.getAlternates());
        return this;
    }

    public Integer getWeightedOccurrence() {
        int value = occur;
//        value += 10 * StringUtils.countMatches(term, " ");
        if (pos < 10) {
            return value + 100;
        }
        return value;
    }
}
