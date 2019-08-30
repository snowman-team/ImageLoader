package com.xueqiu.image.loader.svg

import android.graphics.drawable.Drawable
import com.facebook.imagepipeline.drawable.DrawableFactory
import com.facebook.imagepipeline.image.CloseableImage

class SvgDrawableFactory : DrawableFactory {

    override fun supportsImageType(image: CloseableImage): Boolean {
        return image is CloseableSvgImage
    }

    override fun createDrawable(image: CloseableImage): Drawable? {
        return SvgPictureDrawable((image as CloseableSvgImage).svg)
    }
}
