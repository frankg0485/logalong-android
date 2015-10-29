package com.swoag.logalong.views;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import android.app.Activity;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.widget.CursorAdapter;
import android.text.Editable;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AlphabetIndexer;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.SectionIndexer;
import android.widget.TextView;

import com.swoag.logalong.utils.LLog;
import com.swoag.logalong.utils.LOnClickListener;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.List;

public class LSelectionDialog extends Dialog
        implements AdapterView.OnItemClickListener, android.text.TextWatcher {
    private static final String TAG = LSelectionDialog.class.getSimpleName();
    private Cursor mCursor;

    public Context context;
    private EditText searchText;
    private ListView mList;

	/* 0: layoutId;
     * 1: itemLayoutId;
	 * 2: titleId;
	 * 3: saveBtnId;
	 * 4: cancelBtnId;
	 * 5: selectradioButtonId;
	 * 6: mainEntryTextId;
	 * 7: listViewId;
	 * 8: searchText
	 * 9: titleStringId
	 */

    private int[] ids;
    private LSelectionDialog.OnSelectionDialogItf callback;

    /* 0: mainEntryColumn;
     * 1: subEntryColumn;
     */
    private String table;
    private String column;
    private BitSet bitSet;
    private int startPosition;
    private int dlgId;
    private MyClickListener myClickListener;

    private void init(Context context, LSelectionDialog.OnSelectionDialogItf callback,
                      int[] ids, String table, String column, int startPosition, int dlgId) {
        this.context = context;
        this.callback = callback;

        this.mCursor = callback.onGetCursor(table, column);

        this.ids = ids;
        this.table = table;
        this.column = column;

        this.bitSet = new BitSet();
        this.startPosition = startPosition;

        this.dlgId = dlgId;
        myClickListener = new MyClickListener();
    }

    public interface OnSelectionDialogItf {
        public Cursor onGetCursor(String table, String column);

        public void onSelectionDialogExit(int dlgId, long selectedId);
    }

    public LSelectionDialog(Context context, LSelectionDialog.OnSelectionDialogItf callback,
                            int[] ids, String table, String column, int startPosition, int dlgId) {
        super(context, android.R.style.Theme_Translucent_NoTitleBar);
        init(context, callback, ids, table, column, startPosition, dlgId);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(ids[0]);

        //((LinearLayout) findViewById(ids[5])).setOnClickListener(this);
        ((TextView) findViewById(ids[3])).setOnClickListener(myClickListener);
        ((TextView) findViewById(ids[4])).setOnClickListener(myClickListener);
        ((TextView) findViewById(ids[2])).setText(context.getString(ids[9]));

        bitSet.clear();
        try {
            mList = (ListView) findViewById(ids[7]);
            mList.setOnItemClickListener(this);

            searchText = (EditText) findViewById(ids[8]);
            searchText.addTextChangedListener(this);
            searchText.setCursorVisible(false);

            mList.setFastScrollEnabled(true);
            mList.setAdapter(new MyCursorAdapter(context.getApplicationContext(),
                    mCursor, ids[1], column, ids[6], ids[5]));

            if (startPosition != -1) {
                bitSet.set(startPosition, true);
                mList.setSelection(startPosition);
            }
        } catch (Exception e) {
            LLog.e(TAG, "unexpected error: " + e.getMessage());
        }
    }

    public void setSelection(int position) {
        try {
            mList.setSelection(position);
            bitSet.set(position, true);
            mList.invalidateViews();
        } catch (Exception e) {
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            leave(true);
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

            if (id == ids[3]) {
                leave(true);
            } else if (id == ids[4]) {
                leave(false);
            } else if (id == ids[8]) {
                hideSoftKeyboard();
            }
        }
    }

    private void hideSoftKeyboard() {
        InputMethodManager inputManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        //LLog.d(TAG, "input manager " + inputManager + "@" + searchText);
        inputManager.hideSoftInputFromWindow(searchText.getWindowToken(), 0);
    }

    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
        boolean checked = bitSet.get(arg2);
        bitSet.clear();

        bitSet.set(arg2, !checked);
        RadioButton rb = (RadioButton) arg1.findViewById(ids[5]);
        rb.setChecked(!checked);
        mList.invalidateViews();
    }

    private void leave(boolean save) {
        long selectedId = -1;
        hideSoftKeyboard();
        dismiss();

        int selectedPosition = save ? bitSet.nextSetBit(0) : startPosition;
        if (selectedPosition >= 0) {
            mCursor.moveToPosition(selectedPosition);
            selectedId = mCursor.getLong(0);
        }
        this.callback.onSelectionDialogExit(dlgId, selectedId);
    }

    @Override
    public void afterTextChanged(Editable s) {
        String newStr = searchText.getText().toString().trim();
        mCursor = callback.onGetCursor(table, newStr);

        mList.setAdapter(new MyCursorAdapter(context.getApplicationContext(),
                mCursor, ids[1], column, ids[6], ids[5]));

    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        ////SonrLog.d(TAG, "text change: " + start + " " + after + " " + count);
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        ////SonrLog.d(TAG, "text change: " + start + " " + before + " " + count);
    }

    /**
     * Adapter that exposes data from a Cursor to a ListView widget.
     */
    private class MyCursorAdapter extends CursorAdapter implements SectionIndexer {
        private AlphabetIndexer mAlphabetIndexer;
        private int layoutId;
        private String column1;
        private int textId1;

        private int radioButtonId;
        private int photoId;
        private int defaultImage;

        private void init(Context context, Cursor cursor,
                          int layoutId, String column1,
                          int textId1, int radioButtonId) {
            this.layoutId = layoutId;
            this.column1 = column1;
            this.textId1 = textId1;
            this.radioButtonId = radioButtonId;
            mAlphabetIndexer = new AlphabetIndexer(cursor, cursor.getColumnIndex(column1),
                    " ABCDEFGHIJKLMNOPQRTSUVWXYZ");
            mAlphabetIndexer.setCursor(cursor);
        }

        public MyCursorAdapter(Context context, Cursor cursor,
                               int layoutId, String column1,
                               int textId1, int radioButtonId) {
            //TODO: deprecated API is used here for max OS compatibility, provide alternative
            //      using LoaderManager with a CursorLoader.
            //super(context, cursor, 0);
            super(context, cursor, false);

            init(context, cursor, layoutId, column1, textId1, radioButtonId);
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

            RadioButton rb = (RadioButton) view.findViewById(radioButtonId);
            int ii = cursor.getPosition();
            boolean ss = bitSet.get(ii);
            rb.setChecked(bitSet.get(cursor.getPosition()));
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
