package com.ishland.c2me.opts.dfc.common.ast.opt;

import com.ishland.c2me.opts.dfc.common.ast.AstNode;
import com.ishland.c2me.opts.dfc.common.ast.binary.AbstractBinaryNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.CacheLikeNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.ConstantNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.RangeChoiceNode;
import com.ishland.c2me.opts.dfc.common.ast.noise.DFTShiftANode;
import com.ishland.c2me.opts.dfc.common.ast.noise.DFTShiftBNode;
import com.ishland.c2me.opts.dfc.common.ast.unary.AbstractUnaryNode;

final class YDependencyAnalyzer {

    private YDependencyAnalyzer() {
    }

    static boolean isYIndependent(AstNode node) {
        return switch (node) {
            case ConstantNode ignored -> true;
            case DFTShiftANode ignored -> true;
            case DFTShiftBNode ignored -> true;
            case CacheLikeNode cache -> CacheLikeKind.from(cache.getCacheLike()).isYIndependentCache();
            case AbstractBinaryNode binary -> isYIndependent(binary.getLeft()) && isYIndependent(binary.getRight());
            case AbstractUnaryNode unary -> isYIndependent(unary.getOperand());
            case RangeChoiceNode range -> isYIndependent(range.getInput())
                    && isYIndependent(range.getWhenInRange())
                    && isYIndependent(range.getWhenOutOfRange());
            default -> false;
        };
    }
}
