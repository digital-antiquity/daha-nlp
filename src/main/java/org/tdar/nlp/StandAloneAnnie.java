/*
 *  StandAloneAnnie.java
 *
 *
 * Copyright (c) 2000-2001, The University of Sheffield.
 *
 * This file is part of GATE (see http://gate.ac.uk/), and is free
 * software, licenced under the GNU Library General Public License,
 * Version 2, June1991.
 *
 * A copy of this licence is included in the distribution in the file
 * licence.html, and is also available at http://gate.ac.uk/gate/licence.html.
 *
 *  hamish, 29/1/2002
 *
 *  $Id: StandAloneAnnie.java,v 1.6 2006/01/09 16:43:22 ian Exp $
 */

package org.tdar.nlp;

import gate.Annotation;
import gate.AnnotationSet;
import gate.Corpus;
import gate.CorpusController;
import gate.Document;
import gate.Factory;
import gate.FeatureMap;
import gate.Gate;
import gate.GateConstants;
import gate.corpora.RepositioningInfo;
import gate.util.GateException;
import gate.util.persistence.PersistenceManager;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class illustrates how to use ANNIE as a sausage machine
 * in another application - put ingredients in one end (URLs pointing
 * to documents) and get sausages (e.g. Named Entities) out the
 * other end.
 * <P>
 * <B>NOTE:</B><BR>
 * For simplicity's sake, we don't do any exception handling.
 */
public class StandAloneAnnie {

    private final transient static Logger logger = LoggerFactory.getLogger(StandAloneAnnie.class);
    final static String startTagPart_1 = "";// "<span GateID=\"";
    final static String startTagPart_2 = "";// "\" title=\"";
    final static String startTagPart_3 = "";// "\" style=\"background:Red;\">";
    final static String endTag = "";// "</span>";

    /** The Corpus Pipeline application to contain ANNIE */
    private CorpusController annieController;

    /**
     * Initialise the ANNIE system. This creates a "corpus pipeline"
     * application that can be used to run sets of documents through
     * the extraction system.
     */
    public void initAnnie() throws GateException, IOException {
        logger.debug("Initialising ANNIE...");

        // load the ANNIE application from the saved state in plugins/ANNIE
        File pluginsHome = Gate.getPluginsHome();
        File anniePlugin = new File(pluginsHome, "ANNIE");
        File annieGapp = new File(anniePlugin, "ANNIE_with_defaults.gapp");
        annieController = (CorpusController) PersistenceManager.loadObjectFromFile(annieGapp);

        logger.debug("...ANNIE loaded");
    } // initAnnie()

    /** Tell ANNIE's controller about the corpus you want to run on */
    public void setCorpus(Corpus corpus) {
        annieController.setCorpus(corpus);
    } // setCorpus

    /** Run ANNIE */
    public void execute() throws GateException {
        logger.debug("Running ANNIE...");
        annieController.execute();
        logger.debug("...ANNIE complete");
    } // execute()

    /**
     * Run from the command-line, with a list of URLs as argument.
     * <P>
     * <B>NOTE:</B><BR>
     * This code will run with all the documents in memory - if you want to unload each from memory after use, add code to store the corpus in a DataStore.
     */
    public static void main(String args[]) throws GateException, IOException {
        // initialise the GATE library
        logger.debug("Initialising GATE...");
        System.setProperty("gate.home", "gate/");
        System.setProperty("gate.plugins.home", "gate/plugins/");
        System.setProperty("gate.site.config", "gate/gate.xml");
        System.setProperty("java.awt.headless", "true");
        Gate.init();
        logger.debug("...GATE initialised");

        // initialise ANNIE (this may take several minutes)
        StandAloneAnnie annie = new StandAloneAnnie();
        annie.initAnnie();

        // create a GATE corpus and add a document for each command-line
        // argument
        Corpus corpus = Factory.newCorpus("StandAloneAnnie corpus");
        for (int i = 0; i < args.length; i++) {
            URL u = new URL(args[i]);
            FeatureMap params = Factory.newFeatureMap();
            params.put("sourceUrl", u);
            params.put("preserveOriginalContent", new Boolean(true));
            params.put("collectRepositioningInfo", new Boolean(true));
            logger.debug("Creating doc for " + u);
            Document doc = (Document)Factory.createResource("gate.corpora.DocumentImpl", params);
            corpus.add(doc);
        } // for each of args

        // tell the pipeline about the corpus and run it
        annie.setCorpus(corpus);
        logger.debug("Start:" + new Date());
        annie.execute();
        logger.debug("Done:" + new Date());

        // for each document, get an XML document with the
        // person and location names added
        Iterator<Document> iter = corpus.iterator();
        int count = 0;
        Map<String, Map<String, Integer>> types = new HashMap<>();

        while (iter.hasNext()) {
            Document doc = (Document) iter.next();
            AnnotationSet defaultAnnotSet = doc.getAnnotations();
            Set<String> annotTypesRequired = new HashSet<>();
            annotTypesRequired.add("Person");
            annotTypesRequired.add("Months");
            annotTypesRequired.add("Location");
            Set<Annotation> peopleAndPlaces = new HashSet<Annotation>(defaultAnnotSet.get(annotTypesRequired));

            FeatureMap features = doc.getFeatures();
            String originalContent = (String) features.get(GateConstants.ORIGINAL_DOCUMENT_CONTENT_FEATURE_NAME);
            RepositioningInfo info = (RepositioningInfo) features.get(GateConstants.DOCUMENT_REPOSITIONING_INFO_FEATURE_NAME);

            count++;
            File file = new File("StANNIE_" + count + ".HTML");
            logger.debug("File name: '" + file.getAbsolutePath() + "'");
            if (originalContent != null && info != null) {
                logger.debug("OrigContent and reposInfo existing. Generate file...");
                process(types, peopleAndPlaces, originalContent, info, file);
            } // if - should generate
            else if (originalContent != null) {
                logger.debug("OrigContent existing. Generate file...");
                process(types, peopleAndPlaces, originalContent, null, file);
            }
            else {
                logger.debug("Content : " + originalContent);
                logger.debug("Repositioning: " + info);
            }

            for (String key : types.keySet()) {
                NLPHelper.printInOccurrenceOrder(key, types.get(key));
            }

        } // for each doc
    } // main

