package com.swoag.logalong.views;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */


import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import com.swoag.logalong.LApp;
import com.swoag.logalong.R;
import com.swoag.logalong.utils.LOnClickListener;
import com.swoag.logalong.utils.LViewUtils;

public class LDollarAmountPicker extends Dialog {
    private static final String TAG = LDollarAmountPicker.class.getSimpleName();

    private Context context;
    private double value;
    private LDollarAmountPickerItf callback;

    private View view0, view1, view2, view3, view4, view5, view6, view7, view8, view9;
    private View viewDot, viewBackSpace, viewPlus, viewMinus, viewMultiply, viewDivide;
    private View viewClear, viewCancel, viewSave;
    private TextView valueTV;

    private String inputString = "";
    private double lastValue;
    private int lastValueEnd;
    private int mathOperator;
    private MyClickListener myClickListener;

    public interface LDollarAmountPickerItf {
        public void onDollarAmountPickerExit(double value, boolean save);
    }

    public LDollarAmountPicker(Context context, double value, LDollarAmountPickerItf callback) {
        super(context, android.R.style.Theme_Translucent_NoTitleBar);

        this.context = context;
        this.value = value;
        this.callback = callback;
        myClickListener = new MyClickListener();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dollar_amount_picker);

        view0 = setViewListener(R.id.b0);
        view1 = setViewListener(R.id.b1);
        view2 = setViewListener(R.id.b2);
        view3 = setViewListener(R.id.b3);
        view4 = setViewListener(R.id.b4);
        view5 = setViewListener(R.id.b5);
        view6 = setViewListener(R.id.b6);
        view7 = setViewListener(R.id.b7);
        view8 = setViewListener(R.id.b8);
        view9 = setViewListener(R.id.b9);
        viewBackSpace = setViewListener(R.id.backspace);
        viewDot = setViewListener(R.id.dot);
        viewPlus = setViewListener(R.id.plus);
        viewMinus = setViewListener(R.id.minus);
        viewMultiply = setViewListener(R.id.multiply);
        viewDivide = setViewListener(R.id.divide);
        viewClear = setViewListener(R.id.clear);
        viewCancel = setViewListener(R.id.cancel);
        viewSave = setViewListener(R.id.ok);

        valueTV = (TextView) findViewById(R.id.value);
        clearInputString();
        inputString = value2string(value);
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
        viewSave = null;
        valueTV = null;
    }

    private class MyClickListener extends LOnClickListener {
        @Override
        public void onClicked(View v) {
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

                case R.id.cancel:
                    callback.onDollarAmountPickerExit(0, false);
                    destroy();
                    dismiss();
                    break;

                case R.id.ok:
                    saveLog();
                    break;

                default:
                    break;
            }
        }
    }

    private View setViewListener(int id) {
        View view = findViewById(id);
        view.setOnClickListener(myClickListener);
        return view;
    }

    private void enableOk(boolean yes) {
        viewSave.setEnabled(yes);
        if (yes) LViewUtils.setAlpha((View) viewSave, 1.0f);
        else LViewUtils.setAlpha((View) viewSave, 0.5f);
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
        ((Button) viewSave).setText(LApp.ctx.getString(android.R.string.ok));

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
                    ((Button) viewSave).setText(LApp.ctx.getString(android.R.string.ok));
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
        ((Button) viewSave).setText("=");
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
            callback.onDollarAmountPickerExit(string2value(inputString), true);
            dismiss();
        }
    }
}
