package dev.omakasekim.silica

import java.math.BigInteger
import kotlin.math.abs

/**
 * Classification of a finite pattern's long-term behavior under a life-like rule.
 */
sealed class PatternOutcome {
    /** The pattern vanished completely at [generation]. */
    data class Extinct(val generation: Int) : PatternOutcome()

    /** The pattern settled into an unchanging configuration at [onset] with [population] live cells. */
    data class StillLife(val onset: Int, val population: Int) : PatternOutcome()

    /** The pattern repeats in place every [period] generations starting at [onset]. */
    data class Oscillator(val onset: Int, val period: Int, val population: Int) : PatternOutcome()

    /**
     * The pattern repeats every [period] generations starting at [onset], translated by
     * `([dx], [dy])` cells per period.
     */
    data class Spaceship(
        val onset: Int,
        val period: Int,
        val dx: Long,
        val dy: Long,
        val population: Int,
    ) : PatternOutcome() {
        /** Human-readable canonical velocity string such as `c/4 diagonal` or `c/2 orthogonal`. */
        val velocity: String
            get() = formatVelocity(dx, dy, period)
    }

    /** The pattern did not become extinct or strictly periodic within [generations] steps. */
    data class Unsettled(val generations: Int, val population: Long, val boundingBox: Bounds?) : PatternOutcome()
}

/** Result of classifying a pattern under [rule]. */
data class Classification(val rule: Rule, val outcome: PatternOutcome)

/**
 * Exact, translation-invariant pattern dynamics classifier backed by [HashLife].
 *
 * Stepping on [HashLife]'s unbounded plane ensures moving spaceships never collide with a grid
 * boundary. Canonical bounding-box normalization with exact cell-coordinate equality guarantees
 * zero false positives from hash collisions.
 */
object PatternClassifier {
    const val DEFAULT_MAX_GENERATIONS: Int = 2_000
    const val DEFAULT_MAX_CELLS: Int = 50_000

    private data class Snapshot(val generation: Int, val minX: Long, val minY: Long)

    private class NormalizedShape(val width: Long, val height: Long, val cells: LongArray) {
        private val cachedHash: Int = run {
            var h = 31 * width.hashCode() + height.hashCode()
            h = 31 * h + cells.contentHashCode()
            h
        }

        override fun equals(other: Any?): Boolean =
            other is NormalizedShape &&
                width == other.width &&
                height == other.height &&
                cells.contentEquals(other.cells)

        override fun hashCode(): Int = cachedHash
    }

    /**
     * Classifies [cells] under [rule] by stepping up to [maxGenerations] generations.
     *
     * Rejects `B0` rules (which [HashLife] cannot represent on an unbounded plane), negative
     * [maxGenerations], and non-positive [maxCells].
     */
    @JvmOverloads
    fun classify(
        cells: Collection<Pair<Int, Int>>,
        rule: Rule = Rule.CONWAY,
        maxGenerations: Int = DEFAULT_MAX_GENERATIONS,
        maxCells: Int = DEFAULT_MAX_CELLS,
    ): Classification {
        require(maxGenerations >= 0) { "maxGenerations must be non-negative: $maxGenerations" }
        require(maxCells > 0) { "maxCells must be positive: $maxCells" }
        val hash = HashLife(rule)
        for ((x, y) in cells) hash[x.toLong(), y.toLong()] = true

        val seen = HashMap<NormalizedShape, Snapshot>()
        for (gen in 0..maxGenerations) {
            if (hash.exactPopulation == BigInteger.ZERO) {
                return Classification(rule, PatternOutcome.Extinct(gen))
            }
            if (hash.exactPopulation <= BigInteger.valueOf(maxCells.toLong())) {
                val box = hash.boundingBox()
                if (box != null && fitsInLong(box)) {
                    val minX = box.minX.longValueExact()
                    val minY = box.minY.longValueExact()
                    val width = box.maxX.subtract(box.minX).longValueExact() + 1L
                    val height = box.maxY.subtract(box.minY).longValueExact() + 1L
                    if (width <= 0x7FFF_FFFFL && height <= 0x7FFF_FFFFL) {
                        val live = hash.cells(maxCells)
                        val packed = LongArray(live.size)
                        for (i in live.indices) {
                            val (cx, cy) = live[i]
                            packed[i] = ((cy - minY) shl 32) or ((cx - minX) and 0xFFFF_FFFFL)
                        }
                        packed.sort()
                        val shape = NormalizedShape(width, height, packed)
                        val prev = seen[shape]
                        if (prev != null) {
                            val period = gen - prev.generation
                            val dx = minX - prev.minX
                            val dy = minY - prev.minY
                            val outcome = when {
                                dx == 0L && dy == 0L && period == 1 -> PatternOutcome.StillLife(prev.generation, live.size)
                                dx == 0L && dy == 0L -> PatternOutcome.Oscillator(prev.generation, period, live.size)
                                else -> PatternOutcome.Spaceship(prev.generation, period, dx, dy, live.size)
                            }
                            return Classification(rule, outcome)
                        }
                        seen[shape] = Snapshot(gen, minX, minY)
                    }
                }
            }
            if (gen < maxGenerations) {
                hash.advance(BigInteger.ONE)
            }
        }
        return Classification(rule, PatternOutcome.Unsettled(maxGenerations, hash.population, hash.boundingBox()))
    }

    /**
     * Parses [rle] and classifies it under [ruleOverride] (or the RLE header's rule, or Conway's).
     */
    @JvmOverloads
    fun classifyRle(
        rle: String,
        ruleOverride: Rule? = null,
        maxGenerations: Int = DEFAULT_MAX_GENERATIONS,
        maxCells: Int = DEFAULT_MAX_CELLS,
    ): Classification {
        val parsed = Rle.read(rle)
        val rule = ruleOverride ?: parsed.rule ?: Rule.CONWAY
        return classify(parsed.cells, rule, maxGenerations, maxCells)
    }

    private fun fitsInLong(box: Bounds): Boolean =
        box.minX.bitLength() <= 62 &&
            box.minY.bitLength() <= 62 &&
            box.maxX.bitLength() <= 62 &&
            box.maxY.bitLength() <= 62
}

private fun formatVelocity(dx: Long, dy: Long, period: Int): String {
    val adx = abs(dx)
    val ady = abs(dy)
    val p = period.toLong()
    return when {
        adx == 0L || ady == 0L -> {
            val dist = maxOf(adx, ady)
            val g = gcd(dist, p)
            val num = dist / g
            val den = p / g
            val ratio = if (num == 1L) "c/$den" else "${num}c/$den"
            "$ratio orthogonal"
        }
        adx == ady -> {
            val g = gcd(adx, p)
            val num = adx / g
            val den = p / g
            val ratio = if (num == 1L) "c/$den" else "${num}c/$den"
            "$ratio diagonal"
        }
        else -> "($dx,$dy)/${period}c oblique"
    }
}

private fun gcd(a: Long, b: Long): Long {
    var x = abs(a)
    var y = abs(b)
    while (y != 0L) {
        val t = x % y
        x = y
        y = t
    }
    return if (x == 0L) 1L else x
}
