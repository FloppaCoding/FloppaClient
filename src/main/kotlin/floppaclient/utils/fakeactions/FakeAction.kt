package floppaclient.utils.fakeactions

data class FakeAction(
    var fakeYaw: Float = 0f,
    var fakePitch: Float = 0f,

    // right click if true left click otherwise
    var rightClick:Boolean = true,

    var itemSlot: Int = 0,

    var forceSneak: Boolean = false,
    /**
     * Determines whether the item should be swung client side on left click.
     */
    var swingItem: Boolean = true,
    /**
     * Will be run right after the fake action
     */
    var extraActionFun: () -> Unit = {}
){
    /**
     * Additional action that will be performed after the fake action.
     */
    fun extraAction(){
        extraActionFun()
    }
}