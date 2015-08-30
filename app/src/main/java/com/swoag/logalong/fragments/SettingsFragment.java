package com.swoag.logalong.fragments;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ViewFlipper;

import com.swoag.logalong.LFragment;
import com.swoag.logalong.MainActivity;
import com.swoag.logalong.R;

public class SettingsFragment extends LFragment implements
        View.OnClickListener, GenericListEdit.GenericListEditItf {
    private static final String TAG = SettingsFragment.class.getSimpleName();

    ViewFlipper viewFlipper;
    View accountsV, categoriesV, vendorsV, tagsV;
    View backV, editV, addV;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_settings, container, false);

        viewFlipper = (ViewFlipper) rootView.findViewById(R.id.viewFlipper);
        viewFlipper.setAnimateFirstView(false);
        viewFlipper.setDisplayedChild(0);

        View view = rootView.findViewById(R.id.settings);
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
        MainActivity actv;
        switch (v.getId()) {
            case R.id.accounts:
            case R.id.categories:
            case R.id.vendors:
            case R.id.tags:
                GenericListEdit listEdit = new GenericListEdit(getActivity(),
                        viewFlipper.findViewById(R.id.viewSettings), v.getId(), this);
                viewFlipper.setInAnimation(getActivity(), R.anim.slide_in_right);
                viewFlipper.setOutAnimation(getActivity(), R.anim.slide_out_left);
                viewFlipper.showNext();

                actv = (MainActivity) getActivity();
                actv.disablePager();
                break;
            case R.id.goback:
            case R.id.edit:
            case R.id.add:
                viewFlipper.setInAnimation(getActivity(), R.anim.slide_in_left);
                viewFlipper.setOutAnimation(getActivity(), R.anim.slide_out_right);
                viewFlipper.showPrevious();

                actv = (MainActivity) getActivity();
                actv.enablePager();
                break;
            default:
                break;
        }
    }

    @Override
    public void onGenericListEditExit() {

    }

    @Override
    public boolean onBackPressed() {
        if (viewFlipper.getDisplayedChild() == 1) {
            viewFlipper.setInAnimation(getActivity(), R.anim.slide_in_left);
            viewFlipper.setOutAnimation(getActivity(), R.anim.slide_out_right);
            viewFlipper.showPrevious();

            MainActivity actv = (MainActivity) getActivity();
            actv.enablePager();
            return true;
        }
        return false;
    }

    private View setViewListener(View v, int id) {
        View view = v.findViewById(id);
        view.setOnClickListener(this);
        return view;
    }

}
