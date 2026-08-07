package com.example.appbike

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CoroutineResultTest {
    @Test
    fun regularFailureIsReturned() = runBlocking {
        val result = runSuspendCatching<Int> { error("network") }

        assertTrue(result.isFailure)
        assertEquals("network", result.exceptionOrNull()?.message)
    }

    @Test(expected = CancellationException::class)
    fun cancellationIsRethrown() {
        runBlocking {
            runSuspendCatching<Int> { throw CancellationException("screen changed") }
        }
    }
}
