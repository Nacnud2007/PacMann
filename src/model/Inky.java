package model;

import graph.MazeGraph.IPair;
import graph.MazeGraph.MazeVertex;
import java.awt.Color;

public class Inky extends Ghost {

    /**
     * Constructs Inky with cyan color and enters after a delay of 6 seconds.
     */
    public Inky(GameModel model) {
        super(model, Color.CYAN, 6000);
    }

    @Override
    protected MazeVertex target() {
        if (state == GhostState.CHASE) {
            MazeVertex pac = model.pacMann().nearestVertex();
            IPair loc1 = pac.loc();
            MazeVertex blinkyVertex = model.blinky().nearestVertex();
            IPair loc2 = blinkyVertex.loc();
            int i1 = loc1.i();
            int j1 = loc1.j();
            int i2 = loc2.i();
            int j2 = loc2.j();

            int i3 = 2 * i1 - i2;
            int j3 = 2 * j1 - j2;
            return model.graph().closestTo(i3, j3);

        } else {
            return model.graph().closestTo(2, model.height() - 3);
        }
    }
}
