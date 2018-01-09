package org.tdar.nlp.nlp;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tdar.nlp.SourceType;
import org.tdar.nlp.Utils;
import org.tdar.nlp.document.NlpDocument;
import org.tdar.nlp.document.NlpPage;
import org.tdar.nlp.result.ResultDocument;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import opennlp.tools.doccat.DoccatFactory;
import opennlp.tools.doccat.FeatureGenerator;
import opennlp.tools.doccat.NGramFeatureGenerator;
import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.RegexNameFinder;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.util.InvalidFormatException;
import opennlp.tools.util.Span;

public class DocumentAnalyzer {

    public static final String SPLIT_SENTENCE = "(\n|\r\n){2,5}";

    private final Logger log = LogManager.getLogger(getClass());

    public static final String END_PAGE = "______END___PAGE______";
    public static final String START_PAGE = "______START_PAGE______";
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

        TokenizerModel tokenizerModel = new TokenizerModel(new FileInputStream(new File(dir, "en-token.bin")));
        TokenNameFinderModel modelCitation = new TokenNameFinderModel(new FileInputStream(new File(dir, SourceType.CITATION.getOutputFilename())));
        TokenNameFinderModel modelSitename = new TokenNameFinderModel(new FileInputStream(new File(dir, SourceType.SITE.getOutputFilename())));
        TokenNameFinderModel modelCulture = new TokenNameFinderModel(new FileInputStream(new File(dir, SourceType.CULTURE.getOutputFilename())));
        TokenNameFinderModel modelCeramic = new TokenNameFinderModel(new FileInputStream(new File(dir, SourceType.CERAMIC.getOutputFilename())));
        TokenNameFinderModel modelObject = new TokenNameFinderModel(new FileInputStream(new File(dir, SourceType.OBJECTS.getOutputFilename())));
        TokenNameFinderModel modelDate = new TokenNameFinderModel(new FileInputStream(new File(dir, "en-ner-date.bin")));
        TokenNameFinderModel modelFeature = new TokenNameFinderModel(new FileInputStream(new File(dir, SourceType.FEATURES.getOutputFilename())));
        TokenNameFinderModel modelPerson = new TokenNameFinderModel(new FileInputStream(new File(dir, "en-ner-person.bin")));
        TokenNameFinderModel modelCustomPerson = new TokenNameFinderModel(new FileInputStream(new File(dir, SourceType.PERSON.getOutputFilename())));
        TokenNameFinderModel modelCustomOrganization = new TokenNameFinderModel(new FileInputStream(new File(dir, SourceType.INSTITUTION.getOutputFilename())));
        TokenNameFinderModel modelCustomLocation = new TokenNameFinderModel(new FileInputStream(new File(dir, SourceType.LOCATION.getOutputFilename())));
        TokenNameFinderModel modelOrganization = new TokenNameFinderModel(new FileInputStream(new File(dir, "en-ner-organization.bin")));
        TokenNameFinderModel modelLocation = new TokenNameFinderModel(new FileInputStream(new File(dir, "en-ner-location.bin")));
        TokenNameFinderModel modelMaterial = new TokenNameFinderModel(new FileInputStream(new File(dir, SourceType.MATERIAL.getOutputFilename())));

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
        NLPHelper person = new NLPHelper(SourceType.PERSON, modelPerson, modelCustomPerson);
        NLPHelper date = new NLPHelper(SourceType.DATE, modelDate);
        
        NLPHelper site = new NLPHelper(SourceType.SITE, modelSitename);
        NLPHelper feature = new NLPHelper(SourceType.FEATURES, modelFeature);
        NLPHelper culture = new NLPHelper(SourceType.CULTURE, modelCulture);
        NLPHelper ceramic = new NLPHelper(SourceType.CERAMIC, modelCeramic);
        NLPHelper object = new NLPHelper(SourceType.OBJECTS, modelObject);
        NLPHelper material = new NLPHelper(SourceType.MATERIAL, modelMaterial);
        site.setBoostValues(Arrays.asList("site","ruin","excavation"));
        NLPHelper cite = new NLPHelper(SourceType.CITATION, modelCitation);
        cite.setRegexBoost(".+\\d++.*");
        
