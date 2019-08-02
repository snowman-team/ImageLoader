package com.xueqiu.image.loader

import android.graphics.Bitmap
import com.facebook.common.references.CloseableReference
import com.facebook.datasource.BaseDataSubscriber
import com.facebook.datasource.DataSource
import com.facebook.imagepipeline.image.CloseableBitmap
import com.facebook.imagepipeline.image.CloseableImage

class ImageDataSubscriber(val callback: Callback) : BaseDataSubscriber<CloseableReference<CloseableImage>>() {

    override fun onNewResultImpl(dataSource: DataSource<CloseableReference<CloseableImage>>?) {
        try {
            val imageReference = dataSource?.result
            if (imageReference != null) {
                try {
                    val image = imageReference.get() as? CloseableBitmap?
                    if (null == image) {
                        callback.onError(NullPointerException("Get null bitmap or bitmap is recycled"))
                        return
                    }
                    val underlyingBitmap = image.underlyingBitmap
                    if (null != underlyingBitmap && !underlyingBitmap.isRecycled) {
                        callback.onSuccess(Bitmap.createBitmap(image.underlyingBitmap))
                        return
                    } else {
                        callback.onError(NullPointerException("Get null bitmap or bitmap is recycled"))
                        return
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

    interface Callback {
        fun onSuccess(bitmap: Bitmap)
        fun onError(throwable: Throwable?)
        fun onFinish()
    }
}