package com.swoag.logalong.fragments;
/* Copyright (C) 2015 - 2017 SWOAG Technology <www.swoag.com> */


import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.swoag.logalong.LFragment;
import com.swoag.logalong.R;
import com.swoag.logalong.ScheduleActivity;
import com.swoag.logalong.entities.LAllBalances;
import com.swoag.logalong.entities.LTransaction;
import com.swoag.logalong.utils.DBHelper;
import com.swoag.logalong.utils.DBLoaderHelper;
import com.swoag.logalong.utils.LOnClickListener;
import com.swoag.logalong.utils.LPreferences;
import com.swoag.logalong.views.GenericListOptionDialog;
import com.swoag.logalong.views.LReminderDialog;

import java.util.Calendar;

public class NewTransactionFragment extends LFragment implements TransactionEdit.TransitionEditItf,
        DBLoaderHelper.DBLoaderHelperCallbacks {
    private static final String TAG = NewTransactionFragment.class.getSimpleName();

    private View btnExpense, btnIncome, btnTransaction, selectTypeV;
    private ViewFlipper viewFlipper;

    private View rootView, entryView;
    private TransactionEdit edit;
    private LTransaction item;
    private ListView listView;
    private TextView balanceTV;
    private MyCursorAdapter adapter;
    private LAllBalances allBalances;
    private MyClickListener myClickListener;

    private DBLoaderHelper dbLoaderHelper;

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        switch (loader.getId()) {
            case DBLoaderHelper.LOADER_ALL_ACCOUNT_BALANCES:
                allBalances = new LAllBalances(data);
                showBalance(balanceTV, 0);
                adapter.notifyDataSetChanged();

                break;
            case DBLoaderHelper.LOADER_ALL_ACCOUNTS:
                adapter.swapCursor(data);
                adapter.notifyDataSetChanged();
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        switch (loader.getId()) {
            case DBLoaderHelper.LOADER_ALL_ACCOUNTS:
                adapter.swapCursor(null);
                adapter.notifyDataSetChanged();
                break;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_new_transaction, container, false);
        entryView = rootView.findViewById(R.id.entryView);

        selectTypeV = entryView.findViewById(R.id.selectType);
        selectTypeV.setVisibility(View.GONE);

        myClickListener = new MyClickListener();

        balanceTV = (TextView) entryView.findViewById(R.id.balance);

        listView = (ListView) entryView.findViewById(R.id.listView);
        adapter = new MyCursorAdapter(getActivity(), null);
        listView.setAdapter(adapter);

        viewFlipper = (ViewFlipper) rootView.findViewById(R.id.viewFlipper);
        viewFlipper.setAnimateFirstView(false);
        viewFlipper.setDisplayedChild(0);

        btnExpense = setViewListener(entryView, R.id.expense);
        btnIncome = setViewListener(entryView, R.id.income);
        btnTransaction = setViewListener(entryView, R.id.transaction);

        bottombarSetup();

        dbLoaderHelper = new DBLoaderHelper(this.getActivity(), this);
        dbLoaderHelper.restart(getLoaderManager(), DBLoaderHelper.LOADER_ALL_ACCOUNT_BALANCES);
        dbLoaderHelper.restart(getLoaderManager(), DBLoaderHelper.LOADER_ALL_ACCOUNTS);

        return rootView;
    }

    @Override
    public void onDestroyView() {
        viewFlipper.setInAnimation(null);
        viewFlipper.setOutAnimation(null);
        viewFlipper = null;

        btnExpense = null;
        btnIncome = null;
        btnTransaction = null;
        selectTypeV = null;
        edit = null;
        rootView = null;
        super.onDestroyView();
    }

    @Override
    public void onSelected(boolean selected) {
        if (edit != null) {
            edit.dismiss();
        }
    }

    private void bottombarSetup() {
        setViewListener(entryView, R.id.exit);
        setViewListener(entryView, R.id.add);

        ImageView iv = (ImageView) entryView.findViewById(R.id.exitImg);
        iv.setImageResource(R.drawable.ic_action_alarms);
        iv.setClickable(false);
    }

    private class MyClickListener extends LOnClickListener {
        @Override
        public void onClicked(View v) {
            switch (v.getId()) {
                case R.id.expense:
                    newLog(LTransaction.TRANSACTION_TYPE_EXPENSE);
                    break;
                case R.id.income:
                    newLog(LTransaction.TRANSACTION_TYPE_INCOME);
                    break;
                case R.id.transaction:
                    newLog(LTransaction.TRANSACTION_TYPE_TRANSFER);
                    break;

                case R.id.exit:
                    if (TextUtils.isEmpty(LPreferences.getUserId())) {
                        new LReminderDialog(getActivity(), getActivity().getResources().getString(R.string
                                .please_complete_your_profile)).show();
                        break;
                    }
                    startActivity(new Intent(getActivity(), ScheduleActivity.class));
                    break;

                case R.id.add:
                    if (selectTypeV.getVisibility() != View.VISIBLE) {
                        selectTypeV.setVisibility(View.VISIBLE);
                    } else {
                        selectTypeV.setVisibility(View.GONE);
                    }
                    return;

                default:
                    break;
            }
            selectTypeV.setVisibility(View.GONE);
        }
    }

    @Override
    public void onTransactionEditExit(int action, boolean changed) {
        viewFlipper.setInAnimation(getActivity(), R.anim.slide_in_left);
        viewFlipper.setOutAnimation(getActivity(), R.anim.slide_out_right);
        viewFlipper.showPrevious();
        edit = null;
    }

    @Override
    public boolean onBackPressed() {
        if (viewFlipper.getDisplayedChild() == 1) {
            edit.dismiss();
            return true;
        }
        return false;
    }

    private void newLog(int type) {
        item = new LTransaction();
        item.setType(type);

        edit = new TransactionEdit(getActivity(), rootView.findViewById(R.id.editView), item, true, false, true, this);

        viewFlipper.setInAnimation(getActivity(), R.anim.slide_in_right);
        viewFlipper.setOutAnimation(getActivity(), R.anim.slide_out_left);
        viewFlipper.showNext();
    }

    private View setViewListener(View v, int id) {
        View view = v.findViewById(id);
        view.setOnClickListener(myClickListener);
        return view;
    }

    private void showBalance(TextView textView, long accountId) {
        if (null == allBalances) return;

        Calendar now = Calendar.getInstance();
        int year = now.get(Calendar.YEAR);
        int month = now.get(Calendar.MONTH);
        double balance;

        balance = (accountId <= 0) ? allBalances.getBalance() : allBalances.getBalance(accountId);
        if (balance < 0) {
            textView.setTextColor(getActivity().getResources().getColor(R.color.base_red));
        } else {
            textView.setTextColor(getActivity().getResources().getColor(R.color.base_green));
        }
        textView.setText(String.format("%.2f", Math.abs(balance)));
    }

    private class MyCursorAdapter extends CursorAdapter {
        GenericListOptionDialog optionDialog;

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
            View mainView = view.findViewById(R.id.mainView);
            if ((!cursor.isNull(cursor.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_SHOW_BALANCE))) &&
                    0 == cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_SHOW_BALANCE))) {
                mainView.setVisibility(View.GONE);
            } else {
                mainView.setVisibility(View.VISIBLE);
                TextView tv = (TextView) view.findViewById(R.id.name);
                String name = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_NAME));
                tv.setText(name);

                Calendar now = Calendar.getInstance();
                int year = now.get(Calendar.YEAR);
                int month = now.get(Calendar.MONTH);

                tv = (TextView) view.findViewById(R.id.dollor);
                showBalance(tv, cursor.getLong(0));
            }
        }

        /**
         * Makes a new view to hold the data pointed to by cursor.
         */
        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View newView = LayoutInflater.from(context).inflate(R.layout.account_summary_list_item, parent, false);
            return newView;
        }
    }
}
