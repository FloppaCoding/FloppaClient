package floppaclient

import floppaclient.commands.Clip3DCommand
import floppaclient.commands.FloppaClientCommands
import floppaclient.commands.HClipCommand
import floppaclient.commands.VertClipCommand
import floppaclient.config.ModuleConfig
import floppaclient.funnymap.features.dungeon.Dungeon
import floppaclient.module.ModuleManager
import floppaclient.module.impl.render.DungeonWarpTimer
import floppaclient.ui.clickgui.ClickGUI
import floppaclient.utils.ScoreboardUtils
import gg.essential.api.EssentialAPI
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
        ).forEach {
            ClientCommandHandler.instance.registerCommand((it))
        }

        listOf(
            this,
            Dungeon,
            ModuleManager
        ).forEach(MinecraftForge.EVENT_BUS::register)

        clickGUI = ClickGUI()
    }

    @Mod.EventHandler
    fun postInit(event: FMLLoadCompleteEvent) = runBlocking {

        launch {
            moduleConfig.loadConfig()

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

                inDungeons = inSkyblock && ScoreboardUtils.sidebarLines.any {
                    ScoreboardUtils.cleanSB(it).run {
                        (contains("The Catacombs") && !contains("Queue")) || contains("Dungeon Cleared:")
                    }
                }
            }
            tickCount = 0
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
    }

    companion object {
        const val MOD_ID = "fc"
        const val MOD_NAME = "Floppa Client"
        const val MOD_VERSION = "0.1.1"
        const val CHAT_PREFIX = "§0§l[§4§lFloppa Client§0§l]§r"
        const val SHORT_PREFIX = "§0§l[§4§lFC§0§l]§r"

        @JvmField
        val mc: Minecraft = Minecraft.getMinecraft()

        var display: GuiScreen? = null

        val moduleConfig = ModuleConfig(File(mc.mcDataDir, "config/floppaclient"))

        lateinit var clickGUI: ClickGUI

        var inSkyblock = false
        var inDungeons = false
        var tickCount = 0
    }
}
