// Stanford Classifier, ColumnDataClassifier - a multiclass maxent classifier
// Copyright (c) 2003-2010 The Board of Trustees of
// The Leland Stanford Junior University. All Rights Reserved.
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//
// This code is a parameter language for front-end feature
// generation for the loglinear model classification code in
// the Stanford Classifier package (mainly written by Dan Klein).
//
// For more information, bug reports, fixes, contact:
//    Christopher Manning
//    Dept of Computer Science, Gates 1A
//    Stanford CA 94305-9010
//    USA
//    Support/Questions: java-nlp-user@lists.stanford.edu
//    Licensing: java-nlp-support@lists.stanford.edu
//    http://www-nlp.stanford.edu/software/classifier.shtml

package edu.stanford.nlp.classify;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.File;
import java.text.NumberFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import edu.stanford.nlp.io.FileUtils;
import edu.stanford.nlp.ling.BasicDatum;
import edu.stanford.nlp.ling.Datum;
import edu.stanford.nlp.ling.RVFDatum;
import edu.stanford.nlp.optimization.DiffFunction;
import edu.stanford.nlp.optimization.Minimizer;
import edu.stanford.nlp.process.WordShapeClassifier;
import edu.stanford.nlp.stats.ClassicCounter;
import edu.stanford.nlp.stats.Counter;
import edu.stanford.nlp.stats.Counters;
import edu.stanford.nlp.stats.Distribution;
import edu.stanford.nlp.stats.TwoDimensionalCounter;
import edu.stanford.nlp.objectbank.ObjectBank;
import edu.stanford.nlp.util.ErasureUtils;
import edu.stanford.nlp.util.Pair;
import edu.stanford.nlp.util.ReflectionLoading;
import edu.stanford.nlp.util.StringUtils;
import edu.stanford.nlp.util.Triple;


