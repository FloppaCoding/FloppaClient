package floppaclient.module

import floppaclient.events.PreKeyInputEvent
import floppaclient.events.PreMouseInputEvent
import floppaclient.module.impl.dungeon.*
import floppaclient.module.impl.keybinds.AddKeybind
import floppaclient.module.impl.keybinds.KeyBind
import floppaclient.module.impl.misc.*
import floppaclient.module.impl.player.*
import floppaclient.module.impl.render.*
import floppaclient.module.settings.Setting
import floppaclient.ui.clickgui.ClickGUI
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

/**
 * ## This object handles all the modules of the mod.
 *
 * After making a [Module] it just has to be added to the [modules] list and
 * everything else will be taken care of automatically. This entails:
 *
 * It will be added to the click gui in the order it is put in here. But keep in mind that the category is set within
 * the module. The comments here are only for readability.
 *
 * All settings that are registered within the module will be saved to and loaded from the module config.
 * For this to properly work remember to register the settings to the module.
 *
 * The module will be registered and unregistered to the forge eventbus when it is enabled / disabled.
 *
 * The module will be informed of its keybind presses.
 *
 *
 * @author Aton
 * @see Module
 * @see Setting
 */
object ModuleManager {
    /**
     * All modules have to be added to this list to function!
     */
    val modules: ArrayList<Module> = arrayListOf(
        //DUNGEON
        SecretChime,
        SecretAura,
        AutoWeirdos,
        AutoWater,
        AutoBlaze,
        TicTacToeSolver,
        ExtraStats,
        TrapGearSwap,
        AutoLeap,
        MelodyMessage,
        InstaCrystal,
        TerminalAura,
        AutoTerms,
        AutoDevices,
        DungeonKillAura,
        IceSprayAura,
        CancelChestOpen,
        PartyTracker,
        M7P5,

        //RENDER
        ClickGui,
        EditHud,
        DrawRoutes,
        DungeonESP,
        ChestEsp,
        FullBright,
        XRay,
        DungeonWarpTimer,
        DungeonMap,
        MapRooms,
        ExtraBlocks,
        DoorESP,
        CoordinateDisplay,
        DayCounter,
        ItemAnimations,
        Camera,
        RunOverview,

        //PLAYER
        HotbarSwapper,
        ArmorSwapper,
        AutoClip,
        AutoEther,
        FreeCam,
        Clip,
        HClip,
        BarPhase,
        NoRotate,
        JerryRocket,

        //MISC
        QOL,
        ClipSettings,
        InvActions,
        AutoSprint,
        AutoRagnarock,
        AutoHarp,
        Salvage,
        Enchanting,
        SellGarbo,
        JerryBoxOpener,
        GhostBlocks,
        StonkDelay,
        SecretTriggerbot,
        TerminatorClicker,
        AutoClicker,
        AutoWeaponSwap,
        CancelInteract,
        FastMine,
        ChatCleaner,

//        DevModule,

        //KEYBIND
        AddKeybind,
    )

    /**
     * Initialize the Modules.
     * This is run on game startup during the FMLInitializationEvent.
     */
    fun initializeModules() {
        modules.forEach {
            it.initializeModule()
        }
    }

    /**
     * Creates a new keybind module and adds it to the list.
     */
    fun addNewKeybind(): KeyBind {
        val number = (modules
            .filter{module -> module.name.startsWith("New")}
            .map {module -> module.name.filter { c -> c.isDigit() }.toIntOrNull()}
            .maxByOrNull { it ?: 0} ?: 0) + 1
        val keyBind = KeyBind("New $number")
        modules.add(keyBind)
        return keyBind
    }

    fun removeKeyBind(bind: KeyBind) {
        modules.remove(bind)
        ClickGUI.panels.find { it.category === Category.KEY_BIND }?.moduleButtons?.removeIf { it.module === bind }
    }

    /**
     * Handles the key binds for the modules.
     * Note that the custom event fired in the minecraft mixin is used here and not the forge event.
     * That is done to run this code before the vanilla minecraft code.
     */
    @SubscribeEvent
    fun activateModuleKeyBinds(event: PreKeyInputEvent) {
        modules.stream().filter { module -> module.keyCode == event.key }.forEach { module -> module.onKeyBind() }
    }

    /**
     * Handles the key binds for the modules.
     * Note that the custom event fired in the minecraft mixin is used here and not the forge event.
     * That is done to run this code before the vanilla minecraft code.
     */
    @SubscribeEvent
    fun activateModuleMouseBinds(event: PreMouseInputEvent) {
        modules.stream().filter { module -> module.keyCode + 100 == event.button }.forEach { module -> module.onKeyBind() }
    }

    fun getModuleByName(name: String): Module? {
        return modules.find{ it.name.equals(name, ignoreCase = true) }
    }
}