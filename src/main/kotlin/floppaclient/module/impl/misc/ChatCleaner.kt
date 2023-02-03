package floppaclient.module.impl.misc

import floppaclient.module.Category
import floppaclient.module.Module
import floppaclient.module.settings.impl.BooleanSetting
import net.minecraft.util.StringUtils
import net.minecraftforge.client.event.ClientChatReceivedEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

/**
 * A modules meant for removing spammy messages from chat.
 *
 * @author Stivais
 */
object ChatCleaner : Module(
    "Chat Cleaner",
    category = Category.MISC,
    description = "Cleans chat from spam."
) {

    private val abilityHider = BooleanSetting("Hide Ability Damage", true, description = "Hides Ability Damage from chat.")
    private val stashHider = BooleanSetting("Hide Stash", true, description = "Hides Stash Messages")
    private val blocksInTheWay = BooleanSetting("Blocks in way", true, description = "Hides §c§oThere are blocks in the way!§r messages")
    private val comboHider = BooleanSetting("Hide Combo", true, description = "Hides §6§l§o+50 Kill Combo§r messages.")
    private val autoRecombHider = BooleanSetting("Hide Auto Recomb", true, description = "Hides  §e§oYour §6§oAuto Recombobulator §e§orecombobulated§r messages.")
    private val playingOnProfile = BooleanSetting("Profile Message", true, description = "Hides §a§oYou are playing on profile: §e§oFruit §b§o(Co-op)§r messages.")
    private val sbGxp = BooleanSetting("SB Guild EXP", true, description = "Hides §a§oYou earned §2§o15 GEXP §a§ofrom playing SkyBlock!§r messages")
    private val essenceHider = BooleanSetting("Wither Essence", true, description = "Hides §b§oPlayer§r§o found a §d§oWither Essence§r§o! Everyone gains an extra essence!§r messages")
    private val chestEssenceHider = BooleanSetting("unlocked Essence", true, description = "Hides §b§o[MVP§c§o+§b§o] Player §e§o unlocked §d§oUndead Essence §8§ox100§r messages")

    init {
        this.addSettings(
            blocksInTheWay,
            abilityHider,
            comboHider,
            autoRecombHider,
            stashHider,
            playingOnProfile,
            sbGxp,
        )
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    fun onChat(event: ClientChatReceivedEvent) {

        val text = StringUtils.stripControlCodes(event.message.unformattedText)
        val fText = event.message.formattedText // formatted Text

        when {
            text.startsWith("There are blocks in the way!") && blocksInTheWay.enabled  -> { event.isCanceled = true }
            text.startsWith("Your") && text.endsWith("damage.") && abilityHider.enabled -> { event.isCanceled = true }
            (text.contains("'+'(?=[0-9])".toRegex()) || (text.contains("Kill Combo".toRegex()) || text.startsWith("Your Kill Combo has expired!") && comboHider.enabled)) -> { event.isCanceled = true }
            text.startsWith("Your Auto-Recombobulator recombobulated") && autoRecombHider.enabled -> { event.isCanceled = true }
            text.endsWith("Click here to pick it all up!") && stashHider.enabled -> { event.isCanceled = true }
            text.startsWith("You are playing on profile:") && playingOnProfile.enabled -> { event.isCanceled = true }
            text.startsWith("You earned") && text.endsWith("from playing SkyBlock!") && sbGxp.enabled -> { event.isCanceled = true }
            fText.contains("§eunlocked §dUndead Essence ") || fText.contains("§eunlocked §dWither Essence ") && chestEssenceHider.enabled -> { event.isCanceled = true }
            fText.endsWith("found a §dWither Essence§r! Everyone gains an extra essence!")&& essenceHider.enabled -> { event.isCanceled = true }

            else -> return
        }
    }
}