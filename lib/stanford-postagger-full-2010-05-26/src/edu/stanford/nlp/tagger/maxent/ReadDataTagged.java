/**
 * Title:        StanfordMaxEnt<p>
 * Description:  A Maximum Entropy Toolkit<p>
 * Copyright:    Copyright (c) Kristina Toutanova<p>
 * Company:      Stanford University<p>
 */
package edu.stanford.nlp.tagger.maxent;

import edu.stanford.nlp.io.NumberRangesFileFilter;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.ling.WordTag;
import edu.stanford.nlp.trees.*;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.StringTokenizer;


/**
 * Reads tagged data from a file and creates a dictionary.
 * The tagged data has to be whitespace-separated items, with the word and
 * tag split off by a delimiter character, which is found as the last instance
 * of the delimiter character in the item.
 *
 * @author Kristina Toutanova
 * @version 1.0
 */
public class ReadDataTagged {

  private final String filename;
  private ArrayList<DataWordTag> v = new ArrayList<DataWordTag>();
  private int numElements = 0;
  private final PairsHolder pairs;
  private final MaxentTagger maxentTagger;

  private static final String eosWord = "EOS";
  private static final String eosTag = "EOS";
  //TODO: make a class DataHolder that holds the dict, tags, pairs, etc, for tagger
  // and pass it around

  protected ReadDataTagged(TaggerConfig config, MaxentTagger maxentTagger, PairsHolder pairs) {
    this.maxentTagger = maxentTagger;
    this.pairs = pairs;
    this.filename = config.getFile();
    try {
      if (config.getInitFromTrees()) {
        initFromTrees(config);
      } else {
        init(config.getTagSeparator(), config.getEncoding());
      }
    } catch (Exception e) {
      System.err.println("Error reading data from " + filename);
      e.printStackTrace();
    }
  }


  /** Frees the memory that is stored in this object by dropping the word-tag data.
   */
  void release() {
    v = null;
  }


  DataWordTag get(int index) {
    return v.get(index);
  }

  private void initFromTrees(TaggerConfig config) {
    System.err.println("Training a tagger from treebank" + filename);
    ArrayList<String> words = new ArrayList<String>();
    ArrayList<String> tags = new ArrayList<String>();
    int numSentences = 0;
    int numWords = 0;

    int maxLen = Integer.MIN_VALUE;
    int minLen = Integer.MAX_VALUE;
    TreeReaderFactory trf = new LabeledScoredTreeReaderFactory();
    TreeTransformer transformer = config.getTreeTransformer();
    TreeNormalizer normalizer = config.getTreeNormalizer();
    DiskTreebank treebank = new DiskTreebank(trf, config.getEncoding());
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
      ArrayList<TaggedWord> yield = t.taggedYield();
      for(TaggedWord tw : yield) {
        if(tw != null) {
          words.add(tw.word());
          tags.add(tw.tag());
          if (!maxentTagger.tagTokens.containsKey(tw.tag())) {
            maxentTagger.tagTokens.put(tw.tag(), new HashSet<String>());
          }
          maxentTagger.tagTokens.get(tw.tag()).add(tw.word());
        }
      }
      maxLen = (yield.size() > maxLen ? yield.size() : maxLen);
      minLen = (yield.size() < minLen ? yield.size() : minLen);
      words.add(eosWord);
      tags.add(eosTag);
      numElements = numElements + yield.size() + 1;
      // iterate over the words in the sentence
      for (int i = 0; i < yield.size() + 1; i++) {
        History h = new History(numWords+numSentences, numWords+numSentences + yield.size(), numWords+numSentences + i, pairs, maxentTagger.extractors);
        String tag = tags.get(i);
        String word = words.get(i);
        pairs.add(new WordTag(word,tag));
        int y = maxentTagger.tags.add(tag);
        DataWordTag dat = new DataWordTag(h, y, maxentTagger.tags);
        v.add(dat);
        maxentTagger.dict.add(word, tag);

      }
      numSentences++;
      numWords += yield.size();
      words.clear();
      tags.clear();
      if ((numSentences % 100000) == 0) System.err.println("Read " + numSentences + " sentences, min " + minLen + " words, max " + maxLen + " words ... [still reading]");

    }

