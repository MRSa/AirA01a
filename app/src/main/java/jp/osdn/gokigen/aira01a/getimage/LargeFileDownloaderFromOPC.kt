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
import androidx.fragment.app.FragmentActivity
import com.google.android.material.snackbar.Snackbar
import jp.co.olympus.camerakit.OLYCamera
import jp.co.olympus.camerakit.OLYCamera.DownloadLargeContentCallback
import jp.osdn.gokigen.aira01a.R
import jp.osdn.gokigen.aira01a.playback.OLYCameraContentInfoEx
import java.io.File
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*


/**
 *   でっかいファイルをダウンロードするための処理クラス
 *
 *
 */
//@Suppress("DEPRECATION")
class LargeFileDownloaderFromOPC(private val activity: FragmentActivity, private val camera: OLYCamera)  : DownloadLargeContentCallback
{
    @Suppress("DEPRECATION")
    private var downloadDialog = ProgressDialog(activity)
    private var fileName: String = "" //
    private var isShareContent = false
    private var outputStream: OutputStream? = null
    private var imageUri: Uri? = null
    private var receivedSize : Long = 0

    private fun getExternalOutputDirectory(): File
    {
        @Suppress("DEPRECATION") val directoryPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).path + File.separator + activity.getString(R.string.app_name2) + File.separator
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
     * 共有の呼び出し
     *
     * @param fileUri         : ファイルのUri
     * @param targetFileName  : 共有するファイル名
     */
    private fun shareContent(fileUri: Uri, targetFileName: String)
    {
        val intent = Intent()
        intent.action = Intent.ACTION_SEND
        try
        {
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (targetFileName.endsWith(".ORF"))
            {
                intent.type = "image/x-olympus-orf"
            }
            else
            {
                intent.type = "video/mp4"
            }
            intent.putExtra(Intent.EXTRA_STREAM, fileUri)
            activity.startActivityForResult(intent, 0)
        }
        catch (e: Exception)
        {
            e.printStackTrace()
        }
    }

    /**
     * ダウンロードの開始
     *
     */
    @Suppress("DEPRECATION")
    fun startDownload(content: OLYCameraContentInfoEx, isShare: Boolean)
    {
        val file = content.fileInfo
        var isVideo = false
        var targetFileName = file.filename.toUpperCase(Locale.US)
        if (content.hasRaw())
        {
            targetFileName = targetFileName.replace(".JPG", ".ORF")
        }
        val extendName = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Calendar.getInstance().time)
        val periodPosition: Int = targetFileName.indexOf(".")
        val extension: String = targetFileName.substring(periodPosition)
        val baseName: String = targetFileName.substring(0, periodPosition)

        receivedSize = 0
        fileName = "$baseName-$extendName$extension".toUpperCase(Locale.US)
        Log.v(TAG, "startDownload() : $fileName")

        isShareContent = isShare
        downloadDialog = ProgressDialog(activity)
        downloadDialog.setTitle(activity.getString(R.string.dialog_download_file_title))
        downloadDialog.setMessage(" ${activity.getString(R.string.dialog_download_message)} $fileName")
        downloadDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
        downloadDialog.setCancelable(false)
        downloadDialog.show()

