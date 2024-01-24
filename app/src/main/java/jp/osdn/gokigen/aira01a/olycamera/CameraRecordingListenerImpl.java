package jp.osdn.gokigen.aira01a.olycamera;

import android.app.Activity;
import android.widget.ImageView;

import jp.co.olympus.camerakit.OLYCamera;
import jp.co.olympus.camerakit.OLYCameraAutoFocusResult;
import jp.co.olympus.camerakit.OLYCameraRecordingListener;
import jp.osdn.gokigen.aira01a.liveview.LiveViewFragment;

/**
 *
 *
 */
public class CameraRecordingListenerImpl implements OLYCameraRecordingListener
{
    //private final String TAG = this.toString();
    private LiveViewFragment parent;
    private ImageView imageView = null;

    /**
     *   コンストラクタ
     *
     */
    public CameraRecordingListenerImpl(LiveViewFragment parent)
    {
        this.parent = parent;
    }

    @Override
    public void onStartRecordingVideo(OLYCamera olyCamera)
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (imageView != null) {
                    imageView.setSelected(true);
                }
            }
        });
    }

    /**
     *   更新するImageViewを拾う
     *
     */
    public void setImageView(ImageView target)
    {
        this.imageView = target;
    }

    @Override
    public void onStopRecordingVideo(OLYCamera olyCamera)
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (imageView != null) {
                    imageView.setSelected(false);
                }
            }
        });
    }

    @Override
    public void onChangeAutoFocusResult(OLYCamera olyCamera, OLYCameraAutoFocusResult olyCameraAutoFocusResult)
    {
        // do nothing!
    }

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
