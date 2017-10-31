package org.tdar.nlp;

import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

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
        train("en-ner-citation.bin", "citation", "cite.train");
        train("en-ner-customperson.bin", "person", "person.train");
    }

    private static void train(String modelFile, String part, String trainFile) throws IOException, FileNotFoundException {
        InputStreamFactory isf = new InputStreamFactoryImplementation(trainFile);

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
            modelOut = new BufferedOutputStream(new FileOutputStream(modelFile));
            model.serialize(modelOut);
        } finally {
            if (modelOut != null)
                modelOut.close();
        }
    }
}
