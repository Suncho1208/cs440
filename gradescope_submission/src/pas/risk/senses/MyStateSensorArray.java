package pas.risk.senses;


// SYSTEM IMPORTS
import edu.bu.jmat.Matrix;

import edu.bu.pas.risk.GameView;
import edu.bu.pas.risk.TerritoryOwnerView;
import edu.bu.pas.risk.agent.senses.StateSensorArray;
import edu.bu.pas.risk.territory.Territory;
import edu.bu.pas.risk.territory.TerritoryCard;


// JAVA PROJECT IMPORTS


/**
 * A suite of sensors to convert a {@link GameView} into a feature vector (must be a row-vector)
 */ 
public class MyStateSensorArray
    extends StateSensorArray
{
    public static final int NUM_FEATURES = 15;

    public MyStateSensorArray(final int agentId)
    {
        super(agentId);
    }

    public Matrix getSensorValues(final GameView state)
    {
        final Matrix features = Matrix.zeros(1, NUM_FEATURES);

        final int myId = this.getAgentId();
        final int totalTerritories = Math.max(1, state.getBoard().territories().size());
        final int numAgents = Math.max(1, state.getNumAgents());

        int myTerritories = 0;
        int myArmies = 0;
        int totalArmies = 0;
        int myBorderTerritories = 0;
        int aliveOpponents = 0;
        int maxOpponentTerritories = 0;
        int maxOpponentArmies = 0;
        int myMaxArmiesOnTerritory = 0;
        int myMinArmiesOnTerritory = Integer.MAX_VALUE;

        final int[] territoryCountsByOwner = new int[numAgents];
        final int[] armyCountsByOwner = new int[numAgents];

        for(TerritoryOwnerView territoryOwner : state.getTerritoryOwners())
        {
            final int armies = territoryOwner.getArmies();
            final int owner = territoryOwner.getOwner();

            totalArmies += armies;

            if(owner >= 0 && owner < numAgents)
            {
                territoryCountsByOwner[owner] += 1;
                armyCountsByOwner[owner] += armies;
            }

            if(owner == myId)
            {
                myTerritories += 1;
                myArmies += armies;
                myMaxArmiesOnTerritory = Math.max(myMaxArmiesOnTerritory, armies);
                myMinArmiesOnTerritory = Math.min(myMinArmiesOnTerritory, armies);

                boolean border = false;
                final Territory territory = territoryOwner.getTerritory();
                for(Territory adjacent : territory.adjacentTerritories())
                {
                    final TerritoryOwnerView adjacentView = state.getTerritoryOwners().get(adjacent);
                    if(adjacentView.getOwner() != myId)
                    {
                        border = true;
                        break;
                    }
                }
                if(border)
                {
                    myBorderTerritories += 1;
                }
            }
        }

        for(int agentId = 0; agentId < numAgents; ++agentId)
        {
            if(agentId == myId)
            {
                continue;
            }

            if(territoryCountsByOwner[agentId] > 0)
            {
                aliveOpponents += 1;
            }
            maxOpponentTerritories = Math.max(maxOpponentTerritories, territoryCountsByOwner[agentId]);
            maxOpponentArmies = Math.max(maxOpponentArmies, armyCountsByOwner[agentId]);
        }

        final int cardsInHand = state.getAgentInventory(myId).size();
        final int continentsOwned = state.getContinentsOwnedBy(myId).size();
        final int totalContinents = Math.max(1, state.getBoard().continents().size());
        final int unownedTerritories = state.getUnownedTerritories().size();
        final double myTerritoryFrac = (double)myTerritories / totalTerritories;
        final double myArmyFrac = (double)myArmies / Math.max(1, totalArmies);
        final double borderFrac = myTerritories == 0 ? 0.0 : (double)myBorderTerritories / myTerritories;
        final double avgArmiesOnOwned = myTerritories == 0 ? 0.0 : (double)myArmies / myTerritories;
        final double minArmiesOnOwned = myTerritories == 0 ? 0.0 : (double)myMinArmiesOnTerritory;

        int c = 0;
        features.set(0, c++, myTerritoryFrac);
        features.set(0, c++, myArmyFrac);
        features.set(0, c++, borderFrac);
        features.set(0, c++, state.getBonusArmiesFor(myId) / 20.0);
        features.set(0, c++, cardsInHand / 10.0);
        features.set(0, c++, (double)unownedTerritories / totalTerritories);
        features.set(0, c++, state.getNumTurns() / 1000.0);
        features.set(0, c++, state.getNumPreviousRedemptions() / 100.0);
        features.set(0, c++, avgArmiesOnOwned / 10.0);
        features.set(0, c++, myMaxArmiesOnTerritory / 20.0);
        features.set(0, c++, minArmiesOnOwned / 10.0);
        features.set(0, c++, (double)continentsOwned / totalContinents);
        features.set(0, c++, (double)aliveOpponents / Math.max(1, numAgents - 1));
        features.set(0, c++, (myTerritories - maxOpponentTerritories) / (double)totalTerritories);
        features.set(0, c++, (myArmies - maxOpponentArmies) / (double)Math.max(1, totalArmies));

        return features;
    }

}

