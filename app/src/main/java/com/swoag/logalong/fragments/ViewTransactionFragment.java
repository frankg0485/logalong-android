package com.swoag.logalong.fragments;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import android.content.Context;
import android.content.Loader;
import android.content.res.Resources;
import android.database.Cursor;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.widget.CursorAdapter;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.swoag.logalong.LFragment;
import com.swoag.logalong.MainActivity;
import com.swoag.logalong.R;
import com.swoag.logalong.entities.LAccountSummary;
import com.swoag.logalong.entities.LAllBalances;
import com.swoag.logalong.entities.LJournal;
import com.swoag.logalong.entities.LTransaction;
import com.swoag.logalong.entities.LSectionSummary;
import com.swoag.logalong.utils.AppPersistency;
import com.swoag.logalong.utils.DBAccess;
import com.swoag.logalong.utils.DBAccount;
import com.swoag.logalong.utils.DBCategory;
import com.swoag.logalong.utils.DBHelper;
import com.swoag.logalong.utils.DBLoaderHelper;
import com.swoag.logalong.utils.DBProvider;
import com.swoag.logalong.utils.DBTag;
import com.swoag.logalong.utils.DBTransaction;
import com.swoag.logalong.utils.DBVendor;
import com.swoag.logalong.utils.LLog;
import com.swoag.logalong.utils.LOnClickListener;
import com.swoag.logalong.utils.LPreferences;
import com.swoag.logalong.utils.LViewUtils;
import com.swoag.logalong.views.LShareAccountDialog;
import com.swoag.logalong.views.TransactionSearchDialog;

import org.w3c.dom.Text;

import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

