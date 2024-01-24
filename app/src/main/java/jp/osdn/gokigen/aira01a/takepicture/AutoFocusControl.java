package jp.osdn.gokigen.aira01a.takepicture;

import android.graphics.PointF;
import android.graphics.RectF;
import android.util.Log;

import jp.co.olympus.camerakit.OLYCamera;
import jp.co.olympus.camerakit.OLYCameraAutoFocusResult;
import jp.co.olympus.camerakit.OLYCameraKitException;
import jp.osdn.gokigen.aira01a.liveview.CameraLiveImageView;

/**
 *   オートフォーカス制御くらす
 *
 * Created by MRSa on 2016/06/19.
 */
public class AutoFocusControl implements OLYCamera.TakePictureCallback
{
    private final String TAG = toString();
    private OLYCamera camera;
    private ITakePictureRequestedControl control;
    private  RectF focusFrameRect =null;

    /**
     *   コンストラクタ
     *
     */
    AutoFocusControl(OLYCamera camera, ITakePictureRequestedControl control)
    {
        this.camera = camera;
        this.control = control;
    }

    /**
     *   オートフォーカスを駆動させ、ロックする
     *
     * @param point  ターゲットAF点
     */
    boolean driveAutoFocus(PointF point, boolean isFocusLock)
    {
        if (camera.isTakingPicture() || camera.isRecordingVideo())
        {
            //  撮影中の場合にはフォーカスロックはやらない。
            return (false);
        }
        Log.v(TAG, "AF LOCK (driveAutoFocus): " + isFocusLock);

        // Display a provisional focus frame at the touched point.
        final RectF preFocusFrameRect;
        {
            float focusWidth = 0.125f;  // 0.125 is rough estimate.
            float focusHeight = 0.125f;
            float imageWidth = control.getIntrinsicContentSizeWidth();
            float imageHeight = control.getIntrinsicContentSizeHeight();
            if (imageWidth > imageHeight) {
                focusHeight *= (imageWidth / imageHeight);
            } else {
                focusHeight *= (imageHeight / imageWidth);
            }
            preFocusFrameRect = new RectF(point.x - focusWidth / 2.0f, point.y - focusHeight / 2.0f,
                    point.x + focusWidth / 2.0f, point.y + focusHeight / 2.0f);
        }
        control.showFocusFrame(preFocusFrameRect, CameraLiveImageView.FocusFrameStatus.Running);

        try
        {
            // Set auto-focus point.
            camera.setAutoFocusPoint(point);

            // Lock auto-focus.
            if (isFocusLock)
            {
                focusFrameRect = preFocusFrameRect;
                camera.lockAutoFocus(this);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            // Lock failed.
            try
            {
                camera.clearAutoFocusPoint();
                camera.unlockAutoFocus();
                control.setFocusFrameStatus(false);
                control.showFocusFrame(preFocusFrameRect, CameraLiveImageView.FocusFrameStatus.Failed, 1.0);
            }
            catch (Exception ee)
            {
                ee.printStackTrace();
            }
            return (false);
        }
        return (true);
    }

    /**
     *   AF-Lを解除する
     *
     */
    void unlockAutoFocus()
    {
        if (camera.isTakingPicture() || camera.isRecordingVideo())
        {
            // 撮影中の場合には、フォーカスロック解除はやらない
            return;
        }

        // Unlock auto-focus.
        try
        {
            camera.unlockAutoFocus();
            camera.clearAutoFocusPoint();
            control.setFocusFrameStatus(false);
            control.hideFocusFrame(false);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }


    @Override
    public void onProgress(OLYCamera olyCamera, OLYCamera.TakingProgress takingProgress, OLYCameraAutoFocusResult olyCameraAutoFocusResult)
    {
        String result = olyCameraAutoFocusResult.getResult();
        Log.v(TAG, "AutoFocusControl::onProgress() : " + result);
        RectF postFocusFrameRect = olyCameraAutoFocusResult.getRect();
        if (takingProgress == OLYCamera.TakingProgress.EndFocusing)
        {
            if (result.equals("ok") && postFocusFrameRect != null)
            {
                // Lock succeed.
                control.setFocusFrameStatus(true);

                //focusedSoundPlayer.start();
                //RectF postFocusFrameRect = olyCameraAutoFocusResult.getRect();
                control.showFocusFrame(postFocusFrameRect, CameraLiveImageView.FocusFrameStatus.Focused);

            } else if (result.equals("none")) {
                // Could not lock.
                try
                {
                    camera.clearAutoFocusPoint();
                    camera.unlockAutoFocus();
                }
                catch (OLYCameraKitException ee)
                {
                    ee.printStackTrace();
                }
                control.setFocusFrameStatus(false);
                control.hideFocusFrame(false);
            }
            else
            {
                // Lock failed.
                try {
                    camera.clearAutoFocusPoint();
                    camera.unlockAutoFocus();
                } catch (OLYCameraKitException ee)
                {
                    ee.printStackTrace();
                }
                control.setFocusFrameStatus(false);
                control.showFocusFrame(focusFrameRect, CameraLiveImageView.FocusFrameStatus.Failed, 1.0);
            }
        }
    }

    @Override
    public void onCompleted()
    {
        // なにもしない
        Log.v(TAG, "AutoFocusControl::onCompleted()");
    }

    @Override
    public void onErrorOccurred(Exception e)
    {
        // Lock failed.
        e.printStackTrace();
        try
        {
            camera.clearAutoFocusPoint();
            camera.unlockAutoFocus();
            control.setFocusFrameStatus(false);
            control.showFocusFrame(focusFrameRect, CameraLiveImageView.FocusFrameStatus.Unknown, 1.0);
            //control.hideFocusFrame(false);
        }
        catch (Exception ee)
        {
            ee.printStackTrace();
        }
        Log.i(TAG, "AF LOCK FAIL.");
        //control.presentMessage(R.string.shutter_control_af_failed, e.getMessage());
    }
}
