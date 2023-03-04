package floppaclient.utils

import floppaclient.FloppaClient
import floppaclient.FloppaClient.Companion.mc
import floppaclient.commands.FloppaClientCommands
import floppaclient.floppamap.core.Room
import floppaclient.floppamap.dungeon.Dungeon
import floppaclient.floppamap.utils.RoomUtils
import floppaclient.utils.ChatUtils.chatMessage
import floppaclient.utils.ChatUtils.modMessage
import floppaclient.utils.Utils.equalsOneOf
import floppaclient.utils.Utils.isInt
import floppaclient.utils.Utils.isValidEtherwarpPos
import floppaclient.utils.Utils.timer
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.math.floor

// TODO maybe move some of the coordinate transformation methods in here into RoomUtils or even merge both classes.
/**
 * A collection of methods for handling everything related to modifying the data for autoactions.
 *
 * There are methods for adding and removing actions to and from the config.
 * There are also methods for modifying already stored data, such as clearing a room or rotating faulty data.
 * Lastly there are methods for coordinate transformations.
 *
 * @see floppaclient.floppamap.utils.RoomUtils
 * @see floppaclient.commands.WhereCommand
 * @see floppaclient.commands.AddCommand
 * @see floppaclient.commands.AddEtherCommand
 * @see floppaclient.commands.RemoveCommand
 * @see floppaclient.commands.RemoveEtherCommand
 * @author Aton
 */
object DataHandler {
    // Cache for undoing room clear.
    private val cachedClips: MutableList<Pair<Room, MutableMap<MutableList<Int>, MutableList<Double>>>> =
        mutableListOf()
    private val cachedEtherwarps: MutableList<Pair<Room, MutableMap<MutableList<Int>, BlockPos>>> = mutableListOf()
    private val cachedCmds: MutableList<Pair<Room, MutableMap<MutableList<Int>, String>>> = mutableListOf()
    private val cachedPreBlocks: MutableList<Pair<Room, MutableMap<Int, MutableSet<BlockPos>>>> = mutableListOf()

    private var lastEtherTarget: Pair<Room, BlockPos>? = null