public class ViewTransactionFragment extends LFragment implements DBLoaderHelper.DBLoaderHelperCallbacks,
        TransactionSearchDialog.TransactionSearchDialogItf {
    private static final String TAG = ViewTransactionFragment.class.getSimpleName();

    private ListView listView;
    private ImageView filterView, searchView;

    private MyCursorAdapter adapter;
    private TextView monthTV, balanceTV, incomeTV, expenseTV, altMonthTV, altBalanceTV, altIncomeTV, altExpenseTV;
    private View dispV, altDispV;
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

        listView.post(new Runnable() {
            @Override
            public void run() {
                //listView.setSelection(adapter.getCount() - 1);
                Parcelable history = AppPersistency.getViewHistory(AppPersistency.getViewLevel());
                if (history != null) {
                    listView.onRestoreInstanceState(history);
                } else {
                    listView.setSelection(adapter.getCount() - 1);
                }
            }
        });
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
            summary.setBalance(allBalances.getBalance(id, AppPersistency.viewTransactionYear, AppPersistency.viewTransactionMonth));
        } else {
            //quarterly
            summary.setBalance(allBalances.getBalance(id, AppPersistency.viewTransactionYear, AppPersistency.viewTransactionQuarter * 3 + 2));
        }
    }

    private void setSectionSummary(int filterId, Cursor data) {
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
        String lastTransferRid = "";
        double v = 0, income = 0, expense = 0;
        int type;
        data.moveToFirst();
        do {
            indexId = data.getLong(0);
            type = data.getInt(data.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_TYPE));
            if ((type == LTransaction.TRANSACTION_TYPE_TRANSFER) ||
                    (type == LTransaction.TRANSACTION_TYPE_TRANSFER_COPY) ) {
                if (0 == lastTransferId) {
                    sectionSummary.addVisible(indexId, true);
                    lastTransferId = indexId;
                    lastTransferRid = data.getString(data.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_RID));
                } else {
                    String rid = data.getString(data.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_RID));
                    if (rid.startsWith(lastTransferRid.substring(0, 35))) {
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
                            summary.setName(DBAccount.getNameById(lastId));
                            if (LPreferences.getSearchAllTime()) setAccountSummary(summary, lastId);
                            break;
                        case DBLoaderHelper.LOADER_TRANSACTION_FILTER_BY_CATEGORY:
                            summary.setName(DBCategory.getNameById(lastId));
                            break;
                        case DBLoaderHelper.LOADER_TRANSACTION_FILTER_BY_TAG:
                            summary.setName(DBTag.getNameById(lastId));
                            break;
                        case DBLoaderHelper.LOADER_TRANSACTION_FILTER_BY_VENDOR:
                            summary.setName(DBVendor.getNameById(lastId));
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
                    summary.setName(DBAccount.getNameById(id));
                    if (LPreferences.getSearchAllTime()) setAccountSummary(summary, id);
                    break;
                case DBLoaderHelper.LOADER_TRANSACTION_FILTER_BY_CATEGORY:
                    summary.setName(DBCategory.getNameById(id));
                    break;
                case DBLoaderHelper.LOADER_TRANSACTION_FILTER_BY_TAG:
                    summary.setName(DBTag.getNameById(id));
                    break;
                case DBLoaderHelper.LOADER_TRANSACTION_FILTER_BY_VENDOR:
                    summary.setName(DBVendor.getNameById(id));
                    break;
            }
            sectionSummary.addSummary(lastIndex, summary);
        }
    }

    private void initDbLoader() {
        int loaderCode;
        switch (AppPersistency.viewTransactionFilter)
        {
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

        prevView = setViewListener(rootView, R.id.prev);
        nextView = setViewListener(rootView, R.id.next);
        filterView = (ImageView) setViewListener(rootView, R.id.filter);
        searchView = (ImageView) setViewListener(rootView, R.id.search);

        if (LPreferences.getSearchAllTime() && LPreferences.getSearchAll()) {
            LViewUtils.setAlpha(searchView, 0.8f);
        } else {
            LViewUtils.setAlpha(searchView, 1.0f);
        }

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

        View tmp = listViewFlipper.findViewById(R.id.logs);
        tmp.setOnClickListener(myClickListener);
        queryOrderIV = (ImageView) tmp.findViewById(R.id.ascend);
        queryOrderIV.setImageResource(LPreferences.getQueryOrderAscend()? R.drawable.ic_action_expand : R.drawable.ic_action_collapse);
        dispV = tmp.findViewById(R.id.display);
        monthTV = (TextView) tmp.findViewById(R.id.month);
        balanceTV = (TextView) dispV.findViewById(R.id.balance);
        expenseTV = (TextView) dispV.findViewById(R.id.expense);
        incomeTV = (TextView) dispV.findViewById(R.id.income);

        tmp = listViewFlipper.findViewById(R.id.logsAlt);
        tmp.setOnClickListener(myClickListener);
        altQueryOrderIV = (ImageView) tmp.findViewById(R.id.ascend);
        altQueryOrderIV.setImageResource(LPreferences.getQueryOrderAscend()? R.drawable.ic_action_expand : R.drawable.ic_action_collapse);
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
        if (edit != null) {
            edit.dismiss();
        }
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

        if (LPreferences.getSearchAllTime() && LPreferences.getSearchAll()) {
            LViewUtils.setAlpha(searchView, 0.8f);
        } else {
            LViewUtils.setAlpha(searchView, 1.0f);
        }
        dbLoaderHelper.restart(getLoaderManager(), DBLoaderHelper.LOADER_INIT_RANGE);
    }

    private class MyClickListener extends LOnClickListener {
        @Override
        public void onClicked(View v) {
            switch (v.getId()) {
                case R.id.filter:
                    changeFilter();
                    break;
                case R.id.search:
                    changeSearch();
                    break;
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
                    queryOrderIV.setImageResource(LPreferences.getQueryOrderAscend() ? R.drawable.ic_action_expand : R.drawable.ic_action_collapse);
                    altQueryOrderIV.setImageResource(LPreferences.getQueryOrderAscend()? R.drawable.ic_action_expand : R.drawable.ic_action_collapse);
                    initDbLoader();
                    break;
                default:
                    break;
            }
        }
    }

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

            String category = DBCategory.getNameById(categoryId);
            String tag = DBTag.getNameById(tagId);

            if ((type == LTransaction.TRANSACTION_TYPE_TRANSFER) || (type == LTransaction.TRANSACTION_TYPE_TRANSFER_COPY)) {
                if (!sectionSummary.isVisible(id)) {
                    mainView.setVisibility(View.GONE);
                } else{
                    mainView.setVisibility(View.VISIBLE);

                    int accountId = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_ACCOUNT));
                    int account2Id = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_ACCOUNT2));
                    String account = DBAccount.getNameById(accountId);
                    String account2 = DBAccount.getNameById(account2Id);
                    if (AppPersistency.TRANSACTION_FILTER_BY_ACCOUNT != AppPersistency.viewTransactionFilter) {
                        if (type == LTransaction.TRANSACTION_TYPE_TRANSFER) {
                            vTag.categoryTV.setText(account + " --> " + account2);
                        } else {
                            vTag.categoryTV.setText(account + " <-- " + account2);
                        }
                    } else {
                        if (type == LTransaction.TRANSACTION_TYPE_TRANSFER) {
                            vTag.categoryTV.setText(getActivity().getResources().getString(R.string.transfer_to_report_view) + " " + account2);
                        } else {
                            vTag.categoryTV.setText(getActivity().getResources().getString(R.string.transfer_from_report_view) + " " + account2);
                        }
                    }
                    String note = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_NOTE)).trim();
                    vTag.noteTV.setText(note);
                }
            }
            else {
                mainView.setVisibility(View.VISIBLE);

                String str = "";
                if (!TextUtils.isEmpty(tag)) str = tag + ":";
                str += category;
                vTag.categoryTV.setText(str);

                int vendorId = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_VENDOR));
                String vendor = DBVendor.getNameById(vendorId);
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
                VTag tag = (VTag) v.getTag();

                item = DBTransaction.getById(tag.id);

                //handle edit of transfer: require both records to be present, and only edit
                //the original record (not the duplicate)
                boolean allowEdit = true;
                if (item.getType() == LTransaction.TRANSACTION_TYPE_TRANSFER) {
                    if (null == DBTransaction.getByRid(item.getRid() + "2")) allowEdit = false;
                } else if (item.getType() == LTransaction.TRANSACTION_TYPE_TRANSFER_COPY) {
                    LTransaction item2 = DBTransaction.getByRid(item.getRid().substring(0, item.getRid().length() - 1));
                    if (null == item2) {
                        allowEdit = false;
                    } else {
                        item = item2;
                    }
                }

                itemOrig = new LTransaction(item);

                edit = new TransactionEdit(getActivity(), rootView, item, false, false, allowEdit, MyCursorAdapter.this);

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
                        LJournal journal = new LJournal();
                        if ((item.getAccount() != itemOrig.getAccount()) ||
                                (item.getAccount2() != itemOrig.getAccount2())) {
                            LLog.d(TAG, "account changed, recreate record");
                            LTransaction oldItem = new LTransaction(itemOrig);
                            oldItem.setTimeStampLast(LPreferences.getServerUtc());
                            oldItem.setState(DBHelper.STATE_DELETED);
                            DBTransaction.update(oldItem, false);
                            journal.updateItem(oldItem, DBHelper.STATE_ACTIVE);

                            //delete the duplicate record for transfer
                            if (item.getType() == LTransaction.TRANSACTION_TYPE_TRANSFER) {
                                LTransaction copy = DBTransaction.getByRid(oldItem.getRid() + "2");
                                if (copy == null) {
                                    LLog.w(TAG, "warn, unable to locate duplicate transfer record?");
                                } else {
                                    journal.updateItem(copy, DBHelper.STATE_ACTIVE);
                                }
                            }

                            //reset UUID of existing record and treat it as new
                            item.setRid(UUID.randomUUID().toString());
                            item.setTimeStampLast(LPreferences.getServerUtc());
                            DBTransaction.add(item, true, true);
                        } else {
                            item.setTimeStampLast(LPreferences.getServerUtc());
                            DBTransaction.update(item, false);
                            journal.updateItem(item, itemOrig);

                            //update also transfer copy
                            if (item.getType() == LTransaction.TRANSACTION_TYPE_TRANSFER) {
                                LTransaction copy = DBTransaction.getByRid(item.getRid() + "2");
                                LTransaction copyOrig = new LTransaction(itemOrig);
                                copyOrig.setRid(copy.getRid() + "2");
                                copyOrig.setAccount(itemOrig.getAccount2());
                                copyOrig.setAccount2(itemOrig.getAccount());
                                if (copy == null) {
                                    LLog.w(TAG, "warn, unable to locate duplicate transfer record?");
                                } else {
                                    journal.updateItem(copy, copyOrig);
                                }
                            }
                        }
                        onSelected(true);
                    }
                    break;
                case TransactionEdit.TransitionEditItf.EXIT_CANCEL:
                    break;
                case TransactionEdit.TransitionEditItf.EXIT_DELETE:
                    AppPersistency.transactionChanged = true;

                    itemOrig.setTimeStampLast(LPreferences.getServerUtc());
                    itemOrig.setState(DBHelper.STATE_DELETED);
                    DBTransaction.update(itemOrig, false);

                    LJournal journal = new LJournal();
                    journal.updateItem(itemOrig, DBHelper.STATE_ACTIVE);

                    if (itemOrig.getType() == LTransaction.TRANSACTION_TYPE_TRANSFER) {
                        LTransaction copy = DBTransaction.getByRid(itemOrig.getRid() + "2");
                        if (copy == null) {
                            LLog.w(TAG, "warn, unable to locate duplicate transfer record?");
                        } else {
                            journal.updateItem(copy, DBHelper.STATE_ACTIVE);
                        }
                    }

                    onSelected(true);
                    break;
            }
            viewFlipper.setInAnimation(getActivity(), R.anim.slide_in_left);
            viewFlipper.setOutAnimation(getActivity(), R.anim.slide_out_right);
            viewFlipper.showPrevious();
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
        View dispv;

        if (altView) {
            mtv = altMonthTV;
            btv = altBalanceTV;
            itv = altIncomeTV;
            etv = altExpenseTV;
            dispv = altDispV;
        } else {
            mtv = monthTV;
            btv = balanceTV;
            itv = incomeTV;
            etv = expenseTV;
            dispv = dispV;
        }

        if (LPreferences.getSearchAll() || (LPreferences.getSearchCategories() == null &&
                LPreferences.getSearchTags() == null &&
                LPreferences.getSearchVendors() == null)) {
            dispv.setVisibility(View.VISIBLE);
        } else {
            dispv.setVisibility(View.INVISIBLE);
        }

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

        if (LPreferences.getSearchAllTime() && data != null && data.getCount() > 0) {
            switch (AppPersistency.viewTransactionTime) {
                case AppPersistency.TRANSACTION_TIME_ALL:
                    mtv.setText(getString(R.string.balance));
                    break;
                case AppPersistency.TRANSACTION_TIME_MONTHLY:
                    mtv.setText(new DateFormatSymbols().getMonths()[AppPersistency.viewTransactionMonth]);
                    break;
                case AppPersistency.TRANSACTION_TIME_QUARTERLY:
                    mtv.setText("Q" + (AppPersistency.viewTransactionQuarter + 1) + " " + AppPersistency.viewTransactionYear);
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

        DBAccess.getAccountSummaryForCurrentCursor(summary, data, LPreferences.getSearchAccounts());
        if (!LPreferences.getSearchAllTime()) {
            summary.setBalance(summary.getIncome() - summary.getExpense());
            return;
        }

        if (allBalances == null) return;

        //get balance for All accounts at current year/month
        switch (AppPersistency.viewTransactionTime) {
            case AppPersistency.TRANSACTION_TIME_ALL:
                if (LPreferences.getSearchAll() || LPreferences.getSearchAccounts() == null)
                    summary.setBalance(allBalances.getBalance());
                else
                    summary.setBalance(allBalances.getBalance(LPreferences.getSearchAccounts()));
                break;
            case AppPersistency.TRANSACTION_TIME_MONTHLY:
                if (LPreferences.getSearchAll() || LPreferences.getSearchAccounts() == null)
                    summary.setBalance(allBalances.getBalance(
                            AppPersistency.viewTransactionYear, AppPersistency.viewTransactionMonth));
                else
                    summary.setBalance(allBalances.getBalance(LPreferences.getSearchAccounts(),
                            AppPersistency.viewTransactionYear, AppPersistency.viewTransactionMonth));
                break;
            case AppPersistency.TRANSACTION_TIME_QUARTERLY:
                if (LPreferences.getSearchAll() || LPreferences.getSearchAccounts() == null)
                    summary.setBalance(allBalances.getBalance(
                            AppPersistency.viewTransactionYear, AppPersistency.viewTransactionQuarter * 3 + 2));
                else
                    summary.setBalance(allBalances.getBalance(LPreferences.getSearchAccounts(),
                            AppPersistency.viewTransactionYear, AppPersistency.viewTransactionQuarter * 3 + 2));

                break;
            case AppPersistency.TRANSACTION_TIME_ANNUALLY:
                if (LPreferences.getSearchAll() || LPreferences.getSearchAccounts() == null)
                    summary.setBalance(allBalances.getBalance(AppPersistency.viewTransactionYear, 11));
                else
                    summary.setBalance(allBalances.getBalance(LPreferences.getSearchAccounts(), AppPersistency.viewTransactionYear, 11));
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
                    return false;
                }
                return true;

            case AppPersistency.TRANSACTION_TIME_QUARTERLY:
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

            case AppPersistency.TRANSACTION_TIME_ANNUALLY:
                AppPersistency.viewTransactionYear += (prev) ? -1 : 1;
                ymS = getMs(AppPersistency.viewTransactionYear, 0);
                ymE = getMs(AppPersistency.viewTransactionYear + 1, 0);
                if (dbLoaderHelper.getAllStartMs() >= ymE || dbLoaderHelper.getAllEndMs() <= ymS) {
                    AppPersistency.viewTransactionYear = year;
                    return false;
                }
                return true;
        }
        return false;
    }

    private void showPrevNext(boolean prev) {
        if (!prevNext(prev)) return;

        AppPersistency.setViewHistory(AppPersistency.getViewLevel(), listView.onSaveInstanceState());

        isAltView = !isAltView;
        if (prev) {
            listViewFlipper.setInAnimation(getActivity(), R.anim.slide_in_left);
            listViewFlipper.setOutAnimation(getActivity(), R.anim.slide_out_right);
            listViewFlipper.showPrevious();
            AppPersistency.setViewLevel(AppPersistency.getViewLevel() - 1);
        } else {
            listViewFlipper.setInAnimation(getActivity(), R.anim.slide_in_right);
            listViewFlipper.setOutAnimation(getActivity(), R.anim.slide_out_left);
            listViewFlipper.showNext();
            AppPersistency.setViewLevel(AppPersistency.getViewLevel() + 1);
        }
        initDbLoader();
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

            if (LPreferences.getSearchAllTime() && LPreferences.getSearchAll()) {
                LViewUtils.setAlpha(searchView, 0.8f);
            } else {
                LViewUtils.setAlpha(searchView, 1.0f);
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

    private void showTime() {
        if (LPreferences.getSearchAllTime()) {
            customTimeView.setVisibility(View.GONE);
            monthlyView.setVisibility(View.VISIBLE);
            prevView.setVisibility(View.VISIBLE);
            nextView.setVisibility(View.VISIBLE);

        } else {
            customTimeView.setVisibility(View.VISIBLE);
            monthlyView.setVisibility(View.GONE);
            prevView.setVisibility(View.GONE);
            nextView.setVisibility(View.GONE);

            if (LPreferences.getSearchFilterByEditTIme()) {
                customTimeView.setText("M: " + (new SimpleDateFormat("MMM d, yyy").format(LPreferences.getSearchAllTimeFrom()) + " - " +
                        new SimpleDateFormat("MMM d, yyy").format(LPreferences.getSearchAllTimeTo())));
            } else {
                customTimeView.setText(new SimpleDateFormat("MMM d, yyy").format(LPreferences.getSearchAllTimeFrom()) + " - " +
                        new SimpleDateFormat("MMM d, yyy").format(LPreferences.getSearchAllTimeTo()));
            }
            prevView.setEnabled(false);
            nextView.setEnabled(false);
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

        if (AppPersistency.viewTransactionTime == AppPersistency.TRANSACTION_TIME_ALL) {
            prevView.setEnabled(false);
            nextView.setEnabled(false);
            LViewUtils.setAlpha(prevView, 0.8f);
            LViewUtils.setAlpha(nextView, 0.8f);
        } else {
            prevView.setEnabled(true);
            nextView.setEnabled(true);
            LViewUtils.setAlpha(prevView, 1.0f);
            LViewUtils.setAlpha(nextView, 1.0f);
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


