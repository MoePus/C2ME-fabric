package com.ishland.c2me.opts.dfc.common.ast.opt;

import com.ishland.c2me.opts.dfc.common.ducks.IFastCacheLike;
import net.minecraft.world.gen.chunk.ChunkNoiseSampler;
import net.minecraft.world.gen.densityfunction.DensityFunctionTypes;

enum CacheLikeKind {
    INTERPOLATED,
    FLAT_CACHE,
    CACHE2D,
    CACHE_ONCE,
    CACHE_ALL_IN_CELL,
    UNKNOWN;

    static CacheLikeKind from(IFastCacheLike cacheLike) {
        if ((Object) cacheLike instanceof DensityFunctionTypes.Wrapping wrapping) {
            return fromWrappingType(wrapping.type());
        }
        if ((Object) cacheLike instanceof ChunkNoiseSampler.Cache2D) {
            return CACHE2D;
        }
        if ((Object) cacheLike instanceof ChunkNoiseSampler.CacheOnce) {
            return CACHE_ONCE;
        }
        if ((Object) cacheLike instanceof ChunkNoiseSampler.FlatCache) {
            return FLAT_CACHE;
        }
        if ((Object) cacheLike instanceof ChunkNoiseSampler.CellCache) {
            return CACHE_ALL_IN_CELL;
        }
        if ((Object) cacheLike instanceof ChunkNoiseSampler.DensityInterpolator) {
            return INTERPOLATED;
        }
        return UNKNOWN;
    }

    boolean isYIndependentCache() {
        return this == CACHE2D || this == FLAT_CACHE;
    }

    static boolean canAbsorb(CacheLikeKind outer, CacheLikeKind inner, boolean isDelegateYIndependent) {
        return switch (outer) {
            case INTERPOLATED -> switch (inner) {
                case INTERPOLATED, CACHE_ONCE, CACHE_ALL_IN_CELL -> true;
                case CACHE2D -> isDelegateYIndependent;
                default -> false;
            };
            case FLAT_CACHE -> switch (inner) {
                case FLAT_CACHE, CACHE2D, CACHE_ONCE, CACHE_ALL_IN_CELL -> true;
                default -> false;
            };
            case CACHE2D -> switch (inner) {
                case CACHE2D, CACHE_ONCE, CACHE_ALL_IN_CELL -> true;
                default -> false;
            };
            case CACHE_ONCE, CACHE_ALL_IN_CELL -> switch (inner) {
                case CACHE2D -> isDelegateYIndependent;
                case CACHE_ONCE, CACHE_ALL_IN_CELL -> true;
                default -> false;
            };
            case UNKNOWN -> false;
        };
    }

    DensityFunctionTypes.Wrapping.Type toWrappingType() {
        return switch (this) {
            case CACHE2D -> DensityFunctionTypes.Wrapping.Type.CACHE2D;
            case CACHE_ONCE -> DensityFunctionTypes.Wrapping.Type.CACHE_ONCE;
            default -> throw new IllegalArgumentException("Unsupported inserted cache kind: " + this);
        };
    }

    private static CacheLikeKind fromWrappingType(DensityFunctionTypes.Wrapping.Type type) {
        return switch (type) {
            case INTERPOLATED -> INTERPOLATED;
            case FLAT_CACHE -> FLAT_CACHE;
            case CACHE2D -> CACHE2D;
            case CACHE_ONCE -> CACHE_ONCE;
            case CACHE_ALL_IN_CELL -> CACHE_ALL_IN_CELL;
        };
    }
}
