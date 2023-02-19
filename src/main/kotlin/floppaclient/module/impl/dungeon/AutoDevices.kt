package floppaclient.module.impl.dungeon

import floppaclient.FloppaClient.Companion.inDungeons
import floppaclient.FloppaClient.Companion.mc
import floppaclient.events.BlockStateChangeEvent
import floppaclient.events.PositionUpdateEvent
import floppaclient.module.Category
import floppaclient.module.Module
import floppaclient.module.settings.impl.BooleanSetting
import floppaclient.module.settings.impl.NumberSetting
import floppaclient.module.settings.impl.StringSetting
import floppaclient.utils.GeometryUtils.getDirection
import floppaclient.utils.Utils.inF7Boss
import floppaclient.utils.fakeactions.FakeActionManager
import floppaclient.utils.fakeactions.FakeActionUtils
import floppaclient.utils.inventory.InventoryUtils.isHolding
import floppaclient.utils.inventory.SkyblockItem
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.entity.item.EntityItemFrame
import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraft.item.Item
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.*

object AutoDevices : Module(
    "Auto Devices",
    category = Category.DUNGEON,
    description = "Automatically completes floor 7 devices."
){
    private val simonSays = BooleanSetting("Simon Says", true, description = "Toggle the Simon Says solver.")
    val lights = BooleanSetting("Lights", true, description = "Toggle the Lights solver. §cRequires Secret Aura to be enabled.")
    private val alignment = BooleanSetting("Alignment", true, description = "Toggle the Alignment solver.")
    private val aiming = BooleanSetting("Aiming", true, description = "Toggle the Aiming solver.")
    val lightFixTime = NumberSetting("Fix Time", 400.0, 0.0, 1000.0, 1.0, description = "Delay until it retries to do the device in case it failed. §cMust be greater than your ping.")
    val slot = NumberSetting("Lights Slot", 5.0, 0.0, 7.0, 1.0, description = "The default slot that will be used to click the lights lever when the Item setting is left empty or not found in the hotbar.")
    val itemName = StringSetting("Lights Item", description = "Item to use to click the lights lever. This will take priority over the slot, but if the item is not found the item in the specified slot will be used.")
    private val alignmentReach = NumberSetting("Align Reach", 4.0, 2.0, 6.0, 0.1, description = "Once within this reach the Arrow Alignment device will automatically be solved.")
    private val clicksPerTick = NumberSetting("Clicks Per Tick", 1.0, 1.0, 10.0, 1.0, description = "Determines how fast the arrow align frames will be clicked.")
    private val ssReach = NumberSetting("SS Reach", 6.0, 2.0, 6.0, 0.1, description = "Block reach for the Auto Simon Says solver.")
    private val ssDelay = NumberSetting("SS Delay", 200.0, 50.0, 500.0, 10.0, description = "Delay between Auto SS clicks.")

    init {
        this.addSettings(
            simonSays,
            lights,
            alignment,
            aiming,
            lightFixTime,
            slot,
            itemName,
            alignmentReach,
            clicksPerTick,
            ssReach,
            ssDelay,
        )
    }


    //<editor-fold desc="Alignment">

    private val area = BlockPos.getAllInBox(BlockPos(-2, 125, 79), BlockPos(-2, 121, 75))
        .toList().sortedWith { a, b ->
            if (a.y == b.y) return@sortedWith b.z - a.z
            if (a.y < b.y) return@sortedWith 1
            if (a.y > b.y) return@sortedWith -1
            return@sortedWith 0
        }
    private val neededRotations = HashMap<Pair<Int, Int>, Pair<EntityItemFrame,Int>>()
    private var ticks = 0

    @SubscribeEvent
    fun onPositionUpdate(event: PositionUpdateEvent.Post){
        if (!inDungeons || !alignment.enabled || !inF7Boss()) return

        if (mc.thePlayer.getDistanceSq(BlockPos(-2, 122, 76)) <= 15 * 15) {
            ticks++
            if (ticks % 20 == 0) {
                calculate()
                ticks = 0
            }
            var avaliableClicks = clicksPerTick.value.toInt()
            for (entry in neededRotations) {
                if (avaliableClicks <= 0) break
                if (mc.thePlayer.getDistanceToEntity(entry.value.first) > alignmentReach.value) continue
                val clicks = entry.value.second.coerceAtMost(avaliableClicks)
                if (clicks <= 0 ) continue
                for (ii in 1..clicks) {
                    FakeActionUtils.legitClickEntity(entry.value.first)
                }
                entry.setValue(entry.value.let { Pair(it.first, it.second - clicks) } )
                avaliableClicks -= clicks
            }
        }
    }


    private fun calculate() {
        val frames = mc.theWorld.getEntities(EntityItemFrame::class.java) {
            it != null && area.contains(it.position) && it.displayedItem != null
        }
        if (frames.isNotEmpty()) {
            val solutions = HashMap<Pair<Int, Int>, Int>()
            val maze = Array(5) { IntArray(5) }
            val queue = LinkedList<Pair<Int, Int>>()
            val visited = Array(5) { BooleanArray(5) }
            neededRotations.clear()
            area.withIndex().forEach { (i, pos) ->
                val x = i % 5
                val y = i / 5
                val frame = frames.find { it.position == pos } ?: return@forEach
                // 0 = null, 1 = arrow, 2 = end, 3 = start
                maze[x][y] = when (frame.displayedItem.item) {
                    Items.arrow -> 1
                    Item.getItemFromBlock(Blocks.wool) -> {
                        when (frame.displayedItem.itemDamage) {
                            5 -> 3
                            14 -> 2
                            else -> 0
                        }
                    }
                    else -> 0
                }
                when (maze[x][y]) {
                    1 -> neededRotations[Pair(x, y)] = Pair(frame, frame.rotation)
                    3 -> queue.add(Pair(x, y))
                }
            }
            while (queue.size != 0) {
                val s = queue.poll()
                val directions = arrayOf(intArrayOf(1, 0), intArrayOf(0, 1), intArrayOf(-1, 0), intArrayOf(0, -1))
                for (i in 3 downTo 0) {
                    val x = (s.first + directions[i][0])
                    val y = (s.second + directions[i][1])
                    if (x in 0..4 && y in 0..4) {
                        val rotations = i * 2 + 1
                        if (solutions[Pair(x, y)] == null && maze[x][y] in 1..2) {
                            queue.add(Pair(x, y))
                            solutions[s] = rotations
                            if (!visited[s.first][s.second]) {
                                var neededRotation = neededRotations[s]?.second ?: continue
                                val frame = neededRotations[s]?.first ?: continue
                                neededRotation = rotations - neededRotation
                                if (neededRotation < 0) neededRotation += 8
                                neededRotations[s] = Pair(frame, neededRotation)
                                visited[s.first][s.second] = true
                            }
                        }
                    }
                }
            }
        }
    }

    //</editor-fold>

    //<editor-fold desc="Aiming">

    @SubscribeEvent
    fun onBlockChange(event: BlockStateChangeEvent){
        if (!inDungeons || !aiming.enabled || !inF7Boss()) return
        if (event.pos.z != 50) return
        if (!mc.thePlayer.isHolding(SkyblockItem.Attribute.SHORTBOW)) return
        if (event.newState.block !== Blocks.emerald_block || event.oldState.block !== Blocks.stained_hardened_clay) return
        val direction = mc.thePlayer.getDirection(Vec3(event.pos).addVector(0.5, 1.15, 0.0), -mc.thePlayer.eyeHeight.toDouble())
        FakeActionManager.stageRightClickSlot(direction[1], direction[2])
    }

    //</editor-fold>

    //<editor-fold desc="Simon Says">

    private val ssOrder = mutableListOf<BlockPos>()
    private val startButton = BlockPos(110, 121, 91)
    private var started = false
    private var nextSSClick = System.currentTimeMillis()
    private var lastSSUpdate = 0L

    @SubscribeEvent
    fun onBlockChange2(event: BlockStateChangeEvent){
        if (!inDungeons || !simonSays.enabled || !inF7Boss()) return
        if (event.newState.block !== Blocks.sea_lantern || event.pos.x != 111) return
        // Prevent duplicate
        started = true
        // If it has been so long that you are not in a ss chain reset (The lantern stays for 400ms, it takes 500ms for the buttons to appear after the last lantern went away, and it takes another ~300+ ms for the lights to show up after last button click)
        if (lastSSUpdate + 800 < System.currentTimeMillis()) {
            ssOrder.clear()
        }
        lastSSUpdate = System.currentTimeMillis()
        if (ssOrder.lastOrNull() == event.pos.west()) return
        ssOrder.add(event.pos.west())
    }

    @SubscribeEvent
    fun onPositionUpdate2(event: PositionUpdateEvent.Post) {
        if (!inDungeons || !simonSays.enabled || !inF7Boss()) return

        // Try to start the device
        if (!started) {
            mc.theWorld.loadedEntityList
                .filterIsInstance<EntityArmorStand>()
                .filter { entity -> entity.name.contains("Inactive") }
                .firstOrNull { entity -> entity.getDistanceSq(startButton) < 3*3 }
                ?.run {
                    started = SecretAura.interactWith(startButton, ssReach.value)
                }
        }

        //Click the blocks
        if (ssOrder.isNotEmpty() && mc.theWorld.getBlockState(startButton.south()).block === Blocks.stone_button
            && mc.thePlayer.getDistanceSq(startButton.south(3)) < 8*8
        ) {
            if (System.currentTimeMillis() >= nextSSClick) {
                val success = SecretAura.interactWith(ssOrder.first(), ssReach.value)
                if (!success) return
                nextSSClick = System.currentTimeMillis() + ssDelay.value.toLong() - 5
                ssOrder.removeFirstOrNull()
            }
        }
    }

    //</editor-fold>

    @SubscribeEvent
    fun onWorldChange(event: WorldEvent.Unload){
        ssOrder.clear()
        ticks = 0
        neededRotations.clear()
        started = false
    }
}