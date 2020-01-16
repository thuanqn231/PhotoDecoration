package com.hmman.photodecoration.ui

import android.graphics.*
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.annotation.Nullable
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.hmman.photodecoration.BuildConfig
import com.hmman.photodecoration.R
import com.hmman.photodecoration.model.Font
import com.hmman.photodecoration.model.Layer
import com.hmman.photodecoration.model.TextLayer
import com.hmman.photodecoration.util.FontProvider
import com.hmman.photodecoration.widget.entity.ImageEntity
import com.hmman.photodecoration.widget.entity.TextEntity
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    private lateinit var fontProvider: FontProvider

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fontProvider = FontProvider(resources)


        btnAdd.setOnClickListener {
            addSticker(R.drawable.mankey)
        }

        btnUndo.setOnClickListener {
            motionView.undo()
        }

        btnRedo.setOnClickListener {
            motionView.redo()
        }

        btnReset.setOnClickListener {
            motionView.reset()
        }

        btnSave.setOnClickListener {
            val bitmap =
                Bitmap.createBitmap(
                    motionView.getWidth(),
                    motionView.getHeight(),
                    Bitmap.Config.ARGB_8888
                )
            val canvas = Canvas(bitmap)
            motionView.draw(canvas)

            imgResult.setBackgroundColor(Color.RED)
            imgResult.setImageBitmap(bitmap)
            imgResult.visibility = View.VISIBLE
        }

        btnAddText.setOnClickListener {
            val textLayer = createTextLayer()!!
            val textEntity =
                TextEntity(textLayer, motionView.width, motionView.height, fontProvider)
            motionView.addEntityAndPosition(textEntity)

            val center: PointF = textEntity.absoluteCenter()
            center.y = center.y * 0.5f
            textEntity.moveCenterTo(center)

            motionView.invalidate()

            startTextEntityEditing()
        }

    }

    private fun addSticker(stickerResId: Int) {
        motionView.post {
            val layer = Layer()
            val pica = BitmapFactory.decodeResource(resources, stickerResId)
            val entity =
                ImageEntity(layer, pica, motionView.width, motionView.height)
            motionView.addEntityAndPosition(entity)
        }
    }

    private fun startTextEntityEditing() {
//        val textEntity: TextEntity = currentTextEntity()!!
//        if (textEntity != null) {
//            val fragment: TextEditorDialogFragment =
//                TextEditorDialogFragment.getInstance(textEntity.getLayer().getText())
//            fragment.show(fragmentManager, TextEditorDialogFragment::class.java.getName())
//        }
    }

    @Nullable
    private fun currentTextEntity(): TextEntity? {
        return if (motionView != null && motionView.selectedEntity is TextEntity) {
            motionView.selectedEntity as TextEntity?
        } else {
            null
        }
    }

    private fun createTextLayer(): TextLayer? {
        val textLayer = TextLayer()
//        val font = Font()
//        font.color = TextLayer.Limits.INITIAL_FONT_COLOR
//        font.size = TextLayer.Limits.INITIAL_FONT_SIZE
//        font.size = 20F
//        font.typeface = fontProvider.getDefaultFontName()
//        textLayer.font = font
        textLayer.text = "Hello, world :))"
        if (BuildConfig.DEBUG) {
            textLayer.text = "Hello, world :))"
        }
        return textLayer
    }
}
