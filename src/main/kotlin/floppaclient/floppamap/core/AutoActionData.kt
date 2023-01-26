package floppaclient.floppamap.core

import net.minecraft.util.BlockPos

data class AutoActionData(
    // Room core, or the first in room list if there are multiple
    val baseCore: Int,
    // Map of Clip start position to clip route
    val clips: MutableMap<MutableList<Int>, MutableList<Double>> = mutableMapOf(),
    // Map of Auto Ether start position to target
    val etherwarps: MutableMap<MutableList<Int>, BlockPos> = mutableMapOf(),
)
