package jp.osdn.gokigen.aira01a.liveview;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import jp.osdn.gokigen.aira01a.R;

/**
 *   画像の保管クラス
 *
 */
public class StoreImage implements IStoreImage
{
    private final String TAG = toString();
    private final Context context;
    private Activity activity = null;

    public StoreImage(Context context)
    {
        this.context = context;
    }

    @Override
    public void doStore(final Bitmap target, final Location location, final boolean isShare)
    {
        // 保存処理(プログレスダイアログ（「保存中...」）を表示して処理する)
        try
        {
            final ProgressDialog saveDialog = new ProgressDialog(context);
            saveDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            saveDialog.setMessage(context.getString(R.string.data_saving));
            saveDialog.setIndeterminate(true);
            saveDialog.setCancelable(false);
            saveDialog.show();
            Thread thread = new Thread(new Runnable()
            {
                public void run()
                {
                    System.gc();
                    saveImageImpl(target, location, isShare);
                    System.gc();
                    saveDialog.dismiss();
                }
            });
            try
            {
                thread.start();
            }
            catch (Throwable t)
            {
                t.printStackTrace();
                System.gc();
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void setActivity(Activity activity)
    {
        this.activity = activity;
    }

    private void saveImageImpl(Bitmap targetImage, Location location, boolean isShare)
    {
        try
        {
            if (saveImageExternalImpl(targetImage, location, isShare))
            {
                return;
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        // 失敗した時は、ローカルのディレクトリに保存するのみ。
        saveImageLocalImpl(targetImage);
    }

    /**
     *   ビットマップイメージをファイルに出力する
     *
     * @param targetImage  出力するビットマップイメージ
     */
    private void saveImageLocalImpl(Bitmap targetImage)
    {
        try
        {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US);
            String filename =  "L" + dateFormat.format(System.currentTimeMillis()) + ".jpg";
            File mediaDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            boolean mkdirs = true;
            if (mediaDir != null)
            {
                try
                {
                    mkdirs = mediaDir.mkdirs();
                    if (!mkdirs)
                    {
                        Log.v(TAG, " MKDIRS RETURNS FALSE : " + mediaDir.getPath());
                    }
                    mkdirs = true;
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                    mkdirs = false;
                }
            }
            if (!mkdirs)
            {
                mediaDir = context.getFilesDir();
            }
            File photoFile = new File(mediaDir, filename);
            FileOutputStream outputStream = new FileOutputStream(photoFile);
            targetImage.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            outputStream.flush();
            outputStream.close();

            if (mediaDir != null)
            {
                Log.v(TAG, "  >>> STORED IMAGE : " + mediaDir.getPath() + filename);
            }
        }
        catch (Throwable t)
        {
            t.printStackTrace();
        }
    }


    /**
     *   ビットマップイメージをファイルに出力する
     *
     * @param targetImage  出力するビットマップイメージ
     */
    private boolean saveImageExternalImpl(Bitmap targetImage, Location location, boolean isShare)
    {
        boolean ret = false;
        try
        {
            Calendar calendar = Calendar.getInstance();
            final String directoryPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getPath() + "/" + context.getString(R.string.app_name2) + "/";
            String filename = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(calendar.getTime()) + "_lv.jpg";
            //String filepath = new File(directoryPath.toLowerCase(), filename).getPath();
            final File outputDir = new File(directoryPath);
            if (!outputDir.exists())
            {
                if (!outputDir.mkdirs())
                {
                    Log.v(TAG, "MKDIR FAIL. : " + directoryPath);
                }
            }

            ContentResolver resolver = context.getContentResolver();

            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.TITLE, filename);
            values.put(MediaStore.Images.Media.DISPLAY_NAME, filename);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");

            Uri extStorageUri;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            {
                String path = Environment.DIRECTORY_DCIM + File.separator + context.getString(R.string.app_name2);
                values.put(MediaStore.Images.Media.RELATIVE_PATH, path);
                values.put(MediaStore.Images.Media.IS_PENDING, true);
                extStorageUri = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
            }
            else
            {
                values.put(MediaStore.Images.Media.DATA, outputDir.getAbsolutePath() + File.separator + filename);
                extStorageUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            }

            Uri imageUri = resolver.insert(extStorageUri, values);
            if (imageUri != null)
            {
                OutputStream outputStream = resolver.openOutputStream(imageUri, "wa");
                if (outputStream != null)
                {
                    targetImage.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                    outputStream.flush();
                    outputStream.close();
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                {
                    values.put(MediaStore.Images.Media.IS_PENDING, false);
                    resolver.update(imageUri, values, null, null);
                }
            }
            else
            {
                Log.v(TAG, " cannot get imageUri...");
            }

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)
            {
                if (location != null)
                {
                    // 位置情報を入れる
                    values.put(MediaStore.Images.Media.LATITUDE, location.getLatitude());
                    values.put(MediaStore.Images.Media.LONGITUDE, location.getLongitude());
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
                {
                    values.put(MediaStore.Images.Media.WIDTH, targetImage.getWidth());
                    values.put(MediaStore.Images.Media.HEIGHT, targetImage.getHeight());
                }
                final Uri pictureUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                if (isShare)
                {
                    shareContent(pictureUri);
                }
            }
            ret = true;
        }
        catch (Throwable t)
        {
            t.printStackTrace();
        }
        return (ret);
    }

/*
    //
    //ビットマップイメージをファイルに出力する (ScopedStorage未対応版)
    //
    private boolean saveImageExternalImplLegacy(Bitmap targetImage, Location location, boolean isShare)
    {
        boolean ret = false;
        try
        {
            Calendar calendar = Calendar.getInstance();
            final String directoryPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getPath() + "/" + context.getString(R.string.app_name2) + "/";
            String filename = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(calendar.getTime()) + "_lv.jpg";
            String filepath = new File(directoryPath.toLowerCase(), filename).getPath();

            final File directory = new File(directoryPath);
            if (!directory.exists())
            {
                if (!directory.mkdirs())
                {
                    Log.v(TAG, "MKDIR FAIL. : " + directoryPath);
                }
            }
            FileOutputStream outputStream = new FileOutputStream(filepath);
            targetImage.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            outputStream.flush();
            outputStream.close();

            long now = System.currentTimeMillis();
            ContentValues values = new ContentValues();
            ContentResolver resolver = context.getContentResolver();
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
            values.put(MediaStore.Images.Media.DATA, filepath);
            values.put(MediaStore.Images.Media.DATE_ADDED, now);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            {
                values.put(MediaStore.Images.Media.DATE_TAKEN, now);
            }
            values.put(MediaStore.Images.Media.DATE_MODIFIED, now);
            if (location != null)
            {
                // 位置情報を入れる
                values.put(MediaStore.Images.Media.LATITUDE, location.getLatitude());
                values.put(MediaStore.Images.Media.LONGITUDE, location.getLongitude());
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
            {
                values.put(MediaStore.Images.Media.WIDTH, targetImage.getWidth());
                values.put(MediaStore.Images.Media.HEIGHT, targetImage.getHeight());
            }
            final Uri pictureUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (isShare)
            {
                shareContent(pictureUri);
            }
            ret = true;
        }
        catch (Throwable t)
        {
            t.printStackTrace();
        }
        return (ret);
    }
*/

    /**
     *   共有の呼び出し
     *
     * @param pictureUri  画像ファイル名
     */
    private void shareContent(final Uri pictureUri)
    {
        // activity が nullなら、共有はしない
        if (activity == null)
        {
            return;
        }
        try
        {
            activity.runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    Intent intent = new Intent();
                    intent.setAction(Intent.ACTION_SEND);
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    intent.setType("image/jpeg");
                    intent.putExtra(Intent.EXTRA_STREAM, pictureUri);
                    activity.startActivityForResult(intent, 0);
                }
            });
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
