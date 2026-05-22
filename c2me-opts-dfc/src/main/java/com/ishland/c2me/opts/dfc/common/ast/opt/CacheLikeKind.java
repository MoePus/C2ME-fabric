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
        if (cacheLike instanceof CacheLikeTypeProvider provider) {
            return provider.c2me$getCacheLikeKind();
        }
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

    boolean isMemoLike() {
        return this == CACHE2D || this == CACHE_ONCE;
    }

    boolean isYIndependentCache() {
        return this == CACHE2D || this == FLAT_CACHE;
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

interface CacheLikeTypeProvider {

    CacheLikeKind c2me$getCacheLikeKind();

}
