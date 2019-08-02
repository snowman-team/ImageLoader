package com.xueqiu.image.loader.transform

import android.graphics.Bitmap
import com.facebook.imagepipeline.nativecode.NativeBlurFilter

class BlurTransformer(private val mIterations: Int = 1, private val mBlurRadius: Int) : BaseTransformer() {

    override fun transform(bitmap: Bitmap?) {
        if (mIterations <= 0 || mBlurRadius <= 0) {
            return
        }
        NativeBlurFilter.iterativeBoxBlur(bitmap, mIterations, mBlurRadius)
    }

    override fun getCacheKey(): String = "blur_${mIterations}_$mBlurRadius"

}