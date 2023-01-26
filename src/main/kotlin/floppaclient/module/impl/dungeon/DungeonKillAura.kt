package floppaclient.module.impl.dungeon

import floppaclient.FloppaClient.Companion.inDungeons
import floppaclient.FloppaClient.Companion.mc
import floppaclient.events.PositionUpdateEvent
import floppaclient.floppamap.core.RoomType
import floppaclient.floppamap.dungeon.Dungeon.currentRoom
import floppaclient.floppamap.dungeon.Dungeon.inBoss
import floppaclient.module.Category
import floppaclient.module.Module
import floppaclient.module.settings.impl.BooleanSetting
import floppaclient.module.settings.impl.NumberSetting
import floppaclient.module.settings.impl.SelectorSetting
import floppaclient.module.settings.impl.StringSetting
import floppaclient.utils.GeometryUtils.getDirection
import floppaclient.utils.Utils
import floppaclient.utils.Utils.containsOneOf
import floppaclient.utils.fakeactions.FakeActionManager
import floppaclient.utils.fakeactions.FakeActionManager.doAction
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.entity.Entity
import net.minecraft.entity.boss.EntityWither
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.entity.monster.EntityEnderman
import net.minecraft.entity.monster.EntityGiantZombie
import net.minecraft.entity.player.EntityPlayer
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

/**
 * Module to automatically kill all relevant mobs in dungeons.
 * Can be Used as Terminator aura, Blood camp aura, Spirit Bear aura and as mage to assist with killing high hp targets in clear.
 *
 * @author Aton
 */
