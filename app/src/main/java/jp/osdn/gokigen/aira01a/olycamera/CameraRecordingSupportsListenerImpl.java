package jp.osdn.gokigen.aira01a.olycamera;

import android.app.Activity;

import java.util.Map;

import jp.co.olympus.camerakit.OLYCamera;
import jp.co.olympus.camerakit.OLYCameraRecordingSupportsListener;
import jp.osdn.gokigen.aira01a.liveview.LiveViewFragment;

/**
 *
 *
 *
 */
public class CameraRecordingSupportsListenerImpl implements OLYCameraRecordingSupportsListener
{
    //private final String TAG = this.toString();
    private LiveViewFragment parent;

    /**
     *   コンストラクタ
     *
     */
    public CameraRecordingSupportsListenerImpl(LiveViewFragment parent)
    {
        this.parent = parent;
    }


    @Override
    public void onReadyToReceiveCapturedImagePreview(OLYCamera olyCamera)
    {

    }

    @Override
    public void onReceiveCapturedImagePreview(OLYCamera olyCamera, byte[] bytes, Map<String, Object> map)
    {
        parent.transToPreviewFragment(bytes, map);
    }

    @Override
    public void onFailToReceiveCapturedImagePreview(OLYCamera olyCamera, Exception e)
    {

    }

    @Override
    public void onReadyToReceiveCapturedImage(OLYCamera olyCamera)
    {

    }

    @Override
    public void onReceiveCapturedImage(OLYCamera olyCamera, byte[] bytes, Map<String, Object> map)
    {

    }

    @Override
    public void onFailToReceiveCapturedImage(OLYCamera olyCamera, Exception e)
    {

    }

    @Override
    public void onStopDrivingZoomLens(OLYCamera olyCamera)
    {
        try {
            Activity activity = parent.getActivity();
            if (activity != null)
            {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        parent.updateFocalLengthView();
                    }
                });
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
