package jp.osdn.gokigen.aira01a.liveview;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.graphics.RectF;
import android.preference.PreferenceManager;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.PointF;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;
import jp.co.olympus.camerakit.OLYCamera;
import jp.osdn.gokigen.aira01a.myprops.CameraPropertyBackupRestore;
import jp.osdn.gokigen.aira01a.olycamera.CameraPropertyListenerImpl;
import jp.osdn.gokigen.aira01a.olycamera.CameraRecordingListenerImpl;
import jp.osdn.gokigen.aira01a.olycamera.CameraRecordingSupportsListenerImpl;
import jp.osdn.gokigen.aira01a.olycamera.CameraStatusListenerImpl;
import jp.osdn.gokigen.aira01a.R;
import jp.osdn.gokigen.aira01a.myprops.LoadSaveMyCameraPropertyDialog;
import jp.osdn.gokigen.aira01a.playback.ImageGridViewFragment;
import jp.osdn.gokigen.aira01a.preference.SettingFragment;
import jp.osdn.gokigen.aira01a.preview.CapturedDataViewFragment;
import jp.osdn.gokigen.aira01a.takepicture.BracketingShotControl;
import jp.osdn.gokigen.aira01a.takepicture.CameraController;
import jp.osdn.gokigen.aira01a.takepicture.TakePictureControl;


/**
 *  撮影用ライブビュー画面
 *
 * Created by MRSa on 2016/04/29.
 */
public class LiveViewFragment extends Fragment implements IStatusViewDrawer, CameraPropertyBackupRestore.IPropertiesOperation, ISelfTimerControl
{
    private final String TAG = this.toString();
    private OLYCamera camera = null;
    //private ProgressDialog busyDialog = null;

    private static final int COMMAND_MY_PROPERTY = 0x00000100;

    private LiveViewMagnifingControl liveViewMagnifingControl = null;
    private CameraLiveViewListenerImpl liveViewListener = null;
    private CameraPropertyListenerImpl propertyListener = null;
    private CameraRecordingListenerImpl recordingListener = null;
    private CameraRecordingSupportsListenerImpl recordingSupportsListener = null;
    private CameraStatusListenerImpl statusListener = null;
    private CameraViewOnTouchClickListenerImpl viewOnTouchClickListener = null;
    private CameraPropertyBackupRestore backupRestoreProperties = null;

    private CameraController cameraController = null;
    private TakePictureControl takePictureControl = null;

    private ImageView unlockImageView = null;
    private CameraLiveImageView imageView = null;
    private ImageView batteryLevelImageView = null;
    private TextView remainingRecordableImagesTextView = null;
    private ImageView driveModeImageView = null;
    private TextView takeModeTextView = null;
    private TextView shutterSpeedTextView = null;
    private TextView apertureValueTextView = null;
    private TextView exposureCompensationTextView = null;
    private TextView isoSensitivityTextView = null;
    private TextView focalLengthTextView = null;
    private TextView aeModeTextView = null;
    private TextView informationTextView = null;
    private TextView warningTextView = null;
    private TextView otherTextView = null;
    private TextView hideControlPanelTextView = null;
    private TextView showControlPanelTextView = null;
    private ImageView whiteBalanceImageView = null;
    private ImageView settingImageView = null;
    private ImageView shutterImageView = null;
    private ImageView playbackImageView = null;
    private ImageView zoomOutImageView = null;
    private ImageView zoomInImageView = null;
    private ImageView aeLockImageView = null;
    private ImageView manualFocusImageView = null;
    private ImageView saveLoadImageView = null;
    private ImageView magnifyImageView = null;
    private ImageView selfTimerImageView = null;

    private CameraPropertyHolder exposureCompensationHolder = null;
    private CameraPropertyHolder driveModeHolder = null;
    private CameraPropertyHolder takeModeHolder = null;
    private CameraPropertyHolder whiteBalanceHolder = null;
    private CameraPropertyHolder apertureHolder = null;
    private CameraPropertyHolder shutterSpeedHolder = null;
    private CameraPropertyHolder isoSensitivityHolder = null;
    private CameraPropertyHolder aeModeHolder = null;

    private boolean selfTimerFeature = false;

    // TODO: きれいにしたい
    @SuppressWarnings("serial")
    private static final Map<String, Integer> driveModeList = new HashMap<String, Integer>() {
        {
            put("<TAKE_DRIVE/DRIVE_NORMAL>"  , R.drawable.icn_drive_setting_single);
            put("<TAKE_DRIVE/DRIVE_CONTINUE>", R.drawable.icn_drive_setting_seq_l);
        }
    };

    // TODO: きれいにしたい
    @SuppressWarnings("serial")
    private static final Map<String, Integer> whiteBalanceIconList = new HashMap<String, Integer>() {
        {
            put("<WB/WB_AUTO>"          , R.drawable.icn_wb_setting_wbauto);
            put("<WB/MWB_SHADE>"        , R.drawable.icn_wb_setting_16);
            put("<WB/MWB_CLOUD>"        , R.drawable.icn_wb_setting_17);
            put("<WB/MWB_FINE>"         , R.drawable.icn_wb_setting_18);
            put("<WB/MWB_LAMP>"         , R.drawable.icn_wb_setting_20);
            put("<WB/MWB_FLUORESCENCE1>", R.drawable.icn_wb_setting_35);
            put("<WB/MWB_WATER_1>"      , R.drawable.icn_wb_setting_64);
            put("<WB/WB_CUSTOM1>"       , R.drawable.icn_wb_setting_512);
        }
    };

