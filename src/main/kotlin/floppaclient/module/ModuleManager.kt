package floppaclient.module

import floppaclient.FloppaClient
import floppaclient.module.impl.keybinds.AddKeybind
import floppaclient.events.PreKeyInputEvent
import floppaclient.events.PreMouseInputEvent
import floppaclient.module.impl.dungeon.*
import floppaclient.module.impl.keybinds.KeyBind
import floppaclient.module.impl.misc.*
import floppaclient.module.impl.player.*
import floppaclient.module.impl.render.*
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent


object ModuleManager {

    val modules: ArrayList<Module> = arrayListOf(
        //DUNGEON
        SecretChime,
        SecretAura,
        AutoWeirdos,
        TicTacToeSolver,
        ExtraStats,
        TrapGearSwap,
        AutoLeap,
        MelodyMessage,
        TerminalAura,
        AutoTerms,
        AutoDevices,
        DungeonKillAura,
        IceSprayAura,
        CancelChestOpen,

        //RENDER
        ClickGui,
        EditHud,
        DungeonESP,
        DungeonWarpTimer,
        DungeonMap,
        MapRooms,
        CoordinateDisplay,
        ItemAnimations,
        Camera,

        //PLAYER
        HotbarSwapper,
        ArmorSwapper,
        FreeCam,
        Clip,
        HClip,
        BarPhase,
        NoRotate,
        Velocity,

        //MISC
        QOL,
        AutoSprint,
        AutoRagnarock,
        AutoHarp,
        GhostBlocks,
        RemoveFrontView,
        TerminatorClicker,
        AutoClicker,
        AutoWeaponSwap,
        CancelInteract,

        //KEYBIND
        AddKeybind,
    )

    /**
     * Creates a new keybind module and adds it to the list.
     */
    fun addNewKeybind(): KeyBind {
        val number = (modules
            .filter{module -> module.name.startsWith("New Keybind")}
            .map {module -> module.name.filter { c -> c.isDigit() }.toIntOrNull()}
            .maxByOrNull { it ?: 0} ?: 0) + 1
        val keyBind = KeyBind("New Keybind $number")
        modules.add(keyBind)
        return keyBind
    }

    fun removeKeyBind(bind: KeyBind) {
        modules.remove(bind)
        FloppaClient.clickGUI.setUpPanels()
    }

    /**
     * Handles the key binds for the modules.
     * Note that the custom event fired in the minecraft mixin is used here and not the forge event.
     * That is done to run this code before the vanilla minecraft code.
     */
    @SubscribeEvent
    fun activateModuleKeyBinds(event: PreKeyInputEvent) {
        modules.stream().filter { module -> module.keyCode == event.key }.forEach { module -> module.keyBind() }
    }

    /**
     * Handles the key binds for the modules.
     * Note that the custom event fired in the minecraft mixin is used here and not the forge event.
     * That is done to run this code before the vanilla minecraft code.
     */
    @SubscribeEvent
    fun activateModuleMouseBinds(event: PreMouseInputEvent) {
        modules.stream().filter { module -> module.keyCode + 100 == event.button }.forEach { module -> module.keyBind() }
    }

    fun getModuleByName(name: String): Module? {
        return modules.stream().filter{module -> module.name.equals(name, ignoreCase = true)}.findFirst().orElse(null)
    }
}