package com.pasic.receipt.ui.scan.util

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import androidx.core.content.ContextCompat

/**
 * 갤러리의 가장 최근 사진 썸네일 비트맵을 로드하는 헬퍼 함수 (안전 권한 체크 및 폴백 적용)
 */
fun loadLatestGalleryThumbnail(context: Context): Bitmap? {
    return try {
        // 권한 체크
        val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATE_ADDED
        )
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val id = cursor.getLong(idColumn)
                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id
                )

                // 1. Android 10 (Q) 이상 loadThumbnail 시도
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    try {
                        context.contentResolver.loadThumbnail(contentUri, Size(120, 120), null)
                    } catch (e: Exception) {
                        decodeSampledBitmapFromUri(context, contentUri, 120, 120)
                    }
                } else {
                    decodeSampledBitmapFromUri(context, contentUri, 120, 120)
                }
            } else {
                null
            }
        }
    } catch (e: Exception) {
        null
    }
}

private fun decodeSampledBitmapFromUri(context: Context, uri: Uri, reqWidth: Int, reqHeight: Int): Bitmap? {
    return try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, options)

            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
            options.inJustDecodeBounds = false

            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }
        }
    } catch (e: Exception) {
        null
    }
}

private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
    val (height: Int, width: Int) = options.run { outHeight to outWidth }
    var inSampleSize = 1

    if (height > reqHeight || width > reqWidth) {
        val halfHeight: Int = height / 2
        val halfWidth: Int = width / 2

        while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
            inSampleSize *= 2
        }
    }
    return inSampleSize
}
