package mega.privacy.android.app.modalbottomsheet

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.DatabaseHandler
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.interfaces.ActivityLauncher
import mega.privacy.android.app.utils.Util
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaChatApiAndroid
import java.util.HashMap
import javax.inject.Inject

@AndroidEntryPoint
open class BaseBottomSheetDialogFragment : BottomSheetDialogFragment(), ActivityLauncher {

    companion object {
        private const val HEIGHT_SEPARATOR = 1
        private const val TYPE_OPTION = "TYPE_OPTION"
        private const val TYPE_SEPARATOR = "TYPE_SEPARATOR"
    }

    @MegaApi
    @Inject
    lateinit var megaApi: MegaApiAndroid

    @Inject
    lateinit var megaChatApi: MegaChatApiAndroid

    @Inject
    lateinit var dbH: DatabaseHandler

    lateinit var contentView: View
    lateinit var itemsLayout: LinearLayout

    private var halfHeightDisplay = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        halfHeightDisplay = resources.displayMetrics.heightPixels / 2
        view.post { setBottomSheetBehavior() }
    }

    override fun onResume() {
        super.onResume()

        val dialog = dialog ?: return

        val window = dialog.window ?: return

        // In landscape mode, we need limit the bottom sheet dialog width.

        // In landscape mode, we need limit the bottom sheet dialog width.
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            val displayMetrics = resources.displayMetrics
            val maxSize = displayMetrics.heightPixels
            window.setLayout(maxSize, ViewGroup.LayoutParams.MATCH_PARENT)
        }

        // But `setLayout` causes navigation buttons almost invisible in light mode,
        // in this case we set navigation bar background with light grey to make
        // navigation buttons visible.

        // But `setLayout` causes navigation buttons almost invisible in light mode,
        // in this case we set navigation bar background with light grey to make
        // navigation buttons visible.
        if (!Util.isDarkMode(requireContext())) {
            // Only set navigation bar elements colour, View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR = 0x00000010
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = 0x00000010
        }
    }

    /**
     * Sets the initial state of a BottomSheet and its state.
     */
    private fun setBottomSheetBehavior() {
        BottomSheetBehavior.from(contentView.parent as View).apply {
            peekHeight = getNewPeekHeight()
            state = BottomSheetBehavior.STATE_COLLAPSED
            addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                        dismissAllowingStateLoss()
                    }
                }

                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                }
            })
        }
    }

    /**
     * Hides the BottomSheet.
     */
    protected fun setStateBottomSheetBehaviorHidden() {
        BottomSheetBehavior.from(contentView.parent as View).state =
            BottomSheetBehavior.STATE_HIDDEN
    }

    /**
     * Gets the initial height of a BottomSheet.
     * It depends on the number of visible options on it, the display height
     * and current orientation of the used device.
     * The maximum height will be a bit more than half of the screen.
     *
     * @return The initial height of a BottomSheet.
     */
    private fun getNewPeekHeight(): Int {
        if (contentView.height <= halfHeightDisplay) {
            return contentView.height
        }

        var numVisibleOptions = 0
        var childHeight = 0
        val visibleItems: MutableMap<Int, String> = HashMap()
        var peekHeight = 0
        val heightSeparator = Util.dp2px(HEIGHT_SEPARATOR.toFloat())

        for (i in 0 until itemsLayout.childCount) {
            val v: View = itemsLayout.getChildAt(i)

            if (v.visibility == View.VISIBLE) {
                val height = v.layoutParams.height
                childHeight += height
                if (height == heightSeparator) {
                    //Is separator
                    visibleItems[i] = TYPE_SEPARATOR
                } else {
                    //Is visible option
                    numVisibleOptions++
                    visibleItems[i] = TYPE_OPTION
                }
            }
        }

        var countVisibleOptions = 0
        for ((visibleItemPosition, visibleItemType) in visibleItems) {
            val heightVisiblePosition =
                itemsLayout.getChildAt(visibleItemPosition).layoutParams.height

            if (visibleItemType == TYPE_OPTION) {
                countVisibleOptions++
            }

            peekHeight += if (peekHeight < halfHeightDisplay || visibleItemType == TYPE_SEPARATOR
                || countVisibleOptions == numVisibleOptions
            ) {
                heightVisiblePosition
            } else {
                return peekHeight + heightVisiblePosition / 2
            }
        }

        return peekHeight
    }

    override fun launchActivity(intent: Intent) {
        startActivity(intent)
    }

    override fun launchActivityForResult(intent: Intent, requestCode: Int) {
        startActivityForResult(intent, requestCode)
    }
}