/**
 * ColumnDataClassifier provides a command-line interface for doing
 * context-free (independent) classification of a series of data items,
 * where each data item is represented by a line of
 * a file, as a list of String variables, in tab-separated columns.  Some
 * features will interpret these variables as numbers, but
 * the code is mainly oriented towards generating features for string
 * classification.  To designate a real-valued feature, use the realValued
 * option described below. The classifier can be either a Bernoulli Naive
 * Bayes model or a loglinear discriminative (i.e., maxent) model.
 * <p/>
 * Input files are expected to
 * be one data item per line with two or more columns indicating the class
 * of the item and one or more predictive features.  Columns are
 * separated by tab characters.  Tab and newline characters cannot occur
 * inside field values (there is no escaping mechanism); any other characters
 * are legal in field values.
 * <p/>
 * Typical usage:
 * <p><code>
 * java ColumnDataClassifier -prop propFile
 * </code><p>or<p>
 * <code>java -mx300m ColumnDataClassifier
 * -trainFile trainFile -testFile testFile -useNGrams|... &gt; output
 * </code>
 * <p/>
 * (Note that for large data sets, you may wish to specify
 * the amount of memory available to Java, such
 * as in the second example above.)
 * <p/>
 * In the simplest case, there are just two tab-separated columns in the
 * training input: the first for the class, and the second for the String
 * datum which has that class.   In more complex uses, each datum can
 * be multidimensional, and there are many columns of data attributes.
 * <p/>
 * To illustrate simple uses, and the behavior of Naive Bayes and Maximum
 * entropy classifiers, example files corresponding to the examples from the
 * Manning and Klein maxent classifier tutorial, slides 46-49, available at
 * http://nlp.stanford.edu/downloads/classifier.shtml are included in the
 * classify package source directory (files starting with "easy").  Other
 * examples appear in the <code>examples</code> directory of the distributed
 * classifier.
 * <p/>
 * In many instances, parameters can either be given on the command line
 * or provided using a Properties file
 * (specified on the command-line with <code>-prop</code> <i>propFile</i>).
 * Option names are the same as property names with a preceding dash.  Boolean
 * properties can simply be turned on via the command line.  Parameters of
 * types int, String, and double take a following argument after the option.
 * Command-line parameters can only define features for the first column
 * describing the datum.  If you have multidimensional data, you need to use
 * a properties file.  Property names, as below, are either global (things
 * like the testFile name) or are seen as properties that define features
 * for the first data column (we count columns from 0 - unlike the Unix cut
 * command!).  To specify features for a particular data column, precede a
 * feature by a column number and then a period (for example,
 * <code>3.wordShape=chris4</code>).  Note that in properties files you must
 * give a value to boolean properties (e.g., <code>2.useString=true</code>);
 * just giving the property name (as <code>2.useString</code>) isn't
 * sufficient.
 * <p/>
 * The following properties are recognized:
 * </p>
 * <table border="1">
 * <tr><td><b>Property Name</b></td><td><b>Type</b></td><td><b>Default Value</b></td><td><b>Description</b></td><td><b>FeatName</b></td></tr>
 * <tr><td> loadClassifier </td><td>String</td><td>n/a</td><td>Path of serialized classifier file to load</td></tr>
 * <tr><td> serializeTo</td><td>String</td><td>n/a</td><td>Path to serialize classifier to</td></tr>
 * <tr><td> printTo</td><td>String</td><td>n/a</td><td>Path to print a text representation of the linear classifier to</td></tr>
 * <tr><td> trainFile</td><td>String</td><td>n/a</td><td>Path of file to use as training data</td></tr>
 * <tr><td> testFile</td><td>String</td><td>n/a</td><td>Path of file to use as test data</td></tr>
 * <tr><td>displayedColumn</td><td>int</td><td>1</td><td>Column number that will be printed out to stdout in the output next to the gold class and the chosen class.  This is just an aide memoire.  If the value is negative, nothing is printed. </td></tr>
 * <tr><td>goldAnswerColumn</td><td>int</td><td>0</td><td>Column number that contains the correct class for each data item (again, columns are numbered from 0 up).</td></tr>
 * <tr><td>groupingColumn</td><td>int</td><td>-1</td><td>Column for grouping multiple data items for the purpose of computing ranking accuracy.  This is appropriate when only one datum in a group can be correct, and the intention is to choose the highest probability one, rather than accepting all above a threshold.  Multiple items in the same group must be contiguous in the test file (otherwise it would be necessary to cache probabilities and groups for the entire test file to check matches).  If it is negative, no grouping column is used, and no ranking accuracy is reported.</td></tr>
 * <tr><td>rankingScoreColumn</td><td>int</td><td>-1</td><td>If this parameter is non-negative and a groupingColumn is defined, then an average ranking score will be calculated by scoring the chosen candidate from a group according to its value in this column (for instance, the values of this column can be set to a mean reciprocal rank of 1.0 for the best answer, 0.5 for the second best and so on, or the value of this column can be a similarity score reflecting the similarity of the answer to the true answer.</td></tr>
 * <tr><td>rankingAccuracyClass</td><td>String</td><td>null</td><td>If this and groupingColumn are defined (positive), then the system will compute a ranking accuracy under the assumption that there is (at most) one assignment of this class for each group, and ranking accuracy counts the classifier as right if that datum is the one with highest probability according to the model.</td></tr>
 * <p/>
 * <tr><td>useString</td><td>boolean</td><td>false</td><td>Gives you a feature for whole string s</td><td>S-<i>str</i></td></tr>
 * <tr><td>useClassFeature</td><td>boolean</td><td>false</td><td>Include a feature for the class (as a class marginal)</td><td>CLASS</td></tr>
 * <tr><td>binnedLengths</td><td>String</td><td>null</td><td>If non-null, treat as a sequence of comma separated integer bounds, where items above the previous bound (if any) up to the next bound (inclusive) are binned (e.g., "1,5,15,30,60"). The feature represents the length of the String in this column.</td><td>Len-<i>range</i></td></tr>
 * <tr><td>binnedLengthsStatistics</td><td>boolean</td><td>false</td><td>If true, print to stderr contingency table of statistics for binnedLengths.</td><td></td></tr>
 * <tr><td>binnedValues</td><td>String</td><td>null</td><td>If non-null, treat as a sequence of comma separated double bounds, where data items above the previous bound up to the next bound (inclusive) are binned. If a value in this column isn't a legal <code>double</code>, then the value is treated as <code>binnedValuesNaN</code>.</td><td>Val-<i>range</i></td></tr>
 * <tr><td>binnedValuesNaN</td><td>double</td><td>-1.0</td><td>If the value of a numeric binnedValues field is not a number, it will be given this value.</td></tr>
 * <tr><td>binnedValuesStatistics</td><td>boolean</td><td>false</td><td>If true, print to stderr a contingency table of statistics for binnedValues.</td><td></td></tr>
 * <tr><td>countChars</td><td>String</td><td>null</td><td>If non-null, count the number of occurrences of each character in the String, and make a feature for each character, binned according to <code>countCharsBins</code></td><td>Char-<i>ch</i>-<i>range</i></td></tr>
 * <tr><td>countCharsBins</td><td>String</td><td>"0,1"</td><td>Treat as a sequence of comma separated integer bounds, where character counts above the previous bound up to and including the next bound are binned. For instance, a value of "0,2" will give 3 bins, dividing a character count into bins of 0, 1-or-2, and 3-or-more occurrences.</td><td></td></tr>
 * <tr><td>splitWordsRegexp</td><td>String</td><td>null</td><td>If defined, use this as a regular expression on which to split the whole string (as in the String.split() function, which will return the things between delimiters, and discard the delimiters).  The resulting split-up "words" will be used in classifier features iff one of the other "useSplit" options is turned on.</td></tr>
 * <tr><td>splitWordsTokenizerRegexp</td><td>String</td><td>null</td><td>If defined, use this as a regular expression to cut initial pieces off a String.  This regular expression <i>should always match</i> the String, and the size of the token is the number of characters matched.  So, for example, one can group letter and number characters but do nothing else with a regular expression like <code>([A-Za-z]+|[0-9]+|.)</code>.  (If the regular expression doesn't match, the first character of the string is treated as a one character word, and then matching is tried again, but in this case a warning message is printed.)  Note that, for Java regular expressions with disjunctions like this, the match is the first matching disjunction, not the longest matching disjunction, so patterns with common prefixes need to be ordered from most specific (longest) to least specific (shortest).)  The resulting split up "words" will be used in classifier features iff one of the other "useSplit" options is turned on.  Note that as usual for Java String processing, backslashes must be doubled in the regular expressions that you write.</td></tr>
 * <tr><td>splitWordsIgnoreRegexp</td><td>String</td><td>null</td><td>If defined, this regexp is used to determine character sequences which should not be returned as tokens when using the splitWordsTokenizerRegexp.  Typically, these might be whitespace tokens (i.e., \\s+).</td></tr>
 * <tr><td> useSplitWords</td><td>boolean</td><td>false</td><td>Make features from the "words" that are returned by dividing the string on splitWordsRegexp or splitWordsTokenizerRegexp.  Requires splitWordsRegexp or splitWordsTokenizerRegexp.</td><td>SW-<i>str</i></td></tr>
 * <tr><td> useLowercaseSplitWords</td><td>boolean</td><td>false</td><td>Make features from the "words" that are returned by dividing the string on splitWordsRegexp or splitWordsTokenizerRegexp and then lowercasing the result.  Requires splitWordsRegexp or splitWordsTokenizerRegexp.  Note that this can be specified independently of useSplitWords. You can put either or both original cased and lowercased words in as features.</td><td>SW-<i>str</i></td></tr>
 * <tr><td> useSplitWordPairs</td><td>boolean</td><td>false</td><td>Make features from the pairs of adjacent "words" that are returned by dividing the string into splitWords.  Requires splitWordsRegexp or splitWordsTokenizerRegexp.</td><td>SWP-<i>str1</i>-<i>str2</i></td></tr>
 * <tr><td>  maxWordNGramLeng</td><td>int</td><td>-1</td><td>If this number is positive, word n-grams above this size will not be used in the model</td></tr>
 * <tr><td>  minWordNGramLeng</td><td>int</td><td>1</td><td>Must be positive. word n-grams below this size will not be used in the model</td></tr>
 * <tr><td>  wordNGramBoundaryRegexp</td><td>String</td><td>null</td><td>If this is defined and the regexp matches, then the ngram stops</td></tr>
 * <tr><td> useSplitFirstLastWords</td><td>boolean</td><td>false</td><td>Make features from the first and last "words" that are returned as splitWords.  Requires splitWordsRegexp or splitWordsTokenizerRegexp.</td><td>SFW-<i>str</i>, SLW-<i>str</i></td></tr>
 * <tr><td> useSplitNGrams</td><td>boolean</td><td>false</td><td>Make features from letter n-grams - internal as well as edge all treated the same - after the data string has been split into tokens.  Requires splitWordsRegexp or splitWordsTokenizerRegexp.</td><td>S#-<i>str</i></td></tr>
 * <tr><td> useSplitPrefixSuffixNGrams</td><td>boolean</td><td>false</td><td>Make features from prefixes and suffixes after splitting with splitWordsRegexp.  Requires splitWordsRegexp or splitWordsTokenizerRegexp.</td><td>S#B-<i>str</i>, S#E-<i>str</i></td></tr>
 * <tr><td> useNGrams</td><td>boolean</td><td>false</td><td>Make features from letter n-grams - internal as well as edge all treated the same.</td><td>#-<i>str</i></td></tr>
 * <tr><td> usePrefixSuffixNGrams</td><td>boolean</td><td>false</td><td>Make features from prefix and suffix strings.</td><td>#B-<i>str</i>, #E-<i>str</i></td></tr>
 * <tr><td> lowercase</td><td>boolean</td><td>false</td><td>Make the input string lowercase so all features work unicase</td></tr>
 * <tr><td> lowercaseNGrams</td><td>boolean</td><td>false</td><td>Make features from letter n-grams all lowercase (for both useNGrams and usePrefixSuffixNGrams)</td></tr>
 * <tr><td> maxNGramLeng</td><td>int</td><td>-1</td><td>If this number is positive, n-grams above this size will not be used in the model</td></tr>
 * <tr><td> minNGramLeng</td><td>int</td><td>2</td><td>Must be positive. n-grams below this size will not be used in the model</td></tr>
 * <tr><td> partialNGramRegexp</td><td>String</td><td>null</td><td>If this is defined and the regexp matches, then n-grams are made only from the matching text (if no capturing groups are defined) or from the first capturing group of the regexp, if there is one.  This substring is used for both useNGrams and usePrefixSuffixNGrams.</td></tr>
 * <tr><td> realValued</td><td>boolean</td><td>false</td><td>Treat this column as real-valued and do not perform any transforms on the feature value.</td><td>Value</td></tr>
 * <tr><td> logTransform</td><td>boolean</td><td>false</td><td>Treat this column as real-valued and use the log of the value as the feature value.</td><td>Log</td></tr>
 * <tr><td> logitTransform</td><td>boolean</td><td>false</td><td>Treat this column as real-valued and use the logit of the value as the feature value.</td><td>Logit</td></tr>
 * <tr><td> sqrtTransform</td><td>boolean</td><td>false</td><td>Treat this column as real-valued and use the square root of the value as the feature value.</td><td>Sqrt</td></tr>
 * <tr><td> wordShape</td><td>String</td><td>none</td><td>Either "none" for no wordShape use, or the name of a word shape function recognized by {@link edu.stanford.nlp.process.WordShapeClassifier#lookupShaper(String)}, such as "dan1" or "chris4".  WordShape functions equivalence-class strings based on the pattern of letter, digit, and symbol characters that they contain.  The details depend on the particular function chosen.</td><td>SHAPE-<i>str</i></td></tr>
 * <tr><td> splitWordShape</td><td>String</td><td>none</td><td>Either "none" for no wordShape or the name of a word shape function recognized by {@link WordShapeClassifier#lookupShaper(String)}.  This is applied to each "word" found by splitWordsRegexp or splitWordsTokenizerRegexp.</td><td>SSHAPE-<i>str</i></td></tr>
 * <p/>
 * <tr><td>featureMinimumSupport</td><td>int</td><td>0</td><td>A feature, that is, an (observed,class) pair, will only be included in the model providing it is seen a minimum of this number of times in the training data.</td></tr>
 * <tr><td> biasedHyperplane</td><td>String</td><td>null</td><td>If non-null, a sequence of comma-separated pairs of <i>className prob</i>.  An item will only be classified to a certain class <i>className</i> if its probability of class membership exceeds the given conditional probability <i>prob</i>; otherwise it will be assigned to a different class.  If this list of classes is exhaustive, and no condition is satisfied, then the most probable class is chosen.</td></tr>
 * <tr><td> printFeatures</td><td>String</td><td>null</td><td>Print out the features of the classifier to a file based on this name.</td></tr>
 * <tr><td> printClassifier</td><td>String</td><td>null</td><td>Style in which to print the classifier. One of: HighWeight, HighMagnitude, AllWeights, WeightHistogram, WeightDistribution. See LinearClassifier class for details.</td></tr>
 * <tr><td> printClassifierParam</td><td>int</td><td>100</td><td>A parameter to the printing style, which may give, for example the number of parameters to print (for HighWeight or HighMagnitude).</td></tr>
 * <tr><td>justify</td><td>boolean</td><td>false</td><td>For each test data
 * item, print justification
 * (weights) for active features used in classification.</td></tr>
 * <tr><td>exitAfterTrainingFeaturization</td><td>boolean</td><td>false</td><td>If true, the program exits after reading the training data (trainFile) and before building a classifier.  This is useful in conjunction with printFeatures, if one only wants to convert data to features for use with another classifier.</td></tr>
 * <p/>
 * <tr><td>intern</td><td>boolean</td><td>false</td><td>If true, (String) intern all of the (final) feature names.  Recommended (this saves memory, but slows down feature generation in training).</td></tr>
 * <tr><td>cacheNGrams</td><td>boolean</td><td>false</td><td>If true, record the NGram features that correspond to a String (under the current option settings and reuse rather than recalculating if the String is seen again.  <b>Disrecommended (speeds training but can require enormous amounts of memory).</b></td></tr>
 * <p/>
 * <tr><td>useNB</td><td>boolean</td><td>false</td><td>Use a Naive Bayes generative classifier (over all features) rather than a discriminative logistic regression classifier.  (Set <code>useClass</code> to true to get a prior term.)</td></tr>
 * <tr><td>useBinary</td><td>boolean</td><td>false</td><td>Use the binary classifier (i.e. use LogisticClassifierFactory, rather than LinearClassifierFactory) to get classifier</td></tr>
 * <tr><td>l1reg</td><td>double</td><td>0.0</td><td>If set to be larger than 0, uses L1 regularization</td></tr>
 * <tr><td>useAdaptL1</td><td>boolean</td><td>false</td><td>If true, uses adaptive L1 regularization to find value of l1reg that gives the desired number of features set by limitFeatures</td></tr>
 * <tr><td> l1regmin</td><td>double</td><td>0.0</td><td>Minimum L1 in search</td></tr>
 * <tr><td> l1regmax</td><td>double</td><td>500.0</td><td>Maximum L1 in search</td></tr>
 * <tr><td> featureWeightThreshold</td><td>double</td><td>0.0</td><td>Threshold of model weight at which feature is kept. "Unimportant" low weight features are discarded. (Currently only implemented for adaptL1.)</td></tr>
 * <tr><td> limitFeaturesLabels</td><td>String</td><td>null</td><td>If set, only include features for these labels in the desired number of features</td></tr>
 * <tr><td> limitFeatures</td><td>int</td><td>0</td><td>If set to be larger than 0, uses adaptive L1 regularization to find value of l1reg that gives the desired number of features</td></tr>
 * <tr><td>prior</td><td>String/int</td><td>quadratic</td><td>Type of prior (regularization penalty on weights). Possible values are null, "no", "quadratic", "huber", "quartic", "cosh", or "adapt". See {@link edu.stanford.nlp.classify.LogPrior LogPrior} for more information.</td></tr>
 * <tr><td> useSum</td><td>boolean</td><td>false</td><td>Do optimization via summed conditional likelihood, rather than the product.  (This is expensive, non-standard, and somewhat unstable, but can be quite effective: see Klein and Manning 2002 EMNLP paper.)</td></tr>
 * <tr><td> tolerance</td><td>double</td><td>1e-4</td><td>Convergence tolerance in parameter optimization</td></tr>
 * <tr><td> sigma</td><td>double</td><td>1.0</td><td>A parameter to several of the smoothing (i.e., regularization) methods, usually giving a degree of smoothing as a standard deviation (with small positive values being stronger smoothing, and bigger values weaker smoothing)</td></tr>
 * <tr><td> epsilon</td><td>double</td><td>0.01</td><td>Used only as a parameter in the Huber loss: this is the distance from 0 at which the loss changes from quadratic to linear</td></tr>
 * <tr><td>useQN</td><td>boolean</td><td>true</td><td>Use Quasi-Newton optimization if true, otherwise use Conjugate Gradient optimization.  Recommended.</td></tr>
 * <tr><td>QNsize</td><td>int</td><td>15</td><td>Number of previous iterations of Quasi-Newton to store (this increases memory use, but speeds convergence by letting the Quasi-Newton optimization more effectively approximate the second derivative).</td></tr>
 * <tr><td>featureFormat</td><td>boolean</td><td>false</td><td>Assumes the input file isn't text strings but already featurized.  One column is treated as the class column (as defined by <code>goldAnswerColumn</code>, and all other columns are treated as features of the instance.  (If answers are not present, set <code>goldAnswerColumn</code> to a negative number.)</td></tr>
 * <tr><td>trainFromSVMLight</td><td>boolean</td><td>false</td><td>Assumes the trainFile is in SVMLight format (see <a href="http://svmlight.joachims.org/">SVMLight webpage</a> for more information)</td></tr>
 * <tr><td>testFromSVMLight</td><td>boolean</td><td>false</td><td>Assumes the testFile is in SVMLight format</td></tr>
 * </table>
 *
 * @author Christopher Manning
 * @author Anna Rafferty
 * @author Angel Chang (add options for using l1reg)
 */
