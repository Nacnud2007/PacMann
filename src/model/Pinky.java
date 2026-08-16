package model;

import graph.MazeGraph.Direction;
import graph.MazeGraph.IPair;
import graph.MazeGraph.MazeVertex;
import java.awt.Color;

public class Pinky extends Ghost {

    /**
     * Constructs Pinky with the color pink and a four-second delay.
     */
    public Pinky(GameModel model) {
        super(model, Color.PINK, 4000);
    }

    @Override
    protected MazeVertex target() {
        if (state == GhostState.CHASE) {
            MazeVertex pac = model.pacMann().nearestVertex();
            IPair loc = pac.loc();
            int i = loc.i();
            int j = loc.j();
            Direction dir = model.pacMann().currentEdge().direction();

            switch (dir) {
                case LEFT -> i -= 4;
                case RIGHT -> i += 4;
                case UP -> j -= 4;
                case DOWN -> j += 4;
            }

            return model.graph().closestTo(i, j);
        } else {
            return model.graph().closestTo(model.width() - 3, 2);
        }
    }
}
