package com.xueqiu.image.loader

import android.graphics.Bitmap
import io.reactivex.Observer
import io.reactivex.disposables.Disposable

abstract class ImageObserver : Observer<Bitmap> {

    lateinit var disposable: Disposable

    final override fun onNext(bitmap: Bitmap) {
        onSuccess(bitmap)
    }

    final override fun onComplete() {
        onFinish()
    }

    final override fun onError(e: Throwable) {
        if (e is OutOfMemoryError) {
            onOutOfMemory()
        } else {
            onLoadError(e)
        }
    }

    override fun onSubscribe(d: Disposable) {
        disposable = d
    }

    fun neverDispose() {
        // do nothing
    }

    open fun onFinish() {}

    open fun onOutOfMemory() {
        ImageLoader.clearMemoryCache()
    }

    open fun onLoadError(e: Throwable) {}

    abstract fun onSuccess(bitmap: Bitmap)
}