        try
        {
            val now = System.currentTimeMillis()
            val outputDir = getExternalOutputDirectory()
            val downloadPath = file.directoryPath + File.separator + targetFileName
            Log.v(TAG, "downloadLargeContent : $downloadPath ($fileName)")

            try
            {
                val resolver = activity.contentResolver
                val values = ContentValues()
                values.put(MediaStore.MediaColumns.TITLE, fileName)
                values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                if (fileName.endsWith(".ORF"))
                {
                    values.put(MediaStore.Images.Media.MIME_TYPE, "image/x-olympus-orf")
                }
                else
                {
                    values.put(MediaStore.Images.Media.MIME_TYPE, "video/mp4")
                    isVideo = true
                }
                val extStorageUri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val path = Environment.DIRECTORY_DCIM + File.separator + activity.getString(R.string.app_name2)
                    values.put(MediaStore.MediaColumns.DATE_TAKEN, now)
                    values.put(MediaStore.MediaColumns.RELATIVE_PATH, path)
                    values.put(MediaStore.MediaColumns.IS_PENDING, true)
                    if (isVideo)
                    {
                        MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                    }
                    else
                    {
                        MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                    }

                } else {
                    values.put(MediaStore.Images.Media.DATA, outputDir.absolutePath + File.separator + fileName)
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                }
                imageUri = resolver.insert(extStorageUri, values)
                if (imageUri != null)
                {
                    outputStream = resolver.openOutputStream(imageUri!!, "wa")
                }
                ////////////////////////////////////////////////////////////////
                //if (imageUri != null)
                //{
                //    val cursor = resolver.query(imageUri!!, null, null, null, null)
                //    DatabaseUtils.dumpCursor(cursor)
                //    cursor!!.close()
                //}
                ////////////////////////////////////////////////////////////////

                camera.downloadLargeContent(downloadPath, this)
            }
            catch (e: Exception)
            {
                e.printStackTrace()
                outputStream = null
                val message : String = if (e.message == null) { "" } else { e.message!! }
                activity.runOnUiThread {
                    downloadDialog.dismiss()
                    presentMessage(activity.getString(R.string.download_control_save_failed), message)
                }
            }
        }
        catch (ex: Exception)
        {
            ex.printStackTrace()
        }
    }

    override fun onProgress(bytes: ByteArray?, progressEvent: OLYCamera.ProgressEvent)
    {
        val percent = (progressEvent.progress * 100.0f).toInt()
        val size = bytes?.size ?: 0
        @Suppress("DEPRECATION")
        downloadDialog.progress = percent
        //downloadDialog.setCancelable(progressEvent.isCancellable()); // キャンセルできるようにしないほうが良さそうなので
        //Log.v(TAG, " onProgress() : ${percent}% (${receivedSize} bytes.)")
        receivedSize += size
        try
        {
            outputStream?.write(bytes)
        }
        catch (e: Exception)
        {
            e.printStackTrace()
        }
    }

    override fun onCompleted()
    {
        try
        {
            Log.v(TAG, " onCompleted() [${fileName}] $receivedSize bytes.")
            outputStream?.flush()
            outputStream?.close()
            outputStream = null
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            {
                val resolver = activity.contentResolver
                val values = ContentValues()
                values.put(MediaStore.Images.Media.TITLE, fileName)
                values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                values.put(MediaStore.Images.Media.IS_PENDING, false)
                if (imageUri != null)
                {
                    resolver?.update(imageUri!!, values, null, null)
                }
            }
            activity.runOnUiThread {
                try {
                    downloadDialog.dismiss()
                    Snackbar.make(activity.findViewById(R.id.fragment1), "${activity.getString(R.string.download_control_save_success)}  $fileName ", Snackbar.LENGTH_SHORT).show()
                    //Toast.makeText(getActivity(), getString(R.string.download_control_save_success) + " " + filename, Toast.LENGTH_SHORT).show();
                    if (imageUri != null)
                    {
                        if (isShareContent)
                        {
                            shareContent(imageUri!!, fileName)
                        }
                    }
                    imageUri = null
                    System.gc()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        catch (e: Exception)
        {
            e.printStackTrace()
            val message : String = if (e.localizedMessage == null) { "" } else { e.localizedMessage!! }
            activity.runOnUiThread {
                downloadDialog.dismiss()
                presentMessage(activity.getString(R.string.download_control_save_failed), message)
            }
        }
        System.gc()
    }

    override fun onErrorOccurred(e: Exception)
    {
        val message : String = if (e.localizedMessage == null) { "" } else { e.localizedMessage!! }
        try
        {
            outputStream?.flush()
            outputStream?.close()
            outputStream = null
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            {
                val resolver = activity.contentResolver
                val values = ContentValues()
                values.put(MediaStore.Images.Media.TITLE, fileName)
                values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                values.put(MediaStore.Images.Media.IS_PENDING, false)
                if (imageUri != null)
                {
                    resolver?.update(imageUri!!, values, null, null)
                    imageUri = null
                }
            }
        }
        catch (ex: Exception)
        {
            e.printStackTrace()
            ex.printStackTrace()
        }
        activity.runOnUiThread {
            downloadDialog.dismiss()
            presentMessage(activity.getString(R.string.download_control_download_failed), message)
            System.gc()
        }
        System.gc()
    }

    companion object
    {
        private val TAG = this.toString()
    }
}
