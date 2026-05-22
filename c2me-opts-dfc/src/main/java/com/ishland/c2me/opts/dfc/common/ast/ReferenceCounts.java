package com.ishland.c2me.opts.dfc.common.ast;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

public final class ReferenceCounts {

    private static final ReferenceCounts EMPTY = new ReferenceCounts(new Object2IntOpenHashMap<>());

    private final Object2IntOpenHashMap<AstNode> counts;

    private ReferenceCounts(Object2IntOpenHashMap<AstNode> counts) {
        this.counts = counts;
    }

    public static ReferenceCounts empty() {
        return EMPTY;
    }

    public static ReferenceCounts collect(AstNode node) {
        Object2IntOpenHashMap<AstNode> counts = new Object2IntOpenHashMap<>();
        count(node, counts);
        return new ReferenceCounts(counts);
    }

    public int get(AstNode node) {
        return this.counts.getInt(node);
    }

    private static void count(AstNode node, Object2IntOpenHashMap<AstNode> counts) {
        for (AstNode child : node.getChildren()) {
            count(child, counts);
        }
        counts.addTo(node, 1);
    }
}
