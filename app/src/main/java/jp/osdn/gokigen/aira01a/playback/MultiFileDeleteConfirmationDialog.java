package jp.osdn.gokigen.aira01a.playback;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import jp.osdn.gokigen.aira01a.R;

/**
 *   複数ファイルの一括ダウンロードを実施することを確認するダイアログ
 *
 */
public class MultiFileDeleteConfirmationDialog extends DialogFragment
{
    private final String TAG = this.toString();

    private DeleteConfirmationCallback callback;
    private int nofPictures;

    public static MultiFileDeleteConfirmationDialog newInstance(@NonNull DeleteConfirmationCallback callback, int nofPictures)
    {
        MultiFileDeleteConfirmationDialog instance = new MultiFileDeleteConfirmationDialog();
        instance.prepare(callback, nofPictures);

        return (instance);
    }

    private void prepare(DeleteConfirmationCallback callback, int nofPictures)
    {
        this.callback = callback;
        this.nofPictures = nofPictures;
    }

    @Override
    public @NonNull Dialog onCreateDialog(Bundle savedInstanceState)
    {
        Activity activity = getActivity();
        LayoutInflater inflater = activity.getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_confirmation_batch_delete, null, false);
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(activity.getString(R.string.dialog_title_batch_delete));
        builder.setIcon(R.drawable.ic_baseline_warning_amber_24);
        TextView textView = view.findViewById(R.id.label_batch_delete_info);
        if (textView != null)
        {
            String label = getString(R.string.dialog_label_batch_download) + nofPictures;
            textView.setText(label);
        }
        final CheckBox checkWithRaw = view.findViewById(R.id.radio_delete__raw);
        final CheckBox checkOnlyRaw = view.findViewById(R.id.radio_delete_only_raw);

        // ボタンを設定する（実行ボタン）
        builder.setPositiveButton(activity.getString(R.string.dialog_positive_delete),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        int buttonId = 0;
                        boolean withRaw = false;
                        boolean onlyRaw = false;
                        try
                        {
                            if (checkWithRaw != null)
                            {
                                withRaw = checkWithRaw.isChecked();
                            }
                            if (checkOnlyRaw != null)
                            {
                                onlyRaw = checkOnlyRaw.isChecked();
                            }
                        }
                        catch (Exception e)
                        {
                            e.printStackTrace();
                        }
                        dialog.dismiss();
                        Log.v(TAG, "confirmSelection() " + buttonId + " [" + withRaw + "] [" + onlyRaw + "]");
                        callback.confirmDeleteSelection(buttonId, withRaw, onlyRaw);
                     }
                });

        // ボタンを設定する (キャンセルボタン）
        builder.setNegativeButton(activity.getString(R.string.dialog_negative_cancel),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

        builder.setView(view);
        return (builder.create());
    }

    // コールバックインタフェース
    public interface DeleteConfirmationCallback
    {
        void confirmDeleteSelection(int selectedButtonId, boolean withRaw, boolean onlyRaw);
    }

}
