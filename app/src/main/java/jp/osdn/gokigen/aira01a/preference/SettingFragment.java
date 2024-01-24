package jp.osdn.gokigen.aira01a.preference;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Bundle;

import android.util.Log;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import jp.co.olympus.camerakit.OLYCamera;
import jp.co.olympus.camerakit.OLYCameraKitException;
import jp.osdn.gokigen.aira01a.MainActivity;
import jp.osdn.gokigen.aira01a.R;
import jp.osdn.gokigen.aira01a.connection.ble.OlyCameraPowerOnSelector;

/**
 *   SettingFragment
 *   (ほぼOLYMPUS imagecapturesampleのサンプルコードそのまま)
 *
 */
public class SettingFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener
{
    private final String TAG = this.toString();

    private CameraPowerOff powerOffController = null;
    private SharedPreferences preferences = null;
    private OlyCameraPowerOnSelector powerOnSelector = null;
    private OLYCamera camera = null;

    public static SettingFragment newInstance(FragmentActivity context, OLYCamera camera)
    {
        SettingFragment instance = new SettingFragment();
        instance.prepare(context, camera);
        return (instance);
    }

    private void prepare(FragmentActivity context, OLYCamera camera)
    {
        this.camera = camera;
        powerOnSelector = new OlyCameraPowerOnSelector(context);
        powerOnSelector.prepare();
    }

    private boolean isBluetoothPowerOn()
    {
        return  (preferences != null)&&(preferences.getBoolean("ble_power_on", false));
    }

    private boolean isShowPreviewEnabled()
    {
        return  (preferences != null)&&(preferences.getBoolean("show_preview", true));
    }

    private boolean isTakeRaw()
    {
        return  (preferences != null)&&(preferences.getBoolean("raw", true));
    }

    private boolean isWbKeepWarmColors()
    {
        return  (preferences != null)&&(preferences.getBoolean("auto_wb_denkyu_colored_leaving", false));
    }

    private boolean isFullTimeAF()
    {
        return  (preferences != null)&&(preferences.getBoolean("full_time_af", false));
    }

    private String getCompressibilityRatio()
    {
        if (preferences != null)
        {
            return (preferences.getString("compressibility_ratio", "CMP_2_7"));
        }
        return ("CMP_2_7");

    }

    private String getContinuousShootingVelocity()
    {
        if (preferences != null)
        {
            return (preferences.getString("shooting_velocity", "10"));
        }
        return ("10");
    }

    private String getSoundVolume()
    {
        if (preferences != null)
        {
            return (preferences.getString("sound_volume_level", "OFF"));
        }
        return ("OFF");
    }

    private String getFaceScan()
    {
        if (preferences != null)
        {
            return preferences.getString("face_scan", "FACE_SCAN_OFF");
        }
        return ("FACE_SCAN_OFF");
    }

    private String getLiveViewQuality()
    {
        if (preferences != null)
        {
            return preferences.getString("live_view_quality", "QVGA");
        }
        return "QVGA";
    }

    private String getImageSize()
    {
        if (preferences != null)
        {
            return preferences.getString("image_size", "4608x3456");
        }
        return "4608x3456";
    }

    private String getArtFilterMode()
    {
        if (preferences != null)
        {
            return preferences.getString("recently_art_filter", "POPART");
        }
        return "POPART";
    }

    private String getColorTone()
    {
        if (preferences != null)
        {
            return (preferences.getString("color_tone", "I_FINISH"));
        }
        return "I_FINISH";
    }

    private String getColorCreatorColor()
    {
        if (preferences != null)
        {
            return (preferences.getString("color_creator_color", "0"));
        }
        return "0";
    }

    private String getColorCreatorVivid()
    {
        if (preferences != null)
        {
            return (preferences.getString("color_creator_vivid", "0"));
        }
        return "0";
    }

    private String getMonotoneFilter()
    {
        if (preferences != null)
        {
            return (preferences.getString("monotonefilter_monochrome", "NORMAL"));
        }
        return "NORMAL";
    }

    private String getMonotoneColor()
    {
        if (preferences != null)
        {
            return (preferences.getString("monotonecolor_monochrome", "NORMAL"));
        }
        return "NORMAL";
    }

    private String getCustomWBTemp()
    {
        if (preferences != null)
        {
            return (preferences.getString("custom_wb_temp", "5400"));
        }
        return "5400";
    }
    private String getColorPhase()
    {
        if (preferences != null)
        {
            return (preferences.getString("color_phase", "0"));
        }
        return "0";
    }

