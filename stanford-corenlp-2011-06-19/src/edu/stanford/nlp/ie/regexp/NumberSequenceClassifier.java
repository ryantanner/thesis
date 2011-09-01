package edu.stanford.nlp.ie.regexp;

import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import edu.stanford.nlp.ling.CoreAnnotations.AnswerAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.sequences.DocumentReaderAndWriter;
import edu.stanford.nlp.sequences.PlainTextDocumentReaderAndWriter;
import edu.stanford.nlp.util.PaddedList;
import edu.stanford.nlp.util.StringUtils;

import java.io.ObjectInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

/**
 * A set of deterministic rules for marking certain entities, to add
 * categories and to correct for failures of statistical NER taggers.
 * This is an extremely simple and ungeneralized implementation of
 * AbstractSequenceClassifier that was written for PASCAL RTE.
 * It could profitably be extended and generalized.
 * It marks a NUMBER category based on part-of-speech tags in a
 * deterministic manner.
 * It marks an ORDINAL category based on word form in a deterministic manner.
 * It tags as MONEY currency signs and things tagged CD after a currency sign.
 * It marks a number before a month name as a DATE.
 * It marks as a DATE a word of the form xx/xx/xxxx
 * (where x is a digit from a suitable range).
 * It marks as a TIME a word of the form x(x):xx (where x is a digit).
 * It marks everything else tagged "CD" as a NUMBER, and instances
 * of "and" appearing between CD tags in contexts suggestive of a number.
 * It requires text to be POS-tagged (have the getString(TagAnnotation.class) attribute).
 * Effectively these rules assume that
 * this classifier will be used as a secondary classifier by
 * code such as ClassifierCombiner: it will mark most CD as NUMBER, and it
 * is assumed that something else with higher priority is marking ones that
 * are PERCENT, ADDRESS, etc.
 *
 * @author Christopher Manning
 */
public class NumberSequenceClassifier extends AbstractSequenceClassifier<CoreLabel> {

  private static final boolean DEBUG = false;
  
  public NumberSequenceClassifier() {
    this(new Properties());
    if (! CURRENCY_WORD_PATTERN.matcher("pounds").matches()) {
      System.err.println("NumberSequence: Currency pattern broken");
    }
  }

  public NumberSequenceClassifier(Properties props) {
    super(props);
  }

  public static final Pattern MONTH_PATTERN = Pattern.compile("January|Jan\\.?|February|Feb\\.?|March|Mar\\.?|April|Apr\\.?|May|June|Jun\\.?|July|Jul\\.?|August|Aug\\.?|September|Sept?\\.?|October|Oct\\.?|November|Nov\\.?|December|Dec\\.");

  public static final Pattern YEAR_PATTERN = Pattern.compile("[1-3][0-9]{3}|'?[0-9]{2}");

  public static final Pattern DAY_PATTERN = Pattern.compile("(?:[1-9]|[12][0-9]|3[01])(?:st|nd|rd)?");

  public static final Pattern DATE_PATTERN = Pattern.compile("[0-3][0-9]/[0-3][0-9]/[1-9][0-9]{3}");

  public static final Pattern DATE_PATTERN2 = Pattern.compile("[12][0-9]{3}[-/][01][0-9][-/][0-3][0-9]");

  public static final Pattern TIME_PATTERN = Pattern.compile("[0-2]?[0-9]:[0-5][0-9]");

  public static final Pattern TIME_PATTERN2 = Pattern.compile("[0-2][0-9]:[0-5][0-9]:[0-5][0-9]");

  public static final Pattern CURRENCY_WORD_PATTERN = Pattern.compile("(?:dollar|cent|euro|pound)s?|penny|pence|yen|yuan");

  public static final Pattern ORDINAL_PATTERN = Pattern.compile("(?i)[2-9]?1st|[2-9]?2nd|[2-9]?3rd|1[0-9]th|[2-9]?[04-9]th|100+th|zeroth|first|second|third|fourth|fifth|sixth|seventh|eighth|ninth|tenth|eleventh|twelfth|thirteenth|fourteenth|fifteenth|sixteenth|seventeenth|eighteenth|nineteenth|twentieth|twenty-first|twenty-second|twenty-third|twenty-fourth|twenty-fifth|twenty-sixth|twenty-seventh|twenty-eighth|twenty-ninth|thirtieth|thirty-first|fortieth|fiftieth|sixtieth|seventieth|eightieth|ninetieth|hundredth|thousandth|millionth");
  
