package com.xueqiu.image.loader

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.PictureDrawable
import com.facebook.common.references.CloseableReference
import com.facebook.datasource.BaseDataSubscriber
import com.facebook.datasource.DataSource
import com.facebook.imagepipeline.image.CloseableBitmap
import com.facebook.imagepipeline.image.CloseableImage
import com.xueqiu.image.loader.svg.CloseableSvgImage


class ImageDataSubscriber(val callback: Callback) : BaseDataSubscriber<CloseableReference<CloseableImage>>() {

    override fun onNewResultImpl(dataSource: DataSource<CloseableReference<CloseableImage>>?) {
        try {
            val imageReference = dataSource?.result
            if (imageReference != null) {
                try {
                    when (val image = imageReference.get()) {
                        is CloseableBitmap -> fromBitmapImage(image)
                        is CloseableSvgImage -> fromSvgImage(image)
                        else -> callback.onError(IllegalStateException("Get unknown image reference"))
                    }
                } catch (e: OutOfMemoryError) {
                    callback.onError(e)
                } finally {
                    CloseableReference.closeSafely(imageReference)
                }
            } else {
                callback.onError(NullPointerException("Get null bitmap or bitmap is recycled"))
            }
        } catch (e: Throwable) {
            callback.onError(e)
        } finally {
            dataSource?.close()
            callback.onFinish()
        }
    }

    override fun onFailureImpl(dataSource: DataSource<CloseableReference<CloseableImage>>?) {
        dataSource?.failureCause?.let {
            callback.onError(it)
        }
        callback.onFinish()
    }

    override fun onProgressUpdate(dataSource: DataSource<CloseableReference<CloseableImage>>?) {}

    private fun fromBitmapImage(image: CloseableBitmap) {
        val underlyingBitmap = image.underlyingBitmap
        if (null != underlyingBitmap && !underlyingBitmap.isRecycled) {
            callback.onSuccess(Bitmap.createBitmap(image.underlyingBitmap))
            return
        } else {
            callback.onError(NullPointerException("Get null bitmap or bitmap is recycled"))
            return
        }
    }

    private fun fromSvgImage(image: CloseableSvgImage) {
        val drawable = PictureDrawable(image.svg.renderToPicture())
        val bitmap = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).drawPicture(drawable.picture)
        if (null != bitmap && !bitmap.isRecycled) {
            callback.onSuccess(Bitmap.createBitmap(bitmap))
            return
        } else {
            callback.onError(NullPointerException("Get null bitmap or bitmap is recycled"))
            return
        }
    }

    interface Callback {
        fun onSuccess(bitmap: Bitmap)
        fun onError(throwable: Throwable?)
        fun onFinish()
    }
}