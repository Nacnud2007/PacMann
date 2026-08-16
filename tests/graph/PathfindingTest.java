package graph;

import graph.Pathfinding.PathEnd;
import java.util.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static graph.SimpleGraph.*;

/**
 * Uses the `SimpleGraph` class to verify the functionality of the `Pathfinding` class.
 */
public class PathfindingTest {

    /*
     * Text graph format ([weight] is optional):
     * Directed edge: tailLabel -> headLabel [weight]
     * Undirected edge (so two directed edges in both directions): tailLabel -- headLabel [weight]
     */

    // a small, strongly-connected graph consisting of three vertices and four directed edges
    public static final String graph1 = """
            A -> B 2
            A -- C 6
            B -> C 3
            """;

    // Example graph
    public static final String graph2 = """
            A -> B 9
            A -> C 14
            A -> D 15
            B -> E 23
            C -> E 17
            C -> D 5
            C -> F 30
            D -> F 20
            D -> G 37
            E -> F 3
            E -> G 20
            F -> G 16""";

    @DisplayName("WHEN we compute the `pathInfo` from a vertex `v`, THEN it includes a correct "
            + "entry for each vertex `w` reachable along a non-backtracking path from `v`.")
    @Nested
    class pathInfoTest {

        // Recall that "strongly connected" describes a graph that includes a (directed) path from
        // any vertex to any other vertex
        @DisplayName("In a strongly connected graph with no `previousEdge`.")
        @Test
        void testStronglyConnectedNoPrevious() {
            SimpleGraph g = SimpleGraph.fromText(graph1);
            SimpleVertex va = g.getVertex("A");
            SimpleVertex vb = g.getVertex("B");
            SimpleVertex vc = g.getVertex("C");

            // compute paths from source vertex "A"
            Map<SimpleVertex, PathEnd<SimpleEdge>> paths = Pathfinding.pathInfo(va, null);
            assertEquals(3, paths.size()); // all vertices are reachable
            assertEquals(0, paths.get(va).distance());
            // since the shortest path A -> A is empty, we can't assert anything about its last edge
            assertEquals(2, paths.get(vb).distance());
            assertEquals(g.getEdge(va, vb), paths.get(vb).lastEdge());
            assertEquals(5, paths.get(vc).distance());
            assertEquals(g.getEdge(vb, vc), paths.get(vc).lastEdge());

            // compute paths from source vertex "B"
            paths = Pathfinding.pathInfo(vb, null);
            assertEquals(3, paths.size()); // all vertices are reachable
            assertEquals(9, paths.get(va).distance());
            assertEquals(g.getEdge(vc, va), paths.get(va).lastEdge());
            assertEquals(0, paths.get(vb).distance());
            assertEquals(3, paths.get(vc).distance());
            assertEquals(g.getEdge(vb, vc), paths.get(vc).lastEdge());

            // compute paths from source vertex "C"
            paths = Pathfinding.pathInfo(vc, null);
            assertEquals(3, paths.size()); // all vertices are reachable
            assertEquals(6, paths.get(va).distance());
            assertEquals(g.getEdge(vc, va), paths.get(va).lastEdge());
            assertEquals(8, paths.get(vb).distance());
            assertEquals(g.getEdge(va, vb), paths.get(vb).lastEdge());
            assertEquals(0, paths.get(vc).distance());
        }

        @DisplayName("In a graph that is *not* strongly connected and `pathInfo` is computed "
                + "starting from a vertex that cannot reach all other vertices.")
        @Test
        void testNotStronglyConnected() {
            SimpleGraph g = SimpleGraph.fromText("B -> A 2");
            SimpleVertex va = g.getVertex("A");
            SimpleVertex vb = g.getVertex("B");

            Map<SimpleVertex, PathEnd<SimpleEdge>> paths = Pathfinding.pathInfo(va, null);
            assertEquals(1, paths.size()); // only va is reachable
            assertTrue(paths.containsKey(va));
            assertFalse(paths.containsKey(vb));
        }

