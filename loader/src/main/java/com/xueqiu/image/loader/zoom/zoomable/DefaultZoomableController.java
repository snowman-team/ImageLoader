package com.xueqiu.image.loader.zoom.zoomable;

import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.RectF;
import android.view.MotionEvent;

import androidx.annotation.IntDef;

import com.xueqiu.image.loader.zoom.gestures.TransformGestureDetector;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

// Copied from https://github.com/facebook/fresco/tree/master/samples/zoomable/
public class DefaultZoomableController
        implements ZoomableController, TransformGestureDetector.Listener {

    @IntDef(flag = true, value = {
            LIMIT_NONE,
            LIMIT_TRANSLATION_X,
            LIMIT_TRANSLATION_Y,
            LIMIT_SCALE,
            LIMIT_ALL
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface LimitFlag {
    }

    public static final int LIMIT_NONE = 0;
    public static final int LIMIT_TRANSLATION_X = 1;
    public static final int LIMIT_TRANSLATION_Y = 2;
    public static final int LIMIT_SCALE = 4;
    public static final int LIMIT_ALL = LIMIT_TRANSLATION_X | LIMIT_TRANSLATION_Y | LIMIT_SCALE;

    private static final float EPS = 1e-3f;

    private static final Class<?> TAG = DefaultZoomableController.class;

    private static final RectF IDENTITY_RECT = new RectF(0, 0, 1, 1);

    private static final float MAX_SCALE_FACTOR = 3.0F;
    private static final float MIN_SCALE_FACTOR = 0.7F;

    private TransformGestureDetector mGestureDetector;

    private Listener mListener = null;

    private boolean mIsEnabled = false;
    private boolean mEnableGestureDiscard = true;
    private boolean mIsRotationEnabled = false;
    private boolean mIsScaleEnabled = true;
    private boolean mIsTranslationEnabled = true;

    private float mMinScaleFactor = MIN_SCALE_FACTOR;
    private float mMaxScaleFactor = MAX_SCALE_FACTOR;
    private float mOriginScaleFactor = 1.0f;

    private final RectF mViewBounds = new RectF();
    private final RectF mImageBounds = new RectF();
    private final RectF mTransformedImageBounds = new RectF();

    private final Matrix mPreviousTransform = new Matrix();
    private final Matrix mActiveTransform = new Matrix();
    private final Matrix mActiveTransformInverse = new Matrix();
    private final float[] mTempValues = new float[9];
    private final RectF mTempRect = new RectF();
    private boolean mWasTransformCorrected;

    private boolean mCanScrollUpThisGesture;
    private boolean mIsInSwipeDown;
    protected OnSwipeDownListener mSwipeDownListener;

    public static DefaultZoomableController newInstance() {
        return new DefaultZoomableController(TransformGestureDetector.newInstance());
    }

    public DefaultZoomableController(TransformGestureDetector gestureDetector) {
        mGestureDetector = gestureDetector;
        mGestureDetector.setListener(this);
    }

    @Override
    public void setSwipeDownListener(OnSwipeDownListener listener) {
        mSwipeDownListener = listener;
    }

    public void reset() {
        mGestureDetector.reset();
        mPreviousTransform.reset();
        mActiveTransform.reset();
        onTransformChanged();
    }

    @Override
    public void setListener(Listener listener) {
        mListener = listener;
    }

    @Override
    public void setEnabled(boolean enabled) {
        mIsEnabled = enabled;
        if (!enabled) {
            reset();
        }
    }

    @Override
    public void setEnableGestureDiscard(boolean enable) {
        mEnableGestureDiscard = enable;
    }

    @Override
    public boolean isEnabled() {
        return mIsEnabled;
    }

    public void setRotationEnabled(boolean enabled) {
        mIsRotationEnabled = enabled;
    }

    public boolean isRotationEnabled() {
        return mIsRotationEnabled;
    }

    public void setScaleEnabled(boolean enabled) {
        mIsScaleEnabled = enabled;
    }

    public boolean isScaleEnabled() {
        return mIsScaleEnabled;
    }

    public void setTranslationEnabled(boolean enabled) {
        mIsTranslationEnabled = enabled;
    }

    public boolean isTranslationEnabled() {
        return mIsTranslationEnabled;
    }

    public void setMinScaleFactor(float minScaleFactor) {
        mMinScaleFactor = minScaleFactor;
    }

    public float getMinScaleFactor() {
        // Edit By BoSong
        return mMinScaleFactor * mOriginScaleFactor;
    }

    public void setMaxScaleFactor(float maxScaleFactor) {
        mMaxScaleFactor = maxScaleFactor;
    }

    public float getMaxScaleFactor() {
        // Edit By BoSong
        return mMaxScaleFactor * mOriginScaleFactor;
    }

    public void setOriginScaleFactor(float originScaleFactor) {
        mOriginScaleFactor = originScaleFactor;
    }

    @Override
    public float getOriginScaleFactor() {
        return mOriginScaleFactor;
    }

    @Override
    public float getScaleFactor() {
        return getMatrixScaleFactor(mActiveTransform);
    }

    @Override
    public float getTranslateY() {
        return getMatrixTranslateY(mActiveTransform);
    }

    @Override
    public void setImageBounds(RectF imageBounds) {
        if (!imageBounds.equals(mImageBounds)) {
            mImageBounds.set(imageBounds);
            onTransformChanged();
        }
    }

    @Override
    public RectF getImageBounds() {
        return mImageBounds;
    }

    private RectF getTransformedImageBounds() {
        return mTransformedImageBounds;
    }

    @Override
    public void setViewBounds(RectF viewBounds) {
        mViewBounds.set(viewBounds);
    }

    public RectF getViewBounds() {
        return mViewBounds;
    }

    @Override
    public void initDefaultScale(RectF viewBounds, RectF imageBounds) {
        if (imageBounds.left > viewBounds.left) { // if image not fits width, scale it to fitting width
            float scale = (viewBounds.right - viewBounds.left) / (imageBounds.right - imageBounds.left);
            setOriginScaleFactor(scale);
            zoomToPoint(scale, new PointF(0.f, 0.f), new PointF(0.f, 0.f));
        }
    }

    @Override
    public boolean isIdentity() {
        return isMatrixIdentity(mActiveTransform, 1e-3f);
    }

    @Override
    public boolean wasTransformCorrected() {
        return mWasTransformCorrected;
    }

    @Override
    public Matrix getTransform() {
        return mActiveTransform;
    }

    public void getImageRelativeToViewAbsoluteTransform(Matrix outMatrix) {
        outMatrix.setRectToRect(IDENTITY_RECT, mTransformedImageBounds, Matrix.ScaleToFit.FILL);
    }

    public PointF mapViewToImage(PointF viewPoint) {
        float[] points = mTempValues;
        points[0] = viewPoint.x;
        points[1] = viewPoint.y;
        mActiveTransform.invert(mActiveTransformInverse);
        mActiveTransformInverse.mapPoints(points, 0, points, 0, 1);
        mapAbsoluteToRelative(points, points, 1);
        return new PointF(points[0], points[1]);
    }

    public PointF mapImageToView(PointF imagePoint) {
        float[] points = mTempValues;
        points[0] = imagePoint.x;
        points[1] = imagePoint.y;
        mapRelativeToAbsolute(points, points, 1);
        mActiveTransform.mapPoints(points, 0, points, 0, 1);
        return new PointF(points[0], points[1]);
    }

    private void mapAbsoluteToRelative(float[] destPoints, float[] srcPoints, int numPoints) {
        for (int i = 0; i < numPoints; i++) {
            destPoints[i * 2 + 0] = (srcPoints[i * 2 + 0] - mImageBounds.left) / mImageBounds.width();
            destPoints[i * 2 + 1] = (srcPoints[i * 2 + 1] - mImageBounds.top) / mImageBounds.height();
        }
    }

    private void mapRelativeToAbsolute(float[] destPoints, float[] srcPoints, int numPoints) {
        for (int i = 0; i < numPoints; i++) {
            destPoints[i * 2 + 0] = srcPoints[i * 2 + 0] * mImageBounds.width() + mImageBounds.left;
            destPoints[i * 2 + 1] = srcPoints[i * 2 + 1] * mImageBounds.height() + mImageBounds.top;
        }
    }

    public void zoomToPoint(float scale, PointF imagePoint, PointF viewPoint) {
        calculateZoomToPointTransform(mActiveTransform, scale, imagePoint, viewPoint, LIMIT_ALL);
        onTransformChanged();
    }

    protected boolean calculateZoomToPointTransform(
            Matrix outTransform,
            float scale,
            PointF imagePoint,
            PointF viewPoint,
            @LimitFlag int limitFlags) {
        float[] viewAbsolute = mTempValues;
        viewAbsolute[0] = imagePoint.x;
        viewAbsolute[1] = imagePoint.y;
        mapRelativeToAbsolute(viewAbsolute, viewAbsolute, 1);
        float distanceX = viewPoint.x - viewAbsolute[0];
        float distanceY = viewPoint.y - viewAbsolute[1];
        boolean transformCorrected = false;
        outTransform.setScale(scale, scale, viewAbsolute[0], viewAbsolute[1]);
        transformCorrected |= limitScale(outTransform, viewAbsolute[0], viewAbsolute[1], limitFlags);
        outTransform.postTranslate(distanceX, distanceY);
        transformCorrected |= limitTranslation(outTransform, limitFlags);
        return transformCorrected;
    }

    public void translateTo(float distanceX, float distanceY) {
        calculateTranslateTransform(mActiveTransform, distanceX, distanceY);
        onTransformChanged();
    }

    protected void calculateTranslateTransform(Matrix outTransform, float distanceX, float distanceY) {
        outTransform.postTranslate(distanceX, distanceY);
        float[] viewAbsolute = mTempValues;
        viewAbsolute[0] = 0.5f;
        viewAbsolute[1] = 0.5f;
        mapRelativeToAbsolute(viewAbsolute, viewAbsolute, 1);
        float scale = (getViewBounds().height() - distanceY) / getViewBounds().height();
        outTransform.postScale(scale, scale, viewAbsolute[0], viewAbsolute[1]);
        limitScale(outTransform, viewAbsolute[0], viewAbsolute[1], LIMIT_ALL);
    }

    public void setTransform(Matrix newTransform) {
        mActiveTransform.set(newTransform);
        onTransformChanged();
    }

    protected TransformGestureDetector getDetector() {
        return mGestureDetector;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mIsEnabled) {
            return mGestureDetector.onTouchEvent(event);
        }
        return false;
    }

    /* TransformGestureDetector.Listener methods  */

    @Override
    public void onGestureBegin(TransformGestureDetector detector) {
        mPreviousTransform.set(mActiveTransform);
        mWasTransformCorrected = !canScrollInAllDirection();
        if (!canScrollUp()) {
            mCanScrollUpThisGesture = false;
        } else {
            mCanScrollUpThisGesture = true;
        }
    }

    @Override
    public void onGestureUpdate(TransformGestureDetector detector) {
        boolean transformCorrected = calculateGestureTransform(mActiveTransform, LIMIT_ALL);
        float translateX = detector.getTranslationX();
        float translateY = detector.getTranslationY();

        if (getScaleFactor() == getOriginScaleFactor() && !mCanScrollUpThisGesture && translateY > 0) {
            translateTo(translateX, translateY);
            mIsInSwipeDown = true;
            if (mSwipeDownListener != null) {
                mSwipeDownListener.onSwipeDown(translateY);
            }
        }
        onTransformChanged();
        mWasTransformCorrected = transformCorrected;
    }

    @Override
    public void onGestureEnd(TransformGestureDetector detector) {
        dispatchSwipeRelease(detector.getTranslationY());

        if (mEnableGestureDiscard && isGestureNeedDiscard()) {
            restoreImage(detector.getCurrentX(), detector.getCurrentY());
        }
    }

    protected boolean isGestureNeedDiscard() {
        return getScaleFactor() < getOriginScaleFactor() ||
                (getScaleFactor() == getOriginScaleFactor() && getTranslateY() != 0.0f);
    }

    protected void restoreImage(float fromX, float fromY) {
        PointF viewPoint = new PointF(fromX, fromY);

        zoomToPoint(getOriginScaleFactor(), mapViewToImage(viewPoint), viewPoint);
    }

    protected void dispatchSwipeRelease(float translateY) {
        if (mIsInSwipeDown) {
            mIsInSwipeDown = false;
            if (mSwipeDownListener != null) {
                mSwipeDownListener.onSwipeRelease(translateY);
            }
        }
    }

    protected boolean calculateGestureTransform(
            Matrix outTransform,
            @LimitFlag int limitTypes) {
        TransformGestureDetector detector = mGestureDetector;
        boolean transformCorrected = false;
        outTransform.set(mPreviousTransform);
        if (mIsRotationEnabled) {
            float angle = detector.getRotation() * (float) (180 / Math.PI);
            outTransform.postRotate(angle, detector.getPivotX(), detector.getPivotY());
        }
        if (mIsScaleEnabled) {
            float scale = detector.getScale();
            outTransform.postScale(scale, scale, detector.getPivotX(), detector.getPivotY());
        }
        transformCorrected |=
                limitScale(outTransform, detector.getPivotX(), detector.getPivotY(), limitTypes);
        if (mIsTranslationEnabled) {
            outTransform.postTranslate(detector.getTranslationX(), detector.getTranslationY());
        }
        transformCorrected |= limitTranslation(outTransform, limitTypes);
        return transformCorrected;
    }

    private void onTransformChanged() {
        mActiveTransform.mapRect(mTransformedImageBounds, mImageBounds);
        if (mListener != null && isEnabled()) {
            mListener.onTransformChanged(mActiveTransform);
        }
    }

    private boolean limitScale(
            Matrix transform,
            float pivotX,
            float pivotY,
            @LimitFlag int limitTypes) {
        if (!shouldLimit(limitTypes, LIMIT_SCALE)) {
            return false;
        }
        float currentScale = getMatrixScaleFactor(transform);
        // Edit by BoSong
        float targetScale = limit(currentScale, mMinScaleFactor * mOriginScaleFactor, mMaxScaleFactor * mOriginScaleFactor);
        if (targetScale != currentScale) {
            float scale = targetScale / currentScale;
            transform.postScale(scale, scale, pivotX, pivotY);
            return true;
        }
        return false;
    }

    private boolean limitTranslation(Matrix transform, @LimitFlag int limitTypes) {
        if (!shouldLimit(limitTypes, LIMIT_TRANSLATION_X | LIMIT_TRANSLATION_Y)) {
            return false;
        }
        RectF b = mTempRect;
        b.set(mImageBounds);
        transform.mapRect(b);
        float offsetLeft = shouldLimit(limitTypes, LIMIT_TRANSLATION_X) ?
                getOffset(b.left, b.right, mViewBounds.left, mViewBounds.right, mImageBounds.centerX()) : 0;
        float offsetTop = shouldLimit(limitTypes, LIMIT_TRANSLATION_Y) ?
                getOffset(b.top, b.bottom, mViewBounds.top, mViewBounds.bottom, mImageBounds.centerY()) : 0;
        if (offsetLeft != 0 || offsetTop != 0) {
            transform.postTranslate(offsetLeft, offsetTop);
            return true;
        }
        return false;
    }

    private static boolean shouldLimit(@LimitFlag int limits, @LimitFlag int flag) {
        return (limits & flag) != LIMIT_NONE;
    }

    private float getOffset(
            float imageStart,
            float imageEnd,
            float limitStart,
            float limitEnd,
            float limitCenter) {
        float imageWidth = imageEnd - imageStart, limitWidth = limitEnd - limitStart;
        float limitInnerWidth = Math.min(limitCenter - limitStart, limitEnd - limitCenter) * 2;
        if (imageWidth < limitInnerWidth) {
            return limitCenter - (imageEnd + imageStart) / 2;
        }
        if (imageWidth < limitWidth) {
            if (limitCenter < (limitStart + limitEnd) / 2) {
                return limitStart - imageStart;
            } else {
                return limitEnd - imageEnd;
            }
        }
        if (imageStart > limitStart) {
            return limitStart - imageStart;
        }
        if (imageEnd < limitEnd) {
            return limitEnd - imageEnd;
        }
        return 0;
    }

    private float limit(float value, float min, float max) {
        return Math.min(Math.max(min, value), max);
    }

    private float getMatrixScaleFactor(Matrix transform) {
        transform.getValues(mTempValues);
        return mTempValues[Matrix.MSCALE_X];
    }

    private float getMatrixTranslateY(Matrix transform) {
        transform.getValues(mTempValues);
        return mTempValues[Matrix.MTRANS_Y];
    }

    private boolean isMatrixIdentity(Matrix transform, float eps) {
        transform.getValues(mTempValues);
        mTempValues[0] -= 1.0f; // m00
        mTempValues[4] -= 1.0f; // m11
        mTempValues[8] -= 1.0f; // m22
        for (int i = 0; i < 9; i++) {
            if (Math.abs(mTempValues[i]) > eps) {
                return false;
            }
        }
        return true;
    }

    private boolean canScrollInAllDirection() {
        return mTransformedImageBounds.left < mViewBounds.left - EPS &&
                mTransformedImageBounds.top < mViewBounds.top - EPS &&
                mTransformedImageBounds.right > mViewBounds.right + EPS &&
                mTransformedImageBounds.bottom > mViewBounds.bottom + EPS;
    }

    private boolean canScrollUp() {
        return mTransformedImageBounds.top < mViewBounds.top - EPS;
    }

    private boolean canScrollDown() {
        return mTransformedImageBounds.bottom > mViewBounds.bottom + EPS;
    }

    @Override
    public int computeHorizontalScrollRange() {
        return (int) mTransformedImageBounds.width();
    }

    @Override
    public int computeHorizontalScrollOffset() {
        return (int) (mViewBounds.left - mTransformedImageBounds.left);
    }

    @Override
    public int computeHorizontalScrollExtent() {
        return (int) mViewBounds.width();
    }

    @Override
    public int computeVerticalScrollRange() {
        return (int) mTransformedImageBounds.height();
    }

    @Override
    public int computeVerticalScrollOffset() {
        return (int) (mViewBounds.top - mTransformedImageBounds.top);
    }

    @Override
    public int computeVerticalScrollExtent() {
        return (int) mViewBounds.height();
    }

}
