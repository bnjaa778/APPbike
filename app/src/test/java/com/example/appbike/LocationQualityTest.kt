package com.example.appbike

import org.junit.Assert.assertTrue
import org.junit.Test

class LocationQualityTest {
    @Test
    fun accurateFixWinsOverNewerCoarseFix() {
        val accurate = LocationQuality(accuracyMeters = 8f, timestampMillis = 1_000L)
        val newerButCoarse = LocationQuality(accuracyMeters = 120f, timestampMillis = 2_000L)

        assertTrue(compareLocationQuality(accurate, newerButCoarse) < 0)
    }

    @Test
    fun newerFixWinsWhenAccuracyMatches() {
        val older = LocationQuality(accuracyMeters = 12f, timestampMillis = 1_000L)
        val newer = LocationQuality(accuracyMeters = 12f, timestampMillis = 2_000L)

        assertTrue(compareLocationQuality(newer, older) < 0)
    }

    @Test
    fun measuredAccuracyWinsOverMissingAccuracy() {
        val measured = LocationQuality(accuracyMeters = 60f, timestampMillis = 1_000L)
        val unknown = LocationQuality(accuracyMeters = null, timestampMillis = 2_000L)

        assertTrue(compareLocationQuality(measured, unknown) < 0)
    }
}
