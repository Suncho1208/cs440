package labs.doppeltree.models;


// SYSTEM IMPORTS
import edu.bu.jmat.Matrix;
import edu.bu.jmat.Pair;
import edu.bu.labs.doppeltree.features.*;
import edu.bu.labs.doppeltree.enums.*;
import edu.bu.labs.doppeltree.models.Model;


// JAVA PROJECT IMPORTS
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class DecisionTreeModel
    extends Model
{

    private static final boolean PRUNE_AFTER_TRAINING = true;
    private static final double COST_ADMIT_DOPPELGANGER = 25.0;
    private static final double COST_REJECT_HUMAN = 1.0;
    private static final double TRAIN_FRACTION_FOR_TREE = 0.82;
    // an abstract Node type. This is extended to make Interior Nodes and Leaf Nodes
    public static abstract class Node
        extends Object
    {

        // the dataset that was used to construct this node
        private Matrix X;
        private Matrix y_gt;
        private FeatureHeader featureHeader;

        public Node(final Matrix X,
                    final Matrix y_gt,
                    final FeatureHeader featureHeader)
        {
            this.X = X;
            this.y_gt = y_gt;
            this.featureHeader = featureHeader;
        }

        public final Matrix getX() { return this.X; }
        public final Matrix getY() { return this.y_gt; }
        public final FeatureHeader getFeatureHeader() { return this.featureHeader; }

        // a method to get the majority class (i.e. the most popular class) from ground truth.
        public int getMajorityClass(final Matrix X,
                                    final Matrix y_gt)
        {
            Pair<Matrix, Matrix> uniqueYGtAndCounts = y_gt.unique();
            Matrix uniqueYGtVals = uniqueYGtAndCounts.first();
            Matrix counts = uniqueYGtAndCounts.second();

            // find the argmax of the counts
            int rowIdxOfMaxCount = -1;
            double maxCount = Double.NEGATIVE_INFINITY;

            for(int rowIdx = 0; rowIdx < counts.getShape().numRows(); ++rowIdx)
            {
                if(counts.get(rowIdx, 0) > maxCount)
                {
                    rowIdxOfMaxCount = rowIdx;
                    maxCount = counts.get(rowIdx, 0);
                }
            }

            return (int)uniqueYGtVals.get(rowIdxOfMaxCount, 0);
        }

        // an abstract method to predict the class for this example
        public abstract int predict(final Matrix x);

        // an abstract method to get the datasets that each child node should be built from
        public abstract List<Pair<Matrix, Matrix> > getChildData() throws Exception;

    }

    // leaf node type
    public static class LeafNode
        extends Node
    {

        // a leaf node has the class label inside it
        private int predictedClass;

        public LeafNode(final Matrix X,
                        final Matrix y_gt,
                        final FeatureHeader featureHeader)
        {
            super(X, y_gt, featureHeader);
            this.predictedClass = this.getMajorityClass(X, y_gt);
        }

        public LeafNode(final Matrix X,
                        final Matrix y_gt,
                        final FeatureHeader featureHeader,
                        final int predictedClass)
        {
            super(X, y_gt, featureHeader);
            this.predictedClass = predictedClass;
        }

        @Override
        public int predict(final Matrix x)
        {
            // predict the class (an integer)
            return this.predictedClass;
        }

        // leaf nodes have no children
        @Override
        public List<Pair<Matrix, Matrix> > getChildData() throws Exception { return null; }

    }

    // interior node type
    public static class InteriorNode
        extends Node
    {

        static final class SplitChoice
        {
            final int featureIdx;
            final FeatureType featureType;
            final List<Double> splitValues;
            final Set<Integer> childColIdxs;

            SplitChoice(final int featureIdx,
                        final FeatureType featureType,
                        final List<Double> splitValues,
                        final Set<Integer> childColIdxs)
            {
                this.featureIdx = featureIdx;
                this.featureType = featureType;
                this.splitValues = splitValues;
                this.childColIdxs = childColIdxs;
            }
        }

        static SplitChoice chooseSplit(final Matrix X,
                                       final Matrix y_gt,
                                       final FeatureHeader featureHeader,
                                       final Set<Integer> availableColIdxs) throws Exception
        {
            List<Integer> cols = new ArrayList<Integer>(availableColIdxs);
            Collections.sort(cols);
            double hy = entropy(y_gt);
            int bestIdx = -1;
            double bestGain = Double.NEGATIVE_INFINITY;
            List<Double> bestSplits = null;
            for(int colIdx : cols)
            {
                Pair<Double, Matrix> p = getConditionalEntropy(
                    X, y_gt, colIdx, featureHeader);
                double ig = hy - p.first();
                if(ig > bestGain)
                {
                    bestGain = ig;
                    bestIdx = colIdx;
                    bestSplits = new ArrayList<Double>();
                    Matrix sm = p.second();
                    for(int r = 0; r < sm.getShape().numRows(); ++r)
                    {
                        bestSplits.add(sm.get(r, 0));
                    }
                }
            }
            if(bestIdx < 0 || bestSplits == null)
            {
                return new SplitChoice(-1, null, new ArrayList<Double>(),
                    new HashSet<Integer>());
            }
            FeatureType ft = featureHeader.getFeature(bestIdx).getFeatureType();
            Set<Integer> childCols = new HashSet<Integer>(availableColIdxs);
            if(ft.equals(FeatureType.DISCRETE))
            {
                childCols.remove(bestIdx);
            }
            return new SplitChoice(bestIdx, ft, bestSplits, childCols);
        }

        // the column index of the feature that this interior node has chosen
        private int             featureIdx;

        // the type (continuous or discrete) of the feature this interior node has chosen
        private FeatureType     featureType;

        // when we're processing a discrete feature, it is possible that even though that discrete feature
        // can take on any value in its domain (for example, like 5 values), the data we have may not contain
        // all of those values in it. Therefore, whenever we want to predict a test point, it is possible
        // that the test point has a discrete value that we haven't seen before. When we encounter such scenarios
        // we should predict the majority class (aka assign an "out-of-bounds" leaf node)
        private int             majorityClass;

        // the values of the feature that identify each child
        // if the feature this node has chosen is discrete, then |splitValues| = |children|
        // if the feature this node has chosen is continuous, then |splitValues| = 1 and |children| = 2
        private List<Double>    splitValues; 
        private List<Node>      children;

        // what features are the children of this node allowed to use?
        // this is different if the feature this node has chosen is discrete or continuous
        private Set<Integer>    childColIdxs;

        public InteriorNode(final Matrix X,
                            final Matrix y_gt,
                            final FeatureHeader featureHeader,
                            final SplitChoice sc)
        {
            super(X, y_gt, featureHeader);
            this.splitValues = new ArrayList<Double>(sc.splitValues);
            this.children = new ArrayList<Node>();
            this.majorityClass = this.getMajorityClass(X, y_gt);
            this.featureIdx = sc.featureIdx;
            this.featureType = sc.featureType;
            this.childColIdxs = new HashSet<Integer>(sc.childColIdxs);
        }

        //------------------------ some getters and setters (cause this is java) ------------------------
        public int getFeatureIdx() { return this.featureIdx; }
        public final FeatureType getFeatureType() { return this.featureType; }

        private List<Double> getSplitValues() { return this.splitValues; }
        private List<Node> getChildren() { return this.children; }

        public Set<Integer> getChildColIdxs() { return this.childColIdxs; }
        public int getMajorityClass() { return this.majorityClass; }
        //-----------------------------------------------------------------------------------------------

        // make sure we add children in the correct order when we use this!
        public void addChild(final Node n) { this.getChildren().add(n); }


        private static double entropy(final Matrix y_gt) throws Exception
        {
            Pair<Matrix, Matrix> uc = y_gt.unique();
            Matrix counts = uc.second();
            int n = y_gt.getShape().numRows();
            double h = 0.0;
            for(int i = 0; i < counts.getShape().numRows(); ++i)
            {
                double c = counts.get(i, 0);
                if(c <= 0.0)
                {
                    continue;
                }
                double p = c / (double)n;
                h -= p * (Math.log(p) / Math.log(2.0));
            }
            return h;
        }

        private static Pair<Double, Matrix> getConditionalEntropyDiscrete(final Matrix X,
                                                                          final Matrix y_gt,
                                                                          final int colIdx) throws Exception
        {
            Matrix featCol = X.getCol(colIdx);
            Pair<Matrix, Matrix> up = featCol.unique();
            Matrix uvals = up.first();
            List<Double> sorted = new ArrayList<Double>();
            for(int i = 0; i < uvals.getShape().numRows(); ++i)
            {
                sorted.add(uvals.get(i, 0));
            }
            Collections.sort(sorted);
            int n = X.getShape().numRows();
            double ce = 0.0;
            for(double v : sorted)
            {
                Matrix mask = X.getRowMaskEq(v, colIdx);
                Matrix ySub = y_gt.filterRows(mask);
                int nj = ySub.getShape().numRows();
                ce += (nj / (double)n) * InteriorNode.entropy(ySub);
            }
            Matrix splitMat = Matrix.zeros(sorted.size(), 1);
            for(int i = 0; i < sorted.size(); ++i)
            {
                splitMat.set(i, 0, sorted.get(i));
            }
            return new Pair<Double, Matrix>(ce, splitMat);
        }

        private static double entropyFromHistogram(final Map<Double, Integer> counts,
                                                   final int n)
        {
            if(n <= 0)
            {
                return 0.0;
            }
            double h = 0.0;
            for(int c : counts.values())
            {
                if(c <= 0)
                {
                    continue;
                }
                double p = (double)c / (double)n;
                h -= p * (Math.log(p) / Math.log(2.0));
            }
            return h;
        }

        private static double entropyRightFromTotalLeft(final Map<Double, Integer> total,
                                                        final Map<Double, Integer> left,
                                                        final int nR)
        {
            if(nR <= 0)
            {
                return 0.0;
            }
            double h = 0.0;
            for(Map.Entry<Double, Integer> e : total.entrySet())
            {
                int c = e.getValue() - left.getOrDefault(e.getKey(), 0);
                if(c <= 0)
                {
                    continue;
                }
                double p = (double)c / (double)nR;
                h -= p * (Math.log(p) / Math.log(2.0));
            }
            return h;
        }

        private static Pair<Double, Matrix> getConditionalEntropyContinuous(final Matrix X,
                                                                            final Matrix y_gt,
                                                                            final int colIdx) throws Exception
        {
            int n = X.getShape().numRows();
            Map<Double, Integer> totalY = new HashMap<Double, Integer>();
            for(int r = 0; r < n; ++r)
            {
                double yv = y_gt.get(r, 0);
                totalY.merge(yv, 1, Integer::sum);
            }
            List<Integer> order = new ArrayList<Integer>(n);
            for(int r = 0; r < n; ++r)
            {
                order.add(r);
            }
            Collections.sort(order, (a, b) ->
                Double.compare(X.get(a, colIdx), X.get(b, colIdx)));
            List<Double> runVals = new ArrayList<Double>();
            List<Map<Double, Integer> > runHist = new ArrayList<Map<Double, Integer> >();
            int k = 0;
            while(k < n)
            {
                int j = k;
                double v = X.get(order.get(k), colIdx);
                Map<Double, Integer> hm = new HashMap<Double, Integer>();
                while(j < n && Double.compare(X.get(order.get(j), colIdx), v) == 0)
                {
                    double yv = y_gt.get(order.get(j), 0);
                    hm.merge(yv, 1, Integer::sum);
                    ++j;
                }
                runVals.add(v);
                runHist.add(hm);
                k = j;
            }
            if(runVals.size() <= 1)
            {
                Matrix m = Matrix.zeros(1, 1);
                m.set(0, 0, runVals.get(0));
                return new Pair<Double, Matrix>(InteriorNode.entropy(y_gt), m);
            }
            Map<Double, Integer> left = new HashMap<Double, Integer>();
            int nL = 0;
            boolean found = false;
            double bestCe = 0.0;
            double bestT = 0.0;
            for(int r = 0; r < runHist.size() - 1; ++r)
            {
                Map<Double, Integer> run = runHist.get(r);
                int runSize = 0;
                for(int c : run.values())
                {
                    runSize += c;
                }
                for(Map.Entry<Double, Integer> e : run.entrySet())
                {
                    left.merge(e.getKey(), e.getValue(), Integer::sum);
                }
                nL += runSize;
                int nR = n - nL;
                if(nR == 0)
                {
                    continue;
                }
                double t = (runVals.get(r) + runVals.get(r + 1)) / 2.0;
                double hL = entropyFromHistogram(left, nL);
                double hR = entropyRightFromTotalLeft(totalY, left, nR);
                double ce = (nL / (double)n) * hL + (nR / (double)n) * hR;
                if(!found || ce < bestCe - 1e-15
                    || (Math.abs(ce - bestCe) <= 1e-15 && t < bestT))
                {
                    found = true;
                    bestCe = ce;
                    bestT = t;
                }
            }
            if(!found)
            {
                Matrix m = Matrix.zeros(1, 1);
                m.set(0, 0, runVals.get(0));
                return new Pair<Double, Matrix>(InteriorNode.entropy(y_gt), m);
            }
            Matrix splitMat = Matrix.zeros(1, 1);
            splitMat.set(0, 0, bestT);
            return new Pair<Double, Matrix>(bestCe, splitMat);
        }

        private static Pair<Double, Matrix> getConditionalEntropy(final Matrix X,
                                                                  final Matrix y_gt,
                                                                  final int colIdx,
                                                                  final FeatureHeader featureHeader) throws Exception
        {
            Feature f = featureHeader.getFeature(colIdx);
            if(f.getFeatureType().equals(FeatureType.DISCRETE))
            {
                return getConditionalEntropyDiscrete(X, y_gt, colIdx);
            }
            return getConditionalEntropyContinuous(X, y_gt, colIdx);
        }

        @Override
        public int predict(final Matrix x)
        {
            if(this.getFeatureType().equals(FeatureType.DISCRETE))
            {
                double val = x.get(0, this.getFeatureIdx());
                for(int i = 0; i < this.getSplitValues().size(); ++i)
                {
                    if(Double.compare(this.getSplitValues().get(i), val) == 0)
                    {
                        return this.getChildren().get(i).predict(x);
                    }
                }
                return this.getMajorityClass();
            }
            double val = x.get(0, this.getFeatureIdx());
            double t = this.getSplitValues().get(0);
            int idx = (val <= t) ? 0 : 1;
            return this.getChildren().get(idx).predict(x);
        }

        @Override
        public List<Pair<Matrix, Matrix> > getChildData() throws Exception
        {
            List<Pair<Matrix, Matrix> > out = new ArrayList<Pair<Matrix, Matrix> >();
            int fidx = this.getFeatureIdx();
            if(this.getFeatureType().equals(FeatureType.DISCRETE))
            {
                for(double v : this.getSplitValues())
                {
                    Matrix mask = this.getX().getRowMaskEq(v, fidx);
                    out.add(new Pair<Matrix, Matrix>(
                        this.getX().filterRows(mask),
                        this.getY().filterRows(mask)));
                }
            }
            else
            {
                double t = this.getSplitValues().get(0);
                Matrix mLe = this.getX().getRowMaskLe(t, fidx);
                Matrix mGt = this.getX().getRowMaskGt(t, fidx);
                out.add(new Pair<Matrix, Matrix>(
                    this.getX().filterRows(mLe),
                    this.getY().filterRows(mLe)));
                out.add(new Pair<Matrix, Matrix>(
                    this.getX().filterRows(mGt),
                    this.getY().filterRows(mGt)));
            }
            return out;
        }

    }




    private Node root;

    public DecisionTreeModel(final FeatureHeader featureHeader)
    {
        super(featureHeader);
        this.root = null;
    }

    public Node getRoot() { return this.root; }
    private void setRoot(Node n) { this.root = n; }

    private static int routeChildIndex(final InteriorNode in, final Matrix xrow)
    {
        if(in.getFeatureType().equals(FeatureType.DISCRETE))
        {
            double val = xrow.get(0, in.getFeatureIdx());
            for(int i = 0; i < in.getSplitValues().size(); ++i)
            {
                if(Double.compare(in.getSplitValues().get(i), val) == 0)
                {
                    return i;
                }
            }
            return -1;
        }
        double val = xrow.get(0, in.getFeatureIdx());
        double t = in.getSplitValues().get(0);
        return (val <= t) ? 0 : 1;
    }

    private static Matrix selectRows(final Matrix M,
                                     final List<Integer> idx,
                                     final int nCols) throws Exception
    {
        int k = idx.size();
        if(k == 0)
        {
            return Matrix.zeros(0, nCols);
        }
        Matrix out = Matrix.zeros(k, nCols);
        for(int i = 0; i < k; ++i)
        {
            int r = idx.get(i);
            for(int c = 0; c < nCols; ++c)
            {
                out.set(i, c, M.get(r, c));
            }
        }
        return out;
    }

    private static Matrix selectYRows(final Matrix y, final List<Integer> idx) throws Exception
    {
        int k = idx.size();
        if(k == 0)
        {
            return Matrix.zeros(0, 1);
        }
        Matrix out = Matrix.zeros(k, 1);
        for(int i = 0; i < k; ++i)
        {
            out.set(i, 0, y.get(idx.get(i), 0));
        }
        return out;
    }

    private static double lossForPrediction(final int pred, final double yTrue)
    {
        int yt = (int)Math.round(yTrue);
        if(pred == yt)
        {
            return 0.0;
        }
        if(pred == 1 && yt == 0)
        {
            return COST_ADMIT_DOPPELGANGER;
        }
        if(pred == 0 && yt == 1)
        {
            return COST_REJECT_HUMAN;
        }
        return 0.0;
    }

    private static int costMinLeafLabel(final Matrix y_gt)
    {
        int n0 = 0;
        int n1 = 0;
        int n = y_gt.getShape().numRows();
        for(int r = 0; r < n; ++r)
        {
            double v = y_gt.get(r, 0);
            if(Math.abs(v) < 0.5)
            {
                ++n0;
            }
            else
            {
                ++n1;
            }
        }
        double costReject = (double)n1 * COST_REJECT_HUMAN;
        double costAdmit = (double)n0 * COST_ADMIT_DOPPELGANGER;
        if(costReject < costAdmit)
        {
            return 0;
        }
        if(costAdmit < costReject)
        {
            return 1;
        }
        return (n1 >= n0) ? 1 : 0;
    }

    private static double subtreeValCost(final Node n,
                                         final Matrix Xv,
                                         final Matrix yv)
    {
        int nv = Xv.getShape().numRows();
        int nc = Xv.getShape().numCols();
        double t = 0.0;
        for(int r = 0; r < nv; ++r)
        {
            Matrix xrow = Xv.getSlice(r, r + 1, 0, nc);
            int pred = n.predict(xrow);
            t += lossForPrediction(pred, yv.get(r, 0));
        }
        return t;
    }

    private Node pruneRecursive(final Node node,
                                final Matrix Xv,
                                final Matrix yv) throws Exception
    {
        if(!(node instanceof InteriorNode))
        {
            return node;
        }
        InteriorNode in = (InteriorNode)node;
        int nc = Xv.getShape().numCols();
        int numCh = in.getChildren().size();
        List<List<Integer> > buck = new ArrayList<List<Integer> >();
        for(int i = 0; i < numCh; ++i)
        {
            buck.add(new ArrayList<Integer>());
        }
        int nv = Xv.getShape().numRows();
        for(int r = 0; r < nv; ++r)
        {
            Matrix xrow = Xv.getSlice(r, r + 1, 0, nc);
            int j = routeChildIndex(in, xrow);
            if(j >= 0 && j < numCh)
            {
                buck.get(j).add(r);
            }
        }
        List<Node> ch = in.getChildren();
        for(int i = 0; i < numCh; ++i)
        {
            Matrix Xc = selectRows(Xv, buck.get(i), nc);
            Matrix yc = selectYRows(yv, buck.get(i));
            ch.set(i, pruneRecursive(ch.get(i), Xc, yc));
        }
        double treeCost = subtreeValCost(in, Xv, yv);
        int leafLab = costMinLeafLabel(in.getY());
        double leafCost = 0.0;
        for(int r = 0; r < nv; ++r)
        {
            leafCost += lossForPrediction(leafLab, yv.get(r, 0));
        }
        if(leafCost <= treeCost)
        {
            return new LeafNode(in.getX(), in.getY(), this.getFeatureHeader(), leafLab);
        }
        return in;
    }

    private Node dfsBuild(Matrix X, Matrix y_gt, Set<Integer> availableColIdxs) throws Exception
    {
        int n = X.getShape().numRows();
        Pair<Matrix, Matrix> u = y_gt.unique();
        if(u.first().getShape().numRows() <= 1 || availableColIdxs.isEmpty())
        {
            return new LeafNode(X, y_gt, this.getFeatureHeader());
        }
        InteriorNode.SplitChoice sc = InteriorNode.chooseSplit(
            X, y_gt, this.getFeatureHeader(), availableColIdxs);
        if(sc.featureIdx < 0)
        {
            return new LeafNode(X, y_gt, this.getFeatureHeader());
        }
        InteriorNode inode = new InteriorNode(X, y_gt, this.getFeatureHeader(), sc);
        List<Pair<Matrix, Matrix> > childPairs = inode.getChildData();
        for(Pair<Matrix, Matrix> p : childPairs)
        {
            Matrix Xc = p.first();
            Matrix yc = p.second();
            Node c;
            if(Xc.getShape().numRows() == 0)
            {
                c = new LeafNode(inode.getX(), inode.getY(), this.getFeatureHeader());
            }
            else if(Xc.getShape().numRows() == n
                && inode.getFeatureType().equals(FeatureType.CONTINUOUS))
            {
                c = new LeafNode(Xc, yc, this.getFeatureHeader());
            }
            else
            {
                c = this.dfsBuild(Xc, yc, inode.getChildColIdxs());
            }
            inode.addChild(c);
        }
        return inode;
    }

    // did this for you, feel free to change the printouts if you want
    @Override
    public void train(final Matrix trainFeatures,
                      final Matrix trainGroundTruth)
    {
        System.out.println("DecisionTree.fit: X.shape=" + trainFeatures.getShape() +
            " y_gt.shape=" + trainGroundTruth.getShape());
        try
        {
            Set<Integer> allColIdxs = new HashSet<Integer>();
            for(int colIdx = 0; colIdx < trainFeatures.getShape().numCols(); ++colIdx)
            {
                allColIdxs.add(colIdx);
            }
            int n = trainFeatures.getShape().numRows();
            if(PRUNE_AFTER_TRAINING && n >= 16)
            {
                int nTrain = (int)Math.floor((double)n * TRAIN_FRACTION_FOR_TREE);
                if(nTrain < 1)
                {
                    nTrain = 1;
                }
                if(nTrain >= n)
                {
                    nTrain = n - 1;
                }
                Matrix maskTr = Matrix.zeros(n, 1);
                for(int i = 0; i < nTrain; ++i)
                {
                    maskTr.set(i, 0, 1.0);
                }
                Matrix maskVa = Matrix.zeros(n, 1);
                for(int i = nTrain; i < n; ++i)
                {
                    maskVa.set(i, 0, 1.0);
                }
                Matrix Xtr = trainFeatures.filterRows(maskTr);
                Matrix ytr = trainGroundTruth.filterRows(maskTr);
                Matrix Xva = trainFeatures.filterRows(maskVa);
                Matrix yva = trainGroundTruth.filterRows(maskVa);
                this.setRoot(this.dfsBuild(Xtr, ytr, allColIdxs));
                if(Xva.getShape().numRows() > 0)
                {
                    for(int rep = 0; rep < 3; ++rep)
                    {
                        this.setRoot(this.pruneRecursive(this.getRoot(), Xva, yva));
                    }
                }
            }
            else
            {
                this.setRoot(this.dfsBuild(trainFeatures, trainGroundTruth, allColIdxs));
            }
        } catch(Exception e)
        {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    // did this for you, feel free to change the printouts if you want
    @Override
    public int classify(final Matrix featureVec)
    {
        // class 0 means Human (i.e. not a zombie), class 1 means zombie
        System.out.println("DecisionTree.predict: x=" + featureVec);
        return this.getRoot().predict(featureVec);
    }
}
