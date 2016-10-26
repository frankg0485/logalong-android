package com.swoag.logalong.fragments;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.swoag.logalong.BuildConfig;
import com.swoag.logalong.LFragment;
import com.swoag.logalong.MainActivity;
import com.swoag.logalong.R;
import com.swoag.logalong.network.LProtocol;
import com.swoag.logalong.utils.LOnClickListener;

public class SettingsFragment extends LFragment implements
        GenericListEdit.GenericListEditItf, ProfileEdit.ProfileEditItf, DataBackupEdit.DataBackupEditItf {
    private static final String TAG = SettingsFragment.class.getSimpleName();

    ViewFlipper viewFlipper;
    View profileV, accountsV, categoriesV, vendorsV, tagsV;
    ImageView addV;
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
        setViewListener(view, R.id.exit);
        addV = (ImageView) setViewListener(view, R.id.addImg);

        TextView versionV = (TextView)rootView.findViewById(R.id.version);
        versionV.setText(BuildConfig.VERSION_NAME);
        return rootView;
    }

    @Override
    public void onDestroyView() {
        accountsV = null;
        categoriesV = null;
        vendorsV = null;
        tagsV = null;

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

                    addV.setImageResource(R.drawable.ic_action_accept);
                    addV.setClickable(false);

                    dataBackupEdit = new DataBackupEdit(getActivity(), viewSettings, SettingsFragment.this);

                    viewFlipper.setInAnimation(getActivity(), R.anim.slide_in_right);
                    viewFlipper.setOutAnimation(getActivity(), R.anim.slide_out_left);
                    viewFlipper.showNext();
                    break;

                case R.id.profile:
                    viewSettings.findViewById(R.id.listView).setVisibility(View.GONE);
                    viewSettings.findViewById(R.id.profileSettings).setVisibility(View.VISIBLE);
                    viewSettings.findViewById(R.id.dataBackupSettings).setVisibility(View.GONE);

                    addV.setImageResource(R.drawable.ic_action_accept);
                    addV.setClickable(false);

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

                    addV.setImageResource(R.drawable.ic_action_new);
                    addV.setClickable(false);

                    listEdit = new GenericListEdit(getActivity(),
                            viewSettings, v.getId(), SettingsFragment.this);

                    viewFlipper.setInAnimation(getActivity(), R.anim.slide_in_right);
                    viewFlipper.setOutAnimation(getActivity(), R.anim.slide_out_left);
                    viewFlipper.showNext();
                    break;

                case R.id.exit:
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
