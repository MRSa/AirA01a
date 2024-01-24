package jp.osdn.gokigen.aira01a.playback;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.exifinterface.media.ExifInterface;
import androidx.fragment.app.FragmentActivity;
import jp.co.olympus.camerakit.OLYCamera;
import jp.co.olympus.camerakit.OLYCameraFileInfo;
import jp.osdn.gokigen.aira01a.R;

/**
 *
 *
 */
public class ImageDownloader
{
    private final String TAG = this.toString();
    private final String JPEG_SUFFIX = ".jpg";
    private final FragmentActivity activity;
    private final OLYCamera camera;
    private final MyJpegDownloader jpegDownloader;
    private final MyMovieDownloader movieDownloader;
    private Callback callback = null;
    private int successCount;
    private int failureCount;
    private int currentCount;
    private int loopCount;
    private int maxCount;
    private  List<OLYCameraContentInfoEx> contentList;
    private float imageSize;
    private boolean getWithRaw;
    private boolean getRaw;
    private boolean requestAbort;

    public interface Callback
    {
        void finishedDownloadMulti(boolean isAbort, int successCount, int failureCount);
    }

    ImageDownloader(FragmentActivity activity, OLYCamera camera)
    {
        this.activity = activity;
        this.camera = camera;
        jpegDownloader  = new MyJpegDownloader();
        movieDownloader = new MyMovieDownloader();
    }

