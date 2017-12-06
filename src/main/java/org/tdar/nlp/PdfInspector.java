package org.tdar.nlp;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDDocumentNameDictionary;
import org.apache.pdfbox.pdmodel.PDEmbeddedFilesNameTreeNode;
import org.apache.pdfbox.pdmodel.common.PDNameTreeNode;
import org.apache.pdfbox.pdmodel.common.filespecification.PDComplexFileSpecification;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.pdmodel.graphics.optionalcontent.PDOptionalContentGroup;
import org.apache.pdfbox.pdmodel.graphics.optionalcontent.PDOptionalContentProperties;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;

import edu.emory.mathcs.backport.java.util.Arrays;

public class PdfInspector {

    static final Logger logger = LogManager.getLogger(App.class);

    public void inspect(File file) throws InvalidPasswordException, IOException {
        PDDocument document = PDDocument.load(file, MemoryUsageSetting.setupTempFileOnly());
        PDDocumentCatalog cat = document.getDocumentCatalog();
        int hasForm = 0;
        int hasLayers = 0;
        boolean hasEmbeddedFiles = false;
        PDAcroForm form = cat.getAcroForm();
        if (form != null) {
            logger.debug("{}\tfields:\t{}", file.getName(), form.getFields());
            if (form.getFields().size() > 0 || form.hasXFA() == true) {
                hasForm = form.getFields().size();
                if (hasForm == 0 && form.hasXFA()) {
                    hasForm = 200;
                }
            }
        }
        PDOptionalContentProperties layers = cat.getOCProperties();
        if (layers != null) {
            List<String> names = new ArrayList<>();
            try {
                names.addAll(Arrays.asList(layers.getGroupNames()));
            } catch (Exception e) {
                hasLayers = -100;
            }
            if (names.size() == 0) {
                try {
                    if (CollectionUtils.isNotEmpty(layers.getOptionalContentGroups())) {
                        for (PDOptionalContentGroup group : layers.getOptionalContentGroups()) {
                            names.add(group.getName());
                        }
                    }
                } catch (Exception e) {
                    hasLayers = -100;
                }
            }
            logger.debug("{}\tlayers:\t{}", file.getName(), names);
            hasLayers = names.size();
        }
        // http://www.javased.com/?api=org.apache.pdfbox.pdmodel.PDDocumentCatalog
        PDDocumentNameDictionary names = cat.getNames();
        if (names != null) {
            PDEmbeddedFilesNameTreeNode embeddedFiles = names.getEmbeddedFiles();
            if (embeddedFiles != null && embeddedFiles.getNames() != null) {
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
                logger.error("error with: {} [{}]", file, e, e);
            }
        }
    }

}
