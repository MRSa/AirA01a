package jp.osdn.gokigen.aira01a.takepicture;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.RectF;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import jp.co.olympus.camerakit.OLYCamera;
import jp.co.olympus.camerakit.OLYCameraKitException;
import jp.osdn.gokigen.aira01a.liveview.CameraLiveImageView;
import jp.osdn.gokigen.aira01a.liveview.CameraPropertyHolder;
import jp.osdn.gokigen.aira01a.olycamera.CameraPropertyListenerImpl;
import jp.osdn.gokigen.aira01a.liveview.LiveViewFragment;
import jp.osdn.gokigen.aira01a.R;

/**
 *  カメラの制御と状態を管理するクラス
 *
 *
 */
public class CameraController implements ITakePictureRequestedControl
{
    private final String TAG = this.toString();
    private LiveViewFragment parent;
    private OLYCamera camera = null;
    private boolean enabledFocusLock = false;
    private boolean enabledManualFocus = false;
    private boolean enabledAutoExposureLock = false;

    private String otherText = "";

    /**
     * constructor
     *
     * @param parent 親のFragment
     */
    public CameraController(LiveViewFragment parent) {
        this.parent = parent;
    }

    /**
     * OLYMPUS Cameraオブジェクトの登録
     *
     * @param camera OLYMPUS Camera オブジェクト
     */
    public void setCamera(OLYCamera camera) {
        this.camera = camera;
    }

