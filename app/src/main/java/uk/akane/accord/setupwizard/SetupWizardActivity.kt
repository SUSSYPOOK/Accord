package uk.akane.accord.setupwizard

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.button.MaterialButton
import uk.akane.accord.R
import uk.akane.accord.logic.enableEdgeToEdgeProperly
import uk.akane.accord.logic.isEssentialPermissionGranted
import uk.akane.accord.logic.setCurrentItemInterpolated
import uk.akane.cupertino.widget.utils.AnimationUtils
import uk.akane.accord.setupwizard.adapters.SetupWizardViewPagerAdapter
import uk.akane.accord.ui.MainActivity
import uk.akane.accord.ui.viewmodels.AccordViewModel

class SetupWizardActivity : AppCompatActivity() {

    private lateinit var viewPager2: ViewPager2
    private lateinit var viewPagerAdapter: SetupWizardViewPagerAdapter
    private lateinit var continueButton: MaterialButton

    private var inactiveBtnColor = 0
    private var activeBtnColor = 0

    private var onInactiveBtnColor = 0
    private var onActiveBtnColor = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        inactiveBtnColor = getColor(R.color.accentColorFainted)
        activeBtnColor = getColor(R.color.accentColor)

        onInactiveBtnColor = getColor(R.color.onAccentColorFainted)
        onActiveBtnColor = getColor(R.color.onAccentColor)

        installSplashScreen()

        enableEdgeToEdgeProperly()

        setContentView(R.layout.activity_setup_wizard)

        viewPager2 = findViewById(R.id.sw_viewpager)
        continueButton = findViewById(R.id.continue_btn)
        viewPagerAdapter = SetupWizardViewPagerAdapter(supportFragmentManager, lifecycle)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        viewPager2.adapter = viewPagerAdapter
        viewPager2.isUserInputEnabled = false
        viewPager2.offscreenPageLimit = 9999

        continueButton.setOnClickListener {
            if (viewPager2.currentItem + 1 < viewPagerAdapter.itemCount) {
                if (viewPager2.currentItem + 1 == 1 && !isEssentialPermissionGranted()) {
                    continueButton.isEnabled = false
                    AnimationUtils.createValAnimator(
                        activeBtnColor,
                        inactiveBtnColor,
                        isArgb = true
                    ) {
                        continueButton.backgroundTintList = ColorStateList.valueOf(
                            it
                        )
                    }
                    AnimationUtils.createValAnimator(
                        onActiveBtnColor,
                        onInactiveBtnColor,
                        isArgb = true
                    ) {
                        continueButton.setTextColor(
                            it
                        )
                    }
                }
                viewPager2.setCurrentItemInterpolated(viewPager2.currentItem + 1)
            } else {
                this.startActivity(
                    Intent(this, MainActivity::class.java)
                )
                finish()
                return@setOnClickListener
            }
        }

    }

    fun releaseContinueButton() {
        AnimationUtils.createValAnimator(
            inactiveBtnColor,
            activeBtnColor,
            isArgb = true,
            doOnEnd = {
                continueButton.isEnabled = true
            }
        ) {
            continueButton.backgroundTintList = ColorStateList.valueOf(
                it
            )
        }
        AnimationUtils.createValAnimator(
            onInactiveBtnColor,
            onActiveBtnColor,
            isArgb = true
        ) {
            continueButton.setTextColor(
                it
            )
        }
    }
}