package floppaclient.module.impl.dungeon

import floppaclient.FloppaClient.Companion.inDungeons
import floppaclient.FloppaClient.Companion.mc
import floppaclient.funnymap.features.dungeon.Dungeon
import floppaclient.module.Category
import floppaclient.module.Module
import floppaclient.utils.Utils.modMessage
import floppaclient.utils.fakeactions.FakeActionUtils
import kotlinx.coroutines.runBlocking
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.init.Blocks
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import net.minecraft.util.StringUtils.stripControlCodes
import net.minecraftforge.client.event.ClientChatReceivedEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent

/**
 * Module to automatically solve the three weirdos puzzle and remove the wrong chest.
 * @author Aton
 */
object AutoWeirdos : Module(
    "Auto Weirdos",
    category = Category.DUNGEON,
    description = "Automatically solves the three weirdos puzzle. Removes the chests in the Room, " +
            "clicks the weirdos once in range and places the correct chest back. The solver does not click the " +
            "correct chest for you, enable secret aura for that."
){

    private val solutions = listOf("The reward isn't in any of our chests.",
        "The reward is not in my chest!",
        "My chest doesn't have the reward. We are all telling the truth.",
        "My chest has the reward and I'm telling the truth!",
        "Both of them are telling the truth.",
        "At least one of them is lying, and the reward is not in"
    )
    private var bozos = mutableListOf<String>()
    private var clickedBozos = mutableListOf<Int>()
    private var removedChests = mutableListOf<BlockPos>()
    private var correctBozo: String? = null
    private var correctChest: BlockPos? = null

    /**
     * Used to check incoming chat messages for solutions to the three weirdos puzzle.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onChat(event: ClientChatReceivedEvent) {
        if (event.type.toInt() == 2 || !inDungeons) return
        val room = Dungeon.room ?: return
        if (room.data.name != "Three Weirdos") return
        val unformatted = stripControlCodes(event.message.unformattedText)
        if (unformatted.contains("[NPC]")) {
            val npcName = unformatted.substring(unformatted.indexOf("]") + 2, unformatted.indexOf(":"))
            var isSolution = false
            for (solution in solutions) {
                if (unformatted.contains(solution)) {
                    modMessage("§c§l${stripControlCodes(npcName)} §2has the blessing.")
                    isSolution = true
                    correctBozo = npcName
                    break
                }
            }
            // if the NPC message does not match a solution add that npcs name to the bozo list
            if (!isSolution) {
                if (!bozos.contains(npcName)) {
                    bozos.add(npcName)
                }
            }
        }
    }

    /**
     * Handles the removal of the chests in the room and puts the correct chest back.
     */
    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) = runBlocking {
        if (event.phase != TickEvent.Phase.START || !inDungeons) return@runBlocking
        val room = Dungeon.room ?: return@runBlocking
        if (room.data.name != "Three Weirdos") return@runBlocking

        // get armorstands
        mc.theWorld.loadedEntityList.stream()
            .filter { entity ->
                entity is EntityArmorStand && entity.hasCustomName()
                        && entity.getDistance(room.x.toDouble(), entity.posY, room.z.toDouble()) < 15
            }.forEach { entity ->

                // make ghost blocks
                for (direction in EnumFacing.HORIZONTALS) {
                    val potentialPos = entity.position.offset(direction)
                    if (correctBozo?.let { entity.customNameTag.contains(it) } == true) {
                    // this is the npc with the blessing
                        if (removedChests.contains(potentialPos)
                            && mc.theWorld.getBlockState(potentialPos).block === Blocks.air) {
                            // the chest has been removed here
                            mc.theWorld.setBlockState(potentialPos, Blocks.chest.defaultState)
                            correctChest = potentialPos
                        }
                    }else {
                    // solution not found yet or wrong npc
                    // might be good to add a check for CLICK as name, to only create ghost blocks next to the
                    // actual weirdos. Otherwise, complications with mobs / player in the room or in an adjacent room
                    // possible
                        if (mc.theWorld.getBlockState(potentialPos).block === Blocks.chest && correctChest != potentialPos) {
                            mc.theWorld.setBlockToAir(potentialPos)
                            removedChests.add(potentialPos)
                            break
                        }
                    }
                }

                // only click once on weirdos
                if (clickedBozos.contains(entity.entityId) || mc.thePlayer.getDistance(entity.posX, entity.posY, entity.posZ) > 5) return@forEach
                FakeActionUtils.clickEntity(entity.entityId)
                clickedBozos.add(entity.entityId)
            }



    }

    /**
     * Resets the values when changing world.
     */
    @SubscribeEvent
    fun onWorldChange(event: WorldEvent.Load) {
        bozos = mutableListOf()
        clickedBozos = mutableListOf()
        removedChests = mutableListOf<BlockPos>()
        correctBozo = null
        correctChest = null
    }
}