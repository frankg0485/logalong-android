package com.swoag.logalong.views;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import com.swoag.logalong.R;

import org.w3c.dom.Text;

import java.util.Dictionary;

public class GenericListOptionDialog extends Dialog implements
        View.OnClickListener {

    private Object context;
    private String title;
    private boolean showCategories;
    private GenericListOptionDialogItf callback;

    public interface GenericListOptionDialogItf {
        public boolean onGenericListOptionDialogExit(final Object context, int viewId);
    }

    public GenericListOptionDialog(Context parent, final Object context, String title, boolean showCategories,
                                   GenericListOptionDialogItf callback) {
        super(parent, android.R.style.Theme_Translucent_NoTitleBar);
        this.context = context;
        this.title = title;
        this.callback = callback;
        this.showCategories = showCategories;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.generic_list_option_dialog);

        TextView tv = (TextView) findViewById(R.id.title);
        tv.setText(title);

        findViewById(R.id.closeDialog).setOnClickListener(this);
        findViewById(R.id.remove).setOnClickListener(this);
        findViewById(R.id.rename).setOnClickListener(this);
        View view = findViewById(R.id.associated_categories);
        if (showCategories) {
            view.setVisibility(View.VISIBLE);
            view.setOnClickListener(this);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.closeDialog:
                break;
            default:
                if (callback.onGenericListOptionDialogExit(context, v.getId())) return;
                break;
        }
        dismiss();
    }
}
