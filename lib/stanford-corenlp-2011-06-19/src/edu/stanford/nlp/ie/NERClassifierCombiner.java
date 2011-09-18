package edu.stanford.nlp.ie;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import edu.stanford.nlp.ie.regexp.NumberSequenceClassifier;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.AnswerAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.util.StringUtils;

/**
 * Subclass of ClassifierCombiner that behaves like a NER, by copying the AnswerAnnotation labels to NERAnnotation
 * Also, it runs an additional classifier (QuantifiableEntityNormalizer) to recognize numeric entities
 * @author Mihai Surdeanu
 *
 */
public class NERClassifierCombiner extends ClassifierCombiner {
  private final boolean applyNumericClassifiers;
  public static final String APPLY_NUMERIC_CLASSIFIERS_STRING = "true";
  public static final boolean APPLY_NUMERIC_CLASSIFIERS_DEFAULT = true;
  public static final String APPLY_NUMERIC_CLASSIFIERS_PROPERTY = 
    "ner.applyNumericClassifiers";

  private AbstractSequenceClassifier nsc = new NumberSequenceClassifier();

  public NERClassifierCombiner(Properties props) 
    throws FileNotFoundException 
  {
    super(props);
    applyNumericClassifiers = Boolean.parseBoolean(props.getProperty
                                (APPLY_NUMERIC_CLASSIFIERS_PROPERTY, 
                                 APPLY_NUMERIC_CLASSIFIERS_STRING));
  }

  public NERClassifierCombiner(String... loadPaths)
    throws FileNotFoundException 
  {
    this(APPLY_NUMERIC_CLASSIFIERS_DEFAULT, loadPaths);
  }

  public NERClassifierCombiner(boolean applyNumericClassifiers,
                               String... loadPaths) 
    throws FileNotFoundException 
  {
    super(loadPaths);
    this.applyNumericClassifiers = applyNumericClassifiers;
  }

  public NERClassifierCombiner(AbstractSequenceClassifier... classifiers) 
    throws FileNotFoundException 
  {
    this(APPLY_NUMERIC_CLASSIFIERS_DEFAULT, classifiers);
  }

  public NERClassifierCombiner(boolean applyNumericClassifiers,
                               AbstractSequenceClassifier... classifiers) 
    throws FileNotFoundException 
  {
    super(classifiers);
    this.applyNumericClassifiers = applyNumericClassifiers;
  }

  public boolean isApplyNumericClassifiers() {
    return applyNumericClassifiers;
  }

  private static void copyAnswerFieldsToNERField(List<? extends CoreLabel> l) {
    for (CoreLabel m: l) {
      m.set(NamedEntityTagAnnotation.class, m.get(AnswerAnnotation.class));
    }
  }

  @Override
  public List<CoreLabel> classify(List<CoreLabel> tokens) {
    List<CoreLabel> output = super.classify(tokens);
    if (applyNumericClassifiers) {
      
      // recognizes additional MONEY, TIME, DATE, and NUMBER using a set of deterministic rules
      // note: requires TextAnnotation, PartOfSpeechTagAnnotation, and AnswerAnnotation
      // note: this sets AnswerAnnotation!
      recognizeNumberSequences(output);
      
      // AnswerAnnotation -> NERAnnotation
      copyAnswerFieldsToNERField(output);

      try {
        // normalizes numeric entities such as MONEY, TIME, DATE, or PERCENT
        // note: this uses and sets NamedEntityTagAnnotation!
        QuantifiableEntityNormalizer.addNormalizedQuantitiesToEntities(output);
      } catch (Exception e) {
        System.err.println("Ignored an exception in QuantifiableEntityNormalizer: (result is that entities were not normalized)");
        System.out.println("Tokens: " + StringUtils.joinWords(tokens, " "));
        e.printStackTrace(System.err);
      } catch(AssertionError e){
        System.err.println("Ignored an assertion in QuantifiableEntityNormalizer: (result is that entities were not normalized)");
        System.out.println("Tokens: " + StringUtils.joinWords(tokens, " "));
        e.printStackTrace(System.err);
      }
    } else {
      // AnswerAnnotation -> NERAnnotation
      copyAnswerFieldsToNERField(output);
    }
    return output;
  }

  private void recognizeNumberSequences(List<CoreLabel> words) {
    // we need to copy here because NumberSequenceClassifier overwrites the AnswerAnnotation
    List<CoreLabel> newWords = new ArrayList<CoreLabel>();
    for (CoreLabel fl : words) {
      CoreLabel newFL = new CoreLabel();
      newFL.setWord(fl.word());
      newFL.setTag(fl.tag());
      newWords.add(newFL);
    }

    nsc.classify(newWords);

    // copy AnswerAnnotation back. Do not overwrite!
    for (int i = 0, sz = words.size(); i < sz; i++){
      CoreLabel origWord = words.get(i);
      CoreLabel newWord = newWords.get(i);

      // System.err.println(newWord.word() + " => " + newWord.get(AnswerAnnotation.class) + " " + origWord.ner());

      String before = origWord.get(AnswerAnnotation.class);
      String newGuess = newWord.get(AnswerAnnotation.class);
      if ((before == null || before.equals(nsc.flags.backgroundSymbol) || before.equals("MISC")) && !newGuess.equals(nsc.flags.backgroundSymbol)) {
        origWord.set(AnswerAnnotation.class, newGuess);
      }
    }
  }
}

