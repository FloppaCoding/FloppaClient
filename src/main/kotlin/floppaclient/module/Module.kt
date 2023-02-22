package floppaclient.module

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import floppaclient.FloppaClient
import floppaclient.module.settings.Setting
import floppaclient.utils.ChatUtils
import net.minecraftforge.common.MinecraftForge
import kotlin.reflect.full.hasAnnotation

/**
 * Super class for all Modules in the mod.
 *
 * To make your own Module simply create an object which inherits from this class and add it to the list of
 * [modules][ModuleManager.modules] in [ModuleManager].
 * The sample provided below shows how you can do this
 *
 * Annotate with [AlwaysActive] to have a Module always registered to the Event Bus regardless of the Module being [enabled].
 *
 * @author Aton
 * @see ModuleManager
 * @param name The name of the Module. **This has to be unique!** This name is shown in the GUI and used to identify the module in the config.
 * @param keyCode Key code for the Modules key-bind.
 * @param category Determines in which category Panel the module will appear in the GUI.
 * @param description A description of the module and its usage that is shown in the [Advanced GUI][floppaclient.ui.clickgui.advanced.AdvancedMenu].
 *
 * @sample floppaclient.module.impl.misc.AutoSprint
 */
abstract class Module(
    name: String,
    keyCode: Int = 0,
    category: Category = Category.MISC,
    toggled: Boolean = false,
    settings: ArrayList<Setting<*>> = ArrayList(),
    description: String = ""
){
    @Expose
    @SerializedName("name")
    val name: String

    /**
     * Key code of the corresponding key bind.
     * Mouse binds will be negative: -100 + mouse button.
     * This is the same way as minecraft treats mouse binds.
     */
    @Expose
    @SerializedName("key")
    var keyCode: Int
    val category: Category

    /**
     * Do NOT set this value directly, use [toggle()][toggle] instead!
     */
    @Expose
    @SerializedName("enabled")
    var enabled: Boolean = toggled
        private set
    @Expose
    @SerializedName("settings")
    val settings: ArrayList<Setting<*>>

    /**
     * A description of the module and its usage that is shown in the [Advanced GUI][floppaclient.ui.clickgui.advanced.AdvancedMenu].
     */
    var description: String

    init {
        this.name = name
        this.keyCode = keyCode
        this.category = category
        this.settings = settings
        this.description = description
    }

    /**
     * Triggers the module initialization.
     *
     * Also takes care of registering the module to the Forge [EventBus][MinecraftForge.EVENT_BUS] if it has
     * the [AlwaysActive] annotation.
     */
    fun initializeModule() {
        if (this::class.hasAnnotation<AlwaysActive>()) {
            MinecraftForge.EVENT_BUS.register(this)
        }
        onInitialize()
    }

    /**
     * This method will be run on the FMLLoadCompleteEvent on game startup after the config is loaded.
     *
     * Override it in your implementation to perform additional initialization actions
     * which require certain things like the config to be loaded.
     * In the example below you can see this being used to register the module to run in the background
     * if the corresponding setting is enabled.
     * @see FloppaClient.postInit
     * @sample floppaclient.module.impl.render.DungeonWarpTimer.onInitialize
     */
    open fun onInitialize() {}

    /**
     * This method is run whenever the module is enabled.
     *
     * Its default implementation registers the module to the Forge [EventBus][MinecraftForge.EVENT_BUS].
     *
     * Override it in your implementation to perform extra actions when the module is activated.
     * Keep in mind that you still have to invoke this implementation to register the module to the event bus.
     *
     * The following example shows how you can use this to prevent a module from being enabled in certain conditions and
     * to run some extra code.
     *
     * @sample floppaclient.module.impl.player.FreeCam.onEnable
     */
    open fun onEnable() {
        MinecraftForge.EVENT_BUS.register(this)
    }

    /**
     * This method is run whenever the module is disabled.
     *
     * Its default implementation unregisters the module to the Forge [EventBus][MinecraftForge.EVENT_BUS] unless it has
     * the [AlwaysActive] annotation.
     *
     * Override it in your implementation to change this behaviour or add extra functionality.
     */
    open fun onDisable() {
        //Only allow unregistering the class if it is not set to be always active.
        if (!this::class.hasAnnotation<AlwaysActive>()) {
            MinecraftForge.EVENT_BUS.unregister(this)
        }
    }

    /**
     * This method is run whenever the [key-bind][keyCode] for the Module is pressed.
     *
     * By default, this will toggle the module and send a chat message.
     * It can be overwritten in the module to change that behaviour.
     */
    open fun onKeyBind() {
        this.toggle()
        ChatUtils.modMessage("$name ${if (enabled) "§aenabled" else "§cdisabled"}.")
    }

    /**
     * Will toggle the module.
     *
     * Invokes [onEnable] or [onDisable] accordingly.
     */
    fun toggle() {
        enabled = !enabled
        if (enabled)
            onEnable()
        else
            onDisable()
    }

    /**
     * Adds all settings in the input to the settings field of the module.
     * This is required for saving and loading these settings to / from a file.
     * Keep in mind, that these settings are passed by reference, which will get lost if the original setting is reassigned.
     */
    fun addSettings(setArray: ArrayList<Setting<*>>) {
        setArray.forEach {
            settings.add(it)
        }
    }

    /**
     * Adds all settings in the input to the [settings] field of the module.
     * This is required for saving and loading these settings to / from a file.
     * Keep in mind, that these settings are passed by reference, which will get lost if the original setting is reassigned.
     */
    fun addSettings(vararg setArray: Setting<*>) {
        this.addSettings(ArrayList(setArray.asList()))
    }

    fun getSettingByName(name: String): Setting<*>? {
        for (set in settings) {
            if (set.name.equals(name, ignoreCase = true)) {
                return set
            }
        }
        System.err.println("[" + FloppaClient.MOD_NAME + "] Error Setting NOT found: '" + name + "'!")
        return null
    }
}