    private void updatePreferenceIconFromCamera(String propertyKey, String preferenceKey, int arrayValueId, int arrayIconId)
    {
        try
        {
            String data[] = OLYCamera.decodeCameraPropertyValue(camera.getCameraPropertyValue(propertyKey));
            Resources res = getResources();
            int index = Arrays.asList(res.getStringArray(arrayValueId)).indexOf(data[1]);
            TypedArray images = getResources().obtainTypedArray(arrayIconId);
            ListPreference pref =  (ListPreference) findPreference(preferenceKey);
            pref.setIcon(images.getDrawable(index));
            pref.setSummary(" " + index);
            images.recycle();
            Log.v(TAG, "key : " + propertyKey + " (" + preferenceKey + ") , index : "+ index + " data: " + data[1]);
        }
        catch (Exception e)
        {
            Log.v(TAG, "ERR>> key : " + propertyKey + " (" + preferenceKey + ") set icon Failure.");
            e.printStackTrace();
        }
    }

    private void updatePreferenceIcon(String preferenceKey, int arrayIconId)
    {
        try
        {
            int index = 0;
            if (preferences != null)
            {
                index = Integer.parseInt(preferences.getString(preferenceKey, "0"));
            }
            TypedArray images = getResources().obtainTypedArray(arrayIconId);
            ListPreference pref =  (ListPreference) findPreference(preferenceKey);
            pref.setIcon(images.getDrawable(index));
            pref.setSummary(" " + index);
            images.recycle();
            Log.v(TAG, "key : " +  preferenceKey + ", index : "+ index);
        }
        catch (Exception e)
        {
            Log.v(TAG, "ERR>> key : " + preferenceKey + " set icon Failure.");
            e.printStackTrace();
        }
    }

