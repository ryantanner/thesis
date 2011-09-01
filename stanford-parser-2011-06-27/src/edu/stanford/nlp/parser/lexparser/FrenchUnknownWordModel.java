package edu.stanford.nlp.parser.lexparser;

import edu.stanford.nlp.ling.LabeledWord;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.parser.lexparser.Options.LexOptions;
import edu.stanford.nlp.stats.ClassicCounter;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.util.Numberer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

public class FrenchUnknownWordModel extends BaseUnknownWordModel {

  private static final long serialVersionUID = -776564693549194424L;

  protected boolean smartMutation = false;

  /**
   * We cache the last signature looked up, because it asks for the same one
   * many times when an unknown word is encountered! (Note that under the
   * current scheme, one unknown word, if seen sentence-initially and
   * non-initially, will be parsed with two different signatures....)
   */
  protected transient int lastSignatureIndex = -1;

  protected transient int lastSentencePosition = -1;

  protected transient int lastWordToSignaturize = -1;

  protected int unknownSuffixSize = 0;
  protected int unknownPrefixSize = 0;

  private static final String BOUNDARY_TAG = ".$$."; // boundary tag -- assumed not a real tag

  public FrenchUnknownWordModel(LexOptions op, Lexicon lex) {
    super(op, lex);
    unknownLevel = op.useUnknownWordSignatures;
    this.smartMutation = op.smartMutation;
    this.unknownSuffixSize = op.unknownSuffixSize;
    this.unknownPrefixSize = op.unknownPrefixSize;
  }

  /**
   * Trains this lexicon on the Collection of trees.
   */
  @Override
  public void train(Collection<Tree> trees) {
    train(trees, 1.0, false);
  }


  private void train(Collection<Tree> trees, double weight, boolean keepTagsAsLabels) {
    ClassicCounter<IntTaggedWord> seenCounter = new ClassicCounter<IntTaggedWord>();

    int tNum = 0;
    int tSize = trees.size();
    int indexToStartUnkCounting = (int) (tSize * Train.fractionBeforeUnseenCounting);

    for (Tree tree : trees) {
      tNum++;
      List<IntTaggedWord> taggedWords = treeToEvents(tree, keepTagsAsLabels);
      for (int w = 0, sz = taggedWords.size(); w < sz; w++) {
        IntTaggedWord iTW = taggedWords.get(w);
        IntTaggedWord iT = new IntTaggedWord(nullWord, iTW.tag);
        IntTaggedWord iW = new IntTaggedWord(iTW.word, nullTag);
        seenCounter.incrementCount(iW, weight);
        IntTaggedWord i = new IntTaggedWord(nullWord, nullTag);

        if (tNum > indexToStartUnkCounting) {
          // start doing this once some way through trees; tNum is 1 based counting
          if (seenCounter.getCount(iW) < 2) {
            // it's an entirely unknown word
            int s = getSignatureIndex(iTW.word, w);
            IntTaggedWord iTS = new IntTaggedWord(s, iTW.tag);
            IntTaggedWord iS = new IntTaggedWord(s, nullTag);
            unSeenCounter.incrementCount(iTS, weight);
            unSeenCounter.incrementCount(iT, weight);
            unSeenCounter.incrementCount(iS, weight);
            unSeenCounter.incrementCount(i, weight);
          }
        }
      }
    }
    // make sure the unseen counter isn't empty!  If it is, put in
    // a uniform unseen over tags
    if (unSeenCounter.isEmpty()) {
      System.err.printf("%s: WARNING: Unseen word counter is empty!%n",this.getClass().getName());
      int numTags = tagNumberer().total();
      for (int tt = 0; tt < numTags; tt++) {
        if ( ! BOUNDARY_TAG.equals(tagNumberer().object(tt))) {
          IntTaggedWord iT = new IntTaggedWord(nullWord, tt);
          IntTaggedWord i = new IntTaggedWord(nullWord, nullTag);
          unSeenCounter.incrementCount(iT, weight);
          unSeenCounter.incrementCount(i, weight);
        }
      }
    }
  }

  protected List<IntTaggedWord> treeToEvents(Tree tree, boolean keepTagsAsLabels) {
    if (!keepTagsAsLabels) { return treeToEvents(tree); }
    List<LabeledWord> labeledWords = tree.labeledYield();
    return listOfLabeledWordsToEvents(labeledWords);
  }

  protected List<IntTaggedWord> treeToEvents(Tree tree) {
    List<TaggedWord> taggedWords = tree.taggedYield();
    return listToEvents(taggedWords);
  }

  protected List<IntTaggedWord> listToEvents(List<TaggedWord> taggedWords) {
    List<IntTaggedWord> itwList = new ArrayList<IntTaggedWord>();
    for (TaggedWord tw : taggedWords) {
      IntTaggedWord iTW = new IntTaggedWord(wordNumberer().number(tw.word()),
          tagNumberer().number(tw.tag()));
      itwList.add(iTW);
    }
    return itwList;
  }

