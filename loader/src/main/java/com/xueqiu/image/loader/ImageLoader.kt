package com.xueqiu.image.loader

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.net.toUri
import com.facebook.binaryresource.FileBinaryResource
import com.facebook.cache.disk.DiskCacheConfig
import com.facebook.common.executors.UiThreadImmediateExecutorService
import com.facebook.common.memory.MemoryTrimType
import com.facebook.common.memory.NoOpMemoryTrimmableRegistry
import com.facebook.common.references.CloseableReference
import com.facebook.common.util.UriUtil
import com.facebook.datasource.DataSources
import com.facebook.drawee.backends.pipeline.DraweeConfig
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.imagepipeline.backends.okhttp3.OkHttpImagePipelineConfigFactory
import com.facebook.imagepipeline.common.ResizeOptions
import com.facebook.imagepipeline.core.ImagePipeline
import com.facebook.imagepipeline.core.ImagePipelineFactory
import com.facebook.imagepipeline.decoder.ImageDecoderConfig
import com.facebook.imagepipeline.image.CloseableBitmap
import com.facebook.imagepipeline.request.ImageRequest
import com.facebook.imagepipeline.request.ImageRequestBuilder
import com.xueqiu.image.loader.svg.*
import io.reactivex.Emitter
import io.reactivex.Observable
import okhttp3.OkHttpClient
import java.io.File

object ImageLoader {

    var options = ImageLoaderOptions()

    fun init(context: Context, options: ImageLoaderOptions) {
        this.options = options
        initFresco(context)
    }

    fun loadImage(imageBuilder: ImageBuilder): Observable<Bitmap> {
        return loadImageWithFresco(imageBuilder)
    }

    fun loadImageSync(imageBuilder: ImageBuilder): Bitmap? {
        var builder = imageBuilder
        options.loaderInterceptor?.let {
            builder = it.onIntercept(builder)
        }
        val uri = getUriFromBuilder(builder) ?: return null
        val imagePipeline = Fresco.getImagePipeline() ?: return null
        val imageRequestBuilder = ImageRequestBuilder.newBuilderWithSource(uri)
                .setCacheChoice(if (builder.isSmall) ImageRequest.CacheChoice.SMALL else ImageRequest.CacheChoice.DEFAULT)
        builder.transformer?.let {
            imageRequestBuilder.postprocessor = it
        }
        builder.customSize?.let {
            imageRequestBuilder.setResizeOptions(ResizeOptions(it.first, it.second))
        }
        val imageDataSource = if (imagePipeline.isInBitmapMemoryCache(uri)) {
            imagePipeline.fetchImageFromBitmapCache(imageRequestBuilder.build(), null)
        } else {
            imagePipeline.fetchDecodedImage(imageRequestBuilder.build(), null)
        }
        val imageReference = DataSources.waitForFinalResult(imageDataSource) ?: return null
        val image = imageReference.get() as CloseableBitmap
        if (null != image.underlyingBitmap) {
            return Bitmap.createBitmap(image.underlyingBitmap)
        }
        CloseableReference.closeSafely(imageReference)
        imageDataSource.close()
        return null
    }

    fun clearCache() {
        Fresco.getImagePipeline().clearCaches()
    }

    fun clearMemoryCache() {
        Fresco.getImagePipeline().clearMemoryCaches()
    }

    fun clearDiskCache() {
        Fresco.getImagePipeline().clearDiskCaches()
    }

    fun getDiskCachedImage(imageBuilder: ImageBuilder): File? {
        val uri = getUriFromBuilder(imageBuilder) ?: return null
        if (!Fresco.getImagePipeline().isInDiskCacheSync(uri)) return null
        val imageRequest = ImageRequest.fromUri(uri) ?: return null
        val cacheKey = ImageCacheKeyFactory.getEncodedCacheKey(imageRequest, null)
        val resource = (if (imageBuilder.isSmall) {
            ImagePipelineFactory.getInstance().smallImageFileCache.getResource(cacheKey)
        } else {
            ImagePipelineFactory.getInstance().mainFileCache.getResource(cacheKey)
        }) ?: return null
        return (resource as FileBinaryResource).file
    }

