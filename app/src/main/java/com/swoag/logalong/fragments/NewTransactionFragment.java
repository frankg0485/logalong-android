package com.swoag.logalong.fragments;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ViewFlipper;

import com.swoag.logalong.LFragment;
import com.swoag.logalong.MainActivity;
import com.swoag.logalong.R;
import com.swoag.logalong.entities.LJournal;
import com.swoag.logalong.entities.LTransaction;
import com.swoag.logalong.network.LProtocol;
import com.swoag.logalong.utils.AppPersistency;
import com.swoag.logalong.utils.DBAccess;
import com.swoag.logalong.ScheduleActivity;

public class NewTransactionFragment extends LFragment implements View.OnClickListener, TransactionEdit.TransitionEditItf {
    private static final String TAG = NewTransactionFragment.class.getSimpleName();

    private Button btnExpense, btnIncome, btnTransaction, btnSchedule;
    private ViewFlipper viewFlipper;

    private View rootView;
    private TransactionEdit edit;
    private LTransaction item;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_new_transaction, container, false);

        viewFlipper = (ViewFlipper) rootView.findViewById(R.id.viewFlipper);
        viewFlipper.setAnimateFirstView(false);
        viewFlipper.setDisplayedChild(0);

        btnExpense = setBtnListener(rootView, R.id.expense);
        btnIncome = setBtnListener(rootView, R.id.income);
        btnTransaction = setBtnListener(rootView, R.id.transaction);
        btnSchedule = setBtnListener(rootView, R.id.schedule);
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
                newLog(LTransaction.TRANSACTION_TYPE_EXPENSE);
                break;
            case R.id.income:
                newLog(LTransaction.TRANSACTION_TYPE_INCOME);
                break;
            case R.id.transaction:
                //newLog(LTransaction.TRANSACTION_TYPE_TRANSFER);
                //DBPorter.exportDb("logalong" + DBHelper.DB_VERSION + ".db");
                //DBPorter.importDb("logalong" + DBHelper.DB_VERSION + ".db");
                LProtocol.ui.ping();
                break;

            case R.id.schedule:
                startActivity(new Intent(getActivity(), ScheduleActivity.class));
                break;

            default:
                break;
        }
    }

    @Override
    public void onTransactionEditExit(int action, boolean changed) {

        switch (action) {
            case TransactionEdit.TransitionEditItf.EXIT_OK:
                AppPersistency.transactionChanged = changed;

                item.setTimeStampLast(System.currentTimeMillis());
                DBAccess.addItem(item);

                LJournal journal = new LJournal();
                journal.updateItem(item);

                break;
            case TransactionEdit.TransitionEditItf.EXIT_CANCEL:
                break;
            case TransactionEdit.TransitionEditItf.EXIT_DELETE:
                AppPersistency.transactionChanged = changed;
                break;
        }
        viewFlipper.setInAnimation(getActivity(), R.anim.slide_in_left);
        viewFlipper.setOutAnimation(getActivity(), R.anim.slide_out_right);
        viewFlipper.showPrevious();

        MainActivity actv = (MainActivity) getActivity();
        actv.enablePager();
    }

    @Override
    public boolean onBackPressed() {
        if (viewFlipper.getDisplayedChild() == 1) {
            edit.dismiss();
            return true;
        }
        return false;
    }

    private void newLog(int type) {
        item = new LTransaction();
        item.setType(type);
        item.setAccount(4);
        item.setCategory(2);

        edit = new TransactionEdit(getActivity(), rootView, item, true, this);

        viewFlipper.setInAnimation(getActivity(), R.anim.slide_in_right);
        viewFlipper.setOutAnimation(getActivity(), R.anim.slide_out_left);
        viewFlipper.showNext();

        MainActivity actv = (MainActivity) getActivity();
        actv.disablePager();
    }

    private Button setBtnListener(View v, int id) {
        Button btn = (Button) v.findViewById(id);
        btn.setOnClickListener(this);
        return btn;
    }
}
