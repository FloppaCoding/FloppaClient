package floppaclient.module.impl.dungeon

import floppaclient.funnymap.features.dungeon.Dungeon
import floppaclient.module.Category
import floppaclient.module.Module
import floppaclient.module.settings.impl.NumberSetting
import floppaclient.module.settings.impl.StringSetting
import floppaclient.utils.Utils.isFloor
import floppaclient.utils.Utils.sendChat
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.inventory.ContainerChest
import net.minecraftforge.client.event.GuiOpenEvent
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

    @SubscribeEvent
    fun onGuiOpen(event: GuiOpenEvent) {
        if (event.gui !is GuiChest ||  !isFloor(7) || !Dungeon.inBoss) return
        val container = (event.gui as GuiChest).inventorySlots
        if (container is ContainerChest) {
            val chestName = container.lowerChestInventory.displayName.unformattedText
            if (chestName == "Click the button on time!") {
                if (melodyTicks <= 0) {
                    sendChat("/pc ${message.text}")
                }
                melodyTicks = cooldown.value.toInt()
            }
        }
    }

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (event.phase != TickEvent.Phase.START) return
        if (melodyTicks > 0) melodyTicks--
    }
}