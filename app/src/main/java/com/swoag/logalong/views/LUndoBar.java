package com.swoag.logalong.views;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.text.TextUtils;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.TextView;

import com.swoag.logalong.R;
import com.swoag.logalong.utils.LViewUtils;

public class LUndoBar {
    private static final int mAnimTimeMs = 1000;
    private AlphaAnimation mAnimShow, mAnimHide;

    private View mBarView;
    private TextView mMessageView;
    private Handler mHideHandler = new Handler();

    private UndoListener mUndoListener;
    private Parcelable mUndoToken;

    private CharSequence mUndoMessage;

    private int mTimeOutMs;

    public interface UndoListener {
        void onUndo(Parcelable token);
        void onConfirm(Parcelable token);
    }

    public LUndoBar (View undoBarView, UndoListener undoListener) {
        mAnimShow = new AlphaAnimation(0.0f, 1.0f);
        mAnimShow.setDuration(mAnimTimeMs);
        mAnimHide = new AlphaAnimation(1.0f, 0.0f);
        mAnimHide.setDuration(mAnimTimeMs);
        mAnimHide.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationRepeat(Animation animation) {}
            @Override
            public void onAnimationStart(Animation animation) {}
            @Override
            public void onAnimationEnd(Animation animation) {
                mBarView.setVisibility(View.GONE);
                mUndoMessage = null;
                mUndoToken = null;
            }
        });

        mBarView = undoBarView;
        mUndoListener = undoListener;
        //mBottomBarView = ((View)mBarView.getParent()).findViewById(R.id.bottomBar);

        mMessageView = (TextView) mBarView.findViewById(R.id.msg);
        mBarView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });
        mBarView.findViewById(R.id.accept).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (LViewUtils.isDoubleClick()) return;
                hideUndoBar(true);
                mUndoListener.onConfirm(mUndoToken);
            }
        });
        mBarView.findViewById(R.id.undo).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (LViewUtils.isDoubleClick()) return;
                hideUndoBar(false);
                mUndoListener.onUndo(mUndoToken);
            }
        });

        hideUndoBar(true);
    }

    public boolean isVisible () {
        return mBarView.getVisibility() == View.VISIBLE;
    }

    public void showUndoBar(boolean immediate, CharSequence message, Parcelable undoToken, int timeOutMs) {
        mUndoToken = undoToken;
        mUndoMessage = message;
        mMessageView.setText(mUndoMessage);
        mTimeOutMs = timeOutMs;

        mHideHandler.removeCallbacks(mHideRunnable);
        if (timeOutMs > 0) mHideHandler.postDelayed(mHideRunnable, timeOutMs);

        //if (mBottomBarView != null) mBottomBarView.setVisibility(View.GONE);
        mBarView.setVisibility(View.VISIBLE);
        if (!immediate) {
            mBarView.startAnimation(mAnimShow);
        }
    }

    public void hideUndoBar(boolean immediate) {
        mHideHandler.removeCallbacks(mHideRunnable);
        if (immediate) {
            mBarView.setVisibility(View.GONE);
            //if (mBottomBarView != null) mBottomBarView.setVisibility(View.VISIBLE);
            mUndoMessage = null;
            mUndoToken = null;
        } else {
            mBarView.startAnimation(mAnimHide);
        }
    }

    public void onSaveInstanceState(Bundle outState) {
        outState.putCharSequence("undo_message", mUndoMessage);
        outState.putParcelable("undo_token", mUndoToken);
    }

    public void onRestoreInstanceState(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            mUndoMessage = savedInstanceState.getCharSequence("undo_message");
            mUndoToken = savedInstanceState.getParcelable("undo_token");

            if (mUndoToken != null || !TextUtils.isEmpty(mUndoMessage)) {
                showUndoBar(true, mUndoMessage, mUndoToken, mTimeOutMs);
            }
        }
    }

    private Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hideUndoBar(false);
        }
    };
}
