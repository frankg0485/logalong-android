package com.swoag.logalong.fragments;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.swoag.logalong.LFragment;
import com.swoag.logalong.MainActivity;
import com.swoag.logalong.R;
import com.swoag.logalong.entities.LAccount;
import com.swoag.logalong.entities.LAccountBalance;
import com.swoag.logalong.entities.LAccountSummary;
import com.swoag.logalong.entities.LAllBalances;
import com.swoag.logalong.entities.LItem;
import com.swoag.logalong.entities.LSectionSummary;
import com.swoag.logalong.utils.AppPersistency;
import com.swoag.logalong.utils.DBAccess;
import com.swoag.logalong.utils.DBHelper;

import org.w3c.dom.Text;

import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class ViewTransactionFragment extends LFragment implements View.OnClickListener {
    private static final String TAG = ViewTransactionFragment.class.getSimpleName();

    private ListView listView;
    private Cursor logsCursor;
    private MyCursorAdapter adapter;
    private TextView monthTV, balanceTV, incomeTV, expenseTV, altMonthTV, altBalanceTV, altIncomeTV, altExpenseTV;
    private LSectionSummary sectionSummary;

    private ViewFlipper viewFlipper, listViewFlipper;
    private View rootView, prevView, nextView, filterView;
    private TransactionEdit edit;

    private LAllBalances allBalances;

    private long getMs(int year, int month) {
        Calendar now = Calendar.getInstance();
        now.clear();
        now.set(year, month, 1);
        return now.getTimeInMillis();
    }

    private void getLogsCursor() {
        String column = "";
        if (AppPersistency.viewTransactionYear == -1 || AppPersistency.viewTransactionMonth == -1) {
            Calendar now = Calendar.getInstance();
            AppPersistency.viewTransactionYear = now.get(Calendar.YEAR);
            AppPersistency.viewTransactionMonth = now.get(Calendar.MONTH);
        }

        switch (AppPersistency.viewTransactionFilter) {
            case AppPersistency.TRANSACTION_FILTER_ALL:
                logsCursor = DBAccess.getActiveItemsCursorInRange(
                        getMs(AppPersistency.viewTransactionYear, AppPersistency.viewTransactionMonth),
                        getMs(AppPersistency.viewTransactionYear, AppPersistency.viewTransactionMonth + 1));
                break;
            case AppPersistency.TRANSACTION_FILTER_BY_ACCOUNT:
                column = DBHelper.TABLE_COLUMN_ACCOUNT;
                logsCursor = DBAccess.getActiveItemsCursorInRangeSortByAccount(
                        getMs(AppPersistency.viewTransactionYear, AppPersistency.viewTransactionMonth),
                        getMs(AppPersistency.viewTransactionYear, AppPersistency.viewTransactionMonth + 1));
                break;
            case AppPersistency.TRANSACTION_FILTER_BY_CATEGORY:
                column = DBHelper.TABLE_COLUMN_CATEGORY;
                logsCursor = DBAccess.getActiveItemsCursorInRangeSortByCategory(
                        getMs(AppPersistency.viewTransactionYear, AppPersistency.viewTransactionMonth),
                        getMs(AppPersistency.viewTransactionYear, AppPersistency.viewTransactionMonth + 1));
                break;
            case AppPersistency.TRANSACTION_FILTER_BY_TAG:
                column = DBHelper.TABLE_COLUMN_TAG;
                logsCursor = DBAccess.getActiveItemsCursorInRangeSortByTag(
                        getMs(AppPersistency.viewTransactionYear, AppPersistency.viewTransactionMonth),
                        getMs(AppPersistency.viewTransactionYear, AppPersistency.viewTransactionMonth + 1));
                break;
            case AppPersistency.TRANSACTION_FILTER_BY_VENDOR:
                column = DBHelper.TABLE_COLUMN_VENDOR;
                logsCursor = DBAccess.getActiveItemsCursorInRangeSortByVendor(
                        getMs(AppPersistency.viewTransactionYear, AppPersistency.viewTransactionMonth),
                        getMs(AppPersistency.viewTransactionYear, AppPersistency.viewTransactionMonth + 1));
                break;
        }

        sectionSummary.clear();
        // generate section summary
        if (AppPersistency.TRANSACTION_FILTER_ALL == AppPersistency.viewTransactionFilter) return;
        if (logsCursor == null || logsCursor.getCount() <= 0) return;

        LAccountSummary summary;
        boolean hasLog = false;
        long lastId = 0;
        long lastIndex = 0;
        long id;
        double v, income = 0, expense = 0;
        int type;
        logsCursor.moveToFirst();
        do {
            v = logsCursor.getDouble(logsCursor.getColumnIndexOrThrow(DBHelper.TABLE_LOG_COLUMN_VALUE));
            type = logsCursor.getInt(logsCursor.getColumnIndexOrThrow(DBHelper.TABLE_LOG_COLUMN_TYPE));
            id = logsCursor.getLong(logsCursor.getColumnIndexOrThrow(column));

            if (hasLog && id != lastId) {
                summary = new LAccountSummary();
                summary.setExpense(expense);
                summary.setIncome(income);
                summary.setBalance(income - expense);

                switch (AppPersistency.viewTransactionFilter) {
                    case AppPersistency.TRANSACTION_FILTER_BY_ACCOUNT:
                        summary.setName(DBAccess.getAccountNameById(lastId));
                        break;
                    case AppPersistency.TRANSACTION_FILTER_BY_CATEGORY:
                        summary.setName(DBAccess.getCategoryNameById(lastId));
                        break;
                    case AppPersistency.TRANSACTION_FILTER_BY_TAG:
                        summary.setName(DBAccess.getTagNameById(lastId));
                        break;
                    case AppPersistency.TRANSACTION_FILTER_BY_VENDOR:
                        summary.setName(DBAccess.getVendorNameById(lastId));
                        break;
                }
                sectionSummary.addSummary(lastIndex, summary);
                hasLog = false;
                income = 0;
                expense = 0;
                lastId = id;
            }

            lastIndex = logsCursor.getLong(0);
            if (type == LItem.LOG_TYPE_EXPENSE) expense += v;
            else income += v;
            if (!hasLog) lastId = id;
            hasLog = true;
        } while (logsCursor.moveToNext());

        if (hasLog) {
            summary = new LAccountSummary();
            switch (AppPersistency.viewTransactionFilter) {
                case AppPersistency.TRANSACTION_FILTER_BY_ACCOUNT:
                    summary.setName(DBAccess.getAccountNameById(id));
                    break;
                case AppPersistency.TRANSACTION_FILTER_BY_CATEGORY:
                    summary.setName(DBAccess.getCategoryNameById(id));
                    break;
                case AppPersistency.TRANSACTION_FILTER_BY_TAG:
                    summary.setName(DBAccess.getTagNameById(id));
                    break;
                case AppPersistency.TRANSACTION_FILTER_BY_VENDOR:
                    summary.setName(DBAccess.getVendorNameById(id));
                    break;
            }
            summary.setExpense(expense);
            summary.setIncome(income);
            summary.setBalance(income - expense);
            sectionSummary.addSummary(lastIndex, summary);
        }
    }

    private void initListView() {
        getLogsCursor();

        adapter = new MyCursorAdapter(getActivity(), logsCursor, sectionSummary);
        listView.setAdapter(adapter);
        listView.post(new Runnable() {
            @Override
            public void run() {
                listView.setSelection(adapter.getCount() - 1);
            }
        });

        showBalance(false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_view_transaction, container, false);

        allBalances = LAllBalances.getInstance();

        prevView = setViewListener(rootView, R.id.prev);
        nextView = setViewListener(rootView, R.id.next);
        filterView = setViewListener(rootView, R.id.filter);

        viewFlipper = (ViewFlipper) rootView.findViewById(R.id.viewFlipper);
        viewFlipper.setAnimateFirstView(false);
        viewFlipper.setDisplayedChild(0);

        listViewFlipper = (ViewFlipper) rootView.findViewById(R.id.listViewFlipper);
        listViewFlipper.setAnimateFirstView(false);
        listViewFlipper.setDisplayedChild(0);

        listView = (ListView) rootView.findViewById(R.id.logsList);

        View tmp = listViewFlipper.findViewById(R.id.logs);
        monthTV = (TextView) tmp.findViewById(R.id.month);
        balanceTV = (TextView) tmp.findViewById(R.id.balance);
        expenseTV = (TextView) tmp.findViewById(R.id.expense);
        incomeTV = (TextView) tmp.findViewById(R.id.income);

        tmp = listViewFlipper.findViewById(R.id.logsAlt);
        altMonthTV = (TextView) tmp.findViewById(R.id.month);
        altBalanceTV = (TextView) tmp.findViewById(R.id.balance);
        altExpenseTV = (TextView) tmp.findViewById(R.id.expense);
        altIncomeTV = (TextView) tmp.findViewById(R.id.income);

        sectionSummary = new LSectionSummary();

        initListView();

        return rootView;
    }

    @Override
    public void onDestroyView() {
        prevView = null;
        nextView = null;
        filterView = null;

        viewFlipper.setInAnimation(null);
        viewFlipper.setOutAnimation(null);
        viewFlipper = null;

        listViewFlipper.setInAnimation(null);
        listViewFlipper.setOutAnimation(null);
        listViewFlipper = null;

        balanceTV = null;
        expenseTV = null;
        incomeTV = null;
        altBalanceTV = null;
        altExpenseTV = null;
        altIncomeTV = null;

        rootView = null;
        super.onDestroyView();
    }

    @Override
    public void onSelected(boolean selected) {
        //LLog.d(TAG, "onSelected" + selected);
        if (selected && AppPersistency.transactionChanged) {
            refreshLogs();
        }
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
            case R.id.filter:
                changeFilter();
                break;
            case R.id.prev:
                showPrevNext(true);
                break;
            case R.id.next:
                showPrevNext(false);
                break;
            default:
                break;
        }
    }

    private class MyCursorAdapter extends CursorAdapter implements View.OnClickListener,
            TransactionEdit.TransitionEditItf {
        private LItem item;
        private LSectionSummary sectionSummary;

        public MyCursorAdapter(Context context, Cursor cursor) {
            //TODO: deprecated API is used here for max OS compatibility, provide alternative
            //      using LoaderManager with a CursorLoader.
            //super(context, cursor, 0);
            super(context, cursor, false);
            sectionSummary = null;
        }

        public MyCursorAdapter(Context context, Cursor cursor, LSectionSummary sectionSummary) {
            //TODO: deprecated API is used here for max OS compatibility, provide alternative
            //      using LoaderManager with a CursorLoader.
            //super(context, cursor, 0);
            super(context, cursor, false);
            this.sectionSummary = sectionSummary;
        }

        /**
         * Bind an existing view to the data pointed to by cursor
         */
        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            View mainView = view.findViewById(R.id.mainView);
            View sectionView = view.findViewById(R.id.sectionView);

            TextView tv = (TextView) mainView.findViewById(R.id.category);
            int categoryId = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_CATEGORY));
            int tagId = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_TAG));

            String category = DBAccess.getCategoryNameById(categoryId);
            String tag = DBAccess.getTagNameById(tagId);

            String str = "";
            if (!tag.isEmpty()) str = tag + ":";
            str += category;
            tv.setText(str);

            tv = (TextView) mainView.findViewById(R.id.note);
            int vendorId = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_VENDOR));
            String vendor = DBAccess.getVendorNameById(vendorId);
            String note = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.TABLE_LOG_COLUMN_NOTE));

            if ((!note.isEmpty()) && (!vendor.isEmpty())) vendor += " - " + note;
            else if (!note.isEmpty()) vendor = note;
            tv.setText(vendor);

            tv = (TextView) mainView.findViewById(R.id.date);
            long tm = cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.TABLE_LOG_COLUMN_TIMESTAMP));
            tv.setText(new SimpleDateFormat("MMM d, yyy").format(tm));

            tv = (TextView) mainView.findViewById(R.id.dollor);
            int type = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.TABLE_LOG_COLUMN_TYPE));
            Resources rsc = getActivity().getResources();
            switch (type) {
                case LItem.LOG_TYPE_EXPENSE:
                    tv.setTextColor(rsc.getColor(R.color.base_red));
                    break;
                case LItem.LOG_TYPE_INCOME:
                    tv.setTextColor(rsc.getColor(R.color.base_green));
                    break;
                case LItem.LOG_TYPE_TRNASACTION:
                    tv.setTextColor(rsc.getColor(R.color.base_blue));
                    break;
            }
            double dollar = cursor.getDouble(cursor.getColumnIndexOrThrow(DBHelper.TABLE_LOG_COLUMN_VALUE));
            tv.setText(String.format("%.2f", dollar));

            mainView.setOnClickListener(this);
            long id = cursor.getLong(0);
            mainView.setTag(new VTag(id));

            if (sectionSummary.hasId(id)) {
                tv = (TextView) sectionView.findViewById(R.id.sortName);
                LAccountSummary summary = sectionSummary.getSummaryById(id);
                tv.setText(summary.getName());

                tv = (TextView) sectionView.findViewById(R.id.income);
                tv.setText(String.format("%.2f", summary.getIncome()));
                tv = (TextView) sectionView.findViewById(R.id.expense);
                tv.setText(String.format("%.2f", summary.getExpense()));

                sectionView.setVisibility(View.VISIBLE);
            } else {
                sectionView.setVisibility(View.GONE);
            }
        }

        /**
         * Makes a new view to hold the data pointed to by cursor.
         */
        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View newView = LayoutInflater.from(context).inflate(R.layout.transaction_item, parent, false);
            return newView;
        }

        @Override
        public void onClick(View v) {
            VTag tag = (VTag) v.getTag();

            item = DBAccess.getLogItemById(tag.id);

            edit = new TransactionEdit(getActivity(), rootView, item, false, this);

            viewFlipper.setInAnimation(getActivity(), R.anim.slide_in_right);
            viewFlipper.setOutAnimation(getActivity(), R.anim.slide_out_left);
            viewFlipper.showNext();

            MainActivity actv = (MainActivity) getActivity();
            actv.disablePager();
        }

        @Override
        public void onTransactionEditExit(int action, boolean changed) {
            switch (action) {
                case TransactionEdit.TransitionEditItf.EXIT_OK:
                    AppPersistency.transactionChanged = changed;
                    if (changed) {
                        DBAccess.updateItem(item);
                        onSelected(true);
                    }
                    break;
                case TransactionEdit.TransitionEditItf.EXIT_CANCEL:
                    break;
                case TransactionEdit.TransitionEditItf.EXIT_DELETE:
                    AppPersistency.transactionChanged = true;
                    DBAccess.deleteItemById(item.getId());
                    onSelected(true);
                    break;
            }
            viewFlipper.setInAnimation(getActivity(), R.anim.slide_in_left);
            viewFlipper.setOutAnimation(getActivity(), R.anim.slide_out_right);
            viewFlipper.showPrevious();

            MainActivity actv = (MainActivity) getActivity();
            actv.enablePager();
        }

        private class VTag {
            long id;

            public VTag(long id) {
                this.id = id;
            }
        }
    }

    @Override
    public boolean onBackPressed() {
        if (viewFlipper.getDisplayedChild() == 1) {
            edit.dismiss();
            return true;
        }
        return false;
    }

    private void showBalance(boolean altView) {
        LAccountSummary summary = new LAccountSummary();
        getBalance(summary);
        TextView mtv, btv, itv, etv;
        if (altView) {
            mtv = altMonthTV;
            btv = altBalanceTV;
            itv = altIncomeTV;
            etv = altExpenseTV;
        } else {
            mtv = monthTV;
            btv = balanceTV;
            itv = incomeTV;
            etv = expenseTV;
        }

        if (summary.getBalance() < 0) {
            btv.setTextColor(getActivity().getResources().getColor(R.color.base_red));
        } else {
            btv.setTextColor(getActivity().getResources().getColor(R.color.base_green));
        }
        btv.setText(String.format("%.2f", Math.abs(summary.getBalance())));

        itv.setText(String.format("%.2f", summary.getIncome()));
        etv.setText(String.format("%.2f", summary.getExpense()));

        mtv.setText(new DateFormatSymbols().getMonths()[AppPersistency.viewTransactionMonth]);
    }

    private void getBalance(LAccountSummary summary) {
        DBAccess.getAccountSummaryForCurrentCursor(summary, 0, logsCursor);
    }

    private boolean isAltView = false;
    private ListView lv;

    private boolean prevNextMonth(boolean prev) {
        int year = AppPersistency.viewTransactionYear;
        int month = AppPersistency.viewTransactionMonth;
        boolean ret = true;

        if (prev) {
            AppPersistency.viewTransactionMonth--;
            if (AppPersistency.viewTransactionMonth < 0) {
                AppPersistency.viewTransactionYear--;
                AppPersistency.viewTransactionMonth = 11;
            }
        } else {
            AppPersistency.viewTransactionMonth++;
            if (AppPersistency.viewTransactionMonth > 11) {
                AppPersistency.viewTransactionYear++;
                AppPersistency.viewTransactionMonth = 0;
            }
        }

        long ym = getMs(AppPersistency.viewTransactionYear, AppPersistency.viewTransactionMonth);
        if (ym < allBalances.getStartDate() || ym > allBalances.getEndDate()) {
            AppPersistency.viewTransactionYear = year;
            AppPersistency.viewTransactionMonth = month;
            return false;
        }
        return true;
    }

    private void refreshLogs() {
        AppPersistency.transactionChanged = false;

        getLogsCursor();
        adapter.swapCursor(logsCursor);
        adapter.notifyDataSetChanged();
        listView.post(new Runnable() {
            @Override
            public void run() {
                listView.setSelection(adapter.getCount() - 1);
            }
        });
        showBalance(isAltView);
    }

    private void showPrevNext(boolean prev) {
        if (!prevNextMonth(prev)) return;

        getLogsCursor();

        isAltView = !isAltView;
        showBalance(isAltView);

        if (prev) {
            listViewFlipper.setInAnimation(getActivity(), R.anim.slide_in_left);
            listViewFlipper.setOutAnimation(getActivity(), R.anim.slide_out_right);
            listViewFlipper.showPrevious();
        } else {
            listViewFlipper.setInAnimation(getActivity(), R.anim.slide_in_right);
            listViewFlipper.setOutAnimation(getActivity(), R.anim.slide_out_left);
            listViewFlipper.showNext();
        }

        adapter.swapCursor(logsCursor);
        adapter.notifyDataSetChanged();
        listView.post(new Runnable() {
            @Override
            public void run() {
                listView.setSelection(adapter.getCount() - 1);
            }
        });
    }

    private View setViewListener(View v, int id) {
        View view = v.findViewById(id);
        view.setOnClickListener(this);
        return view;
    }

    private void nextFilter() {
        switch (AppPersistency.viewTransactionFilter) {
            case AppPersistency.TRANSACTION_FILTER_ALL:
//                AppPersistency.viewTransactionFilter = AppPersistency.TRANSACTION_FILTER_BY_ACCOUNT;
//                break;
            case AppPersistency.TRANSACTION_FILTER_BY_ACCOUNT:
                AppPersistency.viewTransactionFilter = AppPersistency.TRANSACTION_FILTER_BY_CATEGORY;
                break;
            case AppPersistency.TRANSACTION_FILTER_BY_CATEGORY:
                AppPersistency.viewTransactionFilter = AppPersistency.TRANSACTION_FILTER_BY_TAG;
                break;
            case AppPersistency.TRANSACTION_FILTER_BY_TAG:
                AppPersistency.viewTransactionFilter = AppPersistency.TRANSACTION_FILTER_BY_VENDOR;
                break;
            case AppPersistency.TRANSACTION_FILTER_BY_VENDOR:
                AppPersistency.viewTransactionFilter = AppPersistency.TRANSACTION_FILTER_ALL;
                break;
        }
    }

    private void changeFilter() {
        nextFilter();
        refreshLogs();
    }
}


