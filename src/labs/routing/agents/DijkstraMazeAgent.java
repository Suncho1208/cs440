package src.labs.routing.agents;

// SYSTEM IMPORTS
import edu.bu.labs.routing.Coordinate;
import edu.bu.labs.routing.Direction;
import edu.bu.labs.routing.Path;
import edu.bu.labs.routing.State.StateView;
import edu.bu.labs.routing.Tile;
import edu.bu.labs.routing.agents.MazeAgent;
import edu.bu.labs.routing.Unit.UnitView;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue; // heap in java
import java.util.Set;


// JAVA PROJECT IMPORTS


public class DijkstraMazeAgent
    extends MazeAgent
{

    public DijkstraMazeAgent(final int agentId)
    {
        super(agentId);
    }

    public double getEdgeWeight(final Coordinate src,
                                final Coordinate dst,
                                final StateView stateView)
    {
        double weight = 1.0d;
    
        for (Integer enemyId : stateView.getEnemyUnitIds()) {
              UnitView enemy = stateView.getUnitView(enemyId);
                 if (enemy != null) {
                    Coordinate enemyCoord = enemy.getUnitCoord();
                    int dist = Math.abs(dst.row() - enemyCoord.row()) + Math.abs(dst.col() - enemyCoord.col());
                    if (dist <= 2) {
                        weight += 10.0d;
                    }
                 }
        }
        return weight;
    }

    @Override
    public void initializeFromState(final StateView stateView)
    {
        // find the FINISH tile
        Coordinate finishCoord = null;
        for(int rowIdx = 0; rowIdx < stateView.getNumRows(); ++rowIdx)
        {
            for(int colIdx = 0; colIdx < stateView.getNumCols(); ++colIdx)
            {
                if(stateView.getTileState(new Coordinate(rowIdx, colIdx)) == Tile.State.FINISH)
                {
                    finishCoord = new Coordinate(rowIdx, colIdx);
                }
            }
        }
        this.setFinishCoordinate(finishCoord);

        // make sure to call the super-class' version!
        super.initializeFromState(stateView);
    }

    @Override
    public boolean shouldReplacePlan(final StateView stateView)
    {
        return false;
    }

    @Override
    public Path<Coordinate> search(final Coordinate src,
                                   final Coordinate goal,
                                   final StateView stateView)
    {
        PriorityQueue<Path<Coordinate>> pq = new PriorityQueue<>(Comparator.comparingDouble(Path::trueCost));
        Map<Coordinate, Double> minCosts = new HashMap<>();

        pq.add(new Path<Coordinate>(src));
        minCosts.put(src, 0.0d);

        while (!pq.isEmpty()) {
            Path<Coordinate> currentPath = pq.poll();
            Coordinate currentCoord = currentPath.current();

            if (currentCoord.equals(goal)) {
                return currentPath;
            }

            if (currentPath.trueCost() > minCosts.getOrDefault(currentCoord, Double.MAX_VALUE)) {
                continue;
            }

            for (Direction dir : Direction.values()) {
                Coordinate neighbor = currentCoord.getNeighbor(dir);

                if (stateView.isInBounds(neighbor) && stateView.getTileState(neighbor) != Tile.State.WALL) {
                    double edgeWeight = getEdgeWeight(currentCoord, neighbor, stateView);
                    double newTotalCost = currentPath.trueCost() + edgeWeight;

                    if (newTotalCost < minCosts.getOrDefault(neighbor, Double.MAX_VALUE)) {
                        minCosts.put(neighbor, newTotalCost);
                        pq.add(new Path<Coordinate>(currentPath, neighbor, edgeWeight));
                    }
                }
            }
        }
        return null;
	}    
}
