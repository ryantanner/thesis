package edu.stanford.nlp.tagger.maxent;

import edu.stanford.nlp.io.NumberRangesFileFilter;
import edu.stanford.nlp.io.PrintFile;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.ling.Word;
import edu.stanford.nlp.ling.Sentence;
import edu.stanford.nlp.trees.*;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Arrays;


/** Tags data and can handle either data with gold-standard tags (computing
 *  performance statistics) or unlabeled data.
 *  (The Constructor actually runs the tagger. The main entry points are the
 *  static methods at the bottom of the class.)
 *
 *  Also can train data using the saveModel method.  This class is really the entry
 *  point to all tagger operations, it seems.
 *
 *  @author Kristina Toutanova
 *  @version 1.0
 */
// TODO: can we break this class up in some way?  Perhaps we can
// spread some functionality into TestSentence and some into MaxentTagger
// TODO: at the very least, it doesn't seem to make sense to make it
// an object with state, rather than just some static methods
public class TestClassifier {

  private String filename;
  private TestSentence ts;
  private int numRight;
  private int numWrong;
  private int unknownWords;
  private int numWrongUnknown;
  private int numCorrectSentences;
  private int numSentences;

  // TODO: only one boolean here instead of 3?
  private boolean writeUnknDict;
  private boolean writeWords;
  private boolean writeTopWords;

  private Dictionary wrongWords = new Dictionary();
  // Dictionary unknownWordsDict = new Dictionary();


  TestClassifier(TaggerConfig config, 
                 MaxentTagger maxentTagger) throws IOException {
    /* format can be either of 1 and 2
     *  1 means the test file has the correct tags and is in format one word tag per line
     *  0 means the test file does not have the correct tags and is just tokenized.
     *  In this case the file is in the format one sentence per line (not ending in eos)
     */
    //Default format is 1
    this(config, 1, maxentTagger);
  }

  /** format can be either of 1 or 0
   *  1 means the test file has the correct tags and is in format one word tag per line
   *  0 means the test file does not have the correct tags and is just tokenized.
   *  In this case the file is in the format one sentence per line (not ending in eos)
   *  @throws IOException
   */
  private TestClassifier(TaggerConfig config, int format, 
                         MaxentTagger maxentTagger) throws IOException {
    setDebug(config.getDebug());

    this.filename = config.getFile();
    ts = new TestSentence(maxentTagger);

    String dPrefix = config.getDebugPrefix();
    if (dPrefix == null || dPrefix.equals("")) {
      dPrefix = config.getFile();
    }
    if (config.getInitFromTrees()) {
      test(config, dPrefix, maxentTagger);
    } else {
      //throw new UnsupportedOperationException();
      test(format, dPrefix, config.getTagSeparator(), config.getEncoding(),
           config.getVerboseResults(), maxentTagger);
    }
  }

  /**
   * Begin tagging. The format variable (one of 0,1) determines whether data are assumed
   * to have gold standard tags (1) or to be unlabeled (0).
   * @throws IOException
   */
  private void test(int format, String saveRoot, String tagSeparator, 
                    String encoding, boolean verboseResults, MaxentTagger maxentTagger) throws IOException {
    if ((format == 1)) {
      test(saveRoot, tagSeparator, encoding, verboseResults, maxentTagger); //the data is tagged
    } else {
      BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(filename), encoding));
      PrintFile pf = null;
      PrintFile pf1 = null;
      if (writeWords) pf = new PrintFile(saveRoot + ".words");
      if (writeUnknDict) pf1 = new PrintFile(saveRoot + ".un.dict");

      for (String s; (s = in.readLine()) != null; ) {
        ArrayList<Word> sent = Sentence.toUntaggedList(Arrays.asList(s.split("\\s+")));
        ts.tagSentence(sent);
        if (pf != null) {
          pf.println(ts.getTaggedNice());
        }
      }

