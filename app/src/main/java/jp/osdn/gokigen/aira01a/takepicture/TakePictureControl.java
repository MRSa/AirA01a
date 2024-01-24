package jp.osdn.gokigen.aira01a.takepicture;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.PointF;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import jp.co.olympus.camerakit.OLYCamera;
import jp.osdn.gokigen.aira01a.liveview.ISelfTimerControl;
import jp.osdn.gokigen.aira01a.liveview.ISelfTimerDrawer;

/**
 *   カメラのAF制御と撮影の制御
 *
 *
 */
public class TakePictureControl
{
    private final String TAG = this.toString();
    private final OLYCamera camera;
    private final Context context;
    private final ITakePictureRequestedControl control;
    private final ISelfTimerControl selfTimerControl;

    private ISelfTimerDrawer selfTimerDrawer = null;
    private boolean isSelfTimerCountdown = false;

    private final MovieRecordingControl movieShot;
    private final SequentialShotControl sequentialShot;
    private final SingleShotControl singleShot;
    private final AutoFocusControl autoFocus;
    private final BracketingShotControl bracketing;

    /**
     * コンストラクタ
     */
    public TakePictureControl(Context context, @NonNull OLYCamera camera, @NonNull ITakePictureRequestedControl control, @NonNull ISelfTimerControl selfTimerControl)
    {
        this.context =context;
        this.camera = camera;
        this.control = control;
        this.selfTimerControl = selfTimerControl;
        movieShot = new MovieRecordingControl(camera, control);
        sequentialShot = new SequentialShotControl(camera, control);
        singleShot = new SingleShotControl(camera, control);
        autoFocus = new AutoFocusControl(camera, control);
        bracketing = new BracketingShotControl(camera, control);
    }

    public void setSelfTimerDrawer(@Nullable ISelfTimerDrawer drawer)
    {
        this.selfTimerDrawer = drawer;
    }

    /**
     * 写真撮影を開始する
     */
    public void startTakePicture(WithCaptureLiveView captureLiveView)
    {
        if (isSelfTimerCountdown)
        {
            // セルフタイマーがすでに稼働中...終了する
            Log.v(TAG, "[a] SELF TIMER IS ALREADY COUNTING.");
            return;
        }

        int delaySeconds = selfTimerControl.getShutterDelaySeconds();
        Log.v(TAG, " Delay Seconds[a] : " + delaySeconds);
        if ((delaySeconds > 0)&&(selfTimerDrawer != null))
        {
            startSelfTimerTakePictureImpl(captureLiveView, delaySeconds * 1000);
            return;
        }
        startTakePictureImpl(captureLiveView);
    }