public class ColumnDataClassifier {

  //Distinguishes whether this file has real valued features or if the more efficient non-RVF representation can be used
  private static boolean usesRealValues; // = false;
  private static final double DEFAULT_VALUE = 1.0;

  // not a class that can be instantiated
  private ColumnDataClassifier() {
  }


  /**
   * Entry point to importing a line of a file and translating it into features.
   * If real valued features are used, this method accesses makeRVFDatumFromLine
   * and returns an RVFDatum; otherwise, categorical features are used.
   *
   * @param line Line of file
   * @param lineNo The line number
   * @return A Datum (may be an RVFDatum)
   */
  public static Datum<String,String> makeDatumFromLine(String line, int lineNo) {
    if (usesRealValues) {
      return makeRVFDatumFromLine(line,lineNo);
    }

    if (globalFlags.featureFormat) {
      String[] fields = tab.split(line);
      Collection<String> theFeatures = new ArrayList<String>();
      for (int i = 0; i < fields.length; i++) {
        if (i != globalFlags.goldAnswerColumn)
            if (globalFlags.significantColumnId) {
              theFeatures.add(String.format("%d:%s", i, fields[i]));
            } else {
              theFeatures.add(fields[i]);
            }
      }
      return new BasicDatum<String,String>(theFeatures, fields[globalFlags.goldAnswerColumn]);
    } else {
      String[] wi = makeSimpleLineInfo(line, lineNo);
      // System.err.println("Read in " + wi);
      return makeDatum(wi);
    }
  }



  private static RVFDatum<String,String> makeRVFDatumFromLine(String line, int lineNo) {
    if (globalFlags.featureFormat) {
      String[] fields = tab.split(line);
      ClassicCounter<String> theFeatures = new ClassicCounter<String>();
      for (int i = 0; i < fields.length; i++) {
        if (i != globalFlags.goldAnswerColumn) {
          if (flags[i] != null && (flags[i].isRealValued || flags[i].logTransform || flags[i].logitTransform || flags[i].sqrtTransform)) {
            addFeatureValue(fields[i], flags[i], theFeatures);
          } else {
            theFeatures.setCount(fields[i], 1.0);
          }
        }
      }

      return new RVFDatum<String,String>(theFeatures, fields[globalFlags.goldAnswerColumn]);
    } else {
      String[] wi = makeSimpleLineInfo(line, lineNo);
      // System.err.println("Read in " + wi);
      return makeRVFDatum(wi);
    }
  }

  // NB: This is meant to do splitting strictly only on tabs, and to thus work with things that are exactly TSV files.  It shouldn't split on all whitespace, because it is useful to be able to have spaces inside fields for short text documents, and then to be able to split them into words with features like useSplitWords
  private static final Pattern tab = Pattern.compile("\\t");

  public static String[] makeSimpleLineInfo(String line, int lineNo) {
    String[] strings = tab.split(line);
    if (strings.length < 2) {
      throw new RuntimeException("Line format error at line " + lineNo + ": " + line);
    }
    return strings;
  }

  public static GeneralDataset<String,String> readTrainingExamples(String fileName) {
    if (globalFlags.printFeatures != null) {
      newFeaturePrinter(globalFlags.printFeatures, "train");
    }
    Pair<GeneralDataset<String,String>, List<String[]>> dataInfo = readDataset(fileName, false);
    GeneralDataset<String,String> train = dataInfo.first();
    if (globalFlags.featureMinimumSupport > 1) {

      System.err.println("Removing Features with counts < " + globalFlags.featureMinimumSupport);
      train.applyFeatureCountThreshold(globalFlags.featureMinimumSupport);
    }
    train.summaryStatistics();
    return train;

  }

  private static Pair<GeneralDataset<String,String>,List<String[]>> readTestExamples(String filename) {
    return readDataset(filename, true);
  }

  public static List<String[]> makeSimpleLineInfos(List<String> lines) {
    List<String[]> lineInfos = new ArrayList<String[]>();
    for (String line : lines) {
      lineInfos.add(makeSimpleLineInfo(line,-1));
    }
    return lineInfos;
  }

  //In test phase, returns the String[][] with the simple line infos for print purpose
  //Otherwise, returns null for string[][]
  private static Pair<GeneralDataset<String,String>, List<String[]>> readDataset(String filename, boolean inTestPhase) {
    GeneralDataset<String,String> dataset;
    List<String[]> lineInfos = null;
    if ((inTestPhase && Flags.testFromSVMLight) || (!inTestPhase && Flags.trainFromSVMLight)) {
      List<String> lines = null;
      if (inTestPhase)
        lines = new ArrayList<String>();
      if (usesRealValues) {
        dataset = RVFDataset.readSVMLightFormat(filename, lines);
      } else {
        dataset = Dataset.readSVMLightFormat(filename, lines);
      }
      if(lines != null)
        lineInfos = makeSimpleLineInfos(lines);
    } else {
      try {
        if(inTestPhase)
          lineInfos = new ArrayList<String[]>();
        if (usesRealValues) {
          dataset = new RVFDataset<String,String>();
        } else {
          dataset = new Dataset<String,String>();
        }
        int lineNo = 0;
        //String line;
        for(String line: ObjectBank.getLineIterator(new File(filename))){
          lineNo++;
          if(inTestPhase) {
            String[] wi = makeSimpleLineInfo(line, lineNo);
            lineInfos.add(wi);
          }
          Datum<String,String> d = makeDatumFromLine(line, lineNo);
          if (d != null) {
            dataset.add(d);
          }
        }
      } catch(Exception e) {
        e.printStackTrace();
        throw new RuntimeException("Training set could not be found:"+e);
      }
    }

    return new Pair<GeneralDataset<String,String>,List<String[]>>(dataset, lineInfos);
  }


  /**
   * Write summary statistics about a group of answers.
   */
  private static void writeResultsSummary(int num, Counter<String> contingency, Collection<String> labels) {
    System.err.println();
    NumberFormat nf = NumberFormat.getNumberInstance();
    nf.setMinimumFractionDigits(3);
    nf.setMaximumFractionDigits(3);
    System.err.print(num + " examples");
    if (globalFlags.groupingColumn >= 0 && globalFlags.rankingAccuracyClass != null) {
      System.err.print(" and " + numGroups + " ranking groups");
    }
    System.err.println(" in test set");
    int numClasses = 0;
    double microAccuracy = 0.0;
    double macroF1 = 0.0;
    for (Object key : labels) {
      numClasses++;
      int tp = (int) contingency.getCount(key + "|TP");
      int fn = (int) contingency.getCount(key + "|FN");
      int fp = (int) contingency.getCount(key + "|FP");
      int tn = (int) contingency.getCount(key + "|TN");
      double p = (tp == 0) ? 0.0 : ((double) tp) / (tp + fp);
      double r = (tp == 0) ? 0.0 : ((double) tp) / (tp + fn);
      double f = (p == 0.0 && r == 0.0) ? 0.0 : 2 * p * r / (p + r);
      double acc = ((double) tp + tn)/num;
      macroF1 += f;
      microAccuracy += tp;
      System.err.println("Cls " + key + ": TP=" + tp + " FN=" + fn + " FP=" + fp + " TN=" + tn + "; Acc " + nf.format(acc) + " P " + nf.format(p) + " R " + nf.format(r) + " F1 " + nf.format(f));
    }
    if (globalFlags.groupingColumn >= 0 && globalFlags.rankingAccuracyClass != null) {
      double cor = (int) contingency.getCount("Ranking|Correct");
      double err = (int) contingency.getCount("Ranking|Error");
      double rankacc = (cor + err == 0) ? 0 : cor / (cor + err);
      System.err.print("Ranking accuracy: " + nf.format(rankacc));
      double cov = (int) contingency.getCount("Ranking|Covered");
      double coverr = (int) contingency.getCount("Ranking|Uncovered");
      double covacc = (cov + coverr == 0) ? 0 : cov / (cov + coverr);
      if (coverr > 0.5) {
        double ce = (int) (contingency.getCount("Ranking|Error") - contingency.getCount("Ranking|Uncovered"));
        double crankacc = (cor + ce == 0) ? 0 : cor / (cor + ce);
        System.err.println(" (on " + nf.format(covacc) + " of groups with correct answer: " + nf.format(crankacc) + ")");
      } else {
        System.err.println();
      }
    }
    if (globalFlags.groupingColumn >= 0 && globalFlags.rankingScoreColumn >= 0 && globalFlags.rankingAccuracyClass != null) {
      double totalSim = contingency.getCount("Ranking|Score");
      double cor = (int) contingency.getCount("Ranking|Correct");
      double err = (int) contingency.getCount("Ranking|Error");
      double ranksim = (cor + err == 0) ? 0 : totalSim / (cor + err);
      System.err.println("Ranking average score: " + nf.format(ranksim));
    }
    microAccuracy = microAccuracy / num;
    macroF1 = macroF1 / numClasses;
    nf.setMinimumFractionDigits(5);
    nf.setMaximumFractionDigits(5);
    System.err.println("Micro-averaged accuracy/F1: " + nf.format(microAccuracy));
    System.err.println("Macro-averaged F1: " + nf.format(macroF1));
  }


  private static int numGroups = 0;
  private static String lastGroup = "";
  private static int numInGroup = 0;
  private static double bestProb = 0.0;
  private static double bestSim = 0.0;
  private static boolean currentHighestProbCorrect = false;
  private static boolean foundAnswerInGroup = false;

