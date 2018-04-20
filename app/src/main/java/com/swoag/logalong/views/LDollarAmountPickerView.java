package com.swoag.logalong.views;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */


import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.swoag.logalong.R;
import com.swoag.logalong.utils.LLog;
import com.swoag.logalong.utils.LOnClickListener;
import com.swoag.logalong.utils.LViewUtils;

public class LDollarAmountPickerView implements View.OnClickListener {
    private static final String TAG = LDollarAmountPickerView.class.getSimpleName();
    private static final int MAX_INPUT_LENGHT = 16;
    private static final int MAX_DECIMAL_DIGITS = 3;
    private static final double MAX_VALUE = 999999999999f;
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
    private boolean allowZero;
    private boolean pickerActive;

    private String inputString = "";
    private int firstValueEnd = 0;
    private char mathOperator;

    private MyClickListener myClickListener;

    public interface LDollarAmountPickerViewItf {
        public void onDollarAmountPickerExit(double value, boolean save);
    }

    private enum State {
        INIT_NO_INPUT,
        INTEGER_1,
        DECIMAL_1,
        DECIMAL_1_0,
        MATH_INIT,
        INTEGER_2,
        DECIMAL_2
    }

    private enum InputStringLastBit {
        EMPTY,
        DOT,
        MATH,
        DIGIT
    }

    private State state = State.INIT_NO_INPUT;

    public LDollarAmountPickerView(View rootView, double value, int colorCode, boolean allowZero, LDollarAmountPickerViewItf callback) {
        this.rootView = rootView;
        this.value = value;
        this.colorCode = colorCode;
        this.allowZero = allowZero;
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

        if (value < 0.01) {
            inputString = "";
            state = initNoInputState();
        } else {
            inputString = value2string(value);
            state = initDecimal1State();
        }
        pickerActive = true;
    }

