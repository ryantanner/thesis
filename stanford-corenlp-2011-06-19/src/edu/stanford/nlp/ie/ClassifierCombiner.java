package edu.stanford.nlp.ie;

import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.ie.ner.CMMClassifier;
import edu.stanford.nlp.ling.CoreAnnotations.AnswerAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.util.StringUtils;

import java.io.FileNotFoundException;
import java.io.ObjectInputStream;
import java.io.IOException;
import java.util.*;

/**
 * Merges the outputs of two or more AbstractSequenceClassifiers according to 
 * a simple precedence scheme: any given base classifier contributes only 
 * classifications of labels that do not exist in the base classifiers specified
 * before, and that do not have any token overlap with labels assigned by 
 * higher priority classifiers.
 * <p>
 * This is a pure AbstractSequenceClassifier, i.e., it set the AnswerAnnotation label.
 * If you work with NER classifiers, you should use NERClassifierCombiner. This class
 * inherits from ClassifierCombiner, and takes care that all AnswerAnnotations are also
 * copied in NERAnnotation.
 * <p>
 * You can specify up to 10 base classifiers using the -loadClassifier1 to -loadClassifier10
 * properties. We also maintain the older usage when only two base classifiers were accepted,
 * specified using -loadClassifier and -loadAuxClassifier.
 * <p>
 * ms 2009: removed all NER functionality (see NERClassifierCombiner), changed code so it accepts an arbitrary number of base classifiers, removed dead code.
 * todo cdm 2009: the semantics could do with some cleaning up. Some methods still call Quantifiable entity normalization, and they probably shouldn't.
 *
 * @author Chris Cox
 * @author Mihai Surdeanu
 */
public class ClassifierCombiner extends AbstractSequenceClassifier<CoreLabel> {

  private static final boolean DEBUG = false;
  private List<AbstractSequenceClassifier> baseClassifiers;

  private static final String DEFAULT_AUX_CLASSIFIER_PATH="/u/nlp/data/ner/goodClassifiers/muc.7class.distsim.crf.ser.gz";
  private static final String DEFAULT_CLASSIFIER_PATH="/u/nlp/data/ner/goodClassifiers/all.3class.distsim.crf.ser.gz";

  /**
   * @param p Properties File that specifies <code>loadClassifier</code>
   * and <code>loadAuxClassifier</code> properties or, alternatively, <code>loadClassifier[1-10]</code> properties.
   * @throws FileNotFoundException If classifierfiles not found
   */
  public ClassifierCombiner(Properties p) throws FileNotFoundException {
    super(p);
    String loadPath1, loadPath2;
    List<String> paths = new ArrayList<String>();
    
    //
    // prefered configuration: specify up to 10 base classifiers using loadClassifier1 to loadClassifier10 properties
    //
    if((loadPath1 = p.getProperty("loadClassifier1")) != null && (loadPath2 = p.getProperty("loadClassifier2")) != null){
      paths.add(loadPath1);
      paths.add(loadPath2);
      for(int i = 3; i <= 10; i ++){
        String path;
        if((path = p.getProperty("loadClassifier" + i)) != null){
          paths.add(path);
        }
      }
      loadClassifiers(paths);
    }

    //
    // second accepted setup (backward compatible): two classifier given in loadClassifier and loadAuxClassifier
    //
    else if((loadPath1 = p.getProperty("loadClassifier")) != null && (loadPath2 = p.getProperty("loadAuxClassifier")) != null){
      paths.add(loadPath1);
      paths.add(loadPath2);
      loadClassifiers(paths);
    }

    //
    // fall back strategy: use the two default paths on NLP machines 
    //
    else {
      paths.add(DEFAULT_CLASSIFIER_PATH);
      paths.add(DEFAULT_AUX_CLASSIFIER_PATH);
      loadClassifiers(paths);
    }
  }

  /** Loads a series of base classifiers from the paths specified.
   *
   * @param loadPaths Paths to the base classifiers
   * @throws FileNotFoundException If classifier files not found
   */
  public ClassifierCombiner(String... loadPaths) throws FileNotFoundException {
    super(new Properties());
    List<String> paths = new ArrayList<String>();
    for(String path: loadPaths) paths.add(path);
    loadClassifiers(paths);
  }


  /** Combines a series of base classifiers
   *
   * @param classifiers The base classifiers
   */
  public ClassifierCombiner(AbstractSequenceClassifier... classifiers) {
    super(new Properties());
    baseClassifiers = new ArrayList<AbstractSequenceClassifier>();
    for(AbstractSequenceClassifier cls: classifiers) baseClassifiers.add(cls);
    flags.backgroundSymbol = baseClassifiers.get(0).flags.backgroundSymbol;
  }


