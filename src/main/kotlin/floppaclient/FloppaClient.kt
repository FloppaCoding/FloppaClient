package floppaclient

import floppaclient.commands.*
import floppaclient.config.AutoActionConfig
import floppaclient.config.ExtrasConfig
import floppaclient.config.ModuleConfig
import floppaclient.floppamap.core.Room
import floppaclient.floppamap.dungeon.Dungeon
import floppaclient.floppamap.extras.EditMode
import floppaclient.floppamap.extras.Extras
import floppaclient.module.ModuleManager
import floppaclient.ui.clickgui.ClickGUI
import floppaclient.utils.LocationManager
import floppaclient.utils.fakeactions.FakeInventoryActionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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

/**
 * ## This is the main class of the mod.
 *
 * It dispatches things such as setting up all Modules and loading in data from the config.
 *
 * The companion object also provides some very frequently used or fundamental properties.
 *
 * @author Aton
 */
@Suppress("UNUSED_PARAMETER")
@Mod(
    modid = FloppaClient.MOD_ID,
    name = FloppaClient.MOD_NAME,
    version = FloppaClient.MOD_VERSION,
    clientSideOnly = true
)
class FloppaClient {
    @Mod.EventHandler
    fun preInit(event: FMLPreInitializationEvent) {
        if (!MODULES_PATH.exists()) {
            MODULES_PATH.mkdirs()
        }
        scope.launch {
            launch(Dispatchers.IO) {
                autoactions.loadConfig()
            }
            launch(Dispatchers.IO) {
                extras.loadConfig()
            }
        }
    }

    @Mod.EventHandler
    fun onInit(event: FMLInitializationEvent) {
        ModuleManager.loadModules()

        listOf(
            FloppaClientCommands(),
            AddCommand(),
            RemoveCommand(),
            AddEtherCommand(),
            RemoveEtherCommand(),
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
            ModuleManager,
            FakeInventoryActionManager,
            LocationManager,
        ).forEach(MinecraftForge.EVENT_BUS::register)
    }

    @Mod.EventHandler
    fun postInit(event: FMLLoadCompleteEvent) = runBlocking {
        //Load in the module config post init so that all the minecraft classes are already present.
        runBlocking {
            launch(Dispatchers.IO) {
                moduleConfig.loadConfig()
            }
        }
        ModuleManager.initializeModules()

        clickGUI = ClickGUI()
    }

    @SubscribeEvent
    fun onTick(event: ClientTickEvent) {
        if (event.phase != TickEvent.Phase.START) return
        tickRamp++
        totalTicks++
        if (display != null) {
            mc.displayGuiScreen(display)
            display = null
        }
        if (tickRamp % 20 == 0) {
            tickRamp = 0
        }
    }

    @SubscribeEvent
    fun onDisconnect(event: ClientDisconnectionFromServerEvent) {
        moduleConfig.saveConfig()
    }

    @SubscribeEvent
    fun onWorldChange(@Suppress("UNUSED_PARAMETER") event: WorldEvent.Load) {
        tickRamp = 18
    }

    companion object {
        const val MOD_ID = "fc"
        const val MOD_NAME = "Floppa Client"
        const val MOD_VERSION = "1.0.3"
        const val CHAT_PREFIX = "§6§lFloppa§r§eClient §6§l»§r"
        const val SHORT_PREFIX = "§6§lF§r§eC §6§l»§r"
        const val RESOURCE_DOMAIN = "floppaclient"
        const val CONFIG_DOMAIN = RESOURCE_DOMAIN


        @JvmField
        val mc: Minecraft = Minecraft.getMinecraft()

        var display: GuiScreen? = null

        val scope = CoroutineScope(EmptyCoroutineContext)

        var autoactions = AutoActionConfig(File(mc.mcDataDir, "config/$CONFIG_DOMAIN/autoaction"))
        val extras = ExtrasConfig(File(mc.mcDataDir, "config/$CONFIG_DOMAIN/extras"))
        val moduleConfig = ModuleConfig(File(mc.mcDataDir, "config/$CONFIG_DOMAIN"))

        val MODULES_PATH = File(mc.mcDataDir,"config/$CONFIG_DOMAIN/modules")

        lateinit var clickGUI: ClickGUI

        //TODO remove these
        var currentRegionPair: Pair<Room, Int>? by LocationManager::currentRegionPair
        var onHypixel: Boolean  by LocationManager::onHypixel
        var inSkyblock: Boolean  by LocationManager::inSkyblock
        var inDungeons: Boolean  by LocationManager::inDungeons
        /**
         * Keeps track of elapsed ticks, gets reset at 20
         */
        var tickRamp = 0
        var totalTicks: Long = 0
    }
}
