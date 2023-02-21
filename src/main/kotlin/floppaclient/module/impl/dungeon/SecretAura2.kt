package floppaclient.module.impl.dungeon

import floppaclient.FloppaClient.Companion.inDungeons
import floppaclient.FloppaClient.Companion.mc
import floppaclient.events.PositionUpdateEvent
import floppaclient.floppamap.dungeon.Dungeon
import floppaclient.module.AlwaysActive
import floppaclient.module.Category
import floppaclient.module.Module
import floppaclient.module.settings.Setting.Companion.withDependency
import floppaclient.module.settings.impl.BooleanSetting
import floppaclient.module.settings.impl.NumberSetting
import floppaclient.module.settings.impl.SelectorSetting
import floppaclient.module.settings.impl.StringSetting
import floppaclient.utils.ChatUtils
import floppaclient.utils.RenderObject
import floppaclient.utils.Utils
import floppaclient.utils.fakeactions.FakeActionUtils
import net.minecraft.init.Blocks
import net.minecraft.tileentity.TileEntitySkull
import net.minecraft.util.BlockPos
import net.minecraft.util.MovingObjectPosition
import net.minecraft.util.Vec3i
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.client.event.sound.PlaySoundSourceEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.awt.Color

/**
 * Will click secrets that are in range.
 * @author Aton, Stivais
 */

