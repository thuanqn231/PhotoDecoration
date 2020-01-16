package com.hmman.photodecoration.widget

import android.annotation.TargetApi
import android.content.Context
import android.graphics.*
import android.os.Build
import android.util.AttributeSet
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener
import android.view.View.OnTouchListener
import android.widget.FrameLayout
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.core.content.ContextCompat
import androidx.core.view.GestureDetectorCompat
import com.hmman.photodecoration.R
import com.hmman.photodecoration.multitouch.MoveGestureDetector
import com.hmman.photodecoration.multitouch.RotateGestureDetector
import com.hmman.photodecoration.widget.entity.MotionEntity
import java.util.*

class MotionView : FrameLayout {
    interface Constants {
        companion object {
            const val SELECTED_LAYER_ALPHA = 0.15f
        }
    }

    interface MotionViewCallback {
        fun onEntitySelected(@Nullable entity: MotionEntity?)
        fun onEntityDoubleTap(@NonNull entity: MotionEntity?)
    }

    // layers
    private val entities: MutableList<MotionEntity> =
        ArrayList()
    private val undoEntities: Stack<MotionEntity> = Stack()
    private val resetEntities: MutableList<MotionEntity> = ArrayList<MotionEntity>()

    @Nullable
    var selectedEntity: MotionEntity? = null
        private set
    private var selectedLayerPaint: Paint? = null
    // callback
    @Nullable
    private var motionViewCallback: MotionViewCallback? = null
    // gesture detection
    private var scaleGestureDetector: ScaleGestureDetector? = null
    private var rotateGestureDetector: RotateGestureDetector? = null
    private var moveGestureDetector: MoveGestureDetector? = null
    private var gestureDetectorCompat: GestureDetectorCompat? = null
    private var motionEventTouch: MotionEvent? = null

    // constructors
    constructor(context: Context) : super(context) {
        init(context)
    }

    constructor(
        context: Context,
        attrs: AttributeSet?
    ) : super(context, attrs) {
        init(context)
    }

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int
    ) : super(context, attrs, defStyleAttr) {
        init(context)
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes) {
        init(context)
    }

    private fun init(@NonNull context: Context) { // I fucking love Android
        setWillNotDraw(false)
        selectedLayerPaint = Paint()
        selectedLayerPaint!!.alpha = (255 * Constants.SELECTED_LAYER_ALPHA).toInt()
        selectedLayerPaint!!.isAntiAlias = true
        selectedLayerPaint!!.color = Color.BLUE
        // init listeners
        scaleGestureDetector = ScaleGestureDetector(context, ScaleListener())
        rotateGestureDetector = RotateGestureDetector(context, RotateListener())
        moveGestureDetector = MoveGestureDetector(context, MoveListener())
        gestureDetectorCompat = GestureDetectorCompat(context, TapsListener())
        setOnTouchListener(onTouchListener)
        updateUI()
    }

    fun getEntities(): List<MotionEntity> {
        return entities
    }

    fun setMotionViewCallback(@Nullable callback: MotionViewCallback?) {
        motionViewCallback = callback
    }

    fun addEntity(@Nullable entity: MotionEntity?) {
        if (entity != null) {
            entities.add(entity)
            selectEntity(entity, false)
        }
    }

    fun addEntityAndPosition(@Nullable entity: MotionEntity?) {
        if (entity != null) {
            initEntityBorder(entity)
            initialTranslateAndScale(entity)
            entities.add(entity)
            selectEntity(entity, true)
        }
    }

    private fun initEntityBorder(@NonNull entity: MotionEntity) { // init stroke
        val strokeSize = resources.getDimensionPixelSize(R.dimen.stroke_size)
        val borderPaint = Paint()
        borderPaint.strokeWidth = strokeSize.toFloat()
        borderPaint.isAntiAlias = true
        borderPaint.color = ContextCompat.getColor(context, R.color.stroke_color)
        entity.setBorderPaint(borderPaint)
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        // dispatch draw is called after child views is drawn.
// the idea that is we draw background stickers, than child views (if any), and than selected item
// to draw on top of child views - do it in dispatchDraw(Canvas)
// to draw below that - do it in onDraw(Canvas)
        if (selectedEntity != null) {
            selectedEntity!!.draw(canvas, selectedLayerPaint)
        }
    }

    override fun onDraw(canvas: Canvas) {
        drawAllEntities(canvas)
        super.onDraw(canvas)
    }

    /**
     * draws all entities on the canvas
     * @param canvas Canvas where to draw all entities
     */
    private fun drawAllEntities(canvas: Canvas) {
        for (i in entities.indices) {
            println(i)
            entities[i].draw(canvas, null)
        }
    }// IMPORTANT: always create white background, cos if the image is saved in JPEG format,
