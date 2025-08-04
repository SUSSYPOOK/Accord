package uk.akane.accord.ui.components

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.WindowInsets
import android.widget.Button
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnLayout
import androidx.core.view.marginBottom
import androidx.core.view.marginLeft
import androidx.core.view.marginRight
import androidx.core.view.marginTop
import androidx.core.view.updateLayoutParams
import uk.akane.accord.R
import uk.akane.accord.logic.getUriToDrawable
import uk.akane.accord.ui.components.lyrics.Lyrics
import uk.akane.accord.ui.components.lyrics.LyricsView
import uk.akane.accord.ui.components.lyrics.LyricsViewModel
import uk.akane.cupertino.widget.divider.OverlayDivider

class FullPlayer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr, defStyleRes),
    FloatingPanelLayout.OnSlideListener {

    private var initialMargin = IntArray(4)

    private var blendView: BlendView
    private var overlayDivider: OverlayDivider
    private var fadingEdgeLayout: FadingVerticalEdgeLayout
    private var lyricsBtn: Button
    private var lyricsViewModel: LyricsViewModel? = null
    private val floatingPanelLayout: FloatingPanelLayout?
        get() = parent as FloatingPanelLayout?

    init {
        inflate(context, R.layout.layout_full_player, this)

        blendView = findViewById(R.id.blend_view)
        overlayDivider = findViewById(R.id.divider)
        fadingEdgeLayout = findViewById(R.id.fading)
        lyricsBtn = findViewById(R.id.lyrics)

        blendView.setImageUri(context.getUriToDrawable(R.drawable.eg))
        blendView.startRotationAnimation()
        clipToOutline = true

        doOnLayout {
            floatingPanelLayout?.addOnSlideListener(this)

            fadingEdgeLayout.visibility = GONE
            lyricsViewModel = LyricsViewModel(context)

            lyricsBtn.setOnClickListener {
                fadingEdgeLayout.visibility = VISIBLE
                lyricsViewModel?.onViewCreated(fadingEdgeLayout)
                lyricsBtn.visibility = GONE
            }
        }
    }

    override fun dispatchApplyWindowInsets(platformInsets: WindowInsets): WindowInsets {
        if (initialMargin[3] != 0) return super.dispatchApplyWindowInsets(platformInsets)
        val insets = WindowInsetsCompat.toWindowInsetsCompat(platformInsets)
        val floatingInsets = insets.getInsets(
            WindowInsetsCompat.Type.systemBars()
                    or WindowInsetsCompat.Type.displayCutout()
        )
        Log.d(TAG, "marginBottom: ${marginBottom}, InsetsBottom: ${floatingInsets.bottom}, marginTop: ${floatingInsets.top}")
        if (floatingInsets.bottom != 0) {
            initialMargin = intArrayOf(
                marginLeft,
                marginTop + floatingInsets.top,
                marginRight,
                marginBottom + floatingInsets.bottom
            )
            Log.d(TAG, "initTop: ${initialMargin[1]}")
            overlayDivider.updateLayoutParams<MarginLayoutParams> {
                topMargin = initialMargin[1] + overlayDivider.marginTop
            }
        }
        return super.dispatchApplyWindowInsets(platformInsets)
    }

    override fun onSlideStatusChanged(status: FloatingPanelLayout.SlideStatus) {
        when (status) {
            else -> {}
        }
    }

    override fun onSlide(value: Float) {
        // PLACEHOLDER TODO
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val screenWidth = resources.displayMetrics.widthPixels
        val screenHeight = resources.displayMetrics.heightPixels

        val widthSpec = MeasureSpec.makeMeasureSpec(screenWidth, MeasureSpec.EXACTLY)
        val heightSpec = MeasureSpec.makeMeasureSpec(screenHeight, MeasureSpec.EXACTLY)

        super.onMeasure(widthSpec, heightSpec)
    }

    companion object {
        const val TAG = "FullPlayer"
    }
}