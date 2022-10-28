package floppaclient.funnymap.core

import net.minecraft.util.BlockPos

data class ExtrasData(
    // Room core, or the first in room list if there are multiple
    val baseCore: Int,
    val preBlocks: MutableMap<Int, MutableSet<BlockPos>> = mutableMapOf()
)
