package org.tdar.nlp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.tools.PDFText2HTML;
import org.tdar.utils.SiteCodeExtractor;

import opennlp.tools.doccat.DoccatFactory;
import opennlp.tools.doccat.FeatureGenerator;
import opennlp.tools.doccat.NGramFeatureGenerator;
import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.util.Span;

public class App {




    public static void main(String[] args) throws Exception {
        String input = new String(
                "Omar Turney then recommended him for a job with J.A. Mewkes at Elden Pueblo in Flagstaff, but he apparently was not hired, though his brother Paul was.");

        String filename = args[0];
        filename = "/Users/abrin/Downloads/ABDAHA/Farmingonthefldpln_400agg_Redacted-ocr_OCR_PDFA.pdf";
        // filename = "/Users/abrin/Downloads/ABDAHA-2/Kelly-et-al-2010_OCR_PDFA.pdf";
        if (filename != null) {
            File file = new File(filename);

            if (FilenameUtils.getExtension(filename).equalsIgnoreCase("pdf")) {
                String base = FilenameUtils.getBaseName(filename);
                File textfile = new File(base + ".txt");
                if (textfile.exists()) {
                    file = textfile;
                } else {
                    PDFTextStripper stripper = new PDFText2HTML();
                    stripper.setAddMoreFormatting(true);
                    stripper.setPageStart("______START_PAGE______");
                    stripper.setPageEnd("______END_PAGE______");
                    // stripper.set
                    PDDocument pdDoc = PDDocument.load(file, MemoryUsageSetting.setupMixed(Runtime.getRuntime().freeMemory() / 5L));
                    stripper.writeText(pdDoc, new FileWriter(textfile));
                    pdDoc.close();
                    file = textfile;
                }
            }

            input = FileUtils.readFileToString(file);

        }
        try {
            List<URL> urls = new ArrayList<URL>();
            urls.add(new URL("http://opennlp.sourceforge.net/models-1.5/en-sent.bin"));
            urls.add(new URL("http://opennlp.sourceforge.net/models-1.5/en-token.bin"));
            urls.add(new URL("http://opennlp.sourceforge.net/models-1.5/en-ner-organization.bin"));
            urls.add(new URL("http://opennlp.sourceforge.net/models-1.5/en-ner-person.bin"));
            urls.add(new URL("http://opennlp.sourceforge.net/models-1.5/en-ner-location.bin"));
            for (URL url : urls) {
                String urlToLoad = StringUtils.substringAfter(url.toString(), "1.5/");
                File f = new File(urlToLoad);
                if (!f.exists()) {
                    ReadableByteChannel rbc = Channels.newChannel(url.openStream());
                    FileOutputStream fos = new FileOutputStream(urlToLoad);
                    fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
                    IOUtils.closeQuietly(fos);
                }
            }

            SentenceModel sModel = new SentenceModel(new FileInputStream("en-sent.bin"));
            SentenceDetectorME sentenceDetector = new SentenceDetectorME(sModel);
            TokenizerModel tModel = new TokenizerModel(new FileInputStream("en-token.bin"));
            TokenNameFinderModel modelcite = new TokenNameFinderModel(new FileInputStream("en-ner-citation.bin"));
            TokenNameFinderModel model = new TokenNameFinderModel(new FileInputStream("en-ner-person.bin"));
            TokenNameFinderModel modelp = new TokenNameFinderModel(new FileInputStream("en-ner-customperson.bin"));
            TokenNameFinderModel modelo = new TokenNameFinderModel(new FileInputStream("en-ner-customorganization.bin"));
            TokenNameFinderModel modell = new TokenNameFinderModel(new FileInputStream("en-ner-customlocation.bin"));
            TokenNameFinderModel model2 = new TokenNameFinderModel(new FileInputStream("en-ner-organization.bin"));
            TokenNameFinderModel model3 = new TokenNameFinderModel(new FileInputStream("en-ner-location.bin"));

            FeatureGenerator[] featureGenerators = { new NGramFeatureGenerator(1, 1),
                    new NGramFeatureGenerator(2, 3) };
            DoccatFactory factory = new DoccatFactory(featureGenerators);
            // https://www.tutorialkart.com/opennlp/ngram-features-for-document-classification-in-opennlp/ document classification
            // https://stackoverflow.com/questions/22983003/convert-natural-language-questions-to-sql-queries/23012263#23012263
            // http://blog.thedigitalgroup.com/sagarg/2015/10/30/open-nlp-name-finder-model-training/
            // https://www.searchtechnologies.com/nlp-project-feasibility-flowchart

            // FIXME: Handle cases like
            // Person 251 | Debbie Corbett Diane Cure Santo Cuollo Terry Dean Pete Devine Jean Elsasser Tammy Ewing Scott Fedick Joe Finken Debbi Foldi Paul
            // Fortin Dale Fournier Mark Gaff
            System.out.println("------------------");
            NLPHelper person = new NLPHelper("person");
            NLPHelper cite = new NLPHelper("citation");
            cite.setRegexBoost(".+\\d++.*");
            NLPHelper institution = new NLPHelper("institution");
            institution.setBoostValues(
                    Arrays.asList("inc.", "co.", "university", "college", "museum", "company", "llc", "ltd", " of ", "office", "services", "group", "society"));
            person.setMinTermLength(4);
            // matching initials 
            person.setRegexBoost("(.+\\s\\w\\.\\s.+|\\w\\.\\s\\.+)");
            person.setBoostValues(Arrays.asList("dr.", "mr.", "mrs."));
            person.setMinPercentNumberWords(2);
            person.setSkipPreviousTerms(Arrays.asList("of", "in", "the"));
            NLPHelper location = new NLPHelper("location");
            location.setBoostValues(Arrays.asList("north", "south", "east", "west", "valley", "mountain", "state", "country", "county", "city", "town", "base",
                    "hill", "ranch"));
            int pos = 0;
            Map<String,Integer> siteCodes = new HashMap<>();
            for (String sentence : sentenceDetector.sentDetect(input)) {
                Tokenizer tokenizer = new TokenizerME(tModel);
                for (String code : SiteCodeExtractor.extractSiteCodeTokens(sentence, false)) {
                    Integer integer = siteCodes.get(code);
                    if (integer == null) {
                        integer = 1;
                    } else {
                        integer = integer + 1;
                    }
                    siteCodes.put(code, integer);
                }
                String tokens[] = tokenizer.tokenize(sentence);
                processResults(model, tokens, pos, person);
                processResults(modelp, tokens, pos, person);
                processResults(modelcite, tokens, pos, cite);
                processResults(modelo, tokens, pos, institution);
                processResults(model2, tokens, pos, institution);
                processResults(model3, tokens, pos, location);
                processResults(modell, tokens, pos, location);
                pos++;
                // System.out.println(pos);
            }

            cite.printInOccurrenceOrder();
            person.printInOccurrenceOrder();
            institution.printInOccurrenceOrder();
            location.printInOccurrenceOrder();

            Map<Integer,List<String>> rev = new HashMap<>();
            for (Entry<String, Integer> entry : siteCodes.entrySet()) {
                List<String> key = rev.get(entry.getValue());
                if (key == null) {
                    key = new ArrayList<>();
                }
                key.add(entry.getKey());
                rev.put(entry.getValue(), key);
            }
            List<Integer> keys = new ArrayList<>(rev.keySet());
            Collections.sort(keys, Collections.reverseOrder());
            for (Integer key : keys) {
                System.out.println("site code " + key + " |" + rev.get(key));
            }
            // DoccatModel m = new DoccatModel(new FileInputStream(""));
            // DocumentCategorizerME myCategorizer = new DocumentCategorizerME(m);
            // double[] outcomes = myCategorizer.categorize(input);
            // for (int category = 0; category < myCategorizer.getNumberOfCategories(); category++) {
            // System.out.println("Category: " + myCategorizer.getCategory(category));
            // }
            //

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static void processResults(TokenNameFinderModel model3, String[] tokens, int pos, NLPHelper helper) {
        // https://opennlp.apache.org/documentation/1.5.3/manual/opennlp.html
        NameFinderME nameFinder = new NameFinderME(model3);

        Span[] names = nameFinder.find(tokens);
        String[] matches = Span.spansToStrings(names, tokens);
        for (int i = 0; i < matches.length; i++) {
            Span span = names[i];
            String name = matches[i];
            String prev = "";
            if (span.getStart() > 0) {
                prev = tokens[span.getStart() - 1];
            }
            for (String match : helper.getSkipPreviousTerms()) {
                if (match.equalsIgnoreCase(prev)) {
                    System.out.println("skipping: " + prev + " " + name);
                    continue;
                }
            }
            if (span.getProb() > .9) {
                helper.appendOcurrenceMap(name, pos);
            }
        }
        nameFinder.clearAdaptiveData();
    }

}
