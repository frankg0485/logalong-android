package com.swoag.logalong;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import android.app.Activity;
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

import com.swoag.logalong.R;
import com.swoag.logalong.entities.LJournal;
import com.swoag.logalong.entities.LScheduledTransaction;
import com.swoag.logalong.entities.LTransaction;
import com.swoag.logalong.fragments.ScheduledTransactionEdit;
import com.swoag.logalong.fragments.TransactionEdit;
import com.swoag.logalong.utils.DBAccess;
import com.swoag.logalong.utils.DBCategory;
import com.swoag.logalong.utils.DBHelper;
import com.swoag.logalong.utils.DBScheduledTransaction;
import com.swoag.logalong.utils.DBTag;
import com.swoag.logalong.utils.DBVendor;
import com.swoag.logalong.utils.LOnClickListener;
import com.swoag.logalong.utils.LViewUtils;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class ScheduleActivity extends Activity implements
        ScheduledTransactionEdit.ScheduledTransitionEditItf {

    private View rootView, selectTypeV;
    private ViewFlipper viewFlipper;

    private ScheduledTransactionEdit edit;
    private LScheduledTransaction scheduledItem;
    private ListView listView;
    private MyCursorAdapter adapter;
    private boolean createNew;
    private MyClickListener myClickListener;

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
                    calendar.setTimeInMillis(scheduledItem.getItem().getTimeStamp());
                    calendar.set(Calendar.HOUR_OF_DAY, 0);
                    calendar.set(Calendar.MINUTE, 0);
                    calendar.set(Calendar.SECOND, 0);
                    scheduledItem.getItem().setTimeStamp(calendar.getTimeInMillis());
                    scheduledItem.initNextTimeMs();

                    scheduledItem.getItem().setTimeStampLast(System.currentTimeMillis());
                    if (createNew) DBScheduledTransaction.add(scheduledItem);
                    else DBScheduledTransaction.update(scheduledItem);

                    scheduledItem.setAlarm();

                    LJournal journal = new LJournal();
                    journal.updateScheduledItem(scheduledItem);
                }
                break;

            case TransactionEdit.TransitionEditItf.EXIT_CANCEL:
                return;

            case TransactionEdit.TransitionEditItf.EXIT_DELETE:
                scheduledItem.cancelAlarm();
                DBScheduledTransaction.deleteById(scheduledItem.getItem().getId());
                //TODO: journal support
                break;
        }

        Cursor cursor = DBScheduledTransaction.getCursor(DBHelper.TABLE_COLUMN_SCHEDULE_TIMESTAMP);
        Cursor oldCursor = adapter.swapCursor(cursor);
        adapter.notifyDataSetChanged();
        oldCursor.close();
    }

    private void openSchedule(LScheduledTransaction item, boolean create) {
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
                scheduledItem.getItem().setType(LTransaction.TRANSACTION_TYPE_EXPENSE);
                break;
            case R.id.income:
                scheduledItem.getItem().setType(LTransaction.TRANSACTION_TYPE_INCOME);
                break;
            case R.id.transaction:
                scheduledItem.getItem().setType(LTransaction.TRANSACTION_TYPE_TRANSFER);
                break;
        }

        openSchedule(scheduledItem, true);
    }

    private void initListView() {
        Cursor cursor = DBScheduledTransaction.getCursor(DBHelper.TABLE_COLUMN_SCHEDULE_TIMESTAMP);
        if (null == cursor) return;

        adapter = new MyCursorAdapter(this, cursor);
        listView.setAdapter(adapter);
        listView.post(new Runnable() {
            @Override
            public void run() {
                listView.setSelection(adapter.getCount() - 1);
            }
        });
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

            String category = DBCategory.getNameById(categoryId);
            String tag = DBTag.getNameById(tagId);

            String str = "";
            if (!tag.isEmpty()) str = tag + ":";
            str += category;
            tv.setText(str);

            tv = (TextView) mainView.findViewById(R.id.note);
            int vendorId = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_VENDOR));
            String vendor = DBVendor.getNameById(vendorId);
            String note = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_NOTE));

            if (vendor.isEmpty()) {
                if ((note != null) && (!note.isEmpty())) vendor = note;
            } else {
                if ((note != null) && (!note.isEmpty())) vendor += " - " + note;
            }
            tv.setText(vendor);

            tv = (TextView) mainView.findViewById(R.id.date);
            long tm = cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_SCHEDULE_TIMESTAMP));
            tv.setText(new SimpleDateFormat("MMM d, yyy").format(tm));

            tv = (TextView) mainView.findViewById(R.id.dollor);
            int type = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_TYPE));
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
                scheduledItem = DBScheduledTransaction.getById(tag.id);
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
}
