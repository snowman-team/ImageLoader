package com.xueqiu.image.loader.svg

import android.graphics.Rect
import android.graphics.drawable.PictureDrawable
import com.caverock.androidsvg.SVG

class SvgPictureDrawable(private val mSvg: SVG) : PictureDrawable(null) {

    override fun onBoundsChange(bounds: Rect) {
        super.onBoundsChange(bounds)
        picture = mSvg.renderToPicture(bounds.width(), bounds.height())
    }
}