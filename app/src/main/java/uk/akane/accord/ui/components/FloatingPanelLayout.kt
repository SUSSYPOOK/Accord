package uk.akane.accord.ui.components

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.Outline
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewOutlineProvider
import android.view.WindowInsets
import android.view.animation.PathInterpolator
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnLayout
import androidx.core.view.marginBottom
import androidx.core.view.marginLeft
import androidx.core.view.marginRight
import androidx.core.view.marginTop
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.window.layout.WindowMetricsCalculator
import uk.akane.accord.R
import uk.akane.accord.logic.dp
import uk.akane.accord.logic.scale
import uk.akane.accord.ui.MainActivity
import uk.akane.accord.ui.viewmodels.AccordViewModel
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min


class FloatingPanelLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr, defStyleRes),
    GestureDetector.OnGestureListener {

    companion object {
        const val TAG = "FloatingPanelLayout"
    }

    private val activity
        get() = context as MainActivity
    private val viewModel: AccordViewModel =
        ViewModelProvider(context as ViewModelStoreOwner)[AccordViewModel::class.java]
    private val windowHeight: Int
        get() = WindowMetricsCalculator.getOrCreate()
            .computeCurrentWindowMetrics(context).bounds.height()
    private var valueAnimator: ValueAnimator? = null

    private var gestureDetector = GestureDetector(context, this)

    private var progress: Float = 0f

    private var initialMargin = IntArray(4)
    private var initialHeight = 0
    private var instanceHeight = 0
    private var instanceBottomMargin = 0

    private var scrollHeight = 0
    private var scrollBottomMargin = 0

    private var isScrollingUp = false

    private var retractThreshold = 0.05F

    private var defaultMaxDuration = 350L
    private var defaultMinDuration = 220L
    private var defaultUpActionDuration = 285L
    private var defaultInterpolator = PathInterpolator(0.2f, 0f, 0f, 1f)

    private var fullPlayer: FullPlayer? = null
    private var previewPlayer: PreviewPlayer

    private val insetController = WindowCompat.getInsetsController(activity.window, this)

    enum class SlideStatus {
        COLLAPSED, EXPANDED, SLIDING
    }

    interface OnSlideListener {
        fun onSlideStatusChanged(status: SlideStatus)

        fun onSlide(value: Float)
    }

    private var state: SlideStatus
        get() = viewModel.floatingPanelStatus.value
        set(value) {
            viewModel.floatingPanelStatus.value = value
        }

    private var onSlideListeners: MutableList<OnSlideListener> = mutableListOf()

    inner class OutlineProvider(
        private val rect: Rect = Rect(),
        var scaleX: Float,
        var scaleY: Float,
        private var yShift: Int
    ) : ViewOutlineProvider() {

        override fun getOutline(view: View?, outline: Outline?) {
            view?.background?.copyBounds(rect)
            rect.scale(scaleX, scaleY)
            rect.offset(0, yShift)

            val cornerRadius =
                resources.getDimensionPixelSize(R.dimen.bottom_panel_radius).toFloat()

            outline?.setRoundRect(rect, cornerRadius)
        }
    }

    init {
        inflate(context, R.layout.layout_floating_panel, this)
        fullPlayer = findViewById(R.id.full_player)
        previewPlayer = findViewById(R.id.preview_player)
        outlineProvider = OutlineProvider(scaleX = 1.02f, scaleY = 1.00f, yShift = (-2).dp.px.toInt())
        addOnSlideListener(object : OnSlideListener {
            override fun onSlideStatusChanged(status: SlideStatus) {
                when (status) {
                    SlideStatus.EXPANDED -> {
                        if (!isDarkMode(context) && insetController.isAppearanceLightStatusBars) {
                            WindowCompat
                                .getInsetsController(activity.window, this@FloatingPanelLayout)
                                .isAppearanceLightStatusBars = false
                        }
                        fullPlayer?.alpha = 1F
                        previewPlayer.alpha = 0F
                    }
                    SlideStatus.COLLAPSED -> {
                        if (!isDarkMode(context) && !insetController.isAppearanceLightStatusBars) {
                            WindowCompat
                                .getInsetsController(activity.window, this@FloatingPanelLayout)
                                .isAppearanceLightStatusBars = true
                        }
                        fullPlayer?.alpha = 0F
                        previewPlayer.alpha = 1F
                    }
                    SlideStatus.SLIDING -> {
                        if (!isDarkMode(context) && !insetController.isAppearanceLightStatusBars) {
                            WindowCompat
                                .getInsetsController(activity.window, this@FloatingPanelLayout)
                                .isAppearanceLightStatusBars = true
                        }
                    }
                }
            }

            override fun onSlide(value: Float) {
                fullPlayer?.alpha = value
                previewPlayer.alpha = 1f - value
            }

        })

        doOnLayout {
            initialHeight = height
        }
    }

    fun addOnSlideListener(onSlideListener: OnSlideListener) {
        onSlideListeners.add(onSlideListener)
    }

    override fun setBackground(background: Drawable?) {
        super.setBackground(background)
        fullPlayer?.let { it.background = background }
    }

    override fun dispatchApplyWindowInsets(platformInsets: WindowInsets): WindowInsets {
        if (initialMargin[3] != 0) return super.dispatchApplyWindowInsets(platformInsets)
        val insets = WindowInsetsCompat.toWindowInsetsCompat(platformInsets)
        val floatingInsets = insets.getInsets(
            WindowInsetsCompat.Type.systemBars()
                    or WindowInsetsCompat.Type.displayCutout()
        )
        if (floatingInsets.bottom != 0) {
            initialMargin = intArrayOf(
                marginLeft,
                marginTop,
                marginRight,
                marginBottom + floatingInsets.bottom
            )
            updateLayoutParams<MarginLayoutParams> {
                bottomMargin = initialMargin[3]
            }
        }
        return super.dispatchApplyWindowInsets(platformInsets)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        Log.d("TAG", "State: $state")
        doOnLayout {
            onUp(
                when (state) {
                    SlideStatus.COLLAPSED -> false
                    SlideStatus.EXPANDED -> true
                    else -> null
                },
                false
            )
            onSlideListeners.forEach {
                it.onSlideStatusChanged(state)
                it.onSlide(
                    when (state) {
                        SlideStatus.COLLAPSED -> 0f
                        SlideStatus.EXPANDED -> 1f
                        else -> 0f
                    }
                )
            }
        }
    }

    override fun onDetachedFromWindow() {
        onSlideListeners.clear()
        super.onDetachedFromWindow()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        return if (gestureDetector.onTouchEvent(event)) {
            true
        } else if (event.action == MotionEvent.ACTION_UP) {
            onUp()
            true
        } else {
            super.onTouchEvent(event)
        }
    }

    override fun onDown(event: MotionEvent): Boolean {
        valueAnimator?.cancel()
        instanceHeight = height
        instanceBottomMargin = marginBottom
        scrollHeight = height
        scrollBottomMargin = marginBottom
        return true
    }

    override fun onShowPress(event: MotionEvent) {
    }

    override fun onSingleTapUp(event: MotionEvent): Boolean {
        if (state == SlideStatus.COLLAPSED) {
            onUp(true)
        }
        return true
    }

    private var lastEventTime = 0L
    private var lastFlingSpeed = 0F

    override fun onScroll(
        event1: MotionEvent?,
        event2: MotionEvent,
        distanceX: Float,
        distanceY: Float
    ): Boolean {
        val distanceMoved = -(scrollHeight + scrollBottomMargin - height - marginBottom)
        val timeDelta = (event2.eventTime - lastEventTime).toFloat()
        val lastVelocity = distanceMoved.absoluteValue / timeDelta
        if (lastVelocity != 0F) lastFlingSpeed = lastVelocity
        val distanceMovedTotal = -(instanceHeight + instanceBottomMargin - height - marginBottom)
        isScrollingUp = distanceMoved >= 0 && distanceMovedTotal >= 0
        val distanceMotionEvent1ToTop = event1!!.y
        if (distanceMotionEvent1ToTop > height) return true
        val distanceMotionEvent2ToTop = -event2.y
        val compensateHeight = (distanceMotionEvent1ToTop + distanceMotionEvent2ToTop + height).toInt()
        valueAnimator?.cancel()
        setHeight(compensateHeight)
        scrollHeight = height
        scrollBottomMargin = marginBottom
        lastEventTime = event2.eventTime
        return true
    }

    override fun onLongPress(event: MotionEvent) {
    }

    override fun onFling(
        event1: MotionEvent?,
        event2: MotionEvent,
        velocityX: Float,
        velocityY: Float
    ): Boolean {
        Log.d("TAG", "yesOnFling")
        valueAnimator?.cancel()
        valueAnimator = ValueAnimator.ofInt(
            height,
            if (isScrollingUp) windowHeight else initialHeight
        )
        valueAnimator!!.apply {
            addUpdateListener {
                val value = it.animatedValue as Int
                setHeight(value)
            }
            duration = min(
                max(((windowHeight - height) / lastFlingSpeed).toLong(), defaultMinDuration),
                defaultMaxDuration
            )
            interpolator = defaultInterpolator
            start()
        }
        return true
    }

    fun onUp(isUp: Boolean? = null, shouldAnimate: Boolean = true) {
        if (!shouldAnimate) {
            setHeight(
                height = if ((isUp == null && height > retractThreshold * windowHeight) ||
                    isUp == true
                )
                    windowHeight
                else
                    initialHeight,
                onRestart = true,
                getProgress = {
                    if ((isUp == null && height > retractThreshold * windowHeight) ||
                        isUp == true
                    )
                        1f
                    else
                        0f
                }
            )
            return
        }
        valueAnimator?.cancel()
        valueAnimator = ValueAnimator.ofInt(
            height,
            if ((isUp == null && height > retractThreshold * windowHeight) ||
                isUp == true
            )
                windowHeight
            else
                initialHeight
        )
        valueAnimator!!.apply {
            addUpdateListener {
                val value = it.animatedValue as Int
                setHeight(value)
            }
            duration = defaultUpActionDuration
            interpolator = defaultInterpolator
            start()
        }
    }

    private fun calculateProgressTillTop(compensateHeight: Int) : Float =
        min(1f, compensateHeight / windowHeight.toFloat() - ((1f - (height - initialHeight) / (windowHeight - initialHeight).toFloat()) * initialHeight / windowHeight.toFloat()))

    private fun getMarginWithProgress(progress: Float): IntArray =
        intArrayOf(
            (initialMargin[0] * (1 - progress)).toInt(),
            (initialMargin[1] * (1 - progress)).toInt(),
            (initialMargin[2] * (1 - progress)).toInt(),
            (initialMargin[3] * (1 - progress)).toInt()
        )

    private fun setHeight(
        height: Int = 0,
        onRestart: Boolean = false,
        getProgress: (height: Int) -> Float = {
            calculateProgressTillTop(it)
        }
    ) {
        Log.d(TAG, "setHeight: $height, onRestart: $onRestart")
        progress = getProgress(height)
        getMarginWithProgress(progress).let {
            val layoutParams = CoordinatorLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                max(
                    min(height, windowHeight), initialHeight
                )
            )
            layoutParams.setMargins(it[0], it[1], it[2], it[3])
            layoutParams.gravity = Gravity.BOTTOM
            this.layoutParams = layoutParams
            if (!onRestart) {
                onSlide(progress)
            }
        }
    }

    private fun onSlide(progress: Float) {
        Log.d(TAG, "onSlide: $progress")
        onSlideListeners.forEach {
            it.onSlide(progress)
        }
        val prevState = state
        BigDecimal(progress.toDouble()).setScale(3, RoundingMode.HALF_UP).toDouble().let {
            state = when (it) {
                1.0 -> {
                    SlideStatus.EXPANDED
                }
                0.0 -> {
                    SlideStatus.COLLAPSED
                }
                else -> {
                    SlideStatus.SLIDING
                }
            }
        }
        if (prevState != state) {
            onSlideListeners.forEach { it.onSlideStatusChanged(state) }
        }
    }

    private fun isDarkMode(context: Context): Boolean =
        context.resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
}