package graph;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import graph.MazeGraph.Direction;
import graph.MazeGraph.MazeEdge;
import graph.MazeGraph.IPair;
import graph.MazeGraph.MazeVertex;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import util.GameMap;
import util.MazeGenerator.TileType;

public class MazeGraphTest {

    /* Note, to conform to the precondition of the `MazeGraph` constructor, make sure that any
     * TileType arrays that you construct contain a `PATH` tile at index [2][2] and represent a
     * single, orthogonally connected component of `PATH` tiles. */

    /**
     * Create a game map with tile types corresponding to the letters on each line of `template`.
     * 'w' = WALL, 'p' = PATH, and 'g' = GHOSTBOX.  The letters of `template` must form a rectangle.
     * Elevations will be a gradient from the top-left to the bottom-right corner with a horizontal
     * slope of 2 and a vertical slope of 1.
     */
    GameMap createMap(String template) {
        Scanner lines = new Scanner(template);
        ArrayList<ArrayList<TileType>> lineLists = new ArrayList<>();

        while (lines.hasNextLine()) {
            ArrayList<TileType> lineList = new ArrayList<>();
            for (char c : lines.nextLine().toCharArray()) {
                switch (c) {
                    case 'w' -> lineList.add(TileType.WALL);
                    case 'p' -> lineList.add(TileType.PATH);
                    case 'g' -> lineList.add(TileType.GHOSTBOX);
                }
            }
            lineLists.add(lineList);
        }

        int height = lineLists.size();
        int width = lineLists.getFirst().size();

        TileType[][] types = new TileType[width][height];
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                types[i][j] = lineLists.get(j).get(i);
            }
        }

        double[][] elevations = new double[width][height];
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                elevations[i][j] = (2.0 * i + j);
            }
        }
        return new GameMap(types, elevations);
    }

    @DisplayName("WHEN a GameMap with exactly one path tile in position [2][2] is passed into the "
            + "MazeGraph constructor, THEN a graph with one vertex is created.")
    @Test
    void testOnePathCell() {
        GameMap map = createMap("""
                wwwww
                wwwww
                wwpww
                wwwww
                wwwww""");
        MazeGraph graph = new MazeGraph(map);
        Map<IPair, MazeVertex> vertices = new HashMap<>();
        graph.vertices().forEach(v -> vertices.put(v.loc(), v));

        assertEquals(1, vertices.size());
        assertTrue(vertices.containsKey(new IPair(2, 2)));
    }

    @DisplayName("WHEN a GameMap with exactly two horizontally adjacent path tiles is passed into "
            + "the MazeGraph constructor, THEN a graph with two vertices is created in which the two "
            + "vertices are connected by two directed edges with weights determined by evaluating "
            + "`MazeGraph.edgeWeight` on their elevations.")
    @Test
    void testTwoPathCellsHorizontal() {
        GameMap map = createMap("""
                wwwww
                wwwww
                wwppw
                wwwww
                wwwww""");
        MazeGraph graph = new MazeGraph(map);
        Map<IPair, MazeVertex> vertices = new HashMap<>();
        graph.vertices().forEach(v -> vertices.put(v.loc(), v));

        // graph contains two vertices with the correct locations
        assertEquals(2, vertices.size());
        IPair left = new IPair(2, 2);
        IPair right = new IPair(3, 2);
        assertTrue(vertices.containsKey(left));
        assertTrue(vertices.containsKey(right));

        MazeVertex vl = vertices.get(left);
        MazeVertex vr = vertices.get(right);

        // left vertex has one edge to the vertex to its right
        assertNull(vl.edgeInDirection(Direction.LEFT));
        assertNull(vl.edgeInDirection(Direction.UP));
        assertNull(vl.edgeInDirection(Direction.DOWN));
        MazeEdge l2r = vl.edgeInDirection(Direction.RIGHT);
        assertNotNull(l2r);

        // edge from left to right has the correct fields
        double lElev = map.elevations()[2][2];
        double rElev = map.elevations()[3][2];
        assertEquals(vl, l2r.tail());
        assertEquals(vr, l2r.head());
        assertEquals(Direction.RIGHT, l2r.direction());
        assertEquals(MazeGraph.edgeWeight(lElev, rElev), l2r.weight());

        // right vertex has one edge to the vertex to its left with the correct fields
        assertNull(vr.edgeInDirection(Direction.RIGHT));
        assertNull(vr.edgeInDirection(Direction.UP));
        assertNull(vr.edgeInDirection(Direction.DOWN));
        MazeEdge r2l = vr.edgeInDirection(Direction.LEFT);
        assertNotNull(r2l);
        assertEquals(vr, r2l.tail());
        assertEquals(vl, r2l.head());
        assertEquals(Direction.LEFT, r2l.direction());
        assertEquals(MazeGraph.edgeWeight(rElev, lElev), r2l.weight());
    }


    @DisplayName("WHEN a GameMap with exactly two vertically adjacent path tiles is passed into "
            + "the MazeGraph constructor, THEN a graph with two vertices is created in which the "
            + "two vertices are connected by two directed edges with weights determined by "
            + "evaluating `MazeGraph.edgeWeight` on their elevations.")
    @Test
    void testTwoPathCellsVertical() {
        GameMap map = createMap("""
            wwwww
            wwwww
            wwpww
            wwpww
            wwwww""");
        MazeGraph graph = new MazeGraph(map);
        Map<IPair, MazeVertex> vertices = new HashMap<>();
        graph.vertices().forEach(v -> vertices.put(v.loc(), v));

        IPair top = new IPair(2, 2);
        IPair bottom = new IPair(2, 3);

        MazeVertex vt = vertices.get(top);
        MazeVertex vb = vertices.get(bottom);

        // Test Top-to-Bottom edge
        MazeEdge t2b = vt.edgeInDirection(Direction.DOWN);
        assertNotNull(t2b);
        assertEquals(vb, t2b.head());
        assertEquals(MazeGraph.edgeWeight(map.elevations()[2][2], map.elevations()[2][3]), t2b.weight());

        // Test Bottom-to-Top edge
        MazeEdge b2t = vb.edgeInDirection(Direction.UP);
        assertNotNull(b2t);
        assertEquals(vt, b2t.head());
        assertEquals(MazeGraph.edgeWeight(map.elevations()[2][3], map.elevations()[2][2]), b2t.weight());
    }



    @DisplayName("WHEN a GameMap includes two path tiles in the first and last column of the same "
            + "row, THEN (tunnel) edges are created between these tiles with the correct properties.")
    @Test
    void testHorizontalTunnelEdgeCreation() {
        // A 5-wide connected path row where the endpoints form a horizontal tunnel
        GameMap map = createMap("""
            wwwww
            wwwww
            ppppp
            wwwww
            wwwww""");
        MazeGraph graph = new MazeGraph(map);
        Map<IPair, MazeVertex> vertices = new HashMap<>();
        graph.vertices().forEach(v -> vertices.put(v.loc(), v));

        MazeVertex vLeft = vertices.get(new IPair(0, 2));
        MazeVertex vRight = vertices.get(new IPair(4, 2));

        MazeEdge tunnelWest = vLeft.edgeInDirection(Direction.LEFT);
        assertNotNull(tunnelWest, "Should have a LEFT edge that wraps to the right side.");
        assertEquals(vRight, tunnelWest.head(), "LEFT from col 0 should lead to col 4.");

        MazeEdge tunnelEast = vRight.edgeInDirection(Direction.RIGHT);
        assertNotNull(tunnelEast, "Should have a RIGHT edge that wraps to the left side.");
        assertEquals(vLeft, tunnelEast.head(), "RIGHT from col 4 should lead to col 0.");
    }

    @DisplayName("WHEN a GameMap includes a cyclic connected component of path tiles with a "
            + "non-path tile in the middle, THEN its graph includes edges between all adjacent "
            + "pairs of vertices.")
    @Test
    void testCyclicPaths() {
        GameMap map = createMap("""
                wwwwwww
                wwwwwww
                wwpppww
                wwpwpww
                wwpppww
                wwwwwww""");
        MazeGraph graph = new MazeGraph(map);
        Map<IPair, MazeVertex> vertices = new HashMap<>();
        graph.vertices().forEach(v -> vertices.put(v.loc(), v));

        // Test the vertex just above the center wall [3, 2]
        MazeVertex topMid = vertices.get(new IPair(3, 2));
        assertNotNull(topMid.edgeInDirection(Direction.LEFT));  // Connects to [2,2]
        assertNotNull(topMid.edgeInDirection(Direction.RIGHT)); // Connects to [4,2]

        // Should not connect down because [3,3] is a WALL
        assertNull(topMid.edgeInDirection(Direction.DOWN), "Should not connect to center wall.");    }

    @Test
    @DisplayName("A path with only one neighbor should only have one outgoing edge.")
    void testDeadEnd() {
        GameMap map = createMap("""
            wwwww
            wwwww
            wwppw
            wwwww
            wwwww""");
        MazeGraph graph = new MazeGraph(map);
        // The tile at [2,2] only has a neighbor to its RIGHT.
        MazeVertex deadEnd = null;
        for(MazeVertex v : graph.vertices()) if(v.loc().i() == 2) deadEnd = v;

        assertNotNull(deadEnd.edgeInDirection(Direction.RIGHT));
        assertNull(deadEnd.edgeInDirection(Direction.LEFT));
        assertNull(deadEnd.edgeInDirection(Direction.UP));
        assertNull(deadEnd.edgeInDirection(Direction.DOWN));
    }

    @Test
    @DisplayName("Paths at the very top and very bottom of a column should connect via tunnel.")
    void testVerticalTunnel() {
        GameMap map = createMap("""
            wwpww
            wwpww
            wwpww
            wwpww
            wwpww""");
        MazeGraph graph = new MazeGraph(map);
        Map<IPair, MazeVertex> vertices = new HashMap<>();
        graph.vertices().forEach(v -> vertices.put(v.loc(), v));

        MazeVertex top = vertices.get(new IPair(2, 0));
        MazeVertex bottom = vertices.get(new IPair(2, 4));

        // UP from the top should wrap to the bottom
        assertEquals(bottom, top.edgeInDirection(Direction.UP).head());
        // DOWN from the bottom should wrap to the top
        assertEquals(top, bottom.edgeInDirection(Direction.DOWN).head());
    }

    @Test
    @DisplayName("WHEN elevations differ by more than 0.25, THEN the weight is correctly clamped.")
    void testElevationClamping() {
        // Create a map where p1 is at (2,2) and p2 is at (3,2); elevation is (2.0 * i + j)
        // [2,2] elev = 6.0 | [3,2] elev = 8.0. Diff is 2.0
        GameMap map = createMap("""
            wwwww
            wwwww
            wwppw
            wwwww
            wwwww""");
        MazeGraph graph = new MazeGraph(map);

        MazeVertex v22 = graph.closestTo(2, 2);
        MazeEdge edge = v22.edgeInDirection(Direction.RIGHT);

        // The diff (8-6) is 2.0, but clamp limits it to 0.25.
        assertEquals(1.75, edge.weight(), 1e-6, "Weight should be clamped even for large elevation gaps.");
    }

    @Test
    @DisplayName("A T-junction vertex should have exactly three edges and no diagonal connections.")
    void testTJunction() {
        GameMap map = createMap("""
            wwwww
            wwpww
            wpppw
            wwwww
            wwwww""");
        MazeGraph graph = new MazeGraph(map);
        Map<IPair, MazeVertex> vertices = new HashMap<>();
        graph.vertices().forEach(v -> vertices.put(v.loc(), v));

        // The center at [2,2]
        MazeVertex center = vertices.get(new IPair(2, 2));

        // Should have UP, LEFT, RIGHT, NOT DOWN
        assertNotNull(center.edgeInDirection(Direction.UP));
        assertNotNull(center.edgeInDirection(Direction.LEFT));
        assertNotNull(center.edgeInDirection(Direction.RIGHT));
        assertNull(center.edgeInDirection(Direction.DOWN));

        int edgeCount = 0;
        for (MazeEdge e : center.outgoingEdges()) { edgeCount++; }
        assertEquals(3, edgeCount, "Center of a T-junction should have exactly 3 outgoing edges.");
    }
}
