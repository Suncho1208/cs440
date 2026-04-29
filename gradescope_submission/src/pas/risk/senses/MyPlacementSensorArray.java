package pas.risk.senses;


// SYSTEM IMPORTS
import edu.bu.jmat.Matrix;

import edu.bu.pas.risk.GameView;
import edu.bu.pas.risk.TerritoryOwnerView;
import edu.bu.pas.risk.agent.senses.PlacementSensorArray;
import edu.bu.pas.risk.territory.Territory;


// JAVA PROJECT IMPORTS


/**
 * A suite of sensors to convert a {@link Territory} into a feature vector (must be a row-vector)
 */ 
public class MyPlacementSensorArray
    extends PlacementSensorArray
{

    public static final int NUM_FEATURES = 5;

    public MyPlacementSensorArray(final int agentId)
    {
        super(agentId);
    }

    public Matrix getSensorValues(final GameView state,
                                  final int numRemainingArmies,
                                  final Territory territory)
    {
        final Matrix features = Matrix.zeros(1, NUM_FEATURES);
        final int myId = this.getAgentId();
        final TerritoryOwnerView territoryView = state.getTerritoryOwners().get(territory);

        int adjacentAllies = 0;
        int adjacentEnemies = 0;
        int adjacentEnemyArmies = 0;
        for(Territory adjacent : territory.adjacentTerritories())
        {
            final TerritoryOwnerView adjacentView = state.getTerritoryOwners().get(adjacent);
            if(adjacentView.getOwner() == myId)
            {
                adjacentAllies += 1;
            }
            else
            {
                adjacentEnemies += 1;
                adjacentEnemyArmies += adjacentView.getArmies();
            }
        }

        final int myBonusArmies = state.getBonusArmiesFor(myId);
        final int totalTerritories = Math.max(1, state.getBoard().territories().size());

        int c = 0;
        features.set(0, c++, territoryView.getArmies() / 20.0);
        features.set(0, c++, adjacentEnemies / (double)Math.max(1, territory.adjacentTerritories().size()));
        features.set(0, c++, adjacentEnemyArmies / 50.0);
        features.set(0, c++, numRemainingArmies / (double)Math.max(1, myBonusArmies));
        features.set(0, c++, territory.id() / (double)totalTerritories);

        return features;
    }

}

