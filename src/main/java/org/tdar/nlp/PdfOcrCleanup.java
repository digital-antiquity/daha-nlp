package org.tdar.nlp;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

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

public class PdfOcrCleanup {

    Logger log = LogManager.getLogger(getClass());
    
    public File processFile(File file, boolean html) throws IOException, InvalidPasswordException {
        String filename = file.getName();
        // if we're a PDF extract the text
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
            // if we have a file already, ignore
            if (textfile.exists()) {
                file = textfile;
            } else {
                stripper.setAddMoreFormatting(true);
                stripper.setPageStart(DocumentAnalyzer.START_PAGE);
                stripper.setPageEnd(DocumentAnalyzer.END_PAGE);
                // stripper.set
                PDDocument pdDoc = PDDocument.load(file, MemoryUsageSetting.setupMixed(Runtime.getRuntime().freeMemory() / 5L));
                StringWriter sw = new StringWriter();
                stripper.writeText(pdDoc, sw);
                pdDoc.close();

                StringBuilder sb2 = cleanupArtifacts(sw);
                IOUtils.write(sb2.toString(), new FileWriter(textfile));

                file = textfile;
            }
        }
        return file;
    }


    /**
     * for each of these string pairs which appear to be common issues with our OCR engine, replace left with right
     */
    private StringBuilder cleanupArtifacts(StringWriter sw) {
        // as we get better here, for performance, this could be moved up into the cached text above
        List<String[]> pairs = new ArrayList<>();
        pairs.add(new String[] { " o f ", " of " });
        pairs.add(new String[] { " w it", " wit" });
        pairs.add(new String[] { " w id", " wid" });
        pairs.add(new String[] { " w hi", " whi" });
        pairs.add(new String[] { " V a", " Va" });
        pairs.add(new String[] { " V e", " Ve" });
        pairs.add(new String[] { " W a", " Wa" });
        pairs.add(new String[] { " G la", " Gla" });
        pairs.add(new String[] { " W o", " Wo" });
        pairs.add(new String[] { "W ith", "With" });
        pairs.add(new String[] { " W e", " We" });
        pairs.add(new String[] { " W h", " Wh" });
        pairs.add(new String[] { " I f ", "If " });
        pairs.add(new String[] { " F loor ", "Floor " });
        pairs.add(new String[] { " i f ", " if " });
        pairs.add(new String[] { " L ind", " Lind" });
        pairs.add(new String[] { "ava­ tio", "ava­tio" });
        pairs.add(new String[] { " m idd", " midd" });
        pairs.add(new String[] { "prim ar", "primar" });
        pairs.add(new String[] { "tem poral", "temporal" });
        pairs.add(new String[] { " m o", " mo" });
        pairs.add(new String[] { "sp atial", "spatial" });
        pairs.add(new String[] { "-m or", "-mor" });
        pairs.add(new String[] { " m a", " ma" });
        pairs.add(new String[] { " m ic", " mic" });
        pairs.add(new String[] { " M o", " Mo" });
        pairs.add(new String[] { " N o", " No" });
        pairs.add(new String[] { " H a", " Ha" });
        pairs.add(new String[] { " H ei", " Hei" });
        pairs.add(new String[] { " H en", " Hen" });
        pairs.add(new String[] { " M e", " Me" });
        pairs.add(new String[] { " M i", " Mi" });
        pairs.add(new String[] { " M u", " Mu" });
        pairs.add(new String[] { " U n", " Un" });
        pairs.add(new String[] { " A riz", " Ariz" });
        pairs.add(new String[] { " A rc", " Arc" });
        pairs.add(new String[] { " A nthr", " Anthr" });
        pairs.add(new String[] { " D if", " Dif" });
        pairs.add(new String[] { "am et", "amet" });
        pairs.add(new String[] { "cum ent", "cument" });
        pairs.add(new String[] { " pow erf", " powerf" });
        pairs.add(new String[] { "dm ark", "dmark" });
        pairs.add(new String[] { "sm an", "sm an" });
        pairs.add(new String[] { "nw are", "nwware" });
        pairs.add(new String[] { "ari­ abi", "ari­abi" });
        pairs.add(new String[] { " com bin", " combin" });
        pairs.add(new String[] { "com pli", "compli" });
        pairs.add(new String[] { "com pa", "compa" });
        pairs.add(new String[] { "tm ent", "tment" });
        pairs.add(new String[] { " num ber", " number" });
        pairs.add(new String[] { "eden­ tar", "edentar" });
        pairs.add(new String[] { "im in", "imin" });
        pairs.add(new String[] { " m ethod", " method" });
        pairs.add(new String[] { "chrom e ", "chrome " });
        pairs.add(new String[] { "em pe", "empe" });
        pairs.add(new String[] { "eem ent", "eement" });
        pairs.add(new String[] { "hem ist", "hemist" });
        pairs.add(new String[] { "hem ic", "hemic" });
        pairs.add(new String[] { "fw are", "fware" });

        String txt_ = sw.toString();
        StringBuilder sb2 = new StringBuilder();
        for (String txt__ : txt_.split("[\r\n]")) {
            String orig = new String(txt__);
            String  txt = txt__;
            for (String[] pair : pairs) {
                txt = txt.replaceAll(pair[0], pair[1]);
            }

            if (txt.indexOf(" ") == 1 && (txt.indexOf("A") != 0) && txt.indexOf("I") != 0) {
                txt = StringUtils.replaceOnce(txt, " ", "");
            }
            if (!txt.equals(orig)) {
                log.trace(orig);
                log.trace(txt);
            }
            sb2.append(txt);
            sb2.append("\n");
        }
        return sb2;
    }

}
