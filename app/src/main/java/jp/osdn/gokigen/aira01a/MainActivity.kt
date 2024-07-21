package jp.osdn.gokigen.aira01a

import android.Manifest.permission
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.preference.PreferenceManager
import jp.co.olympus.camerakit.OLYCamera
import jp.osdn.gokigen.aira01a.connection.CameraConnectCoordinator
import jp.osdn.gokigen.aira01a.connection.CameraConnectCoordinator.ICameraCallback
import jp.osdn.gokigen.aira01a.connection.ConnectingFragment
import jp.osdn.gokigen.aira01a.connection.ble.ICameraPowerOn.PowerOnCameraCallback
import jp.osdn.gokigen.aira01a.connection.ble.IOlyCameraBleProperty
import jp.osdn.gokigen.aira01a.liveview.LiveViewFragment


class MainActivity : AppCompatActivity(), ICameraCallback, PowerOnCameraCallback
{
    private lateinit var coordinator: CameraConnectCoordinator
    private lateinit var liveViewFragment : LiveViewFragment

    /**
     *
     */
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        // 画面全体レイアウトの設定
        setContentView(R.layout.activity_main)

        val bar = supportActionBar
        try
        {
            bar?.hide()
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        catch (e: Exception)
        {
            e.printStackTrace()
        }

        try
        {
            setupWindowInset(findViewById(R.id.base_layout))
        }
        catch (e: Exception)
        {
            e.printStackTrace()
        }

        if (!::coordinator.isInitialized)
        {
            coordinator = CameraConnectCoordinator(this, this)
        }

/*
        // 外部メモリアクセス権のオプトイン
        if ((ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED) ||
            (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_NETWORK_STATE
            ) != PackageManager.PERMISSION_GRANTED) ||
            (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_WIFI_STATE
            ) != PackageManager.PERMISSION_GRANTED) ||
            (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH
            ) != PackageManager.PERMISSION_GRANTED) ||
            (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_ADMIN
            ) != PackageManager.PERMISSION_GRANTED) ||
            (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED) ||
            (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.INTERNET
            ) != PackageManager.PERMISSION_GRANTED)
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.ACCESS_NETWORK_STATE,
                    Manifest.permission.ACCESS_WIFI_STATE,
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.INTERNET,
                ),
                REQUEST_NEED_PERMISSIONS
            )
        }
*/
        try
        {
            ///////// SET PERMISSIONS /////////
            Log.v(TAG, " ----- SET PERMISSIONS -----")
            if (!allPermissionsGranted())
            {
                val requestPermission = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {

                    ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_NEED_PERMISSIONS)
                    if(!allPermissionsGranted())
                    {
                        // Abort launch application because required permissions was rejected.
                        Toast.makeText(this, getString(R.string.permission_not_granted), Toast.LENGTH_SHORT).show()
                        Log.v(TAG, "----- APPLICATION LAUNCH ABORTED -----")
                        finish()
                    }
                }
                requestPermission.launch(REQUIRED_PERMISSIONS)
            }
        }
        catch (e: Exception)
        {
            e.printStackTrace()
        }