    System.err.println("Read " + numWords + " words from " + filename + " [done].");
    System.err.println("Read " + numSentences + " sentences, min " + minLen + " words, max " + maxLen + " words.");
  }


  // Read the data.
  private void init(String tagSeparator, String encoding) throws IOException {
    ArrayList<String> words = new ArrayList<String>();
    ArrayList<String> tags = new ArrayList<String>();
    int numSentences = 0;
    int numWords = 0;
    int endPos = 0;
    int prevPos = 0;
    BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(filename), encoding));

    int maxLen = Integer.MIN_VALUE;
    int minLen = Integer.MAX_VALUE;

    //loop over sentences
    for  (String s; (s = in.readLine()) != null; ) {
      StringTokenizer st = new StringTokenizer(s);
      //loop over words in a single sentence

      while (st.hasMoreTokens()) {
        String token = st.nextToken();
        numWords++;
        int indexUnd = token.lastIndexOf(tagSeparator);
        if (indexUnd < 0) {
          throw new RuntimeException("Data format error: can't find delimiter \"" + tagSeparator + "\" in word \"" + token + "\" (line " + numSentences + " of " + filename + ')');
        }
        String word = token.substring(0, indexUnd).intern();
        String tag = token.substring(indexUnd + 1).intern();
        words.add(word);
        tags.add(tag);
        if(!maxentTagger.tagTokens.containsKey(tag)) {
          maxentTagger.tagTokens.put(tag, new HashSet<String>());
        }
        maxentTagger.tagTokens.get(tag).add(word);
        endPos++;
      }

      if (endPos > maxLen) maxLen = endPos;
      if (endPos < minLen) minLen = endPos;

      // add the EOS as well
      words.add(eosWord);
      tags.add(eosTag);
      numElements = numElements + endPos + 1;

      // iterate over the words in the sentence
      for (int i = 0; i < endPos + 1; i++) {
        History h = new History(prevPos, prevPos + endPos, prevPos + i, pairs, maxentTagger.extractors);
        String tag = tags.get(i);
        String word = words.get(i);
        pairs.add(new WordTag(word,tag));
        int y = maxentTagger.tags.add(tag);
        DataWordTag dat = new DataWordTag(h, y, maxentTagger.tags);
        v.add(dat);
        maxentTagger.dict.add(word, tag);
      }

      numSentences++;
      prevPos += endPos + 1;
      endPos = 0;
      words.clear();
      tags.clear();
      if ((numSentences % 100000) == 0) System.err.println("Read " + numSentences + " sentences, min " + minLen + " words, max " + maxLen + " words ... [still reading]");
    }

    in.close();
    System.err.println("Read " + numWords + " words from " + filename + " [done].");
    System.err.println("Read " + numSentences + " sentences, min " + minLen + " words, max " + maxLen + " words.");
  }

  /*
  public void addPartTakingVerb(History h, String tag) {
    if (!(tag.equals(rpTag) || tag.equals(rbTag) || tag.equals(inTag))) {
      return;
    }
    String cWord = ExtractorFrames.cWord.extract(h);
    if (maxentTagger.dict.getCount(cWord, rpTag) == 0) {
      return;
    }
    String verb = ExtractorParticles.extractLV(h, 1);
    if (!verb.startsWith("NA"))
    // add it
    {
      maxentTagger.dict.addVPTaking(verb, tag, cWord);
    }
  }


  public void addPRRBINTakingVerb(History h, String tag) {
    if (!(tag.equals(rpTag) || tag.equals(rbTag) || tag.equals(inTag))) {
      return;
    }
    String word = ExtractorFrames.cWord.extract(h);
    String verb = ((ExtractorLastVerb) (ExtractorFrames.lastVerb)).extractLV(h, 6);
    // add it
    maxentTagger.dict.add(word + "|" + verb, tag); //correct later
  }

  public void addThatTakingVerb(History h, String word, String tag) {
    if (!(word.equals(thatWord))) {
      return;
    }
    if (!(tag.equals(inTag))) {
      return;
    }
    String s = ExtractorFrames.lastVerbThat.extract(h, 0);
    String verb = ExtractorFrames.lastVerbThat.extractLV(h);
    if (!s.equals("0"))
    // add it
    {
      maxentTagger.dict.addVThatTaking(verb);
    }
  }
  */

  /** Returns the number of tokens in the data read, which is the number of words
   *  plus one end sentence token per sentence.
   *  @return The number of tokens in the data
   */
  public int getSize() {
    return numElements;
  }

  /*
  public static void main(String[] args) {
  ReadDataTagged rDT1=new ReadDataTagged("testhuge.txt");
  Dictionary dOld=maxentTagger.dict;
  maxentTagger.dict=new Dictionary();
  ReadDataTagged rDT2=new ReadDataTagged("trainhuge.txt");
  // how many ambiguous praticles are there in testhuge, how many amb. INs, how many amb. RBs
  // how many of the amb. praticles in trainhuge have appeared as a particle with the same verb before
  // how many of the amb. particles have appeared with the same verb before as INs or RBs
  // how many of the RPs have appeared with the same verb before
  // read in sequentially the testhuge dictionary
  int numARP=0;
  int numAIN=0;
  int numARB=0;
  int[] napp=new int[3];
  int[][] appbefore=new int[3][3]; // RP RB IN
  int[][] nappbefore=new int[3][3];
  Object[] arr=dOld.dict.keySet().toArray();

   for(int i=0;i<arr.length;i++)
  {
    String word=(String)arr[i];
    if(word.indexOf("|")==-1) continue;
    String wordA=word.substring(0,word.indexOf("|"));
    if(maxentTagger.dict.sum(wordA)==0)
    {
     System.out.println(" unknown "+wordA);
     continue;
    }
    if(maxentTagger.dict.getTags(wordA).length==1) continue; // unambiguous
    numARP+=dOld.getCount(word,rpTag);
    numARB+=dOld.getCount(word,rbTag);
    numAIN+=dOld.getCount(word,inTag);
    int numRP=dOld.getCount(word,rpTag);
    int numRB=dOld.getCount(word,rbTag);
    int numIN=dOld.getCount(word,inTag);
    TagCount tC=maxentTagger.dict.get(word);
    if(tC==null){
    napp[0]+=numRP;
    napp[1]+=numRB;
    napp[2]+=numIN;
    continue;
    }
    else{// the word was seen before with the same verb
     int numRPM=maxentTagger.dict.getCount(word,rpTag);
     int numRBM=maxentTagger.dict.getCount(word,rbTag);
     int numINM=maxentTagger.dict.getCount(word,inTag);
     if(numRPM>0){
      appbefore[0][0]+=numRP;
      appbefore[1][0]+=numRB;
      appbefore[2][0]+=numIN;
      }
     else{
      nappbefore[0][0]+=numRP;
      nappbefore[1][0]+=numRB;
      nappbefore[2][0]+=numIN;
        }
      if(numRBM>0){
      appbefore[0][1]+=numRP;
      appbefore[1][1]+=numRB;
      appbefore[2][1]+=numIN;
      }
     else{
      nappbefore[0][1]+=numRP;
      nappbefore[1][1]+=numRB;
      nappbefore[2][1]+=numIN;
        }

      if(numINM>0){
      appbefore[0][2]+=numRP;
      appbefore[1][2]+=numRB;
      appbefore[2][2]+=numIN;
      }
     else{
      nappbefore[0][2]+=numRP;
      nappbefore[1][2]+=numRB;
      nappbefore[2][2]+=numIN;
        }

    } // else the word was seen before

   }// for

   System.out.println(numARP+" "+numARB+" "+numAIN);
   System.out.println(" not appeared at all before "+ napp[0]+" "+napp[1]+" "+napp[2]);
   for(int i=0;i<3;i++)
    for(int j=0;j<3;j++){
   System.out.println(" napp as this before "+i+" "+j+" "+nappbefore[i][j]);
   }
   for(int i=0;i<3;i++)
    for(int j=0;j<3;j++){
   System.out.println(" napp as this before "+i+" "+j+" "+nappbefore[i][j]);
   System.out.println(" appeared as this before " +i+" "+j+" "+appbefore[i][j]);
   }

    //saveTreebankToFile(args[0],(Integer.parseInt(args[1]), Integer.parseInt(args[2]));

    //System.out.println(" ambg "+countAmbiguous+" unambg "+countUnAmbiguous+" disamb "+countAmbDisamb);

  //}
  */

  /**
   * Save the treebank sections to a file in format [word_tag ]+
   * per sentence, one sentence per line.
   */
  /*
  public static void saveTreebankToFile(String filename, int start,int end) {
    String treebankPath="/dfs/ah/1/tmp/klein/corpora/Treebank3/parsed/mrg/wsj";

    Treebank trainTreebank = new MemoryTreebank(new TreeReaderFactory() {
        public TreeReader newTreeReader(Reader in) {
          return new PennTreeReader(in,
                        new LabeledScoredTreeFactory(
                               new StringLabelFactory()),
                               new BobChrisTreeNormalizer());
        }
      });
    FileFilter filter=new NumberRangeFileFilter(start,end,true);
    trainTreebank.loadPath(treebankPath, filter);
    try{
      PrintWriter bw=new PrintWriter(new BufferedWriter(new FileWriter(filename)));

      for (Tree nextTree : trainTreebank) {
        Sentence s=nextTree.taggedYield();

        //System.out.println(s.toString());

        for (int i=0;i<s.size();i++) {
          TaggedWord w=(TaggedWord)s.getWord(i);
          String st=w.value();
          //System.out.println("value is "+st+" tag is "+w.tag());
          bw.print(st+'_'+w.tag()+' ');
        }
        bw.println();
      }

      bw.close();

    } catch(Exception e){
      e.printStackTrace();
    }
  }
  */
}
