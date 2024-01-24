package jp.osdn.gokigen.aira01a.takepicture;

import android.graphics.PointF;
import android.util.Log;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jp.co.olympus.camerakit.OLYCamera;
import jp.co.olympus.camerakit.OLYCameraAutoFocusResult;
import jp.osdn.gokigen.aira01a.olycamera.CameraPropertyListenerImpl;

/**
 *   オートブラケッティング実行クラス
 *
 *
 * Created by MRSa on 2016/06/18.
 */
public class BracketingShotControl implements OLYCamera.TakePictureCallback
{
    private final String TAG = toString();

    private static final int BRACKETING_INTERVAL_MILLISECOND = 250; // 撮影待ち時間(ms)
    private static final int BRACKETING_PROPERTY_VALUE_SET_RETRY = 10;

    public static final int BRACKET_NONE = 0;       // 通常のショット
    private static final int BRACKET_EXPREV = 1;     // 露出補正
    private static final int BRACKET_APERTURE = 2;   // 絞り
    private static final int BRACKET_ISO = 3;         // ISO
    private static final int BRACKET_SHUTTER = 4;    // シャッター
    private static final int BRACKET_WB = 5;          // ホワイトバランス
    private static final int BRACKET_COLOR_TONE = 6; // カラートーン

    private OLYCamera camera;
    private ITakePictureRequestedControl control;
    private boolean isShootingWait = false;
    private boolean isBracketingAction = false;
    private int retryUpdateBracketingStatus = 0;

    private int bracketCount = 0;
    private String targetPropertyName = null;
    private String originalProperty = null;
    private int  originalPropertyIndex = -1;
    private List<String> propertyValueList = null;

    /**
     *　 コンストラクタ
     *
     * @param camera   カメラ
     * @param control  撮影時に必要となる雑多なインタフェース
     */
    BracketingShotControl(OLYCamera camera, ITakePictureRequestedControl control)
    {
        this.control = control;
        this.camera = camera;

    }

    /**
     *　 ブラケッティング対象のプロパティの現在設定値と、その選択肢を記憶する
     *
     * @param name ブラケッティング対象の名前
     * @return  ブラケッティングの現在設定値
     */
    private int prepareBracketProperty(String name)
    {
        try
        {
            targetPropertyName = name;
            originalProperty = camera.getCameraPropertyValue(name);
            propertyValueList = camera.getCameraPropertyValueList(name);
            if (bracketCount < 0)
            {
                bracketCount = propertyValueList.size();
            }
            return (propertyValueList.indexOf(originalProperty));
        }
        catch (Exception e)
        {
            originalProperty = null;
            propertyValueList = null;
            e.printStackTrace();
            System.gc();
        }
        return (-1);
    }


    /**
     *   ブラケッティング対象のプロパティを特定する
     *
     * @param isBracketing プロパティ
     * @return true : 対象の特定完了 / false : 対象の特定失敗
     */
    private boolean decideBracketProperty(int isBracketing)
    {
        bracketCount = control.getAutoBracketingSetting(true);
        switch (isBracketing)
        {
            case BRACKET_EXPREV:
                // 露出ブラケット
                targetPropertyName = CameraPropertyListenerImpl.CAMERA_PROPERTY_EXPOSURE_COMPENSATION;
                break;

            case BRACKET_APERTURE:
                // 絞り値設定
                targetPropertyName = CameraPropertyListenerImpl.CAMERA_PROPERTY_APERTURE_VALUE;
                break;

            case BRACKET_ISO:
                // ISO
                targetPropertyName = CameraPropertyListenerImpl.CAMERA_PROPERTY_ISO_SENSITIVITY;
                break;

            case BRACKET_SHUTTER:
                // シャッターブラケット
                targetPropertyName = CameraPropertyListenerImpl.CAMERA_PROPERTY_SHUTTER_SPEED;
                break;

            case BRACKET_WB:
                // ホワイトバランスブラケット
                targetPropertyName = CameraPropertyListenerImpl.CAMERA_PROPERTY_WHITE_BALANCE;
                bracketCount = -1;
                break;

            case BRACKET_COLOR_TONE:
                // ピクチャーモードブラケット
                targetPropertyName = CameraPropertyListenerImpl.CAMERA_PROPERTY_COLOR_TONE;
                bracketCount = -1;
                break;

            case BRACKET_NONE:
            default:
                // 何もしない
                return (false);
        }
        originalPropertyIndex = prepareBracketProperty(targetPropertyName);
        return (true);
    }


