package com.xueqiu.image.loader.view.progress

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.xueqiu.image.loader.view.progress.BaseNetProgressBar

class CircleProgressBarDrawable : BaseNetProgressBar() {

    private val maxLevel = 10000

    var stokeWidth = 6f

    override fun drawProgressBar(canvas: Canvas) {
        if (hideWhenZero && mLevel == 0) {
            return
        }
        drawBar(canvas, maxLevel, backgroundColor)
        drawBar(canvas, mLevel, color)
    }

    private fun drawBar(canvas: Canvas, level: Int, color: Int) {
        val bounds = bounds
        val rectF = RectF(
                (bounds.right * .4).toFloat(), (bounds.bottom * .4).toFloat(),
                (bounds.right * .6).toFloat(), (bounds.bottom * .6).toFloat()
        )
        mPaint.color = color
        mPaint.style = Paint.Style.STROKE
        mPaint.strokeWidth = stokeWidth
        if (level != 0) {
            canvas.drawArc(rectF, 0f, (level * 360 / maxLevel).toFloat(), false, mPaint)
        }
    }
}