package com.example.appbike

import kotlinx.coroutines.CancellationException

/**
 * Equivalent to [runCatching] for suspend work, except structured-concurrency
 * cancellation is never converted into a visible network failure.
 */
internal suspend inline fun <T> runSuspendCatching(
    crossinline block: suspend () -> T
): Result<T> = try {
    Result.success(block())
} catch (cancellation: CancellationException) {
    throw cancellation
} catch (error: Throwable) {
    Result.failure(error)
}
