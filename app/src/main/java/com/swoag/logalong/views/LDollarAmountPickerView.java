package com.swoag.logalong.views;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */


import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.TextView;

import com.swoag.logalong.R;
import com.swoag.logalong.utils.LLog;
import com.swoag.logalong.utils.LOnClickListener;
import com.swoag.logalong.utils.LViewUtils;

public class LDollarAmountPickerView implements View.OnClickListener {
    private static final String TAG = LDollarAmountPickerView.class.getSimpleName();
    public static final int VALUE_COLOR_RED = 10;
    public static final int VALUE_COLOR_GREEN = 20;
    public static final int VALUE_COLOR_BLUE = 30;
    public static final int VALUE_COLOR_NEUTRAL = 40;

    private static final float VIEW_DIM_ALPHA_VALUE = 0.8f;

    private View rootView;
    private double value;
    private LDollarAmountPickerViewItf callback;

    private View view0, view1, view2, view3, view4, view5, view6, view7, view8, view9;
    private View viewDot, viewBackSpace, viewPlus, viewMinus, viewMultiply, viewDivide;
    private View viewClear, viewCancel, viewSave;
    private TextView valueTV;
    private View pickerBgdView;
    private int colorCode;

    private String inputString = "";
    private double lastValue;
    private int lastValueEnd;
    private int mathOperator;
    private MyClickListener myClickListener;

    public interface LDollarAmountPickerViewItf {
        public void onDollarAmountPickerExit(double value, boolean save);
    }

    public LDollarAmountPickerView(View rootView, double value, int colorCode, LDollarAmountPickerViewItf callback) {
        this.rootView = rootView;
        this.value = value;
        this.colorCode = colorCode;
        this.callback = callback;
        myClickListener = new MyClickListener();

        pickerBgdView = rootView.findViewById(R.id.pickerBgdView);
        try {
            GradientDrawable drawable = (GradientDrawable) pickerBgdView.getBackground();
            switch (colorCode) {
                case VALUE_COLOR_RED:
                    drawable.setStroke(1, 0xffff0000);
                    break;
                case VALUE_COLOR_GREEN:
                    drawable.setStroke(1, 0xff00ff00);
                    break;
                default:
                    drawable.setStroke(1, 0xff0000ff);
                    break;
                case VALUE_COLOR_NEUTRAL:
                    drawable.setStroke(2, 0xff808080);
                    break;
            }
        } catch (Exception e) {
            LLog.w(TAG, "fail to set background color");
        }

        view0 = setViewListener(R.id.b0, false);
        view1 = setViewListener(R.id.b1, false);
        view2 = setViewListener(R.id.b2, false);
        view3 = setViewListener(R.id.b3, false);
        view4 = setViewListener(R.id.b4, false);
        view5 = setViewListener(R.id.b5, false);
        view6 = setViewListener(R.id.b6, false);
        view7 = setViewListener(R.id.b7, false);
        view8 = setViewListener(R.id.b8, false);
        view9 = setViewListener(R.id.b9, false);
        viewBackSpace = setViewListener(R.id.backspace, false);
        viewDot = setViewListener(R.id.dot, true);
        viewPlus = setViewListener(R.id.plus, true);
        viewMinus = setViewListener(R.id.minus, true);
        viewMultiply = setViewListener(R.id.multiply, true);
        viewDivide = setViewListener(R.id.divide, true);
        viewClear = setViewListener(R.id.clear, true);
        viewCancel = setViewListener(R.id.cancel, true);
        viewSave = setViewListener(R.id.ok, true);

        valueTV = (TextView) rootView.findViewById(R.id.value);

        //TODO: pass in activity context to access base_red/base_green/base_blue color codes.
        switch (colorCode) {
            case VALUE_COLOR_RED:
                valueTV.setTextColor(0xffbb4238);
                break;
            case VALUE_COLOR_GREEN:
                valueTV.setTextColor(0xff42bb38);
                break;
            default:
                valueTV.setTextColor(0xff4238bb);
                break;
            case VALUE_COLOR_NEUTRAL:
                valueTV.setTextColor(0xff303030);
                break;
        }

        clearInputString();
        inputString = value2string(value);
        if (TextUtils.isEmpty(inputString)) {
            valueTV.setText("0.0");
        } else {
            valueTV.setText(inputString);
            enableDot(false);
            enableOk(true);
        }
    }

    public void onDestroy() {
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

    public void onBackPressed() {
        callback.onDollarAmountPickerExit(0, false);
    }

    private class MyClickListener extends LOnClickListener {
        @Override
        public void onClicked(View v) {
            switch (v.getId()) {
                case R.id.dot:
                    appendToString(-1);
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
                    break;

                case R.id.ok:
                    saveLog();
                    break;

                default:
                    break;
            }
        }
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
            case R.id.backspace:
                removeLastDigitFromString();
                break;
            default:
                break;
        }
    }

    private View setViewListener(int id, boolean myListener) {
        View view = rootView.findViewById(id);
        view.setOnClickListener(myListener ? myClickListener : this);
        return view;
    }

    private void enableOk(boolean yes) {
        viewSave.setEnabled(yes);
        if (yes) LViewUtils.setAlpha((View) viewSave, 1.0f);
        else LViewUtils.setAlpha((View) viewSave, VIEW_DIM_ALPHA_VALUE);
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
            LViewUtils.setAlpha((View) viewPlus, VIEW_DIM_ALPHA_VALUE);
            LViewUtils.setAlpha((View) viewMinus, VIEW_DIM_ALPHA_VALUE);
            LViewUtils.setAlpha((View) viewMultiply, VIEW_DIM_ALPHA_VALUE);
            LViewUtils.setAlpha((View) viewDivide, VIEW_DIM_ALPHA_VALUE);
        }
    }

    private void enableDot(boolean yes) {
        viewDot.setEnabled(yes);
        if (yes) LViewUtils.setAlpha((View) viewDot, 1.0f);
        else LViewUtils.setAlpha((View) viewDot, VIEW_DIM_ALPHA_VALUE);
    }

    private void clearInputString() {
        inputString = "";
        valueTV.setText("0.0");
        ((ImageButton) viewSave).setImageResource(R.drawable.ic_action_accept);

        mathOperator = -1;
        lastValue = 0;
        enableOk(false);
        enableDot(true);
        enableMath(false);
    }

    private void appendToString(int digit) {
        if (digit == -1) {
            if (TextUtils.isEmpty(inputString)) {
                inputString = "0.";
            } else {
                inputString += '.';
            }
            enableDot(false);
        } else {
            if (TextUtils.isEmpty(inputString) && digit == 0) {
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
        if (TextUtils.isEmpty(inputString)) {
            clearInputString();
        } else {
            char lastDigit = inputString.charAt(inputString.length() - 1);
            if (lastDigit == '.') {
                enableDot(true);
            } else {
                if (lastDigit == '+' || lastDigit == '-' || lastDigit == '*' || lastDigit == '/') {
                    mathOperator = -1;
                    ((ImageButton) viewSave).setImageResource(R.drawable.ic_action_accept);
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
        if (TextUtils.isEmpty(str)) return str;
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
        ((ImageButton) viewSave).setImageResource(R.drawable.ic_action_equal);
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
        if (TextUtils.isEmpty(str)) return 0f;

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
        }
    }
}
