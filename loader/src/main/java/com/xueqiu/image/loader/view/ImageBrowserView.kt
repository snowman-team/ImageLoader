package com.xueqiu.image.loader.view

import android.content.Context
import android.graphics.drawable.Animatable
import android.net.Uri
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.drawee.controller.BaseControllerListener
import com.facebook.drawee.drawable.ScalingUtils
import com.facebook.drawee.generic.GenericDraweeHierarchyBuilder
import com.facebook.imagepipeline.common.ResizeOptions
import com.facebook.imagepipeline.image.ImageInfo
import com.facebook.imagepipeline.request.ImageRequestBuilder
import com.xueqiu.image.loader.R
import com.xueqiu.image.loader.zoom.zoomable.DoubleTapGestureListener
import com.xueqiu.image.loader.zoom.zoomable.ZoomableController
import com.xueqiu.image.loader.zoom.zoomable.ZoomableDraweeView

class ImageBrowserView : FrameLayout {

    var listener: ImageBrowserListener? = null
    private var mUri: Uri? = null
    private var mHasMeasured = false

    private var mView: ZoomableDraweeView

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle) {

        context.theme.obtainStyledAttributes(
                attrs,
                R.styleable.NetImageView,
                defStyle, 0
        ).apply {
            try {
                // place holder
            } finally {
                recycle()
            }
        }

        mView = ZoomableDraweeView(context, attrs, defStyle)
        mView.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        mView.setIsLongpressEnabled(false)
        mView.setSwipeDownListener(object : ZoomableController.OnSwipeDownListener {
            override fun onSwipeDown(translateY: Float) {
                listener?.onSwipeDown()
            }

            override fun onSwipeRelease(translateY: Float) {
                listener?.onSwipeRelease()
            }

        })
        mView.setTapListener(object : DoubleTapGestureListener(mView) {
            override fun onSingleTapConfirmed(event: MotionEvent): Boolean {
                return listener?.onSingleTap(event) ?: super.onSingleTapConfirmed(event)
            }
        })
        addView(mView)

        mView.hierarchy = GenericDraweeHierarchyBuilder.newInstance(resources)
                .setActualImageColorFilter(mView.colorFilter)
                .setFadeDuration(300)
                .setActualImageScaleType(ScalingUtils.ScaleType.FIT_CENTER)
                .build()
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

    fun reset() {
        mView.controller = Fresco.newDraweeControllerBuilder().setOldController(mView.controller).reset().build()
    }

    fun loadImage(url: String?) {
        if (url.isNullOrEmpty()) {
            return
        }
        val uri = Uri.parse(url)
        if (mUri == uri) return
        loadImage(uri, measuredWidth, measuredHeight)
    }

    fun loadImage(uri: Uri, width: Int = measuredWidth, height: Int = measuredHeight) {
        mUri = uri
        if (!mHasMeasured) {
            return
        }

        val requestBuilder = ImageRequestBuilder.newBuilderWithSource(uri)
                .setResizeOptions(ResizeOptions(width, height))
        val request = requestBuilder.build()

        val controllerBuilder = Fresco.newDraweeControllerBuilder()
                .setTapToRetryEnabled(false)
                .setOldController(mView.controller)
                .setControllerListener(object : BaseControllerListener<ImageInfo>() {

                    override fun onFinalImageSet(id: String?, imageInfo: ImageInfo?, animatable: Animatable?) {
                        listener?.onImageSet()
                    }

                    override fun onFailure(id: String?, throwable: Throwable?) {
                        listener?.onLoadFail(throwable)
                    }

                })
                .setImageRequest(request)
                .setAutoPlayAnimations(true)

        mView.controller = controllerBuilder.build()
    }

    interface ImageBrowserListener {
        fun onSingleTap(event: MotionEvent): Boolean
        fun onSwipeDown() {}
        fun onSwipeRelease() {}
        fun onImageSet()
        fun onLoadFail(throwable: Throwable?)
    }


}