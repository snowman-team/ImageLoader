Snowball Android Image Loader
============

Make fresco easier to use.

## Installation

```groovy
repositories {
    maven { url "https://xueqiumobile.bintray.com/maven" }
}
dependencies {
    // add dependency, please replace x.y.z to the latest version
    implementation "com.xueqiu.image:loader:x.y.z"
}
```

## Usage

ImageLoader use [Fresco](https://frescolib.org/) & [RxJava3](https://github.com/ReactiveX/RxJava), please read their documentation before using it.

### ImageLoader

Initialize image loader with options in your application.
```kotlin
val imageOptions = ImageLoaderOptions()
        .withCacheDir(File(cacheDir, "big"))
        .withCacheSize(1024 * 1024 * 100)
        .withSmallCacheDir(File(cacheDir, "small"))
        .withSmallCacheSize(1024 * 1024 * 5)
        .withDecodeFormat(Bitmap.Config.RGB_565)
        .withHeader(object : BaseHeaderProcessor() {
            override fun setHeaders(url: URL): Map<String, String>? {
                // replace to your header
                val header = HashMap<String, String>()
                header["test-key"] = "test-value"
                return header
            }
        })
        .withLoaderInterceptor(object : BaseLoaderInterceptor() {
            override fun onIntercept(builder: ImageBuilder): ImageBuilder {
                // do something if you need
                Log.i("load image type ->", builder.imageType)
                return builder
            }
        })
ImageLoader.init(this, imageOptions)
```

Create the request with image builder.
```kotlin
val builder = ImageBuilder()
        .withNetImage(imageUrl)
        .withTransformer(BlurTransformer(2, 5))
        .isSmall(false)
```

Then use image loader to load image.
```kotlin

ImageLoader.loadImage(builder).subscribeWith(object : ImageObserver() {

    override fun onSuccess(bitmap: Bitmap) {
        // do something
    }

    override fun onOutOfMemory() {
        // do something
    }

    override fun onLoadError(e: Throwable) {
        // do something
    }
})
```
And do not forget to dispose the subscription when you no longer need the callback.

### NetImageView

```xml
<com.xueqiu.image.loader.view.NetImageView
    android:id="@+id/iv_image"
    android:layout_width="match_parent"
    android:layout_height="200dp" />
```

```kotlin
mIvImage.loadImage(imageUrl) // image or gif url
```

If you want to make image zoomable, use image browser view
```kotlin
<com.xueqiu.image.loader.view.ImageBrowserView
    android:id="@+id/iv_image"
    android:layout_width="match_parent"
    android:layout_height="200dp" />
```

Be careful, if you modify the attributes of view after image loading, you may need to call 'sketch()' method to reload image.

### Transformer

Use transformer to change the image.
```kotlin
withTransformer(BlurTransformer(2, 5))
withTransformer(CircleTransformer())
```

Or create your own transformer.
```kotlin

class CustomTransformer : BaseTransformer() {

    override fun transform(bitmap: Bitmap?) {
        // do something
    }

    override fun getCacheKey(): String {
        // make your own cache key
        return cacheKey
    }

}
```