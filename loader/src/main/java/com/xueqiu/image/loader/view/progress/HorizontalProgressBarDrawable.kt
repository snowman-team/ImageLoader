package com.xueqiu.image.loader.view.progress

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import kotlin.math.min

class HorizontalProgressBarDrawable : BaseNetProgressBar() {

    private var mPadding = 10

    var isVertical: Boolean = false
        set(value) {
            field = value
            invalidateSelf()
        }

    var stokeWidth: Int = 20
        set(value) {
            field = value
            invalidateSelf()
        }

    var radius: Int = 0
        set(value) {
            field = value
            invalidateSelf()
        }


    fun setPadding(padding: Int) {
        if (mPadding != padding) {
            mPadding = padding
            invalidateSelf()
        }
    }

    override fun getPadding(padding: Rect): Boolean {
        padding.set(mPadding, mPadding, mPadding, mPadding)
        return mPadding != 0
    }

    override fun drawProgressBar(canvas: Canvas) {
        if (hideWhenZero && mLevel == 0) {
            return
        }
        if (isVertical) {
            drawVerticalBar(canvas, 10000, backgroundColor)
            drawVerticalBar(canvas, mLevel, color)
        } else {
            drawHorizontalBar(canvas, 10000, backgroundColor)
            drawHorizontalBar(canvas, mLevel, color)
        }
    }

    private fun drawHorizontalBar(canvas: Canvas, level: Int, color: Int) {
        val bounds = bounds
        val length = (bounds.width() - 2 * mPadding) * level / 10000
        val xpos = bounds.left + mPadding
        val ypos = bounds.bottom - mPadding - stokeWidth
        mRect.set(xpos.toFloat(), ypos.toFloat(), (xpos + length).toFloat(), (ypos + stokeWidth).toFloat())
        drawBar(canvas, color)
    }

    private fun drawVerticalBar(canvas: Canvas, level: Int, color: Int) {
        val bounds = bounds
        val length = (bounds.height() - 2 * mPadding) * level / 10000
        val xpos = bounds.left + mPadding
        val ypos = bounds.top + mPadding
        mRect.set(xpos.toFloat(), ypos.toFloat(), (xpos + stokeWidth).toFloat(), (ypos + length).toFloat())
        drawBar(canvas, color)
    }

    private fun drawBar(canvas: Canvas, color: Int) {
        mPaint.color = color
        mPaint.style = Paint.Style.FILL_AND_STROKE
        mPath.reset()
        mPath.fillType = Path.FillType.EVEN_ODD
        mPath.addRoundRect(
                mRect,
                min(radius, stokeWidth / 2).toFloat(),
                min(radius, stokeWidth / 2).toFloat(),
                Path.Direction.CW)
        canvas.drawPath(mPath, mPaint)
    }

}