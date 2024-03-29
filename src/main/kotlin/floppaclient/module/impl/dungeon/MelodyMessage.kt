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
import net.minecraftforge.client.event.GuiOpenEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent

/**
 * Send a chat message to let your teammates know that you got the melody terminal.
 * @author Aton
 */
object MelodyMessage : Module(
    "Melody Message",
    category = Category.DUNGEON,
    description = "Automatically sends a message in chat when you open the melody terminal."
){

    private val message = StringSetting("Message", "Melody on me!", 40, description = "The message that will be sent.")
    private var cooldown = NumberSetting("Cooldwon Ticks", 30.0, 10.0, 200.0, 10.0, description = "Increase this if the message gets spammed, should not be needed.")

    /**
     * Used to create a cooldown for the melody open message.
     * The terminal reopens the gui at least once per second.
     */
    private var melodyTicks = 0

    init {
        this.addSettings(
            message,
            cooldown
        )
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    fun onGuiOpen(event: GuiOpenEvent) {
        if (event.gui !is GuiChest ||  !RunInformation.isInFloor(7) || !Dungeon.inBoss) return
        if (currentTerminal == AutoTerms.TerminalType.TIMING) {
            if (melodyTicks <= 0) {
                sendChat("/pc ${message.text}")
            }
            melodyTicks = cooldown.value.toInt()
        }
    }

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (event.phase != TickEvent.Phase.START) return
        if (melodyTicks > 0) melodyTicks--
    }
}