        NLPHelper institution = new NLPHelper(SourceType.INSTITUTION, modelCustomOrganization, modelOrganization);
        institution.setBoostValues(
                Arrays.asList("inc.", "co.", "university", "college", "museum", "company", "llc", "ltd", " of ", "office", "services", "society"));
        person.setMinTermLength(4);
        // matching initials
//        person.setRegexBoost("(.+\\s\\w\\.\\s.+|\\w\\.\\s\\.+)");
//        person.setBoostValues(Arrays.asList("dr.", "mr.", "mrs."));
        person.setMinPercentNumberWords(2);
        person.setTermIgnore(Arrays.asList("mesa"));
        person.setSkipPreviousTerms(Arrays.asList("of", "in", "the"));
        NLPHelper location = new NLPHelper(SourceType.LOCATION, modelCustomLocation, modelLocation);
        location.setBoostValues(Arrays.asList("valley", "mountain", "state", "country", "county", "city", "town", "base",
                "hill", "ranch"));
        int pos = 0;
        int pageNum = 1;
        NlpDocument doc = new NlpDocument(filename);
        if (includePerson) {
            doc.getHelpers().add(person);
        }
        doc.getHelpers().add(material);
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
        doc.getHelpers().add(date);
        doc.getHelpers().add(feature);
        doc.getHelpers().add(ceramic);
        doc.getHelpers().add(object);
        NlpPage page = new NlpPage(pageNum);
        List<String> cultures = new ArrayList<>();
        Pattern testPattern = Pattern.compile("("+StringUtils.join(cultures,"|")+")");

        Pattern[] patterns = new Pattern[]{testPattern};
        Map<String, Pattern[]> regexMap = new HashMap<>();
        String type = "testtype";

        regexMap.put(type, patterns);

        RegexNameFinder finder =         new RegexNameFinder(regexMap);
        
        boolean seenApachePagemarker = false;
        
        String sentence = "";
        if (input.contains(END_PAGE)) {
            seenApachePagemarker = true;
        }
        for (String sentence__ : sentenceDetector.sentDetect(input)) {
            // remove page #'s
            // as we get better here, for performance, this could be moved up into the cached text above
            sentence__ = sentence__.replaceAll("(?:^|\r?\n)[0-9]+(\r?\n)+"+END_PAGE, END_PAGE);
            
            for (String sentence_ : sentence__.split(SPLIT_SENTENCE)) {
                sentence = sentence_;

                log.trace("::" + sentence);
                if (sentence.contains(END_PAGE) || (seenApachePagemarker == false && sentence.contains("     ----------------------------------------------------------------------"))) {
                    sentence = addPage(doc, page, sentence);
                    pageNum++;
                    page = new NlpPage(pageNum);
                } else {
                    page.addSentence(sentence);
                }
                if (includeSiteCode) {
                    page.extractSiteCodes(sentence);
                }
                Tokenizer tokenizer = new TokenizerME(tokenizerModel);
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
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
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

    private void processResults(NlpPage page, TokenNameFinderModel model3, String[] tokens, int pos, NLPHelper helper) {
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
                    log.trace("skipping: " + prev + " " + name);
                    continue;
                }
            }
            String key = Utils.cleanString(name);
            if (!helper.stringValid(key)) {
                return;
            }
//            if (helper.getType().equalsIgnoreCase("person") && StringUtils.indexOfIgnoreCase(key, "jerry howard") > -1) {
//                log.debug("{} - {}", page.getPageNumber(), key);
//            }
            page.addSpan(span, key, pos, helper.getType());
        }
        nameFinder.clearAdaptiveData();
    }

    public double getMinProbability() {
        return minProbability;
    }

}
