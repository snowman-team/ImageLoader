package com.xueqiu.image.loader.zoom.zoomable;

import android.graphics.Matrix;
import android.graphics.PointF;

import androidx.annotation.Nullable;

import com.xueqiu.image.loader.zoom.gestures.TransformGestureDetector;

// Copied from https://github.com/facebook/fresco/tree/master/samples/zoomable/
public abstract class AbstractAnimatedZoomableController extends DefaultZoomableController {

    private boolean mIsAnimating;
    private final float[] mStartValues = new float[9];
    private final float[] mStopValues = new float[9];
    private final float[] mCurrentValues = new float[9];
    private final Matrix mNewTransform = new Matrix();
    private final Matrix mWorkingTransform = new Matrix();


    public AbstractAnimatedZoomableController(TransformGestureDetector transformGestureDetector) {
        super(transformGestureDetector);
    }

    @Override
    public void reset() {
        stopAnimation();
        mWorkingTransform.reset();
        mNewTransform.reset();
        super.reset();
    }

    @Override
    public boolean isIdentity() {
        return !isAnimating() && super.isIdentity();
    }

    @Override
    public void zoomToPoint(
            float scale,
            PointF imagePoint,
            PointF viewPoint) {
        zoomToPoint(scale, imagePoint, viewPoint, LIMIT_ALL, 0, null);
    }

    public void zoomToPoint(
            float scale,
            PointF imagePoint,
            PointF viewPoint,
            @LimitFlag int limitFlags,
            long durationMs,
            @Nullable Runnable onAnimationComplete) {
        calculateZoomToPointTransform(
                mNewTransform,
                scale,
                imagePoint,
                viewPoint,
                limitFlags);
        setTransform(mNewTransform, durationMs, onAnimationComplete);
    }

    public void setTransform(
            Matrix newTransform,
            long durationMs,
            @Nullable Runnable onAnimationComplete) {
        if (durationMs <= 0) {
            setTransformImmediate(newTransform);
        } else {
            setTransformAnimated(newTransform, durationMs, onAnimationComplete);
        }
    }

    private void setTransformImmediate(final Matrix newTransform) {
        stopAnimation();
        mWorkingTransform.set(newTransform);
        super.setTransform(newTransform);
        getDetector().restartGesture();
    }

    protected boolean isAnimating() {
        return mIsAnimating;
    }

    protected void setAnimating(boolean isAnimating) {
        mIsAnimating = isAnimating;
    }

    protected float[] getStartValues() {
        return mStartValues;
    }

    protected float[] getStopValues() {
        return mStopValues;
    }

    protected Matrix getWorkingTransform() {
        return mWorkingTransform;
    }

    @Override
    public void onGestureBegin(TransformGestureDetector detector) {
        stopAnimation();
        super.onGestureBegin(detector);
    }

    @Override
    public void onGestureUpdate(TransformGestureDetector detector) {
        if (isAnimating()) {
            return;
        }
        super.onGestureUpdate(detector);
    }

    @Override
    protected void restoreImage(float fromX, float fromY) {
        PointF viewPoint = new PointF(fromX, fromY);

        zoomToPoint(getOriginScaleFactor(), mapViewToImage(viewPoint), viewPoint, LIMIT_ALL, 300, null);
    }

    protected void calculateInterpolation(Matrix outMatrix, float fraction) {
        for (int i = 0; i < 9; i++) {
            mCurrentValues[i] = (1 - fraction) * mStartValues[i] + fraction * mStopValues[i];
        }
        outMatrix.setValues(mCurrentValues);
    }

    public abstract void setTransformAnimated(
            final Matrix newTransform,
            long durationMs,
            @Nullable final Runnable onAnimationComplete);

    protected abstract void stopAnimation();

    protected abstract Class<?> getLogTag();
}
