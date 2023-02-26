package floppaclient.module

/**
 * Annotate your [HUDElements][floppaclient.ui.hud.HudElement] with this to register them.
 *
 * This annotation tells the [ModuleManager] to take care of registering the hud element for you.
 * It only works for objects inheriting from [HudElement][floppaclient.ui.hud.HudElement] declared within your module.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class RegisterHudElement()
