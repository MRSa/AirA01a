package jp.osdn.gokigen.aira01a.getimage


import android.app.AlertDialog
import androidx.exifinterface.media.ExifInterface
import androidx.fragment.app.FragmentActivity
import jp.osdn.gokigen.aira01a.R
import jp.osdn.gokigen.aira01a.playback.ExifInformationDumper

class ExifInfoToShow(private val activity: FragmentActivity, information: ExifInterface) : Runnable
{
    private val message = formMessage(information)

    /**
     * Exif情報を表示に適した形式に変更し、情報を表示する
     * @param exifInterface Exif情報
     * @return 表示に適したExif情報
     */
    private fun formMessage(exifInterface: ExifInterface?): String?
    {
        var msg = ""
        try {
            if (exifInterface != null)
            {
                // 撮影時刻
                msg += activity.getString(R.string.exif_datetime_title)
                msg += " ${getExifAttribute(exifInterface, ExifInterface.TAG_DATETIME)} \r\n"

                // 焦点距離
                val focalLength = exifInterface.getAttributeDouble(ExifInterface.TAG_FOCAL_LENGTH, 0.0)
                msg += activity.getString(R.string.exif_focal_length_title)
                msg += " " + focalLength.toString() + "mm "
                msg += "(${activity.getString(R.string.exif_focal_35mm_equiv_title)} ${focalLength * 2.0}mm)\r\n\r\n"

                // カスタムイメージプロセッシング利用の有無
                //if (exifInterface.getAttributeInt(ExifInterface.TAG_CUSTOM_RENDERED, 0) != 0)
                //{
                //	msg = msg + getString(R.string.exif_custom_process_title) + "\r\n";
                //}

                // 撮影モード
                val stringArray: Array<String> = activity.getResources().getStringArray(R.array.exif_exposure_program_value)
                val exposureProgram = exifInterface.getAttributeInt(ExifInterface.TAG_EXPOSURE_PROGRAM, 0)
                msg += activity.getString(R.string.exif_camera_mode_title)
                msg += if (stringArray.size > exposureProgram) stringArray[exposureProgram] else "? ($exposureProgram)"
                msg += "\r\n"

                // 測光モードの表示
                val meteringStringArray: Array<String> = activity.getResources().getStringArray(R.array.exif_metering_mode_value)
                val metering = exifInterface.getAttributeInt(ExifInterface.TAG_METERING_MODE, 0)
                msg += activity.getString(R.string.exif_metering_mode_title)
                msg += " ${if (meteringStringArray.size > metering) meteringStringArray[metering] else "? ($metering)"}"
                msg += "\r\n"

                // 露光時間
                msg += activity.getString(R.string.exif_exposure_time_title)
                val expTime = getExifAttribute(exifInterface, ExifInterface.TAG_EXPOSURE_TIME)
                val `val`: Float
                var inv = 0.0f
                try {
                    `val` = expTime.toFloat()
                    if (`val` < 1.0f) {
                        inv = 1.0f / `val`
                    }
                    if (inv < 2.0f) // if (inv < 10.0f)
                    {
                        inv = 0.0f
                    }
                } catch (e: Exception) {
                    //
                    e.printStackTrace()
                }

                //msg = msg + " " + expTime + "s "; //(string)
                msg = if (inv > 0.0f)
                {
                    // シャッター速度を分数で表示する
                    var intValue = inv.toInt()
                    val modValue = intValue % 10
                    if (modValue == 9 || modValue == 4) {
                        // ちょっと格好が悪いけど...切り上げ
                        intValue++
                    }
                    "$msg 1/$intValue s "
                }
                else
                {
                    // シャッター速度を数値(秒数)で表示する
                    msg + " " + expTime + "s " //(string)
                }
                msg += "\r\n"

                // 絞り値
                msg += activity.getString(R.string.exif_aperture_title)
                msg += " ${getExifAttribute(exifInterface, ExifInterface.TAG_F_NUMBER)}\r\n"

                // ISO感度
                msg += activity.getString(R.string.exif_iso_title)
                msg += " ${getExifAttribute(exifInterface, ExifInterface.TAG_ISO_SPEED_RATINGS)}\r\n"

                // カメラの製造元
                msg += activity.getString(R.string.exif_maker_title)
                msg += " ${getExifAttribute(exifInterface, ExifInterface.TAG_MAKE)}\r\n"

                // カメラのモデル名
                msg += activity.getString(R.string.exif_camera_title)
                msg += " ${getExifAttribute(exifInterface, ExifInterface.TAG_MODEL)}\r\n"// (string)
                val lat = getExifAttribute(exifInterface, ExifInterface.TAG_GPS_LATITUDE)
                if (lat != null && lat.length > 0)
                {
                    // 「位置情報あり」と表示
                    msg += "${activity.getString(R.string.exif_with_gps)} \r\n"
                }
                //msg = msg + getExifAttribute(exifInterface, ExifInterface.TAG_FLASH);      // フラッシュ (int)
                //msg = msg + getExifAttribute(exifInterface, ExifInterface.TAG_ORIENTATION);  // 画像の向き (int)
                //msg = msg + getExifAttribute(exifInterface, ExifInterface.TAG_WHITE_BALANCE);  // ホワイトバランス (int)

                // その他の情報...EXIFタグで取得できたものをログにダンプする
                msg += ExifInformationDumper.dumpExifInformation(exifInterface, false)
            } else {
                msg = activity.getString(R.string.download_control_get_information_failed)
            }
        }
        catch (eee: Exception)
        {
            eee.printStackTrace()
        }
        return msg
    }

    private fun getExifAttribute(attr: ExifInterface, tag: String): String
    {
        try
        {
            var value = attr.getAttribute(tag)
            if (value == null)
            {
                value = ""
            }
            return value
        }
        catch (e: Exception)
        {
            e.printStackTrace()
        }
        return ""
    }

    override fun run() {
        AlertDialog.Builder(activity).setTitle(activity.getString(R.string.download_control_get_information_title))?.setMessage(message)?.show()
        System.gc()
    }
}
