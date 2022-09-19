package floppaclient.module.impl.dungeon

import floppaclient.FloppaClient
import floppaclient.FloppaClient.Companion.mc
import floppaclient.events.PositionUpdateEvent
import floppaclient.module.Category
import floppaclient.module.Module
import floppaclient.module.settings.impl.BooleanSetting
import floppaclient.module.settings.impl.NumberSetting
import floppaclient.utils.Utils
import floppaclient.utils.Utils.isInTerminal
import floppaclient.utils.fakeactions.FakeActionUtils
import net.minecraft.entity.item.EntityArmorStand
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

/**
 * Module that opens floor 7 terminals and collects the crystals for you.
 * @author Aton
 */
object TerminalAura : Module(
    "Terminal Aura",
    category = Category.DUNGEON,
    description = "Automatically opens a terminal when in range. Also picks up and places crystals."
){

    private val reach = NumberSetting("Reach",5.0,3.0,10.0,0.1, description = "Maximum distance to the terminal for activation. Hypixel has a range check of 5.")
    private val cooldown = NumberSetting("Cooldown",200.0,20.0,1000.0,20.0, description = "Minimum delay in between clicks. §4 This has to be higher than your ping, or terminals will be opened twice, which will reset them.")
    private val onGround = BooleanSetting("On Ground", false, description = "If enabled will only click when on ground.")
    private val inLava = BooleanSetting("In Lava",false, description = "If enabled will also try to open terminals when in lava.")

    private var lastClicked = System.currentTimeMillis()

    init {
        this.addSettings(
            reach,
            cooldown,
            onGround,
            inLava
        )
    }

    @SubscribeEvent
    fun onPositionUpdatePost(event: PositionUpdateEvent.Post) {
        if (!FloppaClient.inDungeons || mc.thePlayer == null || !Utils.inF7Boss() || isInTerminal()) return

        if(System.currentTimeMillis() - lastClicked < cooldown.value) return

        if (mc.thePlayer.isInLava && !inLava.enabled) return
        if (!mc.thePlayer.onGround && onGround.enabled ) return

        val term = mc.theWorld.getLoadedEntityList()
            .filterIsInstance<EntityArmorStand>()
            .filter { entity -> entity.name.contains("CLICK HERE") }
            .filter { entity -> mc.thePlayer.getDistance(entity.posX, entity.posY - mc.thePlayer.eyeHeight, entity.posZ) < this.reach.value }
            .minByOrNull { entity -> mc.thePlayer.getDistance(entity.posX, entity.posY - mc.thePlayer.eyeHeight, entity.posZ) }
            ?: return

        FakeActionUtils.clickEntity(term)
        lastClicked = System.currentTimeMillis()
    }
}