      in.close();
      if (pf != null) pf.close();
      if (pf1 != null) pf1.close();
    }
  }

  /**
   * Tag at the end of a sentence.
   */
  static final String EOS_TAG = "EOS";

  /**
   * Word that denotes the end of a sentence.
   */
  static final String EOS_WORD = "EOS";

  /**
   * Adds the EOS marker to both a list of words and a list of tags.
   */
  private void appendSentenceEnd(List<String> words, List<String> tags) {
    //the sentence is read already, add eos
    words.add(EOS_WORD);
    tags.add(EOS_TAG);
  }

  private void testOneSentence(List<String> sentence, List<String> tagsArr,
                               PrintFile wordsFile, PrintFile unknDictFile,
                               PrintFile topWordsFile, boolean verboseResults,
                               MaxentTagger maxentTagger) {
    numSentences++;

    int len = sentence.size();
    String[] testSent = new String[len];
    String[] correctTags = new String[len];
    for (int i = 0; i < len; i++) {
      testSent[i] = sentence.get(i);
      correctTags[i] = tagsArr.get(i);
    }
    
    TestSentence testS = new TestSentence(maxentTagger, 
                                          testSent, correctTags, 
                                          wordsFile, wrongWords, 
                                          verboseResults);
    if (writeUnknDict) testS.printUnknown(numSentences, unknDictFile);
    if (writeTopWords) testS.printTop(topWordsFile);
    
    numWrong = numWrong + testS.numWrong;
    numRight = numRight + testS.numRight;
    unknownWords = unknownWords + testS.numUnknown;
    numWrongUnknown = numWrongUnknown + testS.numWrongUnknown;
    if (testS.numWrong == 0) {
      numCorrectSentences++;
    }
    if (verboseResults) {
      System.out.println("Sentence number: " + numSentences + "; length " + (len-1) + 
                         "; correct: " + testS.numRight + "; wrong: " + testS.numWrong + 
                         "; unknown wrong: " + testS.numWrongUnknown);
      System.out.println("  Total tags correct: " + numRight + "; wrong: " + numWrong + 
                         "; unknown wrong: " + numWrongUnknown);
    }
  }

  /**
   * Test on a file containing correct tags already when init'ing from trees
   * TODO: Add the ability to have a second transformer to transform output back; possibly combine this method
   * with method below
   */
  private void test(TaggerConfig config, String saveRoot,
                    MaxentTagger maxentTagger) throws IOException {
    numSentences = 0;
    PrintFile pf = null;
    PrintFile pf1 = null;
    PrintFile pf3 = null;

    if(writeWords) pf = new PrintFile(saveRoot + ".words");
    if(writeUnknDict) pf1 = new PrintFile(saveRoot + ".un.dict");
    if(writeTopWords) pf3 = new PrintFile(saveRoot + ".words.top");
    TreeReaderFactory trf = new LabeledScoredTreeReaderFactory();
    DiskTreebank treebank = new DiskTreebank(trf,config.getEncoding());
    TreeTransformer transformer = config.getTreeTransformer();
    TreeNormalizer normalizer = config.getTreeNormalizer();

    boolean verboseResults = config.getVerboseResults();

    if (config.getTreeRange() != null) {
      treebank.loadPath(filename, new NumberRangesFileFilter(config.getTreeRange(), true));
    } else {
      treebank.loadPath(filename);
    }
    for (Tree t : treebank) {
      if (normalizer != null) {
        t = normalizer.normalizeWholeTree(t, t.treeFactory());
      }
      if (transformer != null) {
        t = t.transform(transformer);
      }

      List<String> sentence = new ArrayList<String>();
      List<String> tagsArr = new ArrayList<String>();

      for (TaggedWord cur : t.taggedYield()) {
        tagsArr.add(cur.tag());
        sentence.add(cur.word());
      }

      appendSentenceEnd(sentence, tagsArr);
      testOneSentence(sentence, tagsArr, pf, pf1, pf3, verboseResults, maxentTagger);
    }

    if(pf != null) pf.close();
    if(pf1 != null) pf1.close();
    if(pf3 != null) pf3.close();
  }

  /**
   * Test on a file containing correct tags already.
   */
  private void test(String saveRoot, String tagSeparator, 
                    String encoding, boolean verboseResults,
                    MaxentTagger maxentTagger) throws IOException {
    numSentences = 0;
    PrintFile pf = null;
    PrintFile pf1 = null;
    PrintFile pf3 = null;

    BufferedReader rf = new BufferedReader(new InputStreamReader(new FileInputStream(filename), encoding));
    if (writeWords) pf = new PrintFile(saveRoot + ".words");
    if (writeUnknDict) pf1 = new PrintFile(saveRoot + ".un.dict");
    if (writeTopWords) pf3 = new PrintFile(saveRoot + ".words.top");

    for (String s; (s = rf.readLine()) != null; ) {
      List<String> sentence = new ArrayList<String>();
      List<String> tagsArr = new ArrayList<String>();
      StringTokenizer st = new StringTokenizer(s);
      while (st.hasMoreTokens()) { // find the sentence there

        String token = st.nextToken();
        int index = token.lastIndexOf(tagSeparator);

        if (index == -1) {
          throw new RuntimeException("I was unable to find the delimiter '" + tagSeparator + "' in the token '" + token + "'. Consider using -delimiter.");
        }

        String w1 = token.substring(0, index);
        sentence.add(w1);
        String t1 = token.substring(index + 1);
        tagsArr.add(t1);
      }

      appendSentenceEnd(sentence, tagsArr);
      testOneSentence(sentence, tagsArr, pf, pf1, pf3, verboseResults, maxentTagger);
    }

    rf.close();
    if (pf != null) pf.close();
    if (pf1 != null) pf1.close();
    if (pf3 != null) pf3.close();
  }

  /**
   * Warning: This method almost certainly no longer works.
   */
  /*
  @SuppressWarnings({"UnusedDeclaration"})
  private static void iterate(String filename) {
    try {
      MaxentTagger.readModelAndInit(filename);
      MaxentTagger.getLambdaSolve().improvedIterative();
      if (MaxentTagger.getLambdaSolve().checkCorrectness()) {
        System.out.println("model is correct");
      } else {
        System.out.println("model is not correct");
      }
      MaxentTagger.saveModel(filename, null);
    } catch (Exception e) {
      System.err.println("Exception while iterating.");
      e.printStackTrace();
    }
  }
  */