    @Override
    public void onAttach(@NonNull Context activity)
    {
        super.onAttach(activity);

        try
        {
            powerOffController = new CameraPowerOff((MainActivity) getActivity());
            powerOffController.prepare();

            preferences = PreferenceManager.getDefaultSharedPreferences(activity);
            Map<String, ?> items = preferences.getAll();
            SharedPreferences.Editor editor = preferences.edit();
            if (!items.containsKey("touch_shutter")) {
                editor.putBoolean("touch_shutter", true);
            }
            if (!items.containsKey("show_preview")) {
                editor.putBoolean("show_preview", true);
            }
            if (!items.containsKey("raw")) {
                editor.putBoolean("raw", true);
            }
            if (!items.containsKey("sound_volume_level")) {
                editor.putString("sound_volume_level", "OFF");
            }
            if (!items.containsKey("face_scan")) {
                editor.putString("face_scan", "FACE_SCAN_OFF");
            }
            if (!items.containsKey("recently_art_filter")) {
                editor.putString("recently_art_filter", "POPART");
            }
            if (!items.containsKey("image_size")) {
                editor.putString("image_size", "4608x3456");
            }
            if (!items.containsKey("shooting_velocity")) {
                editor.putString("shooting_velocity", "10");
            }
            if (!items.containsKey("compressibility_ratio")) {
                editor.putString("compressibility_ratio", "CMP_2_7");
            }
            if (!items.containsKey("auto_wb_denkyu_colored_leaving")) {
                editor.putBoolean("auto_wb_denkyu_colored_leaving", false);
            }
            if (!items.containsKey("full_time_af")) {
                editor.putBoolean("full_time_af", false);
            }
            if (!items.containsKey("bracket_pict_popart")) {
                editor.putBoolean("bracket_pict_popart", false);
            }
            if (!items.containsKey("bracket_pict_fantasic_focus")) {
                editor.putBoolean("bracket_pict_fantasic_focus", false);
            }
            if (!items.containsKey("bracket_pict_daydream")) {
                editor.putBoolean("bracket_pict_daydream", false);
            }
            if (!items.containsKey("bracket_pict_light_tone")) {
                editor.putBoolean("bracket_pict_light_tone", false);
            }
            if (!items.containsKey("bracket_pict_rough_monochrome")) {
                editor.putBoolean("bracket_pict_rough_monochrome", false);
            }
            if (!items.containsKey("bracket_pict_toy_photo")) {
                editor.putBoolean("bracket_pict_toy_photo", false);
            }
            if (!items.containsKey("bracket_pict_miniature")) {
                editor.putBoolean("bracket_pict_miniature", false);
            }
            if (!items.containsKey("bracket_pict_cross_process")) {
                editor.putBoolean("bracket_pict_cross_process", false);
            }
            if (!items.containsKey("bracket_pict_gentle_sepia")) {
                editor.putBoolean("bracket_pict_gentle_sepia", false);
            }
            if (!items.containsKey("bracket_pict_dramatic_tone")) {
                editor.putBoolean("bracket_pict_dramatic_tone", false);
            }
            if (!items.containsKey("bracket_pict_ligne_clair")) {
                editor.putBoolean("bracket_pict_ligne_clair", false);
            }
            if (!items.containsKey("bracket_pict_pastel")) {
                editor.putBoolean("bracket_pict_pastel", false);
            }
            if (!items.containsKey("bracket_pict_vintage")) {
                editor.putBoolean("bracket_pict_vintage", false);
            }
            if (!items.containsKey("bracket_pict_partcolor")) {
                editor.putBoolean("bracket_pict_partcolor", false);
            }

            if(!items.containsKey("color_tone")) {
                editor.putString("color_tone", "I_FINISH");
            }

            if(!items.containsKey("color_creator_color")) {
                editor.putString("color_creator_color", "0");
            }

            if(!items.containsKey("color_creator_vivid")) {
                editor.putString("color_creator_vivid", "0");
            }

            if(!items.containsKey("monotonefilter_monochrome")) {
                editor.putString("monotonefilter_monochrome", "NORMAL");
            }

            if(!items.containsKey("monotonecolor_monochrome")) {
                editor.putString("monotonecolor_monochrome", "NORMAL");
            }

            if(!items.containsKey("auto_bracketing")) {
                editor.putString("auto_bracketing", "0");
            }

            if(!items.containsKey("shooting_count")) {
                editor.putString("shooting_count", "3");
            }

            if(!items.containsKey("custom_wb_temp")) {
                editor.putString("custom_wb_temp", "5400");
            }

            if(!items.containsKey("color_phase")) {
                editor.putString("color_phase", "0");
            }

            if (!items.containsKey("frame_grid")) {
                editor.putString("frame_grid", "0");
            }

            if (!items.containsKey("level_gauge")) {
                editor.putBoolean("level_gauge", false);
            }

            if (!items.containsKey("share_after_receive")) {
                editor.putBoolean("share_after_receive", false);
            }

            if (!items.containsKey("capture_live_view")) {
                editor.putBoolean("capture_live_view", false);
            }

            if (!items.containsKey("ble_power_on")) {
                editor.putBoolean("ble_power_on", false);
            }

            if (!items.containsKey("live_view_scale")) {
                editor.putString("live_view_scale", "STEP");
            }

            if (!items.containsKey("self_timer_seconds")) {
                editor.putString("self_timer_seconds", "3");
            }

            //editor.commit();
            editor.apply();

            preferences.registerOnSharedPreferenceChangeListener(this);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey)
    {
        //super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        try
        {
            {
                final HashMap<String, String> sizeTable = new HashMap<>();
                sizeTable.put("QVGA", "(320x240)");
                sizeTable.put("VGA", "(640x480)");
                sizeTable.put("SVGA", "(800x600)");
                sizeTable.put("XGA", "(1024x768)");
                sizeTable.put("QUAD_VGA", "(1280x960)");

                ListPreference liveViewQuality = (ListPreference)findPreference("live_view_quality");

                liveViewQuality.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue)
                    {
                        try
                        {
                            preference.setSummary(newValue + " " + sizeTable.get(newValue));
                        }
                        catch (Exception e)
                        {
                            e.printStackTrace();
                        }
                        return true;
                    }
                });
                liveViewQuality.setSummary(liveViewQuality.getValue() + " " + sizeTable.get(liveViewQuality.getValue()));
            }
            //findPreference("power_off").setOnPreferenceClickListener(powerOffController);
            findPreference("exit_application").setOnPreferenceClickListener(powerOffController);
            findPreference("olympus_air_bt").setOnPreferenceClickListener(powerOnSelector);

            updatePreferenceIconFromCamera("COLOR_CREATOR_COLOR", "color_creator_color", R.array.prop_color_creator_color_value, R.array.prop_color_creator_color_icon);
            updatePreferenceIconFromCamera("COLOR_PHASE", "color_phase", R.array.prop_part_color_phase_value, R.array.prop_part_color_phase_icon);

            updatePreferenceIcon("frame_grid", R.array.frame_grid_type_icon);

            //updatePreferenceIcon("color_creator_color", R.array.prop_color_creator_color_icon);
            //updatePreferenceIcon("color_phase", R.array.prop_part_color_phase_icon);

