package floppaclient.floppamap.core

enum class RoomState {
    /** White Checked */
    CLEARED,
    DISCOVERED,
    FAILED,
    /** Green Checked */
    GREEN,
    UNDISCOVERED,
    /** Used for the question mark rooms */
    QUESTION_MARK;

    val revealed: Boolean
        get() = this != UNDISCOVERED && this != QUESTION_MARK

}
