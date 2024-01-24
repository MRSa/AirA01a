package jp.osdn.gokigen.aira01a.olycamera;

import android.util.Log;

import jp.co.olympus.camerakit.OLYCamera;

public class ChangeCameraRunMode implements Runnable
{
    private final String TAG = toString();
    private final OLYCamera camera;
    private final OLYCamera.RunMode checkRunMode;
    private final IChangeRunModeCallback callback;
    public ChangeCameraRunMode(OLYCamera camera, OLYCamera.RunMode checkRunMode, IChangeRunModeCallback callback)
    {
        this.camera = camera;
        this.callback = callback;
        this.checkRunMode = checkRunMode;
    }

    @Override
    public void run()
    {
        try
        {
            if (camera != null)
            {
                // 再生モードかどうかを確認して、再生モードでなかった場合には再生モードに切り替える。
                OLYCamera.RunMode runMode = camera.getRunMode();
                if (runMode != checkRunMode)
                {
                    Log.v(TAG, "changeRunMode(" + checkRunMode.toString() + ") : Start");
                    camera.changeRunMode(checkRunMode);
                    Log.v(TAG, "changeRunMode(" + checkRunMode.toString() + ") : End");
                }
                if (callback != null)
                {
                    callback.changedCameraRunMode(checkRunMode);
                }
                return;
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        if (callback != null)
        {
            callback.abortToChangeCameraRunMode();
        }
    }

    public interface IChangeRunModeCallback
    {
        void changedCameraRunMode(OLYCamera.RunMode runMode);
        void abortToChangeCameraRunMode();
    }
}