  /**
   * Write out an answer, and update statistics.
   */
  private static void writeAnswer(String[] strs, String clAnswer, Distribution<String> cntr, Counter<String> contingency, Classifier<String,String> c, double sim) {
    String goldAnswer = strs[globalFlags.goldAnswerColumn];
    String printedText = "";
    if (globalFlags.displayedColumn >= 0) {
      printedText = strs[globalFlags.displayedColumn];
    }
    String line;
    if ("".equals(printedText)) {
      line = goldAnswer + "\t" + clAnswer + "\t" + cntr.probabilityOf(clAnswer);
    } else {
      line = printedText + "\t" + goldAnswer + "\t" + clAnswer + "\t" + cntr.probabilityOf(clAnswer);
    }
    System.out.println(line);
    for (String next : c.labels()) {
      if (next.equals(goldAnswer) && next.equals(clAnswer)) {
        contingency.incrementCount(next + "|TP");
      } else if (next.equals(goldAnswer)) {
        contingency.incrementCount(next + "|FN");
      } else if (next.equals(clAnswer)) {
        contingency.incrementCount(next + "|FP");
      } else {
        contingency.incrementCount(next + "|TN");
      }
    }
    if (globalFlags.groupingColumn >= 0 && globalFlags.rankingAccuracyClass != null) {
      String group = strs[globalFlags.groupingColumn];
      // System.err.println("Group is " + group);
      if (group.equals(lastGroup)) {
        numInGroup++;
        double prob = cntr.probabilityOf(globalFlags.rankingAccuracyClass);
        // System.err.println("  same group; prob is " + prob);
        if (prob > bestProb) {
          bestProb = prob;
          bestSim = sim;
          // System.err.println("  better prob than before");
          currentHighestProbCorrect = goldAnswer.equals(globalFlags.rankingAccuracyClass);
        }
        if (globalFlags.rankingAccuracyClass.equals(goldAnswer)) {
          foundAnswerInGroup = true;
        }
      } else {
        finishRanking(contingency, bestSim);
        numGroups++;
        lastGroup = group;
        bestProb = cntr.probabilityOf(globalFlags.rankingAccuracyClass);
        bestSim = sim;
        // System.err.println("  different; prob is " + bestProb);
        numInGroup = 1;
        currentHighestProbCorrect = goldAnswer.equals(globalFlags.rankingAccuracyClass);
        foundAnswerInGroup = globalFlags.rankingAccuracyClass.equals(goldAnswer);
      }
    }
  }


  static void finishRanking(Counter<String> contingency, double sim) {
    if (numInGroup > 0) {
      if (globalFlags.justify) {
        NumberFormat nf = NumberFormat.getNumberInstance();
        nf.setMinimumFractionDigits(3);
        nf.setMaximumFractionDigits(3);
        System.err.print("Previous group of " + numInGroup + ": ");
        if (!foundAnswerInGroup) {
          System.err.print("no correct answer; ");
        }
        System.err.print("highest ranked guess was: " + ((currentHighestProbCorrect ? "correct" : "incorrect")));
        System.err.println(" (sim. = " + nf.format(sim) + ")");
      }
      if (currentHighestProbCorrect) {
        contingency.incrementCount("Ranking|Correct");
      } else {
        contingency.incrementCount("Ranking|Error");
      }
      if (foundAnswerInGroup) {
        contingency.incrementCount("Ranking|Covered");
      } else {
        contingency.incrementCount("Ranking|Uncovered");
      }
      contingency.incrementCount("Ranking|Score", sim);
    }
  }


  static void readAndTestExamples(Classifier<String,String> cl, String filename) {
    if (globalFlags.printFeatures != null) {
      newFeaturePrinter(globalFlags.printFeatures, "test");
    }

    Counter<String> contingency = new ClassicCounter<String>();  // store tp,fp,fn,tn
    Pair<GeneralDataset<String,String>,List<String[]>> testInfo = readTestExamples(filename);
    GeneralDataset<String,String> test = testInfo.first();
    List<String[]> lineInfos = testInfo.second();
    for(int i = 0; i < test.size; i++) {
      String[] simpleLineInfo = lineInfos.get(i);
      Datum<String,String> d;
      if (usesRealValues) {
        d = test.getRVFDatum(i);
      } else {
        d = test.getDatum(i);
      }
      if (globalFlags.justify) {
        System.err.println("### Test item " + i);
        for (String field : simpleLineInfo) {
          System.err.print(field);
          System.err.print('\t');
        }
        System.err.println();
        if (cl instanceof LinearClassifier<?,?>) {
          ((LinearClassifier<String,String>)cl).justificationOf(d);
        }
        System.err.println();
      }
      Counter<String> logScores;
      if(usesRealValues) {
        logScores = ErasureUtils.<RVFClassifier<String,String>>uncheckedCast(cl).scoresOf((RVFDatum<String,String>)d);
      } else {
        logScores = cl.scoresOf(d);
      }
      Distribution<String> dist = Distribution.distributionFromLogisticCounter(logScores);
      String answer = null;
      if (globalFlags.biasedHyperplane != null) {
        // System.err.println("Biased using counter: " +
        //         globalFlags.biasedHyperplane);
        List<String> biggestKeys = new ArrayList<String>(logScores.keySet());
        Collections.sort(biggestKeys, Counters.toComparatorDescending(logScores));
        for (String key : biggestKeys) {
          double prob = dist.probabilityOf(key);
          double threshold = globalFlags.biasedHyperplane.getCount(key);
          // System.err.println("  Trying " + key + " prob is " + prob +
          //           " threshold is " + threshold);
          if (prob > threshold) {
            answer = key;
            break;
          }
        }
      }
      if (answer == null) {
        if (usesRealValues) {
          answer = ErasureUtils.<RVFClassifier<String,String>>uncheckedCast(cl).classOf((RVFDatum<String,String>) d);
        } else {
          answer = cl.classOf(d);
        }
      }
      double sim = 0.0;
      if (globalFlags.rankingScoreColumn >= 0) {
        try {
          sim = Double.parseDouble(simpleLineInfo[globalFlags.rankingScoreColumn]);
        } catch (NumberFormatException nfe) {
          // just don't print it
        }
      }
      writeAnswer(simpleLineInfo, answer, dist, contingency, cl, sim);

    }
    if (globalFlags.groupingColumn >= 0 && globalFlags.rankingAccuracyClass != null)
      finishRanking(contingency, bestSim);

    if (globalFlags.printFeatures != null) {
      closeFeaturePrinter();
    }
    writeResultsSummary(test.size, contingency, cl.labels());
  }



  /**
   * Extracts all the features from a certain input datum.
   *
   * @param strs The data String[] to extract features from
   * @return The constructed Datum
   */
  static Datum<String,String> makeDatum(String[] strs) {
    List<String> theFeatures = new ArrayList<String>();
    Collection<String> globalFeatures = new HashSet<String>();
    if (globalFlags.useClassFeature) {
      globalFeatures.add("CLASS");
    }
    addAllInterningAndPrefixing(theFeatures, globalFeatures, "");

    for (int i = 0; i < flags.length; i++) {
      Collection<String> featuresC = new HashSet<String>();//important that this is a hash set to prevent same feature from being added multiple times
      makeDatum(strs[i], flags[i], featuresC, strs[globalFlags.goldAnswerColumn]);
      addAllInterningAndPrefixing(theFeatures, featuresC, i + "-");
    }

    if (globalFlags.printFeatures != null) {
      printFeatures(strs, theFeatures);
    }
    //System.out.println("Features are: " + theFeatures);
    return new BasicDatum<String,String>(theFeatures, strs[globalFlags.goldAnswerColumn]);
  }

/**
 * Extracts all the features from a certain input array and makes
 * a real valued feature datum; those features that are not real valued
 * are given value 1.0.
 *
 * @param strs The data String[] to extract features from
 * @return The constructed RVFDatum
 */
  static RVFDatum<String,String> makeRVFDatum(String[] strs) {
    ClassicCounter<String> theFeatures = new ClassicCounter<String>();
    ClassicCounter<String> globalFeatures = new ClassicCounter<String>();
    if (globalFlags.useClassFeature) {
      globalFeatures.setCount("CLASS",1.0);
    }
    addAllInterningAndPrefixingRVF(theFeatures, globalFeatures, "");

    for (int i = 0; i < flags.length; i++) {
      ClassicCounter<String> featuresC = new ClassicCounter<String>();
      makeDatum(strs[i], flags[i], featuresC, strs[globalFlags.goldAnswerColumn]);
      addAllInterningAndPrefixingRVF(theFeatures, featuresC, i + "-");
    }

    if (globalFlags.printFeatures != null) {
      printFeatures(strs, theFeatures);
    }
    //System.out.println("Features are: " + theFeatures);
    return new RVFDatum<String,String>(theFeatures, strs[globalFlags.goldAnswerColumn]);
  }

  private static void addAllInterningAndPrefixingRVF(ClassicCounter<String> accumulator, ClassicCounter<String> addend, String prefix) {
    for (String protoFeat : addend.keySet()) {
      double count = addend.getCount(protoFeat);
      if (!"".equals(prefix)) {
        protoFeat = prefix + protoFeat;
      }
      if (globalFlags.intern) {
        protoFeat = protoFeat.intern();
      }
      accumulator.incrementCount(protoFeat,count);
    }
  }

  private static void addAllInterningAndPrefixing(Collection<String> accumulator, Collection<String> addend, String prefix) {
    for (String protoFeat : addend) {
      if ( ! "".equals(prefix)) {
        protoFeat = prefix + protoFeat;
      }
      if (globalFlags.intern) {
        protoFeat = protoFeat.intern();
      }
      accumulator.add(protoFeat);
    }
  }

  /**
   * This method takes care of adding features to the collection-ish object features when
   * the value of the feature must be parsed as a real number, including performing
   * math transforms.
   */
  private static void addFeatureValue(String cWord, Flags flags, Object featuresC) {
    double value = Double.valueOf(cWord);
    if (flags.logTransform) {
      double log = Math.log(value/(1-value));
      if(Double.isInfinite(log) || Double.isNaN(log)) {
        System.err.println("WARNING: Log transform attempted on out of range value; feature ignored");
      } else
        addFeature(featuresC,"Log",Math.log(value));
    } else if(flags.logitTransform) {
      double logit = Math.log(value/(1-value));
      if(Double.isInfinite(logit) || Double.isNaN(logit)) {
        System.err.println("WARNING: Logit transform attempted on out of range value; feature ignored");
      } else {
        addFeature(featuresC,"Logit",logit);
      }
    } else if(flags.sqrtTransform) {
      double sqrt = Math.sqrt(value);
      addFeature(featuresC,"Sqrt",sqrt);
    } else {
      addFeature(featuresC,Flags.realValuedFeaturePrefix,value);
    }
  }

  /**
   * This method takes care of adding features to the collection-ish object features via
   * instanceof checks.  Features must be a type of collection or a counter, and value is used
   * iff it is a counter
   */
    private static <F> void addFeature(Object features, F newFeature, double value) {
      if(features instanceof Counter<?>) {
        ErasureUtils.<Counter<F>>uncheckedCast(features).setCount(newFeature, value);
      } else if(features instanceof Collection<?>) {
        ErasureUtils.<Collection<F>>uncheckedCast(features).add(newFeature);
      } else {
        throw new RuntimeException("addFeature was called with a features object that is neither a counter nor a collection!");
      }
    }

