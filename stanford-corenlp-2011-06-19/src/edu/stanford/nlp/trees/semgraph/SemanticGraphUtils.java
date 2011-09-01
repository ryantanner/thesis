package edu.stanford.nlp.trees.semgraph;

import edu.stanford.nlp.ling.CoreAnnotations.CurrentAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.ling.LabeledWord;
import edu.stanford.nlp.process.Morphology;
import edu.stanford.nlp.trees.EnglishGrammaticalRelations;
import edu.stanford.nlp.trees.GrammaticalRelation;
import edu.stanford.nlp.trees.GrammaticalStructure;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.util.Generics;
import edu.stanford.nlp.util.MapList;
import edu.stanford.nlp.util.StringUtils;
import org.jgraph.JGraph;
import org.jgraph.graph.DefaultGraphCell;
import org.jgraph.graph.GraphConstants;
import org.jgrapht.ext.JGraphModelAdapter;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.StringWriter;
import java.util.*;
import java.util.List;


/**
 * Generic utilities for dealing with Dependency graphs and other structures, useful for
 * text simplification and rewriting.
 *
 * @author Eric Yeh
 *
 * TODO: Migrate some of the functions (that make sense) into SemanticGraph proper.
 */
public class SemanticGraphUtils {

  private SemanticGraphUtils() {}

  /**
   * Given a collection of nodes from srcGraph, generates a new SemanticGraph based off the
   * subset represented by those nodes.  This uses the same vertices as in the original graph,
   * which allows for equality and comparisons between the two graphs.
   * @throws Exception
   */
  public static SemanticGraph makeGraphFromNodes(Collection<IndexedWord> nodes, SemanticGraph srcGraph) {
    if (nodes.size() == 1) {
      SemanticGraph retSg = new SemanticGraph();
      for (IndexedWord node :nodes)
        retSg.addVertex(node);
      return retSg;
    }

    if (nodes.isEmpty()) {
      return null;
    }

    List<SemanticGraphEdge> edges = new ArrayList<SemanticGraphEdge>();
    for (IndexedWord nodeG : nodes) {
      for (IndexedWord nodeD: nodes) {
        SemanticGraphEdge edge = srcGraph.getEdge(nodeG, nodeD);
        if (edge != null) {
          edges.add(edge);
        }
      }
    }
    return SemanticGraphFactory.makeFromEdges(edges);
  }

  //----------------------------------------------------------------------------------------
  //Query routines (obtaining sets of edges/vertices over predicates, etc)
  //----------------------------------------------------------------------------------------

  /**
   * Finds the vertex in the given SemanticGraph that corresponds to the given node.
   * Returns null if cannot find. Uses first match on index, sentIndex, and word values.
   */
  public static IndexedWord findMatchingNode(IndexedWord node, SemanticGraph sg) {
    for (IndexedWord tgt : sg.vertexList()) {
      if ((tgt.index() == node.index()) &&
          (tgt.sentIndex() == node.sentIndex()) &&
          (tgt.word().equals(node.word())) )
        return tgt;
    }
    return null;
  }


  /**
   * Given a starting vertice, grabs the subtree encapsulated by portion of the semantic graph, excluding
   * a given edge.  A tabu list is maintained, in order to deal with cyclical relations (such as between a
   * rcmod (relative clause) and its nsubj).
   *
   */
  public static Set<SemanticGraphEdge> getSubTreeEdges(IndexedWord vertice, SemanticGraph sg, SemanticGraphEdge excludedEdge) {
    HashSet<SemanticGraphEdge> tabu = new HashSet<SemanticGraphEdge>();
    tabu.add(excludedEdge);
    getSubTreeEdgesHelper(vertice, sg, tabu);
    tabu.remove(excludedEdge); // Do not want this in the returned edges
    return tabu;
  }

  public static void getSubTreeEdgesHelper(IndexedWord vertice, SemanticGraph sg, Set<SemanticGraphEdge> tabuEdges) {
    ArrayList<SemanticGraphEdge> outgoingEdges = new ArrayList<SemanticGraphEdge>(sg.outgoingEdgesOf(vertice));
    for (SemanticGraphEdge edge : outgoingEdges) {
      if (!tabuEdges.contains(edge)) {
        IndexedWord dep = edge.getDependent();
        tabuEdges.add(edge);
        getSubTreeEdgesHelper(dep, sg, tabuEdges);
      }
    }
  }

  /**
   * Given an iterable set of edges, returns the set of  vertices covered by these edges.
   * NOTE: duplicates not removed.
   */
  // XXX why is this a List rather than a Set (i.e. are the duplicates useful)?
  public static Collection<IndexedWord> getVerticesFromEdgeSet(Iterable<SemanticGraphEdge> edges) {
    Collection<IndexedWord> retSet = new ArrayList<IndexedWord>();
    for (SemanticGraphEdge edge : edges) {
      retSet.add(edge.getGovernor());
      retSet.add(edge.getDependent());
    }
    return retSet;
  }

  /**
   * GIven a set of nodes from a SemanticGraph, returns the set of edges that are
   * spanned between these nodes.
   */
  public static Collection<SemanticGraphEdge> getEdgesSpannedByVertices(Collection<IndexedWord> nodes, SemanticGraph sg) {
    Collection<SemanticGraphEdge> ret = new HashSet<SemanticGraphEdge>();
    for (IndexedWord n1 : nodes)
      for (IndexedWord n2: nodes) {
        if (n1 != n2) {
          SemanticGraphEdge edge = sg.getEdge(n1, n2);
          if (edge != null) ret.add(edge);
        }
      }
    return ret;
  }

  /**
   * Returns a list of all children bearing a grammatical relation starting with the given string, relnPrefix
   */
  public static List<IndexedWord> getChildrenWithRelnPrefix(SemanticGraph graph, IndexedWord vertex, String relnPrefix) {
    if (vertex.equals(IndexedWord.NO_WORD))
      return new ArrayList<IndexedWord>();
    if (!graph.vertexSet().contains(vertex)) {
      throw new IllegalArgumentException();
    }
    List<IndexedWord> childList = new ArrayList<IndexedWord>();
    Collection<SemanticGraphEdge> edges =  graph.outgoingEdgesOf(vertex);
    for (SemanticGraphEdge edge : edges) {
      if (edge.getRelation().toString().startsWith(relnPrefix)) {
        childList.add(edge.getTarget());
      }
    }
    return childList;
  }

