package org.tdar.nlp.train;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import org.apache.commons.io.FileUtils;
import org.tdar.nlp.SourceType;

import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.NameSample;
import opennlp.tools.namefind.NameSampleDataStream;
import opennlp.tools.namefind.TokenNameFinderFactory;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.util.InputStreamFactory;
import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.PlainTextByLineStream;
import opennlp.tools.util.TrainingParameters;

public class OpenNLPTrain {


    private static final class InputStreamFactoryImplementation implements InputStreamFactory {
        private final String trainFile;

        private InputStreamFactoryImplementation(String trainFile) {
            this.trainFile = trainFile;
        }

        public InputStream createInputStream() throws IOException {
            return new FileInputStream(trainFile);
        }
    }

    public static void main(String[] args) throws IOException {
        for (SourceType type: SourceType.values()) {
            train("en-ner-" + type.getTrainingFilename() + ".bin", type.name().toLowerCase(), type.name().toLowerCase() + ".train");
        }
//        train("en-ner-site.bin", "site", "site.train");
//        train("en-ner-culture.bin", "culture", "culture.train");
//        train("en-ner-ceramic.bin", "ceramic", "ceramic.train");
//        train("en-ner-custom-person.bin", "person", "person.train");
//        train("en-ner-custom-organization.bin", "organization", "organization.train");
//        train("en-ner-custom-location.bin", "location", "location.train");
    }

    private static void train(String modelFile, String part, String trainFile) throws IOException, FileNotFoundException {
        String path = "training/" + trainFile;
        File file = new File(path);
        if (!file.exists()) {
            return;
        }
        InputStreamFactory isf = new InputStreamFactoryImplementation(path);

        Charset charset = Charset.forName("UTF-8");
        ObjectStream<String> lineStream = new PlainTextByLineStream(isf, charset);
        ObjectStream<NameSample> sampleStream = new NameSampleDataStream(lineStream);

        TokenNameFinderModel model;
        TokenNameFinderFactory nameFinderFactory = new TokenNameFinderFactory();

        try {
            TrainingParameters defaultParams = TrainingParameters.defaultParams();
            model = NameFinderME.train("en", part, sampleStream, defaultParams,
                    nameFinderFactory);
        } finally {
            sampleStream.close();
        }

        BufferedOutputStream modelOut = null;

        try {
            modelOut = new BufferedOutputStream(new FileOutputStream("models/"+ modelFile));
            model.serialize(modelOut);
        } finally {
            if (modelOut != null)
                modelOut.close();
        }
    }
}
