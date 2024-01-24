package jp.osdn.gokigen.aira01a.connection.ble;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.fragment.app.ListFragment;
import jp.osdn.gokigen.aira01a.R;

public class OlyCameraEntryListFragment extends ListFragment
{
    private String TAG = toString();
    private IOlyCameraSetDialogDismiss dialogDismiss = null;

    static OlyCameraEntryListFragment newInstance(IOlyCameraSetDialogDismiss dismiss)
    {
        OlyCameraEntryListFragment instance = new OlyCameraEntryListFragment();
        instance.dialogDismiss = dismiss;

        return (instance);
    }

    /**/
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        Log.v(TAG, "onCreateView()");
        return (inflater.inflate(R.layout.list_camera_properties, container, false));
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        Log.v(TAG, "onActivityCreated()");
        super.onActivityCreated(savedInstanceState);

        List<OlyCameraSetArrayItem> listItems = new ArrayList<>();

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        for (int index = 1; index <= IOlyCameraBleProperty.MAX_STORE_PROPERTIES; index++)
        {
            String idHeader = String.format(Locale.ENGLISH, "%03d", index);
            String prefDate = preferences.getString(idHeader + IOlyCameraBleProperty.DATE_KEY, "");
            if (prefDate.length() <= 0)
            {
                listItems.add(new OlyCameraSetArrayItem(idHeader, "", "", ""));
                break;  // 最後の１個は空白で出す
                //continue;  // 全部出す
            }
            String btName = preferences.getString(idHeader + IOlyCameraBleProperty.NAME_KEY, "");
            String btCode = preferences.getString(idHeader + IOlyCameraBleProperty.CODE_KEY, "");
            listItems.add(new OlyCameraSetArrayItem(idHeader, btName, btCode, prefDate));
        }
        OlyCameraSetArrayAdapter adapter = new OlyCameraSetArrayAdapter(getActivity(),  R.layout.column_save_bt, listItems, dialogDismiss);
        setListAdapter(adapter);
    }

    @Override
    public void onDestroyView()
    {
        Log.v(TAG, "onDestroyView()");
        super.onDestroyView();
    }
}