  /**
   * Returns a list of all children bearing a grammatical relation starting with the given set of relation prefixes
   */
  public static List<IndexedWord> getChildrenWithRelnPrefix(SemanticGraph graph, IndexedWord vertex, Collection<String> relnPrefixes) {
    if (vertex.equals(IndexedWord.NO_WORD))
      return new ArrayList<IndexedWord>();
    if (!graph.vertexSet().contains(vertex)) {
      throw new IllegalArgumentException();
    }
    List<IndexedWord> childList = new ArrayList<IndexedWord>();
    Collection<SemanticGraphEdge> edges =  graph.outgoingEdgesOf(vertex);
    edgeLoop:
      for (SemanticGraphEdge edge : edges) {
        String edgeString = edge.getRelation().toString();
        for (String relnPrefix : relnPrefixes)
          if (edgeString.startsWith(relnPrefix)) {
            childList.add(edge.getTarget());
            continue edgeLoop;
          }
      }
    return childList;
  }

  /**
   * Since graphs can be have preps collapsed, finds all the immediate children of this node
   * that are linked by a collapsed preposition edge.
   */
  public static List<IndexedWord> getChildrenWithPrepC(SemanticGraph sg, IndexedWord vertex) {
    List<IndexedWord> ret =new ArrayList<IndexedWord>();
    //  Collection<GrammaticalRelation> prepCs = EnglishGrammaticalRelations.getPrepsC();
    //  for (SemanticGraphEdge edge : sg.outgoingEdgesOf(vertex)) {
    //  if (prepCs.contains(edge.getRelation()))
    for (SemanticGraphEdge edge : sg.outgoingEdgesOf(vertex)) {
      if (edge.getRelation().toString().startsWith("prep"))
        ret.add(edge.getDependent());
    }
    return ret;
  }

  /**
   * Returns the set of incoming edges for the given node that have the given
   * relation.
   *
   * Because certain edges may remain in string form (prepcs), check for both
   * string and object form of relations.
   */
  public static List<SemanticGraphEdge> incomingEdgesWithReln(IndexedWord node, SemanticGraph sg, GrammaticalRelation reln) {
    return edgesWithReln(sg.incomingEdgesOf(node), reln);
  }

  /**
   * Checks for outgoing edges of the node, in the given graph, which contain
   * the given relation.  Relations are matched on if they are GrammaticalRelation
   * objects or strings.
   */
  public static List<SemanticGraphEdge> outgoingEdgesWithReln(IndexedWord node, SemanticGraph sg, GrammaticalRelation reln) {
    return edgesWithReln(sg.outgoingEdgesOf(node), reln);
  }

  /**
   * Given a list of edges, returns those which match the given relation (can be string or
   * GrammaticalRelation object).
   */
  public static List<SemanticGraphEdge> edgesWithReln(Collection<SemanticGraphEdge> edges,
                                                      GrammaticalRelation reln) {
    List<SemanticGraphEdge> found = Generics.newArrayList();
    for (SemanticGraphEdge edge : edges) {
      GrammaticalRelation tgtReln = edge.getRelation();
      if (tgtReln.equals(reln)) {
        found.add(edge);
      }
    }
    return found;
  }

  /**
   * Given a semantic graph, and a relation prefix, returns a list of all relations (edges)
   * that start with the given prefix (e.g., prefix "prep" gives you all the prep relations: prep_by, pref_in,etc.)
   *
   */
  public static List<SemanticGraphEdge> findAllRelnsWithPrefix(SemanticGraph sg, String prefix) {
    ArrayList<SemanticGraphEdge> relns = new ArrayList<SemanticGraphEdge>();
    for (SemanticGraphEdge edge : sg.edgeList()) {
      GrammaticalRelation edgeRelation = edge.getRelation();
      if (edgeRelation.toString().startsWith(prefix)) {
        relns.add(edge);
      }
    }
    return relns;
  }

  /**
   * Finds the descendents of the given node in graph, avoiding the given set of nodes
   */
  public static Set<IndexedWord> tabuDescendants(SemanticGraph sg, IndexedWord vertex, Collection<IndexedWord> tabu) {
    if (!sg.vertexSet().contains(vertex)) {
      throw new IllegalArgumentException();
    }
    // Do a depth first search
    Set<IndexedWord> descendantSet = new HashSet<IndexedWord>();
    tabuDescendantsHelper(sg, vertex, descendantSet, tabu, null, null);
    return descendantSet;
  }

  /**
   * Finds the set of descendants for a node in the graph, avoiding the set of nodes and the
   * set of edge relations.  NOTE: these edges are encountered from the downward cull,
   * from governor to dependent.
   */
  public static Set<IndexedWord> tabuDescendants(SemanticGraph sg, IndexedWord vertex, Collection<IndexedWord> tabu,
                                                 Collection<GrammaticalRelation> tabuRelns) {
    if (!sg.vertexSet().contains(vertex)) {
      throw new IllegalArgumentException();
    }
    // Do a depth first search
    Set<IndexedWord> descendantSet = new HashSet<IndexedWord>();
    tabuDescendantsHelper(sg, vertex, descendantSet, tabu, tabuRelns, null);
    return descendantSet;
  }

  public static Set<IndexedWord> descendantsTabuRelns(SemanticGraph sg, IndexedWord vertex,
                                                      Collection<GrammaticalRelation> tabuRelns) {
    if (!sg.vertexSet().contains(vertex)) {
      throw new IllegalArgumentException();
    }
    // Do a depth first search
    Set<IndexedWord> descendantSet = new HashSet<IndexedWord>();
    tabuDescendantsHelper(sg, vertex, descendantSet, new HashSet<IndexedWord>(), tabuRelns, null);
    return descendantSet;
  }

  public static Set<IndexedWord> descendantsTabuTestAndRelns(SemanticGraph sg, IndexedWord vertex,
      Collection<GrammaticalRelation> tabuRelns, IndexedWordUnaryPred tabuTest) {
    if (!sg.vertexSet().contains(vertex)) {
      throw new IllegalArgumentException();
    }
    // Do a depth first search
    Set<IndexedWord> descendantSet = new HashSet<IndexedWord>();
    tabuDescendantsHelper(sg, vertex, descendantSet, new HashSet<IndexedWord>(), tabuRelns, tabuTest);
    return descendantSet;
  }

  public static Set<IndexedWord> descendantsTabuTestAndRelns(SemanticGraph sg, IndexedWord vertex,
      Collection<IndexedWord> tabuNodes, Collection<GrammaticalRelation> tabuRelns, IndexedWordUnaryPred tabuTest) {
    if (!sg.vertexSet().contains(vertex)) {
      throw new IllegalArgumentException();
    }
    // Do a depth first search
    Set<IndexedWord> descendantSet = new HashSet<IndexedWord>();
    tabuDescendantsHelper(sg, vertex, descendantSet, tabuNodes, tabuRelns, tabuTest);
    return descendantSet;
  }



