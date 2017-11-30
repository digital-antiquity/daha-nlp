package org.tdar.nlp;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

public class ModelDownloader {

    private static final String OPENNLP_MODEL_VER = "1.5";
    private static final String OPENNLP_URL = "http://opennlp.sourceforge.net/models-" + OPENNLP_MODEL_VER + "/";

    public static File downloadModels() throws MalformedURLException, IOException, FileNotFoundException {
        List<URL> urls = new ArrayList<URL>();
        urls.add(new URL(OPENNLP_URL + "en-sent.bin"));
        urls.add(new URL(OPENNLP_URL + "en-token.bin"));
        urls.add(new URL(OPENNLP_URL + "en-ner-organization.bin"));
        urls.add(new URL(OPENNLP_URL + "en-ner-date.bin"));
        urls.add(new URL(OPENNLP_URL + "en-ner-time.bin"));
        urls.add(new URL(OPENNLP_URL + "en-ner-person.bin"));
        urls.add(new URL(OPENNLP_URL + "en-ner-location.bin"));
        urls.add(new URL(OPENNLP_URL + "en-pos-maxent.bin"));
        File dir = new File("models");
        dir.mkdir();
        for (URL url : urls) {
            String urlToLoad = StringUtils.substringAfter(url.toString(), OPENNLP_MODEL_VER + "/");
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
}
