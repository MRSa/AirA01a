package jp.osdn.gokigen.aira01a.preview;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.widget.ImageView;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Map;

import androidx.exifinterface.media.ExifInterface;
import jp.co.olympus.camerakit.OLYCamera;
import jp.osdn.gokigen.aira01a.olycamera.RecordingSupportsListenerImpl;

/**
 *   撮影後画像の描画（表示）に特化したクラス
 *
 * Created by MRSa on 2016/04/29.
 */
public class RecordedImageDrawer
{
    //private final String TAG = this.toString();

    private Activity parent = null;
    private RecordingSupportsListenerImpl listener;
    private OLYCamera camera = null;
    private ImageView imageView;
    private byte[] data;
    private Map<String, Object> metadata;

    /**
     * コンストラクタ : コンストラクタでは何もしない
     *
     * @param targetView ImageView クラス
     *
     */
    RecordedImageDrawer(ImageView targetView)
    {
        //
        this.imageView = targetView;
        this.listener = new RecordingSupportsListenerImpl(this, null);
    }

    /**
     *   カメラ関係の設定を更新する
     *
     * @param camera    OLYCamera
     * @param activity  Activity
     */
    void setImageArea(OLYCamera camera, Activity activity)
    {
        this.camera = camera;
        this.parent = activity;
    }

    /**
     *   描画領域を設定する
     *
     *   @param targetView  描画領域
     *
     */
    void setTargetView(ImageView targetView)
    {
        this.imageView = targetView;
    }

    /**
     *   表示する画像を受け取る
     *
     * @param data        データ
     * @param metadata   メタデータ
     */
    void setImageData(byte[] data, Map<String, Object> metadata)
    {
        this.data = data;
        this.metadata = metadata;
    }

    /**
     *   画像描画の開始
     *
     */
    void startDrawing()
    {
        if (camera != null)
        {
            camera.setRecordingSupportsListener(listener);
            doDraw();
        }
    }

    /**
     *   画像描画の終了
     *
     */
    void stopDrawing()
    {
        if (camera != null)
        {
            camera.setRecordingSupportsListener(null);
        }
    }

    /**
     *   ビットマップデータを貰って画像描画を実行する
     *
     * @param camera　　　OLYCamera
     * @param data　　　　イメージデータ
     * @param metadata   メタデータ
     */
    public void onReceiveCapturedImagePreview(OLYCamera camera, byte[] data, Map<String, Object> metadata)
    {
        this.data = data;
        this.metadata = metadata;
        if (parent != null)
        {
            parent.runOnUiThread(new Runnable() {
                @Override
                public void run()
                {
                    doDraw();
                }
            });
        }
    }

    private void doDraw()
    {
        if (imageView != null)
        {
            imageView.setImageBitmap(createRotatedBitmap(data, metadata));
        }
    }

    private Bitmap createRotatedBitmap(byte[] data, Map<String, Object> metadata)
    {
        Bitmap bitmap = null;
        try
        {
            bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
        }
        catch (OutOfMemoryError e)
        {
            e.printStackTrace();
            System.gc();
        }
        if (bitmap == null)
        {
            return null;
        }

        int degrees = getRotationDegrees(data, metadata);
        if (degrees != 0) {
            Matrix m = new Matrix();
            m.postRotate(degrees);
            try {
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);
            }
            catch (OutOfMemoryError e)
            {
                e.printStackTrace();
                System.gc();
            }
        }
        return bitmap;
    }


    private int getRotationDegrees(byte[] data, Map<String, Object> metadata)
    {
        int degrees;
        int orientation = ExifInterface.ORIENTATION_UNDEFINED;

        if (metadata != null && metadata.containsKey("Orientation"))
        {
            String orientationStr = (String) metadata.get("Orientation");
            if (orientationStr != null)
            {
                orientation = Integer.parseInt(orientationStr);
            }
        }
        else
        {
            // Gets image orientation to display a picture.
            try {
                File tempFile = File.createTempFile("temp", null);
                {
                    FileOutputStream outStream = new FileOutputStream(tempFile.getAbsolutePath());
                    outStream.write(data);
                    outStream.close();
                }

                ExifInterface exifInterface = new ExifInterface(tempFile.getAbsolutePath());
                orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);

                tempFile.delete();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

        switch (orientation)
        {
            case ExifInterface.ORIENTATION_ROTATE_90:
                degrees = 90;
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                degrees = 180;
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                degrees = 270;
                break;
            case ExifInterface.ORIENTATION_NORMAL:
            default:
                degrees = 0;
                break;
        }
        return degrees;
    }
}
