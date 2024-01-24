package jp.osdn.gokigen.aira01a.connection;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import jp.co.olympus.camerakit.OLYCamera;
import jp.co.olympus.camerakit.OLYCameraConnectionListener;
import jp.co.olympus.camerakit.OLYCameraKitException;
import jp.osdn.gokigen.aira01a.connection.ble.ICameraPowerOn;
import jp.osdn.gokigen.aira01a.connection.ble.PowerOnCamera;
import jp.osdn.gokigen.aira01a.myprops.CameraPropertyBackupRestore;
import jp.osdn.gokigen.aira01a.R;

/**
 * CameraInteractionCoordinator : Olympus Air との接続、切断の間をとりもつクラス。
 *                                (OLYCameraクラスの実体を保持する)
 *
 *    1. クラスを作成する
 *    2. prepare() で接続の準備を行う
 *    3. connect() でカメラと接続する
 *    4. disconnect() でカメラと切断する
 *
 *    X. onDisconnectedByError() でカメラの通信状態が変更されたことを受信する
 *    o. CameraInteractionCoordinator.ICameraCallback でカメラとの接続状態を通知する
 *
 */
public class CameraConnectCoordinator implements OLYCameraConnectionListener
{
    private final String TAG = this.toString();
    private final CameraPropertyBackupRestore propertyBackupRestore;
    private final ICameraPowerOn cameraPowerOn;
    private Activity parent;
    private ICameraCallback callbackReceiver;
    private IStatusView statusView = null;
    private Executor connectionExecutor = Executors.newFixedThreadPool(1);
    private BroadcastReceiver connectionReceiver;
    private OLYCamera camera;
    private String connectStatusMessage = "";
    private boolean isConnect = false;

