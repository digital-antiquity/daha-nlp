package org.tdar.nlp;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;
import org.tdar.nlp.train.SentenceProcessor;
import org.tdar.nlp.train.VocabList;

import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.tokenize.TokenizerModel;

public class TrainingTest {
     final Logger log = LogManager.getLogger(getClass());

    @Test
    public void test() throws FileNotFoundException, IOException {
        SentenceProcessor sp = setup();
        String processSentence = sp.processSentence("The Cohonina may have been producing much of the cotton found in the Flagstaff area , not the Hohokam .\n");
        log.debug(processSentence);
        assertNotNull(processSentence);
    }

    @Test
    public void testPuebloLowerCase() throws FileNotFoundException, IOException {
        SentenceProcessor sp = setup();
        sp.setCaseSensitive(true);
        String processSentence = sp.processSentence("[ Note that mat impressions later were found on the floors of \" Rooms \" 13 and 14.] [ From ( Midvale 1937 ) : \" Room 8 was the largest and best preserved in the pueblo . \n");
        log.debug(processSentence);
        assertNull(processSentence);
    }

    @Test
    public void testNounChainInvalid() throws FileNotFoundException, IOException {
        SentenceProcessor sp = setup();
        sp.setCaseSensitive(true);
        String processSentence = sp.processSentence("In the first place , the decorated ceramics ( Gila Polychrome , Hopi yellowware ) found in Rooms 7 and 30 and areas 13 and 14 provide specific additional evidence for the age of the roomblock .");
        log.debug(processSentence);
        assertNull(processSentence);
    }

    @Test
    public void testNounChain() throws FileNotFoundException, IOException {
        SentenceProcessor sp = setup();
        sp.setCaseSensitive(true);
        String processSentence = sp.processSentence("In the first place , the decorated ceramics ( Gila Polychrome , Hopi Yellowware ) found in Rooms 7 and 30 and areas 13 and 14 provide specific additional evidence for the late Classic Period age of the roomblock .");
        log.debug(processSentence);
        assertNotNull(processSentence);
    }
    
    @Test
    public void testMultiNounChain() throws FileNotFoundException, IOException {
        SentenceProcessor sp = setup();
        sp.setCaseSensitive(true);
        String processSentence = sp.processSentence("1977 Classic Period Hohokam in the Escalante Ruin Group .");
        log.debug(processSentence);
        assertNotNull(processSentence);
    }
    
    
    @Test
    public void testPuebloProperNoun_Place() throws FileNotFoundException, IOException {
        SentenceProcessor sp = setup();
        sp.setCaseSensitive(true);
        String processSentence = sp.processSentence("Samples from Polles Pueblo were dominated by felsic volcanic temper .");
        log.debug(processSentence);
        assertNull(processSentence);
    }
    
    

    private SentenceProcessor setup() throws MalformedURLException, IOException, FileNotFoundException {
        File dir = ModelDownloader.downloadModels();
        POSModel posModel = new POSModel(new File(dir, "en-pos-maxent.bin"));
        POSTaggerME tagger = new POSTaggerME(posModel);
        TokenizerModel tokenizerModel = new TokenizerModel(new FileInputStream(new File(dir, "en-token.bin")));
        
        VocabList list = new VocabList(new FileInputStream("ontologies/Cultures_flattened.txt"));
        log.debug(list.getList());
        SentenceProcessor sp = new SentenceProcessor(tokenizerModel, tagger, "culture", list.getList());
        return sp;
    }

}