  /**
   * Performs a cull for the descendents of the given node in the graph, subject to the tabu nodes to avoid,
   * relations to avoid crawling over, and child nodes to avoid traversing to based upon a predicate test.
   */
  private static void tabuDescendantsHelper(SemanticGraph sg, IndexedWord curr, Set<IndexedWord> descendantSet, Collection<IndexedWord> tabu,
      Collection<GrammaticalRelation> relnsToAvoid, IndexedWordUnaryPred tabuTest) {
    if (tabu.contains(curr))
      return;
    if (descendantSet.contains(curr)) {
      return;
    }

    descendantSet.add(curr);
    for (IndexedWord child : sg.getChildList(curr)) {
      SemanticGraphEdge edge = sg.getEdge(curr, child);
      if (relnsToAvoid != null && relnsToAvoid.contains(edge.getRelation()))
        continue;
      if (tabuTest != null && tabuTest.test(edge.getDependent(), sg))
        continue;
      tabuDescendantsHelper(sg, child, descendantSet, tabu, relnsToAvoid, tabuTest);
    }
  }


  //------------------------------------------------------------------------------------
  //"Constituent" extraction and manipulation
  //------------------------------------------------------------------------------------


  /**
   * Returns the vertice that is "leftmost."  Note this requires that the IndexedFeatureLabels present actually have
   * ordering information.
   * TODO: can be done more efficiently?
   */
  public static IndexedWord leftMostChildVertice(IndexedWord startNode, SemanticGraph sg) {
    TreeSet<IndexedWord> vertices = new TreeSet<IndexedWord>();
    for (IndexedWord vertex : sg.descendants(startNode)) {
      vertices.add(vertex);
    }
    return vertices.first();
  }

  /**
   * Given a SemanticGraph, and a set of nodes, finds the "blanket" of nodes that are one
   * edge away from the set of nodes passed in.  This is similar to the idea of a Markov
   * Blanket, except in the context of a SemanticGraph.
   * TODO: optimize
   */
  public static Collection<IndexedWord> getDependencyBlanket(SemanticGraph sg, Collection<IndexedWord> assertedNodes) {
    HashSet<IndexedWord> retSet = new HashSet<IndexedWord>();
    for (IndexedWord curr : sg.vertexList()) {
      if (!assertedNodes.contains(curr) && !retSet.contains(curr)) {
        for (IndexedWord assertedNode : assertedNodes) {
          if (sg.containsEdge(assertedNode, curr) || sg.containsEdge(curr, assertedNode)) {
            retSet.add(curr);
          }
        }
      }
    }
    return retSet;
  }

  /**
   * Resets the indices for the vertices in the graph, using the current
   * ordering returned by vertexList (presumably in order).  This is to ensure
   * accesses to the InfoFile word table do not fall off after a SemanticGraph has
   * been edited.
   *
   * NOTE: the vertices will be replaced, as JGraphT does not permit in-place
   * modification of the nodes.
   */
  public static SemanticGraph resetVerticeOrdering(SemanticGraph sg) {
    SemanticGraph nsg = new SemanticGraph();
    List<IndexedWord> vertices = new ArrayList<IndexedWord>(sg.vertexList(true));
    List<SemanticGraphEdge> edges = new ArrayList<SemanticGraphEdge>(sg.edgeSet());
    int index = 1;
    HashMap<IndexedWord, IndexedWord> oldToNewVertices = new HashMap<IndexedWord, IndexedWord>();
    List<IndexedWord> newVertices = new ArrayList<IndexedWord>();
    for (IndexedWord vertex : vertices) {
      IndexedWord newVertex = new IndexedWord(vertex);
      newVertex.setIndex(index++);
      oldToNewVertices.put(vertex, newVertex);
      ///sg.removeVertex(vertex);
      newVertices.add(newVertex);
    }

    for (IndexedWord nv : newVertices) {
      nsg.addVertex(nv);
    }

    List<IndexedWord> newRoots = new ArrayList<IndexedWord>();
    for (IndexedWord or : sg.getRoots()) {
      newRoots.add(oldToNewVertices.get(or));
    }
    nsg.setRoots(newRoots);

    for (SemanticGraphEdge edge : edges) {
      IndexedWord newGov = oldToNewVertices.get(edge.getGovernor());
      IndexedWord newDep = oldToNewVertices.get(edge.getDependent());
      SemanticGraphEdge newEdge = new SemanticGraphEdge(newGov, newDep,
          edge.getRelation(), edge.getWeight());
      nsg.addEdge(newGov,newDep,newEdge);
    }
    return nsg;
  }


  /**
   * Given a graph, ensures all edges are EnglishGrammaticalRelations
   * NOTE: this is English specific
   * NOTE: currently EnglishGrammaticalRelations does not link collapsed prep string forms
   * back to their object forms, for its valueOf relation.  This may need to be repaired if
   * generated edges indeed do have collapsed preps as strings.
   */
  public static void enRepairEdges(SemanticGraph sg, boolean verbose) {
    List<SemanticGraphEdge> edges = Generics.newArrayList(sg.edgeSet());
    for (SemanticGraphEdge edge : edges) {
      if (edge.getRelation().isFromString()) {
        GrammaticalRelation newReln =
          EnglishGrammaticalRelations.valueOf(edge.getRelation().toString());
        if (newReln != null) {
          IndexedWord gov = edge.getGovernor();
          IndexedWord dep = edge.getDependent();
          double weight = edge.getWeight();
          sg.removeEdge(edge);
          sg.addEdge(gov, dep, newReln, weight);
        } else {
          if (verbose)
            System.err.println("Warning, could not find matching GrammaticalRelation for reln="+edge.getRelation());
        }
      }
    }
  }

  public static void enRepairEdges(SemanticGraph sg) {
    enRepairEdges(sg, false);
  }

  /**
   * Deletes all nodes that are not rooted (such as dangling vertices after a series of
   * edges have been chopped).
   */
  public static void killNonRooted(SemanticGraph sg) {
    List<IndexedWord> nodes = new ArrayList<IndexedWord>(sg.vertexSet());

    // Hack: store all of the nodes we know are in the rootset
    Set<IndexedWord> guaranteed = new HashSet<IndexedWord>();
    for (IndexedWord root : sg.getRoots()) {
      guaranteed.add(root);
      guaranteed.addAll(sg.descendants(root));
    }

    for (IndexedWord node : nodes) {
      if (!guaranteed.contains(node)) {
        sg.removeVertex(node);
      }
    }
  }

