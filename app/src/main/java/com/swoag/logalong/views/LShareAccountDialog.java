package com.swoag.logalong.views;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
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
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.swoag.logalong.MainService;
import com.swoag.logalong.R;
import com.swoag.logalong.entities.LAccount;
import com.swoag.logalong.entities.LUser;
import com.swoag.logalong.network.LAppServer;
import com.swoag.logalong.network.LProtocol;
import com.swoag.logalong.utils.CountDownTimer;
import com.swoag.logalong.utils.DBAccount;
import com.swoag.logalong.utils.LBroadcastReceiver;
import com.swoag.logalong.utils.LLog;
import com.swoag.logalong.utils.LPreferences;
import com.swoag.logalong.utils.LViewUtils;

import java.util.ArrayList;
import java.util.HashSet;

public class LShareAccountDialog extends Dialog
        implements AdapterView.OnItemClickListener, View.OnClickListener, TextWatcher,
        LBroadcastReceiver.BroadcastReceiverListener, DialogInterface.OnDismissListener {
    private static final String TAG = LShareAccountDialog.class.getSimpleName();
    public Context context;
    private ListView mList;

    private LShareAccountDialog.LShareAccountDialogItf callback;

    private ArrayList<LUser> users;
    private HashSet<Integer> selectedIds;
    private HashSet<Integer> origSelectedIds;
    private EditText editText;
    private TextView errorMsgV;
    private ProgressBar progressBar;
    private ImageView addIV;
    private CountDownTimer countDownTimer;
    private MyArrayAdapter myArrayAdapter;
    private BroadcastReceiver broadcastReceiver;
    private CheckBox checkBoxAllAccounts;
    private View userCtrlView;
    private View selectAllView;
    private LAccount account;
    private long accountId;

    private void init(Context context, long accountId, HashSet<Integer> selectedIds,
                      LShareAccountDialog.LShareAccountDialogItf callback,
                      ArrayList<LUser> users) {
        this.context = context;
        this.callback = callback;
        this.origSelectedIds = selectedIds;
        this.selectedIds = new HashSet<Integer>(selectedIds);
        this.users = users;
        this.accountId = accountId;
        this.account = DBAccount.getById(accountId);
    }

    public interface LShareAccountDialogItf {
        public void onShareAccountDialogExit(boolean ok, boolean applyToAllAccounts, long accountId,
                                             HashSet<Integer> selections, HashSet<Integer> origSelections);
    }

    public LShareAccountDialog(Context context, long accountId, HashSet<Integer> selectedIds,
                               LShareAccountDialog.LShareAccountDialogItf callback,
                               ArrayList<LUser> users) {
        super(context, android.R.style.Theme_Translucent_NoTitleBar);
        if (!LAppServer.getInstance().UiIsLoggedIn()) {
            MainService.start(context);
        }
        init(context, accountId, selectedIds, callback, users);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.account_share_dialog);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        broadcastReceiver = LBroadcastReceiver.getInstance().register(new int[]{
                LBroadcastReceiver.ACTION_GET_SHARE_USER_BY_NAME}, this);

        TextView tv = (TextView)findViewById(R.id.shareAccountName);
        tv.setText(DBAccount.getNameById(accountId));

        findViewById(R.id.save).setOnClickListener(this);
        findViewById(R.id.cancel).setOnClickListener(this);
        checkBoxAllAccounts = (CheckBox) findViewById(R.id.checkBoxShareAllAccounts);
        userCtrlView = findViewById(R.id.userCtrlView);

        selectAllView = findViewById(R.id.selectall);
        selectAllView.setOnClickListener(this);

        addIV = (ImageView) findViewById(R.id.add);
        LViewUtils.setAlpha(addIV, 0.3f);
        addIV.setOnClickListener(this);
        addIV.setEnabled(false);

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

        checkBoxAllAccounts.setChecked(false);
        enableShare(true);

        this.setOnDismissListener(this);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        if (broadcastReceiver != null) {
            LBroadcastReceiver.getInstance().unregister(broadcastReceiver);
            broadcastReceiver = null;
        }
    }

    @Override
    public void onBroadcastReceiverReceive(int action, int ret, Intent intent) {
        switch (action) {
            case LBroadcastReceiver.ACTION_GET_SHARE_USER_BY_NAME:
                if (countDownTimer != null) countDownTimer.cancel();
                if (ret == LProtocol.RSPS_OK) {
                    int id = intent.getIntExtra("id", 0);
                    String name = intent.getStringExtra("name");
                    String fullName = intent.getStringExtra("fullName");

                    LUser user = new LUser(name, fullName, id);
                    users.add(user);
                    selectedIds.add(id);
                    myArrayAdapter.notifyDataSetChanged();

                    account = DBAccount.getById(accountId);
                    account.addShareUser(id, LAccount.ACCOUNT_SHARE_PREPARED);
                    DBAccount.update(account);
                    LPreferences.setShareUserName(id, name);
                    LPreferences.setShareUserFullName(id, fullName);

                    editText.setText("");
                    hideIME();
                } else {
                    displayErrorMsg(LShareAccountDialog.this.getContext().getString(R.string.warning_unable_to_find_share_user));
                }

                progressBar.setVisibility(View.GONE);
                addIV.setVisibility(View.VISIBLE);
                editText.setEnabled(true);
                break;
        }
    }

    private void do_add_share_user(String name) {
        editText.setEnabled(false);
        hideErrorMsg();
        progressBar.setVisibility(View.VISIBLE);
        addIV.setVisibility(View.GONE);

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
        LAppServer.getInstance().UiGetShareUserByName(name);
    }

    private void hideErrorMsg() {
        errorMsgV.setVisibility(View.GONE);
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
            leave(false);
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
                String str = "";
                if (editText.getText() != null) {
                    str = editText.getText().toString().trim();
                }
                if (!TextUtils.isEmpty(str)) do_add_share_user(editText.getText().toString().trim());
                else leave(true);
                break;
            case R.id.cancel:
                leave(false);
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

    private void leave(boolean ok) {
        hideIME();
        callback.onShareAccountDialogExit(ok, checkBoxAllAccounts.isChecked(), accountId, selectedIds, origSelectedIds);
        dismiss();
    }

    private void enableShare(boolean enabled) {
        LViewUtils.disableEnableControls(enabled, mList);
        LViewUtils.disableEnableControls(enabled, (ViewGroup) userCtrlView);
        LViewUtils.disableEnableControls(enabled, (ViewGroup) selectAllView);
        if (enabled) {
            LViewUtils.setAlpha(mList, 1.0f);
            LViewUtils.setAlpha(userCtrlView, 1.0f);
            LViewUtils.setAlpha(selectAllView, 1.0f);
        } else {
            LViewUtils.setAlpha(mList, 0.5f);
            LViewUtils.setAlpha(userCtrlView, 0.5f);
            LViewUtils.setAlpha(selectAllView, 0.5f);
        }
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
            txtView.setText(user.getFullName() + " (" + user.getName() + ")");

            CheckBox cb = (CheckBox) convertView.findViewById(checkboxId);
            cb.setChecked(selectedIds.contains(user.getId()));

            ImageView imageView = (ImageView) convertView.findViewById(R.id.share);
            int share = account.getShareUserState(user.getId());
            switch (share) {
                case LAccount.ACCOUNT_SHARE_CONFIRMED:
                case LAccount.ACCOUNT_SHARE_CONFIRMED_SYNCED:
                    imageView.setImageResource(R.drawable.ic_action_share_green);
                    break;
                case LAccount.ACCOUNT_SHARE_INVITED:
                    imageView.setImageResource(R.drawable.ic_action_share_yellow);
                    break;
                default:
                    imageView.setImageResource(R.drawable.ic_action_share);
                    break;
            }

            return convertView;
        }
    }
}
