package org.tdar.nlp;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDDocumentNameDictionary;
import org.apache.pdfbox.pdmodel.PDEmbeddedFilesNameTreeNode;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.pdmodel.graphics.optionalcontent.PDOptionalContentProperties;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;

public class PdfInspector {

    static final Logger logger = LogManager.getLogger(App.class);

    public void inspect(File file) throws InvalidPasswordException, IOException {
        PDDocument document = PDDocument.load(file, MemoryUsageSetting.setupTempFileOnly());
        PDDocumentCatalog cat = document.getDocumentCatalog();
        boolean hasForm = false;
        boolean hasLayers = false;
        boolean hasEmbeddedFiles = false;
        PDAcroForm form = cat.getAcroForm();
        if (form != null) {
            logger.debug("]t{}\tfields:\t{}",file.getName(), form.getFields());
            hasForm = true;
        }
        PDOptionalContentProperties layers = cat.getOCProperties();
        if (layers != null) {
            logger.debug("{}\tlayers:\t{}",file.getName(), layers.getGroupNames());
            hasLayers = true;
        }
        // http://www.javased.com/?api=org.apache.pdfbox.pdmodel.PDDocumentCatalog
        PDDocumentNameDictionary names = cat.getNames();
        if (names != null) {
            PDEmbeddedFilesNameTreeNode embeddedFiles = names.getEmbeddedFiles();
            if (embeddedFiles != null) {
                hasEmbeddedFiles = true;
                logger.debug("{}\tfiles:\t{}", file.getName(), embeddedFiles.getNames().keySet());
            }
        }
        document.getDocumentInformation().getCreator();
        logger.debug("{}\t{}\t{}\t{}\t{}", file.getName(), document.getVersion(), hasEmbeddedFiles, hasForm, hasLayers);

    }

    public static void main(String[] args) {
        File file = new File(args[0]);
        Set<File> files = new HashSet<>();
        if (file.isDirectory()) {
            files.addAll(FileUtils.listFiles(file, new String[] { "txt", "pdf" }, true));
        } else {
            files.add(file);
        }
        PdfInspector inspect = new PdfInspector();
        for (File f : files) {
            try {
                inspect.inspect(f);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                logger.error("error with: {} [{}]", file,e,e);
            }
        }
    }

}