    // TODO: きれいにしたい
    @SuppressWarnings("serial")
    private static final Map<String, Integer> batteryIconList = new HashMap<String, Integer>() {
        {
            put("<BATTERY_LEVEL/UNKNOWN>"       , R.drawable.tt_icn_battery_unknown);
            put("<BATTERY_LEVEL/CHARGE>"        , R.drawable.tt_icn_battery_charge);
            put("<BATTERY_LEVEL/EMPTY>"         , R.drawable.tt_icn_battery_empty);
            put("<BATTERY_LEVEL/WARNING>"       , R.drawable.tt_icn_battery_half);
            put("<BATTERY_LEVEL/LOW>"           , R.drawable.tt_icn_battery_middle);
            put("<BATTERY_LEVEL/FULL>"          , R.drawable.tt_icn_battery_full);
            put("<BATTERY_LEVEL/EMPTY_AC>"       , R.drawable.tt_icn_battery_supply_empty);
            put("<BATTERY_LEVEL/SUPPLY_WARNING>", R.drawable.tt_icn_battery_supply_half);
            put("<BATTERY_LEVEL/SUPPLY_LOW>"    , R.drawable.tt_icn_battery_supply_middle);
            put("<BATTERY_LEVEL/SUPPLY_FULL>"   , R.drawable.tt_icn_battery_supply_full);
        }
    };