// TODO redo description and add stonkless
@AlwaysActive
object SecretAura2 : Module(
    "Secret Aura",
    category = Category.DUNGEON,
    description = "Automatically clicks dungeon secrets when you get close to them. \n" +
            "The Aura will keep clicking either until the maximum amount of clicks is reached, or the secret is obtained. " +
            "Deactivates in water board." +
            "Deactivates in Three Weirdos if you have Auto Weirdos disabled"
) {
    private val mode = SelectorSetting("Mode", "Aura", arrayListOf("Aura", "Triggerbot"))
    private val maxClicks = NumberSetting("Clicks", 5.0, 1.0, 20.0, 1.0, description = "Maximum amount of clicks on a secret.")
    private val sleep = NumberSetting("Sleep", 400.0, 0.0, 1000.0, 1.0, description = "Delay in between clicks. This should be chose higher than your ping, so that the secret is only clicked once if the click was successful.")
    private val trappedChest = BooleanSetting("Trapped Chests", true, description = "Determines whether trapped chests should be clicked.")
    private val redstoneKey = BooleanSetting("Redstone Key", true, description = "Automatically grabs the Redstone key and places it on the Redstone block.")

    // settings only for secret aura and in future stonkless
    private val reach = NumberSetting("Reach", 6.0, 1.0, 6.0, 0.1, description = "Maximum distance from where chests and levers will be clicked. 6 should work well.")
        .withDependency { this.mode.index == 0 }
    private val essenceReach = NumberSetting("Essence Reach", 5.0, 1.0, 6.0, 0.1, description = "Maximum distance from where Wither Essences be clicked. 5 should work well.")
        .withDependency { this.mode.index == 0 }
    private val slot = NumberSetting("Slot", 5.0, 0.0, 7.0, 1.0, description = "The default slot that will be used to click the secret when the Item setting is left empty or not found in the hotbar.")
        .withDependency { this.mode.index == 0 }
    private val itemName = StringSetting("Item", description = "Item to use to click the secrets. This will take priority over the slot, but if the item is not found the item in the specified slot will be used.")
        .withDependency { this.mode.index == 0 }
    private val renderBox = BooleanSetting("Render box", true, description = "Will render a box for secrets in reach")
        .withDependency { this.mode.index == 0 }

    private val swingItem = BooleanSetting("Swing Item", true, description = "Will swing your item when clicking a secret")
        .withDependency { this.mode.index == 1 }

    /**
     * stores found secrets as Position mapped to a pair of the secret type save as the Block it is, and the amount
     * of clicks it has received as well as the last tick when it was clicked.
     * confirmation of gotten secrets will be saved as a high amount of clicks.
     */
    private var secrets = mutableMapOf<BlockPos, MutableList<Long>>()

    init {
        this.addSettings(
            mode,
            trappedChest,
            redstoneKey,
            maxClicks,
            sleep,
            reach,
            essenceReach,
            slot,
            itemName,
            renderBox,
            swingItem,
        )
    }

    /**
     * for detection of gotten secrets
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    fun onSoundPlay(event: PlaySoundSourceEvent) {
        if (!inDungeons) return
        val blockPos = BlockPos(event.sound.xPosF.toDouble(), event.sound.yPosF.toDouble(), event.sound.zPosF.toDouble())
        when(event.name) {
            "random.click" -> if (secrets.containsKey(blockPos)) secrets[blockPos] = secrets[blockPos]?.let { mutableListOf(1000,0) } ?: mutableListOf(1000,0)
            "random.chestopen"-> if (secrets.containsKey(blockPos)) secrets[blockPos] = secrets[blockPos]?.let { mutableListOf(1000,0) } ?: mutableListOf(1000,0)
        }
    }

    /**
     * Initiates the scan for secrets in range
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun onTick(event: PositionUpdateEvent.Post)  {
        if (mc.thePlayer == null || !inDungeons) return
        val room = Dungeon.currentRoom?.data
        if (room?.name == "Water Board" || (room?.name == "Three Weirdos" && !AutoWeirdos.enabled)) return
        if ((room?.name == "Blaze" || room?.name == "Blaze 2") && !(mc.thePlayer.posY.toInt() == 67 || mc.thePlayer.posY < 25 || mc.thePlayer.posY > 110) ) return
        // These checks might be better performed in the method responsible for clicking the secret

        try {
            val scanRange = Vec3i(10, 10, 10)
            BlockPos.getAllInBox(
                mc.thePlayer.position.subtract(scanRange),
                mc.thePlayer.position.add(scanRange)
            )
                .forEach {
                    handleBlock(it)
                }
        } catch (e: Exception) {
            ChatUtils.modMessage("Aura Error! This should not have happened.")
        }
        // secret triggerbot
        if (this.mode.index == 1 && this.enabled && mc.objectMouseOver?.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            if (!mc.thePlayer.isSneaking && swingItem.enabled) mc.thePlayer.swingItem()
            tryInteract(mc.objectMouseOver.blockPos, itemName = mc.thePlayer.heldItem.displayName)
        }
    }

    @SubscribeEvent
    fun onWorldRender(event: RenderWorldLastEvent) {
        if (this.enabled && this.renderBox.enabled && this.mode.index == 0)
        secrets.keys.forEach {
            val color = if ((secrets[it]?.get(0) ?: 1000) < maxClicks.value) Color(204, 204, 204) else Color(255, 0, 0)
            if (inDistance(it)) RenderObject.drawBoxAtBlock(it, color, true, opacity = 0.15f)
        }
    }

    /**
     * Check whether a block is a secret and stores it in the secrets list.
     * If all criteria are met, it will click the secret.
     */
    private fun handleBlock(blockPos: BlockPos) {

        val block = mc.theWorld.getBlockState(blockPos).block
        if (!secrets.containsKey(blockPos)) {

            if (block == Blocks.chest || block == Blocks.lever || (block == Blocks.trapped_chest && trappedChest.enabled)
                || (block == Blocks.redstone_block && redstoneKey.enabled && Utils.findItem("Redstone Key", inInv = true) != null))
                secrets[blockPos] = mutableListOf(0, System.currentTimeMillis() - 10000)

            else if (block == Blocks.skull) {
                val tileEntity: TileEntitySkull = mc.theWorld.getTileEntity(blockPos) as TileEntitySkull
                val id = tileEntity.playerProfile?.id?.toString()
                if (id == "26bb1a8d-7c66-31c6-82d5-a9c04c94fb02" || (id == "edb0155f-379c-395a-9c7d-1b6005987ac8" && redstoneKey.enabled))
                    secrets[blockPos] = mutableListOf(0, System.currentTimeMillis() - 10000)
            }
        }

        if (Dungeon.inBoss) { // Aura for boss, will work without secret aura on because of the always active annotation
            if (blockPos.z == 142 && block == Blocks.lever && AutoDevices.enabled && AutoDevices.lights.enabled) {
                    if (mc.theWorld.getBlockState(blockPos.south()).block != Blocks.lit_redstone_lamp)
                        tryInteract(blockPos, itemSlot = AutoDevices.slot.value.toInt(), itemName = AutoDevices.itemName.text, sleepTime = AutoDevices.lightFixTime.value)
            } else {
                if (this.enabled) tryInteract(blockPos)
            }
        } else {
            if (this.enabled && this.mode.index == 0) tryInteract(blockPos)
        }
    }

    /**
     * Performs a distance check, includes wither essence range check
     */
    private fun inDistance(blockPos: BlockPos) : Boolean {
        val block = mc.theWorld.getBlockState(blockPos).block
        val blockReach = if (secrets[blockPos] == Blocks.skull) essenceReach.value else reach.value
        val yOffs = if (secrets[blockPos] == Blocks.skull) 0.0 else mc.thePlayer.eyeHeight.toDouble()

        return (block != Blocks.air && block != Blocks.iron_bars && mc.thePlayer.getDistance(blockPos.x.toDouble(), blockPos.y.toDouble() - yOffs, blockPos.z.toDouble()) < blockReach)
    }

    /**
     * Checks if the secret hasn't been clicked recently or hasn't been clicked too many times and clicks the chest
     */
    private fun tryInteract(blockPos: BlockPos, blockReach: Double = reach.value , itemSlot: Int = slot.value.toInt(), itemName: String = this.itemName.text, sleepTime: Double = this.sleep.value) {
       if (inDistance(blockPos) && (secrets[blockPos]?.get(0) ?: 1000) < maxClicks.value
           && (System.currentTimeMillis() - (secrets[blockPos]?.get(1) ?: Long.MAX_VALUE)) >= sleepTime) {
                interactWith(blockPos, blockReach, itemSlot, itemName)
                secrets[blockPos] = secrets[blockPos]?.let { mutableListOf(it[0] + 1, System.currentTimeMillis()) } ?: mutableListOf(1000, 0)
        }
    }

    /**
     * Right-clicks the specified block with the aura item.
     */
    fun interactWith(blockPos: BlockPos, reach: Double = this.reach.value, itemSlot: Int = slot.value.toInt(), itemName: String = this.itemName.text): Boolean {
        val block = mc.theWorld.getBlockState(blockPos).block
        val clicked = if (block == Blocks.redstone_block) FakeActionUtils.clickBlockWithItem(blockPos, itemSlot, "Redstone Key", fromInv = true, abortIfNotFound = true)
        else FakeActionUtils.clickBlockWithItem(blockPos, itemSlot, itemName, reach)

        if (SecretChime.enabled && clicked) SecretChime.playSecretSound()
        return clicked
    }

    /**
     * Reset secrets list for the next dungeon
     */
    @SubscribeEvent
    fun onWorldChange(event: WorldEvent.Load) {
        secrets.clear()
    }
}