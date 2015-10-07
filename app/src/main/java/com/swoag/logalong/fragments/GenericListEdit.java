package com.swoag.logalong.fragments;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import com.swoag.logalong.R;
import com.swoag.logalong.entities.LAccount;
import com.swoag.logalong.entities.LCategory;
import com.swoag.logalong.entities.LJournal;
import com.swoag.logalong.entities.LTag;
import com.swoag.logalong.entities.LTransaction;
import com.swoag.logalong.entities.LUser;
import com.swoag.logalong.entities.LVendor;
import com.swoag.logalong.network.LProtocol;
import com.swoag.logalong.utils.AppPersistency;
import com.swoag.logalong.utils.DBAccess;
import com.swoag.logalong.utils.DBAccount;
import com.swoag.logalong.utils.DBCategory;
import com.swoag.logalong.utils.DBHelper;
import com.swoag.logalong.utils.DBTag;
import com.swoag.logalong.utils.DBVendor;
import com.swoag.logalong.utils.LLog;
import com.swoag.logalong.utils.LPreferences;
import com.swoag.logalong.views.GenericListOptionDialog;
import com.swoag.logalong.views.LMultiSelectionDialog;
import com.swoag.logalong.views.LNewAccountDialog;
import com.swoag.logalong.views.LReminderDialog;
import com.swoag.logalong.views.LRenameDialog;
import com.swoag.logalong.views.LShareAccountDialog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;

