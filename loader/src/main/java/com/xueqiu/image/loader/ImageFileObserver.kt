package com.xueqiu.image.loader

import io.reactivex.Observer
import io.reactivex.disposables.Disposable
import java.io.File

abstract class ImageFileObserver : Observer<File> {

    lateinit var disposable: Disposable

    final override fun onNext(file: File) {
        onSuccess(file)
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

    abstract fun onSuccess(file: File)
}