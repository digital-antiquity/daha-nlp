package org.tdar.nlp.nlp;

import opennlp.tools.util.Span;

public class SpanWrapper {

    private Span span;
    private String text;
    private String type;
    private int pos;
    private double prob;

    public SpanWrapper(Span span, String text, int pos, String type) {
        this.span = span;
        this.prob = span.getProb();
        this.text = text;
        this.pos = pos;
        this.type = type;
    }

    public Span getSpan() {
        return span;
    }

    public void setSpan(Span span) {
        this.span = span;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return String.format("%s [%s-%s] %s", getText(), getSpan().getStart(), getSpan().getEnd(), getType());
    }

    public int getPos() {
        return pos;
    }

    public void setPos(int pos) {
        this.pos = pos;
    }

    public void addProb(double prob) {
        this.prob += prob;
    }

    public double getProb() {
        return prob;
    }

    public void setProb(double prob) {
        this.prob = prob;
    }
}