        @DisplayName("In a strongly connected graph with a `previousEdge` that prevents some vertex "
                + "from being reached.")
        @Test
        void testStronglyConnectedPreviousPreventsReaching() {
            SimpleGraph g = SimpleGraph.fromText("""
                    A -> B 1
                    B -> A 1
                    A -> C 1
                    C -> B 1
                    """);
            SimpleVertex va = g.getVertex("A");
            SimpleVertex vb = g.getVertex("B");
            SimpleVertex vc = g.getVertex("C");

            Map<SimpleVertex, PathEnd<SimpleEdge>> paths = Pathfinding.pathInfo(vb, g.getEdge(va, vb));
            assertEquals(1, paths.size()); // only the source remains reachable
            assertTrue(paths.containsKey(vb));
            assertFalse(paths.containsKey(va));
            assertFalse(paths.containsKey(vc));
        }

        @DisplayName("In a graph where the shortest path with backtracking is shorter than the "
                + "shortest non-backtracking path.")
        @Test
        void testBacktrackingShorter() {
            SimpleGraph g = SimpleGraph.fromText("""
                    A -> B 1
                    B -> A 1
                    A -> C 1
                    B -> C 10
                    """);
            SimpleVertex va = g.getVertex("A");
            SimpleVertex vb = g.getVertex("B");
            SimpleVertex vc = g.getVertex("C");

            Map<SimpleVertex, PathEnd<SimpleEdge>> paths = Pathfinding.pathInfo(vb, g.getEdge(va, vb));
            assertEquals(0, paths.get(vb).distance());
            assertEquals(10, paths.get(vc).distance());
            assertEquals(g.getEdge(vb, vc), paths.get(vc).lastEdge());
        }

        @DisplayName("In a graph where some shortest non-backtracking path includes at least 3 edges.")
        @Test
        void testLongerPaths() {
            SimpleGraph g = SimpleGraph.fromText("""
                    A -> B 1
                    B -> C 1
                    C -> D 1
                    A -> D 10
                    """);
            SimpleVertex va = g.getVertex("A");
            SimpleVertex vb = g.getVertex("B");
            SimpleVertex vc = g.getVertex("C");
            SimpleVertex vd = g.getVertex("D");

            Map<SimpleVertex, PathEnd<SimpleEdge>> paths = Pathfinding.pathInfo(va, null);
            assertEquals(0, paths.get(va).distance());
            assertEquals(1, paths.get(vb).distance());
            assertEquals(2, paths.get(vc).distance());
            assertEquals(3, paths.get(vd).distance());
            assertEquals(g.getEdge(vc, vd), paths.get(vd).lastEdge());
        }
    }

    /**
     * Ensures `pathEdges` is a well-formed path: the `dst` of each edge equals the `src` of the
     * subsequent edge, and that the ordered list of all vertices in the path equals
     * `expectedVertices`. Requires `path` is non-empty.
     */
    private void assertPathVertices(List<String> expectedVertices, List<SimpleEdge> pathEdges) {
        ArrayList<String> pathVertices = new ArrayList<>();
        pathVertices.add(pathEdges.getFirst().tail().label);
        for (SimpleEdge e : pathEdges) {
            assertEquals(pathVertices.getLast(), e.tail().label);
            pathVertices.add(e.head().label);
        }
        assertIterableEquals(expectedVertices, pathVertices);
    }

    @DisplayName("WHEN a weighted, directed graph is given, THEN `shortestNonBacktracking` returns "
            + "the list of edges in the shortest non-backtracking path from a `src` vertex to a "
            + "`dst` vertex, or null if no such path exists.")
    @Nested
    class testShortestNonBacktrackingPath {

        @DisplayName("When the shortest non-backtracking path consists of multiple edges.")
        @Test
        void testLongPath() {
            SimpleGraph g = SimpleGraph.fromText(graph2);
            List<SimpleEdge> path = Pathfinding.shortestNonBacktrackingPath(g.getVertex("A"),
                    g.getVertex("G"), null);
            assertNotNull(path);
            assertPathVertices(Arrays.asList("A", "C", "E", "F", "G"), path);
        }