  public static final Pattern ARMY_TIME_MORNING = Pattern.compile("0([0-9])([0-9]){2}");
  
  public static final Pattern GENERIC_TIME_WORDS = Pattern.compile("(morning|evening|night|noon|midnight|teatime|lunchtime|dinnertime|suppertime|afternoon|midday|dusk|dawn|sunup|sundown|daybreak|day)");

  /**
   * Classify a {@link List} of {@link CoreLabel}s.
   *
   * @param document A {@link List} of {@link CoreLabel}s.
   * @return the same {@link List}, but with the elements annotated
   *         with their answers.
   */
  @Override
  public List<CoreLabel> classify(List<CoreLabel> document) {
    // if (DEBUG) { System.err.println("NumberSequenceClassifier tagging"); }
    PaddedList<CoreLabel> pl = new PaddedList<CoreLabel>(document, pad);
    for (int i = 0, sz = pl.size(); i < sz; i++) {
      CoreLabel me = pl.get(i);
      CoreLabel prev = pl.get(i - 1);
      CoreLabel next = pl.get(i + 1);
      CoreLabel next2 = pl.get(i + 2);
      //if (DEBUG) { System.err.println("Tagging:" + me.word()); }
      me.set(AnswerAnnotation.class, flags.backgroundSymbol);
      if ((me.word().equals("$") || me.word().equals("&#163") || me.word().equals("\u00A3") || me.word().equals("\u00A5") || me.word().equals("#") || me.word().equals("\u20AC") || me.word().equals("US$") || me.word().equals("HK$") || me.word().equals("A$")) &&
              (prev.getString(PartOfSpeechAnnotation.class).equals("CD") || next.getString(PartOfSpeechAnnotation.class).equals("CD"))) {
        // dollar, pound, pound, yen,
        // Penn Treebank ancient # as pound, euro,
        if (DEBUG) {
          System.err.println("Found currency sign:" + me.word());
        }
        me.set(AnswerAnnotation.class, "MONEY");
      } else if (me.getString(PartOfSpeechAnnotation.class).equals("CD")) {
        if (DEBUG) {
          System.err.println("Tagging CD:" + me.word());
        }

        if (TIME_PATTERN.matcher(me.word()).matches()) {
          me.set(AnswerAnnotation.class, "TIME");
        } else if (TIME_PATTERN2.matcher(me.word()).matches()) {
            me.set(AnswerAnnotation.class, "TIME");
        } else if (DATE_PATTERN.matcher(me.word()).matches()) {
          me.set(AnswerAnnotation.class, "DATE");
        } else if (DATE_PATTERN2.matcher(me.word()).matches()) {
          me.set(AnswerAnnotation.class, "DATE");

        } else if (next.get(TextAnnotation.class) != null && me.get(TextAnnotation.class) != null && DAY_PATTERN.matcher(me.get(TextAnnotation.class)).matches() && MONTH_PATTERN.matcher(next.get(TextAnnotation.class)).matches()) {
          // deterministically make DATE for British-style number before month
          me.set(AnswerAnnotation.class, "DATE");
        } else if (prev.get(TextAnnotation.class) != null && MONTH_PATTERN.matcher(prev.get(TextAnnotation.class)).matches() &&
            me.get(TextAnnotation.class) != null && DAY_PATTERN.matcher(me.get(TextAnnotation.class)).matches()) {
          // deterministically make DATE for number after month
          me.set(AnswerAnnotation.class, "DATE");
        } else if (rightScanFindsMoneyWord(pl, i) && ! leftScanFindsWeightWord(pl, i)) {
          me.set(AnswerAnnotation.class, "MONEY");
        } else if(ARMY_TIME_MORNING.matcher(me.word()).matches()) {
          me.set(AnswerAnnotation.class, "TIME");
        } else
        if (YEAR_PATTERN.matcher(me.word()).matches() && prev.getString(AnswerAnnotation.class).equals("DATE") && (MONTH_PATTERN.matcher(prev.word()).matches() || pl.get(i - 2).get(AnswerAnnotation.class).equals("DATE")))
        {
          me.set(AnswerAnnotation.class, "DATE");
        } else {
          if (DEBUG) {
            System.err.println("Found number:" + me.word());
          }
          if (prev.getString(AnswerAnnotation.class).equals("MONEY")) {
            me.set(AnswerAnnotation.class, "MONEY");
          } else {
            me.set(AnswerAnnotation.class, "NUMBER");
          }
        }
      } else if (me.getString(PartOfSpeechAnnotation.class) != null && me.getString(PartOfSpeechAnnotation.class).equals(",") && prev.getString(AnswerAnnotation.class).equals("DATE") && next.word() != null && YEAR_PATTERN.matcher(next.word()).matches()) {
        me.set(AnswerAnnotation.class, "DATE");
      } else if (me.getString(PartOfSpeechAnnotation.class).equals("NNP") && MONTH_PATTERN.matcher(me.word()).matches()) {
        if (prev.getString(AnswerAnnotation.class).equals("DATE") || next.getString(PartOfSpeechAnnotation.class).equals("CD")) {
          me.set(AnswerAnnotation.class, "DATE");
        }
      } else if (me.getString(PartOfSpeechAnnotation.class) != null && me.getString(PartOfSpeechAnnotation.class).equals("CC")) {
        if (prev.get(PartOfSpeechAnnotation.class) != null && prev.get(PartOfSpeechAnnotation.class).equals("CD") &&
            next.get(PartOfSpeechAnnotation.class) != null && next.get(PartOfSpeechAnnotation.class).equals("CD") &&
            me.get(TextAnnotation.class) != null && me.get(TextAnnotation.class).equalsIgnoreCase("and")) {
          if (DEBUG) {
            System.err.println("Found number and:" + me.word());
          }
          String wd = prev.word();
          if (wd.equalsIgnoreCase("hundred") || wd.equalsIgnoreCase("thousand") || wd.equalsIgnoreCase("million") || wd.equalsIgnoreCase("billion") || wd.equalsIgnoreCase("trillion"))
          {
            me.set(AnswerAnnotation.class, "NUMBER");
          }
        }
      } else if (me.getString(PartOfSpeechAnnotation.class) != null && (me.getString(PartOfSpeechAnnotation.class).equals("NN") || me.getString(PartOfSpeechAnnotation.class).equals("NNS"))) {
        if (CURRENCY_WORD_PATTERN.matcher(me.word()).matches()) {
          if (prev.getString(PartOfSpeechAnnotation.class).equals("CD") && prev.getString(AnswerAnnotation.class).equals("MONEY")) {
            me.set(AnswerAnnotation.class, "MONEY");
          }
        } else if (me.word().equals("m") || me.word().equals("b")) {
          // could be metres, but it's probably million or billion in our
          // applications
          if (prev.getString(AnswerAnnotation.class).equals("MONEY")) {
            me.set(AnswerAnnotation.class, "MONEY");
          } else {
            me.set(AnswerAnnotation.class, "NUMBER");
          }
        } else if (ORDINAL_PATTERN.matcher(me.word()).matches()) {
          if ((next.word() != null && MONTH_PATTERN.matcher(next.word()).matches()) ||
              (next.word() != null && next.word().equalsIgnoreCase("of") && 
               next2.word() != null && MONTH_PATTERN.matcher(next2.word()).matches())) {
            me.set(AnswerAnnotation.class, "DATE");
          }
        } else if(GENERIC_TIME_WORDS.matcher(me.word()).matches()){
          me.set(AnswerAnnotation.class, "TIME");          
        }
      } else if (me.getString(PartOfSpeechAnnotation.class).equals("JJ")) {
        if ((next.word() != null && MONTH_PATTERN.matcher(next.word()).matches()) ||
            next.word() != null && next.word().equalsIgnoreCase("of") && next2.word() != null && MONTH_PATTERN.matcher(next2.word()).matches()) {
          me.set(AnswerAnnotation.class, "DATE");
        } else if (ORDINAL_PATTERN.matcher(me.word()).matches()) {
          // don't do other tags: don't want 'second' as noun, or 'first' as adverb
          // introducing reasons
          me.set(AnswerAnnotation.class, "ORDINAL");
        }
      } else if (me.getString(PartOfSpeechAnnotation.class).equals("IN") && me.word().equalsIgnoreCase("of")) {
        if (prev.get(TextAnnotation.class) != null && ORDINAL_PATTERN.matcher(prev.get(TextAnnotation.class)).matches() &&
            next.get(TextAnnotation.class) != null && MONTH_PATTERN.matcher(next.get(TextAnnotation.class)).matches()) {
          me.set(AnswerAnnotation.class, "DATE");
        }
      } 
    }
    return document;
  }


