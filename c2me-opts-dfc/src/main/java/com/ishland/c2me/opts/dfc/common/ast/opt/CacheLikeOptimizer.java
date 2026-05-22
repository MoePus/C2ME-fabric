package com.ishland.c2me.opts.dfc.common.ast.opt;

import com.ishland.c2me.opts.dfc.common.ast.AstNode;
import com.ishland.c2me.opts.dfc.common.ast.ReferenceCounts;
import com.ishland.c2me.opts.dfc.common.ast.binary.AbstractBinaryNode;
import com.ishland.c2me.opts.dfc.common.ast.binary.AddNode;
import com.ishland.c2me.opts.dfc.common.ast.binary.MaxNode;
import com.ishland.c2me.opts.dfc.common.ast.binary.MaxShortNode;
import com.ishland.c2me.opts.dfc.common.ast.binary.MinNode;
import com.ishland.c2me.opts.dfc.common.ast.binary.MinShortNode;
import com.ishland.c2me.opts.dfc.common.ast.binary.MulNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.CacheLikeNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.ConstantNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.RangeChoiceNode;
import com.ishland.c2me.opts.dfc.common.ast.noise.DFTWeirdScaledSamplerNode;
import com.ishland.c2me.opts.dfc.common.ast.noise.ShiftedNoiseNode;
import com.ishland.c2me.opts.dfc.common.ast.spline.SplineAstNode;
import com.ishland.c2me.opts.dfc.common.ast.unary.AbstractUnaryNode;
import com.ishland.c2me.opts.dfc.common.ast.unary.AbsNode;
import com.ishland.c2me.opts.dfc.common.ast.unary.CubeNode;
import com.ishland.c2me.opts.dfc.common.ast.unary.NegMulNode;
import com.ishland.c2me.opts.dfc.common.ast.unary.NegNode;
import com.ishland.c2me.opts.dfc.common.ast.unary.SquareNode;
import com.ishland.c2me.opts.dfc.common.ast.unary.SqueezeNode;
import com.ishland.c2me.opts.dfc.common.ducks.IFastCacheLike;
import net.minecraft.world.gen.densityfunction.DensityFunctionTypes;

public final class CacheLikeOptimizer {

    private static final int CACHE2D_INSERT_THRESHOLD = 32;
    private static final int CACHE_ONCE_INSERT_THRESHOLD = 96;

    private final CacheFactory cacheFactory;
    private final ReferenceCounts referenceCounts;

    private CacheLikeOptimizer(CacheFactory cacheFactory, ReferenceCounts referenceCounts) {
        this.cacheFactory = cacheFactory;
        this.referenceCounts = referenceCounts;
    }

    public static AstNode optimize(AstNode node) {
        return optimize(node, DefaultCacheFactory.INSTANCE);
    }

    static AstNode optimize(AstNode node, CacheFactory cacheFactory) {
        AstNode cleaned = stripMemoCaches(node);
        ReferenceCounts referenceCounts = ReferenceCounts.collect(cleaned);
        return new CacheLikeOptimizer(cacheFactory, referenceCounts).insertCaches(cleaned);
    }

    private static AstNode stripMemoCaches(AstNode node) {
        return stripNestedCaches(node, null, false);
    }

    private static AstNode stripNestedCaches(AstNode node, CacheLikeKind absorbingOuter, boolean directChild) {
        if (node instanceof CacheLikeNode cache) {
            return stripCache(cache, absorbingOuter, directChild);
        }
        return rewriteChildrenForStrip(node, absorbingOuter);
    }

    private static AstNode stripCache(CacheLikeNode node, CacheLikeKind absorbingOuter, boolean directChild) {
        CacheLikeKind nodeKind = CacheLikeKind.from(node.getCacheLike());
        AstNode delegate = stripNestedCaches(node.getDelegate(), nodeKind, true);
        CacheLikeNode stripped = delegate == node.getDelegate() ? node : new CacheLikeNode(node.getCacheLike(), delegate);
        if (absorbingOuter != null) {
            boolean isDelegateYIndependent = YDependencyAnalyzer.isYIndependent(delegate);
            if (canAbsorb(absorbingOuter, nodeKind, isDelegateYIndependent, directChild)) {
                return stripNestedCaches(delegate, absorbingOuter, directChild);
            }
        }
        return stripped;
    }

    private static boolean canAbsorb(CacheLikeKind outer, CacheLikeKind inner, boolean isDelegateYIndependent, boolean directChild) {
        if (!CacheLikeKind.canAbsorb(outer, inner, isDelegateYIndependent)) {
            return false;
        }
        return directChild || inner != CacheLikeKind.INTERPOLATED;
    }

