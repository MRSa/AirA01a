package jp.osdn.gokigen.aira01a.preference;

import android.content.DialogInterface;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import jp.osdn.gokigen.aira01a.MainActivity;
import jp.osdn.gokigen.aira01a.R;

/**
 *  Preferenceがクリックされた時に処理するクラス
 *
 */
public class CameraPowerOff implements Preference.OnPreferenceClickListener
{
    private final MainActivity parent;

    /**
     *   コンストラクタ
     *
     * @param activity　親分
     */
    CameraPowerOff(MainActivity activity)
    {
        parent = activity;
    }

    /**
     *   クラスの準備
     *
     */
    void prepare()
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
        if (!preference.hasKey())
        {
            return (false);
        }

        DialogInterface.OnClickListener listener;
        int positiveButtonResId; // = R.string.dialog_positive_power_off;
        int negativeButtonResId = R.string.dialog_negative_cancel;
        int titleResId =R.string.dialog_title_confirmation;
        int messageResId; // = R.string.dialog_message_disconnect;

        String key = preference.getKey();

        // 処理を設定する
        if (key.contains("power_off"))
        {
            messageResId = R.string.dialog_message_power_off;
            positiveButtonResId = R.string.dialog_positive_power_off;
            listener = new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    // カメラの電源をOFFにする
                    parent.disconnectWithPowerOff(true);
                }
            };
        }
        else if (key.contains("exit_application"))
        {
            messageResId = R.string.dialog_message_exit_application;
            positiveButtonResId = R.string.dialog_positive_exit_application;
            listener = new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    // カメラの電源をOFFにしたうえで、アプリケーションを終了する。
                    parent.disconnectWithPowerOff(true);
                    parent.finish();
                }
            };
        }
        else
        {
            // 何もしない
            return (false);
        }

        // 確認ダイアログの生成
        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(parent);
        alertDialog.setTitle(parent.getString(titleResId));
        alertDialog.setIcon(android.R.drawable.ic_dialog_alert);
        alertDialog.setMessage(parent.getString(messageResId));
        alertDialog.setCancelable(true);

        // ボタンを設定する
        alertDialog.setPositiveButton(parent.getString(positiveButtonResId), listener);
        alertDialog.setNegativeButton(parent.getString(negativeButtonResId),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

        // ダイアログを表示する
        alertDialog.show();

        return (true);
    }
}
