package com.xueqiu.image.loader.svg

import com.caverock.androidsvg.SVG
import com.facebook.imagepipeline.image.CloseableImage

class CloseableSvgImage(val svg: SVG) : CloseableImage() {

    private var mClosed = false

    override fun getSizeInBytes(): Int {
        return 0
    }

    override fun close() {
        mClosed = true
    }

    override fun isClosed(): Boolean {
        return mClosed
    }

    override fun getWidth(): Int {
        return 0
    }

    override fun getHeight(): Int {
        return 0
    }
}