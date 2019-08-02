package com.xueqiu.image.loader

import android.net.Uri
import com.xueqiu.image.loader.transform.BaseTransformer


class ImageBuilder {

    companion object {

        const val TYPE_URL = "url"
        const val TYPE_URI = "uri"
        const val TYPE_FILE = "file"
        const val TYPE_ASSETS = "assets"
        const val TYPE_DRAWABLE_RES = "drawable_res"
    }

    var imageType: String = ""
        private set

    var imageUrl: String? = null
        private set

    var imageUri: Uri? = null
        private set

    var imageAssetsPath: String? = null
        private set

    var imageFilePath: String? = null
        private set

    var drawableRes: Int = 0
        private set

    var isSmall: Boolean = false
        private set

    var transformer: BaseTransformer? = null
        private set

    var customSize: Pair<Int, Int>? = null
        private set

    fun withNetImage(url: String): ImageBuilder {
        imageType = TYPE_URL
        imageUrl = url
        return this
    }

    fun withImageUri(uri: Uri): ImageBuilder {
        imageType = TYPE_URI
        imageUri = uri
        return this
    }

    fun withLocaleImage(filePath: String): ImageBuilder {
        imageType = TYPE_FILE
        imageFilePath = filePath
        return this
    }

    fun withAssetsImage(filePath: String): ImageBuilder {
        imageType = TYPE_ASSETS
        imageAssetsPath = filePath
        return this
    }

    fun withDrawableImage(drawableRes: Int): ImageBuilder {
        imageType = TYPE_DRAWABLE_RES
        this.drawableRes = drawableRes
        return this
    }


    fun withTransformer(transformer: BaseTransformer): ImageBuilder {
        this.transformer = transformer
        return this
    }

    fun isSmall(boolean: Boolean): ImageBuilder {
        isSmall = boolean
        return this
    }

    fun withCustomSize(width: Int, height: Int): ImageBuilder {
        customSize = Pair(width, height)
        return this
    }
}