object DungeonKillAura : Module(
    "Kill Aura",
    category = Category.DUNGEON,
    description = "Kills mobs close to you. Can be Used as Terminator aura, Blood camp aura, Spirit Bear aura and as mage to assist with killing high hp targets in clear."
){
    private val bloodAura = BooleanSetting("Blood Aura", true, description = "Toggle whether blood mobs should be attacked when in blood.")
    private val starMobAura =  BooleanSetting("Star Mobs", true, description = "Toggle whether star mobs should be attacked.")
    private val miniBossAura =  BooleanSetting("Mini Bosses", true, description = "Toggle whether Mini Bosses should be attacked.")
    private val spiritBearAura =  BooleanSetting("Spirit Bears", true, description = "Toggle whether floor 4 Spirit Bears should be attacked.")
    private val range = NumberSetting("Range",10.0,4.0,30.0,1.0, description = "Maximum distance for targets to be attacked.")
    private val priorityRange = NumberSetting("Priority Range",8.0,4.0,30.0,1.0, description = "Within this range entities will be sorted by relevance instead of distance.")
    private val sleep = NumberSetting("Sleep ms", 1000.0, 100.0,2000.0,50.0, description = "Delay between clicks.")
    private val item = SelectorSetting("Item","Claymore", arrayListOf("Claymore", "Terminator", "Custom"), description = "Item to be used.")
    private val customItem = StringSetting("Custom Item","Claymore", description = "This item will be used when Custom is selected for Item.")
    private val leftClick = BooleanSetting("Left Click", true, description = "Left click if enabled, right click otherwise.")
    private val swingItem = BooleanSetting("Swing Item", false, description = "Swing the held item upon left clicking if enabled.")
    private val offset = NumberSetting("Offset", 0.0,-3.0,1.0,0.1, description = "General aim y offset.")
    private val bloodOffset = NumberSetting("Blood Offset", -1.5,-3.0,1.0,0.1, description = "Aim y offset for Blood mobs.")
    private val bearOffset = NumberSetting("Bear Offset", -0.5,-3.0,1.0,0.1, description = "Aim y offset for Spirit Bears.")
    private val predictionTime = NumberSetting("Prediction Time", 50.0,0.0,500.0,10.0, description = "Time in ms, for how far it will try to predict the position of your target based on its current momentum. Use this to account for ping.")

    private var lastClicked = System.currentTimeMillis()
//    private var lastSpiritBearDeath = System.currentTimeMillis()

    private val bloodMobs = mapOf(
        "L.A.S.R."          to -100000,
        "The Diamond Giant" to -100000,
        "Jolly Pink Giant"  to -100000,
        "Bigfoot"           to -100000,
        "Bonzo"    to -99000,
        "Scarf"    to -99000,
        "Livid"    to -99000,
        "Putrid"    to -90000,
        "Revoker"   to -90000,
        "Reaper"    to -90000,
        "Mr. Dead"  to -90000,
        "Vader"     to -90000,
        "Tear"      to -90000,
        "Frost"     to -90000,
        "Cannibal"  to -90000,
        "Skull"     to -90000,
        "Psycho"    to -90000,
        "Ooze"      to -90000,
        "Freak"     to -90000,
        "Flamer"    to -90000,
        "Mute"      to -90000,
        "Leech"     to -90000,
        "Parasite"  to -90000,
        "Walker"    to -90000,
    )

    private val miniBosses = listOf(
        "Diamond Guy",
        "Frozen Adventurer", // It should always be called Lost Adventurer
        "Lost Adventurer",
        "Shadow Assassin",
    )
    // Type: EntityOtherPlayerMPName: Lost Adventurer, Custom Name: , Id: 67942
    // Type: EntityOtherPlayerMP, Name: Spirit Bear, Custom Name: , Id: 145297
    // Type: EntityArmorStand, Name: Spirit Bear 4.9M❤, Custom Name: Spirit Bear 4.9M❤, Id: 145298

    init {
        this.addSettings(
            bloodAura,
            starMobAura,
            miniBossAura,
            spiritBearAura,
            range,
            priorityRange,
            sleep,
            item,
            customItem,
            leftClick,
            swingItem,
            offset,
            bloodOffset,
            bearOffset,
            predictionTime,
        )
    }

    /**
     * Checks for mobs in range and stages the click.
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    fun preMove(event: PositionUpdateEvent.Pre) {
        if(!inDungeons || doAction) return

        if (System.currentTimeMillis() - lastClicked < sleep.value) return

        /** Look for mobs  */
        mc.thePlayer.position

        val target = getTarget() ?: return
        val offset = when {
            target.second == "Spirit Bear" -> {
                if (target.first.onGround)
                    0.0
                else
                    bearOffset.value
            }
            target.second.containsOneOf(bloodMobs.keys) -> this.bloodOffset.value
            else -> this.offset.value
        }

        val predictedPosition = target.first.positionVector
            .addVector(
                target.first.motionX * predictionTime.value / 50.0,
                0.0,
                target.first.motionZ * predictionTime.value / 50.0,
            )

        val direction = mc.thePlayer.getDirection(predictedPosition, offset)

        val itemName = if (item.isSelected("Custom")) {
            customItem.text
        }else {
            item.selected
        }

        val slot = Utils.findItem(itemName) ?: return
        if (leftClick.enabled) {
            FakeActionManager.stageLeftClickSlot(direction[1], direction[2], slot, swingItem.enabled)
        }else {
            FakeActionManager.stageRightClickSlot(direction[1], direction[2], slot, false)
        }
        lastClicked = System.currentTimeMillis()
    }

    /**
     * Returns the best valid lcm aura target together with the name of the corresponding armorstand.
     * Returns null if no target found.
     */
    private fun getTarget(): Pair<Entity,String>? {
        if (inBoss && spiritBearAura.enabled) {
            val bears = mc.theWorld.loadedEntityList
                .filterIsInstance<EntityOtherPlayerMP>()
                .filter { it.name.contains("Spirit Bear") }
                .sortedBy { mc.thePlayer.getDistanceToEntity(it) }
            val livingBears = bears.filter {
                it.health / it.maxHealth > 0.01
            }.size
            val totalBears = bears.size
            if ((livingBears == 1 && totalBears > 1) || livingBears == 0) return null
            val bear = bears
                .filter { mc.thePlayer.getDistanceToEntity(it) < range.value }
                .filter { mc.thePlayer.getDistanceToEntity(it) < range.value }.getOrNull(0) ?: return null
            return Pair(bear, "Spirit Bear")
        }

        mc.theWorld.loadedEntityList
            .filter {
                ( starMobAura.enabled && it is EntityArmorStand && it.customNameTag.contains("✯"))
                || ( miniBossAura.enabled && it is EntityOtherPlayerMP && it.name.containsOneOf(miniBosses) )
                || ( bloodAura.enabled && currentRoom?.data?.type == RoomType.BLOOD &&
                        ((it is EntityArmorStand && it.customNameTag.containsOneOf(bloodMobs.keys) )
                                || it is EntityGiantZombie ) )

            }
            .filter { mc.thePlayer.getDistanceToEntity(it) < range.value }
            .sortedBy { entity -> entitySelector(entity) }
            .forEach { entity ->
                val possibleTarget = if(entity is EntityArmorStand) {

                    /** Minibosses appear duplicate in the list. Once as star mob name at as an armorstand and once as the OtherPlayerMp entity. */
                    if (entity.customNameTag.contains("Angry Archeologist")
                        || entity.customNameTag.contains("Frozen Adventurer")
                        || entity.customNameTag.contains("Lost Adventurer")
                    ) { return@forEach }


                    val possibleEntities = entity.entityWorld.getEntitiesInAABBexcluding(
                        entity, entity.entityBoundingBox.offset(0.0, -1.0, 0.0)
                    ) { it !is EntityArmorStand }

                    possibleEntities.find {
                        when (it) {
                            is EntityPlayer -> it.getUniqueID()
                                .version() == 2 && it != mc.thePlayer
                            is EntityWither -> false
                            is EntityEnderman -> !it.isInvisible
                            else -> true
                        }
                    }
                }else {
                    entity
                } ?: return@forEach

                /** Visibility Check */
                if (mc.thePlayer.canEntityBeSeen(possibleTarget)) {
                    return Pair(possibleTarget, entity.customNameTag)
                }

            }

        return null
    }

