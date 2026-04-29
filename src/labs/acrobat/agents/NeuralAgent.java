package labs.acrobat.agents;


// SYSTEM IMPORTS
import edu.bu.jmat.Matrix;
import edu.bu.jmat.Pair;
import edu.bu.jmat.Shape;
import edu.bu.jnn.LossFunction;
import edu.bu.jnn.Module;
import edu.bu.jnn.Model;
import edu.bu.jnn.Optimizer;
import edu.bu.jnn.Parameter;
import edu.bu.jnn.layers.*;
import edu.bu.jnn.losses.MeanSquaredError;
import edu.bu.jnn.models.Sequential;
import edu.bu.jnn.optimizers.*;
import edu.bu.labs.acrobat.agents.QAgent;
import edu.bu.labs.acrobat.utils.DiscreteUtils;
import net.sourceforge.argparse4j.inf.Namespace;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


// JAVA PROJECT IMPORTS
import labs.acrobat.ReplayBuffer;
import labs.acrobat.Dataset;


public class NeuralAgent
    extends Object
    implements QAgent
{

    private Model qFunction;
    private Optimizer optimizer;
    private LossFunction lossFunction;

    public NeuralAgent(final double lr)
    {
        Sequential m = new Sequential();

        // input is 6d
        m.add(new Dense(6, 36));
        m.add(new Sigmoid());

        // since the number of actions in this world is fixed, we can ask the network to predict
        // one q-value per (fixed ahead of time) actions. In this world there are three actions.
        m.add(new Dense(36, 3));

        this.qFunction = m;
        this.optimizer = new SGDOptimizer(qFunction.getParameters(), lr);
        this.lossFunction = new MeanSquaredError();
    }

    public NeuralAgent(Namespace ns) { this((double)(ns.get("lr"))); }

    public final Model getQFunction() { return this.qFunction; }
    public final Optimizer getOptimizer() { return this.optimizer; }
    public final LossFunction getLossFunction() { return this.lossFunction; }

    public final double getQValue(final Matrix state,
                                  final int action) throws Exception
    {
        return this.getQFunction().forward(state).get(0, action);
    }

    @Override
    public final void update(final Matrix states,
                             final Matrix actions,
                             final Matrix YGt,
                             final double lr) throws Exception
    {
        Matrix YHat = this.getQFunction().forward(states);
        this.getOptimizer().reset();
        this.getQFunction().backwards(states, this.getLossFunction().backwards(YHat, YGt));
        this.getOptimizer().step();
    }

    @Override
    public final int argmax(final Matrix state) throws Exception
    {
        Double bestQValue = null;
        int bestAction = -1;

        for(int action = 0; action < 3; ++action)
        {
            final double qValue = this.getQValue(state, action);
            if(bestQValue == null || qValue > bestQValue)
            {
                bestQValue = qValue;
                bestAction = action;
            }
        }
        return bestAction;
    }

    @Override
    public final double max(final Matrix state) throws Exception
    {
        return this.getQValue(state, this.argmax(state));
    }


    @Override
    public void save(final String filePath) throws Exception
    {
        this.getQFunction().save(filePath);
    }

    /**
     * jmat {@code DataBuffer.fromStringData} splits on every {@code '-'}, so lines like
     * {@code 36--0.79,...} (negative first entry after the count) break the stock loader.
     * Parse the count and CSV explicitly, then copy into each {@link Parameter} matrix.
     */
    private static Matrix matrixFromCheckpointLine(final String line) throws Exception
    {
        final int semi = line.indexOf(';');
        if(semi < 0)
        {
            throw new Exception("[NeuralAgent.load] missing ';' in checkpoint line");
        }
        final String shapeString = line.substring(0, semi);
        final String bufferString = line.substring(semi + 1);
        final Matcher m = Pattern.compile("^(\\d+)-(.*)$").matcher(bufferString);
        if(!m.matches())
        {
            throw new Exception("[NeuralAgent.load] malformed buffer prefix");
        }
        final int n = Integer.parseInt(m.group(1));
        final String csv = m.group(2);
        final String[] tokens = csv.split(",", -1);
        if(tokens.length != n)
        {
            throw new Exception("[NeuralAgent.load] expected " + n + " comma-separated values, got " + tokens.length);
        }
        final double[] data = new double[n];
        for(int i = 0; i < n; ++i)
        {
            data[i] = Double.parseDouble(tokens[i].trim());
        }
        final Shape shape = Shape.parseShapeString(shapeString);
        if(shape.numRows() * shape.numCols() != n)
        {
            throw new Exception("[NeuralAgent.load] shape numel mismatch");
        }
        final Matrix mat = Matrix.zeros(shape.numRows(), shape.numCols());
        int idx = 0;
        for(int r = 0; r < shape.numRows(); ++r)
        {
            for(int c = 0; c < shape.numCols(); ++c)
            {
                mat.set(r, c, data[idx++]);
            }
        }
        return mat;
    }

    @Override
    public void load(final String filePath) throws Exception
    {
        final List<String> lines = Files.readAllLines(Paths.get(filePath));
        final List<Matrix> mats = new ArrayList<>();
        for(String raw : lines)
        {
            final String line = raw.trim();
            if(line.isEmpty()) { continue; }
            mats.add(matrixFromCheckpointLine(line));
        }
        final List<Parameter> params = this.getQFunction().getParameters();
        if(mats.size() != params.size())
        {
            throw new Exception("[NeuralAgent.load] expected " + params.size()
                + " parameter tensors, file has " + mats.size());
        }
        for(int i = 0; i < params.size(); ++i)
        {
            final Matrix dst = params.get(i).getValue();
            final Matrix src = mats.get(i);
            if(dst.getShape().numRows() != src.getShape().numRows()
                || dst.getShape().numCols() != src.getShape().numCols())
            {
                throw new Exception("[NeuralAgent.load] shape mismatch at parameter index " + i);
            }
            for(int r = 0; r < dst.getShape().numRows(); ++r)
            {
                for(int c = 0; c < dst.getShape().numCols(); ++c)
                {
                    dst.set(r, c, src.get(r, c));
                }
            }
        }
    }
}
