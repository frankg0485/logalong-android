package com.swoag.logalong.fragments;
/* Copyright (C) 2015 - 2018 SWOAG Technology <www.swoag.com> */

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.widget.CursorAdapter;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.swoag.logalong.ChartActivity;
import com.swoag.logalong.LFragment;
import com.swoag.logalong.R;
import com.swoag.logalong.entities.LAccountSummary;
import com.swoag.logalong.entities.LAllBalances;
import com.swoag.logalong.entities.LJournal;
import com.swoag.logalong.entities.LSearch;
import com.swoag.logalong.entities.LSectionSummary;
import com.swoag.logalong.entities.LTransaction;
import com.swoag.logalong.utils.AppPersistency;
import com.swoag.logalong.utils.DBAccount;
import com.swoag.logalong.utils.DBAccountBalance;
import com.swoag.logalong.utils.DBCategory;
import com.swoag.logalong.utils.DBHelper;
import com.swoag.logalong.utils.DBLoaderHelper;
import com.swoag.logalong.utils.DBTag;
import com.swoag.logalong.utils.DBTransaction;
import com.swoag.logalong.utils.DBVendor;
import com.swoag.logalong.utils.LLog;
import com.swoag.logalong.utils.LOnClickListener;
import com.swoag.logalong.utils.LPreferences;
import com.swoag.logalong.utils.LViewUtils;
import com.swoag.logalong.views.TransactionSearchDialog;