    /**
     * Extracts all the features from a certain input column
     *
     * @param cWord The String to extract data from
     */
    static void makeDatum(String cWord, Flags flags, Object featuresC, String goldAns) {
       //System.err.println("Making features for " + cWord + " flags " + flags);
      if (flags == null) {
        // no features for this column
        return;
      }
      if (flags.lowercase) {
        cWord = cWord.toLowerCase();
      }

      if (flags.useString) {
        addFeature(featuresC,"S-"+cWord,DEFAULT_VALUE);
      }
      if (flags.binnedLengths != null) {
        int len = cWord.length();
        String featureName = null;
        for (int i = 0; i <= flags.binnedLengths.length; i++) {
          if (i == flags.binnedLengths.length || len <= flags.binnedLengths[i]) {
            featureName = "Len-" + ((i == 0) ? 0 : (flags.binnedLengths[i - 1] + 1)) + "-" + ((i == flags.binnedLengths.length) ? "Inf" : Integer.toString(flags.binnedLengths[i]));
            if (flags.binnedLengthsCounter != null) {
              flags.binnedLengthsCounter.incrementCount(featureName, goldAns);
            }
            break;
          }
        }
        addFeature(featuresC,featureName,DEFAULT_VALUE);
      }
      if (flags.binnedValues != null) {
        double val = flags.binnedValuesNaN;
        try {
          val = Double.parseDouble(cWord);
        } catch (NumberFormatException nfe) {
          // do nothing -- keeps value of flags.binnedValuesNaN
        }
        String featureName = null;
        for (int i = 0; i <= flags.binnedValues.length; i++) {
          if (i == flags.binnedValues.length || val <= flags.binnedValues[i]) {
            featureName = "Val-(" + ((i == 0) ? "-Inf" : Double.toString(flags.binnedValues[i - 1])) + "," + ((i == flags.binnedValues.length) ? "Inf" : Double.toString(flags.binnedValues[i])) + "]";
            if (flags.binnedValuesCounter != null) {
              flags.binnedValuesCounter.incrementCount(featureName, goldAns);
            }
            break;
          }
        }
        addFeature(featuresC,featureName,DEFAULT_VALUE);
      }
      if (flags.countChars != null) {
        int[] cnts = new int[flags.countChars.length];
        for (int i = 0; i < cnts.length; i++) {
          cnts[i] = 0;
        }
        for (int i = 0, len = cWord.length(); i < len; i++) {
          char ch = cWord.charAt(i);
          for (int j = 0; j < cnts.length; j++) {
            if (ch == flags.countChars[j]) {
              cnts[j]++;
            }
          }
        }
        for (int j = 0; j < cnts.length; j++) {
          String featureName = null;
          for (int i = 0; i <= flags.countCharsBins.length; i++) {
            if (i == flags.countCharsBins.length || cnts[j] <= flags.countCharsBins[i]) {
              featureName = "Char-" + flags.countChars[j] + "-" + ((i == 0) ? 0 : (flags.countCharsBins[i - 1] + 1)) + "-" + ((i == flags.countCharsBins.length) ? "Inf" : Integer.toString(flags.countCharsBins[i]));
              break;
            }
          }
          addFeature(featuresC,featureName,DEFAULT_VALUE);
        }
      }
      if (flags.splitWordsRegexp != null || flags.splitWordsTokenizerRegexp != null ) {
        String[] bits;
        if (flags.splitWordsTokenizerRegexp != null) {
          bits = regexpTokenize(flags.splitWordsTokenizerPattern, flags.splitWordsIgnorePattern, cWord);
        } else {
          bits = flags.splitWordsPattern.split(cWord);
          if (flags.splitWordsIgnorePattern != null) {
            List<String> keepBits = new ArrayList<String>(bits.length);
            for (String bit:bits) {
              if (!flags.splitWordsIgnorePattern.matcher(bit).matches()) {
                keepBits.add(bit);
              }
            }
            if (keepBits.size() != bits.length) {
              bits = new String[keepBits.size()];
              keepBits.toArray(bits);
            }
          }
        }
        for (int i = 0; i < bits.length; i++) {
          if (flags.useSplitWords) {
            addFeature(featuresC, "SW-" + bits[i], DEFAULT_VALUE);
          }
          if (flags.useLowercaseSplitWords) {
            addFeature(featuresC, "LSW-" + bits[i].toLowerCase(), DEFAULT_VALUE);
          }
          if (flags.useSplitWordPairs) {
            if (i + 1 < bits.length) {
              addFeature(featuresC, "SWP-" + bits[i] + '-' + bits[i + 1], DEFAULT_VALUE);
            }
          }
          if (flags.useSplitWordNGrams) {
            StringBuilder sb = new StringBuilder("SW#");
            for (int j = i; j < i+flags.minWordNGramLeng-1; j++) {
              sb.append('-');
              sb.append(bits[j]);
            }
            int maxIndex = (flags.maxWordNGramLeng > 0)? Math.min(bits.length, i + flags.maxWordNGramLeng): bits.length;
            for (int j = i + flags.minWordNGramLeng-1; j < maxIndex; j++) {
              if (flags.wordNGramBoundaryRegexp != null) {
                if (flags.wordNGramBoundaryPattern.matcher(bits[j]).matches()) {
                  break;
                }
              }
              sb.append('-');
              sb.append(bits[j]);
              addFeature(featuresC, sb.toString(), DEFAULT_VALUE);
            }
          }
          if (flags.useSplitFirstLastWords) {
            if (i == 0) {
              addFeature(featuresC,"SFW-" + bits[i],DEFAULT_VALUE);
            } else if (i == bits.length - 1) {
              addFeature(featuresC,"SLW-" + bits[i],DEFAULT_VALUE);
            }
          }
          if (flags.useSplitNGrams || flags.useSplitPrefixSuffixNGrams) {
            Collection<String> featureNames = makeNGramFeatures(bits[i], flags, true, "S#");
            for(String featureName : featureNames)
              addFeature(featuresC,featureName,DEFAULT_VALUE);
          }
          if (flags.splitWordShape > edu.stanford.nlp.process.WordShapeClassifier.NOWORDSHAPE) {
            String shape = edu.stanford.nlp.process.WordShapeClassifier.wordShape(bits[i], flags.splitWordShape);
            // System.err.println("Shaper is " + flags.splitWordShape + " word len " + bits[i].length() + " shape is " + shape);
            addFeature(featuresC,"SSHAPE-" + shape,DEFAULT_VALUE);
          }
        }
      }
      if (flags.wordShape > WordShapeClassifier.NOWORDSHAPE) {
        String shape = edu.stanford.nlp.process.WordShapeClassifier.wordShape(cWord, flags.wordShape);
        addFeature(featuresC,"SHAPE-" + shape,DEFAULT_VALUE);
      }
      if (flags.useNGrams || flags.usePrefixSuffixNGrams) {
        Collection<String> featureNames = makeNGramFeatures(cWord, flags, false, "#");
        for(String featureName : featureNames)
          addFeature(featuresC,featureName,DEFAULT_VALUE);
      }
      if (flags.isRealValued || flags.logTransform || flags.logitTransform || flags.sqrtTransform) {
        addFeatureValue(cWord, flags, featuresC);

      }
       //System.err.println("Made featuresC " + featuresC);
    }  //end makeDatum




  /**
   * Caches a hash of word -> all substring features.  A _lot_ of memory!
   * If the String space is large, you shouldn't turn this on.
   */
  private static Map<String,Collection<String>> wordToSubstrings = new HashMap<String,Collection<String>>();


  private static String intern(String s) {
    if (globalFlags.intern) {
      return s.intern();
    }
    return s;
  }

  /**
   * Return a Collection of NGrams from the input.
   */
  private static Collection<String> makeNGramFeatures(final String input, Flags flags, boolean useSplit, String featPrefix) {
    String toNGrams = input;
    boolean internalNGrams;
    boolean prefixSuffixNGrams;
    if (useSplit) {
      internalNGrams = flags.useSplitNGrams;
      prefixSuffixNGrams = flags.useSplitPrefixSuffixNGrams;
    } else {
      internalNGrams = flags.useNGrams;
      prefixSuffixNGrams = flags.usePrefixSuffixNGrams;
    }
    if (flags.lowercaseNGrams) {
      toNGrams = toNGrams.toLowerCase();
    }
    if (flags.partialNGramRegexp != null) {
      Matcher m = flags.partialNGramPattern.matcher(toNGrams);
      // System.err.print("Matching |" + flags.partialNGramRegexp +
      //                "| against |" + toNGrams + "|");
      if (m.find()) {
        if (m.groupCount() > 0) {
          toNGrams = m.group(1);
        } else {
          toNGrams = m.group();
        }
        // System.err.print(" Matched |" + toNGrams + "|");
      }
      // System.err.println();
    }
    Collection<String> subs = null;
    if (flags.cacheNGrams) {
      subs = wordToSubstrings.get(toNGrams);
    }
    if (subs == null) {
      subs = new ArrayList<String>();
      String strN = featPrefix + "-";
      String strB = featPrefix + "B-";
      String strE = featPrefix + "E-";
      int wleng = toNGrams.length();
      for (int i = 0; i < wleng; i++) {
        for (int j = i + flags.minNGramLeng, min = Math.min(wleng, i + flags.maxNGramLeng); j <= min; j++) {
          if (prefixSuffixNGrams) {
            if (i == 0) {
              subs.add(intern(strB + toNGrams.substring(i, j)));
            }
            if (j == wleng) {
              subs.add(intern(strE + toNGrams.substring(i, j)));
            }
          }
          if (internalNGrams) {
            subs.add(intern(strN + toNGrams.substring(i, j)));
          }
        }
      }
      if (flags.cacheNGrams) {
        wordToSubstrings.put(toNGrams, subs);
      }
    }
    return subs;
  }


  private static PrintWriter cliqueWriter;

  private static void newFeaturePrinter(String prefix, String suffix) {
    if (cliqueWriter != null) {
      closeFeaturePrinter();
    }
    try {
      cliqueWriter = new PrintWriter(new FileOutputStream(prefix + "." + suffix), true);
    } catch (IOException ioe) {
      cliqueWriter = null;
    }
  }

  private static void closeFeaturePrinter() {
    cliqueWriter.close();
    cliqueWriter = null;
  }

  private static void printFeatures(String[] wi, ClassicCounter<String> features) {
    if (cliqueWriter != null) {
      for (int i = 0; i < wi.length; i++) {
        if (i > 0) {
          cliqueWriter.print("\t");
        }
        cliqueWriter.print(wi[i]);
      }
      for (String feat : features.keySet()) {
        cliqueWriter.print("\t");
        cliqueWriter.print(feat);
        cliqueWriter.print("\t");
        cliqueWriter.print(features.getCount(feat));
      }
      cliqueWriter.println();
    }
  }

