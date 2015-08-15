package com.swoag.logalong.fragments;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import com.swoag.logalong.LApp;
import com.swoag.logalong.LFragment;
import com.swoag.logalong.LFragmentActivity;
import com.swoag.logalong.R;
import com.swoag.logalong.utils.LViewUtils;

import java.util.ArrayList;
import java.util.List;

public class NewLogFragment extends LFragment implements View.OnClickListener {
    private static final String TAG = NewLogFragment.class.getSimpleName();

    private Spinner spinnerCategory, spinnerAccount;
    private Button btn0, btn1, btn2, btn3, btn4, btn5, btn6, btn7, btn8, btn9;
    private Button btnDot, btnBack, btnPlus, btnMinus, btnMultiply, btnDivide;
    private Button btnClear, btnOk;
    private TextView valueTV;

    private String inputString = "";
    private double lastValue;
    private int lastValueEnd;
    private int mathOperator;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_new_log, container, false);
        spinnerCategory = (Spinner) rootView.findViewById(R.id.spinnerCategory);

        spinnerAccount = (Spinner) rootView.findViewById(R.id.spinnerAccount);
        List<String> list = new ArrayList<String>();
        list.add("Cash");
        list.add("Credit:Discover");
        list.add("Credit:Master");
        list.add("Credit:Visa");
        list.add("Checkings");
        list.add("Savings");
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(LApp.ctx, android.R.layout.simple_spinner_item, list);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAccount.setAdapter(dataAdapter);

        List<String> list2 = new ArrayList<String>();
        list2.add("Food");
        list2.add("Grocery");
        list2.add("Gas");
        list2.add("Entertainment");
        list2.add("General");
        ArrayAdapter<String> dataAdapter2 = new ArrayAdapter<String>(LApp.ctx, android.R.layout.simple_spinner_item, list2);
        dataAdapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(dataAdapter2);

        btn0 = setBtnListener(rootView, R.id.b0);
        btn0 = setBtnListener(rootView, R.id.b1);
        btn0 = setBtnListener(rootView, R.id.b2);
        btn0 = setBtnListener(rootView, R.id.b3);
        btn0 = setBtnListener(rootView, R.id.b4);
        btn0 = setBtnListener(rootView, R.id.b5);
        btn0 = setBtnListener(rootView, R.id.b6);
        btn0 = setBtnListener(rootView, R.id.b7);
        btn0 = setBtnListener(rootView, R.id.b8);
        btn0 = setBtnListener(rootView, R.id.b9);
        btnBack = setBtnListener(rootView, R.id.back);
        btnDot = setBtnListener(rootView, R.id.dot);
        btnPlus = setBtnListener(rootView, R.id.plus);
        btnMinus = setBtnListener(rootView, R.id.minus);
        btnMultiply = setBtnListener(rootView, R.id.multiply);
        btnDivide = setBtnListener(rootView, R.id.divide);
        btnClear = setBtnListener(rootView, R.id.clear);
        btnOk = setBtnListener(rootView, R.id.ok);

        valueTV = (TextView) rootView.findViewById(R.id.value);
        clearInputString();

        return rootView;
    }

    @Override
    public void onDestroyView() {
        btn0 = null;
        btn1 = null;
        btn2 = null;
        btn3 = null;
        btn4 = null;
        btn5 = null;
        btn6 = null;
        btn7 = null;
        btn8 = null;
        btn9 = null;
        btnBack = null;
        btnDot = null;
        btnPlus = null;
        btnMultiply = null;
        btnDivide = null;
        btnClear = null;
        btnOk = null;

        spinnerAccount = null;
        spinnerCategory = null;

        valueTV = null;

        super.onDestroyView();
    }

    @Override
    public void onSelected(boolean selected) {
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
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
            case R.id.back:
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

            case R.id.ok:
                saveLog();
                break;

            default:
                break;
        }
    }

    private Button setBtnListener(View v, int id) {
        Button btn = (Button) v.findViewById(id);
        btn.setOnClickListener(this);
        return btn;
    }

    private void enableOk(boolean yes) {
        btnOk.setEnabled(yes);
        if (yes) LViewUtils.setAlpha((View) btnOk, 1.0f);
        else LViewUtils.setAlpha((View) btnOk, 0.5f);
    }

    private void enableMath(boolean yes) {
        btnPlus.setEnabled(yes);
        btnMinus.setEnabled(yes);
        btnMultiply.setEnabled(yes);
        btnDivide.setEnabled(yes);

        if (yes) {
            LViewUtils.setAlpha((View) btnPlus, 1.0f);
            LViewUtils.setAlpha((View) btnMinus, 1.0f);
            LViewUtils.setAlpha((View) btnMultiply, 1.0f);
            LViewUtils.setAlpha((View) btnDivide, 1.0f);
        } else {
            LViewUtils.setAlpha((View) btnPlus, 0.5f);
            LViewUtils.setAlpha((View) btnMinus, 0.5f);
            LViewUtils.setAlpha((View) btnMultiply, 0.5f);
            LViewUtils.setAlpha((View) btnDivide, 0.5f);
        }
    }

    private void enableDot(boolean yes) {
        btnDot.setEnabled(yes);
        if (yes) LViewUtils.setAlpha((View) btnDot, 1.0f);
        else LViewUtils.setAlpha((View) btnDot, 0.5f);
    }

    private void clearInputString() {
        inputString = "";
        valueTV.setText("0.0");
        btnOk.setText(LApp.ctx.getString(android.R.string.ok));

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
                    btnOk.setText(LApp.ctx.getString(android.R.string.ok));
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
            if (mathOperator < 0) enableMath(true);
        } else {
            if (mathOperator == 3 || mathOperator < 0) enableOk(false);
            if (mathOperator < 0) enableMath(false);
        }
    }

    private void doMathToString(int operator) {
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
        btnOk.setText("=");
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

        }
    }
}
