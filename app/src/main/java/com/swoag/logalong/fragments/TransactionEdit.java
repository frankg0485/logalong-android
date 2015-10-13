package com.swoag.logalong.fragments;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;

import com.swoag.logalong.LApp;
import com.swoag.logalong.R;
import com.swoag.logalong.entities.LTransaction;
import com.swoag.logalong.utils.DBAccess;
import com.swoag.logalong.utils.DBAccount;
import com.swoag.logalong.utils.DBCategory;
import com.swoag.logalong.utils.DBHelper;
import com.swoag.logalong.utils.DBTag;
import com.swoag.logalong.utils.DBVendor;
import com.swoag.logalong.utils.LLog;
import com.swoag.logalong.utils.LViewUtils;
import com.swoag.logalong.views.LDollarAmountPicker;
import com.swoag.logalong.views.LSelectionDialog;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class TransactionEdit implements View.OnClickListener, LSelectionDialog.OnSelectionDialogItf,
        DatePickerDialog.OnDateSetListener, LDollarAmountPicker.LDollarAmountPickerItf {
    private static final String TAG = TransactionEdit.class.getSimpleName();

    private Activity activity;
    private View rootView;
    private boolean bCreate;
    private LTransaction item;
    private LTransaction savedItem;
    private TransitionEditItf callback;

    private View viewDiscard, viewSave, viewFrom, viewTo, viewAccount2, viewCategory, viewVendor, viewTag;
    private TextView amountTV, accountTV, account2TV, categoryTV, vendorTV, tagTV;

    private TextView dateTV;
    private EditText noteET;

    private LSelectionDialog mSelectionDialog;
    private boolean firstTimeAmountPicker = true;

    public interface TransitionEditItf {
        public static final int EXIT_DELETE = 10;
        public static final int EXIT_OK = 20;
        public static final int EXIT_CANCEL = 30;

        public void onTransactionEditExit(int action, boolean changed);
    }

    public TransactionEdit(Activity activity, View rootView, LTransaction item, boolean bCreate,
                           TransitionEditItf callback) {
        this.activity = activity;
        this.rootView = rootView;
        this.item = item;
        this.callback = callback;

        this.savedItem = new LTransaction(item);

        this.bCreate = bCreate;
        create();
    }

    private void updateItemDisplay() {
        dateTV.setText(new SimpleDateFormat("MMM d, yyy").format(item.getTimeStamp()));

        accountTV.setTypeface(null, item.getAccount() <= 0 ? Typeface.NORMAL : Typeface.BOLD);
        accountTV.setText(DBAccess.getAccountNameById(item.getAccount()));

        if (item.getType() == LTransaction.TRANSACTION_TYPE_TRANSFER) {
            account2TV.setTypeface(null, item.getVendor() <= 0 ? Typeface.NORMAL : Typeface.BOLD);
            account2TV.setText(DBAccess.getAccountNameById(item.getVendor()));
        } else {
            categoryTV.setTypeface(null, item.getCategory() <= 0 ? Typeface.NORMAL : Typeface.BOLD);
            categoryTV.setText(DBAccess.getCategoryNameById(item.getCategory()));

            vendorTV.setTypeface(null, item.getVendor() <= 0 ? Typeface.NORMAL : Typeface.BOLD);
            if (item.getType() == LTransaction.TRANSACTION_TYPE_EXPENSE) {
                vendorTV.setHint(activity.getString(R.string.hint_payee_not_specified));
            } else if (item.getType() == LTransaction.TRANSACTION_TYPE_INCOME) {
                vendorTV.setHint(activity.getString(R.string.hint_payer_not_specified));
            }
            vendorTV.setText(DBAccess.getVendorNameById(item.getVendor()));

            tagTV.setTypeface(null, item.getTag() <= 0 ? Typeface.NORMAL : Typeface.BOLD);
            tagTV.setText(DBAccess.getTagNameById(item.getTag()));
        }
        updateOkDisplay();
    }

    private void updateOkDisplay() {
        if ((item.getAccount() > 0) && (item.getValue() > 0)
                && ((item.getType() != LTransaction.TRANSACTION_TYPE_TRANSFER) ||
                (item.getVendor() > 0 && item.getVendor() != item.getAccount()))) {
            enableOk(true);
        } else {
            enableOk(false);
        }
    }

    private void create() {
        setViewListener(rootView, R.id.goback);
        setViewListener(rootView, R.id.back);

        viewSave = setViewListener(rootView, R.id.save);

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

        noteET = (EditText) setViewListener(rootView, R.id.noteEditText);
        noteET.setText(item.getNote());

        clearInputString();
        String inputString = value2string(item.getValue());

        if (item.getType() == LTransaction.TRANSACTION_TYPE_TRANSFER) {
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
            LDollarAmountPicker picker = new LDollarAmountPicker(activity, item.getValue(), this);
            picker.show();
        } else {
            viewDiscard = setViewListener(rootView, R.id.discard);
            viewDiscard.setVisibility(View.VISIBLE);
        }
    }

    private void destroy() {
        viewSave = null;
        viewDiscard = null;

        viewCategory = null;
        viewVendor = null;
        viewTag = null;
        viewAccount2 = null;

        noteET = null;
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
                destroy();
                callback.onTransactionEditExit(TransitionEditItf.EXIT_CANCEL, false);
            }
        }

        if (firstTimeAmountPicker) firstTimeAmountPicker = false;
        updateOkDisplay();
    }

    @Override
    public void onClick(View v) {
        hideIME();
        switch (v.getId()) {
            case R.id.tvDate:
                final Calendar c = Calendar.getInstance();
                DatePickerDialog datePickerDialog = new DatePickerDialog(activity, android.R.style.Theme_Holo_Light_Dialog_NoActionBar,
                        this, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
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

            case R.id.amountRow:
                LDollarAmountPicker picker = new LDollarAmountPicker(activity, item.getValue(), this);
                picker.show();
                break;

            case R.id.account2Row:
                try {
                    ids[9] = R.string.select_account;
                    mSelectionDialog = new LSelectionDialog
                            (activity, this, ids,
                                    DBHelper.TABLE_ACCOUNT_NAME,
                                    DBHelper.TABLE_COLUMN_NAME, DBAccess.getAccountIndexById(item.getVendor()), DLG_ID_ACCOUNT2);
                    mSelectionDialog.show();
                    mSelectionDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
                } catch (Exception e) {
                }
                break;

            case R.id.accountRow:
                try {
                    ids[9] = R.string.select_account;
                    mSelectionDialog = new LSelectionDialog
                            (activity, this, ids,
                                    DBHelper.TABLE_ACCOUNT_NAME,
                                    DBHelper.TABLE_COLUMN_NAME, DBAccess.getAccountIndexById(item.getAccount()), DLG_ID_ACCOUNT);
                    mSelectionDialog.show();
                    mSelectionDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
                } catch (Exception e) {
                }
                break;

            case R.id.categoryRow:
                try {
                    ids[9] = R.string.select_category;
                    mSelectionDialog = new LSelectionDialog
                            (activity, this, ids,
                                    DBHelper.TABLE_CATEGORY_NAME,
                                    DBHelper.TABLE_COLUMN_NAME, DBAccess.getCategoryIndexById(item.getCategory()), DLG_ID_CATEGORY);
                    mSelectionDialog.show();
                    mSelectionDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
                } catch (Exception e) {
                }
                break;
            case R.id.vendorRow:
                try {
                    ids[9] = R.string.select_vendor;
                    mSelectionDialog = new LSelectionDialog
                            (activity, this, ids,
                                    DBHelper.TABLE_VENDOR_NAME,
                                    DBHelper.TABLE_COLUMN_NAME, DBAccess.getVendorIndexById(item.getVendor()), DLG_ID_VENDOR);
                    mSelectionDialog.show();
                    mSelectionDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
                } catch (Exception e) {
                }
                break;
            case R.id.tagRow:
                try {
                    ids[9] = R.string.select_tag;
                    mSelectionDialog = new LSelectionDialog
                            (activity, this, ids,
                                    DBHelper.TABLE_TAG_NAME,
                                    DBHelper.TABLE_COLUMN_NAME, DBAccess.getTagIndexById(item.getTag()), DLG_ID_TAG);
                    mSelectionDialog.show();
                    mSelectionDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
                } catch (Exception e) {
                }
                break;
            case R.id.discard:
                destroy();
                callback.onTransactionEditExit(TransitionEditItf.EXIT_DELETE, false);
                break;

            case R.id.goback:
            case R.id.back:
                callback.onTransactionEditExit(TransitionEditItf.EXIT_CANCEL, false);
                destroy();
                break;

            case R.id.save:
                saveLog();
                break;

            default:
                break;
        }
    }

    public void dismiss() {
        callback.onTransactionEditExit(TransitionEditItf.EXIT_CANCEL, false);
        destroy();
    }

    @Override
    public Cursor onGetCursor(String table, String column) {
        if (table.contentEquals(DBHelper.TABLE_ACCOUNT_NAME))
            return DBAccount.getCursorSortedBy(DBHelper.TABLE_COLUMN_NAME);
        else if (table.contentEquals(DBHelper.TABLE_CATEGORY_NAME))
            return DBCategory.getCursorSortedBy(DBHelper.TABLE_COLUMN_NAME);
        else if (table.contentEquals(DBHelper.TABLE_VENDOR_NAME))
            return DBVendor.getCursorSortedBy(DBHelper.TABLE_COLUMN_NAME);
        else if (table.contentEquals(DBHelper.TABLE_TAG_NAME))
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
                if (selectedId == item.getVendor() && item.getType() == LTransaction.TRANSACTION_TYPE_TRANSFER) {
                    selectedId = -1;
                }
                item.setAccount(selectedId);
                updateOkDisplay();
                break;
            case DLG_ID_ACCOUNT2:
                if (selectedId == item.getAccount() && item.getType() == LTransaction.TRANSACTION_TYPE_TRANSFER) {
                    selectedId = -1;
                }
                item.setVendor(selectedId);
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
            R.string.select_account};

    private View setViewListener(View v, int id) {
        View view = v.findViewById(id);
        view.setOnClickListener(this);
        return view;
    }

    private void enableOk(boolean yes) {
        viewSave.setEnabled(yes);
        if (yes) LViewUtils.setAlpha((View) viewSave, 1.0f);
        else LViewUtils.setAlpha((View) viewSave, 0.5f);
    }

    private void clearInputString() {
        amountTV.setText("0.0");
        enableOk(false);
    }

    private String value2string(double value) {
        String str = "";
        if (value != 0) {
            str = String.format("%.2f", value);
        }
        return str;
    }

    private void saveLog() {
        item.setNote(noteET.getText().toString());
        boolean changed = !item.isEqual(savedItem);
        if (changed) item.setTimeStampLast(System.currentTimeMillis());
        destroy();
        callback.onTransactionEditExit(TransitionEditItf.EXIT_OK, changed);
    }

    private void hideIME() {
        try {
            InputMethodManager inputManager = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.hideSoftInputFromWindow(noteET.getWindowToken(), 0);
            noteET.setCursorVisible(false);
        } catch (Exception e) {
        }
    }
}
