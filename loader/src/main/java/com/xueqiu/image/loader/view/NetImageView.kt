package com.xueqiu.image.loader.view

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.IntDef
import com.facebook.common.util.UriUtil
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.drawee.controller.BaseControllerListener
import com.facebook.drawee.drawable.ScalingUtils
import com.facebook.drawee.generic.GenericDraweeHierarchyBuilder
import com.facebook.drawee.generic.RoundingParams
import com.facebook.drawee.view.SimpleDraweeView
import com.facebook.imagepipeline.common.ResizeOptions
import com.facebook.imagepipeline.image.ImageInfo
import com.facebook.imagepipeline.request.ImageRequest
import com.facebook.imagepipeline.request.ImageRequestBuilder
import com.xueqiu.image.loader.R
import com.xueqiu.image.loader.transform.BaseTransformer
import com.xueqiu.image.loader.view.progress.BaseNetProgressBar

class NetImageView : FrameLayout {

    companion object {
        const val SCALE_TYPE_CENTER_CROP = 0
        const val SCALE_TYPE_CENTER_INSIDE = 1
        const val SCALE_TYPE_FIT_XY = 2
        const val SCALE_TYPE_FIT_CENTER = 3
        const val SCALE_TYPE_FIT_START = 4
        const val SCALE_TYPE_FIT_END = 5
    }

    @ScaleType
    var imageScaleType: Int = SCALE_TYPE_CENTER_CROP

    @ScaleType
    var placeHolderScaleType: Int = SCALE_TYPE_CENTER_CROP

    @ScaleType
    var failImageScaleType: Int = SCALE_TYPE_CENTER_CROP

    var animDuration: Int = 300 // default 300ms

    var placeHolderImage: Drawable? = null

    var failImage: Drawable? = null

    var isSmall: Boolean = false

    var isCircle: Boolean = false

    var radius: Float = 0f

    var autoPlay: Boolean = false

    @ColorInt
    var borderColor: Int = Color.WHITE

    var borderSize: Float = 0f

    var transformer: BaseTransformer? = null

    var progressBar: BaseNetProgressBar? = null
        set(value) {
            field = value
            mView.hierarchy.setProgressBarImage(field)
        }

    var listener: OnLoadListener? = null

