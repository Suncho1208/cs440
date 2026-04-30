package pas.risk.rewards;


import edu.bu.pas.risk.GameView;
import edu.bu.pas.risk.TerritoryOwnerView;
import edu.bu.pas.risk.action.AttackAction;
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
            // Strongly negative: during eval (argmax, no exploration) a near-zero NoAction
            // reward causes the model to always skip attacking, creating an infinite game loop
            // (Thread 014 @397). Must be clearly the worst option in the attack phase.
            return -3.0;
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

            // Reward attacking even at bad odds slightly — we need the model to
            // prefer any attack over NoAction so eval games always terminate.
            final double ratio = sourceArmies / (double)Math.max(1, targetArmies);
            // Base reward: always positive (min ~+0.3) so attacks always beat NoAction (-3.0).
            // Scales up to ~+2.5 for strong attacks.
            return 0.3 + 2.2 * Math.tanh(Math.max(0, ratio - 0.5));
        }

        return 0.0;
    }

    /** {@inheritDoc} */
    public double getFullTransitionReward(final GameView state,
                                          final Action action,
                                          final GameView nextState) { return this.getHalfTransitionReward(state, action); }

}

