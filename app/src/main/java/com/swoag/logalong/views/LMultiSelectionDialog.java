package com.swoag.logalong.views;
/* Copyright (C) 2015 - 2018 SWOAG Technology <www.swoag.com> */

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.widget.CursorAdapter;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AlphabetIndexer;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SectionIndexer;
import android.widget.TextView;

import com.swoag.logalong.utils.LLog;
import com.swoag.logalong.utils.LOnClickListener;

import java.util.ArrayList;
import java.util.HashSet;

public class LMultiSelectionDialog extends Dialog
        implements AdapterView.OnItemClickListener {
    private static final String TAG = LMultiSelectionDialog.class.getSimpleName();
    private Cursor mCursor;
    private int idColumnIndex;

    public Context context;
    private ListView mList;
    private MyClickListener myClickListener;
    private boolean allSelected;
    private CheckBox allSelectionCB;

	/* 0: layoutId;
     * 1: itemLayoutId;
	 * 2: titleId;
	 * 3: saveBtnId;
	 * 4: cancelBtnId;
	 * 5: selectAllCheckBoxId;
	 * 6: selectCheckBoxId;
	 * 7: mainEntryTextId;
	 * 8: listViewId;
	 * 9: titleStringId
	 */

    private int[] ids;
    private LMultiSelectionDialog.MultiSelectionDialogItf callback;

    /* 0: mainEntryColumn;
     * 1: subEntryColumn;
     */
    private String[] columns;
    private HashSet<Long> selectedIds;
    private Object obj;
    private ArrayList<String> strings;

    private void init(Context context, Object obj, HashSet<Long> selectedIds,
                      LMultiSelectionDialog.MultiSelectionDialogItf callback,
                      int[] ids, String[] columns) {
        this.context = context;
        this.callback = callback;
        this.obj = obj;
        this.selectedIds = selectedIds;

        this.mCursor = callback.onMultiSelectionGetCursor("");
        this.strings = callback.onMultiSelectionGetStrings();
        if (this.mCursor != null) {
            this.idColumnIndex = this.mCursor.getColumnIndex("_id");
        }

        this.ids = ids;
        this.columns = columns;
    }

    public interface MultiSelectionDialogItf {
        public Cursor onMultiSelectionGetCursor(String column);

        public ArrayList<String> onMultiSelectionGetStrings();

        public void onMultiSelectionDialogExit(Object obj, HashSet<Long> selections, boolean allSelected);
    }

    public LMultiSelectionDialog(Context context, Object obj, HashSet<Long> selectedIds,
                                 LMultiSelectionDialog.MultiSelectionDialogItf callback,
                                 int[] ids, String[] columns) {
        super(context, android.R.style.Theme_Translucent_NoTitleBar);
        myClickListener = new MyClickListener();
        init(context, obj, selectedIds, callback, ids, columns);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(ids[0]);

        ((LinearLayout) findViewById(ids[5])).setOnClickListener(myClickListener);
        ((TextView) findViewById(ids[3])).setOnClickListener(myClickListener);
        ((TextView) findViewById(ids[4])).setOnClickListener(myClickListener);
        ((TextView) findViewById(ids[2])).setText(context.getString(ids[9]));

        try {
            mList = (ListView) findViewById(ids[8]);
            mList.setOnItemClickListener(this);

            mList.setFastScrollEnabled(true);

            if (mCursor != null) {
                mList.setAdapter(new MyCursorAdapter(context.getApplicationContext(),
                        mCursor, ids[1], columns[0], null/*columns[1]*/,
                        ids[7], ids[6]));
            } else {
                mList.setAdapter(new MyArrayAdapter(context.getApplicationContext(), strings, ids[1], ids[7], ids[6]));
            }
        } catch (Exception e) {
            LLog.e(TAG, "unexpected error: " + e.getMessage());
        }

        allSelected = isAllSelected();
        allSelectionCB = (CheckBox) findViewById(ids[6]);
        allSelectionCB.setChecked(allSelected);
    }

    private boolean isAllSelected() {
        if (selectedIds.isEmpty()) return false;
        if (strings != null) {
            return (selectedIds.size() == strings.size());
        } else {
            if (null == mCursor || mCursor.getCount() < 1) return false;
            return (selectedIds.size() == mCursor.getCount());
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            //bitSet.clear();
            leave();
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent ev) {
        ((Activity) context).onUserInteraction();

        try {
            if (Build.VERSION.SDK_INT >= 12) {
                return super.dispatchGenericMotionEvent(ev);
            }
        } catch (Exception e) {
        }
        return false;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        ((Activity) context).onUserInteraction();
        return super.dispatchKeyEvent(event);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        ((Activity) context).onUserInteraction();
        return super.dispatchTouchEvent(ev);
    }

    private class MyClickListener extends LOnClickListener {
        @Override
        public void onClicked(View v) {
            int id = v.getId();

            if (id == ids[5]) {
                try {
                    CheckBox cb = (CheckBox) v.findViewById(ids[6]);
                    cb.setChecked(!cb.isChecked());
                    allSelected = cb.isChecked();

                    if (mCursor != null) {
                        mCursor.moveToFirst();
                        for (int ii = 0; ii < mCursor.getCount(); ii++) {

                            if (cb.isChecked()) {
                                selectedIds.add(mCursor.getLong(idColumnIndex));
                            } else {
                                selectedIds.remove(mCursor.getLong(idColumnIndex));
                            }
                            mCursor.moveToNext();
                        }
                    } else {
                        for (int ii = 0; ii < strings.size(); ii++) {
                            if (cb.isChecked()) {
                                selectedIds.add((long) ii);
                            } else {
                                selectedIds.remove((long) ii);
                            }
                        }
                    }
                    mList.invalidateViews();

                } catch (Exception e) {
                    LLog.e(TAG, "unexpected error: " + e.getMessage());
                }
            } else if (id == ids[3]) {
                leave();
            } else if (id == ids[4]) {
                selectedIds.clear();
                allSelected = false;
                leave();
            }
        }
    }

    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
        long id;

        if (mCursor != null) {
            this.mCursor.moveToPosition(arg2);
            id = this.mCursor.getLong(idColumnIndex);
        } else {
            id = arg2;
        }

        boolean checked = !selectedIds.contains(id);
        if (checked) {
            selectedIds.add(id);
        } else {
            selectedIds.remove(id);
        }

        CheckBox cb = (CheckBox) arg1.findViewById(ids[6]);
        cb.setChecked(checked);
        arg1.findViewById(ids[7]).setSelected(checked);

        allSelected = isAllSelected();
        allSelectionCB.setChecked(allSelected);
    }

    private void leave() {
        callback.onMultiSelectionDialogExit(obj, selectedIds, allSelected);
        dismiss();
    }

    private class MyArrayAdapter extends ArrayAdapter<String> {
        int layoutId;
        private int textId1;
        private int checkboxId;
        private ArrayList<String> arrayList;

        public MyArrayAdapter(Context context, ArrayList<String> arrayList, int layoutId, int textId1, int checkboxId) {
            super(context, 0, arrayList);
            this.layoutId = layoutId;
            this.textId1 = textId1;
            this.checkboxId = checkboxId;
            this.arrayList = arrayList;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            String item = getItem(position);

            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(layoutId, parent, false);
            }

            TextView txtView = (TextView) convertView.findViewById(textId1);
            txtView.setText(item);

            CheckBox cb = (CheckBox) convertView.findViewById(checkboxId);
            cb.setChecked(selectedIds.contains((long) position));
            return convertView;
        }
    }

    /**
     * Adapter that exposes data from a Cursor to a ListView widget.
     */
    private class MyCursorAdapter extends CursorAdapter implements SectionIndexer {
        private AlphabetIndexer mAlphabetIndexer;
        private int layoutId;
        private String column1;
        private String column2;
        private String column3;
        private int textId1;
        private int checkboxId;

        private void init(Context context, Cursor cursor,
                          int layoutId, String column1, String column2,
                          int textId1, int checkboxId) {
            this.layoutId = layoutId;
            this.column1 = column1;
            this.column2 = column2;
            this.textId1 = textId1;
            this.checkboxId = checkboxId;
            mAlphabetIndexer = new AlphabetIndexer(cursor, cursor.getColumnIndex(column1),
                    " ABCDEFGHIJKLMNOPQRTSUVWXYZ");
            mAlphabetIndexer.setCursor(cursor);
        }

        public MyCursorAdapter(Context context, Cursor cursor,
                               int layoutId, String column1, String column2,
                               int textId1, int checkboxId) {
            //TODO: deprecated API is used here for max OS compatibility, provide alternative
            //      using LoaderManager with a CursorLoader.
            //super(context, cursor, 0);
            super(context, cursor, false);

            init(context, cursor, layoutId, column1, column2, textId1, checkboxId);
        }

        /**
         * Performs a binary search or cache lookup
         * to find the first row that matches a given section's starting letter.
         */
        @Override
        public int getPositionForSection(int sectionIndex) {
            return mAlphabetIndexer.getPositionForSection(sectionIndex);
        }

        /**
         * Returns the section index for a given position in the list
         * by querying the item and comparing it with all items in the section array.
         */
        @Override
        public int getSectionForPosition(int position) {
            return mAlphabetIndexer.getSectionForPosition(position);
        }

        /**
         * Returns the section array constructed from the alphabet provided in the constructor.
         */
        @Override
        public Object[] getSections() {
            return mAlphabetIndexer.getSections();
        }

        /**
         * Bind an existing view to the data pointed to by cursor
         */
        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            TextView txtView = (TextView) view.findViewById(textId1);
            txtView.setText(cursor.getString(cursor.getColumnIndex(column1)));

            CheckBox cb = (CheckBox) view.findViewById(checkboxId);
            cb.setChecked(selectedIds.contains(cursor.getLong(idColumnIndex)));
        }

        /**
         * Makes a new view to hold the data pointed to by cursor.
         */
        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            LayoutInflater inflater = LayoutInflater.from(context);
            View newView = inflater.inflate(layoutId, parent, false);
            return newView;
        }
    }
}
