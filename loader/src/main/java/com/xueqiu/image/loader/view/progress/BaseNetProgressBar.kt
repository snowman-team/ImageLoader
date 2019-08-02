package com.xueqiu.image.loader.view.progress

import android.graphics.*
import android.graphics.drawable.Drawable

abstract class BaseNetProgressBar : Drawable() {

    protected val mPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    protected val mPath = Path()
    protected val mRect = RectF()

    protected var mLevel = 0

    var hideWhenZero = false

    var color: Int = -0x7fff7f01
        set(value) {
            field = value
            invalidateSelf()
        }

    var backgroundColor: Int = -0x80000000
        set(value) {
            field = value
            invalidateSelf()
        }

    override fun onLevelChange(level: Int): Boolean {
        mLevel = level
        invalidateSelf()
        return true
    }

    override fun setAlpha(alpha: Int) {
        mPaint.alpha = alpha
    }

    override fun setColorFilter(cf: ColorFilter?) {
        mPaint.colorFilter = cf
    }

    override fun getOpacity(): Int {
        return when (color.ushr(24)) {
            255 -> PixelFormat.OPAQUE
            0 -> PixelFormat.TRANSPARENT
            else -> PixelFormat.TRANSLUCENT
        }
    }

    final override fun draw(canvas: Canvas) = drawProgressBar(canvas)

    abstract fun drawProgressBar(canvas: Canvas)

}