package jp.osdn.gokigen.aira01a.connection.ble

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context.BLUETOOTH_SERVICE
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.preference.PreferenceManager
import jp.co.olympus.camerakit.OLYCamera
import jp.osdn.gokigen.aira01a.R
import java.util.Locale

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
class PowerOnCameraLP(private val context: Activity, private val camera: OLYCamera) : ICameraPowerOn
{
    private val myCameraList: MutableList<OlyCameraSetArrayItem> = ArrayList()
    private var myBluetoothDevice: BluetoothDevice? = null
    private var myBtDevicePassCode = ""

    init
    {
        Log.v(TAG, "PowerOnCameraLP()")
        setupCameraList()
    }

    override fun wakeup(callback: ICameraPowerOn.PowerOnCameraCallback)
    {
        Log.v(TAG, "PowerOnCameraLP::wakeup()")
        val bluetoothManager = context.getSystemService(BLUETOOTH_SERVICE) as BluetoothManager?
        val btAdapter = bluetoothManager?.adapter
        val btScanner = btAdapter?.bluetoothLeScanner
        if ((bluetoothManager == null)||(btAdapter == null)||(btScanner == null) || !btAdapter.isEnabled)
        {
            // ------ Bluetoothの設定がOFFだった
            Log.v(TAG, "Bluetooth is currently off.")
            context.runOnUiThread {
                // Toastで カメラ起動エラーがあったことを通知する
                Toast.makeText(
                    context,
                    context.getString(R.string.ble_setting_is_off),
                    Toast.LENGTH_LONG
                ).show()
            }
            callback.wakeupExecuted(false)
            return
        }

        @SuppressLint("MissingPermission")
        val thread = Thread {
            try {
                // スキャン前のチェック
                myBluetoothDevice = null
                myBtDevicePassCode = ""
                val scanCallback: ScanCallback = object : ScanCallback() {
                    override fun onScanResult(callbackType: Int, result: ScanResult) {
                        try
                        {
                            super.onScanResult(callbackType, result)
                            val bluetoothDevice = result.device
                            val btDeviceName = bluetoothDevice?.name
                            Log.v(TAG, "onScanResult() : (${bluetoothDevice?.name}) ${bluetoothDevice.address}")
                            for (device in myCameraList) {
                                val btName = device.btName
                                if (btName == btDeviceName) {
                                    // マイカメラ発見！
                                    Log.v(TAG, "onLeScan() FIND: [$btName]")

                                    // 別スレッドで起動する
                                    myBluetoothDevice = bluetoothDevice
                                    myBtDevicePassCode = device.btPassCode
                                    break
                                }
                            }
                        }
                        catch (e: Exception)
                        {
                            e.printStackTrace()
                        }
                    }

                    override fun onScanFailed(errorCode: Int) {
                        super.onScanFailed(errorCode)
                        Log.v(TAG, "onScanFailed() : $errorCode")
                        callback.wakeupExecuted(false)
                    }
                }

                // スキャン開始
                Log.v(TAG, " - - - BT SCAN STARTED - - -")
                btScanner.startScan(scanCallback)

                //  BLE_SCAN_TIMEOUT_MILLIS の間だけBLEのスキャンを実施する
                var passed = 0
                while (passed < BLE_SCAN_TIMEOUT_MILLIS) {
                    // BLEデバイスが見つかったときは抜ける...
                    if (myBluetoothDevice != null) {
                        break
                    }

                    // BLEのスキャンが終わるまで待つ
                    Thread.sleep(BLE_WAIT_DURATION.toLong())
                    passed += BLE_WAIT_DURATION
                }

                // スキャンを止める
                //btScanner.flushPendingScanResults(scanCallback)
                btScanner.stopScan(scanCallback)
                Log.v(TAG, " - - - BT SCAN STOPPED - - -")

                if (myBluetoothDevice != null) {
                    // カメラの起動
                    Log.v(
                        TAG,
                        "WAKE UP CAMERA : " + myBluetoothDevice?.name + " [" + myBluetoothDevice?.address + "]"
                    )
                    camera.bluetoothDevice = myBluetoothDevice
                    camera.bluetoothPassword = myBtDevicePassCode
                    camera.wakeup()
                    callback.wakeupExecuted(true)
                } else {
                    callback.wakeupExecuted(false)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Log.v(TAG, "Bluetooth LE SCAN EXCEPTION...")
                callback.wakeupExecuted(false)

                try {
                    val btName = if ((myBluetoothDevice != null)) myBluetoothDevice!!.name else ""
                    context.runOnUiThread {
                        // Toastで カメラ起動エラーがあったことを通知する
                        Toast.makeText(
                            context,
                            context.getString(R.string.launch_fail_via_ble) + btName,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } catch (ee: Exception) {
                    ee.printStackTrace()
                }
            }
            Log.v(TAG, "Bluetooth LE SCAN STOPPED")
        }
        thread.start()
    }

    private fun setupCameraList()
    {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        for (index in 1..IOlyCameraBleProperty.MAX_STORE_PROPERTIES)
        {
            val idHeader = String.format(Locale.ENGLISH, "%03d", index)
            val prefDate = preferences.getString(idHeader + IOlyCameraBleProperty.DATE_KEY, "") ?: ""
            if (prefDate.isEmpty())
            {
                // 登録が途中までだったとき
                break
            }
            val btName = preferences.getString(idHeader + IOlyCameraBleProperty.NAME_KEY, "")?: ""
            val btCode = preferences.getString(idHeader + IOlyCameraBleProperty.CODE_KEY, "")?: ""
            if ((btName.isNotEmpty())&&(btCode.isNotEmpty()))
            {
                myCameraList.add(OlyCameraSetArrayItem(idHeader, btName, btCode, prefDate))
            }
        }
        Log.v(TAG, "setupCameraList() : " + myCameraList.size)
    }

    companion object
    {
        private val TAG = PowerOnCameraLP::class.java.simpleName

        private const val BLE_SCAN_TIMEOUT_MILLIS = 10 * 1000 // 10秒間
        private const val BLE_WAIT_DURATION = 100 // 100ms間隔
    }
}