    private static AstNode rewriteChildrenForStrip(AstNode node, CacheLikeKind absorbingOuter) {
        return switch (node) {
            case AbstractBinaryNode binary -> stripBinary(binary, absorbingOuter);
            case AbstractUnaryNode unary -> stripUnary(unary, absorbingOuter);
            case RangeChoiceNode range -> stripRangeChoice(range, absorbingOuter);
            case ShiftedNoiseNode shiftedNoise -> stripShiftedNoise(shiftedNoise, absorbingOuter);
            case DFTWeirdScaledSamplerNode weirdScaled -> stripWeirdScaledSampler(weirdScaled, absorbingOuter);
            case SplineAstNode spline -> spline.mapLocationFunctions(child -> stripNestedCaches(child, absorbingOuter, false));
            default -> node;
        };
    }

    private static AstNode stripBinary(AbstractBinaryNode node, CacheLikeKind absorbingOuter) {
        AstNode left = stripNestedCaches(node.getLeft(), absorbingOuter, false);
        AstNode right = stripNestedCaches(node.getRight(), absorbingOuter, false);
        if (left == node.getLeft() && right == node.getRight()) {
            return node;
        }
        return switch (node) {
            case AddNode ignored -> new AddNode(left, right);
            case MulNode ignored -> new MulNode(left, right);
            case MinNode ignored -> new MinNode(left, right);
            case MaxNode ignored -> new MaxNode(left, right);
            case MinShortNode minShort -> new MinShortNode(left, right, minShort.getRightMin());
            case MaxShortNode maxShort -> new MaxShortNode(left, right, maxShort.getRightMax());
            default -> node;
        };
    }

    private static AstNode stripUnary(AbstractUnaryNode node, CacheLikeKind absorbingOuter) {
        AstNode operand = stripNestedCaches(node.getOperand(), absorbingOuter, false);
        if (operand == node.getOperand()) {
            return node;
        }
        return switch (node) {
            case AbsNode ignored -> new AbsNode(operand);
            case SquareNode ignored -> new SquareNode(operand);
            case CubeNode ignored -> new CubeNode(operand);
            case NegNode ignored -> new NegNode(operand);
            case NegMulNode negMul -> new NegMulNode(operand, negMul.getNegMul());
            case SqueezeNode ignored -> new SqueezeNode(operand);
            default -> node;
        };
    }

    private static AstNode stripRangeChoice(RangeChoiceNode node, CacheLikeKind absorbingOuter) {
        AstNode input = stripNestedCaches(node.getInput(), absorbingOuter, false);
        AstNode whenInRange = stripNestedCaches(node.getWhenInRange(), absorbingOuter, false);
        AstNode whenOutOfRange = stripNestedCaches(node.getWhenOutOfRange(), absorbingOuter, false);
        if (input == node.getInput() && whenInRange == node.getWhenInRange() && whenOutOfRange == node.getWhenOutOfRange()) {
            return node;
        }
        return new RangeChoiceNode(input, node.getMinInclusive(), node.getMaxExclusive(), whenInRange, whenOutOfRange);
    }

    private static AstNode stripShiftedNoise(ShiftedNoiseNode node, CacheLikeKind absorbingOuter) {
        AstNode shiftX = stripNestedCaches(node.getShiftX(), absorbingOuter, false);
        AstNode shiftY = stripNestedCaches(node.getShiftY(), absorbingOuter, false);
        AstNode shiftZ = stripNestedCaches(node.getShiftZ(), absorbingOuter, false);
        if (shiftX == node.getShiftX() && shiftY == node.getShiftY() && shiftZ == node.getShiftZ()) {
            return node;
        }
        return new ShiftedNoiseNode(shiftX, shiftY, shiftZ, node.getXzScale(), node.getYScale(), node.getNoise());
    }

    private static AstNode stripWeirdScaledSampler(DFTWeirdScaledSamplerNode node, CacheLikeKind absorbingOuter) {
        AstNode input = stripNestedCaches(node.getInput(), absorbingOuter, false);
        if (input == node.getInput()) {
            return node;
        }
        return new DFTWeirdScaledSamplerNode(input, node.getNoise(), node.getMapper());
    }

    private AstNode insertCaches(AstNode node) {
        if (node instanceof CacheLikeNode) {
            return node;
        }
        AstNode rewritten = rewriteChildren(node);
        return wrapIfBeneficial(node, rewritten);
    }

