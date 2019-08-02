package com.xueqiu.image.loader.zoom.gestures;

import android.view.MotionEvent;

// Copied from https://github.com/facebook/fresco/tree/master/samples/zoomable/
public class TransformGestureDetector implements MultiPointerGestureDetector.Listener {

    public interface Listener {
        public void onGestureBegin(TransformGestureDetector detector);
        public void onGestureUpdate(TransformGestureDetector detector);
        public void onGestureEnd(TransformGestureDetector detector);
    }

    private final MultiPointerGestureDetector mDetector;

    private Listener mListener = null;

    public TransformGestureDetector(MultiPointerGestureDetector multiPointerGestureDetector) {
        mDetector = multiPointerGestureDetector;
        mDetector.setListener(this);
    }

    public static TransformGestureDetector newInstance() {
        return new TransformGestureDetector(MultiPointerGestureDetector.newInstance());
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    public void reset() {
        mDetector.reset();
    }

    public boolean onTouchEvent(final MotionEvent event) {
        return mDetector.onTouchEvent(event);
    }

    @Override
    public void onGestureBegin(MultiPointerGestureDetector detector) {
        if (mListener != null) {
            mListener.onGestureBegin(this);
        }
    }

    @Override
    public void onGestureUpdate(MultiPointerGestureDetector detector) {
        if (mListener != null) {
            mListener.onGestureUpdate(this);
        }
    }

    @Override
    public void onGestureEnd(MultiPointerGestureDetector detector) {
        if (mListener != null) {
            mListener.onGestureEnd(this);
        }
    }

    private float calcAverage(float[] arr, int len) {
        float sum = 0;
        for (int i = 0; i < len; i++) {
            sum += arr[i];
        }
        return (len > 0) ? sum / len : 0;
    }

    public void restartGesture() {
        mDetector.restartGesture();
    }

    public boolean isGestureInProgress() {
        return mDetector.isGestureInProgress();
    }

    public int getNewPointerCount() {
        return mDetector.getNewPointerCount();
    }

    public int getPointerCount() {
        return mDetector.getPointerCount();
    }

    public float getPivotX() {
        return calcAverage(mDetector.getStartX(), mDetector.getPointerCount());
    }

    public float getPivotY() {
        return calcAverage(mDetector.getStartY(), mDetector.getPointerCount());
    }

    public float getCurrentX() {
        return calcAverage(mDetector.getCurrentX(), mDetector.getPointerCount());
    }

    public float getCurrentY() {
        return calcAverage(mDetector.getCurrentY(), mDetector.getPointerCount());
    }

    public float getTranslationX() {
        return calcAverage(mDetector.getCurrentX(), mDetector.getPointerCount()) -
                calcAverage(mDetector.getStartX(), mDetector.getPointerCount());
    }

    public float getTranslationY() {
        return calcAverage(mDetector.getCurrentY(), mDetector.getPointerCount()) -
                calcAverage(mDetector.getStartY(), mDetector.getPointerCount());
    }

    public float getScale() {
        if (mDetector.getPointerCount() < 2) {
            return 1;
        } else {
            float startDeltaX = mDetector.getStartX()[1] - mDetector.getStartX()[0];
            float startDeltaY = mDetector.getStartY()[1] - mDetector.getStartY()[0];
            float currentDeltaX = mDetector.getCurrentX()[1] - mDetector.getCurrentX()[0];
            float currentDeltaY = mDetector.getCurrentY()[1] - mDetector.getCurrentY()[0];
            float startDist = (float) Math.hypot(startDeltaX, startDeltaY);
            float currentDist = (float) Math.hypot(currentDeltaX, currentDeltaY);
            return currentDist / startDist;
        }
    }

    public float getRotation() {
        if (mDetector.getPointerCount() < 2) {
            return 0;
        } else {
            float startDeltaX = mDetector.getStartX()[1] - mDetector.getStartX()[0];
            float startDeltaY = mDetector.getStartY()[1] - mDetector.getStartY()[0];
            float currentDeltaX = mDetector.getCurrentX()[1] - mDetector.getCurrentX()[0];
            float currentDeltaY = mDetector.getCurrentY()[1] - mDetector.getCurrentY()[0];
            float startAngle = (float) Math.atan2(startDeltaY, startDeltaX);
            float currentAngle = (float) Math.atan2(currentDeltaY, currentDeltaX);
            return currentAngle - startAngle;
        }
    }
}
