package dev.omakasekim.silica

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ClassifyTest {
    @Test
    fun classifiesStillLifesAndOscillatorsExactly() {
        val block = PatternClassifier.classifyRle(Patterns.BLOCK)
        assertEquals(PatternOutcome.StillLife(onset = 0, population = 4), block.outcome)

        val blinker = PatternClassifier.classifyRle(Patterns.BLINKER)
        assertEquals(PatternOutcome.Oscillator(onset = 0, period = 2, population = 3), blinker.outcome)

        val toad = PatternClassifier.classifyRle(Patterns.TOAD)
        assertEquals(PatternOutcome.Oscillator(onset = 0, period = 2, population = 6), toad.outcome)

        val beacon = PatternClassifier.classifyRle(Patterns.BEACON)
        val beaconOutcome = assertIs<PatternOutcome.Oscillator>(beacon.outcome)
        assertEquals(0, beaconOutcome.onset)
        assertEquals(2, beaconOutcome.period)

        val pulsar = PatternClassifier.classifyRle(Patterns.PULSAR)
        assertEquals(PatternOutcome.Oscillator(onset = 0, period = 3, population = 48), pulsar.outcome)

        val pentadecathlon = PatternClassifier.classifyRle(Patterns.PENTADECATHLON)
        val pentaOutcome = assertIs<PatternOutcome.Oscillator>(pentadecathlon.outcome)
        assertEquals(0, pentaOutcome.onset)
        assertEquals(15, pentaOutcome.period)
    }

    @Test
    fun classifiesDiagonalAndOrthogonalSpaceshipsWithExactVelocity() {
        val glider = PatternClassifier.classifyRle(Patterns.GLIDER)
        val g = assertIs<PatternOutcome.Spaceship>(glider.outcome)
        assertEquals(0, g.onset)
        assertEquals(4, g.period)
        assertEquals(1L, g.dx)
        assertEquals(1L, g.dy)
        assertEquals(5, g.population)
        assertEquals("c/4 diagonal", g.velocity)

        val lwss = PatternClassifier.classifyRle(Patterns.LWSS)
        val ship = assertIs<PatternOutcome.Spaceship>(lwss.outcome)
        assertEquals(0, ship.onset)
        assertEquals(4, ship.period)
        assertEquals(-2L, ship.dx)
        assertEquals(0L, ship.dy)
        assertEquals(9, ship.population)
        assertEquals("c/2 orthogonal", ship.velocity)
    }

    @Test
    fun detectsExtinctionAndOnsetDelay() {
        val diehard = PatternClassifier.classifyRle(Patterns.DIEHARD)
        assertEquals(PatternOutcome.Extinct(generation = 130), diehard.outcome)

        val empty = PatternClassifier.classify(emptyList())
        assertEquals(PatternOutcome.Extinct(generation = 0), empty.outcome)

        // Pre-block (3 cells in an L) stabilizes into a 4-cell block at generation 1.
        val preBlock = PatternClassifier.classify(listOf(0 to 0, 1 to 0, 0 to 1))
        assertEquals(PatternOutcome.StillLife(onset = 1, population = 4), preBlock.outcome)
    }

    @Test
    fun reportsUnsettledForGrowingPatternsAndSupportsNonConwayRules() {
        val gosper = PatternClassifier.classifyRle(Patterns.GOSPER_GUN, maxGenerations = 60)
        val unsettled = assertIs<PatternOutcome.Unsettled>(gosper.outcome)
        assertEquals(60, unsettled.generations)
        assertTrue(unsettled.population > 36L)

        // HighLife (B36/S23) replicator grows rather than settling in 40 gens.
        val replicator = PatternClassifier.classifyRle(Patterns.REPLICATOR, ruleOverride = Rule.parse("B36/S23"), maxGenerations = 40)
        assertEquals(Rule.parse("B36/S23"), replicator.rule)
        assertIs<PatternOutcome.Unsettled>(replicator.outcome)

        // Glider is still a c/4 diagonal spaceship under HighLife (B36/S23).
        val highLifeGlider = PatternClassifier.classifyRle(Patterns.GLIDER, ruleOverride = Rule.parse("B36/S23"))
        val hg = assertIs<PatternOutcome.Spaceship>(highLifeGlider.outcome)
        assertEquals(4, hg.period)
        assertEquals("c/4 diagonal", hg.velocity)
    }

    @Test
    fun rejectsB0RulesAndInvalidBounds() {
        assertFailsWith<IllegalArgumentException> {
            PatternClassifier.classifyRle(Patterns.GLIDER, ruleOverride = Rule.parse("B0/S8"))
        }
        assertFailsWith<IllegalArgumentException> {
            PatternClassifier.classifyRle(Patterns.GLIDER, maxGenerations = -1)
        }
        assertFailsWith<IllegalArgumentException> {
            PatternClassifier.classifyRle(Patterns.GLIDER, maxCells = 0)
        }
    }
}