    private var mView: SimpleDraweeView
    private var mUri: Uri? = null
    private var mHasMeasured = false
    private var mAnim: Animatable? = null

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle) {

        context.theme.obtainStyledAttributes(
                attrs,
                R.styleable.NetImageView,
                defStyle, 0
        ).apply {
            try {
                isSmall = getBoolean(R.styleable.NetImageView_small, false)
                animDuration = getInteger(R.styleable.NetImageView_anim_duration, 300)
                placeHolderImage = getDrawable(R.styleable.NetImageView_place_holder_image)
                failImage = getDrawable(R.styleable.NetImageView_fail_image)
                isCircle = getBoolean(R.styleable.NetImageView_circle, false)
                radius = getDimension(R.styleable.NetImageView_radius, 0f)
                borderColor = getColor(R.styleable.NetImageView_border_color, Color.WHITE)
                borderSize = getDimension(R.styleable.NetImageView_border_size, 0f)
                imageScaleType = getInt(R.styleable.NetImageView_image_scale_type, SCALE_TYPE_CENTER_CROP)
                placeHolderScaleType = getInt(R.styleable.NetImageView_place_holder_scale_type, SCALE_TYPE_CENTER_CROP)
                failImageScaleType = getInt(R.styleable.NetImageView_fail_image_scale_type, SCALE_TYPE_CENTER_CROP)
            } finally {
                recycle()
            }
        }

        mView = SimpleDraweeView(context, attrs, defStyle)
        mView.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        addView(mView)
        sketch()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val height = measuredHeight
        val width = measuredWidth
        if (height > 0 && width > 0) {
            mHasMeasured = true
            mUri?.let {
                loadImage(uri = it)
            }
        }
    }

    private fun getFrescoScaleType(@ScaleType scaleType: Int): ScalingUtils.ScaleType {
        return when (scaleType) {
            SCALE_TYPE_CENTER_CROP -> ScalingUtils.ScaleType.CENTER_CROP
            SCALE_TYPE_CENTER_INSIDE -> ScalingUtils.ScaleType.CENTER_INSIDE
            SCALE_TYPE_FIT_CENTER -> ScalingUtils.ScaleType.FIT_CENTER
            SCALE_TYPE_FIT_XY -> ScalingUtils.ScaleType.FIT_XY
            SCALE_TYPE_FIT_START -> ScalingUtils.ScaleType.FIT_START
            SCALE_TYPE_FIT_END -> ScalingUtils.ScaleType.FIT_END
            else -> ScalingUtils.ScaleType.CENTER_CROP
        }
    }

    fun reset() {
        mView.controller = Fresco.newDraweeControllerBuilder().setOldController(mView.controller).reset().build()
    }

    @Suppress("DEPRECATION")
    fun clear() {
        mUri = null
        mView.setImageBitmap(null)
    }

    fun getCurrentUri() = mUri

    fun playAnimImage() {
        mAnim?.start()
    }

    fun stopAnimImage() {
        mAnim?.stop()
    }

    fun isAnimImagePlaying() = mAnim?.isRunning ?: false

    fun sketch() {

        val roundingParams = RoundingParams()
        roundingParams.setCornersRadius(radius)
        roundingParams.setBorder(borderColor, borderSize)
        roundingParams.roundAsCircle = isCircle

        mView.hierarchy = GenericDraweeHierarchyBuilder.newInstance(resources)
                .setActualImageColorFilter(mView.colorFilter)
                .setFadeDuration(animDuration)
                .setFailureImage(failImage)
                .setPlaceholderImage(placeHolderImage)
                .setRoundingParams(roundingParams)
                .setProgressBarImage(progressBar)
                .setActualImageScaleType(getFrescoScaleType(imageScaleType))
                .setFailureImageScaleType(getFrescoScaleType(failImageScaleType))
                .setPlaceholderImageScaleType(getFrescoScaleType(placeHolderScaleType))
                .build()
    }

    fun loadImageFile(
            imageFilePath: String?,
            width: Int = measuredWidth,
            height: Int = measuredHeight) {
        if (imageFilePath.isNullOrEmpty()) return
        val uri = Uri.Builder()
                .scheme(UriUtil.LOCAL_FILE_SCHEME)
                .path(imageFilePath)
                .build()
        loadImage(uri, width, height)
    }

    fun loadDrawable(
            @DrawableRes resId: Int,
            width: Int = measuredWidth,
            height: Int = measuredHeight) {
        if (0 == resId) return
        val uri = Uri.Builder()
                .scheme(UriUtil.LOCAL_RESOURCE_SCHEME)
                .path(resId.toString())
                .build()
        if (mUri == uri) return
        loadImage(uri, width, height)
    }

    fun loadAssetsImage(
            imageFilePath: String?,
            width: Int = measuredWidth,
            height: Int = measuredHeight) {
        if (imageFilePath.isNullOrEmpty()) return
        val uri = Uri.Builder()
                .scheme(UriUtil.LOCAL_ASSET_SCHEME)
                .path(imageFilePath)
                .build()
        if (mUri == uri) return
        loadImage(uri, width, height)
    }

    fun loadImage(url: String?) {
        if (url.isNullOrEmpty()) {
            return
        }
        val uri = Uri.parse(url)
        if (mUri == uri) return
        loadImage(uri, measuredWidth, measuredHeight)
    }

    fun loadImage(
            url: String?,
            width: Int = measuredWidth,
            height: Int = measuredHeight) {

        if (url.isNullOrEmpty()) {
            return
        }
        val uri = Uri.parse(url)
        if (mUri == uri) return
        loadImage(uri, width, height)
    }

    fun loadImage(
            uri: Uri,
            width: Int = measuredWidth,
            height: Int = measuredHeight) {
        mUri = uri
        if (!mHasMeasured) {
            return
        }

        val requestBuilder = ImageRequestBuilder.newBuilderWithSource(uri)
                .setResizeOptions(ResizeOptions(width, height))
        transformer?.let {
            requestBuilder.postprocessor = it
        }
        if (isSmall) {
            requestBuilder.cacheChoice = ImageRequest.CacheChoice.SMALL
        }
        val request = requestBuilder.build()

        val controllerBuilder = Fresco.newDraweeControllerBuilder()
                .setTapToRetryEnabled(false)
                .setOldController(mView.controller)
                .setControllerListener(object : BaseControllerListener<ImageInfo>() {

                    override fun onFinalImageSet(id: String?, imageInfo: ImageInfo?, animatable: Animatable?) {
                        mAnim = animatable
                        listener?.onImageSet(imageInfo?.width, imageInfo?.height, animatable != null)
                    }

                    override fun onFailure(id: String?, throwable: Throwable?) {
                        listener?.onFail(throwable)
                    }

                })
                .setImageRequest(request)
                .setAutoPlayAnimations(autoPlay)

        mView.controller = controllerBuilder.build()
    }

    interface OnLoadListener {
        fun onImageSet(imageWidth: Int?, imageHeight: Int?, isAnimated: Boolean)
        fun onFail(throwable: Throwable?)
    }


    @Retention(AnnotationRetention.SOURCE)
    @IntDef(SCALE_TYPE_CENTER_CROP,
            SCALE_TYPE_CENTER_INSIDE,
            SCALE_TYPE_FIT_XY,
            SCALE_TYPE_FIT_CENTER,
            SCALE_TYPE_FIT_START,
            SCALE_TYPE_FIT_END)
    annotation class ScaleType
}