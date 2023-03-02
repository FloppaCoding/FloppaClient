package floppaclient.module

import floppaclient.FloppaClient.Companion.MODULES_PATH
import floppaclient.events.PreKeyInputEvent
import floppaclient.events.PreMouseInputEvent
import floppaclient.module.ModuleManager.modules
import floppaclient.module.impl.dungeon.*
import floppaclient.module.impl.keybinds.AddKeybind
import floppaclient.module.impl.keybinds.KeyBind
import floppaclient.module.impl.misc.*
import floppaclient.module.impl.player.*
import floppaclient.module.impl.render.*
import floppaclient.module.settings.Setting
import floppaclient.tweaker.FloppaClientTweaker
import floppaclient.ui.clickgui.ClickGUI
import floppaclient.ui.hud.EditHudGUI
import net.minecraft.launchwrapper.LaunchClassLoader
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.net.URL
import java.util.*
import java.util.jar.JarEntry
import java.util.jar.JarFile
import kotlin.reflect.KClass
import kotlin.reflect.full.staticProperties


/**
 * # This object handles all the modules of the mod.
 *
 * After making a [Module] it just has to be added to the [modules] list and
 * everything else will be taken care of automatically. This entails:
 *
 * + It will be added to the click gui in the order it is put in here. But keep in mind that the category is set within
 * the module. The comments here are only for readability.
 *
 * + All settings that are registered within the module will be saved to and loaded from the module config.
 * For this to properly work remember to register the settings to the module.
 *
 * + The module will be registered and unregistered to the forge eventbus when it is enabled / disabled.
 *
 * + The module will be informed of its keybind presses.
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
     * Loads in all modules and their elements.
     *
     * Self registering modules are loaded by this.
     * Self registering Hud elements will also be loaded.
     *
     * This method also accesses instances of all modules and their hud elements.
     * That way all module instances are created and loaded into memory.
     *
     * This step is required before the config is loaded.
     */
    fun loadModules() {
        val externalModules = loadExternalModules()

        modules.addAll(externalModules)

        modules.forEach { it.loadModule() }
    }

    /**
     * Initialize the Modules.
     * This is run on game startup during the FMLInitializationEvent.
     */
    fun initializeModules() {
        modules.forEach {
            it.initializeModule()
            EditHudGUI.addHUDElements(it.hudElements)
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

    /**
     * Loads all classes from all jars in [MODULES_PATH].
     *
     * From those classes all valid Module Instances get extracted and returned as a list for registering.
     *
     * Valid Modules Instances must either be kotlin objects annotated with [SelfRegisterModule].
     *
     *     @SelfRegisterModule
     *     object ExternalModule : Module("External Module", category = Category.MISC) {
     *          // Module code
     *     }
     * Or java classes with that annotation which have a public static field called **INSTANCE** which is initialized as
     * an instance of the module class.
     *
     *     @SelfRegisterModule
     *     public class ExternalJavaModule extends Module {
     *         @NotNull
     *         public static final ExternalJavaModule INSTANCE = new ExternalJavaModule();
     *
     *         private ExternalJavaModule() {
     *             super("External Java Module", Category.MISC, "An external Java Module");
     *         }
     *         // Module code
     *     }
     */
    private fun loadExternalModules(): List<Module> {
        val loadedClasses = mutableSetOf<KClass<*>>()
        MODULES_PATH.walk().filter {
            it.isFile && it.extension == "jar"
        }.forEach {
            val pathToJar = it

            val jarFile = JarFile(pathToJar)
            val entries: Enumeration<JarEntry> = jarFile.entries()

            val url = URL("jar:file:$pathToJar!/")
            val urlClassLoader: LaunchClassLoader = FloppaClientTweaker.launchClassLoader
            urlClassLoader.addURL(url)

            while (entries.hasMoreElements()) {
                val jarEntry: JarEntry = entries.nextElement()
                if (jarEntry.isDirectory || !jarEntry.name.endsWith(".class")) {
                    continue
                }
                // -6 because of .class
                var className: String = jarEntry.name.substring(0, jarEntry.name.length - 6)
                className = className.replace('/', '.')
                val loadedClass: KClass<*> = urlClassLoader.loadClass(className).kotlin
                loadedClasses.add(loadedClass)
            }
        }

        // For the annotation check the Java reflection is used here.
        // The Kotlin version (it.hasAnnotation<SelfRegisterModule>()) is less robust and will throw an exception for
        // synthetic Classes generated by the Kotlin compiler.
        return loadedClasses.filterIsInstance<KClass<Module>>()
            .filter {  it.java.isAnnotationPresent(SelfRegisterModule::class.java) }
            .mapNotNull { module ->  module.objectInstance ?:
            (module.staticProperties.find { it.name == "INSTANCE" }?.get() as? Module  )}
            .filter { !modules.contains(it) }
    }
}