package com.swoag.logalong.fragments;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
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
import com.swoag.logalong.utils.DBCategory;
import com.swoag.logalong.utils.DBHelper;
import com.swoag.logalong.utils.DBTag;
import com.swoag.logalong.utils.DBVendor;
import com.swoag.logalong.utils.LBroadcastReceiver;
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

public class GenericListEdit implements LNewEntryDialog.LNewEntryDialogItf, LBroadcastReceiver
        .BroadcastReceiverListener {
    private static final String TAG = GenericListEdit.class.getSimpleName();

    private Activity activity;
    private View rootView;
    private GenericListEditItf callback;
    private int listId;
    private ListView listView;
    private MyCursorAdapter adapter;
    private View addV;
    private MyClickListener myClickListener;
    private BroadcastReceiver broadcastReceiver;

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
                csr = DBAccount.getInstance().getCursorSortedBy(DBHelper.TABLE_COLUMN_NAME);
                break;
            case R.id.categories:
                csr = DBCategory.getInstance().getCursorSortedBy(DBHelper.TABLE_COLUMN_NAME);
                break;
            case R.id.vendors:
                csr = DBVendor.getInstance().getCursorSortedBy(DBHelper.TABLE_COLUMN_NAME);
                break;
            case R.id.tags:
                csr = DBTag.getInstance().getCursorSortedBy(DBHelper.TABLE_COLUMN_NAME);
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

        broadcastReceiver = LBroadcastReceiver.getInstance().register(new int[]{
                LBroadcastReceiver.ACTION_UI_UPDATE_ACCOUNT,
                LBroadcastReceiver.ACTION_UI_UPDATE_CATEGORY,
                LBroadcastReceiver.ACTION_UI_UPDATE_TAG,
                LBroadcastReceiver.ACTION_UI_UPDATE_VENDOR}, this);

    }

    private void destroy() {
        if (broadcastReceiver != null) {
            LBroadcastReceiver.getInstance().unregister(broadcastReceiver);
            broadcastReceiver = null;
        }

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
                    LNewEntryDialog newEntryDialog = new LNewEntryDialog(activity, 0, type, GenericListEdit.this,
                            title, null, attr1, attr2);
                    newEntryDialog.show();
                    break;
            }
        }
    }

    @Override
    public void onBroadcastReceiverReceive(int action, int ret, Intent intent) {
        switch (action) {
            case LBroadcastReceiver.ACTION_UI_UPDATE_ACCOUNT:
            case LBroadcastReceiver.ACTION_UI_UPDATE_CATEGORY:
            case LBroadcastReceiver.ACTION_UI_UPDATE_TAG:
            case LBroadcastReceiver.ACTION_UI_UPDATE_VENDOR:
                updateCursor();
                break;
        }
    }

    @Override
    public boolean onNewEntryDialogExit(int id, int type, boolean created, String name, boolean attr1, boolean attr2) {
        if (created && name != null && !TextUtils.isEmpty(name)) {
            updateCursor();
        }
        return true;
    }

    public void dismiss() {
        myClickListener.disableEnable(false);
        adapter.clickListener.disableEnable(false);
        destroy();
        callback.onGenericListEditExit();
    }

    private void updateCursor() {
        Cursor cursor = getMyCursor();
        if (null == cursor) {
            LLog.e(TAG, "fatal: unable to open database");
        } else {
            adapter.swapCursor(cursor);
            adapter.notifyDataSetChanged();
        }
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
                account = DBAccount.getInstance().getByCursor(cursor);
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
                ImageView iv = (ImageView) view.findViewById(R.id.share);
                if (account.isAnySharePending()) {
                    iv.setImageResource(R.drawable.ic_action_share_yellow);
                } else if (account.isShareConfirmed()) {
                    iv.setImageResource(R.drawable.ic_action_share_green);
                } else {
                    iv.setImageResource(R.drawable.ic_action_share_dark);
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

        private HashSet<Long> getAccountCurrentShares(LAccount account) {
            HashSet<Long> selectedUsers = new HashSet<>();
            if (account.getShareIds() != null) {
                for (long ii : account.getShareIds()) {
                    if (ii == LPreferences.getUserIdNum()) continue;
                    if (!TextUtils.isEmpty(LPreferences.getShareUserId(ii))) {
                        int shareState = account.getShareUserState(ii);
                        if (LAccount.ACCOUNT_SHARE_PERMISSION_OWNER >= shareState
                                || LAccount.ACCOUNT_SHARE_INVITED == shareState) {
                            selectedUsers.add(ii);
                        }
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
                        boolean allowDelete = true;
                        if (listId == R.id.vendors) {
                            LVendor vendor = DBVendor.getInstance().getById(tag.id);
                            if (vendor.getType() == LVendor.TYPE_PAYEE) attr1 = true;
                            else if (vendor.getType() == LVendor.TYPE_PAYER) attr2 = true;
                            else {
                                attr1 = attr2 = true;
                            }
                        } else if (listId == R.id.accounts) {
                            LAccount account = DBAccount.getInstance().getById(tag.id);
                            attr1 = account.isShowBalance();
                            if (account.getOwner() != LPreferences.getUserIdNum()) allowDelete = false;
                        }
                        optionDialog = new GenericListOptionDialog(activity, tag, tag.name,
                                listId, MyCursorAdapter.this, attr1, attr2, allowDelete);
                        optionDialog.show();
                        break;

                    case R.id.share:
                        if (TextUtils.isEmpty(LPreferences.getUserId())) {
                            new LReminderDialog(activity, activity.getResources().getString(R.string
                                    .please_complete_your_profile)).show();
                            break;
                        }

                        DBAccount dbAccount = DBAccount.getInstance();
                        ArrayList<LUser> users = new ArrayList<LUser>();
                        HashSet<Long> userSet = dbAccount.getAllShareUser();
                        for (long ii : userSet) {
                            if (ii == LPreferences.getUserIdNum()) continue;
                            if (!TextUtils.isEmpty(LPreferences.getShareUserId(ii))) {
                                users.add(new LUser(LPreferences.getShareUserId(ii),
                                        LPreferences.getShareUserName(ii), ii));
                            }
                        }

                        LAccount account = DBAccount.getInstance().getById(tag.id);
                        HashSet<Long> selectedUsers = getAccountCurrentShares(account);

                        LShareAccountDialog shareAccountDialog = new LShareAccountDialog
                                (activity, account.getId(), selectedUsers, MyCursorAdapter.this, users);
                        shareAccountDialog.show();
                        break;
                }
            }
        }

        private void unshareAllFromAccount(long accountId) {
            DBAccount dbAccount = DBAccount.getInstance();
            LAccount account = dbAccount.getById(accountId);
            account.removeAllShareUsers();
            account.setOwner(LPreferences.getUserIdNum());
            dbAccount.update(account);

            LJournal journal = new LJournal();
            journal.removeUserFromAccount(0, account.getGid());
        }

        private void unshareMyselfFromAccount(long accountId) {
            DBAccount dbAccount = DBAccount.getInstance();
            LAccount account = dbAccount.getById(accountId);

            LTask.start(new DBAccount.MyAccountDeleteTask(), account.getId());
            dbAccount.deleteById(accountId);

            LJournal journal = new LJournal();
            journal.removeUserFromAccount(LPreferences.getUserIdNum(), account.getGid());
            updateCursor();
        }

        private void do_account_share_update(long accountId, HashSet<Long> selections, HashSet<Long> origSelections) {
            DBAccount dbAccount = DBAccount.getInstance();

            LAccount account = dbAccount.getById(accountId);
            if (selections.isEmpty()) {
                if (account.getOwner() == LPreferences.getUserIdNum()) {
                    //removing everyone from shared group
                    unshareAllFromAccount(accountId);
                } else {
                    //unshare myself
                    unshareMyselfFromAccount(accountId);
                }
                return;
            }

            LJournal journal = new LJournal();
            //first update all existing users if there's any removal
            for (Long ii : origSelections) {
                if (!selections.contains(ii)) {
                    account.removeShareUser(ii);
                    journal.removeUserFromAccount(ii, account.getGid());
                }
            }

            //now request for new share
            for (Long ii : selections) {
                boolean newShare = false;
                if (!origSelections.contains(ii)) newShare = true;
                else if (account.getShareUserState(ii) > LAccount.ACCOUNT_SHARE_PERMISSION_OWNER)
                    newShare = true;
                if (newShare) {
                    // new share request: new memeber is added to group
                    account.addShareUser(ii, LAccount.ACCOUNT_SHARE_INVITED);
                    journal.addUserToAccount(ii, account.getGid());
                }
            }
            dbAccount.update(account);
        }

        @Override
        public void onShareAccountDialogExit(boolean ok, boolean applyToAllAccounts, long accountId,
                                             HashSet<Long> selections, HashSet<Long> origSelections) {
            if (!ok) return;

            HashSet<Long> set;
            DBAccount dbAccount = DBAccount.getInstance();
            if (applyToAllAccounts) {
                set = dbAccount.getAllActiveIds();
                for (long id : set) {
                    if (dbAccount.getById(id).getOwner() == LPreferences.getUserIdNum()) {
                        LAccount account = dbAccount.getById(id);
                        HashSet<Long> selectedUsers = getAccountCurrentShares(account);
                        do_account_share_update(id, selections, selectedUsers);
                    }
                }
            } else {
                do_account_share_update(accountId, selections, origSelections);
            }
            adapter.swapCursor(getMyCursor());
            adapter.notifyDataSetChanged();
        }

        @Override
        public void onRenameDialogExit(Object id, boolean renamed, String newName) {
            if (renamed) {
                LJournal journal = new LJournal();
                VTag tag = (VTag) id;
                switch (listId) {
                    case R.id.accounts:
                        DBAccount dbAccount = DBAccount.getInstance();
                        LAccount naccount = dbAccount.getByName(newName);
                        LAccount account = dbAccount.getById(tag.id);
                        if ((naccount != null) && (!account.getName().equalsIgnoreCase(newName))) {
                            //TODO: prompt user for name duplicate
                        } else {
                            account.setName(newName);
                            account.setTimeStampLast(LPreferences.getServerUtc());
                            dbAccount.update(account);
                            journal.updateAccount(account.getId());
                        }
                        break;

                    case R.id.categories:
                        DBCategory dbCategory = DBCategory.getInstance();
                        LCategory ncategory = dbCategory.getByName(newName);
                        LCategory category = dbCategory.getById(tag.id);
                        if ((ncategory != null) && (!category.getName().equalsIgnoreCase(newName))) {
                            //TODO: prompt user for name duplicate
                        } else {
                            category.setName(newName);
                            category.setTimeStampLast(LPreferences.getServerUtc());
                            dbCategory.update(category);
                            journal.updateCategory(category.getId());
                        }
                        break;

                    case R.id.vendors:
                        DBVendor dbVendor = DBVendor.getInstance();
                        LVendor nvendor = dbVendor.getByName(newName);
                        LVendor vendor = dbVendor.getById(tag.id);
                        if ((nvendor != null) && (!vendor.getName().equalsIgnoreCase(newName))) {
                            //TODO: prompt user for name duplicate
                        } else {
                            String oldName = vendor.getName();
                            vendor.setName(newName);
                            vendor.setTimeStampLast(LPreferences.getServerUtc());
                            dbVendor.update(vendor);
                            journal.updateVendor(vendor.getId());
                        }
                        break;

                    case R.id.tags:
                        DBTag dbTag = DBTag.getInstance();
                        LTag ntag = dbTag.getByName(newName);
                        LTag tag1 = dbTag.getById(tag.id);
                        if ((ntag != null) && (!tag1.getName().equalsIgnoreCase(newName))) {
                            //TODO: prompt user for name duplicate
                        } else {
                            String oldName = tag1.getName();
                            tag1.setName(newName);
                            tag1.setTimeStampLast(LPreferences.getServerUtc());
                            dbTag.update(tag1);
                            journal.updateTag(tag1.getId());
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
            LJournal journal = new LJournal();
            if (listId == R.id.vendors) {
                DBVendor dbVendor = DBVendor.getInstance();
                LVendor vendor = dbVendor.getById(tag.id);
                if (null != vendor) {
                    int type = LVendor.TYPE_PAYEE;
                    if (attr1 && attr2) type = LVendor.TYPE_PAYEE_PAYER;
                    else if (attr1) type = LVendor.TYPE_PAYEE;
                    else type = LVendor.TYPE_PAYER;
                    vendor.setType(type);
                    dbVendor.update(vendor);
                    journal.updateVendor(vendor.getId());
                }
            } else if (listId == R.id.accounts) {
                DBAccount dbAccount = DBAccount.getInstance();
                LAccount account = dbAccount.getById(tag.id);
                if (null != account) {
                    account.setShowBalance(attr1);
                    dbAccount.update(account);
                    journal.updateAccount(account.getId());
                }
            }
        }

        @Override
        public void onWarnDialogExit(Object obj, boolean confirm, boolean ok) {
            VTag tag = (VTag) obj;

            if (confirm && ok) {
                LJournal journal = new LJournal();
                switch (listId) {
                    case R.id.accounts:
                        //check to make sure we still own account
                        LAccount account = DBAccount.getInstance().getById(tag.id);
                        if (null != account) {
                            if (account.getOwner() == LPreferences.getUserIdNum()) {
                                LTask.start(new DBAccount.MyAccountDeleteTask(), tag.id);
                                DBAccount.getInstance().deleteById(tag.id);
                                journal.deleteAccount(tag.id);
                            }
                        }
                        break;

                    case R.id.categories:
                        DBCategory.getInstance().deleteById(tag.id);
                        journal.deleteCategory(tag.id);
                        break;

                    case R.id.vendors:
                        DBVendor.getInstance().deleteById(tag.id);
                        journal.deleteVendor(tag.id);
                        break;

                    case R.id.tags:
                        DBTag.getInstance().deleteById(tag.id);
                        journal.deleteTag(tag.id);
                        break;
                }
                updateCursor();
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

                    HashSet<Long> selectedIds = DBVendor.getInstance().getCategories(tag.id);
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

            DBVendor.getInstance().setCategories(tag.id, selections);
            optionDialog.dismiss();
        }

        @Override
        public Cursor onMultiSelectionGetCursor(String column) {
            return DBCategory.getInstance().getCursorSortedBy(DBHelper.TABLE_COLUMN_NAME);
        }

        @Override
        public ArrayList<String> onMultiSelectionGetStrings() {
            return null;
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
}

