package floppaclient.module.impl.dungeon

import floppaclient.FloppaClient.Companion.inDungeons
import floppaclient.FloppaClient.Companion.mc
import floppaclient.events.PositionUpdateEvent
import floppaclient.funnymap.features.dungeon.Dungeon
import floppaclient.module.Category
import floppaclient.module.Module
import floppaclient.module.settings.impl.BooleanSetting
import floppaclient.module.settings.impl.NumberSetting
import floppaclient.module.settings.impl.StringSetting
import floppaclient.utils.Utils
import floppaclient.utils.Utils.equalsOneOf
import floppaclient.utils.ChatUtils.modMessage
import floppaclient.utils.fakeactions.FakeActionUtils
import net.minecraft.block.Block
import net.minecraft.init.Blocks
import net.minecraft.tileentity.TileEntitySkull
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import net.minecraft.util.Vec3i
import net.minecraftforge.client.event.sound.PlaySoundSourceEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

/**
 * Will click secrets that are in range.
 * @author Aton
 */
object SecretAura : Module(
    "Secret Aura",
    category = Category.DUNGEON,
    description = "Automatically clicks dungeon secrets when you get close to them. \n" +
            "The Aura will keep clicking either until the maximum amount of clicks is reached, or the secret is obtained. " +
            "Deactivates in water board."
){

    private val reach = NumberSetting("Reach", 6.0, 1.0, 6.0, 0.1, description = "Maximum distance from where chests and levers will be clicked. 6 should work well.")
    private val essenceReach = NumberSetting("Essence Reach", 5.0, 1.0, 6.0, 0.1, description = "Maximum distance from where Wither Essences be clicked. 5 should work well.")
    private val maxClicks = NumberSetting("Clicks", 5.0, 1.0, 20.0, 1.0, description = "Maximum amount of clicks on a secret.")
    private val sleep = NumberSetting("Sleep", 400.0, 0.0, 1000.0, 1.0, description = "Delay in between clicks. This should be chose higher than your ping, so that the secret is only clicked once if the click was successful.")
    private val slot = NumberSetting("Slot", 5.0, 0.0, 7.0, 1.0, description = "The default slot that will be used to click the secret when the Item setting is left empty or not found in the hotbar.")
    private val itemName = StringSetting("Item", description = "Item to use to click the secrets. This will take priority over the slot, but if the item is not found the item in the specified slot will be used.")
    private val trappedChest = BooleanSetting("Trapped Chests", true, description = "Determines whether trapped chests should be clicked.")
    private val redstoneKey = BooleanSetting("Redstone Key", true, description = "Automatically grabs the Redstone key and places it on the Redstone block.")

    /**
     * stores found secrets as Position mapped to a pair of the secret type save as the Block it is, and the amount
     * of clicks it has received as well as the last tick when it was clicked.
     * confirmation of gotten secrets will be saved as a high amount of clicks.
     */
    private var secrets = mutableMapOf<BlockPos, Pair<Block, MutableList<Long>>>()

    init {
        this.addSettings(
            reach,
            essenceReach,
            maxClicks,
            sleep,
            slot,
            itemName,
            trappedChest,
            redstoneKey
        )
    }


    /**
     * for detection of gotten secrets
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    fun onSoundPlay(event: PlaySoundSourceEvent) {
        if (!inDungeons) return
        val position = Vec3(event.sound.xPosF.toDouble(), event.sound.yPosF.toDouble(), event.sound.zPosF.toDouble())
        val blockPos = BlockPos(position)
        when(event.name) {
            "random.click" -> {
                if (secrets.containsKey(blockPos)) {
                    secrets[blockPos] = secrets[blockPos]?.let { Pair(it.first, mutableListOf(1000,0)) } ?: Pair(Blocks.air, mutableListOf(1000,0))
                }
            }
            "random.chestopen"-> {
                if (secrets.containsKey(blockPos)) {
                    secrets[blockPos] = secrets[blockPos]?.let { Pair(it.first, mutableListOf(1000,0)) } ?: Pair(Blocks.air,mutableListOf(1000,0))
                }
            }
        }
    }
    /* Secret sounds
    // LEVER:
    // lever sound: random.anvil_break; at player position, slightly different x and y decimals; played once
    // followed by: random.click; at lever position in the middle of the Block; played once
    // followed by: random.wood_click; at some weird position; played 9 times
    // CHEST:
    // chest sound: random.chestopen; played twice, first at the blocks blockPos, then in the center of the Block
    // WITHER ESSENCE
    // essence sound: random.orb; at the player Position with slightly different decimals; played twice and a bit delayed
    // ITEM PICKUP
    // pickup sound: random.pop; played a bit away from the player but at same y, should be within like 1 to 2 blocks range; played once
    // BAT
    // kill sound: mob.bat.death; at bat position probably; played once
    // followed by random.orb, played 4 times
    */

    /**
     * Initiates the scan for secrets in range
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun onTick(event: PositionUpdateEvent.Post)  {
        if (mc.thePlayer == null || !inDungeons) return
        val room = Dungeon.currentRoom
        if (room?.data?.name == "Water Board") return
        if (room?.data?.name == "Three Weirdos" && !AutoWeirdos.enabled) return
        if (( room?.data?.name == "Blaze"
                    || room?.data?.name == "Blaze 2")
            && !(mc.thePlayer.posY.toInt() == 67 || mc.thePlayer.posY < 25 || mc.thePlayer.posY > 110) ) return
        // These checks might be better performed in the method responsible for clicking the secret

        try {
            // For whatever reason this keeps breaking
            val scanRange = Vec3i(10, 10, 10)
            BlockPos.getAllInBox(
                mc.thePlayer.position.subtract(scanRange),
                mc.thePlayer.position.add(scanRange)
            )
                .forEach {
                    handleBlock(it)
                }
        } catch (e: Exception) {
            modMessage("Aura Error! This should not have happened.")
        }
    }

    /**
     * Check whether a block is a secret and stores it in the secrets list.
     * If all criteria are met clicks the secret.
     */
    private fun handleBlock(blockPos: BlockPos) {
        // add the block to the list of secrets if it's not on there
        val block = mc.theWorld.getBlockState(blockPos).block
        if (!secrets.containsKey(blockPos)){
            if (block == Blocks.chest || block == Blocks.lever ||
                (block == Blocks.trapped_chest && trappedChest.enabled)
                || (block == Blocks.redstone_block && redstoneKey.enabled && Utils.findItem("Redstone Key", inInv = true) != null)
            ){
                secrets[blockPos] = Pair(block,mutableListOf(0, System.currentTimeMillis()-10000))
            }else if (block == Blocks.skull) {
                val tileEntity: TileEntitySkull = mc.theWorld.getTileEntity(blockPos) as TileEntitySkull
                val id = tileEntity.playerProfile?.id?.toString()
                if (id == "26bb1a8d-7c66-31c6-82d5-a9c04c94fb02" || (id == "edb0155f-379c-395a-9c7d-1b6005987ac8" && redstoneKey.enabled)) {
                    secrets[blockPos] = Pair(block,mutableListOf(0, System.currentTimeMillis()-10000))
                }
            }
        }
        // if the block is in the list and has not been clicked to many times, nor to recently
        // then distance will be checked and then it will be clicked
        // It also is checked whether the block is air, to account for mimic chests and essences disappearing

        if(Dungeon.inBoss) {
            // custom aura when in boss, this is very redundant and can definetly be dont more elegant, but this way gives me more control over it.

            // Check for lights device.
            val southBlock = mc.theWorld.getBlockState(blockPos.south()).block
            if (block == Blocks.lever && southBlock.equalsOneOf(Blocks.lit_redstone_lamp, Blocks.redstone_lamp)) {
                if (AutoDevices.enabled && AutoDevices.lights.enabled) {
                    if (southBlock != Blocks.lit_redstone_lamp && shouldClickBlock(block, blockPos, AutoDevices.lightFixTime.value)) {
                        tryInteract(blockPos, itemSlot = AutoDevices.slot.value.toInt(), itemName2 = AutoDevices.itemName.text)
                    }
                }
            }else if ( shouldClickBlock(block, blockPos, sleep.value) ) {
                tryInteract(blockPos)
            }

        } else{
            if ( shouldClickBlock(block, blockPos, sleep.value) ) {
                val blockReach = when (secrets[blockPos]?.first) {
                    Blocks.skull -> essenceReach.value
                    else -> reach.value
                }
                // The reach check for essences does not factor in the players eye height.
                val yOffs = when (secrets[blockPos]?.first) {
                    Blocks.skull -> 0.0
                    else -> mc.thePlayer.eyeHeight.toDouble()
                }
                tryInteract(blockPos, blockReach, yOffs)
            }
        }
    }
    // Profile id for essence skulls: 26bb1a8d-7c66-31c6-82d5-a9c04c94fb02

    /*
    Redstone key Skull:
    com.mojang.authlib.GameProfile@538f3d27[id=edb0155f-379c-395a-9c7d-1b6005987ac8,name=<null>,properties={textures=[com.mojang.authlib.properties.Property@1d7b3ef0]},legacy=false]
    Profile id:     edb0155f-379c-395a-9c7d-1b6005987ac8
     */

    private fun shouldClickBlock(block: Block, blockPos: BlockPos, sleepTime: Double): Boolean{
        return block != Blocks.air
                &&(secrets[blockPos]?.second?.get(0) ?: 1000) < maxClicks.value
                && (System.currentTimeMillis() - (secrets[blockPos]?.second?.get(1) ?: Long.MAX_VALUE) )>= sleepTime
    }

    /**
     * performs a range check and clicks the block. Updates the secrets list.
     */
    private fun tryInteract(blockPos: BlockPos, blockReach: Double = reach.value, yOffs: Double = mc.thePlayer.eyeHeight.toDouble(), itemSlot: Int = slot.value.toInt(), itemName2: String = itemName.text) {
        // Distance check It seems like hypixel checks the distance not to the center of the Block, but to the corner / it's blockPos
        if (mc.thePlayer.getDistance(blockPos.x.toDouble(), blockPos.y.toDouble() - yOffs, blockPos.z.toDouble()) < blockReach ) {
            interactWith(blockPos, 10.0, itemSlot, itemName2)
            // This is a bit awkward because i have to avoid null pointers even tho they are not possible here
            secrets[blockPos] = secrets[blockPos]?.let { Pair(it.first, mutableListOf( it.second[0] + 1, System.currentTimeMillis())) } ?: Pair(Blocks.air,mutableListOf(1000,0))
        }
    }

    /**
     * Right clicks the specified block with the aura item.
     */
    fun interactWith(blockPos: BlockPos, reach: Double? = null, itemSlot: Int = slot.value.toInt(), itemName2: String = itemName.text): Boolean {
        val newReach = reach ?: this.reach.value
        val block = mc.theWorld.getBlockState(blockPos).block
        val clicked = if (block == Blocks.redstone_block) {
            FakeActionUtils.clickBlockWithItem(blockPos, itemSlot, "Redstone Key", fromInv = true, abortIfNotFound = true)
        }else {
            FakeActionUtils.clickBlockWithItem(blockPos, itemSlot, itemName2, newReach)
        }
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