package com.ishland.c2me.opts.dfc.common.ast.opt;

import com.ishland.c2me.opts.dfc.common.ast.AstNode;
import com.ishland.c2me.opts.dfc.common.ast.McToAst;
import com.ishland.c2me.opts.dfc.common.ast.binary.AbstractBinaryNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.CacheLikeNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.ConstantNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.RangeChoiceNode;
import com.ishland.c2me.opts.dfc.common.ast.noise.DFTNoiseNode;
import com.ishland.c2me.opts.dfc.common.ast.noise.DFTShiftANode;
import com.ishland.c2me.opts.dfc.common.ast.noise.DFTShiftBNode;
import com.ishland.c2me.opts.dfc.common.ast.noise.ShiftedNoiseNode;
import com.ishland.c2me.opts.dfc.common.ast.spline.SplineAstNode;
import com.ishland.c2me.opts.dfc.common.ast.unary.AbstractUnaryNode;
import net.minecraft.util.math.Spline;
import net.minecraft.world.gen.densityfunction.DensityFunctionTypes;

final class YDependencyAnalyzer {

    private YDependencyAnalyzer() {
    }

    static boolean isYIndependent(AstNode node) {
        return switch (node) {
            case ConstantNode ignored -> true;
            case DFTNoiseNode noise -> noise.getYScale() == 0.0;
            case DFTShiftANode ignored -> true;
            case DFTShiftBNode ignored -> true;
            case ShiftedNoiseNode shiftedNoise -> shiftedNoise.getYScale() == 0.0
                    && isYIndependent(shiftedNoise.getShiftX())
                    && isYIndependent(shiftedNoise.getShiftY())
                    && isYIndependent(shiftedNoise.getShiftZ());
            case CacheLikeNode cache -> CacheLikeKind.from(cache.getCacheLike()).isYIndependentCache();
            case SplineAstNode spline -> isSplineYIndependent(spline.getSpline());
            case AbstractBinaryNode binary -> isYIndependent(binary.getLeft()) && isYIndependent(binary.getRight());
            case AbstractUnaryNode unary -> isYIndependent(unary.getOperand());
            case RangeChoiceNode range -> isYIndependent(range.getInput())
                    && isYIndependent(range.getWhenInRange())
                    && isYIndependent(range.getWhenOutOfRange());
            default -> false;
        };
    }

    private static boolean isSplineYIndependent(Spline<DensityFunctionTypes.Spline.SplinePos, DensityFunctionTypes.Spline.DensityFunctionWrapper> spline) {
        if (spline instanceof Spline.FixedFloatFunction<DensityFunctionTypes.Spline.SplinePos, DensityFunctionTypes.Spline.DensityFunctionWrapper>) {
            return true;
        }
        if (spline instanceof Spline.Implementation<DensityFunctionTypes.Spline.SplinePos, DensityFunctionTypes.Spline.DensityFunctionWrapper> impl) {
            if (!isYIndependent(McToAst.toAst(impl.locationFunction().function().value()))) {
                return false;
            }
            for (Spline<DensityFunctionTypes.Spline.SplinePos, DensityFunctionTypes.Spline.DensityFunctionWrapper> value : impl.values()) {
                if (!isSplineYIndependent(value)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }
}
