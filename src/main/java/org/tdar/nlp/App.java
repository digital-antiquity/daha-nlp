package org.tdar.nlp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.util.Span;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import com.bericotech.clavin.GeoParser;
import com.bericotech.clavin.GeoParserFactory;
import com.bericotech.clavin.resolver.ResolvedLocation;

/**
 * Hello world!
 *
 */
public class App
{
    public static void main(String[] args) throws Exception
    {
        String input = new String(
                "Pierre Vinken, 61 years old, will join the board as a nonexecutive director Nov. 29. Mr. Vinken is chairman of Elsevier N.V., the "
                        + "Dutch publishing group in Amsterdam. Rudolph Agnew, 55 years old and former chairman of Consolidated Gold Fields PLC,  was named "
                        + "a director of this British industrial conglomerate.");

        if (args[0] != null) {
            File file = new File(args[0]);
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
                String filename = StringUtils.substringAfter(url.toString(), "1.5/");
                File f = new File(filename);
                if (!f.exists()) {
                    ReadableByteChannel rbc = Channels.newChannel(url.openStream());
                    FileOutputStream fos = new FileOutputStream(filename);
                    fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
                }
            }

            SentenceModel sModel = new SentenceModel(new FileInputStream("en-sent.bin"));
            SentenceDetectorME sentenceDetector = new SentenceDetectorME(sModel);
            TokenizerModel tModel = new TokenizerModel(new FileInputStream("en-token.bin"));
            TokenNameFinderModel model = new TokenNameFinderModel(new FileInputStream("en-ner-person.bin"));
            TokenNameFinderModel model2 = new TokenNameFinderModel(new FileInputStream("en-ner-organization.bin"));
            TokenNameFinderModel model3 = new TokenNameFinderModel(new FileInputStream("en-ner-location.bin"));
            for (String sentence : sentenceDetector.sentDetect(input)) {
                Tokenizer tokenizer = new TokenizerME(tModel);
                String tokens[] = tokenizer.tokenize(sentence);
                Map<String, Integer> ocur = processResults(model, tokens);
                NLPHelper.printInOccurrenceOrder("Person", ocur);
                ocur = processResults(model2, tokens);
                NLPHelper.printInOccurrenceOrder("Institution", ocur);
                ocur = processResults(model3, tokens);
                NLPHelper.printInOccurrenceOrder("Location", ocur);
            }

            // DoccatModel m = new DoccatModel(new FileInputStream(""));
            // DocumentCategorizerME myCategorizer = new DocumentCategorizerME(m);
            // double[] outcomes = myCategorizer.categorize(input);
            // for (int category = 0; category < myCategorizer.getNumberOfCategories(); category++) {
            // System.out.println("Category: " + myCategorizer.getCategory(category));
            // }
            //

            
            if (false) {
                GeoParser parser = GeoParserFactory.getDefault("./IndexDirectory");

                // Unstructured text file about Somalia to be geoparsed

                // Parse location names in the text into geographic entities
                List<ResolvedLocation> resolvedLocations = parser.parse(input);

                // Display the ResolvedLocations found for the location names
                for (ResolvedLocation resolvedLocation : resolvedLocations) {
                    System.out.println(resolvedLocation);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    private static Map<String,Integer> processResults(TokenNameFinderModel model3, String[] tokens) {
        Map<String,Integer> ocur = new HashMap<>();
        // https://opennlp.apache.org/documentation/1.5.3/manual/opennlp.html
        NameFinderME nameFinder = new NameFinderME(model3);

        Span[] names;
        names = nameFinder.find(tokens);
        for (String name : Span.spansToStrings(names, tokens)) {
            String key = NLPHelper.cleanString(name);
            if (ocur.containsKey(key)) {
                ocur.put(key, ocur.get(key) + 1);
            } else {
                ocur.put(key, 1);
            }
        }
        nameFinder.clearAdaptiveData();
        return ocur;
    }
}