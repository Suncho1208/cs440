package src.labs.rttt.agents;

import edu.bu.labs.rttt.agents.SearchAgent;
import edu.bu.labs.rttt.game.CellType;
import edu.bu.labs.rttt.game.PlayerType;
import edu.bu.labs.rttt.game.RecursiveTicTacToeGame.RecursiveTicTacToeGameView;
import edu.bu.labs.rttt.traversal.Node;
import java.util.List;
import src.labs.rttt.heuristics.Heuristics;

public class ExtraCreditAgent extends SearchAgent {
    public ExtraCreditAgent(PlayerType p) {
        super(p);
    }

    public Node search(Node n) {
        return ab(n, 5, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
    }

    private Node ab(Node n, int d, double a, double b) {
        if (n.isTerminal()) return n;
        if (d == 0) {
            n.setUtilityValue(ev(n));
            return n;
        }
        boolean max = n.getCurrentPlayerType() == getMyPlayerType();
        double v = max ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
        Node res = null;
        for (Node c : n.getChildren()) {
            ab(c, d - 1, a, b);
            double u = c.getUtilityValue();
            if (max) {
                if (u > v) {
                    v = u;
                    res = c;
                }
                a = Math.max(a, v);
            } else {
                if (u < v) {
                    v = u;
                    res = c;
                }
                b = Math.min(b, v);
            }
            if (b <= a) break;
        }
        n.setUtilityValue(v);
        return res;
    }

    private double ev(Node n) {
        if (n.isTerminal()) return n.getTerminalUtility();
        double s = Heuristics.calculateHeuristicValue(n);
        RecursiveTicTacToeGameView v = n.getView();
        PlayerType me = getMyPlayerType();
        PlayerType op = (me == PlayerType.X) ? PlayerType.O : PlayerType.X;
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                if (v.getOutcome(r, c) == null) s += evI(v.getGameView(r, c), me, op);
            }
        }
        return Math.max(-99.9, Math.min(99.9, s));
    }

    private double evI(edu.bu.labs.rttt.game.TicTacToeGame.TicTacToeGameView g, PlayerType me, PlayerType op) {
        double s = 0;
        for (int i = 0; i < 3; i++) {
            s += evL(g.getCellType(i, 0), g.getCellType(i, 1), g.getCellType(i, 2), me, op);
            s += evL(g.getCellType(0, i), g.getCellType(1, i), g.getCellType(2, i), me, op);
        }
        s += evL(g.getCellType(0, 0), g.getCellType(1, 1), g.getCellType(2, 2), me, op);
        s += evL(g.getCellType(0, 2), g.getCellType(1, 1), g.getCellType(2, 0), me, op);
        return s;
    }

    private double evL(CellType c1, CellType c2, CellType c3, PlayerType me, PlayerType op) {
        int m = 0, o = 0;
        if (c1 != null && c1.name().equals(me.name())) m++;
        else if (c1 != null && c1.name().equals(op.name())) o++;
        if (c2 != null && c2.name().equals(me.name())) m++;
        else if (c2 != null && c2.name().equals(op.name())) o++;
        if (c3 != null && c3.name().equals(me.name())) m++;
        else if (c3 != null && c3.name().equals(op.name())) o++;
        if (m > 0 && o > 0) return 0;
        if (m == 2) return 0.5;
        if (m == 1) return 0.1;
        if (o == 2) return -0.5;
        if (o == 1) return -0.1;
        return 0;
    }

    @Override
    public void afterGameEnds(final RecursiveTicTacToeGameView g) {}
}
