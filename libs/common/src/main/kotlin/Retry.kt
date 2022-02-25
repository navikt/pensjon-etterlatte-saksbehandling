package no.nav.etterlatte.libs.common

import no.nav.etterlatte.libs.common.RetryResult.Failure
import no.nav.etterlatte.libs.common.RetryResult.Success

sealed class RetryResult<T> {
    data class Success<T>(val content: T, val previousExceptions: List<Exception> =emptyList()) : RetryResult<T>()

    data class Failure<T>(val exceptions: List<Exception> =emptyList()) : RetryResult<T>() {
        fun lastError() = exceptions.lastOrNull()
    }
}

suspend fun <T> unsafeRetry(times: Int = 2, block: suspend () -> T) = retry(times, block).let{
    when (it) {
        is Success -> it.content
        is Failure -> throw it.exceptions.last()
    }
}

suspend fun <T> retry(times: Int = 2, block: suspend () -> T) = retryInner(times,emptyList(), block)

private suspend fun <T> retryInner(times: Int, exceptions: List<Exception>, block: suspend () -> T): RetryResult<T> {
    return try {
        Success(block(), exceptions)
    } catch (ex: Exception) {
        if (times < 1) Failure(exceptions + ex)
        else retryInner(times - 1, exceptions + ex, block)
    }
}