package floppaclient.module

/**
 * [Module] classes with this annotation will automatically get loaded on game launch.
 * They do not have to be within [ModuleManager.modules].
 * @author Aton
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class SelfRegisterModule
