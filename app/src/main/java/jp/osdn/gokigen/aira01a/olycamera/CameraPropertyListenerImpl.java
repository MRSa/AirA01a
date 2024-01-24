package jp.osdn.gokigen.aira01a.olycamera;

import android.app.Activity;

import jp.co.olympus.camerakit.OLYCamera;
import jp.co.olympus.camerakit.OLYCameraPropertyListener;
import jp.osdn.gokigen.aira01a.liveview.LiveViewFragment;

/**
 *  OLYCameraPropertyListenerの実装
 *  (LiveViewFragment用)
 *
 */
public class CameraPropertyListenerImpl implements OLYCameraPropertyListener
{
    private final String TAG = this.toString();

    public static final String CAMERA_PROPERTY_TAKE_MODE = "TAKEMODE";
    public static final String CAMERA_PROPERTY_DRIVE_MODE = "TAKE_DRIVE";
    public static final String CAMERA_PROPERTY_APERTURE_VALUE = "APERTURE";
    public static final String CAMERA_PROPERTY_SHUTTER_SPEED = "SHUTTER";
    public static final String CAMERA_PROPERTY_EXPOSURE_COMPENSATION = "EXPREV";
    public static final String CAMERA_PROPERTY_ISO_SENSITIVITY = "ISO";
    public static final String CAMERA_PROPERTY_WHITE_BALANCE = "WB";
    public static final String CAMERA_PROPERTY_BATTERY_LEVEL = "BATTERY_LEVEL";
    public static final String CAMERA_PROPERTY_AE_MODE = "AE";
    public static final String CAMERA_PROPERTY_AE_LOCK_STATE = "AE_LOCK_STATE";
    public static final String CAMERA_PROPERTY_FOCUS_STILL = "FOCUS_STILL";
    public static final String CAMERA_PROPERTY_FOCUS_MOVIE = "FOCUS_MOVIE";
    public static final String CAMERA_PROPERTY_COLOR_TONE = "COLORTONE";

    public static final String CAMERA_PROPERTY_VALUE_FOCUS_MF = "FOCUS_MF";
    public static final String CAMERA_PROPERTY_VALUE_FOCUS_SAF = "FOCUS_SAF";
    public static final String CAMERA_PROPERTY_VALUE_FOCUS_CAF = "FOCUS_CAF";

    private LiveViewFragment parent;

    public CameraPropertyListenerImpl(LiveViewFragment parent)
    {
        this.parent = parent;
    }

    @Override
    public void onUpdateCameraProperty(OLYCamera camera, final String name)
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run()
            {
                if (name.equals(CAMERA_PROPERTY_TAKE_MODE)) {
                   parent.updateTakemodeTextView();
                } else if (name.equals(CAMERA_PROPERTY_DRIVE_MODE)) {
                    parent.updateDrivemodeImageView();
                } else if (name.equals(CAMERA_PROPERTY_WHITE_BALANCE)) {
                    parent.updateWhiteBalanceImageView();
                } else if (name.equals(CAMERA_PROPERTY_BATTERY_LEVEL)) {
                    parent.updateBatteryLevelImageView();
                } else if (name.equals(CAMERA_PROPERTY_AE_MODE)) {
                    parent.updateAEModeTextView();
                } else if (name.equals(CAMERA_PROPERTY_AE_LOCK_STATE)) {
                    parent.updateAELockStateImageView();
                    parent.updateApertureValueTextView();
                    parent.updateShutterSpeedTextView();
                }
            }
        });
    }

    private void runOnUiThread(Runnable action)
    {
        Activity activity = parent.getActivity();
        if (activity == null)
        {
            return;
        }
        activity.runOnUiThread(action);
    }

    /**
     *   Read Onlyのプロパティかどうかを確認する
     *   ※ 機能的には "OLYCamera::canSetCameraProperty(String name)" と同じ(はず)。
     *
     *   以下のプロパティが Read Only (CameraKit 1.1.2 の場合)
     *    - AE_LOCK_STATE                       : AEロック状態
     *    - AF_LOCK_STATE                       : AFロック状態
     *    - BATTERY_LEVEL                       : バッテリーレベル
     *    - SSID                                : WiFi SSID
     *    - TOUCH_AE_EFFECTIVE_AREA_LOWER_RIGHT : タッチAE可能範囲(右下座標)
     *    - TOUCH_AE_EFFECTIVE_AREA_UPPER_LEFT  : タッチAE可能範囲(左上座標)
     *    - TOUCH_EFFECTIVE_AREA_LOWER_RIGHT    : タッチAF可能範囲(右下座標)
     *    - TOUCH_EFFECTIVE_AREA_UPPER_LEFT     : タッチAF可能範囲(左上座標)
     *
     * @param name チェックするプロパティ名
     */
    public static boolean canSetCameraProperty(String name)
    {
        return ((name.contains("LOCK_STATE"))||
                (name.contains("EFFECTIVE_AREA"))||
                (name.contains("SSID"))||
                (name.contains("BATTERY_LEVEL")));
    }
}
