package model;

import graph.MazeGraph.IPair;
import graph.MazeGraph.MazeEdge;
import graph.MazeGraph.MazeVertex;
import graph.Pathfinding;
import java.util.ArrayList;
import java.util.List;
import model.GameModel.Item;
import model.Ghost.GhostState;

public class PacMannAI extends PacMann {

    /**
     * Doubles that store variables that help calculate where to move next
     */
    private static final int SEARCH_DEPTH = 2;
    private static final double MOVE_COST = 8.0;
    private static final double REVISIT_PENALTY = 250.0;
    private static final double PELLET_REWARD = 260.0;
    private static final double LATE_PELLET_REWARD = 520.0;
    private static final double DOT_REWARD = 70.0;
    private static final double EMPTY_VERTEX_PENALTY = 30.0;
    private static final double LATE_EMPTY_VERTEX_PENALTY = 45.0;
    private static final double NEAR_CHASE_PENALTY = 2500.0;
    private static final double CHASE_DISTANCE_THRESHOLD = 4.0;
    private static final double CHASE_DISTANCE_WEIGHT = 180.0;
    private static final double FLEE_GHOST_REWARD = 180.0;
    private static final double PELLET_PRIORITY_TIME = 30000.0;
    private static final double FOOD_FINISH_TIME = 60000.0;
    private List<MazeEdge> plannedPath;

    /**
     * Constructor for PacMannAI. Uses super from PacMann.
     */
    public PacMannAI(GameModel model) {
        super(model);
        plannedPath = List.of();
    }

    /**
     * Choose the first edge of the highest-scoring path found by this AI's lookahead search.
     */
    @Override
    public MazeEdge nextEdge() {
        MazeVertex current = nearestVertex();
        MazeEdge previousEdge = (location.progress() == 1.0) ? currentEdge() : null;

        if (model.time() >= FOOD_FINISH_TIME) {
            MazeEdge nearestFoodEdge = nearestFoodEdge(current, previousEdge);
            if (nearestFoodEdge != null) {
                plannedPath = List.of(nearestFoodEdge);
                return nearestFoodEdge;
            }
        }

        MazeEdge bestEdge = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        List<MazeEdge> bestPath = List.of();

        for (MazeEdge edge : current.outgoingEdges()) {
            if (isImmediateBacktrack(edge, previousEdge)) {
                continue;
            }

            List<MazeEdge> candidatePath = new ArrayList<>();
            candidatePath.add(edge);
            double score = edgeScore(edge)
                    + search(edge.head(), edge, SEARCH_DEPTH - 1, candidatePath);

            if (score > bestScore) {
                bestScore = score;
                bestEdge = edge;
                bestPath = List.copyOf(candidatePath);
            }
        }

        plannedPath = bestPath;
        return bestEdge;
    }

    /**
     * Recursively evaluate future paths up to `depth` moves ahead and return the best score found.
     */
    private double search(MazeVertex current, MazeEdge previousEdge, int depth,
            List<MazeEdge> pathSoFar) {
        double bestContinuation = evaluateVertex(current);

        if (depth == 0) {
            return bestContinuation;
        }

        for (MazeEdge edge : current.outgoingEdges()) {
            if (isImmediateBacktrack(edge, previousEdge)) {
                continue;
            }

            pathSoFar.add(edge);
            double revisitPenalty = pathRevisitsVertex(pathSoFar, edge.head()) ? REVISIT_PENALTY : 0.0;
            double score = edgeScore(edge) - MOVE_COST - revisitPenalty
                    + search(edge.head(), edge, depth - 1, pathSoFar);
            pathSoFar.removeLast();
            bestContinuation = Math.max(bestContinuation, score);
        }

        return bestContinuation;
    }

    /**
     * In late game, route directly toward the nearest remaining food instead of relying only on
     * heuristic lookahead.
     */
    private MazeEdge nearestFoodEdge(MazeVertex current, MazeEdge previousEdge) {
        List<MazeEdge> bestPath = null;

        for (MazeVertex vertex : model.graph().vertices()) {
            if (model.itemAt(vertex) == Item.NONE) {
                continue;
            }

            List<MazeEdge> path = Pathfinding.shortestNonBacktrackingPath(current, vertex,
                    previousEdge);
            if (path == null) {
                continue;
            }

            if (bestPath == null || path.size() < bestPath.size()) {
                bestPath = path;
            }
        }

        if (bestPath == null || bestPath.isEmpty()) {
            return null;
        }

        plannedPath = bestPath;
        return bestPath.getFirst();
    }

