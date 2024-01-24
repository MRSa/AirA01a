package jp.osdn.gokigen.aira01a.liveview;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.PointF;

import androidx.preference.PreferenceManager;
import jp.co.olympus.camerakit.OLYCamera;

/**
 *   ライブビューの拡大・縮小
 *
 */
class LiveViewMagnifingControl implements ILiveViewMagnifyControl
{
    private final Context context;
    private final OLYCamera camera;
    private  OLYCamera.MagnifyingLiveViewScale liveViewScale = OLYCamera.MagnifyingLiveViewScale.X5;

    /**
     *   コンストラクタ
     *
     */
    LiveViewMagnifingControl(Context context, OLYCamera camera)
    {
        this.context = context;
        this.camera = camera;
    }

    /**
     *   ライブビューの拡大倍率を変更する
     *
     */
    @Override
    public float changeLiveViewMagnifyScale()
    {
        float scale = 1.0f;
        try
        {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            String value = preferences.getString("live_view_scale", "STEP");

            // 段階的にライブビュー表示の拡大を行う
            if (value.equals("STEP"))
            {
                return (changeLiveViewMagnifyScaleStep());
            }

            // ワンプッシュで拡大と通常表示を切り替える
            if (!camera.isMagnifyingLiveView())
            {
                // ライブビューの拡大を行っていない場合は、ライブビューの拡大を行う
                liveViewScale = decideLiveViewScale(value);
                camera.startMagnifyingLiveViewAtPoint(new PointF(0.5f, 0.5f), liveViewScale);
                scale = Float.parseFloat(value);
            }
            else
            {
                // ライブビューの拡大をやめる
                camera.stopMagnifyingLiveView();
                //scale = 1.0f;
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return (scale);
    }

    @Override
    public float isLiveViewMagnify()
    {
        try
        {
            if (camera.isMagnifyingLiveView())
            {
                return (decideLiveViewScale(liveViewScale));
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return (1.0f);
    }

    /**
     *   ライブビューの拡大倍率を返す
     *
     *
     * @param value  拡大倍率
     * @return  拡大倍率（制御用）
     */
    private OLYCamera.MagnifyingLiveViewScale decideLiveViewScale(final String value)
    {
        OLYCamera.MagnifyingLiveViewScale scale = OLYCamera.MagnifyingLiveViewScale.X5;
        if (value == null)
        {
            return (scale);
        }
        switch (value)
        {
            case "7.0":
                scale =  OLYCamera.MagnifyingLiveViewScale.X7;
                break;
            case "10.0":
                scale =  OLYCamera.MagnifyingLiveViewScale.X10;
                break;
            case "14.0":
                scale =  OLYCamera.MagnifyingLiveViewScale.X14;
                break;
            case "5.0":
            default:
                scale =  OLYCamera.MagnifyingLiveViewScale.X5;
                break;
        }
        return (scale);
    }

    private float decideLiveViewScale(final OLYCamera.MagnifyingLiveViewScale scale)
    {
        float value = 1.0f;
        if (scale == OLYCamera.MagnifyingLiveViewScale.X7)
        {
            value = 7.0f;
        } else if (scale == OLYCamera.MagnifyingLiveViewScale.X10)
        {
            value = 10.0f;
        } else if (scale == OLYCamera.MagnifyingLiveViewScale.X14)
        {
            value = 14.0f;
        } else if (scale == OLYCamera.MagnifyingLiveViewScale.X5)
        {
            value = 5.0f;
        }
        return (value);
    }

    private float changeLiveViewMagnifyScaleStep()
    {
        float scale = 1.0f;
        try
        {
            // ライブビューの拡大を行っていない場合...
            if (!camera.isMagnifyingLiveView())
            {
                // ライブビューの拡大開始
                liveViewScale = OLYCamera.MagnifyingLiveViewScale.X5;
                camera.startMagnifyingLiveViewAtPoint(new PointF(0.5f, 0.5f), liveViewScale);
                return (5.0f);
            }
            // ライブビューの最大拡大中...
            if (liveViewScale == OLYCamera.MagnifyingLiveViewScale.X14)
            {
                // ライブビューの拡大終了
                liveViewScale = OLYCamera.MagnifyingLiveViewScale.X5;
                camera.stopMagnifyingLiveView();
                return (1.0f);
            }

            // ライブビューの拡大倍率を変えていく  ( x5 → x7 → x10 → x14 )
            if (liveViewScale == OLYCamera.MagnifyingLiveViewScale.X5)
            {
                liveViewScale = OLYCamera.MagnifyingLiveViewScale.X7;
                scale = 7.0f;
            }
            else if (liveViewScale == OLYCamera.MagnifyingLiveViewScale.X7)
            {
                liveViewScale = OLYCamera.MagnifyingLiveViewScale.X10;
                scale = 10.0f;
            }
            else // if (liveViewScale == OLYCamera.MagnifyingLiveViewScale.X10)
            {
                liveViewScale = OLYCamera.MagnifyingLiveViewScale.X14;
                scale = 14.0f;
            }
            camera.changeMagnifyingLiveViewScale(liveViewScale);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return (scale);
    }
}