// /**
// * This saves the parameters in a file like for the Improved Iterative.
// * This calculates the model from a filename, with the specified
// * parameters for the history and saves the result back to that filename.
// *
// * Warning: This method almost certainly no longer works.
// */
//  public static void save_param(String filename, String delimiter, String encoding) throws Exception {
//    MaxentTagger.init();
//    TaggerExperiments samples = new TaggerExperiments(filename, null, delimiter, encoding);
//    MaxentTagger.domain = samples;
//    TaggerFeatures feats = TaggerExperiments.feats;
//    System.out.println("Before" + feats.size());
//    //feats.print_by_numbers();
//    Problem p = new Problem(samples, feats);
//    System.out.println(" Entering lambda solve ");
//    LambdaSolveTagger prob = new LambdaSolveTagger(p, 0.0001, 0.00001);
//    MaxentTagger.prob = prob;
//    OutDataStreamFile rf = new OutDataStreamFile(filename);
//    MaxentTagger.save_prev(null, rf);
//    Runtime rt = Runtime.getRuntime();
//    System.out.println(" before " + rt.freeMemory());
//    MaxentTagger.release_mem();
//    rt.gc();
//    System.out.println(" after " + rt.freeMemory());
//    //prob.improvedIterative();
//    //prob.save_problem(filename+".math");
//    MaxentTagger.prob = prob;
//    if (prob.checkCorrectness()) {
//      System.out.println("model is correct");
//    } else {
//      System.out.println("model is not correct");
//    }
//    MaxentTagger.save_after(rf);
//    rf.close();
//
//  }


///**
// * Warning: This method almost certainly no longer works.
// */
//  public static void expandModel(String filename, String oldModelFile, int iters, String delimiter, String encoding) throws Exception {
//    MaxentTagger.init();
//    TaggerExperiments samples = new TaggerExperiments(filename, arg_outputs, delimiter, encoding);
//    MaxentTagger.domain = samples;
//    TaggerFeatures feats = TaggerExperiments.feats;
//    System.out.println("Before" + feats.size());
//    //feats.print_by_numbers();
//    Problem p = new Problem(samples, feats);
//    LambdaSolveTagger prob = new LambdaSolveTagger(p, 0.0001, 0.00001);
//    MaxentTagger.prob = prob;
//    OutDataStreamFile rf = new OutDataStreamFile(filename);
//    MaxentTagger.save_prev(null, rf);
//    Runtime rt = Runtime.getRuntime();
//    System.out.println(" before " + rt.freeMemory());
//    MaxentTagger.release_mem();
//    rt.gc();
//    System.out.println(" after " + rt.freeMemory());
//    prob.readOldLambdas(filename, oldModelFile);
//    MaxentTagger.getLambdaSolve().improvedIterative(iters);
//    if (prob.checkCorrectness()) {
//      System.out.println("model is correct");
//    } else {
//      System.out.println("model is not correct");
//    }
//    MaxentTagger.save_after(rf);
//    rf.close();
//  }

  String resultsString(TaggerConfig config, MaxentTagger maxentTagger) {
    StringBuilder output = new StringBuilder();
    output.append("Model " + config.getModel() + " has xSize=" + maxentTagger.xSize +
                  ", ySize=" + maxentTagger.ySize + ", and numFeatures=" + 
                  maxentTagger.prob.lambda.length + ".\n");
    output.append("Results on " + numSentences + " sentences and " + 
                  (numRight + numWrong) + " words, of which " + 
                  unknownWords + " were unknown.\n");
    output.append(String.format("Total sentences right: %d (%f%%); wrong: %d (%f%%).\n", 
                                numCorrectSentences, numCorrectSentences * 100.0 / numSentences, 
                                numSentences - numCorrectSentences, 
                                (numSentences - numCorrectSentences) * 100.0 / (numSentences)));
    output.append(String.format("Total tags right: %d (%f%%); wrong: %d (%f%%).\n", 
                                numRight, numRight * 100.0 / (numRight + numWrong), numWrong, 
                                numWrong * 100.0 / (numRight + numWrong)));
    if (unknownWords > 0) { 
      output.append(String.format("Unknown words right: %d (%f%%); wrong: %d (%f%%).\n", 
                                  (unknownWords - numWrongUnknown), 
                                  100.0 - (numWrongUnknown * 100.0 / unknownWords), 
                                  numWrongUnknown, numWrongUnknown * 100.0 / unknownWords)); 
    }
    return output.toString();
  }

  void printModelAndAccuracy(TaggerConfig config, MaxentTagger maxentTagger) {
    // print the output all at once so that multiple threads don't clobber each other's output
    System.out.println(resultsString(config, maxentTagger));
  }


  int getNumWords() {
    return numRight + numWrong;
  }

  void setDebug(boolean status) {
    writeUnknDict = status;
    writeWords = status;
    writeTopWords = status;
  }


}