    /**
     * Returns the private score that we give for a given edge.
     */
    private double edgeScore(MazeEdge edge) {
        double score = evaluateVertex(edge.head());

        if (model.itemAt(edge.head()) == Item.PELLET) {
            score += pelletReward();
        } else if (model.itemAt(edge.head()) == Item.DOT) {
            score += dotReward();
        }

        return score;
    }

    /**
     * Assign a heuristic score to a vertex based on food, ghost danger, and escape options.
     */
    private double evaluateVertex(MazeVertex vertex) {
        double score = 0.0;

        Item item = model.itemAt(vertex);
        if (item == Item.PELLET) {
            score += pelletReward();
        } else if (item == Item.DOT) {
            score += dotReward();
        } else {
            score -= emptyVertexPenalty();
        }

        score += ghostScore(vertex);

        int exits = 0;
        for (MazeEdge ignored : vertex.outgoingEdges()) {
            exits += 1;
        }
        score += 4.0 * exits;

        return score;
    }

    /**
     * Estimate how favorable `vertex` is with respect to the current ghost positions and states.
     */
    private double ghostScore(MazeVertex vertex) {
        double score = 0.0;

        for (Ghost ghost : ghosts()) {
            double dist = vertexDistance(vertex, ghost.nearestVertex());

            if (ghost.state() == GhostState.CHASE) {
                if (dist < CHASE_DISTANCE_THRESHOLD) {
                    score -= NEAR_CHASE_PENALTY;
                } else {
                    score -= CHASE_DISTANCE_WEIGHT / dist;
                }
            } else if (ghost.state() == GhostState.FLEE) {
                score += fleeGhostReward() / Math.max(dist, 1.0);
            }
        }

        return score;
    }

    /**
     * Return the pellet reward, increasing it once the game has been running for a while.
     */
    private double pelletReward() {
        return model.time() >= PELLET_PRIORITY_TIME ? LATE_PELLET_REWARD : PELLET_REWARD;
    }

    /**
     * Lower dot reward later so pellets become relatively more attractive.
     */
    private double dotReward() {
        return model.time() >= PELLET_PRIORITY_TIME ? 0.75 * DOT_REWARD : DOT_REWARD;
    }

    /**
     * Return the penalty for empty vertices, increasing it later to discourage wandering.
     */
    private double emptyVertexPenalty() {
        return model.time() >= PELLET_PRIORITY_TIME ? LATE_EMPTY_VERTEX_PENALTY
                : EMPTY_VERTEX_PENALTY;
    }

    /**
     * Reduce ghost-chasing late so the AI focuses on finishing the board before timeout.
     */
    private double fleeGhostReward() {
        return model.time() >= FOOD_FINISH_TIME ? 0.5 * FLEE_GHOST_REWARD : FLEE_GHOST_REWARD;
    }

    /**
     * Return whether taking `edge` would immediately reverse the direction of `previousEdge`.
     */
    private boolean isImmediateBacktrack(MazeEdge edge, MazeEdge previousEdge) {
        return previousEdge != null && edge.head().equals(previousEdge.tail());
    }

    /**
     * Return whether the candidate path has already visited `vertex`, discouraging small loops.
     */
    private boolean pathRevisitsVertex(List<MazeEdge> pathSoFar, MazeVertex vertex) {
        for (MazeEdge edge : pathSoFar) {
            if (edge.tail().equals(vertex)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Compute the Euclidean distance between two maze vertices using their tile coordinates.
     */
    private double vertexDistance(MazeVertex a, MazeVertex b) {
        IPair aLoc = a.loc();
        IPair bLoc = b.loc();
        int di = aLoc.i() - bLoc.i();
        int dj = aLoc.j() - bLoc.j();
        return Math.sqrt(di * di + dj * dj);
    }

    /**
     * Return a list of the four ghosts so the AI can score danger and opportunities uniformly.
     */
    private List<Ghost> ghosts() {
        return List.of(model.blinky(), model.pinky(), model.inky(), model.clyde());
    }

    @Override
    public List<MazeEdge> guidancePath() {
        return plannedPath.isEmpty() ? super.guidancePath() : plannedPath;
    }
}
