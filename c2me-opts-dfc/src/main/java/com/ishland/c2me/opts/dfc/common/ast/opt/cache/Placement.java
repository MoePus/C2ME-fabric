package com.ishland.c2me.opts.dfc.common.ast.opt.cache;

import com.ishland.c2me.opts.dfc.common.ast.AstNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.CacheLikeNode;
import com.ishland.c2me.opts.dfc.common.ducks.IFastCacheLike;
import com.ishland.c2me.opts.dfc.common.vif.AstVanillaInterface;
import com.ishland.c2me.opts.dfc.common.ast.opt.cache.Use.*;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenCustomHashMap;
import net.minecraft.world.gen.densityfunction.DensityFunction;
import net.minecraft.world.gen.densityfunction.DensityFunctionTypes;

import java.util.IdentityHashMap;

final class Placement {
    private final IdentityHashMap<AstNode, BoundaryUse> boundaryUses;
    private final RegionUses regionUses;
    private final IdentityHashMap<AstNode, AstNode> placedNodes = new IdentityHashMap<>();
    private final IdentityHashMap<AstNode, AstNode> placedNodesInsideCacheOnce = new IdentityHashMap<>();
    private final IdentityHashMap<AstNode, IdentityHashMap<AstNode, AstNode>> placedNodesByCovering2DRoot = new IdentityHashMap<>();
    private final Object2ReferenceOpenCustomHashMap<AstNode, AstNode> placedCacheTargets = new Object2ReferenceOpenCustomHashMap<>(AstNodeIdentityStrategy.STRICT);

    Placement(IdentityHashMap<AstNode, BoundaryUse> boundaryUses, RegionUses regionUses) {
        this.boundaryUses = boundaryUses;
        this.regionUses = regionUses;
    }

    AstNode place(AstNode node) {
        return place(node, null, null, 0, false);
    }

    private AstNode place(AstNode node, AstNode covering2DCacheRoot, AstNode parent, int depth, boolean insideCacheOnce) {
        if (covering2DCacheRoot != null) {
            IdentityHashMap<AstNode, AstNode> placedCoveredNodes = this.placedNodesByCovering2DRoot.computeIfAbsent(
                    covering2DCacheRoot,
                    unused -> new IdentityHashMap<>()
            );
            AstNode placed = placedCoveredNodes.get(node);
            if (placed != null) {
                return placed;
            }

            AstNode result = placeUncached(node, covering2DCacheRoot, parent, depth, insideCacheOnce);
            placedCoveredNodes.put(node, result);
            return result;
        }
        IdentityHashMap<AstNode, AstNode> placedNodeMap = insideCacheOnce ? this.placedNodesInsideCacheOnce : this.placedNodes;
        AstNode placed = placedNodeMap.get(node);
        if (placed != null) {
            return placed;
        }

        AstNode result = placeUncached(node, null, parent, depth, insideCacheOnce);
        placedNodeMap.put(node, result);
        return result;
    }

    private AstNode placeUncached(AstNode node, AstNode covering2DCacheRoot, AstNode parent, int depth, boolean insideCacheOnce) {
        CachePlacement placement = placementFor(node);
        if (placement == CachePlacement.CACHE2D) {
            return wrap(node, placeDelegate(node, node, depth, insideCacheOnce));
        } else if (placement == CachePlacement.CACHE_ONCE) {
            return wrap(node, placeDelegate(node, null, depth, true));
        } else if (node instanceof CacheLikeNode cacheLikeNode && cacheLikeNode.hasSideEffects()) {
            return placeSideEffectCache(cacheLikeNode, covering2DCacheRoot, depth, insideCacheOnce);
        } else if (node instanceof CacheLikeNode) {
            return node;
        } else {
            return placeChildren(node, covering2DCacheRoot, depth, insideCacheOnce);
        }
    }

    private AstNode placeDelegate(AstNode node, AstNode covering2DCacheRoot, int depth, boolean insideCacheOnce) {
        if (node instanceof CacheLikeNode cacheLikeNode) {
            if (cacheLikeNode.hasSideEffects()) {
                return placeSideEffectCache(cacheLikeNode, covering2DCacheRoot, depth, insideCacheOnce);
            }
            return cacheLikeNode.getCacheLike() == null
                    ? place(cacheLikeNode.getDelegate(), covering2DCacheRoot, node, depth + 1, insideCacheOnce)
                    : cacheLikeNode;
        }
        return placeChildren(node, covering2DCacheRoot, depth, insideCacheOnce);
    }

    private AstNode placeSideEffectCache(CacheLikeNode cacheLikeNode, AstNode covering2DCacheRoot, int depth, boolean insideCacheOnce) {
        AstNode delegate = place(cacheLikeNode.getDelegate(), covering2DCacheRoot, cacheLikeNode, depth + 1, insideCacheOnce);
        return delegate == cacheLikeNode.getDelegate() ? cacheLikeNode : new CacheLikeNode(cacheLikeNode.getCacheLike(), delegate);
    }

    private AstNode wrap(AstNode node, AstNode delegate) {
        AstNode placed = this.placedCacheTargets.get(node);
        if (placed != null) {
            return placed;
        }
        DensityFunctionTypes.Wrapping.Type type = wrapperType(node);
        AstNode result = new CacheLikeNode(newWrapper(node, type), delegate);
        this.placedCacheTargets.put(node, result);
        return result;
    }

    private CachePlacement placementFor(AstNode node) {
        if (DagCseOptimizer.containsStrictBoundary(node)) {
            return CachePlacement.NONE;
        }
        if (!node.YDependency()) {
            if (!this.regionUses.isSelectedCache2D(node)) {
                return CachePlacement.NONE;
            }
            return CachePlacement.CACHE2D;
        }
        return CachePlacement.NONE;
    }

    private AstNode placeChildren(AstNode node, AstNode covering2DCacheRoot, int depth, boolean insideCacheOnce) {
        AstNode[] children = node.getChildren();
        if (children.length == 0) {
            return node;
        }
        AstNode[] placed = new AstNode[children.length];
        boolean changed = false;
        for (int i = 0; i < children.length; i++) {
            placed[i] = place(children[i], covering2DCacheRoot, node, depth + 1, insideCacheOnce);
            changed |= placed[i] != children[i];
        }
        if (!changed) {
            return node;
        }
        return node.withChildren(placed);
    }

    private static IFastCacheLike newWrapper(AstNode node, DensityFunctionTypes.Wrapping.Type type) {
        DensityFunction delegate = new AstVanillaInterface(node, null);
        return switch (type) {
            case CACHE2D -> new RebuiltCache2D(delegate);
            case CACHE_ONCE -> new RebuiltCacheOnce(delegate);
            default -> throw new IllegalArgumentException("Unsupported rebuilt cache type: " + type);
        };
    }

    private static DensityFunctionTypes.Wrapping.Type wrapperType(AstNode node) {
        return node.YDependency()
                ? DensityFunctionTypes.Wrapping.Type.CACHE_ONCE
                : DensityFunctionTypes.Wrapping.Type.CACHE2D;
    }

    private enum CachePlacement {
        NONE,
        CACHE2D,
        CACHE_ONCE
    }
}
