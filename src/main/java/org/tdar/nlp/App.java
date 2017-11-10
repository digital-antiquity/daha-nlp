package org.tdar.nlp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import opennlp.tools.util.InvalidFormatException;
import opennlp.tools.util.Span;

public class App {

    private final Logger log = LogManager.getLogger(getClass());

    private static final String END_PAGE = "______END___PAGE______";
    private static final String START_PAGE = "______START_PAGE______";
    private final double minProbability = .9;
    private boolean includeCitation = true;
    private boolean includePerson = true;
    private boolean includeInstitution = true;
    private boolean includeLocation = true;
    private boolean includeSiteCode = true;
    static boolean html = false;

    public static void main(String[] args) throws Exception {
        String input = new String(
                "Omar Turney then recommended him for a job with J.A. Mewkes at Elden Pueblo in Flagstaff, but he apparently was not hired, though his brother Paul was.");

        String filename = args[0];
//         filename = "/Users/abrin/Downloads/ABDAHA-2/Kelly-et-al-2010_OCR_PDFA.pdf";
        filename = "/Users/abrin/Downloads/ABDAHA-2/2001_Abbott_GreweArchaeologicalVol2PartI_OCR_PDFA.pdf";
        if (filename != null) {
            File file = new File(filename);

            if (FilenameUtils.getExtension(filename).equalsIgnoreCase("pdf")) {
                String base = FilenameUtils.getBaseName(filename);
                File parent = new File("tmp");
                if (!parent.exists()) {
                    parent.mkdirs();
                }
                PDFTextStripper stripper = new PDFTextStripper();
                if (html) {
                    stripper = new PDFText2HTML();
                    base += ".html";
                }
                File textfile = new File(parent, base + ".txt");
                if (textfile.exists()) {
                    file = textfile;
                } else {
                    stripper.setAddMoreFormatting(true);
                    stripper.setPageStart(START_PAGE);
                    stripper.setPageEnd(END_PAGE);
                    // stripper.set
                    PDDocument pdDoc = PDDocument.load(file, MemoryUsageSetting.setupMixed(Runtime.getRuntime().freeMemory() / 5L));
                    stripper.writeText(pdDoc, new FileWriter(textfile));
                    pdDoc.close();
                    file = textfile;
                }
            }

            input = FileUtils.readFileToString(file);

        }
        File dir = downloadModels();
        try {
            App app = new App();
            app.run(filename, input, dir);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void run(String filename, String input, File dir) throws IOException, FileNotFoundException, InvalidFormatException {
        SentenceModel sModel = new SentenceModel(new FileInputStream(new File(dir, "en-sent.bin")));
        SentenceDetectorME sentenceDetector = new SentenceDetectorME(sModel);
        TokenizerModel tModel = new TokenizerModel(new FileInputStream(new File(dir, "en-token.bin")));
        TokenNameFinderModel modelcite = new TokenNameFinderModel(new FileInputStream(new File(dir, "en-ner-citation.bin")));
        TokenNameFinderModel model = new TokenNameFinderModel(new FileInputStream(new File(dir, "en-ner-person.bin")));
        TokenNameFinderModel modelp = new TokenNameFinderModel(new FileInputStream(new File(dir, "en-ner-customperson.bin")));
        TokenNameFinderModel modelo = new TokenNameFinderModel(new FileInputStream(new File(dir, "en-ner-customorganization.bin")));
        TokenNameFinderModel modell = new TokenNameFinderModel(new FileInputStream(new File(dir, "en-ner-customlocation.bin")));
        TokenNameFinderModel model2 = new TokenNameFinderModel(new FileInputStream(new File(dir, "en-ner-organization.bin")));
        TokenNameFinderModel model3 = new TokenNameFinderModel(new FileInputStream(new File(dir, "en-ner-location.bin")));

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
        log.debug("------------------------------------------------------------------------------------------------------------");
        log.debug("    " + filename);
        log.debug("------------------------------------------------------------------------------------------------------------");
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
        location.setBoostValues(Arrays.asList("valley", "mountain", "state", "country", "county", "city", "town", "base",
                "hill", "ranch"));
        int pos = 0;
        int page = 1;
        int total = 0;
        Map<String, Integer> siteCodes = new HashMap<>();
        Page pageDoc = new Page(page);
        List<Page> pages = new ArrayList<>();
        for (String sentence : sentenceDetector.sentDetect(input)) {
            log.trace(sentence);
            if (sentence.contains(END_PAGE)) {
//                log.debug(" ::    " + sentence);
                log.debug("::: Page:" + page + " : totalTokens: " + total);
                sentence = sentence.replace(END_PAGE, "");
                total = 0;
                page++;
                pages.add(pageDoc);
                pageDoc = new Page(page);
            }
            Tokenizer tokenizer = new TokenizerME(tModel);
            if (includeSiteCode) {
                extractSiteCodes(siteCodes, sentence);
            }
            String tokens[] = tokenizer.tokenize(sentence);
            if (includePerson) {
                total += processResults(pageDoc, model, tokens, pos, person);
                total += processResults(pageDoc, modelp, tokens, pos, person);
            }
            if (includeCitation) {
                total += processResults(pageDoc, modelcite, tokens, pos, cite);
            }
            if (includeInstitution) {
                total += processResults(pageDoc, modelo, tokens, pos, institution);
                total += processResults(pageDoc, model2, tokens, pos, institution);
            }
            if (includeLocation) {
                total += processResults(pageDoc, model3, tokens, pos, location);
                total += processResults(pageDoc, modell, tokens, pos, location);
            }
            pos++;
            // log.debug(pos);
        }

        total = 0;
        for (Page p : pages) {
            total += p.getTotalReferences().get(Page.ALL);
        }
        int avg = total / pages.size();
        int bib = -1;
        for (int i = pages.size() -pages.size() / 3; i >= 0; i --) {
            if (pages.get(i).getTotalReferences().get(Page.ALL) > avg) {
                bib = i;
            } else if (bib > 0) {
                break;
            }
        }
        log.debug("BibStart:" + bib);
        cite.printInOccurrenceOrder();
        person.printInOccurrenceOrder();
        institution.printInOccurrenceOrder();
        location.printInOccurrenceOrder();

        printSiteCodes(siteCodes);
        // DoccatModel m = new DoccatModel(new FileInputStream(""));
        // DocumentCategorizerME myCategorizer = new DocumentCategorizerME(m);
        // double[] outcomes = myCategorizer.categorize(input);
        // for (int category = 0; category < myCategorizer.getNumberOfCategories(); category++) {
        // log.debug("Category: " + myCategorizer.getCategory(category));
        // }
        //
    }

    private void extractSiteCodes(Map<String, Integer> siteCodes, String sentence) {
        for (String code : SiteCodeExtractor.extractSiteCodeTokens(sentence, false)) {
            Integer integer = siteCodes.get(code);
            if (integer == null) {
                integer = 1;
            } else {
                integer = integer + 1;
            }
            siteCodes.put(code, integer);
        }
    }

    private void printSiteCodes(Map<String, Integer> siteCodes) {
        Map<Integer, List<String>> rev = new HashMap<>();
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
        int maxSiteCodeNum = 10;
        for (Integer key : keys) {
            log.debug("site code " + key + " |" + rev.get(key));
            maxSiteCodeNum--;
            if (maxSiteCodeNum < 0) {
                break;
            }
        }
    }

    private static File downloadModels() throws MalformedURLException, IOException, FileNotFoundException {
        List<URL> urls = new ArrayList<URL>();
        urls.add(new URL("http://opennlp.sourceforge.net/models-1.5/en-sent.bin"));
        urls.add(new URL("http://opennlp.sourceforge.net/models-1.5/en-token.bin"));
        urls.add(new URL("http://opennlp.sourceforge.net/models-1.5/en-ner-organization.bin"));
        urls.add(new URL("http://opennlp.sourceforge.net/models-1.5/en-ner-person.bin"));
        urls.add(new URL("http://opennlp.sourceforge.net/models-1.5/en-ner-location.bin"));
        File dir = new File("models");
        dir.mkdir();
        for (URL url : urls) {
            String urlToLoad = StringUtils.substringAfter(url.toString(), "1.5/");
            File f = new File(dir, urlToLoad);
            if (!f.exists()) {

                ReadableByteChannel rbc = Channels.newChannel(url.openStream());
                FileOutputStream fos = new FileOutputStream(f);
                fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
                IOUtils.closeQuietly(fos);
            }
        }
        return dir;
    }

    private int processResults(Page page, TokenNameFinderModel model3, String[] tokens, int pos, NLPHelper helper) {
        // https://opennlp.apache.org/documentation/1.5.3/manual/opennlp.html
        NameFinderME nameFinder = new NameFinderME(model3);

        // NEED TO CREATE A DATA MODEL OF A PAGE, and THEN APPLY TOKENS TO PAGE, IF TOKEN IS LARGER THAN AN EXISITNG TOKEN (overlap) THEN
        // EMBED ONE INSIDE THE OTHER
        Span[] names = nameFinder.find(tokens);
        String[] matches = Span.spansToStrings(names, tokens);
        int total = 0;
        for (int i = 0; i < matches.length; i++) {
            boolean valid = false;
            Span span = names[i];
            String name = matches[i];
            String prev = "";
            if (span.getStart() > 0) {
                prev = tokens[span.getStart() - 1];
            }
            for (String match : helper.getSkipPreviousTerms()) {
                if (match.equalsIgnoreCase(prev)) {
                    log.trace("skipping: " + prev + " " + name);
                    continue;
                }
            }
            if (span.getProb() > getMinProbability()) {
                valid = page.appendOcurrenceMap(name, pos, span.getProb(), helper);
            }

            if (valid) {
                total++;
            }
        }
        nameFinder.clearAdaptiveData();
        return total;
    }

    public double getMinProbability() {
        return minProbability;
    }

}
