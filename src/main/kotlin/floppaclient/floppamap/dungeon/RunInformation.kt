package floppaclient.floppamap.dungeon

import floppaclient.utils.ScoreboardUtils
import floppaclient.utils.Utils.equalsOneOf
import net.minecraft.client.network.NetworkPlayerInfo
import kotlin.math.floor

/**
 * Keeps track of dungeon run information.
 *
 * @author Aton
 */
object RunInformation {

    // TODO Mimic, paul, spirit pet and speed score are just assumed so far.

    private val deathsPattern = Regex("§r§a§lDeaths: §r§f\\((?<deaths>\\d+)\\)§r")
    private val puzzleCountPattern = Regex("§r§b§lPuzzles: §r§f\\((?<count>\\d)\\)§r")

    /**
     * Matches all three states a puzzle can be in (unfinished, completed, failed).
     */
    private val puzzlePattern = Regex("§r (?<puzzle>.+): §r§7\\[§r§[ac6]§l(?<state>[✔✖✦])§r§7].+")
    private val failedPuzzlePattern = Regex("§r (?<puzzle>.+): §r§7\\[§r§c§l✖§r§7] §.+")
    private val solvedPuzzlePattern = Regex("§r (?<puzzle>.+): §r§7\\[§r§a§l✔§r§7] §.+")
    private val secretsFoundPattern = Regex("§r Secrets Found: §r§b(?<secrets>\\d+)§r")
    private val secretsFoundPercentagePattern = Regex("§r Secrets Found: §r§[ae](?<percentage>[\\d.]+)%§r")
    private val cryptsPattern = Regex("§r Crypts: §r§6(?<crypts>\\d+)§r")
    private val dungeonClearedPattern = Regex("Cleared: (?<percentage>\\d+)% \\(\\d+\\)")
    private val timeElapsedPattern = Regex(" Elapsed: (?:(?<hrs>\\d+)h )?(?:(?<min>\\d+)m )?(?:(?<sec>\\d+)s)?")

    var deathCount = 0
    var secretCount = 0
    var cryptsCount = 0
    var secretPercentage = 0.0
    var totalSecrets: Int? = null
    var clearedPercentage = 0
    var secondsElapsed = 0

    /**
     * List of Puzzle name with completion state: true -> solved, false -> failed, null -> not finished.
     */
    val puzzles = mutableListOf<Pair<String, Boolean?>>()
    var uncompletedPuzzles = 0
    var currentFloor: Floor? = null

    var score = 0

    /**
     * Updates the run information from the tab entries provided and from the tab list.
     * Should be run on some loop.
     */
    fun updateRunInformation(tabEntries: List<Pair<NetworkPlayerInfo, String>>) {
        updateFromScoreboard()

        /** Used to determine whether the current line is a puzzle */
        var readingPuzzles = false
        var tempUncompletedPuzz = 0
        tabEntries.forEach { pair ->
            val text = pair.second
            when {
                readingPuzzles -> {
                    val matcher = puzzlePattern.find(text) ?: return@forEach Unit.also { readingPuzzles = false }
                    matcher.groups["puzzle"]?.value?.let { name ->
                        val state: Boolean? = when (matcher.groups["state"]?.value) {
                            "✔" -> true
                            "✖" -> false
                            else -> null
                        }
                        if (state != true) tempUncompletedPuzz ++
                        if (!name.contains("???")) puzzles.add(Pair(name,state))
                    }
                }
                text.contains("Deaths: ") -> {
                    val matcher = deathsPattern.find(text) ?: return@forEach
                    deathCount = matcher.groups["deaths"]?.value?.toIntOrNull() ?: deathCount
                }
                text.contains("Secrets Found: ") -> {
                    if (text.contains("%")) {
                        val matcher = secretsFoundPercentagePattern.find(text) ?: return@forEach
                        secretPercentage = (matcher.groups["percentage"]?.value?.toDoubleOrNull() ?: 0.0)
                        totalSecrets = if (secretCount > 0 && secretPercentage > 0) floor(100f / secretPercentage * secretCount + 0.5).toInt() else null
                    } else {
                        val matcher = secretsFoundPattern.find(text) ?: return@forEach
                        secretCount = matcher.groups["secrets"]?.value?.toIntOrNull() ?: secretCount
                    }
                    val matcher = secretsFoundPattern.find(text) ?: return@forEach
                    secretCount = matcher.groups["secrets"]?.value?.toIntOrNull() ?: secretCount
                }
                text.contains("Crypts: ") -> {
                    val matcher = cryptsPattern.find(text) ?: return@forEach
                    cryptsCount = matcher.groups["crypts"]?.value?.toIntOrNull() ?: cryptsCount
                }
                text.contains("Puzzles: ") -> {
                    readingPuzzles = true
                    puzzles.clear()
                }
            }
        }
        uncompletedPuzzles = tempUncompletedPuzz

        score = skillScore + exploreScore + speedScore + bonusScore
    }