    private static StringBuffer process(Map<String, Map<String, Integer>> types, Set<Annotation> peopleAndPlaces, String originalContent,
            RepositioningInfo info, File file) throws IOException {
        Iterator<Annotation> it = peopleAndPlaces.iterator();
        Annotation currAnnot;
        SortedAnnotationList sortedAnnotations = new SortedAnnotationList();

        while (it.hasNext()) {
            currAnnot = (Annotation) it.next();
            sortedAnnotations.addSortedExclusive(currAnnot);
        } // while

        StringBuffer editableContent = new StringBuffer(originalContent);
        long insertPositionEnd;
        long insertPositionStart;
        // insert anotation tags backward
        logger.debug("Unsorted annotations count: " + peopleAndPlaces.size());
        logger.debug("Sorted annotations count: " + sortedAnnotations.size());
        for (int i = sortedAnnotations.size() - 1; i >= 0; --i) {
            currAnnot = (Annotation) sortedAnnotations.get(i);
            insertPositionStart = currAnnot.getStartNode().getOffset().longValue();
            insertPositionEnd = currAnnot.getEndNode().getOffset().longValue();
            if (info != null) {
                insertPositionStart = info.getOriginalPos(insertPositionStart);
                insertPositionEnd = info.getOriginalPos(insertPositionEnd, true);
            }

            if (insertPositionEnd != -1 && insertPositionStart != -1) {
                if (!types.containsKey(currAnnot.getType())) {
                    types.put(currAnnot.getType(), new HashMap<String, Integer>());
                }
                increment(currAnnot, editableContent, insertPositionEnd, insertPositionStart, types);
                editableContent.insert((int) insertPositionEnd, endTag);
                editableContent.insert((int) insertPositionStart, startTagPart_3);
                editableContent.insert((int) insertPositionStart, currAnnot.getType());
                editableContent.insert((int) insertPositionStart, startTagPart_2);
                editableContent.insert((int) insertPositionStart, currAnnot.getId().toString());
                editableContent.insert((int) insertPositionStart, startTagPart_1);

            } // if
        } // for

        FileWriter writer = new FileWriter(file);
        writer.write(editableContent.toString());
        writer.close();

        return editableContent;
    }

    private static void increment(Annotation currAnnot, StringBuffer editableContent, long insertPositionEnd, long insertPositionStart,
            Map<String, Map<String, Integer>> types) {
        Map<String, Integer> map = types.get(currAnnot.getType());
        String str = editableContent.substring((int) insertPositionStart, (int) insertPositionEnd);
        str = NLPHelper.cleanString(str);
        if (!map.containsKey(str)) {
            map.put(str, 1);
        } else {
            map.put(str, map.get(str) + 1);
        }
    }

    /**
   *
   */
    public static class SortedAnnotationList extends Vector<Annotation> {
        private static final long serialVersionUID = -5577934614761502989L;

        public SortedAnnotationList() {
            super();
        } // SortedAnnotationList

        public boolean addSortedExclusive(Annotation annot) {
            Annotation currAnot = null;

            // overlapping check
            for (int i = 0; i < size(); ++i) {
                currAnot = (Annotation) get(i);
                if (annot.overlaps(currAnot)) {
                    return false;
                } // if
            } // for

            long annotStart = annot.getStartNode().getOffset().longValue();
            long currStart;
            // insert
            for (int i = 0; i < size(); ++i) {
                currAnot = (Annotation) get(i);
                currStart = currAnot.getStartNode().getOffset().longValue();
                if (annotStart < currStart) {
                    insertElementAt(annot, i);
                    logger.trace("Insert start: " + annotStart + " at position: " + i + " size=" + size());
                    logger.trace("Current start: " + currStart);
                    return true;
                } // if
            } // for

            int size = size();
            insertElementAt(annot, size);
            logger.trace("Insert start: " + annotStart + " at size position: " + size);
            return true;
        } // addSorted
    } // SortedAnnotationList
} // class StandAloneAnnie
