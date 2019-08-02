package com.xueqiu.image.loader

import android.graphics.Bitmap
import com.facebook.common.util.ByteConstants.MB
import java.io.File

class ImageLoaderOptions {

    var cacheDir: File? = null
        private set

    var cacheSize = 50L * MB
        private set

    var smallCacheDir: File? = null
        private set

    var smallCacheSize = 10L * MB
        private set

    var decodeFormat = Bitmap.Config.RGB_565
        private set

    var isDownSampling = true
        private set

    var isNetworkResize = true
        private set

    var loaderInterceptor: BaseLoaderInterceptor? = null
        private set

    var headerProcessor: BaseHeaderProcessor? = null
        private set

    fun withCacheSize(size: Long): ImageLoaderOptions {
        cacheSize = size
        return this
    }

    fun withCacheDir(dir: File): ImageLoaderOptions {
        cacheDir = dir
        return this
    }

    fun withSmallCacheSize(size: Long): ImageLoaderOptions {
        smallCacheSize = size
        return this
    }

    fun withSmallCacheDir(dir: File): ImageLoaderOptions {
        smallCacheDir = dir
        return this
    }

    fun withDecodeFormat(format: Bitmap.Config): ImageLoaderOptions {
        decodeFormat = format
        return this
    }

    fun enableDownSampling(downSampling: Boolean): ImageLoaderOptions {
        isDownSampling = downSampling
        return this
    }

    fun enableNetworkResize(networkResize: Boolean): ImageLoaderOptions {
        isNetworkResize = networkResize
        return this
    }

    fun <T : BaseLoaderInterceptor> withLoaderInterceptor(loaderInterceptor: T): ImageLoaderOptions {
        this.loaderInterceptor = loaderInterceptor
        return this
    }

    fun withHeader(processor: BaseHeaderProcessor): ImageLoaderOptions {
        this.headerProcessor = processor
        return this
    }
}