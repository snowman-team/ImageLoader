package com.aquarids.image.app

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import com.xueqiu.image.loader.*
import java.io.File
import java.net.URL

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        val imageOptions = ImageLoaderOptions()
                .withCacheDir(File(cacheDir, "big"))
                .withCacheSize(1024 * 1024 * 100)
                .withSmallCacheDir(File(cacheDir, "small"))
                .withSmallCacheSize(1024 * 1024 * 5)
                .withDecodeFormat(Bitmap.Config.RGB_565)
                .withHeader(object : BaseHeaderProcessor() {
                    override fun setHeaders(url: URL): Map<String, String>? {
                        val header = HashMap<String, String>()
                        header["test-key"] = "test-value"
                        return header
                    }
                })
                .withLoaderInterceptor(object : BaseLoaderInterceptor() {
                    override fun onIntercept(builder: ImageBuilder): ImageBuilder {
                        Log.i("load image type ->", builder.imageType)
                        return builder
                    }
                })
        ImageLoader.init(this, imageOptions)
    }
}