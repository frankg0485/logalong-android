package com.swoag.logalong.views;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

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
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AlphabetIndexer;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SectionIndexer;
import android.widget.TextView;

import com.swoag.logalong.R;
import com.swoag.logalong.entities.LUser;
import com.swoag.logalong.utils.LLog;

import java.util.ArrayList;
import java.util.HashSet;

public class LShareAccountDialog extends Dialog
        implements AdapterView.OnItemClickListener, View.OnClickListener {
    private static final String TAG = LShareAccountDialog.class.getSimpleName();
    public Context context;
    private ListView mList;

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
    private LShareAccountDialog.LShareAccountDialogItf callback;

    private ArrayList<LUser> users;
    private HashSet<Integer> selectedIds;
    private Object obj;
    private EditText editText;

    private void init(Context context, Object obj, HashSet<Integer> selectedIds,
                      LShareAccountDialog.LShareAccountDialogItf callback,
                      int[] ids, ArrayList<LUser> users) {
        this.context = context;
        this.callback = callback;
        this.obj = obj;
        this.selectedIds = selectedIds;

        this.ids = ids;
        this.users = users;
    }

    public interface LShareAccountDialogItf {
        public void onShareAccountDialogExit(Object obj, HashSet<Integer> selections);
    }

    public LShareAccountDialog(Context context, Object obj, HashSet<Integer> selectedIds,
                               LShareAccountDialog.LShareAccountDialogItf callback,
                               int[] ids, ArrayList<LUser> users) {
        super(context, android.R.style.Theme_Translucent_NoTitleBar);
        init(context, obj, selectedIds, callback, ids, users);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(ids[0]);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        ((LinearLayout) findViewById(ids[5])).setOnClickListener(this);
        ((TextView) findViewById(ids[3])).setOnClickListener(this);
        ((TextView) findViewById(ids[4])).setOnClickListener(this);
        ((TextView) findViewById(ids[2])).setText(context.getString(ids[9]));
        editText = (EditText) findViewById(R.id.newname);

        try {
            mList = (ListView) findViewById(ids[8]);
            mList.setOnItemClickListener(this);

            mList.setFastScrollEnabled(true);
            //mList.setAdapter(new MyCursorAdapter(context.getApplicationContext(),
            //        mCursor, ids[1], columns[0], null/*columns[1]*/,
            //        ids[7], ids[6]));
            mList.setAdapter(new MyArrayAdapter(context.getApplicationContext(), users, ids[1], ids[7], ids[6]));
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

                for (int ii = 0; ii < this.users.size(); ii++) {

                    if (cb.isChecked()) {
                        selectedIds.add(users.get(ii).getId());
                    } else {
                        selectedIds.remove(users.get(ii).getId());
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
            leave();
        }
    }

    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
        int id = users.get(arg2).getId();
        boolean checked = !selectedIds.contains(id);
        if (checked) {
            selectedIds.add(id);
        } else {
            selectedIds.remove(id);
        }

        CheckBox cb = (CheckBox) arg1.findViewById(ids[6]);
        cb.setChecked(checked);
        arg1.findViewById(ids[7]).setSelected(checked);
    }

    private void leave() {
        try {
            InputMethodManager inputManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.hideSoftInputFromWindow(editText.getWindowToken(), 0);
        } catch (Exception e) {
        }
        callback.onShareAccountDialogExit(obj, selectedIds);
        dismiss();
    }

    private class MyArrayAdapter extends ArrayAdapter<LUser> {
        private ArrayList<LUser> users;
        private LayoutInflater inflater;
        private int layoutId;
        int textId1;
        int checkboxId;

        public MyArrayAdapter(Context context, ArrayList<LUser> users, int layoutId, int textId1, int checkboxId) {
            super(context, 0, users);
            inflater = LayoutInflater.from(context);
            this.layoutId = layoutId;
        }

        @Override
        public boolean isEnabled(int position) {
            return false;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LUser user = getItem(position);
            if (null == convertView) {
                convertView = inflater.inflate(layoutId, null);
            }

            TextView txtView = (TextView) convertView.findViewById(textId1);
            txtView.setText(user.getName());

            CheckBox cb = (CheckBox) convertView.findViewById(checkboxId);
            cb.setChecked(selectedIds.contains(user.getId()));

            return convertView;
        }
    }
}
