package model;

import graph.MazeGraph.IPair;
import graph.MazeGraph.MazeVertex;
import java.awt.Color;
import java.util.Random;

public class Clyde extends Ghost {
    private final Random rand;

    /**
     * Constructs an instance of Clyde with color orange and delay time 8 seconds.
     */
    public Clyde(GameModel model, Random rand) {
        super(model, Color.ORANGE, 8000);
        this.rand = rand;
    }

    @Override
    protected MazeVertex target() {
        MazeVertex clydeVertex = nearestVertex();
        MazeVertex pacMannVertex= model.pacMann().nearestVertex();
        IPair loc1 = clydeVertex.loc();
        IPair loc2 = pacMannVertex.loc();
        double distance = Math.pow(Math.pow((loc1.i() - loc2.i()), 2)
                + Math.pow((loc1.j() - loc2.j()), 2), 0.5);
        if (state == GhostState.CHASE) {
            if (distance >= 10) {
                return model.pacMann().nearestVertex();
            } else {
                int i = rand.nextInt(model.width());
                int j = rand.nextInt(model.height());
                return model.graph().closestTo(i, j);
            }
        } else {
            return model.graph().closestTo(model.width() - 3, model.height() - 3);
        }
    }
}
