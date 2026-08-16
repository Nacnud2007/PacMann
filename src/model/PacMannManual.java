package model;

import graph.MazeGraph.Direction;
import graph.MazeGraph.MazeEdge;
import graph.MazeGraph.MazeVertex;

public class PacMannManual extends PacMann{
    private final GameModel model;

    public PacMannManual(GameModel model) {
        super(model);
        this.model = model;
    }

    @Override
    public MazeEdge nextEdge() {
        MazeVertex currentVertex = this.nearestVertex();

        Direction command = model.playerCommand();
        MazeEdge commandedEdge = currentVertex.edgeInDirection(command);
        if (commandedEdge != null) {
            return commandedEdge;
        }

        Direction currentDir = this.currentEdge().direction();
        MazeEdge continuingEdge = currentVertex.edgeInDirection(currentDir);

        if (continuingEdge != null) {
            return continuingEdge;
        }

        return null;
    }


}
