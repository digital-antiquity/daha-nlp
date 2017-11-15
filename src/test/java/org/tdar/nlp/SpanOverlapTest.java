package org.tdar.nlp;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import opennlp.tools.util.Span;

public class SpanOverlapTest {

    @Test
    public void test() {
        List<SpanWrapper> spans = new ArrayList<>();
        spans.add(new SpanWrapper(new Span(0, 2, "person", 100), "Omar Turney", 1, "person"));
        spans.add(new SpanWrapper(new Span(15,16, "location", 100), "Flagstaff", 1,  "location"));
        spans.add(new SpanWrapper(new Span(20,21, "culture", 100), "Hohokam",  1, "culture"));
        spans.add(new SpanWrapper(new Span(0, 2, "person", 100), "Omar Turney",  1, "person"));
        spans.add(new SpanWrapper(new Span(20,23, "institution", 100), "Hohokam High School",  1, "institution"));
        Page page = new Page(1);
        for (SpanWrapper sw : spans) {
            page.addSpan(sw.getSpan(), sw.getText(),  1, sw.getType());
        }
        page.reconcileSpans();
    }

}
