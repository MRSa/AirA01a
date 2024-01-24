@file:Suppress("DEPRECATION")

package jp.osdn.gokigen.aira01a.getimage

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import androidx.fragment.app.FragmentActivity
import com.google.android.material.snackbar.Snackbar
import jp.co.olympus.camerakit.OLYCamera
import jp.co.olympus.camerakit.OLYCamera.DownloadImageCallback
import jp.osdn.gokigen.aira01a.R
import jp.osdn.gokigen.aira01a.playback.OLYCameraContentInfoEx
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

//@Suppress("DEPRECATION")
class JpegDownloaderFromOPC(private val activity: FragmentActivity, private val camera: OLYCamera, private val isGetOnlyInformation: Boolean)  : DownloadImageCallback
{
    private var downloadDialog = ProgressDialog(activity)
    private lateinit var outputDir : File
    private var fileName: String = "" //
    private var isShareContent = false

    private fun getExternalOutputDirectory(): File
    {
        val directoryPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).path + File.separator + activity.getString(R.string.app_name2) + File.separator
        val target = File(directoryPath)
        try
        {
            target.mkdirs()
        }
        catch (e: Exception)
        {
            e.printStackTrace()
        }
        return (target)
    }

    private fun presentMessage(title: String, message: String)
    {
        AlertDialog.Builder(activity).setTitle(title)?.setMessage(message)?.show()
    }

    /**
     * EXIF情報の表示 (ExifInterface を作って、表示クラスに渡す)
     *
     * @param bytes データ並び
     */
    private fun showExifInformation(bytes: ByteArray?)
    {
        try
        {
            if (bytes == null)
            {
                Log.v(TAG, " received data is NULL...")
                return
            }
            val filename = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Calendar.getInstance().time)
            val tempFile = File.createTempFile(filename, null)
            val outStream = FileOutputStream(tempFile.absolutePath)
            outStream.write(bytes)
            outStream.close()

            val exif = ExifInterface(tempFile.absolutePath)
            if (!tempFile.delete())
            {
                Log.v(TAG, "temp file delete failure.")
            }
            activity.runOnUiThread(ExifInfoToShow(activity, exif))
        }
        catch (t: Throwable)
        {
            t.printStackTrace()
        }
    }

    private fun getRotationDegrees(data: ByteArray?, metadata: MutableMap<String, Any>?): Int
    {
        var orientation = ExifInterface.ORIENTATION_UNDEFINED
        if (metadata != null && metadata.containsKey("Orientation"))
        {
            try
            {
                val orientationString = metadata["Orientation"] as String
                orientation = orientationString.toInt()
            }
            catch (e: Exception)
            {
                e.printStackTrace()
            }
        }
        else
        {
            try
            {
                val tempFile = File.createTempFile("temp", null)
                val outStream = FileOutputStream(tempFile.absolutePath)
                outStream.write(data)
                outStream.close()
                val exifInterface = ExifInterface(tempFile.absolutePath)
                orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)
                if (!tempFile.delete())
                {
                    Log.v(TAG, "temp file delete failure.")
                }
            }
            catch (e: Exception)
            {
                e.printStackTrace()
            }
        }
        var degrees = 0
        when (orientation)
        {
            ExifInterface.ORIENTATION_ROTATE_90 -> degrees = 90
            ExifInterface.ORIENTATION_ROTATE_180 -> degrees = 180
            ExifInterface.ORIENTATION_ROTATE_270 -> degrees = 270
            ExifInterface.ORIENTATION_NORMAL -> {
            }
            else -> {
            }
        }
        return degrees
    }

    /**
     * ダウンロードの開始
     *
     */
    fun startDownload(content: OLYCameraContentInfoEx, downloadImageSize: Float, isShare: Boolean)
    {
        val file = content.fileInfo
        val targetFileName = file.filename.toUpperCase(Locale.US)
        val extendName = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Calendar.getInstance().time)
        val periodPosition: Int = targetFileName.indexOf(".")
        val extension: String = targetFileName.substring(periodPosition)
        val baseName: String = targetFileName.substring(0, periodPosition)

        fileName = "$baseName-$extendName$extension".toUpperCase(Locale.US)
        Log.v(TAG, "startDownload() : $fileName")

        isShareContent = isShare
        downloadDialog = ProgressDialog(activity)
        if (isGetOnlyInformation)
        {
            downloadDialog.setTitle(activity.getString(R.string.dialog_get_information_title))
        }
        else
        {
            downloadDialog.setTitle(activity.getString(R.string.dialog_download_title))
        }
        downloadDialog.setMessage(" ${activity.getString(R.string.dialog_download_message)} $fileName")
        downloadDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
        downloadDialog.setCancelable(false)
        downloadDialog.show()

        try
        {
            outputDir = getExternalOutputDirectory()
            val downloadPath = file.directoryPath + File.separator + targetFileName
            Log.v(TAG, "downloadJpegContent : $downloadPath ($fileName) ${outputDir.absolutePath} (SIZE : $downloadImageSize) onlyGetInformation: $isGetOnlyInformation")
            camera.downloadImage(downloadPath, downloadImageSize, this)
        }
        catch (ex: Exception)
        {
            ex.printStackTrace()
        }
    }

    override fun onProgress(progressEvent: OLYCamera.ProgressEvent)
    {
        val percent = (progressEvent.progress * 100.0f).toInt()
        downloadDialog.progress = percent
        //downloadDialog.setCancelable(progressEvent.isCancellable()); // キャンセルできるようにしないほうが良さそうなので
    }

    override fun onCompleted(bytes: ByteArray?, map: MutableMap<String, Any>?)
    {
        if (isGetOnlyInformation)
        {
            // Exif情報をダイアログ表示して終わる
            activity.runOnUiThread { downloadDialog.dismiss() }
            showExifInformation(bytes)
            System.gc()
            return
        }

        try
        {
            val now = System.currentTimeMillis()
            val resolver = activity.contentResolver
            val values = ContentValues()
            values.put(MediaStore.Images.Media.TITLE, fileName)
            values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            //values.put(MediaStore.Images.Media.DATE_ADDED, now)
            //values.put(MediaStore.Images.Media.DATE_MODIFIED, now)
            val extStorageUri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            {
                val path = Environment.DIRECTORY_DCIM + File.separator + activity.getString(R.string.app_name2)
                values.put(MediaStore.Images.Media.DATE_TAKEN, now)
                values.put(MediaStore.Images.Media.RELATIVE_PATH, path)
                values.put(MediaStore.Images.Media.IS_PENDING, true)
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            }
            else
            {
                values.put(MediaStore.Images.Media.DATA, outputDir.absolutePath + File.separator + fileName)
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }
            val imageUri = resolver.insert(extStorageUri, values)
            if (imageUri != null)
            {
                val outputStream = resolver.openOutputStream(imageUri, "wa")
                if (outputStream != null)
                {
                    outputStream.write(bytes)
                    outputStream.flush()
                    outputStream.close()
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                {
                    values.put(MediaStore.Images.Media.IS_PENDING, false)
                    resolver.update(imageUri, values, null, null)
                }

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)
                {
                    var hasGps = false
                    val latLong = FloatArray(2)
                    try
                    {
                        //val directoryPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).path + "/" + activity.getString(R.string.app_name2) + "/"
                        val filepath = File(outputDir, fileName).path
                        val exif = ExifInterface(filepath)
                        hasGps = exif.getLatLong(latLong)
                    }
                    catch (e: java.lang.Exception)
                    {
                        e.printStackTrace()
                    }
                    if (hasGps)
                    {
                        values.put(MediaStore.Images.Media.LATITUDE, latLong[0])
                        values.put(MediaStore.Images.Media.LONGITUDE, latLong[1])
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                    {
                        values.put(MediaStore.Images.Media.ORIENTATION, getRotationDegrees(bytes, map))
                    }
                    try
                    {
                        resolver.update(imageUri, values, null, null)
                    }
                    catch (e: Exception)
                    {
                        e.printStackTrace()
                    }
                }
            }
            else
            {
                Log.v(TAG, " imageUri is NULL...")
            }
            ////////////////////////////////////////////////////////////////
            //if (imageUri != null)
            //{
            //    val cursor = resolver.query(imageUri!!, null, null, null, null)
            //    DatabaseUtils.dumpCursor(cursor)
            //    cursor!!.close()
            //}
            ////////////////////////////////////////////////////////////////

            activity.runOnUiThread {
                try
                {
                    downloadDialog.dismiss()
                    Snackbar.make(activity.findViewById(R.id.fragment1), "${activity.getString(R.string.download_control_save_success)}  $fileName ", Snackbar.LENGTH_SHORT).show()
                    //Toast.makeText(getActivity(), getString(R.string.download_control_save_success) + " " + filename, Toast.LENGTH_SHORT).show();
                    if (imageUri != null)
                    {
                        if (isShareContent)
                        {
                            shareContent(imageUri)
                        }
                    }
                    System.gc()
                }
                catch (e: Exception)
                {
                    e.printStackTrace()
                }
            }

        }
        catch (e: Exception)
        {
            e.printStackTrace()
            val message : String = if (e.message == null) { "" } else { e.message!! }
            activity.runOnUiThread {
                downloadDialog.dismiss()
                presentMessage(activity.getString(R.string.download_control_save_failed), message)
            }
        }
    }

    override fun onErrorOccurred(e: Exception)
    {
        val message : String = if (e.localizedMessage == null) { "" } else { e.localizedMessage!! }
        try
        {
            e.printStackTrace()
        }
        catch (ex: Exception)
        {
            ex.printStackTrace()
        }
        activity.runOnUiThread {
            downloadDialog.dismiss()
            presentMessage(activity.getString(R.string.download_control_download_failed), message)
            System.gc()
        }
        System.gc()
    }

    /**
     * 共有の呼び出し
     *
     * @param imageUri  ファイルのUri
     */
    private fun shareContent(imageUri: Uri)
    {
        val intent = Intent()
        intent.action = Intent.ACTION_SEND
        try
        {
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            intent.type = "image/jpeg"
            intent.putExtra(Intent.EXTRA_STREAM, imageUri)
            activity.startActivityForResult(intent, 0)
        }
        catch (e: Exception)
        {
            e.printStackTrace()
        }
    }

    companion object
    {
        private val TAG = this.toString()
    }
}
