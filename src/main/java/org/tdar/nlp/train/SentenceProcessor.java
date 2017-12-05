package org.tdar.nlp.train;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tdar.nlp.Utils;

import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;

public class SentenceProcessor {
    Logger log = LogManager.getLogger(getClass());

    private TokenizerModel tokenizerModel;
    private POSTaggerME tagger;
    private String tagName;
    private Collection<String> vocabularyList;
    private boolean caseSensitive = true;

    public SentenceProcessor(TokenizerModel tokenizerModel, POSTaggerME tagger, String tagName, Collection<String> list) {
        this.tokenizerModel = tokenizerModel;
        this.tagger = tagger;
        this.tagName = tagName;
        this.vocabularyList = list;
    }

    public SentenceResult processSentence(String sentence___) {
        String sentence__ = sentence___.replaceAll("[\r\n]", " ");
        sentence__ = sentence__.replaceAll("\\s+", " ");
        sentence__ = Utils.replaceSmartQuotes(sentence__);
        List<Integer> starts = new ArrayList<>();
        List<Integer> ends = new ArrayList<>();
        List<String> terms = new ArrayList<>();
        List<String> terms_ = new ArrayList<>();
        for (String term : vocabularyList) {
            if (StringUtils.containsIgnoreCase(sentence__, term)) {
            terms.add(new String(term));
            if (term.contains(" ")) {
                String term_ = StringUtils.replace(term, " ", "_");
                term_ = StringUtils.replace(term_,",", "_");
                term_ = StringUtils.replace(term_,".", "_");
                term_ = StringUtils.replace(term_,"__", "_");
                if (term_.endsWith(".")) {
                    term_ = term_.substring(0, term_.length() - 1);
                }
                sentence__ = sentence__.replaceAll("(?i)" + term, term_);
                term = term_;
            }
            terms_.add(term);
            }
        }
        if (!sentence__.equals(sentence___)) {
            log.trace(sentence__);
        }
        Tokenizer tokenizer = new TokenizerME(tokenizerModel);
        String[] words = tokenizer.tokenize(sentence__);
        String[] tags = tagger.tag(words);
        log.trace("matching terms in sentence: {}", terms);
        SentenceResult result = new SentenceResult();
//        if (sentence__.toLowerCase().contains("abbott") || sentence__.toLowerCase().contains("watkins"))  {
//            log.debug("{} -- {}", terms_, sentence__);
//        }
        for (String term : terms_) {
            log.trace(term);
            if (StringUtils.isBlank(term) || !StringUtils.containsIgnoreCase(sentence__, term)) {
                continue;
            }

            StringBuilder sent = new StringBuilder();
            for (int i = 0; i < words.length; i++) {
                sent.append(words[i]);
                sent.append("_");
                sent.append(tags[i]);
                sent.append(" ");
            }
            log.trace(" __ {} __ {}", term, sent.toString().trim());
            result.setTaggedSentence(sent.toString().trim());
            boolean reject = false;
            for (int i = 0; i < words.length; i++) {
                String word = words[i];
                // if (!tags[i].equals("FW") && !tags[i].contains("NP") ) {
                // continue;
                // }
                // //
                if (term.equalsIgnoreCase(word) && !caseSensitive || term.equals(word) && caseSensitive) {
                    log.trace("{}|{}", term, word);
                    int j = i + 1;
                    while (true) {
                        if (j > words.length - 1) {
                            log.trace("skipped because at end");
                            break;
                        }

                        // Pueblo del
                        String nextWord = words[j];
                        log.trace(nextWord);
                        if (nextWord.toLowerCase().equals("del") || nextWord.toLowerCase().equals("de") || nextWord.toLowerCase().contains("wall")) {
                            reject = true;
                        }
                        boolean containsValidSuffix = ArrayUtils.contains(new String[] { "indian", "indians", "empire", "culture", "population",
                                "pottery", "ceramics", "vessels", "ceramic", "vessel", "sherd", "shard", "period", "Period","age" }, nextWord.toLowerCase());
                        String nextTag = tags[j];
                        
                        if (containsValidSuffix || Utils.percentNumericCharacters(nextWord) > 1) {
                            log.trace("stopped because valid suffix / numeric [{}, {}]", nextWord, nextTag);
                            break;
                        }
                        
                        if (!nextTag.contains("NP") && !nextTag.contains("NN")) {
                            log.trace("stopped because next word is not noun [{}, {}]", nextWord, nextTag);
                            break;
                        }
                        j++;
                    }

                    int h = i - 1;
                    // Gila Pueblo
                    // of prescott
                    // in prescott
                    String previousTag = "";
                    String previousWord = "";
                    while (true) {
                        if (i > 0) {
                            previousTag = tags[h];
                            previousWord = words[h];
                        }
                        if (previousTag.equals("IN") || words[i].equalsIgnoreCase("pueblo") && termIsProperNoun(previousTag, previousWord)) {
                            reject = true;
                            log.trace("rejected because word is pueblo or previous word is a prepopsition ");
                            break;
                        }
                        
                        if (previousWord.equalsIgnoreCase("no") || previousWord.equalsIgnoreCase("not")) {
                            reject = true;
                            log.trace("rejected because negation");
                            break;
                        }

                        boolean containsTableTerm = ArrayUtils.contains(new String[] { "context","total", "type","class","sequence","comments" }, previousWord.toLowerCase());

                        if (containsTableTerm) {
                            reject = true;
                            log.trace("rejected because likely table");
                            break;
                        }
                        
                        boolean containsPrepositionOrThe = ArrayUtils.contains(new String[] {"at","from","the","ofthe","in", "of", "map","collections"}, previousWord.toLowerCase());
                        // fixme need to catch "classic period Hohokam"
                        if ((!ArrayUtils.contains(new String[] { "early", "middle", "late", "probably" }, previousWord) && !termIsProperNoun(previousTag, previousWord))
                                || containsPrepositionOrThe || StringUtils.containsAny(previousWord, new char[]{':','='})
                                || Utils.percentNumericCharacters(previousWord) > 0 || previousWord.length() < 2) {
                            log.trace("stopped previous word is not early/late/etc and not proper noun [{} , {}]", previousWord, previousTag);
                            h++;
                            break;
                        }
                        
                        if (h == 0) {
                            break;
                        }
                        h--;
                    }
//                    log.trace("{} ... {} [{}]", words[h], words[j], reject);
                    if (i != j - 1 || reject) {
                        log.trace("rejected {} {} | {}", i, j , reject);
                        continue;
                    }
                    log.trace("{} {} {}", term, h, j);
                    starts.add(h);
                    ends.add(j);
                    String out = "";
                    for (int a = h; a <= j - 1; a++) {
                        out += words[a] + " ";
                        log.trace(" :: {} -> {}", words[a], tags[a]);
                    }
                    result.getTags().add(out.trim().replace("_", " "));
                }
            }

        }
        result.setSentence(printNewString(starts, ends, terms, terms_, words, tagName));
        return result;
    }