  private static void printFeatures(String[] wi, List<String> features) {
    if (cliqueWriter != null) {
      for (int i = 0; i < wi.length; i++) {
        if (i > 0) {
          cliqueWriter.print("\t");
        }
        cliqueWriter.print(wi[i]);
      }
      for (String feat : features) {
        cliqueWriter.print("\t");
        cliqueWriter.print(feat);
      }
      cliqueWriter.println();
    }
  }

  /**
   * Creates classifier from training data
   * - searches for appropriate l1reg parameter to use to get specified number of features
   * @param train training data
   * @return trained classifier
   */
  static Classifier<String,String> makeClassifierAdaptL1(GeneralDataset<String,String> train){
    assert(globalFlags.useAdaptL1 && globalFlags.limitFeatures > 0);
    Classifier<String, String> lc = null;
    double l1reg = globalFlags.l1reg;
    double l1regmax = globalFlags.l1regmax;
    double l1regmin = globalFlags.l1regmin;
    if (globalFlags.l1reg <= 0.0)  {
      System.err.println("WARNING: useAdaptL1 set and limitFeatures to " + globalFlags.limitFeatures
              + ", but invalid value of l1reg=" + globalFlags.l1reg + ", defaulting to " + globalFlags.l1regmax);
      l1reg = l1regmax;
    } else {
      System.err.println("TRAIN: useAdaptL1 set and limitFeatures to " + globalFlags.limitFeatures
              + ", l1reg=" + globalFlags.l1reg + ", l1regmax=" + globalFlags.l1regmax + ", l1regmin=" + globalFlags.l1regmin);

    }
    Set<String> limitFeatureLabels = null;
    if (globalFlags.limitFeaturesLabels != null) {
      String[] labels = globalFlags.limitFeaturesLabels.split(",");
      limitFeatureLabels = new HashSet<String>();
      for (String label:labels) {
        limitFeatureLabels.add(label.trim());
      }
    }
    // Do binary search starting with specified l1reg to find reasonable value of l1reg that gives desired number of features
    double l1regtop = l1regmax;
    double l1regbottom = l1regmin;
    int limitFeatureTol = 5;
    double l1regminchange = 0.05;
    while (true) {
      System.err.println("Training: l1reg=" + l1reg + ", threshold=" + globalFlags.featureWeightThreshold
              + ", target=" + globalFlags.limitFeatures);
      LinearClassifierFactory<String,String> lcf;
      Minimizer<DiffFunction> minim = ReflectionLoading.loadByReflection("edu.stanford.nlp.optimization.OWLQNMinimizer", l1reg);
      lcf = new LinearClassifierFactory<String,String>(minim, globalFlags.tolerance, globalFlags.useSum, globalFlags.prior, globalFlags.sigma, globalFlags.epsilon);
      int featureCount = -1;
      try {
        LinearClassifier<String,String> c = lcf.trainClassifier(train);
        lc = c;
        featureCount = c.getFeatureCount(limitFeatureLabels, globalFlags.featureWeightThreshold, false /*useMagnitude*/);

        System.err.println("Training Done: l1reg=" + l1reg + ", threshold=" + globalFlags.featureWeightThreshold
                + ", features=" + featureCount + ", target=" + globalFlags.limitFeatures);
        //         String classifierDesc = c.toString(globalFlags.printClassifier, globalFlags.printClassifierParam);
        List<Triple<String,String,Double>> topFeatures = c.getTopFeatures(
                limitFeatureLabels, globalFlags.featureWeightThreshold, false /*useMagnitude*/,
                globalFlags.limitFeatures, true /*descending order*/);
        String classifierDesc = c.topFeaturesToString(topFeatures);
        System.err.println("Printing top " + globalFlags.limitFeatures + " features with weights above "
                + globalFlags.featureWeightThreshold);
        if (globalFlags.limitFeaturesLabels != null) {
          System.err.println("  Limited to labels: " + globalFlags.limitFeaturesLabels);
        }
        System.err.println(classifierDesc);
      } catch (RuntimeException ex) {
        if (ex.getMessage() != null && ex.getMessage().startsWith("L-BFGS chose a non-descent direction")) {
          System.err.println("Error in optimization, will try again with different l1reg");
          ex.printStackTrace(System.err);
        } else {
          throw ex;
        }
      }
      if (featureCount < 0 || featureCount < globalFlags.limitFeatures - limitFeatureTol) {
        // Too few features or some other bad thing happened => decrease l1reg
        l1regtop = l1reg;
        l1reg = 0.5*(l1reg + l1regbottom);
        if (l1regtop - l1reg < l1regminchange) {
          System.err.println("Stopping: old l1reg  " + l1regtop + "- new l1reg " + l1reg
                  + ", difference less than " + l1regminchange);
          break;
        }
      } else if (featureCount > globalFlags.limitFeatures + limitFeatureTol) {
        // Too many features => increase l1reg
        l1regbottom = l1reg;
        l1reg = 0.5*(l1reg + l1regtop);
        if (l1reg - l1regbottom < l1regminchange) {
          System.err.println("Stopping: new l1reg  " + l1reg + "- old l1reg " + l1regbottom
                  + ", difference less than " + l1regminchange);
          break;
        }
      } else {
        System.err.println("Stopping: # of features within " + limitFeatureTol + " of target");
        break;
      }
    }
    // Update flags for later serialization
    globalFlags.l1reg = l1reg;
    return lc;
  }

  /**
   * Creates classifier from training data
   * @param train training data
   * @return trained classifier
   */
  static Classifier<String,String> makeClassifier(GeneralDataset<String,String> train) {
    Classifier<String, String> lc;
    if (globalFlags.useClassifierFactory != null) {
      ClassifierFactory<String, String, Classifier<String,String>> cf;
      if (globalFlags.classifierFactoryArgs != null) {
        cf = ReflectionLoading.loadByReflection(globalFlags.useClassifierFactory, globalFlags.classifierFactoryArgs);
      } else {
        cf = ReflectionLoading.loadByReflection(globalFlags.useClassifierFactory);
      }
      lc = cf.trainClassifier(train);
    } else if (globalFlags.useNB) {
      double sigma = (globalFlags.prior == 0) ? 0.0 : globalFlags.sigma;
      lc = new NBLinearClassifierFactory<String,String>(sigma, globalFlags.useClassFeature).trainClassifier(train);
    } else if (globalFlags.useBinary) {
      LogisticClassifierFactory<String,String> lcf = new LogisticClassifierFactory<String,String>();
      LogPrior prior = new LogPrior(globalFlags.prior, globalFlags.sigma, globalFlags.epsilon);
      lc = lcf.trainClassifier(train, globalFlags.l1reg, globalFlags.tolerance, prior, globalFlags.biased);
    } else if (globalFlags.biased) {
      LogisticClassifierFactory<String,String> lcf = new LogisticClassifierFactory<String,String>();
      LogPrior prior = new LogPrior(globalFlags.prior, globalFlags.sigma, globalFlags.epsilon);
      lc = lcf.trainClassifier(train, prior, true);
    } else if (globalFlags.useAdaptL1 && globalFlags.limitFeatures > 0) {
      lc = makeClassifierAdaptL1(train);
    } else {
      LinearClassifierFactory<String,String> lcf;
      if (globalFlags.l1reg > 0.0) {
        Minimizer<DiffFunction> minim = ReflectionLoading.loadByReflection("edu.stanford.nlp.optimization.OWLQNMinimizer", globalFlags.l1reg);
        lcf = new LinearClassifierFactory<String,String>(minim, globalFlags.tolerance, globalFlags.useSum, globalFlags.prior, globalFlags.sigma, globalFlags.epsilon);
      } else {
        lcf  = new LinearClassifierFactory<String,String>(globalFlags.tolerance, globalFlags.useSum, globalFlags.prior, globalFlags.sigma, globalFlags.epsilon, globalFlags.QNsize);
      }
      if (!globalFlags.useQN) {
        lcf.useConjugateGradientAscent();
      }
      lc = lcf.trainClassifier(train);
    }
    return lc;
  }


  private static String[] regexpTokenize(Pattern splitRegexp, Pattern ignoreRegexp, String inWord) {
    List<String> al = new ArrayList<String>();
    String word = inWord;
    while (word.length() > 0) {
      // System.err.println("String to match on is " + word);
      Matcher mig = null;
      if (ignoreRegexp != null) {
        mig = ignoreRegexp.matcher(word);
      }
      if (mig != null && mig.lookingAt()) {
        word = word.substring(mig.end());
      } else {
        Matcher m = splitRegexp.matcher(word);
        if (m.lookingAt()) {
          // System.err.println("Matched " + m.end() + " chars: " +
          //		       word.substring(0, m.end()));
          al.add(word.substring(0, m.end()));
          word = word.substring(m.end());
        } else {
          System.err.println("Warning: regexpTokenize pattern " + splitRegexp + " didn't match on " + word);
          // System.err.println("Default matched 1 char: " +
          //		       word.substring(0, 1));
          al.add(word.substring(0, 1));
          word = word.substring(1);
        }
      }
    }
    String[] bits = al.toArray(new String[al.size()]);
//    System.err.println("Split |" + inWord + "| as: ");
//    for (int i = 0; i < bits.length; i++) {
//      System.err.println("  |" + bits[i] + "|");
//    }
    return bits;
  }


  private static Flags[] flags = new Flags[1];
  private static Flags globalFlags;


