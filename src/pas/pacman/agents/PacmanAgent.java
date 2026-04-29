package src.pas.pacman.agents;


// SYSTEM IMPORTS
import edu.bu.pas.pacman.agents.Agent;
import edu.bu.pas.pacman.agents.SearchAgent;
import edu.bu.pas.pacman.game.Action;
import edu.bu.pas.pacman.game.Game.GameView;
import edu.bu.pas.pacman.graph.Path;
import edu.bu.pas.pacman.graph.PelletGraph.PelletVertex;
import edu.bu.pas.pacman.routing.BoardRouter;
import edu.bu.pas.pacman.routing.PelletRouter;
import edu.bu.pas.pacman.utils.Coordinate;
import edu.bu.pas.pacman.utils.Pair;
import edu.bu.pas.pacman.game.Tile;


import java.util.Random;
import java.util.Set;
import java.util.Stack;
import java.util.Collection;

// JAVA PROJECT IMPORTS
import src.pas.pacman.routing.ThriftyBoardRouter;  // responsible for how to get somewhere
import src.pas.pacman.routing.ThriftyPelletRouter; // responsible for pellet order


public class PacmanAgent
    extends SearchAgent
{

    private final Random random;
    private BoardRouter  boardRouter;
    private PelletRouter pelletRouter;

    public PacmanAgent(int myUnitId,
                       int pacmanId,
                       int ghostChaseRadius)
    {
        super(myUnitId, pacmanId, ghostChaseRadius);
        this.random = new Random();

        this.boardRouter = new ThriftyBoardRouter(myUnitId, pacmanId, ghostChaseRadius);
        this.pelletRouter = new ThriftyPelletRouter(myUnitId, pacmanId, ghostChaseRadius);
    }

    public final Random getRandom() { return this.random; }
    public final BoardRouter getBoardRouter() { return this.boardRouter; }
    public final PelletRouter getPelletRouter() { return this.pelletRouter; }

    @Override
    public void makePlan(final GameView game)
    {
        PelletVertex root = new PelletVertex(game);
        Collection<Coordinate> pellets = root.getRemainingPelletCoordinates();
        if (pellets.isEmpty()) return;

        Stack<Coordinate> plan = new Stack<>();

        if (pellets.size() <= 8) {
            Path<PelletVertex> path = this.getPelletRouter().graphSearch(game);
            if (path != null) {
                Path<PelletVertex> current = path;
                while (current != null && current.getParentPath() != null) {
                    plan.push(current.getDestination().getPacmanCoordinate());
                    current = current.getParentPath();
                }
            }
        } else {
            Coordinate current = game.getEntity(game.getPacmanId()).getCurrentCoordinate();
            Coordinate closest = null;
            float minDist = Float.MAX_VALUE;
            for (Coordinate p : pellets) {
                float dist = (float) edu.bu.pas.pacman.utils.DistanceMetric.manhattanDistance(current, p);
                if (dist < minDist) {
                    minDist = dist;
                    closest = p;
                }
            }
            if (closest != null) {
                plan.push(closest);
            }
        }
        
        this.setPlanToGetToTarget(plan);
    }

    @Override
    public Action makeMove(final GameView game)
    {
        if (this.getPlanToGetToTarget() == null || this.getPlanToGetToTarget().isEmpty()) {
            this.makePlan(game);
        }

        if (this.getPlanToGetToTarget() == null || this.getPlanToGetToTarget().isEmpty()) {
            return null;
        }

        Coordinate current = game.getEntity(game.getPacmanId()).getCurrentCoordinate();
        Coordinate target = this.getPlanToGetToTarget().peek();

        if (current.equals(target)) {
            this.getPlanToGetToTarget().pop();
            if (this.getPlanToGetToTarget().isEmpty()) {
                this.makePlan(game);
                if (this.getPlanToGetToTarget().isEmpty()) return null;
            }
            target = this.getPlanToGetToTarget().peek();
        }

        Path<Coordinate> boardPath = this.getBoardRouter().graphSearch(current, target, game);
        if (boardPath != null) {
            Path<Coordinate> step = boardPath;
            while (step.getParentPath() != null && !step.getParentPath().getDestination().equals(current)) {
                step = step.getParentPath();
            }
            Coordinate nextStep = step.getDestination();
            if (nextStep.x() > current.x()) return Action.RIGHT;
            if (nextStep.x() < current.x()) return Action.LEFT;
            if (nextStep.y() > current.y()) return Action.DOWN;
            if (nextStep.y() < current.y()) return Action.UP;
        }

        return null;
    }

    @Override
    public void afterGameEnds(final GameView game)
    {
        // if you want to log stuff after a game ends implement me!
    }
}
