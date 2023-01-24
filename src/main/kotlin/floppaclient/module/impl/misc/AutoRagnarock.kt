package floppaclient.module.impl.misc

import floppaclient.FloppaClient.Companion.inDungeons
import floppaclient.module.Category
import floppaclient.module.Module
import floppaclient.module.settings.impl.BooleanSetting
import floppaclient.utils.ChatUtils.modMessage
import floppaclient.utils.fakeactions.FakeActionUtils
import net.minecraft.util.StringUtils
import net.minecraftforge.client.event.ClientChatReceivedEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

/**
 * Module to automatically get the bonus form the Fagnarock axe when activating the Withercloak sword.
 * @author Aton
 */
object AutoRagnarock : Module(
    "Auto Ragnarock",
    category = Category.MISC,
    description = "Automatically swaps to and right clicks the ragnarock axe if it is found in your hotbar upon " +
            "activating creeper veil." +
            "Will only activate outside of dungeons"
) {

    private val chatMessage = BooleanSetting("Chat message", true)

    init {
        this.addSettings(chatMessage)
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onChat(event: ClientChatReceivedEvent) {
        if (inDungeons || event.type.toInt() == 2) return
        when (StringUtils.stripControlCodes(event.message.unformattedText)) {
            "Creeper Veil Activated!" -> {
                val used = FakeActionUtils.clickItem("Ragnarock Axe",true,false)
                if (chatMessage.enabled && used) modMessage("Ragnarock Axe activated.")
                return
            }
        }
    }
}