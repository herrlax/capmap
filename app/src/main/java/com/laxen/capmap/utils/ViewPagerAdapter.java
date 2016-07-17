package com.laxen.capmap.utils;


import com.laxen.capmap.MainActivity;
import com.laxen.capmap.R;
import com.laxen.capmap.tabs.MapFragmentTab;
import com.laxen.capmap.tabs.ListFragmentTab;

import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ImageSpan;
import android.graphics.drawable.Drawable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

public class ViewPagerAdapter extends FragmentStatePagerAdapter{

    private MainActivity context;

    // Tabs in the slider
    private ListFragmentTab listFragmentTab;
    private MapFragmentTab mapFragmentTab;

    // icons for the different tabs
    private int[] icons = {R.drawable.ic_map_white_24dp, R.drawable.ic_camera_roll_white_24dp};
    private int numberOfTabs;

    public ViewPagerAdapter(FragmentManager fragmentManager, int numberOfTabs) {
        super(fragmentManager);

        this.numberOfTabs = numberOfTabs;
    }

    public void setContext (MainActivity context) {
        this.context = context;
    }

    @Override
    public Fragment getItem(int pos) {

        switch (pos){
            case 0:
                return mapFragmentTab;
            case 1:
                return listFragmentTab;
        }

        return null;
    }

    @Override
    public int getCount() {
        return numberOfTabs;
    }

    @Override
    public CharSequence getPageTitle(int position) {

        SpannableString spannableString = new SpannableString(" ");

        Drawable image = context.getDrawable(icons[position]); //context.getResources().getDrawable(icons[position]);
        image.setBounds(0, 0, image.getIntrinsicWidth(), image.getIntrinsicHeight());

        ImageSpan imageSpan = new ImageSpan(image, ImageSpan.ALIGN_BASELINE);

        spannableString.setSpan(imageSpan, 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        return spannableString;
    }

    public void setListFragmentTab(ListFragmentTab listFragmentTab) {
        this.listFragmentTab = listFragmentTab;
    }

    public void setMapFragmentTab(MapFragmentTab mapFragmentTab) {
        this.mapFragmentTab = mapFragmentTab;
    }
}