        /*
         String key = "";
         try
         {
         // 音量の現在設定を表示
         key = "SOUND_VOLUME_LEVEL";
         String data[] = camera.decodeCameraPropertyValue(camera.getCameraPropertyValue(key));
         findPreference("sound_volume_level").setSummary(data[1]);

         // 静止画サイズの現在設定を表示
         key = "IMAGESIZE";
         findPreference("image_size").setSummary(camera.getCameraPropertyValueTitle(camera.getCameraPropertyValue(key)));

         // 顔検出の現在設定を表示
         key = "FACE_SCAN";
         findPreference("face_scan").setSummary(camera.getCameraPropertyValueTitle(camera.getCameraPropertyValue(key)));

         // アートフィルターの現在設定を表示
         key = "RECENTLY_ART_FILTER";
         findPreference("recently_art_filter").setSummary(camera.getCameraPropertyValueTitle(camera.getCameraPropertyValue(key)));

         }
         catch (Exception e)
         {
         // 何もしない
         String message = "ERROR to get camera property : " + key;
         Log.v(TAG, message);
         e.printStackTrace();
         }
         ****/

            // カメラキットのバージョン
            findPreference("camerakit_version").setSummary(OLYCamera.getVersion());

            // レンズ状態
            findPreference("lens_status").setSummary(camera.getLensMountStatus());

            // メディア状態
            findPreference("media_status").setSummary(camera.getMediaMountStatus());

            // 焦点距離
            String focalLength;
            float minLength = camera.getMinimumFocalLength();
            float maxLength = camera.getMaximumFocalLength();
            float actualLength = camera.getActualFocalLength();
            if (minLength == maxLength)
            {
                focalLength = String.format(Locale.ENGLISH, "%3.0fmm", actualLength);
            }
            else
            {
                focalLength = String.format(Locale.ENGLISH, "%3.0fmm - %3.0fmm (%3.0fmm)", minLength, maxLength, actualLength);
            }
            findPreference("focal_length").setSummary(focalLength);

