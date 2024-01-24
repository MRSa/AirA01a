package jp.osdn.gokigen.aira01a.takepicture;

import android.graphics.RectF;
import jp.co.olympus.camerakit.OLYCamera;
import jp.co.olympus.camerakit.OLYCameraAutoFocusResult;
import jp.osdn.gokigen.aira01a.liveview.CameraLiveImageView;
import jp.osdn.gokigen.aira01a.R;

/**
 *   連続撮影用のクラス
 *
 * Created by MRSa on 2016/06/18.
 */
public class SequentialShotControl implements OLYCamera.TakePictureCallback
{
    private OLYCamera camera;
    private ITakePictureRequestedControl control;

    // 撮影状態の記録
    private enum shootingStatus
    {
        Unknown,
        Starting,
        Stopping,
    }

    private shootingStatus currentStatus = shootingStatus.Unknown;

    /**
     *   コンストラクタ
     *
     */
    SequentialShotControl(OLYCamera camera, ITakePictureRequestedControl control)
    {
        this.camera = camera;
        this.control = control;
    }


    /**
     *   撮影の開始と終了
     *
     */
    void shotControl()
    {
        if (camera.isRecordingVideo())
        {
            // ビデオ撮影中の場合は、何もしない（モード異常なので）
            return;
        }
        try
        {
            if (!camera.isTakingPicture())
            {
                // 連続撮影の開始
                currentStatus = shootingStatus.Starting;
                camera.startTakingPicture(null, this);
            }
            else
            {
                // 連続撮影の終了
                currentStatus = shootingStatus.Stopping;
                camera.stopTakingPicture(this);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }


    @Override
    public void onProgress(OLYCamera olyCamera, OLYCamera.TakingProgress takingProgress, OLYCameraAutoFocusResult olyCameraAutoFocusResult)
    {
        if (currentStatus == shootingStatus.Stopping)
        {
            // 終了中の時にはなにもしない
            return;
        }

        // 撮影中の更新処理
        if (takingProgress == OLYCamera.TakingProgress.EndFocusing)
        {
            if (!control.getFocusFrameStatus())
            {
                String result = olyCameraAutoFocusResult.getResult();
                if (result.equals("ok"))
                {
                    RectF postFocusFrameRect = olyCameraAutoFocusResult.getRect();
                    if (postFocusFrameRect == null)
                    {
                        // フォーカスが合っているはずなのにフォーカスフレームがない異常...
                        return;
                    }
                    control.showFocusFrame(postFocusFrameRect, CameraLiveImageView.FocusFrameStatus.Focused);
                }
                else if (result.equals("none"))
                {
                    control.setFocusFrameStatus(false);
                    control.hideFocusFrame(false);
                }
                else
                {
                    control.setFocusFrameStatus(false);
                    control.hideFocusFrame(false);
                }
            }
        }
        //else if (takingProgress == OLYCamera.TakingProgress.BeginCapturing)
        //{
        //    shutterSoundPlayer.start();
        //}

    }

    @Override
    public void onCompleted()
    {
        if (currentStatus != shootingStatus.Stopping)
        {
            // 撮影停止中以外ではなにもしない。
            return;
        }

        if (!control.getFocusFrameStatus())
        {
            try
            {
                camera.clearAutoFocusPoint();
                control.setFocusFrameStatus(false);
                control.hideFocusFrame(false);
            }
            catch (Exception e)
            {
                // 例外を拾う
                e.printStackTrace();
            }
        }
        currentStatus = shootingStatus.Unknown;
    }

    @Override
    public void onErrorOccurred(Exception e)
    {
        if (!control.getFocusFrameStatus())
        {
            try
            {
                camera.clearAutoFocusPoint();
                control.setFocusFrameStatus(false);
                control.hideFocusFrame(false);
            }
            catch (Exception ee)
            {
                ee.printStackTrace();
            }
        }
        e.printStackTrace();
        control.presentMessage(R.string.shutter_control_take_failed, e.getMessage());
        currentStatus = shootingStatus.Unknown;
    }
}