    /**
     * コンストラクタ
     */
    public CameraConnectCoordinator(Activity context, ICameraCallback receiver)
    {
        this.parent = context;
        this.callbackReceiver = receiver;

        // OLYMPUS CAMERA クラスの初期化、リスナの設定
        camera = new OLYCamera();
        camera.setContext(context.getApplicationContext());
        camera.setConnectionListener(this);

        propertyBackupRestore = new CameraPropertyBackupRestore(context, camera);
        cameraPowerOn = new PowerOnCamera(context, camera);

        connectionReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                onReceiveBroadcastOfConnection(intent);
            }
        };
    }

    /**
     * クラスの初期化準備を行う
     */
    public boolean prepare(IStatusView statusView)
    {
        this.statusView = statusView;
        return ((callbackReceiver != null));
    }

    /**
     * カメラとの接続
     * (接続の実処理は onReceiveBroadcastOfConnection() で実施)
     */
    public void connect()
    {
        connectStatusMessage = parent.getString(R.string.connect_prepare);
        updateStatus();

        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        parent.registerReceiver(connectionReceiver, filter);
        isConnect = true;
    }

    /**
     * カメラから切断
     */
    public void disconnect()
    {
        try
        {
            isConnect = false;
            parent.unregisterReceiver(connectionReceiver);
            disconnect(false);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
     *   接続処理を行っているかどうかを応答する
     *
     * @return  true : 接続中 / false : 切断中
     */
    public boolean isConnect()
    {
        return (isConnect);
    }

    /**
     *   Wifiが使える状態だったら、カメラと接続して動作するよ
     *
     */
    private void onReceiveBroadcastOfConnection(Intent intent)
    {
        connectStatusMessage = parent.getString(R.string.connect_check_wifi);
        updateStatus();

        String action = intent.getAction();
        if ((action != null)&&(action.equals(ConnectivityManager.CONNECTIVITY_ACTION)))
        {
            WifiManager wifiManager = (WifiManager) parent.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wifiManager != null)
            {
                WifiInfo info = wifiManager.getConnectionInfo();
                if ((wifiManager.isWifiEnabled()) && (info != null))
                {
                    if (info.getNetworkId() != -1) {
                        Log.w(TAG, "NetworkId is -1.");
                    }
                    startConnectingCamera();
                }
            }
        }
    }

    /**
     *   現在状態をを表示する
     *
     */
    private void updateStatus()
    {
        parent.runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                if ((statusView != null)&&(connectStatusMessage != null))
                {
                    statusView.setInformationText(connectStatusMessage);
                }
            }
        });
    }

    /**
     *   カメラとの接続処理...LiveViewの開始まで
     *
     */
    private void startConnectingCamera()
    {
        connectionExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try
                {
                    connectStatusMessage = parent.getString(R.string.connect_connect);
                    updateStatus();
                    camera.connect(OLYCamera.ConnectionType.WiFi);

                    connectStatusMessage = parent.getString(R.string.connect_change_run_mode);
                    updateStatus();
                    OLYCamera.RunMode runMode = camera.getRunMode();
                    if (runMode == OLYCamera.RunMode.Unknown)
                    {
                        Log.v(TAG, "RunMode is " + runMode + "...");

                        // "Unknown"は、SDKが動作しないモード...
                        //  「未接続の時や実行モード変更時に異常終了した時にこの値になることがあります。」
                        //  だって... でもドキュメントに復旧させる方法が書いてないし...カメラのリセットが必要ぽい。
                        callbackReceiver.onCameraConnectFatalError(parent.getString(R.string.fatal_cannot_use_camera));
                        return;
                    }
                    if (runMode != OLYCamera.RunMode.Recording)
                    {
                        Log.v(TAG, "Change RunMode from " + runMode + " to Recording...");
                        camera.changeRunMode(OLYCamera.RunMode.Recording);
                    }
                    connectStatusMessage = parent.getString(R.string.connect_restore_camera_settings);
                    updateStatus();
                    propertyBackupRestore.restoreCameraSettings("");
                }
                catch (OLYCameraKitException e)
                {
                    callbackReceiver.onCameraOccursException(connectStatusMessage, e);
                    return;
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                    return;
                }

                connectStatusMessage = parent.getString(R.string.connect_connected);
                updateStatus();

                parent.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        callbackReceiver.onCameraConnected(camera);
                    }
                });
            }
        });
    }

    /**
     *　 カメラとの接続を解除する
     *
     * @param powerOff 真ならカメラの電源オフを伴う
     */
    public void disconnect(final boolean powerOff)
    {
        callbackReceiver.onCameraDisconnected();

        connectionExecutor.execute(new Runnable() {
            @Override
            public void run()
            {
                // カメラの設定値をSharedPreferenceに記録する
                propertyBackupRestore.storeCameraSettings("");

                // カメラをPowerOffして接続を切る
                try
                {
                    camera.disconnectWithPowerOff(powerOff);
                }
                catch (OLYCameraKitException e)
                {
                    // エラー情報をログに出力する
                    Log.w(TAG, "To disconnect from the camera is failed. : " + e.getLocalizedMessage());
                }
            }
        });
    }

    /**
     *  カメラの通信状態変化を監視するためのインターフェース
     *
     * @param camera 例外が発生した OLYCamera
     * @param e  カメラクラスの例外
     */
    @Override
    public void onDisconnectedByError(OLYCamera camera, OLYCameraKitException e)
    {
        parent.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                callbackReceiver.onCameraDisconnected();
            }
        });
    }

    /**
     *   カメラとの再接続を指示する
     *
     */
    public void retryConnect()
    {
        startConnectingCamera();
    }

    /**
     * 　カメラを起動する
     *
     * @param callback カメラ起動結果の通知先
     */
    public void wakeup(ICameraPowerOn.PowerOnCameraCallback callback)
    {
        cameraPowerOn.wakeup(callback);
    }

    /**
     *　 CameraInteractionCoordinatorクラスのcallback
     *
     */
    public interface ICameraCallback
    {
        void onCameraConnected(OLYCamera myCamera);
        void onCameraDisconnected();
        void onCameraOccursException(String message, Exception e);
        void onCameraConnectFatalError(String message);
    }

    interface IStatusView
    {
        void setInformationText(String message);
    }
}
