package com.ishland.c2me.opts.dfc.common.ast;

import com.ishland.c2me.opts.dfc.common.ast.opt.AlgebraicSimplification;
import com.ishland.c2me.opts.dfc.common.ast.opt.CacheLikeOptimizer;
import com.ishland.c2me.opts.dfc.common.ast.opt.ConstantFolding;
import com.ishland.c2me.opts.dfc.common.ast.opt.IdentityElimination;
import com.ishland.c2me.opts.dfc.common.ast.opt.RangeChoicePruning;
import com.ishland.c2me.opts.dfc.common.ast.opt.Reassociation;
import com.ishland.c2me.opts.dfc.common.ast.opt.StrengthReduction;

public final class AstOptimizer {

    private static final int MAX_ITERATIONS = 10;

    private AstOptimizer() {}

    public static AstNode optimize(AstNode node) {
        AstNode prev;
        int iterations = 0;
        do {
            prev = node;
            node = Reassociation.optimize(node);
            node = ConstantFolding.optimize(node);
            node = StrengthReduction.optimize(node);
            node = AlgebraicSimplification.optimize(node);
            node = IdentityElimination.optimize(node);
            node = RangeChoicePruning.optimize(node);
            iterations++;
        } while (prev != node && iterations < MAX_ITERATIONS);
        return CacheLikeOptimizer.optimize(node);
    }
}
