package pas.risk.rewards;


import edu.bu.pas.risk.GameView;
import edu.bu.pas.risk.TerritoryOwnerView;
import edu.bu.pas.risk.action.AttackAction;
import edu.bu.pas.risk.territory.Territory;
import edu.bu.pas.risk.action.Action;
import edu.bu.pas.risk.action.FortifyAction;
import edu.bu.pas.risk.action.NoAction;
import edu.bu.pas.risk.action.RedeemCardsAction;
import edu.bu.pas.risk.agent.rewards.RewardFunction;
import edu.bu.pas.risk.agent.rewards.RewardType;


// JAVA PROJECT IMPORTS


/**
 * <p>Represents a function which punishes/pleasures your model according to how well the {@link Action}s its been
 * choosing have been. Your reward function could calculate R(s), R(s,a), or (R,s,a'): whichever is easiest for you to
 * think about (for instance does it make more sense to you to evaluate behavior when you see a state, the action you
 * took in that state, and how that action resolved? If so you want to pick R(s,a,s')).
 *
 * <p>By default this is configured to calculate R(s). If you want to change this you need to change the
 * {@link RewardType} enum in the constructor *and* you need to implement the corresponding method. Refer to
 * {@link RewardFunction} and {@link RewardType} for more details.
 */
public class MyActionRewardFunction
    extends RewardFunction<Action>
{

    public MyActionRewardFunction(final int agentId)
    {
        super(RewardType.HALF_TRANSITION, agentId);
    }

    public double getLowerBound() { return -5.0; }
    public double getUpperBound() { return 5.0; }

    /** {@inheritDoc} */
    public double getStateReward(final GameView state) { return 0.0; }

    /** {@inheritDoc} */
    public double getHalfTransitionReward(final GameView state,
                                          final Action action)
    {
        final int myId = this.getAgentId();

        if(action instanceof NoAction)
        {
            // NoAction appears in TWO phases:
            //   Attack phase: "I choose not to attack" → strongly penalize (-3.0) to prevent
            //     infinite eval loops (Thread 014 @397) and force the model to attack.
            //   Fortify phase: "I choose not to fortify" → completely normal, neutral (-0.1).
            //
            // Distinguish by checking if there are any legal attack targets right now.
            // If any owned territory (≥2 armies) borders an enemy → we're in attack phase.
            boolean hasLegalAttack = false;
            for(TerritoryOwnerView tv : state.getTerritoryOwners())
            {
                if(tv.getOwner() == myId && tv.getArmies() >= 2)
                {
                    for(Territory adj : tv.getTerritory().adjacentTerritories())
                    {
                        if(state.getTerritoryOwners().get(adj).getOwner() != myId)
                        {
                            hasLegalAttack = true;
                            break;
                        }
                    }
                }
                if(hasLegalAttack) break;
            }
            return hasLegalAttack ? -3.0 : -0.1;
        }

        if(action instanceof RedeemCardsAction)
        {
            return 0.5;
        }

        if(action instanceof FortifyAction fortify)
        {
            final TerritoryOwnerView from = state.getTerritoryOwners().get(fortify.from());
            final TerritoryOwnerView to = state.getTerritoryOwners().get(fortify.to());
            final boolean legalFriendlyMove = from.getOwner() == myId && to.getOwner() == myId;
            return legalFriendlyMove ? 0.3 : -1.5;
        }

        if(action instanceof AttackAction attack)
        {
            final TerritoryOwnerView source = state.getTerritoryOwners().get(attack.from());
            final TerritoryOwnerView target = state.getTerritoryOwners().get(attack.to());
            final int sourceArmies = source.getArmies();
            final int targetArmies = target.getArmies();

            if(source.getOwner() != myId || target.getOwner() == myId)
            {
                return -2.0;
            }

            final double ratio = sourceArmies / (double)Math.max(1, targetArmies);
            final double pressure = attack.attackingArmies() / (double)Math.max(1, sourceArmies);

            // Thread 047 (@494): with a positive floor on every attack, the agent farms
            // small positive rewards forever without winning (rational stalemate).
            // Fix: center at 0 for 1:1 odds, negative for bad odds, positive for good odds.
            // Range: approx [-2.0, +2.8]. All attacks still beat NoAction (-3.0) by a
            // wide margin, so eval games still terminate — but the agent is NOT rewarded
            // for pointless attacks and must push for a decisive win to accumulate positive returns.
            return 2.5 * Math.tanh(ratio - 1.0) + 0.3 * Math.tanh(pressure);
        }

        return 0.0;
    }

    /** {@inheritDoc} */
    public double getFullTransitionReward(final GameView state,
                                          final Action action,
                                          final GameView nextState) { return this.getHalfTransitionReward(state, action); }

}