        // ConnectingFragmentを表示する
        changeViewToConnectingFragment()
    }

    private fun setupWindowInset(view: View)
    {
        try
        {
            // Display cutout insets
            //   https://developer.android.com/develop/ui/views/layout/edge-to-edge
            ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
                val bars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                            or WindowInsetsCompat.Type.displayCutout()
                )
                v.updatePadding(
                    left = bars.left,
                    top = bars.top,
                    right = bars.right,
                    bottom = bars.bottom,
                )
                WindowInsetsCompat.CONSUMED
            }
        }
        catch (e: Exception)
        {
            e.printStackTrace()
        }
    }

    private fun allPermissionsGranted() : Boolean
    {
        var result = true
        for (param in REQUIRED_PERMISSIONS)
        {
            if (ContextCompat.checkSelfPermission(
                    baseContext,
                    param
                ) != PackageManager.PERMISSION_GRANTED
            )
            {
                // ----- Permission Denied...
                if ((param == permission.ACCESS_MEDIA_LOCATION)&&(Build.VERSION.SDK_INT < Build.VERSION_CODES.Q))
                {
                    //　この場合は権限付与の判断を除外 (デバイスが (10) よりも古く、ACCESS_MEDIA_LOCATION がない場合）
                }
                else if ((param == permission.READ_EXTERNAL_STORAGE)&&(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU))
                {
                    // この場合は、権限付与の判断を除外 (SDK: 33以上はエラーになる...)
                }
                else if ((param == permission.WRITE_EXTERNAL_STORAGE)&&(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU))
                {
                    // この場合は、権限付与の判断を除外 (SDK: 33以上はエラーになる...)
                }
                else if ((param == permission.BLUETOOTH_SCAN)&&(Build.VERSION.SDK_INT < Build.VERSION_CODES.S))
                {
                    // この場合は、権限付与の判断を除外 (SDK: 31よりも下はエラーになるはず...)
                    Log.v(TAG, "BLUETOOTH_SCAN")
                }
                else if ((param == permission.BLUETOOTH_CONNECT)&&(Build.VERSION.SDK_INT < Build.VERSION_CODES.S))
                {
                    // この場合は、権限付与の判断を除外 (SDK: 31よりも下はエラーになるはず...)
                    Log.v(TAG, "BLUETOOTH_CONNECT")
                }
                else
                {
                    // ----- 権限が得られなかった場合...
                    Log.v(TAG, " Permission: $param : ${Build.VERSION.SDK_INT}")
                    result = false
                }
            }
        }
        return (result)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.v(TAG, "------------------------- onRequestPermissionsResult() ")
        if (requestCode == REQUEST_NEED_PERMISSIONS)
        {
            if (allPermissionsGranted())
            {
                // ----- 権限が有効だった、最初の画面を開く
                Log.v(TAG, "onRequestPermissionsResult()")
                // ConnectingFragmentを表示する
                changeViewToConnectingFragment()
            }
            else
            {
                Log.v(TAG, "----- onRequestPermissionsResult() : false")
                Toast.makeText(this, getString(R.string.permission_not_granted), Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    override fun onResume()
    {
        super.onResume()
        try
        {
            if (isBlePowerOn)
            {
                // Bluetooth 経由でカメラをONにする場合...
                try
                {
                    // カメラの電源ONクラスを呼び出しておく (電源ONができたら、コールバックをもらう）
                    coordinator.wakeup(this)
                }
                catch (e: Exception)
                {
                    e.printStackTrace()
                }
            }
            else
            {
                // そうじゃない場合は、直接WiFi接続
                coordinator.connect()
            }
        }
        catch (e :Exception)
        {
            e.printStackTrace()
        }
    }

    /**
     *
     */
    override fun onPause()
    {
        super.onPause()
        try
        {
            coordinator.disconnect()
        }
        catch (e: Exception)
        {
            e.printStackTrace()
        }
    }

    /**
     *
     *
     */
    public override fun onStart()
    {
        super.onStart()
    }

    /**
     *
     *
     */
    public override fun onStop()
    {
        super.onStop()
    }

    /**
     * 　 カメラとの接続を解除する
     *
     * @param powerOff 真ならカメラの電源オフを伴う
     */
    fun disconnectWithPowerOff(powerOff: Boolean)
    {
        try
        {
            coordinator.disconnect(powerOff)
        }
        catch (e: Exception)
        {
            e.printStackTrace()
        }
    }

    /**
     * カメラとの接続が確立した時 ... LiveViewFragmentに切り替える
     * (CameraCoordinator.ICameraCallback の実装)
     */
    override fun onCameraConnected(myCamera: OLYCamera)
    {
        try
        {
            if (coordinator.isConnect)
            {
                changeViewToLiveViewFragment(myCamera)
            }
        }
        catch (e: Exception)
        {
            e.printStackTrace()
        }
    }

    /**
     * カメラとの接続が切れた時 ... ConnectingFragmentに切り替える
     * (CameraCoordinator.ICameraCallback の実装)
     */
    override fun onCameraDisconnected()
    {
        try
        {
            if (coordinator.isConnect)
            {
                changeViewToConnectingFragment()
            }
        }
        catch (e: Exception)
        {
            e.printStackTrace()
        }
    }

    /**
     * カメラとの接続エラーが発生した時 ... ConnectingFragmentに切り替える
     * (CameraCoordinator.ICameraCallback の実装)
     *
     */
    override fun onCameraOccursException(message: String, e: Exception)
    {
        try
        {
            alertConnectingFailed(message, e)
            onCameraDisconnected()
        }
        catch (e: Exception)
        {
            e.printStackTrace()
        }
    }

    /**
     * 致命的なエラーが発生した時... メッセージを表示し、アプリケーションを終了させる
     *
     * @param message ユーザに知らせるメッセージ
     */
    override fun onCameraConnectFatalError(message: String)
    {
        try
        {
            val builder = AlertDialog.Builder(this)
                .setTitle(getString(R.string.title_fatal_error))
                .setMessage(message)
                .setPositiveButton(
                    getString(R.string.button_exit_application)
                ) { _, _ -> // アプリケーションを終了させる
                    finish()
                }
            runOnUiThread { builder.show() }
        }
        catch (e: Exception)
        {
            e.printStackTrace()
        }
    }

    /**
     * 接続リトライのダイアログを出す
     *
     */
    private fun alertConnectingFailed(message: String, e: Exception)
    {
        try
        {
            val builder = AlertDialog.Builder(this)
                .setTitle(getString(R.string.title_connect_failed))
                .setMessage(if (e.message != null) "<" + message + "> " + e.message else "$message : Unknown error")
                .setPositiveButton(
                    getString(R.string.button_retry)
                ) { _, _ -> coordinator.retryConnect() }
                .setNeutralButton(
                    R.string.button_wifi_settings
                ) { _, _ ->
                    try {
                        // Wifi 設定画面を表示する
                        startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
                    }
                    catch (ex: ActivityNotFoundException)
                    {
                        // Activity が存在しなかった...設定画面が起動できなかった
                        Log.v(TAG, "android.content.ActivityNotFoundException...")

                        // この場合は、再試行と等価な動きとする
                        coordinator.retryConnect()
                    }
                    catch (e: Exception)
                    {
                        e.printStackTrace()
                    }
                }
            runOnUiThread { builder.show() }
        }
        catch (e : Exception)
        {
            e.printStackTrace()
        }
    }

    /**
     * ConnectingFragmentに表示を切り替える実処理
     */
    private fun changeViewToConnectingFragment()
    {
        try
        {
            if (!::coordinator.isInitialized)
            {
                coordinator = CameraConnectCoordinator(this, this)
            }
            val fragment = ConnectingFragment()
            coordinator.prepare(fragment)
            val transaction = supportFragmentManager.beginTransaction()
            transaction.replace(R.id.fragment1, fragment)
            transaction.commitAllowingStateLoss()
        }
        catch (e : Exception)
        {
            e.printStackTrace()
        }
    }

    /**
     * LiveViewFragmentに表示を切り替える実処理
     */
    private fun changeViewToLiveViewFragment(myCamera: OLYCamera)
    {
        try
        {
            if (!::liveViewFragment.isInitialized)
            {
                liveViewFragment = LiveViewFragment()
            }
            liveViewFragment.setCamera(myCamera)
            val transaction = supportFragmentManager.beginTransaction()
            transaction.replace(R.id.fragment1, liveViewFragment)
            transaction.commitAllowingStateLoss()
        }
        catch (e: Exception)
        {
            e.printStackTrace()
        }
    }

    /**
     * Bluetooth Smartでのカメラ起動シーケンスが終了したとき
     */
    override fun wakeupExecuted(isExecuted: Boolean)
    {
        Log.v(TAG, "wakeupExecuted() : $isExecuted")
        try
        {
            // このタイミングでカメラへWiFi接続する
            coordinator.connect()
        }
        catch (e: Exception)
        {
            e.printStackTrace()
        }
    }

    private val isBlePowerOn: Boolean
        /**
         * BLE経由でカメラの電源を入れるかどうか
         */
        get() {
            var ret = false
            try {
                val preferences = PreferenceManager.getDefaultSharedPreferences(
                    this
                )
                ret = preferences.getBoolean(
                    IOlyCameraBleProperty.OLYCAMERA_BLUETOOTH_POWER_ON,
                    false
                )
                // Log.v(TAG, "isBlePowerOn() : " + ret);
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return (ret)
        }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean
    {
        Log.v(TAG, "onKeyDown() $keyCode")
        try
        {
            if ((event.action == KeyEvent.ACTION_DOWN) && ((keyCode == KeyEvent.KEYCODE_VOLUME_UP) || (keyCode == KeyEvent.KEYCODE_CAMERA)))
            {
                return (liveViewFragment.handleKeyDown(keyCode, event))
            }
        }
        catch (e: Exception)
        {
            e.printStackTrace()
        }
        return (super.onKeyDown(keyCode, event))
    }

    companion object
    {
        private val TAG = MainActivity::class.java.simpleName
        private const val REQUEST_NEED_PERMISSIONS = 1010
        private val REQUIRED_PERMISSIONS = arrayOf(
            permission.WRITE_EXTERNAL_STORAGE,
            permission.ACCESS_NETWORK_STATE,
            permission.ACCESS_WIFI_STATE,
            permission.INTERNET,
            permission.BLUETOOTH,
            permission.BLUETOOTH_ADMIN,
            permission.ACCESS_COARSE_LOCATION,
            permission.ACCESS_FINE_LOCATION,
            permission.READ_EXTERNAL_STORAGE,
            permission.ACCESS_MEDIA_LOCATION,
            permission.BLUETOOTH_CONNECT,
            permission.BLUETOOTH_SCAN,
        )
    }
}