        @DisplayName("When the shortest non-backtracking path consists of a single edge.")
        @Test
        void testOneEdgePath() {
            SimpleGraph g = SimpleGraph.fromText(graph1);
            List<SimpleEdge> path = Pathfinding.shortestNonBacktrackingPath(g.getVertex("A"),
                    g.getVertex("B"), null);
            assertNotNull(path);
            assertEquals(1, path.size());
            assertPathVertices(Arrays.asList("A", "B"), path);
        }

        @DisplayName("Path is empty when `src` and `dst` are the same.")
        @Test
        void testEmptyPath() {
            SimpleGraph g = SimpleGraph.fromText(graph1);
            List<SimpleEdge> path = Pathfinding.shortestNonBacktrackingPath(g.getVertex("A"),
                    g.getVertex("A"), null);
            assertNotNull(path);
            assertTrue(path.isEmpty());
        }

        @DisplayName("Path is null when there is not a path from `src` to `dst` (even without "
                + "accounting for back-tracking.")
        @Test
        void testNoPath() {
            SimpleGraph g = SimpleGraph.fromText("B -> A 2");
            List<SimpleEdge> path = Pathfinding.shortestNonBacktrackingPath(g.getVertex("A"),
                    g.getVertex("B"), null);
            assertNull(path);
        }

        @DisplayName("Path is null when the non-backtracking condition prevents finding a path "
                + "from `src` to `dst`.")
        @Test
        void testNonBacktrackingPreventsPath() {
            SimpleGraph g = SimpleGraph.fromText("A -- B 1");
            List<SimpleEdge> path = Pathfinding.shortestNonBacktrackingPath(g.getVertex("B"),
                    g.getVertex("A"), g.getEdge(g.getVertex("A"), g.getVertex("B")));
            assertNull(path);
        }

        @DisplayName("When the graph includes multiple shortest paths from `src` to `dst`, one of "
                + "them is returned")
        @Test
        void testMultipleShortestPaths() {
            SimpleGraph g = SimpleGraph.fromText("""
                    A -> B 1
                    B -> D 1
                    A -> C 1
                    C -> D 1
                    """);
            List<SimpleEdge> path = Pathfinding.shortestNonBacktrackingPath(g.getVertex("A"),
                    g.getVertex("D"), null);
            assertNotNull(path);
            assertEquals(2, path.size());

            List<String> pathVertices = List.of(path.getFirst().tail().label(), path.getFirst().head().label(),
                    path.getLast().head().label());
            assertTrue(pathVertices.equals(Arrays.asList("A", "B", "D"))
                    || pathVertices.equals(Arrays.asList("A", "C", "D")));
        }

        @DisplayName("Immediate backtracking is forbidden, but returning later by a longer route is allowed.")
        @Test
        void testBacktrackingOnlyBlocksImmediateReverse() {
            SimpleGraph g = SimpleGraph.fromText("""
                    A -> B 1
                    B -> A 1
                    B -> C 1
                    C -> A 1
                    """);
            List<SimpleEdge> path = Pathfinding.shortestNonBacktrackingPath(g.getVertex("B"),
                    g.getVertex("A"), g.getEdge(g.getVertex("A"), g.getVertex("B")));
            assertNotNull(path);
            assertPathVertices(Arrays.asList("B", "C", "A"), path);
        }

        @DisplayName("The path from a vertex to itself is empty even when a previous edge is provided.")
        @Test
        void testEmptyPathWithPreviousEdge() {
            SimpleGraph g = SimpleGraph.fromText("A -- B 1");
            List<SimpleEdge> path = Pathfinding.shortestNonBacktrackingPath(g.getVertex("B"),
                    g.getVertex("B"), g.getEdge(g.getVertex("A"), g.getVertex("B")));
            assertNotNull(path);
            assertTrue(path.isEmpty());
        }
    }

}