  /**
   * Replaces a node in the given SemanticGraph with the new node, replacing its position in the
   * node edges.
   */
  public static void replaceNode(IndexedWord newNode, IndexedWord oldNode,
      SemanticGraph sg) {
    // Obtain the edges where the old node was the governor and the dependent.
    // Remove the old node, insert the new, and re-insert the edges.
    List<SemanticGraphEdge> govEdges = new ArrayList<SemanticGraphEdge>(sg.outgoingEdgesOf(oldNode));
    List<SemanticGraphEdge> depEdges = new ArrayList<SemanticGraphEdge>(sg.incomingEdgesOf(oldNode));
    boolean oldNodeRemoved = sg.removeVertex(oldNode);
    if (oldNodeRemoved) {
      // If the new node is not present, be sure to add it in.
      if (!sg.containsVertex(newNode))
        sg.addVertex(newNode);
      for (SemanticGraphEdge govEdge : govEdges) {
        sg.removeEdge(govEdge);
        sg.addEdge(newNode, govEdge.getDependent(), govEdge.getRelation(), govEdge.getWeight());
      }
      for (SemanticGraphEdge depEdge : depEdges) {
        sg.removeEdge(depEdge);
        sg.addEdge(depEdge.getGovernor(), newNode, depEdge.getRelation(), depEdge.getWeight());
      }
    } else {
      System.err.println("SemanticGraphUtils.replaceNode: previous node does not exist");
    }
    sg.vertexList(true); // force the vlist cache to be updated
  }

  public static final String WILDCARD_VERTICE_TOKEN = "WILDCARD";
  public static final IndexedWord WILDCARD_VERTICE = new IndexedWord();
  static {
    WILDCARD_VERTICE.setWord("*");
    WILDCARD_VERTICE.setValue("*");
    WILDCARD_VERTICE.setCurrent("*");
  }

  /**
   * GIven an iterable set of distinct vertices, creates a new mapping that maps the
   * original vertices to a set of "generic" versions.  Used for generalizing tokens in discovered rules.
   * @param verts Vertices to anonymize
   * @param prefix Prefix to assign to this anonymization
   */
  public static Map<IndexedWord, IndexedWord> anonymyizeNodes(Iterable<IndexedWord> verts, String prefix) {
    Map<IndexedWord, IndexedWord> retMap = new HashMap<IndexedWord, IndexedWord>();
    int index = 1;
    for (IndexedWord orig: verts) {
      IndexedWord genericVert = new IndexedWord(orig);
      genericVert.set(LemmaAnnotation.class, "");
      String genericValue = prefix+index;
      genericVert.setValue(genericValue);
      genericVert.setWord(genericValue);
      genericVert.setCurrent(genericValue);
      index++;
      retMap.put(orig, genericVert);
    }
    return retMap;
  }


  public static final String SHARED_NODE_ANON_PREFIX ="A";
  public static final String BLANKET_NODE_ANON_PREFIX ="B";

  /**
   * Used to make a mapping that lets you create "anonymous" versions of shared nodes between two
   * graphs (given in the arg) using the shared prefix.
   */
  public static Map<IndexedWord, IndexedWord> makeGenericVertices(Iterable<IndexedWord> verts) {
    return anonymyizeNodes(verts, SHARED_NODE_ANON_PREFIX);
  }

  /**
   * Used to assign generic labels to the nodes in the "blanket" for a set of vertices in a graph.  Here, a "blanket" node is
   * similar to nodes in a Markov Blanket, i.e. nodes that are one edge away from a set of asserted vertices in a
   * SemanticGraph.
   */
  public static Map<IndexedWord, IndexedWord> makeBlanketVertices(Iterable<IndexedWord> verts) {
    return anonymyizeNodes(verts, BLANKET_NODE_ANON_PREFIX);
  }


  /**
   * Given a set of edges, and a mapping between the replacement and target vertices that comprise the
   * vertices of the edges, returns a new set of edges with the replacement vertices.  If a replacement
   * is not present, the WILDCARD_VERTICE is used in its place (i.e. can be anything).
   *
   * Currently used to generate "generic" versions of Semantic Graphs, when given a list of generic
   * vertices to replace with, but can conceivably be used for other purposes where vertices must
   * be replaced.
   */
  public static List<SemanticGraphEdge> makeReplacedEdges(Iterable<SemanticGraphEdge> edges, Map<IndexedWord, IndexedWord> vertReplacementMap,
      boolean useGenericReplacement) {
    List<SemanticGraphEdge> retList = new ArrayList<SemanticGraphEdge>();
    for (SemanticGraphEdge edge : edges) {
      IndexedWord gov = edge.getGovernor();
      IndexedWord dep = edge.getDependent();
      IndexedWord newGov = vertReplacementMap.get(gov);
      IndexedWord newDep = vertReplacementMap.get(dep);
      if (useGenericReplacement) {
        if (newGov == null) {
          newGov = new IndexedWord(gov);
          newGov.set(TextAnnotation.class, WILDCARD_VERTICE_TOKEN);
          newGov.set(CurrentAnnotation.class, WILDCARD_VERTICE_TOKEN);
          newGov.set(LemmaAnnotation.class, WILDCARD_VERTICE_TOKEN);
        }
        if (newDep == null) {
          newDep = new IndexedWord(dep);
          newDep.set(TextAnnotation.class, WILDCARD_VERTICE_TOKEN);
          newDep.set(CurrentAnnotation.class, WILDCARD_VERTICE_TOKEN);
          newDep.set(LemmaAnnotation.class,WILDCARD_VERTICE_TOKEN);
        }
      } else {
        if (newGov == null)
          newGov = edge.getGovernor();
        if (newDep == null)
          newDep = edge.getDependent();
      }
      SemanticGraphEdge newEdge = new SemanticGraphEdge(
          newGov, newDep,
          edge.getRelation(), edge.getWeight());
      retList.add(newEdge);
    }
    return retList;
  }

  /**
   * Given a set of vertices from the same graph, returns the set of all edges between these
   * vertices.
   */
  public static Set<SemanticGraphEdge> allEdgesInSet(Iterable<IndexedWord> vertices, SemanticGraph sg) {
    HashSet<SemanticGraphEdge> edges = new HashSet<SemanticGraphEdge>();
    for (IndexedWord v1 : vertices) {
      for (Object edge : sg.outgoingEdgesOf(v1)) {
        edges.add((SemanticGraphEdge) edge);
      }
      for (Object edge : sg.incomingEdgesOf(v1)) {
        edges.add((SemanticGraphEdge) edge);
      }
    }
    return edges;
  }

