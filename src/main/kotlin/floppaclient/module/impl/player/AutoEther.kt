package floppaclient.module.impl.player

import floppaclient.FloppaClient
import floppaclient.FloppaClient.Companion.inSkyblock
import floppaclient.FloppaClient.Companion.mc
import floppaclient.events.*
import floppaclient.floppamap.dungeon.Dungeon
import floppaclient.floppamap.extras.EditMode
import floppaclient.floppamap.utils.RoomUtils
import floppaclient.module.Category
import floppaclient.module.Module
import floppaclient.module.settings.Visibility
import floppaclient.module.settings.impl.BooleanSetting
import floppaclient.module.settings.impl.NumberSetting
import floppaclient.utils.DataHandler
import floppaclient.utils.Utils.flooredPosition
import floppaclient.utils.Utils.isHolding
import floppaclient.utils.ChatUtils.modMessage
import floppaclient.utils.fakeactions.FakeActionUtils
import net.minecraft.network.play.client.C03PacketPlayer.C06PacketPlayerPosLook
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import net.minecraft.util.Vec3i
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.*
import kotlin.concurrent.schedule

/**
 * This modules enables you to automatically etherwarp along predefined paths in Skyblock.
 *
 * @author Aton
 */
object AutoEther : Module(
    "Auto Etherwarp",
    category = Category.PLAYER,
    description = "Automatically etherwarps to predefined positions when left clicking the AOTV in range of a starting point." +
            "Use commands /ad and /rm to edit your routes."
) {

    private val chatInfo = BooleanSetting("Chat Info", true, description = "Show a message in chat when etherwarping.")
    private val registerSecret = BooleanSetting("Register Secret", false, description = "Attempts to auto etherwarp when you pick up a secret. §eRequires Secret Chimes to be enabled.")
    private val delayAfterSecret = NumberSetting("Secret Delay", 70.0, 0.0, 500.0, 10.0, description = "Delay in ms when auto etherwarp is attempted after getting a secret.")
    private val detectionRange = NumberSetting("Det. Range", 2.0, 1.0, 10.0, description = "Max distance from a start point to register the Auto Etherwarp.")
    private val chainEther = BooleanSetting("Chain Warps", true, description = "Automatically perform the next Etherwarp, when it starts at the same block where the last one ended.")
    private val checkCooldown = BooleanSetting("Cooldown", true, description = "Puts a cooldown on activations.")
    private val pingless = BooleanSetting("Pingless", false, description = "Pre moves client side before the teleport packet is received. Only for chains.")
    private val packetLimit = NumberSetting("Max Packets", 10.0, 1.0, 15.0, 1.0, visibility = Visibility.HIDDEN, description = "Sets the limit for overflow packets.")
    private val visibilityCheck = BooleanSetting("Visibility Check", true, description = "Will perform a visibility check for pingless routes.")
    private val debugMessages = BooleanSetting("Debug Messages", false, description = "Shows debug messages for fake responses.")
    private val blockClick = BooleanSetting("Cancel click", false, description = "Cancels left clicks with ATOV")
    private val noPinglessOnDouble = BooleanSetting("Normal on Double", true, description = "Disables pingless mode when you are not in a chain longer than 2.")

    private var tryEther = false
    private var inChain = false

    private var cooldownTicks = 0
    private const val maxCD = 10

    /**
     * Used for when a pingless chain is broken.
     */
    private var nextStartPos: Pair<BlockPos, Long>? = null

    private val fakeTpList = mutableListOf<Pair<C06PacketPlayerPosLook, Long>>()
    private var overflowPackets: Int = 0
    private var lastReduction = 0L
    private const val reductionCooldown = 30 * 1000

    init {
        this.addSettings(
            chatInfo,
            registerSecret,
            delayAfterSecret,
            detectionRange,
            chainEther,
            checkCooldown,
            pingless,
            packetLimit,
            visibilityCheck,
            debugMessages,
            blockClick,
            noPinglessOnDouble
        )
    }

    /**
     * For left click detection with aotv to activate the action.
     */
    @SubscribeEvent
    fun onLeftClick(event: ClickEvent.LeftClickEvent) {
        if (!inSkyblock || EditMode.enabled || cooldownTicks > 0) return
        if (!mc.thePlayer.isHolding("Aspect of the Void")) return
        tryEther = true
        if (blockClick.enabled) event.isCanceled = true
    }

    /**
     * For activation on gotten secret.
     */
    @SubscribeEvent
    fun onSecret(event: DungeonSecretEvent) {
        if (!registerSecret.enabled) return
        if (!inSkyblock || EditMode.enabled || cooldownTicks > 0) return
        Timer().schedule(delayAfterSecret.value.toLong()) { tryEther = true }
    }

    /**
     * Registers Teleport source by the sound
     */
    @SubscribeEvent(priority = EventPriority.HIGH, receiveCanceled = true)
    fun onSoundPlay(event: PlaySoundEventPre) {
        if (tryEther) return
        when (event.p_sound.soundLocation.resourcePath) {
            "mob.enderdragon.hit" -> {

                if (inChain && cooldownTicks > 0) {
                    tryEther = true
                    inChain = false
                }else if(mc.thePlayer.flooredPosition == nextStartPos?.first && (nextStartPos?.second ?: 0) > System.currentTimeMillis()) {
                    tryEther = true
                    nextStartPos = null
                }
                cooldownTicks = 0
            }
        }
    }

    /**
     * Checks once per Tick whether a clip action is staged, and if so executes it.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun preWalk(event: PositionUpdateEvent.Pre) {
        if (overflowPackets > 0 && lastReduction + reductionCooldown < System.currentTimeMillis()) {
            overflowPackets--
            lastReduction = System.currentTimeMillis()
        }
        if (cooldownTicks > 0) cooldownTicks--
        if (!tryEther) return
        tryEtherWarp()
        tryEther = false
    }

    /**
     * Checks which action to perform at the current coordinates.
     * And calls the corresponding function to perform the clip.
     */
    private fun tryEtherWarp() {
        try { // stop module from breaking when dungoen not scanned

            var shouldDoPingless = pingless.enabled && overflowPackets <= packetLimit.value

            val pos = mc.thePlayer.position
            var target: BlockPos? = null

            val room = Dungeon.currentRoomPair ?: FloppaClient.currentRegionPair ?: return
            if (room.first.isSeparator) return

            var key: MutableList<Int>
            var newkey: MutableList<Int> = mutableListOf()

            RoomUtils.getRoomAutoActionData(room.first)?.run {
                /** check for the start pos in config
                 * Note here that a new BlockPos instace is created, so that the Etherwarp data can not be overwritten.*/
                val range = detectionRange.value
                val point1 = pos.add(range, range, range)
                val point2 = pos.add(-range, -range, -range)
                for (blockPos in BlockPos.getAllInBox(point1, point2)
                    .sortedBy { mc.thePlayer.getDistanceSqToCenter(it) }
                ) {
                    key = DataHandler.getKey(
                        Vec3(blockPos),
                        room.first.x,
                        room.first.z,
                        room.second
                    )

                    if (this.etherwarps.containsKey(key)) {
                        /** getOrDefault should always be able to get here
                         * A new BlockPos instance is created to make sure that the data can only be read here.*/
                        val rawTarget = BlockPos(this.etherwarps.getOrDefault(key, BlockPos(0, 0, 0)))
                        target = BlockPos(
                            DataHandler.getRotatedCoords(
                                Vec3(rawTarget), room.second
                            )
                                .addVector(room.first.x.toDouble(), 0.0, room.first.z.toDouble())
                        )

                        //Check for chained etherwarps
                        if (chainEther.enabled) {
                            newkey = mutableListOf(rawTarget.x, rawTarget.y, rawTarget.z)
                            if (this.etherwarps.containsKey(newkey)) {
                                inChain = true
                                // Check for chain of 2
                                val rawTarget2 = BlockPos(this.etherwarps.getOrDefault(newkey, BlockPos(0, 0, 0)))
                                val newkey2 = mutableListOf(rawTarget2.x, rawTarget2.y, rawTarget2.z)
                                if (!this.etherwarps.containsKey(newkey2) && noPinglessOnDouble.enabled) {
                                    // chain has length 2
                                    shouldDoPingless = false
                                }
                            }
                        }
                        break
                    }
                }
            }

            // If target found etherwarp there
            if (target == null) return
            cooldownTicks = maxCD
            // Stop movement.
            mc.thePlayer.motionX = 0.0
            mc.thePlayer.motionY = -0.0784000015258789
            mc.thePlayer.motionZ = 0.0
            FakeActionUtils.etherwarpTo(target!!, true, inChain && shouldDoPingless)

            // handle the chain in case pingless is active
            if (inChain && shouldDoPingless && newkey.isNotEmpty()) {
                RoomUtils.getRoomAutoActionData(room.first)?.run {
                    val max = packetLimit.value.toInt() - overflowPackets
                    for (ii in 0..max) {
                        if (!inChain) break

                        // to Mutable list to create a copy
                        key = newkey.toMutableList()
                        @Suppress("UNUSED_VARIABLE") var start: Vec3 //For some weird reason the IDE says that this is not used.

                        if (this.etherwarps.containsKey(key)) {
                            /** getOrDefault should always be able to get here
                             * A new BlockPos instance is created to make sure that the data can only be read here.*/
                            val rawTarget = BlockPos(this.etherwarps.getOrDefault(key, BlockPos(0, 0, 0)))
                            target = BlockPos(
                                DataHandler.getRotatedCoords(
                                    Vec3(rawTarget), room.second
                                )
                                    .addVector(room.first.x.toDouble(), 0.0, room.first.z.toDouble())
                            )

                            // break if limit is reached
                            if (ii == max) {
                                nextStartPos = Pair(target!!, System.currentTimeMillis() + 500L)
                                inChain = false
                                break
                            }

                            val rawStart = Vec3(Vec3i(key[0], key[1], key[2]))
                            start = DataHandler.getRotatedCoords(rawStart, room.second)
                                .addVector(room.first.x.toDouble() + 0.5, 1.0, room.first.z.toDouble() + 0.5)


                            //Check for chained etherwarps
                            newkey = mutableListOf(rawTarget.x, rawTarget.y, rawTarget.z)
                            inChain = this.etherwarps.containsKey(newkey)

                        } else return // This return should never be reached

                        val flag = if(visibilityCheck.enabled)
                            FakeActionUtils.tryEtherwarp(start, target!!, true, true, true)
                        else
                            FakeActionUtils.forceEtherwarp(start, target!!, true, true, true)
                        inChain = inChain && flag
                    }

                }
            }
        } catch (e: Throwable) {
            return
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onTeleportPacket(event: TeleportEventPre) {
        if (!pingless.enabled) return
        val sizeBefore = fakeTpList.size
        fakeTpList.removeIf {
            it.second < System.currentTimeMillis() - 1000
        }
        overflowPackets += sizeBefore - fakeTpList.size
        if (debugMessages.enabled) modMessage("Teleported to ${event.packet.x}, ${event.packet.y}, ${event.packet.z} (${event.packet.yaw} / ${event.packet.pitch})")

        // Check whether the packet was alr faked

        for (fakedPacketPair in fakeTpList) {

            var doesPacketMatch = true

            val fakePacket = fakedPacketPair.first
            val packetIn = event.packet

            if (!packetIn.func_179834_f().contains(S08PacketPlayerPosLook.EnumFlags.X)) {
                doesPacketMatch = (packetIn.x == fakePacket.positionX) && doesPacketMatch
            }

            if (!packetIn.func_179834_f().contains(S08PacketPlayerPosLook.EnumFlags.Y)) {
                doesPacketMatch = (packetIn.y == fakePacket.positionY) && doesPacketMatch
            }

            if (!packetIn.func_179834_f().contains(S08PacketPlayerPosLook.EnumFlags.Z)) {
                doesPacketMatch = (packetIn.z == fakePacket.positionZ) && doesPacketMatch
            }

            if (!packetIn.func_179834_f().contains(S08PacketPlayerPosLook.EnumFlags.X_ROT)) {
                doesPacketMatch = (packetIn.pitch == fakePacket.pitch) && doesPacketMatch
            }

            if (!packetIn.func_179834_f().contains(S08PacketPlayerPosLook.EnumFlags.Y_ROT)) {
                doesPacketMatch = (packetIn.yaw == fakePacket.yaw) && doesPacketMatch
            }

            if (doesPacketMatch) {
                fakeTpList.remove(fakedPacketPair)
                event.isCanceled = true
                break
            }

        }
    }

    fun fakeTPResponse(x: Double, y: Double, z: Double, yaw: Float, pitch: Float) {
        if (!pingless.enabled || overflowPackets > packetLimit.value) return
        val packet = C06PacketPlayerPosLook(x, y, z, yaw, pitch, false)
        mc.netHandler.networkManager.sendPacket(
            packet
        )
        fakeTpList.add(Pair(packet, System.currentTimeMillis()))
    }

    /**
     * Reset on warp
     */
    @SubscribeEvent
    fun onWorldChange(event: WorldEvent.Load) {
        tryEther = false
        inChain = false
        fakeTpList.clear()
        overflowPackets = 0
        nextStartPos = null
    }
}