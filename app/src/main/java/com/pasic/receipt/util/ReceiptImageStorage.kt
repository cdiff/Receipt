package com.pasic.receipt.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

object ReceiptImageStorage {

    fun getReceiptsDir(context: Context): File {
        val dir = File(context.filesDir, "receipts")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun saveBitmap(context: Context, bitmap: Bitmap): String {
        val file = File(getReceiptsDir(context), "receipt_${UUID.randomUUID()}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        return file.absolutePath
    }

    fun copyUriToAppStorage(context: Context, uri: Uri): String? {
        return try {
            val file = File(getReceiptsDir(context), "receipt_${UUID.randomUUID()}.jpg")
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(file).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun loadBitmapFromPath(path: String): Bitmap? {
        val file = File(path)
        return if (file.exists()) {
            BitmapFactory.decodeFile(file.absolutePath)
        } else {
            null
        }
    }
}
