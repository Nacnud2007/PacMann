package model;


import graph.MazeGraph.MazeVertex;
import java.awt.Color;

public class Blinky extends Ghost {

    /**
     * Construct BLINKY colored red with a 2 second delay
     *
     */
    public Blinky(GameModel model) {
        super(model, Color.RED, 2000);
    }

    /**
     * Returns the vertex where Blinky is targeting. In the FLEE state, it is the vertex (2,2)
     * in the CHASE state, Blinky targets PacMann's nearest vertex.
     */
    @Override
    protected MazeVertex target() {
        if (state == GhostState.CHASE) {
            return model.pacMann().nearestVertex();
        } else {
            return model.graph().closestTo(2,2);
        }
    }
}