  /**
   * Look for a distance of up to 3 for something that indicates weight not
   * money.
   *
   * @param pl The list of CoreLabel
   * @param i The position to scan right from
   * @return whether a weight word is found
   */
  private static boolean leftScanFindsWeightWord(List<CoreLabel> pl, int i) {
    if (DEBUG) {
      System.err.println("leftScan from: " + pl.get(i).word());
    }
    for (int j = i - 1; j >= 0 && j >= i - 3; j--) {
      CoreLabel fl = pl.get(j);
      if (fl.word().startsWith("weigh")) {
        if (DEBUG) {
          System.err.println("leftScan found weight: " + fl.word());
        }
        return true;
      }
    }
    return false;
  }


  /**
   * Look along CD words and see if next thing is a money word
   * like cents or pounds.
   *
   * @param pl The list of CoreLabel
   * @param i The position to scan right from
   * @return Whether a money word is found
   */
  private static boolean rightScanFindsMoneyWord(List<CoreLabel> pl, int i) {
    int j = i;
    if (DEBUG) {
      System.err.println("rightScan from: " + pl.get(j).word());
    }
    int sz = pl.size();
    while (j < sz && pl.get(j).getString(PartOfSpeechAnnotation.class).equals("CD")) {
      j++;
    }
    if (j >= sz) {
      return false;
    }
    String tag = pl.get(j).getString(PartOfSpeechAnnotation.class);
    String word = pl.get(j).word();
    if (DEBUG) {
      System.err.println("rightScan testing: " + word + '/' + tag + "; answer is: " + Boolean.toString((tag.equals("NN") || tag.equals("NNS")) && CURRENCY_WORD_PATTERN.matcher(word).matches()));
    }
    return (tag.equals("NN") || tag.equals("NNS")) && CURRENCY_WORD_PATTERN.matcher(word).matches();
  }

