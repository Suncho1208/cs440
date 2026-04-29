package src.pas.pacman.routing;


// SYSTEM IMPORTS
import java.util.Collection;
import java.util.ArrayList;
import java.util.Queue;
import java.util.LinkedList;
import java.util.HashSet;
import java.util.Set;

// JAVA PROJECT IMPORTS
import edu.bu.pas.pacman.game.Action;
import edu.bu.pas.pacman.game.Game.GameView;
import edu.bu.pas.pacman.graph.Path;
import edu.bu.pas.pacman.routing.BoardRouter;
import edu.bu.pas.pacman.routing.BoardRouter.ExtraParams;
import edu.bu.pas.pacman.utils.Coordinate;

public class ThriftyBoardRouter
    extends BoardRouter
{

    public static class BoardExtraParams
        extends ExtraParams
    {

    }

    public ThriftyBoardRouter(int myUnitId,
                              int pacmanId,
                              int ghostChaseRadius)
    {
        super(myUnitId, pacmanId, ghostChaseRadius);
    }

    @Override
    public Collection<Coordinate> getOutgoingNeighbors(final Coordinate src,
                                                       final GameView game,
                                                       final ExtraParams params)
    {
        Collection<Coordinate> neighbors = new ArrayList<>();
        for (Action a : Action.values()) {
            if (game.isLegalPacmanMove(src, a)) {
                neighbors.add(src.getNeighbor(a));
            }
        }
        return neighbors;
    }

    @Override
    public Path<Coordinate> graphSearch(final Coordinate src,
                                        final Coordinate tgt,
                                        final GameView game)
    {
        Queue<Path<Coordinate>> queue = new LinkedList<>();
        Set<Coordinate> visited = new HashSet<>();

        Path<Coordinate> startPath = new Path<>(src);
        queue.add(startPath);
        visited.add(src);

        while (!queue.isEmpty()) {
            Path<Coordinate> currentPath = queue.poll();
            Coordinate currentCoord = currentPath.getDestination();

            if (currentCoord.equals(tgt)) {
                return currentPath;
            }

            for (Coordinate neighbor : getOutgoingNeighbors(currentCoord, game, null)) {
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    Path<Coordinate> nextPath = new Path<>(neighbor, 1.0f, currentPath);
                    queue.add(nextPath);
                }
            }
        }
        return null;
    }
}
