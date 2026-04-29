package labs.cp;


// SYSTEM IMPORTS
import edu.bu.jmat.Matrix;
import edu.bu.jmat.Pair;
import edu.bu.jnn.Model;

import java.util.Random;


// JAVA PROJECT IMPORTS


public class ReplayBuffer
    extends Object
{

    public static enum ReplacementType
    {
        RANDOM,
        OLDEST;
    }

    private ReplacementType     type;
    private int                 size;
    private int                 newestSampleIdx;

    private Matrix              prevStates;
    private Matrix              rewards;
    private Matrix              nextStates;
    private boolean             isStateTerminalMask[];

    private Random              rng;

    public ReplayBuffer(ReplacementType type,
                        int numSamples,
                        int dim,
                        Random rng)
    {
        this.type = type;
        this.size = 0;
        this.newestSampleIdx = -1;

        this.prevStates = Matrix.zeros(numSamples, dim);
        this.rewards = Matrix.zeros(numSamples, 1);
        this.nextStates = Matrix.zeros(numSamples, dim);
        this.isStateTerminalMask = new boolean[numSamples];

        this.rng = rng;

    }

    public int size() { return this.size; }
    public final ReplacementType getReplacementType() { return this.type; }
    private int getNewestSampleIdx() { return this.newestSampleIdx; }
    private Matrix getPrevStates() { return this.prevStates; }
    private Matrix getNextStates() { return this.nextStates; }
    private Matrix getRewards() { return this.rewards; }
    private boolean[] getIsStateTerminalMask() { return this.isStateTerminalMask; }

    private Random getRandom() { return this.rng; }

    private void setSize(int i) { this.size = i; }
    private void setNewestSampleIdx(int i) { this.newestSampleIdx = i; }

    private int chooseSampleToEvict()
    {
        int idxToEvict = -1;

        switch(this.getReplacementType())
        {
            case RANDOM:
                idxToEvict = this.getRandom().nextInt(this.getNextStates().getShape().numRows());
                break;
            case OLDEST:
                idxToEvict = (this.getNewestSampleIdx() + 1) % this.getNextStates().getShape().numRows();
                break;
            default:
                System.err.println("[ERROR] ReplayBuffer.chooseSampleToEvict: unknown replacement type "
                    + this.getReplacementType());
                System.exit(-1);
        }

        return idxToEvict;
    }

    public void addSample(Matrix prevState,
                          double reward,
                          Matrix nextState)
    {
        final int capacity = this.getPrevStates().getShape().numRows();
        final int insertIdx;

        if(this.size() < capacity)
        {
            insertIdx = this.size();
            this.setSize(this.size() + 1);
        } else
        {
            insertIdx = this.chooseSampleToEvict();
            if(this.getReplacementType() == ReplacementType.OLDEST)
            {
                this.setNewestSampleIdx((this.getNewestSampleIdx() + 1) % capacity);
            }
        }

        try
        {
            this.getPrevStates().copySlice(insertIdx, insertIdx + 1, 0, this.getPrevStates().getShape().numCols(),
                                           prevState);
        } catch(Exception e)
        {
            e.printStackTrace();
            System.exit(-1);
        }

        this.getRewards().set(insertIdx, 0, reward);

        final boolean terminal = (nextState == null);
        this.getIsStateTerminalMask()[insertIdx] = terminal;

        if(!terminal)
        {
            try
            {
                this.getNextStates().copySlice(insertIdx, insertIdx + 1, 0, this.getNextStates().getShape().numCols(),
                                               nextState);
            } catch(Exception e)
            {
                e.printStackTrace();
                System.exit(-1);
            }
        }
    }

    public static double max(Matrix qValues) throws IndexOutOfBoundsException
    {
        double maxVal = 0;
        boolean initialized = false;

        for(int colIdx = 0; colIdx < qValues.getShape().numCols(); ++colIdx)
        {
            double qVal = qValues.get(0, colIdx);
            if(!initialized || qVal > maxVal)
            {
                maxVal = qVal;
                initialized = true;
            }
        }
        return maxVal;
    }


    public Matrix getGroundTruth(Model qFunction,
                                 double discountFactor)
    {
        // This method should calculate the bellman update for temporal difference learning so that
        // we can use it as ground truth for updating our neural network
        //
        // Remember, the bellman ground truth we want for a Q function looks like this:
        //      R(s) + \gamma * max_{a'} Q(s', a')

        // Since the number of actions is fixed in the CartPole (cp) world, we don't need to include
        // action information directly in the input vector to the q function. Instead, we'll make the neural
        // network always produce (in this case since there are 2 actions) 2 q values: one per action.
        // So whenever we need to max_{a'} Q(s', a'), we're literally going to feed s' into our network,
        // which will produce two scores, one for a_1' and one for a_2'. We can choose max_{a'} Q(s', a')
        // by choosing whichever value is largest!

        // Now note that this bellman update reduces to just R(s) whenever we're processing a terminal transition
        // (so s' doesn't exist).

        // This method should calculate a column vector. The number of rows in this column vector is equal to the
        // number of transitions currently stored in the ReplayBuffer. Each row corresponds to a transition
        // which could either be (s, r, s') or (s, r, null), so when calculating the bellman update for that row,
        // you need to check the mask to see which version you're calculating! 

        final int n = this.size();
        // CartPole has two discrete actions; the bundled q-network outputs one value per action.
        final int numQ = 2;
        Matrix y = Matrix.zeros(n, numQ);

        for(int rIdx = 0; rIdx < n; ++rIdx)
        {
            final double rs = this.getRewards().get(rIdx, 0);
            double target = rs;

            if(!this.getIsStateTerminalMask()[rIdx])
            {
                try
                {
                    Matrix qNext = qFunction.forward(this.getNextStates().getRow(rIdx));
                    target = rs + discountFactor * ReplayBuffer.max(qNext);
                } catch(Exception e)
                {
                    e.printStackTrace();
                    System.exit(-1);
                }
            }

            for(int cIdx = 0; cIdx < numQ; ++cIdx)
            {
                y.set(rIdx, cIdx, target);
            }
        }

        return y;
    }

    public Pair<Matrix, Matrix> getTrainingData(Model qFunction,
                                                double discountFactor)
    {
        Matrix X = Matrix.zeros(this.size(), this.getPrevStates().getShape().numCols());
        try
        {
            for(int rIdx = 0; rIdx < this.size(); ++rIdx)
            {
                X.copySlice(rIdx, rIdx+1, 0, X.getShape().numCols(),
                            this.getPrevStates().getRow(rIdx));
            }
        } catch(Exception e)
        {
            e.printStackTrace();
            System.exit(-1);
        }
        Matrix YGt = this.getGroundTruth(qFunction, discountFactor);

        return new Pair<Matrix, Matrix>(X, YGt);
    }

}

