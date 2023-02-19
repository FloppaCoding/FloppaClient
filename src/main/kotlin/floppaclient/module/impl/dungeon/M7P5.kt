package floppaclient.module.impl.dungeon

import floppaclient.FloppaClient
import floppaclient.FloppaClient.Companion.mc
import floppaclient.events.BlockStateChangeEvent
import floppaclient.events.ReceivePacketEvent
import floppaclient.module.Category
import floppaclient.module.Module
import floppaclient.module.settings.Setting.Companion.withDependency
import floppaclient.module.settings.Visibility
import floppaclient.module.settings.impl.BooleanSetting
import floppaclient.module.settings.impl.NumberSetting
import floppaclient.ui.hud.HudElement
import floppaclient.utils.render.WorldRenderUtils.drawCustomSizedBoxAt
import floppaclient.utils.Utils
import net.minecraft.init.Blocks
import net.minecraft.network.play.server.S2APacketParticles
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumParticleTypes
import net.minecraft.util.Vec3
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.awt.Color

/**
 * QOL Stuff For M7,
 * @author Stivais
 */
object M7P5 : Module(
    "M7 Dragon QOL",
    category = Category.DUNGEON,
    description = "A collection of QOL features for the Dragon Phase in Master Mode Floor 7."
) {
    private val statueBox = BooleanSetting("Statue Box", true, description = "Renders a box, showing where you can kill a dragon.")
    private val boxThickness = NumberSetting("Box Thickness", 2.5, 0.1, 10.0, 0.1, description = "Thickness of the Statue Box.")
            .withDependency { this.statueBox.enabled }
    private val dragonSpawnTimer = BooleanSetting("Dragon Spawn Timer", true, description = "Renders a timer for when a dragon spawns in.")

    private val xHud = NumberSetting("x", default = 200.0, visibility = Visibility.HIDDEN)
    private val yHud = NumberSetting("y", default = 50.0, visibility = Visibility.HIDDEN)
    private val scaleHud = NumberSetting("scale", 1.0, 0.1, 4.0, 0.01, Visibility.HIDDEN)

    private val dragAliveTime: MutableMap<M7Drags, Long> = mutableMapOf()

    init {
        this.addSettings(
            statueBox,
            boxThickness,
            dragonSpawnTimer
        )
    }

    override fun onEnable() {
        MinecraftForge.EVENT_BUS.register(SpawnTimer)
        super.onEnable()
    }

    override fun onDisable() {
        MinecraftForge.EVENT_BUS.unregister(SpawnTimer)
        super.onDisable()
    }

    @SubscribeEvent
    fun onWorldRender(event: RenderWorldLastEvent) {
        if (!statueBox.enabled) return
        if (M7Drags.GREEN.isAlive) drawCustomSizedBoxAt(7.0, 30.0, 8.0, 20.0, 80.0, 30.0, Color(0, 255, 0, 1), boxThickness.value.toFloat(), false)
        if (M7Drags.RED.isAlive) drawCustomSizedBoxAt(14.5, 25.0, 13.0, 15.0, 45.5, 25.0, Color(255, 0, 0, 1), boxThickness.value.toFloat(), false)
        if (M7Drags.ORANGE.isAlive) drawCustomSizedBoxAt(72.0, 30.0, 7.0, 20.0, 47.0, 30.0, Color(255, 128, 0, 1), boxThickness.value.toFloat(), false)
        if (M7Drags.BLUE.isAlive) drawCustomSizedBoxAt(71.5, 25.0, 16.0, 10.0, 82.5, 25.0, Color(0, 255, 255, 1), boxThickness.value.toFloat(), false)
        if (M7Drags.PURPLE.isAlive) drawCustomSizedBoxAt(45.5, 23.0, 13.0, 10.0, 113.5, 23.0, Color(128, 0, 255), boxThickness.value.toFloat(), false)
    }

    @SubscribeEvent(receiveCanceled = true)
    fun onParticle(event: ReceivePacketEvent) {
        if (!FloppaClient.inDungeons) return

        val packet = event.packet
        if (packet !is S2APacketParticles) return
        if (packet.particleType.equals(EnumParticleTypes.FLAME)) {
            val drags = M7Drags.values().find { it.particles.xCoord == packet.xCoordinate && it.particles.zCoord == packet.zCoordinate } ?: return
            if (!drags.isAlive) dragAliveTime[drags] = System.currentTimeMillis() + 5000
            drags.isAlive = true
        }
    }

    @SubscribeEvent
    fun onBlockStateChange(event: BlockStateChangeEvent) {
        if (!FloppaClient.inDungeons) return
        if (event.pos.y == 18 || event.pos.y == 19 && event.newState.block === Blocks.air && event.oldState.block === Blocks.stone_slab)
            M7Drags.values().find { it.statue == event.pos }?.isAlive = false
    }

    @SubscribeEvent
    fun onWorldLoad(event: WorldEvent.Load) {
        M7Drags.values().forEach { it.isAlive = false; dragAliveTime.clear() }
    }

    object SpawnTimer : HudElement(
        xHud,
        yHud,
        mc.fontRendererObj.getStringWidth("Purple: 5000"),
        mc.fontRendererObj.FONT_HEIGHT,
        scaleHud
    ) {
        override fun renderHud() {
            if (dragonSpawnTimer.enabled) {
                var yOffs = 0

                dragAliveTime.forEach {
                    val time = it.value - System.currentTimeMillis()
                    if (time < 0) return@forEach

                    mc.fontRendererObj.drawString("${it.key.Name}§r: ${Utils.timeFormat(time)}s", 0, yOffs, 0xffffff)
                    yOffs += mc.fontRendererObj.FONT_HEIGHT
                }
            }
            super.renderHud()
        }
    }

    enum class M7Drags(
        val Name: String,
        val particles: Vec3,
        val statue: BlockPos,
        var isAlive: Boolean = false
    ) {
        GREEN("§a§lGreen", Vec3(27.0, 19.0, 94.0), BlockPos(32, 19, 94)),
        RED("§c§lRed", Vec3(27.0, 19.0, 59.0), BlockPos(32, 18, 59)),
        ORANGE("§6§lOrange", Vec3(85.0, 19.0, 56.0), BlockPos(80, 19, 56)),
        BLUE("§b§lBlue", Vec3(84.0, 19.0, 94.0), BlockPos(79, 19, 94)),
        PURPLE("§d§lPurple", Vec3(56.0, 19.0, 125.0), BlockPos(56, 18, 120))
    }
}