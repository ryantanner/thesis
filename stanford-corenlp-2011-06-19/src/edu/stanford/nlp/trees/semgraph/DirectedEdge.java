package edu.stanford.nlp.trees.semgraph;

import org.jgrapht.EdgeFactory;
import org.jgrapht.graph.DefaultEdge;

public class DirectedEdge<V> extends DefaultEdge {
  
  /**
   * 
   */
  private static final long serialVersionUID = -2602872409031697690L;
  private V source;
  private V target;
  
  @Override
  public String toString() {
    return source + " :: " + target;
  }

  public DirectedEdge(V source, V target) {
    super();
    this.source = source;
    this.target = target;
  }

  public V getSource() {
    return source;
  }

  public V getTarget() {
    return target;
  }
  
  public static class Factory<V> implements EdgeFactory<V,DirectedEdge<V>> {

    public DirectedEdge<V> createEdge(V arg0, V arg1) {
      return new DirectedEdge<V>(arg0,arg1);
    }
    
  }
  
  public V oppositeVertex(V v) {
    if (v == target) {
      return source;
    } else if (v == source) {
      return target;
    } else {
      return null;
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public boolean equals(Object othat) {
    if(othat == this) return true;
    else if (othat instanceof DirectedEdge) {
      DirectedEdge<V> that = (DirectedEdge<V>)othat;
      return (this.source == null || that.source.equals(source))
          && (this.target == null || that.target.equals(target));
    } else return false;
  }
}