    /**
     *  写真撮影を開始する
     *
     */
    void startShootBracketing()
    {
        if ((camera.isTakingPicture())||(camera.isRecordingVideo())||(isBracketingAction))
        {
            // スチル or ムービー撮影中、ブラケッティング撮影中なら、何もしない
            return;
        }

        // ブラケッティング撮影の準備
        if (!decideBracketProperty(control.getAutoBracketingSetting(false)))
        {
            // ブラケッティング指定ではないので、何もせずに終了する
            return;
        }

        // ブラケッティング撮影開始！ (別スレッドでブラケッティング撮影を開始する）
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(new Runnable()
        {
            @Override
            public void run()
            {
                isBracketingAction = true;
                control.showOtherInformation("BKT");
                try
                {
                    startBracket();
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
                isBracketingAction = false;
                control.showOtherInformation("");
            }
        });
    }

    /**
     *   ブラケッティング撮影を開始する
     *   (これは別スレッドで処理する)
     *
     *      一番小さい選択肢（インデックス）から設定された撮影枚数分、
     *      徐々に選択肢をずらして撮影する。
     *
     */
    private void startBracket()
    {
        int startIndex = originalPropertyIndex - (bracketCount / 2);
        if (startIndex < 0)
        {
            startIndex = 0;
        }
        if ((startIndex + bracketCount) > propertyValueList.size())
        {
            startIndex = propertyValueList.size() - bracketCount;
        }

        PointF afPoint = camera.getActualAutoFocusPoint();
        for (int index = 0; index < bracketCount; index++)
        {
            // 撮影条件を更新する
            updateBracketingStatus(index, startIndex);
            startIndex++;

            try
            {
                // AFポイントを設定する
                if (afPoint != null)
                {
                    camera.setAutoFocusPoint(afPoint);
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            try
            {
                // 写真を撮影する
                camera.takePicture(new HashMap<String, Object>(), this);
                isShootingWait = true;
                while (isShootingWait)
                {
                    // ここで撮影状態が整うまで少し待つ
                    Thread.sleep(BRACKETING_INTERVAL_MILLISECOND);
                    updateShootingWaitStatus();
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        try
        {
            // 変更したプロパティ値を元の値に戻す...ちょっと待ってから
            Thread.sleep(BRACKETING_INTERVAL_MILLISECOND);
            camera.setCameraPropertyValue(targetPropertyName, originalProperty);

            // とにかくAF枠を消す。
            camera.clearAutoFocusPoint();
            control.setFocusFrameStatus(false);

            // AF枠の消去はUIスレッドで動かしてやる
            control.hideFocusFrame(true);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
     *   ブラケッティング撮影の状態を更新する
     *
     * @param index  撮影が終了したカウント（０始まり）
     */
    private void updateBracketingStatus(int index, int currentIndex)
    {
        Log.v(TAG, "updateBracketingStatus() : " + index + "(" + currentIndex + ")");

        // カメラのプロパティ設定を変える
        try
        {
            Thread.sleep(BRACKETING_INTERVAL_MILLISECOND);
            camera.setCameraPropertyValue(targetPropertyName, propertyValueList.get(currentIndex));
        }
        catch (Exception e)
        {
            e.printStackTrace();

            // 頭に来たので、再度呼ぶ (リトライオーバーするまで)
            if (retryUpdateBracketingStatus < BRACKETING_PROPERTY_VALUE_SET_RETRY)
            {
                retryUpdateBracketingStatus++;
                updateBracketingStatus(index, currentIndex);
            }
        }
        retryUpdateBracketingStatus = 0;

        // BKT表示(撮影枚数表示)を変える
        control.showOtherInformation("BKT " + (index + 1));

    }

    /**
     *   カメラの状態を取得し、撮影可能か確認する。
     *   （trueならまだ撮影処理中、falseなら撮影可能）
     */
    private void updateShootingWaitStatus()
    {
        isShootingWait = control.getShootingBusyStatus();
    }

    /**
     *   OLYCamera.TakePictureCallback の実装
     *
     *
     */
    @Override
    public void onProgress(OLYCamera olyCamera, OLYCamera.TakingProgress takingProgress, OLYCameraAutoFocusResult olyCameraAutoFocusResult)
    {
        // 特に何もしないでおこう

    }

    /**
     *   OLYCamera.TakePictureCallback の実装
     *
     */
    @Override
    public void onCompleted()
    {
        // 特に何もしないでおこう

        // 撮影待ち状態の更新
        updateShootingWaitStatus();
    }

    /**
     *   OLYCamera.TakePictureCallback の実装
     *
     * @param e 例外情報
     */
    @Override
    public void onErrorOccurred(Exception e)
    {
         e.printStackTrace();

         // 撮影待ち状態の更新
         updateShootingWaitStatus();
    }

}
