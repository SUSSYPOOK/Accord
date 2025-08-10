package uk.akane.accord.ui.components

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.doOnLayout
import com.google.android.material.button.MaterialButton
import uk.akane.accord.R
import uk.akane.cupertino.widget.image.SimpleImageView

class PreviewPlayer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr, defStyleRes),
    FloatingPanelLayout.OnSlideListener {
    private var controlMaterialButton: MaterialButton
    private var coverSimpleImageView: SimpleImageView
    private val floatingPanelLayout: FloatingPanelLayout
        get() = parent as FloatingPanelLayout

    override fun onSlideStatusChanged(status: FloatingPanelLayout.SlideStatus) {
        when (status) {
            FloatingPanelLayout.SlideStatus.COLLAPSED -> {
                coverSimpleImageView.alpha = 1F
            }
            else -> {
                coverSimpleImageView.alpha = 0F
            }
        }
    }

    override fun onSlide(value: Float) {
    }

    init {
        inflate(context, R.layout.layout_preview_player, this)
        controlMaterialButton = findViewById(R.id.control_btn)
        coverSimpleImageView = findViewById(R.id.preview_cover)

        coverSimpleImageView.doOnLayout {
            floatingPanelLayout.setupMetrics(
                coverSimpleImageView.width
            )
        }

        doOnLayout {
            floatingPanelLayout.addOnSlideListener(this)
        }
    }
}