// which doesn't have transparent pixels, the background will be black

    /**
     * as a side effect - the method deselects Entity (if any selected)
     * @return bitmap with all the Entities at their current positions
     */
    val thumbnailImage: Bitmap
        get() {
            selectEntity(null, false)
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            // IMPORTANT: always create white background, cos if the image is saved in JPEG format,
            // which doesn't have transparent pixels, the background will be black
            bmp.eraseColor(Color.WHITE)
            val canvas = Canvas(bmp)
            drawAllEntities(canvas)
            return bmp
        }

    private fun updateUI() {
        invalidate()
    }

    private fun handleTranslate(delta: PointF) {
        if (selectedEntity != null) {
            val newCenterX = selectedEntity!!.absoluteCenterX() + delta.x
            val newCenterY = selectedEntity!!.absoluteCenterY() + delta.y
            // limit entity center to screen bounds
            var needUpdateUI = false
            if (newCenterX >= 0 && newCenterX <= width) {
                selectedEntity!!.layer.postTranslate(delta.x / width, 0.0f)
                needUpdateUI = true
            }
            if (newCenterY >= 0 && newCenterY <= height) {
                selectedEntity!!.layer.postTranslate(0.0f, delta.y / height)
                needUpdateUI = true
            }
            if (needUpdateUI) {
                updateUI()
            }
        }
    }

    private fun initialTranslateAndScale(@NonNull entity: MotionEntity) {
        entity.moveToCanvasCenter()
        entity.layer.scale = entity.layer.initialScale()
    }

    private fun selectEntity(@Nullable entity: MotionEntity?, updateCallback: Boolean) {
        if (selectedEntity != null) {
            selectedEntity!!.setIsSelected(false)
        }
        entity?.setIsSelected(true)
        selectedEntity = entity
        invalidate()
        if (updateCallback && motionViewCallback != null) {
            motionViewCallback!!.onEntitySelected(entity)
        }
    }

    fun unselectEntity() {
        if (selectedEntity != null) {
            selectEntity(null, true)
        }
    }

    @Nullable
    private fun findEntityAtPoint(x: Float, y: Float): MotionEntity? {
        var selected: MotionEntity? = null
        val p = PointF(x, y)
        for (i in entities.indices.reversed()) {
            if (entities[i].pointInLayerRect(p)) {
                selected = entities[i]
                break
            }
        }
        return selected
    }

    private fun updateSelectionOnTap(e: MotionEvent): Boolean {
        val entity = findEntityAtPoint(e.x, e.y)
        return if (entity != null) {
            selectEntity(entity, true)
            true
        } else {
            unselectEntity()
            false
        }
    }

    private fun updateOnLongPress(e: MotionEvent) { // if layer is currently selected and point inside layer - move it to front
        if (selectedEntity != null) {
            val p = PointF(e.x, e.y)
            if (selectedEntity!!.pointInLayerRect(p)) {
                bringLayerToFront(selectedEntity!!)
            }
        }
    }

    private fun bringLayerToFront(@NonNull entity: MotionEntity) { // removing and adding brings layer to front
        if (entities.remove(entity)) {
            entities.add(entity)
            invalidate()
        }
    }

    private fun moveEntityToBack(@Nullable entity: MotionEntity?) {
        if (entity == null) {
            return
        }
        if (entities.remove(entity)) {
            entities.add(0, entity)
            invalidate()
        }
    }

    fun flipSelectedEntity() {
        if (selectedEntity == null) {
            return
        }
        selectedEntity!!.layer.flip()
        invalidate()
    }

    fun moveSelectedBack() {
        moveEntityToBack(selectedEntity)
    }

    fun deletedSelectedEntity() {
        if (selectedEntity == null) {
            return
        }
        if (entities.remove(selectedEntity!!)) {
            selectedEntity!!.release()
            selectedEntity = null
            invalidate()
        }
    }

    // memory
    fun release() {
        for (entity in entities) {
            entity.release()
        }
    }

    fun redo() {
        if (undoEntities.size > 0) {
            entities.add(undoEntities.pop())
            updateUI()
        } else {
            Toast.makeText(this.context, "Nothing to Redo", Toast.LENGTH_SHORT).show();
        }
    }

    fun undo() {
        val lastItemPosition = entities.size - 1
        val listSize = entities.size
        when {
            isReseted -> {
                entities.addAll(resetEntities)
                resetEntities.clear()
                updateUI()
                isReseted = false
            }
            listSize > 0 -> {
                undoEntities.push(entities.removeAt(lastItemPosition))
                selectEntity(null, false)
                updateUI()
            }
            else -> {
                Toast.makeText(this.context, "Nothing to Undo", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private var isReseted = false
    fun reset() {
        resetEntities.addAll(entities)
        entities.clear()
        updateUI()
        isReseted = true
    }

    // gesture detectors
    private val onTouchListener = OnTouchListener { _, event ->
        if (scaleGestureDetector != null) {
            scaleGestureDetector!!.onTouchEvent(event)
            rotateGestureDetector!!.onTouchEvent(event)
            gestureDetectorCompat!!.onTouchEvent(event)
//            if (updateSelectionOnTap(event)) {
            moveGestureDetector!!.onTouchEvent(event)
//            }
        }

        true
    }

    private inner class TapsListener : SimpleOnGestureListener() {

        override fun onDoubleTap(e: MotionEvent): Boolean {
            if (motionViewCallback != null && selectedEntity != null) {
                motionViewCallback!!.onEntityDoubleTap(selectedEntity)
            }
            return true
        }

        override fun onLongPress(e: MotionEvent) {
            updateOnLongPress(e)
        }


        override fun onSingleTapUp(e: MotionEvent): Boolean {
            updateSelectionOnTap(e)
            return true
        }

    }

    private inner class ScaleListener : SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            if (selectedEntity != null) {
                val scaleFactorDiff = detector.scaleFactor
                selectedEntity!!.layer.postScale(scaleFactorDiff - 1.0f)
                updateUI()
            }
            return true
        }
    }

    private inner class RotateListener : RotateGestureDetector.SimpleOnRotateGestureListener() {
        override fun onRotate(detector: RotateGestureDetector?): Boolean {
            if (selectedEntity != null) {
                selectedEntity!!.layer.postRotate(-detector!!.rotationDegreesDelta)
                updateUI()
            }
            return true
        }
    }

    private inner class MoveListener : MoveGestureDetector.SimpleOnMoveGestureListener() {
        override fun onMove(detector: MoveGestureDetector): Boolean {
            handleTranslate(detector.getFocusDelta())
            return true
        }
    }

    companion object {
        private val TAG = MotionView::class.java.simpleName
    }
}
