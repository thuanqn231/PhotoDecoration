package com.hmman.photodecoration.widget.entity

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.annotation.IntRange
import androidx.annotation.NonNull
import com.hmman.photodecoration.model.Layer

class ImageEntity (@NonNull layer: Layer,
                   @NonNull val bitmap: Bitmap,
                   @IntRange (from = 1) canvasWidth: Int,
                   @IntRange (from = 1) canvasHeight: Int
): MotionEntity (layer, canvasWidth, canvasHeight) {

    init {
        val width = bitmap.width
        val height = bitmap.height
        val widthAspect = 1F * canvasWidth/width
        val heightAspect =1F * canvasHeight/height

        holyScale = Math.min(widthAspect, heightAspect)

        srcPoints[0] = 0f
        srcPoints[1] = 0f
        srcPoints[2] = width.toFloat()
        srcPoints[3] = 0f
        srcPoints[4] = width.toFloat()
        srcPoints[5] = height.toFloat()
        srcPoints[6] = 0f
        srcPoints[7] = height.toFloat()
        srcPoints[8] = 0f
        srcPoints[8] = 0f
    }

    override fun drawContent(canvas: Canvas, drawingPaint: Paint?) {
        canvas!!.drawBitmap(bitmap, matrix, drawingPaint)
    }

    override val width: Int
        get() = canvasWidth
    override val height: Int
        get() = canvasHeight

    override fun release() {
        if (!bitmap.isRecycled) bitmap.recycle()
    }
}