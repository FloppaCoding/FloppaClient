package floppaclient.module.impl.player

import floppaclient.FloppaClient
import floppaclient.FloppaClient.Companion.inDungeons
import floppaclient.FloppaClient.Companion.mc
import floppaclient.events.ClipFinishEvent
import floppaclient.events.PlaySoundEventPre
import floppaclient.events.PositionUpdateEvent
import floppaclient.events.TeleportEventPre
import floppaclient.floppamap.dungeon.Dungeon
import floppaclient.floppamap.extras.RoomUtils
import floppaclient.module.Category
import floppaclient.module.settings.impl.BooleanSetting
import floppaclient.module.settings.impl.NumberSetting
import floppaclient.utils.DataHandler
import floppaclient.utils.Utils.inF7Boss
import floppaclient.utils.ChatUtils.modMessage
import floppaclient.utils.ClipTools.executeClipRoute
import floppaclient.module.Module
import net.minecraft.client.settings.KeyBinding
import net.minecraft.init.Blocks
import net.minecraft.util.BlockPos
import net.minecraft.util.StringUtils
import net.minecraft.util.Vec3
import net.minecraftforge.client.event.ClientChatReceivedEvent
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent
import java.util.*
import kotlin.concurrent.schedule
import kotlin.math.floor

/**
 * This module enables automatic clip routes. (Patched)
 *
 * @author Aton
 */