    private AstNode wrapIfBeneficial(AstNode original, AstNode rewritten) {
        int nodeCost = AstCostEstimator.estimate(original);
        int referenceCount = this.referenceCounts.get(original);
        if (nodeCost <= AstCostEstimator.CACHE_QUERY_COST || referenceCount <= 1) {
            return rewritten;
        }
        boolean yIndependent = YDependencyAnalyzer.isYIndependent(original);
        int threshold = yIndependent ? CACHE2D_INSERT_THRESHOLD : CACHE_ONCE_INSERT_THRESHOLD;
        if (nodeCost * referenceCount <= threshold) {
            return rewritten;
        }
        CacheLikeKind kind = yIndependent ? CacheLikeKind.CACHE2D : CacheLikeKind.CACHE_ONCE;
        return new CacheLikeNode(this.cacheFactory.create(kind), rewritten);
    }

    private AstNode rewriteChildren(AstNode node) {
        return switch (node) {
            case AbstractBinaryNode binary -> rewriteBinary(binary);
            case AbstractUnaryNode unary -> rewriteUnary(unary);
            case RangeChoiceNode range -> rewriteRangeChoice(range);
            case ShiftedNoiseNode shiftedNoise -> rewriteShiftedNoise(shiftedNoise);
            case DFTWeirdScaledSamplerNode weirdScaled -> rewriteWeirdScaledSampler(weirdScaled);
            case SplineAstNode spline -> rewriteSpline(spline);
            default -> node;
        };
    }

    private AstNode rewriteBinary(AbstractBinaryNode node) {
        AstNode left = insertCaches(node.getLeft());
        AstNode right = insertCaches(node.getRight());
        if (left == node.getLeft() && right == node.getRight()) {
            return node;
        }
        return switch (node) {
            case AddNode ignored -> new AddNode(left, right);
            case MulNode ignored -> new MulNode(left, right);
            case MinNode ignored -> new MinNode(left, right);
            case MaxNode ignored -> new MaxNode(left, right);
            case MinShortNode minShort -> new MinShortNode(left, right, minShort.getRightMin());
            case MaxShortNode maxShort -> new MaxShortNode(left, right, maxShort.getRightMax());
            default -> node;
        };
    }

    private AstNode rewriteUnary(AbstractUnaryNode node) {
        AstNode operand = insertCaches(node.getOperand());
        if (operand == node.getOperand()) {
            return node;
        }
        return switch (node) {
            case AbsNode ignored -> new AbsNode(operand);
            case SquareNode ignored -> new SquareNode(operand);
            case CubeNode ignored -> new CubeNode(operand);
            case NegNode ignored -> new NegNode(operand);
            case NegMulNode negMul -> new NegMulNode(operand, negMul.getNegMul());
            case SqueezeNode ignored -> new SqueezeNode(operand);
            default -> node;
        };
    }

    private AstNode rewriteRangeChoice(RangeChoiceNode node) {
        AstNode input = insertCaches(node.getInput());
        AstNode whenInRange = insertCaches(node.getWhenInRange());
        AstNode whenOutOfRange = insertCaches(node.getWhenOutOfRange());
        if (input == node.getInput() && whenInRange == node.getWhenInRange() && whenOutOfRange == node.getWhenOutOfRange()) {
            return node;
        }
        return new RangeChoiceNode(input, node.getMinInclusive(), node.getMaxExclusive(), whenInRange, whenOutOfRange);
    }

    private AstNode rewriteShiftedNoise(ShiftedNoiseNode node) {
        AstNode shiftX = insertCaches(node.getShiftX());
        AstNode shiftY = insertCaches(node.getShiftY());
        AstNode shiftZ = insertCaches(node.getShiftZ());
        if (shiftX == node.getShiftX() && shiftY == node.getShiftY() && shiftZ == node.getShiftZ()) {
            return node;
        }
        return new ShiftedNoiseNode(shiftX, shiftY, shiftZ, node.getXzScale(), node.getYScale(), node.getNoise());
    }

    private AstNode rewriteWeirdScaledSampler(DFTWeirdScaledSamplerNode node) {
        AstNode input = insertCaches(node.getInput());
        if (input == node.getInput()) {
            return node;
        }
        return new DFTWeirdScaledSamplerNode(input, node.getNoise(), node.getMapper());
    }

    private AstNode rewriteSpline(SplineAstNode node) {
        return node.mapLocationFunctions(this::insertCaches);
    }

    interface CacheFactory {

        IFastCacheLike create(CacheLikeKind kind);

    }

    private enum DefaultCacheFactory implements CacheFactory {
        INSTANCE;

        @Override
        public IFastCacheLike create(CacheLikeKind kind) {
            return (IFastCacheLike) (Object) new DensityFunctionTypes.Wrapping(
                    kind.toWrappingType(),
                    DensityFunctionTypes.constant(0.0)
            );
        }
    }
}
