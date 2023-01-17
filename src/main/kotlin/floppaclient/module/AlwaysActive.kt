package floppaclient.module

/**
 * [Module] classes with this annotation will be registered to the Event Bus on game start up and will not be
 * unregistered on disable.
 *
 * @author Aton
 */
@Target(AnnotationTarget.CLASS)
annotation class AlwaysActive
