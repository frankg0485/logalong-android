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
import android.widget.SectionIndexer;
import android.widget.TextView;

import com.swoag.logalong.utils.LLog;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class LMultiSelectionDialog extends Dialog
        implements AdapterView.OnItemClickListener, View.OnClickListener, android.text.TextWatcher {
    private static final String TAG = LMultiSelectionDialog.class.getSimpleName();
    private Cursor mCursor;
    private int idColumnIndex;

    public Context context;
    private TextView countView;
    private EditText searchText;
    private TextView searchButton;
    private ListView mList;

	/* 0: layoutId;
     * 1: itemLayoutId;
	 * 2: titleId;
	 * 3: saveBtnId;
	 * 4: cancelBtnId;
	 * 5: selectAllCheckBoxId;
	 * 6: selectCheckBoxId;
	 * 7: countTextId;
	 * 8: mainEntryTextId;
	 * 9: subEntryTextId;
	 * 10: listViewId;
	 * 11: photoImageId;
	 * 12: defaultPhotoResourceId;
	 * 13: searchText
	 * 14: searchButton
	 * 15: titleStringId
	 */

    private int[] ids;
    private LMultiSelectionDialog.OnSelectionDialogItf callback;

    /* 0: mainEntryColumn;
     * 1: subEntryColumn;
     */
    private String[] columns;
    private HashSet<Integer> selectedIds;

    private boolean photo;

    private void init(Context context, LMultiSelectionDialog.OnSelectionDialogItf callback,
                      int[] ids, String[] columns) {
        this.context = context;
        this.callback = callback;

        selectedIds = new HashSet<Integer>();

        this.mCursor = callback.onGetCursor("");
        this.idColumnIndex = this.mCursor.getColumnIndex("_id");

        this.ids = ids;
        this.columns = columns;
        this.photo = false;
    }

    public interface OnSelectionDialogItf {

        public void onClick();

        public Cursor onGetCursor(String column);
    }

    public LMultiSelectionDialog(Context context, LMultiSelectionDialog.OnSelectionDialogItf callback,
                                 int[] ids, String[] columns) {
        super(context, android.R.style.Theme_Translucent_NoTitleBar);
        init(context, callback, ids, columns);
    }

    public LMultiSelectionDialog(Context context, LMultiSelectionDialog.OnSelectionDialogItf callback,
                                 int[] ids, String[] columns, boolean photo) {
        super(context, android.R.style.Theme_Translucent_NoTitleBar);
        init(context, callback, ids, columns);
        this.photo = photo;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(ids[0]);

        ((LinearLayout) findViewById(ids[5])).setOnClickListener(this);
        ((TextView) findViewById(ids[3])).setOnClickListener(this);
        ((TextView) findViewById(ids[4])).setOnClickListener(this);
        countView = (TextView) findViewById(ids[7]);
        ((TextView) findViewById(ids[2])).setText(context.getString(ids[15]));

        ////SonrLog.d(TAG, "creating dialog");
        try {
            mList = (ListView) findViewById(ids[10]);
            mList.setOnItemClickListener(this);

            searchText = (EditText) findViewById(ids[13]);
            searchButton = (TextView) findViewById(ids[14]);
            searchButton.setOnClickListener(this);
            searchText.addTextChangedListener(this);

            mList.setFastScrollEnabled(true);
            /*if (photo) {
                mList.setAdapter(new MyCursorAdapter(context.getApplicationContext(),
                        mCursor, ids[1], columns[0], columns[1], columns[2],
                        ids[8], ids[9], ids[6], ids[11], ids[12]));
            } else*/
            {
                mList.setAdapter(new MyCursorAdapter(context.getApplicationContext(),
                        mCursor, ids[1], columns[0], null/*columns[1]*/,
                        ids[8], ids[9], ids[6]));
            }
        } catch (Exception e) {
            LLog.e(TAG, "unexpected error: " + e.getMessage());
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

    @Override
    public void onClick(View v) {
        int id = v.getId();

        if (id == ids[5]) {
            try {
                CheckBox cb = (CheckBox) v.findViewById(ids[6]);
                cb.setChecked(!cb.isChecked());

                this.mCursor.moveToFirst();
                for (int ii = 0; ii < this.mCursor.getCount(); ii++) {

                    if (cb.isChecked()) {
                        selectedIds.add(this.mCursor.getInt(idColumnIndex));
                    } else {
                        selectedIds.remove(this.mCursor.getInt(idColumnIndex));
                    }
                    this.mCursor.moveToNext();
                }
                countView.setText(Integer.toString(selectedIds.size()));
                mList.invalidateViews();
            } catch (Exception e) {
                LLog.e(TAG, "unexpected error: " + e.getMessage());
            }
        } else if (id == ids[3]) {
            leave();
        } else if (id == ids[4]) {
            selectedIds.clear();
            leave();
        } else if (id == ids[14]) {
            hideSoftKeyboard();
        }
    }

    private void hideSoftKeyboard() {
        InputMethodManager inputManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        //LLog.d(TAG, "input manager " + inputManager + "@" + searchText);
        inputManager.hideSoftInputFromWindow(searchText.getWindowToken(), 0);
    }

    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
        this.mCursor.moveToPosition(arg2);
        int id = this.mCursor.getInt(idColumnIndex);
        //SonrLog.d(TAG, "click on id: " + id + "@" + arg2);
        boolean checked = !selectedIds.contains(id);
        if (checked) {
            selectedIds.add(id);
        } else {
            selectedIds.remove(id);
        }

        CheckBox cb = (CheckBox) arg1.findViewById(ids[6]);
        cb.setChecked(checked);
        arg1.findViewById(ids[8]).setSelected(checked);
        countView.setText(Integer.toString(selectedIds.size()));
        //LLog.d(TAG, "set num: " + bitSet.length());
    }

    private void leave() {
        List<HashSet> lb = new ArrayList<HashSet>();

        lb.add(selectedIds);
        //this.clickListener.onClick(this.context, 0, ISonrDialog.BUTTON_NEUTRAL, lb);
        hideSoftKeyboard();
        dismiss();
    }

    @Override
    public void afterTextChanged(Editable s) {
        ////SonrLog.d(TAG, "text changed to: " + alarmName.getText());
        String newStr = searchText.getText().toString().trim();
        mCursor = callback.onGetCursor(newStr);
        idColumnIndex = mCursor.getColumnIndex("_id");

        //countView.setText(Integer.toString(selectedIds.size()));

        /*if (photo) {
            mList.setAdapter(new MyCursorAdapter(context.getApplicationContext(), mCursor,
                    ids[1], columns[0], columns[1], columns[2],
                    ids[8], ids[9], ids[6], ids[11], ids[12]));
        } else */
        {
            mList.setAdapter(new MyCursorAdapter(context.getApplicationContext(),
                    mCursor, ids[1], columns[0], null/*columns[1]*/,
                    ids[8], ids[9], ids[6]));
        }
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
        private String column2;
        private String column3;
        private int textId1;
        private int textId2;
        private int checkboxId;
        private int photoId;
        private int defaultImage;

        private void init(Context context, Cursor cursor,
                          int layoutId, String column1, String column2,
                          int textId1, int textId2, int checkboxId) {
            this.layoutId = layoutId;
            this.column1 = column1;
            this.column2 = column2;
            this.textId1 = textId1;
            this.textId2 = textId2;
            this.checkboxId = checkboxId;
            mAlphabetIndexer = new AlphabetIndexer(cursor, cursor.getColumnIndex(column1),
                    " ABCDEFGHIJKLMNOPQRTSUVWXYZ");
            mAlphabetIndexer.setCursor(cursor);
        }

        public MyCursorAdapter(Context context, Cursor cursor,
                               int layoutId, String column1, String column2,
                               int textId1, int textId2, int checkboxId) {
            //TODO: deprecated API is used here for max OS compatibility, provide alternative
            //      using LoaderManager with a CursorLoader.
            //super(context, cursor, 0);
            super(context, cursor, false);

            init(context, cursor, layoutId, column1, column2, textId1, textId2, checkboxId);
        }

        public MyCursorAdapter(Context context, Cursor cursor,
                               int layoutId, String column1, String column2, String column3,
                               int textId1, int textId2, int checkboxId, int photoId, int defaultImage) {
            //TODO: deprecated API is used here for max OS compatibility, provide alternative
            //      using LoaderManager with a CursorLoader.
            //super(context, cursor, 0);
            super(context, cursor, false);
            init(context, cursor, layoutId, column1, column2, textId1, textId2, checkboxId);

            this.column3 = column3;
            this.photoId = photoId;
            this.defaultImage = defaultImage;
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
            txtView = (TextView) view.findViewById(textId2);
            if (photo) {
                txtView.setText(cursor.getString(cursor.getColumnIndex(column2)));
                int id = cursor.getInt(cursor.getColumnIndex(column3));
                byte[] photoBytes = null;
                ////SonrLog.d(TAG, column3 + " id: " + id);
                if (id > 0) {
                    Uri photoUri = ContentUris.withAppendedId(ContactsContract.Data.CONTENT_URI, id);

                    ContentResolver cr = context.getContentResolver();
                    Cursor csr = cr.query(photoUri, new String[]{ContactsContract.CommonDataKinds.Photo.PHOTO},
                            null, null, null);

                    try {
                        if (csr.moveToFirst()) {
                            photoBytes = csr.getBlob(0);
                        }
                    } catch (Exception e) {

                    } finally {
                        csr.close();
                    }
                }
                if (null != photoBytes) {
                    ((ImageView) view.findViewById(photoId)).setImageBitmap
                            (BitmapFactory.decodeByteArray(photoBytes, 0, photoBytes.length));
                } else {
                    ((ImageView) view.findViewById(photoId)).setImageResource(defaultImage);
                }
            } else {
                //txtView.setText(SonrHelpers.durationToHMS(cursor.getLong(cursor.getColumnIndex(column2))));
            }

            CheckBox cb = (CheckBox) view.findViewById(checkboxId);
            cb.setChecked(selectedIds.contains(cursor.getInt(idColumnIndex)));
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
