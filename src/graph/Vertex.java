package graph;

/**
 * Represents a vertex in an edge-weighted directed graph.
 */
public interface Vertex<E extends WeightedEdge<?>> {

    /**
     * Return an object supporting iteration over all the edges connecting this vertex to another
     * vertex in the graph.  This vertex serves as the "tail" vertex for each such edge.
     */
    Iterable<E> outgoingEdges();
}
