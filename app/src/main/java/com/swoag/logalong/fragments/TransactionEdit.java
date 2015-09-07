package com.swoag.logalong.fragments;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.swoag.logalong.LApp;
import com.swoag.logalong.R;
import com.swoag.logalong.entities.LTransaction;
import com.swoag.logalong.utils.DBAccess;
import com.swoag.logalong.utils.DBHelper;
import com.swoag.logalong.utils.LLog;
import com.swoag.logalong.utils.LViewUtils;
import com.swoag.logalong.views.LSelectionDialog;

import java.text.SimpleDateFormat;

public class TransactionEdit implements View.OnClickListener, LSelectionDialog.OnSelectionDialogItf {
    private static final String TAG = TransactionEdit.class.getSimpleName();

    private Activity activity;
    private View rootView;
    private boolean bCreate;
    private LTransaction item;
    private LTransaction savedItem;
    private TransitionEditItf callback;

    private View view0, view1, view2, view3, view4, view5, view6, view7, view8, view9;
    private View viewDot, viewBackSpace, viewPlus, viewMinus, viewMultiply, viewDivide;
    private View viewAccount, viewCategory, viewVendor, viewTag;
    private View viewClear, viewBack, viewDiscard, viewCancel, viewOk;
    private TextView valueTV, accountTV, categoryTV, vendorTV, tagTV, dateTV;
    private EditText noteET;

    private LSelectionDialog mSelectionDialog;

    private String inputString = "";
    private double lastValue;
    private int lastValueEnd;
    private int mathOperator;

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
        accountTV.setText(DBAccess.getAccountNameById(item.getAccount()));
        categoryTV.setText(DBAccess.getCategoryNameById(item.getCategory()));

        String tmp = DBAccess.getVendorNameById(item.getVendor());
        if (tmp.isEmpty()) {
            vendorTV.setText(activity.getString(R.string.unspecified_vendor));
        } else {
            vendorTV.setText(tmp);
        }

