package src.pas.uno.agents;


// SYSTEM IMPORTS
import edu.bu.pas.uno.Card;
import edu.bu.pas.uno.Game;
import edu.bu.pas.uno.Game.GameView;
import edu.bu.pas.uno.Hand.HandView;
import edu.bu.pas.uno.agents.Agent;
import edu.bu.pas.uno.agents.MCTSAgent;
import edu.bu.pas.uno.enums.Color;
import edu.bu.pas.uno.enums.Value;
import edu.bu.pas.uno.moves.Move;
import edu.bu.pas.uno.tree.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;


// JAVA PROJECT IMPORTS


public class ExpectedOutcomeAgent
    extends MCTSAgent
{
    private Integer currentSearchDrawnCardIdx;

    private static class ProxyAgent
        extends Agent
    {
        public ProxyAgent(final int playerIdx)
        {
            super(playerIdx, 1L);
        }

        @Override
        public Move chooseCardToPlay(final GameView game)
        {
            return null;
        }

        @Override
        public Move maybePlayDrawnCard(final GameView game, final int drawnCardIdx)
        {
            return null;
        }
    }

    private static Agent[] buildProxyAgents(final GameView game)
    {
        final int numPlayers = game.getNumPlayers();
        final Agent[] proxies = new Agent[numPlayers];
        for(int logicalIdx = 0; logicalIdx < numPlayers; ++logicalIdx)
        {
            final int playerIdx = game.getPlayerOrder().getAgentIdx(logicalIdx);
            proxies[logicalIdx] = new ProxyAgent(playerIdx);
            proxies[logicalIdx].setLogicalPlayerIdx(logicalIdx);
        }
        return proxies;
    }

    public static class MCTSNode
        extends Node
    {
        public MCTSNode(final GameView game,
                        final int logicalPlayerIdx,
                        final Node parent)
        {
            super(game, logicalPlayerIdx, parent);
        }

        @Override
        public Node getChild(final Move move)
        {
            final Game simulationGame = new Game(this.getGameView(), ExpectedOutcomeAgent.buildProxyAgents(this.getGameView()));
            simulationGame.resolveMove(move);
            final int nextLogicalPlayerIdx = simulationGame.getPlayerOrder().getCurrentLogicalPlayerIdx();
            return new MCTSNode(simulationGame.getOmniscientView(), nextLogicalPlayerIdx, this);
        }
    }

    public ExpectedOutcomeAgent(final int playerIdx,
                                final long maxThinkingTimeInMS)
    {
        super(playerIdx, maxThinkingTimeInMS);
    }

    /**
     * A method to perform the MCTS search on the game tree
     *
     * @param   game            The {@link GameView} that should be the root of the game tree
     * @param   drawnCardIdx    This will be non-null when this method is being called by the 
     *                          <code>maybePlayDrawnCard</code> method of {@link Agent} and will
     *                          be <code>null</code> when being called by <code>chooseCardToPlay</code>
     *                          method of {@link Agent}
     * @return  The {@link Node} of the root who'se q-values should now be populated and ready to argmax
     */
    @Override
    public Node search(final GameView game,
                       final Integer drawnCardIdx)
    {
        this.currentSearchDrawnCardIdx = drawnCardIdx;
        final int rootLogicalPlayerIdx = game.getPlayerOrder().getCurrentLogicalPlayerIdx();
        final Node root = new MCTSNode(game, rootLogicalPlayerIdx, null);
        final List<Move> candidateMoves = this.getCandidateMoves(root, drawnCardIdx);
        if(candidateMoves.isEmpty())
        {
            return root;
        }

        final int rolloutsPerAction = 2;
        for(int moveIdx = 0; moveIdx < candidateMoves.size(); ++moveIdx)
        {
            final Move candidateMove = candidateMoves.get(moveIdx);
            final Node child = root.getChild(candidateMove);
            float qValueTotal = 0.0f;
            for(int rollout = 0; rollout < rolloutsPerAction; ++rollout)
            {
                qValueTotal += this.runRandomRollout(child.getGameView(), rootLogicalPlayerIdx);
            }

            root.setQValueTotal(moveIdx, qValueTotal);
            root.setQCount(moveIdx, rolloutsPerAction);
        }

        return root;
    }

    /**
     * A method to argmax the Q values inside a {@link Node}
     *
     * @param   node            The {@link Node} who has populated q-values
     * @return  The {@link Move} corresponding to whichever {@link Move} has the largest q-value. Note
     *          that this can be <code>null</code> if you choose to not play the drawn card (you will
     *          have to detect whether or not you are in that scenario by examining the @{link Node}'s state).
     */
    @Override
    public Move argmaxQValues(final Node node)
    {
        final List<Move> candidateMoves = this.getCandidateMoves(node, this.currentSearchDrawnCardIdx);
        if(candidateMoves.isEmpty())
        {
            return null;
        }

        float bestQValue = Float.NEGATIVE_INFINITY;
        final List<Integer> bestMoveIndices = new ArrayList<Integer>();

        for(int idx = 0; idx < candidateMoves.size(); ++idx)
        {
            final long qCount = node.getQCount(idx);
            final float qValue = qCount > 0L ? node.getQValue(idx) : Float.NEGATIVE_INFINITY;
            if(qValue > bestQValue)
            {
                bestQValue = qValue;
                bestMoveIndices.clear();
                bestMoveIndices.add(idx);
            } else if(qValue == bestQValue)
            {
                bestMoveIndices.add(idx);
            }
        }

        final int selectedBestIdx = bestMoveIndices.get(this.getRandom().nextInt(bestMoveIndices.size()));
        return candidateMoves.get(selectedBestIdx);
    }

    private List<Move> getCandidateMoves(final Node node, final Integer drawnCardIdx)
    {
        final List<Move> moves = new ArrayList<Move>();
        final GameView game = node.getGameView();
        final HandView hand = game.getHandView(node.getLogicalPlayerIdx());

        switch(node.getNodeState())
        {
            case HAS_LEGAL_MOVES:
                for(final Integer legalMoveIdx: node.getOrderedLegalMoves())
                {
                    final Card card = hand.getCard(legalMoveIdx);
                    if(card.value().equals(Value.WILD) || card.value().equals(Value.WILD_DRAW_FOUR))
                    {
                        moves.add(Move.createMove(this, legalMoveIdx, this.chooseBestColor(hand)));
                    } else
                    {
                        moves.add(Move.createMove(this, legalMoveIdx));
                    }
                }
                break;
            case NO_LEGAL_MOVES_MAY_PLAY_DRAWN_CARD:
                final int playIdx = drawnCardIdx != null ? drawnCardIdx : (hand.size() - 1);
                if(playIdx >= 0 && playIdx < hand.size())
                {
                    final Card drawnCard = hand.getCard(playIdx);
                    if(drawnCard.value().equals(Value.WILD) || drawnCard.value().equals(Value.WILD_DRAW_FOUR))
                    {
                        moves.add(Move.createMove(this, playIdx, this.chooseBestColor(hand)));
                    } else
                    {
                        moves.add(Move.createMove(this, playIdx));
                    }
                }
                moves.add(null);
                break;
            case NO_LEGAL_MOVES_UNRESOLVED_CARDS_PRESENT:
                moves.add(null);
                break;
            default:
                break;
        }

        return moves;
    }

    private float runRandomRollout(final GameView gameView, final int rootLogicalPlayerIdx)
    {
        final Agent[] proxies = buildProxyAgents(gameView);
        final Game game = new Game(gameView, proxies);
        final Random random = this.getRandom();

        int rolloutDepth = 0;
        final int rolloutDepthLimit = 40;
        while(!game.isOver() && rolloutDepth < rolloutDepthLimit)
        {
            final int currentLogicalIdx = game.getPlayerOrder().getCurrentLogicalPlayerIdx();
            final Agent currentAgent = game.getAgent(currentLogicalIdx);
            final GameView currentView = game.getOmniscientView();
            final HandView hand = currentView.getHandView(currentLogicalIdx);
            final Set<Integer> legalMoves = hand.getLegalMoves(currentView);

            Move move = null;
            if(!legalMoves.isEmpty())
            {
                final int chosenCardIdx = this.getRandomMoveIdx(legalMoves, random);
                move = this.createMoveForCard(currentAgent, hand, chosenCardIdx, random);
            } else if(game.getUnresolvedCards().isEmpty())
            {
                final int drawnCardIdx = game.drawCard(game.getHand(currentLogicalIdx));
                final GameView afterDrawView = game.getOmniscientView();
                final HandView afterDrawHand = afterDrawView.getHandView(currentLogicalIdx);
                final Set<Integer> legalAfterDraw = afterDrawHand.getLegalMoves(afterDrawView);
                if(legalAfterDraw.contains(drawnCardIdx) && random.nextBoolean())
                {
                    move = this.createMoveForCard(currentAgent, afterDrawHand, drawnCardIdx, random);
                }
            }

            game.resolveMove(move);
            ++rolloutDepth;
        }

        if(game.getHand(rootLogicalPlayerIdx).isEmpty())
        {
            return 1.0f;
        }

        for(int i = 0; i < game.getNumPlayers(); ++i)
        {
            if(i != rootLogicalPlayerIdx && game.getHand(i).isEmpty())
            {
                return -1.0f;
            }
        }

        final int rootHandSize = game.getHand(rootLogicalPlayerIdx).size();
        int opponentHandTotal = 0;
        for(int i = 0; i < game.getNumPlayers(); ++i)
        {
            if(i != rootLogicalPlayerIdx)
            {
                opponentHandTotal += game.getHand(i).size();
            }
        }

        final float averageOpponentHandSize = (float)opponentHandTotal / Math.max(1, game.getNumPlayers() - 1);
        return (averageOpponentHandSize - rootHandSize) / 20.0f;
    }

    private int getRandomMoveIdx(final Set<Integer> legalMoves, final Random random)
    {
        int offset = random.nextInt(legalMoves.size());
        for(final Integer moveIdx: legalMoves)
        {
            if(offset == 0)
            {
                return moveIdx;
            }
            --offset;
        }
        return legalMoves.iterator().next();
    }

    private Move createMoveForCard(final Agent agent, final HandView hand, final int cardIdx, final Random random)
    {
        final Card card = hand.getCard(cardIdx);
        if(card.value().equals(Value.WILD) || card.value().equals(Value.WILD_DRAW_FOUR))
        {
            return Move.createMove(agent, cardIdx, Color.getRandomColor(random));
        }
        return Move.createMove(agent, cardIdx);
    }

    private Color chooseBestColor(final HandView hand)
    {
        int red = 0;
        int green = 0;
        int blue = 0;
        int yellow = 0;

        for(int i = 0; i < hand.size(); ++i)
        {
            final Color color = hand.getCard(i).color();
            if(color == null)
            {
                continue;
            }
            switch(color)
            {
                case RED:
                    ++red;
                    break;
                case GREEN:
                    ++green;
                    break;
                case BLUE:
                    ++blue;
                    break;
                case YELLOW:
                    ++yellow;
                    break;
                default:
                    break;
            }
        }

        if(red >= green && red >= blue && red >= yellow)
        {
            return Color.RED;
        }
        if(green >= red && green >= blue && green >= yellow)
        {
            return Color.GREEN;
        }
        if(blue >= red && blue >= green && blue >= yellow)
        {
            return Color.BLUE;
        }
        if(yellow >= red && yellow >= green && yellow >= blue)
        {
            return Color.YELLOW;
        }

        return Color.getRandomColor(this.getRandom());
    }
}
