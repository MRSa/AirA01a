package jp.osdn.gokigen.aira01a.liveview;

import android.graphics.PointF;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;

import jp.osdn.gokigen.aira01a.R;
import jp.osdn.gokigen.aira01a.takepicture.CameraController;
import jp.osdn.gokigen.aira01a.takepicture.TakePictureControl;

/**
 *
 *
 */
class CameraViewOnTouchClickListenerImpl implements View.OnClickListener, View.OnTouchListener, TakePictureControl.WithCaptureLiveView
{
    private final String TAG = this.toString();
    private final LiveViewFragment parent;
    private final CameraController cameraController;
    private final TakePictureControl takePictureControl;
    private final ILiveViewMagnifyControl liveViewControl;

    /**
     * コンストラクタ
     */
    CameraViewOnTouchClickListenerImpl(LiveViewFragment parent, CameraController controller, TakePictureControl takePicture, ILiveViewMagnifyControl liveViewControl)
    {
        this.parent = parent;
        this.cameraController = controller;
        this.takePictureControl = takePicture;
        this.liveViewControl = liveViewControl;
    }

    /**
     * オブジェクトがクリックされた時の処理(分岐)
     *
     */
    @Override
    public void onClick(View v)
    {
        int id = v.getId();
        switch (id)
        {
            case R.id.drivemodeImageView:
                cameraController.changeDriveMode();
                break;
            case R.id.takemodeTextView:
                cameraController.changeTakeMode();
                break;
            case R.id.shutterSpeedTextView:
                cameraController.changeShutterSpeed();
                break;
            case R.id.apertureValueTextView:
                cameraController.changeApertureValue();
                break;
            case R.id.exposureCompensationTextView:
                cameraController.changeExposureCompensation();
                break;
            case R.id.isoSensitivityTextView:
                cameraController.changeIsoSensitivity();
                break;
            case R.id.aeModeTextView:
                cameraController.changeAEModeValue();
                break;
            case R.id.whiteBalanceImageView:
                cameraController.changeWhiteBalance();
                break;
            case R.id.settingImageView:
                takePictureControl.abortTakingPictures();
                parent.transToSettingFragment();
                break;
            case R.id.unlockImageView:
                takePictureControl.unlockAutoFocus();
                break;
            case R.id.showPlaybackImageView:
                if (cameraController.changeToPlaybackMode())
                {
                    parent.transToPlaybackFragment();
                }
                break;
            case R.id.zoomInImageView:
                if (!cameraController.isZooming())
                {
                    // ズーム動作中ではない場合、ズームインする
                    cameraController.driveZoomLens(+1);
                }
                break;
            case R.id.zoomOutImageView:
                if (!cameraController.isZooming())
                {
                    // ズーム動作中ではない場合、ズームアウトする
                    cameraController.driveZoomLens(-1);
                }
                break;
            case R.id.aelockImageView:
                // AE LOCK状態をトグルする
                cameraController.toggleAELockStatus();
                break;
            case R.id.focalLengthTextView:
                // 焦点距離情報を更新する
                parent.updateFocalLengthView();
                break;
            case R.id.manualFocusImageView:
                // AF/MF状態をトグルする
                cameraController.toggleManualFocusStatus();
                break;
            case R.id.hideControlPanelTextView:
                // 制御パネルを非表示にする
                parent.hideControlPanel();
                break;
            case R.id.showControlPanelTextView:
                // 制御パネルを表示する
                parent.showControlPanel();
                break;
            case R.id.saveLoadImageView:
                // カメラプロパティの読み出し・保存ダイアログを表示する
                parent.showSaveLoadMyCameraPropertyDialog();
                break;
            case R.id.magnifyImageView:
                //  ライブビューの拡大・縮小
                changeLiveViewMagnify();
                break;
            case R.id.selfTimerImageView:
                // セルフタイマー設定の変更
                changeSelfTimerSetting();
                break;
            default:
                //  do nothing!
                break;
        }
    }

    /**
     * オブジェクトがタッチされた時の処理(分岐)
     *
     */
    @Override
    public boolean onTouch(View v, MotionEvent event)
    {
        int id = v.getId();
        if (id == R.id.cameraLiveImageView)
        {
            processCameraLiveImageView(event);
            return (true);
        }
        else if (id == R.id.shutterImageView)
        {
            processShutterImageView(event);
            return (true);
        }
        //return (v.performClick());
        return (false);
    }

    /**
     *   ライブビュー画像の拡大・縮小
     *
     */
    private void changeLiveViewMagnify()
    {
        Log.v(TAG, "changeLiveViewMagnify()");
        try
        {
            parent.updateLiveViewScale(liveViewControl.changeLiveViewMagnifyScale());
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }


    /**
     *   セルフタイマー設定の変更
     *
     */
    private void changeSelfTimerSetting()
    {
        Log.v(TAG, "changeSelfTimerSetting()");
        try
        {
            parent.toggleSelfTimerIcon();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }



    /**
     *
     */
    private void processCameraLiveImageView(MotionEvent event)
    {
        if (event.getAction() == MotionEvent.ACTION_DOWN)
        {
            PointF point = parent.getPointWithEvent(event);
            if (parent.isFocusPointArea(point))
            {
                takePictureControl.startTakePicture(point, this);
            }
        }
        else if (event.getAction() == MotionEvent.ACTION_UP)
        {
            takePictureControl.finishTakePicture();
        }
    }

    /**
     *
     */
    private void processShutterImageView(MotionEvent event)
    {
        if (event.getAction() == MotionEvent.ACTION_DOWN)
        {
            //takePictureControl.startTakePicture(TakePictureControl.AutoBracketingType.None);
            takePictureControl.startTakePicture(this);
        }
        else if (event.getAction() == MotionEvent.ACTION_UP)
        {
            takePictureControl.finishTakePicture();
        }
    }

    @Override
    public void doCapture(boolean isShare)
    {
        try
        {
            CameraLiveImageView imageView = parent.getLiveImageView();
            if (imageView != null)
            {
                imageView.storeImage(isShare);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
     *   ボタンを押したときの対応
     *
     */
    public boolean onKey(View view, int keyCode, @NonNull KeyEvent keyEvent)
    {
        Log.v(TAG, "onKey() : " + keyCode);
        try
        {
            if ((keyEvent.getAction() == KeyEvent.ACTION_DOWN)&&
                    ((keyCode == KeyEvent.KEYCODE_VOLUME_UP)||(keyCode == KeyEvent.KEYCODE_CAMERA)))
            {
                takePictureControl.startTakePicture(this);
                return (true);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return (false);
    }


    // -------------------------------------------------------------------------
    // Camera actions
    // -------------------------------------------------------------------------

    //
    // Touch Shutter mode:
    //   - Tap a subject to focus and automatically release the shutter.
    //
    // Touch AF mode:
    //   - Tap to display a focus frame and focus on the subject in the selected area.
    //   - You can use the image view to choose the position of the focus frame.
    //   - Photographs can be taken by tapping the shutter button.
    //
}