  // Implement other methods of AbstractSequenceClassifier interface

  @Override
  public void train(Collection<List<CoreLabel>> docs) {
  }

  @Override
  public void printProbsDocument(List<CoreLabel> document) {
  }

  @Override
  public void serializeClassifier(String serializePath) {
    System.err.print("Serializing classifier to " + serializePath + "...");
    System.err.println("done.");
  }

  @Override
  public void loadClassifier(ObjectInputStream in, Properties props) throws IOException, ClassCastException, ClassNotFoundException {
  }

  public static void main(String[] args) throws Exception {
    Properties props = StringUtils.argsToProperties(args);
    NumberSequenceClassifier cmm = new NumberSequenceClassifier(props);
    String trainFile = cmm.flags.trainFile;
    String testFile = cmm.flags.testFile;
    String textFile = cmm.flags.textFile;
    String loadPath = cmm.flags.loadClassifier;
    String serializeTo = cmm.flags.serializeTo;

    if (loadPath != null) {
      cmm.loadClassifierNoExceptions(loadPath);
      cmm.flags.setProperties(props);
    } else if (trainFile != null) {
      cmm.train(trainFile);
    }

    if (serializeTo != null) {
      cmm.serializeClassifier(serializeTo);
    }

    if (testFile != null) {
      cmm.classifyAndWriteAnswers(testFile);
    }

    if (textFile != null) {
      DocumentReaderAndWriter oldRW = cmm.readerAndWriter;
      cmm.readerAndWriter = new PlainTextDocumentReaderAndWriter();
      cmm.classifyAndWriteAnswers(textFile);
      cmm.readerAndWriter = oldRW;
    }
  } // end main

}