    private void startSelfTimerTakePictureImpl(final WithCaptureLiveView captureLiveView, final int delaySeconds)
    {
        try
        {
            isSelfTimerCountdown = true;
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run()
                {
                    int timer = 0;
                    while ((isSelfTimerCountdown)&&(timer < delaySeconds))
                    {
                        try
                        {
                            //noinspection BusyWait
                            Thread.sleep(250);
                        }
                        catch (Exception e)
                        {
                            e.printStackTrace();
                        }
                        timer += 250;
                        selfTimerDrawer.setTimerCount((delaySeconds - timer) / 1000 + 1);
                    }
                    if (isSelfTimerCountdown)
                    {
                        startTakePictureImpl(captureLiveView);
                    }
                    isSelfTimerCountdown = false;
                    selfTimerDrawer.setTimerCount(0);
                }
            });
            thread.start();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            selfTimerDrawer.setTimerCount(0);
        }
    }

    private void startTakePictureImpl(WithCaptureLiveView captureLiveView)
    {
        try
        {
            OLYCamera.ActionType actionType = camera.getActionType();
            if (actionType == OLYCamera.ActionType.Movie)
            {
                // 動画撮影モードの時には、撮影開始・撮影終了を指示する
                movieShot.movieControl();
                return;
            }

            if ((camera.isTakingPicture()) || (camera.isRecordingVideo()))
            {
                // スチル or ムービー撮影中の時には、何もしない
                return;
            }

            if (actionType == OLYCamera.ActionType.Single)
            {
                if (control.getAutoBracketingSetting(false) == BracketingShotControl.BRACKET_NONE)
                {
                    // 一枚とる
                    singleShot.singleShot();
                }
                else
                {
                    // オートブラケッティングの実行
                    bracketing.startShootBracketing();
                }

                // ライブビュー
                processCaptureLiveView(captureLiveView);
            }
            else if (actionType == OLYCamera.ActionType.Sequential)
            {
                // 連続でとる
                sequentialShot.shotControl();
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
     * 撮影の終了
     */
    public void finishTakePicture()
    {
        if (OLYCamera.ActionType.Sequential == camera.getActionType())
        {
            // 連続撮影モードの時のみ、撮影終了を指示する
            sequentialShot.shotControl();
        }
    }


    /**
     * @param point フォーカス（焦点を合わせる）ポイント
     */
    public void startTakePicture(PointF point, WithCaptureLiveView captureLiveView)
    {
        OLYCamera.ActionType actionType = camera.getActionType();
        if (!control.getTouchShutterStatus())
        {
            // Touch AF mode
            if (actionType == OLYCamera.ActionType.Single || actionType == OLYCamera.ActionType.Sequential)
            {
                lockAutoFocus(point);
            }
            return;
        }

        if (isSelfTimerCountdown)
        {
            // セルフタイマーがすでに稼働中...終了する
            Log.v(TAG, "[b] SELF TIMER IS ALREADY COUNTING.");
            return;
        }

        int delaySeconds = selfTimerControl.getShutterDelaySeconds();
        Log.v(TAG, " Delay Seconds[b] : " + delaySeconds);
        if ((delaySeconds > 0)&&(selfTimerDrawer != null))
        {
            startSelfTimerTakePictureImpl(actionType, point, captureLiveView, (delaySeconds * 1000));
            return;
        }
        startTakePictureImpl(actionType, point, captureLiveView);
    }

    private void startSelfTimerTakePictureImpl(final OLYCamera.ActionType actionType, final PointF point, final WithCaptureLiveView captureLiveView, final int delaySeconds)
    {
        try
        {
            isSelfTimerCountdown = true;
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run()
                {
                    int timer = 0;
                    while ((isSelfTimerCountdown)&&(timer < delaySeconds))
                    {
                        try
                        {
                            //noinspection BusyWait
                            Thread.sleep(250);
                        }
                        catch (Exception e)
                        {
                            e.printStackTrace();
                        }
                        timer += 250;
                        selfTimerDrawer.setTimerCount((delaySeconds - timer) / 1000 + 1);
                    }
                    if (isSelfTimerCountdown)
                    {
                        startTakePictureImpl(actionType, point, captureLiveView);
                    }
                    isSelfTimerCountdown = false;
                    selfTimerDrawer.setTimerCount(0);
                }
            });
            thread.start();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            selfTimerDrawer.setTimerCount(0);
        }
    }

    private void startTakePictureImpl(OLYCamera.ActionType actionType, PointF point, WithCaptureLiveView captureLiveView)
    {
        // Touch Shutter mode
        if (actionType == OLYCamera.ActionType.Single)
        {
            takePictureWithPointImpl(point);
        }
        else if (actionType == OLYCamera.ActionType.Sequential)
        {
            autoFocus.driveAutoFocus(point, false);
            sequentialShot.shotControl();
        }
        else if (actionType == OLYCamera.ActionType.Movie)
        {
            movieShot.movieControl();
        }
        processCaptureLiveView(captureLiveView);
    }

    /**
     * ポイント指定で撮影する
     *
     * @param point フォーカス（焦点を合わせる）ポイント
     */
    public void takePictureWithPoint(PointF point)
    {
        if (isSelfTimerCountdown)
        {
            // セルフタイマーがすでに稼働中...終了する
            Log.v(TAG, "[c] SELF TIMER IS ALREADY COUNTING.");
            return;
        }

        final int delaySeconds = selfTimerControl.getShutterDelaySeconds();
        if ((delaySeconds > 0)&&(selfTimerDrawer != null))
        {
            // セルフタイマーを使ってシャッターを切る
            takePictureWithPointAndSelfTimer(point, delaySeconds * 1000);
            return;
        }
        // セルフタイマーを使わずにシャッターを切る
        takePictureWithPointImpl(point);
    }

    private void takePictureWithPointAndSelfTimer(final PointF point, final int delaySeconds)
    {
        try
        {
            isSelfTimerCountdown = true;
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run()
                {
                    int timer = 0;
                    while ((isSelfTimerCountdown)&&(timer < delaySeconds))
                    {
                        try
                        {
                            //noinspection BusyWait
                            Thread.sleep(250);
                        }
                        catch (Exception e)
                        {
                            e.printStackTrace();
                        }
                        timer += 250;
                        selfTimerDrawer.setTimerCount((delaySeconds - timer) / 1000 + 1);
                    }
                    if (isSelfTimerCountdown)
                    {
                        takePictureWithPointImpl(point);
                    }
                    isSelfTimerCountdown = false;
                    selfTimerDrawer.setTimerCount(0);
                }
            });
            thread.start();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            selfTimerDrawer.setTimerCount(0);
        }
    }

    private void takePictureWithPointImpl(PointF point)
    {
        try
        {
            // LiveViewFragmentから直接呼ばれていた...タッチシャッターモードで顔検出した時の場合...
            if (autoFocus.driveAutoFocus(point, false))
            {
                if (control.getAutoBracketingSetting(false) == BracketingShotControl.BRACKET_NONE)
                {
                    // AF成功時のみ一枚写真を撮影する
                    singleShot.singleShot();
                }
                else
                {
                    // オートブラケッティングの実行
                    bracketing.startShootBracketing();
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
     * AF枠をクリアする
     */
    public void resetAutoFocus()
    {
        try
        {
            camera.clearAutoFocusPoint();
            camera.unlockAutoFocus();
            control.setFocusFrameStatus(false);
        }
        catch (Exception ee)
        {
            ee.printStackTrace();
        }
    }

    /**
     * 撮影を中断する
     */
    public void abortTakingPictures()
    {
        if (camera.isTakingPicture())
        {
            // 撮影中の時には撮影を終わらせる
            sequentialShot.shotControl();
        }
        if (camera.isRecordingVideo())
        {
            // ムービー撮影中の場合は終了させる
            movieShot.movieControl();
        }
    }

    /**
     *    ライブビュー画像の保管（と、共有）
     *
     */
    private void processCaptureLiveView(WithCaptureLiveView captureLiveView)
    {
        try
        {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            boolean isCapture = preferences.getBoolean("capture_live_view", false);
            if ((isCapture) && (captureLiveView != null))
            {
                captureLiveView.doCapture(false);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
     *   オートフォーカスをロックする
     *
     * @param point  ターゲットAF点
     */
    public void lockAutoFocus(PointF point)
    {
        autoFocus.driveAutoFocus(point, true);
    }

    /**
     *   AF-Lを解除する
     *
     */
    public void unlockAutoFocus()
    {
        autoFocus.unlockAutoFocus();
    }

    /**
     *   セルフタイマーカウントダウン中の場合、カウンタをリセットする
     *
     */
    public void releaseSelfTimer()
    {
        Log.v(TAG, "releaseSelfTimer()");
        isSelfTimerCountdown = false;
        if (selfTimerDrawer != null)
        {
            selfTimerDrawer.setTimerCount(0);
        }
    }

    public interface WithCaptureLiveView
    {
        void doCapture(boolean isShare);
    }

}
