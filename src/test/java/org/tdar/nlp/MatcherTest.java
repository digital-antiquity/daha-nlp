package org.tdar.nlp;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;
import org.tdar.nlp.train.SentenceStripper;

import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.TokenNameFinder;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.util.Span;

/**
 * Unit test for simple App.
 */
public class MatcherTest {

    static final Logger log = LogManager.getLogger(SentenceStripper.class);

    @Test
    public void testCoverPageSplit() throws FileNotFoundException, IOException {
        /**
         * PROBLEM: Sophia E. Kelly is not found as a name.
         * Thesis: changing Sophia E. Kelly to David E. Kelly suggests it's partially an issue of not enough training data.
         * 
         * Question: what other ways find people's name when we don't have the context of a sentence?
         *  * could try running the rest of the page through the parser and then evaluate the "remaining" text to see if it's proper nouns?
         *  * could try a dictionary approach for matching the leading first name
         *  * could just focus on training lots more name data
         *  * ???
         */
        File dir = ModelDownloader.downloadModels();
        // load the default and custom name finder
        TokenNameFinderModel modelPerson = new TokenNameFinderModel(new FileInputStream(new File(dir, "en-ner-person.bin")));
        TokenNameFinderModel modelInst = new TokenNameFinderModel(new FileInputStream(new File(dir, "en-ner-organization.bin")));
        
        // run OpenNLPTrain.java prior to enable these two models
        TokenNameFinderModel modelCustomPerson = new TokenNameFinderModel(new FileInputStream(new File(dir, "en-ner-custom-person.bin")));
        TokenNameFinderModel modelCustomInst = new TokenNameFinderModel(new FileInputStream(new File(dir, "en-ner-custom-organization.bin")));

        // word tokenizer
        TokenizerModel tokenizerModel = new TokenizerModel(new FileInputStream(new File(dir, "en-token.bin")));
        // sentence model
        SentenceModel sModel = new SentenceModel(new FileInputStream(new File(dir, "en-sent.bin")));

        // load the test text
        String sentence = IOUtils.toString(new FileInputStream("src/test/resources/person.txt"));
        SentenceDetectorME sentenceDetector = new SentenceDetectorME(sModel);
        // sentence = StringUtils.replace(sentence, "\n\n", ". ");

        List<TokenNameFinderModel> nameFinders = Arrays.asList(modelPerson, modelCustomPerson, modelInst, modelCustomInst);
        boolean seenSophia = false;

        // for each sentence
        for (String _sentence : sentenceDetector.sentDetect(sentence)) {
            log.debug(_sentence);

            // if we have more than one new line; e.g. Sophia E. Kelly\n\nArizona State University;
            // treat these as new sentences. I.e. break paragraphs or separated text
            for (String sentence____ : _sentence.split("(\n|\r\n){2,5}")) {
                Tokenizer tokenizer = new TokenizerME(tokenizerModel);
                log.debug(sentence____);

                
                // tokenize Sentence into word
                String tokens[] = tokenizer.tokenize(sentence____);
                for (TokenNameFinderModel model3 : nameFinders) {
                    // for each model... evaluate
                    NameFinderME nameFinder = new NameFinderME(model3);
                    Span[] names = nameFinder.find(tokens);
                    String[] matches = Span.spansToStrings(names, tokens);
                    Span[] names2 = nameFinder.find(new String[]{sentence____});
                    String[] matches2 = Span.spansToStrings(names2, new String[]{sentence____});
                    if (matches2.length > 0) {
                    log.debug("<<< {}", matches2[0]);
                    }
                    // try and find matches
                    for (String match : matches) {
                        log.debug(">>> {}", match);
                        if (match.equalsIgnoreCase("sophia e. kelly")) {
                            seenSophia = true;
                        }
                    }
                }

            }
        }
        assertTrue(seenSophia);
    }

}