    fun addClip(args: List<Double>) {
        if (args.size < 6) {
            return modMessage("§cNot enough arguments")
        }
        val room =
            Dungeon.currentRoomPair ?: FloppaClient.currentRegionPair ?: return modMessage("§cRoom not recognized.")

        val key = getKey(
            Vec3(
                floor(args[0]),
                floor(args[1]),
                floor(args[2])
            ),
            room.first.x,
            room.first.z,
            room.second
        )
        var coords1 =
            getRotatedCoords(
                Vec3(
                    args[3] - floor(args[0]), args[4] - floor(args[1]),
                    args[5] - floor(args[2])
                ), 360 - room.second
            )
        val route = mutableListOf(coords1.xCoord, coords1.yCoord, coords1.zCoord)

        if (args.size >= 9) {
            coords1 =
                getRotatedCoords(
                    Vec3(args[6] - args[3], args[7] - args[4], args[8] - args[5]),
                    360 - room.second
                )
            route.addAll(listOf(coords1.xCoord, coords1.yCoord, coords1.zCoord))
        }
        if (args.size >= 12) {
            coords1 =
                getRotatedCoords(
                    Vec3(args[9] - args[6], args[10] - args[7], args[11] - args[8]),
                    360 - room.second
                )
            route.addAll(listOf(coords1.xCoord, coords1.yCoord, coords1.zCoord))
        }
        if (args.size >= 15) {
            coords1 =
                getRotatedCoords(
                    Vec3(args[12] - args[9], args[13] - args[10], args[14] - args[11]),
                    360 - room.second
                )
            route.addAll(listOf(coords1.xCoord, coords1.yCoord, coords1.zCoord))
        }
        if (args.size >= 18) {
            coords1 =
                getRotatedCoords(
                    Vec3(args[15] - args[12], args[16] - args[13], args[17] - args[14]),
                    360 - room.second
                )
            route.addAll(listOf(coords1.xCoord, coords1.yCoord, coords1.zCoord))
        }
        RoomUtils.getOrPutRoomAutoActionData(room.first)?.run {
            if (this.clips.containsKey(key)) {

                val start = getRotatedCoords(
                    Vec3(key[0].toDouble(), key[1].toDouble(), key[2].toDouble()), room.second
                )
                    .addVector(room.first.x.toDouble(), 0.0, room.first.z.toDouble())
                val targets = mutableListOf<Vec3>()
                for (j in 0 until (route.size) / 3) {
                    targets.add(
                        getRotatedCoords(
                            Vec3(
                                route[3 * j], route[3 * j + 1], route[3 * j + 2]
                            ), room.second
                        )
                    )
                }
                modMessage("§cOverwriting existing Clip starting at: $key")
                when (targets.size) {
                    0 -> chatMessage(start.toCoords())
                    1 -> chatMessage("/add ${start.toCoords()} ${start.add(targets[0]).toCoords()}")
                    2 -> chatMessage(
                        "/add ${start.toCoords()} ${
                            start.add(targets[0]).toCoords()
                        } ${start.add(targets[0]).add(targets[1]).toCoords()}"
                    )
                    3 -> chatMessage(
                        "/add ${start.toCoords()} ${
                            start.add(targets[0]).toCoords()
                        } ${start.add(targets[0]).add(targets[1]).toCoords()} ${
                            start.add(targets[0]).add(targets[1]).add(targets[2]).toCoords()
                        }"
                    )
                    4 -> chatMessage(
                        "/add ${start.toCoords()} ${
                            start.add(targets[0]).toCoords()
                        } ${start.add(targets[0]).add(targets[1]).toCoords()} ${
                            start.add(targets[0]).add(targets[1]).add(targets[2]).toCoords()
                        } ${start.add(targets[0]).add(targets[1]).add(targets[2]).add(targets[3]).toCoords()}"
                    )
                    5 -> chatMessage(
                        "/add ${start.toCoords()} ${
                            start.add(targets[0]).toCoords()
                        } ${start.add(targets[0]).add(targets[1]).toCoords()} ${
                            start.add(targets[0]).add(targets[1]).add(targets[2]).toCoords()
                        } ${start.add(targets[0]).add(targets[1]).add(targets[2]).toCoords()} ${
                            start.add(targets[0]).add(targets[1]).add(targets[2]).add(targets[3]).add(targets[4])
                                .toCoords()
                        }"
                    )
                }
            }
            this.clips.put(key, route)
        } ?: return modMessage("§cRoom not properly scanned.")
        FloppaClient.autoactions.saveConfig()
        FloppaClient.autoactions.loadConfig()
        modMessage("Registered new Clip!")
    }

    fun removeClip(args: List<Double>) {
        if (args.size < 3) {
            return modMessage("§cNot enough arguments.")
        }
        val room =
            Dungeon.currentRoomPair ?: FloppaClient.currentRegionPair ?: return modMessage("§cRoom not recognized.")

        val key = getKey(
            Vec3(
                floor(args[0]),
                floor(args[1]),
                floor(args[2])
            ),
            room.first.x,
            room.first.z,
            room.second
        )
        RoomUtils.getOrPutRoomAutoActionData(room.first)?.run {
            if (!this.clips.containsKey(key)) { // if this clip does not exist return
                modMessage("§cNo clips found for these coordinates.")
                return
            }
            this.clips.remove(key)
        } ?: return modMessage("§cRoom not properly scanned.")
        FloppaClient.autoactions.saveConfig()
        FloppaClient.autoactions.loadConfig()
        modMessage("Clip removed!")
    }

    fun addEther(args: List<Double>) {
        val room =
            Dungeon.currentRoomPair ?: FloppaClient.currentRegionPair ?: return modMessage("§cRoom not recognized.")
        val key: MutableList<Int>
        val target: BlockPos

        // Depending on the arguments define key and target
        //If not enough arguments use existing data to fill in
        if (args.size < 6) {

            // If empty use block standing on as start and looking at as target.
            if (args.isEmpty()) {
                val pos = BlockPos(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ).down()
                key = getKey(
                    Vec3(pos),
                    room.first.x,
                    room.first.z,
                    room.second
                )

                val dist = 60.0
                val vec3 = mc.thePlayer.getPositionEyes(mc.timer.renderPartialTicks)
                val vec31 = mc.thePlayer.getLook(mc.timer.renderPartialTicks)
                val vec32 = vec3.addVector(
                    vec31.xCoord * dist,
                    vec31.yCoord * dist,
                    vec31.zCoord * dist
                )
                val obj =
                    mc.theWorld.rayTraceBlocks(vec3, vec32, true, false, true) ?: return modMessage("Not in range!")
                val blockPos = obj.blockPos ?: return modMessage("Invalid target!")
                if (isValidEtherwarpPos(obj)) {
                    target = getRelativePos(
                        Vec3(blockPos),
                        room.first.x,
                        room.first.z,
                        room.second
                    )
                } else {
                    return modMessage("Target not valid!")
                }

            }
            // Otherwise return because arguments dont match
            else {
                modMessage("§cNot enough arguments")
                return
            }
        }
        // If more or equal than 6 arguments use those
        else {
            key = getKey(
                Vec3(
                    floor(args[0]),
                    floor(args[1]),
                    floor(args[2])
                ),
                room.first.x,
                room.first.z,
                room.second
            )

            target = getRelativePos(
                Vec3(
                    floor(args[3]),
                    floor(args[4]),
                    floor(args[5])
                ),
                room.first.x,
                room.first.z,
                room.second
            )
        }

        RoomUtils.getOrPutRoomAutoActionData(room.first)?.run {
            if (this.etherwarps.containsKey(key)) {
                val start = getRotatedCoords(
                    Vec3(key[0].toDouble(), key[1].toDouble(), key[2].toDouble()), room.second
                )
                    .addVector(room.first.x.toDouble(), 0.0, room.first.z.toDouble())
                val targetOld = getRotatedCoords(
                    this.etherwarps[key]!!, room.second
                )
                    .addVector(room.first.x.toDouble(), 0.0, room.first.z.toDouble())
                modMessage("§cOverwriting existing Etherwarp")
                chatMessage("/add ${start.toIntCoords()} ${targetOld.toIntCoords()}")
            }
            this.etherwarps.put(key, target)
        } ?: return modMessage("§cRoom not properly scanned.")
        lastEtherTarget = Pair(room.first, target)
        FloppaClient.autoactions.saveConfig()
        FloppaClient.autoactions.loadConfig()
        modMessage("Registered new Etherwarp!")
    }

    fun addCmd(args: List<String>) {
        val room =
            Dungeon.currentRoomPair ?: FloppaClient.currentRegionPair ?: return modMessage("§cRoom not recognized.")
        val key: MutableList<Int>
        var command: String

        //if there are more then 4 arguments use those but if the first 3 args are not numbers use the current position and if there are no arguments send a message that there are not enough arguments
        if (args.size >= 4) {
            if (isInt(args[0]) && isInt(args[1]) && isInt(args[2]) && args[3].startsWith("/")) {
                key = getKey(
                    Vec3(
                        floor(args[0].toInt().toDouble()),
                        floor(args[1].toInt().toDouble()),
                        floor(args[2].toInt().toDouble())
                    ),
                    room.first.x,
                    room.first.z,
                    room.second
                )
                command = args.drop(3).joinToString(" ")
            } else if (args.isNotEmpty()) {
                //check if the first argument starts with a / if it does use the current position and the rest of the arguments as the command
                if (args[0].startsWith("/")) {
                    key = getKey(
                        Vec3(
                            floor(mc.thePlayer.posX),
                            floor(mc.thePlayer.posY),
                            floor(mc.thePlayer.posZ)
                        ),
                        room.first.x,
                        room.first.z,
                        room.second
                    )
                    command = args.joinToString(" ")
                } else {
                    modMessage("§cInvalid coordinates")
                    return
                }
            } else {
                modMessage("§cInvalid coordinates")
                return
            }
        } else {
            modMessage("§cNot enough arguments")
            return
        }

        RoomUtils.getOrPutRoomAutoActionData(room.first)?.run {
            if (this.autocmds.containsKey(key)) {
                val start = getRotatedCoords(
                    Vec3(key[0].toDouble(), key[1].toDouble(), key[2].toDouble()), room.second
                )
                    .addVector(room.first.x.toDouble(), 0.0, room.first.z.toDouble())
                val targetOld = autocmds[key]!!
                modMessage("§cOverwriting existing Command")
                chatMessage("/add ${start.toIntCoords()} $targetOld")
            }
            this.autocmds.put(key, command)
        } ?: return modMessage("§cRoom not properly scanned.")
        FloppaClient.autoactions.saveConfig()
        FloppaClient.autoactions.loadConfig()
        modMessage("Registered new Command!")
    }

    fun removeEther(args: List<Double>) {
        val room = Dungeon.currentRoomPair ?: FloppaClient.currentRegionPair ?: return
        val key: MutableList<Int>
        var showMessage = false
        if (args.size < 3) {
            modMessage("Removing closest etherwarp.")
            key = RoomUtils.getOrPutRoomAutoActionData(room.first)?.run {
                val ethers = this.etherwarps
                if (ethers.isNotEmpty()) {
                    showMessage = true
                    return@run ethers.minByOrNull { (key, _) ->
                        val start = getRotatedCoords(
                            Vec3(key[0].toDouble(), key[1].toDouble(), key[2].toDouble()), room.second
                        )
                            .addVector(room.first.x.toDouble(), 0.0, room.first.z.toDouble())
                        mc.thePlayer.getDistance(start.xCoord, start.yCoord, start.zCoord)
                    }!!.key
                } else {
                    modMessage("&r&eNo Etherwarps found in this room")
                    return
                }
            } ?: return modMessage("§cRoom not properly scanned.")
        } else {
            key = getKey(
                Vec3(
                    floor(args[0]),
                    floor(args[1]),
                    floor(args[2])
                ),
                room.first.x,
                room.first.z,
                room.second
            )
        }

        RoomUtils.getOrPutRoomAutoActionData(room.first)?.run {
            if (!this.etherwarps.containsKey(key)) { // if this etherwarp does not exist return
                modMessage("§cNo Etherwarp found for these coordinates.")
                return
            }
            if (showMessage) {
                val start = getRotatedCoords(
                    Vec3(key[0].toDouble(), key[1].toDouble(), key[2].toDouble()), room.second
                )
                    .addVector(room.first.x.toDouble(), 0.0, room.first.z.toDouble())
                val targetOld = getRotatedCoords(
                    this.etherwarps[key]!!, room.second
                )
                    .addVector(room.first.x.toDouble(), 0.0, room.first.z.toDouble())
                chatMessage("/add ${start.toIntCoords()} ${targetOld.toIntCoords()}")
            }
            this.etherwarps.remove(key)
        } ?: return modMessage("§cRoom not properly scanned.")
        FloppaClient.autoactions.saveConfig()
        FloppaClient.autoactions.loadConfig()
        modMessage("Etherwarp removed!")
    }

    fun removeCmd(args: List<Double>) {
        val room = Dungeon.currentRoomPair ?: FloppaClient.currentRegionPair ?: return
        val key: MutableList<Int>
        var showMessage = false
        if (args.size < 3) {
            modMessage("Removing closest cmd.")
            key = RoomUtils.getOrPutRoomAutoActionData(room.first)?.run {
                val cmds = this.autocmds
                if (cmds.isNotEmpty()) {
                    showMessage = true
                    return@run cmds.minByOrNull { (key, _) ->
                        val start = getRotatedCoords(
                            Vec3(key[0].toDouble(), key[1].toDouble(), key[2].toDouble()), room.second
                        )
                            .addVector(room.first.x.toDouble(), 0.0, room.first.z.toDouble())
                        mc.thePlayer.getDistance(start.xCoord, start.yCoord, start.zCoord)
                    }!!.key
                } else {
                    modMessage("&r&eNo Cmds found in this room")
                    return
                }
            } ?: return modMessage("§cRoom not properly scanned.")
        } else {
            key = getKey(
                Vec3(
                    floor(args[0]),
                    floor(args[1]),
                    floor(args[2])
                ),
                room.first.x,
                room.first.z,
                room.second
            )
        }

        RoomUtils.getOrPutRoomAutoActionData(room.first)?.run {
            if (!this.autocmds.containsKey(key)) { // if this etherwarp does not exist return
                modMessage("§cNo Cmd found for these coordinates.")
                return
            }
            if (showMessage) {
                val start = getRotatedCoords(
                    Vec3(key[0].toDouble(), key[1].toDouble(), key[2].toDouble()), room.second
                )
                    .addVector(room.first.x.toDouble(), 0.0, room.first.z.toDouble())
                val targetOld = autocmds[key]!!
                chatMessage("/add ${start.toIntCoords()} $targetOld")
            }
            this.autocmds.remove(key)
        } ?: return modMessage("§cRoom not properly scanned.")
        FloppaClient.autoactions.saveConfig()
        FloppaClient.autoactions.loadConfig()
        modMessage("Cmd removed!")
    }

    /**
     * Clears all auto clips in the current room.
     */
    fun clearClipsInRoom() {
        val room =
            Dungeon.currentRoomPair ?: FloppaClient.currentRegionPair ?: return modMessage("Room not recognized.")

        RoomUtils.getRoomAutoActionData(room.first)?.run {
            // Important to create new instance, so that not just a reference is created which also will be cleared by this.clips.clear()
            val tempMap = mutableMapOf<MutableList<Int>, MutableList<Double>>()
            tempMap.putAll(this.clips)
            cachedClips.add(Pair(room.first, tempMap))
            this.clips.clear()
        }
        FloppaClient.autoactions.saveConfig()
        FloppaClient.autoactions.loadConfig()
        modMessage("All clips removed in ${room.first.data.name}! Run \"/${FloppaClientCommands().commandName} undo clips\" to undo.")
    }

    /**
     * Clears all auto etherwarps in the current room.
     */
    fun clearEtherInRoom() {
        val room =
            Dungeon.currentRoomPair ?: FloppaClient.currentRegionPair ?: return modMessage("Room not recognized.")

        RoomUtils.getRoomAutoActionData(room.first)?.run {
            // Important to create new instance, so that not just a reference is created which also will be cleared by this.etherwarps.clear()
            val tempMap = mutableMapOf<MutableList<Int>, BlockPos>()
            tempMap.putAll(this.etherwarps)
            cachedEtherwarps.add(Pair(room.first, tempMap))
            this.etherwarps.clear()
        }
        FloppaClient.autoactions.saveConfig()
        FloppaClient.autoactions.loadConfig()
        modMessage("All etherwarps removed in ${room.first.data.name}! Run \"/${FloppaClientCommands().commandName} undo ether\" to undo.")
    }

    /**
     * Clears all auto cmds in the current room.
     */
    fun clearCmdsInRoom() {
        val room =
            Dungeon.currentRoomPair ?: FloppaClient.currentRegionPair ?: return modMessage("Room not recognized.")

        RoomUtils.getRoomAutoActionData(room.first)?.run {
            // Important to create new instance, so that not just a reference is created which also will be cleared by this.etherwarps.clear()
            val tempMap = mutableMapOf<MutableList<Int>, String>()
            tempMap.putAll(this.autocmds)
            cachedCmds.add(Pair(room.first, tempMap))
            this.autocmds.clear()
        }
        FloppaClient.autoactions.saveConfig()
        FloppaClient.autoactions.loadConfig()
        modMessage("All cmds removed in ${room.first.data.name}! Run \"/${FloppaClientCommands().commandName} undo cmds\" to undo.")
    }

    /**
     * Clears all extras blocks in the current room.
     */
    fun clearBlocksInRoom() {
        val room = Dungeon.currentRoomPair ?: FloppaClient.currentRegionPair ?: FloppaClient.currentRegionPair
        ?: return modMessage("Room not recognized.")

        RoomUtils.getRoomExtrasData(room.first)?.run {
            // Important to create new instance, so that not just a reference is created which also will be cleared by this.preBlocks.clear()
            val tempMap = mutableMapOf<Int, MutableSet<BlockPos>>()
            tempMap.putAll(this.preBlocks)
            cachedPreBlocks.add(Pair(room.first, tempMap))
            this.preBlocks.clear()
        }
        FloppaClient.extras.saveConfig()
        FloppaClient.extras.loadConfig()
        modMessage("All blocks removed in ${room.first.data.name}! Run \"/${FloppaClientCommands().commandName} undo blocks\" to undo.")
    }

    /**
     * Undos the last clearClip action.
     */
    fun undoClearClips() {
        if (cachedClips.isEmpty()) return modMessage("No data cached for undo!")
        RoomUtils.getOrPutRoomAutoActionData(cachedClips.last().first)?.run {
            this.clips.putAll(cachedClips.last().second)
        } ?: return modMessage("§cThis should not have happened. Report this issue.")
        FloppaClient.autoactions.saveConfig()
        FloppaClient.autoactions.loadConfig()
        modMessage("Clips recovered in ${cachedClips.last().first.data.name}.")
        cachedClips.removeLastOrNull()
    }

    /**
     * Undos the last clearEther action.
     */
    fun undoClearEther() {
        if (cachedEtherwarps.isEmpty()) return modMessage("No data cached for undo!")
        RoomUtils.getOrPutRoomAutoActionData(cachedEtherwarps.last().first)?.run {
            this.etherwarps.putAll(cachedEtherwarps.last().second)
        } ?: return modMessage("§cThis should not have happened. Report this issue.")
        FloppaClient.autoactions.saveConfig()
        FloppaClient.autoactions.loadConfig()
        modMessage("Etherwarps recovered in ${cachedEtherwarps.last().first.data.name}.")
        cachedEtherwarps.removeLastOrNull()
    }

    /**
     * Undos the last clearCmds action.
     */
    fun undoClearCmds() {
        if (cachedCmds.isEmpty()) return modMessage("No data cached for undo!")
        RoomUtils.getOrPutRoomAutoActionData(cachedCmds.last().first)?.run {
            this.autocmds.putAll(cachedCmds.last().second)
        } ?: return modMessage("§cThis should not have happened. Report this issue.")
        FloppaClient.autoactions.saveConfig()
        FloppaClient.autoactions.loadConfig()
        modMessage("Etherwarps recovered in ${cachedCmds.last().first.data.name}.")
        cachedCmds.removeLastOrNull()
    }

    /**
     * Undos the last clearBlocks action.
     */
    fun undoClearBlocks() {
        if (cachedPreBlocks.isEmpty()) return modMessage("No data cached for undo!")
        RoomUtils.getOrPutRoomExtrasData(cachedPreBlocks.last().first)?.run {
            this.preBlocks.putAll(cachedPreBlocks.last().second)
        } ?: return modMessage("§cThis should not have happened. Report this issue.")
        FloppaClient.extras.saveConfig()
        FloppaClient.extras.loadConfig()
        modMessage("Blocks recovered in ${cachedPreBlocks.last().first.data.name}.")
        cachedPreBlocks.removeLastOrNull()
    }

    /**
     * Rotates the clip data in the current room by 90°.
     */
    fun rotateClips(rotation: Int = 90) {
        val room = Dungeon.currentRoomPair ?: return modMessage("Room not recognized.")

        RoomUtils.getRoomAutoActionData(room.first)?.run {
            val newClips: MutableMap<MutableList<Int>, MutableList<Double>> = mutableMapOf()
            this.clips.forEach { (key, route) ->
                val newKey = getRotatedKey(key, rotation)
                val newRoute = getRotatedRoute(route, rotation)
                newClips[newKey] = newRoute
            }
            this.clips.clear()
            this.clips.putAll(newClips)
        }
        FloppaClient.autoactions.saveConfig()
        FloppaClient.autoactions.loadConfig()
        val rot2 = when {
            rotation.equalsOneOf(90, -270) -> 90
            rotation.equalsOneOf(180, -180) -> 180
            rotation.equalsOneOf(270, -90) -> 270
            else -> 0
        }
        modMessage("Rotated all clips in ${room.first.data.name} by $rot2°!")
    }

    /**
     * Rotates the auto ether data in the current room.
     */
    fun rotateEther(rotation: Int = 90) {
        val room = Dungeon.currentRoomPair ?: return modMessage("Room not recognized.")

        RoomUtils.getRoomAutoActionData(room.first)?.run {
            val newEthers: MutableMap<MutableList<Int>, BlockPos> = mutableMapOf()
            this.etherwarps.forEach { (key, target) ->
                val newKey = getRotatedKey(key, rotation)
                val newTarget = RoomUtils.getRotatedPos(target, rotation)
                newEthers[newKey] = newTarget
            }
            this.etherwarps.clear()
            this.etherwarps.putAll(newEthers)
        }
        FloppaClient.autoactions.saveConfig()
        FloppaClient.autoactions.loadConfig()
        val rot2 = when {
            rotation.equalsOneOf(90, -270) -> 90
            rotation.equalsOneOf(180, -180) -> 180
            rotation.equalsOneOf(270, -90) -> 270
            else -> 0
        }
        modMessage("Rotated all etherwarps in ${room.first.data.name} by $rot2°!")
    }

    /**
     * Rotates the auto ether data in the current room.
     */
    fun rotateCmds(rotation: Int = 90) {
        val room = Dungeon.currentRoomPair ?: return modMessage("Room not recognized.")

        RoomUtils.getRoomAutoActionData(room.first)?.run {
            val newCmds: MutableMap<MutableList<Int>, String> = mutableMapOf()
            this.autocmds.forEach { (key, target) ->
                val newKey = getRotatedKey(key, rotation)
                newCmds[newKey] = target
            }
            this.autocmds.clear()
            this.autocmds.putAll(newCmds)
        }
        FloppaClient.autoactions.saveConfig()
        FloppaClient.autoactions.loadConfig()
        val rot2 = when {
            rotation.equalsOneOf(90, -270) -> 90
            rotation.equalsOneOf(180, -180) -> 180
            rotation.equalsOneOf(270, -90) -> 270
            else -> 0
        }
        modMessage("Rotated all cmds in ${room.first.data.name} by $rot2°!")
    }

    /**
     * Rotates the extras blocks data in the current room.
     */
    fun rotateBlocks(rotation: Int = 90) {
        val room = Dungeon.currentRoomPair ?: return modMessage("Room not recognized.")

        RoomUtils.getRoomExtrasData(room.first)?.run {
            val newBlocks: MutableMap<Int, MutableSet<BlockPos>> = mutableMapOf()
            this.preBlocks.forEach { (key, posSet) ->
                val newPos = RoomUtils.getRotatedPosSet(posSet, rotation)
                newBlocks[key] = newPos
            }
            this.preBlocks.clear()
            this.preBlocks.putAll(newBlocks)
        }
        FloppaClient.extras.saveConfig()
        FloppaClient.extras.loadConfig()
        val rot2 = when {
            rotation.equalsOneOf(90, -270) -> 90
            rotation.equalsOneOf(180, -180) -> 180
            rotation.equalsOneOf(270, -90) -> 270
            else -> 0
        }
        modMessage("Rotated all pre blocks in ${room.first.data.name} by $rot2°!")
    }


    /**
     * Returns a mutbale list of 3 Integers representing the players position in the current room.
     * BlockPos or Vec3i would probably be the better data format, but list was chosen, because it is easier to serialize.
     */
    fun getKey(vec: Vec3, roomX: Int, roomZ: Int, rotation: Int): MutableList<Int> {
        getRelativeCoords(vec, roomX, roomZ, rotation).run {
            return mutableListOf(this.xCoord.toInt(), this.yCoord.toInt(), this.zCoord.toInt())
        }
    }

    /**
     * Gets the relative player position in a room as BlockPos
     */
    private fun getRelativePos(vec: Vec3, roomX: Int, roomZ: Int, rotation: Int): BlockPos {
        return BlockPos(getRelativeCoords(vec, roomX, roomZ, rotation))
    }

    /**
     * Gets the relative player position in a room
     */
    fun getRelativeCoords(vec: Vec3, roomX: Int, roomZ: Int, rotation: Int): Vec3 {
        return getRotatedCoords(vec.subtract(roomX.toDouble(), 0.0, roomZ.toDouble()), -rotation)
    }

    /**
     * Returns the real rotations of the given vec in a room with given rotation.
     * To get the relative rotation inside a room use 360 - rotation.
     */
    fun getRotatedCoords(vec: Vec3, rotation: Int): Vec3 {
        return when {
            rotation.equalsOneOf(90, -270) -> Vec3(-vec.zCoord, vec.yCoord, vec.xCoord)
            rotation.equalsOneOf(180, -180) -> Vec3(-vec.xCoord, vec.yCoord, -vec.zCoord)
            rotation.equalsOneOf(270, -90) -> Vec3(vec.zCoord, vec.yCoord, -vec.xCoord)
            else -> vec
        }
    }

    /**
     * Returns the real rotations of the given vec in a room with given rotation.
     * To get the relative rotation inside a room use 360 - rotation.
     */
    fun getRotatedCoords(blockPos: BlockPos, rotation: Int): Vec3 {
        return getRotatedCoords(Vec3(blockPos), rotation)
    }

    /**
     * Returns the real rotations of the given key in a room with given rotation.
     * To get the relative rotation inside a room use 360 - rotation.
     * @throws NullPointerException or similar when key.size is less than 3
     */
    private fun getRotatedKey(key: MutableList<Int>, rotation: Int): MutableList<Int> {
        return when {
            rotation.equalsOneOf(90, -270) -> mutableListOf(-key[2], key[1], key[0])
            rotation.equalsOneOf(180, -180) -> mutableListOf(-key[0], key[1], -key[2])
            rotation.equalsOneOf(270, -90) -> mutableListOf(key[2], key[1], -key[0])
            else -> key
        }
    }

    /**
     * Returns the real rotations of the given coordinate list of lenght 3 in a room with given rotation.
     * To get the relative rotation inside a room use 360 - rotation.
     * @throws NullPointerException or similar when coords.size is less than 3
     */
    private fun getRotatedCoordList(coords: MutableList<Double>, rotation: Int): MutableList<Double> {
        return when {
            rotation.equalsOneOf(90, -270) -> mutableListOf(-coords[2], coords[1], coords[0])
            rotation.equalsOneOf(180, -180) -> mutableListOf(-coords[0], coords[1], -coords[2])
            rotation.equalsOneOf(270, -90) -> mutableListOf(coords[2], coords[1], -coords[0])
            else -> coords
        }
    }

    /**
     * Returns the real rotations of the given route in a room with given rotation.
     * To get the relative rotation inside a room use 360 - rotation.
     */
    private fun getRotatedRoute(route: MutableList<Double>, rotation: Int): MutableList<Double> {
        val returnRoute = mutableListOf<Double>()
        for (j in 0 until (route.size) / 3) {
            returnRoute.addAll(getRotatedCoordList(route.subList(3 * j, 3 * j + 3), rotation))
        }
        return returnRoute
    }


    fun Vec3.toCoords(): String {
        return this.xCoord.toString() + " " + this.yCoord.toString() + " " + this.zCoord.toString()
    }

    fun Vec3.toIntCoords(): String {
        return this.xCoord.toInt().toString() + " " + this.yCoord.toInt().toString() + " " + this.zCoord.toInt()
            .toString()
    }

    fun Vec3.toMutableList(): MutableList<Double> {
        return mutableListOf(this.xCoord, this.yCoord, this.zCoord)
    }

    fun Vec3.toMutableIntList(): MutableList<Int> {
        return mutableListOf(this.xCoord.toInt(), this.yCoord.toInt(), this.zCoord.toInt())
    }

    @SubscribeEvent
    fun onWarp(event: WorldEvent.Load) {
        lastEtherTarget = null
    }
}