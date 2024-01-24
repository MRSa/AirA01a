package jp.osdn.gokigen.aira01a.myprops;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import java.text.DateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import jp.co.olympus.camerakit.OLYCamera;
import jp.co.olympus.camerakit.OLYCameraKitException;
import jp.osdn.gokigen.aira01a.olycamera.CameraPropertyListenerImpl;

/**
 *   カメラプロパティを一括でバックアップしたり、リストアしたりするクラス
 *
 */
public class CameraPropertyBackupRestore
{
    private final String TAG = toString();

    static final int MAX_STORE_PROPERTIES = 128;   // お気に入り設定の最大記憶数...
    static final String TITLE_KEY = "CameraPropTitleKey";
    static final String DATE_KEY = "CameraPropDateTime";

    private final Context parent;
    private final OLYCamera camera;

    public CameraPropertyBackupRestore(Context context, OLYCamera camera)
    {
        this.camera = camera;
        this.parent = context;
    }

    /**
     *   カメラの現在の設定を本体から読みだして記憶する
     *
     */
    public void storeCameraSettings(String idHeader)
    {
        // カメラから設定を一括で読みだして、Preferenceに記録する
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
            //Log.v(TAG, "CameraPropertyBackupRestore::storeCameraSettings() : " + idHeader);

            if (values != null)
            {
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(parent);
                SharedPreferences.Editor editor = preferences.edit();
                for (String key : values.keySet())
                {
                    editor.putString(idHeader + key, values.get(key));
                    //Log.v(TAG, "storeCameraSettings(): " + idHeader + key + " , " + values.get(key));
                }
                DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM);
                editor.putString(idHeader + DATE_KEY, dateFormat.format(new Date()));
                //editor.commit();
                editor.apply();

                Log.v(TAG, "storeCameraSettings() COMMITED : " + idHeader);
            }
        }
    }

    /**
     *   Preferenceにあるカメラの設定をカメラに登録する
     *　(注： Read Onlyなパラメータを登録しようとするとエラーになるので注意）
     */
    public void restoreCameraSettings(String idHeader)
    {
        //restoreCameraSettingsMulti(idHeader);
        restoreCameraSettingsOnlyDifference(idHeader);
    }

    /*
     *   プロパティを一括設定する
     *
     */
/*
    private void restoreCameraSettingsMulti(String idHeader)
    {
        //Log.v(TAG, "restoreCameraSettings() : START [" + idHeader + "]");

        // Restores my settings.
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(parent);
        if (camera.isConnected())
        {
            Map<String, String> values = new HashMap<>();
            Set<String> names = camera.getCameraPropertyNames();
            for (String name : names)
            {
                String value = preferences.getString(idHeader + name, null);
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
            //Log.v(TAG, "restoreCameraSettings() : END [" + idHeader + "]" + " " + values.size());
        }
    }
*/

    /**
     *   カメラプロパティを差分だけ設定する
     *
     */
    private void restoreCameraSettingsOnlyDifference(String idHeader)
    {
        Log.v(TAG, "restoreCameraSettings() : START [" + idHeader + "]");

        int setCount = 0;

        // Restores my settings.
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(parent);
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

    public void setCameraSettingDataName(String idHeader, String dataName)
    {
        try
        {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(parent);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString(idHeader + TITLE_KEY, dataName);
            editor.apply();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public interface IPropertiesOperation
    {
        void loadProperties(final Activity activity, final String id, final String name);
        void saveProperties(final Activity activity, final String id, final String name);
    }
}
