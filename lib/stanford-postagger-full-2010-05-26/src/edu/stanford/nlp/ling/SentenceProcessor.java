package edu.stanford.nlp.ling;

import java.util.ArrayList;

/**
 * This is a simple interface for applying a transformer to a
 * <code>Sentence</code>. It typically is called iteratively over
 * sentences in a <code>Sentencebank</code>
 *
 * @author Christopher Manning
 * @author Sarah Spikes (sdspikes@cs.stanford.edu) (Templatization)
 */
public interface SentenceProcessor<I extends HasWord, O extends HasWord> {

  /**
   * Does whatever one needs to do to a particular sentence.
   *
   * @param s A sentence.  Classes implementing this interface can assume
   *          that the sentence passed in is not null.
   */
  public ArrayList<O> processSentence(ArrayList<I> s);

}
