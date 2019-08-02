package com.xueqiu.image.loader.transform

import android.graphics.Bitmap
import com.facebook.imagepipeline.nativecode.NativeRoundingFilter

class CircleTransformer : BaseTransformer() {

    companion object {
        const val CACHE_KEY = "ROUND_CIRCLE"
    }

    override fun transform(bitmap: Bitmap?) {
        bitmap?.let {
            NativeRoundingFilter.toCircle(it)
        }
    }

    override fun getCacheKey(): String = CACHE_KEY
}