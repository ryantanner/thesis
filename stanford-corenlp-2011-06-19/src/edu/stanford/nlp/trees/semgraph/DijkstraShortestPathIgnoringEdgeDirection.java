package edu.stanford.nlp.trees.semgraph;

import java.util.List;

import org.jgrapht.DirectedGraph;
import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.graph.AsUndirectedGraph;

public final class DijkstraShortestPathIgnoringEdgeDirection<V,E> {

    /**
     * Find the shortest path between two vertices, represented as a List of
     * Edges in order from start to end vertex.
     *
     * @param graph the graph to be searched
     * @param startVertex the vertex at which the path should start
     * @param endVertex the vertex at which the path should end
     *
     * @return List of Edges, or null if no path exists
     */
    public static <V,E> List<E> findPathBetween( DirectedGraph<V,E> graph, V startVertex,
        V endVertex ) {
          return DijkstraShortestPath.findPathBetween(new AsUndirectedGraph<V, E>(graph), startVertex, endVertex);
    }

}
