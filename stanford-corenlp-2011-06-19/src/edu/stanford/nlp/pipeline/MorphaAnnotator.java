package edu.stanford.nlp.pipeline;

import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.Word;
import edu.stanford.nlp.pipeline.DeprecatedAnnotations.WordsPLAnnotation;
import edu.stanford.nlp.process.Morphology;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.Timing;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * This class will add the lemmas of all the
 * words to the
 * Annotation.  It assumes that the Annotation
 * already contains the tokenized words as a 
 * List<? extends CoreLabel> or a List<List<? extends CoreLabel>> under Annotation.WORDS_KEY
 * and addes stem information to each CoreLabel,
 * in the CoreLabel.LEMMA_KEY field.
 *
 * @author Jenny Finkel
 */
public class MorphaAnnotator implements Annotator{

  private static AtomicLong millisecondsAnnotating = new AtomicLong();
  private boolean VERBOSE = false;


  private static final String[] prep = new String[]{"abroad", "across", "after", "ahead", "along", "aside", "away", "around", "back", "down", "forward", "in", "off", "on", "over", "out", "round", "together", "through", "up"};
  private List<String> particles = Arrays.asList(prep);
  
  public MorphaAnnotator() {
    this(true);
  }

  public MorphaAnnotator(boolean verbose) {
    VERBOSE = verbose;
  }
  
  public void annotate(Annotation annotation) {
    Timing timer = null;
    if (VERBOSE) {
      timer = new Timing();
      timer.start();
      System.err.print("Finding lemma...");
    }
    Morphology morpha = new Morphology();
    if (annotation.has(WordsPLAnnotation.class)) {
      List<List<? extends CoreLabel>> sentences = 
        annotation.get(WordsPLAnnotation.class);
      for (List<? extends CoreLabel> words : sentences) {
        doOneSentence(morpha, words);
      }
    } else if (annotation.has(CoreAnnotations.SentencesAnnotation.class)) {
      for (CoreMap sentence : 
           annotation.get(CoreAnnotations.SentencesAnnotation.class)) {
        List<CoreLabel> tokens = 
          sentence.get(CoreAnnotations.TokensAnnotation.class);
        //System.err.println("Lemmatizing sentence: " + tokens);
        for (int i = 0; i < tokens.size(); i ++){
          CoreLabel token = tokens.get(i);
          String text = token.get(CoreAnnotations.TextAnnotation.class);
          //System.err.println("Token #" + i + ": " + token);
          String posTag = token.get(PartOfSpeechAnnotation.class);
          this.addLemma(morpha, LemmaAnnotation.class, token, text, posTag);
        }
      }
    } else {
      throw new RuntimeException("unable to find words/tokens in: " + 
                                 annotation);
    }
    if (VERBOSE) {
      millisecondsAnnotating.getAndAdd(timer.stop("done."));
    }
  }

  private void doOneSentence(Morphology morpha,
                             List<? extends CoreLabel> words) {
    for (CoreLabel word : words) {
      this.addLemma(morpha, LemmaAnnotation.class, 
                    word, word.word(), word.tag());
    }
  }
  
  private void addLemma(Morphology morpha, 
                        Class<? extends CoreAnnotation<String>> ann, 
                        CoreMap map, String word, String tag) {
    if (tag.length() > 0) {
      String phrasalVerb = phrasalVerb(morpha, word, tag);
      if (phrasalVerb == null) {
        map.set(ann, morpha.stem(word, tag).word());
      } else {
        map.set(ann, phrasalVerb);
      }
    } else {
      map.set(ann, morpha.stem(new Word(word)).word());
    }
  }

  // if word is a phrasal verb, return the phrasal verb lemmatized
  // if not, return null

  private String phrasalVerb(Morphology morpha, String word, String tag) {

    // must be a verb and contain an underscore
    assert(word != null);
    assert(tag != null);
    if(!tag.startsWith("VB")  && !word.contains("_")) return null;

    // check whether the last part is a particle
    String[] verb = word.split("_");
    if(verb.length != 2) return null;
    String particle = verb[1];
    if(particles.contains(particle)) {
      String base = verb[0];
      String lemma = morpha.stem(base, tag).word();
      return lemma + "_" + particle;
    }
    
    return null;

  }
}