object AutoClip : Module(
    "Auto Clip",
    category = Category.PLAYER,
    description = "Automatically initiates a clip route when teleported to predefined start position. Use /addc and /remc " +
            "to edit routes. Use the delay settings to change with which delay it will start." +
            "§eWith clip being basically patched in general there is little use left for this module."
){

    private val chatInfo = BooleanSetting("Chat Info", true, description = "Show a chat message when auto clipping")
    private val onEtherwarp = BooleanSetting("Etherwarp", true, description = "Activate on Etherwarp.")
    private val onPearlTp = BooleanSetting("Pearl", true, description = "Activate on Ender Pearl teleport.")
    private val onSinSeeker = BooleanSetting("Sin Seeker", false, description = "Activate on SinSeeker Scythe teleport.")
    private val onLava = BooleanSetting("Lava", false, description = "Activate when touching lava. Only in Boss.")
    private val onLeap = BooleanSetting("Leap", false, description = "Activate on Spirit Leap. Only in Boss.")
    private val pearlDetectionRange = NumberSetting("Det. Range", 2.0, 1.0,5.0, description = "Max distance from the clip start for Pearls to register.")
    private val bossDetectionRange = NumberSetting("Det. Range Boss", 2.0, 1.0,5.0, description = "Max distance from the clip start for lava and Leaps to register.")
    private val hopperDelay = NumberSetting("Hopper Delay", 200.0, 0.0, 500.0,10.0, description = "Delay when Etherwarping to a Hopper.")
    private val fenceDelay = NumberSetting("Fence Delay", 150.0, 0.0, 500.0,10.0, description = "Delay when Etherwapred to a fence.")
    private val lavaDelay = NumberSetting("Lava Delay", 0.0, 0.0, 200.0,10.0, description = "Activation delay when touching lava.")
    private val defaultDelay = NumberSetting("Default Delay", 30.0, 0.0, 500.0,10.0, description = "Default delay for all other situations.")
    val delayOffset = NumberSetting("Delay Offset", 1.0, 0.0, 5.0,1.0, description = "Start index in the delay chain.")
    val preset = NumberSetting("Boss Preset", 0.0,0.0,9.0,1.0, description = "Choose in between different configs for the boss room.")

    private val fences = listOf(
        Blocks.cobblestone_wall,
        Blocks.oak_fence,
        Blocks.dark_oak_fence,
        Blocks.acacia_fence,
        Blocks.birch_fence,
        Blocks.jungle_fence,
        Blocks.nether_brick_fence,
        Blocks.spruce_fence
    )

    private var doClip = false

    /**
     * Used to determine time since last pearl throw.
     * In particular used as the time window between pearl throw and teleport detection.
     * Gets set to pearlMaxTicks once a pearl throw is detected.
     * Gets reduced by 1 per tick if above 0.
     */
    private var pearlTicks = 0
    private var pearlMaxTicks = 20

    /**
     * Used to determine time since last lava clip.
     * Used to set a cooldown for clips from lava
     * Gets set to lavaMaxTicks once lava is detected.
     * Gets reduced by 1 per tick if above 0.
     */
    private var lavaTicks = 0
    private var lavaMaxTicks = 10

    /**
     * Used when planning the clip route to adjust for different positioning.
     * Used for clipping with pearls and in boss with lava and leaps.
     */
    private var clipType: ClipType = ClipType.DEFAULT

    init {
        this.addSettings(
            chatInfo,
            onEtherwarp,
            onPearlTp,
            onSinSeeker,
            onLava,
            onLeap,
            pearlDetectionRange,
            bossDetectionRange,
            hopperDelay,
            fenceDelay,
            lavaDelay,
            defaultDelay,
            delayOffset,
            preset
        )
    }

    /**
     * Registers Teleport source by the sound
     */
    @SubscribeEvent(priority = EventPriority.HIGH, receiveCanceled = true)
    fun onSoundPlay(event: PlaySoundEventPre) {
        if (doClip) return
        when(event.p_sound.soundLocation.resourcePath) {
            "mob.enderdragon.hit" -> if(onEtherwarp.enabled) {
                doClip = true
                clipType = ClipType.DEFAULT
            }
            "random.burp" -> if (onSinSeeker.enabled) {
                if (event.p_sound.volume == 0.4f && event.p_sound.pitch == 0.61904764f) {
                    doClip = true
                    clipType = ClipType.DEFAULT
                }
            }
            "random.bow" -> {
                /** Pearl detection */
                if (!onPearlTp.enabled) return
                if(event.p_sound.pitch < 0.5 && event.p_sound.volume == 0.5f) {
                    pearlTicks = pearlMaxTicks
                }
            }
        }
    }

    /**
     * Registers teleport by recieved packet.
     * Note: each teleport in skyblock is sent twice.
     */
    @SubscribeEvent(receiveCanceled = true)
    fun onTeleport(event: TeleportEventPre) {
        if (doClip) return
        if (!onPearlTp.enabled) return
        if (pearlTicks <= 0) return
//        if (mc.thePlayer.getDistance(event.packet.x, event.packet.y, event.packet.z) < 0.1) return
        doClip = true
        clipType = ClipType.PEARL
        // important to set this to 0, so that the next teleport packet in the time will not trigger it again
        pearlTicks = 0
    }

    /**
     * Registers whether in lava.
     */
    @SubscribeEvent
    fun onTick(event: ClientTickEvent) {
        if (!onLava.enabled)  return
        if (event.phase != TickEvent.Phase.START) return
        if (!inF7Boss()) return
        if (doClip) return
        if (lavaTicks > 0) return

        if (!mc.thePlayer.isInLava) return
        lavaTicks = lavaMaxTicks
        doClip = true
        clipType = ClipType.LAVA
    }

    /**
     * Registers Leap.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onChat(event: ClientChatReceivedEvent) {
        if (!inDungeons || event.type.toInt() == 2) return
        if (!onLeap.enabled) return
        if (doClip) return
        if (!inF7Boss()) return

        val text = StringUtils.stripControlCodes(event.message.unformattedText)
        when {
            text.startsWith("You have teleported to ") -> {
                /** Get the player you leaped to */
//                val regex = Regex("You have teleported to (.+)")
//                val player = regex.find(text)?.groupValues?.get(1)

                doClip = true
                clipType = ClipType.LEAP
            }
        }
    }

    /**
     * Checks once per Tick whether a clip action is staged, and if so executes it.
     */
    @SubscribeEvent
    fun preWalk(event: PositionUpdateEvent.Pre) {
        if (pearlTicks > 0) pearlTicks--
        if (lavaTicks > 0) lavaTicks--

        if (doClip) {
            autoClip()
            doClip = false
        }
    }

    /**
     * Checks which action to perform at the current coordinates.
     * And calls the corresponding function to perform the clip.
     */
    private fun autoClip() { // triggerd when etherwarp is detected in dungeon
        try { // stop module from breaking when dungoen not scanned
            var startDelay: Int
            val pos = Vec3(floor(mc.thePlayer.posX), floor(mc.thePlayer.posY), floor(mc.thePlayer.posZ))

            /**  Auto Clip */
            val room = Dungeon.currentRoomPair ?: FloppaClient.currentRegionPair ?: return
            if (room.first.isSeparator) return
            RoomUtils.getRoomAutoActionData(room.first)?.run {
                var key = DataHandler.getKey(
                    pos,
                    room.first.x,
                    room.first.z,
                    room.second
                )
                /** check for the start pos in config
                 * Note here the use of .toMutablelist() to force the creation of a new list, so that the clip data does not get overwritten.*/
                var route = this.clips[key]?.toMutableList() ?: mutableListOf()
                /** if it's not in there check adjacent blocks */
                if(clipType.ordinal > 0) kotlin.run adjustClip@{
                    if (!this.clips.containsKey(key)) {
                        // this start position was not found in the config -> check all blocks in range
                        val range = if (clipType == ClipType.PEARL) pearlDetectionRange.value
                                    else bossDetectionRange.value
                        val point1 = BlockPos(pos).add( range,  range,  range)
                        val point2 = BlockPos(pos).add(-range, -range, -range)
                        for (blockPos in BlockPos.getAllInBox(point1, point2)
                            .sortedBy { mc.thePlayer.getDistanceSqToCenter(it) }
                        ) {
                            key = DataHandler.getKey(
                                Vec3(blockPos),
                                room.first.x,
                                room.first.z,
                                room.second
                            )
                            if (this.clips.containsKey(key)) {
                                // this should not be able to be null here
                                route = this.clips[key]?.toMutableList() ?: mutableListOf()
                                break
                            }
                        }
                    }
                    if (route.isEmpty()) return@adjustClip
                    /* adjust the route for the different coordinates
                     * Note here, that the player position vector is shifted by -0.5 in x and z.
                     * That is to adjust for routes assuming the player to start in the middle of the block.
                     * This offset however is removed in the auto clip data by flooring.
                     * As the player position will be used to calculate a difference to the start position
                     * it has to be corrected for this shift.
                     */
                    val relPlayerPos = DataHandler.getRelativeCoords(
                        mc.thePlayer.positionVector.subtract(0.5, 0.0, 0.5),
                        room.first.x,
                        room.first.z,
                        room.second
                    )
                    route[0] = (route.getOrNull(0) ?: 0.0) + key[0] - relPlayerPos.xCoord
                    route[1] = (route.getOrNull(1) ?: 0.0) + key[1] - relPlayerPos.yCoord
                    route[2] = (route.getOrNull(2) ?: 0.0) + key[2] - relPlayerPos.zCoord
                }

                /** return if no clip route was found */
                if (route.isEmpty()) return
                /** a route is defined for this position */
                if(chatInfo.enabled) modMessage("Attempting clip")
                /** setting correct delay */
                startDelay = getStartDelay(pos)
                Timer().schedule(startDelay.toLong()) {
                    mc.thePlayer.motionX = 0.0
                    mc.thePlayer.motionY = 0.0
                    mc.thePlayer.motionZ = 0.0
                    if (clipType == ClipType.LAVA) {
                        KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.keyCode, false)
                        KeyBinding.setKeyBindState(mc.gameSettings.keyBindLeft.keyCode, false)
                        KeyBinding.setKeyBindState(mc.gameSettings.keyBindRight.keyCode, false)
                        KeyBinding.setKeyBindState(mc.gameSettings.keyBindBack.keyCode, false)
                    }
                }
                val eta = executeClipRoute(route,room.second,startDelay)
                Timer().schedule(eta) {
                    MinecraftForge.EVENT_BUS.post(ClipFinishEvent())
                }
            }
        }catch (e: Throwable){ return }
    }

    /**
     * Checks which delay to use according to the block you are on.
     */
    fun getStartDelay (pos: Vec3): Int {
        if (clipType == ClipType.LAVA) return lavaDelay.value.toInt()
        else if(clipType.ordinal > 0) return defaultDelay.value.toInt()
        val position = BlockPos(pos.subtract(0.0,1.0,0.0))
        return when(mc.theWorld.getBlockState(position).block) {
            Blocks.hopper -> hopperDelay.value.toInt()
            in fences -> fenceDelay.value.toInt()
            else -> defaultDelay.value.toInt()
        }
    }

    /**
     * Determins by which action the clip was triggered. Used  to select the correct range and delay check.
     */
    enum class ClipType {
        DEFAULT, PEARL, LAVA, LEAP
    }
}