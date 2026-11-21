package com.example.pocketenglish.ui.common

import android.content.ActivityNotFoundException
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException

/** 看板・メニューの撮影（カメラ翻訳）。端末の標準カメラアプリで撮るので CAMERA 権限は不要。 */
class CameraCapture internal constructor(
    private val context: Context,
    private val launchCamera: (Uri) -> Unit,
) {
    /** @return カメラアプリを起動できたら true（カメラアプリがない端末では false）。 */
    fun launch(): Boolean {
        val file = photoFile(context)
        file.parentFile?.mkdirs()
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return try {
            launchCamera(uri)
            true
        } catch (e: ActivityNotFoundException) {
            false
        }
    }
}

/**
 * 撮影後、[onPhoto] に撮った写真のファイルを渡す（キャンセル時は呼ばない）。
 * 写真はアプリのキャッシュ領域に1枚だけ上書き保存し、ギャラリーには残さない。
 */
@Composable
fun rememberCameraCapture(onPhoto: (File) -> Unit): CameraCapture {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val file = photoFile(context)
        if (saved && file.exists() && file.length() > 0) onPhoto(file)
    }
    return remember(context, launcher) { CameraCapture(context) { uri -> launcher.launch(uri) } }
}

private fun photoFile(context: Context) = File(File(context.cacheDir, "camera"), "photo.jpg")

/** Gemini に送る前に写真を縮小・向き補正して JPEG にする（通信量と待ち時間を減らすため）。 */
object PhotoEncoder {

    private const val MAX_EDGE = 1600
    private const val JPEG_QUALITY = 85

    /** @return 読み込めなければ null。送信後は元の写真を消す。 */
    suspend fun encodeForUpload(file: File): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.path, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null

            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_EDGE) sample *= 2
            val decoded = BitmapFactory.decodeFile(
                file.path,
                BitmapFactory.Options().apply { inSampleSize = sample },
            ) ?: return@withContext null

            val scaled = scaleDown(decoded)
            val rotated = rotate(scaled, rotationDegrees(file))
            ByteArrayOutputStream().use { out ->
                rotated.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
                if (rotated !== decoded) rotated.recycle()
                decoded.recycle()
                out.toByteArray()
            }
        } catch (e: IOException) {
            null
        } catch (e: OutOfMemoryError) {
            null
        } finally {
            file.delete()
        }
    }

    private fun scaleDown(bitmap: Bitmap): Bitmap {
        val longEdge = maxOf(bitmap.width, bitmap.height)
        if (longEdge <= MAX_EDGE) return bitmap
        val ratio = MAX_EDGE.toFloat() / longEdge
        return Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * ratio).toInt().coerceAtLeast(1),
            (bitmap.height * ratio).toInt().coerceAtLeast(1),
            true,
        )
    }

    private fun rotationDegrees(file: File): Int =
        when (ExifInterface(file.path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }

    private fun rotate(bitmap: Bitmap, degrees: Int): Bitmap {
        if (degrees == 0) return bitmap
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        val result = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (result !== bitmap) bitmap.recycle()
        return result
    }
}

/** 画面に出す、写真の送信先についての注意書き。 */
const val CAMERA_PRIVACY_NOTE = "📷 撮った写真は翻訳のため Google の Gemini API に送信されます。人の顔や個人情報が写らないようにご注意ください"
