package com.swoag.logalong.fragments;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.support.v4.widget.CursorAdapter;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.swoag.logalong.LApp;
import com.swoag.logalong.R;
import com.swoag.logalong.entities.LAccount;
import com.swoag.logalong.entities.LCategory;
import com.swoag.logalong.entities.LJournal;
import com.swoag.logalong.entities.LTag;
import com.swoag.logalong.entities.LUser;
import com.swoag.logalong.entities.LVendor;
import com.swoag.logalong.utils.DBAccount;
import com.swoag.logalong.utils.DBAccountBalance;
import com.swoag.logalong.utils.DBCategory;
import com.swoag.logalong.utils.DBHelper;
import com.swoag.logalong.utils.DBScheduledTransaction;
import com.swoag.logalong.utils.DBTag;
import com.swoag.logalong.utils.DBTransaction;
import com.swoag.logalong.utils.DBVendor;
import com.swoag.logalong.utils.LLog;
import com.swoag.logalong.utils.LOnClickListener;
import com.swoag.logalong.utils.LPreferences;
import com.swoag.logalong.utils.LTask;
import com.swoag.logalong.utils.LViewUtils;
import com.swoag.logalong.views.GenericListOptionDialog;
import com.swoag.logalong.views.LMultiSelectionDialog;
import com.swoag.logalong.views.LNewEntryDialog;
import com.swoag.logalong.views.LReminderDialog;
import com.swoag.logalong.views.LRenameDialog;
import com.swoag.logalong.views.LShareAccountDialog;
import com.swoag.logalong.views.LWarnDialog;

import java.util.ArrayList;
import java.util.HashSet;

public class GenericListEdit implements LNewEntryDialog.LNewEntryDialogItf {
    private static final String TAG = GenericListEdit.class.getSimpleName();

    private Activity activity;
    private View rootView;
    private GenericListEditItf callback;
    private int listId;
    private ListView listView;
    private MyCursorAdapter adapter;
    private View addV;
    private MyClickListener myClickListener;


    public interface GenericListEditItf {
        public void onGenericListEditExit();
    }

    public GenericListEdit(Activity activity, View rootView, int listId,
                           GenericListEditItf callback) {
        this.activity = activity;
        this.rootView = rootView;
        this.callback = callback;
        this.listId = listId;
        myClickListener = new MyClickListener();
        create();
    }

    private Cursor getMyCursor() {
        Cursor csr = null;
        switch (listId) {
            case R.id.accounts:
                csr = DBAccount.getCursorSortedBy(DBHelper.TABLE_COLUMN_NAME);
                break;
            case R.id.categories:
                csr = DBCategory.getCursorSortedBy(DBHelper.TABLE_COLUMN_NAME);
                break;
            case R.id.vendors:
                csr = DBVendor.getCursorSortedBy(DBHelper.TABLE_COLUMN_NAME);
                break;
            case R.id.tags:
                csr = DBTag.getCursorSortedBy(DBHelper.TABLE_COLUMN_NAME);
                break;
        }
        return csr;
    }

    private void create() {
        this.listView = (ListView) rootView.findViewById(R.id.listView);
        addV = rootView.findViewById(R.id.add);
        LViewUtils.setAlpha(addV, 1.0f);
        addV.setEnabled(true);
        addV.setOnClickListener(myClickListener);

        Cursor cursor = getMyCursor();
        if (null == cursor) {
            LLog.e(TAG, "fatal: unable to open database");
        } else {
            adapter = new MyCursorAdapter(activity, cursor);
            listView.setAdapter(adapter);
        }
    }

    private void destroy() {
        listView = null;

        addV.setOnClickListener(null);
        addV = null;
    }

    private class MyClickListener extends LOnClickListener {
        @Override
        public void onClicked(View v) {
            boolean attr1 = true, attr2 = false;
            switch (v.getId()) {
                case R.id.add:
                    String title = "";
                    int type = 0;
                    switch (listId) {
                        case R.id.accounts:
                            title = activity.getString(R.string.new_account);
                            type = LNewEntryDialog.TYPE_ACCOUNT;
                            break;
                        case R.id.categories:
                            title = activity.getString(R.string.new_category);
                            type = LNewEntryDialog.TYPE_CATEGORY;
                            break;
                        case R.id.vendors:
                            title = activity.getString(R.string.new_vendor);
                            type = LNewEntryDialog.TYPE_VENDOR;
                            break;
                        case R.id.tags:
                            title = activity.getString(R.string.new_tag);
                            type = LNewEntryDialog.TYPE_TAG;
                            break;
                        default:
                            break;
                    }
                    LNewEntryDialog newEntryDialog = new LNewEntryDialog(activity, 0, type, GenericListEdit.this, title, null, attr1, attr2);
                    newEntryDialog.show();
                    break;
            }
        }
    }

