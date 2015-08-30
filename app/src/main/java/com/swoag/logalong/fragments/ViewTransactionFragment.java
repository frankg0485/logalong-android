package com.swoag.logalong.fragments;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.swoag.logalong.LFragment;
import com.swoag.logalong.MainActivity;
import com.swoag.logalong.R;
import com.swoag.logalong.entities.LAccountSummary;
import com.swoag.logalong.entities.LItem;
import com.swoag.logalong.utils.AppPersistency;
import com.swoag.logalong.utils.DBAccess;
import com.swoag.logalong.utils.DBHelper;

import java.text.SimpleDateFormat;

public class ViewTransactionFragment extends LFragment implements View.OnClickListener {
    private static final String TAG = ViewTransactionFragment.class.getSimpleName();

    private ListView listView;
    private Cursor logsCursor;
    private MyCursorAdapter adapter;
    private ViewFlipper viewFlipper, listViewFlipper;
    private TextView balanceTV, deltaTV;
    private double balance, delta;
    private View rootView;

    private TransactionEdit edit;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_view_transaction, container, false);

        viewFlipper = (ViewFlipper) rootView.findViewById(R.id.viewFlipper);
        viewFlipper.setAnimateFirstView(false);
        viewFlipper.setDisplayedChild(0);

        listViewFlipper = (ViewFlipper) rootView.findViewById(R.id.listViewFlipper);
        listViewFlipper.setAnimateFirstView(false);
        listViewFlipper.setDisplayedChild(1);

        logsCursor = DBAccess.getAllActiveItemsCursor();
        adapter = new MyCursorAdapter(getActivity(), logsCursor);
        listView = (ListView) rootView.findViewById(R.id.logsList);
        listView.setAdapter(adapter);
        listView.post(new Runnable() {
            @Override
            public void run() {
                listView.setSelection(adapter.getCount() - 1);
            }
        });

        balanceTV = (TextView) rootView.findViewById(R.id.balance);
        deltaTV = (TextView) rootView.findViewById(R.id.delta);
        showBalance();
        return rootView;
    }

    @Override
    public void onDestroyView() {
        viewFlipper.setInAnimation(null);
        viewFlipper.setOutAnimation(null);
        viewFlipper = null;

        listViewFlipper.setInAnimation(null);
        listViewFlipper.setOutAnimation(null);
        listViewFlipper = null;

        balanceTV = null;
        deltaTV = null;

        rootView = null;
        super.onDestroyView();
    }

    @Override
    public void onSelected(boolean selected) {
        //LLog.d(TAG, "onSelected" + selected);
        if (selected && AppPersistency.transactionChanged) {
            AppPersistency.transactionChanged = false;
            logsCursor = DBAccess.getAllActiveItemsCursor();
            adapter.swapCursor(logsCursor);
            adapter.notifyDataSetChanged();
            listView.post(new Runnable() {
                @Override
                public void run() {
                    listView.setSelection(adapter.getCount() - 1);
                }
            });
            showBalance();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            default:
                break;
        }
    }

    private class MyCursorAdapter extends CursorAdapter implements View.OnClickListener,
            TransactionEdit.TransitionEditItf {
        private LItem item;


        public MyCursorAdapter(Context context, Cursor cursor) {
            //TODO: deprecated API is used here for max OS compatibility, provide alternative
            //      using LoaderManager with a CursorLoader.
            //super(context, cursor, 0);
            super(context, cursor, false);
        }

        /**
         * Bind an existing view to the data pointed to by cursor
         */
        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            TextView tv = (TextView) view.findViewById(R.id.category);
            int categoryId = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.TABLE_LOG_COLUMN_CATEGORY));
            int tagId = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.TABLE_LOG_COLUMN_TAG));

            String category = DBAccess.getCategoryById(categoryId);
            String tag = DBAccess.getTagById(tagId);

            String str = "";
            if (!tag.isEmpty()) str = tag + ":";
            str += category;
            tv.setText(str);

            tv = (TextView) view.findViewById(R.id.note);
            int vendorId = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.TABLE_LOG_COLUMN_VENDOR));
            String vendor = DBAccess.getVendorById(vendorId);
            String note = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.TABLE_LOG_COLUMN_NOTE));

            if ((!note.isEmpty()) && (!vendor.isEmpty())) vendor += " - " + note;
            else if (!note.isEmpty()) vendor = note;
            tv.setText(vendor);

            tv = (TextView) view.findViewById(R.id.date);
            long tm = cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.TABLE_LOG_COLUMN_TIMESTAMP));
            tv.setText(new SimpleDateFormat("MMM d, yyy").format(tm));

            tv = (TextView) view.findViewById(R.id.dollor);
            int type = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.TABLE_LOG_COLUMN_TYPE));
            Resources rsc = getActivity().getResources();
            switch (type) {
                case LItem.LOG_TYPE_EXPENSE:
                    tv.setTextColor(rsc.getColor(R.color.base_red));
                    break;
                case LItem.LOG_TYPE_INCOME:
                    tv.setTextColor(rsc.getColor(R.color.base_green));
                    break;
                case LItem.LOG_TYPE_TRNASACTION:
                    tv.setTextColor(rsc.getColor(R.color.base_blue));
                    break;
            }
            double dollar = cursor.getDouble(cursor.getColumnIndexOrThrow(DBHelper.TABLE_LOG_COLUMN_VALUE));
            tv.setText(String.format("%.2f", dollar));

            view.setOnClickListener(this);
            view.setTag(new VTag(cursor.getLong(0)));
        }

        /**
         * Makes a new view to hold the data pointed to by cursor.
         */
        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View newView = LayoutInflater.from(context).inflate(R.layout.transaction_item, parent, false);
            return newView;
        }

        @Override
        public void onClick(View v) {
            VTag tag = (VTag) v.getTag();

            item = DBAccess.getLogItemById(tag.id);

            edit = new TransactionEdit(getActivity(), rootView, item, false, this);

            viewFlipper.setInAnimation(getActivity(), R.anim.slide_in_right);
            viewFlipper.setOutAnimation(getActivity(), R.anim.slide_out_left);
            viewFlipper.showNext();

            MainActivity actv = (MainActivity) getActivity();
            actv.disablePager();
        }

        @Override
        public void onTransactionEditExit(int action, boolean changed) {
            switch (action) {
                case TransactionEdit.TransitionEditItf.EXIT_OK:
                    AppPersistency.transactionChanged = changed;
                    if (changed) {
                        DBAccess.updateItem(item);
                        onSelected(true);
                    }
                    break;
                case TransactionEdit.TransitionEditItf.EXIT_CANCEL:
                    break;
                case TransactionEdit.TransitionEditItf.EXIT_DELETE:
                    AppPersistency.transactionChanged = true;
                    DBAccess.deleteItemById(item.getId());
                    onSelected(true);
                    break;
            }
            viewFlipper.setInAnimation(getActivity(), R.anim.slide_in_left);
            viewFlipper.setOutAnimation(getActivity(), R.anim.slide_out_right);
            viewFlipper.showPrevious();

            MainActivity actv = (MainActivity) getActivity();
            actv.enablePager();
        }

        private class VTag {
            long id;

            public VTag(long id) {
                this.id = id;
            }
        }
    }

    @Override
    public boolean onBackPressed() {
        if (viewFlipper.getDisplayedChild() == 1) {
            edit.dismiss();
            return true;
        }
        return false;
    }

    private void showBalance() {
        getBalance();
        if (balance < 0) {
            balanceTV.setTextColor(getActivity().getResources().getColor(R.color.base_red));
        } else {
            balanceTV.setTextColor(getActivity().getResources().getColor(R.color.base_green));
        }
        balanceTV.setText(String.format("%.2f", balance));

        if (delta < 0) {
            deltaTV.setTextColor(getActivity().getResources().getColor(R.color.base_red));
        } else {
            deltaTV.setTextColor(getActivity().getResources().getColor(R.color.base_green));
        }
        deltaTV.setText(String.format("%.2f", delta));
    }

    private void getBalance() {
        LAccountSummary summary = new LAccountSummary();
        DBAccess.getSummaryForAll(summary);
        balance = summary.getBalance();
        delta = summary.getIncome() - summary.getExpense();
    }
}


