package jp.osdn.gokigen.aira01a.connection.ble;

import android.app.Dialog;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.text.DateFormat;
import java.util.Date;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import jp.osdn.gokigen.aira01a.R;

public class OlyCameraEntryListDialog extends DialogFragment implements IOlyCameraSetDialogDismiss
{
    private final String TAG = this.toString();
    private boolean viewCreated = false;
    private View myView = null;
    private String message = "";
    private String title = "";
    private OlyCameraEntryListFragment listFragment = OlyCameraEntryListFragment.newInstance(this);

    static OlyCameraEntryListDialog newInstance(String title, String message)
    {
        OlyCameraEntryListDialog instance = new OlyCameraEntryListDialog();

        // ダイアログに渡すパラメータはBundleにまとめておく
        Bundle arguments = new Bundle();
        arguments.putString("title", title);
        arguments.putString("message", message);
        instance.setArguments(arguments);

        Log.v("dialog", "title: " + title + " message: " + message);
        return (instance);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        Log.v(TAG, "onCreateView()");

        Bundle arguments = getArguments();
        if (arguments != null)
        {
            title = arguments.getString("title");
            message = arguments.getString("message");
            Log.v(TAG, "title: " + title + " message: " + message);
        }

        if ((viewCreated)&&(myView != null))
        {
            // Viewを再利用。。。
            Log.v(TAG, "onCreateView() : called again, so do nothing... : " + myView);
            return (myView);
        }
        View view = inflater.inflate(R.layout.dialog_my_camera_entries, container, false);

        myView = view;
        viewCreated = true;

        // listFragmentを子フラグメントとする（Nested Fragment を使う）
        FragmentManager fragmentManager = getChildFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.add(R.id.layout_content, listFragment, "list_fragment");
        transaction.commit();

        return (view);
    }
    @Override
    public @NonNull Dialog onCreateDialog(Bundle savedInstanceState)
    {
        Log.v(TAG, "onCreateDialog() : " + title + " (" + message + ")");
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setTitle(title);
        return (dialog);
    }

    /**
     *
     *
     */
    @Override
    public void setOlyCameraSet(String id, String name, String code, String info)
    {
        String namePrefKey = id + IOlyCameraBleProperty.NAME_KEY;
        String codePrefKey = id + IOlyCameraBleProperty.CODE_KEY;
        String infoPrefKey = id + IOlyCameraBleProperty.DATE_KEY;

        DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
        String dateInfo = dateFormat.format(new Date());

        try
        {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
            SharedPreferences.Editor editor = preferences.edit();

            editor.putString(namePrefKey, name);
            editor.putString(codePrefKey, code);
            editor.putString(infoPrefKey, dateInfo);

            editor.apply();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        Log.v(TAG, "setOlyCameraSet() REGISTERED : [" + id + "] " + name + " " + code + " " + dateInfo);

        dismiss();
    }
}
