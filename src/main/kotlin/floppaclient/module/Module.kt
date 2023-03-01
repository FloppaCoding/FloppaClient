package floppaclient.module

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import floppaclient.FloppaClient
import floppaclient.module.settings.Setting
import floppaclient.ui.hud.HudElement
import floppaclient.utils.ChatUtils
import net.minecraftforge.common.MinecraftForge
import org.lwjgl.input.Keyboard
import kotlin.reflect.full.hasAnnotation

/**
 * # Super class for all Modules in the mod.
 *
 * Annotate with [AlwaysActive] to have a Module always registered to the Event Bus regardless of the Module being [enabled].
 *
 * ## Making your own Module
 * To make your own Module simply create an object which inherits from this class and add it to the list of
 * [modules][ModuleManager.modules] in [ModuleManager].
 * The sample provided below shows how you can do this.
 *
 * ## Adding settings to your Module.
 * If you want your Module to have Settings which appear in the GUI you need to define the settings in your Module and
 * also register them by adding them to the [settings] list.
 *
 * There are four different ways to achieve this.
 * These are shown in the following on the example of a [NumberSetting][floppaclient.module.settings.impl.NumberSetting].
 * Probably the easiest to understand is to first define a property in your Module which is an Instance of the
 * setting you want. And then registering that setting in the initializer.
 *
 *     private val distance : NumberSetting = NumberSetting("Distance", 4.0)
 *
 *     init{
 *         this.addSettings(distance)
 *     }
 *
 * This syntax can be shortened by using the [register] method directly when defining your setting.
 * Inside of Module classes the unary Add operator is overridden for members of [Setting] to register them.
 * So the following two lines will do exactly the same.
 *
 *     private val distance : NumberSetting = register(NumberSetting("Distance", 4.0))
 *     private val distance : NumberSetting = +NumberSetting("Distance", 4.0)
 *
 * Lastly the probably most convenient way of adding settings is through the use of delegation.
 * The key difference here is that the property you are defining is not referring to the Setting but rather directly
 * to its [value][Setting.value].
 * Since often the value is the only relevant aspect of the setting within the Modules code
 * this approach is a lot more convenient to use in most cases.
 * Notice how the type of *distance* is Double in the following example and not NumberSetting.
 *
 *     private val distance : Double by NumberSetting("Distance", 4.0)
 *
 * ### Adding more functionality to your Settings.
 * The setting classes offer support for more functionality, namely adding a dependency for the setting to show in the GUI
 * and the option to perform custom processing of changes to the setting.
 * The sample from [AutoClicker][floppaclient.module.impl.misc.AutoClicker] below shows both of these being used.
 * The input transform is used to further limit the range in which the slider can be moved.
 * And the dependency is set so that the setting only shows in the click gui when the corresponding click mode is enabled.
 *
 * ## Adding a HUD to your Module
 * To add a HUD to your module simply declare an object which inherits from the [HudElement] class within your Module.
 * For this HUD element to be active you need to register it. All you need to do for that is to annotate it with the
 * [RegisterHudElement] annotation.
 * Inside your HUD element object you need to implement the [renderHud][HudElement.renderHud] method.
 * This ís responsible for rendering the element.
 *
 * The following example shows how to use it.
 *
 *     object MyModule : Module("My Module", category = Category.RENDER) {
 *         @RegisterHudElement
 *         object MyHudElement : HudElement(this, 0, 150, 100, 20) {
 *             override fun renderHud() {
 *                 // Rendering implementation
 *             }
 *         }
 *     }
 *
 *
 * @author Aton
 * @see ModuleManager
 * @param name The name of the Module. **This has to be unique!** This name is shown in the GUI and used to identify the module in the config.
 * @param keyCode Key code for the Modules key-bind.
 * @param category Determines in which category Panel the module will appear in the GUI.
 * @param description A description of the module and its usage that is shown in the [Advanced GUI][floppaclient.ui.clickgui.advanced.AdvancedMenu].
 * @sample floppaclient.module.impl.misc.AutoClicker.maxCps
 * @sample floppaclient.module.impl.misc.AutoSprint
 */
abstract class Module(
    name: String,
    keyCode: Int = Keyboard.KEY_NONE,
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
    val description: String

    val hudElements: MutableList<HudElement> = mutableListOf()

    init {
        this.name = name
        this.keyCode = keyCode
        this.category = category
        this.settings = settings
        this.description = description
    }

    /**
     * A simplified constructor to inherit from.
     */
    constructor(
        name: String,
        category: Category = Category.MISC,
        description: String = ""
    ) : this(name, 0,  category =  category, description =  description)

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
     * Loads self registering elements of the module such as hud elements.
     */
    fun loadModule() {
        this::class.nestedClasses.filter { it.hasAnnotation<RegisterHudElement>() }
            .mapNotNull { it.objectInstance }.filterIsInstance<HudElement>().forEach {
                hudElements.add(it)
            }
    }

    /**
     * Triggers the module initialization.
     *
     * Is run on startup.
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
        hudElements.forEach { MinecraftForge.EVENT_BUS.register(it) }
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
        hudElements.forEach { MinecraftForge.EVENT_BUS.unregister(it) }
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
     * Adds all settings in the input to the [settings] field of the module.
     * This is required for saving and loading these settings to / from a file.
     * Keep in mind, that these settings are passed by reference, which will get lost if the original setting is reassigned.
     */
    fun addSettings(vararg setArray: Setting<*>) {
        setArray.forEach {
            settings.add(it)
        }
    }

    /**
     * Registers the setting to this Module.
     * This will make the setting appear in the GUI and save to the config.
     * The following is an example of how it can be used to define a setting for a module.
     *
     *     private val distance = register(NumberSetting("Distance", 4.0, 1.0,10.0,0.1))
     */
    fun <K: Setting<*>> register(setting: K): K{
        addSettings(setting)
        return setting
    }

    /**
     * Overloads the unaryPlus operator for [Setting] classes to register them to the module.
     * The following is an example of how it can be used to define a setting for a module.
     *
     *     private val distance = +NumberSetting("Distance", 4.0, 1.0,10.0,0.1)
     * @see register
     */
    operator fun <K: Setting<*>> K.unaryPlus(): K = register(this)

    fun getSettingByName(name: String): Setting<*>? {
        return settings.find { it.name.equals(name, ignoreCase = true) }
    }
}