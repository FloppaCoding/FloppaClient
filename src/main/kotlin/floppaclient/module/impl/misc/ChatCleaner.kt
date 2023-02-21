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
    private val dungeon = BooleanSetting("Dungeon Messages", true, description = "Hides useless messages in dungeons.")
    private val dungPot = BooleanSetting("Dungeon potion", true, description = "Hides dungeon potion messages")
    private val milestones = BooleanSetting("Milestone Messages", true, description = "Hides Milestone messages in dungeons.")
    private val hypixelMsgs = BooleanSetting("Useless Hypixel Msgs", true, description = "Hides useless Messages")

    private val abilityHider = BooleanSetting("Hide Ability Damage", true, description = "Hides Ability Damage from chat.")
    private val stashHider = BooleanSetting("Hide Stash", true, description = "Hides Stash Messages")
    private val blocksInTheWay = BooleanSetting("Blocks in way", true, description = "Hides §c§oThere are blocks in the way!§r messages")
    private val comboHider = BooleanSetting("Hide Combo", true, description = "Hides §6§l§o+50 Kill Combo§r messages.")
    private val autoRecombHider = BooleanSetting("Hide Auto Recomb", true, description = "Hides  §e§oYour §6§oAuto Recombobulator §e§orecombobulated§r messages.")

    init {
        this.addSettings(
            dungeon,
            dungPot,
            milestones,
            abilityHider,
            blocksInTheWay,
            comboHider,
            autoRecombHider,
        )
    }

    private val dontCancel = listOf("Party >", "Guild >")
    private val dung = setOf(
        "DUNGEON BUFF!",
        "A Blessing of",
        "Granted you +",
        "unlocked Undead Essence",
        "unlocked Wither Essence",
        "found a Wither Essence",
        "has obtained",
        "RIGHT CLICK on a",
        "Guided Sheep is now available"
    )
    private val dungPots = setOf(
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
        "Staff have banned an additional"
    )

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    fun onChat(event: ClientChatReceivedEvent) {

        val text = StringUtils.stripControlCodes(event.message.unformattedText)
        if (text.containsOneOf(dontCancel)) return // This is here, so it doesn't cancel party and guild chat messages
        when {
            text.containsOneOf(dung) && FloppaClient.inDungeons && dungeon.enabled -> event.isCanceled = true
            text.containsOneOf(dungPots) && dungPot.enabled -> event.isCanceled = true
            text.containsOneOf(uselessMSG) && hypixelMsgs.enabled -> event.isCanceled = true
            text.containsOneOf(dungClasses) && FloppaClient.inDungeons && milestones.enabled -> event.isCanceled = true

            text.startsWith("There are blocks in the way!") && blocksInTheWay.enabled -> event.isCanceled = true
            text.startsWith("Your") && text.endsWith("damage.") && abilityHider.enabled -> event.isCanceled = true
            (text.contains("Kill Combo") && !text.contains(":") && comboHider.enabled) -> event.isCanceled = true
            text.startsWith("Your Auto-Recombobulator recombobulated") && autoRecombHider.enabled -> event.isCanceled = true
            text.endsWith("Click here to pick it all up!") && stashHider.enabled -> event.isCanceled = true
        }
    }
}