  /**
   * initialize using values in Properties file.
   */
  static void setProperties(Properties props) {
    Pattern prefix;
    try {
      prefix = Pattern.compile("([0-9]+)\\.(.*)");
    } catch (PatternSyntaxException pse) {
      pse.printStackTrace();
      return;
    }
    flags[0] = new Flags();  // initialize one used generally
    globalFlags = flags[0];

    for (Enumeration<?> e = props.propertyNames(); e.hasMoreElements();) {
      String key = (String) e.nextElement();
      String val = props.getProperty(key);
      int col = 0;  // the default (first after class)
      // System.err.println(key + " = " + val);
      Matcher matcher = prefix.matcher(key);
      if (matcher.matches()) {
        col = Integer.parseInt(matcher.group(1));
        key = matcher.group(2);
      }
      if (col >= flags.length) {
        Flags[] newFl = new Flags[col + 1];
        System.arraycopy(flags, 0, newFl, 0, flags.length);
        flags = newFl;
      }
      if (flags[col] == null) {
        flags[col] = new Flags();
      }

      if (key.equals("useString")) {
        flags[col].useString = Boolean.parseBoolean(val);
      } else if (key.equals("binnedLengths")) {
        if (val != null) {
          String[] binnedLengthStrs = val.split("[, ]+");
          flags[col].binnedLengths = new int[binnedLengthStrs.length];
          for (int i = 0; i < flags[col].binnedLengths.length; i++) {
            flags[col].binnedLengths[i] = Integer.parseInt(binnedLengthStrs[i]);
          }
        }
      } else if (key.equals("binnedLengthsStatistics")) {
        if (Boolean.parseBoolean(val)) {
          flags[col].binnedLengthsCounter = new TwoDimensionalCounter<String,String>();
        }
      } else if (key.equals("countChars")) {
        flags[col].countChars = val.toCharArray();
      } else if (key.equals("countCharsBins")) {
        if (val != null) {
          String[] binnedCountStrs = val.split("[, ]+");
          flags[col].countCharsBins = new int[binnedCountStrs.length];
          for (int i = 0; i < binnedCountStrs.length; i++) {
            flags[col].countCharsBins[i] = Integer.parseInt(binnedCountStrs[i]);
          }
        }
      } else if (key.equals("binnedValues")) {
        if (val != null) {
          String[] binnedValuesStrs = val.split("[, ]+");
          flags[col].binnedValues = new double[binnedValuesStrs.length];
          for (int i = 0; i < flags[col].binnedValues.length; i++) {
            flags[col].binnedValues[i] = Double.parseDouble(binnedValuesStrs[i]);
          }
        }
      } else if (key.equals("binnedValuesNaN")) {
        flags[col].binnedValuesNaN = Double.parseDouble(val);
      } else if (key.equals("binnedValuesStatistics")) {
        if (Boolean.parseBoolean(val)) {
          flags[col].binnedValuesCounter = new TwoDimensionalCounter<String,String>();
        }
      } else if (key.equals("useNGrams")) {
        flags[col].useNGrams = Boolean.parseBoolean(val);
      } else if (key.equals("usePrefixSuffixNGrams")) {
        flags[col].usePrefixSuffixNGrams = Boolean.parseBoolean(val);
      } else if (key.equals("useSplitNGrams")) {
        flags[col].useSplitNGrams = Boolean.parseBoolean(val);
      } else if (key.equals("wordShape")) {
        flags[col].wordShape = WordShapeClassifier.lookupShaper(val);
      } else if (key.equals("splitWordShape")) {
        flags[col].splitWordShape = WordShapeClassifier.lookupShaper(val);
      } else if (key.equals("useSplitPrefixSuffixNGrams")) {
        flags[col].useSplitPrefixSuffixNGrams = Boolean.parseBoolean(val);
      } else if (key.equals("lowercaseNGrams")) {
        flags[col].lowercaseNGrams = Boolean.parseBoolean(val);
      } else if (key.equals("lowercase")) {
        flags[col].lowercase = Boolean.parseBoolean(val);
      } else if (key.equals("useLowercaseSplitWords")) {
        flags[col].useLowercaseSplitWords = Boolean.parseBoolean(val);
      } else if (key.equals("useSum")) {
        flags[col].useSum = Boolean.parseBoolean(val);
      } else if (key.equals("tolerance")) {
        flags[col].tolerance = Double.parseDouble(val);
      } else if (key.equals("printFeatures")) {
        flags[col].printFeatures = val;
      } else if (key.equals("printClassifier")) {
        flags[col].printClassifier = val;
      } else if (key.equals("printClassifierParam")) {
        flags[col].printClassifierParam = Integer.parseInt(val);
      } else if (key.equals("exitAfterTrainingFeaturization")) {
        flags[col].exitAfterTrainingFeaturization = Boolean.parseBoolean(val);
      } else if (key.equals("intern") || key.equals("intern2")) {
        flags[col].intern = Boolean.parseBoolean(val);
      } else if (key.equals("cacheNGrams")) {
        flags[col].cacheNGrams = Boolean.parseBoolean(val);
      } else if (key.equals("useClassifierFactory")) {
        flags[col].useClassifierFactory = val;
      } else if (key.equals("classifierFactoryArgs")) {
        flags[col].classifierFactoryArgs = val;
      } else if (key.equals("useNB")) {
        flags[col].useNB = Boolean.parseBoolean(val);
      } else if (key.equals("useBinary")) {
        flags[col].useBinary = Boolean.parseBoolean(val);
      } else if (key.equals("l1reg")) {
        flags[col].l1reg = Double.parseDouble(val);
      } else if (key.equals("useAdaptL1")) {
        flags[col].useAdaptL1 = Boolean.parseBoolean(val);
      } else if (key.equals("limitFeatures")) {
        flags[col].limitFeatures = Integer.parseInt(val);
      } else if (key.equals("l1regmin")) {
        flags[col].l1regmin = Double.parseDouble(val);
      } else if (key.equals("l1regmax")) {
        flags[col].l1regmax = Double.parseDouble(val);
      } else if (key.equals("limitFeaturesLabels")) {
        flags[col].limitFeaturesLabels = val;
      } else if (key.equals("featureWeightThreshold")) {
        flags[col].featureWeightThreshold = Double.parseDouble(val);
      } else if (key.equals("useClassFeature")) {
        flags[col].useClassFeature = Boolean.parseBoolean(val);
      } else if (key.equals("featureMinimumSupport")) {
        flags[col].featureMinimumSupport = Integer.parseInt(val);
      } else if (key.equals("prior")) {
        if (val.equalsIgnoreCase("no")) {
          flags[col].prior = LogPrior.LogPriorType.NULL.ordinal();
        } else if (val.equalsIgnoreCase("huber")) {
          flags[col].prior = LogPrior.LogPriorType.HUBER.ordinal();
        } else if (val.equalsIgnoreCase("quadratic")) {
          flags[col].prior = LogPrior.LogPriorType.QUADRATIC.ordinal();
        } else if (val.equalsIgnoreCase("quartic")) {
          flags[col].prior = LogPrior.LogPriorType.QUARTIC.ordinal();
        } else {
          try {
            flags[col].prior = Integer.parseInt(val);
          } catch (NumberFormatException nfe) {
            System.err.println("Unknown prior " + val + "; using none.");
          }
        }
      } else if (key.equals("sigma")) {
        flags[col].sigma = Double.parseDouble(val);
      } else if (key.equals("epsilon")) {
        flags[col].epsilon = Double.parseDouble(val);
      } else if (key.equals("maxNGramLeng")) {
        flags[col].maxNGramLeng = Integer.parseInt(val);
      } else if (key.equals("minNGramLeng")) {
        flags[col].minNGramLeng = Integer.parseInt(val);
      } else if (key.equals("partialNGramRegexp")) {
        flags[col].partialNGramRegexp = val;
        try {
          flags[col].partialNGramPattern = Pattern.compile(flags[col].partialNGramRegexp);
        } catch (PatternSyntaxException pse) {
          System.err.println("Ill-formed partialNGramPattern: " + flags[col].partialNGramPattern);
          flags[col].partialNGramRegexp = null;
        }
      } else if (key.equals("splitWordsRegexp")) {
        flags[col].splitWordsRegexp = val;
        try {
          flags[col].splitWordsPattern = Pattern.compile(flags[col].splitWordsRegexp);
        } catch (PatternSyntaxException pse) {
          System.err.println("Ill-formed splitWords regexp: " + flags[col].splitWordsRegexp);
          flags[col].splitWordsRegexp = null;
        }
      } else if (key.equals("splitWordsTokenizerRegexp")) {
        flags[col].splitWordsTokenizerRegexp = val;
        try {
          flags[col].splitWordsTokenizerPattern = Pattern.compile(flags[col].splitWordsTokenizerRegexp);
        } catch (PatternSyntaxException pse) {
          System.err.println("Ill-formed splitWordsTokenizerRegexp: " + flags[col].splitWordsTokenizerRegexp);
          flags[col].splitWordsTokenizerRegexp = null;
        }
      } else if (key.equals("splitWordsIgnoreRegexp")) {
        flags[col].splitWordsIgnoreRegexp = val;
        try {
          flags[col].splitWordsIgnorePattern = Pattern.compile(flags[col].splitWordsIgnoreRegexp);
        } catch (PatternSyntaxException pse) {
          System.err.println("Ill-formed splitWordsIgnoreRegexp: " + flags[col].splitWordsIgnoreRegexp);
          flags[col].splitWordsIgnoreRegexp = null;
        }

      } else if (key.equals("useSplitWords")) {
        flags[col].useSplitWords = Boolean.parseBoolean(val);
      } else if (key.equals("useSplitWordPairs")) {
        flags[col].useSplitWordPairs = Boolean.parseBoolean(val);
      } else if (key.equals("useSplitWordNGrams")) {
        flags[col].useSplitWordNGrams = Boolean.parseBoolean(val);
      } else if (key.equals("maxWordNGramLeng")) {
        flags[col].maxWordNGramLeng = Integer.parseInt(val);
      } else if (key.equals("minWordNGramLeng")) {
        flags[col].minWordNGramLeng = Integer.parseInt(val);
      } else if (key.equals("wordNGramBoundaryRegexp")) {
        flags[col].wordNGramBoundaryRegexp = val;
        try {
          flags[col].wordNGramBoundaryPattern = Pattern.compile(flags[col].wordNGramBoundaryRegexp);
        } catch (PatternSyntaxException pse) {
          System.err.println("Ill-formed wordNGramBoundary regexp: " + flags[col].wordNGramBoundaryRegexp);
          flags[col].wordNGramBoundaryRegexp = null;
        }
      } else if (key.equals("useSplitFirstLastWords")) {
        flags[col].useSplitFirstLastWords = Boolean.parseBoolean(val);
      } else if (key.equals("loadClassifier")) {
        Flags.loadClassifier = val;
      } else if (key.equals("serializeTo")) {
        Flags.serializeTo = val;
      } else if (key.equals("printTo")) {
        Flags.printTo = val;
      } else if (key.equals("trainFile")) {
        Flags.trainFile = val;
      } else if (key.equals("testFile")) {
        Flags.testFile = val;
      } else if (key.equals("trainFromSVMLight")) {
        Flags.trainFromSVMLight = Boolean.parseBoolean(val);
      } else if (key.equals("testFromSVMLight")) {
        Flags.testFromSVMLight = Boolean.parseBoolean(val);
      }else if (key.equals("displayedColumn")) {
        flags[col].displayedColumn = Integer.parseInt(val);
      } else if (key.equals("groupingColumn")) {
        flags[col].groupingColumn = Integer.parseInt(val);
        System.err.println("Grouping column is " + (flags[col].groupingColumn));
      } else if (key.equals("rankingScoreColumn")) {
        flags[col].rankingScoreColumn = Integer.parseInt(val);
        System.err.println("Ranking score column is " + (flags[col].rankingScoreColumn));
      } else if (key.equals("rankingAccuracyClass")) {
        flags[col].rankingAccuracyClass = val;
      } else if (key.equals("goldAnswerColumn")) {
        flags[col].goldAnswerColumn = Integer.parseInt(val);
        System.err.println("Gold answer column is " + (flags[col].goldAnswerColumn));
      } else if (key.equals("useQN")) {
        flags[col].useQN = Boolean.parseBoolean(val);
      } else if (key.equals("QNsize")) {
        flags[col].QNsize = Integer.parseInt(val);
      } else if (key.equals("featureFormat")) {
        flags[col].featureFormat = Boolean.parseBoolean(val);
      } else if (key.equals("significantColumnId")) {
        flags[col].significantColumnId = Boolean.parseBoolean(val);
      } else if (key.equals("justify")) {
        flags[col].justify = Boolean.parseBoolean(val);
      }  else if (key.equals("realValued")) {
        flags[col].isRealValued = Boolean.parseBoolean(val);
        if(!ColumnDataClassifier.usesRealValues)
          ColumnDataClassifier.usesRealValues = flags[col].isRealValued;
      } else if (key.equals("logTransform")) {
        flags[col].logTransform = Boolean.parseBoolean(val);
        if(!ColumnDataClassifier.usesRealValues)
          ColumnDataClassifier.usesRealValues = flags[col].logTransform;
      } else if (key.equals("logitTransform")) {
        flags[col].logitTransform = Boolean.parseBoolean(val);
        if(!ColumnDataClassifier.usesRealValues)
          ColumnDataClassifier.usesRealValues = flags[col].logitTransform;
      } else if (key.equals("sqrtTransform")) {
        flags[col].sqrtTransform = Boolean.parseBoolean(val);
        if(!ColumnDataClassifier.usesRealValues)
          ColumnDataClassifier.usesRealValues = flags[col].sqrtTransform;
      } else if (key.equals("biased")) {
        flags[col].biased = Boolean.parseBoolean(val);
      } else if (key.equals("biasedHyperplane")) {
        // System.err.println("Constraints is " + constraints);
        if (val != null && val.trim().length() > 0) {
          String[] bits = val.split("[, ]+");
          flags[col].biasedHyperplane = new ClassicCounter<String>();
          for (int i = 0; i < bits.length; i += 2) {
            flags[col].biasedHyperplane.setCount(bits[i], Double.parseDouble(bits[i + 1]));
          }
        }
        // System.err.println("Biased Hyperplane is " + biasedHyperplane);
      } else if (key.length() > 0 && !key.equals("prop")) {
        System.err.println("Unknown property: |" + key + "|");
      }
    }
  }


