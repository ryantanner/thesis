package edu.stanford.nlp.trees.semgraph;

import java.io.Serializable;
import java.util.Comparator;

import org.jgrapht.EdgeFactory;

import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.trees.GrammaticalRelation;


/**
 * Represents an edge in the dependency graph. Equal only if source, target, and relation are equal.
 *
 * @author Christopher Cox
 * @author Teg Grenager
 * @see SemanticGraph
 */
public class SemanticGraphEdge extends DirectedEdge<IndexedWord> implements Comparable<SemanticGraphEdge> {

  public static boolean printOnlyRelation = false; // a hack for displaying SemanticGraph in JGraph.  Should be redone better.

  private GrammaticalRelation relation;
  private double weight;

  // These references to the governor (source) and dependent (target) may appear redundant,
  // but are necessary, as modifying either of the nodes (e.g. calling setIndex on them)
  // will destroy the indices set up by JGrapht, making getSource and getTarget throw an
  // exception.  The cache references are used to "re-index" a modified graph.
  private IndexedWord cachedSourceRef;
  private IndexedWord cachedTargetRef;

  public SemanticGraphEdge(IndexedWord source,
                           IndexedWord target,
                           GrammaticalRelation relation,
                           double weight) {
    super(source, target);
    cachedSourceRef = source;
    cachedTargetRef = target;
    this.relation = relation;
    setWeight(weight);
  }

  public SemanticGraphEdge(SemanticGraphEdge e) {
    super(e.getSource(), e.getTarget());
    this.relation = e.getRelation();
    this.weight = e.weight;
  }

  @Override
  public String toString() {
    if (printOnlyRelation) {
      return getSource() + " -> " + getTarget() + " (" + getRelation() + ")";
    } else {
      return getRelation().toString();
    }
  }

  public GrammaticalRelation getRelation() {
    return relation;
  }

  public void setRelation(GrammaticalRelation relation) {
    this.relation = relation;
  }

  public IndexedWord getGovernor() {
    return getSource();
  }

  public IndexedWord getDependent() {
    return getTarget();
  }

  public void setWeight(double weight) {
    this.weight = weight;
  }

  public double getWeight() {
    return weight;
  }

  /**
   * @return true if the edges are of the same relation type
   */
  public boolean typeEquals(SemanticGraphEdge e) {
    return (this.relation.equals(e.relation));
  }

  private static class SemanticGraphEdgeTargetComparator implements Comparator<SemanticGraphEdge> {

    public int compare(SemanticGraphEdge o1, SemanticGraphEdge o2) {
      int targetVal = o1.getTarget().compareTo(o2.getTarget());
      if (targetVal != 0) {
        return targetVal;
      }
      int sourceVal = o1.getSource().compareTo(o2.getSource());
      if (sourceVal != 0) {
        return sourceVal;
      }
      return o1.getRelation().toString().compareTo(o2.getRelation().toString()); // todo: cdm: surely we shouldn't have to do toString() now?
    }

  }

  private static Comparator<SemanticGraphEdge> targetComparator = new SemanticGraphEdgeTargetComparator();

  public static Comparator<SemanticGraphEdge> orderByTargetComparator() {
    return targetComparator;
  }

  /** Compares SemanticGraphEdges.
   * Warning: compares on the sources, targets, and then the STRINGS of the relations.
   * @param other Edge to compare to
   * @return Whether this is smaller, same, or larger
   */
  public int compareTo(SemanticGraphEdge other) {
    int sourceVal = getSource().compareTo(other.getSource());
    if (sourceVal != 0) {
      return sourceVal;
    }
    int targetVal = getTarget().compareTo(other.getTarget());
    if (targetVal !=0 ) {
      return targetVal;
    }
    String thisRelation = getRelation().toString();
    String thatRelation = other.getRelation().toString();
    return thisRelation.compareTo(thatRelation);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof SemanticGraphEdge)) return false;

    final SemanticGraphEdge semanticGraphEdge = (SemanticGraphEdge) o;

    if (relation != null) {
      boolean retFlag = relation.equals(semanticGraphEdge.relation);
      boolean govMatch = getGovernor().equals(semanticGraphEdge.getGovernor());
      boolean depMatch = getDependent().equals(semanticGraphEdge.getDependent());
      boolean matched = retFlag && govMatch && depMatch;
      return matched;
    }

 //   if (relation != null ? !relation.equals(semanticGraphEdge.relation) : semanticGraphEdge.relation != null) return false;
    return super.equals(o);
  }

  @Override
  public int hashCode() {
    int result;
    result = (relation != null ? relation.hashCode() : 0);
    result = 29 * result + (getSource() != null ? getSource().hashCode() : 0);
    result = 29 * result + (getTarget() != null ? getTarget().hashCode() : 0);
    return result;
  }

  public static class Factory implements EdgeFactory<IndexedWord,SemanticGraphEdge>, Serializable {

    private static final long serialVersionUID = -8778385005883694770L;

    public SemanticGraphEdge createEdge(IndexedWord arg0, IndexedWord arg1) {
      return new SemanticGraphEdge(arg0,arg1,null,0.0);
    }

  }

  public IndexedWord getCachedSourceRef() {
    return cachedSourceRef;
  }

  public IndexedWord getCachedTargetRef() {
    return cachedTargetRef;
  }

  private static final long serialVersionUID = 1L;

}