    fun preloadToMemoryCache(imageBuilder: ImageBuilder) {
        val uri = getUriFromBuilder(imageBuilder)
        val imageRequest = ImageRequest.fromUri(uri)
        Fresco.getImagePipeline().prefetchToBitmapCache(imageRequest, null)
    }

    fun preloadToDiskCache(imageBuilder: ImageBuilder) {
        val uri = getUriFromBuilder(imageBuilder)
        val imageRequest = ImageRequest.fromUri(uri)
        Fresco.getImagePipeline().prefetchToDiskCache(imageRequest, null)
    }

    fun pause() {
        Fresco.getImagePipeline().pause()
    }

    fun resume() {
        Fresco.getImagePipeline().resume()
    }

    private fun getUriFromBuilder(imageBuilder: ImageBuilder): Uri? {
        return when (imageBuilder.imageType) {
            ImageBuilder.TYPE_URL -> {
                imageBuilder.imageUrl?.toUri()
            }
            ImageBuilder.TYPE_URI -> {
                imageBuilder.imageUri
            }
            ImageBuilder.TYPE_FILE -> {
                Uri.Builder()
                        .scheme(UriUtil.LOCAL_FILE_SCHEME)
                        .path(imageBuilder.imageFilePath)
                        .build()
            }
            ImageBuilder.TYPE_ASSETS -> {
                Uri.Builder()
                        .scheme(UriUtil.LOCAL_ASSET_SCHEME)
                        .path(imageBuilder.imageAssetsPath)
                        .build()
            }
            ImageBuilder.TYPE_DRAWABLE_RES -> {
                Uri.Builder()
                        .scheme(UriUtil.LOCAL_RESOURCE_SCHEME)
                        .path(imageBuilder.drawableRes.toString())
                        .build()
            }
            else -> null
        }
    }

    private fun initFresco(context: Context) {

        // https://www.fresco-cn.org/javadoc/reference/com/facebook/common/memory/MemoryTrimType.html
        val memoryTrimmableRegistry = NoOpMemoryTrimmableRegistry.getInstance()
        memoryTrimmableRegistry.registerMemoryTrimmable { trimType ->
            val suggestedTrimRatio = trimType.suggestedTrimRatio
            if (MemoryTrimType.OnCloseToDalvikHeapLimit.suggestedTrimRatio == suggestedTrimRatio
                    || MemoryTrimType.OnSystemLowMemoryWhileAppInBackground.suggestedTrimRatio == suggestedTrimRatio
                    || MemoryTrimType.OnSystemLowMemoryWhileAppInForeground.suggestedTrimRatio == suggestedTrimRatio
            ) {
                Fresco.getImagePipeline().clearMemoryCaches()
            }
        }

        val okHttpClientBuilder = OkHttpClient.Builder()
        options.headerProcessor?.let {
            okHttpClientBuilder.addInterceptor(it)
        }

        val cacheFile = if (null == options.cacheDir) {
            File(context.cacheDir, "image_cache")
        } else {
            options.cacheDir
        }
        val diskCacheConfig = DiskCacheConfig.newBuilder(context)
                .setBaseDirectoryName(cacheFile?.name)
                .setBaseDirectoryPath(cacheFile)
                .setMaxCacheSize(options.cacheSize)
                .build()

        val smallCacheFile = if (null == options.smallCacheDir) {
            File(context.cacheDir, "image_small_cache")
        } else {
            options.smallCacheDir
        }
        val smallImageDiskCacheConfig = DiskCacheConfig.newBuilder(context)
                .setBaseDirectoryName(smallCacheFile?.name)
                .setBaseDirectoryPath(smallCacheFile)
                .setMaxCacheSize(options.smallCacheSize)
                .build()

        val imageDecoderConfig = ImageDecoderConfig.newBuilder()
                .addDecodingCapability(SvgConstant.SVG_FORMAT,
                        SvgFormatChecker(),
                        SvgDecoder())
                .build()

        val pipelineConfig = OkHttpImagePipelineConfigFactory.newBuilder(context, okHttpClientBuilder.build())
                .setMainDiskCacheConfig(diskCacheConfig)
                .setSmallImageDiskCacheConfig(smallImageDiskCacheConfig)
                .setResizeAndRotateEnabledForNetwork(options.isNetworkResize)
                .setBitmapsConfig(options.decodeFormat)
                .setImageDecoderConfig(imageDecoderConfig)
                .setMemoryTrimmableRegistry(memoryTrimmableRegistry)
                .setDownsampleEnabled(options.isDownSampling)
                .setCacheKeyFactory(ImageCacheKeyFactory)
                .build()

        val draweeConfig = DraweeConfig.newBuilder()
                .addCustomDrawableFactory(SvgDrawableFactory())
                .build()

        Fresco.initialize(context, pipelineConfig, draweeConfig)
    }

