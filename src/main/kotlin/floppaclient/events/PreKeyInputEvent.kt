package floppaclient.events

import net.minecraftforge.fml.common.eventhandler.Event

/**
 * This event is mixed in to fire before the vanilla key binds are evaluated.
 * The forge event fires after those.
 */
class PreKeyInputEvent constructor(
    val key: Int,
    val character: Char
) : Event() {
}