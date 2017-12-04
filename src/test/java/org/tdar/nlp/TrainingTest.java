package org.tdar.nlp;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Ignore;
import org.junit.Test;
import org.tdar.nlp.train.SentenceProcessor;
import org.tdar.nlp.train.SentenceResult;
import org.tdar.nlp.train.VocabList;

import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.tokenize.TokenizerModel;

public class TrainingTest {
     final Logger log = LogManager.getLogger(getClass());

    @Test
    public void test() throws FileNotFoundException, IOException {
        SentenceProcessor sp = setup("ontologies/Cultures_flattened.txt");
        SentenceResult processSentence = sp.processSentence("The Cohonina may have been producing much of the cotton found in the Flagstaff area , not the Hohokam .\n");
        log.debug(processSentence.getSentence());
        assertNotNull(processSentence.getSentence());
    }

    
    @Test
    public void testColon() throws FileNotFoundException, IOException {
        SentenceProcessor sp = setup("ontologies/Site_ProjectNames_flattened.txt");
        SentenceResult processSentence = sp.processSentence("Haury , Emil W. 1932 Roosevelt 9:6 : A Hohokam Site .");
        log.debug(processSentence.getSentence());
        assertTrue(processSentence.getTags().contains("Roosevelt 9:6"));
    }
    
    
    
    @Test
    public void testPunctuationBreak() throws FileNotFoundException, IOException {
        SentenceProcessor sp = setup("ontologies/Cultures_flattened.txt");
        SentenceResult processSentence = sp.processSentence("1998b Shifting Scales ofProduction  : Hohokam  Ceramics from the Sedentary and Classic_Periods .");
        log.debug(processSentence.getSentence());
        assertTrue(processSentence.getTags().contains("Hohokam"));
    }

    
    @Test
    public void testNegation() throws FileNotFoundException, IOException {
        SentenceProcessor sp = setup("ontologies/Cultures_flattened.txt");
        SentenceResult processSentence = sp.processSentence("1998b Shifting Scales of no Hohokam  Ceramics from the Sedentary and Classic_Periods .");
        log.debug(processSentence.getSentence());
        assertFalse(processSentence.getTags().contains("Hohokam"));
    }

    @Test
    public void testCombinedTagsWithOfTHe() throws FileNotFoundException, IOException {
        SentenceProcessor sp = setup("ontologies/Cultures_flattened.txt");
        SentenceResult processSentence = sp.processSentence("Explore Regional Interaction ofthe Sedentary Period Hohokam -- Lindauer , Owen 1988 A Study of Vessel Form and Painted Designs to  Explore Regional Interaction ofthe Sedentary Period Hohokam  .");
        log.debug(processSentence.getSentence());
        assertTrue(processSentence.getTags().contains("Sedentary Period Hohokam"));
    }


    @Test
    public void testMajorityNumeric() throws FileNotFoundException, IOException {
        SentenceProcessor sp = setup("ontologies/Cultures_flattened.txt");
        SentenceResult processSentence = sp.processSentence("1983a Classic Period  Ceramic Manufacture : Exploring Vari­ ability in the Production and Use of Hohokam Vessels .");
        log.debug(processSentence.getSentence());
        assertTrue(processSentence.getTags().contains("Classic Period"));
    }


    @Test
    public void testTheInNoun() throws FileNotFoundException, IOException {
        SentenceProcessor sp = setup("ontologies/Cultures_flattened.txt");
        SentenceResult processSentence = sp.processSentence("1982b Sociopolitical Organization in the  Desert Southwest : The Hohokam  of the Gila Butte Region , South-Central Arizona .");
        log.debug(processSentence.getSentence());
        assertTrue(processSentence.getTags().contains("Hohokam"));
    }

    @Test
    public void testOneWordNoun() throws FileNotFoundException, IOException {
        SentenceProcessor sp = setup("ontologies/Cultures_flattened.txt");
        SentenceResult processSentence = sp.processSentence("GRW - 10765 X X X X None GRW - 13130 X X X X None GRW - 14500 X X X X None GRW - 4633 X X X X None GRW - 5592 X X X X None GRW - 10842  X X X Pima  , .0256");
        log.debug(processSentence.getSentence());
        assertTrue(processSentence.getTags().contains("Pima"));
    }

    @Test
    public void testPuebloLowerCase() throws FileNotFoundException, IOException {
        SentenceProcessor sp = setup("ontologies/Cultures_flattened.txt");
        sp.setCaseSensitive(true);
        SentenceResult processSentence = sp.processSentence("[ Note that mat impressions later were found on the floors of \" Rooms \" 13 and 14.] [ From ( Midvale 1937 ) : \" Room 8 was the largest and best preserved in the pueblo . \n");
        log.debug(processSentence.getSentence());
        assertNull(processSentence.getSentence());
    }

    @Test
    public void testNounChainInvalid() throws FileNotFoundException, IOException {
        SentenceProcessor sp = setup("ontologies/Cultures_flattened.txt");
        sp.setCaseSensitive(true);
        SentenceResult processSentence = sp.processSentence("In the first place , the decorated ceramics ( Gila Polychrome , Hopi yellowware ) found in Rooms 7 and 30 and areas 13 and 14 provide specific additional evidence for the age of the roomblock .");
        log.debug(processSentence.getSentence());
        assertNull(processSentence.getSentence());
    }

    @Test
    public void testNounChain() throws FileNotFoundException, IOException {
        SentenceProcessor sp = setup("ontologies/Cultures_flattened.txt");
        sp.setCaseSensitive(true);
        SentenceResult processSentence = sp.processSentence("In the first place , the decorated ceramics ( Gila Polychrome , Hopi Yellowware ) found in Rooms 7 and 30 and areas 13 and 14 provide specific additional evidence for the late Classic Period age of the roomblock .");
        log.debug(processSentence.getSentence());
        assertNotNull(processSentence.getSentence());
    }
    
    @Test
    public void testMultiNounChain() throws FileNotFoundException, IOException {
        SentenceProcessor sp = setup("ontologies/Cultures_flattened.txt");
        sp.setCaseSensitive(true);
        SentenceResult processSentence = sp.processSentence("1977 Classic Period Hohokam in the Escalante Ruin Group .");
        log.debug(processSentence.getSentence());
        assertNotNull(processSentence.getSentence());
    }
    
    
    @Test
    public void testPuebloProperNoun_Place() throws FileNotFoundException, IOException {
        SentenceProcessor sp = setup("ontologies/Cultures_flattened.txt");
        sp.setCaseSensitive(true);
        SentenceResult processSentence = sp.processSentence("Samples from Polles Pueblo were dominated by felsic volcanic temper .");
        log.debug(processSentence.getSentence());
        assertNull(processSentence.getSentence());
    }
    
    

    private SentenceProcessor setup(String file) throws MalformedURLException, IOException, FileNotFoundException {
        File dir = ModelDownloader.downloadModels();
        POSModel posModel = new POSModel(new File(dir, "en-pos-maxent.bin"));
        POSTaggerME tagger = new POSTaggerME(posModel);
        TokenizerModel tokenizerModel = new TokenizerModel(new FileInputStream(new File(dir, "en-token.bin")));
        
        VocabList list = new VocabList(new FileInputStream(file));
        log.debug(list.getList());
        SentenceProcessor sp = new SentenceProcessor(tokenizerModel, tagger, "culture", list.getList());
        return sp;
    }

}