            // カメラのバージョン
            try
            {
                Map<String, Object> hardwareInformation = camera.inquireHardwareInformation();
                findPreference("camera_version").setSummary((String)hardwareInformation.get(OLYCamera.HARDWARE_INFORMATION_CAMERA_FIRMWARE_VERSION_KEY));
            } catch (OLYCameraKitException e) {
                findPreference("camera_version").setSummary("Unknown");
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private void setProperty(String name, String value, String message)
    {
        try
        {
            camera.setCameraPropertyValue(name, value);
        }
        catch (OLYCameraKitException e)
        {
            Log.w(TAG, message);
        }
    }

    /**
     * ListPreference の表示データを設定
     *
     * @param pref_key     Preference(表示)のキー
     * @param key          Preference(データ)のキー
     * @param defaultValue Preferenceのデフォルト値
     */
    private void setListPreference(String pref_key, String key, String defaultValue)
    {
        ListPreference pref;
        pref = (ListPreference) findPreference(pref_key);
        String value = preferences.getString(key, defaultValue);
        if (pref != null)
        {
            pref.setValue(value);
            pref.setSummary(value);
        }
    }

    @Override
    public void onPause()
    {
        super.onPause();

        if (camera.isConnected())
        {
            Thread thread = new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    try
                    {
                        // カメラプロパティの設定
                        setProperties();
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }
            });
            thread.start();
        }
        // Preference変更のリスナを解除
        preferences.unregisterOnSharedPreferenceChangeListener(this);
    }


    /**
     *   カメラプロパティの設定
     *
     */
    private void setProperties()
    {
        String message = "";
        String value;

        // Apply settings
        try {
            message = "To change the live view size is failed.";
            camera.changeLiveViewSize(toLiveViewSize(getLiveViewQuality()));
        }
        catch (Exception e)
        {
            Log.w(TAG, message);
        }

        // /** TODO: 多くなってきたので、今後、一括登録を考える **/
        message = "To change sound volume is failed.";
        value = "<SOUND_VOLUME_LEVEL/" + getSoundVolume() + ">";
        setProperty("SOUND_VOLUME_LEVEL", value, message);

        message = "To change the rec-view is failed.";
        value = isShowPreviewEnabled() ? "<RECVIEW/ON>" : "<RECVIEW/OFF>";
        setProperty("RECVIEW", value, message);

        message = "To change RAW is failed.";
        value = isTakeRaw() ?  "<RAW/ON>" : "<RAW/OFF>";
        setProperty("RAW", value, message);

        message = "To change image size is failed.";
        value = "<IMAGESIZE/" + getImageSize() + ">";
        setProperty("IMAGESIZE", value, message);

        message = "To change keeping warm colors is failed.";
        value = isWbKeepWarmColors() ? "<AUTO_WB_DENKYU_COLORED_LEAVING/ON>" : "<AUTO_WB_DENKYU_COLORED_LEAVING/OFF>";
        setProperty("AUTO_WB_DENKYU_COLORED_LEAVING", value, message);

        message = "To change shooting velocity is failed.";
        value = "<CONTINUOUS_SHOOTING_VELOCITY/" + getContinuousShootingVelocity() + ">";
        setProperty("CONTINUOUS_SHOOTING_VELOCITY", value, message);

        message = "To change compressibility ratio is failed.";
        value = "<COMPRESSIBILITY_RATIO/" + getCompressibilityRatio() + ">";
        setProperty("COMPRESSIBILITY_RATIO", value, message);

        message = "To change FULL TIME AF is failed.";
        value = isFullTimeAF() ?  "<FULL_TIME_AF/ON>" : "<FULL_TIME_AF/OFF>";
        setProperty("FULL_TIME_AF", value, message);

        message = "To change face scan mode is failed.";
        value = "<FACE_SCAN/" + getFaceScan() + ">";
        setProperty("FACE_SCAN", value, message);

        message = "To change art filter is failed.";
        value = "<RECENTLY_ART_FILTER/" + getArtFilterMode() + ">";
        setProperty("RECENTLY_ART_FILTER", value, message);

        message = "To change color tone is failed.";
        value = "<COLORTONE/" + getColorTone() + ">";
        setProperty("COLORTONE", value, message);

        message = "To change color creator color is failed.";
        value = "<COLOR_CREATOR_COLOR/" + getColorCreatorColor() + ">";
        setProperty("COLOR_CREATOR_COLOR", value, message);

        message = "To change color creator vivid is failed.";
        value = "<COLOR_CREATOR_VIVID/" + getColorCreatorVivid() + ">";
        setProperty("COLOR_CREATOR_VIVID", value, message);

        message = "To change monotone filter is failed.";
        value = "<MONOTONEFILTER_MONOCHROME/" + getMonotoneFilter() + ">";
        setProperty("MONOTONEFILTER_MONOCHROME", value, message);

        message = "To change monotone color is failed.";
        value = "<MONOTONECOLOR_MONOCHROME/" + getMonotoneColor() + ">";
        setProperty("MONOTONECOLOR_MONOCHROME", value, message);

        message = "To change CustomWB Color Temp. is failed.";
        value = "<CUSTOM_WB_KELVIN_1/" + getCustomWBTemp() + ">";
        setProperty("CUSTOM_WB_KELVIN_1", value, message);

        message = "To change Color Phase is failed.";
        value = "<COLOR_PHASE/" + getColorPhase() + ">";
        setProperty("COLOR_PHASE", value, message);
    }

    /**
     *   Preferenceが更新された時に呼び出される処理
     *
     * @param sharedPreferences  sharedPreferences
     * @param key  変更されたキー
     */
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
    {
        Log.v(TAG, "Preference Changed : " + key);
        if (key.contains("color_creator_color"))
        {
            updatePreferenceIcon("color_creator_color", R.array.prop_color_creator_color_icon);
        }
        else if (key.contains("color_phase"))
        {
            updatePreferenceIcon("color_phase", R.array.prop_part_color_phase_icon);
        }
        else if (key.contains("frame_grid"))
        {
            updatePreferenceIcon("frame_grid", R.array.frame_grid_type_icon);
        }
    }

    /**
     *   toLiveViewSize() : スクリーンサイズの文字列から、OLYCamera.LiveViewSize型へ変換する
     *
     * @param quality スクリーンサイズ文字列
     * @return OLYCamera.LiveViewSize型
     */
    private static OLYCamera.LiveViewSize toLiveViewSize(String quality)
    {
        if (quality.equalsIgnoreCase("QVGA"))
        {
            return OLYCamera.LiveViewSize.QVGA;
        }
        else if (quality.equalsIgnoreCase("VGA"))
        {
            return OLYCamera.LiveViewSize.VGA;
        } else if (quality.equalsIgnoreCase("SVGA"))
        {
            return OLYCamera.LiveViewSize.SVGA;
        } else if (quality.equalsIgnoreCase("XGA"))
        {
            return OLYCamera.LiveViewSize.XGA;
        } else if (quality.equalsIgnoreCase("QUAD_VGA"))
        {
            return OLYCamera.LiveViewSize.QUAD_VGA;
        }
        return OLYCamera.LiveViewSize.QVGA;
    }
}
