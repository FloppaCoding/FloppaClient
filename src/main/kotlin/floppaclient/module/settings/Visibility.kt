package floppaclient.module.settings

/**
 * This enum will determine where a setting should be visible.
 *
 * It can be used as a two bit binary number where the left bit determines the visibility in the advanced gui
 * and the right bit determines the visibility in the click gui.
 *
 * @author Aton
 */
enum class Visibility {
    HIDDEN, CLICK_GUI_ONLY, ADVANCED_ONLY, VISIBLE;

    /**
     * Checks whether the 2nd bit is 1.
     */
    val visibleInAdvanced: Boolean
        get() = this.ordinal and 0b10 > 0

    /**
     * Checks whether the 1st bit is 1.
     */
    val visibleInClickGui: Boolean
        get() = this.ordinal and 0b01 > 0
}