    /**
     *
     *
     */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "onCreate()");

        if (cameraController == null)
        {
            cameraController = new CameraController(this);
        }
        if ((takePictureControl == null)&&(camera != null))
        {
            takePictureControl = new TakePictureControl(getContext(), camera, cameraController, this);
        }
        if (liveViewListener == null)
        {
            liveViewListener = new CameraLiveViewListenerImpl();
        }
        if (recordingListener == null)
        {
            recordingListener = new CameraRecordingListenerImpl(this);
        }
        if (propertyListener == null)
        {
            propertyListener = new CameraPropertyListenerImpl(this);
        }
        if (recordingSupportsListener == null)
        {
            recordingSupportsListener = new CameraRecordingSupportsListenerImpl(this);
        }
        if (statusListener == null)
        {
            statusListener = new CameraStatusListenerImpl(this);
        }
        if (backupRestoreProperties == null)
        {
            backupRestoreProperties = new CameraPropertyBackupRestore(getContext(), camera);
        }
        if (liveViewMagnifingControl == null)
        {
            liveViewMagnifingControl = new LiveViewMagnifingControl(getContext(), camera);
        }
    }

    /**
     *
     *
     */
    @Override
    public void onAttach(@NonNull Context context)
    {
        super.onAttach(context);
        Log.v(TAG, "onAttach()");
    }

    /**
     *
     *
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        Log.v(TAG, "onCreateView()");

        View view = inflater.inflate(R.layout.fragment_live_view, container, false);

        try
        {
            prepareViewObjectHolders(view);

            if (liveViewListener == null)
            {
                liveViewListener = new CameraLiveViewListenerImpl();
            }
            liveViewListener.setCameraLiveImageView(imageView);

            if (recordingListener == null)
            {
                recordingListener = new CameraRecordingListenerImpl(this);
            }
            recordingListener.setImageView(shutterImageView);

            preparePropertyHolders();

            if (viewOnTouchClickListener == null)
            {
                viewOnTouchClickListener = new CameraViewOnTouchClickListenerImpl(this, cameraController, takePictureControl, liveViewMagnifingControl);
            }

            if (takePictureControl != null)
            {
                takePictureControl.setSelfTimerDrawer(imageView);
            }

            // imageViewにActivityを教えてやる
            imageView.setActivity(getActivity());

            // Click または touch された時のリスナクラスを設定する
            imageView.setOnTouchListener(viewOnTouchClickListener);
            shutterImageView.setOnTouchListener(viewOnTouchClickListener);
            driveModeImageView.setOnClickListener(viewOnTouchClickListener);
            takeModeTextView.setOnClickListener(viewOnTouchClickListener);
            shutterSpeedTextView.setOnClickListener(viewOnTouchClickListener);
            apertureValueTextView.setOnClickListener(viewOnTouchClickListener);
            exposureCompensationTextView.setOnClickListener(viewOnTouchClickListener);
            isoSensitivityTextView.setOnClickListener(viewOnTouchClickListener);
            aeModeTextView.setOnClickListener(viewOnTouchClickListener);
            whiteBalanceImageView.setOnClickListener(viewOnTouchClickListener);
            settingImageView.setOnClickListener(viewOnTouchClickListener);
            unlockImageView.setOnClickListener(viewOnTouchClickListener);
            playbackImageView.setOnClickListener(viewOnTouchClickListener);
            zoomInImageView.setOnClickListener(viewOnTouchClickListener);
            zoomOutImageView.setOnClickListener(viewOnTouchClickListener);
            aeLockImageView.setOnClickListener(viewOnTouchClickListener);
            focalLengthTextView.setOnClickListener(viewOnTouchClickListener);
            manualFocusImageView.setOnClickListener(viewOnTouchClickListener);
            saveLoadImageView.setOnClickListener(viewOnTouchClickListener);
            showControlPanelTextView.setOnClickListener(viewOnTouchClickListener);
            hideControlPanelTextView.setOnClickListener(viewOnTouchClickListener);
            magnifyImageView.setOnClickListener(viewOnTouchClickListener);
            selfTimerImageView.setOnClickListener(viewOnTouchClickListener);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return (view);
    }

    /**
     *
     *
     *
     */
    @Override
    public void onStart()
    {
        super.onStart();
        Log.v(TAG, "onStart()");

    }

    /**
     *
     *
     */
    @Override
    public void onResume()
    {
        super.onResume();
        Log.v(TAG, "onResume() : Start");

/*
        // ダイアログを表示する(念のため...)
        busyDialog = new ProgressDialog(getActivity());
        busyDialog.setMessage(getString(R.string.dialog_start_wait_message));
        busyDialog.setTitle(getString(R.string.dialog_start_wait_title));
        busyDialog.setIndeterminate(false);
        busyDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        busyDialog.show();
*/
        try
        {
            updateSelfTimerIcon(selfTimerFeature);
            controlTouchShutter();
            if (camera != null)
            {
                camera.setLiveViewListener(liveViewListener);
                camera.setCameraPropertyListener(propertyListener);
                camera.setCameraStatusListener(statusListener);
                camera.setRecordingListener(recordingListener);
                camera.setRecordingSupportsListener(recordingSupportsListener);

                final ISelfTimerControl selfTimerControl = this;
                Thread thread = new Thread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        if (cameraController != null)
                        {
                            try
                            {
                                if (takePictureControl == null)
                                {
                                    takePictureControl = new TakePictureControl(getContext(), camera, cameraController, selfTimerControl);
                                }
                                cameraController.changeToRecordingMode();
                                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
                                camera.changeLiveViewSize(toLiveViewSize(preferences.getString("live_view_quality", "QVGA")));
                                if (!camera.isAutoStartLiveView())
                                {
                                    camera.startLiveView();
                                }
                            }
                            catch (Exception e)
                            {
                                e.printStackTrace();
                            }
                            System.gc();
                        }
                        //busyDialog.dismiss();
                        //busyDialog = null;
                    }
                });

                thread.start();
                // ズームレンズのボタンを表示するかどうかを決める
                updateZoomButtonVisibility();
            }
            updateLevelGauge();
            updateView(false);
            resetAutoFocus();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        Log.v(TAG, "onResume() End");
    }

    /**
     *
     *
     */
    @Override
    public void onPause()
    {
        super.onPause();
        Log.v(TAG, "onPause()");

        if (camera != null)
        {
            camera.setLiveViewListener(null);
            camera.setCameraPropertyListener(null);
            camera.setCameraStatusListener(null);
            camera.setRecordingListener(null);
            camera.setRecordingSupportsListener(null);
        }
    }

    /**
     * カメラクラスをセットする
     *
     * @param camera  OLYCameraクラス
     */
    public void setCamera(OLYCamera camera)
    {
        Log.v(TAG, "setInterface()");
        this.camera = camera;
        if (cameraController == null)
        {
            cameraController = new CameraController(this);
        }
        cameraController.setCamera(camera);
    }

    /**
     *
     *
     */
    private void updateView(final boolean isShowPanel)
    {
        // いったんパネルを閉じて動作させる
        if (isShowPanel)
        {
            showControlPanel();
        }
        else
        {
            hideControlPanel();
        }

        //  TODO: この処理だと手抜きなので、、、runOnUiThreadでは更新だけ、データの取得はその前に行うようにしたい
        Thread thread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                Activity activity = getActivity();
                if (activity == null)
                {
                    Log.v(TAG, "activity is null.");
                    return;
                }
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateDrivemodeImageView();

                        takeModeTextView.setText(takeModeHolder.getCameraPropertyValueTitle());

                        updateShutterSpeedTextView();
                        updateApertureValueTextView();
                        updateExposureCompensationTextView();
                        updateIsoSensitivityTextView();
                        updateFocalLengthView();
                        updateWhiteBalanceImageView();
                        updateBatteryLevelImageView();
                        updateRemainingRecordableImagesTextView();
                        updateAEModeTextView();
                        updateAELockStateImageView();
                        updateInformationTextView();
                        updateWarningTextView();
                        updateOtherTextView();
                        updateManualFocusStateImageView();

                        showControlPanel();
                    }
                });
            }
        });
        thread.start();
    }

    /**
     *
     *
     */
    private void resetAutoFocus()
    {
        takePictureControl.resetAutoFocus();
    }

    boolean isFocusPointArea(PointF point)
    {
        // If the focus point is out of area, ignore the touch.
        return (imageView.isContainsPoint(point));
    }

    PointF getPointWithEvent(MotionEvent event)
    {
        return (imageView.getPointWithEvent(event));
    }

    /**
     *
     *
     */
    private void controlTouchShutter()
    {
        boolean touchShutterStatus = cameraController.getTouchShutterStatus();
        unlockImageView.setVisibility(touchShutterStatus ? View.INVISIBLE : View.VISIBLE);
    }

    /**
     *
     *
     */
    public void updateDrivemodeImageView()
    {
        try
        {
            driveModeImageView.setEnabled(driveModeHolder.canSetCameraProperty());
            String drivemode = driveModeHolder.getCameraPropertyValue();
            if (driveModeList.containsKey(drivemode))
            {
                int resId = driveModeList.get(drivemode);
                driveModeImageView.setImageResource(resId);
            } else {
                driveModeImageView.setImageDrawable(null);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public void updateTakemodeTextView()
    {
        try
        {
            takeModeTextView.setEnabled(takeModeHolder.canSetCameraProperty());
            takeModeTextView.setText(takeModeHolder.getCameraPropertyValueTitle());

            // Changing take mode may have an influence for drive mode and white balance.
            updateDrivemodeImageView();
            updateShutterSpeedTextView();
            updateApertureValueTextView();
            updateExposureCompensationTextView();
            updateIsoSensitivityTextView();
            updateWhiteBalanceImageView();
            updateFocalLengthView();
            updateAELockStateImageView();
            updateManualFocusStateImageView();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public void updateShutterSpeedTextView()
    {
        shutterSpeedTextView.setEnabled(shutterSpeedHolder.canSetCameraProperty());
        //shutterSpeedTextView.setText(shutterSpeedHolder.getCameraPropertyValueTitle());
        shutterSpeedTextView.setText(cameraController.getActualShutterSpeed());
    }

    public void updateApertureValueTextView()
    {
        apertureValueTextView.setEnabled(apertureHolder.canSetCameraProperty());
        //String title = apertureHolder.getCameraPropertyValueTitle();
        String title = cameraController.getActualApertureValue();
        if (title == null)
        {
            title = "";
        }
        else
        {
            title = String.format("F%s", title);
        }
        apertureValueTextView.setText(title);
    }

    public void updateExposureCompensationTextView()
    {
        exposureCompensationTextView.setEnabled(exposureCompensationHolder.canSetCameraProperty());
        //exposureCompensationTextView.setText(exposureCompensationHolder.getCameraPropertyValueTitle());
        exposureCompensationTextView.setText(cameraController.getActualExposureCompensation());
    }

    void hideControlPanel()
    {
        FragmentActivity activity = getActivity();
        try
        {
            if (activity != null)
            {
                activity.findViewById(R.id.controlPanelLayout).setVisibility(View.INVISIBLE);
                activity.findViewById(R.id.showControlPanelTextView).setVisibility(View.VISIBLE);
            }
            else
            {
                Log.v(TAG, "getActivity() returns NULL.");
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    void showControlPanel()
    {
        FragmentActivity activity = getActivity();
        try
        {
            if (activity != null)
            {
                activity.findViewById(R.id.controlPanelLayout).setVisibility(View.VISIBLE);
                activity.findViewById(R.id.showControlPanelTextView).setVisibility(View.INVISIBLE);
            }
            else
            {
                Log.v(TAG, "getActivity() returns NULL.");
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public void updateIsoSensitivityTextView()
    {
        isoSensitivityTextView.setEnabled(isoSensitivityHolder.canSetCameraProperty());
        String titlePrefix = "ISO";
        String title = isoSensitivityHolder.getCameraPropertyValueTitle();
        if ("Auto".equals(title))
        {
            titlePrefix = "ISO-A";
            title = cameraController.getActualIsoSensitivity();
        }
        isoSensitivityTextView.setText(String.format("%s\n%s" ,titlePrefix, title));
    }

    /**
     *   カメラプロパティの読み出し・保存ダイアログを表示する
     *
     */
    void showSaveLoadMyCameraPropertyDialog()
    {
        LoadSaveMyCameraPropertyDialog dialog = new LoadSaveMyCameraPropertyDialog();
        dialog.setTargetFragment(this, COMMAND_MY_PROPERTY);
        dialog.setPropertyOperationsHolder(this);
        FragmentActivity activity = getActivity();
        try
        {
            if (activity != null)
            {
                dialog.show(activity.getSupportFragmentManager(), "my_dialog");
            }
            else
            {
                Log.v(TAG, "FragmentActivity IS NULL.");
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
     *   測光モード
     *
     */
    public void updateAEModeTextView()
    {
        aeModeTextView.setEnabled(aeModeHolder.canSetCameraProperty());
        aeModeTextView.setText(aeModeHolder.getCameraPropertyValueTitle());
    }

    /**
     *
     */
    @Override
    public void updateFocalLengthView()
    {
        focalLengthTextView.setText(cameraController.getFocalLength());
        updateZoomButtonVisibility();
    }

    public void updateWhiteBalanceImageView()
    {
        try
        {
            whiteBalanceImageView.setEnabled(whiteBalanceHolder.canSetCameraProperty());
            String value = whiteBalanceHolder.getCameraPropertyValue();
            if (whiteBalanceIconList.containsKey(value)) {
                int resId = whiteBalanceIconList.get(value);
                whiteBalanceImageView.setImageResource(resId);
            } else {
                whiteBalanceImageView.setImageDrawable(null);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public void updateBatteryLevelImageView()
    {
        try
        {
            String value = cameraController.getCameraPropertyValue(CameraPropertyListenerImpl.CAMERA_PROPERTY_BATTERY_LEVEL);
            if (batteryIconList.containsKey(value))
            {
                int resId = batteryIconList.get(value);
                if (resId != 0) {
                    batteryLevelImageView.setImageResource(resId);
                } else {
                    batteryLevelImageView.setImageDrawable(null);
                }
            } else {
                batteryLevelImageView.setImageDrawable(null);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public void updateAELockStateImageView()
    {
        String value = cameraController.getCameraPropertyValue(CameraPropertyListenerImpl.CAMERA_PROPERTY_AE_LOCK_STATE);
        Log.v(TAG, "LiveViewFragment::updateAELockStateImageView() " + value);
        if (value.contains("UNLOCK"))
        {
            // UNLOCK AE
            cameraController.setAELockStatus(false);
            aeLockImageView.setSelected(false);
        }
        else
        {
            // LOCK AE
            cameraController.setAELockStatus(true);
            aeLockImageView.setSelected(true);
        }
    }

    public void updateManualFocusStateImageView()
    {
        String value = cameraController.getCameraPropertyValue(CameraPropertyListenerImpl.CAMERA_PROPERTY_FOCUS_STILL);
        Log.v(TAG, "LiveViewFragment::updateManualFocusStateImageView() " + value);
        if (value.contains("AF"))
        {
            // AUTO FOCUS MODE
            cameraController.setManualFocusStatus(false);
            manualFocusImageView.setSelected(false);
        }
        else
        {
            // MANUAL FOCUS MODE
            cameraController.setManualFocusStatus(true);
            manualFocusImageView.setSelected(true);
        }
    }

    private void updateInformationTextView()
    {
        informationTextView.setText(cameraController.getInformationText());
    }

    public void updateWarningTextView()
    {
        warningTextView.setText(cameraController.getWarningText());
    }

    public void updateOtherTextView()
    {
        otherTextView.setText(cameraController.getOtherText());
    }

    public void updateRemainingRecordableImagesTextView()
    {
        final String text;
        if (camera.isConnected() || camera.getRunMode() == OLYCamera.RunMode.Recording)
        {
            if (camera.isMediaBusy())
            {
                text = "BUSY";
            }
            else if (camera.isMediaError())
            {
                text = "ERROR";
            }
            else
            {
                text = String.format(Locale.getDefault(), "%d", camera.getRemainingImageCapacity());
            }
        }
        else
        {
            text = "???";
        }
        remainingRecordableImagesTextView.setText(text);
    }

    /**
     *   顔を検出した時の処理
     *
     */
    public void detectedHumanFaces()
    {
        // 顔検出！ （顔の座標をログ出力する）
        Map<String, RectF> faceMap = cameraController.getDetectedHumanFaces();
        Log.v(TAG, "detectedHumanFaces() : " + faceMap.size());
        for (Map.Entry<String, RectF> face : faceMap.entrySet())
        {
            //RectF rect = face.getValue();
            //String outMessage =  "FACE[" + face.getKey() + "] " + "(" + rect.left +"," + rect.top + ")-(" + rect.right + "," + rect.bottom + ") ";
            //Log.v(TAG, "-----");
            //Log.v(TAG, outMessage);
            //Log.v(TAG, "-----");

            // タッチシャッターが有効な場合には、顔に合わせてシャッターを切る（ただしシングルショット）
            if (cameraController.getTouchShutterStatus())
            {
                takePictureControl.takePictureWithPoint(new PointF(face.getValue().centerX(), face.getValue().centerY()));
                break;
            }

            // フォーカスロック中ではない場合は、最初に見つけた顔の位置にフォーカスを設定する
            if (!cameraController.getFocusFrameStatus())
            {
                takePictureControl.lockAutoFocus(new PointF(face.getValue().centerX(), face.getValue().centerY()));
            }
            else
            {
                //
                Log.v(TAG, "NOT FOCUS");
            }
            break;
        }
    }


    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------
    public void presentMessage(String title, String message)
    {
        Context context = getActivity();
        if (context == null)
        {
            return;
        }

        /// Toast で通知する
        /// Toast.makeText(context, title + "  " + message, Toast.LENGTH_LONG);

        /// AlertDialog で通知する
        /**/
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title).setMessage(message);
        builder.show();
        /**/
    }

    /**
     *
     *
     *
     */
    public void presentPropertyValueList(CameraPropertyHolder holder, DialogInterface.OnClickListener listener)
    {
        holder.prepare();
        holder.getTargetView().setSelected(true);
        List<String> list = holder.getValueList();
        String initialValue = holder.getCameraPropertyValue();

        FragmentActivity activity = getActivity();
        if (activity == null)
        {
            return;
        }

        String[] items = new String[list.size()];
        for (int ii = 0; ii < items.length; ++ii)
        {
            items[ii] = camera.getCameraPropertyValueTitle(list.get(ii));
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setSingleChoiceItems(items, list.indexOf(initialValue), listener);
        builder.setOnCancelListener(new DialogInterface.OnCancelListener()
        {
            @Override
            public void onCancel(DialogInterface dialog)
            {
                driveModeImageView.setSelected(false);
                takeModeTextView.setSelected(false);
                shutterSpeedTextView.setSelected(false);
                apertureValueTextView.setSelected(false);
                exposureCompensationTextView.setSelected(false);
                isoSensitivityTextView.setSelected(false);
                whiteBalanceImageView.setSelected(false);
            }
        });
        builder.show();
    }

    /**
     *
     *
     * @return CameraLiveImageView オブジェクト
     */
    public CameraLiveImageView getLiveImageView()
    {
        return (imageView);
    }

    /**
     *   設定項目を表示する
     *
     */
    void transToSettingFragment()
    {
        FragmentActivity activity = getActivity();
        try
        {
            if (activity != null)
            {
                SettingFragment fragment = SettingFragment.newInstance(activity, camera);
                FragmentTransaction transaction = activity.getSupportFragmentManager().beginTransaction();
                //transaction.setCustomAnimations(android.R.anim.fade_out, android.R.anim.fade_in);
                transaction.replace(getId(), fragment);
                transaction.addToBackStack(null);
                transaction.commit();
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
     *   撮影画像のプレビュー画面を表示する
     *
     * @param bytes バイトデータ
     * @param map　 マップ
     */
    public void transToPreviewFragment(byte[] bytes, Map<String, Object> map)
    {
        try
        {
            if (cameraController.getAutoBracketingSetting(false) != BracketingShotControl.BRACKET_NONE)
            {
                // ブラケッティング撮影中は、プレビュー表示を行わない
                return;
            }

            if (camera.getActionType() == OLYCamera.ActionType.Single)
            {
                CapturedDataViewFragment fragment = new CapturedDataViewFragment();
                fragment.prepareImageToShow(camera, bytes, map);
                FragmentActivity activity = getActivity();
                if (activity != null)
                {
                    FragmentTransaction transaction = activity.getSupportFragmentManager().beginTransaction();
                    transaction.replace(getId(), fragment);
                    transaction.addToBackStack(null);
                    transaction.commit();
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
     *   カメラ内の画像を表示する
     *
     */
    void transToPlaybackFragment()
    {
        ImageGridViewFragment fragment = new ImageGridViewFragment();
        fragment.setInterface(camera);
        FragmentActivity activity = getActivity();
        if (activity != null)
        {
            FragmentTransaction transaction = activity.getSupportFragmentManager().beginTransaction();
            //transaction.setCustomAnimations(android.R.anim.fade_out, android.R.anim.fade_in);
            transaction.replace(getId(), fragment); // transaction.replace(R.id.gragment1, fragment);
            transaction.addToBackStack(null);
            transaction.commit();
        }
    }

    public void setShutterImageSelected(boolean isSelected)
    {
        shutterImageView.setSelected(isSelected);
    }

    public CameraPropertyHolder getExposureCompensationHolder()
    {
        return (exposureCompensationHolder);
    }

    public CameraPropertyHolder getTakeModeHolder()
    {
        return (takeModeHolder);
    }
    public CameraPropertyHolder getDriveModeHolder()
    {
        return (driveModeHolder);
    }

    public CameraPropertyHolder getWhiteBalanceHolder()
    {
        return (whiteBalanceHolder);
    }

    public CameraPropertyHolder getApertureHolder()
    {
        return (apertureHolder);
    }

    public CameraPropertyHolder getShutterSpeedHolder()
    {
        return (shutterSpeedHolder);
    }

    public CameraPropertyHolder getIsoSensitivityHolder()
    {
        return (isoSensitivityHolder);
    }

    public CameraPropertyHolder getAEModeHolder()
    {
        return (aeModeHolder);
    }

    /**
     *
     */
    private void preparePropertyHolders()
    {
        if (cameraController == null)
        {
            cameraController = new CameraController(this);
        }
        exposureCompensationHolder = new CameraPropertyHolder(CameraPropertyListenerImpl.CAMERA_PROPERTY_EXPOSURE_COMPENSATION, exposureCompensationTextView, cameraController);
        driveModeHolder = new CameraPropertyHolder(CameraPropertyListenerImpl.CAMERA_PROPERTY_DRIVE_MODE, driveModeImageView, cameraController);
        takeModeHolder =  new CameraPropertyHolder(CameraPropertyListenerImpl.CAMERA_PROPERTY_TAKE_MODE, takeModeTextView, cameraController);
        whiteBalanceHolder = new CameraPropertyHolder(CameraPropertyListenerImpl.CAMERA_PROPERTY_WHITE_BALANCE, whiteBalanceImageView, cameraController);
        apertureHolder =  new CameraPropertyHolder(CameraPropertyListenerImpl.CAMERA_PROPERTY_APERTURE_VALUE, apertureValueTextView, cameraController);
        shutterSpeedHolder = new CameraPropertyHolder(CameraPropertyListenerImpl.CAMERA_PROPERTY_SHUTTER_SPEED, shutterSpeedTextView, cameraController);
        isoSensitivityHolder = new CameraPropertyHolder(CameraPropertyListenerImpl.CAMERA_PROPERTY_ISO_SENSITIVITY, isoSensitivityTextView, cameraController);
        aeModeHolder = new CameraPropertyHolder(CameraPropertyListenerImpl.CAMERA_PROPERTY_AE_MODE, aeModeTextView, cameraController);
    }

    /**
     *
     */
    private void prepareViewObjectHolders(View view)
    {
        imageView = view.findViewById(R.id.cameraLiveImageView);
        batteryLevelImageView = view.findViewById(R.id.batteryLevelImageView);
        remainingRecordableImagesTextView = view.findViewById(R.id.remainingRecordableImagesTextView);
        driveModeImageView = view.findViewById(R.id.drivemodeImageView);
        takeModeTextView = view.findViewById(R.id.takemodeTextView);
        shutterSpeedTextView = view.findViewById(R.id.shutterSpeedTextView);
        apertureValueTextView = view.findViewById(R.id.apertureValueTextView);
        exposureCompensationTextView = view.findViewById(R.id.exposureCompensationTextView);
        isoSensitivityTextView = view.findViewById(R.id.isoSensitivityTextView);
        focalLengthTextView = view.findViewById(R.id.focalLengthTextView);
        aeModeTextView = view.findViewById(R.id.aeModeTextView);
        informationTextView = view.findViewById(R.id.informationTextView);
        warningTextView = view.findViewById(R.id.warningTextView);
        otherTextView = view.findViewById(R.id.otherTextView);
        showControlPanelTextView =view.findViewById(R.id.showControlPanelTextView);
        hideControlPanelTextView = view.findViewById(R.id.hideControlPanelTextView);
        whiteBalanceImageView = view.findViewById(R.id.whiteBalanceImageView);
        shutterImageView = view.findViewById(R.id.shutterImageView);
        settingImageView = view.findViewById(R.id.settingImageView);
        unlockImageView = view.findViewById(R.id.unlockImageView);
        playbackImageView = view.findViewById(R.id.showPlaybackImageView);
        zoomInImageView = view.findViewById(R.id.zoomInImageView);
        zoomOutImageView = view.findViewById(R.id.zoomOutImageView);
        aeLockImageView = view.findViewById(R.id.aelockImageView);
        manualFocusImageView = view.findViewById(R.id.manualFocusImageView);
        saveLoadImageView = view.findViewById(R.id.saveLoadImageView);
        magnifyImageView = view.findViewById(R.id.magnifyImageView);
        selfTimerImageView = view.findViewById(R.id.selfTimerImageView);
    }

    /**
     *  ズームボタンの表示を切り替える
     *
     */
    private void updateZoomButtonVisibility()
    {
        if (cameraController == null)
        {
            return;
        }

        // ズームレンズのボタンを表示するかどうかを決める
        int visibility = (cameraController.isElectricZoomLens()) ? View.VISIBLE : View.INVISIBLE;
        if (zoomInImageView != null)
        {
            zoomInImageView.setVisibility(visibility);
        }
        if (zoomOutImageView != null)
        {
            zoomOutImageView.setVisibility(visibility);
        }
    }

    /**
     *   デジタル水準器の状態を設定する
     *
     */
    private void updateLevelGauge()
    {
        try
        {
            boolean levelGaugeStatus = cameraController.getLevelGaugeStatus();
            if (imageView != null)
            {
                imageView.updateLevelGauge(levelGaugeStatus);
            }
            if (statusListener != null)
            {
                statusListener.updateLevelGaugeStatus(levelGaugeStatus);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
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

    @Override
    public void loadProperties(final Activity activity, final String id, final String name)
    {
        //Log.v(TAG, "PROPERTY RESTORE ENTER : (" + id + ") " + name);

        //
        // BUSYダイアログを表示する
        //
        final ProgressDialog busyDialog = new ProgressDialog(activity);
        busyDialog.setMessage(getString(R.string.dialog_start_load_property_message));
        busyDialog.setTitle(getString(R.string.dialog_start_load_property_title));
        busyDialog.setIndeterminate(false);
        busyDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        busyDialog.show();

        try
        {
            Thread thread = new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    final boolean toast = restoreCameraSettings(id, name);
                    busyDialog.dismiss();

                    activity.runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            updateView(true);
                            // Toast で展開したよのメッセージを表示
                            if (toast)
                            {
                                String restoredMessage = getString(R.string.restored_my_props) + name;
                                Toast.makeText(getContext(), restoredMessage, Toast.LENGTH_SHORT).show();
                            }
                        }
                    });

                }
            });
            thread.start();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        //Log.v(TAG, "PROPERTY RESTORE EXIT : (" + id + ") " + name);
    }

    @Override
    public void saveProperties(final Activity activity, final String id, final String name)
    {
        //
        // BUSYダイアログを表示する
        //
        final ProgressDialog busyDialog = new ProgressDialog(activity);
        busyDialog.setMessage(getString(R.string.dialog_start_save_property_message));
        busyDialog.setTitle(getString(R.string.dialog_start_save_property_title));
        busyDialog.setIndeterminate(false);
        busyDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        busyDialog.show();

        try
        {
            Thread thread = new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    final boolean toast = storeCameraSettings(id, name);
                    busyDialog.dismiss();

                    activity.runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            // updateView();
                            // Toast で保存したよのメッセージを表示
                            if (toast)
                            {
                                String storedMessage = "";
                                Context context = getContext();
                                if (context != null)
                                {
                                    storedMessage = context.getString(R.string.saved_my_props);
                                }
                                storedMessage = storedMessage + name;
                                Toast.makeText(getContext(), storedMessage, Toast.LENGTH_SHORT).show();
                            }
                        }
                    });

                }
            });
            thread.start();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        Log.v(TAG, "PROPERTY STORED : " + id + " " + name);
    }

    private boolean storeCameraSettings(String itemId, String restoredDataName)
    {
        boolean toast = false;
        //Log.v(TAG, "storeCameraSettings() : START");
        try
        {
            if (backupRestoreProperties != null)
            {
                if (itemId.contentEquals("000"))
                {
                    Log.v(TAG, "AUTO SAVE DATA AREA...(NOT STORE PROPERTIES)");
                }
                else
                {
                    // データを保管する
                    backupRestoreProperties.storeCameraSettings(itemId);
                    backupRestoreProperties.setCameraSettingDataName(itemId, restoredDataName);
                    Log.v(TAG, "STORED : (" + itemId + ") " + restoredDataName);
                    toast = true;
                }
            }
            else
            {
                Log.v(TAG, "STORE INTERFACE IS NULL...");
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            Log.v(TAG, "STORE FAILED...");
        }
        //Log.v(TAG, "storeCameraSettings() : END");
        return (toast);
    }

    private boolean restoreCameraSettings(String itemId, String restoredDataName)
    {
        boolean toast = false;
        //Log.v(TAG, "restoreCameraSettings() : START");
        try
        {
            if (backupRestoreProperties != null)
            {
                if (itemId.contentEquals("000"))
                {
                    backupRestoreProperties.restoreCameraSettings("");
                    Log.v(TAG, "RESTORED AUTO SAVE DATA...");
                }
                else
                {
                    backupRestoreProperties.restoreCameraSettings(itemId);
                    Log.v(TAG, "RESTORED : (" + itemId + ") " + restoredDataName);
                }
                toast = true;
            }
            else
            {
                Log.v(TAG, "RESTORE INTERFACE IS NULL...");
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            Log.v(TAG, "RESTORE FAILED...");
        }
        //Log.v(TAG, "restoreCameraSettings() : END");
        return (toast);
    }

    /**
     *   水準器情報を伝達する
     */
    public void updateLevelGauge(String orientation, float roll, float pitch)
    {
        if (imageView != null)
        {
            imageView.updateLevelGauge(orientation, roll, pitch);
        }
    }

    /**
     *   セルフタイマーのカウントを更新する
     *
     */
    public void setTimerCount(int remainSeconds)
    {
        if (imageView != null)
        {
            imageView.setTimerCount(remainSeconds);
        }
    }

    /**
     *   ライブビューの表示中スケールを伝達する
     *
     * @param scale 拡大サイズ
     */
    void updateLiveViewScale(float scale)
    {
        if (imageView != null)
        {
            imageView.updateLiveViewScale(scale);
        }
    }

    void toggleSelfTimerIcon()
    {
        try
        {
            selfTimerFeature = !selfTimerFeature;
            if (!selfTimerFeature)
            {
                // セルフタイマーをOFFにする
                resetSelfTimer();
            }
            updateSelfTimerIcon(selfTimerFeature);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
     *   セルフタイマーのカウントダウンを止める
     *
     */
    private void resetSelfTimer()
    {
        if (takePictureControl != null)
        {
            takePictureControl.releaseSelfTimer();
        }
        setTimerCount(0);
    }

    private void updateSelfTimerIcon(final boolean isSelfTimer)
    {
        try
        {
            Activity activity = getActivity();
            if ((activity != null)&&(selfTimerImageView != null))
            {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        selfTimerImageView.setSelected(isSelfTimer);
                    }
                });
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public boolean handleKeyDown(int keyCode, KeyEvent event)
    {
        if (viewOnTouchClickListener == null)
        {
            return (false);
        }
        return (viewOnTouchClickListener.onKey(null, keyCode, event));
    }

    @Override
    public int getShutterDelaySeconds()
    {
        if (!selfTimerFeature)
        {
            // セルフィタイマー無効時は、0を応答する
            return (0);
        }
        try
        {
            // セルフタイマー秒数を応答する
            Context context = getContext();
            if (context != null)
            {
                SharedPreferences preferences = androidx.preference.PreferenceManager.getDefaultSharedPreferences(context);
                String delaySeconds = preferences.getString("self_timer_seconds", "3");
                return (Integer.parseInt(delaySeconds));
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        // エラー発生時は、セルフタイマーは無効とする
        return (0);
    }
}
