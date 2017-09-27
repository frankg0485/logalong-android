package com.swoag.logalong;
/* Copyright (C) 2015 2017 SWOAG Technology <www.swoag.com> */

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.swoag.logalong.entities.LJournal;
import com.swoag.logalong.entities.LScheduledTransaction;
import com.swoag.logalong.entities.LTransaction;
import com.swoag.logalong.fragments.ScheduledTransactionEdit;
import com.swoag.logalong.fragments.TransactionEdit;
import com.swoag.logalong.utils.DBAccount;
import com.swoag.logalong.utils.DBCategory;
import com.swoag.logalong.utils.DBHelper;
import com.swoag.logalong.utils.DBProvider;
import com.swoag.logalong.utils.DBScheduledTransaction;
import com.swoag.logalong.utils.DBTag;
import com.swoag.logalong.utils.DBVendor;
import com.swoag.logalong.utils.LOnClickListener;
import com.swoag.logalong.utils.LPreferences;
import com.swoag.logalong.utils.LViewUtils;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class ScheduleActivity extends LFragmentActivity implements
        ScheduledTransactionEdit.ScheduledTransitionEditItf,
        LoaderManager.LoaderCallbacks<Cursor> {
    private static final int LOADER_SCHEDULES = 20;
    private View rootView, selectTypeV;
    private ViewFlipper viewFlipper;

    private ScheduledTransactionEdit edit;
    private LScheduledTransaction scheduledItem;
    private ListView listView;
    private MyCursorAdapter adapter;
    private boolean createNew;
    private MyClickListener myClickListener;

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Uri uri;
        switch (id) {
            case LOADER_SCHEDULES:
                uri = DBProvider.URI_SCHEDULED_TRANSACTIONS;
                return new CursorLoader(this,
                        uri,
                        null,
                        DBHelper.TABLE_COLUMN_STATE + "=? OR " + DBHelper.TABLE_COLUMN_STATE + "=?",
                        new String[]{"" + DBHelper.STATE_ACTIVE, "" + DBHelper.STATE_DISABLED},
                        DBHelper.TABLE_COLUMN_SCHEDULE_TIMESTAMP + " ASC");
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        switch (loader.getId()) {
            case LOADER_SCHEDULES:
                adapter.swapCursor(data);
                adapter.notifyDataSetChanged();
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        switch (loader.getId()) {
            case LOADER_SCHEDULES:
                adapter.swapCursor(null);
                adapter.notifyDataSetChanged();
                break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.scheduled_transactions);
        myClickListener = new MyClickListener();

        rootView = findViewById(R.id.scheduleEdit);
        View entryView = findViewById(R.id.entryView);

        selectTypeV = entryView.findViewById(R.id.selectType);
        selectTypeV.setVisibility(View.GONE);

        entryView.findViewById(R.id.exit).setOnClickListener(myClickListener);
        entryView.findViewById(R.id.expense).setOnClickListener(myClickListener);
        entryView.findViewById(R.id.income).setOnClickListener(myClickListener);
        entryView.findViewById(R.id.transaction).setOnClickListener(myClickListener);
        entryView.findViewById(R.id.add).setOnClickListener(myClickListener);

        listView = (ListView) entryView.findViewById(R.id.logsList);
        initListView();

        viewFlipper = (ViewFlipper) findViewById(R.id.viewFlipper);
        viewFlipper.setAnimateFirstView(false);
        viewFlipper.setDisplayedChild(0);

        getSupportLoaderManager().restartLoader(LOADER_SCHEDULES, null, this);
    }

    private class MyClickListener extends LOnClickListener {
        @Override
        public void onClicked(View v) {
            switch (v.getId()) {
                case R.id.exit:
                    finish();
                    break;

                case R.id.add:
                    if (selectTypeV.getVisibility() != View.VISIBLE) {
                        selectTypeV.setVisibility(View.VISIBLE);
                    } else {
                        selectTypeV.setVisibility(View.GONE);
                    }
                    return;

                case R.id.expense:
                case R.id.income:
                case R.id.transaction:
                    addNewSchedule(v.getId());
                    break;
            }
            selectTypeV.setVisibility(View.GONE);
        }
    }

    @Override
    public void onScheduledTransactionEditExit(int action, boolean changed) {
        viewFlipper.setInAnimation(ScheduleActivity.this, R.anim.slide_in_left);
        viewFlipper.setOutAnimation(ScheduleActivity.this, R.anim.slide_out_right);
        viewFlipper.showPrevious();

        switch (action) {
            case TransactionEdit.TransitionEditItf.EXIT_OK:
                if (changed) {
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTimeInMillis(scheduledItem.getTimeStamp());
                    calendar.set(Calendar.HOUR_OF_DAY, 0);
                    calendar.set(Calendar.MINUTE, 0);
                    calendar.set(Calendar.SECOND, 0);
                    scheduledItem.setTimeStamp(calendar.getTimeInMillis());
                    scheduledItem.setState(DBHelper.STATE_ACTIVE);
                    //scheduledItem.initNextTimeMs();

                    scheduledItem.setTimeStampLast(LPreferences.getServerUtc());
                    DBScheduledTransaction dbScheduledTransaction = DBScheduledTransaction.getInstance();
                    if (createNew) dbScheduledTransaction.add(scheduledItem);
                    else dbScheduledTransaction.update(scheduledItem);

                    //scheduledItem.setAlarm();

                    LJournal journal = new LJournal();
                    if (createNew) {
                        journal.addSchedule(scheduledItem.getId());
                    } else {
                        journal.updateSchedule(scheduledItem.getId());
                    }
                }
                break;

            case TransactionEdit.TransitionEditItf.EXIT_CANCEL:
                return;

            case TransactionEdit.TransitionEditItf.EXIT_DELETE:
                //scheduledItem.cancelAlarm();
                DBScheduledTransaction.getInstance().deleteById(scheduledItem.getId());
                LJournal journal = new LJournal();
                journal.deleteSchedule(scheduledItem.getId());
                break;
        }

        Cursor cursor = DBScheduledTransaction.getInstance().getCursor(DBHelper.TABLE_COLUMN_SCHEDULE_TIMESTAMP);
        Cursor oldCursor = adapter.swapCursor(cursor);
        adapter.notifyDataSetChanged();
        oldCursor.close();
    }

    private LScheduledTransaction itemOrig;

    private void openSchedule(LScheduledTransaction item, boolean create) {
        itemOrig = new LScheduledTransaction(item);

        createNew = create;
        edit = new ScheduledTransactionEdit(this, rootView, item, create, this);

        viewFlipper.setInAnimation(this, R.anim.slide_in_right);
        viewFlipper.setOutAnimation(this, R.anim.slide_out_left);
        viewFlipper.showNext();
    }

    private void addNewSchedule(int id) {
        scheduledItem = new LScheduledTransaction();

        switch (id) {
            case R.id.expense:
                scheduledItem.setType(LTransaction.TRANSACTION_TYPE_EXPENSE);
                break;
            case R.id.income:
                scheduledItem.setType(LTransaction.TRANSACTION_TYPE_INCOME);
                break;
            case R.id.transaction:
                scheduledItem.setType(LTransaction.TRANSACTION_TYPE_TRANSFER);
                break;
        }

        openSchedule(scheduledItem, true);
    }

    private void initListView() {
        adapter = new MyCursorAdapter(this, null);
        listView.setAdapter(adapter);
    }

    private class MyCursorAdapter extends CursorAdapter {
        private LTransaction item;
        private ClickListener clickListener;

        public MyCursorAdapter(Context context, Cursor cursor) {
            //TODO: deprecated API is used here for max OS compatibility, provide alternative
            //      using LoaderManager with a CursorLoader.
            //super(context, cursor, 0);
            super(context, cursor, false);
            clickListener = new ClickListener();
        }

        /**
         * Bind an existing view to the data pointed to by cursor
         */
        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            View mainView = view.findViewById(R.id.mainView);
            View sectionView = view.findViewById(R.id.sectionView);

            TextView tv = (TextView) mainView.findViewById(R.id.category);
            int categoryId = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_CATEGORY));
            int tagId = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_TAG));
            int state = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_STATE));
            int type = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_TYPE));

            String category = DBCategory.getInstance().getNameById(categoryId);
            String tag = DBTag.getInstance().getNameById(tagId);

            String str = "";
            if (type == LTransaction.TRANSACTION_TYPE_TRANSFER) {
                int accountId = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_ACCOUNT));
                int account2Id = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_ACCOUNT2));
                String account = DBAccount.getInstance().getNameById(accountId);
                String account2 = DBAccount.getInstance().getNameById(account2Id);
                str = account + " --> " + account2;
            } else {
                if (!TextUtils.isEmpty(tag)) str = tag + ":";
                str += category;
            }
            tv.setText(str);

            tv = (TextView) mainView.findViewById(R.id.note);
            int vendorId = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_VENDOR));
            String vendor = DBVendor.getInstance().getNameById(vendorId);
            String note = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_NOTE));

            if (TextUtils.isEmpty(vendor)) {
                if ((note != null) && (!TextUtils.isEmpty(note))) vendor = note;
            } else {
                if ((note != null) && (!TextUtils.isEmpty(note))) vendor += " - " + note;
            }
            tv.setText(vendor);

            tv = (TextView) mainView.findViewById(R.id.date);
            long tm = cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_SCHEDULE_TIMESTAMP));
            tv.setText(new SimpleDateFormat("MMM d, yyy").format(tm));

            tv = (TextView) mainView.findViewById(R.id.dollor);

            Resources rsc = getResources();
            switch (type) {
                case LTransaction.TRANSACTION_TYPE_EXPENSE:
                    tv.setTextColor(rsc.getColor(R.color.base_red));
                    break;
                case LTransaction.TRANSACTION_TYPE_INCOME:
                    tv.setTextColor(rsc.getColor(R.color.base_green));
                    break;
                case LTransaction.TRANSACTION_TYPE_TRANSFER:
                    tv.setTextColor(rsc.getColor(R.color.base_blue));
                    break;
            }
            double dollar = cursor.getDouble(cursor.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_AMOUNT));
            tv.setText(String.format("%.2f", dollar));

            mainView.setOnClickListener(clickListener);
            long id = cursor.getLong(0);
            mainView.setTag(new VTag(id));

            if (state == DBHelper.STATE_DISABLED) {
                LViewUtils.setAlpha(mainView, 0.4f);
            } else {
                LViewUtils.setAlpha(mainView, 1.0f);
            }
            sectionView.setVisibility(View.GONE);
        }

        /**
         * Makes a new view to hold the data pointed to by cursor.
         */
        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View newView = LayoutInflater.from(context).inflate(R.layout.transaction_item, parent, false);
            return newView;
        }

        private class ClickListener extends LOnClickListener {
            @Override
            public void onClicked(View v) {
                VTag tag = (VTag) v.getTag();
                scheduledItem = DBScheduledTransaction.getInstance().getById(tag.id);
                openSchedule(scheduledItem, false);
            }
        }

        private class VTag {
            long id;

            public VTag(long id) {
                this.id = id;
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (viewFlipper.getDisplayedChild() == 0) {
            super.onBackPressed();
        } else {
            edit.dismiss();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        MainService.start(this);
    }

    @Override
    protected void onPause() {
        MainService.stop(this);
        super.onPause();
    }
}