    private fun loadImageWithFresco(imageBuilder: ImageBuilder): Observable<Bitmap> {
        var builder = imageBuilder
        options.loaderInterceptor?.let {
            builder = it.onIntercept(builder)
        }
        val uri = getUriFromBuilder(builder)
                ?: return Observable.error(IllegalArgumentException("Get null image resources"))
        return Observable.create { emitter ->
            val imagePipeline = Fresco.getImagePipeline()
            if (imagePipeline.isInBitmapMemoryCache(uri)) {
                getImageBitmapFromMemory(uri, imagePipeline, builder, emitter)
            } else {
                getImageBitmapFromNet(uri, imagePipeline, builder, emitter)
            }
        }
    }

    private fun getImageBitmapFromNet(uri: Uri, imagePipeline: ImagePipeline, builder: ImageBuilder, emitter: Emitter<Bitmap>) {

        val imageRequestBuilder = ImageRequestBuilder.newBuilderWithSource(uri)
                .setCacheChoice(if (builder.isSmall) ImageRequest.CacheChoice.SMALL else ImageRequest.CacheChoice.DEFAULT)
        builder.transformer?.let {
            imageRequestBuilder.postprocessor = it
        }
        builder.customSize?.let {
            imageRequestBuilder.setResizeOptions(ResizeOptions(it.first, it.second))
        }
        val imageRequest = imageRequestBuilder.build()
        val imageDataSource = imagePipeline.fetchDecodedImage(imageRequest, null)
        imageDataSource.subscribe(ImageDataSubscriber(object : ImageDataSubscriber.Callback {

            override fun onSuccess(bitmap: Bitmap) {
                emitter.onNext(bitmap)
            }

            override fun onError(throwable: Throwable?) {
                emitter.onError(throwable ?: UnknownError())
            }

            override fun onFinish() {
                imageDataSource.close()
                emitter.onComplete()
            }

        }), UiThreadImmediateExecutorService.getInstance())

    }

    private fun getImageBitmapFromMemory(
            uri: Uri,
            imagePipeline: ImagePipeline,
            builder: ImageBuilder,
            emitter: Emitter<Bitmap>
    ) {
        val imageRequestBuilder = ImageRequestBuilder.newBuilderWithSource(uri)
                .setCacheChoice(if (builder.isSmall) ImageRequest.CacheChoice.SMALL else ImageRequest.CacheChoice.DEFAULT)
        builder.transformer?.let {
            imageRequestBuilder.postprocessor = it
        }
        builder.customSize?.let {
            imageRequestBuilder.setResizeOptions(ResizeOptions(it.first, it.second))
        }
        val imageRequest = imageRequestBuilder.build()
        if (imagePipeline.isInBitmapMemoryCache(uri)) {
            val imageDataSource = imagePipeline.fetchImageFromBitmapCache(imageRequest, null)
            imageDataSource.subscribe(ImageDataSubscriber(object : ImageDataSubscriber.Callback {

                override fun onSuccess(bitmap: Bitmap) {
                    emitter.onNext(bitmap)
                }

                override fun onError(throwable: Throwable?) {
                    emitter.onError(throwable ?: UnknownError())
                }

                override fun onFinish() {
                    imageDataSource.close()
                    emitter.onComplete()
                }

            }), UiThreadImmediateExecutorService.getInstance())
        }
    }

}