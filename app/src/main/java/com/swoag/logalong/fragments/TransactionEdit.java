package com.swoag.logalong.fragments;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.swoag.logalong.R;
import com.swoag.logalong.entities.LTransaction;
import com.swoag.logalong.utils.DBAccess;
import com.swoag.logalong.utils.DBAccount;
import com.swoag.logalong.utils.DBCategory;
import com.swoag.logalong.utils.DBHelper;
import com.swoag.logalong.utils.DBTag;
import com.swoag.logalong.utils.DBTransaction;
import com.swoag.logalong.utils.DBVendor;
import com.swoag.logalong.utils.LOnClickListener;
import com.swoag.logalong.utils.LViewUtils;
import com.swoag.logalong.views.LDollarAmountPicker;
import com.swoag.logalong.views.LNewEntryDialog;
import com.swoag.logalong.views.LSelectionDialog;
import com.swoag.logalong.views.LWarnDialog;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class TransactionEdit implements LSelectionDialog.OnSelectionDialogItf,
        LNewEntryDialog.LNewEntryDialogItf,
        DatePickerDialog.OnDateSetListener, LDollarAmountPicker.LDollarAmountPickerItf,
        LWarnDialog.LWarnDialogItf {
    private static final String TAG = TransactionEdit.class.getSimpleName();

    private Activity activity;
    private View rootView;
    private boolean bCreate;
    private boolean bScheduleMode;
    private LTransaction item;
    private LTransaction savedItem;
    private TransitionEditItf callback;

    private View viewDiscard, viewSave, viewFrom, viewTo, viewAccount2, viewCategory, viewVendor, viewTag;
    private TextView amountTV, accountTV, account2TV, categoryTV, vendorTV, tagTV;

    private TextView dateTV;
    private EditText noteET;

    private LSelectionDialog mSelectionDialog;
    private boolean firstTimeAmountPicker = true;
    private MyClickListener myClickListener;

    public interface TransitionEditItf {
        public static final int EXIT_DELETE = 10;
        public static final int EXIT_OK = 20;
        public static final int EXIT_CANCEL = 30;

        public void onTransactionEditExit(int action, boolean changed);
    }

    public TransactionEdit(Activity activity, View rootView, LTransaction item, boolean bCreate, boolean bScheduleMode,
                           TransitionEditItf callback) {
        this.activity = activity;
        this.rootView = rootView;
        this.item = item;
        this.callback = callback;

        this.savedItem = new LTransaction(item);

        this.bCreate = bCreate;
        this.bScheduleMode = bScheduleMode;
        myClickListener = new MyClickListener();
        create();
    }

    private void updateItemDisplay() {
        dateTV.setText(new SimpleDateFormat("MMM d, yyy").format(item.getTimeStamp()));

        accountTV.setTypeface(null, item.getAccount() <= 0 ? Typeface.NORMAL : Typeface.BOLD);
        accountTV.setText(DBAccount.getNameById((item.getType() == LTransaction.TRANSACTION_TYPE_TRANSFER_COPY) ?
                item.getAccount2() : item.getAccount()));
        if (item.getType() == LTransaction.TRANSACTION_TYPE_TRANSFER ||
                item.getType() == LTransaction.TRANSACTION_TYPE_TRANSFER_COPY) {
            account2TV.setTypeface(null, item.getAccount2() <= 0 ? Typeface.NORMAL : Typeface.BOLD);
            account2TV.setText(DBAccount.getNameById((item.getType() == LTransaction.TRANSACTION_TYPE_TRANSFER) ?
                    item.getAccount2() : item.getAccount()));
        } else {
            categoryTV.setTypeface(null, item.getCategory() <= 0 ? Typeface.NORMAL : Typeface.BOLD);
            categoryTV.setText(DBCategory.getNameById(item.getCategory()));

            vendorTV.setTypeface(null, item.getVendor() <= 0 ? Typeface.NORMAL : Typeface.BOLD);
            if (item.getType() == LTransaction.TRANSACTION_TYPE_EXPENSE) {
                vendorTV.setHint(activity.getString(R.string.hint_payee_not_specified));
            } else if (item.getType() == LTransaction.TRANSACTION_TYPE_INCOME) {
                vendorTV.setHint(activity.getString(R.string.hint_payer_not_specified));
            }
            vendorTV.setText(DBVendor.getNameById(item.getVendor()));

            tagTV.setTypeface(null, item.getTag() <= 0 ? Typeface.NORMAL : Typeface.BOLD);
            tagTV.setText(DBTag.getNameById(item.getTag()));
        }
        updateOkDisplay();
    }

    private void updateOkDisplay() {
        if ((item.getAccount() > 0) && (item.getValue() > 0)
                && ((item.getType() != LTransaction.TRANSACTION_TYPE_TRANSFER
                && item.getType() != LTransaction.TRANSACTION_TYPE_TRANSFER_COPY) ||
                (item.getAccount2() > 0 && item.getAccount2() != item.getAccount()))) {
            enableOk(true);
        } else {
            enableOk(false);
        }
    }

    private void create() {
        setViewListener(rootView, R.id.exit);
        if (!bScheduleMode) setViewListener(rootView, R.id.back);

        viewSave = setViewListener(rootView, R.id.add);
        ImageView iv = (ImageView) viewSave.findViewById(R.id.addImg);
        iv.setImageResource(R.drawable.ic_action_accept);
        iv.setClickable(false);

        setViewListener(rootView, R.id.amountRow);
        setViewListener(rootView, R.id.accountRow);
        viewCategory = setViewListener(rootView, R.id.categoryRow);
        viewVendor = setViewListener(rootView, R.id.vendorRow);
        viewTag = setViewListener(rootView, R.id.tagRow);
        viewAccount2 = setViewListener(rootView, R.id.account2Row);

        dateTV = (TextView) setViewListener(rootView, R.id.tvDate);

        TextView typeTV = (TextView) rootView.findViewById(R.id.type);
        typeTV.setText(activity.getString(LTransaction.getTypeStringId(item.getType())));

        amountTV = (TextView) rootView.findViewById(R.id.tvAmount);
        accountTV = (TextView) rootView.findViewById(R.id.tvAccount);
        account2TV = (TextView) rootView.findViewById(R.id.tvAccount2);
        categoryTV = (TextView) rootView.findViewById(R.id.tvCategory);
        vendorTV = (TextView) rootView.findViewById(R.id.tvVendor);
        tagTV = (TextView) rootView.findViewById(R.id.tvTag);
        updateItemDisplay();

        if (!bScheduleMode) {
            noteET = (EditText) setViewListener(rootView, R.id.noteEditText);
            noteET.setText(item.getNote());
            setViewListener(rootView, R.id.clearText);
        }

        String inputString = value2string(item.getValue());

        if (item.getType() == LTransaction.TRANSACTION_TYPE_TRANSFER ||
                item.getType() == LTransaction.TRANSACTION_TYPE_TRANSFER_COPY) {
            viewAccount2.setVisibility(View.VISIBLE);
            viewCategory.setVisibility(View.GONE);
            viewVendor.setVisibility(View.GONE);
            viewTag.setVisibility(View.GONE);
            rootView.findViewById(R.id.from).setVisibility(View.VISIBLE);
            rootView.findViewById(R.id.to).setVisibility(View.VISIBLE);

            amountTV.setTextColor(activity.getResources().getColor(R.color.base_blue));
        } else {
            viewAccount2.setVisibility(View.GONE);
            viewCategory.setVisibility(View.VISIBLE);
            viewVendor.setVisibility(View.VISIBLE);
            viewTag.setVisibility(View.VISIBLE);
            rootView.findViewById(R.id.from).setVisibility(View.GONE);
            rootView.findViewById(R.id.to).setVisibility(View.GONE);

            if (item.getType() == LTransaction.TRANSACTION_TYPE_EXPENSE) {
                amountTV.setTextColor(activity.getResources().getColor(R.color.base_red));
            } else {
                amountTV.setTextColor(activity.getResources().getColor(R.color.base_green));
            }
        }

        if (inputString.isEmpty()) {
            amountTV.setText("0.0");
        } else {
            amountTV.setText(inputString);
        }

        if (bCreate) {
            if (!bScheduleMode) {
                LDollarAmountPicker picker = new LDollarAmountPicker(activity, item.getValue(), this);
                picker.show();
            }
        } else {
            viewDiscard = setViewListener(rootView, R.id.discard);
            viewDiscard.setVisibility(View.VISIBLE);
        }

        hideIME();
    }

    private void destroy() {
        viewSave = null;
        viewDiscard = null;

        viewCategory = null;
        viewVendor = null;
        viewTag = null;
        viewAccount2 = null;

        if (!bScheduleMode) noteET = null;
        accountTV = null;
        categoryTV = null;
        vendorTV = null;
        tagTV = null;
        dateTV = null;
        savedItem = null;
    }

    @Override
    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, monthOfYear, dayOfMonth);
        dateTV.setText(new SimpleDateFormat("MMM d, yyy").format(calendar.getTimeInMillis()));
        item.setTimeStamp(calendar.getTimeInMillis());
    }

    @Override
    public void onDollarAmountPickerExit(double value, boolean save) {
        if (save) {
            item.setValue(value);
            String inputString = value2string(value);
            if (inputString.isEmpty()) {
                amountTV.setText("0.0");
            } else {
                amountTV.setText(inputString);
            }
        } else if (firstTimeAmountPicker) {
            if (bCreate) {
                callback.onTransactionEditExit(TransitionEditItf.EXIT_CANCEL, false);
                destroy();
                return;
            }
        }

        if (firstTimeAmountPicker) firstTimeAmountPicker = false;
        updateOkDisplay();
    }

    private class MyClickListener extends LOnClickListener {
        @Override
        public void onClicked(View v) {
            hideIME();
            switch (v.getId()) {
                case R.id.tvDate:
                    final Calendar c = Calendar.getInstance();
                    DatePickerDialog datePickerDialog = new DatePickerDialog(activity, android.R.style.Theme_Holo_Light_Dialog_NoActionBar,
                            TransactionEdit.this, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
                    datePickerDialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
                    datePickerDialog.show();
                    break;

                case R.id.noteEditText:
                    noteET.setCursorVisible(true);
                    try {
                        if (noteET.requestFocus()) {
                            InputMethodManager keyboard = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                            keyboard.showSoftInput(noteET, 0);
                        }
                    } catch (Exception e) {
                    }
                    break;

                case R.id.clearText:
                    noteET.setText("");
                    break;

                case R.id.amountRow:
                    LDollarAmountPicker picker = new LDollarAmountPicker(activity, item.getValue(), TransactionEdit.this);
                    picker.show();
                    break;

                case R.id.account2Row:
                    try {
                        ids[9] = R.string.select_account;
                        mSelectionDialog = new LSelectionDialog
                                (activity, TransactionEdit.this, ids,
                                        DBHelper.TABLE_ACCOUNT_NAME,
                                        DBHelper.TABLE_COLUMN_NAME, DBAccount.getDbIndexById(item.getAccount2()), DLG_ID_ACCOUNT2);
                        mSelectionDialog.show();
                        mSelectionDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
                    } catch (Exception e) {
                    }
                    break;

                case R.id.accountRow:
                    try {
                        ids[9] = R.string.select_account;
                        mSelectionDialog = new LSelectionDialog
                                (activity, TransactionEdit.this, ids,
                                        DBHelper.TABLE_ACCOUNT_NAME,
                                        DBHelper.TABLE_COLUMN_NAME, DBAccount.getDbIndexById(item.getAccount()), DLG_ID_ACCOUNT);
                        mSelectionDialog.show();
                        mSelectionDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
                    } catch (Exception e) {
                    }
                    break;

                case R.id.categoryRow:
                    try {
                        ids[9] = R.string.select_category;
                        mSelectionDialog = new LSelectionDialog
                                (activity, TransactionEdit.this, ids,
                                        DBHelper.TABLE_CATEGORY_NAME,
                                        DBHelper.TABLE_COLUMN_NAME, DBCategory.getDbIndexById(item.getCategory()), DLG_ID_CATEGORY);
                        mSelectionDialog.show();
                        mSelectionDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
                    } catch (Exception e) {
                    }
                    break;
                case R.id.vendorRow:
                    try {
                        ids[9] = R.string.select_vendor;
                        mSelectionDialog = new LSelectionDialog
                                (activity, TransactionEdit.this, ids,
                                        DBHelper.TABLE_VENDOR_NAME,
                                        DBHelper.TABLE_COLUMN_NAME, item.getType() == LTransaction.TRANSACTION_TYPE_INCOME ?
                                        DBVendor.getPayerIndexById(item.getVendor()) : DBVendor.getPayeeIndexById(item.getVendor()), DLG_ID_VENDOR);
                        mSelectionDialog.show();
                        mSelectionDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
                    } catch (Exception e) {
                    }
                    break;
                case R.id.tagRow:
                    try {
                        ids[9] = R.string.select_tag;
                        mSelectionDialog = new LSelectionDialog
                                (activity, TransactionEdit.this, ids,
                                        DBHelper.TABLE_TAG_NAME,
                                        DBHelper.TABLE_COLUMN_NAME, DBTag.getDbIndexById(item.getTag()), DLG_ID_TAG);
                        mSelectionDialog.show();
                        mSelectionDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
                    } catch (Exception e) {
                    }
                    break;
                case R.id.discard:
                    LWarnDialog warnDialog = new LWarnDialog(activity, null, TransactionEdit.this,
                            activity.getString(R.string.delete),
                            activity.getString(R.string.warning_delete_record),
                            activity.getString(R.string.delete_now),
                            true);
                    warnDialog.show();
                    break;

                case R.id.exit:
                case R.id.back:
                    callback.onTransactionEditExit(TransitionEditItf.EXIT_CANCEL, false);
                    destroy();
                    break;

                case R.id.add:
                    saveLog();
                    break;

                default:
                    break;
            }
        }
    }

    public void dismiss() {
        callback.onTransactionEditExit(TransitionEditItf.EXIT_CANCEL, false);
        destroy();
    }

    @Override
    public void onWarnDialogExit(Object obj, boolean confirm, boolean ok) {
        if (confirm && ok) {
            callback.onTransactionEditExit(TransitionEditItf.EXIT_DELETE, false);
            destroy();
        }
    }

    @Override
    public boolean onNewEntryDialogExit(int id, int type, boolean created, String name, boolean attr1, boolean attr2) {
        if (created && (!name.isEmpty())) {
            int selection = -1;
            switch (id) {
                case DLG_ID_ACCOUNT:
                    selection = DBAccount.getDbIndexById(DBAccount.getIdByName(name));
                    break;
                case DLG_ID_ACCOUNT2:
                    selection = DBAccount.getDbIndexById(DBAccount.getIdByName(name));
                    break;
                case DLG_ID_CATEGORY:
                    selection = DBCategory.getDbIndexById(DBCategory.getIdByName(name));
                    break;
                case DLG_ID_VENDOR:
                    selection = item.getType() == LTransaction.TRANSACTION_TYPE_INCOME ?
                            DBVendor.getPayerIndexById(DBVendor.getIdByName(name))
                            : DBVendor.getPayeeIndexById(DBVendor.getIdByName(name));
                    break;
                case DLG_ID_TAG:
                    selection = DBTag.getDbIndexById(DBTag.getIdByName(name));
                    break;
            }
            mSelectionDialog.refresh(selection);
        }
        return true;
    }

    @Override
    public void onSelectionAddNew(int dlgId) {
        boolean attr1 = true, attr2 = false;
        String title = "";
        int type = 0;
        switch (dlgId) {
            case DLG_ID_ACCOUNT:
            case DLG_ID_ACCOUNT2:
                title = activity.getString(R.string.new_account);
                type = LNewEntryDialog.TYPE_ACCOUNT;
                break;
            case DLG_ID_CATEGORY:
                title = activity.getString(R.string.new_category);
                type = LNewEntryDialog.TYPE_CATEGORY;
                break;
            case DLG_ID_VENDOR:
                title = activity.getString(R.string.new_vendor);
                type = LNewEntryDialog.TYPE_VENDOR;
                if (item.getType() == LTransaction.TRANSACTION_TYPE_EXPENSE) {
                    attr1 = true;
                    attr2 = false;
                } else if (item.getType() == LTransaction.TRANSACTION_TYPE_INCOME) {
                    attr1 = false;
                    attr2 = true;
                } else {
                    attr1 = attr2 = true;
                }
                break;
            case DLG_ID_TAG:
                title = activity.getString(R.string.new_tag);
                type = LNewEntryDialog.TYPE_TAG;
                break;
        }
        LNewEntryDialog newEntryDialog = new LNewEntryDialog(activity, dlgId, type, TransactionEdit.this, title, null, attr1, attr2);
        newEntryDialog.show();
    }

    @Override
    public Cursor onSelectionGetCursor(String table, String column) {
        if (table.contentEquals(DBHelper.TABLE_ACCOUNT_NAME))
            return DBAccount.getCursorSortedBy(DBHelper.TABLE_COLUMN_NAME);
        else if (table.contentEquals(DBHelper.TABLE_CATEGORY_NAME))
            return DBCategory.getCursorSortedBy(DBHelper.TABLE_COLUMN_NAME);
        else if (table.contentEquals(DBHelper.TABLE_VENDOR_NAME)) {
            if (item.getType() == LTransaction.TRANSACTION_TYPE_INCOME)
                return DBVendor.getPayerCursorSortedBy(DBHelper.TABLE_COLUMN_NAME);
            else
                return DBVendor.getPayeeCursorSortedBy(DBHelper.TABLE_COLUMN_NAME);
        } else if (table.contentEquals(DBHelper.TABLE_TAG_NAME))
            return DBTag.getCursorSortedBy(DBHelper.TABLE_COLUMN_NAME);
        return null;
    }

    private static final int DLG_ID_ACCOUNT = 10;
    private static final int DLG_ID_ACCOUNT2 = 15;
    private static final int DLG_ID_CATEGORY = 20;
    private static final int DLG_ID_VENDOR = 30;
    private static final int DLG_ID_TAG = 40;

    @Override
    public void onSelectionDialogExit(int dlgId, long selectedId) {
        switch (dlgId) {
            case DLG_ID_ACCOUNT:
                if (selectedId == item.getAccount2() && (item.getType() == LTransaction.TRANSACTION_TYPE_TRANSFER
                        || item.getType() == LTransaction.TRANSACTION_TYPE_TRANSFER_COPY)) {
                    selectedId = -1;
                }
                item.setAccount(selectedId);
                updateOkDisplay();
                break;
            case DLG_ID_ACCOUNT2:
                if (selectedId == item.getAccount() && (item.getType() == LTransaction.TRANSACTION_TYPE_TRANSFER
                        || item.getType() == LTransaction.TRANSACTION_TYPE_TRANSFER_COPY)) {
                    selectedId = -1;
                }
                item.setAccount2(selectedId);
                updateOkDisplay();
                break;
            case DLG_ID_CATEGORY:
                item.setCategory(selectedId);
                break;
            case DLG_ID_VENDOR:
                item.setVendor(selectedId);
                break;
            case DLG_ID_TAG:
                item.setTag(selectedId);
                break;
        }
        updateItemDisplay();
    }

    private int[] ids = new int[]{
            R.layout.selection_dialog,
            R.layout.selection_item,
            R.id.title,
            R.id.save,
            R.id.cancel,
            R.id.radioButton,
            R.id.name,
            R.id.list,
            R.id.searchText,
            R.string.select_account,
            R.id.add};

    private View setViewListener(View v, int id) {
        View view = v.findViewById(id);
        view.setOnClickListener(myClickListener);
        return view;
    }

    private void enableOk(boolean yes) {
        viewSave.setEnabled(yes);
        if (yes) LViewUtils.setAlpha((View) viewSave, 1.0f);
        else LViewUtils.setAlpha((View) viewSave, 0.5f);
    }

    private String value2string(double value) {
        String str = "";
        if (value != 0) {
            str = String.format("%.2f", value);
        }
        return str;
    }

    private void saveLog() {
        if (!bScheduleMode) item.setNote(noteET.getText().toString());
        boolean changed = !item.isEqual(savedItem);
        if (changed) item.setTimeStampLast(System.currentTimeMillis());
        callback.onTransactionEditExit(TransitionEditItf.EXIT_OK, changed);
        destroy();
    }

    private void hideIME() {
        if (!bScheduleMode) {
            try {
                InputMethodManager inputManager = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                inputManager.hideSoftInputFromWindow(noteET.getWindowToken(), 0);
                noteET.setCursorVisible(false);
            } catch (Exception e) {
            }
        }
    }
}
