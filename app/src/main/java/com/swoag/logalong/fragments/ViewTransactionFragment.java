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

    private ListView listView, altListView;
    private TextView balanceTV, incomeTV, expenseTV, altBalanceTV, altIncomeTV, altExpenseTV;
    private Cursor altLogsCursor, logsCursor;
    private MyCursorAdapter adapter, altAdapter;
    private LAccountSummary summary, altSummary;

    private ViewFlipper viewFlipper, listViewFlipper;
    private View rootView, prevView, nextView, filterView;
    private TransactionEdit edit;

    private void initListView(boolean alt) {
        if (alt) {
            altLogsCursor = DBAccess.getAllActiveItemsCursor();
            altAdapter = new MyCursorAdapter(getActivity(), altLogsCursor);
            altListView.setAdapter(altAdapter);
            altSummary = new LAccountSummary();
        } else {
            logsCursor = DBAccess.getAllActiveItemsCursor();
            adapter = new MyCursorAdapter(getActivity(), logsCursor);
            listView.setAdapter(adapter);
            listView.post(new Runnable() {
                @Override
                public void run() {
                    listView.setSelection(adapter.getCount() - 1);
                }
            });
            summary = new LAccountSummary();
            showBalance();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_view_transaction, container, false);

        prevView = setViewListener(rootView, R.id.prev);
        nextView = setViewListener(rootView, R.id.next);
        filterView = setViewListener(rootView, R.id.filter);

        viewFlipper = (ViewFlipper) rootView.findViewById(R.id.viewFlipper);
        viewFlipper.setAnimateFirstView(false);
        viewFlipper.setDisplayedChild(0);

        listViewFlipper = (ViewFlipper) rootView.findViewById(R.id.listViewFlipper);
        listViewFlipper.setAnimateFirstView(false);
        listViewFlipper.setDisplayedChild(1);


        listView = (ListView) listViewFlipper.findViewById(R.id.logs).findViewById(R.id.logsList);
        balanceTV = (TextView) listViewFlipper.findViewById(R.id.logs).findViewById(R.id.balance);
        expenseTV = (TextView) listViewFlipper.findViewById(R.id.logs).findViewById(R.id.expense);
        incomeTV = (TextView) listViewFlipper.findViewById(R.id.logs).findViewById(R.id.income);

        altListView = (ListView) listViewFlipper.findViewById(R.id.logsAlt).findViewById(R.id.logsList);
        altBalanceTV = (TextView) listViewFlipper.findViewById(R.id.logsAlt).findViewById(R.id.balance);
        altExpenseTV = (TextView) listViewFlipper.findViewById(R.id.logsAlt).findViewById(R.id.expense);
        altIncomeTV = (TextView) listViewFlipper.findViewById(R.id.logsAlt).findViewById(R.id.income);

        initListView(true);
        initListView(false);

        return rootView;
    }

    @Override
    public void onDestroyView() {
        prevView = null;
        nextView = null;
        filterView = null;

        viewFlipper.setInAnimation(null);
        viewFlipper.setOutAnimation(null);
        viewFlipper = null;

        listViewFlipper.setInAnimation(null);
        listViewFlipper.setOutAnimation(null);
        listViewFlipper = null;

        balanceTV = null;
        expenseTV = null;
        incomeTV = null;
        altBalanceTV = null;
        altExpenseTV = null;
        altIncomeTV = null;

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
            case R.id.filter:
                break;
            case R.id.prev:
                showPrevNext(true);
                break;
            case R.id.next:
                showPrevNext(false);
                break;
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
            int categoryId = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_CATEGORY));
            int tagId = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_TAG));

            String category = DBAccess.getCategoryNameById(categoryId);
            String tag = DBAccess.getTagNameById(tagId);

            String str = "";
            if (!tag.isEmpty()) str = tag + ":";
            str += category;
            tv.setText(str);

            tv = (TextView) view.findViewById(R.id.note);
            int vendorId = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_VENDOR));
            String vendor = DBAccess.getVendorNameById(vendorId);
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

        if (summary.getBalance() < 0) {
            balanceTV.setTextColor(getActivity().getResources().getColor(R.color.base_red));
        } else {
            balanceTV.setTextColor(getActivity().getResources().getColor(R.color.base_green));
        }
        balanceTV.setText(String.format("%.2f", summary.getBalance()));

        incomeTV.setText(String.format("%.2f", summary.getIncome()));
        expenseTV.setText(String.format("%.2f", summary.getExpense()));
    }

    private void getBalance() {
        DBAccess.getSummaryForAll(summary);
        DBAccess.getSummaryForAll(altSummary);
    }

    private boolean isAltView = false;
    private ListView lv;

    private void showPrevNext(boolean prev) {
        isAltView = !isAltView;

        if (isAltView) {
            altLogsCursor = DBAccess.getAllActiveItemsCursor();
            altAdapter.swapCursor(altLogsCursor);
            altAdapter.notifyDataSetChanged();
            altListView.post(new Runnable() {
                @Override
                public void run() {
                    altListView.setSelection(altAdapter.getCount() - 1);
                }
            });
        } else {
            logsCursor = DBAccess.getAllActiveItemsCursor();
            adapter.swapCursor(logsCursor);
            adapter.notifyDataSetChanged();
            listView.post(new Runnable() {
                @Override
                public void run() {
                    listView.setSelection(adapter.getCount() - 1);
                }
            });
        }

        if (prev) {
            listViewFlipper.setInAnimation(getActivity(), R.anim.slide_in_left);
            listViewFlipper.setOutAnimation(getActivity(), R.anim.slide_out_right);
            listViewFlipper.showPrevious();
        } else {
            listViewFlipper.setInAnimation(getActivity(), R.anim.slide_in_right);
            listViewFlipper.setOutAnimation(getActivity(), R.anim.slide_out_left);
            listViewFlipper.showNext();
        }
    }

    private View setViewListener(View v, int id) {
        View view = v.findViewById(id);
        view.setOnClickListener(this);
        return view;
    }
}


