package jp.osdn.gokigen.aira01a.takepicture;

import android.graphics.RectF;
import java.util.HashMap;
import jp.co.olympus.camerakit.OLYCamera;
import jp.co.olympus.camerakit.OLYCameraAutoFocusResult;
import jp.osdn.gokigen.aira01a.liveview.CameraLiveImageView;
import jp.osdn.gokigen.aira01a.R;

/**
 *   一枚撮影用のクラス
 *
 * Created by MRSa on 2016/06/18.
 */
public class SingleShotControl implements OLYCamera.TakePictureCallback
{
    private OLYCamera camera;
    private ITakePictureRequestedControl control;

    /**
     *  コンストラクタ
     *
     */
    SingleShotControl(OLYCamera camera, ITakePictureRequestedControl control)
    {
        this.camera = camera;
        this.control = control;
    }

    /**
     *   1枚撮影する
     *
     */
    void singleShot()
    {
        try
        {
            camera.takePicture(new HashMap<String, Object>(), this);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void onProgress(OLYCamera olyCamera, OLYCamera.TakingProgress takingProgress, OLYCameraAutoFocusResult olyCameraAutoFocusResult)
    {
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
        try
        {
            if (!control.getFocusFrameStatus())
            {
                camera.clearAutoFocusPoint();
                control.setFocusFrameStatus(false);
                control.hideFocusFrame(false);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
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
    }
}
