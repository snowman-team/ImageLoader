package com.xueqiu.image.loader.zoom.zoomable;

import android.graphics.Matrix;
import android.graphics.RectF;
import android.view.MotionEvent;

// Copied from https://github.com/facebook/fresco/tree/master/samples/zoomable/
public interface ZoomableController {

    interface Listener {

        void onTransformChanged(Matrix transform);
    }

    interface OnSwipeDownListener {
        void onSwipeDown(float translateY);
        void onSwipeRelease(float translateY);
    }

    void setSwipeDownListener(OnSwipeDownListener listener);

    void setEnabled(boolean enabled);

    boolean isEnabled();

    void setEnableGestureDiscard(boolean discardGesture);

    void setListener(Listener listener);

    float getScaleFactor();

    float getOriginScaleFactor();

    float getTranslateY();

    boolean isIdentity();

    boolean wasTransformCorrected();

    int computeHorizontalScrollRange();

    int computeHorizontalScrollOffset();

    int computeHorizontalScrollExtent();

    int computeVerticalScrollRange();

    int computeVerticalScrollOffset();

    int computeVerticalScrollExtent();

    Matrix getTransform();

    RectF getImageBounds();

    void setImageBounds(RectF imageBounds);

    void setViewBounds(RectF viewBounds);

    void initDefaultScale(RectF viewBounds, RectF imageBounds);

    boolean onTouchEvent(MotionEvent event);
}
