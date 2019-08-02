package com.xueqiu.image.loader.zoom.gestures;

import android.view.MotionEvent;

public class MultiPointerGestureDetector {

    public interface Listener {
        public void onGestureBegin(MultiPointerGestureDetector detector);
        public void onGestureUpdate(MultiPointerGestureDetector detector);
        public void onGestureEnd(MultiPointerGestureDetector detector);
    }

    private static final int MAX_POINTERS = 2;

    private boolean mGestureInProgress;
    private int mPointerCount;
    private int mNewPointerCount;
    private final int mId[] = new int[MAX_POINTERS];
    private final float mStartX[] = new float[MAX_POINTERS];
    private final float mStartY[] = new float[MAX_POINTERS];
    private final float mCurrentX[] = new float[MAX_POINTERS];
    private final float mCurrentY[] = new float[MAX_POINTERS];

    private Listener mListener = null;

    public MultiPointerGestureDetector() {
        reset();
    }

    public static MultiPointerGestureDetector newInstance() {
        return new MultiPointerGestureDetector();
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    public void reset() {
        mGestureInProgress = false;
        mPointerCount = 0;
        for (int i = 0; i < MAX_POINTERS; i++) {
            mId[i] = MotionEvent.INVALID_POINTER_ID;
        }
    }

    protected boolean shouldStartGesture() {
        return true;
    }

    private void startGesture() {
        if (!mGestureInProgress) {
            if (mListener != null) {
                mListener.onGestureBegin(this);
            }
            mGestureInProgress = true;
        }
    }

    private void stopGesture() {
        if (mGestureInProgress) {
            mGestureInProgress = false;
            if (mListener != null) {
                mListener.onGestureEnd(this);
            }
        }
    }

    private int getPressedPointerIndex(MotionEvent event, int i) {
        final int count = event.getPointerCount();
        final int action = event.getActionMasked();
        final int index = event.getActionIndex();
        if (action == MotionEvent.ACTION_UP ||
                action == MotionEvent.ACTION_POINTER_UP) {
            if (i >= index) {
                i++;
            }
        }
        return (i < count) ? i : -1;
    }

    private static int getPressedPointerCount(MotionEvent event) {
        int count = event.getPointerCount();
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_UP ||
                action == MotionEvent.ACTION_POINTER_UP) {
            count--;
        }
        return count;
    }

    private void updatePointersOnTap(MotionEvent event) {
        mPointerCount = 0;
        for (int i = 0; i < MAX_POINTERS; i++) {
            int index = getPressedPointerIndex(event, i);
            if (index == -1) {
                mId[i] = MotionEvent.INVALID_POINTER_ID;
            } else {
                mId[i] = event.getPointerId(index);
                mCurrentX[i] = mStartX[i] = event.getX(index);
                mCurrentY[i] = mStartY[i] = event.getY(index);
                mPointerCount++;
            }
        }
    }

    private void updatePointersOnMove(MotionEvent event) {
        for (int i = 0; i < MAX_POINTERS; i++) {
            int index = event.findPointerIndex(mId[i]);
            if (index != -1) {
                mCurrentX[i] = event.getX(index);
                mCurrentY[i] = event.getY(index);
            }
        }
    }

    public boolean onTouchEvent(final MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_MOVE: {
                updatePointersOnMove(event);
                if (!mGestureInProgress && mPointerCount > 0 && shouldStartGesture()) {
                    startGesture();
                }
                if (mGestureInProgress && mListener != null) {
                    mListener.onGestureUpdate(this);
                }
                break;
            }

            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                mNewPointerCount = getPressedPointerCount(event);
                updatePointersOnTap(event);
                if (mPointerCount > 0 && shouldStartGesture()) {
                    startGesture();
                }
                break;
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_UP: {
                stopGesture();
                mNewPointerCount = getPressedPointerCount(event);
                updatePointersOnTap(event);
                break;
            }

            case MotionEvent.ACTION_CANCEL: {
                mNewPointerCount = 0;
                stopGesture();
                reset();
                break;
            }
        }
        return true;
    }

    public void restartGesture() {
        if (!mGestureInProgress) {
            return;
        }
        stopGesture();
        for (int i = 0; i < MAX_POINTERS; i++) {
            mStartX[i] = mCurrentX[i];
            mStartY[i] = mCurrentY[i];
        }
        startGesture();
    }

    public boolean isGestureInProgress() {
        return mGestureInProgress;
    }

    public int getNewPointerCount() {
        return mNewPointerCount;
    }

    public int getPointerCount() {
        return mPointerCount;
    }

    public float[] getStartX() {
        return mStartX;
    }

    public float[] getStartY() {
        return mStartY;
    }

    public float[] getCurrentX() {
        return mCurrentX;
    }

    public float[] getCurrentY() {
        return mCurrentY;
    }
}
