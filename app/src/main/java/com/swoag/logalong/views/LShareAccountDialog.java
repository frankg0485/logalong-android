package com.swoag.logalong.views;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.Image;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.widget.CursorAdapter;
import android.text.Editable;
import android.text.TextWatcher;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SectionIndexer;
import android.widget.TextView;

import com.swoag.logalong.R;
import com.swoag.logalong.entities.LUser;
import com.swoag.logalong.network.LProtocol;
import com.swoag.logalong.utils.CountDownTimer;
import com.swoag.logalong.utils.LBroadcastReceiver;
import com.swoag.logalong.utils.LLog;
import com.swoag.logalong.utils.LViewUtils;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.HashSet;

public class LShareAccountDialog extends Dialog
        implements AdapterView.OnItemClickListener, View.OnClickListener, TextWatcher,
        LBroadcastReceiver.BroadcastReceiverListener {
    private static final String TAG = LShareAccountDialog.class.getSimpleName();
    public Context context;
    private ListView mList;

    private LShareAccountDialog.LShareAccountDialogItf callback;

    private ArrayList<LUser> users;
    private HashSet<Integer> selectedIds;
    private Object obj;
    private EditText editText;
    private TextView errorMsgV;
    private ProgressBar progressBar;
    private ImageView addIV;
    private CountDownTimer countDownTimer;
    private MyArrayAdapter myArrayAdapter;

    private void init(Context context, Object obj, HashSet<Integer> selectedIds,
                      LShareAccountDialog.LShareAccountDialogItf callback,
                      ArrayList<LUser> users) {
        this.context = context;
        this.callback = callback;
        this.obj = obj;
        this.selectedIds = selectedIds;
        this.users = users;
    }

    public interface LShareAccountDialogItf {
        public void onShareAccountDialogExit(Object obj, HashSet<Integer> selections);
    }

    public LShareAccountDialog(Context context, Object obj, HashSet<Integer> selectedIds,
                               LShareAccountDialog.LShareAccountDialogItf callback,
                               ArrayList<LUser> users) {
        super(context, android.R.style.Theme_Translucent_NoTitleBar);
        init(context, obj, selectedIds, callback, users);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.account_share_dialog);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        LBroadcastReceiver.getInstance().register(LBroadcastReceiver.ACTION_GET_SHARE_USER_BY_NAME, this);

        findViewById(R.id.selectall).setOnClickListener(this);
        findViewById(R.id.save).setOnClickListener(this);
        findViewById(R.id.cancel).setOnClickListener(this);
        addIV = (ImageView) findViewById(R.id.add);
        LViewUtils.setAlpha(addIV, 0.3f);
        addIV.setOnClickListener(this);
        addIV.setEnabled(false);

        ((TextView) findViewById(R.id.title)).setText(context.getString(R.string.share_account_with));
        errorMsgV = (TextView) findViewById(R.id.errorMsg);

        progressBar = (ProgressBar) findViewById(R.id.progressBar);

        editText = (EditText) findViewById(R.id.newname);
        editText.addTextChangedListener(this);

        try {
            mList = (ListView) findViewById(R.id.list);
            mList.setOnItemClickListener(this);

            mList.setFastScrollEnabled(true);
            myArrayAdapter = new MyArrayAdapter(context.getApplicationContext(), users,
                    R.layout.account_share_item, R.id.name, R.id.checkBox1);
            mList.setAdapter(myArrayAdapter);
        } catch (Exception e) {
            LLog.e(TAG, "unexpected error: " + e.getMessage());
        }
    }

    @Override
    public void onBroadcastReceiverReceive(String action, int ret, Intent intent) {
        countDownTimer.cancel();

        if (ret == 0) {
            int id = intent.getIntExtra("id", 0);
            String name = intent.getStringExtra("name");
            LUser user = new LUser(name, id);
            users.add(user);
            selectedIds.add(id);

            myArrayAdapter.notifyDataSetChanged();

            editText.setText("");
            hideIME();
        } else {
            displayErrorMsg(LShareAccountDialog.this.getContext().getString(R.string.warning_unable_to_find_share_user));
        }
        progressBar.setVisibility(View.GONE);
        addIV.setVisibility(View.VISIBLE);
        editText.setEnabled(true);
    }

    private void do_add_share_user(String name) {
        editText.setEnabled(false);
        hideErrorMsg();
        progressBar.setVisibility(View.VISIBLE);
        addIV.setVisibility(View.GONE);

        LProtocol.ui.getShareUserByName(name);

        countDownTimer = new CountDownTimer(15000, 15000) {
            @Override
            public void onTick(long millisUntilFinished) {

            }

            @Override
            public void onFinish() {
                displayErrorMsg(LShareAccountDialog.this.getContext().getString(R.string.warning_get_share_user_time_out));
                progressBar.setVisibility(View.GONE);
                addIV.setVisibility(View.VISIBLE);
                editText.setEnabled(true);
            }
        }.start();
    }

    private void hideErrorMsg() {
        errorMsgV.setVisibility(View.INVISIBLE);
    }

    private void displayErrorMsg(String msg) {
        errorMsgV.setText(msg);
        errorMsgV.setVisibility(View.VISIBLE);
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void afterTextChanged(Editable s) {
        hideErrorMsg();

        String name = editText.getText().toString().trim();
        if (name.length() >= 2) {
            if (!addIV.isEnabled()) {
                LViewUtils.setAlpha(addIV, 1.0f);
                addIV.setEnabled(true);
            }
        } else if (addIV.isEnabled()) {
            LViewUtils.setAlpha(addIV, 0.3f);
            addIV.setEnabled(false);
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

        switch (id) {
            case R.id.selectall:
                try {
                    CheckBox cb = (CheckBox) v.findViewById(R.id.checkBox1);
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
                break;
            case R.id.save:
                leave();
                break;
            case R.id.cancel:
                selectedIds.clear();
                leave();
                break;
            case R.id.add:
                do_add_share_user(editText.getText().toString().trim());
                break;
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

        CheckBox cb = (CheckBox) arg1.findViewById(R.id.checkBox1);
        cb.setChecked(checked);
        arg1.findViewById(R.id.name).setSelected(checked);
    }

    private void hideIME() {
        try {
            InputMethodManager inputManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.hideSoftInputFromWindow(editText.getWindowToken(), 0);
        } catch (Exception e) {
        }
    }

    private void leave() {
        hideIME();
        callback.onShareAccountDialogExit(obj, selectedIds);
        dismiss();
    }

    private class MyArrayAdapter extends ArrayAdapter<LUser> {
        private LayoutInflater inflater;
        private int layoutId;
        int textId1;
        int checkboxId;

        public MyArrayAdapter(Context context, ArrayList<LUser> users, int layoutId, int textId1, int checkboxId) {
            super(context, 0, users);
            inflater = LayoutInflater.from(context);
            this.layoutId = layoutId;
            this.textId1 = textId1;
            this.checkboxId = checkboxId;
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