    @Override
    public boolean onNewEntryDialogExit(int id, int type, boolean created, String name, boolean attr1, boolean attr2) {
        if (created && name != null && !TextUtils.isEmpty(name)) {
            Cursor cursor = getMyCursor();
            if (null == cursor) {
                LLog.e(TAG, "fatal: unable to open database");
            } else {
                adapter.swapCursor(cursor);
                adapter.notifyDataSetChanged();
            }
        }
        return true;
    }

    public void dismiss() {
        myClickListener.disableEnable(false);
        adapter.clickListener.disableEnable(false);
        callback.onGenericListEditExit();
        destroy();
    }

    private View setViewListener(View v, int id) {
        View view = v.findViewById(id);
        view.setOnClickListener(myClickListener);
        return view;
    }

    private class MyCursorAdapter extends CursorAdapter implements
            GenericListOptionDialog.GenericListOptionDialogItf,
            LRenameDialog.LRenameDialogItf,
            LShareAccountDialog.LShareAccountDialogItf,
            LMultiSelectionDialog.MultiSelectionDialogItf,
            LWarnDialog.LWarnDialogItf {
        private GenericListOptionDialog optionDialog;
        private ClickListener clickListener;

        public MyCursorAdapter(Context context, Cursor cursor) {
            //TODO: deprecated API is used here for max OS compatibility, provide alternative
            //      using LoaderManager with a CursorLoader.
            //super(context, cursor, 0);
            super(context, cursor, false);
            clickListener = new ClickListener();
        }

        /**
         * Bind an existing view to the data pointed to by cursor
         */
        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            TextView tv = (TextView) view.findViewById(R.id.name);
            String name;
            LAccount account = null;
            if (listId == R.id.accounts) {
                account = DBAccount.getByCursor(cursor);
                name = account.getName();
            } else {
                name = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_NAME));
            }
            tv.setText(name);

            View v = view.findViewById(R.id.option);
            v.setOnClickListener(clickListener);
            VTag tag = new VTag(cursor.getLong(0), name);
            v.setTag(tag);

            if (listId == R.id.accounts) {
                ImageView iv = (ImageView)view.findViewById(R.id.share);
                if (account.isAnySharePending()) {
                    iv.setImageResource(R.drawable.ic_action_share_yellow);
                } else if (account.isShareConfirmed()) {
                    iv.setImageResource(R.drawable.ic_action_share_green);
                } else {
                    iv.setImageResource(R.drawable.ic_action_share);
                }
                iv.setVisibility(View.VISIBLE);
                iv.setOnClickListener(clickListener);
                iv.setTag(tag);
            }
        }

        /**
         * Makes a new view to hold the data pointed to by cursor.
         */
        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View newView = LayoutInflater.from(context).inflate(R.layout.generic_list_item, parent, false);
            return newView;
        }

        private HashSet<Integer> getAccountCurrentShares(LAccount account) {
            HashSet<Integer> selectedUsers = new HashSet<Integer>();
            if (account.getShareIds() != null) {
                for (int ii : account.getShareIds()) {
                    if (!TextUtils.isEmpty(LPreferences.getShareUserName(ii))) {
                        selectedUsers.add(ii);
                    } else {
                        LLog.w(TAG, "unexpected: unknown shared user");
                    }
                }
            }
            return selectedUsers;
        }

        private class ClickListener extends LOnClickListener {
            @Override
            public void onClicked(View v) {
                VTag tag = (VTag) v.getTag();

                switch (v.getId()) {
                    case R.id.option:
                        boolean attr1 = false, attr2 = false;
                        if (listId == R.id.vendors) {
                            LVendor vendor = DBVendor.getById(tag.id);
                            if (vendor.getType() == LVendor.TYPE_PAYEE) attr1 = true;
                            else if (vendor.getType() == LVendor.TYPE_PAYER) attr2 = true;
                            else {
                                attr1 = attr2 = true;
                            }
                        } else if (listId == R.id.accounts) {
                            attr1 = LPreferences.getShowAccountBalance(tag.id);
                        }
                        optionDialog = new GenericListOptionDialog(activity, tag, tag.name,
                                listId, MyCursorAdapter.this, attr1, attr2);
                        optionDialog.show();
                        break;

                    case R.id.share:
                        if ((TextUtils.isEmpty(LPreferences.getUserFullName())) || (TextUtils.isEmpty(LPreferences.getUserName()))) {
                            new LReminderDialog(activity, activity.getResources().getString(R.string.please_complete_your_profile)).show();
                            break;
                        }

                        ArrayList<LUser> users = new ArrayList<LUser>();
                        HashSet<Integer> userSet = DBAccount.getAllShareUser();
                        for (int ii : userSet) {
                            if (!TextUtils.isEmpty(LPreferences.getShareUserName(ii))) {
                                users.add(new LUser(LPreferences.getShareUserName(ii), LPreferences.getShareUserFullName(ii), ii));
                            }
                        }

                        LAccount account = DBAccount.getById(tag.id);
                        HashSet<Integer> selectedUsers = getAccountCurrentShares(account);

                        LShareAccountDialog shareAccountDialog = new LShareAccountDialog
                                (activity, account.getId(), selectedUsers, MyCursorAdapter.this, users);
                        shareAccountDialog.show();
                        break;
                }
            }
        }

        private void unshareMyselfFromAccount(long accountId) {
            LAccount account = DBAccount.getById(accountId);
            account.removeAllShareUsers();
            DBAccount.update(account);

            //racing: before the following actually get posted and acked by server,
            //        if this account is 'shared' by peer, the following would happen,
            // - account is first backed to shared to state, then go to unshared state
            //   when ack comes back.
            LJournal journal = new LJournal();
            journal.unshareAccount(LPreferences.getUserId(), (int) account.getId(), account.getGid(), account.getName());
        }

        @Override
        public void onShareAccountDialogExit(boolean ok, boolean applyToAllAccounts, long accountId,
                                             HashSet<Integer> selections, HashSet<Integer> origSelections) {
            if (!ok) return;

            HashSet<Long> set;
            if (applyToAllAccounts) set = DBAccount.getAllActiveAccountIds();
            else {
                set = new HashSet<Long>();
                set.add(accountId);
            }

            if (selections.isEmpty()) {
                //unshare myself, instead of removing everyone from shared group
                for (long id : set) unshareMyselfFromAccount(id);
                return;
            }

            for (long aid : set) {
                LAccount account = DBAccount.getById(aid);
                origSelections = getAccountCurrentShares(account);

                //first update all existing users if there's any removal
                for (Integer ii : origSelections) {
                    if (!selections.contains(ii)) {
                        account.removeShareUser(ii);
                        LJournal journal = new LJournal();
                        journal.unshareAccount(ii, (int) account.getId(), account.getGid(), account.getName());
                    }
                }

                //now request for new share
                for (Integer ii : selections) {
                    boolean newShare = false;
                    if (!origSelections.contains(ii)) newShare = true;
                    else if (account.getShareUserState(ii) != LAccount.ACCOUNT_SHARE_CONFIRMED_SYNCED)
                        newShare = true;
                    if (newShare) {
                        // new share request: new memeber is added to group
                        account.addShareUser(ii, LAccount.ACCOUNT_SHARE_INVITED);
                        LJournal journal = new LJournal();
                        journal.shareAccount(ii, (int) account.getId(), account.getGid(), account.getName());
                    }
                }
                DBAccount.update(account);
            }
            adapter.swapCursor(getMyCursor());
            adapter.notifyDataSetChanged();
        }

        @Override
        public void onRenameDialogExit(Object id, boolean renamed, String newName) {
            if (renamed) {
                VTag tag = (VTag) id;
                switch (listId) {
                    case R.id.accounts:
                        LAccount naccount = DBAccount.getByName(newName);
                        LAccount account = DBAccount.getById(tag.id);
                        if ((naccount != null) && (!account.getName().equalsIgnoreCase(newName))) {
                            //TODO: prompt user for name duplicate
                        } else {
                            String oldName = account.getName();
                            account.setName(newName);
                            account.setTimeStampLast(LPreferences.getServerUtc());
                            DBAccount.update(account);

                            LJournal journal = new LJournal();
                            journal.updateAccount(account, oldName);
                        }
                        break;

                    case R.id.categories:
                        LCategory ncategory = DBCategory.getByName(newName);
                        LCategory category = DBCategory.getById(tag.id);
                        if ((ncategory != null) && (!category.getName().equalsIgnoreCase(newName))) {
                            //TODO: prompt user for name duplicate
                        } else {
                            String oldName = category.getName();
                            category.setName(newName);
                            category.setTimeStampLast(LPreferences.getServerUtc());
                            DBCategory.update(category);

                            LJournal journal = new LJournal();
                            journal.updateCategory(category, oldName);
                        }
                        break;

                    case R.id.vendors:
                        LVendor nvendor = DBVendor.getByName(newName);
                        LVendor vendor = DBVendor.getById(tag.id);
                        if ((nvendor != null) && (!vendor.getName().equalsIgnoreCase(newName))) {
                            //TODO: prompt user for name duplicate
                        } else {
                            String oldName = vendor.getName();
                            vendor.setName(newName);
                            vendor.setTimeStampLast(LPreferences.getServerUtc());
                            DBVendor.update(vendor);

                            LJournal journal = new LJournal();
                            journal.updateVendor(vendor, oldName);
                        }
                        break;
                    case R.id.tags:
                        LTag ntag = DBTag.getByName(newName);
                        LTag tag1 = DBTag.getById(tag.id);
                        if ((ntag != null) && (!tag1.getName().equalsIgnoreCase(newName))) {
                            //TODO: prompt user for name duplicate
                        } else {
                            String oldName = tag1.getName();
                            tag1.setName(newName);
                            tag1.setTimeStampLast(LPreferences.getServerUtc());
                            DBTag.update(tag1);

                            LJournal journal = new LJournal();
                            journal.updateTag(tag1, oldName);
                        }
                        break;
                }

                Cursor cursor = getMyCursor();
                if (null == cursor) {
                    LLog.e(TAG, "fatal: unable to open database");
                } else {
                    adapter.swapCursor(cursor);
                    adapter.notifyDataSetChanged();
                }
            }
            optionDialog.dismiss();
        }

        @Override
        public void onGenericListOptionDialogDismiss(Object context, boolean attr1, boolean attr2) {
            VTag tag = (VTag) context;
            if (listId == R.id.vendors) {
                LVendor vendor = DBVendor.getById(tag.id);
                int type = LVendor.TYPE_PAYEE;
                if (attr1 && attr2) type = LVendor.TYPE_PAYEE_PAYER;
                else if (attr1) type = LVendor.TYPE_PAYEE;
                else type = LVendor.TYPE_PAYER;
                vendor.setType(type);
                DBVendor.update(vendor);
            } else if (listId == R.id.accounts) {
                LPreferences.setShowAccountBalance(tag.id, attr1);
            }
        }

        @Override
        public void onWarnDialogExit(Object obj, boolean confirm, boolean ok) {
            VTag tag = (VTag) obj;

            if (confirm && ok) {
                switch (listId) {
                    case R.id.accounts:
                        LTask.start(new MyAccountDeleteTask(), tag.id);

                        LAccount account = DBAccount.getById(tag.id);
                        account.setState(DBHelper.STATE_DELETED);
                        account.setTimeStampLast(LPreferences.getServerUtc());
                        DBAccount.update(account);

                        LJournal journal = new LJournal();
                        journal.updateAccount(account, DBHelper.STATE_ACTIVE);
                        break;

                    case R.id.categories:
                        LCategory category = DBCategory.getById(tag.id);
                        category.setState(DBHelper.STATE_DELETED);
                        category.setTimeStampLast(LPreferences.getServerUtc());
                        DBCategory.update(category);

                        journal = new LJournal();
                        journal.updateCategory(category, DBHelper.STATE_ACTIVE);
                        break;

                    case R.id.vendors:
                        LVendor vendor = DBVendor.getById(tag.id);
                        vendor.setState(DBHelper.STATE_DELETED);
                        vendor.setTimeStampLast(LPreferences.getServerUtc());
                        DBVendor.update(vendor);

                        journal = new LJournal();
                        journal.updateVendor(vendor, DBHelper.STATE_ACTIVE);
                        break;

                    case R.id.tags:
                        LTag tag1 = DBTag.getById(tag.id);
                        tag1.setState(DBHelper.STATE_DELETED);
                        tag1.setTimeStampLast(LPreferences.getServerUtc());
                        DBTag.update(tag1);

                        journal = new LJournal();
                        journal.updateTag(tag1, DBHelper.STATE_ACTIVE);
                        break;
                }

                Cursor cursor = getMyCursor();
                if (null == cursor) {
                    LLog.e(TAG, "fatal: unable to open database");
                } else {
                    adapter.swapCursor(cursor);
                    adapter.notifyDataSetChanged();
                }
            }
            optionDialog.dismiss();
        }

        @Override
        public boolean onGenericListOptionDialogExit(Object context, int viewId) {
            VTag tag = (VTag) context;
            switch (viewId) {
                case R.id.remove:
                    String msg = "";
                    switch (listId) {
                        case R.id.accounts:
                            msg = activity.getString(R.string.warning_delete_account);
                            break;
                        case R.id.categories:
                            msg = activity.getString(R.string.warning_delete_category);
                            break;
                        case R.id.vendors:
                            msg = activity.getString(R.string.warning_delete_vendor);
                            break;
                        case R.id.tags:
                            msg = activity.getString(R.string.warning_delete_tag);
                            break;
                    }
                    LWarnDialog warnDialog = new LWarnDialog(activity, tag, this,
                            activity.getString(R.string.delete), msg,
                            activity.getString(R.string.delete_now),
                            true);
                    warnDialog.show();
                    return true;
                case R.id.rename:
                    String title = "";
                    String hint = "";
                    switch (listId) {
                        case R.id.accounts:
                            title = activity.getString(R.string.rename_account);
                            break;
                        case R.id.categories:
                            title = activity.getString(R.string.rename_category);
                            hint = LApp.ctx.getResources().getString(R.string.hint_primary_category_sub_category);
                            break;
                        case R.id.vendors:
                            title = activity.getString(R.string.rename_vendor);
                            break;
                        case R.id.tags:
                            title = activity.getString(R.string.rename_tag);
                            break;
                    }
                    LRenameDialog renameDialog = new LRenameDialog(activity, context, this,
                            title, tag.name, hint);
                    renameDialog.show();
                    return true;
                case R.id.associated_categories:
                    int[] ids = new int[]{
                            R.layout.multi_selection_dialog,
                            R.layout.multi_selection_item,
                            R.id.title,
                            R.id.save,
                            R.id.cancel,
                            R.id.selectall,
                            R.id.checkBox1,
                            R.id.name,
                            R.id.list,
                            R.string.select_categories};
                    String[] columns = new String[]{
                            DBHelper.TABLE_COLUMN_NAME
                    };

                    HashSet<Long> selectedIds = DBVendor.getCategories(tag.id);
                    LMultiSelectionDialog dialog = new LMultiSelectionDialog
                            (activity, context, selectedIds, this, ids, columns);
                    dialog.show();
                    return true;
            }
            return false;
        }


        @Override
        public void onMultiSelectionDialogExit(Object obj, HashSet<Long> selections, boolean allSelected) {
            VTag tag = (VTag) obj;

            DBVendor.setCategories(tag.id, selections);
            optionDialog.dismiss();
        }

        @Override
        public Cursor onMultiSelectionGetCursor(String column) {
            return DBCategory.getCursorSortedBy(DBHelper.TABLE_COLUMN_NAME);
        }

        private class VTag {
            long id;
            String name;

            public VTag(long id, String name) {
                this.id = id;
                this.name = name;
            }
        }
    }

    private class MyAccountDeleteTask extends AsyncTask<Long, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Long... params) {
            Long accountId = params[0];

            DBTransaction.deleteByAccount(accountId);
            DBScheduledTransaction.deleteByAccount(accountId);

            DBAccountBalance.deleteByAccountId(accountId);
            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
        }

        @Override
        protected void onPreExecute() {
        }
    }
}