  protected List<IntTaggedWord> listOfLabeledWordsToEvents(List<LabeledWord> taggedWords) {
    List<IntTaggedWord> itwList = new ArrayList<IntTaggedWord>();
    for (LabeledWord tw : taggedWords) {
      IntTaggedWord iTW = new IntTaggedWord(wordNumberer().number(tw.word()),
          tagNumberer().number(tw.tag()));
      itwList.add(iTW);
    }
    return itwList;
  }

  @Override
  public float score(IntTaggedWord iTW, int loc, double c_Tseen, double total, double smooth) {
    int word = iTW.word;
    short tag = iTW.tag;
    double pb_W_T; // always set below

    //  unknown word model for P(T|S)

    iTW.word = getSignatureIndex(iTW.word, loc);
    double c_TS = unSeenCounter.getCount(iTW);
    iTW.tag = nullTag;
    double c_S = unSeenCounter.getCount(iTW);
    iTW.word = nullWord;
    double c_U = unSeenCounter.getCount(iTW);
    iTW.tag = tag;
    double c_T = unSeenCounter.getCount(iTW);
    iTW.word = word;

    double p_T_U = c_T / c_U;

    if (unknownLevel == 0) {
      c_TS = 0;
      c_S = 0;
    }
    double pb_T_S = (c_TS + smooth * p_T_U) / (c_S + smooth);

    double p_T = (c_Tseen / total);
    double p_W = 1.0 / total;
    pb_W_T = Math.log(pb_T_S * p_W / p_T);

    return (float) pb_W_T;
  }


  private transient Numberer tagNumberer;

  private Numberer tagNumberer() {
    if (tagNumberer == null) {
      tagNumberer = Numberer.getGlobalNumberer("tags");
    }
    return tagNumberer;
  }

  private transient Numberer wordNumberer;

  private Numberer wordNumberer() {
    if (wordNumberer == null) {
      wordNumberer = Numberer.getGlobalNumberer("words");
    }
    return wordNumberer;
  }

  /**
   * Returns the index of the signature of the word numbered wordIndex, where
   * the signature is the String representation of unknown word features.
   * Caches the last signature index returned.
   */
  @Override
  public int getSignatureIndex(int wordIndex, int sentencePosition) {
    if (wordIndex == lastWordToSignaturize && sentencePosition == lastSentencePosition) {
      return lastSignatureIndex;

    } else {
      String uwSig = getSignature((String) wordNumberer().object(wordIndex), sentencePosition);
      int sig = wordNumberer().number(uwSig);
      lastSignatureIndex = sig;
      lastSentencePosition = sentencePosition;
      lastWordToSignaturize = wordIndex;
      return sig;
    }
  }

  /**
   * TODO Can add various signatures, setting the signature via Options.
   *
   * @param word The word to make a signature for
   * @param loc Its position in the sentence (mainly so sentence-initial
   *          capitalized words can be treated differently)
   * @return A String that is its signature (equivalence class)
   */
  @Override
  public String getSignature(String word, int loc) {
    final String BASE_LABEL = "UNK";
    StringBuilder sb = new StringBuilder(BASE_LABEL);
    switch (unknownLevel) {
      case 1: //Marie's initial attempt
        sb.append(FrenchUnknownWordSignatures.nounSuffix(word));
        if(sb.toString().equals(BASE_LABEL)) {
          sb.append(FrenchUnknownWordSignatures.adjSuffix(word));
          if(sb.toString().equals(BASE_LABEL)) {
            sb.append(FrenchUnknownWordSignatures.verbSuffix(word));
            if(sb.toString().equals(BASE_LABEL)) {
              sb.append(FrenchUnknownWordSignatures.advSuffix(word));
            }  
          }
        }
 
        sb.append(FrenchUnknownWordSignatures.possiblePlural(word));
        
        String hasDigit = FrenchUnknownWordSignatures.hasDigit(word);
        String isDigit = FrenchUnknownWordSignatures.isDigit(word);
        
        if( ! hasDigit.equals("")) {
          if(isDigit.equals("")) {
            sb.append(hasDigit);
          } else {
            sb.append(isDigit);
          }
        }
                
//        if(FrenchUnknownWordSignatures.isPunc(word).equals(""))
          sb.append(FrenchUnknownWordSignatures.hasPunc(word));
//        else  
//          sb.append(FrenchUnknownWordSignatures.isPunc(word));
        
        sb.append(FrenchUnknownWordSignatures.isAllCaps(word));

        if(loc > 0) {
          if(FrenchUnknownWordSignatures.isAllCaps(word).equals(""))
            sb.append(FrenchUnknownWordSignatures.isCapitalized(word));
        }
        
        //Backoff to suffix if we haven't matched anything else
        if(unknownSuffixSize > 0 && sb.toString().equals(BASE_LABEL)) {
          int min = word.length() < unknownSuffixSize ? word.length(): unknownSuffixSize;
          sb.append('-').append(word.substring(word.length() - min));
        }
        
        break;
      
      case 2: //successive matching
        if( ! FrenchUnknownWordSignatures.advSuffix(word).equals(""))
          sb.append(FrenchUnknownWordSignatures.advSuffix(word));
        else if( ! FrenchUnknownWordSignatures.verbSuffix(word).equals(""))
          sb.append(FrenchUnknownWordSignatures.verbSuffix(word));
        else if( ! FrenchUnknownWordSignatures.nounSuffix(word).equals(""))
          sb.append(FrenchUnknownWordSignatures.nounSuffix(word));
        
        sb.append(FrenchUnknownWordSignatures.adjSuffix(word));
        sb.append(FrenchUnknownWordSignatures.hasDigit(word));
        sb.append(FrenchUnknownWordSignatures.possiblePlural(word));
        
        if(FrenchUnknownWordSignatures.isPunc(word).equals(""))
          sb.append(FrenchUnknownWordSignatures.isPunc(word));
        else
          sb.append(FrenchUnknownWordSignatures.hasPunc(word));
        
        if(loc > 0)
          sb.append(FrenchUnknownWordSignatures.isCapitalized(word));
        
      default:
        System.err.printf("%s: Invalid unknown word signature! (%d)%n", this.getClass().getName(),unknownLevel);
    }

    return sb.toString();
  }


