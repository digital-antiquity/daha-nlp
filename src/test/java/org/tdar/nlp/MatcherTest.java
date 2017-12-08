package org.tdar.nlp;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;
import org.tdar.nlp.nlp.NLPHelper;
import org.tdar.nlp.train.SentenceStripper;

import opennlp.tools.namefind.NameFinderME;
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
        File dir = ModelDownloader.downloadModels();
        TokenNameFinderModel modelPerson = new TokenNameFinderModel(new FileInputStream(new File(dir, "en-ner-person.bin")));
        TokenNameFinderModel modelCustomPerson = new TokenNameFinderModel(new FileInputStream(new File(dir, "en-ner-customperson.bin")));
        TokenizerModel tokenizerModel = new TokenizerModel(new FileInputStream(new File(dir, "en-token.bin")));

        String sentence = IOUtils.toString(new FileInputStream("src/test/resources/kelly.txt"));
        SentenceModel sModel = new SentenceModel(new FileInputStream(new File(dir, "en-sent.bin")));
        SentenceDetectorME sentenceDetector = new SentenceDetectorME(sModel);
        sentence = StringUtils.replace(sentence, "\n\n", ". ");
        NLPHelper person = new NLPHelper("person", modelPerson, modelCustomPerson);
        boolean seenSophia =  false;
        for (String _sentence : sentenceDetector.sentDetect(sentence)) {
            log.debug(_sentence);
            for (String sentence____ : _sentence.split("(\n|\r\n){3,5}")) {
                Tokenizer tokenizer = new TokenizerME(tokenizerModel);
                log.debug(sentence____);
                String tokens[] = tokenizer.tokenize(sentence____);
                for (TokenNameFinderModel model3 : person.getModels()) {
                    NameFinderME nameFinder = new NameFinderME(model3);
                    Span[] names = nameFinder.find(tokens);
                    String[] matches = Span.spansToStrings(names, tokens);
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