import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class ViewTransactionFragment extends LFragment implements DBLoaderHelper.DBLoaderHelperCallbacks,
        TransactionSearchDialog.TransactionSearchDialogItf, TransactionEdit.TransitionEditItf {
    private static final String TAG = ViewTransactionFragment.class.getSimpleName();

    private ListView listView;
    private ListView.OnScrollListener onScrollListener;
    private ImageView filterView, searchView;
    private View btnExpense, btnIncome, btnTransaction, selectTypeV;

    private MyCursorAdapter adapter;
    private TextView monthTV, balanceTV, incomeTV, expenseTV, altMonthTV, altBalanceTV, altIncomeTV, altExpenseTV;
    private View dispV, altDispV, chartView, newRecordView;
    private ImageView queryOrderIV, altQueryOrderIV;
    private LSectionSummary sectionSummary;

    private ViewFlipper viewFlipper, listViewFlipper;
    private View rootView, prevView, nextView;
    private TextView monthlyView, customTimeView;
    private TransactionEdit edit;

    private LAllBalances allBalances;
    private MyClickListener myClickListener;

    private DBLoaderHelper dbLoaderHelper;

    @Override
    public void onLoadFinished(android.support.v4.content.Loader<Cursor> loader, Cursor data) {

        if (loader.getId() == DBLoaderHelper.LOADER_INIT_RANGE) {
            dbLoaderHelper.restart(getLoaderManager(), DBLoaderHelper.LOADER_ALL_ACCOUNT_BALANCES);
            return;
        }

        if (loader.getId() == DBLoaderHelper.LOADER_ALL_ACCOUNT_BALANCES) {
            allBalances = new LAllBalances(data);
            initDbLoader();
            return;
        }

        setSectionSummary(loader.getId(), data);

        adapter.swapCursor(data);
        adapter.notifyDataSetChanged();

        showBalance(isAltView, data);

        int nextIndex = -1;
        try {
            if (AppPersistency.lastTransactionChangeTimeMs != 0) {
                // if currently loaded cursor contains last changed transaction, let's put that as the first item
                try {
                    int count = data.getCount();

                    int low = 0, to = 0, high = 0, tmp;
                    boolean search = false;
                    boolean ascend = LPreferences.getQueryOrderAscend();
                    data.moveToFirst();
                    long ms = data.getLong(data.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_TIMESTAMP));
                    if (AppPersistency.lastTransactionChangeTimeMs == ms) {
                        nextIndex = 0;
                    } else if (count > 1) {
                        if ((ascend && (AppPersistency.lastTransactionChangeTimeMs > ms)) ||
                                ((!ascend) && (AppPersistency.lastTransactionChangeTimeMs < ms))) {
                            search = true;
                        }
                    }

                    if (search) {
                        search = false;

                        data.moveToLast();
                        ms = data.getLong(data.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_TIMESTAMP));
                        if (AppPersistency.lastTransactionChangeTimeMs == ms) {
                            nextIndex = count - 1;
                        } else if (count > 2) {
                            if ((ascend && (AppPersistency.lastTransactionChangeTimeMs < ms)) ||
                                    ((!ascend) && (AppPersistency.lastTransactionChangeTimeMs > ms))) {
                                search = true;
                                low = 0;
                                high = count - 1;
                                to = count / 2;
                            }
                        }
                    }

                    while (search && low != to && high != to) {
                        data.moveToPosition(to);
                        ms = data.getLong(data.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_TIMESTAMP));

                        if (AppPersistency.lastTransactionChangeTimeMs == ms) {
                            //match found
                            nextIndex = to;
                            break;
                        } else {
                            if ((ascend && AppPersistency.lastTransactionChangeTimeMs > ms) ||
                                    ((!ascend) && AppPersistency.lastTransactionChangeTimeMs < ms)) {
                                low = to;
                                to += (high - to + 1) / 2;
                            } else {
                                high = to;
                                to -= (to - low + 1) / 2;
                            }
                        }
                    }
                } catch (Exception e) {
                    LLog.w(TAG, "fail to match record: " + e.getMessage());
                }
            }

            if (nextIndex == -1) {
                AppPersistency.ListViewHistory history = AppPersistency.getViewHistory(AppPersistency.getViewLevel());
                if (history != null) {
                    listView.setSelectionFromTop(history.index, history.top);
                } else {
                    if (LPreferences.getQueryOrderAscend()) {
                        listView.setSelection(adapter.getCount() - 1);
                    } else {
                        listView.setSelection(0);
                    }
                }
            } else {
                listView.setSelection(nextIndex);
                AppPersistency.lastTransactionChangeTimeMsHonored = true;
            }
        } catch (Exception e) {
            LLog.w(TAG, "unexpected listview history error: " + e.getMessage());
        }
        showPrevNextControls();
    }

    @Override
    public void onLoaderReset(android.support.v4.content.Loader<Cursor> loader) {
        adapter.swapCursor(null);
    }

    private long getMs(int year, int month) {
        Calendar now = Calendar.getInstance();
        now.clear();
        now.set(year, month, 1);
        return now.getTimeInMillis();
    }

    private void setAccountSummary(LAccountSummary summary, long id) {
        if (AppPersistency.viewTransactionTime == AppPersistency.TRANSACTION_TIME_ALL) return;
        if (AppPersistency.viewTransactionTime == AppPersistency.TRANSACTION_TIME_ANNUALLY) {
            summary.setBalance(allBalances.getBalance(id, AppPersistency.viewTransactionYear, 11));
        } else if (AppPersistency.viewTransactionTime == AppPersistency.TRANSACTION_TIME_MONTHLY) {
            summary.setBalance(allBalances.getBalance(id, AppPersistency.viewTransactionYear, AppPersistency
                    .viewTransactionMonth));
        } else {
            //quarterly
            summary.setBalance(allBalances.getBalance(id, AppPersistency.viewTransactionYear, AppPersistency
                    .viewTransactionQuarter * 3 + 2));
        }
    }

    private void setSectionSummary(int filterId, Cursor data) {
        LSearch search = LPreferences.getSearchControls();

        String column = "";
        switch (filterId) {
            case DBLoaderHelper.LOADER_TRANSACTION_FILTER_ALL:
                break;
            case DBLoaderHelper.LOADER_TRANSACTION_FILTER_BY_ACCOUNT:
                column = DBHelper.TABLE_COLUMN_ACCOUNT;
                break;
            case DBLoaderHelper.LOADER_TRANSACTION_FILTER_BY_CATEGORY:
                column = DBHelper.TABLE_COLUMN_CATEGORY;
                break;
            case DBLoaderHelper.LOADER_TRANSACTION_FILTER_BY_TAG:
                column = DBHelper.TABLE_COLUMN_TAG;
                break;
            case DBLoaderHelper.LOADER_TRANSACTION_FILTER_BY_VENDOR:
                column = DBHelper.TABLE_COLUMN_VENDOR;
                break;
        }

        sectionSummary.clear();

        // generate section summary
        boolean generateSummary = !TextUtils.isEmpty(column);
        if (data == null || data.getCount() <= 0) return;

        LAccountSummary summary;
        boolean hasLog = false;
        long lastId = 0;
        long lastIndex = 0;
        long id = 0;
        long indexId;
        long lastTransferId = 0;
        long lastTransferRid = 0;
        double v = 0, income = 0, expense = 0;
        int type;
        data.moveToFirst();
        do {
            indexId = data.getLong(0);
            type = data.getInt(data.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_TYPE));
            if ((type == LTransaction.TRANSACTION_TYPE_TRANSFER) ||
                    (type == LTransaction.TRANSACTION_TYPE_TRANSFER_COPY)) {
                if (0 == lastTransferId) {
                    sectionSummary.addVisible(indexId, true);
                    lastTransferId = indexId;
                    lastTransferRid = data.getLong(data.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_IRID));
                } else {
                    long rid = data.getLong(data.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_IRID));
                    if (rid == lastTransferRid) {
                        if (type == LTransaction.TRANSACTION_TYPE_TRANSFER) {
                            sectionSummary.addVisible(lastTransferId, false);
                            sectionSummary.addVisible(indexId, true);
                        } else {
                            sectionSummary.addVisible(indexId, false);
                        }
                        lastTransferId = 0;
                    } else {
                        sectionSummary.addVisible(indexId, true);
                        lastTransferId = indexId;
                        lastTransferRid = rid;
                    }
                }
            } else {
                sectionSummary.addVisible(indexId, true);
                lastTransferId = 0;
            }

            if (generateSummary) {
                v = data.getDouble(data.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_AMOUNT));
                id = data.getLong(data.getColumnIndexOrThrow(column));
                if (id < 0) id = 0;

                if (hasLog && id != lastId) {
                    //first transfer entry of new section is always visiable
                    sectionSummary.addVisible(indexId, true);

                    summary = new LAccountSummary();
                    summary.setExpense(expense);
                    summary.setIncome(income);
                    summary.setBalance(income - expense);

                    switch (filterId) {
                        case DBLoaderHelper.LOADER_TRANSACTION_FILTER_BY_ACCOUNT:
                            summary.setName(DBAccount.getInstance().getNameById(lastId));
                            if (search.isbAllTime()) setAccountSummary(summary, lastId);
                            break;
                        case DBLoaderHelper.LOADER_TRANSACTION_FILTER_BY_CATEGORY:
                            summary.setName(DBCategory.getInstance().getNameById(lastId));
                            break;
                        case DBLoaderHelper.LOADER_TRANSACTION_FILTER_BY_TAG:
                            summary.setName(DBTag.getInstance().getNameById(lastId));
                            break;
                        case DBLoaderHelper.LOADER_TRANSACTION_FILTER_BY_VENDOR:
                            summary.setName(DBVendor.getInstance().getNameById(lastId));
                            break;
                    }
                    sectionSummary.addSummary(lastIndex, summary);
                    hasLog = false;
                    income = 0;
                    expense = 0;
                    lastId = id;
                }

                lastIndex = indexId;

                if (type == LTransaction.TRANSACTION_TYPE_EXPENSE) expense += v;
                else if (type == LTransaction.TRANSACTION_TYPE_INCOME) income += v;
                else if (DBLoaderHelper.LOADER_TRANSACTION_FILTER_BY_ACCOUNT == filterId) {
                    if (type == LTransaction.TRANSACTION_TYPE_TRANSFER) expense += v;
                    else income += v;
                }
                if (!hasLog) lastId = id;
                hasLog = true;
            }
        } while (data.moveToNext());

        if (hasLog && generateSummary) {
            //first transfer entry of new section is always visiable
            sectionSummary.addVisible(indexId, true);

            summary = new LAccountSummary();
            summary.setExpense(expense);
            summary.setIncome(income);
            summary.setBalance(income - expense);

            switch (filterId) {
                case DBLoaderHelper.LOADER_TRANSACTION_FILTER_BY_ACCOUNT:
                    summary.setName(DBAccount.getInstance().getNameById(id));
                    if (search.isbAllTime()) setAccountSummary(summary, id);
                    break;
                case DBLoaderHelper.LOADER_TRANSACTION_FILTER_BY_CATEGORY:
                    summary.setName(DBCategory.getInstance().getNameById(id));
                    break;
                case DBLoaderHelper.LOADER_TRANSACTION_FILTER_BY_TAG:
                    summary.setName(DBTag.getInstance().getNameById(id));
                    break;
                case DBLoaderHelper.LOADER_TRANSACTION_FILTER_BY_VENDOR:
                    summary.setName(DBVendor.getInstance().getNameById(id));
                    break;
            }
            sectionSummary.addSummary(lastIndex, summary);
        }
    }

    private void initDbLoader() {
        int loaderCode;
        switch (AppPersistency.viewTransactionFilter) {
            default:
            case AppPersistency.TRANSACTION_FILTER_ALL:
                loaderCode = DBLoaderHelper.LOADER_TRANSACTION_FILTER_ALL;
                break;
            case AppPersistency.TRANSACTION_FILTER_BY_TAG:
                loaderCode = DBLoaderHelper.LOADER_TRANSACTION_FILTER_BY_TAG;
                break;
            case AppPersistency.TRANSACTION_FILTER_BY_ACCOUNT:
                loaderCode = DBLoaderHelper.LOADER_TRANSACTION_FILTER_BY_ACCOUNT;
                break;
            case AppPersistency.TRANSACTION_FILTER_BY_CATEGORY:
                loaderCode = DBLoaderHelper.LOADER_TRANSACTION_FILTER_BY_CATEGORY;
                break;
            case AppPersistency.TRANSACTION_FILTER_BY_VENDOR:
                loaderCode = DBLoaderHelper.LOADER_TRANSACTION_FILTER_BY_VENDOR;
                break;
        }

        //reiniitialize start/end time, so it tracks current user selection.
        dbLoaderHelper.initStartEndMs();
        dbLoaderHelper.restart(getLoaderManager(), loaderCode);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_view_transaction, container, false);

        myClickListener = new MyClickListener();

        selectTypeV = rootView.findViewById(R.id.selectType);
        selectTypeV.setVisibility(View.GONE);
        btnExpense = setViewListener(selectTypeV, R.id.expense);
        btnIncome = setViewListener(selectTypeV, R.id.income);
        btnTransaction = setViewListener(selectTypeV, R.id.transaction);

        //prevView = setViewListener(rootView, R.id.prev);
        //nextView = setViewListener(rootView, R.id.next);
        prevView = rootView.findViewById(R.id.prev);
        nextView = rootView.findViewById(R.id.next);
        LViewUtils.RepeatListener repeatListener = new LViewUtils.RepeatListener(1000, 300, myClickListener);
        prevView.setOnTouchListener(repeatListener);
        nextView.setOnTouchListener(repeatListener);

        filterView = (ImageView) setViewListener(rootView, R.id.filter);
        searchView = (ImageView) setViewListener(rootView, R.id.search);
        newRecordView = setViewListener(rootView, R.id.newRecord);
        chartView = setViewListener(rootView, R.id.tabChart);

        monthlyView = (TextView) setViewListener(rootView, R.id.monthly);
        customTimeView = (TextView) rootView.findViewById(R.id.customTime);

        viewFlipper = (ViewFlipper) rootView.findViewById(R.id.viewFlipper);
        viewFlipper.setAnimateFirstView(false);
        viewFlipper.setDisplayedChild(0);

        listViewFlipper = (ViewFlipper) rootView.findViewById(R.id.listViewFlipper);
        listViewFlipper.setAnimateFirstView(false);
        listViewFlipper.setDisplayedChild(0);

        sectionSummary = new LSectionSummary();
        listView = (ListView) rootView.findViewById(R.id.logsList);
        adapter = new MyCursorAdapter(getActivity(), null, sectionSummary);
        listView.setAdapter(adapter);

        onScrollListener = new ListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                //LLog.d(TAG, "scroll state changed");
                if (AppPersistency.lastTransactionChangeTimeMsHonored) {
                    AppPersistency.lastTransactionChangeTimeMs = 0;
                    AppPersistency.lastTransactionChangeTimeMsHonored = false;
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

            }
        };
        listView.setOnScrollListener(onScrollListener);

        View tmp = listViewFlipper.findViewById(R.id.logs);
        tmp.setOnClickListener(myClickListener);
        queryOrderIV = (ImageView) tmp.findViewById(R.id.ascend);
        queryOrderIV.setImageResource(LPreferences.getQueryOrderAscend() ? R.drawable.ic_action_expand : R.drawable
                .ic_action_collapse);
        dispV = tmp.findViewById(R.id.display);
        monthTV = (TextView) tmp.findViewById(R.id.month);
        balanceTV = (TextView) dispV.findViewById(R.id.balance);
        expenseTV = (TextView) dispV.findViewById(R.id.expense);
        incomeTV = (TextView) dispV.findViewById(R.id.income);

        tmp = listViewFlipper.findViewById(R.id.logsAlt);
        tmp.setOnClickListener(myClickListener);
        altQueryOrderIV = (ImageView) tmp.findViewById(R.id.ascend);
        altQueryOrderIV.setImageResource(LPreferences.getQueryOrderAscend() ? R.drawable.ic_action_expand : R
                .drawable.ic_action_collapse);
        altDispV = tmp.findViewById(R.id.display);
        altMonthTV = (TextView) tmp.findViewById(R.id.month);
        altBalanceTV = (TextView) altDispV.findViewById(R.id.balance);
        altExpenseTV = (TextView) altDispV.findViewById(R.id.expense);
        altIncomeTV = (TextView) altDispV.findViewById(R.id.income);

        if (AppPersistency.viewTransactionYear == -1 || AppPersistency.viewTransactionMonth == -1) {
            Calendar now = Calendar.getInstance();
            AppPersistency.viewTransactionYear = now.get(Calendar.YEAR);
            AppPersistency.viewTransactionMonth = now.get(Calendar.MONTH);
        }
        if (AppPersistency.viewTransactionQuarter == -1) {
            AppPersistency.viewTransactionQuarter = AppPersistency.viewTransactionMonth / 3;
        }

        dbLoaderHelper = new DBLoaderHelper(this.getActivity(), this);
        return rootView;
    }

    @Override
    public void onDestroyView() {
        prevView = null;
        nextView = null;
        monthlyView = null;
        customTimeView = null;

        viewFlipper.setInAnimation(null);
        viewFlipper.setOutAnimation(null);
        viewFlipper = null;

        listView.setOnScrollListener(null);
        onScrollListener = null;
        listView = null;

        listViewFlipper.setInAnimation(null);
        listViewFlipper.setOutAnimation(null);
        listViewFlipper = null;

        balanceTV = null;
        expenseTV = null;
        incomeTV = null;
        altBalanceTV = null;
        altExpenseTV = null;
        altIncomeTV = null;
        filterView = null;
        searchView = null;
        newRecordView = null;

        btnExpense = null;
        btnIncome = null;
        btnTransaction = null;
        selectTypeV = null;

        chartView = null;

        rootView = null;
        super.onDestroyView();
    }

    @Override
    public void onSelected(boolean selected) {
        if (edit != null) {
            edit.dismiss();
        }
        //if (selectTypeV != null) {
        //    selectTypeV.setVisibility(View.GONE);
        //}
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();

        showTime();
        showFilterView();

        LSearch search = LPreferences.getSearchControls();
        if (search.isbAllTime() && search.isbShowAll() && search.isbAllValue()) {
            //LViewUtils.setAlpha(searchView, 0.8f);
            searchView.setImageResource(R.drawable.ic_action_search);
        } else {
            //LViewUtils.setAlpha(searchView, 1.0f);
            searchView.setImageResource(R.drawable.ic_action_search_enabled);
        }
        dbLoaderHelper.restart(getLoaderManager(), DBLoaderHelper.LOADER_INIT_RANGE);
    }

    private class MyClickListener extends LOnClickListener {
        @Override
        public void onClicked(View v) {
            switch (v.getId()) {
                case R.id.expense:
                    newLog(LTransaction.TRANSACTION_TYPE_EXPENSE);
                    break;
                case R.id.income:
                    newLog(LTransaction.TRANSACTION_TYPE_INCOME);
                    break;
                case R.id.transaction:
                    newLog(LTransaction.TRANSACTION_TYPE_TRANSFER);
                    break;
                case R.id.filter:
                    changeFilter();
                    break;
                case R.id.search:
                    changeSearch();
                    break;
                case R.id.newRecord:
                    if (selectTypeV.getVisibility() != View.VISIBLE) {
                        selectTypeV.setVisibility(View.VISIBLE);
                    } else {
                        selectTypeV.setVisibility(View.GONE);
                    }
                    return;
                case R.id.prev:
                    showPrevNext(true);
                    break;
                case R.id.next:
                    showPrevNext(false);
                    break;
                case R.id.monthly:
                    changeTime();
                    break;
                case R.id.logs:
                case R.id.logsAlt:
                    LPreferences.setQueryOrderAscend(!LPreferences.getQueryOrderAscend());
                    queryOrderIV.setImageResource(LPreferences.getQueryOrderAscend() ? R.drawable.ic_action_expand :
                            R.drawable.ic_action_collapse);
                    altQueryOrderIV.setImageResource(LPreferences.getQueryOrderAscend() ? R.drawable.ic_action_expand
                            : R.drawable.ic_action_collapse);
                    initDbLoader();
                    break;
                case R.id.tabChart:
                    startActivity(new Intent(ViewTransactionFragment.this.getActivity(), ChartActivity.class));
                    break;

                default:
                    break;
            }
            selectTypeV.setVisibility(View.GONE);
        }
    }

    private static boolean listItemClicked = false;

    private class MyCursorAdapter extends CursorAdapter implements TransactionEdit.TransitionEditItf {
        private LTransaction item;
        private LTransaction itemOrig;
        private LSectionSummary sectionSummary;
        private ClickListener clickListener;

        @Override
        public Cursor swapCursor(Cursor newCursor) {
            return super.swapCursor(newCursor);
        }

        public MyCursorAdapter(Context context, Cursor cursor) {
            //TODO: deprecated API is used here for max OS compatibility, provide alternative
            //      using LoaderManager with a CursorLoader.
            //super(context, cursor, 0);
            super(context, cursor, false);
            sectionSummary = null;
            clickListener = new ClickListener();
        }

        public MyCursorAdapter(Context context, Cursor cursor, LSectionSummary sectionSummary) {
            //TODO: deprecated API is used here for max OS compatibility, provide alternative
            //      using LoaderManager with a CursorLoader.
            //super(context, cursor, 0);
            super(context, cursor, false);
            this.sectionSummary = sectionSummary;
            clickListener = new ClickListener();
        }

        /**
         * Bind an existing view to the data pointed to by cursor
         */
        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            VTag vTag = (VTag) view.getTag();
            long id = cursor.getLong(0);
            vTag.id = id;

            View mainView = vTag.mainView;
            View sectionView = vTag.sectionView;

            int categoryId = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_CATEGORY));
            int tagId = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_TAG));
            int type = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_TYPE));

            String category = DBCategory.getInstance().getNameById(categoryId);
            String tag = DBTag.getInstance().getNameById(tagId);

            if ((type == LTransaction.TRANSACTION_TYPE_TRANSFER) || (type == LTransaction
                    .TRANSACTION_TYPE_TRANSFER_COPY)) {
                if (!sectionSummary.isVisible(id)) {
                    mainView.setVisibility(View.GONE);
                } else {
                    mainView.setVisibility(View.VISIBLE);

                    int accountId = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_ACCOUNT));
                    int account2Id = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_ACCOUNT2));
                    DBAccount dbAccount = DBAccount.getInstance();
                    String account = dbAccount.getNameById(accountId);
                    String account2 = dbAccount.getNameById(account2Id);
                    if (AppPersistency.TRANSACTION_FILTER_BY_ACCOUNT != AppPersistency.viewTransactionFilter) {
                        if (type == LTransaction.TRANSACTION_TYPE_TRANSFER) {
                            vTag.categoryTV.setText(account + " --> " + account2);
                        } else {
                            vTag.categoryTV.setText(account + " <-- " + account2);
                        }
                    } else {
                        if (type == LTransaction.TRANSACTION_TYPE_TRANSFER) {
                            vTag.categoryTV.setText(getActivity().getResources().getString(R.string
                                    .transfer_to_report_view) + " " + account2);
                        } else {
                            vTag.categoryTV.setText(getActivity().getResources().getString(R.string
                                    .transfer_from_report_view) + " " + account2);
                        }
                    }
                    String note = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_NOTE)).trim();
                    vTag.noteTV.setText(note);
                }
            } else {
                mainView.setVisibility(View.VISIBLE);

                String str = "";
                if (!TextUtils.isEmpty(tag)) {
                    str = tag;
                    if (!TextUtils.isEmpty(category)) str += ":" + category;
                } else {
                    if (!TextUtils.isEmpty(category)) str = category;
                }
                vTag.categoryTV.setText(str);

                int vendorId = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_VENDOR));
                String vendor = DBVendor.getInstance().getNameById(vendorId);
                String note = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_NOTE)).trim();

                if (TextUtils.isEmpty(vendor)) {
                    if ((note != null) && (!TextUtils.isEmpty(note))) vendor = note;
                } else {
                    if ((note != null) && (!TextUtils.isEmpty(note))) vendor += " - " + note;
                }
                vTag.noteTV.setText(vendor);
            }

            long tm = cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_TIMESTAMP));
            vTag.dateTV.setText(new SimpleDateFormat("MMM d, yyy").format(tm));

            Resources rsc = getActivity().getResources();
            switch (type) {
                case LTransaction.TRANSACTION_TYPE_EXPENSE:
                    vTag.amountTV.setTextColor(rsc.getColor(R.color.base_red));
                    break;
                case LTransaction.TRANSACTION_TYPE_INCOME:
                    vTag.amountTV.setTextColor(rsc.getColor(R.color.base_green));
                    break;
                case LTransaction.TRANSACTION_TYPE_TRANSFER:
                case LTransaction.TRANSACTION_TYPE_TRANSFER_COPY:
                    vTag.amountTV.setTextColor(rsc.getColor(R.color.base_blue));
                    break;
            }

            //String uuid = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_RID));
            //Long modms = cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_TIMESTAMP_LAST_CHANGE));

            double dollar = cursor.getDouble(cursor.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_AMOUNT));
            vTag.amountTV.setText(String.format("%.2f", dollar));

            if (sectionSummary.hasId(id)) {
                LAccountSummary summary = sectionSummary.getSummaryById(id);
                vTag.sortNameTV.setText(summary.getName());

                if (summary.getBalance() < 0) {
                    vTag.balanceTV.setTextColor(getActivity().getResources().getColor(R.color.base_red));
                    vTag.balanceTV.setText(String.format("%.2f", -summary.getBalance()));
                } else {
                    vTag.balanceTV.setTextColor(getActivity().getResources().getColor(R.color.base_green));
                    vTag.balanceTV.setText(String.format("%.2f", summary.getBalance()));
                }

                vTag.incomeTV.setText(String.format("%.2f", summary.getIncome()));
                vTag.expenseTV.setText(String.format("%.2f", summary.getExpense()));

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
            View mainView = newView.findViewById(R.id.mainView);
            mainView.setOnClickListener(clickListener);

            VTag vTag = new VTag(0);
            vTag.categoryTV = (TextView) mainView.findViewById(R.id.category);
            vTag.amountTV = (TextView) mainView.findViewById(R.id.dollor);
            vTag.noteTV = (TextView) mainView.findViewById(R.id.note);
            vTag.dateTV = (TextView) mainView.findViewById(R.id.date);

            mainView.setTag(vTag);

            vTag.mainView = mainView;

            View sectionView = newView.findViewById(R.id.sectionView);
            vTag.sectionView = sectionView;
            vTag.sortNameTV = (TextView) sectionView.findViewById(R.id.sortName);
            vTag.balanceTV = (TextView) sectionView.findViewById(R.id.balance);
            vTag.incomeTV = (TextView) sectionView.findViewById(R.id.income);
            vTag.expenseTV = (TextView) sectionView.findViewById(R.id.expense);

            newView.setTag(vTag);

            return newView;
        }

        private class ClickListener extends LOnClickListener {
            @Override
            public void onClicked(View v) {
                if (listItemClicked) return;
                else listItemClicked = true;

                selectTypeV.setVisibility(View.GONE);

                VTag tag = (VTag) v.getTag();

                item = DBTransaction.getInstance().getById(tag.id);

                //handle edit of transfer: require both records to be present, and only edit
                //the original record (not the duplicate)
                boolean allowEdit = true;

                DBTransaction dbTransaction = DBTransaction.getInstance();
                if (item.getType() == LTransaction.TRANSACTION_TYPE_TRANSFER) {
                    if (null == dbTransaction.getByRid(item.getRid(), true)) allowEdit = false;
                } else if (item.getType() == LTransaction.TRANSACTION_TYPE_TRANSFER_COPY) {
                    LTransaction item2 = dbTransaction.getByRid(item.getRid(), false);
                    if (null == item2) {
                        allowEdit = false;
                    } else {
                        item = item2;
                    }
                }

                itemOrig = new LTransaction(item);

                edit = new TransactionEdit(getActivity(), rootView, item, false, false, allowEdit, MyCursorAdapter
                        .this);

                viewFlipper.setInAnimation(getActivity(), R.anim.slide_in_right);
                viewFlipper.setOutAnimation(getActivity(), R.anim.slide_out_left);
                viewFlipper.showNext();
            }
        }

        @Override
        public void onTransactionEditExit(int action, boolean changed) {
            edit = null;
            switch (action) {
                case TransactionEdit.TransitionEditItf.EXIT_OK:
                    AppPersistency.transactionChanged = changed;
                    if (changed) {
                        item.setTimeStampLast(LPreferences.getServerUtc());

                        AppPersistency.lastTransactionChangeTimeMs = item.getTimeStamp();
                        AppPersistency.lastTransactionChangeTimeMsHonored = false;

                        DBTransaction dbTransaction = DBTransaction.getInstance();
                        if (item.getType() == LTransaction.TRANSACTION_TYPE_TRANSFER)
                            dbTransaction.update2(item);
                        else
                            dbTransaction.update(item);

                        LJournal journal = new LJournal();
                        journal.updateRecord(item.getId());

                        onSelected(true);
                    }
                    break;

                case TransactionEdit.TransitionEditItf.EXIT_CANCEL:
                    break;

                case TransactionEdit.TransitionEditItf.EXIT_DELETE:
                    AppPersistency.transactionChanged = true;

                    DBTransaction dbTransaction = DBTransaction.getInstance();
                    if (itemOrig.getType() == LTransaction.TRANSACTION_TYPE_TRANSFER)
                        dbTransaction.deleteTransferByRid(itemOrig.getRid());
                    else
                        dbTransaction.deleteById(itemOrig.getId());

                    LJournal journal = new LJournal();
                    journal.deleteRecord(itemOrig.getId());

                    onSelected(true);
                    break;
            }
            viewFlipper.setInAnimation(getActivity(), R.anim.slide_in_left);
            viewFlipper.setOutAnimation(getActivity(), R.anim.slide_out_right);
            viewFlipper.showPrevious();
            listItemClicked = false;
        }

        private class VTag {
            long id;
            View mainView, sectionView;
            TextView categoryTV, noteTV, dateTV, amountTV;
            TextView sortNameTV, balanceTV, incomeTV, expenseTV;

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

    private void showBalance(boolean altView, Cursor data) {
        TextView mtv, btv, itv, etv;
        //View dispv;

        if (altView) {
            mtv = altMonthTV;
            btv = altBalanceTV;
            itv = altIncomeTV;
            etv = altExpenseTV;
            //dispv = altDispV;
        } else {
            mtv = monthTV;
            btv = balanceTV;
            itv = incomeTV;
            etv = expenseTV;
            //dispv = dispV;
        }

        LSearch search = LPreferences.getSearchControls();
        if (search.isbAllValue() && (search.isbShowAll() || (search.getCategories() == null &&
                search.getTags() == null &&
                search.getVendors() == null &&
                search.getTypes() == null))) {
            //dispv.setVisibility(View.VISIBLE);
            btv.setVisibility(View.VISIBLE);
        } else {
            //dispv.setVisibility(View.INVISIBLE);
            btv.setVisibility(View.INVISIBLE);
        }
        //mtv.setVisibility(search.isbAllValue() ? View.INVISIBLE : View.VISIBLE);

        LAccountSummary summary = new LAccountSummary();
        getBalance(summary, data);

        if (summary.getBalance() < 0) {
            btv.setTextColor(getActivity().getResources().getColor(R.color.base_red));
        } else {
            btv.setTextColor(getActivity().getResources().getColor(R.color.base_green));
        }
        btv.setText(String.format("%.2f", Math.abs(summary.getBalance())));

        itv.setText(String.format("%.2f", summary.getIncome()));
        etv.setText(String.format("%.2f", summary.getExpense()));

        if (search.isbAllTime() /*&& data != null && data.getCount() > 0*/) {
            switch (AppPersistency.viewTransactionTime) {
                case AppPersistency.TRANSACTION_TIME_ALL:
                    mtv.setText(getString(R.string.balance));
                    break;
                case AppPersistency.TRANSACTION_TIME_MONTHLY:
                    mtv.setText(new DateFormatSymbols().getMonths()[AppPersistency.viewTransactionMonth]);
                    break;
                case AppPersistency.TRANSACTION_TIME_QUARTERLY:
                    mtv.setText("Q" + (AppPersistency.viewTransactionQuarter + 1) + " " + AppPersistency
                            .viewTransactionYear);
                    break;

                case AppPersistency.TRANSACTION_TIME_ANNUALLY:
                    mtv.setText("" + AppPersistency.viewTransactionYear);
                    break;
            }
        } else {
            mtv.setText(getString(R.string.balance));
        }
    }

    // only valid for unfiltered all records for accounts,
    private void getBalance(LAccountSummary summary, Cursor data) {
        if (data == null || data.getCount() < 1) return;

        LSearch search = LPreferences.getSearchControls();
        DBAccountBalance.getAccountSummaryForCurrentCursor(summary, data, search.isbShowAll() ? null : search.getAccounts());
        if (!search.isbAllTime()) {
            summary.setBalance(summary.getIncome() - summary.getExpense());
            return;
        }

        if (allBalances == null) return;

        //get balance for All accounts at current year/month
        switch (AppPersistency.viewTransactionTime) {
            case AppPersistency.TRANSACTION_TIME_ALL:
                if (search.isbShowAll() || search.getAccounts() == null)
                    summary.setBalance(allBalances.getBalance());
                else
                    summary.setBalance(allBalances.getBalance(search.getAccounts()));
                break;
            case AppPersistency.TRANSACTION_TIME_MONTHLY:
                if (search.isbShowAll() || search.getAccounts() == null)
                    summary.setBalance(allBalances.getBalance(
                            AppPersistency.viewTransactionYear, AppPersistency.viewTransactionMonth));
                else
                    summary.setBalance(allBalances.getBalance(search.getAccounts(),
                            AppPersistency.viewTransactionYear, AppPersistency.viewTransactionMonth));
                break;
            case AppPersistency.TRANSACTION_TIME_QUARTERLY:
                if (search.isbShowAll() || search.getAccounts() == null)
                    summary.setBalance(allBalances.getBalance(
                            AppPersistency.viewTransactionYear, AppPersistency.viewTransactionQuarter * 3 + 2));
                else
                    summary.setBalance(allBalances.getBalance(search.getAccounts(),
                            AppPersistency.viewTransactionYear, AppPersistency.viewTransactionQuarter * 3 + 2));

                break;
            case AppPersistency.TRANSACTION_TIME_ANNUALLY:
                if (search.isbShowAll() || search.getAccounts() == null)
                    summary.setBalance(allBalances.getBalance(AppPersistency.viewTransactionYear, 11));
                else
                    summary.setBalance(allBalances.getBalance(search.getAccounts(), AppPersistency
                            .viewTransactionYear, 11));
                break;
        }
    }

    private boolean isAltView = false;

    private boolean prevNext(boolean prev) {
        boolean ret = true;
        int year = AppPersistency.viewTransactionYear;
        int month = AppPersistency.viewTransactionMonth;
        int quarter = AppPersistency.viewTransactionQuarter;

        switch (AppPersistency.viewTransactionTime) {
            case AppPersistency.TRANSACTION_TIME_ALL:
                break;
            case AppPersistency.TRANSACTION_TIME_MONTHLY:
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
                if (ym < dbLoaderHelper.getAllStartMs() || ym >= dbLoaderHelper.getAllEndMs()) {
                    AppPersistency.viewTransactionYear = year;
                    AppPersistency.viewTransactionMonth = month;
                    ret = false;
                }
                break;

            /*case AppPersistency.TRANSACTION_TIME_QUARTERLY:
                AppPersistency.viewTransactionQuarter += (prev) ? -1 : 1;
                if (AppPersistency.viewTransactionQuarter >= 4) {
                    AppPersistency.viewTransactionQuarter = 0;
                    AppPersistency.viewTransactionYear++;
                } else if (AppPersistency.viewTransactionQuarter < 0) {
                    AppPersistency.viewTransactionQuarter = 3;
                    AppPersistency.viewTransactionYear--;
                }

                long ymS = getMs(AppPersistency.viewTransactionYear, AppPersistency.viewTransactionQuarter * 3);
                long ymE = getMs(AppPersistency.viewTransactionYear, (AppPersistency.viewTransactionQuarter + 1) * 3);

                if (dbLoaderHelper.getAllStartMs() >= ymE || dbLoaderHelper.getAllEndMs() <= ymS) {
                    AppPersistency.viewTransactionYear = year;
                    AppPersistency.viewTransactionQuarter = quarter;
                    return false;
                }
                return true;
                */

            case AppPersistency.TRANSACTION_TIME_ANNUALLY:
                AppPersistency.viewTransactionYear += (prev) ? -1 : 1;
                long ymS = getMs(AppPersistency.viewTransactionYear, 0);
                long ymE = getMs(AppPersistency.viewTransactionYear + 1, 0);
                if (dbLoaderHelper.getAllStartMs() >= ymE || dbLoaderHelper.getAllEndMs() <= ymS) {
                    AppPersistency.viewTransactionYear = year;
                    return false;
                }
                break;
        }

        showPrevNextControls();
        return ret;
    }

    private long lastClickMs;

    private void showPrevNext(boolean prev) {
        if (!prevNext(prev)) return;

        // save index and top position
        int index = listView.getFirstVisiblePosition();
        View v = listView.getChildAt(0);
        int top = (v == null) ? 0 : (v.getTop() - listView.getPaddingTop());

        AppPersistency.setViewHistory(AppPersistency.getViewLevel(), new AppPersistency.ListViewHistory(index, top));

        long now = System.currentTimeMillis();
        boolean animate = (now - lastClickMs > 1000);
        lastClickMs = now;

        isAltView = !isAltView;
        if (prev) {
            if (animate) {
                listViewFlipper.setInAnimation(getActivity(), R.anim.slide_in_left);
                listViewFlipper.setOutAnimation(getActivity(), R.anim.slide_out_right);
            } else {
                listViewFlipper.setInAnimation(null);
                listViewFlipper.setOutAnimation(null);
            }
            listViewFlipper.showPrevious();
            AppPersistency.setViewLevel(AppPersistency.getViewLevel() - 1);
        } else {
            if (animate) {
                listViewFlipper.setInAnimation(getActivity(), R.anim.slide_in_right);
                listViewFlipper.setOutAnimation(getActivity(), R.anim.slide_out_left);
            } else {
                listViewFlipper.setInAnimation(null);
                listViewFlipper.setOutAnimation(null);
            }
            listViewFlipper.showNext();
            AppPersistency.setViewLevel(AppPersistency.getViewLevel() + 1);
        }
        initDbLoader();
    }

    private LTransaction newItem;

    private void newLog(int type) {
        newItem = new LTransaction();
        newItem.setType(type);

        edit = new TransactionEdit(getActivity(), rootView, newItem, true, false, true, this);

        viewFlipper.setInAnimation(getActivity(), R.anim.slide_in_right);
        viewFlipper.setOutAnimation(getActivity(), R.anim.slide_out_left);
        viewFlipper.showNext();
    }

    @Override
    public void onTransactionEditExit(int action, boolean changed) {
        viewFlipper.setInAnimation(getActivity(), R.anim.slide_in_left);
        viewFlipper.setOutAnimation(getActivity(), R.anim.slide_out_right);
        viewFlipper.showPrevious();
        edit = null;
    }

    private View setViewListener(View v, int id) {
        View view = v.findViewById(id);
        view.setOnClickListener(myClickListener);
        return view;
    }

    private void nextFilter() {
        switch (AppPersistency.viewTransactionFilter) {
            case AppPersistency.TRANSACTION_FILTER_ALL:
                AppPersistency.viewTransactionFilter = AppPersistency.TRANSACTION_FILTER_BY_ACCOUNT;
                break;
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

    @Override
    public void onTransactionSearchDialogDismiss(boolean changed) {
        if (changed) {
            showTime();

            LSearch search = LPreferences.getSearchControls();
            if (search.isbAllTime() && search.isbShowAll() && search.isbAllValue()) {
                //LViewUtils.setAlpha(searchView, 0.8f);
                searchView.setImageResource(R.drawable.ic_action_search);
            } else {
                //LViewUtils.setAlpha(searchView, 1.0f);
                searchView.setImageResource(R.drawable.ic_action_search_enabled);
            }

            AppPersistency.clearViewHistory();
            dbLoaderHelper.restart(getLoaderManager(), DBLoaderHelper.LOADER_INIT_RANGE);
        }
    }

    private void changeSearch() {
        TransactionSearchDialog dialog = new TransactionSearchDialog(getActivity(), this);
        dialog.show();
    }

    private void changeFilter() {
        //getLoaderManager().destroyLoader(AppPersistency.viewTransactionFilter);
        nextFilter();
        showFilterView();

        AppPersistency.clearViewHistory();
        initDbLoader();
    }

    private void showFilterView() {
        switch (AppPersistency.viewTransactionFilter) {
            case AppPersistency.TRANSACTION_FILTER_ALL:
                filterView.setImageResource(R.drawable.ic_menu_sort_by_size);
                break;
            case AppPersistency.TRANSACTION_FILTER_BY_ACCOUNT:
                filterView.setImageResource(R.drawable.ic_menu_sort_by_account);
                break;
            case AppPersistency.TRANSACTION_FILTER_BY_CATEGORY:
                filterView.setImageResource(R.drawable.ic_menu_sort_by_category);
                break;
            case AppPersistency.TRANSACTION_FILTER_BY_TAG:
                filterView.setImageResource(R.drawable.ic_menu_sort_by_tag);
                break;
            case AppPersistency.TRANSACTION_FILTER_BY_VENDOR:
                filterView.setImageResource(R.drawable.ic_menu_sort_by_payer);
                break;
        }
    }

    private void showPrevNextControls() {
        LSearch search = LPreferences.getSearchControls();
        if (!search.isbAllTime()) {
            prevView.setVisibility(View.GONE);
            nextView.setVisibility(View.GONE);
        } else {
            switch (AppPersistency.viewTransactionTime) {
                case AppPersistency.TRANSACTION_TIME_ALL:
                    prevView.setVisibility(View.GONE);
                    nextView.setVisibility(View.GONE);
                    break;
                case AppPersistency.TRANSACTION_TIME_MONTHLY:
                    if (AppPersistency.viewTransactionYear <= dbLoaderHelper.getAllStartYear() &&
                            AppPersistency.viewTransactionMonth <= dbLoaderHelper.getAllStartMonth()) {
                        prevView.setVisibility(View.GONE);
                    } else {
                        prevView.setVisibility(View.VISIBLE);
                    }

                    if (AppPersistency.viewTransactionYear >= dbLoaderHelper.getAllEndYear() &&
                            AppPersistency.viewTransactionMonth >= dbLoaderHelper.getAllEndMonth()) {
                        nextView.setVisibility(View.GONE);
                    } else {
                        nextView.setVisibility(View.VISIBLE);
                    }
                    break;

                case AppPersistency.TRANSACTION_TIME_ANNUALLY:
                    if (AppPersistency.viewTransactionYear <= dbLoaderHelper.getAllStartYear()) {
                        prevView.setVisibility(View.GONE);
                    } else {
                        prevView.setVisibility(View.VISIBLE);
                    }
                    if (AppPersistency.viewTransactionYear >= dbLoaderHelper.getAllEndYear()) {
                        nextView.setVisibility(View.GONE);
                    } else {
                        nextView.setVisibility(View.VISIBLE);
                    }
                    break;
            }
        }
    }

    private void showTime() {
        showPrevNextControls();

        LSearch search = LPreferences.getSearchControls();
        if (search.isbAllTime()) {
            //customTimeView.setVisibility(View.GONE);
            //monthlyView.setVisibility(View.VISIBLE);
            monthlyView.setEnabled(true);
        } else {
            //customTimeView.setVisibility(View.VISIBLE);
            //monthlyView.setVisibility(View.GONE);
            monthlyView.setText("");
            monthlyView.setEnabled(false);

            //if (LPreferences.getSearchFilterByEditTIme()) {
            //    customTimeView.setText("M: " + (new SimpleDateFormat("MMM d, yyy").format(LPreferences
            //            .getSearchAllTimeFrom()) + " - " +
            //            new SimpleDateFormat("MMM d, yyy").format(LPreferences.getSearchAllTimeTo())));
            //} else {
            //    customTimeView.setText(new SimpleDateFormat("MMM d, yyy").format(LPreferences.getSearchAllTimeFrom())
            //            + " - " +
            //            new SimpleDateFormat("MMM d, yyy").format(LPreferences.getSearchAllTimeTo()));
            //}
            //prevView.setEnabled(false);
            //nextView.setEnabled(false);
            return;
        }

        switch (AppPersistency.viewTransactionTime) {
            case AppPersistency.TRANSACTION_TIME_MONTHLY:
                monthlyView.setText(getActivity().getString(R.string.monthly));
                break;
            case AppPersistency.TRANSACTION_TIME_ALL:
                monthlyView.setText(getActivity().getString(R.string.all));
                break;
            case AppPersistency.TRANSACTION_TIME_ANNUALLY:
                monthlyView.setText(getActivity().getString(R.string.annually));
                break;
            case AppPersistency.TRANSACTION_TIME_QUARTERLY:
                monthlyView.setText(getActivity().getString(R.string.quarterly));
                break;
        }
    }

    private void nextTime() {
        switch (AppPersistency.viewTransactionTime) {
            case AppPersistency.TRANSACTION_TIME_ALL:
                AppPersistency.viewTransactionTime = AppPersistency.TRANSACTION_TIME_MONTHLY;
                break;
            case AppPersistency.TRANSACTION_TIME_ANNUALLY:
                if (dbLoaderHelper.getAllStartYear() != dbLoaderHelper.getAllEndYear()) {
                    AppPersistency.viewTransactionTime = AppPersistency.TRANSACTION_TIME_ALL;
                } else {
                    AppPersistency.viewTransactionTime = AppPersistency.TRANSACTION_TIME_MONTHLY;
                }
                break;
            //case AppPersistency.TRANSACTION_TIME_QUARTERLY:
            //    AppPersistency.viewTransactionTime = AppPersistency.TRANSACTION_TIME_ANNUALLY;
            //    break;
            case AppPersistency.TRANSACTION_TIME_MONTHLY:
                //AppPersistency.viewTransactionTime = AppPersistency.TRANSACTION_TIME_QUARTERLY;
                //AppPersistency.viewTransactionQuarter = AppPersistency.viewTransactionMonth / 3;
                AppPersistency.viewTransactionTime = AppPersistency.TRANSACTION_TIME_ANNUALLY;
                break;
        }
        showTime();
    }

    private void changeTime() {
        nextTime();

        AppPersistency.clearViewHistory();
        initDbLoader();
    }
}