  private static class FrenchUnknownWordSignatures {
    private static final Pattern pNounSuffix = Pattern.compile("(ier|ière|ité|ion|ison|isme|ysme|iste|esse|eur|euse|ence|eau|erie|ng|ette|age|ade|ance|ude|ogue|aphe|ate|duc|anthe|archie|coque|érèse|ergie|ogie|lithe|mètre|métrie|odie|pathie|phie|phone|phore|onyme|thèque|scope|some|pole|ôme|chromie|pie)s?$");
    private static final Pattern pAdjSuffix = Pattern.compile("(iste|ième|uple|issime|aire|esque|atoire|ale|al|able|ible|atif|ique|if|ive|eux|aise|ent|ois|oise|ante|el|elle|ente|oire|ain|aine)s?$");
    private static final Pattern pHasDigit = Pattern.compile("\\d+");
    private static final Pattern pIsDigit = Pattern.compile("^\\d+$");
    private static final Pattern pPosPlural = Pattern.compile("(s|ux)$");
    private static final Pattern pVerbSuffix = Pattern.compile("(ir|er|re|ez|ont|ent|ant|ais|ait|ra|era|eras|é|és|ées|isse|it)$");
    private static final Pattern pAdvSuffix = Pattern.compile("(iment|ement|emment|amment)$");
    private static final Pattern pHasPunc = Pattern.compile("([\u0021-\u002F\u003A-\u0040\\u005B\u005C\\u005D\u005E-\u0060\u007B-\u007E\u00A1-\u00BF\u2010-\u2027\u2030-\u205E\u20A0-\u20B5])+");
    private static final Pattern pIsPunc = Pattern.compile("([\u0021-\u002F\u003A-\u0040\\u005B\u005C\\u005D\u005E-\u0060\u007B-\u007E\u00A1-\u00BF\u2010-\u2027\u2030-\u205E\u20A0-\u20B5])+$");
    private static final Pattern pAllCaps = Pattern.compile("^[A-Z\\u00C0-\\u00DD]+$");
    
    public static String nounSuffix(String s) {
      return pNounSuffix.matcher(s).find() ? "-noun" : "";
    }
    
    public static String adjSuffix(String s) {
      return pAdjSuffix.matcher(s).find() ? "-adj" : "";
    }
    
    public static String hasDigit(String s) {
      return pHasDigit.matcher(s).find() ? "-num" : "";
    }
    
    public static String isDigit(String s) {
      return pIsDigit.matcher(s).find() ? "-isNum" : "";
    }
    
    public static String verbSuffix(String s) {
      return pVerbSuffix.matcher(s).find() ? "-verb" : "";
    }
    
    public static String possiblePlural(String s) {
      return pPosPlural.matcher(s).find() ? "-plural" : "";
    }

    public static String advSuffix(String s) {
      return pAdvSuffix.matcher(s).find() ? "-adv" : "";
    }
    
    public static String hasPunc(String s) {
      return pHasPunc.matcher(s).find() ? "-hpunc" : "";
    }

    public static String isPunc(String s) {
      return pIsPunc.matcher(s).matches() ? "-ipunc" : "";
    }
    
    public static String isAllCaps(String s) {
      return pAllCaps.matcher(s).matches() ? "-allcap" : "";
    }
    
    public static String isCapitalized(String s) {
      if(s.length() > 0) {
        Character ch = s.charAt(0);
        return Character.isUpperCase(ch) ? "-upper" : "";
      }
      return "";
    }
  }
}
