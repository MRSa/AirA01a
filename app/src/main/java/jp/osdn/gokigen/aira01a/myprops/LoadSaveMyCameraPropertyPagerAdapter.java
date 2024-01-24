package jp.osdn.gokigen.aira01a.myprops;


import android.util.Log;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

class LoadSaveMyCameraPropertyPagerAdapter extends FragmentPagerAdapter
{
    private final String TAG = toString();
    private final ILoadSaveMyCameraPropertyDialogDismiss dismissInterface;

    //private final CameraPropertyBackupRestore propertyInterface;
    //private Bundle bundle = null;
    private String[] titles = null;
    private LoadMyCameraPropertyFragment loadFragment = null;
    private SaveMyCameraPropertyFragment saveFragment = null;

    LoadSaveMyCameraPropertyPagerAdapter(FragmentManager fm, ILoadSaveMyCameraPropertyDialogDismiss dismissInterface)
    {
        super(fm);
        this.dismissInterface = dismissInterface;
    }

    private void initialize()
    {
        loadFragment = null;
        loadFragment = new LoadMyCameraPropertyFragment();
        loadFragment.setDismissInterface(dismissInterface);

        saveFragment = null;
        saveFragment = new SaveMyCameraPropertyFragment();
        saveFragment.setDismissInterface(dismissInterface);
    }

    @Override
    public Fragment getItem(int position)
    {
        Log.v(TAG, "getItem :" + position);
        if ((loadFragment == null)||(saveFragment == null))
        {
            initialize();
        }
        Fragment returnFragment;
        if (position == 0)
        {
            // loadFragment
            returnFragment = loadFragment;
        }
        else  //
        {
            // saveFragment
            returnFragment = saveFragment;
        }
        return (returnFragment);
    }

    @Override
    public int getCount()
    {
        return 2;
    }

    @Override
    public CharSequence getPageTitle(int position)
    {
        return titles[position];
    }

    void setTitles(String[] titles)
    {
        this.titles = titles;
    }
}