  private void loadClassifiers(List<String> paths) throws FileNotFoundException {
    baseClassifiers = new ArrayList<AbstractSequenceClassifier>();
    for(String path: paths){
      AbstractSequenceClassifier cls = loadClassifierFromPath(path);
      baseClassifiers.add(cls);
      if(DEBUG){
        System.err.printf("Successfully loaded classifier #%d from %s.\n", baseClassifiers.size(), path);
      }
    }
    flags.backgroundSymbol = baseClassifiers.get(0).flags.backgroundSymbol;
  }


  public static AbstractSequenceClassifier loadClassifierFromPath(String path)
      throws FileNotFoundException {
    //try loading as a CRFClassifier
    try {
       return CRFClassifier.getClassifier(path);
    } catch (Exception e) {
      e.printStackTrace();
    }
    //try loading as a CMMClassifier
    try {
      return CMMClassifier.getClassifier(path);
    } catch (Exception e) {
      //fail
      //System.err.println("Couldn't load classifier from path :"+path);
      FileNotFoundException fnfe = new FileNotFoundException();
      fnfe.initCause(e);
      throw fnfe;
    }
  }

  @Override
  public Set<String> labels() {
    Set<String> labs = new HashSet<String>();
    for(AbstractSequenceClassifier cls: baseClassifiers)
      labs.addAll(cls.labels());
    return labs;
  }


  /** 
   * Reads the Answer annotations in the given labelings (produced by the base models)
   *   and combines them using a priority ordering, i.e., for a given baseDocument all 
   *   labelings seen before in the baseDocuments list have higher priority.
   *   Writes the answer to AnswerAnnotation in the labeling at position 0 
   *   (considered to be the main document).
   *
   *  @param baseDocuments Results of all base AbstractSequenceClassifier models
   *  @return A List CoreLabel with the combined annotations.  (This is an
   *     updating of baseDocuments.get(0), not a new List.)
   */
  private List<CoreLabel> mergeDocuments(List<List<CoreLabel>> baseDocuments){
    // we should only get here if there is something to merge
    assert(! baseClassifiers.isEmpty() && ! baseDocuments.isEmpty());
    // all base outputs MUST have the same length (we generated them internally!)
    for(int i = 1; i < baseDocuments.size(); i ++) 
      assert(baseDocuments.get(0).size() == baseDocuments.get(i).size());
    
    // baseLabels.get(i) points to the labels assigned by baseClassifiers.get(i)
    List<Set<String>> baseLabels = new ArrayList<Set<String>>();
    Set<String> seenLabels = new HashSet<String>();
    for(int i = 0; i < baseClassifiers.size(); i ++){
      Set<String> labs = baseClassifiers.get(i).labels();
      labs.removeAll(seenLabels);
      seenLabels.addAll(labs);
      baseLabels.add(labs);
    }
    String background = baseClassifiers.get(0).flags.backgroundSymbol;

    if (DEBUG) {
      for(int i = 0; i < baseLabels.size(); i ++)
        System.err.println("mergeDocuments: Using classifier #" + i + " for " + baseLabels.get(i));
      System.err.println("mergeDocuments: Background symbol is " + background);
      
      System.err.println("Base model outputs:");
      for(int i = 0; i < baseDocuments.size(); i ++){
        System.err.printf("Output of model #%d:", i);
        for(CoreLabel l: baseDocuments.get(i)) System.err.print(" " + l.get(AnswerAnnotation.class));
        System.err.println();
      }
    }
    
    // incrementally merge each additional model with the main model (i.e., baseDocuments.get(0))
    // this keeps adding labels from the additional models to mainDocument
    // hence, when all is done, mainDocument contains the labels of all base models
    List<CoreLabel> mainDocument = baseDocuments.get(0);
    for(int i = 1; i < baseDocuments.size(); i ++){
      mergeTwoDocuments(mainDocument, baseDocuments.get(i), baseLabels.get(i), background);
    }
    
    if(DEBUG){
      System.err.print("Output of combined model:");
      for(CoreLabel l: mainDocument) System.err.print(" " + l.get(AnswerAnnotation.class));
      System.err.println("\n");
    }
    
    return mainDocument;
  }
  
