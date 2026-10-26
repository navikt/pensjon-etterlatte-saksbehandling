package no.nav.etterlatte.libs.common

import no.nav.etterlatte.libs.common.RetryResult.Failure
import no.nav.etterlatte.libs.common.RetryResult.Success
import no.nav.etterlatte.libs.common.logging.samleExceptions

sealed class RetryResult<T> {
    data class Success<T>(
        val content: T,
        val previousExceptions: List<Exception> = emptyList(),
    ) : RetryResult<T>()

    data class Failure<T>(
        val exceptions: List<Exception> = emptyList(),
    ) : RetryResult<T>() {
        fun samlaExceptions(): Exception = samleExceptions(this.exceptions)
    }
}

suspend fun <T> retryOgPakkUt(
    times: Int = 2,
    vent: (timesLeft: Int) -> Unit = {},
    block: suspend () -> T,
) = retry(times, vent, block).let {
    when (it) {
        is Success -> it.content
        is Failure -> throw it.samlaExceptions()
    }
}

suspend fun <T> retry(
    times: Int = 2,
    vent: (timesLeft: Int) -> Unit = {},
    block: suspend () -> T,
) = retryInner(times, vent, emptyList(), block)

private suspend fun <T> retryInner(
    times: Int,
    vent: (timesLeft: Int) -> Unit,
    exceptions: List<Exception>,
    block: suspend () -> T,
): RetryResult<T> =
    try {
        Success(block(), exceptions)
    } catch (ex: Exception) {
        if (times < 1) {
            Failure(exceptions + ex)
        } else {
            val timesLeft = times - 1
            vent(timesLeft)
            retryInner(times - 1, vent, exceptions + ex, block)
        }
    }
