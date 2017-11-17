package org.tdar.nlp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tdar.nlp.document.NlpDocument;
import org.tdar.nlp.document.NlpPage;
import org.tdar.nlp.nlp.NLPHelper;
import org.tdar.nlp.result.ResultDocument;

import com.fasterxml.jackson.databind.ObjectMapper;

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

public class DocumentAnalyzer {

    private final Logger log = LogManager.getLogger(getClass());

    static final String END_PAGE = "______END___PAGE______";
    static final String START_PAGE = "______START_PAGE______";
    private final double minProbability = .5;
    private boolean includeCitation = false;
    private boolean includePerson = true;
    private boolean includeInstitution = true;
    private boolean includeLocation = true;
    private boolean includeSiteCode = true;
    private boolean includeCulture = true;
    int maxPages = 1000;
    static boolean html = false;

    public void run(String filename, String input, File dir) throws IOException, FileNotFoundException, InvalidFormatException {
        SentenceModel sModel = new SentenceModel(new FileInputStream(new File(dir, "en-sent.bin")));
        SentenceDetectorME sentenceDetector = new SentenceDetectorME(sModel);

        TokenizerModel tModel = new TokenizerModel(new FileInputStream(new File(dir, "en-token.bin")));
        TokenNameFinderModel modelcite = new TokenNameFinderModel(new FileInputStream(new File(dir, "en-ner-citation.bin")));
        TokenNameFinderModel modelsite = new TokenNameFinderModel(new FileInputStream(new File(dir, "en-ner-site.bin")));
        TokenNameFinderModel modelculture = new TokenNameFinderModel(new FileInputStream(new File(dir, "en-ner-culture.bin")));
        TokenNameFinderModel model = new TokenNameFinderModel(new FileInputStream(new File(dir, "en-ner-person.bin")));
        TokenNameFinderModel modelp = new TokenNameFinderModel(new FileInputStream(new File(dir, "en-ner-customperson.bin")));
        TokenNameFinderModel modelo = new TokenNameFinderModel(new FileInputStream(new File(dir, "en-ner-customorganization.bin")));
        TokenNameFinderModel modell = new TokenNameFinderModel(new FileInputStream(new File(dir, "en-ner-customlocation.bin")));
        TokenNameFinderModel modelo2 = new TokenNameFinderModel(new FileInputStream(new File(dir, "en-ner-organization.bin")));
        TokenNameFinderModel modell2 = new TokenNameFinderModel(new FileInputStream(new File(dir, "en-ner-location.bin")));

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
        NLPHelper person = new NLPHelper("person", model, modelp);
        NLPHelper site = new NLPHelper("site", modelsite);
        NLPHelper culture = new NLPHelper("culture", modelculture);
        site.setBoostValues(Arrays.asList("site","ruin","excavation"));
        NLPHelper cite = new NLPHelper("citation", modelcite);
        cite.setRegexBoost(".+\\d++.*");
        NLPHelper institution = new NLPHelper("institution", modelo, modelo2);
        institution.setBoostValues(
                Arrays.asList("inc.", "co.", "university", "college", "museum", "company", "llc", "ltd", " of ", "office", "services", "society"));
        person.setMinTermLength(4);
        // matching initials
        person.setRegexBoost("(.+\\s\\w\\.\\s.+|\\w\\.\\s\\.+)");
        person.setBoostValues(Arrays.asList("dr.", "mr.", "mrs."));
        person.setMinPercentNumberWords(2);
        person.setSkipPreviousTerms(Arrays.asList("of", "in", "the"));
        NLPHelper location = new NLPHelper("location", modell, modell2);
        location.setBoostValues(Arrays.asList("valley", "mountain", "state", "country", "county", "city", "town", "base",
                "hill", "ranch"));
        int pos = 0;
        int pageNum = 1;
        NlpDocument doc = new NlpDocument(filename);
        if (includePerson) {
            doc.getHelpers().add(person);
        }
        if (includeCitation) {
            doc.getHelpers().add(cite);
        }
        if (includeCulture) {
            doc.getHelpers().add(culture);
        }
        if (includeInstitution) {
            doc.getHelpers().add(institution);
        }
        if (includeLocation) {
            doc.getHelpers().add(location);
        }
        if (includeSiteCode) {
            doc.getHelpers().add(site);
        }
        NlpPage page = new NlpPage(pageNum);
        
        
        // as we get better here, for performance, this could be moved up into the cached text above
        List<String[]> pairs = new ArrayList<>();
        pairs.add(new String[] {" o f ", " of "});
        pairs.add(new String[] {" w ith", " with"});
        pairs.add(new String[] {"I f ", "If "});
//        pairs.add(new String[] {"F loor ", "Floor "});
        pairs.add(new String[] {" i f ", " if "});
        pairs.add(new String[] {"m idd", "midd" });
        pairs.add(new String[] {"M idd","Midd" });
        pairs.add(new String[] {"prim ar","primar" });
        pairs.add(new String[] {"tem poral","temporal" });
        pairs.add(new String[] {" m ode"," mode" });
        pairs.add(new String[] {" M ode"," Mode" });
              
        
        String sentence = "";
        for (String sentence__ : sentenceDetector.sentDetect(input)) {
            // remove page #'s
            // as we get better here, for performance, this could be moved up into the cached text above
            sentence__ = sentence__.replaceAll("(?:^|\r?\n)[0-9]+(\r?\n)+"+END_PAGE, END_PAGE);

            for (String sentence_ : sentence__.split("(\n|\r\n)++")) {
                sentence = sentence_;
                for (String[] pair : pairs) {
                    sentence = StringUtils.replace(sentence, pair[0],pair[1]);
                }

                if (sentence.indexOf(" ") == 1 && (sentence.indexOf("A") != 0) && sentence.indexOf("I") != 0) {
                    sentence = StringUtils.replaceOnce(sentence, " ", "");
                }
                log.trace("::" + sentence);
                if (sentence.contains(END_PAGE)) {
                    sentence = addPage(doc, page, sentence);
                    pageNum++;
                    page = new NlpPage(pageNum);
                } else {
                    page.addSentence(sentence);
                }
                Tokenizer tokenizer = new TokenizerME(tModel);
                if (includeSiteCode) {
                    page.extractSiteCodes(sentence);
                }
                String tokens[] = tokenizer.tokenize(sentence);

                for (NLPHelper h : doc.getHelpers()) {
                    for (TokenNameFinderModel model_ : h.getModels()) {
                        processResults(page, model_, tokens, pos, h);
                    }
                }
                pos++;
            }
            if (pageNum > maxPages) {
                break;
            }
        }
        //add last page
        addPage(doc, page, sentence);
        doc.analyze();
        ResultDocument result = doc.printResults();
        ObjectMapper objectMapper = new ObjectMapper();
        File outDir = new File("out");
        outDir.mkdirs();
        objectMapper.writeValue(new File(outDir, FilenameUtils.getBaseName(filename) + ".json"), result);

        // DoccatModel m = new DoccatModel(new FileInputStream(""));
        // DocumentCategorizerME myCategorizer = new DocumentCategorizerME(m);
        // double[] outcomes = myCategorizer.categorize(input);
        // for (int category = 0; category < myCategorizer.getNumberOfCategories(); category++) {
        // log.debug("Category: " + myCategorizer.getCategory(category));
        // }
        //
    }

    private String addPage(NlpDocument doc, NlpPage page, String sentence) {
        sentence = sentence.replace(END_PAGE, "");
        page.addSentence(sentence);
        doc.addPage(page);
        log.debug(page);
        return sentence;
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

    private void processResults(NlpPage page, TokenNameFinderModel model3, String[] tokens, int pos, NLPHelper helper) {
        // https://opennlp.apache.org/documentation/1.5.3/manual/opennlp.html
        NameFinderME nameFinder = new NameFinderME(model3);

        // NEED TO CREATE A DATA MODEL OF A PAGE, and THEN APPLY TOKENS TO PAGE, IF TOKEN IS LARGER THAN AN EXISITNG TOKEN (overlap) THEN
        // EMBED ONE INSIDE THE OTHER
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
                    log.trace("skipping: " + prev + " " + name);
                    continue;
                }
            }
            String key = Utils.cleanString(name);
            if (!helper.stringValid(key)) {
                return;
            }

            page.addSpan(span, key, pos, helper.getType());
        }
        nameFinder.clearAdaptiveData();
    }

    public double getMinProbability() {
        return minProbability;
    }

}
