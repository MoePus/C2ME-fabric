package com.ishland.c2me.opts.dfc.common.ast;

import com.ishland.c2me.opts.dfc.common.ast.spline.SplineAstNode;
import com.ishland.c2me.opts.dfc.common.gen.BytecodeGen;
import it.unimi.dsi.fastutil.objects.Object2IntOpenCustomHashMap;
import net.minecraft.util.math.Spline;
import net.minecraft.world.gen.densityfunction.DensityFunctionTypes;

public final class ReferenceCounts {

    private static final ReferenceCounts EMPTY = new ReferenceCounts(new Object2IntOpenCustomHashMap<>(BytecodeGen.RELAXED_STRATEGY));

    private final Object2IntOpenCustomHashMap<AstNode> counts;

    private ReferenceCounts(Object2IntOpenCustomHashMap<AstNode> counts) {
        this.counts = counts;
    }

    public static ReferenceCounts empty() {
        return EMPTY;
    }

    public static ReferenceCounts collect(AstNode node) {
        Object2IntOpenCustomHashMap<AstNode> counts = new Object2IntOpenCustomHashMap<>(BytecodeGen.RELAXED_STRATEGY);
        count(node, counts);
        return new ReferenceCounts(counts);
    }

    public int get(AstNode node) {
        return this.counts.getInt(node);
    }

    private static void count(AstNode node, Object2IntOpenCustomHashMap<AstNode> counts) {
        for (AstNode child : node.getChildren()) {
            count(child, counts);
        }
        if (node instanceof SplineAstNode splineAstNode) {
            countSpline(splineAstNode.getSpline(), counts);
        }
        counts.addTo(node, 1);
    }

    private static void countSpline(Spline<DensityFunctionTypes.Spline.SplinePos, DensityFunctionTypes.Spline.DensityFunctionWrapper> spline,
                                    Object2IntOpenCustomHashMap<AstNode> counts) {
        if (spline instanceof Spline.Implementation<DensityFunctionTypes.Spline.SplinePos, DensityFunctionTypes.Spline.DensityFunctionWrapper> impl) {
            count(McToAst.toAst(impl.locationFunction().function().value()), counts);
            for (Spline<DensityFunctionTypes.Spline.SplinePos, DensityFunctionTypes.Spline.DensityFunctionWrapper> value : impl.values()) {
                countSpline(value, counts);
            }
        }
    }
}
