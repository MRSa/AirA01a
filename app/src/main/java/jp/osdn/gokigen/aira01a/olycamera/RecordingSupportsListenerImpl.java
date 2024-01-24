package jp.osdn.gokigen.aira01a.olycamera;

import java.util.Map;

import jp.co.olympus.camerakit.OLYCamera;
import jp.co.olympus.camerakit.OLYCameraRecordingSupportsListener;
import jp.osdn.gokigen.aira01a.liveview.IStatusViewDrawer;
import jp.osdn.gokigen.aira01a.preview.RecordedImageDrawer;

/**
 *   OLYCameraRecordingSupportsListener の実装
 *   (RecordedImageDrawer用)
 *
 * Created by MRSa on 2016/04/29.
 */
public class RecordingSupportsListenerImpl implements OLYCameraRecordingSupportsListener
{
    //private final String TAG = this.toString();
    private RecordedImageDrawer imageDrawer;
    private IStatusViewDrawer statusDrawer;

    /**
     *   コンストラクタ
     *
     */
    public RecordingSupportsListenerImpl(RecordedImageDrawer drawer, IStatusViewDrawer statusDrawer)
    {
        this.imageDrawer = drawer;
        this.statusDrawer = statusDrawer;
    }

    /**
     *
     *
     */
    @Override
    public void onReadyToReceiveCapturedImagePreview(OLYCamera camera)
    {

    }

    /**
     *
     *
     */
    @Override
    public void onReceiveCapturedImagePreview(OLYCamera camera, byte[] data, Map<String, Object> metadata)
    {
        if (imageDrawer != null)
        {
            imageDrawer.onReceiveCapturedImagePreview(camera, data, metadata);
        }
    }

    /**
     *
     */
    @Override
    public void onFailToReceiveCapturedImage(OLYCamera camera, Exception e)
    {

    }

    /**
     *
     */
    @Override
    public void onReadyToReceiveCapturedImage(OLYCamera camera)
    {

    }

    /**
     *
     *
     */
    @Override
    public void onReceiveCapturedImage(OLYCamera camera, byte[] data, Map<String, Object> metadata)
    {

    }

    /**
     *
     *
     */
    @Override
    public void onFailToReceiveCapturedImagePreview(OLYCamera camera, Exception e)
    {

    }

    /**
     *   ズーム操作が止まった
     *
     */
    @Override
    public void onStopDrivingZoomLens(OLYCamera camera)
    {
        if (statusDrawer != null)
        {
            statusDrawer.updateFocalLengthView();
        }
    }


}
