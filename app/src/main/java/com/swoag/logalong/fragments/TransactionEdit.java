package com.swoag.logalong.fragments;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.swoag.logalong.LApp;
import com.swoag.logalong.LFragment;
import com.swoag.logalong.R;
import com.swoag.logalong.entities.LItem;
import com.swoag.logalong.utils.DBAccess;
import com.swoag.logalong.utils.LViewUtils;

public class TransactionEdit implements View.OnClickListener {
    private Activity activity;
    private View rootView;
    private LItem item;
    private LItem savedItem;
    private TransitionEditItf callback;

    private View view0, view1, view2, view3, view4, view5, view6, view7, view8, view9;
    private View viewDot, viewBackSpace, viewPlus, viewMinus, viewMultiply, viewDivide;
    private View viewClear, viewBack, viewCancel, viewOk;
    private TextView valueTV;

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

    public TransactionEdit(Activity activity, View rootView, LItem item, TransitionEditItf callback) {
        this.activity = activity;
        this.rootView = rootView;
        this.item = item;
        this.callback = callback;

        this.savedItem = new LItem(item);

        create();
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

        valueTV = (TextView) rootView.findViewById(R.id.value);

        clearInputString();
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

        valueTV = null;
        savedItem = null;
    }

    @Override
    public void onClick(View v) {
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
        ((Button)viewOk).setText(LApp.ctx.getString(android.R.string.ok));

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
                    ((Button)viewOk).setText(LApp.ctx.getString(android.R.string.ok));
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

        if (getValue(tmp) != 0) {
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
        lastValue = getValue(inputString.substring(0, lastValueEnd - 1));
        enableMath(false);
        ((Button)viewOk).setText("=");
        enableDot(true);

        mathOperator = operator;
        valueTV.setText(inputString);
    }

    private double getValue(String str) {
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
            double curValue = getValue(inputString.substring(lastValueEnd, inputString.length()));
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

    private void doSaveLog() {
        LItem item = new LItem();
        item.setCategory(1);
        item.setVendor(1);
        item.setTag(-1);
        item.setValue(getValue(inputString));
        DBAccess.addItem(item);

        clearInputString();

        boolean changed = !item.isEqual(savedItem);
        destroy();
        callback.onTransactionEditExit(TransitionEditItf.EXIT_OK, changed);
    }
}
