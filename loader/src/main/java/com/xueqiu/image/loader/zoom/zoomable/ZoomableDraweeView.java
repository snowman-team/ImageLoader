package com.xueqiu.image.loader.zoom.zoomable;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.Animatable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;

import androidx.annotation.Nullable;
import androidx.core.view.ScrollingView;

import com.facebook.common.internal.Preconditions;
import com.facebook.drawee.controller.AbstractDraweeController;
import com.facebook.drawee.controller.BaseControllerListener;
import com.facebook.drawee.controller.ControllerListener;
import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.drawee.generic.GenericDraweeHierarchy;
import com.facebook.drawee.generic.GenericDraweeHierarchyBuilder;
import com.facebook.drawee.generic.GenericDraweeHierarchyInflater;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.DraweeView;

// Copied from https://github.com/facebook/fresco/tree/master/samples/zoomable/
public class ZoomableDraweeView extends DraweeView<GenericDraweeHierarchy>
        implements ScrollingView {

    private static final Class<?> TAG = ZoomableDraweeView.class;

    private static final float HUGE_IMAGE_SCALE_FACTOR_THRESHOLD = 1.1f;
    private static final boolean DEFAULT_ALLOW_TOUCH_INTERCEPTION_WHILE_ZOOMED = true;

    private boolean mUseSimpleTouchHandling = false;

    private final RectF mImageBounds = new RectF();
    private final RectF mViewBounds = new RectF();

    private DraweeController mHugeImageController;
    private ZoomableController mZoomableController;
    private GestureDetector mTapGestureDetector;
    private boolean mAllowTouchInterceptionWhileZoomed =
            DEFAULT_ALLOW_TOUCH_INTERCEPTION_WHILE_ZOOMED;

    private final ControllerListener mControllerListener = new BaseControllerListener<Object>() {
        @Override
        public void onFinalImageSet(
                String id,
                @Nullable Object imageInfo,
                @Nullable Animatable animatable) {
            ZoomableDraweeView.this.onFinalImageSet();
        }

        @Override
        public void onRelease(String id) {
            ZoomableDraweeView.this.onRelease();
        }
    };

    private final ZoomableController.Listener mZoomableListener = new ZoomableController.Listener() {
        @Override
        public void onTransformChanged(Matrix transform) {
            ZoomableDraweeView.this.onTransformChanged(transform);
        }
    };

    private final GestureListenerWrapper mTapListenerWrapper = new GestureListenerWrapper();

    public ZoomableDraweeView(Context context, GenericDraweeHierarchy hierarchy) {
        super(context);
        setHierarchy(hierarchy);
        init();
    }

    public ZoomableDraweeView(Context context) {
        super(context);
        inflateHierarchy(context, null);
        init();
    }

    public ZoomableDraweeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        inflateHierarchy(context, attrs);
        init();
    }

    public ZoomableDraweeView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        inflateHierarchy(context, attrs);
        init();
    }

    public void setSwipeDownListener(DefaultZoomableController.OnSwipeDownListener listener) {
        mZoomableController.setSwipeDownListener(listener);
    }

    protected void inflateHierarchy(Context context, @Nullable AttributeSet attrs) {
        Resources resources = context.getResources();
        GenericDraweeHierarchyBuilder builder = new GenericDraweeHierarchyBuilder(resources)
                .setActualImageScaleType(ScalingUtils.ScaleType.FIT_CENTER);
        GenericDraweeHierarchyInflater.updateBuilder(builder, context, attrs);
        setAspectRatio(builder.getDesiredAspectRatio());
        setHierarchy(builder.build());
    }

    private void init() {
        mZoomableController = createZoomableController();
        mZoomableController.setListener(mZoomableListener);

        mTapGestureDetector = new GestureDetector(getContext(), mTapListenerWrapper);
    }

    protected void getImageBounds(RectF outBounds) {
        getHierarchy().getActualImageBounds(outBounds);
    }

    protected void getLimitBounds(RectF outBounds) {
        outBounds.set(0, 0, getWidth(), getHeight());
    }

    public void setZoomableController(ZoomableController zoomableController) {
        Preconditions.checkNotNull(zoomableController);
        mZoomableController.setListener(null);
        mZoomableController = zoomableController;
        mZoomableController.setListener(mZoomableListener);
    }

    public ZoomableController getZoomableController() {
        return mZoomableController;
    }

    public boolean allowsTouchInterceptionWhileZoomed() {
        return mAllowTouchInterceptionWhileZoomed;
    }

    public void setAllowTouchInterceptionWhileZoomed(
            boolean allowTouchInterceptionWhileZoomed) {
        mAllowTouchInterceptionWhileZoomed = allowTouchInterceptionWhileZoomed;
    }

    public void setTapListener(GestureDetector.SimpleOnGestureListener tapListener) {
        mTapListenerWrapper.setListener(tapListener);
    }

    public void setEnableGestureDiscard(boolean discard) {
        mZoomableController.setEnableGestureDiscard(discard);
    }

    public void setIsLongpressEnabled(boolean enabled) {
        mTapGestureDetector.setIsLongpressEnabled(enabled);
    }

    @Override
    public void setController(@Nullable DraweeController controller) {
        setControllers(controller, null);
    }

    public void setControllers(
            @Nullable DraweeController controller,
            @Nullable DraweeController hugeImageController) {
        setControllersInternal(null, null);
        mZoomableController.setEnabled(false);
        setControllersInternal(controller, hugeImageController);
    }

    private void setControllersInternal(
            @Nullable DraweeController controller,
            @Nullable DraweeController hugeImageController) {
        removeControllerListener(getController());
        addControllerListener(controller);
        mHugeImageController = hugeImageController;
        super.setController(controller);
    }

    private void maybeSetHugeImageController() {
        if (mHugeImageController != null &&
                mZoomableController.getScaleFactor() > HUGE_IMAGE_SCALE_FACTOR_THRESHOLD) {
            setControllersInternal(mHugeImageController, null);
        }
    }

    private void removeControllerListener(DraweeController controller) {
        if (controller instanceof AbstractDraweeController) {
            ((AbstractDraweeController) controller)
                    .removeControllerListener(mControllerListener);
        }
    }

    private void addControllerListener(DraweeController controller) {
        if (controller instanceof AbstractDraweeController) {
            ((AbstractDraweeController) controller)
                    .addControllerListener(mControllerListener);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int saveCount = canvas.save();
        canvas.concat(mZoomableController.getTransform());
        super.onDraw(canvas);
        canvas.restoreToCount(saveCount);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int a = event.getActionMasked();
        if (mTapGestureDetector.onTouchEvent(event)) {
            return true;
        }

        if (mUseSimpleTouchHandling) {
            if (mZoomableController.onTouchEvent(event)) {
                return true;
            }
        } else if (mZoomableController.onTouchEvent(event)) {
            // Do not allow the parent to intercept touch events if:
            // - we do not allow swiping while zoomed and the image is zoomed
            // - we allow swiping while zoomed and the transform was corrected
            if ((!mAllowTouchInterceptionWhileZoomed && !mZoomableController.isIdentity()) ||
                    (mAllowTouchInterceptionWhileZoomed && !mZoomableController.wasTransformCorrected())) {
                getParent().requestDisallowInterceptTouchEvent(true);
            }
            return true;
        }
        if (super.onTouchEvent(event)) {
            return true;
        }
        MotionEvent cancelEvent = MotionEvent.obtain(event);
        cancelEvent.setAction(MotionEvent.ACTION_CANCEL);
        mTapGestureDetector.onTouchEvent(cancelEvent);
        mZoomableController.onTouchEvent(cancelEvent);
        cancelEvent.recycle();
        return false;
    }

    @Override
    public int computeHorizontalScrollRange() {
        return mZoomableController.computeHorizontalScrollRange();
    }

    @Override
    public int computeHorizontalScrollOffset() {
        return mZoomableController.computeHorizontalScrollOffset();
    }

    @Override
    public int computeHorizontalScrollExtent() {
        return mZoomableController.computeHorizontalScrollExtent();
    }

    @Override
    public int computeVerticalScrollRange() {
        return mZoomableController.computeVerticalScrollRange();
    }

    @Override
    public int computeVerticalScrollOffset() {
        return mZoomableController.computeVerticalScrollOffset();
    }

    @Override
    public int computeVerticalScrollExtent() {
        return mZoomableController.computeVerticalScrollExtent();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        updateZoomableControllerBounds();
    }

    private void onFinalImageSet() {
        if (!mZoomableController.isEnabled()) {
            updateZoomableControllerBounds();
            mZoomableController.setEnabled(true);
        }
    }

    private void onRelease() {
        mZoomableController.setEnabled(false);
    }

    protected void onTransformChanged(Matrix transform) {
        maybeSetHugeImageController();
        invalidate();
    }

    protected void updateZoomableControllerBounds() {
        getImageBounds(mImageBounds);
        getLimitBounds(mViewBounds);

        mZoomableController.setImageBounds(mImageBounds);
        mZoomableController.setViewBounds(mViewBounds);
        mZoomableController.initDefaultScale(mViewBounds, mImageBounds);
    }

    protected Class<?> getLogTag() {
        return TAG;
    }

    protected ZoomableController createZoomableController() {
        return AnimatedZoomableController.newInstance();
    }

    public void setExperimentalSimpleTouchHandlingEnabled(boolean enabled) {
        mUseSimpleTouchHandling = enabled;
    }
}
