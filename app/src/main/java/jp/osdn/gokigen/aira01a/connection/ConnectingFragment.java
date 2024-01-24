package jp.osdn.gokigen.aira01a.connection;

import android.content.Intent;
import android.provider.Settings;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import jp.co.olympus.camerakit.OLYCamera;
import jp.osdn.gokigen.aira01a.R;

/**
 *  ConnectingFragment
 *  (ほぼOLYMPUS imagecapturesampleのサンプルコードそのまま)
 *
 */
public class ConnectingFragment extends Fragment implements CameraConnectCoordinator.IStatusView, View.OnClickListener
{
    private final String TAG = this.toString();
    private TextView connectingTextView = null;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_connecting_view, container, false);
        String versionText = getString(R.string.sdk_version) + " " + OLYCamera.getVersion();
        TextView sdkVersionTextView = view.findViewById(R.id.sdkVersionTextView);
        connectingTextView =view.findViewById(R.id.connectingStatusTextView);
        connectingTextView.setOnClickListener(this);

        sdkVersionTextView.setText(versionText);
        sdkVersionTextView.setOnClickListener(this);

        setHasOptionsMenu(true);

        return (view);
    }

    /**
     *  メッセージを表示する
     *
     * @param message  表示するメッセージ
     */
    @Override
    public void setInformationText(String message)
    {
        if (connectingTextView != null)
        {
            connectingTextView.setText(message);
        }
    }

    /**
     *   （タイトルを表示する場合には）表示する
     *
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
    {
        AppCompatActivity activity = (AppCompatActivity)getActivity();
        if (activity != null)
        {
            ActionBar bar = activity.getSupportActionBar();
            if (bar != null) {
                bar.setTitle(getString(R.string.app_name));
            }
        }
    }

    /**
     *   隠し？機能...WiFi接続の進捗メッセージを押したとき...
     *
     */
    @Override
    public void onClick(View v)
    {
        try
        {
            // Wifi 設定画面を表示する
            startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
        }
        catch (android.content.ActivityNotFoundException ex)
        {
            // Activity が存在しなかった...設定画面が起動できなかった
            Log.v(TAG, "android.content.ActivityNotFoundException...");
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
