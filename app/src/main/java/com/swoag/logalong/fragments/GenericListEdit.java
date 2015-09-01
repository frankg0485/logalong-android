package com.swoag.logalong.fragments;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.swoag.logalong.LApp;
import com.swoag.logalong.MainActivity;
import com.swoag.logalong.R;
import com.swoag.logalong.entities.LItem;
import com.swoag.logalong.utils.AppPersistency;
import com.swoag.logalong.utils.DBAccess;
import com.swoag.logalong.utils.DBHelper;
import com.swoag.logalong.utils.LLog;
import com.swoag.logalong.utils.LViewUtils;
import com.swoag.logalong.views.GenericListOptionDialog;
import com.swoag.logalong.views.LMultiSelectionDialog;
import com.swoag.logalong.views.LRenameDialog;
import com.swoag.logalong.views.LSelectionDialog;

import java.text.SimpleDateFormat;

public class GenericListEdit implements View.OnClickListener {
    private static final String TAG = GenericListEdit.class.getSimpleName();

    private Activity activity;
    private View rootView;
    private GenericListEditItf callback;
    private int listId;
    private ListView listView;
    private Cursor cursor;
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

    private void create() {
        this.listView = (ListView) rootView.findViewById(R.id.listView);
        addV = rootView.findViewById(R.id.add);
        addV.setOnClickListener(this);

        switch (listId) {
            case R.id.accounts:
                cursor = DBAccess.getAllAccountsCursor();
                break;
            case R.id.categories:
                cursor = DBAccess.getAllCategoriesCursor();
                break;
            case R.id.vendors:
                cursor = DBAccess.getAllVendorsCursor();
                break;
            case R.id.tags:
                cursor = DBAccess.getAllTagsCursor();
                break;

        }

        adapter = new MyCursorAdapter(activity, cursor);
        listView.setAdapter(adapter);
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
                break;
            default:
                break;
        }
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
            LRenameDialog.LRenameDialogItf,
            LMultiSelectionDialog.OnMultiSelectionDialogItf {
        private LItem item;
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
            String name = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.TABLE_ACCOUNT_COLUMN_NAME));
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
                    listId == R.id.vendors, this);
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
                adapter.notifyDataSetChanged();
            }
            optionDialog.dismiss();
        }

        @Override
        public boolean onGenericListOptionDialogExit(Object context, int viewId) {
            VTag tag = (VTag) context;
            switch (viewId) {
                case R.id.remove:
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
                            R.string.select_account};
                    String[] columns = new String[]{
                            DBHelper.TABLE_CATEGORY_COLUMN_NAME
                    };

                    LMultiSelectionDialog dialog = new LMultiSelectionDialog(activity, this, ids, columns);
                    dialog.show();
                    return true;
            }
            return false;
        }

        @Override
        public void onMultiSelectionDialogExit() {
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

