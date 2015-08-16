package com.swoag.logalong.fragments;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ViewFlipper;

import com.swoag.logalong.LFragment;
import com.swoag.logalong.R;
import com.swoag.logalong.entities.LItem;

public class NewTransactionFragment extends LFragment implements View.OnClickListener, TransactionEdit.TransitionEditItf {
    private static final String TAG = NewTransactionFragment.class.getSimpleName();

    private Button btnExpense, btnIncome, btnTransaction;
    private ViewFlipper viewFlipper;

    private View rootView;
    private TransactionEdit edit;
    private LItem item;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_new_transaction, container, false);

        viewFlipper = (ViewFlipper) rootView.findViewById(R.id.viewFlipper);
        viewFlipper.setAnimateFirstView(false);
        viewFlipper.setDisplayedChild(0);

        btnExpense = setBtnListener(rootView, R.id.expense);
        btnIncome = setBtnListener(rootView, R.id.income);
        btnTransaction = setBtnListener(rootView, R.id.transaction);

        item = new LItem();

        return rootView;
    }

    @Override
    public void onDestroyView() {
        viewFlipper.setInAnimation(null);
        viewFlipper.setOutAnimation(null);
        viewFlipper = null;

        edit = null;
        rootView = null;
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
            case R.id.expense:
                newLog(LItem.LOG_TYPE_EXPENSE);
                break;
            case R.id.income:
                newLog(LItem.LOG_TYPE_INCOME);
                break;
            case R.id.transaction:
                newLog(LItem.LOG_TYPE_TRNASACTION);
                break;

            default:
                break;
        }
    }

    @Override
    public void onTransactionEditExit(int action, boolean changed) {
        switch (action) {
            case TransactionEdit.TransitionEditItf.EXIT_OK:
                break;
            case TransactionEdit.TransitionEditItf.EXIT_CANCEL:
                break;
            case TransactionEdit.TransitionEditItf.EXIT_DELETE:
                break;
        }
        viewFlipper.setInAnimation(getActivity(), R.anim.slide_in_left);
        viewFlipper.setOutAnimation(getActivity(), R.anim.slide_out_right);
        viewFlipper.showPrevious();
    }

    private void newLog(int type) {
        item.setType(type);

        edit = new TransactionEdit(getActivity(), rootView, item, this);

        viewFlipper.setInAnimation(getActivity(), R.anim.slide_in_right);
        viewFlipper.setOutAnimation(getActivity(), R.anim.slide_out_left);
        viewFlipper.showNext();
    }

    private Button setBtnListener(View v, int id) {
        Button btn = (Button) v.findViewById(id);
        btn.setOnClickListener(this);
        return btn;
    }
}
