package floppaclient.module.impl.player

import floppaclient.FloppaClient
import floppaclient.events.DungeonSecretEvent
import floppaclient.events.PositionUpdateEvent
import floppaclient.floppamap.dungeon.Dungeon
import floppaclient.floppamap.extras.EditMode
import floppaclient.floppamap.utils.RoomUtils
import floppaclient.module.Category
import floppaclient.module.Module
import floppaclient.module.settings.impl.BooleanSetting
import floppaclient.module.settings.impl.NumberSetting
import floppaclient.utils.ChatUtils
import floppaclient.utils.DataHandler
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.*
import kotlin.concurrent.schedule

object AutoCmd : Module(
    "Auto Command",
    category = Category.PLAYER,
    description = "Automatically run a command when stepping on a cmd point or pressing the set keybind." +
            "Use commands /addcmd and /remcmd to edit your routes."
) {

    private val chatInfo = BooleanSetting("Chat Info", true, description = "Show a message in chat when etherwarping.")
    private val registerSecret = BooleanSetting(
        "Register Secret",
        false,
        description = "Attempts to auto run cmd when you pick up a secret. §eRequires Secret Chimes to be enabled."
    )
    private val onlyOnKeybind =
        BooleanSetting("Only On Keybind", false, description = "Only run the command when the keybind is pressed.")
    private val delayAfterSecret = NumberSetting(
        "Secret Delay",
        70.0,
        0.0,
        500.0,
        10.0,
        description = "Delay in ms when auto command is attempted after getting a secret."
    )
    private val overallDelay = NumberSetting(
        "Overall Delay",
        50.0,
        0.0,
        500.0,
        10.0,
        description = "Delay in ms when auto command is attempted after getting a secret."
    )
    private val detectionRange = NumberSetting(
        "Det. Range",
        2.0,
        1.0,
        10.0,
        description = "Max distance from a start point to register the Auto Command."
    )

    //private val checkCooldown = BooleanSetting("Cooldown", true, description = "Puts a cooldown on activations.")
    private val debugMessages =
        BooleanSetting("Debug Messages", false, description = "Shows debug messages for fake responses.")

    private var tryCmd = false


    init {
        this.addSettings(
            chatInfo,
            registerSecret,
            delayAfterSecret,
            detectionRange,
            // checkCooldown,
            debugMessages,
            onlyOnKeybind
        )
    }

/*
    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (!FloppaClient.inSkyblock || EditMode.enabled || !enabled) return
        //if the player is standing in a cmd point, run the command except if onlyOnKeybind is enabled
        if (!onlyOnKeybind.enabled) {
            for (cmd in DataHandler.Ins.autoCmds) {
                if (cmd.key == FloppaClient.mc.thePlayer.flooredPosition) {
                    tryCmd = true
                    break
                }
            }
        }
    }

 */

    override fun onKeyBind() {
        if (!FloppaClient.inSkyblock || EditMode.enabled || !enabled) return
        println("")
        if (onlyOnKeybind.enabled) {
            tryCmd = true
        }
    }

    /**
     * For activation on gotten secret.
     */
    @SubscribeEvent
    fun onSecret(event: DungeonSecretEvent) {
        if (!registerSecret.enabled) return
        if (!FloppaClient.inSkyblock || EditMode.enabled || !enabled) return
        Timer().schedule(delayAfterSecret.value.toLong()) { tryCmd = true }
    }

    /**
     * Checks once per Tick whether a clip action is staged, and if so executes it.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun preWalk(event: PositionUpdateEvent.Pre) {
//        if (cooldownTicks > 0) cooldownTicks--
        if (!tryCmd) return
        tryCmd()
        tryCmd = false
    }

    /**
     * Checks which action to perform at the current coordinates.
     * And calls the corresponding function to perform the clip.
     */
    private fun tryCmd() {
        try { // stop module from breaking when dungoen not scanned

            val pos = FloppaClient.mc.thePlayer.position

            val room = Dungeon.currentRoomPair ?: FloppaClient.currentRegionPair ?: return
            if (room.first.isSeparator) return

            var key: MutableList<Int>

            RoomUtils.getRoomAutoActionData(room.first)?.run {
                /** check for the start pos in config
                 * Note here that a new BlockPos instace is created, so that the Etherwarp data can not be overwritten.*/
                val range = detectionRange.value
                val point1 = pos.add(range, range, range)
                val point2 = pos.add(-range, -range, -range)
                for (blockPos in BlockPos.getAllInBox(point1, point2)
                    .sortedBy { FloppaClient.mc.thePlayer.getDistanceSqToCenter(it) }
                ) {
                    key = DataHandler.getKey(
                        Vec3(blockPos),
                        room.first.x,
                        room.first.z,
                        room.second
                    )

                    if (this.autocmds.containsKey(key)) {
                        val value = this.autocmds[key]!!
                        if (debugMessages.enabled) {
                            println("AutoCmd: $key -> ${this.autocmds[key]}  (range: $range) (pos: $pos) (room: ${room.first}) (region: ${room.second}) (value: $value)")
                        }
                        if (value != "" && !debugMessages.enabled) {

                            if (chatInfo.enabled) {
                                ChatUtils.modMessage("Ran Command: $value")
                            }
                            FloppaClient.mc.thePlayer.sendChatMessage(value)
                        }
                        break
                    }
                }
            }

        } catch (e: Throwable) {
            return
        }
    }


    /**
     * Reset on warp
     */
    @SubscribeEvent
    fun onWorldChange(event: WorldEvent.Load) {
        tryCmd = false
    }
}