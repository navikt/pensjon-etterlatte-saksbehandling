package no.nav.etterlatte.libs.database

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory
import java.sql.SQLException
import kotlin.random.Random

private const val SERIALIZATION_FAILURE_SQLSTATE = "40001"
private const val SERIALIZABLE_RETRY_MAX = 3
private val logger = LoggerFactory.getLogger("SerializableRetry")

suspend fun <T> withSerializableRetry(block: () -> T): T {
    for (attempt in 1..SERIALIZABLE_RETRY_MAX) {
        try {
            return block()
        } catch (e: Exception) {
            if (e is CancellationException ||
                e is EtterCommitException ||
                !e.isSerializationFailure() ||
                attempt == SERIALIZABLE_RETRY_MAX
            ) {
                throw e
            }
            val backoffMillis = Random.nextLong(50L * attempt, 100L * attempt)
            logger.warn("Serialization failure (forsøk $attempt/$SERIALIZABLE_RETRY_MAX), prøver igjen om $backoffMillis ms")
            delay(backoffMillis)
        }
    }
    throw IllegalStateException("Uventet tilstand i withSerializableRetry")
}

private fun Exception.isSerializationFailure(): Boolean {
    var cause: Throwable? = this
    while (cause != null) {
        if (cause is EtterCommitException || cause is CancellationException) return false
        if (cause is SQLException && cause.sqlState == SERIALIZATION_FAILURE_SQLSTATE) return true
        cause = cause.cause
    }
    return false
}

class EtterCommitException(
    cause: Exception,
) : RuntimeException("Transaksjonen er committet, men en etterfølgende handling feilet", cause)
