package com.swoag.logalong.utils;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.os.Build;
import android.os.Handler;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.ScrollView;

import com.swoag.logalong.LApp;

import java.lang.reflect.Field;

public class LViewUtils {
    private static final String TAG = LViewUtils.class.getSimpleName();
    private static long lastClickTime;

    private static int screenW;
    private static int screenH;
    private static float screenScale;

    public static int screenWidth () {
        return screenW;
    }
    public static int screenHeight () {
        return screenH;
    }
    public static int dp2px (int dp) {
        return (int) (dp * screenScale + 0.5f);
    }
    public static int px2dp (int px) {
        if (screenScale == 0) return px;
        return (int) (px / screenScale);
    }
    public static void screenInit () {
        screenScale = LApp.ctx.getResources().getDisplayMetrics().density;

        WindowManager wm = (WindowManager) LApp.ctx.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        screenW = display.getWidth();  // deprecated
        screenH = display.getHeight();  // deprecated
    }

    public static boolean isDoubleClick () {
        long now = System.currentTimeMillis();
        boolean ret = false;
        if (now - lastClickTime < 500) ret = true;
        lastClickTime = now;
        return ret;
    }

    public static void setBackgroundColor(View view, int color) {
        Drawable background = view.getBackground();
        if (background instanceof ShapeDrawable) {
            ShapeDrawable shapeDrawable = (ShapeDrawable) background;
            shapeDrawable.getPaint().setColor(color);
        } else if (background instanceof GradientDrawable) {
            GradientDrawable gradientDrawable = (GradientDrawable) background;
            gradientDrawable.setColor(color);
        } else if (background instanceof ColorDrawable) {
            int currentapiVersion = android.os.Build.VERSION.SDK_INT;
            if (currentapiVersion >= Build.VERSION_CODES.HONEYCOMB) {
                ColorDrawable colorDrawable = (ColorDrawable) background;
                colorDrawable.setColor(color);
            }
        }
    }

    public static void recycleBitmap (ImageView v) {
        //LLog.d(TAG, "recycle image view: " + v);

        BitmapDrawable drw = (BitmapDrawable)v.getDrawable();
        if (drw != null) {
            Bitmap bmp = drw.getBitmap();
            if (bmp != null && !bmp.isRecycled()) bmp.recycle();
            bmp = null;
        }
        drw = null;
    }

