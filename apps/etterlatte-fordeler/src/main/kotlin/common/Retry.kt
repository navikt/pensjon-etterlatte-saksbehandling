package no.nav.etterlatte.common

import no.nav.etterlatte.common.RetryResult.Failure
import no.nav.etterlatte.common.RetryResult.Success

sealed class RetryResult() {
    data class Success(val content: Any? = null, val previousExceptions: List<Exception> = emptyList()) : RetryResult()

    data class Failure(val exceptions: List<Exception> = emptyList()) : RetryResult() {
        fun lastError() = exceptions.lastOrNull()
    }
}

suspend fun <T> unsafeRetry(times: Int = 2, block: suspend () -> T) = retry(times, block).let {
    when (it) {
        is Success -> it.content
        is Failure -> throw it.exceptions.last()
    }
}

suspend fun <T> retry(times: Int = 2, block: suspend () -> T) = retryInner(times, emptyList(), block)

private suspend fun <T> retryInner(times: Int, exceptions: List<Exception>, block: suspend () -> T): RetryResult {
    return try {
        Success(block(), exceptions)
    } catch (ex: Exception) {
        if (times < 1) Failure(exceptions + ex)
        else retryInner(times - 1, exceptions + ex, block)
    }
}