public class GenericListEdit implements View.OnClickListener,
        LNewAccountDialog.LNewAccountDialogItf {
    private static final String TAG = GenericListEdit.class.getSimpleName();

    private Activity activity;
    private View rootView;
    private GenericListEditItf callback;
    private int listId;
    private ListView listView;
    private MyCursorAdapter adapter;
    private View addV;

    public interface GenericListEditItf {
        public void onGenericListEditExit();
    }

    public GenericListEdit(Activity activity, View rootView, int listId,
                           GenericListEditItf callback) {
        this.activity = activity;
        this.rootView = rootView;
        this.callback = callback;
        this.listId = listId;
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
        addV.setOnClickListener(this);

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

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.add:
                String title = "";
                switch (listId) {
                    case R.id.accounts:
                        title = activity.getString(R.string.new_account);
                        break;
                    case R.id.categories:
                        title = activity.getString(R.string.new_category);
                        break;
                    case R.id.vendors:
                        title = activity.getString(R.string.new_vendor);
                        break;
                    case R.id.tags:
                        title = activity.getString(R.string.new_tag);
                        break;
                    default:
                        break;
                }
                LNewAccountDialog newAccountDialog = new LNewAccountDialog(activity, this, this, title, null);
                newAccountDialog.show();
                break;
        }
    }

    @Override
    public boolean isNewAccountNameAvailable(String name) {
        if (name != null && !name.isEmpty()) {
            String table;
            switch (listId) {
                case R.id.accounts:
                    table = DBHelper.TABLE_ACCOUNT_NAME;
                    break;

                case R.id.categories:
                    table = DBHelper.TABLE_CATEGORY_NAME;
                    break;

                case R.id.vendors:
                    table = DBHelper.TABLE_VENDOR_NAME;
                    break;

                case R.id.tags:
                    table = DBHelper.TABLE_TAG_NAME;
                    break;

                default:
                    return false;
            }
            return DBAccess.isNameAvailable(table, name);
        }
        return false;
    }

    @Override
    public boolean onNewAccountDialogExit(Object id, boolean created, String name) {
        if (created && name != null && !name.isEmpty()) {
            switch (listId) {
                case R.id.accounts:
                    DBAccess.addAccount(new LAccount(name));
                    break;

                case R.id.categories:
                    LCategory category = new LCategory(name);
                    DBAccess.addCategory(category);

                    LJournal journal = new LJournal();
                    journal.updateCategory(category);
                    break;

                case R.id.vendors:
                    LVendor vendor = new LVendor(name);
                    DBAccess.addVendor(vendor);

                    journal = new LJournal();
                    journal.updateVendor(vendor);
                    break;

                case R.id.tags:
                    LTag tag = new LTag(name);
                    DBAccess.addTag(tag);

                    journal = new LJournal();
                    journal.updateTag(tag);
                    break;

                default:
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
        return true;
    }

    public void dismiss() {
        destroy();
        callback.onGenericListEditExit();
    }


    private View setViewListener(View v, int id) {
        View view = v.findViewById(id);
        view.setOnClickListener(this);
        return view;
    }

    private class MyCursorAdapter extends CursorAdapter implements View.OnClickListener,
            GenericListOptionDialog.GenericListOptionDialogItf,
            LRenameDialog.LRenameDialogItf, LReminderDialog.LReminderDialogItf,
            LShareAccountDialog.LShareAccountDialogItf,
            LMultiSelectionDialog.OnMultiSelectionDialogItf {
        GenericListOptionDialog optionDialog;

        public MyCursorAdapter(Context context, Cursor cursor) {
            //TODO: deprecated API is used here for max OS compatibility, provide alternative
            //      using LoaderManager with a CursorLoader.
            //super(context, cursor, 0);
            super(context, cursor, false);
        }

        /**
         * Bind an existing view to the data pointed to by cursor
         */
        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            TextView tv = (TextView) view.findViewById(R.id.name);
            String name = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.TABLE_COLUMN_NAME));
            tv.setText(name);

            View v = view.findViewById(R.id.option);
            v.setOnClickListener(this);
            VTag tag = new VTag(cursor.getLong(0), name);
            v.setTag(tag);

            if (listId == R.id.accounts) {
                v = view.findViewById(R.id.share);
                v.setVisibility(View.VISIBLE);
                v.setOnClickListener(this);
                v.setTag(tag);
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

        @Override
        public void onClick(View v) {
            VTag tag = (VTag) v.getTag();

            switch (v.getId()) {
                case R.id.option:

                    optionDialog = new GenericListOptionDialog(activity, tag, tag.name,
                            listId, this);
                    optionDialog.show();
                    break;

                case R.id.share:
                    ArrayList<LUser> users = new ArrayList<LUser>();
                    HashSet<Integer> userSet = DBAccess.getAllAccountsShareUser();
                    for (int ii : userSet) {
                        if (!LPreferences.getShareUserName(ii).isEmpty()) {
                            users.add(new LUser(LPreferences.getShareUserName(ii), ii));
                        }
                    }

                    boolean updateAccount = false;
                    LAccount account = DBAccess.getAccountById(tag.id);
                    HashSet<Integer> selectedUsers = new HashSet<Integer>();
                    if (account.getShareIds() != null) {
                        for (int ii : account.getShareIds()) {
                            if (!LPreferences.getShareUserName(ii).isEmpty()) {
                                selectedUsers.add(ii);
                            } else updateAccount = true;
                        }
                    }

                    if (updateAccount) {
                        account.setShareIds(new ArrayList<Integer>(selectedUsers));
                        DBAccess.updateAccount(account);
                    }

                    LShareAccountDialog shareAccountDialog = new LShareAccountDialog
                            (activity, account, selectedUsers, this, users);
                    shareAccountDialog.show();
            }
        }


        @Override
        public void onShareAccountDialogExit(boolean ok, LAccount account, HashSet<Integer> selections, HashSet<Integer> origSelections) {
            if (!ok) return;

            for (Integer ii : selections) {
                if (!origSelections.contains(ii)) {
                    // new share
                    LProtocol.ui.shareAccountWithUser(ii, account.getName(), account.getRid().toString(), true);
                }
            }

            for (Integer ii : origSelections) {
                if (!selections.contains(ii)) {
                    account.removeShareUser(ii);
                    DBAccess.updateAccount(account);

                    // notify the other party
                    LProtocol.ui.shareAccountUserChange(ii, ii, false, account.getName(), account.getRid().toString());

                    ArrayList<Integer> ids = account.getShareIds();
                    ArrayList<Integer> states = account.getShareStates();
                    for (int jj = 0; jj < ids.size(); jj++) {
                        if (states.get(jj) == LAccount.ACCOUNT_SHARE_CONFIRMED) {
                            LProtocol.ui.shareAccountUserChange(ids.get(jj), ii, false, account.getName(), account.getRid().toString());
                        }
                    }

                    LLog.d(TAG, "TODO: unshare with user: " + ii + " " + LPreferences.getShareUserName(ii));
                }
            }
        }

        @Override
        public void onRenameDialogExit(Object id, boolean renamed, String newName) {
            if (renamed) {
                VTag tag = (VTag) id;
                switch (listId) {
                    case R.id.accounts:
                        LAccount account = DBAccess.getAccountById(tag.id);
                        account.setName(newName);
                        account.setTimeStampLast(System.currentTimeMillis());
                        DBAccess.updateAccount(account);

                        LJournal journal = new LJournal();
                        journal.updateAccount(account);
                        break;

                    case R.id.categories:
                        LCategory category = DBCategory.getById(tag.id);
                        category.setName(newName);
                        category.setTimeStampLast(System.currentTimeMillis());
                        DBAccess.updateCategory(category);

                        journal = new LJournal();
                        journal.updateCategory(category);
                        break;

                    case R.id.vendors:
                        LVendor vendor = DBVendor.getById(tag.id);
                        vendor.setName(newName);
                        vendor.setTimeStampLast(System.currentTimeMillis());
                        DBAccess.updateVendor(vendor);

                        journal = new LJournal();
                        journal.updateVendor(vendor);
                        break;
                    case R.id.tags:
                        LTag tag1 = DBAccess.getTagById(tag.id);
                        tag1.setName(newName);
                        tag1.setTimeStampLast(System.currentTimeMillis());
                        DBAccess.updateTag(tag1);

                        journal = new LJournal();
                        journal.updateTag(tag1);
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
                    switch (listId) {
                        case R.id.accounts:
                            DBAccess.deleteAccountById(tag.id);
                            break;
                        case R.id.categories:
                            DBAccess.deleteCategoryById(tag.id);
                            break;
                        case R.id.vendors:
                            DBAccess.deleteVendorById(tag.id);
                            break;
                        case R.id.tags:
                            DBAccess.deleteTagById(tag.id);
                            break;
                    }

                    Cursor cursor = getMyCursor();
                    if (null == cursor) {
                        LLog.e(TAG, "fatal: unable to open database");
                    } else {
                        adapter.swapCursor(cursor);
                        adapter.notifyDataSetChanged();
                    }
                    break;
                case R.id.rename:
                    String title = "";
                    switch (listId) {
                        case R.id.accounts:
                            title = activity.getString(R.string.rename_account);
                            break;
                        case R.id.categories:
                            title = activity.getString(R.string.rename_category);
                            break;
                        case R.id.vendors:
                            title = activity.getString(R.string.rename_vendor);
                            break;
                        case R.id.tags:
                            title = activity.getString(R.string.rename_tag);
                            break;
                    }
                    LRenameDialog renameDialog = new LRenameDialog(activity, context, this,
                            title, tag.name, null);
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
        public void onReminderDialogExit() {
            optionDialog.dismiss();
        }

        @Override
        public void onMultiSelectionDialogExit(Object obj, HashSet<Long> selections) {
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
}

