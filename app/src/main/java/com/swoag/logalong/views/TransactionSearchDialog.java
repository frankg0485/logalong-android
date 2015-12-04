package com.swoag.logalong.views;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.TextView;

import com.swoag.logalong.R;
import com.swoag.logalong.utils.DBAccount;
import com.swoag.logalong.utils.DBCategory;
import com.swoag.logalong.utils.DBHelper;
import com.swoag.logalong.utils.DBVendor;
import com.swoag.logalong.utils.LOnClickListener;
import com.swoag.logalong.utils.LViewUtils;

import java.util.HashSet;

public class TransactionSearchDialog extends Dialog implements
        DialogInterface.OnDismissListener, LMultiSelectionDialog.MultiSelectionDialogItf {

    private Object context;
    private View filterView;
    private TransactionSearchDialogItf callback;
    private MyClickListener myClickListener;
    private CheckBox checkBox;
    private boolean showAll;

    public interface TransactionSearchDialogItf {
        public void onTransactionSearchDialogDismiss();
    }

    public TransactionSearchDialog(Context parent, TransactionSearchDialogItf callback) {
        super(parent, android.R.style.Theme_Translucent_NoTitleBar);
        this.context = parent;
        this.callback = callback;
        myClickListener = new MyClickListener();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        callback.onTransactionSearchDialogDismiss();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.transaction_search_dialog);

        findViewById(R.id.closeDialog).setOnClickListener(myClickListener);
        findViewById(R.id.accounts).setOnClickListener(myClickListener);
        findViewById(R.id.categories).setOnClickListener(myClickListener);
        findViewById(R.id.vendors).setOnClickListener(myClickListener);
        findViewById(R.id.tags).setOnClickListener(myClickListener);

        filterView = findViewById(R.id.customFilter);

        findViewById(R.id.checkboxView).setOnClickListener(myClickListener);
        checkBox = (CheckBox) findViewById(R.id.checkboxAll);
        showAll = true;

        displayUpdate(showAll);

        this.setOnDismissListener(this);
    }

    private void displayUpdate(boolean all) {
        checkBox.setChecked(all);
        if (all) {
            LViewUtils.disableEnableControls(false, (ViewGroup) filterView);
            LViewUtils.setAlpha(filterView, 0.75f);
        } else {
            LViewUtils.disableEnableControls(true, (ViewGroup) filterView);
            LViewUtils.setAlpha(filterView, 1.0f);
        }
    }

    @Override
    public Cursor onMultiSelectionGetCursor(String column) {
        return DBAccount.getCursorSortedBy(DBHelper.TABLE_COLUMN_NAME);
    }

    @Override
    public void onMultiSelectionDialogExit(Object obj, HashSet<Long> selections) {

    }

    private class MyClickListener extends LOnClickListener {
        @Override
        public void onClicked(View v) {
            switch (v.getId()) {
                case R.id.checkboxView:
                    showAll = !showAll;
                    displayUpdate(showAll);
                    return;

                case R.id.accounts:
                    int[] ids = new int[]{
                            R.layout.multi_selection_dialog,
                            R.layout.multi_selection_item,
                            R.id.title,
                            R.id.save,
                            R.id.cancel,
                            R.id.selectall,
                            R.id.checkBox1,
                            R.id.name,
                            R.id.list,
                            R.string.select_accounts};
                    String[] columns = new String[]{
                            DBHelper.TABLE_COLUMN_NAME
                    };

                    HashSet<Long> selectedIds = new HashSet<Long>();
                    //selectedIds.add(0L);

                    LMultiSelectionDialog dialog = new LMultiSelectionDialog
                            (getContext(), context, selectedIds, TransactionSearchDialog.this, ids, columns);
                    dialog.show();
                    return;

                case R.id.closeDialog:
                default:
                    break;
            }
            dismiss();
        }
    }
}
