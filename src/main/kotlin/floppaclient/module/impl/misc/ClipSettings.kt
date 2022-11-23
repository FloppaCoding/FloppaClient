package floppaclient.module.impl.misc

import floppaclient.module.Category
import floppaclient.module.Module
import floppaclient.module.settings.impl.BooleanSetting
import floppaclient.module.settings.impl.NumberSetting

/**
 * A config module with the purpose to offer finer control over the auto clip behaviour.
 *
 * @author Aton
 */
object ClipSettings : Module(
    "Clip Settings",
    category = Category.MISC,
    description = "Advanced settings for Autoclip. Since Autoclip is anyways shot dead dont worry about this."
){
    val baseClipDistance    = NumberSetting("base Clip Distance",9.5, 0.0, 10.0,0.1, description = "Distance of the individual steps used for all far clips. Default 9.5.")
    val clipDelay1 = NumberSetting("First Delay",   010.0,10.0, 1000.0, 10.0, description = "First delay for far clip in mc. Default 10.")
    val clipDelay2 = NumberSetting("Second Delay",  060.0,10.0, 1000.0, 10.0, description = "Second delay for far clip in mc. Default 60, Possibly better 50.")
    val clipDelay3 = NumberSetting("Third Delay",   120.0,10.0, 1000.0, 10.0, description = "Third delay for far clip in mc. Default 120, Possibly better 100.")
    val clipDelay4 = NumberSetting("Fourth Delay",  190.0,10.0, 1000.0, 10.0, description = "Fourth delay for far clip in mc. Default 190, Possibly better 160.")
    val clipDelay5 = NumberSetting("Fifth Delay",   270.0,10.0, 1000.0, 10.0, description = "Fifth delay for far clip in mc. Default 270, Possibly better 230.")
    val clipDelay6 = NumberSetting("Sixth Delay",   360.0,10.0, 1000.0, 10.0, description = "Sixth delay for far clip in mc. Default 360, Possibly better 310.")
    val clipDelay7 = NumberSetting("Seventh Delay", 470.0,10.0, 1000.0, 10.0, description = "Seventh delay for far clip in mc. Default 470, Possibly better 400.")
    val clipDelay8 = NumberSetting("Eighth Delay",  580.0,10.0, 1000.0, 10.0, description = "Eighth delay for far clip in mc. Default 580, Possibly better 500.")
    val clipDelay9 = NumberSetting("Ninth Delay",   700.0,10.0, 1000.0, 10.0, description = "Ninth delay for far clip in mc. Default 700., Possibly better 610")
    val debugMessages = BooleanSetting("Debug Messages", false, description = "Debug messages for development.")

    init {
        this.addSettings(
            baseClipDistance,
            clipDelay1,
            clipDelay2,
            clipDelay3,
            clipDelay4,
            clipDelay5,
            clipDelay6,
            clipDelay7,
            clipDelay8,
            clipDelay9,
            debugMessages
        )
    }
}