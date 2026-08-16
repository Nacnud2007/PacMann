package graph;

/**
 * Represents a double-weighted edge in a directed graph.
 */
public interface WeightedEdge<V> {

    /**
     * Return the vertex where this edge starts.
     */
    V tail();

    /**
     * Return the vertex where this edge ends.
     */
    V head();

    /**
     * Return the weight of this edge.
     */
    double weight();
}
