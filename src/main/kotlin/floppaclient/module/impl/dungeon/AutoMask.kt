package floppaclient.module.impl.dungeon

import floppaclient.FloppaClient
import floppaclient.module.Category
import floppaclient.module.Module
import floppaclient.module.settings.impl.BooleanSetting
import floppaclient.utils.fakeactions.FakeActionUtils
import net.minecraft.util.StringUtils
import net.minecraftforge.client.event.ClientChatReceivedEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

/**
 * A Module that automatically swaps between Bonzo and Spirit masks
 * @author Stivais
 */

object AutoMask : Module(
    "Auto Mask",
    category = Category.DUNGEON,
    description = "Automatically switches to spirit mask or bonzo mask"
) {
    private val bonzoMask = BooleanSetting("Bonzo's Mask", enabled = true, description = "Swap to Bonzo")
    private val spiritMask = BooleanSetting("Spirit Mask", enabled = true, description = "Swap to Spirit mask")

    private var bonzoCD = System.currentTimeMillis()
    private var spiritCD = System.currentTimeMillis()

    @SubscribeEvent
    fun onChat(event: ClientChatReceivedEvent) {
        val text = StringUtils.stripControlCodes(event.message.unformattedText)

        if (!FloppaClient.inDungeons) return
        if (!text.endsWith("Mask saved your life!")) return

        when {
            text.startsWith("Your") && bonzoMask.enabled -> {
                if (spiritCD > System.currentTimeMillis()) return
                bonzoCD = System.currentTimeMillis() + 180000
                FakeActionUtils.swapArmorItem("Spirit Mask", ignoreCase = false)
            }
            text.startsWith("Second Wind Activated!") && spiritMask.enabled -> {
                if (bonzoCD > System.currentTimeMillis()) return
                spiritCD = System.currentTimeMillis() + 30000
                FakeActionUtils.swapArmorItem("Bonzo's Mask", ignoreCase = false)
            }
        }
    }
}