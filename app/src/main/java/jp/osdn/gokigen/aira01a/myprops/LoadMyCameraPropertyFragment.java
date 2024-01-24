package jp.osdn.gokigen.aira01a.myprops;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import androidx.fragment.app.ListFragment;
import jp.osdn.gokigen.aira01a.R;

public class LoadMyCameraPropertyFragment extends ListFragment implements ListView.OnItemClickListener
{
    private final String TAG = toString();
    private ILoadSaveMyCameraPropertyDialogDismiss dialogDismiss = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        return (inflater.inflate(R.layout.list_camera_properties, container, false));
    }

    void setDismissInterface(ILoadSaveMyCameraPropertyDialogDismiss dismiss)
    {
        this.dialogDismiss = dismiss;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);

        List<MyCameraPropertySetItems> listItems = new ArrayList<>();

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        for (int index = 1; index <= CameraPropertyBackupRestore.MAX_STORE_PROPERTIES; index++)
        {
            String idHeader = String.format(Locale.ENGLISH, "%03d", index);
            String prefDate = preferences.getString(idHeader + CameraPropertyBackupRestore.DATE_KEY, "");
            if (prefDate.length() <= 0)
            {
                //listItems.add(new MyCameraPropertySetItems(0, idHeader, "", ""));
                break;
            }
            String prefTitle = preferences.getString(idHeader + CameraPropertyBackupRestore.TITLE_KEY, "");
            listItems.add(new MyCameraPropertySetItems(0, idHeader, prefTitle, prefDate));
        }

        String prefDate = preferences.getString(CameraPropertyBackupRestore.DATE_KEY, "");
        listItems.add(new MyCameraPropertySetItems(0, "000", getString(R.string.auto_save_props), prefDate));

        MyCameraPropertyLoadArrayAdapter adapter = new MyCameraPropertyLoadArrayAdapter(getActivity(),  R.layout.column_load, listItems);
        setListAdapter(adapter);

        getListView().setOnItemClickListener(this);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long l)
    {
        final MyCameraPropertySetItems item = (MyCameraPropertySetItems) getListAdapter().getItem(position);
        Log.v(TAG, "onItemClick() : " + position + " [" + item.getItemId() + "] " + item.getItemName() + " (" + item.getItemInfo() + ")");

        // アイテムを選択して終わらせる
        if (dialogDismiss != null)
        {
            dialogDismiss.doDismissWithPropertyLoad(item.getItemId(), item.getItemName());
        }
    }
}
