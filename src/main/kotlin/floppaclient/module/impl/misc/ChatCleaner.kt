package floppaclient.module.impl.misc

import floppaclient.module.Category
import floppaclient.module.Module
import floppaclient.module.settings.impl.BooleanSetting
import net.minecraft.util.StringUtils
import net.minecraftforge.client.event.ClientChatReceivedEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object ChatCleaner : Module(
    "Chat Cleaner",
    category = Category.MISC,
    description = "Cleans chat from spam."
) {

    private val abilityHider = BooleanSetting("Hide Ability Damage", false, description = "Hides Ability Damage from chat.")
    private val blocksInTheWay = BooleanSetting("Blocks in way", false, description = "Hides Blocks in the way messages") // rename to fit
    private val comboHider = BooleanSetting("Hide Combo", false, description = "Hides Combo Messages from the Grandma Wolf Pet")
    private val autoRecombHider = BooleanSetting("Hide Auto Recomb", false, description = "Hides Auto Recombobulator Messages")
    private val stashHider = BooleanSetting("Hide Stash", false, description = "Hides Stash Messages")
    private val playingOnProfile = BooleanSetting("Profile Message", false, description = "Hides 'You are playing on profile.'")


    init {
        this.addSettings(
            blocksInTheWay,
            abilityHider,
            comboHider,
            autoRecombHider,
            stashHider,
            playingOnProfile,
        )
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    fun handle(event: ClientChatReceivedEvent) {
        val text = StringUtils.stripControlCodes(event.message.unformattedText)

        when {
            text.startsWith("There are blocks in the way!") && blocksInTheWay.enabled  -> {event.isCanceled = true}
            text.startsWith("Your") && text.endsWith("damage.") && abilityHider.enabled -> {event.isCanceled = true}
            (text.startsWith("+") && text.contains("Kill Combo".toRegex())) || text.startsWith("Your Kill Combo has expired!") && comboHider.enabled -> {event.isCanceled = true} // i'll optimize it later
            text.startsWith("Your Auto-Recomb") && autoRecombHider.enabled -> {event.isCanceled = true}
            text.endsWith("Click here to pick it all up!") && stashHider.enabled -> {event.isCanceled = true}
            text.startsWith("You are playing on profile:") && playingOnProfile.enabled -> {event.isCanceled = true}
            else -> { return}
        }
    }
}