  private void mergeTwoDocuments(List<CoreLabel> mainDocument, List<CoreLabel> auxDocument, Set<String> auxLabels, String background) {    
    boolean insideAuxTag = false;
    boolean auxTagValid = true;
    String prevAnswer = background;
    Collection <CoreLabel> constituents = new ArrayList<CoreLabel>();

    Iterator<CoreLabel> auxIterator = auxDocument.listIterator();

    for (CoreLabel wMain : mainDocument) {
      CoreLabel wAux = auxIterator.next();
      String auxAnswer = wAux.get(AnswerAnnotation.class);
      String mainAnswer = wMain.get(AnswerAnnotation.class);
      boolean insideMainTag = !mainAnswer.equals(background);

      /*if the auxiliary classifier gave it one of the labels unique to
        auxClassifier, we set the mainLabel to that.*/
      if (auxLabels.contains(auxAnswer)) {
        if (!prevAnswer.equals(auxAnswer) && !prevAnswer.equals(background)) {
          if (auxTagValid){
            for (CoreLabel wi : constituents) {
              wi.set(AnswerAnnotation.class, prevAnswer);
            }
          }
          constituents = new ArrayList<CoreLabel>();
        }
        insideAuxTag = true;
        if (insideMainTag) { auxTagValid = false; }
        prevAnswer=auxAnswer;
        constituents.add(wMain);
      } else {
        if (insideAuxTag) {
          if (auxTagValid){
            for (CoreLabel wi : constituents) {
              wi.set(AnswerAnnotation.class, prevAnswer);
            }
          }
          constituents = new ArrayList<CoreLabel>();
        }
        insideAuxTag=false;
        auxTagValid = true;
        prevAnswer = background;
      }
    }
  }
  
  @SuppressWarnings("unused")
  private List<CoreLabel> deepCopy(List<CoreLabel> tokens) {
    List<CoreLabel> copy = new ArrayList<CoreLabel>();
    for (CoreLabel ml : tokens) {
      CoreLabel ml1 = new CoreLabel(ml);  // copy the labels
      copy.add(ml1);
    }
    return copy;
  }

  /**
   * Generates the AnswerAnnotation labels of the combined model for the given tokens
   */
  @Override
  public List<CoreLabel> classify(List<CoreLabel> tokens) {
    if(baseClassifiers.isEmpty()) return tokens;
    List<List<CoreLabel>> baseOutputs = new ArrayList<List<CoreLabel>>();
    
    // the first base model works in place, modifying the original tokens
    List<CoreLabel> output = baseClassifiers.get(0).classifySentence(tokens);
    // classify(List<CoreLabel>) is supposed to work in place, so add AnswerAnnotation to tokens!
    for(int i = 0; i < output.size(); i ++){
      tokens.get(i).set(AnswerAnnotation.class, output.get(i).get(AnswerAnnotation.class));
    }
    baseOutputs.add(tokens);
    
    for(int i = 1; i < baseClassifiers.size(); i ++){
      //List<CoreLabel> copy = deepCopy(tokens);
      // no need for deep copy: classifySentence creates a copy of the input anyway
      List<CoreLabel> copy = tokens;
      output = baseClassifiers.get(i).classifySentence(copy);
      baseOutputs.add(output);
    }
    assert(baseOutputs.size() == baseClassifiers.size());
    List<CoreLabel> finalAnswer = mergeDocuments(baseOutputs);
    
    return finalAnswer;
  }

  // XXX: move this functionality to NERClassifierCombiner
  // ms: not sure this is still needed... probably not.
  /*
  public List<List<CoreLabel>> classify(String s) {
    List<List<CoreLabel>>test1= mainClassifier.classify(s);
    List<List<CoreLabel>>test2= auxClassifier.classify(s);

    List<List<CoreLabel>>nerOutput = mergeClassifierOutputs(test1,test2);
    nerOutput=QuantifiableEntityNormalizer.normalizeClassifierOutput(nerOutput);
    return nerOutput;
  }
  */

  @Override
  public void train(Collection<List<CoreLabel>> docs) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void printProbsDocument(List<CoreLabel> document) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void serializeClassifier(String serializePath) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void loadClassifier(ObjectInputStream in, Properties props) throws IOException, ClassCastException, ClassNotFoundException {
    throw new UnsupportedOperationException();
  }


  /** 
   * Some basic testing of the ClassifierCombiner
   * @throws Exception If IO or serialization error loading classifiers
   */
  public static void main(String[] args) throws Exception {
    Properties props = StringUtils.argsToProperties(args);
    ClassifierCombiner ec = new ClassifierCombiner(props);

    System.err.println(ec.classifyToString("Marketing : Sony Hopes to Win Much Bigger Market For Wide Range of Small-Video Products ---- By Andrew B. Cohen Staff Reporter of The Wall Street Journal"));
  }
}
