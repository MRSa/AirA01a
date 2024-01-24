package jp.osdn.gokigen.aira01a.takepicture;

import java.util.HashMap;

import jp.co.olympus.camerakit.OLYCamera;
import jp.co.olympus.camerakit.OLYCameraKitException;
import jp.osdn.gokigen.aira01a.R;

/**
 *   ビデオ撮影時の制御クラス。
 *
 * Created by MRSa on 2016/06/18.
 */
public class MovieRecordingControl implements OLYCamera.CompletedCallback
{
    private OLYCamera camera;
    private ITakePictureRequestedControl control;
    private boolean isShutterImageSelected = false;

    /**
     *   コンストラクタ
     *
     */
    MovieRecordingControl(OLYCamera camera, ITakePictureRequestedControl control)
    {
        this.camera = camera;
        this.control = control;
    }

    /**
     *   動画撮影の開始と終了
     *
     */
    void movieControl()
    {
        if (camera.isTakingPicture())
        {
            // スチル撮影中の場合は、何もしない（モード異常なので）
            return;
        }
        if (!camera.isRecordingVideo())
        {
            // ムービー撮影の開始
            isShutterImageSelected = true;
            camera.startRecordingVideo(new HashMap<String, Object>(), this);
        }
        else
        {
            // ムービー撮影の終了
            isShutterImageSelected = false;
            camera.stopRecordingVideo(this);
        }
    }

    /**
     *   処理完了
     *
     */
    @Override
    public void onCompleted()
    {
        control.setShutterImageSelected(isShutterImageSelected);
    }

    /**
     *   エラー発生
     *
     * @param e 例外情報
     */
    @Override
    public void onErrorOccurred(OLYCameraKitException e)
    {
        control.presentMessage(R.string.shutter_control_record_failed, e.getMessage());
    }
}