  /**
   * Given two iterable sequences of edges, returns a pair containing the set of
   * edges in the first graph not in the second, and edges in the second not in the first.
   * Edge equality is determined using an object that implements ISemanticGraphEdgeEql.
   *
   */
  public static EdgeDiffResult diffEdges(Collection<SemanticGraphEdge> edges1, Collection<SemanticGraphEdge> edges2,
      SemanticGraph sg1, SemanticGraph sg2,
      ISemanticGraphEdgeEql compareObj) {
    Set<SemanticGraphEdge> remainingEdges1 = new HashSet<SemanticGraphEdge>();
    Set<SemanticGraphEdge> remainingEdges2 = new HashSet<SemanticGraphEdge>();
    Set<SemanticGraphEdge> sameEdges = new HashSet<SemanticGraphEdge>();


    ArrayList<SemanticGraphEdge> edges2Cache = new ArrayList<SemanticGraphEdge>(edges2);
    edge1Loop:
      for (SemanticGraphEdge edge1 : edges1) {
        for (SemanticGraphEdge edge2 : edges2Cache) {
          if (compareObj.equals(edge1, edge2, sg1, sg2)) {
            sameEdges.add(edge1);
            edges2Cache.remove(edge2);
            continue edge1Loop;
          }
        }
        remainingEdges1.add(edge1);
      }

    ArrayList<SemanticGraphEdge> edges1Cache = new ArrayList<SemanticGraphEdge>(edges1);
    edge2Loop:
      for (SemanticGraphEdge edge2 : edges2) {
        for (SemanticGraphEdge edge1 : edges1) {
          if (compareObj.equals(edge1, edge2, sg1, sg2)) {
            edges1Cache.remove(edge1);
            continue edge2Loop;
          }
        }
        remainingEdges2.add(edge2);
      }

    return new EdgeDiffResult(sameEdges, remainingEdges1, remainingEdges2);
  }

  public static class EdgeDiffResult {
    Set<SemanticGraphEdge> sameEdges;
    Set<SemanticGraphEdge> remaining1;
    Set<SemanticGraphEdge> remaining2;

    public EdgeDiffResult(Set<SemanticGraphEdge> sameEdges,
        Set<SemanticGraphEdge> remaining1,
        Set<SemanticGraphEdge> remaining2) {
      this.sameEdges = sameEdges;
      this.remaining1 = remaining1;
      this.remaining2 = remaining2;
    }

    public Set<SemanticGraphEdge> getRemaining1() {
      return remaining1;
    }

    public Set<SemanticGraphEdge> getRemaining2() {
      return remaining2;
    }

    public Set<SemanticGraphEdge> getSameEdges() {
      return sameEdges;
    }
  }


  /**
   * Pretty printers
   */
  public static String printEdges(Iterable<SemanticGraphEdge> edges) {
    StringWriter buf = new StringWriter();
    for (SemanticGraphEdge edge : edges) {
      buf.append("\t");
      buf.append(edge.getRelation().toString());
      buf.append("(");
      buf.append(edge.getGovernor().toString());
      buf.append(", ");
      buf.append(edge.getDependent().toString());
      buf.append(")\n");
    }
    return buf.toString();
  }

  public static class PrintVerticeParams {
    public boolean showWord = true;
    public boolean showIndex = true;
    public boolean showSentIndex = false;
    public boolean showPOS = false;
    public int wrapAt = 8;
  }

  public static String printVertices(SemanticGraph sg) {
    return printVertices(sg, new PrintVerticeParams());
  }

  public static String printVertices(SemanticGraph sg, PrintVerticeParams params) {
    StringWriter buf = new StringWriter();
    int count = 0;
    for (IndexedWord word : sg.vertexList()) {
      count++;
      if (count % params.wrapAt == 0) { buf.write("\n\t"); }
      if (params.showIndex) {
        buf.write(String.valueOf(word.index()));
        buf.write(":");
      }
      if (params.showSentIndex) {
        buf.write("s");
        buf.write(String.valueOf(word.sentIndex()));
        buf.write("/");
      }
      if (params.showPOS) {
        buf.write(word.tag());
        buf.write("/");
      }

      if (params.showWord) {
        buf.write(word.word());
      }

      buf.write(" ");
    }
    return buf.toString();
  }

  /**
   * Given a SemanticGraph, creates a SemgrexPattern string based off of this graph.
   * NOTE: the word() value of the vertice is the name to reference
   * NOTE: currently presumes there is only one root in this graph.
   * TODO: see if Semgrex can allow multiroot patterns
   * @param sg SemanticGraph to base this pattern on.
   */
  public static String semgrexFromGraph(SemanticGraph sg, boolean matchTag, boolean matchWord,
      Map<IndexedWord, String> nodeNameMap) throws Exception {
    return semgrexFromGraph(sg, null, matchTag, matchWord, nodeNameMap);
  }

  public static String semgrexFromGraph(SemanticGraph sg, Collection<IndexedWord> wildcardNodes,
      boolean useTag, boolean useWord, Map<IndexedWord, String> nodeNameMap) throws Exception {
    IndexedWord patternRoot = sg.getFirstRoot();
    StringWriter buf = new StringWriter();
    Set<IndexedWord> tabu = new HashSet<IndexedWord>();
    Set<SemanticGraphEdge> seenEdges = new HashSet<SemanticGraphEdge>();

    buf.append(semgrexFromGraphHelper(patternRoot, sg, tabu, seenEdges, true, true, wildcardNodes,
        useTag, useWord, nodeNameMap));

    String patternString = buf.toString();
    return patternString;
  }


  /**
   * Given a set of edges that form a rooted and connected graph, returns a Semgrex pattern
   * corresponding to it.
   * @throws Exception
   */
  public static String semgrexFromGraph(Iterable<SemanticGraphEdge> edges, boolean matchTag,
      boolean matchWord, Map<IndexedWord, String> nodeNameMap) throws Exception {
    SemanticGraph sg = SemanticGraphFactory.makeFromEdges(edges);
    return semgrexFromGraph(sg, matchTag, matchWord, nodeNameMap);
  }

