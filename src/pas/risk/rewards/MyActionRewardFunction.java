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
        // FULL_TRANSITION: we need nextState to detect terminal win/loss.
        // Thread @542: "make sure there's a big gap between winning/losing situations."
        // HALF_TRANSITION cannot see game end → model never learns winning matters.
        super(RewardType.FULL_TRANSITION, agentId);
    }

    public double getLowerBound() { return -5.0; }
    public double getUpperBound() { return 5.0; }

    /** {@inheritDoc} */
    public double getStateReward(final GameView state) { return 0.0; }

    // Per-step shaping helper (used internally by getFullTransitionReward).
    // Kept scaled to roughly [-1.5, +1.0] so terminal signals (+5/-5) dominate.
    /** {@inheritDoc} */
    public double getHalfTransitionReward(final GameView state,
                                          final Action action)
    {
        final int myId = this.getAgentId();

        if(action instanceof NoAction)
        {
            // Distinguish attack-phase skip (bad) from fortify-phase skip (normal).
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
            // Attack-phase skip: -1.5 (still beats worst attacks so eval never infinite-loops).
            // Fortify-phase skip: -0.05 (neutral end-of-turn).
            return hasLegalAttack ? -1.5 : -0.05;
        }

        if(action instanceof RedeemCardsAction)
        {
            return 0.2;
        }

        if(action instanceof FortifyAction fortify)
        {
            final TerritoryOwnerView from = state.getTerritoryOwners().get(fortify.from());
            final TerritoryOwnerView to   = state.getTerritoryOwners().get(fortify.to());
            return (from.getOwner() == myId && to.getOwner() == myId) ? 0.1 : -0.5;
        }

        if(action instanceof AttackAction attack)
        {
            final TerritoryOwnerView source = state.getTerritoryOwners().get(attack.from());
            final TerritoryOwnerView target = state.getTerritoryOwners().get(attack.to());
            if(source.getOwner() != myId || target.getOwner() == myId) { return -0.5; }

            final double ratio    = source.getArmies() / (double)Math.max(1, target.getArmies());
            final double pressure = attack.attackingArmies() / (double)Math.max(1, source.getArmies());
            // Centered at 0 for 1:1 odds, negative for bad odds, positive for good odds.
            // Range ≈ [-1.0, +1.1] — always better than NoAction (-1.5).
            return 1.0 * Math.tanh(ratio - 1.0) + 0.1 * Math.tanh(pressure);
        }

        return 0.0;
    }

    /** {@inheritDoc} */
    public double getFullTransitionReward(final GameView state,
                                          final Action action,
                                          final GameView nextState)
    {
        if(nextState == null)
        {
            return this.getHalfTransitionReward(state, action);
        }

        final int myId      = this.getAgentId();
        final int totalTerr = Math.max(1, nextState.getBoard().territories().size());

        int myPrev = 0;
        for(TerritoryOwnerView t : state.getTerritoryOwners())
        {
            if(t.getOwner() == myId) myPrev++;
        }

        int myNext = 0;
        for(TerritoryOwnerView t : nextState.getTerritoryOwners())
        {
            if(t.getOwner() == myId) myNext++;
        }

        // Terminal WIN — own every territory.
        if(myNext == totalTerr)
        {
            return this.getUpperBound(); // +5.0
        }

        // Terminal LOSE — just got eliminated.
        if(myNext == 0 && myPrev > 0)
        {
            return this.getLowerBound(); // -5.0
        }

        // Dense immediate reward: proportional to territory fraction in NEXT state.
        // Thread @537: with γ=0.95 per action, sparse terminal signals are discounted
        // to ~0 over 300+ steps. This gives a visible gradient at EVERY step regardless
        // of γ, since the reward reflects "how much am I winning RIGHT NOW."
        // Centers at 0 when owning 50% of territories.
        // Range: [-2.5, +2.5] for non-terminal steps.
        final double terrFrac = (double) myNext / totalTerr;
        final double terrReward = 5.0 * (terrFrac - 0.5);

        // Small per-step action shaping (0.2 scale) as a nudge — does not overwhelm
        // the territory signal but keeps NoAction penalty alive for eval loop prevention.
        return terrReward + 0.2 * this.getHalfTransitionReward(state, action);
    }

}

