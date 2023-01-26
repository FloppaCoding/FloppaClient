package floppaclient.module.impl.dungeon

import floppaclient.FloppaClient
import floppaclient.FloppaClient.Companion.mc
import floppaclient.events.PositionUpdateEvent
import floppaclient.floppamap.core.Room
import floppaclient.floppamap.dungeon.Dungeon
import floppaclient.floppamap.utils.RoomUtils
import floppaclient.floppamap.utils.RoomUtils.getRealPos
import floppaclient.module.Category
import floppaclient.module.Module
import floppaclient.module.settings.impl.BooleanSetting
import floppaclient.module.settings.impl.NumberSetting
import floppaclient.utils.ChatUtils.modMessage
import floppaclient.utils.Utils.playLoudSound
import floppaclient.utils.fakeactions.FakeActionUtils
import net.minecraft.init.Blocks
import net.minecraft.util.BlockPos
import net.minecraftforge.event.entity.player.PlayerInteractEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent
import java.text.DecimalFormat

/**
 * A module to automatically complete the water board puzzle in one flow.
 *
 * It does not actively scan the board to calculate the best path but instead uses preset solutions.
 * It will not work when the board is not in a default state or is being messed with while being solved.
 *
 * @author Aton
 */
object AutoWater : Module(
    "Auto Water",
    category = Category.DUNGEON,
    description = "Automatically solves the water puzzle. Start by flicking the water flow start lever."
){

    private val etherCd = NumberSetting("EtherCD",10.0,5.0,40.0,1.0, description = "Cooldown between successive etherwarp attempts in ticks.")
    private val completionSound = BooleanSetting("Comp. Sound", true, description = "Play a sound upon finishing the solving process.")
    private val volume = NumberSetting("Volume", 1.0, 0.0, 1.0, 0.01, description = "Volume for the completion sound.")
    private val advancedEther = BooleanSetting("Advanced tp",false, description = "The etherwarp positions will be optimized, can break the solver, WIP.")

    var variant = -1
     private set

    private var inWater = false


    // TODO probably can be replaced with a better data type, maybe an object like Gates
    /**
     * All Solutions
     */
    private val variant3Solutions: Map<GateState, List<Step>> = mapOf(
        GateState.RGB to listOf(
            Step(Lever.EMERALD, Condition.Delay(0)),
            Step(Lever.GOLD,    Condition.Delay(0)),
            Step(Lever.CLAY,    Condition.Delay(0)),
            Step(Lever.CLAY,    Condition.WaterState(-1,74)),
            Step(Lever.GOLD,    Condition.Delay(0)),
            Step(Lever.EMERALD, Condition.WaterState(-9,65)),
            Step(Lever.EMERALD, Condition.WaterState(-5,63)),
            Step(Lever.CLAY,    Condition.WaterState(-0,65)),
        ),
        GateState.RGO to listOf(
            Step(Lever.EMERALD, Condition.Delay(0)),
            Step(Lever.GOLD,    Condition.Delay(0)),
            Step(Lever.QUARTZ,  Condition.WaterState(true, BlockPos(-4, 78, 11))),
            Step(Lever.CLAY,    Condition.Delay(60)),
            // -7,73,-11
            Step(Lever.CLAY,    Condition.WaterState(true, BlockPos(7,73,11))),
            Step(Lever.EMERALD, Condition.WaterState(true, BlockPos(7,65,11)))
        ),
        GateState.RGP to listOf(
            Step(Lever.EMERALD, Condition.Delay(0)),
            Step(Lever.GOLD,    Condition.Delay(0)),
            Step(Lever.QUARTZ,  Condition.WaterState(true, BlockPos(-4, 78, 11))),
            Step(Lever.CLAY,    Condition.Delay(60)),
            Step(Lever.CLAY,    Condition.WaterState(true, BlockPos(7,73,11))),
        ),
        GateState.RBO to listOf(
            Step(Lever.EMERALD, Condition.Delay(0)),
            Step(Lever.GOLD,    Condition.Delay(0)),
            Step(Lever.QUARTZ,  Condition.WaterState(true, BlockPos(-4, 78, 11))),
            Step(Lever.CLAY,    Condition.Delay(60)),
            Step(Lever.CLAY,    Condition.WaterState(true, BlockPos(7,73,11))),
            //-5,69,-11
            Step(Lever.CLAY, Condition.WaterState(true, BlockPos(5,69,11))),
            Step(Lever.EMERALD, Condition.WaterState(true, BlockPos(7,65,11)))
        ),
        GateState.RBP to listOf(
            Step(Lever.EMERALD, Condition.Delay(0)),
            Step(Lever.GOLD,    Condition.Delay(0)),
            Step(Lever.QUARTZ,  Condition.WaterState(true, BlockPos(-4, 78, 11))),
            Step(Lever.CLAY,    Condition.Delay(60)),
            Step(Lever.CLAY,    Condition.WaterState(true, BlockPos(7,73,11))),
            //-5,69,-11
            Step(Lever.CLAY, Condition.WaterState(true, BlockPos(5,69,11))),
        ),
        GateState.ROP to listOf(
            Step(Lever.EMERALD, Condition.Delay(0)),
            Step(Lever.GOLD,    Condition.Delay(0)),
            Step(Lever.QUARTZ,  Condition.WaterState(true, BlockPos(-4, 78, 11))),
            Step(Lever.CLAY,    Condition.Delay(60)),
            //-9,63,-11
            Step(Lever.EMERALD, Condition.WaterState(true, BlockPos(9,63,-11)))
        ),
        GateState.GBO to listOf(
            Step(Lever.GOLD, Condition.Delay(10)),
            Step(Lever.QUARTZ,    Condition.Delay(20)),
            // 4,64,-11
            Step(Lever.CLAY, Condition.WaterState(true, BlockPos(-4,64,11)))
        ),
        GateState.GBP to listOf(
            Step(Lever.GOLD, Condition.Delay(10)),
            Step(Lever.QUARTZ,    Condition.Delay(20)),
            Step(Lever.EMERALD,    Condition.Delay(40)),
            // 4,64,-11
            Step(Lever.CLAY, Condition.WaterState(true, BlockPos(-4,64,11)))
        ),
        GateState.GOP to listOf(
            Step(Lever.GOLD, Condition.Delay(10)),
            Step(Lever.QUARTZ,    Condition.Delay(20)),
            // -7,62,-11
            // moved it one further to the right for consistency
            Step(Lever.EMERALD, Condition.WaterState(true, BlockPos(6,62,11)))
        ),
        GateState.BOP to listOf(
            Step(Lever.GOLD, Condition.Delay(10)),
            Step(Lever.QUARTZ,    Condition.Delay(20)),
            Step(Lever.CLAY, Condition.WaterState(true, BlockPos(5,69,11))),
            Step(Lever.EMERALD, Condition.WaterState(true, BlockPos(6,62,11)))
        ),
    )
    private val variant2Solutions: Map<GateState, List<Step>> = mapOf(
        GateState.RGB to listOf(
            Step(Lever.EMERALD, Condition.Delay(0)),
            Step(Lever.GOLD,    Condition.WaterState(-1,76)),
            Step(Lever.EMERALD, Condition.WaterState(2,73)),
            Step(Lever.WATER,   Condition.Delay(0)),
            Step(Lever.EMERALD, Condition.WaterState(-5,60)),
        ),
        GateState.RGO to listOf(
            Step(Lever.EMERALD, Condition.Delay(0)),
            Step(Lever.GOLD,    Condition.WaterState(-2,76)),
            Step(Lever.EMERALD, Condition.WaterState(1,65, false, 155)),
        ),
        GateState.RGP to listOf(
            Step(Lever.QUARTZ,  Condition.Delay(0)),
            Step(Lever.WATER,   Condition.WaterState(-3,76)),
            Step(Lever.DIAMOND, Condition.WaterState(-2,76)),
            Step(Lever.DIAMOND, Condition.WaterState(-0,78, false)),
            Step(Lever.DIAMOND, Condition.WaterState(-5,60)),
            Step(Lever.EMERALD, Condition.Delay(0)),
        ),
        GateState.RBO to listOf(
            Step(Lever.EMERALD, Condition.WaterState(-1,70)),
            Step(Lever.GOLD,    Condition.Delay(0)),
            Step(Lever.DIAMOND, Condition.WaterState(-4,70)),
        ),
        GateState.RBP to listOf(
            Step(Lever.EMERALD, Condition.Delay(0)),
            Step(Lever.QUARTZ,  Condition.Delay(0)),
            Step(Lever.GOLD,    Condition.WaterState(-1,76)),
            Step(Lever.DIAMOND, Condition.Delay(0)),
            Step(Lever.EMERALD, Condition.WaterState(6,78)),
            Step(Lever.EMERALD, Condition.WaterState(1,65, false, 155)),
        ),
        GateState.ROP to listOf(
            Step(Lever.EMERALD, Condition.Delay(0)),
            Step(Lever.GOLD,    Condition.Delay(0)),
            Step(Lever.DIAMOND, Condition.WaterState(-6,76)),
            Step(Lever.QUARTZ,  Condition.Delay(0)),
            Step(Lever.EMERALD, Condition.WaterState(6,78)),
            Step(Lever.EMERALD, Condition.WaterState(9,76)),
            Step(Lever.WATER,   Condition.Delay(0)),
            Step(Lever.DIAMOND, Condition.WaterState(7,67)),
        ),
        GateState.GBO to listOf(
            Step(Lever.EMERALD, Condition.WaterState(-3,70)),
            Step(Lever.EMERALD, Condition.WaterState(-8,67)),
            Step(Lever.EMERALD, Condition.WaterState(0,63)),
        ),
        GateState.GBP to listOf(
            Step(Lever.QUARTZ,  Condition.Delay(0)),
            Step(Lever.GOLD,    Condition.WaterState(-1,70)),
            Step(Lever.DIAMOND, Condition.WaterState(-8,70)),
            Step(Lever.DIAMOND, Condition.WaterState(-8,63)),
            Step(Lever.DIAMOND, Condition.WaterState(4,68)),
        ),
        GateState.GOP to listOf(
            Step(Lever.QUARTZ,  Condition.Delay(0)),
            Step(Lever.GOLD,    Condition.WaterState(-1,76)),
            Step(Lever.DIAMOND, Condition.Delay(0)),
            Step(Lever.DIAMOND, Condition.WaterState(-4,70)),
            Step(Lever.DIAMOND, Condition.WaterState(5,64)),
        ),
        GateState.BOP to listOf(
            Step(Lever.DIAMOND, Condition.Delay(0)),
            Step(Lever.QUARTZ,  Condition.Delay(0)),
            Step(Lever.GOLD,    Condition.WaterState(5,75)),
            Step(Lever.DIAMOND, Condition.WaterState(7,67)),
        ),
    )
    private val variant1Solutions: Map<GateState, List<Step>> = mapOf(
        GateState.RGB to listOf(
            Step(Lever.EMERALD, Condition.Delay(0)),
            Step(Lever.QUARTZ,  Condition.Delay(0)),
            Step(Lever.DIAMOND, Condition.WaterState(-6,71)),
            Step(Lever.CLAY,    Condition.WaterState(2,66)),
        ),
        GateState.RGO to listOf(
            Step(Lever.QUARTZ,  Condition.WaterState(3,74)),
            Step(Lever.EMERALD, Condition.Delay(0)),
            Step(Lever.CLAY,    Condition.WaterState(-5,59)),
        ),
        GateState.RGP to listOf(
            Step(Lever.QUARTZ,  Condition.WaterState(3,74)),
            Step(Lever.EMERALD, Condition.Delay(0)),
            Step(Lever.EMERALD, Condition.WaterState(7,67)),
            Step(Lever.CLAY,    Condition.WaterState(-5,59)),
        ),
        GateState.RBO to listOf(
            Step(Lever.COAL,    Condition.Delay(0)),
            Step(Lever.EMERALD, Condition.Delay(0)),
            Step(Lever.QUARTZ,  Condition.Delay(0)),
            Step(Lever.DIAMOND, Condition.WaterState(-6,78)),
            Step(Lever.DIAMOND, Condition.WaterState(0,63)),
            Step(Lever.GOLD,    Condition.Delay(0)),
        ),
        GateState.RBP to listOf(
            Step(Lever.EMERALD, Condition.Delay(0)),
            Step(Lever.QUARTZ,  Condition.Delay(0)),
            Step(Lever.COAL,    Condition.WaterState(-6,71)),
            Step(Lever.GOLD,    Condition.WaterState(-9,73)),
            Step(Lever.EMERALD, Condition.Delay(0)),
            Step(Lever.QUARTZ,  Condition.Delay(0)),
            Step(Lever.QUARTZ,  Condition.WaterState(4,78)),
        ),
        GateState.ROP to listOf(
            Step(Lever.EMERALD, Condition.WaterState(5,74)),
            Step(Lever.QUARTZ,  Condition.Delay(0)),
            Step(Lever.EMERALD, Condition.WaterState(2,64)),
            Step(Lever.CLAY,    Condition.Delay(0)),
        ),
        GateState.GBO to listOf(
            Step(Lever.EMERALD, Condition.Delay(0)),
            Step(Lever.GOLD,    Condition.Delay(0)),
            Step(Lever.QUARTZ,  Condition.Delay(0)),
            Step(Lever.DIAMOND, Condition.WaterState(-6,78)),
            Step(Lever.DIAMOND, Condition.WaterState(0,76)),
            Step(Lever.COAL,    Condition.WaterState(-5,61)),
        ),
        GateState.GBP to listOf(
            Step(Lever.EMERALD, Condition.Delay(0)),
            Step(Lever.GOLD,    Condition.Delay(0)),
            Step(Lever.QUARTZ,  Condition.Delay(0)),
            Step(Lever.EMERALD, Condition.WaterState(-6,78)),
            Step(Lever.QUARTZ,  Condition.Delay(0)),
            Step(Lever.COAL,    Condition.WaterState(-5,61)),
        ),
        GateState.GOP to listOf(
            Step(Lever.EMERALD, Condition.WaterState(5,74)),
            Step(Lever.QUARTZ,  Condition.Delay(0)),
            Step(Lever.EMERALD, Condition.WaterState(2,64)),
        ),
        GateState.BOP to listOf(
            Step(Lever.GOLD,    Condition.Delay(0)),
            Step(Lever.QUARTZ,  Condition.Delay(0)),
            Step(Lever.DIAMOND, Condition.Delay(0)),
            Step(Lever.QUARTZ, Condition.WaterState(0,75)),
            Step(Lever.DIAMOND, Condition.WaterState(0,72)),
            Step(Lever.GOLD,    Condition.WaterState(2,61)),
            Step(Lever.DIAMOND, Condition.Delay(0)),
            Step(Lever.DIAMOND, Condition.WaterState(0,63)),
        ),
    )
    private val variant0Solutions: Map<GateState, List<Step>> = mapOf(
        GateState.RGB to listOf(
            Step(Lever.EMERALD, Condition.Delay(0)),
            Step(Lever.COAL,    Condition.WaterState(-6,77)),
            Step(Lever.CLAY,    Condition.Delay(0)),
            Step(Lever.EMERALD, Condition.WaterState(-6,64)),
        ),
        GateState.RGO to listOf(
            Step(Lever.EMERALD, Condition.Delay(0)),
            Step(Lever.COAL,    Condition.WaterState(-6,77)),
            Step(Lever.EMERALD, Condition.WaterState(-6,64)),
            Step(Lever.GOLD,    Condition.Delay(0)),
        ),
        GateState.RGP to listOf(
            Step(Lever.CLAY,    Condition.Delay(0)),
            Step(Lever.GOLD,    Condition.Delay(0)),
            Step(Lever.QUARTZ,  Condition.Delay(0)),
            Step(Lever.DIAMOND, Condition.Delay(0)),
            Step(Lever.GOLD,    Condition.WaterState(6,68)),
            Step(Lever.CLAY,    Condition.Delay(0)),
            Step(Lever.DIAMOND, Condition.WaterState(9,66)),
            Step(Lever.QUARTZ,  Condition.Delay(0)),
            Step(Lever.EMERALD, Condition.WaterState(-9,61)),
        ),
        GateState.RBO to listOf(
            Step(Lever.COAL,    Condition.WaterState(-6,77)),
            Step(Lever.GOLD,    Condition.WaterState(1,66)),
            Step(Lever.CLAY,    Condition.WaterState(5,63)),
            Step(Lever.EMERALD, Condition.Delay(0)),
            Step(Lever.QUARTZ,  Condition.Delay(0)),
        ),
        GateState.RBP to listOf(
            Step(Lever.CLAY,    Condition.WaterState(-6,77)),
            Step(Lever.COAL,    Condition.WaterState(-6,70)),
            Step(Lever.DIAMOND, Condition.Delay(0)),
            Step(Lever.DIAMOND, Condition.WaterState(9,66)),
        ),
        GateState.ROP to listOf(
            Step(Lever.CLAY,    Condition.WaterState(-6,77)),
            Step(Lever.COAL,    Condition.WaterState(-6,70)),
            Step(Lever.DIAMOND, Condition.Delay(0)),
            Step(Lever.EMERALD, Condition.WaterState(9,66)),
        ),
        GateState.GBO to listOf(
            Step(Lever.COAL,    Condition.Delay(0)),
            //TODO test faster alternative in two commented lines below
//            Step(Lever.DIAMOND, Condition.Delay(0)),
//            Step(Lever.DIAMOND, Condition.WaterState(2,72)),
            Step(Lever.GOLD,    Condition.WaterState(-5,63)),
            Step(Lever.CLAY,    Condition.WaterState(5,63)),
        ),
        GateState.GBP to listOf(
            //TODO additional clay flick in middle might save a second
            Step(Lever.COAL,    Condition.Delay(0)),
            Step(Lever.DIAMOND, Condition.Delay(0)),
            Step(Lever.CLAY,    Condition.Delay(0)),
            Step(Lever.DIAMOND, Condition.WaterState(9,66)),
            Step(Lever.CLAY, Condition.WaterState(0,61)),
        ),
        GateState.GOP to listOf(
            Step(Lever.COAL,    Condition.Delay(0)),
            Step(Lever.DIAMOND, Condition.Delay(0)),
            Step(Lever.CLAY,    Condition.Delay(0)),
            Step(Lever.DIAMOND, Condition.WaterState(9,66)),
            Step(Lever.EMERALD, Condition.Delay(0)),
            Step(Lever.CLAY,    Condition.Delay(0)),
            Step(Lever.EMERALD, Condition.WaterState(-2,63)),
        ),
        GateState.BOP to listOf(
            Step(Lever.COAL,    Condition.Delay(0)),
            Step(Lever.DIAMOND, Condition.Delay(0)),
            Step(Lever.CLAY,    Condition.Delay(0)),
            Step(Lever.DIAMOND, Condition.WaterState(9,66)),
            Step(Lever.EMERALD, Condition.Delay(0)),
        ),
    )


    /**
     * Used as temporary container for the solution that is currently being executed.
     */
    private var solutionSteps: MutableList<Step> = mutableListOf()
    private var doingWater = false
    private var flowTicks = 0
    private var etherCDTicks = 0

    init {
        this.addSettings(
            etherCd,
            completionSound,
            volume,
            advancedEther
        )
    }


    override fun onEnable() {
        reset()
        super.onEnable()
    }

    /**
     * Registers when a lever is clicked.
     * Used to start auto water when flicking the start lever.
     */
    @SubscribeEvent
    fun onInteract(event: PlayerInteractEvent) {
        if ( !FloppaClient.inDungeons || doingWater) return
        if (Dungeon.currentRoomPair?.first?.data?.name != "Water Board") return
        val roomPair = Dungeon.currentRoomPair ?: return
        val blockPos = event.pos
        try { // for some reason getBlockState can throw null pointer exception
            val block = mc.theWorld?.getBlockState(blockPos)?.block ?: return

            if ( block == Blocks.lever){
                if(RoomUtils.getRelativePos(blockPos,roomPair) == BlockPos(0,60,-10)) {
                    modMessage("Water started.")

                    //TODO CLEAN THIS UP

                    val state = Gates.getGateState() ?: return modMessage("Gates not in default configuration.")
                    solutionSteps.clear()
                    when (variant) {
                        3 -> {
                            solutionSteps.addAll(variant3Solutions[state]!!)
                        }
                        2 -> {
                            solutionSteps.addAll(variant2Solutions[state]!!)
                        }
                        1-> {
                            solutionSteps.addAll(variant1Solutions[state]!!)
                        }
                        0 -> {
                            solutionSteps.addAll(variant0Solutions[state]!!)
                        }
                        else -> {
                            modMessage("Water layout not recognized.")
                            return
                        }
                    }
                    flowTicks = 0
                    doingWater = true
                    modMessage("Attempting to solve water board Variant $variant, in configuration ${state.name}")
                }
            }
        } catch (_: Exception) { }
    }

    /**
     * Used to detect the water layout.
     */
    @SubscribeEvent
    fun onTick(event: ClientTickEvent) {
        if (event.phase != TickEvent.Phase.START) return
        // Update flow ticks
        if (doingWater) flowTicks += 1
        if (doingWater && flowTicks > 20*20){
            modMessage("Auto Water timed out.")
            stop()
        }
        if (etherCDTicks > 0) etherCDTicks -= 1

        inWater = Dungeon.currentRoomPair?.first?.data?.name == "Water Board"
        if (!inWater) return
        val roomPair = Dungeon.currentRoomPair ?: return

        /** Update which gates are open once per tick */
        for (color in GateColor.values()){
            Gates.setColorState(color,mc.theWorld.getBlockState(getRealPos(BlockPos(0,55,color.ordinal), roomPair)).block == Blocks.piston_head)
        }

        /** If the variant has not been determined so far find it */
        if (variant == -1) {
            var foundGold = false
            var foundClay = false
            var foundEmerald = false
            var foundQuartz = false
            var foundDiamond = false

            val waterPos = getRealPos(BlockPos(0,78,11),roomPair)

            val x = waterPos.x
            val z = waterPos.z


            // Detect first blocks near water stream
            for (puzzleBlockPos in BlockPos.getAllInBox(
                BlockPos(x + 1, 78, z + 1),
                BlockPos(x - 1, 77, z - 1)
            )) {
                val block = mc.theWorld.getBlockState(puzzleBlockPos).block
                when {
                    block === Blocks.gold_block -> {
                        foundGold = true
                    }
                    block === Blocks.hardened_clay -> {
                        foundClay = true
                    }
                    block === Blocks.emerald_block -> {
                        foundEmerald = true
                    }
                    block === Blocks.quartz_block -> {
                        foundQuartz = true
                    }
                    block === Blocks.diamond_block -> {
                        foundDiamond = true
                    }
                }
            }
            if (foundGold && foundClay) {
                variant = 0
            } else if (foundEmerald && foundQuartz) {
                variant = 1
            } else if (foundQuartz && foundDiamond) {
                variant = 2
            } else if (foundGold && foundQuartz) {
                variant = 3
            }
        }


    }

    /**
     * Pre move is used for positioning.
     */
    @SubscribeEvent
    fun onPreMove(event: PositionUpdateEvent.Pre) {
        if(!doingWater || !inWater) return
        if(solutionSteps.isEmpty()) {
            finish()
            return
        }
        val nextLever = solutionSteps.first().lever
        val secondLever = solutionSteps.getOrNull(1)?.lever
        if (!nextLever.canReach() && etherCDTicks <= 0) {
            etherCDTicks = etherCd.value.toInt()
            val successful = if( advancedEther.enabled) {
                nextLever.etherwarpToLever(secondLever)
            }else {
                nextLever.etherwarpToLever()
            }
            if (!successful) stop()
        }
    }

    /**
     * PostMove is used for interacting with the levers.
     */
    @SubscribeEvent
    fun onPostMove(event: PositionUpdateEvent.Post) {
        if (!doingWater) return
        if(solutionSteps.isEmpty()) {
            finish()
            return
        }
        val nextStep = solutionSteps.first()
        if (nextStep.condition.isMet()) {
            /** Try tp flick the lever and if successfull move on to the next step. */
            val successful = nextStep.lever.flick()
            if (successful) solutionSteps.removeAt(0)
        }
    }


    enum class GateColor {
        RED, GREEN, BLUE, ORANGE, PURPLE
    }

    data class Step(val lever: Lever, val condition: Condition)

    /**
     * Provides all the lever related actions.
     */
    enum class Lever {
        COAL, GOLD, QUARTZ, DIAMOND, EMERALD, CLAY, WATER;

        /**
         * Attempts to etherwarp to this lever.
         */
        fun etherwarpToLever(): Boolean {
            val roomPair = Dungeon.currentRoomPair ?: return false
            return FakeActionUtils.etherwarpTo(this.etherPos(roomPair))
        }

        /**
         * Attempts to etherwarp to reach this and the given lever.
         */
        fun etherwarpToLever(lever: Lever?): Boolean {
            val roomPair = Dungeon.currentRoomPair ?: return false
            return FakeActionUtils.etherwarpTo(this.etherPos(roomPair,lever))
        }

        /**
         * Flicks the lever.
         * Returns false if not in range.
         */
        fun flick(): Boolean {
            val roomPair = Dungeon.currentRoomPair ?: return false
            return FakeActionUtils.clickBlock(this.leverPos(roomPair), 6.0)
        }

        /**
         * Returns the absolute blockPos for this lever in the given roomPair.
         */
        private fun leverPos(roomPair: Pair<Room, Int>): BlockPos {
            return getRealPos(this.relLeverPos(),roomPair)
        }

        /**
         * returns the room relative blockPos for this lever.
         */
        private fun relLeverPos(): BlockPos {
            return when(this) {
                COAL    -> BlockPos(5,61,-5)
                GOLD    -> BlockPos(5,61,0)
                QUARTZ  -> BlockPos(5,61,5)
                DIAMOND -> BlockPos(-5,61,5)
                EMERALD -> BlockPos(-5,61,0)
                CLAY    -> BlockPos(-5,61,-5)
                WATER   -> BlockPos(0,60,-10)
            }
        }

        /**
         * Returns the absolute blockPos to etherwarp to, to reach this lever.
         */
        private fun etherPos(roomPair: Pair<Room, Int>): BlockPos {
            return getRealPos(this.relEtherPos(),roomPair)
        }

        /**
         * Returs the etherwarp target blockpos in room relative coords.
         */
        private fun relEtherPos(): BlockPos {
            return when(this) {
                COAL, CLAY, WATER -> BlockPos(0,58,-6)
                GOLD, EMERALD     -> BlockPos(0,58,0)
                QUARTZ, DIAMOND   -> BlockPos(0,58,5)
            }
        }

        /**
         * Returns the absolute blockPos to etherwarp to reach given lever pair if possible or just this lever otherwise.
         */
        private fun etherPos(roomPair: Pair<Room, Int>, lever: Lever?): BlockPos {
            return getRealPos(this.relEtherPos(lever),roomPair)
        }

        /**
         * Returs the etherwarp target blockpos in room relative coords to reach given lever pair if possible or just
         * this lever otherwise.
         */
        private fun relEtherPos(lever: Lever?): BlockPos {
            return when(setOf(this, lever)){
                setOf(COAL,GOLD)        -> BlockPos(3,58,-2)
                setOf(QUARTZ,GOLD)      -> BlockPos(3,58,2)
                setOf(DIAMOND,EMERALD)  -> BlockPos(-3,58,2)
                setOf(CLAY,EMERALD)     -> BlockPos(-3,58,-2)
                else -> this.relEtherPos()
            }
        }

        /**
         * Checks whether this lever can be reached.
         */
        fun canReach(): Boolean {
            val roomPair = Dungeon.currentRoomPair ?: return false
            val blockPos = this.leverPos(roomPair)
            return mc.thePlayer.getDistance(blockPos.x.toDouble(), blockPos.y.toDouble() - mc.thePlayer.eyeHeight, blockPos.z.toDouble()) < 6
        }
    }

    /**
     * Conditions for when to flick a lever.
     */
    open class Condition {
        /**
         * A simple delay as condition.
         */
        class Delay(private val ticks: Int) : Condition() {
            /**
             * Returns true when the water has been flowing for at least the in the condition specified amount of ticks.
             */
            override fun isMet():Boolean {
                return flowTicks >= ticks
            }
        }

        /**
         * onWater determines whether the change to water or the change to air is expected.
         * When set to true it will trigger once water appears.
         */
        class WaterState(private val onWater: Boolean, val blockPos: BlockPos, private val minDelay: Int = 0) : Condition() {

            /**
             * Simplified constructor that expects water at x,y on the board.
             */
            constructor(x: Int, y: Int, onWater: Boolean = true, minDelay: Int = 0) : this(onWater,BlockPos(x,y,11), minDelay)

            /**
             * Checks whether the specified block is Water (or air if onWater is false) and the water has been
             * flowing ofr at least minDelay ticks.
             */
            override fun isMet(): Boolean {
                val roomPair = Dungeon.currentRoomPair ?: return false
                val block = mc.theWorld.getBlockState(getRealPos(blockPos,roomPair)).block
                val isCorrectBlock = if (onWater) {
                    block == Blocks.water || block == Blocks.flowing_water
                } else {
                    block == Blocks.air
                }
                return isCorrectBlock && flowTicks >= minDelay
            }
        }

        /**
         * Checks whether the condition is met.
         * Overridden in members.
         */
        open fun isMet(): Boolean {return false}
    }

    /**
     * Contains and manages information about the gates.
     */
    object Gates {
        var red:    Boolean = false
         private set
        var green:  Boolean = false
         private set
        var blue:   Boolean = false
         private set
        var orange: Boolean = false
         private set
        var purple: Boolean = false
         private set

        /**
         * Sets the extended state for the specified color to the given state.
         */
        fun setColorState(color: GateColor, state: Boolean) {
            when(color) {
                GateColor.RED    -> red    = state
                GateColor.GREEN  -> green  = state
                GateColor.BLUE   -> blue   = state
                GateColor.ORANGE -> orange = state
                GateColor.PURPLE -> purple = state
            }
        }

        /**
         * Gets the extended state for the specified color.
         */
        fun getColorState(color: GateColor): Boolean {
            return when(color) {
                GateColor.RED    -> red
                GateColor.GREEN  -> green
                GateColor.BLUE   -> blue
                GateColor.ORANGE -> orange
                GateColor.PURPLE -> purple
            }
        }

        /**
         * Returns the corresponding start state for the current state, or null if not exactly 3 gates are closed.
         */
        fun getGateState(): GateState? {
            return when(listOf(red, green, blue, orange, purple)) {
                listOf(true,true,true,false,false) -> GateState.RGB
                listOf(true,true,false,true,false) -> GateState.RGO
                listOf(true,true,false,false,true) -> GateState.RGP
                listOf(true,false,true,true,false) -> GateState.RBO
                listOf(true,false,true,false,true) -> GateState.RBP
                listOf(true,false,false,true,true) -> GateState.ROP
                listOf(false,true,true,true,false) -> GateState.GBO
                listOf(false,true,true,false,true) -> GateState.GBP
                listOf(false,true,false,true,true) -> GateState.GOP
                listOf(false,false,true,true,true) -> GateState.BOP
                else -> null
            }
        }

        /**
         * Restes all gates to open
         */
        fun reset() {
            red = false
            green = false
            blue = false
            orange = false
            purple = false
        }
    }

    /**
     * All 10 possible configuration that have exactly 3 gates closed. also contains the gate states as values,
     * but that is probably not necessary
     */
    enum class GateState(val redState: Boolean, val greenState: Boolean, val blueState: Boolean, val orangeState: Boolean, val purpleState: Boolean) {
        RGB(true,true,true,false,false),
        RGO(true,true,false,true,false),
        RGP(true,true,false,false,true),
        RBO(true,false,true,true,false),
        RBP(true,false,true,false,true),
        ROP(true,false,false,true,true),
        GBO(false,true,true,true,false),
        GBP(false,true,true,false,true),
        GOP(false,true,false,true,true),
        BOP(false,false,true,true,true)
    }

    /**
     * Reset on warp
     */
    @SubscribeEvent
    fun onWorldChange(event: WorldEvent.Load) {
        reset()
    }

    /**
     * Stops and finishes the auto water process.
     */
    private fun finish() {
        doingWater = false
        modMessage("Finished Auto Water in ${
            DecimalFormat("#.##").format(flowTicks/20.0)} seconds!")
        if (completionSound.enabled) playLoudSound("random.orb", volume.value.toFloat(),0f)
        return
    }

    /**
     * Resets the variables.
     */
    fun reset() {
        variant = -1
        doingWater = false
        inWater = false
        flowTicks = 0
        Gates.reset()
        solutionSteps.clear()
    }

    /**
     * Force stops the auto water process.
     */
    fun stop() {
        doingWater = false
        modMessage("Stopped Auto Water.")
    }
}