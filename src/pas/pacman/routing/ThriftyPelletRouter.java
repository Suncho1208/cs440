package src.pas.pacman.routing;


// SYSTEM IMPORTS
import java.util.Collection;
import java.util.PriorityQueue;
import java.util.HashSet;
import java.util.Set;
import java.util.Comparator;
import java.util.ArrayList;

// JAVA PROJECT IMPORTS
import edu.bu.pas.pacman.game.Action;
import edu.bu.pas.pacman.game.Game.GameView;
import edu.bu.pas.pacman.graph.Path;
import edu.bu.pas.pacman.graph.PelletGraph.PelletVertex;
import edu.bu.pas.pacman.routing.PelletRouter;
import edu.bu.pas.pacman.routing.PelletRouter.ExtraParams;
import edu.bu.pas.pacman.utils.Coordinate;
import edu.bu.pas.pacman.utils.Pair;


public class ThriftyPelletRouter
    extends PelletRouter
{

    // If you want to encode other information you think is useful for planning the order
    // of pellets ot eat besides Coordinates and data available in GameView
    // you can do so here.
    public static class PelletExtraParams
        extends ExtraParams
    {

    }

    // feel free to add other fields here!

    public ThriftyPelletRouter(int myUnitId,
                               int pacmanId,
                               int ghostChaseRadius)
    {
        super(myUnitId, pacmanId, ghostChaseRadius);

        // if you add fields don't forget to initialize them here!
    }

    @Override
    public Collection<PelletVertex> getOutgoingNeighbors(final PelletVertex src,
                                                         final GameView game,
                                                         final ExtraParams params)
    {
        Collection<PelletVertex> neighbors = new ArrayList<>();
        for (Coordinate p : src.getRemainingPelletCoordinates()) {
            neighbors.add(src.removePellet(p));
        }
        return neighbors;
    }

    @Override
    public float getEdgeWeight(final PelletVertex src,
                               final PelletVertex dst,
                               final ExtraParams params)
    {
        return (float) edu.bu.pas.pacman.utils.DistanceMetric.manhattanDistance(src.getPacmanCoordinate(), dst.getPacmanCoordinate());
    }

    @Override
    public float getHeuristic(final PelletVertex src,
                              final GameView game,
                              final ExtraParams params)
    {
        Collection<Coordinate> pellets = src.getRemainingPelletCoordinates();
        if (pellets.isEmpty()) return 0f;

        float maxDist = 0f;
        Coordinate current = src.getPacmanCoordinate();
        for (Coordinate p : pellets) {
            float dist = (float) edu.bu.pas.pacman.utils.DistanceMetric.manhattanDistance(current, p);
            if (dist > maxDist) maxDist = dist;
        }
        return maxDist;
    }

    @Override
    public Path<PelletVertex> graphSearch(final GameView game)
    {
        PriorityQueue<Path<PelletVertex>> frontier = new PriorityQueue<>(new Comparator<Path<PelletVertex>>() {
            @Override
            public int compare(Path<PelletVertex> p1, Path<PelletVertex> p2) {
                return Float.compare(p1.getTrueCost() + p1.getEstimatedPathCostToGoal(), p2.getTrueCost() + p2.getEstimatedPathCostToGoal());
            }
        });
        Set<Pair<Coordinate, Collection<Coordinate>>> visited = new HashSet<>();

        PelletVertex src = new PelletVertex(game);
        Path<PelletVertex> start = new Path<>(src);
        start.setEstimatedPathCostToGoal(getHeuristic(src, game, null));
        frontier.add(start);

        while (!frontier.isEmpty()) {
            Path<PelletVertex> currPath = frontier.poll();
            PelletVertex currV = currPath.getDestination();

            if (currV.getRemainingPelletCoordinates().isEmpty()) {
                return currPath;
            }

            Pair<Coordinate, Collection<Coordinate>> state = new Pair<>(currV.getPacmanCoordinate(), currV.getRemainingPelletCoordinates());
            if (visited.contains(state)) continue;
            visited.add(state);

            for (PelletVertex nextV : getOutgoingNeighbors(currV, game, null)) {
                Path<PelletVertex> nextPath = new Path<>(nextV, getEdgeWeight(currV, nextV, null), currPath);
                nextPath.setEstimatedPathCostToGoal(getHeuristic(nextV, game, null));
                frontier.add(nextPath);
            }
        }
        return null;
    }
}