        tmp = DBAccess.getTagNameById(item.getTag());
        if (tmp.isEmpty()) {
            tagTV.setText(activity.getString(R.string.unspecified_tag));
        } else {
            tagTV.setText(tmp);
        }
    }

    private void create() {
        view0 = setViewListener(rootView, R.id.b0);
        view1 = setViewListener(rootView, R.id.b1);
        view2 = setViewListener(rootView, R.id.b2);
        view3 = setViewListener(rootView, R.id.b3);
        view4 = setViewListener(rootView, R.id.b4);
        view5 = setViewListener(rootView, R.id.b5);
        view6 = setViewListener(rootView, R.id.b6);
        view7 = setViewListener(rootView, R.id.b7);
        view8 = setViewListener(rootView, R.id.b8);
        view9 = setViewListener(rootView, R.id.b9);
        viewBackSpace = setViewListener(rootView, R.id.backspace);
        viewDot = setViewListener(rootView, R.id.dot);
        viewPlus = setViewListener(rootView, R.id.plus);
        viewMinus = setViewListener(rootView, R.id.minus);
        viewMultiply = setViewListener(rootView, R.id.multiply);
        viewDivide = setViewListener(rootView, R.id.divide);
        viewClear = setViewListener(rootView, R.id.clear);
        viewCancel = setViewListener(rootView, R.id.cancel);
        viewBack = setViewListener(rootView, R.id.back);
        viewOk = setViewListener(rootView, R.id.ok);
        viewAccount = setViewListener(rootView, R.id.accountRow);
        viewCategory = setViewListener(rootView, R.id.categoryRow);
        viewVendor = setViewListener(rootView, R.id.vendorRow);
        viewTag = setViewListener(rootView, R.id.tagRow);

        if (!bCreate) {
            viewDiscard = setViewListener(rootView, R.id.discard);
            viewDiscard.setVisibility(View.VISIBLE);
        }

        dateTV = (TextView) setViewListener(rootView, R.id.tvDate);
        accountTV = (TextView) rootView.findViewById(R.id.tvAccount);
        categoryTV = (TextView) rootView.findViewById(R.id.tvCategory);
        vendorTV = (TextView) rootView.findViewById(R.id.tvVendor);
        tagTV = (TextView) rootView.findViewById(R.id.tvTag);
        updateItemDisplay();

        noteET = (EditText) setViewListener(rootView, R.id.noteEditText);

        valueTV = (TextView) rootView.findViewById(R.id.value);
        clearInputString();
        inputString = value2string(item.getValue());
        if (inputString.isEmpty()) {
            valueTV.setText("0.0");
        } else {
            valueTV.setText(inputString);
            enableDot(false);
            enableOk(true);
        }
    }

    private void destroy() {
        view0 = null;
        view1 = null;
        view2 = null;
        view3 = null;
        view4 = null;
        view5 = null;
        view6 = null;
        view7 = null;
        view8 = null;
        view9 = null;
        viewBackSpace = null;
        viewDot = null;
        viewPlus = null;
        viewMultiply = null;
        viewDivide = null;
        viewClear = null;
        viewCancel = null;
        viewBack = null;
        viewOk = null;
        viewAccount = null;
        viewCategory = null;
        viewVendor = null;
        viewTag = null;
        viewDiscard = null;

        noteET = null;
        valueTV = null;
        accountTV = null;
        categoryTV = null;
        vendorTV = null;
        tagTV = null;
        dateTV = null;
        savedItem = null;
    }

    @Override
    public void onClick(View v) {
        hideIME();
        switch (v.getId()) {
            case R.id.b0:
                appendToString(0);
                break;
            case R.id.b1:
                appendToString(1);
                break;
            case R.id.b2:
                appendToString(2);
                break;
            case R.id.b3:
                appendToString(3);
                break;
            case R.id.b4:
                appendToString(4);
                break;
            case R.id.b5:
                appendToString(5);
                break;
            case R.id.b6:
                appendToString(6);
                break;
            case R.id.b7:
                appendToString(7);
                break;
            case R.id.b8:
                appendToString(8);
                break;
            case R.id.b9:
                appendToString(9);
                break;
            case R.id.dot:
                appendToString(-1);
                break;
            case R.id.backspace:
                removeLastDigitFromString();
                break;
            case R.id.clear:
                clearInputString();
                break;
            case R.id.plus:
                doMathToString(0);
                break;
            case R.id.minus:
                doMathToString(1);
                break;
            case R.id.multiply:
                doMathToString(2);
                break;
            case R.id.divide:
                doMathToString(3);
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
                    LLog.w(TAG, "unable to open phone database");
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
                    LLog.w(TAG, "unable to open phone database");
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
                    LLog.w(TAG, "unable to open phone database");
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
                    LLog.w(TAG, "unable to open phone database");
                }
                break;
            case R.id.discard:
                destroy();
                callback.onTransactionEditExit(TransitionEditItf.EXIT_DELETE, false);
                break;

            case R.id.back:
            case R.id.cancel:
                destroy();
                callback.onTransactionEditExit(TransitionEditItf.EXIT_CANCEL, false);
                break;

            case R.id.ok:
                saveLog();
                break;

            default:
                break;
        }
    }

    public void dismiss() {
        destroy();
        callback.onTransactionEditExit(TransitionEditItf.EXIT_CANCEL, false);
    }

    @Override
    public Cursor onGetCursor(String table, String column) {
        if (table.contentEquals(DBHelper.TABLE_ACCOUNT_NAME))
            return DBAccess.getAllAccountsCursor();
        else if (table.contentEquals(DBHelper.TABLE_CATEGORY_NAME))
            return DBAccess.getAllCategoriesCursor();
        else if (table.contentEquals(DBHelper.TABLE_VENDOR_NAME))
            return DBAccess.getAllVendorsCursor();
        else if (table.contentEquals(DBHelper.TABLE_TAG_NAME)) return DBAccess.getAllTagsCursor();
        return null;
    }

    private static final int DLG_ID_ACCOUNT = 10;
    private static final int DLG_ID_CATEGORY = 20;
    private static final int DLG_ID_VENDOR = 30;
    private static final int DLG_ID_TAG = 40;

    @Override
    public void onSelectionDialogExit(int dlgId, long selectedId) {
        switch (dlgId) {
            case DLG_ID_ACCOUNT:
                item.setAccount(selectedId);
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
        viewOk.setEnabled(yes);
        if (yes) LViewUtils.setAlpha((View) viewOk, 1.0f);
        else LViewUtils.setAlpha((View) viewOk, 0.5f);
    }

    private void enableMath(boolean yes) {
        viewPlus.setEnabled(yes);
        viewMinus.setEnabled(yes);
        viewMultiply.setEnabled(yes);
        viewDivide.setEnabled(yes);

        if (yes) {
            LViewUtils.setAlpha((View) viewPlus, 1.0f);
            LViewUtils.setAlpha((View) viewMinus, 1.0f);
            LViewUtils.setAlpha((View) viewMultiply, 1.0f);
            LViewUtils.setAlpha((View) viewDivide, 1.0f);
        } else {
            LViewUtils.setAlpha((View) viewPlus, 0.5f);
            LViewUtils.setAlpha((View) viewMinus, 0.5f);
            LViewUtils.setAlpha((View) viewMultiply, 0.5f);
            LViewUtils.setAlpha((View) viewDivide, 0.5f);
        }
    }

    private void enableDot(boolean yes) {
        viewDot.setEnabled(yes);
        if (yes) LViewUtils.setAlpha((View) viewDot, 1.0f);
        else LViewUtils.setAlpha((View) viewDot, 0.5f);
    }

    private void clearInputString() {
        inputString = "";
        valueTV.setText("0.0");
        ((Button) viewOk).setText(LApp.ctx.getString(android.R.string.ok));

        mathOperator = -1;
        lastValue = 0;
        enableOk(false);
        enableDot(true);
        enableMath(false);
    }

    private void appendToString(int digit) {
        if (digit == -1) {
            if (inputString.isEmpty()) {
                inputString = "0.";
            } else {
                inputString += '.';
            }
            enableDot(false);
        } else {
            if (inputString.isEmpty() && digit == 0) {
                inputString = "0.";
                enableDot(false);
            } else {
                inputString = inputString + digit;
            }
        }

        validateStringAndUpdateDisplay();
        valueTV.setText(inputString);
    }

    private void removeLastDigitFromString() {
        if (inputString.isEmpty()) {
            clearInputString();
        } else {
            char lastDigit = inputString.charAt(inputString.length() - 1);
            if (lastDigit == '.') {
                enableDot(true);
            } else {
                if (lastDigit == '+' || lastDigit == '-' || lastDigit == '*' || lastDigit == '/') {
                    mathOperator = -1;
                    ((Button) viewOk).setText(LApp.ctx.getString(android.R.string.ok));
                }
            }

            inputString = inputString.substring(0, inputString.length() - 1);
            int len = inputString.length();
            while (true && len > 0) {
                lastDigit = inputString.charAt(len - 1);
                if ((!Character.isDigit(lastDigit)) && lastDigit != '.'
                        && lastDigit != '+'
                        && lastDigit != '-'
                        && lastDigit != '*'
                        && lastDigit != '/') {
                    inputString = inputString.substring(0, len - 1);
                    len--;
                } else break;
            }
            if (len == 0) clearInputString();
            else {
                validateStringAndUpdateDisplay();
                valueTV.setText(inputString);
            }
        }
    }

    private String validateString(String str) {
        if (str.isEmpty()) return str;
        char ch0 = str.charAt(0);
        if (ch0 == '.') return "0.";
        if (str.length() == 1) {
            if (ch0 == '-') return "0.0";
            else return str;
        }

        char ch1 = str.charAt(1);
        if (ch0 == '0' && ch1 != '.') return str.substring(1);

        return str;
    }

    private void validateStringAndUpdateDisplay() {
        String tmp = (mathOperator >= 0) ? inputString.substring(lastValueEnd) : inputString;
        tmp = validateString(tmp);
        if (mathOperator < 0) inputString = tmp;
        else inputString = inputString.substring(0, lastValueEnd) + tmp;

        if (string2value(tmp) != 0) {
            enableOk(true);
            enableMath(true);
        } else {
            if (mathOperator == 3 || mathOperator < 0) enableOk(false);
            if (mathOperator < 0) enableMath(false);
        }
    }

    private void doMathToString(int operator) {
        if (mathOperator >= 0) {
            saveLog();
        }

        switch (operator) {
            case 0:
                inputString += '+';
                break;
            case 1:
                inputString += '-';
                break;
            case 2:
                inputString += '*';
                break;
            case 3:
                inputString += '/';
                break;
        }

        lastValueEnd = inputString.length();
        lastValue = string2value(inputString.substring(0, lastValueEnd - 1));
        enableMath(false);
        ((Button) viewOk).setText("=");
        enableDot(true);

        mathOperator = operator;
        valueTV.setText(inputString);
    }

    private String value2string(double value) {
        String str = "";
        if (value != 0) {
            str = String.format("%.2f", value);
        }
        return str;
    }

    private double string2value(String str) {
        if (str.isEmpty()) return 0f;

        char lastDigit = str.charAt(str.length() - 1);
        if (lastDigit == '.'
                || lastDigit == '+'
                || lastDigit == '-'
                || lastDigit == '*'
                || lastDigit == '/') {
            return (Double.parseDouble(str.substring(0, str.length() - 1)));
        }
        return Double.parseDouble(str);
    }

    private void saveLog() {
        if (mathOperator >= 0) {
            double curValue = string2value(inputString.substring(lastValueEnd, inputString.length()));
            switch (mathOperator) {
                case 0:
                    curValue = lastValue + curValue;
                    break;
                case 1:
                    curValue = lastValue - curValue;
                    break;
                case 2:
                    curValue = lastValue * curValue;
                    break;
                case 3:
                    if (curValue != 0) curValue = lastValue / curValue;
                    break;
            }

            clearInputString();
            inputString = String.format("%.2f", curValue);

            validateStringAndUpdateDisplay();
            valueTV.setText(inputString);
            enableDot(false); //math result always has '.' in it.
        } else {
            doSaveLog();
        }
    }

    private void hideIME() {
        try {
            InputMethodManager inputManager = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.hideSoftInputFromWindow(noteET.getWindowToken(), 0);
            noteET.setCursorVisible(false);
        } catch (Exception e) {
        }
    }

    private void doSaveLog() {
        item.setValue(string2value(inputString));

        boolean changed = !item.isEqual(savedItem);
        destroy();
        callback.onTransactionEditExit(TransitionEditItf.EXIT_OK, changed);
    }
}