    public void onDestroy() {
        pickerActive = false;

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
            if (!pickerActive) return;

            if (R.id.cancel == v.getId()) {
                callback.onDollarAmountPickerExit(0, false);
            } else if (R.id.clear == v.getId()) {
                inputString = "";
                state = initNoInputState();
            } else {
                handleButtonPress(v.getId());
            }
        }
    }

    @Override
    public void onClick(View v) {
        if (!pickerActive) return;
        handleButtonPress(v.getId());
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

    private boolean appendToString(char ch) {
        boolean ret = false;
        if (inputString.length() < MAX_INPUT_LENGHT) {
            if (ch >= '0' && ch <= '9') {
                int ii = inputString.lastIndexOf('.');
                if (ii > 0 && ii > firstValueEnd) {
                    if (inputString.length() < ii + MAX_DECIMAL_DIGITS + 1) {
                        inputString += ch;
                        ret = true;
                    }
                } else {
                    inputString += ch;
                    ret = true;
                }
            } else {
                inputString += ch;
                ret = true;
            }
        }
        return ret;
    }

    private boolean appendMathToString(char ch) {
        boolean ret = false;
        if (inputString.length() < MAX_INPUT_LENGHT - 3) {
            inputString += ' ';
            inputString += ch;
            inputString += ' ';
            mathOperator = ch;
            ret = true;
        }
        return ret;
    }

    private InputStringLastBit getLastBit() {
        if (inputString.isEmpty()) {
            return InputStringLastBit.EMPTY;
        }

        char lastDigit = inputString.charAt(inputString.length() - 1);
        if (lastDigit >= '0' && lastDigit <= '9') return InputStringLastBit.DIGIT;
        else if (lastDigit == '.') return InputStringLastBit.DOT;
        else if (lastDigit == ' ')
            return InputStringLastBit.MATH;

        LLog.e(TAG, "unexpected ending character: " + lastDigit);
        return InputStringLastBit.EMPTY;
    }

    private InputStringLastBit removeLastBit() {
        if (inputString.isEmpty()) {
            return InputStringLastBit.EMPTY;
        }

        if (inputString.charAt(inputString.length() - 1) == ' ') {
            inputString = inputString.substring(0, inputString.length() - 3);
        } else {
            inputString = inputString.substring(0, inputString.length() - 1);
        }

        if (inputString.isEmpty()) {
            return InputStringLastBit.EMPTY;
        } else {
            if (inputString.length() == firstValueEnd + 1 &&
                    (inputString.charAt(firstValueEnd) == '-' ||
                            inputString.charAt(firstValueEnd) == '0')) {
                inputString = inputString.substring(0, inputString.length() - 1);
                if (firstValueEnd > 0)
                    return InputStringLastBit.MATH;
                else
                    return InputStringLastBit.EMPTY;
            }
        }

        return getLastBit();
    }

    private String value2string(double value) {
        return String.format("%.2f", value);
    }

    private double string2value(String str) {
        if (TextUtils.isEmpty(str)) return 0f;

        char lastDigit = str.charAt(str.length() - 1);
        if (lastDigit == '.') {
            return (Double.parseDouble(str.substring(0, str.length() - 1)));
        }
        return Double.parseDouble(str);
    }

    private boolean applyMath() {
        boolean ret = true;
        String str1 = inputString.substring(0, firstValueEnd - 3);
        String str2 = inputString.substring(firstValueEnd, inputString.length());
        double val1 = string2value(str1);
        double val2 = string2value(str2);

        switch (mathOperator) {
            case '+':
                value = val1 + val2;
                break;
            case '-':
                value = val1 - val2;
                break;
            case '*':
                value = val1 * val2;
                break;
            case '/':
                if (val2 == 0) {
                    ret = false;
                } else {
                    value = val1 / val2;
                }
                break;
        }

        if (!ret || value > MAX_VALUE) {
            inputString = "";
            value = 0;
            return false;
        } else {
            inputString = value2string(value);
        }
        return true;
    }

    private void saveLog() {
        callback.onDollarAmountPickerExit(string2value(inputString), true);
    }

    private void handleButtonPress( int sender) {
        switch (state) {
            case INIT_NO_INPUT:
                state = doNoInputState(sender);
                break;
            case INTEGER_1:
                state = doInteger1State(sender);
                break;
            case DECIMAL_1_0:
                state = doDecimal10State(sender);
                break;
            case DECIMAL_1:
                state = doDecimal1State(sender);
                break;
            case MATH_INIT:
                state = doMathInitState(sender);
                break;
            case INTEGER_2:
                state = doInteger2State(sender);
                break;
            case DECIMAL_2:
                state = doDecimal2State(sender);
                break;
        }
    }

    private char getDigit(int btn) {
        int digit = 0;
        switch (btn) {
            case R.id.b9: digit++;
            case R.id.b8: digit++;
            case R.id.b7: digit++;
            case R.id.b6: digit++;
            case R.id.b5: digit++;
            case R.id.b4: digit++;
            case R.id.b3: digit++;
            case R.id.b2: digit++;
            case R.id.b1: digit++;
            case R.id.b0: break;
        }
        return (char)('0' + digit);
    }

    private char getMath(int btn) {
        char ch = '+';
        switch (btn) {
            case R.id.minus:
                ch = '-';
                break;
            case R.id.multiply:
                ch ='*';
                break;
            case R.id.divide:
                ch = '/';
                break;
        }
        return ch;
    }

    private State doNoInputState(int btn) {
        switch (btn) {
            case R.id.b9:
            case R.id.b8:
            case R.id.b7:
            case R.id.b6:
            case R.id.b5:
            case R.id.b4:
            case R.id.b3:
            case R.id.b2:
            case R.id.b1:
                appendToString(getDigit(btn));
                return initInteger1State();

            case R.id.b0:
            case R.id.dot:
                appendToString('0');
                appendToString('.');
                return initDecimal10State();

            case R.id.ok:
                if (allowZero) saveLog();
                break;
        }

        return State.INIT_NO_INPUT;
    }

    private State initNoInputState() {
        enableMath(false);
        enableOk(allowZero? true: false);
        enableDot(true);

        valueTV.setText("0.0");
        LViewUtils.setAlpha(valueTV, 0.3f);
        ((ImageButton) viewSave).setImageResource(R.drawable.ic_action_accept);
        inputString = "";
        return State.INIT_NO_INPUT;
    }

    private State doInteger1State(int btn) {
        switch (btn) {
            case R.id.b9:
            case R.id.b8:
            case R.id.b7:
            case R.id.b6:
            case R.id.b5:
            case R.id.b4:
            case R.id.b3:
            case R.id.b2:
            case R.id.b1:
            case R.id.b0:
                if (appendToString(getDigit(btn))) {
                    valueTV.setText(inputString);
                }
                break;

            case R.id.backspace:
                switch (removeLastBit()) {
                    case DIGIT:
                        valueTV.setText(inputString);
                        break;
                    case EMPTY:
                        return initNoInputState();
                }
                break;

            case R.id.dot:
                if (appendToString('.'))
                    return initDecimal1State();

            case R.id.plus:
            case R.id.minus:
            case R.id.multiply:
            case R.id.divide:
                if (appendMathToString(getMath(btn)))
                    return initMathInitState();
                break;

            case R.id.ok:
                saveLog();
        }
        return State.INTEGER_1;
    }

    private State initInteger1State() {
        enableDot(true);
        enableMath(true);
        enableOk(true);

        firstValueEnd = 0;
        LViewUtils.setAlpha(valueTV, 1.0f);
        ((ImageButton) viewSave).setImageResource(R.drawable.ic_action_accept);
        valueTV.setText(inputString);
        return State.INTEGER_1;
    }

    private State doDecimal10State(int btn) {
        switch (btn) {
            case R.id.b9:
            case R.id.b8:
            case R.id.b7:
            case R.id.b6:
            case R.id.b5:
            case R.id.b4:
            case R.id.b3:
            case R.id.b2:
            case R.id.b1:
                if (appendToString(getDigit(btn)))
                    return initDecimal1State();

            case R.id.b0:
                if (appendToString('0')) {
                    valueTV.setText(inputString);
                }
                break;

            case R.id.backspace:
                if (InputStringLastBit.DOT == getLastBit()) {
                    if (InputStringLastBit.EMPTY == removeLastBit())
                        return initNoInputState();
                    else
                        return initInteger1State();
                } else {
                    removeLastBit();
                    valueTV.setText(inputString);
                }
                break;

            case R.id.ok:
                if (allowZero) saveLog();
                break;
        }
        return State.DECIMAL_1_0;
    }

    private State initDecimal10State() {
        enableDot(false);
        enableMath(false);
        enableOk(allowZero? true: false);

        LViewUtils.setAlpha(valueTV, 1.0f);
        ((ImageButton) viewSave).setImageResource(R.drawable.ic_action_accept);
        valueTV.setText(inputString);
        return State.DECIMAL_1_0;
    }

    private State doDecimal1State(int btn) {
        switch (btn) {
            case R.id.b9:
            case R.id.b8:
            case R.id.b7:
            case R.id.b6:
            case R.id.b5:
            case R.id.b4:
            case R.id.b3:
            case R.id.b2:
            case R.id.b1:
            case R.id.b0:
                if (appendToString(getDigit(btn))) {
                    valueTV.setText(inputString);
                    if (string2value(inputString) >= 0.01)
                        enableOk(true);
                    else
                        enableOk(false);
                }
                break;

            case R.id.backspace:
                if (InputStringLastBit.DOT == getLastBit()) {
                    if (InputStringLastBit.EMPTY == removeLastBit())
                        return initNoInputState();
                    else
                        return initInteger1State();
                } else {
                    removeLastBit();
                    if (0f == string2value(inputString))
                        return initDecimal10State();
                    valueTV.setText(inputString);
                }
                break;

            case R.id.plus:
            case R.id.minus:
            case R.id.multiply:
            case R.id.divide:
                if (appendMathToString(getMath(btn)))
                    return initMathInitState();
                break;

            case R.id.ok:
                saveLog();
        }
        return State.DECIMAL_1;
    }

    private State initDecimal1State() {
        enableDot(false);
        enableMath(true);

        if (allowZero) enableOk(true);
        else {
            if (string2value(inputString) >= 0.01)
                enableOk(true);
            else
                enableOk(false);
        }

        firstValueEnd = 0;
        ((ImageButton) viewSave).setImageResource(R.drawable.ic_action_accept);
        LViewUtils.setAlpha(valueTV, 1.0f);
        valueTV.setText(inputString);
        return State.DECIMAL_1;
    }

    private State doMathInitState(int btn) {
        switch (btn) {
            case R.id.b9:
            case R.id.b8:
            case R.id.b7:
            case R.id.b6:
            case R.id.b5:
            case R.id.b4:
            case R.id.b3:
            case R.id.b2:
            case R.id.b1:
                if (appendToString(getDigit(btn))) {
                    return initInteger2State();
                }
                break;

            case R.id.b0:
            case R.id.dot:
                appendToString('0');
                appendToString('.');
                return initDecimal2State();

            case R.id.backspace:
            case R.id.ok:
                removeLastBit();

                if (inputString.lastIndexOf('.') > 0)
                    return initDecimal1State();
                else
                    return initInteger1State();
        }
        return State.MATH_INIT;
    }

    private State initMathInitState() {
        enableMath(false);
        enableDot(true);
        ((ImageButton) viewSave).setImageResource(R.drawable.ic_action_equal);

        valueTV.setText(inputString);
        firstValueEnd = inputString.length();

        return State.MATH_INIT;
    }

    private State doInteger2State(int btn) {
        switch (btn) {
            case R.id.b9:
            case R.id.b8:
            case R.id.b7:
            case R.id.b6:
            case R.id.b5:
            case R.id.b4:
            case R.id.b3:
            case R.id.b2:
            case R.id.b1:
            case R.id.b0:
                if (appendToString(getDigit(btn))) {
                    valueTV.setText(inputString);
                }
                break;

            case R.id.backspace:
                switch (removeLastBit()) {
                    case DIGIT:
                        valueTV.setText(inputString);
                        break;
                    case MATH:
                        return initMathInitState();
                }
                break;

            case R.id.dot:
                if (appendToString('.'))
                    return initDecimal2State();
                break;

            case R.id.plus:
            case R.id.minus:
            case R.id.multiply:
            case R.id.divide:
                if (applyMath()) {
                    if (appendMathToString(getMath(btn))) {
                        return initMathInitState();
                    } else {
                        return initDecimal1State();
                    }
                } else {
                    return initNoInputState();
                }

            case R.id.ok:
                if (applyMath()) {
                    return initDecimal1State();
                } else {
                    return initNoInputState();
                }
        }
        return State.INTEGER_2;
    }

    private State initInteger2State() {
        enableDot(true);
        enableMath(true);
        enableOk(true);

        LViewUtils.setAlpha(valueTV, 1.0f);
        valueTV.setText(inputString);

        return State.INTEGER_2;
    }

    private State doDecimal2State(int btn) {
        switch (btn) {
            case R.id.b9:
            case R.id.b8:
            case R.id.b7:
            case R.id.b6:
            case R.id.b5:
            case R.id.b4:
            case R.id.b3:
            case R.id.b2:
            case R.id.b1:
            case R.id.b0:
                if (appendToString(getDigit(btn))) {
                    valueTV.setText(inputString);
                }
                break;

            case R.id.backspace:
                if (InputStringLastBit.DOT == getLastBit()) {
                    if (InputStringLastBit.MATH == removeLastBit())
                        return initMathInitState();
                    else
                        return initInteger2State();
                } else {
                    removeLastBit();
                    valueTV.setText(inputString);
                }
                break;

            case R.id.plus:
            case R.id.minus:
            case R.id.multiply:
            case R.id.divide:
                if (applyMath()) {
                    if (appendMathToString(getMath(btn))) {
                        return initMathInitState();
                    } else {
                        return initDecimal1State();
                    }
                } else {
                    return initNoInputState();
                }

            case R.id.ok:
                if (applyMath()) {
                    return initDecimal1State();
                } else {
                    return initNoInputState();
                }
        }
        return State.DECIMAL_2;
    }

    private State initDecimal2State() {
        enableDot(false);
        enableMath(true);
        enableOk(true);

        LViewUtils.setAlpha(valueTV, 1.0f);
        valueTV.setText(inputString);

        return State.DECIMAL_2;
    }
}
