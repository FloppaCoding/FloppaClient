package floppaclient.module

import floppaclient.events.PreKeyInputEvent
import floppaclient.events.PreMouseInputEvent
import floppaclient.module.impl.dungeon.*
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
        ExtraStats,
        MelodyMessage,
        TerminalAura,
        DungeonKillAura,
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
        JerryRocket,
        FreeCam,
        Clip,
        NoRotate,
        Velocity,

        //MISC
        QOL,
        AutoSprint,
        AutoRagnarock,
        GhostBlocks,
        RemoveFrontView,
        TerminatorClicker,
        AutoClicker,
        AutoWeaponSwap,
        CancelInteract,
    )

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