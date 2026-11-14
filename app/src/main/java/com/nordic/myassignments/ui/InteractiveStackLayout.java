package com.nordic.myassignments.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class InteractiveStackLayout extends FrameLayout {

    public interface OnStackGestureListener {
        void onSwipedLeft();
        void onSwipedRight();
        void onDoubleTapped();
        void onDragProgress(float dx, float fraction);
        void onDragCanceled();
    }

    private OnStackGestureListener gestureListener;
    private GestureDetector gestureDetector;
    private VelocityTracker velocityTracker;
    private int touchSlop;
    private float downX;
    private float downY;
    private boolean isDragging = false;
    private boolean gesturesEnabled = true;

    public InteractiveStackLayout(@NonNull Context context) {
        super(context);
        init(context);
    }

    public InteractiveStackLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public InteractiveStackLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                if (gesturesEnabled && gestureListener != null) {
                    gestureListener.onDoubleTapped();
                    return true;
                }
                return false;
            }

            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }
        });
    }

    public void setOnStackGestureListener(OnStackGestureListener listener) {
        this.gestureListener = listener;
    }

    public void setGesturesEnabled(boolean enabled) {
        this.gesturesEnabled = enabled;
        if (!enabled) {
            isDragging = false;
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (!gesturesEnabled) {
            return false;
        }

        // Catch double-taps anywhere on the card surface
        gestureDetector.onTouchEvent(ev);

        int action = ev.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                downX = ev.getRawX();
                downY = ev.getRawY();
                isDragging = false;
                if (velocityTracker == null) {
                    velocityTracker = VelocityTracker.obtain();
                } else {
                    velocityTracker.clear();
                }
                velocityTracker.addMovement(ev);
                break;

            case MotionEvent.ACTION_MOVE:
                float dx = ev.getRawX() - downX;
                float dy = ev.getRawY() - downY;

                // If moving horizontally more than vertically, intercept!
                if (!isDragging && Math.abs(dx) > touchSlop && Math.abs(dx) > Math.abs(dy) * 1.15f) {
                    isDragging = true;
                    if (getParent() != null) {
                        getParent().requestDisallowInterceptTouchEvent(true);
                    }
                    return true;
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isDragging = false;
                break;
        }

        return isDragging;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (!gesturesEnabled) {
            return super.onTouchEvent(ev);
        }

        gestureDetector.onTouchEvent(ev);

        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain();
        }
        velocityTracker.addMovement(ev);

        int action = ev.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                downX = ev.getRawX();
                downY = ev.getRawY();
                return true;

            case MotionEvent.ACTION_MOVE:
                float dx = ev.getRawX() - downX;
                float dy = ev.getRawY() - downY;

                if (!isDragging && Math.abs(dx) > touchSlop && Math.abs(dx) > Math.abs(dy) * 1.15f) {
                    isDragging = true;
                    if (getParent() != null) {
                        getParent().requestDisallowInterceptTouchEvent(true);
                    }
                }

                if (isDragging && gestureListener != null) {
                    float width = getWidth() > 0 ? getWidth() : 800;
                    float fraction = Math.min(Math.abs(dx) / (width * 0.45f), 1f);
                    gestureListener.onDragProgress(dx, fraction);
                    return true;
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (isDragging) {
                    isDragging = false;
                    velocityTracker.computeCurrentVelocity(1000);
                    float vx = velocityTracker.getXVelocity();
                    float totalDx = ev.getRawX() - downX;

                    float width = getWidth() > 0 ? getWidth() : 800;
                    boolean isFling = Math.abs(vx) > 850;
                    boolean isPastThreshold = Math.abs(totalDx) > (width * 0.22f);

                    if (isPastThreshold || isFling) {
                        if (totalDx < 0 || vx < -850) {
                            if (gestureListener != null) gestureListener.onSwipedLeft();
                        } else {
                            if (gestureListener != null) gestureListener.onSwipedRight();
                        }
                    } else {
                        if (gestureListener != null) gestureListener.onDragCanceled();
                    }

                    if (velocityTracker != null) {
                        velocityTracker.recycle();
                        velocityTracker = null;
                    }
                    return true;
                }

                if (velocityTracker != null) {
                    velocityTracker.recycle();
                    velocityTracker = null;
                }
                break;
        }

        return super.onTouchEvent(ev);
    }
}
