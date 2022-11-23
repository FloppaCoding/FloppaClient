package floppaclient.funnymap.core

data class RotationData(
    // Room core, or the first in room list if there are multiple
    val baseCore: Int,
    // All rotations cores up to coreDistance
    val rotationCores: MutableList<Int>,
    // Number of blocks the rotationCore is from the baseCore
    val coreDistance: Int = 2,
)