/*
    /**
     * Register a Spirit Bear dying to create a cooldown to prevent boss lock.
     */
    @SubscribeEvent
    fun onEntityDeath(event: EntityRemovedEvent) {
        if (!inDungeons || !inBoss) return
        if (event.entity !is EntityArmorStand) return
        if (event.entity.customNameTag.contains("Spirit Bear")) {
            lastSpiritBearDeath = System.currentTimeMillis()
        }
    }

    /**
     * Register Spirit Bear Appearing.
     */
    @SubscribeEvent
    fun onEntityAppear(event: EntityJoinWorldEvent) {
        if (!inDungeons || !inBoss) return
        if (event.entity !is EntityOtherPlayerMP) return
        if (event.entity.customNameTag.contains("Spirit Bear")) {
            modMessage("Spirit Bear Spawned.")
        }
    }
*/

    /**
     * Selector function for sorting the entities according to distance and priority.
     */
    private fun entitySelector(entity: Entity): Float {
        val priority = when {
            (mc.thePlayer.getDistanceToEntity(entity) > priorityRange.value) -> 0
            entity is EntityGiantZombie -> -100000
            entity.customNameTag.containsOneOf(bloodMobs.keys) -> bloodMobs.entries.let bloodPriority@{ entries ->
                entries.forEach { entry ->
                    if (entity.customNameTag.contains(entry.key)) return@bloodPriority entry.value
                }
                return@bloodPriority -90000
            }
            entity is EntityOtherPlayerMP && entity.name.contains("Shadow Assassin") -> -13000
            entity.name.containsOneOf(miniBosses) -> -12000
            entity.customNameTag.contains("Fel") -> -10000
            entity.customNameTag.contains("Withermancer") -> -8000
            entity.customNameTag.contains("Zombie Commander") -> -7000
            entity.customNameTag.contains("Skeleton Lord") -> -6000
            entity.customNameTag.contains("Zombie Lord") -> -5000
            entity.customNameTag.contains("Skeleton Master") -> -1000
            else -> 0
        }
        return priority + mc.thePlayer.getDistanceToEntity(entity)
    }
}