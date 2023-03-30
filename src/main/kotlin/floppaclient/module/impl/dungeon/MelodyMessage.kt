package floppaclient.module.impl.dungeon

import floppaclient.floppamap.dungeon.Dungeon
import floppaclient.floppamap.dungeon.RunInformation
import floppaclient.module.Category
import floppaclient.module.Module
import floppaclient.module.impl.dungeon.AutoTerms.currentTerminal
import floppaclient.module.settings.impl.NumberSetting
import floppaclient.module.settings.impl.StringSetting
import floppaclient.utils.ChatUtils.sendChat
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.util.StringUtils
import net.minecraftforge.client.event.ClientChatReceivedEvent
import net.minecraftforge.client.event.GuiOpenEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent

/**
 * Send a chat message to let your teammates know that you got the melody terminal.
 * @author Aton, Stivais
 */
object MelodyMessage : Module(
    "Melody Message",
    category = Category.DUNGEON,
    description = "Automatically sends a message in chat when you open the melody terminal."
){

    private val melodyMessage: String by StringSetting("Melody Message", "Melody on me!", 40, description = "The message that will be sent when Melody is opened.")
    private val throttleMessage: String by StringSetting("Throttle Message", "Throttled", 40, description = "The message that will be sent when you get throttled in Melody.")
    private var melodyCooldown: Double by NumberSetting("Cooldown Ticks", 100.0, 10.0, 200.0, 10.0, description = "Increase this if the message gets spammed, should not be needed.")
    private val throttleCooldown: Double by NumberSetting("Cooldown Ticks", 100.0, 10.0, 200.0, 10.0, description = "Increase this if the message gets spammed, should not be needed.")
    //rename throttleCooldown

    /**
     * Used to create a cooldown for the melody open message.
     * The terminal reopens the gui at least once per second.
     */
    private var melodyTicks = 0
    /**
     * Used to create a cooldown for the throttle message.
     * When throttled, the chat gets spammed once per second
     */
    private var throttleTicks = 0


    @SubscribeEvent(priority = EventPriority.LOW)
    fun onGuiOpen(event: GuiOpenEvent) {
        if (event.gui !is GuiChest ||  !RunInformation.isInFloor(7) || !Dungeon.inBoss) return
        if (currentTerminal == AutoTerms.TerminalType.TIMING) {
            if (melodyTicks <= 0) {
                sendChat("/pc $melodyMessage")
            }
            melodyTicks = melodyCooldown.toInt()
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    fun onChat(event: ClientChatReceivedEvent) {
        val text = StringUtils.stripControlCodes(event.message.unformattedText)
        if (!text.startsWith("This menu has been throttled! Please slow down...") ||  !RunInformation.isInFloor(7) || !Dungeon.inBoss) return
        if (currentTerminal == AutoTerms.TerminalType.TIMING) {
            if (melodyTicks <= 0) {
                sendChat("/pc $throttleMessage")
            }
            throttleTicks = throttleCooldown.toInt()
        }
    }

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (event.phase != TickEvent.Phase.START) return
        if (melodyTicks > 0) melodyTicks--
        if (throttleTicks > 0) throttleTicks--
    }
}