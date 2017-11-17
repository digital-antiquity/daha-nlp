package org.tdar.nlp;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.tools.PDFText2HTML;
import org.tdar.nlp.nlp.DocumentAnalyzer;

public class App {

    private static final String OPENNLP_MODEL_VER = "1.5";
    private static final String OPENNLP_URL = "http://opennlp.sourceforge.net/models-"+OPENNLP_MODEL_VER+"/";
    private static boolean html;
    static final Logger log = LogManager.getLogger(App.class);

    public static void main(String[] args) throws Exception {
        String input = new String(
                "Omar Turney then recommended him for a job with J.A. Mewkes at Elden Pueblo in Flagstaff; he went to Hohokam High School.");

        String filename = null;
        if (args != null && args.length > 0) {
            filename = args[0];
        }
        if (filename == null) {
//             filename = "/Users/abrin/Downloads/ABDAHA-2/Kelly-et-al-2010_OCR_PDFA.pdf";
            filename = "/Users/abrin/Downloads/ABDAHA-2/2001_Abbott_GreweArchaeologicalVol2PartI_OCR_PDFA.pdf";
            // filename = "tmp/hedgpeth-hills_locality-1_OCR_PDFA.txt";
        }

        File dir = downloadModels();
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
        for (File file_ : files) {
            log.debug(file_);
            try {
                File file = processFile(file_);
                input = FileUtils.readFileToString(file);
                DocumentAnalyzer app = new DocumentAnalyzer();
                app.run(file.getName(), input, dir);
            } catch (Exception e) {
                log.error("{}",e,e);
            }
        }

    }

    private static File processFile(File file) throws IOException, InvalidPasswordException {
        String filename = file.getName();
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
                stripper.setPageStart(DocumentAnalyzer.START_PAGE);
                stripper.setPageEnd(DocumentAnalyzer.END_PAGE);
                // stripper.set
                PDDocument pdDoc = PDDocument.load(file, MemoryUsageSetting.setupMixed(Runtime.getRuntime().freeMemory() / 5L));
                stripper.writeText(pdDoc, new FileWriter(textfile));
                pdDoc.close();
                file = textfile;
            }
        }
        return file;
    }

    private static File downloadModels() throws MalformedURLException, IOException, FileNotFoundException {
        List<URL> urls = new ArrayList<URL>();
        urls.add(new URL(OPENNLP_URL + "en-sent.bin"));
        urls.add(new URL(OPENNLP_URL + "en-token.bin"));
        urls.add(new URL(OPENNLP_URL + "en-ner-organization.bin"));
        urls.add(new URL(OPENNLP_URL + "en-ner-date.bin"));
        urls.add(new URL(OPENNLP_URL + "en-ner-time.bin"));
        urls.add(new URL(OPENNLP_URL + "en-ner-person.bin"));
        urls.add(new URL(OPENNLP_URL + "en-ner-location.bin"));
        File dir = new File("models");
        dir.mkdir();
        for (URL url : urls) {
            String urlToLoad = StringUtils.substringAfter(url.toString(), OPENNLP_MODEL_VER+"/");
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
