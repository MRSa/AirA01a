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
    private lateinit var liveViewFragment: LiveViewFragment

    // 権限要求ランチャー (onCreate 以前のライフサイクル初期化時に登録)
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        if (allPermissionsGranted())
        {
            Log.v(TAG, "All permissions granted by user.")
            changeViewToConnectingFragment()
        }
        else
        {
            Log.v(TAG, "----- APPLICATION LAUNCH ABORTED (Permission Denied) -----")
            Toast.makeText(this, getString(R.string.permission_not_granted), Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        // 画面全体レイアウトの設定
        setContentView(R.layout.activity_main)

        supportActionBar?.hide()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        findViewById<View>(R.id.base_layout)?.let {
            setupWindowInset(it)
        }

        if (!::coordinator.isInitialized)
        {
            coordinator = CameraConnectCoordinator(this, this)
        }

        // 権限チェックと要求
        Log.v(TAG, " ----- SET PERMISSIONS -----")
        val requiredPermissions = getRequiredPermissions()
        if (!allPermissionsGranted())
        {
            requestPermissionLauncher.launch(requiredPermissions)
        }
        else
        {
            // すでにすべての権限がある場合はConnectingFragmentを表示
            changeViewToConnectingFragment()
        }
    }

    private fun setupWindowInset(view: View)
    {
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

    /**
     * OSバージョンに応じて必要な権限一覧を取得
     */
    private fun getRequiredPermissions(): Array<String>
    {
        val permissions = mutableListOf(
            permission.INTERNET,
            permission.ACCESS_NETWORK_STATE,
            permission.ACCESS_WIFI_STATE
        )

        // ストレージ権限 (Android 12 (API 32) 以下のみ必要)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU)
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
            {
                permissions.add(permission.READ_EXTERNAL_STORAGE)
            }
            permissions.add(permission.WRITE_EXTERNAL_STORAGE)
        }

        // 位置情報・Bluetooth権限 (Android 11 以下と 12 以上で分岐)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S)
        {
            permissions.add(permission.BLUETOOTH)
            permissions.add(permission.BLUETOOTH_ADMIN)
            permissions.add(permission.ACCESS_FINE_LOCATION)
            permissions.add(permission.ACCESS_COARSE_LOCATION)
        }
        else
        {
            permissions.add(permission.BLUETOOTH_SCAN)
            permissions.add(permission.BLUETOOTH_CONNECT)
        }

        // 近接Wi-Fiデバイス権限 (Android 13 以上)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        {
            permissions.add(permission.NEARBY_WIFI_DEVICES)
        }

        // メディア位置情報 (Android 10 以上)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        {
            permissions.add(permission.ACCESS_MEDIA_LOCATION)
        }

        // ローカルネットワークアクセス権限 (Android 17 以上)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CINNAMON_BUN)
        {
            permissions.add(permission.ACCESS_LOCAL_NETWORK)
        }

        return permissions.toTypedArray()
    }

    private fun allPermissionsGranted(): Boolean
    {
        for (param in getRequiredPermissions())
        {
            if (ContextCompat.checkSelfPermission(this, param) != PackageManager.PERMISSION_GRANTED)
            {
                Log.v(TAG, " Permission not granted: $param")
                return false
            }
        }
        return true
    }

    override fun onResume()
    {
        super.onResume()
        try
        {
            if (isBlePowerOn)
            {
                // Bluetooth 経由でカメラをONにする場合
                coordinator.wakeup(this)
            }
            else
            {
                // 直接WiFi接続
                coordinator.connect()
            }
        }
        catch (e: Exception)
        {
            Log.e(TAG, "Error in onResume", e)
        }
    }

    override fun onPause()
    {
        super.onPause()
        try
        {
            coordinator.disconnect()
        }
        catch (e: Exception)
        {
            Log.e(TAG, "Error in onPause", e)
        }
    }

    /**
     * カメラとの接続を解除する
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
            Log.e(TAG, "Error in disconnectWithPowerOff", e)
        }
    }

    /**
     * カメラとの接続が確立した時 ... LiveViewFragmentに切り替える
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
            Log.e(TAG, "Error in onCameraConnected", e)
        }
    }

    /**
     * カメラとの接続が切れた時 ... ConnectingFragmentに切り替える
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
            Log.e(TAG, "Error in onCameraDisconnected", e)
        }
    }

    /**
     * カメラとの接続エラーが発生した時
     */
    override fun onCameraOccursException(message: String, e: Exception)
    {
        try
        {
            alertConnectingFailed(message, e)
        }
        catch (ex: Exception)
        {
            Log.e(TAG, "Error in onCameraOccursException", ex)
        }
    }

    /**
     * 致命的なエラーが発生した時... メッセージを表示し、アプリケーションを終了させる
     */
    override fun onCameraConnectFatalError(message: String)
    {
        try
        {
            val builder = AlertDialog.Builder(this)
                .setTitle(getString(R.string.title_fatal_error))
                .setMessage(message)
                .setPositiveButton(getString(R.string.button_exit_application)) { _, _ ->
                    finish()
                }
            runOnUiThread { builder.show() }
        }
        catch (e: Exception)
        {
            Log.e(TAG, "Error in onCameraConnectFatalError", e)
        }
    }

    /**
     * 接続リトライのダイアログを出す
     */
    private fun alertConnectingFailed(message: String, e: Exception)
    {
        try
        {
            val builder = AlertDialog.Builder(this)
                .setTitle(getString(R.string.title_connect_failed))
                .setMessage(if (e.message != null) "<$message> ${e.message}" else "$message : Unknown error")
                .setPositiveButton(getString(R.string.button_retry)) { _, _ ->
                    coordinator.retryConnect()
                }
                .setNeutralButton(R.string.button_wifi_settings) { _, _ ->
                    try
                    {
                        startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
                    }
                    catch (_: ActivityNotFoundException)
                    {
                        Log.v(TAG, "ActivityNotFoundException: Wi-Fi settings UI not found")
                        coordinator.retryConnect()
                    }
                }
            runOnUiThread { builder.show() }
        }
        catch (ex: Exception)
        {
            Log.e(TAG, "Error in alertConnectingFailed ${ex.localizedMessage}")
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
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment1, fragment)
                .commitAllowingStateLoss()
        }
        catch (e: Exception)
        {
            Log.e(TAG, "Error in changeViewToConnectingFragment", e)
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
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment1, liveViewFragment)
                .commitAllowingStateLoss()
        }
        catch (e: Exception)
        {
            Log.e(TAG, "Error in changeViewToLiveViewFragment", e)
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
            coordinator.connect()
        }
        catch (e: Exception)
        {
            Log.e(TAG, "Error in wakeupExecuted", e)
        }
    }

    private val isBlePowerOn: Boolean
        get() {
            return try
            {
                val preferences = PreferenceManager.getDefaultSharedPreferences(this)
                preferences.getBoolean(IOlyCameraBleProperty.OLYCAMERA_BLUETOOTH_POWER_ON, false)
            }
            catch (_: Exception)
            {
                false
            }
        }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean
    {
        Log.v(TAG, "onKeyDown() $keyCode")
        try
        {
            if (event.action == KeyEvent.ACTION_DOWN &&
                (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_CAMERA))
            {
                // Fragmentが初期化済みかチェックしてから呼び出す
                if (::liveViewFragment.isInitialized)
                {
                    return liveViewFragment.handleKeyDown(keyCode, event)
                }
            }
        }
        catch (e: Exception)
        {
            Log.e(TAG, "Error handling key down", e)
        }
        return super.onKeyDown(keyCode, event)
    }

    companion object
    {
        private val TAG = MainActivity::class.java.simpleName
    }
}
