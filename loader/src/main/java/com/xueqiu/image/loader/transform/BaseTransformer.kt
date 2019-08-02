package com.xueqiu.image.loader.transform

import android.graphics.Bitmap
import com.facebook.cache.common.CacheKey
import com.facebook.cache.common.SimpleCacheKey
import com.facebook.imagepipeline.request.BasePostprocessor

abstract class BaseTransformer : BasePostprocessor() {

    protected var mCacheKey: CacheKey? = null

    abstract fun transform(bitmap: Bitmap?)

    abstract fun getCacheKey(): String

    override fun process(bitmap: Bitmap?) {
        transform(bitmap)
    }

    override fun getPostprocessorCacheKey(): CacheKey? {
        if (null == mCacheKey) {
            mCacheKey = SimpleCacheKey(getCacheKey())
        }
        return mCacheKey
    }
}