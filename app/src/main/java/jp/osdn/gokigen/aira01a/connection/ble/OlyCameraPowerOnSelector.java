package jp.osdn.gokigen.aira01a.connection.ble;

import android.util.Log;

import androidx.fragment.app.FragmentActivity;
import androidx.preference.Preference;
import jp.osdn.gokigen.aira01a.ConfirmationDialog;
import jp.osdn.gokigen.aira01a.R;

/**
 *   Olympus AIR の Bluetooth設定を記録する
 *
 *
 */
public class OlyCameraPowerOnSelector implements Preference.OnPreferenceClickListener, ConfirmationDialog.Callback
{
    private final String TAG = toString();
    private final FragmentActivity context;
    //private String preferenceKey = null;

    /**
     *   コンストラクタ
     *
     */
    public OlyCameraPowerOnSelector(FragmentActivity context)
    {
        this.context = context;
    }

    /**
     *   クラスの準備
     *
     */
    public void prepare()
    {
        // 何もしない
    }

    /**
     *
     *
     * @param preference クリックしたpreference
     * @return false : ハンドルしない / true : ハンドルした
     */
    @Override
    public boolean onPreferenceClick(Preference preference)
    {
        Log.v(TAG, "onPreferenceClick() : ");
        if (!preference.hasKey())
        {
            return (false);
        }

        String preferenceKey = preference.getKey();
        if (preferenceKey.contains(IOlyCameraBleProperty.OLYCAMERA_BLUETOOTH_SETTINGS))
        {
            try
            {
                // My Olympus Air登録用ダイアログを表示する
                OlyCameraEntryListDialog dialogFragment = OlyCameraEntryListDialog.newInstance(context.getString(R.string.pref_air_bt), context.getString(R.string.pref_summary_air_bt));
                dialogFragment.setRetainInstance(false);
                dialogFragment.setShowsDialog(true);
                dialogFragment.show(context.getSupportFragmentManager(), "dialog");
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            return (true);
        }
        return (false);
    }

    /**
     *
     *
     */
    @Override
    public void confirm()
    {
        Log.v(TAG, "confirm() ");
    }
}
