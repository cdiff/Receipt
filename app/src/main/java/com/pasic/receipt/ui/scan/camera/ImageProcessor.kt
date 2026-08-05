package com.pasic.receipt.ui.scan.camera

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.camera.core.ImageProxy

/**
 * CameraX ImageProxy ➔ 올바른 회전 방향이 적용된 Bitmap 변환 헬퍼 함수
 */
fun imageProxyToBitmap(image: ImageProxy): Bitmap {
    val planeProxy = image.planes[0]
    val buffer = planeProxy.buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

    val rotationDegrees = image.imageInfo.rotationDegrees
    return if (rotationDegrees != 0) {
        val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    } else {
        bitmap
    }
}