    private boolean termIsProperNoun(String previousTag, String previousWord) {
        if (StringUtils.isBlank(previousWord)) {
            return true;
        }
        String firstLetter = previousWord.substring(0, 1);
        if (Utils.isPunctuation(firstLetter)) {
            return false;
        }
        return previousTag.contains("NP") || firstLetter.equals(StringUtils.upperCase(firstLetter));
    }

    private String printNewString(List<Integer> starts, List<Integer> ends, List<String> terms, List<String> terms_, String[] words, String tagName) {
        StringBuilder sb = new StringBuilder();
        if (CollectionUtils.isEmpty(starts)) {
            return null;
        }
        for (int x = 0; x < words.length; x++) {
            if (starts.contains(x)) {
                sb.append(" <START:");
                sb.append(tagName);
                sb.append(">");
            }
            if (ends.contains(x)) {
                sb.append(" <END>");
            }
            sb.append(" ");
            String str = words[x];
            int indexOf = terms_.indexOf(str);
            if (indexOf > -1) {
                sb.append(terms.get(indexOf));
            } else {
                sb.append(str);
            }
        }
        if (sb.indexOf("<END>") == -1 && sb.indexOf("<START")> 0) {
            sb.append(" <END>");   
        }
        return sb.toString();
    }

    public boolean isCaseSensitive() {
        return caseSensitive;
    }

    public void setCaseSensitive(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }

}