  /**
   * Runs the ColumnDataClassifier.  Usage: <p><code>
   * java ColumnDataClassifier -trainFile trainFile
   * -testFile testFile [-useNGrams|-useString|-sigma sigma|...]
   * </code><p>or<p><code>
   * java ColumnDataClassifier -prop propFile
   * </code>
   *
   * @param args Command line arguments, as described in the class
   *             documentation
   */
  public static void main(String[] args) throws IOException {
    System.err.println("ColumnDataClassifier invoked at " + new Date() + " with arguments:");
    System.err.print(" ");
    for (String arg : args) {
      System.err.print(" " + arg);
    }
    System.err.println();

    setProperties(StringUtils.argsToProperties(args));

    String trainFile = Flags.trainFile;
    String testFile = Flags.testFile;
    String loadPath = Flags.loadClassifier;
    String serializeTo = Flags.serializeTo;

    Classifier<String,String> classifier = null;

    if (loadPath == null) {
      // if we are training the classifier now
      if ((testFile == null && serializeTo == null) || trainFile == null) {
        System.err.println("usage: java edu.stanford.nlp.classify.ColumnDataClassifier -prop propFile");
        System.err.println(" and/or: -trainFile trainFile -testFile testFile [-useNGrams|-sigma sigma|...]");
        System.exit(0);
      }

      // build the classifier
      GeneralDataset<String,String> train = readTrainingExamples(trainFile);
      // print any binned value histograms
      for (int i = 0; i < flags.length; i++) {
        if (flags[i] != null && flags[i].binnedValuesCounter != null) {
          System.err.println("BinnedValuesStatistics for column " + i);
          System.err.println(flags[i].binnedValuesCounter.toString());
        }
      }
      // print any binned length histograms
      for (int i = 0; i < flags.length; i++) {
        if (flags[i] != null && flags[i].binnedLengthsCounter != null) {
          System.err.println("BinnedLengthsStatistics for column " + i);
          System.err.println(flags[i].binnedLengthsCounter.toString());
        }
      }

      if (globalFlags.exitAfterTrainingFeaturization) {
        return; // ENDS PROCESSING
      }
      classifier = makeClassifier(train);
      String classString = null;
      if (classifier instanceof LinearClassifier<?,?>) {
        classString = ((LinearClassifier<?,?>)classifier).toString(globalFlags.printClassifier, globalFlags.printClassifierParam);
      } else if (classifier instanceof LogisticClassifier<?,?>) {
        classString = classifier.toString();
      }
      if (Flags.printTo != null) {
        try {
          BufferedWriter fw = new BufferedWriter(new FileWriter(Flags.printTo));
          fw.write(classString);
          fw.newLine();
          fw.close();
        } catch (IOException ioe) {
          ioe.printStackTrace();
        }
        System.err.println("Built classifier described in file " + Flags.printTo);
      } else {
        System.err.print("Built this classifier: ");
        System.err.println(classString);
      }

      // serialize the classifier
      if (serializeTo != null) {
        System.err.println("Serializing classifier to " + serializeTo + "...");
        //ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(serializeTo)));
        ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(FileUtils.getFileOutputStream(serializeTo)));
        oos.writeObject(classifier);
        oos.writeObject(flags);
        oos.close();
        System.err.println("Done.");
      }
    } else {
      // if we are loading a classifier
      System.err.println("Loading classifier from " + loadPath + "...");
      try {
        // load the classifier
        //ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(loadPath)));
        ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(FileUtils.getFileInputStream(loadPath)));
        classifier = ErasureUtils.<LinearClassifier<String,String>>uncheckedCast(ois.readObject());
        flags = (Flags[]) ois.readObject();
        ois.close();
        System.err.println("Done.");
      } catch (Exception e) {
        System.err.println("Error deserializing " + loadPath);
        e.printStackTrace();
      }
    }

    // test

    System.err.print("Output format: ");
    if (globalFlags.displayedColumn >= 0) {
      System.err.print("dataColumn" + (globalFlags.displayedColumn) + " ");
    }
    System.err.println("goldAnswer classifierAnswer P(classifierAnswer)");

    if (testFile != null) {
      readAndTestExamples(classifier, testFile);
    }
    //((LinearClassifier) classifier).dumpSorted();
  } // end main()


  static class Flags implements Serializable {

    private static final long serialVersionUID = -7076671761070232566L;

    boolean useNGrams = false;
    boolean usePrefixSuffixNGrams = false;
    boolean lowercaseNGrams = false;
    boolean lowercase;

    boolean useSplitNGrams = false;
    boolean useSplitPrefixSuffixNGrams = false;

    boolean cacheNGrams = false;
    int maxNGramLeng = -1;
    int minNGramLeng = 2;
    String partialNGramRegexp = null;
    Pattern partialNGramPattern = null;

    boolean useSum = false;
    double tolerance = 1e-4;
    String printFeatures = null;
    String printClassifier = null;
    int printClassifierParam = 100;

    boolean exitAfterTrainingFeaturization = false;

    boolean intern = false;

    String splitWordsRegexp = null;
    Pattern splitWordsPattern = null;
    String splitWordsTokenizerRegexp = null;
    Pattern splitWordsTokenizerPattern = null;
    String splitWordsIgnoreRegexp = null;
    Pattern splitWordsIgnorePattern = null;
    boolean useSplitWords = false;
    boolean useSplitWordPairs = false;
    boolean useSplitFirstLastWords = false;
    boolean useLowercaseSplitWords;

    int wordShape = edu.stanford.nlp.process.WordShapeClassifier.NOWORDSHAPE;
    int splitWordShape = WordShapeClassifier.NOWORDSHAPE;

    boolean useString = false;
    boolean useClassFeature = false;

    int[] binnedLengths = null;
    TwoDimensionalCounter<String,String> binnedLengthsCounter = null;
    double[] binnedValues = null;
    TwoDimensionalCounter<String,String> binnedValuesCounter = null;
    double binnedValuesNaN = -1.0;

    //true iff this feature is real valued
    boolean isRealValued = false;
    public static final String realValuedFeaturePrefix = "Value"; //prefix to add before column number for denoting real valued features
    boolean logitTransform = false;
    boolean logTransform = false;
    boolean sqrtTransform = false;

    char[] countChars = null;
    int[] countCharsBins = {0, 1};

    ClassicCounter<String> biasedHyperplane = null;

    boolean justify = false;

    boolean featureFormat = false;
    boolean significantColumnId = false;

    String  useClassifierFactory;
    String  classifierFactoryArgs;

    boolean useNB = false;
    boolean useQN = true;
    int QNsize = 15;

    int prior = LogPrior.LogPriorType.QUADRATIC.ordinal();
    double sigma = 1.0;
    double epsilon = 0.01;

    int featureMinimumSupport = 0;

    int displayedColumn = 1;  // = 2nd column of data file! (Because we count from 0.)
    int groupingColumn = -1;
    int rankingScoreColumn = -1;
    String rankingAccuracyClass = null;

    int goldAnswerColumn = 0;

    boolean biased;

    boolean useSplitWordNGrams = false;
    int maxWordNGramLeng = -1;
    int minWordNGramLeng = 1;
    boolean useBinary = false;
    double l1reg = 0.0;
    String wordNGramBoundaryRegexp;
    Pattern wordNGramBoundaryPattern;

    boolean useAdaptL1 = false;
    int limitFeatures = 0;
    String limitFeaturesLabels = null;
    double l1regmin = 0.0;
    double l1regmax = 500.0;
    double featureWeightThreshold = 0;


    // these are static because we don't want them serialized
    static String trainFile = null;
    static String testFile = null;
    static String loadClassifier = null;
    static String serializeTo = null;
    static String printTo = null;
    static boolean trainFromSVMLight = false;//train file is in SVMLight format
    static boolean testFromSVMLight = false;//test file is in SVMLight format

    @Override
    public String toString() {
      return "Flags[" +
        "goldAnswerColumn = " + goldAnswerColumn +
        ", useString = " + useString +
        ", useNGrams = " + useNGrams +
        ", usePrefixSuffixNGrams = " + usePrefixSuffixNGrams +
        "]";
    }

  } // end class Flags


} // end class ColumnDataClassifier
