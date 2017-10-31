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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gate.Annotation;
import gate.AnnotationSet;
import gate.Corpus;
import gate.Document;
import gate.Factory;
import gate.FeatureMap;
import gate.Gate;
import gate.GateConstants;
import gate.LanguageAnalyser;
import gate.ProcessingResource;
import gate.corpora.RepositioningInfo;
import gate.creole.ConditionalSerialAnalyserController;
import gate.util.GateException;
import gate.util.persistence.PersistenceManager;

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
    private static URL docUrl;

    /** The Corpus Pipeline application to contain ANNIE */
    private ConditionalSerialAnalyserController annieController;
    private LanguageAnalyser mainGazetteer;
    private Corpus corpus;

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
        annieController = (ConditionalSerialAnalyserController) PersistenceManager.loadObjectFromFile(annieGapp);

        ProcessingResource token = (ProcessingResource) Factory.createResource("gate.creole.tokeniser.DefaultTokeniser", Factory.newFeatureMap());

        // corpus = (Corpus) Factory.createResource("gate.corpora.CorpusImpl");
        // corpus.add(doc);
        File gateHome = Gate.getGateHome();

        Gate.getCreoleRegister().registerDirectories(new File(pluginsHome, "ANNIE").toURI().toURL());
        // http://stackoverflow.com/questions/15041809/add-custom-jape-file-in-gate-source-code
        // LanguageAnalyser jape = (LanguageAnalyser)Factory.createResource(
        // "gate.creole.Transducer", gate.Utils.featureMap(
        // "grammarURL", "file:///D:/misc_workspace/gate-7.1-build4485-SRC/plugins/ANNIE/resources/NE/SportsCategory.jape","encoding", "UTF-8"));
        // jape.setCorpus(corpus);
        // jape.setDocument(doc);
        // jape.execute();

        // annieController = (ConditionalSerialAnalyserController) Factory.createResource("gate.creole.ConditionalSerialAnalyserController",
        // Factory.newFeatureMap(), Factory.newFeatureMap(),"ANNIE");
        FeatureMap params = Factory.newFeatureMap();
        params.put("listsURL", "file:///Users/abrin/Dropbox (ASU)/nlp-playground/nlp/gate/tdar/lists.def");
        try {
        mainGazetteer = (LanguageAnalyser) Factory.createResource("gate.creole.gazetteer.DefaultGazetteer", params);
        annieController.add((ProcessingResource) mainGazetteer.init());
        } catch(Exception e) {
            logger.debug(e.getMessage());
            e.printStackTrace();
        }

        logger.debug("...ANNIE loaded");
    } // initAnnie()

    /** Tell ANNIE's controller about the corpus you want to run on */
    public void setCorpus(Corpus corpus) {
        annieController.setCorpus(corpus);
        this.corpus = corpus;
    } // setCorpus

    /**
     * Run ANNIE
     * 
     * @throws MalformedURLException
     */
    public void execute() throws GateException, MalformedURLException {
        logger.debug("Running ANNIE...");
        annieController.execute();
        // FeatureMap params = Factory.newFeatureMap();
        // params.put("sourceUrl", docUrl);
        // params.put("preserveOriginalContent", new Boolean(true));
        // params.put("collectRepositioningInfo", new Boolean(true));
        // mainGazetteer.setDocument((Document) Factory.createResource("gate.corpora.DocumentImpl", params));
        // mainGazetteer.setCorpus(corpus);
        // mainGazetteer.execute();
        // AnnotationSetImpl ann = (AnnotationSetImpl) mainGazetteer.getDocument().getAnnotations();
        // System.out.println(" ...Total annotation: " + ann.getAllTypes());
        // for (int i = ann.size() - 1; i >= 0; --i) {
        // Annotation currAnnot = (Annotation) ann.get(i);
        // System.out.println(currAnnot.getFeatures());
        // }
        //
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
        annie.setCorpus(corpus);
        docUrl = new URL(args[0]);
        FeatureMap params = Factory.newFeatureMap();
        params.put("sourceUrl", docUrl);
        params.put("preserveOriginalContent", new Boolean(true));
        params.put("collectRepositioningInfo", new Boolean(true));
        logger.debug("Creating doc for " + docUrl);
        Document doc = (Document) Factory.createResource("gate.corpora.DocumentImpl", params);
        corpus.add(doc);

        // tell the pipeline about the corpus and run it
        annie.setCorpus(corpus);
        logger.debug("Start:" + new Date());
        annie.execute();
        logger.debug("Done:" + new Date());

        // for each document, get an XML document with the
        int count = 0;
        Map<String, Map<String, Integer>> types = new HashMap<>();

        AnnotationSet defaultAnnotSet = doc.getAnnotations();
        Iterator<Annotation> iterator = defaultAnnotSet.iterator();
        Set<String> tp = new HashSet<String>();
        while (iterator.hasNext()) {
            Annotation ann = iterator.next();
            tp.add(ann.getType());
        }

        System.out.println(tp);
        Set<String> annotTypesRequired = new HashSet<>();
        // [YearTemp, Organization, Address, Percent, Discard, Token, Date, Money, Identifier, Unknown, Lookup,
        // SpaceToken, Split, Sentence, Person, NumberLetter, Location]

        annotTypesRequired.add("Person");
        annotTypesRequired.add("Location");
        annotTypesRequired.add("Identifier");
        annotTypesRequired.add("Organization");
        annotTypesRequired.add("Lookup");
        annotTypesRequired.add("Test");
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
            for (Entry<String,Integer> ent : types.get(key).entrySet()) {
                System.out.println(key + "\t" + ent.getValue() + "\t" + ent.getKey());
            }
//            NLPHelper.printInOccurrenceOrder(key, types.get(key));
        }

    }

    private static StringBuffer process(Map<String, Map<String, Integer>> types, Set<Annotation> peopleAndPlaces, String originalContent,
            RepositioningInfo info, File file) throws IOException {
        Iterator<Annotation> it = peopleAndPlaces.iterator();
        Annotation currAnnot;
        SortedAnnotationList sortedAnnotations = new SortedAnnotationList();

        while (it.hasNext()) {
            currAnnot = it.next();
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
            Object majorType = currAnnot.getFeatures().get("majorType");
            // System.out.println(currAnnot.getId() + " |" + majorType);
            if (insertPositionEnd != -1 && insertPositionStart != -1) {
                String type = currAnnot.getType();
                if (majorType != null) {
                    type = (String) majorType;
                }
                if (!types.containsKey(type)) {
                    types.put(type, new HashMap<String, Integer>());
                }
                increment(type, editableContent, insertPositionEnd, insertPositionStart, types);
                editableContent.insert((int) insertPositionEnd, endTag);
                editableContent.insert((int) insertPositionStart, startTagPart_3);
                editableContent.insert((int) insertPositionStart, type);
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

    private static void increment(String type, StringBuffer editableContent, long insertEnd, long insertStart, Map<String, Map<String, Integer>> types) {
        Map<String, Integer> map = types.get(type);
        String str = editableContent.substring((int) insertStart, (int) insertEnd);
        //str = NLPHelper.cleanString(str);
        String first = type.substring(0, 1);
        if (first.toUpperCase().equals(first)) {
            if (!map.containsKey(str)) {
                map.put(str, 1);
            } else {
                map.put(str, map.get(str) + 1);
            }
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
