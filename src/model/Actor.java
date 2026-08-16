package model;

import java.util.List;
import graph.MazeGraph.Direction;
import graph.MazeGraph.MazeEdge;
import graph.MazeGraph.IPair;
import graph.MazeGraph.MazeVertex;

/**
 * A character in the game, which can store and update its location on the game graph and interact
 * with the game model based on its location.  Subclasses are responsible for that actor's vertex
 * visitation behavior and navigation.
 */
public abstract class Actor {

    /**
     * Models the current location of an Actor, which consists of the `edge` that they are currently
     * traversing / have just finished traversing, as well as the `progress` of that edge's traversal
     * which is a number between 0 and 1 (inclusive): 0 denotes the start of the edge and 1 denotes
     * the end of the edge.
     */
    public record Location(MazeEdge edge, double progress) {

        /**
         * Return the nearest vertex to this actor, depending on their progress along the current
         * edge. When their progress is less than 0.5, the `edge.tail()` is returned. Otherwise,
         * `edge.head()` is returned.
         */
        public MazeVertex nearestVertex() {
            return (progress < 0.5) ? edge.tail() : edge.head();
        }

        /**
         * Return a new Location object reflecting the given amount of progress being made along the
         * current edge.
         */
        public Location progressed(double deltaProgress) {
            // Constructing a new Location object is required because of immutability
            return new Location(edge, Math.clamp(progress + deltaProgress, 0.0, 1.0));
        }

        /**
         * Determines whether this location and the given `other` location have collided.
         */
        public boolean collidesWith(Location other) {
            final double tol = 1e-6;
            if (edge.equals(other.edge)) {
                return Math.abs(progress - other.progress) < tol;
            } else if (edge.tail().equals(other.edge.head()) && edge.head().equals(other.edge.tail())) {
                return Math.abs(progress - (1 - other.progress)) < tol;
            } else if (atVertex() && other.atVertex()) {
                return nearestVertex().equals(other.nearestVertex());
            } else {
                return false;
            }
        }

        /**
         * Return whether this actor's location represents standing atop a vertex (which is true
         * when progress is 0 or 1).
         */
        public boolean atVertex() {
            return progress == 0 || progress == 1;
        }

        /**
         * Return a new Location object reflecting the actor having turned around at the same
         * position on the board.
         */
        public Location reversed() {
            return new Location(edge.reverse(), 1.0 - progress);
        }
    }


    /**
     * The game model with which this actor is associated.
     */
    protected final GameModel model;

    /**
     * The current location of this actor, modeled as a record of the edge that they are currently
     * traversing and their relative progress along this edge.
     */
    protected Location location;

    /**
     * Construct a new actor associated with the given `model`.
     */
    public Actor(GameModel model) {
        this.model = model;
    }

    /* ****************************************************************
     * Abstract Actor methods                                         *
     **************************************************************** */

    /**
     * Update the model based on this actor's arrival at vertex v.
     */
    public abstract void visitVertex(MazeVertex v);

    /**
     * Returns the next edge that this actor will traverse in the game graph. Navigation strategy is
     * delegated to the subclass.  Will only be called when this actor is standing on a vertex,
     * which must equal the returned edge's tail.
     */
    public abstract MazeEdge nextEdge();

    /**
     * Return the list of Edges in this actor's current planned path through the maze (if any). This
     * is intended to be used for visualizing actors' intended behavior.  Sequence of edges must
     * form a continuous path (e.g., `ans.get(i).head().equals(ans.get(i+1).tail())`).
     */
    public abstract List<MazeEdge> guidancePath();

    /**
     * Resets this actor to its state at the start of a life.
     */
    public abstract void reset();

    /**
     * Return the current base speed of this actor, before factoring in the weight of the edge they
     * are traversing.
     */
    public abstract double baseSpeed();

    /* ****************************************************************
     * Methods that report the current position of this actor.        *
     **************************************************************** */

    /**
     * Return Location object summarizing this actor's current location.
     */
    public final Location location() {
        return location;
    }

    /**
     * Return the current edge that this actor is traversing.
     */
    public final MazeEdge currentEdge() {
        return location.edge();
    }

    /**
     * Return the closest vertex to this actor's current position, which will be either
     * `currentEdge.tail()` or `currentEdge.head()`, depending on `distanceAlongEdge`.
     */
    public final MazeVertex nearestVertex() {
        return location.nearestVertex();
    }



    /* ****************************************************************
     * Methods for updating this actor's location                     *
     **************************************************************** */

    /**
     * Return the amount of game time (in ms) that this actor is able to travel at its current speed
     * before reaching a vertex or changing state.  Return POSITIVE_INFINITY if this actor is
     * standing still and has no anticipated state changes.
     */
    public double maxPropagationTime() {
        if (location.progress() < 1) {
            return (1.0 - location.progress()) / edgeSpeed();
        } else {
            return Double.POSITIVE_INFINITY;
        }
    }

    /**
     * Update this actor's location and state by propagating its current motion along its edge by
     * `dt` ms of game time.  If `dt` is sufficient for the actor to traverse the remainder of its
     * edge, it will stop at that edge's destination vertex.
     */
    public void propagate(double dt) {
        double edgeDistance = edgeSpeed() * dt;
        location = location.progressed(edgeDistance);
    }

    /**
     * Set this actor's location to the start of `newEdge`.  May only be called when this actor is
     * standing on a vertex, which must equal `newEdge`'s source.
     */
    public final void traverseEdge(MazeEdge newEdge) {
        assert newEdge.tail().equals(nearestVertex());
        location = new Location(newEdge, 0);
    }

    /**
     * Return the speed of this actor, which is primarily a function of its `currentEdge`'s weight.
     */
    protected final double edgeSpeed() {
        return baseSpeed() / location.edge().weight();
    }


    /* ****************************************************************
     * Information relevant to a View                                 *
     **************************************************************** */

    /**
     * A record class that represents ordered pairs of doubles
     */
    public record DPair(double i, double j) {

    }

    /**
     * Return the upper left corner of this actor's bounding box. When an actor is positioned
     * directly atop a vertex (i.e., `distanceAlongEdge` is 0 or 1), this is 0.25 tiles NW
     * (northwest) of the NW corner of that vertex's tile. Otherwise, `distanceAlongEdge` is used to
     * linearly interpolate the bounding box position between `currentEdge.tail()` and
     * `currentEdge.head()` (accounting for tunnels).
     */
    public DPair getBoundingBoxUL() {
        MazeEdge currentEdge = location.edge();
        IPair loc = currentEdge.tail().loc();
        double x = loc.i() - 0.25;
        double y = loc.j() - 0.25;

        double distanceAlongEdge = location.progress();
        return switch (currentEdge.direction()) {
            case Direction.LEFT -> new DPair(x - distanceAlongEdge, y);
            case Direction.RIGHT -> new DPair(x + distanceAlongEdge, y);
            case Direction.UP -> new DPair(x, y - distanceAlongEdge);
            case Direction.DOWN -> new DPair(x, y + distanceAlongEdge);
        };
    }
}
