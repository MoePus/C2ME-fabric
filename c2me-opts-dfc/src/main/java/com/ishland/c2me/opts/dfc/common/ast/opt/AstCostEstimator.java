package com.ishland.c2me.opts.dfc.common.ast.opt;

import com.ishland.c2me.opts.dfc.common.ast.AstNode;
import com.ishland.c2me.opts.dfc.common.ast.McToAst;
import com.ishland.c2me.opts.dfc.common.ast.binary.AbstractBinaryNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.CacheLikeNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.ConstantNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.DelegateNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.RangeChoiceNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.YClampedGradientNode;
import com.ishland.c2me.opts.dfc.common.ast.noise.DFTNoiseNode;
import com.ishland.c2me.opts.dfc.common.ast.noise.DFTShiftANode;
import com.ishland.c2me.opts.dfc.common.ast.noise.DFTShiftBNode;
import com.ishland.c2me.opts.dfc.common.ast.noise.DFTShiftNode;
import com.ishland.c2me.opts.dfc.common.ast.noise.DFTWeirdScaledSamplerNode;
import com.ishland.c2me.opts.dfc.common.ast.noise.ShiftedNoiseNode;
import com.ishland.c2me.opts.dfc.common.ast.spline.SplineAstNode;
import com.ishland.c2me.opts.dfc.common.ast.unary.AbstractUnaryNode;
import net.minecraft.util.math.Spline;
import net.minecraft.world.gen.densityfunction.DensityFunctionTypes;

final class AstCostEstimator {

    static final int CONSTANT_COST = 1;
    static final int CACHE_QUERY_COST = 4;
    static final int UNARY_SELF_COST = 2;
    static final int BINARY_SELF_COST = 3;
    static final int RANGE_CHOICE_SELF_COST = 8;
    static final int Y_GRADIENT_COST = 5;
    // DoublePerlinNoiseSampler: two OctavePerlinNoiseSamplers, each with multiple Perlin octaves;
    // every octave samples 8 grid corners with permutation-table lookups and dot products.
    static final int NOISE_COST = 240;
    // Single-octave Perlin (DFTShift/A/B use offsetNoise, configured as one octave).
    static final int SHIFT_NOISE_COST = 20;
    // Single Perlin sample plus scalar math; rarity mapper is a fast lookup.
    static final int WEIRD_SCALED_SAMPLER_COST = 24;
    static final int DELEGATE_COST = 24;
    static final int DEFAULT_SELF_COST = 8;
    static final int SPLINE_SELF_COST = 12;
    static final int SPLINE_FIXED_VALUE_COST = 1;
    static final int SPLINE_VALUE_SAMPLE_MULTIPLIER = 2;

    private AstCostEstimator() {
    }

    static int estimate(AstNode node) {
        if (node instanceof CacheLikeNode) {
            return CACHE_QUERY_COST;
        }
        if (node instanceof SplineAstNode splineAstNode) {
            return estimateSpline(splineAstNode.getSpline());
        }
        int cost = selfCost(node);
        for (AstNode child : node.getChildren()) {
            cost += estimate(child);
        }
        return cost;
    }

    private static int estimateSpline(Spline<DensityFunctionTypes.Spline.SplinePos, DensityFunctionTypes.Spline.DensityFunctionWrapper> spline) {
        if (spline instanceof Spline.FixedFloatFunction<DensityFunctionTypes.Spline.SplinePos, DensityFunctionTypes.Spline.DensityFunctionWrapper>) {
            return SPLINE_FIXED_VALUE_COST;
        }
        if (spline instanceof Spline.Implementation<DensityFunctionTypes.Spline.SplinePos, DensityFunctionTypes.Spline.DensityFunctionWrapper> impl) {
            int locationCost = estimate(McToAst.toAst(impl.locationFunction().function().value()));
            return splineImplementationCost(locationCost, averageValueCost(impl));
        }
        return DEFAULT_SELF_COST;
    }

    static int splineImplementationCost(int locationCost, int averageValueCost) {
        return SPLINE_SELF_COST + SPLINE_VALUE_SAMPLE_MULTIPLIER * averageValueCost + locationCost;
    }

    private static int averageValueCost(Spline.Implementation<DensityFunctionTypes.Spline.SplinePos, DensityFunctionTypes.Spline.DensityFunctionWrapper> impl) {
        int valueCount = impl.values().size();
        if (valueCount == 0) {
            return 0;
        }
        int total = 0;
        for (Spline<DensityFunctionTypes.Spline.SplinePos, DensityFunctionTypes.Spline.DensityFunctionWrapper> value : impl.values()) {
            total += estimateSpline(value);
        }
        return total / valueCount;
    }

    private static int selfCost(AstNode node) {
        return switch (node) {
            case ConstantNode ignored -> CONSTANT_COST;
            case AbstractUnaryNode ignored -> UNARY_SELF_COST;
            case AbstractBinaryNode ignored -> BINARY_SELF_COST;
            case RangeChoiceNode ignored -> RANGE_CHOICE_SELF_COST;
            case YClampedGradientNode ignored -> Y_GRADIENT_COST;
            case DFTNoiseNode ignored -> NOISE_COST;
            case DFTShiftNode ignored -> SHIFT_NOISE_COST;
            case DFTShiftANode ignored -> SHIFT_NOISE_COST;
            case DFTShiftBNode ignored -> SHIFT_NOISE_COST;
            case ShiftedNoiseNode ignored -> NOISE_COST;
            case DFTWeirdScaledSamplerNode ignored -> WEIRD_SCALED_SAMPLER_COST;
            case DelegateNode ignored -> DELEGATE_COST;
            default -> DEFAULT_SELF_COST;
        };
    }
}
