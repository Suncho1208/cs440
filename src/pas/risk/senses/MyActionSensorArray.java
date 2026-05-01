package pas.risk.senses;


// SYSTEM IMPORTS
import edu.bu.jmat.Matrix;

import edu.bu.pas.risk.GameView;
import edu.bu.pas.risk.TerritoryOwnerView;
import edu.bu.pas.risk.action.AttackAction;
import edu.bu.pas.risk.action.Action;
import edu.bu.pas.risk.action.FortifyAction;
import edu.bu.pas.risk.action.NoAction;
import edu.bu.pas.risk.action.RedeemCardsAction;
import edu.bu.pas.risk.agent.senses.ActionSensorArray;


// JAVA PROJECT IMPORTS


/**
 * A suite of sensors to convert a {@link Action} into a feature vector (must be a row-vector)
 */ 
public class MyActionSensorArray
    extends ActionSensorArray
{

    public static final int NUM_FEATURES = 10;
    private static double squashCount(final double value, final double scale)
    {
        return Math.tanh(value / scale);
    }

    public MyActionSensorArray(final int agentId)
    {
        super(agentId);
    }

    public Matrix getSensorValues(final GameView state,
                                  final int actionCounter,
                                  final Action action)
    {
        final Matrix features = Matrix.zeros(1, NUM_FEATURES);
        final int myId = this.getAgentId();
        final int totalTerritories = Math.max(1, state.getBoard().territories().size());

        double isAttack = 0.0;
        double isFortify = 0.0;
        double isRedeem = 0.0;
        double isNoAction = 0.0;

        double sourceArmies = 0.0;
        double targetArmies = 0.0;
        double movedOrAttacking = 0.0;
        double ownsSource = 0.0;
        double targetIsEnemy = 0.0;

        if(action instanceof AttackAction attack)
        {
            isAttack = 1.0;

            final TerritoryOwnerView source = state.getTerritoryOwners().get(attack.from());
            final TerritoryOwnerView target = state.getTerritoryOwners().get(attack.to());

            sourceArmies = source.getArmies();
            targetArmies = target.getArmies();
            movedOrAttacking = attack.attackingArmies();
            ownsSource = source.getOwner() == myId ? 1.0 : 0.0;
            targetIsEnemy = target.getOwner() != myId ? 1.0 : 0.0;
        }
        else if(action instanceof FortifyAction fortify)
        {
            isFortify = 1.0;

            final TerritoryOwnerView source = state.getTerritoryOwners().get(fortify.from());
            final TerritoryOwnerView target = state.getTerritoryOwners().get(fortify.to());

            sourceArmies = source.getArmies();
            targetArmies = target.getArmies();
            movedOrAttacking = fortify.deltaArmies();
            ownsSource = source.getOwner() == myId ? 1.0 : 0.0;
            targetIsEnemy = target.getOwner() != myId ? 1.0 : 0.0;
        }
        else if(action instanceof RedeemCardsAction redeem)
        {
            isRedeem = 1.0;
            movedOrAttacking = redeem.card1().armyValue()
                + redeem.card2().armyValue()
                + redeem.card3().armyValue();
        }
        else if(action instanceof NoAction)
        {
            isNoAction = 1.0;
        }

        int c = 0;
        features.set(0, c++, isAttack);
        features.set(0, c++, isFortify);
        features.set(0, c++, isRedeem);
        features.set(0, c++, isNoAction);
        features.set(0, c++, squashCount(sourceArmies, 10.0));
        features.set(0, c++, squashCount(targetArmies, 10.0));
        features.set(0, c++, squashCount(movedOrAttacking, 8.0));
        features.set(0, c++, ownsSource);
        features.set(0, c++, targetIsEnemy);
        features.set(0, c++, actionCounter / (double)totalTerritories);

        return features;
    }

}