  /**
   * Recursive call to generate the Semgrex pattern based off of this SemanticGraph.
   * Currently presumes the only elements to match on are the tags.
   * TODO: consider tag generalization, and matching off of other features?
   */
  protected static String semgrexFromGraphHelper(IndexedWord vertice, SemanticGraph sg,
      Set<IndexedWord> tabu, Set<SemanticGraphEdge> seenEdges, boolean useWordAsLabel, boolean nameEdges, Collection<IndexedWord> wildcardNodes,
      boolean useTag, boolean useWord, Map<IndexedWord, String> nodeNameMap) {
    StringWriter buf = new StringWriter();
    Set<SemanticGraphEdge> edges = sg.outgoingEdgesOf(vertice);

    // If the node is a wildcarded one, treat it as a {}, meaning any match.  Currently these will not
    // be labeled, but this may change later.
    if (wildcardNodes != null && wildcardNodes.contains(vertice)) {
      buf.append("{}");
    } else {
      buf.append("{");
      if (useTag) {
        buf.append("tag:"); buf.append(vertice.tag());
        if (useWord)
          buf.append(";");
      }
      if (useWord) {
        buf.append("word:"); buf.append(vertice.word());
      }
      buf.append("}");
    }
    if (nodeNameMap != null) {
      buf.append("=");
      buf.append(nodeNameMap.get(vertice));
      buf.append(" ");
    } else if (useWordAsLabel) {
      buf.append("=");
      buf.append(sanitizeForSemgrexName(vertice.word()));
      buf.append(" ");
    }

    tabu.add(vertice);

    // For each edge, record the edge, but do not traverse to the vertice if it is already in the
    // tabu list.  If it already is, we emit the edge and the target vertice, as
    // we will not be continuing in that vertex, but we wish to record the relation.
    // If we will proceed down that node, add parens if it will continue recursing down.
    for (SemanticGraphEdge edge : edges) {
      seenEdges.add(edge);
      IndexedWord tgtVert = edge.getDependent();
      boolean applyParens = sg.outDegreeOf(tgtVert) > 0 && !tabu.contains(tgtVert);
      buf.append(" >");
      buf.append(edge.getRelation().toString());
      if (nameEdges) {
        buf.append("=E");
        buf.write(String.valueOf(seenEdges.size()));
      }
      buf.append(" ");
      if (applyParens)
        buf.append("(");
      if (tabu.contains(tgtVert)) {
        buf.append("{tag:"); buf.append(tgtVert.tag()); buf.append("}");
        if (useWordAsLabel) {
          buf.append("=");
          buf.append(tgtVert.word());
          buf.append(" ");
        }
      } else {
        buf.append(semgrexFromGraphHelper(tgtVert, sg, tabu, seenEdges, useWordAsLabel, nameEdges,
            wildcardNodes, useTag, useWord, nodeNameMap));
        if (applyParens)
          buf.append(")");
      }
    }
    return buf.toString();
  }

  /**
   * Sanitizes the given string into a Semgrex friendly name
   */
  public static String sanitizeForSemgrexName(String text) {
    text = text.replaceAll("\\.", "_DOT_");
    text = text.replaceAll("\\,", "_COMMA_");
    text = text.replaceAll("\\\\", "_BSLASH_");
    text = text.replaceAll("\\/", "_BSLASH_");
    text = text.replaceAll("\\?", "_QUES_");
    text = text.replaceAll("\\!", "_BANG_");
    text = text.replaceAll("\\$", "_DOL_");
    text = text.replaceAll("\\!", "_BANG_");
    text = text.replaceAll("\\&", "_AMP_");
    text = text.replaceAll("\\:", "_COL_");
    text = text.replaceAll("\\;", "_SCOL_");
    text = text.replaceAll("\\#", "_PND_");
    text = text.replaceAll("\\@", "_AND_");
    text = text.replaceAll("\\%", "_PER_");
    text = text.replaceAll("\\(","_LRB_");
    text = text.replaceAll("\\)", "_RRB_");
    return text;
  }


  /**
   * Given a <code>SemanticGraph</code>, returns a new graph (with new node), with
   * the lemma fields in place.
   *
   * NOTE: unfortunately, cannot lemmatize in place, due to brittleness with modifying
   * existing nodes in JGraph.
   */
  public static SemanticGraph lemmatize(SemanticGraph sg) {
    // Need to call replacenode on each, otherwise the graph will fall apart.  Eat one time cost to
    // allow lemmas to be retained.
    // Also, maintain a list of the current to new root mappings, and reset
    SemanticGraph newGraph = new SemanticGraph(sg);
    List<IndexedWord> prevRoots = new ArrayList<IndexedWord>(newGraph.getRoots());
    List<IndexedWord> newRoots = new ArrayList<IndexedWord>();
    for (IndexedWord node : newGraph.vertexList()) {
      IndexedWord newWord = new IndexedWord(node);
      String lemma = Morphology.stemStatic(node.word(), node.tag()).word();
      newWord.setLemma(lemma);
      // XXX doesn't seem like replacing the node necessarily maintains the order of the list
      replaceNode(newWord, node, newGraph);
      if (prevRoots.contains(node))
        newRoots.add(newWord);
    }
    newGraph.setRoots(newRoots);
    return newGraph;
  }

  /**
   * GIven a graph, returns a new graph with the the new sentence index enforced.
   * NOTE: new vertices are inserted.
   */
  public static SemanticGraph setSentIndex(SemanticGraph sg, int newSentIndex) {
    SemanticGraph newGraph = new SemanticGraph(sg);
    List<IndexedWord> prevRoots = new ArrayList<IndexedWord>(newGraph.getRoots());
    List<IndexedWord> newRoots = new ArrayList<IndexedWord>();
    for (IndexedWord node : newGraph.vertexList()) {
      IndexedWord newWord = new IndexedWord(node);
      newWord.setSentIndex(newSentIndex);
      SemanticGraphUtils.replaceNode(newWord, node, newGraph);
      if (prevRoots.contains(node))
        newRoots.add(newWord);
    }
    newGraph.setRoots(newRoots);
    return newGraph;
  }

  //-----------------------------------------------------------------------------------------------
  //   Graph redundancy checks
  //-----------------------------------------------------------------------------------------------


  /**
   * Removes duplicate graphs from the set, using the string form of the graph
   * as the key (obviating issues with object equality).
   */
  public static Collection<SemanticGraph> removeDuplicates(Collection<SemanticGraph> graphs) {
    Map<String, SemanticGraph> map = new HashMap<String, SemanticGraph>();
    for (SemanticGraph sg : graphs) {
      String keyVal = sg.toString().intern();
      map.put(keyVal, sg);
    }
    return map.values();
  }

  /**
   * Given the set of graphs to remove duplicates from, also removes those on the tabu graphs
   * (and does not include them in the return set).
   */
  public static Collection<SemanticGraph> removeDuplicates(Collection<SemanticGraph> graphs,
      Collection<SemanticGraph> tabuGraphs) {
    Map<String, SemanticGraph> tabuMap = new HashMap<String, SemanticGraph>();
    for (SemanticGraph tabuSg : tabuGraphs) {
      String keyVal = tabuSg.toString().intern();
      tabuMap.put(keyVal, tabuSg);
    }
    Map<String, SemanticGraph> map = new HashMap<String, SemanticGraph>();
    for (SemanticGraph sg : graphs) {
      String keyVal = sg.toString().intern();
      if (tabuMap.containsKey(keyVal))
        continue;
      map.put(keyVal, sg);
    }
    return map.values();
  }