    /**
     * タッチシャッターが有効かどうかを応答する
     *
     * @return true : enable touch shutter / false : disable touch shutter
     */
    public boolean getTouchShutterStatus() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(parent.getActivity());
        return (preferences.getBoolean("touch_shutter", false));
    }

    /**
     *  デジタル水準器が有効かどうかを応答する
     *
     * @return true : enable level gauge / false : disable level gauge
     */
    public boolean getLevelGaugeStatus()
    {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(parent.getActivity());
        return (preferences.getBoolean("level_gauge", false));
    }

    /**
     *   オートブラケッティングの設定
     *
     * @param isCount True: 撮影枚数  / False: ブラケッティングを実施する種類
     * @return  オートブラケッティングの設定値
     */
    @Override
    public int getAutoBracketingSetting(boolean isCount)
    {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(parent.getActivity());
        String value;
        if (!isCount)
        {
            // ブラケッティングの種類
            value = preferences.getString("auto_bracketing", "0");
        }
        else
        {
            // ブラケッティングで撮影する枚数
            value =  preferences.getString("shooting_count", "3");
        }
        return (Integer.parseInt(value));
    }

    /**
     *
     *
     */
    @Override
    public void setShutterImageSelected(final boolean isSelected)
    {
        Activity activity = parent.getActivity();
        if (activity != null)
        {
            parent.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    parent.setShutterImageSelected(isSelected);
                }
            });
        }
    }

    /**
     *
     *
     */
    @Override
    public boolean getFocusFrameStatus()
    {
        return (enabledFocusLock);
    }

    /**
     *
     *
     */
    @Override
    public void setFocusFrameStatus(boolean isShow)
    {
        enabledFocusLock = isShow;
    }

    /**
     *
     *
     */
    @Override
    public void hideFocusFrame(boolean isUiThread)
    {
        if (isUiThread)
        {
            Activity activity = parent.getActivity();
            if (activity != null)
            {
                // UIスレッドで実行する
                parent.getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        parent.getLiveImageView().hideFocusFrame();
                    }
                });
            }
        }
        else
        {
            // 直接実行する
            parent.getLiveImageView().hideFocusFrame();
        }
    }

    /**
     *
     *
     */
    @Override
    public void showFocusFrame(RectF rect, CameraLiveImageView.FocusFrameStatus status)
    {
        parent.getLiveImageView().showFocusFrame(rect, status);
    }

    /**
     *
     *
     *
     */
    @Override
    public void showFocusFrame(RectF rect, CameraLiveImageView.FocusFrameStatus status, double duration)
    {
        parent.getLiveImageView().showFocusFrame(rect, status, duration);
    }

    /**
     *
     *
     *
     */
    @Override
    public float getIntrinsicContentSizeWidth()
    {
        return (parent.getLiveImageView().getIntrinsicContentSizeWidth());
    }

    /**
     *
     *
     *
     */
    @Override
    public float getIntrinsicContentSizeHeight()
    {
        return (parent.getLiveImageView().getIntrinsicContentSizeHeight());
    }

    /**
     *
     *
     *
     */
    @Override
    public void showOtherInformation(String message)
    {
        otherText = message;
        Activity activity = parent.getActivity();
        if (activity != null)
        {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    parent.updateOtherTextView();
                }
            });
        }
    }

    /**
     *
     *
     */
    @Override
    public boolean getShootingBusyStatus()
    {
        boolean isBusy = false;
        try
        {
            isBusy = ((camera.isTakingPicture())||(camera.isMediaBusy())||(camera.isRecordingVideo()));

            // ちょっと待ち時間をとりたい...
            String messageToShow = "getShootingBusyStatus() : " + String.valueOf(isBusy);
            Log.v(TAG, messageToShow);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return (isBusy);
    }

    /**
     *
     *
     */
    @Override
    public void presentMessage(final int resId, final String message)
    {
        Activity activity = parent.getActivity();
        if (activity != null)
        {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    parent.presentMessage(parent.getString(resId), message);
                }
            });
        }
    }

    /**
     * 電動ズーム機能を持つレンズが装着されているか確認
     *
     * @return true ; 電動ズーム付き / false : 電動ズームなし
     */
    public boolean isElectricZoomLens()
    {
        return ((camera != null)&&(camera.getLensMountStatus()).contains("electriczoom"));
    }

    /**
     * 現在ズーム中か確認する
     *
     * @return true : ズーム中  / false : ズーム中でない
     */
    public boolean isZooming()
    {
        return (camera.isDrivingZoomLens());
    }

    /**
     * ズームレンズを動作させる
     *
     * @param direction ズームさせる方向 (+ズームイン / - ズームアウト)
     */
    public void driveZoomLens(int direction)
    {
        try
        {
            // レンズがサポートする焦点距離と、現在の焦点距離を取得する
            float minLength = camera.getMinimumFocalLength();
            float maxLength = camera.getMaximumFocalLength();
            float currentFocalLength = camera.getActualFocalLength();
            float targetFocalLength = currentFocalLength;

            if (direction > 0)
            {
                // ズームインする
                // TODO: ステップズームにしたい
                targetFocalLength = targetFocalLength * 1.15f;
            }
            else
            {
                // ズームアウトする
                // TODO: ステップズームにしたい
                targetFocalLength = targetFocalLength * 0.9f;
            }

            // 焦点距離が最大値・最小値を超えないようにする
            if (targetFocalLength > maxLength)
            {
                targetFocalLength = maxLength;
            }
            if (targetFocalLength < minLength)
            {
                targetFocalLength = minLength;
            }

            // レンズのスーム操作
            Log.v(TAG, "ZOOMING ACTION: " + currentFocalLength + " to " + targetFocalLength);

            // レンズをズーム動作する
            camera.startDrivingZoomLensToFocalLength(targetFocalLength);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
     * 指定したプロパティが設定可能か確認する
     *
     * @return 設定可否
     */
    public boolean canSetCameraProperty(String name) {
        return (camera.canSetCameraProperty(name));
    }

    /**
     * 設定したカメラのプロパティの値を取得する
     *
     * @return カメラプロパティ
     */
    public String getCameraPropertyValue(String name) {
        String property = "";
        try {
            property = camera.getCameraPropertyValue(name);
        } catch (OLYCameraKitException e) {
            e.printStackTrace();
        }
        return (property);

    }

    /**
     * カメラのプロパティ名（表示用）を取得する
     *
     * @return カメラプロパティ表示タイトル
     */
    public String getCameraPropertyValueTitle(String name) {
        try {
            return (camera.getCameraPropertyValueTitle(camera.getCameraPropertyValue(name)));
        } catch (OLYCameraKitException e) {
            e.printStackTrace();
        }
        return ("");
    }

    /**
     * @return  シャッタースピード
     */
    public String getActualShutterSpeed()
    {
        String value = "";
        try
        {
            String data[] = OLYCamera.decodeCameraPropertyValue(camera.getActualShutterSpeed());
            value = data[1];
        }
        catch (Exception e)
        {
            //
        }
        return (value);
    }

    /**
     *
     *
     * @return 絞り値
     */
    public String getActualApertureValue()
    {
        String value = "";
        try
        {
            String data[] = OLYCamera.decodeCameraPropertyValue(camera.getActualApertureValue());
            value = data[1];
        }
        catch (Exception e)
        {
            //
        }
        return (value);
    }

    /**
     *
     * @return  露出補正値
     */
    public String getActualExposureCompensation()
    {
        String value = "";
        try
        {
            String data[] = OLYCamera.decodeCameraPropertyValue(camera.getActualExposureCompensation());
            value = data[1];
        }
        catch (Exception e)
        {
            //
        }
        return (value);
    }

    /**
     *   現在のISO感度を応答する
     *
     * @return ISO感度
     */
    public String getActualIsoSensitivity()
    {
        String value = camera.getCameraPropertyValueTitle(camera.getActualIsoSensitivity());
        if (value == null)
        {
            value = "";
        }
        return (value);
    }

    /**
     *  現在の焦点距離を応答する
     *
     * @return  焦点距離 (17mm といった文字列)
     */
    public String getFocalLength()
    {
        return (String.format(Locale.ENGLISH, "%3.0fmm", camera.getActualFocalLength()));
    }

    /**
     *  ISO感度を更新する
     *
     */
    public void changeIsoSensitivity()
    {
        parent.presentPropertyValueList(parent.getIsoSensitivityHolder(), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                setPropertyValue(parent.getIsoSensitivityHolder(), which);
            }
        });
    }

    /**
     *  シャッタースピードを更新する
     *
     */
    public void changeShutterSpeed()
    {
        parent.presentPropertyValueList(parent.getShutterSpeedHolder(), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                setPropertyValue(parent.getShutterSpeedHolder(), which);
            }
        });
    }

    /**
     *   絞り値を更新する
     *
     *
     */
    public void changeApertureValue()
    {
        parent.presentPropertyValueList(parent.getApertureHolder(), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                dialog.dismiss();
                setPropertyValue(parent.getApertureHolder(), which);
            }
        });
    }

    /**
     *  測光方式を更新する
     *
     */
    public void changeAEModeValue()
    {
        parent.presentPropertyValueList(parent.getAEModeHolder(), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                setPropertyValue(parent.getAEModeHolder(), which);
            }
        });
    }

    /**
     * 　警告表示テキストを取得する
     *
     * @return  警告表示
     */
    public String getWarningText()
    {
        String text = "";
        Activity activity = parent.getActivity();
        if (activity != null)
        {
            if (camera.isHighTemperatureWarning()) {
                // 温度警告
                text = parent.getActivity().getString(R.string.warn_high_temperature);
            } else if ((camera.isActualIsoSensitivityWarning()) ||
                    (camera.isExposureWarning()) ||
                    (camera.isExposureMeteringWarning())) {
                // 露出警告
                text = parent.getActivity().getString(R.string.warn_exposure);
            }
        }
        return (text);
    }

    /**
     * 　情報表示テキストを取得する
     *
     * @return  情報表示テキスト
     */
    public String getInformationText()
    {
        return ("");
    }

    /**
     *   その他の情報表示テキストを取得する
     *
     * @return その他の情報表示テキスト
     */
    public String getOtherText()
    {
        return (otherText);
    }

    /**
     *   顔検出して応答する
     *
     */
    public Map<String, RectF> getDetectedHumanFaces()
    {
        return (camera.getDetectedHumanFaces());
    }

    /**
     *   AEロック状態をトグルする
     *
     */
    public void toggleAELockStatus()
    {
        try
        {
            if (enabledAutoExposureLock)
            {
                Log.v(TAG, "toggleAELockStatus() : unlockAutoExposure()");
                camera.unlockAutoExposure();
            }
            else
            {
                Log.v(TAG, "toggleAELockStatus() : lockAutoExposure()");
                camera.lockAutoExposure();
            }
            parent.updateAELockStateImageView();  // AE-Lボタンの状態を更新する
            parent.updateApertureValueTextView();
            parent.updateShutterSpeedTextView();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
     *   AEロック状態を記憶する
     *
     * @param lockStatus AEロック状態
     */
    public void setAELockStatus(boolean lockStatus)
    {
        Log.v(TAG, "setAELockStatus :" + lockStatus);
        enabledAutoExposureLock = lockStatus;
    }

    /**
     *   マニュアルフォーカス状態を記憶する
     *
     * @param isManualFocus  マニュアルフォーカス状態
     */
    public void setManualFocusStatus(boolean isManualFocus)
    {
        Log.v(TAG, "setManualFocusStatus :" + isManualFocus);
        enabledManualFocus = isManualFocus;

    }

    /**
     *   マニュアルフォーカス状態をトグルする
     *
     */
    public void toggleManualFocusStatus()
    {
        try
        {
            String property_name = CameraPropertyListenerImpl.CAMERA_PROPERTY_FOCUS_STILL;
            String proverty_value = "<" + CameraPropertyListenerImpl.CAMERA_PROPERTY_FOCUS_STILL + "/";
            // マニュアルフォーカス
            if (enabledManualFocus)
            {
                // MF -> AF
                Log.v(TAG, "toggleManualFocusStatus() : to " + CameraPropertyListenerImpl.CAMERA_PROPERTY_VALUE_FOCUS_SAF);
                proverty_value = proverty_value + CameraPropertyListenerImpl.CAMERA_PROPERTY_VALUE_FOCUS_SAF + ">";
                camera.setCameraPropertyValue(property_name, proverty_value);
            }
            else
            {
                // AF -> MF  : オートフォーカスを解除して設定する
                Log.v(TAG, "toggleManualFocusStatus() : to " + CameraPropertyListenerImpl.CAMERA_PROPERTY_VALUE_FOCUS_MF);
                proverty_value = proverty_value + CameraPropertyListenerImpl.CAMERA_PROPERTY_VALUE_FOCUS_MF + ">";
                camera.unlockAutoFocus();
                camera.setCameraPropertyValue(property_name, proverty_value);
                hideFocusFrame(false);
            }
            parent.updateManualFocusStateImageView(); // MFボタンの状態を更新する
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
     *   ホワイトバランスを更新する
     *
     */
    public void changeWhiteBalance()
    {
        parent.presentPropertyValueList(parent.getWhiteBalanceHolder(), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                dialog.dismiss();
                setPropertyValue(parent.getWhiteBalanceHolder(), which);
                finishWhiteBalance();
            }
        });
    }

    /**
     *   ホワイトバランスの更新処理（後処理）
     *
     */
    private void finishWhiteBalance()
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                parent.updateWhiteBalanceImageView();
            }
        });
    }

    /**
     *   ドライブモードを更新する
     *
     *
     */
    public void changeDriveMode()
    {
        parent.presentPropertyValueList(parent.getDriveModeHolder(), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                dialog.dismiss();
                setPropertyValue(parent.getDriveModeHolder(), which);
                finishDriveMode();
            }
        });
    }

    /**
     *   ドライブモードの更新後処理
     *
     */
    private void finishDriveMode()
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                parent.updateDrivemodeImageView();
            }
        });
    }

    /**
     *
     *
     */
    public void changeExposureCompensation()
    {
        parent.presentPropertyValueList(parent.getExposureCompensationHolder(), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                dialog.dismiss();
                setPropertyValue(parent.getExposureCompensationHolder(), which);
            }
        });
    }

    /**
     *
     *
     */
    public void changeTakeMode()
    {
        parent.presentPropertyValueList(parent.getTakeModeHolder(), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                dialog.dismiss();
                setPropertyValue(parent.getTakeModeHolder(), which);
                finishTakeMode();
            }
        });
    }

    /**
     *
     *
     */
    private void finishTakeMode()
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run() {
                parent.updateTakemodeTextView();
                try
                {
                    camera.clearAutoFocusPoint();
                    camera.unlockAutoFocus();
                }
                catch (OLYCameraKitException e)
                {
                    e.printStackTrace();
                }
                enabledFocusLock = false;
                parent.getLiveImageView().hideFocusFrame();
            }
        });
    }

    /**
     *   カメラを撮影モードに切り替える
     *
     * @return  true : 切り替え成功 / false : 切替え失敗
     */
    public boolean changeToRecordingMode()
    {
        Log.v(TAG, "changeToRecordingMode()");
        if (camera.getRunMode() == OLYCamera.RunMode.Recording)
        {
            Log.v(TAG, "changeToRecordingMode() End");
            return (true);
        }
        boolean changeMode = changeRunMode(OLYCamera.RunMode.Recording);
        restoreCameraSettingsOnlyDifference("");
        Log.v(TAG, "changeToRecordingMode() End");
        return (changeMode);
    }

    /**
     *  カメラを再生モードに切り替える
     *
     * @return  true : 切り替え成功 / false : 切替え失敗
     */
    public boolean changeToPlaybackMode()
    {
        Log.v(TAG, "changeToPlaybackMode()");
        if (camera.getRunMode() == OLYCamera.RunMode.Playback)
        {
            Log.v(TAG, "changeToPlaybackMode() End");
            return (true);
        }
        Thread thread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                storeCameraSettings();
                changeRunMode(OLYCamera.RunMode.Playback);
                Log.v(TAG, "changeToPlaybackMode() End");
            }
        });
        thread.start();

        //boolean ret = changeRunMode(OLYCamera.RunMode.Playback);
        //Log.v(TAG, "changeToPlaybackMode() End");

        return (true);
    }

    /**
     *   カメラの動作モードを切り替える
     *
     */
    private boolean changeRunMode(OLYCamera.RunMode mode)
    {
        boolean ret = false;
        try
        {
            //dumpCameraPropertyValues();
            camera.changeRunMode(mode);
            ret = true;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return (ret);
    }

    /*
     *  カメラのプロパティを取得してログに出力する
     *  （デバッグ用）
     */
/*
    private void dumpCameraPropertyValues()
    {
        try
        {
            Map<String, String> values = null;
            values = camera.getCameraPropertyValues(camera.getCameraPropertyNames());
            if (values != null) {
                for (String key : values.keySet()) {
                    Log.v(TAG, "dumpCameraPropertyValues(): " + values.get(key));
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
*/

    /**
     *   カメラの現在の設定を本体から読みだしてPreferenceに記憶する
     *
     */
    private void storeCameraSettings()
    {
        // カメラから設定を読みだして、Preferenceに記録する
        if (camera.isConnected())
        {
            Map<String, String> values = null;
            try
            {
                values = camera.getCameraPropertyValues(camera.getCameraPropertyNames());
            }
            catch (OLYCameraKitException e)
            {
                Log.w(TAG, "To get the camera properties is failed: " + e.getMessage());
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

            if (values != null)
            {
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(parent.getContext());
                SharedPreferences.Editor editor = preferences.edit();
                for (String key : values.keySet())
                {
                    editor.putString(key, values.get(key));
                    //Log.v(TAG, "storeCameraSettings(): " + values.get(key));
                }
                //editor.commit();
                editor.apply();
            }
        }
    }



    /**
     *   カメラプロパティを差分だけ設定する
     *
     */
    private void restoreCameraSettingsOnlyDifference(String idHeader)
    {
        Log.v(TAG, "restoreCameraSettings() : START [" + idHeader + "]");

        int setCount = 0;

        // Restores my settings.
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(parent.getContext());
        if (camera.isConnected())
        {
            //  まずは、現在の設定値を全部とってくる
            Map<String, String> propertyValues;
            try
            {
                propertyValues = camera.getCameraPropertyValues(camera.getCameraPropertyNames());
            }
            catch (Exception e)
            {
                // 設定値が取得できなかった場合は、終了する。
                e.printStackTrace();
                Log.v(TAG, "restoreCameraSettingsOnlyDifference() : FAIL...");
                return;
            }
            if (propertyValues == null)
            {
                // プロパティの取得が失敗していたら、何もせずに終了する。
                Log.v(TAG, "restoreCameraSettingsOnlyDifference() : PROPERTIES FAIL...");
                return;
            }

            //////// TAKEMODEだけは先行設定 ////
            String takeModeValue = preferences.getString(idHeader + "TAKEMODE", null);
            try
            {
                // TAKEMODE だけは先行して設定する（設定できないカメラプロパティもあるので...）
                if (takeModeValue != null)
                {
                    camera.setCameraPropertyValue("TAKEMODE", takeModeValue);
                    Log.v(TAG, "loadCameraSettingsOnlyDifferences() TAKEMODE : " + takeModeValue);
                    setCount++;
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
                Log.v(TAG, "loadCameraSettings() : restoreCameraSettingsOnlyDifference() TAKEMODE fail...");
            }

            //////////  差分だけカメラプロパティの設定をする  //////////
            Set<String> names = camera.getCameraPropertyNames();
            for (String name : names)
            {
                String value = preferences.getString(idHeader + name, null);
                String currentValue = propertyValues.get(name);
                if ((value != null)&&(currentValue != null)&&(!value.equals(currentValue)))
                {
                    if (!CameraPropertyListenerImpl.canSetCameraProperty(name))
                    {
                        // Read Onlyのプロパティを除外して一つづづ登録
                        try
                        {
                            Log.v(TAG, "restoreCameraSettingsOnlyDifference(): SET : " + value);
                            camera.setCameraPropertyValue(name, value);
                            setCount++;
                        }
                        catch (Exception e)
                        {
                            e.printStackTrace();
                        }
                    }
                }
            }
            Log.v(TAG, "restoreCameraSettingsOnlyDifference() : END [" + idHeader + "]" + " " +setCount);
        }
    }


    /*
     *   Preferenceにあるカメラの設定をカメラに登録する
     *　(注： Read Onlyなパラメータを登録しようとするとエラーになるので注意）
     */
/*
    private void restoreCameraSettings()
    {
        // Restores my settings.
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(parent.getContext());
        if (camera.isConnected())
        {
            Map<String, String> values = new HashMap<String, String>();
            Set<String> names = camera.getCameraPropertyNames();
            for (String name : names)
            {
                String value = preferences.getString(name, null);
                if (value != null)
                {
                    if (!CameraPropertyListenerImpl.canSetCameraProperty(name))
                    {
                        // Read Onlyのプロパティを除外して登録
                        values.put(name, value);
                        //Log.v(TAG, "restoreCameraSettings(): " + value);
                    }
                }
            }
            if (values.size() > 0)
            {
                try
                {
                    camera.setCameraPropertyValues(values);
                }
                catch (OLYCameraKitException e)
                {
                    Log.w(TAG, "To change the camera properties is failed: " + e.getMessage());
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        }
    }
*/

    /**
     *　 プロパティの一覧を取得する
     *
     */
    public List<String> getPropertyList(String propertyName)
    {
        try
        {
            return (camera.getCameraPropertyValueList(propertyName));
        }
        catch (OLYCameraKitException e)
        {
            e.printStackTrace();
            return (null);
        }
    }

    /**
     *   プロパティを設定する
     *
     */
    private void setPropertyValue(CameraPropertyHolder holder, int which)
    {
        try
        {
            holder.getTargetView().setSelected(false);
            String value = holder.getValueList().get(which);
            if (value != null)
            {
                //Log.v(TAG, "SET VALUE:" + holder.getPropertyName() + " (" + value + ")");
                camera.setCameraPropertyValue(holder.getPropertyName(), value);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
     *   UIスレッドでタスクを実行する
     *
     */
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