    void startDownloadMulti(@NonNull List<OLYCameraContentInfoEx> contentList, float imageSize, boolean getWithRaw, Callback callback)
    {
        this.contentList = contentList;
        this.imageSize = imageSize;
        this.getWithRaw = getWithRaw;
        this.callback = callback;
        this.successCount = 0;
        this.failureCount = 0;
        this.currentCount = 0;
        this.loopCount = 1;
        this.maxCount = contentList.size() * (getWithRaw ? 2 : 1);
        this.getRaw = false;
        this.requestAbort = false;
        try
        {
            // 再生モードかどうかを確認して、再生モードでなかった場合には再生モードに切り替える。
            // (なぜか落ちていることがある...)
            OLYCamera.RunMode runMode = camera.getRunMode();
            if (runMode != OLYCamera.RunMode.Playback)
            {
                Thread thread = new Thread() {
                    @Override
                    public void run() {
                        try
                        {
                            Log.v(TAG, "changeRunMode(OLYCamera.RunMode.Playback) : Start");
                            camera.changeRunMode(OLYCamera.RunMode.Playback);
                            Log.v(TAG, "changeRunMode(OLYCamera.RunMode.Playback) : End");
                        }
                        catch (Exception e)
                        {
                            e.printStackTrace();
                        }
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                kickDownloadImage();
                            }
                        });
                    }
                };
                thread.start();
            } else {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        kickDownloadImage();
                    }
                });
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
     *   ダウンロードの開始...
     */
    private void kickDownloadImage()
    {
        try
        {
            Calendar calendar = Calendar.getInstance();
            String appendName = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(calendar.getTime());

            OLYCameraContentInfoEx contentInfo = contentList.get(currentCount);
            String fileName = contentInfo.getFileInfo().getFilename().toLowerCase();

            if (getRaw)
            {
                // RAWの取得ターンになっていた場合の処理
                if (contentInfo.hasRaw())
                {
                    // RAWファイルを持っていたら、RAWファイルを取得する
                    // fileName = fileName.replace(JPEG_SUFFIX, RAW_SUFFIX);
                    movieDownloader.startDownload(appendName, contentInfo);
                }
                else
                {
                    finishedDownload(true, true);
                }
                return;
            }
            if (fileName.endsWith(JPEG_SUFFIX))
            {
                jpegDownloader.startDownload(appendName, contentInfo, imageSize);
            }
            else
            {
                movieDownloader.startDownload(appendName, contentInfo);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private void finishedDownload(boolean isSuccess, boolean skip)
    {
        // ダウンロード回数をカウントアップする。
        if (!skip)
        {
            if (isSuccess)
            {
                successCount++;
            }
            else
            {
                failureCount++;
            }
        }
        loopCount++;
        currentCount++;
        if (requestAbort)
        {
            // ダウンロードの中断が指示された。終了とする
            if (callback != null)
            {
                callback.finishedDownloadMulti(true, successCount, failureCount);
            }
            return;
        }

        if (contentList.size() > currentCount)
        {
            // 次のダウンロードに進む
            kickDownloadImage();
            return;
        }

        // 枚数オーバー
        if ((getWithRaw)&&(!getRaw))
        {
            // 二順目、RAWの取得を行う。
            getRaw = true;
            currentCount = 0;
            kickDownloadImage();
            return;
        }

        // ダウンロード完了！
        if (callback != null)
        {
            callback.finishedDownloadMulti(false, successCount, failureCount);
        }
    }

    /**
     *  JPEGファイルのダウンロード
     */
    private class MyJpegDownloader implements OLYCamera.DownloadImageCallback
    {
        private ProgressDialog downloadDialog = null;
        private String filename = null;

        /**
         * コンストラクタ
         */
        MyJpegDownloader()
        {
            //
        }

        /**
         * 静止画のダウンロード開始指示
         */
        void startDownload(@NonNull String extendName, @NonNull OLYCameraContentInfoEx content, float downloadImageSize)
        {

            String originalFileName = content.getFileInfo().getFilename();
            int periodPosition = originalFileName.indexOf(".");
            String extension = originalFileName.substring(periodPosition);
            String baseFileName = originalFileName.substring(0, periodPosition);
            this.filename =  baseFileName + "-" + extendName + extension;

            Log.v(TAG, "startDownload() " + filename);
            downloadDialog = new ProgressDialog(activity);
            downloadDialog.setTitle(activity.getString(R.string.dialog_download_title) + " (" + loopCount  + " / " + maxCount + ")");
            downloadDialog.setMessage(activity.getString(R.string.dialog_download_message) + " " + filename);
            downloadDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            downloadDialog.setCancelable(false);
            downloadDialog.show();

            // Download the image.
            try
            {
                OLYCameraFileInfo file = content.getFileInfo();
                String path = file.getDirectoryPath() + "/" + file.getFilename();
                camera.downloadImage(path, downloadImageSize, this);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

        /**
         * 進行中の表示 (進捗バーの更新)
         *
         * @param progressEvent 進捗情報
         */
        @Override
        public void onProgress(OLYCamera.ProgressEvent progressEvent)
        {
            if (downloadDialog != null)
            {
                int percent = (int) (progressEvent.getProgress() * 100.0f);
                downloadDialog.setProgress(percent);
                //downloadDialog.setCancelable(progressEvent.isCancellable()); // キャンセルできるようにしないほうが良さそうなので
            }
        }

        /**
         * ファイル受信終了時の処理
         *
         * @param bytes 受信バイト数
         * @param map   ファイルの情報
         */
        @SuppressWarnings("deprecation")
        @SuppressLint("InlinedApi")
        @Override
        public void onCompleted(byte[] bytes, Map<String, Object> map)
        {
            final String directoryPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getPath() + "/" + activity.getString(R.string.app_name2) + "/";
            final String filepath = new File(directoryPath.toLowerCase(), filename).getPath();

            // ファイルを保存する
            try
            {
                final File outputDir = new File(directoryPath);
                if (!outputDir.exists())
                {
                    if (!outputDir.mkdirs())
                    {
                        Log.v(TAG, "MKDIR FAIL. : " + directoryPath);
                    }
                }

                ContentResolver resolver = activity.getContentResolver();

                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.TITLE, filename);
                values.put(MediaStore.Images.Media.DISPLAY_NAME, filename);
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");

                Uri extStorageUri;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                {
                    String path = Environment.DIRECTORY_DCIM + File.separator + activity.getString(R.string.app_name2);
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
                        outputStream.write(bytes);
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

                    boolean hasGps = false;
                    float[] latLong = new float[2];
                    try
                    {
                        //
                        ExifInterface exif = new ExifInterface(filepath);
                        hasGps = exif.getLatLong(latLong);
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }

                    if (hasGps)
                    {
                        values.put(MediaStore.Images.Media.LATITUDE, latLong[0]);
                        values.put(MediaStore.Images.Media.LONGITUDE, latLong[1]);
                    }
                    values.put(MediaStore.Images.Media.ORIENTATION, getRotationDegrees(bytes, map));
                    try
                    {
                        if (imageUri != null)
                        {
                            resolver.update(imageUri, values, null, null);
                        }
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (downloadDialog != null)
                        {
                            downloadDialog.dismiss();
                        }
                        downloadDialog = null;
                        finishedDownload(false, false);
                    }
                });
                // ダウンロード失敗時(保存失敗)には、ギャラリーにデータ登録を行わない。
                return;
            }

            try
            {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (downloadDialog != null)
                        {
                            downloadDialog.dismiss();
                        }
                        downloadDialog = null;
                        finishedDownload(true, false);
                    }
                });
            }
            catch (Exception e)
            {
                e.printStackTrace();
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (downloadDialog != null)
                        {
                            downloadDialog.dismiss();
                        }
                        downloadDialog = null;
                        finishedDownload(true, false);
                    }
                });
            }
        }

        /**
         * エラー発生時の処理
         *
         * @param e エラーの情報
         */
        @Override
        public void onErrorOccurred(Exception e)
        {
            e.printStackTrace();
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (downloadDialog != null)
                    {
                        downloadDialog.dismiss();
                    }
                    downloadDialog = null;
                    finishedDownload(false, false);
                }
            });
        }
    }

    /**
     * 動画(とRAWファイル)のダウンロード
     */
    private class MyMovieDownloader implements OLYCamera.DownloadLargeContentCallback
    {
        private ProgressDialog downloadDialog = null;
        private String filename = null;
        private OutputStream outputStream = null;
        private Uri imageUri = null;

        /**
         * コンストラクタ
         *
         */
        MyMovieDownloader()
        {
            //
        }

        /**
         * ダウンロードの開始
         */
        void startDownload(@NonNull String extendName, @NonNull OLYCameraContentInfoEx content)
        {
            String targetFileName = null;
            String RAW_SUFFIX = ".orf";
            try
            {
                OLYCameraFileInfo file = content.getFileInfo();
                targetFileName = file.getFilename().toLowerCase();
                if (content.hasRaw())
                {
                    targetFileName = targetFileName.replace(JPEG_SUFFIX, RAW_SUFFIX);
                }

                int periodPosition = targetFileName.indexOf(".");
                String extension = targetFileName.substring(periodPosition);
                String baseFileName = targetFileName.substring(0, periodPosition);
                this.filename =  baseFileName + "-" + extendName + extension;
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

            Log.v(TAG, "startDownload() " + filename);
            downloadDialog = new ProgressDialog(activity);
            downloadDialog.setTitle(activity.getString(R.string.dialog_download_file_title) + " (" + loopCount + " / " + maxCount + ")");
            downloadDialog.setMessage(activity.getString(R.string.dialog_download_message) + " " + filename);
            downloadDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            downloadDialog.setCancelable(false);
            downloadDialog.show();

            // Download the image.
            try
            {
                OLYCameraFileInfo file = content.getFileInfo();
                if (targetFileName == null)
                {
                    targetFileName = file.getFilename().toLowerCase();
                }
                String filePath = file.getDirectoryPath() + "/" + targetFileName;
                Log.v(TAG, "downloadLargeContent : " + filePath);
                camera.downloadLargeContent(filePath, this);

                final String directoryPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getPath() + "/" + activity.getString(R.string.app_name2) + "/";
                //filepath = new File(directoryPath.toLowerCase(), filename).getPath();
                try
                {
                    final File directory = new File(directoryPath);
                    if (!directory.exists())
                    {
                        if (!directory.mkdirs())
                        {
                            Log.v(TAG, "MKDIR FAIL. : " + directoryPath);
                        }
                    }

                    boolean isVideo = false;
                    ContentResolver resolver = activity.getContentResolver();
                    ContentValues values = new ContentValues();
                    values.put(MediaStore.Images.Media.TITLE, filename);
                    values.put(MediaStore.Images.Media.DISPLAY_NAME, filename);
                    if (filename.endsWith(RAW_SUFFIX))
                    {
                        values.put(MediaStore.Images.Media.MIME_TYPE, "image/x-olympus-orf");
                    }
                    else
                    {
                        values.put(MediaStore.Images.Media.MIME_TYPE, "video/mp4");
                        isVideo = true;
                    }

                    Uri extStorageUri;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                    {
                        String path = Environment.DIRECTORY_DCIM + File.separator + activity.getString(R.string.app_name2);
                        values.put(MediaStore.Images.Media.RELATIVE_PATH, path);
                        values.put(MediaStore.Images.Media.IS_PENDING, true);
                        extStorageUri = (isVideo) ? MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY) : MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
                    }
                    else
                    {
                        values.put(MediaStore.Images.Media.DATA, directory.getAbsolutePath() + File.separator + filename);
                        extStorageUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                    }

                    imageUri = resolver.insert(extStorageUri, values);
                    if (imageUri != null)
                    {
                        outputStream = resolver.openOutputStream(imageUri, "wa");
                    }
                }
                catch (Exception e)
                {
                    outputStream = null;
                    imageUri = null;
                    e.printStackTrace();
                    activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run()
                            {
                                if (downloadDialog != null)
                                {
                                    downloadDialog.dismiss();
                                }
                                downloadDialog = null;
                                finishedDownload(false, false);
                            }
                    });
                }
            }
            catch (Exception ex)
            {
                ex.printStackTrace();
            }
        }

        @Override
        public void onProgress(byte[] bytes, OLYCamera.ProgressEvent progressEvent)
        {
            try
            {
                if (downloadDialog != null)
                {
                    int percent = (int) (progressEvent.getProgress() * 100.0f);
                    downloadDialog.setProgress(percent);
                    //downloadDialog.setCancelable(progressEvent.isCancellable()); // キャンセルできるようにしないほうが良さそうなので
                }

                if (outputStream != null)
                {
                    outputStream.write(bytes);
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

        @Override
        public void onCompleted()
        {
            try
            {
                if (outputStream != null)
                {
                    outputStream.flush();
                    outputStream.close();
                    outputStream = null;
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                {
                    ContentResolver resolver = activity.getContentResolver();
                    ContentValues values = new ContentValues();
                    values.put(MediaStore.Images.Media.TITLE, filename);
                    values.put(MediaStore.Images.Media.DISPLAY_NAME, filename);
                    values.put(MediaStore.Images.Media.IS_PENDING, false);
                    if (resolver != null)
                    {
                        resolver.update(imageUri, values, null, null);
                        imageUri = null;
                    }
                }
                activity.runOnUiThread(new Runnable()
                {
                    @Override
                    public void run() {
                        if (downloadDialog != null)
                        {
                            downloadDialog.dismiss();
                        }
                        downloadDialog = null;
                        finishedDownload(true, false);
                    }
                });
            }
            catch (Exception e)
            {
                //final String message = e.getMessage();
                e.printStackTrace();
                if (activity != null)
                {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (downloadDialog != null) {
                                downloadDialog.dismiss();
                            }
                            downloadDialog = null;
                            finishedDownload(false, false);
                        }
                    });
                }
            }
        }

        @Override
        public void onErrorOccurred(Exception e)
        {
            e.printStackTrace();
            try
            {
                if (outputStream != null)
                {
                    outputStream.flush();
                    outputStream.close();
                    outputStream = null;
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                {
                    ContentResolver resolver = activity.getContentResolver();
                    ContentValues values = new ContentValues();
                    values.put(MediaStore.Images.Media.TITLE, filename);
                    values.put(MediaStore.Images.Media.DISPLAY_NAME, filename);
                    values.put(MediaStore.Images.Media.IS_PENDING, false);
                    if (resolver != null)
                    {
                        resolver.update(imageUri, values, null, null);
                        imageUri = null;
                    }
                }
            }
            catch (Exception ex)
            {
                ex.printStackTrace();
            }
            if (activity != null)
            {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (downloadDialog != null) {
                            downloadDialog.dismiss();
                        }
                        downloadDialog = null;
                        finishedDownload(false, false);
                    }
                });
            }
        }
    }

    private int getRotationDegrees(byte[] data, Map<String, Object> metadata)
    {
        int degrees = 0;
        int orientation = ExifInterface.ORIENTATION_UNDEFINED;

        if ((metadata != null)&&(metadata.containsKey("Orientation")))
        {
            try
            {
                String ori = (String) metadata.get("Orientation");
                if (ori != null)
                {
                    orientation = Integer.parseInt(ori);
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        else
        {
            // Gets image orientation to display a picture.
            try
            {
                File tempFile = File.createTempFile("temp", null);
                {
                    FileOutputStream outStream = new FileOutputStream(tempFile.getAbsolutePath());
                    outStream.write(data);
                    outStream.close();
                }
                ExifInterface exifInterface = new ExifInterface(tempFile.getAbsolutePath());
                orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
                if (!tempFile.delete())
                {
                    Log.v(TAG, "temp file delete failure.");
                }
            }
            catch (IOException e)
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
                //degrees = 0;
                break;
        }
        return (degrees);
    }
}