    public static void releaseActivityContext (Context context) {
        LayoutInflater inflater = null;
        Class<?> klass = null;
        Field f;

        try {
            inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            klass = inflater.getClass();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (klass != null && inflater != null) {
            try {
                f = klass.getSuperclass().getDeclaredField("mContext");
                if (null != f) {
                    f.setAccessible(true); // prevent IllegalAccessException
                    f.set(inflater, null); // can cause IllegalAccessException
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                f = klass.getSuperclass().getDeclaredField("mPrivateFactory");
                if (null != f) {
                    f.setAccessible(true); // prevent IllegalAccessException
                    f.set(inflater, null); // can cause IllegalAccessException
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                // this is specifically for FragmentActivity
                f = klass.getSuperclass().getDeclaredField("mFactory");
                if (null != f) {
                    f.setAccessible(true); // prevent IllegalAccessException
                    f.set(inflater, null); // can cause IllegalAccessException
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void disableEnableControls(boolean enable, ViewGroup vg){
        try {
            vg.setEnabled(enable);
            for (int ii = 0; ii < vg.getChildCount(); ii++){
                View child = vg.getChildAt(ii);
                child.setEnabled(enable);
                if (child instanceof ViewGroup) {
                    disableEnableControls(enable, (ViewGroup)child);
                }
            }
        } catch (Exception e) {
            LLog.e(TAG, "unexpected error: " + e.getMessage());
        }
    }

    public static void disableEnableClickable(boolean enable, ViewGroup vg){
        try {
            vg.setClickable(enable);
            vg.setFocusable(enable);
            for (int ii = 0; ii < vg.getChildCount(); ii++){
                View child = vg.getChildAt(ii);
                child.setClickable(enable);
                child.setFocusable(enable);
                if (child instanceof ViewGroup) {
                    disableEnableClickable(enable, (ViewGroup)child);
                }
            }
        } catch (Exception e) {
            LLog.e(TAG, "unexpected error: " + e.getMessage());
        }
    }

    public static void setOnTouchListener(View.OnTouchListener listener, ViewGroup vg){
        try {
            for (int ii = 0; ii < vg.getChildCount(); ii++){
                View child = vg.getChildAt(ii);
                child.setOnTouchListener(listener);
                if (child instanceof ViewGroup) {
                    setOnTouchListener(listener, (ViewGroup)child);
                }
            }
        } catch (Exception e) {
            LLog.e(TAG, "unexpected error: " + e.getMessage());
        }
    }

    public static void setTransparentBackground(int color, ViewGroup vg){
        try {
            for (int ii = 0; ii < vg.getChildCount(); ii++){
                View child = vg.getChildAt(ii);
                child.setBackgroundColor(color);
                if (child instanceof ViewGroup) {
                    setTransparentBackground(color, (ViewGroup)child);
                }
            }
        } catch (Exception e) {
            LLog.e(TAG, "unexpected error: " + e.getMessage());
        }
    }

    @SuppressLint("NewApi")
    public static void smoothScrollY (ScrollView scrollView, int target) {
        if (Build.VERSION.SDK_INT >= 11) {
            ObjectAnimator animator=ObjectAnimator.ofInt(scrollView, "scrollY", target);
            animator.setDuration(750);
            animator.start();
        } else {
            scrollView.smoothScrollBy(0, target);
        }
    }

    @SuppressLint("NewApi")
    public static void setAlpha (View v, float alpha) {
        if (Build.VERSION.SDK_INT >= 11) {
            v.setAlpha(alpha);
        } else {
            AlphaAnimation anim = new AlphaAnimation(alpha, alpha);
            anim.setDuration(0);
            anim.setFillAfter(true);
            v.startAnimation(anim);
        }
    }

    @SuppressLint("NewApi")
    public static void setAlphaTransient (View v, float alpha) {
        AlphaAnimation anim = new AlphaAnimation(alpha, alpha);
        anim.setDuration(0);
        anim.setFillAfter(true);
        v.startAnimation(anim);
    }

    public static void smoothSetAlpha (View v, float from, float to) {
        AlphaAnimation anim = new AlphaAnimation(from, to);
        anim.setDuration(500);
        anim.setFillAfter(true);
        v.startAnimation(anim);
    }

    public static void smoothSetAlpha (View v, float from, float to, int duration) {
        AlphaAnimation anim = new AlphaAnimation(from, to);
        anim.setDuration(duration);
        anim.setFillAfter(true);
        v.startAnimation(anim);
    }

    private static class MyAlphaAnimation extends AlphaAnimation {
        private View mView;
        private boolean mFadeOut;
        public MyAlphaAnimation(View v, boolean fadeOut) {
            super(fadeOut? 1.0f : 0.0f, fadeOut? 0.0f : 1.0f);
            mView = v;
            mFadeOut = fadeOut;
            mView.setVisibility(View.VISIBLE);

            setAnimationListener(new AnimationListener() {
                @Override
                public void onAnimationStart(Animation arg0) {}
                @Override
                public void onAnimationRepeat(Animation arg0) {}
                @Override
                public void onAnimationEnd(Animation arg0) {
                    //LLog.d(TAG, "enter: " + mFadeOut);
                    mView.setVisibility(mFadeOut? View.GONE : View.VISIBLE);
                    mView.clearAnimation();
                }
            });
        }
    }
    public static void smoothFade (View v, boolean fadeOut, int milliSecond) {
        final AlphaAnimation anim = new MyAlphaAnimation(v, fadeOut);
        anim.setDuration(milliSecond);
        anim.setFillAfter(true);
        v.startAnimation(anim);
    }

    @SuppressLint("NewApi")
    public static void rotateView (View v, float angle) {
        if (Build.VERSION.SDK_INT >= 11) {
            v.setRotation(angle);
        } else {
            RotateAnimation anim = new RotateAnimation(angle, angle,
                    Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
            anim.setDuration(0);
            anim.setFillAfter(true);
            v.startAnimation(anim);
        }
    }

    /**
     * A class, that can be used as a TouchListener on any view (e.g. a Button).
     * It cyclically runs a clickListener, emulating keyboard-like behaviour. First
     * click is fired immediately, next after initialInterval, and subsequent after
     * normalInterval.
     *
     * <p>Interval is scheduled after the onClick completes, so it has to run fast.
     * If it runs slow, it does not generate skipped onClicks.
     */
    public static class RepeatListener implements View.OnTouchListener {
        private Handler handler = new Handler();

        private int initialInterval;
        private final int normalInterval;
        private int mFastInterval, mFastThreshold, mFastCount;
        private final View.OnClickListener clickListener;
        private boolean singleClick;

        private Runnable handlerRunnable = new Runnable() {
            @Override
            public void run() {
                singleClick = false;
                mFastCount += normalInterval;
                if ((mFastCount >= mFastThreshold) && (mFastThreshold != 0)) {
                    handler.postDelayed(this, mFastInterval);
                    mFastCount = mFastThreshold;
                    LLog.d(TAG, "fast repeat: " + mFastInterval);
                } else {
                    handler.postDelayed(this, normalInterval);
                    LLog.d(TAG, "normal repeat: " + normalInterval);
                }
                clickListener.onClick(downView);
            }
        };

        private View downView = null;

        public void setFastInterval (int fastThreshold, int fastInterval) {
            mFastThreshold = fastThreshold;
            mFastInterval = fastInterval;
        }

        /**
         * @param initialInterval The interval after first click event
         * @param normalInterval The interval after second and subsequent click
         *       events
         * @param clickListener The OnClickListener, that will be called
         *       periodically
         */
        public RepeatListener(int initialInterval, int normalInterval,
                              View.OnClickListener clickListener) {
            if (clickListener == null)
                throw new IllegalArgumentException("null runnable");
            if (initialInterval < 0 || normalInterval < 0)
                throw new IllegalArgumentException("negative interval");

            this.initialInterval = initialInterval;
            this.normalInterval = normalInterval;
            this.clickListener = clickListener;
            mFastInterval = 0;
            mFastThreshold = 0;
        }

        public boolean onTouch(View view, MotionEvent motionEvent) {
            switch (motionEvent.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    singleClick = true;
                    handler.removeCallbacks(handlerRunnable);
                    handler.postDelayed(handlerRunnable, initialInterval);
                    downView = view;
                    downView.setSelected(true);
                    mFastCount = 0;
                    break;
                case MotionEvent.ACTION_CANCEL :
                case MotionEvent.ACTION_UP:
                    handler.removeCallbacks(handlerRunnable);
                    if (singleClick) clickListener.onClick(view);
                    if (downView != null) {
                        downView.setSelected(false);
                        downView = null;
                    }
                    break;
            }
            return true;
        }
    }
}
