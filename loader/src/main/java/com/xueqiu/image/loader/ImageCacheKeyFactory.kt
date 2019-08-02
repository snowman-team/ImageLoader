package com.xueqiu.image.loader

import android.net.Uri
import com.facebook.cache.common.CacheKey
import com.facebook.cache.common.SimpleCacheKey
import com.facebook.imagepipeline.cache.BitmapMemoryCacheKey
import com.facebook.imagepipeline.cache.CacheKeyFactory
import com.facebook.imagepipeline.request.ImageRequest

object ImageCacheKeyFactory : CacheKeyFactory {

    override fun getBitmapCacheKey(request: ImageRequest, callerContext: Any?): CacheKey {
        return BitmapMemoryCacheKey(
            getCacheKeySeed(request.sourceUri),
            request.resizeOptions,
            request.rotationOptions,
            request.imageDecodeOptions, null, null,
            callerContext
        )
    }

    override fun getPostprocessedBitmapCacheKey(request: ImageRequest, callerContext: Any?): CacheKey {
        val postprocessor = request.postprocessor
        val postprocessorCacheKey: CacheKey?
        val postprocessorName: String?
        if (postprocessor != null) {
            postprocessorCacheKey = postprocessor.postprocessorCacheKey
            postprocessorName = postprocessor.javaClass.name
        } else {
            postprocessorCacheKey = null
            postprocessorName = null
        }
        return BitmapMemoryCacheKey(
            getCacheKeySeed(request.sourceUri),
            request.resizeOptions,
            request.rotationOptions,
            request.imageDecodeOptions,
            postprocessorCacheKey,
            postprocessorName,
            callerContext
        )
    }

    override fun getEncodedCacheKey(request: ImageRequest, callerContext: Any?): CacheKey {
        return getEncodedCacheKey(request, request.sourceUri, callerContext)
    }

    override fun getEncodedCacheKey(
        request: ImageRequest,
        sourceUri: Uri,
        callerContext: Any?
    ): CacheKey {
        return SimpleCacheKey(getCacheKeySeed(sourceUri))
    }

    private fun getCacheKeySeed(sourceUri: Uri): String {
        return sourceUri.hashCode().toString()
    }

}