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
import com.swoag.logalong.entities.LTag;
import com.swoag.logalong.entities.LTransaction;
import com.swoag.logalong.entities.LVendor;
import com.swoag.logalong.utils.AppPersistency;
import com.swoag.logalong.utils.DBAccess;
import com.swoag.logalong.utils.DBHelper;
import com.swoag.logalong.utils.LLog;
import com.swoag.logalong.views.GenericListOptionDialog;
import com.swoag.logalong.views.LMultiSelectionDialog;
import com.swoag.logalong.views.LNewAccountDialog;
import com.swoag.logalong.views.LReminderDialog;
import com.swoag.logalong.views.LRenameDialog;
import com.swoag.logalong.views.LShareAccountDialog;

import java.util.HashSet;

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
                csr = DBAccess.getAllAccountsCursor();
                break;
            case R.id.categories:
                csr = DBAccess.getAllCategoriesCursor();
                break;
            case R.id.vendors:
                csr = DBAccess.getAllVendorsCursor();
                break;
            case R.id.tags:
                csr = DBAccess.getAllTagsCursor();
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
    public boolean onNewAccountDialogExit(Object id, boolean created, String name) {
        if (created && name != null && !name.isEmpty()) {
            switch (listId) {
                case R.id.accounts:
                    DBAccess.addAccount(new LAccount(name));
                    break;
                case R.id.categories:
                    DBAccess.addCategory(new LCategory(name));
                    break;
                case R.id.vendors:
                    DBAccess.addVendor(new LVendor(name));
                    break;
                case R.id.tags:
                    DBAccess.addTag(new LTag(name));
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
        private LTransaction item;
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
            v.setTag(new VTag(cursor.getLong(0), name));
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

            optionDialog = new GenericListOptionDialog(activity, tag, tag.name,
                    listId, this);
            optionDialog.show();
        }

        @Override
        public void onRenameDialogExit(Object id, boolean renamed, String newName) {
            if (renamed) {
                VTag tag = (VTag) id;
                switch (listId) {
                    case R.id.accounts:
                        DBAccess.updateAccountNameById(tag.id, newName);
                        break;
                    case R.id.categories:
                        DBAccess.updateCategoryNameById(tag.id, newName);
                        break;
                    case R.id.vendors:
                        DBAccess.updateVendorNameById(tag.id, newName);
                        break;
                    case R.id.tags:
                        DBAccess.updateTagNameById(tag.id, newName);
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

                    HashSet<Long> selectedIds = DBAccess.getVendorCategories(tag.id);
                    LMultiSelectionDialog dialog = new LMultiSelectionDialog
                            (activity, context, selectedIds, this, ids, columns);
                    dialog.show();
                    return true;

                case R.id.share:
                    if (AppPersistency.profileSet) {
                        ids = new int[]{
                                R.layout.account_share_dialog,
                                R.layout.account_share_item,
                                R.id.title,
                                R.id.save,
                                R.id.cancel,
                                R.id.selectall,
                                R.id.checkBox1,
                                R.id.name,
                                R.id.list,
                                R.string.select_categories};
                        columns = new String[]{
                                DBHelper.TABLE_COLUMN_NAME
                        };

                        selectedIds = DBAccess.getVendorCategories(tag.id);
                        LShareAccountDialog shareAccountDialog = new LShareAccountDialog
                                (activity, context, selectedIds, this, ids, columns);
                        shareAccountDialog.show();
                    } else {
                        LReminderDialog reminderDialog = new LReminderDialog(activity, activity.getString(R.string.reminder_empty_profile), this);
                        reminderDialog.show();
                    }
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
            for (Long ii : selections) {
                DBAccess.addVendorCategory(tag.id, ii);
            }
            optionDialog.dismiss();
        }

        @Override
        public Cursor onMultiSelectionGetCursor(String column) {
            return DBAccess.getAllCategoriesCursor();
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

