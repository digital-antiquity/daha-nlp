package org.tdar.nlp;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;

public class SentenceStripper {

    private static boolean html;
    private static String tagName = "culture";
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
//            filename = "/Users/abrin/Downloads/ABDAHA/Kelly-et-al-2010_OCR_PDFA.pdf";
//             filename = "/Users/abrin/Downloads/ABDAHA/2001_Abbott_GreweArchaeologicalVol2PartI_OCR_PDFA.pdf";
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
                String types = IOUtils.toString(new FileInputStream("ontologies/CeramicType_Wares.txt"));
                types = IOUtils.toString(new FileInputStream("ontologies/Cultures_flattened.txt"));
                List<String> list = new ArrayList<>(Arrays.asList(types.split("[\r\n]")));

                list.removeAll(Collections.singleton(""));
                List<String> toAdd = new ArrayList<>();
                for (String entry: list) {
                    if (entry.contains(" ")) {
                        toAdd.add(entry.replace(" ", "_"));
                        toAdd.add(entry.replace("-", "_"));
                    }
                }

                list.addAll(toAdd);
                // get the longest first
                Collections.sort(list, new Comparator<String>() {

                    @Override
                    public int compare(String o1, String o2) {
                        if (o1.length() == o2.length()) {
                            return o1.compareTo(o2);
                        }
                        if (o1.length() > o2.length()) {
                            return -1;
                        } else {
                            return 1;
                        }
                    }});
                log.debug(list);
                for (String sentence___ : sentenceDetector.sentDetect(input)) {
                    // https://www.sketchengine.co.uk/penn-treebank-tagset/
                    String sentence__ = sentence___.replaceAll("[\r\n]", " ");
                    List<Integer> starts = new ArrayList<>();
                    List<Integer> ends = new ArrayList<>();
                    List<String> terms = new ArrayList<>();
                    List<String> terms_ = new ArrayList<>();
                    for (String term : list) {
                        terms.add(new String(term));
                        if (term.contains(" ")) {
                            String term_ = term.replace(" ", "_");
                            sentence__ = sentence__.replaceAll("(?i)" + term, term_);
                            term = term_;
                        }
                        terms_.add(term);
                    }
                    if (!sentence__.equals(sentence___)) {
                        log.trace(sentence__);
                    }
                    Tokenizer tokenizer = new TokenizerME(tokenizerModel);
                    String[] words = tokenizer.tokenize(sentence__);
                    String[] tags = tagger.tag(words);
                    for (String term : list) {
                        if (StringUtils.isBlank(term) || !sentence__.toLowerCase().contains(term.toLowerCase())) {
                            continue;
                        }

                        StringBuilder sent = new StringBuilder();
                        for (int i = 0; i < words.length; i++) {
                            sent.append(words[i]);
                            sent.append("_");
                            sent.append(tags[i]);
                            sent.append(" ");
                        }
                        log.debug(" __ {} __ {}", term, sent.toString().trim());
                        boolean reject = false;
                        for (int i = 0; i < words.length; i++) {
                            String word = words[i];
                            // if (!tags[i].equals("FW") && !tags[i].contains("NP") ) {
                            // continue;
                            // }
                            // //
                            if (term.equalsIgnoreCase(word)) {
                                int j = i + 1;
                                while (true) {
                                    if (j > words.length - 1) {
                                        break;
                                    }
                                    
                                    // Pueblo del
                                    if (words[j].toLowerCase().equals("del") || words[j].toLowerCase().equals("de")) {
                                        reject = true;
                                    }
                                    boolean containsValidSuffix = ArrayUtils.contains(new String[] { "indian", "indians", "empire", "culture", "population","pottery", "ceramics", "vessels", "ceramic", "vessel","sherd","shard" }, words[j].toLowerCase());
                                    if (!tags[j].contains("NP")
                                            || containsValidSuffix || Utils.percentNumericCharacters(words[j]) > 1) {
                                        // Ootam Reassertion Period [late Classic] / NNP NNP NNP NNP NNP vs. Ootam [and Hohokam ] trait / NNP NNP NNP SYM NN
                                        // if (word.equals("Ootam")) {
                                        // log.debug("{} {} {} {} {}", words[i] , words[i+1],words[i+2],words[i+3],words[i+4]);
                                        // log.debug("{} {} {} {} {}", tags[i] , tags[i+1],tags[i+2],tags[i+3],tags[i+4]);
                                        // }
                                        break;
                                    }
                                    j++;
                                }

                                int h = i - 1;
                                // Gila Pueblo
                                if (i > 0 && tags[h].contains("NP") && words[i].equalsIgnoreCase("pueblo")) {
                                    reject = true;
                                }
                                while (true) {
                                    // fixme need to catch "classic period Hohokam"
                                    if (h < 0 || !ArrayUtils.contains(new String[] { "early", "middle", "late", "probably" }, words[h])) {
                                        h++;
                                        break;
                                    }
                                    h--;
                                }
                                if (i != j - 1 || reject) {
                                    continue;
                                }
                                
                                
                                starts.add(h);
                                ends.add(j);
                                for (int a = h; a <= j - 1; a++) {
                                    log.debug(" :: {} -> {}", words[a], tags[a]);
                                }
                            }
                        }

                    }
                    printNewString(starts, ends, terms, terms_, words);
                }
                // DocumentAnalyzer app = new DocumentAnalyzer();
                // app.run(file.getName(), input, dir);
            } catch (Exception e) {
                log.error("{}", e, e);
            }
        }

    }

    private static void printNewString(List<Integer> starts, List<Integer> ends, List<String> terms, List<String> terms_, String[] words) {
        StringBuilder sb = new StringBuilder();
        if (CollectionUtils.isEmpty(starts)) {
            return;
        }
        for (int x = 0; x < words.length; x++) {
            if (starts.contains(x)) {
                sb.append(" <START:");
                sb.append(tagName);
                sb.append(">");
            }
            if (ends.contains(x)) {
                sb.append(" <END>");
            }
            sb.append(" ");
            String str = words[x];
            int indexOf = terms_.indexOf(str);
            if (indexOf > -1) {
                sb.append(terms.get(indexOf));
            } else {
                sb.append(str);
            }
        }
        log.debug(">> " + sb.toString());
    }



}
