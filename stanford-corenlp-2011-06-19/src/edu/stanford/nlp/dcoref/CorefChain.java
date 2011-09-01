//
// StanfordCoreNLP -- a suite of NLP tools
// Copyright (c) 2009-2010 The Board of Trustees of
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
// For more information, bug reports, fixes, contact:
//    Christopher Manning
//    Dept of Computer Science, Gates 1A
//    Stanford CA 94305-9010
//    USA
//

package edu.stanford.nlp.dcoref;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import edu.stanford.nlp.dcoref.Dictionaries.Animacy;
import edu.stanford.nlp.dcoref.Dictionaries.Gender;
import edu.stanford.nlp.dcoref.Dictionaries.MentionType;
import edu.stanford.nlp.dcoref.Dictionaries.Number;
import edu.stanford.nlp.ling.CorefCoreAnnotations.CorefClusterIdAnnotation;
import edu.stanford.nlp.util.IntPair;
import edu.stanford.nlp.util.IntTuple;

/**
 * Output of coref system
 *
 * @author Heeyoung Lee
 */
public class CorefChain {

  private int chainID;
  private List<CorefMention> mentions;
  private HashMap<IntPair, CorefMention> mentionMap;

  /** The most representative mention in this cluster */
  private CorefMention representative = null;

  /** get List of CorefMentions */
  public List<CorefMention> getCorefMentions() { return mentions; }
  
  /** get CorefMention by position (sentence number, headIndex) */
  public CorefMention getMention(IntPair position) { return mentionMap.get(position); }

  /** Return the most representative mention in the chain. 
   *  Proper mention and a mention with more pre-modifiers are preferred.  
   */
  public CorefMention getRepresentativeMention() { return representative; }
  public int getChainID() { return chainID; }

  /** Mention for coref output */
  public static class CorefMention{
    public MentionType mentionType;
    public Number number;
    public Gender gender;
    public Animacy animacy;

    public int startIndex;
    public int endIndex;
    public int headIndex;
    public int corefClusterID;
    public int mentionID;
    public int sentNum;
    public IntTuple position;
    
    public CorefMention(Mention m){
      mentionType = m.mentionType;
      number = m.number;
      gender = m.gender;
      animacy = m.animacy;
      startIndex = m.startIndex + 1;
      endIndex = m.endIndex + 1;
      headIndex = m.headIndex + 1;
      corefClusterID = m.corefClusterID;
      sentNum = m.sentNum + 1;
      mentionID = m.mentionID;
      
      m.headWord.set(CorefClusterIdAnnotation.class, corefClusterID);
    }
    public String toString(){
      return position.toString();
    }
    private boolean moreRepresentativeThan(CorefMention m){
      if(m==null) return true;
      if(mentionType!=m.mentionType) {
        if((mentionType==MentionType.PROPER && m.mentionType!=MentionType.PROPER)
            || (mentionType==MentionType.NOMINAL && m.mentionType==MentionType.PRONOMINAL)) return true;
        else return false;
      } else {
        if(headIndex-startIndex > m.headIndex - m.startIndex) return true;
        else if (sentNum < m.sentNum || (sentNum==m.sentNum && headIndex < m.headIndex)) return true;
        else return false;
      }
    }
  }
  protected static class MentionComparator implements Comparator<CorefMention> {
    public int compare(CorefMention m1, CorefMention m2) {
      if(m1.sentNum < m2.sentNum) return -1;
      else if(m1.sentNum > m2.sentNum) return 1;
      else{
        if(m1.startIndex < m2.startIndex) return -1;
        else if(m1.startIndex > m2.startIndex) return 1;
        else {
          if(m1.endIndex > m2.endIndex) return -1;
          else if(m1.endIndex < m2.endIndex) return 1;
          else return 0;
        }
      }
    }
  }
  public CorefChain(CorefCluster c, HashMap<Mention, IntTuple> positions){
    chainID = c.clusterID;
    mentions = new ArrayList<CorefMention>();
    mentionMap = new HashMap<IntPair, CorefMention>();
    for(Mention m : c.getCorefMentions()) {
      CorefMention men = new CorefMention(m);
      IntTuple pos = positions.get(m);
      men.position = new IntTuple(2);
      // index starts from 1
      men.position.set(0, pos.get(0)+1);
      men.position.set(1, pos.get(1)+1);
      mentions.add(men);
      mentionMap.put(new IntPair(men.sentNum, men.headIndex), men);
      if(men.moreRepresentativeThan(representative)) representative = men;
    }
    Collections.sort(mentions, new MentionComparator());
  }
  public String toString(){
    return mentions.toString(); 
  }
}
