package org.tdar.nlp.train;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tdar.nlp.ModelDownloader;
import org.tdar.nlp.PdfOcrCleanup;
import org.tdar.nlp.nlp.DocumentAnalyzer;

import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.TokenizerModel;

public class SentenceStripper {

    private static boolean html;
    private static String tagName = "person";
    static final Logger log = LogManager.getLogger(SentenceStripper.class);

    // can we extract title by matching on common words?

    public static void main(String[] args) throws Exception {
        String input = new String(
                "Omar Turney then recommended him for a job with J.A. Mewkes at Elden Pueblo in Flagstaff; he went to Hohokam High School.");

        String filename = null;
        if (args != null && args.length > 0) {
            filename = args[0];
        }
        if (filename == null) {
            // Hohokam
            filename = "/Users/abrin/Dropbox (ASU)/PDFA-Analysis/lc4-abbyy12-pdfa.pdf";
            filename = "/Users/abrin/Downloads/ABDAHA/Kelly-et-al-2010_OCR_PDFA.pdf";
            // filename = "/Users/abrin/Downloads/ABDAHA/2001_Abbott_GreweArchaeologicalVol2PartI_OCR_PDFA.pdf";
            // filename = "tmp/hedgpeth-hills_locality-1_OCR_PDFA.txt";
            // filename = "tmp/Underfleet1.html.txt";
        }

        File dir = ModelDownloader.downloadModels();
        System.setProperty("java.awt.headless", "true");

        List<File> files = new ArrayList<>();
        if (filename != null) {
            File file = new File(filename);
            if (file.isDirectory()) {
                files.addAll(FileUtils.listFiles(file, new String[] { "txt", "pdf" }, true));
            } else {
                files.add(file);
            }
        }
        PdfOcrCleanup cleaner = new PdfOcrCleanup();
        for (File file_ : files) {
            try {
                File file = cleaner.processFile(file_, html);
                input = FileUtils.readFileToString(file);
                SentenceModel sModel = new SentenceModel(new FileInputStream(new File(dir, "en-sent.bin")));
                SentenceDetectorME sentenceDetector = new SentenceDetectorME(sModel);
                POSModel posModel = new POSModel(new File(dir, "en-pos-maxent.bin"));
                POSTaggerME tagger = new POSTaggerME(posModel);
                TokenizerModel tokenizerModel = new TokenizerModel(new FileInputStream(new File(dir, "en-token.bin")));
                // types = IOUtils.toString(new FileInputStream("ontologies/Cultures_flattened.txt"));

                // VocabList list = new VocabList(new FileInputStream("ontologies/Cultures_flattened.txt"));
                // VocabList list = new VocabList(new FileInputStream("ontologies/CeramicType_Wares.txt"));
                VocabList list = new VocabList(new FileInputStream("ontologies/People.txt"));
                Set<String> uniqueTags = new HashSet<>();
                log.debug("\n#######\n#######  FILE: {}", file.getName());
                log.debug("\n#######  terms: {}\n#######\n####", list.getList());
                SentenceProcessor sp = new SentenceProcessor(tokenizerModel, tagger, tagName, list.getList());
                for (String _sentence : sentenceDetector.sentDetect(input)) {
                    for (String sentence____ : _sentence.split(DocumentAnalyzer.SPLIT_SENTENCE)) {
//                    if (sentence____.toLowerCase().contains("abbott")) {
//                        log.debug(sentence____);
//                    }
                        // https://www.sketchengine.co.uk/penn-treebank-tagset/
                        SentenceResult result = sp.processSentence(sentence____);
                        uniqueTags.addAll(result.getTags());
                        if (!result.getTags().isEmpty()) {
                            log.debug(" {} __ {}", result.getTags(), result.getTaggedSentence());
                        }
                        if (result.getSentence() != null) {
                            log.debug("## {}", result.getSentence());
                        }
                    }
                }

                // DocumentAnalyzer app = new DocumentAnalyzer();
                // app.run(file.getName(), input, dir);
                log.debug("\n#### UniqueTags: {}", uniqueTags);
            } catch (Exception e) {
                log.error("{}", e, e);
            }
        }
    }

    /**
     * PARTS OF SPEECH LOGIC FROM https://www.sketchengine.co.uk/penn-treebank-tagset/
     * 
     * POS Tag Description Example
     * CC coordinating conjunction and
     * CD cardinal number 1, third
     * DT determiner the
     * EX existential there there is
     * FW foreign word d’hoevre
     * IN preposition, subordinating conjunction in, of, like
     * IN/that that as subordinator that
     * JJ adjective green
     * JJR adjective, comparative greener
     * JJS adjective, superlative greenest
     * LS list marker 1)
     * MD modal could, will
     * NN noun, singular or mass table
     * NNS noun plural tables
     * NP proper noun, singular John
     * NPS proper noun, plural Vikings
     * PDT predeterminer both the boys
     * POS possessive ending friend’s
     * PP personal pronoun I, he, it
     * PP$ possessive pronoun my, his
     * RB adverb however, usually, naturally, here, good
     * RBR adverb, comparative better
     * RBS adverb, superlative best
     * RP particle give up
     * SENT Sentence-break punctuation . ! ?
     * SYM Symbol / [ = *
     * TO infinitive ‘to’ togo
     * UH interjection uhhuhhuhh
     * VB verb be, base form be
     * VBD verb be, past tense was, were
     * VBG verb be, gerund/present participle being
     * VBN verb be, past participle been
     * VBP verb be, sing. present, non-3d am, are
     * VBZ verb be, 3rd person sing. present is
     * VH verb have, base form have
     * VHD verb have, past tense had
     * VHG verb have, gerund/present participle having
     * VHN verb have, past participle had
     * VHP verb have, sing. present, non-3d have
     * VHZ verb have, 3rd person sing. present has
     * VV verb, base form take
     * VVD verb, past tense took
     * VVG verb, gerund/present participle taking
     * VVN verb, past participle taken
     * VVP verb, sing. present, non-3d take
     * VVZ verb, 3rd person sing. present takes
     * WDT wh-determiner which
     * WP wh-pronoun who, what
     * WP$ possessive wh-pronoun whose
     * WRB wh-abverb where, when
     * # # #
     * $ $ $
     * “ Quotation marks ‘ “
     * `` Opening quotation marks ‘ “
     * ( Opening brackets ( {
     * ) Closing brackets ) }
     * , Comma ,
     * : Punctuation – ; : — …
     * Main differences to default Penn tagset
     * In TreeTagger tool
     * 
     * Distinguishes be (VB) and have (VH) from other (non-modal) verbs (VV)
     * For proper nouns, NNP and NNPS have become NP and NPS
     * SENT for end-of-sentence punctuation (other punctuation tags may also differ)
     * 
     * 
     */

}