  public static Collection<SemanticGraph> removeDuplicates(Collection<SemanticGraph> graphs,
      SemanticGraph tabuGraph) {
    Collection<SemanticGraph> tabuSet = new HashSet<SemanticGraph>();
    tabuSet.add(tabuGraph);
    return removeDuplicates(graphs, tabuSet);
  }

  // -----------------------------------------------------------------------------------------------
  // Tree matching code
  // -----------------------------------------------------------------------------------------------

  /**
   * Given a CFG Tree parse, and the equivalent SemanticGraph derived from that Tree, generates a mapping
   * from each of the tree terminals to the best-guess SemanticGraph node(s).
   * This is performed using lexical matching, finding the nth match.
   * NOTE: not all tree nodes may match a Semgraph node, esp. for tokens removed in a collapsed Semgraph,
   * such as prepositions.
   */
  public static Map<PositionedTree, IndexedWord> mapTreeToSg(Tree tree, SemanticGraph sg) {
    // In order to keep track of positions, we store lists, in order encountered, of lex terms.
    // e.g. lexToTreeNode.get("the").get(2) should point to the same word as lexToSemNode.get("the").get(2)
    // Because IndexedWords may be collapsed together "A B" -> "A_B", we check the value of current(), and
    // split on whitespace if present.
    MapList<String, TreeNodeProxy> lexToTreeNode = new MapList<String, TreeNodeProxy>();
    MapList<String, IndexedWordProxy> lexToSemNode = new MapList<String, IndexedWordProxy>();

    for (Tree child : tree.getLeaves()) {
      List<TreeNodeProxy> leafProxies = TreeNodeProxy.create(child, tree);
      for (TreeNodeProxy proxy : leafProxies)
        lexToTreeNode.add(proxy.lex, proxy);
    }

    Map<IndexedWord, Integer> depthMap = new HashMap<IndexedWord, Integer>();
    for (IndexedWord node : sg.vertexList()) {
      List<IndexedWord> path = sg.getPathToRoot(node);
      if (path != null)
        depthMap.put(node, path.size());
      else
        depthMap.put(node, 99999); // Use an arbitrarily deep depth value, to trick it into never being used.
      List<IndexedWordProxy> nodeProxies = IndexedWordProxy.create(node);
      for (IndexedWordProxy proxy : nodeProxies)
        lexToSemNode.add(proxy.lex, proxy);
    }

    // Now the map-lists (string->position encountered indices) are populated,
    // simply go through, finding matches.
    // NOTE: we use TreeNodeProxy instead of keying off of Tree, as hash codes for
    // Tree nodes do not consider position of the tree within a tree: two subtrees
    // with the same layout and child labels will be equal.
    Map<PositionedTree, IndexedWord> map = new HashMap<PositionedTree, IndexedWord>();
    for (String lex : lexToTreeNode.keySet()) {
      for (int i=0;i<lexToTreeNode.size(lex) && i<lexToSemNode.size(lex);i++) {
        map.put(new PositionedTree(lexToTreeNode.get(lex, i).treeNode, tree), lexToSemNode.get(lex,i).node);
      }
    }

    // Now that a terminals to terminals map has been generated, account for the
    // tree non-terminals.
    for (Tree nonTerm : tree) {
      if (!nonTerm.isLeaf()) {
        IndexedWord bestNode = null;
        int bestScore = 99999;
        for (Tree curr : nonTerm) {
          IndexedWord equivNode = map.get(new PositionedTree(curr, tree));
          if ((equivNode == null) || !depthMap.containsKey(equivNode)) continue;
          int currScore = depthMap.get(equivNode);
          if (currScore < bestScore) {
            bestScore = currScore;
            bestNode = equivNode;
          }
        }
        if (bestNode != null) {
          map.put(new PositionedTree(nonTerm, tree), bestNode);
        }
      }
    }

    return map;
  }

  /**
   * Private helper class for <code>mapTreeToSg</code>.   Acts to
   * map between a Tree node and a lexical value.
   * @author Eric Yeh
   *
   */
  private static class TreeNodeProxy {
    Tree treeNode;
    String lex;
    Tree root;

    public String toString() {
      return lex+" -> "+treeNode.toString()+", #="+treeNode.nodeNumber(root);
    }

    private TreeNodeProxy(Tree intree, String lex, Tree root) {
      this.treeNode = intree;
      this.lex = lex;
      this.root = root;
    }

    public static List<TreeNodeProxy> create(Tree intree, Tree root) {
      List<TreeNodeProxy> ret = new ArrayList<TreeNodeProxy>();
      if (intree.isLeaf()) {
        ret.add(new TreeNodeProxy(intree, intree.label().value(), root));
      } else
        for (LabeledWord lword : intree.labeledYield()) {
          ret.add(new TreeNodeProxy(intree, lword.word(), root));
        }

      return ret;
    }
  }

  /**
   * This is used to uniquely index trees within a
   * Tree, maintaining the position of this subtree
   * within the context of the root.
   * @author Eric Yeh
   *
   */
  public static class PositionedTree {
    Tree tree;
    Tree root;
    int nodeNumber;

    public String toString() {
      return tree+"."+nodeNumber;
    }

    public PositionedTree(Tree tree, Tree root) {
      this.tree = tree;
      this.root = root;
      this.nodeNumber = tree.nodeNumber(root);
    }

    public boolean equals(Object obj) {
      if (obj instanceof PositionedTree) {
        PositionedTree tgt = (PositionedTree) obj;
        return tree.equals(tgt.tree) && root.equals(tgt.root) && tgt.nodeNumber == nodeNumber;
      }
      return false;
    }

    /**
     * TODO: verify this is correct
     */
    @Override
    public int hashCode() {
      int hc = tree.hashCode() ^ (root.hashCode() << 8);
      hc ^= (2 ^ nodeNumber);
      return hc;
    }
  }

  /**
   * Private helper class for <code>mapTreeToSg</code>.  Acts to
   * map between an IndexedWord (in a SemanticGraph) and a lexical value.
   * @author lumberjack
   *
   */
  private static final class IndexedWordProxy {
    IndexedWord node;
    String lex;

    public String toString() {
      return lex+" -> "+node.word()+":"+node.sentIndex()+"."+node.index();
    }

    private IndexedWordProxy(IndexedWord node, String lex) {
      this.node = node; this.lex = lex;
    }

    /**
     * Generates a set of IndexedWordProxy objects.  If the current() field is present, splits the tokens by
     * a space, and for each, creates a new IndexedWordProxy, in order encountered, referencing this current
     * node, but using the lexical value of the current split token.  Otherwise just use the value of word().
     * This is used to retain attribution to the originating node.
     */
    public static List<IndexedWordProxy> create(IndexedWord node) {
      List<IndexedWordProxy> ret = new ArrayList<IndexedWordProxy>();
      if (node.current().length() > 0) {
        for (String token : node.current().split(" ")) {
          ret.add(new IndexedWordProxy(node, token));
        }
      } else {
        ret.add(new IndexedWordProxy(node, node.word()));
      }
      return ret;
    }


  }

