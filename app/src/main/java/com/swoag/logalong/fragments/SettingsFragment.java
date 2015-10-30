package com.swoag.logalong.fragments;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.swoag.logalong.LFragment;
import com.swoag.logalong.MainActivity;
import com.swoag.logalong.R;
import com.swoag.logalong.utils.LOnClickListener;

public class SettingsFragment extends LFragment implements
        GenericListEdit.GenericListEditItf, ProfileEdit.ProfileEditItf, DataBackupEdit.DataBackupEditItf {
    private static final String TAG = SettingsFragment.class.getSimpleName();

    ViewFlipper viewFlipper;
    View profileV, accountsV, categoriesV, vendorsV, tagsV;
    View backV, editV, addV;
    private ProfileEdit profileEdit;
    private GenericListEdit listEdit;
    private DataBackupEdit dataBackupEdit;
    private MyClickListener myClickListener;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_settings, container, false);

        myClickListener = new MyClickListener();

        viewFlipper = (ViewFlipper) rootView.findViewById(R.id.viewFlipper);
        viewFlipper.setAnimateFirstView(false);
        viewFlipper.setDisplayedChild(0);

        View view = rootView.findViewById(R.id.settings);
        setViewListener(view, R.id.backup);
        profileV = setViewListener(view, R.id.profile);
        accountsV = setViewListener(view, R.id.accounts);
        categoriesV = setViewListener(view, R.id.categories);
        vendorsV = setViewListener(view, R.id.vendors);
        tagsV = setViewListener(view, R.id.tags);

        view = rootView.findViewById(R.id.viewSettings);
        backV = setViewListener(view, R.id.goback);
        editV = setViewListener(view, R.id.edit);
        addV = setViewListener(view, R.id.add);

        return rootView;
    }

    @Override
    public void onDestroyView() {
        accountsV = null;
        categoriesV = null;
        vendorsV = null;
        tagsV = null;

        backV = null;
        editV = null;
        addV = null;
        super.onDestroyView();
    }

    @Override
    public void onSelected(boolean selected) {
        if (listEdit != null) {
            listEdit.dismiss();
        }
        if (profileEdit != null) {
            profileEdit.dismiss();
        }

        if (dataBackupEdit != null) {
            dataBackupEdit.dismiss();
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

    private class MyClickListener extends LOnClickListener {
        @Override
        public void onClicked(View v) {
            View viewSettings = viewFlipper.findViewById(R.id.viewSettings);
            switch (v.getId()) {
                case R.id.backup:
                    viewSettings.findViewById(R.id.listView).setVisibility(View.GONE);
                    viewSettings.findViewById(R.id.profileSettings).setVisibility(View.GONE);
                    viewSettings.findViewById(R.id.dataBackupSettings).setVisibility(View.VISIBLE);

                    viewSettings.findViewById(R.id.add).setVisibility(View.GONE);
                    TextView tv = (TextView) viewSettings.findViewById(R.id.save);
                    tv.setVisibility(View.VISIBLE);
                    tv.setText(getActivity().getString(R.string.done));

                    dataBackupEdit = new DataBackupEdit(getActivity(), viewSettings, SettingsFragment.this);

                    viewFlipper.setInAnimation(getActivity(), R.anim.slide_in_right);
                    viewFlipper.setOutAnimation(getActivity(), R.anim.slide_out_left);
                    viewFlipper.showNext();
                    break;

                case R.id.profile:
                    viewSettings.findViewById(R.id.listView).setVisibility(View.GONE);
                    viewSettings.findViewById(R.id.profileSettings).setVisibility(View.VISIBLE);
                    viewSettings.findViewById(R.id.dataBackupSettings).setVisibility(View.GONE);

                    viewSettings.findViewById(R.id.add).setVisibility(View.GONE);
                    viewSettings.findViewById(R.id.save).setVisibility(View.VISIBLE);

                    profileEdit = new ProfileEdit(getActivity(), viewSettings, SettingsFragment.this);

                    viewFlipper.setInAnimation(getActivity(), R.anim.slide_in_right);
                    viewFlipper.setOutAnimation(getActivity(), R.anim.slide_out_left);
                    viewFlipper.showNext();
                    break;

                case R.id.accounts:
                case R.id.categories:
                case R.id.vendors:
                case R.id.tags:
                    viewSettings.findViewById(R.id.listView).setVisibility(View.VISIBLE);
                    viewSettings.findViewById(R.id.profileSettings).setVisibility(View.GONE);
                    viewSettings.findViewById(R.id.dataBackupSettings).setVisibility(View.GONE);

                    viewSettings.findViewById(R.id.add).setVisibility(View.VISIBLE);
                    viewSettings.findViewById(R.id.save).setVisibility(View.GONE);

                    listEdit = new GenericListEdit(getActivity(),
                            viewSettings, v.getId(), SettingsFragment.this);

                    viewFlipper.setInAnimation(getActivity(), R.anim.slide_in_right);
                    viewFlipper.setOutAnimation(getActivity(), R.anim.slide_out_left);
                    viewFlipper.showNext();
                    break;

                case R.id.goback:
                    onBackPressed();
                    break;
                default:
                    break;
            }
        }
    }

    private void restoreView() {
        viewFlipper.setInAnimation(getActivity(), R.anim.slide_in_left);
        viewFlipper.setOutAnimation(getActivity(), R.anim.slide_out_right);
        viewFlipper.showPrevious();
    }

    @Override
    public void onDataBackupEditExit() {
        restoreView();
        dataBackupEdit = null;
    }

    @Override
    public void onProfileEditExit() {
        restoreView();
        profileEdit = null;
    }

    @Override
    public void onGenericListEditExit() {
        restoreView();
        listEdit = null;
    }

    @Override
    public boolean onBackPressed() {
        if (profileEdit != null) {
            profileEdit.dismiss();
            return true;
        } else if (listEdit != null) {
            listEdit.dismiss();
            return true;
        } else if (dataBackupEdit != null) {
            dataBackupEdit.dismiss();
            return true;
        }
        return false;
    }

    private View setViewListener(View v, int id) {
        View view = v.findViewById(id);
        view.setOnClickListener(myClickListener);
        return view;
    }

}
