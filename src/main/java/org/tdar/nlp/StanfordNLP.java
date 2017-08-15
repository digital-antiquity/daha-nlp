package org.tdar.nlp;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.FileUtils;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

//https://github.com/stanfordnlp/CoreNLP
public class StanfordNLP {
    // https://github.com/drewfarris/corenlp-examples/blob/master/src/main/java/drew/corenlp/SimpleExample.java

    // other option
    // https://blog.openshift.com/day-14-stanford-ner-how-to-setup-your-own-name-entity-and-recognition-server-in-the-cloud/

    public static void main(String[] args) throws IOException, URISyntaxException {
        // creates a StanfordCoreNLP object, with POS tagging, lemmatization, NER, parsing, and coreference resolution
        Properties props = new Properties();
        props.put("annotators", "tokenize, ssplit, pos, lemma, ner, parse, dcoref");
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

        // read some text from the file..
        File inputFile = new File(args[0]);
        String text = FileUtils.readFileToString(inputFile, Charset.forName("UTF-8"));

        // create an empty Annotation just with the given text
        Annotation document = new Annotation(text);

        // run all Annotators on this text
        pipeline.annotate(document);
        // these are all the sentences in this document
        // a CoreMap is essentially a Map that uses class objects as keys and has values with custom types
        List<CoreMap> sentences = document.get(SentencesAnnotation.class);
        // System.out.println(extractEntities(sentences));

        for (CoreMap sentence : sentences) {
            // traversing the words in the current sentence
            // a CoreLabel is a CoreMap with additional token-specific methods
            String ne_ = "";
            int end = -1;
            String val = ""; 
            for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
                // this is the text of the token
                String word = token.get(TextAnnotation.class);
                // this is the POS tag of the token
                String pos = token.get(PartOfSpeechAnnotation.class);
                // this is the NER label of the token
                String ne = token.get(NamedEntityTagAnnotation.class);
                if (end == token.beginPosition() -1 && ne_.equals(ne)) {
                    val += " " + word;
                } else {
                    if (val != "") {
                        System.out.println(val + "\t" + ne_);
                    } else {
  //                      System.out.println("word: " + word + " pos: " + pos + " ne:" + ne + " bp:" + token.beginPosition() + " ep:" + token.endPosition());
                    }
                    val = word;
                    end = -1;
                    ne_ = "";
                }
//                System.out.println("word: " + word + " pos: " + pos + " ne:" + ne + " bp:" + token.beginPosition() + " ep:" + token.endPosition());
                ne_ = ne;
                end = token.endPosition();
            }
            //
            // // this is the parse tree of the current sentence
            // Tree tree = sentence.get(TreeAnnotation.class);
            // System.out.println("parse tree:\n" + tree);
            //
            // // this is the Stanford dependency graph of the current sentence
            // SemanticGraph dependencies = sentence.get(CollapsedCCProcessedDependenciesAnnotation.class);
            // System.out.println("dependency graph:\n" + dependencies);
        }
        //
        // // This is the coreference link graph
        // // Each chain stores a set of mentions that link to each other,
        // // along with a method for getting the most representative mention
        // // Both sentence and token offsets start at 1!
        // Map<Integer, CorefChain> graph = document.get(CorefChainAnnotation.class);

    }

    // http://stackoverflow.com/questions/13765349/multi-term-named-entities-in-stanford-named-entity-recognizer
    public static HashMap<String, HashMap<String, Integer>> extractEntities(List<CoreMap> sentences) {

        HashMap<String, HashMap<String, Integer>> entities = new HashMap<String, HashMap<String, Integer>>();

        for (CoreMap sentence : sentences) {
            // traversing the words in the current sentence
            // a CoreLabel is a CoreMap with additional token-specific methods
            // for (CoreLabel token: sentence.get(TokensAnnotation.class)) {

            Iterator<CoreLabel> iterator = sentence.get(TokensAnnotation.class).iterator();

            if (!iterator.hasNext())
                continue;

            CoreLabel cl = iterator.next();

            while (iterator.hasNext()) {
                String answer = cl.getString(CoreAnnotations.AnswerAnnotation.class);

                if (answer.equals("O")) {
                    cl = iterator.next();
                    continue;
                }

                if (!entities.containsKey(answer))
                    entities.put(answer, new HashMap<String, Integer>());

                String value = cl.getString(CoreAnnotations.ValueAnnotation.class);

                while (iterator.hasNext()) {
                    cl = iterator.next();
                    if (answer.equals(
                            cl.getString(CoreAnnotations.AnswerAnnotation.class)))
                        value = value + " " +
                                cl.getString(CoreAnnotations.ValueAnnotation.class);
                    else {
                        if (!entities.get(answer).containsKey(value))
                            entities.get(answer).put(value, 0);

                        entities.get(answer).put(value,
                                entities.get(answer).get(value) + 1);

                        break;
                    }
                }

                if (!iterator.hasNext())
                    break;
            }
        }

        return entities;
    }
}
