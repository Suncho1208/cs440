package src.pas.uno.agents;


// SYSTEM IMPORTS
import edu.bu.pas.uno.Card;
import edu.bu.pas.uno.Deck;
import edu.bu.pas.uno.Game;
import edu.bu.pas.uno.Game.GameView;
import edu.bu.pas.uno.Hand;
import edu.bu.pas.uno.Hand.HandView;
import edu.bu.pas.uno.agents.Agent;
import edu.bu.pas.uno.agents.MCTSAgent;
import edu.bu.pas.uno.enums.Color;
import edu.bu.pas.uno.enums.Value;
import edu.bu.pas.uno.moves.Move;
import edu.bu.pas.uno.tree.Node;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;


// JAVA PROJECT IMPORTS


public class UnoMCTSAgent
    extends MCTSAgent
{
    private Integer currentSearchDrawnCardIdx;
    private static final Color[] DETERMINIZATION_COLORS = new Color[] { Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW };
    private static final Value[] DETERMINIZATION_VALUES = new Value[] {
        Value.ZERO, Value.ONE, Value.TWO, Value.THREE, Value.FOUR,
        Value.FIVE, Value.SIX, Value.SEVEN, Value.EIGHT, Value.NINE,
        Value.SKIP, Value.REVERSE, Value.DRAW_TWO
    };

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
        private final Map<String, Node> childrenByKey;
        private final List<Node> children;
        private final Random random;

        public MCTSNode(final GameView game,
                        final int logicalPlayerIdx,
                        final Node parent,
                        final Random random)
        {
            super(game, logicalPlayerIdx, parent);
            this.childrenByKey = new HashMap<String, Node>();
            this.children = new ArrayList<Node>();
            this.random = random;
        }

        private static String moveKey(final Move move)
        {
            if(move == null)
            {
                return "NULL";
            }
            return move.getPlayerIdx() + ":" + move.getCardToPlayIdx() + ":" + String.valueOf(move.getNewColorIfWild());
        }

        @Override
        public Node getChild(final Move move)
        {
            final String key = MCTSNode.moveKey(move);
            if(this.childrenByKey.containsKey(key))
            {
                return this.childrenByKey.get(key);
            }

            final Game simulationGame = UnoMCTSAgent.buildDeterminizedGame(this.getGameView(), this.random);
            simulationGame.resolveMove(move);
            final int nextLogicalPlayerIdx = simulationGame.getPlayerOrder().getCurrentLogicalPlayerIdx();
            final int nextPlayerIdx = simulationGame.getPlayerOrder().getAgentIdx(nextLogicalPlayerIdx);
            final Node child = new MCTSNode(simulationGame.getView(nextPlayerIdx), nextLogicalPlayerIdx, this, this.random);
            this.childrenByKey.put(key, child);
            this.children.add(child);
            return child;
        }
    }

    public UnoMCTSAgent(final int playerIdx,
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
        final Node root = new MCTSNode(game, rootLogicalPlayerIdx, null, this.getRandom());
        final List<Move> rootCandidateMoves = this.getCandidateMoves(root, drawnCardIdx);
        if(rootCandidateMoves.isEmpty())
        {
            return root;
        }

        final long startTime = System.nanoTime();
        final long cappedThinkingTimeMs = Math.min(Math.max(1L, this.getMaxThinkingTimeInMS()), 5L);
        final long budgetNanos = cappedThinkingTimeMs * 1000000L;
        final long deadline = startTime + budgetNanos;
        final int minIterations = 1;
        int iteration = 0;

        while(iteration < minIterations || System.nanoTime() < deadline)
        {
            ++iteration;
            Node current = root;
            final List<Node> pathNodes = new ArrayList<Node>();
            final List<Integer> pathMoveIdxs = new ArrayList<Integer>();
            boolean expanded = false;

            while(!current.isTerminal())
            {
                final Integer contextualDrawnIdx = current == root ? drawnCardIdx : null;
                final List<Move> candidateMoves = this.getCandidateMoves(current, contextualDrawnIdx);
                if(candidateMoves.isEmpty())
                {
                    break;
                }

                final List<Integer> untriedMoveIdxs = new ArrayList<Integer>();
                for(int i = 0; i < candidateMoves.size(); ++i)
                {
                    if(current.getQCount(i) == 0L)
                    {
                        untriedMoveIdxs.add(i);
                    }
                }

                final int chosenMoveIdx;
                if(!untriedMoveIdxs.isEmpty())
                {
                    chosenMoveIdx = untriedMoveIdxs.get(this.getRandom().nextInt(untriedMoveIdxs.size()));
                    expanded = true;
                } else
                {
                    chosenMoveIdx = this.selectMoveByUcb(current, candidateMoves.size());
                }

                pathNodes.add(current);
                pathMoveIdxs.add(chosenMoveIdx);
                current = current.getChild(candidateMoves.get(chosenMoveIdx));

                if(expanded)
                {
                    break;
                }
            }

            final float rolloutValue = current.isTerminal() ? this.evaluateTerminalOrApprox(current.getGameView(), rootLogicalPlayerIdx)
                                                            : this.runRandomRollout(current.getGameView(), rootLogicalPlayerIdx);

            for(int i = 0; i < pathNodes.size(); ++i)
            {
                final Node nodeOnPath = pathNodes.get(i);
                final int moveIdx = pathMoveIdxs.get(i);
                final float propagatedValue = nodeOnPath.getLogicalPlayerIdx() == rootLogicalPlayerIdx ? rolloutValue : -rolloutValue;
                nodeOnPath.setQValueTotal(moveIdx, nodeOnPath.getQValueTotal(moveIdx) + propagatedValue);
                nodeOnPath.setQCount(moveIdx, nodeOnPath.getQCount(moveIdx) + 1L);
            }
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
            if(qCount <= 0L)
            {
                continue;
            }
            final float qValue = node.getQValue(idx);
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

        if(bestMoveIndices.isEmpty())
        {
            return candidateMoves.get(this.getRandom().nextInt(candidateMoves.size()));
        }

        final int selectedBestIdx = bestMoveIndices.get(this.getRandom().nextInt(bestMoveIndices.size()));
        return candidateMoves.get(selectedBestIdx);
    }

    private int selectMoveByUcb(final Node node, final int numMoves)
    {
        final long stateCount = Math.max(1L, node.getStateCount());
        float bestUcb = Float.NEGATIVE_INFINITY;
        final List<Integer> bestMoveIdxs = new ArrayList<Integer>();

        for(int moveIdx = 0; moveIdx < numMoves; ++moveIdx)
        {
            final long qCount = node.getQCount(moveIdx);
            final float ucbValue;
            if(qCount == 0L)
            {
                ucbValue = Float.POSITIVE_INFINITY;
            } else
            {
                final float avgQ = node.getQValue(moveIdx);
                final float exploration = (float)Math.sqrt((2.0 * Math.log(stateCount)) / qCount);
                ucbValue = avgQ + exploration;
            }

            if(ucbValue > bestUcb)
            {
                bestUcb = ucbValue;
                bestMoveIdxs.clear();
                bestMoveIdxs.add(moveIdx);
            } else if(ucbValue == bestUcb)
            {
                bestMoveIdxs.add(moveIdx);
            }
        }

        return bestMoveIdxs.get(this.getRandom().nextInt(bestMoveIdxs.size()));
    }

    private List<Move> getCandidateMoves(final Node node, final Integer drawnCardIdx)
    {
        final List<Move> moves = new ArrayList<Move>();
        final GameView game = node.getGameView();
        final HandView hand = game.getHandView(node.getLogicalPlayerIdx());
        final Agent moveAgent = this.getMoveAgentForNode(node);

        switch(node.getNodeState())
        {
            case HAS_LEGAL_MOVES:
                for(final Integer legalMoveIdx: node.getOrderedLegalMoves())
                {
                    final Card card = hand.getCard(legalMoveIdx);
                    if(card.value().equals(Value.WILD) || card.value().equals(Value.WILD_DRAW_FOUR))
                    {
                        moves.add(Move.createMove(moveAgent, legalMoveIdx, this.chooseBestColor(hand)));
                    } else
                    {
                        moves.add(Move.createMove(moveAgent, legalMoveIdx));
                    }
                }
                break;
            case NO_LEGAL_MOVES_MAY_PLAY_DRAWN_CARD:
                int playIdx = -1;
                if(drawnCardIdx != null)
                {
                    playIdx = drawnCardIdx;
                } else
                {
                    final Set<Integer> legalDrawnMoves = hand.getLegalMoves(game);
                    if(!legalDrawnMoves.isEmpty())
                    {
                        playIdx = legalDrawnMoves.iterator().next();
                    }
                }
                if(playIdx >= 0 && playIdx < hand.size())
                {
                    final Card drawnCard = hand.getCard(playIdx);
                    if(drawnCard.value().equals(Value.WILD) || drawnCard.value().equals(Value.WILD_DRAW_FOUR))
                    {
                        moves.add(Move.createMove(moveAgent, playIdx, this.chooseBestColor(hand)));
                    } else
                    {
                        moves.add(Move.createMove(moveAgent, playIdx));
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

    private Agent getMoveAgentForNode(final Node node)
    {
        final int playerIdx = node.getGameView().getPlayerOrder().getAgentIdx(node.getLogicalPlayerIdx());
        final ProxyAgent proxy = new ProxyAgent(playerIdx);
        proxy.setLogicalPlayerIdx(node.getLogicalPlayerIdx());
        return proxy;
    }

    private float runRandomRollout(final GameView gameView, final int rootLogicalPlayerIdx)
    {
        final Random random = this.getRandom();
        final Game game = buildDeterminizedGame(gameView, random);

        int rolloutDepth = 0;
        final int rolloutDepthLimit = 16;
        while(!game.isOver() && rolloutDepth < rolloutDepthLimit)
        {
            final int currentLogicalIdx = game.getPlayerOrder().getCurrentLogicalPlayerIdx();
            final Agent currentAgent = game.getAgent(currentLogicalIdx);
            final GameView currentView = game.getView(currentAgent.getPlayerIdx());
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
                final GameView afterDrawView = game.getView(currentAgent.getPlayerIdx());
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

        return this.evaluateTerminalOrApprox(game.getOmniscientView(), rootLogicalPlayerIdx);
    }

    private float evaluateTerminalOrApprox(final GameView gameView, final int rootLogicalPlayerIdx)
    {
        if(gameView.getHandView(rootLogicalPlayerIdx).size() == 0)
        {
            return 1.0f;
        }

        for(int i = 0; i < gameView.getNumPlayers(); ++i)
        {
            if(i != rootLogicalPlayerIdx && gameView.getHandView(i).size() == 0)
            {
                return -1.0f;
            }
        }

        final int rootHandSize = gameView.getHandView(rootLogicalPlayerIdx).size();
        int opponentHandTotal = 0;
        for(int i = 0; i < gameView.getNumPlayers(); ++i)
        {
            if(i != rootLogicalPlayerIdx)
            {
                opponentHandTotal += gameView.getHandView(i).size();
            }
        }

        final float averageOpponentHandSize = (float)opponentHandTotal / Math.max(1, gameView.getNumPlayers() - 1);
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

    private static Game buildDeterminizedGame(final GameView view, final Random random)
    {
        final Agent[] proxies = buildProxyAgents(view);

        final Hand[] determinizedHands = new Hand[view.getNumPlayers()];
        for(int logicalIdx = 0; logicalIdx < view.getNumPlayers(); ++logicalIdx)
        {
            final HandView handView = view.getHandView(logicalIdx);
            final Hand hand = new Hand();
            for(int i = 0; i < handView.size(); ++i)
            {
                final Card card = handView.getCard(i);
                if(isUnknownCard(card))
                {
                    hand.add(samplePlausibleCard(random));
                } else
                {
                    hand.add(new Card(card.color(), card.value()));
                }
            }
            determinizedHands[logicalIdx] = hand;
        }

        final Deck determinizedDrawPile = new Deck(false);
        for(final Card card: view.getDrawPile())
        {
            if(isUnknownCard(card))
            {
                determinizedDrawPile.add(samplePlausibleCard(random));
            } else
            {
                determinizedDrawPile.add(new Card(card.color(), card.value()));
            }
        }

        return new Game(determinizedDrawPile,
                        determinizedHands,
                        view.getObservability(),
                        view,
                        proxies);
    }

    private static boolean isUnknownCard(final Card card)
    {
        return card != null && (card.color() == Color.UNKNOWN || card.value() == Value.UNKNOWN);
    }

    private static Card samplePlausibleCard(final Random random)
    {
        final Color color = DETERMINIZATION_COLORS[random.nextInt(DETERMINIZATION_COLORS.length)];
        final Value value = DETERMINIZATION_VALUES[random.nextInt(DETERMINIZATION_VALUES.length)];
        return new Card(color, value);
    }
}