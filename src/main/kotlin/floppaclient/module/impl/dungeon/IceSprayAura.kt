package floppaclient.module.impl.dungeon

import floppaclient.FloppaClient.Companion.inDungeons
import floppaclient.FloppaClient.Companion.inSkyblock
import floppaclient.FloppaClient.Companion.mc
import floppaclient.events.PositionUpdateEvent
import floppaclient.module.Category
import floppaclient.module.Module
import floppaclient.module.settings.impl.BooleanSetting
import floppaclient.module.settings.impl.NumberSetting
import floppaclient.utils.GeometryUtils.getDirection
import floppaclient.utils.Utils.containsOneOf
import floppaclient.utils.fakeactions.FakeActionManager
import floppaclient.utils.inventory.InventoryUtils
import floppaclient.utils.inventory.InventoryUtils.isHolding
import floppaclient.utils.inventory.SkyblockItem
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.entity.Entity
import net.minecraft.entity.monster.EntityZombie
import net.minecraftforge.client.event.ClientChatReceivedEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object IceSprayAura : Module(
    "Ice Spray Aura",
    category = Category.DUNGEON,
    description = "Automatically ice sprays the selected mobs when they are in range. " +
            "Keeps track of the ice spray cooldown and uses the item off cooldown."
){
    private val range = NumberSetting("Range", 4.0, 1.0,6.0,0.1, description = "Only mobs within this distance will be targeted.")
    private val mimic = BooleanSetting("Mimic", true, description = "Determines whether the minic should be targeted.")
    private val minis = BooleanSetting("Minis", true, description = "Determines whether mini bosses should be targeted.")
    private val visiCheck = BooleanSetting("Visibility Check", false, description = "Checks whether the target can be seen.")
    private val pauseWithHype = BooleanSetting("Hype Check",  true, description = "Disables the module when holding a Wither Blade.")
    private val pauseWithAOTV = BooleanSetting("AOTV Check",  true, description = "Disables the module when holding a either an AOTE or an AOTV.")

    private val miniBosses = listOf(
        "Diamond Guy",
        "Frozen Adventurer", // It should always be called Lost Adventurer
        "Lost Adventurer",
        "Shadow Assassin",
    )

    private var cooldown: Long = System.currentTimeMillis()

    init {
        this.addSettings(
            range,
            mimic,
            minis,
            visiCheck,
            pauseWithHype,
            pauseWithAOTV
        )
    }


    /**
     * Checks for mobs in range and stages the click.
     */
    @Suppress("UNUSED_PARAMETER")
    @SubscribeEvent(priority = EventPriority.HIGH)
    fun preMove(event: PositionUpdateEvent.Pre) {
        if(!inDungeons || FakeActionManager.doAction) return
        if (System.currentTimeMillis() < cooldown) return
        if (pauseWithHype.enabled && mc.thePlayer.isHolding(SkyblockItem.Attribute.WITHERBLADE)) return
        if (pauseWithAOTV.enabled && mc.thePlayer.isHolding(SkyblockItem.AOTV, SkyblockItem.AOTE)) return

        /** Look for mobs  */
        val target = getTarget() ?: return

        val direction = mc.thePlayer.getDirection(target, (target.eyeHeight - mc.thePlayer.eyeHeight).toDouble())
        val slot = InventoryUtils.findItem(SkyblockItem.ICE_SPRAY) ?: return
        FakeActionManager.stageRightClickSlot(direction[1], direction[2], slot, false)
        cooldown = System.currentTimeMillis() + 5000
    }

    /**
     * Returns the best valid ice spray aura target together with the name of the corresponding armorstand.
     * Returns null if no target found.
     */
    private fun getTarget(): Entity? {
        mc.theWorld.loadedEntityList
            .filter {
                ( mimic.enabled && it is EntityZombie && it.isChild)
                || ( minis.enabled && it is EntityOtherPlayerMP && it.name.containsOneOf(miniBosses) )
            }
            .filter { mc.thePlayer.getDistanceToEntity(it) < range.value }
            .sortedBy { entity -> mc.thePlayer.getDistanceToEntity(entity) }
            .forEach { entity ->

                /** Visibility Check */
                if (!visiCheck.enabled || mc.thePlayer.canEntityBeSeen(entity)) {
                    return entity
                }
            }
        return null
    }

    /**
     * Keep track of the item cooldown.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    fun onActionBar(event: ClientChatReceivedEvent) {
        if (!inSkyblock || event.type.toInt() != 2) return
        val message = event.message.unformattedText
        if (message.contains("§b-") && message.contains(" Mana (§6")) {
            val itemId = message.substringAfter(" Mana (§6").substringBefore("§b)")
            if (itemId.contains("Ice Spray", ignoreCase = true)) {
                cooldown = System.currentTimeMillis() + 5000
            }
        }
    }
}