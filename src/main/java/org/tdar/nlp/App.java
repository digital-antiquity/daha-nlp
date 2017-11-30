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

    private static boolean html;
    static final Logger log = LogManager.getLogger(App.class);

    
    //can we extract title by matching on common words?
    
    public static void main(String[] args) throws Exception {
        String input = new String(
                "Omar Turney then recommended him for a job with J.A. Mewkes at Elden Pueblo in Flagstaff; he went to Hohokam High School.");

        String filename = null;
        if (args != null && args.length > 0) {
            filename = args[0];
        }
        if (filename == null) {
            //Hohokam
            filename = "/Users/abrin/Dropbox (ASU)/PDFA-Analysis/lc4-abbyy12-pdfa.pdf";
//             filename = "/Users/abrin/Downloads/ABDAHA-2/Kelly-et-al-2010_OCR_PDFA.pdf";
//            filename = "/Users/abrin/Downloads/ABDAHA-2/2001_Abbott_GreweArchaeologicalVol2PartI_OCR_PDFA.pdf";
//             filename = "tmp/hedgpeth-hills_locality-1_OCR_PDFA.txt";
//             filename = "tmp/Underfleet1.html.txt";
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
            log.debug(file_);
            try {
                File file = cleaner.processFile(file_, html);

                input = FileUtils.readFileToString(file);
                DocumentAnalyzer app = new DocumentAnalyzer();
                app.run(file.getName(), input, dir);
            } catch (Exception e) {
                log.error("{}",e,e);
            }
        }

    }

}