    private fun updateFromScoreboard(){
        ScoreboardUtils.sidebarLines.forEach {
            val line = ScoreboardUtils.cleanSB(it)
            when {
                line.startsWith("Cleared: ") -> {
                    val matcher = dungeonClearedPattern.find(line)
                    if (matcher != null) {
                        clearedPercentage = matcher.groups["percentage"]?.value?.toIntOrNull() ?: 0
                    }
                }
                line.startsWith("Time Elapsed:") -> {
                    val matcher = timeElapsedPattern.find(line)
                    if (matcher != null) {
                        val hours = matcher.groups["hrs"]?.value?.toIntOrNull() ?: 0
                        val minutes = matcher.groups["min"]?.value?.toIntOrNull() ?: 0
                        val seconds = matcher.groups["sec"]?.value?.toIntOrNull() ?: 0
                        secondsElapsed = (hours * 3600 + minutes * 60 + seconds)
                    }
                }
                currentFloor == null && line.contains("The Catacombs (") -> {
                    currentFloor = try {
                        Floor.valueOf(line.substringAfter("(").substringBefore(")"))
                    }catch (_ : IllegalArgumentException) { null }
                }
            }
        }
    }

    fun reset() {
        deathCount = 0
        secretCount = 0
        cryptsCount = 0
        secretPercentage = 0.0
        totalSecrets = null
        clearedPercentage = 0
        secondsElapsed = 0
        puzzles.clear()
        uncompletedPuzzles = 0
        currentFloor = null
        score = 0
    }

    private val skillScore: Int
        get() {
            // 20 + floor( 80 * clearRatio ) - 10 * uncompletedPuzzles - (2*Deaths - 1).coerceAtLeast(0)
            val cleared = (clearedPercentage + if(Dungeon.inBoss) 0 else 5).coerceAtMost(100)
            return  20 + (
                    (0.8 * cleared).toInt()
                            - 10 * uncompletedPuzzles
                            - (2* deathCount - 1).coerceAtLeast(0)
                    ).coerceAtLeast(0)
        }

    private val exploreScore: Int
        get() {
            // floor( 60 * clearRatio ) + floor( 40 * (secretFound / secretNeeded).coerceAtMost(1) )
            val cleared = (clearedPercentage + if(Dungeon.inBoss) 0 else 5).coerceAtMost(100)
            return (0.6 * cleared).toInt() + (
                    40 * (secretPercentage / (requiredSecretPercentage[currentFloor] ?: 100.0)).coerceAtMost(1.0)).toInt()
        }

    /**
     * Is just set to 100 for now, anyway really hard to be slower than that.
     */
    private const val speedScore: Int = 100

    /**
     * Assumes mimic, does not assume paul.
     */
    val bonusScore: Int
        get() {
            return (cryptsCount.coerceAtMost(5)
                    + if (currentFloor.equalsOneOf(Floor.F6, Floor.F7, Floor.M6, Floor.M7)) 2 else 0
                    )
        }

    private val requiredSecretPercentage: Map<Floor, Double> = mapOf(
        Floor.E  to 30.0,
        Floor.F1 to 30.0,
        Floor.F2 to 40.0,
        Floor.F3 to 50.0,
        Floor.F4 to 60.0,
        Floor.F5 to 70.0,
        Floor.F6 to 85.0,
        Floor.F7 to 100.0,
        Floor.M1 to 100.0,
        Floor.M2 to 100.0,
        Floor.M3 to 100.0,
        Floor.M4 to 100.0,
        Floor.M5 to 100.0,
        Floor.M6 to 100.0,
        Floor.M7 to 100.0,
    )

    private val timeLimit: Map<Floor, Int> = mapOf(
        Floor.E  to 600,
        Floor.F1 to 600,
        Floor.F2 to 600,
        Floor.F3 to 600,
        Floor.F4 to 720,
        Floor.F5 to 600,
        Floor.F6 to 720,
        Floor.F7 to 840,
        Floor.M1 to 480,
        Floor.M2 to 480,
        Floor.M3 to 480,
        Floor.M4 to 480,
        Floor.M5 to 480,
        Floor.M6 to 480,
        Floor.M7 to 900,
    )

    enum class Floor{
        E, F1, F2, F3, F4, F5, F6, F7, M1, M2, M3, M4, M5, M6, M7
    }
}