  //-----------------------------------------------------------------------------------------------
  //JGraph based graph visualization
  //-----------------------------------------------------------------------------------------------



  private static final int DISPLAY_WIDTH = 1100;
  private static final int DISPLAY_HEIGHT = 800;
  private static final int NODE_STEP_X = 100;
  private static final int NODE_STEP_Y = 100;
  private static final int START_X = DISPLAY_WIDTH/2;
  private static final int START_Y = 10;


  public static JFrame render(GrammaticalStructure gs, String frameLabel) {
    SemanticGraph sg = SemanticGraphFactory.makeFromTree(gs, true, false, true, true, false, true, null, "", 1);
    return render(sg, frameLabel);
  }


  /**
   * Using the JGraph rendering library, creates a JFrame with a simple node-arc display of the
   * given graph.
   */
  public static JFrame render(SemanticGraph sg, String frameLabel) {
    // do yucky fiddle for SemanticGraphEdge printing.  Not thread safe!
    boolean formerState = SemanticGraphEdge.printOnlyRelation;
    SemanticGraphEdge.printOnlyRelation = true;
    JGraphModelAdapter<IndexedWord, SemanticGraphEdge> modelAdapter = new JGraphModelAdapter<IndexedWord, SemanticGraphEdge>(sg);
    JGraph jgraph = new JGraph(modelAdapter);
    JFrame graphFrame = new JFrame(frameLabel);
    //JScrollPane scrollPane = new JScrollPane();
    graphFrame.getContentPane().add(new JLabel("Semantic Graph "), BorderLayout.CENTER);
    // graphFrame.getContentPane().add(scrollPane);
    // scrollPane.setSize(new Dimension(800,600));
    jgraph.setSize(new Dimension(DISPLAY_WIDTH,DISPLAY_HEIGHT));
    graphFrame.setSize(new Dimension(DISPLAY_WIDTH,DISPLAY_HEIGHT));
    jgraph.setBackground(Color.WHITE);
    graphFrame.add(jgraph);
    // scrollPane.add(jgraph);
    layoutJGraphNodes(sg, modelAdapter);
    graphFrame.pack();
    graphFrame.setVisible(true);
    SemanticGraphEdge.printOnlyRelation = formerState;
    return graphFrame;
  }



  /**
   * Lays out the vertices in the JGraph graphical rendering of the semantic graph, similar to
   * the way SemanticGraph.toString() lays nodes out.
   */
  private static void layoutJGraphNodes(SemanticGraph sg, JGraphModelAdapter<IndexedWord, SemanticGraphEdge> modelAdapter) {
    Set<IndexedWord> used = new HashSet<IndexedWord>();
    int xOffset = START_X;
    int yOffset = START_Y;
    for (IndexedWord root : sg.getRoots()) {
      placeNode(root, xOffset, yOffset, used, sg, modelAdapter);
      yOffset += NODE_STEP_Y;
      xOffset += NODE_STEP_X; // cdm added 20091230
    }
  }

  private static void placeNode(IndexedWord currNode, int xOffset, int yOffset, Set<IndexedWord> used,
      SemanticGraph sg, JGraphModelAdapter<IndexedWord, SemanticGraphEdge> modelAdapter) {
    if ( ! used.contains(currNode)) {
      placeJGraphNode(currNode, xOffset, yOffset, modelAdapter);
      used.add(currNode);
    }

    List<SemanticGraphEdge> edges = new ArrayList<SemanticGraphEdge>(sg.outgoingEdgesOf(currNode));
    Collections.sort(edges);
    int startX = -1;
    int effectiveNodeStepX = NODE_STEP_X * 2;
    while (effectiveNodeStepX > 16 &&  startX <= 0) {
      effectiveNodeStepX = effectiveNodeStepX / 2;
      startX = xOffset - (int) ((edges.size() / 2.0) * effectiveNodeStepX);
    }
    for (SemanticGraphEdge edge : edges) {
      IndexedWord target = edge.getTarget();
      if (!used.contains(target)) {
        placeNode(target, startX, yOffset + NODE_STEP_Y, used, sg, modelAdapter);
        startX+= effectiveNodeStepX;
      }
    }
  }

  // used in rte.gui.GUIProblemPanel!
  public static void placeJGraphNode(IndexedWord node, int xPos, int yPos, JGraphModelAdapter<IndexedWord,SemanticGraphEdge> modelAdapter) {
    // System.out.println("placeJgraph node="+node+", x="+xPos+", y="+yPos);
    DefaultGraphCell jCell = modelAdapter.getVertexCell(node);
    Map cellAttr = jCell.getAttributes();
    Rectangle2D cellBounds = GraphConstants.getBounds(cellAttr);
    GraphConstants.setBounds(cellAttr, new Rectangle2D.Double(xPos, yPos, cellBounds.getWidth(), cellBounds.getHeight()));
    //  GraphConstants.setOffset(cellAttr, new Point(xPos, yPos));
    Map<DefaultGraphCell, Map> newCellAttr = new HashMap<DefaultGraphCell, Map>();
    newCellAttr.put(jCell, cellAttr);
    //TODO(dlwh) confirms this works. The manual says it should.
    modelAdapter.edit(newCellAttr,null,null,null);

  }

    /**
     * Emits the given Semantic Graph to the target file in DOT (GraphViz) format.
     * To generate a PDF or PNG version of the graph, run GraphViz on the DOT file.
     * @param sg Semantic Graph to print out
     * @param tgt Target DOT filename
     */
  public static void toDOT(SemanticGraph sg, File tgt) {
    StringWriter buf = new StringWriter();
    buf.write("digraph \"tree\" {order=out; node [shape=plaintext];\n");
    HashMap<IndexedWord, Integer> lookup = new HashMap<IndexedWord, Integer>();
    int uid = 1;
    for (IndexedWord vert : sg.vertexList()) {
      lookup.put(vert, uid);
      buf.write("n"+uid+" [label=\""+vert.toString()+"\"];\n");
      uid += 1;
    }
    for (SemanticGraphEdge edge : sg.edgeList()) {
      String gov = "n"+lookup.get(edge.getGovernor());
      String dep = "n"+lookup.get(edge.getDependent());
      buf.write(gov+" -> "+dep + " [label=\""+edge.toString()+"\" fontsize=10 fontname=\"Times-Italic\"];\n");
    }
    buf.write("}");
    StringUtils.printToFile(tgt, buf.toString());
  }

}
