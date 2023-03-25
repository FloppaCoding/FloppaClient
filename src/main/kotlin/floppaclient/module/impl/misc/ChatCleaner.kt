package floppaclient.module.impl.misc

import floppaclient.FloppaClient
import floppaclient.module.Category
import floppaclient.module.Module
import floppaclient.module.settings.impl.BooleanSetting
import floppaclient.utils.Utils.containsOneOf
import net.minecraft.util.StringUtils
import net.minecraftforge.client.event.ClientChatReceivedEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

/**
 * A modules meant for removing spammy messages from chat.
 * @author Stivais
 */
object ChatCleaner : Module(
    "Chat Cleaner",
    category = Category.MISC,
    description = "Cleans chat from spam."
) {
    private val dungeon: Boolean by BooleanSetting("Dungeon Messages", true, description = "Hides useless messages in dungeons.")
    private val milestones: Boolean by BooleanSetting("Milestone Messages", true, description = "Hides Milestone messages in dungeons.")
    private val hypixel: Boolean by BooleanSetting("Useless Hypixel Msgs", true, description = "Hides useless Messages")

    private val abilityHider: Boolean by BooleanSetting("Hide Ability Damage", true, description = "Hides Ability Damage from chat.")
    private val stashHider: Boolean by BooleanSetting("Hide Stash", true, description = "Hides Stash Messages")
    private val blocksInTheWay: Boolean by BooleanSetting("Blocks in way", true, description = "Hides §c§oThere are blocks in the way!§r messages")
    private val comboHider: Boolean by BooleanSetting("Hide Combo", true, description = "Hides §6§l§o+50 Kill Combo§r messages.")
    private val autoRecombHider: Boolean by BooleanSetting("Hide Auto Recomb", true, description = "Hides  §e§oYour §6§oAuto Recombobulator §e§orecombobulated§r messages.")

    private val dungeonMessages = setOf(
        "DUNGEON BUFF!",
        "A Blessing of",
        "Granted you +",
        "unlocked Undead Essence",
        "unlocked Wither Essence",
        "found a Wither Essence",
        "has obtained",
        "RIGHT CLICK on a",
        "Guided Sheep is now available",
        "Your active Potion Effects have been paused",
        "BUFF! You have gained",
        "You can no longer consume"
    )
    private val dungClasses = setOf(
        "stats are doubled because you are the only player using this class!",
        "[Mage]",
        "[Archer]",
        "[Berserk]",
        "[Tank]",
        "[Healer]",
        "Milestone"
    )
    private val uselessMSG = setOf(
        "You are playing on profile:",
        "Welcome to Hypixel SkyBlock!",
        "from playing Skyblock!",
        "[WATCHDOG ANNOUNCEMENT]",
        "Watchdog has banned",
        "Staff have banned an additional",
        "Blacklisted modifications"
    )

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    fun onChat(event: ClientChatReceivedEvent) {

        val text = StringUtils.stripControlCodes(event.message.unformattedText)
        if (text.containsOneOf(setOf("Party >", "Guild >"))) return // This is here, so it doesn't cancel party and guild chat messages
        when {
            text.containsOneOf(dungeonMessages) && FloppaClient.inDungeons && dungeon -> event.isCanceled = true
            text.containsOneOf(uselessMSG) && hypixel -> event.isCanceled = true
            text.containsOneOf(dungClasses) && FloppaClient.inDungeons && milestones -> event.isCanceled = true

            text.startsWith("There are blocks in the way!") && blocksInTheWay -> event.isCanceled = true
            text.startsWith("Your") && text.endsWith("damage.") && abilityHider -> event.isCanceled = true
            (text.contains("Kill Combo") && !text.contains(":") && comboHider) -> event.isCanceled = true
            text.startsWith("Your Auto-Recombobulator recombobulated") && autoRecombHider -> event.isCanceled = true
            text.endsWith("Click here to pick it all up!") && stashHider -> event.isCanceled = true
        }
    }
}