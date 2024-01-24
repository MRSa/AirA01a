package jp.osdn.gokigen.aira01a.olycamera;

import android.app.Activity;

import java.util.Map;

import jp.co.olympus.camerakit.OLYCamera;
import jp.co.olympus.camerakit.OLYCameraStatusListener;
import jp.osdn.gokigen.aira01a.liveview.LiveViewFragment;

/**
 *   OLYCameraStatusListenerの実装
 *   (LiveViewFragment用)
 */
public class CameraStatusListenerImpl implements OLYCameraStatusListener
{
    //private final String TAG = this.toString();

    private static final String CAMERA_STATUS_APERTURE_VALUE = "ActualApertureValue";
    private static final String CAMERA_STATUS_SHUTTER_SPEED = "ActualShutterSpeed";
    private static final String CAMERA_STATUS_EXPOSURE_COMPENSATION = "ActualExposureCompensation";
    private static final String CAMERA_STATUS_ISO_SENSITIVITY = "ActualIsoSensitivity";
    private static final String CAMERA_STATUS_RECORDABLEIMAGES = "RemainingRecordableImages";
    private static final String CAMERA_STATUS_MEDIA_BUSY = "MediaBusy";
    private static final String CAMERA_STATUS_MEDIA_ERROR = "MediaError";
    private static final String CAMERA_STATUS_DETECT_FACES = "DetectedHumanFaces";
    private static final String CAMERA_STATUS_FOCAL_LENGTH = "ActualFocalLength";
    private static final String CAMERA_STATUS_ACTUAL_ISO_SENSITIITY_WARNING = "ActualIsoSensitivityWarning";
    private static final String CAMERA_STATUS_EXPOSURE_WARNING = "ExposureWarning";
    private static final String CAMERA_STATUS_EXPOSURE_METERING_WARNING = "ExposureMeteringWarning";
    private static final String CAMERA_STATUS_HIGH_TEMPERATURE_WARNING = "HighTemperatureWarning";
    private static final String CAMERA_STATUS_LEVEL_GAUGE = "LevelGauge";


    private static final float GAUGE_SENSITIVITY = 0.5f;
    private float prevRoll = 0.0f;
    private float prevPitch = 0.0f;
    private String prevOrientation = "";
    private boolean isCheckLevelGauge = false;

    /**
            // まだ実装していないステータス ... たぶん必要ない...
            "LensMountStatus"              // レンズマウント状態
            "MediaMountStatus"             // メディアマウント状態
            "RemainingRecordableTime"      // 撮影動画の最大秒数
            "MinimumFocalLength"           // 最小焦点距離
            "MaximumFocalLength"           // 最大焦点距離
     **/

    private LiveViewFragment parent;

    /**
     *   コンストラクタ
     *
     */
    public CameraStatusListenerImpl(LiveViewFragment parent)
    {
        this.parent = parent;
    }

    @Override
    public void onUpdateStatus(final OLYCamera camera, final String name)
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (name.equals(CAMERA_STATUS_APERTURE_VALUE))
                {
                    parent.updateApertureValueTextView();
                } else if (name.equals(CAMERA_STATUS_SHUTTER_SPEED))
                {
                    parent.updateShutterSpeedTextView();
                } else if (name.equals(CAMERA_STATUS_EXPOSURE_COMPENSATION))
                {
                    parent.updateExposureCompensationTextView();
                } else if (name.equals(CAMERA_STATUS_ISO_SENSITIVITY))
                {
                    parent.updateIsoSensitivityTextView();
                } else if (name.equals(CAMERA_STATUS_RECORDABLEIMAGES) ||
                            name.equals(CAMERA_STATUS_MEDIA_BUSY) ||
                            name.equals(CAMERA_STATUS_MEDIA_ERROR))
                {
                    parent.updateRemainingRecordableImagesTextView();
                } else if (name.equals(CAMERA_STATUS_DETECT_FACES))
                {
                    parent.detectedHumanFaces();
                } else if (name.equals(CAMERA_STATUS_FOCAL_LENGTH))
                {
                    parent.updateFocalLengthView();
                } else if (name.equals(CAMERA_STATUS_EXPOSURE_WARNING) ||
                            name.equals(CAMERA_STATUS_EXPOSURE_METERING_WARNING) ||
                            name.equals(CAMERA_STATUS_ACTUAL_ISO_SENSITIITY_WARNING) ||
                            name.equals(CAMERA_STATUS_HIGH_TEMPERATURE_WARNING))
                {
                    parent.updateWarningTextView();
                } else if (name.equals(CAMERA_STATUS_LEVEL_GAUGE))
                {
                    // デジタル水準器の情報が更新された
                    if (isCheckLevelGauge)
                    {
                        checkLevelGauge(camera);
                    }
                }
                //else
                {
                    // まだ実装していない状態変化をロギングする
                    //Log.v(TAG, "onUpdateStatus() :" + name);
                }
            }
        });
    }

    /**
     *   デジタル水準器の更新を行うかどうかの設定
     *
     */
    public void updateLevelGaugeStatus(boolean levelGauge)
    {
        isCheckLevelGauge = levelGauge;
    }

    /**
     *   レベルゲージの情報確認
     *
     *
     */
    private void checkLevelGauge(OLYCamera camera)
    {
        try
        {
            Map<String, Object> levelGauge = camera.getLevelGauge();
            float roll = (float) levelGauge.get(OLYCamera.LEVEL_GAUGE_ROLLING_KEY);
            float pitch = (float) levelGauge.get(OLYCamera.LEVEL_GAUGE_PITCHING_KEY);
            String orientation = (String) levelGauge.get(OLYCamera.LEVEL_GAUGE_ORIENTATION_KEY);

            // 差動が一定以上あったら報告する
            boolean diffOrientation = prevOrientation.equals(orientation);
            float diffRoll = Math.abs(roll - prevRoll);
            float diffPitch = Math.abs(pitch - prevPitch);
            if ((!diffOrientation)||((!Float.isNaN(roll))&&(diffRoll > GAUGE_SENSITIVITY))||((!Float.isNaN(pitch))&&(diffPitch > GAUGE_SENSITIVITY)))
            {
                // 差動が大きいので変動があったと報告する
                parent.updateLevelGauge(orientation, roll, pitch);

                prevOrientation = orientation;
                prevRoll = roll;
                prevPitch = pitch;
            }
            //else
            //{
            // 差動レベルが一定以下の場合は、報告しない
            //Log.v(TAG, "Level Gauge: " + orientation + "[" + roll + "(" + diffRoll + ")" +  "," + pitch + "(" + diffPitch + ")]");
            //}
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
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
}
