package org.deeplearning4j.nn.graph.vertex.impl;

import com.google.common.primitives.Ints;
import org.deeplearning4j.nn.api.Layer;
import org.deeplearning4j.nn.api.MaskState;
import org.deeplearning4j.nn.gradient.Gradient;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.deeplearning4j.nn.graph.vertex.BaseGraphVertex;
import org.deeplearning4j.nn.graph.vertex.VertexIndices;
import org.deeplearning4j.nn.workspace.ArrayType;
import org.deeplearning4j.nn.workspace.LayerWorkspaceMgr;
import org.nd4j.base.Preconditions;
import org.nd4j.evaluation.meta.Prediction;
import org.nd4j.linalg.api.memory.MemoryWorkspace;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.api.ops.impl.broadcast.BroadcastMulOp;
import org.nd4j.linalg.api.ops.impl.reduce3.Dot;
import org.nd4j.linalg.api.ops.impl.reduce3.EuclideanDistance;
import org.nd4j.linalg.factory.Broadcast;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.primitives.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DotProductVertex extends BaseGraphVertex {

    private int[] dimensions;

    public DotProductVertex(ComputationGraph graph, String name, int vertexIndex, int[] dimensions) {
        this(graph, name, vertexIndex, null, null, dimensions);
    }

    public DotProductVertex(ComputationGraph graph, String name, int vertexIndex, VertexIndices[] inputVertices,
                             VertexIndices[] outputVertices, int[] dimensions) {
        super(graph, name, vertexIndex, inputVertices, outputVertices);
        this.dimensions = dimensions;
    }

    public void setDimensions(int[] dimensions) {
        this.dimensions = dimensions;
    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();
        sb.append("DotProductVertex(id=").append(vertexIndex).append(",name=\"").append(vertexName).append("\",inputs=")
                .append(Arrays.toString(inputVertices)).append(",outputs=")
                .append(Arrays.toString(outputVertices)).append(")");
        return sb.toString();
    }

    @Override
    public boolean hasLayer() {
        return false;
    }

    @Override
    public Layer getLayer() {
        return null;
    }

    @Override
    public INDArray doForward(boolean training, LayerWorkspaceMgr workspaceMgr) {
        INDArray a = inputs[0];
        INDArray b = inputs[1];
        Preconditions.checkState(Arrays.equals(a.shape(), b.shape()));
        boolean validDimensions = true;
        for (int i  = 0; i < dimensions.length; ++i) {
            if (dimensions[i] <= 0 || dimensions[i] > a.rank()) {
                validDimensions = false;
                break;
            }
        }
        Preconditions.checkState(validDimensions && (a.rank() <= 4), "Operation not supported for rank %d", a.rank());
        try(MemoryWorkspace ws = workspaceMgr.notifyScopeBorrowed(ArrayType.ACTIVATIONS)) {
            INDArray result = Nd4j.getExecutioner().exec(new Dot(a,b, dimensions));
            if (a.rank() == 3 && result.rank() == 2) {
                return result.reshape(inputs[0].size(0), 1, -1);
            }
            else if (result.rank() <= 2) {
                return result.reshape(-1, 1);
            }
            return result;
        }
    }

    @Override
    public Pair<Gradient, INDArray[]> doBackward(boolean tbptt, LayerWorkspaceMgr workspaceMgr) {
        long epsLength = epsilon.length();
        if (epsLength == 1) {
            return new Pair<>(null, new INDArray[]{inputs[1].mul(epsilon), inputs[0].mul(epsilon)});
        }
        else {
            INDArray output0 = Nd4j.createUninitialized(inputs[1].dataType(), inputs[1].shape());
            INDArray output1 = Nd4j.createUninitialized(inputs[0].dataType(), inputs[0].shape());

            List<Integer> allDims = new ArrayList<>();
            for (int i = 0; i < inputs[0].rank(); ++i) {
                allDims.add(i);
            }
            List<Integer> provided = new ArrayList<>();
            for (int i : dimensions) {
                provided.add(i);
            }
            allDims.removeAll(provided);
            int[] inverted = Ints.toArray(allDims);

            BroadcastMulOp op0 = new BroadcastMulOp(inputs[1], epsilon, output1, inverted);
            BroadcastMulOp op1 = new BroadcastMulOp(inputs[0], epsilon, output0, inverted);

            Nd4j.getExecutioner().exec(op0);
            Nd4j.getExecutioner().exec(op1);

            return new Pair<>(null, new INDArray[]{output1, output0});
        }
    }

    @Override
    public void setBackpropGradientsViewArray(INDArray backpropGradientsViewArray) {
        if (backpropGradientsViewArray != null)
            throw new RuntimeException("Vertex does not have gradients; gradients view array cannot be set here");
    }

    @Override
    public Pair<INDArray, MaskState> feedForwardMaskArrays(INDArray[] maskArrays, MaskState currentMaskState, int minibatchSize) {
        //No op
        if (maskArrays == null || maskArrays.length == 0) {
            return null;
        }

        return new Pair<>(maskArrays[0], currentMaskState);
    }
}