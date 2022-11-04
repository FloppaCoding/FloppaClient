package floppaclient

import floppaclient.commands.*
import floppaclient.config.ExtrasConfig
import floppaclient.config.ModuleConfig
import floppaclient.funnymap.core.Room
import floppaclient.funnymap.features.dungeon.Dungeon
import floppaclient.funnymap.features.extras.EditMode
import floppaclient.funnymap.features.extras.Extras
import floppaclient.funnymap.features.extras.RoomUtils
import floppaclient.module.ModuleManager
import floppaclient.module.impl.dungeon.AutoTerms
import floppaclient.module.impl.render.DungeonWarpTimer
import floppaclient.ui.clickgui.ClickGUI
import floppaclient.utils.ScoreboardUtils
import floppaclient.utils.Utils
import floppaclient.utils.fakeactions.FakeInventoryActionManager
import gg.essential.api.EssentialAPI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiScreen
import net.minecraftforge.client.ClientCommandHandler
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.event.FMLInitializationEvent
import net.minecraftforge.fml.common.event.FMLLoadCompleteEvent
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent
import net.minecraftforge.fml.common.network.FMLNetworkEvent.ClientDisconnectionFromServerEvent
import java.io.File
import kotlin.coroutines.EmptyCoroutineContext

@Mod(
    modid = FloppaClient.MOD_ID,
    name = FloppaClient.MOD_NAME,
    version = FloppaClient.MOD_VERSION,
    clientSideOnly = true
)
class FloppaClient {
    @Mod.EventHandler
    fun preInit(event: FMLPreInitializationEvent) {
        // this seems to be redundant
        val directory = File(event.modConfigurationDirectory, "floppaclient")
        if (!directory.exists()) {
            directory.mkdirs()
        }

    }

    @Mod.EventHandler
    fun onInit(event: FMLInitializationEvent) {

        listOf(
            FloppaClientCommands(),
            VertClipCommand(),
            Clip3DCommand(),
            HClipCommand(),
            EditModeCommand(),
            WhereCommand(),
            WardrobeCommand,
        ).forEach {
            ClientCommandHandler.instance.registerCommand((it))
        }

        listOf(
            this,
            Dungeon,
            Extras,
            EditMode,
            AutoTerms,
            ModuleManager,
            FakeInventoryActionManager,
        ).forEach(MinecraftForge.EVENT_BUS::register)

        clickGUI = ClickGUI()
    }

    @Mod.EventHandler
    fun postInit(event: FMLLoadCompleteEvent) = runBlocking {

        launch {
            extras.loadConfig()
            moduleConfig.loadConfig()
            clickGUI.setUpPanels()

            //This is required for the Warp cooldown to track in the background whithout the need to enable it first.
            if(!DungeonWarpTimer.enabled && DungeonWarpTimer.trackInBackground.enabled) {
                MinecraftForge.EVENT_BUS.register(DungeonWarpTimer)
            }
        }
    }

    @SubscribeEvent
    fun onTick(event: ClientTickEvent) {
        if (event.phase != TickEvent.Phase.START) return
        tickCount++
        if (display != null) {
            mc.displayGuiScreen(display)
            display = null
        }
        if (tickCount % 20 == 0) {
            if (mc.thePlayer != null) {
                val onHypixel = EssentialAPI.getMinecraftUtil().isHypixel()

                inSkyblock = onHypixel && mc.theWorld.scoreboard.getObjectiveInDisplaySlot(1)
                    ?.let { ScoreboardUtils.cleanSB(it.displayName).contains("SKYBLOCK") } ?: false

                // If alr known that in dungeons dont update the value. It does get reset to false on world change.
                if (!inDungeons) {
                    inDungeons = inSkyblock && ScoreboardUtils.sidebarLines.any {
                        ScoreboardUtils.cleanSB(it).run {
                            (contains("The Catacombs") && !contains("Queue")) || contains("Dungeon Cleared:")
                        }
                    }
                }
            }
            tickCount = 0
        }
        val newRegion = Utils.getArea()
        if (currentRegionPair?.first?.data?.name != newRegion){
            currentRegionPair = newRegion?.let { Pair( RoomUtils.instanceRegionRoom(it) , 0) }
        }
    }

    @SubscribeEvent
    fun onDisconnect(event: ClientDisconnectionFromServerEvent) {
        inSkyblock = false
        inDungeons = false
        moduleConfig.saveConfig()
    }

    @SubscribeEvent
    fun onWorldChange(event: WorldEvent.Load) {
        inDungeons = false
        currentRegionPair = null
        tickCount = 18
    }

    companion object {
        const val MOD_ID = "fc"
        const val MOD_NAME = "Floppa Client"
        const val MOD_VERSION = "0.1.8"
        const val CHAT_PREFIX = "§0§l[§4§lFloppa Client§0§l]§r"
        const val SHORT_PREFIX = "§0§l[§4§lFC§0§l]§r"

        @JvmField
        val mc: Minecraft = Minecraft.getMinecraft()

        var display: GuiScreen? = null

        val scope = CoroutineScope(EmptyCoroutineContext)

        var extras = ExtrasConfig(File(mc.mcDataDir, "config/floppaclient/extras"))
        val moduleConfig = ModuleConfig(File(mc.mcDataDir, "config/floppaclient"))

        lateinit var clickGUI: ClickGUI

        var currentRegionPair: Pair<Room, Int>? = null
        var inSkyblock = false
        var inDungeons = false
            get() = inSkyblock && field
        var tickCount = 0
    }
}
