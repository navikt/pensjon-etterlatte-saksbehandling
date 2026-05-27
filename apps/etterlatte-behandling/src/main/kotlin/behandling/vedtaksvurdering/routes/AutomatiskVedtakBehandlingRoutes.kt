package no.nav.etterlatte.behandling.vedtaksvurdering.routes

import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.vedtaksvurdering.service.AutomatiskVedtakBehandlingService
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.ktor.route.BEHANDLINGID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.SAKID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.behandlingId
import no.nav.etterlatte.libs.ktor.route.sakId
import no.nav.etterlatte.libs.ktor.token.brukerTokenInfo
import no.nav.etterlatte.migrering.MigreringKjoringVariant
import no.nav.etterlatte.tilgangsstyring.kunSkrivetilgang
import org.postgresql.util.PSQLException
import org.slf4j.LoggerFactory

private const val SERIALIZATION_FAILURE_SQLSTATE = "40001"
private const val SERIALIZABLE_RETRY_MAX = 3

/**
 * Kjører [block] på nytt ved PostgreSQL serialization failure (SQLState 40001).
 *
 * SERIALIZABLE-isolasjon kan gi falske positive konflikter under høy parallellitet, f.eks. under regulering
 * der mange transaksjoner gjør les-så-skriv mot vedtak-tabellen samtidig. PostgreSQL-dokumentasjonen
 * anbefaler eksplisitt å retry ved SQLState 40001.
 *
 * Hver retry starter en ny transaksjon (kalt via [inTransaction] i [block]).
 */
fun <T> withSerializableRetry(block: () -> T): T {
    for (attempt in 1..SERIALIZABLE_RETRY_MAX) {
        try {
            return block()
        } catch (e: Exception) {
            if (e.isSerializationFailure() && attempt < SERIALIZABLE_RETRY_MAX) {
                logger.warn("Serialization failure (forsøk $attempt/$SERIALIZABLE_RETRY_MAX), prøver igjen")
            } else {
                throw e
            }
        }
    }
    throw IllegalStateException("Uventet tilstand i withSerializableRetry")
}

private fun Exception.isSerializationFailure(): Boolean {
    var cause: Throwable? = this
    while (cause != null) {
        if (cause is PSQLException && cause.sqlState == SERIALIZATION_FAILURE_SQLSTATE) return true
        cause = cause.cause
    }
    return false
}

private val logger = LoggerFactory.getLogger("AutomatiskVedtakBehandlingRoute")

fun Route.automatiskVedtakBehandlingRoutes(automatiskVedtakBehandlingService: AutomatiskVedtakBehandlingService) {
    route("/api/vedtak") {
        // Automatisk hva da?
        post("/{$SAKID_CALL_PARAMETER}/{$BEHANDLINGID_CALL_PARAMETER}/automatisk") {
            kunSkrivetilgang {
                logger.info("Håndterer behandling $behandlingId")
                val nyttVedtak =
                    withSerializableRetry {
                        inTransaction {
                            runBlocking {
                                automatiskVedtakBehandlingService.vedtakStegvis(
                                    behandlingId = behandlingId,
                                    sakId = sakId,
                                    brukerTokenInfo = brukerTokenInfo,
                                    kjoringVariant = MigreringKjoringVariant.FULL_KJORING,
                                )
                            }
                        }
                    }
                call.respond(nyttVedtak)
            }
        }

        post("/{$SAKID_CALL_PARAMETER}/{$BEHANDLINGID_CALL_PARAMETER}/automatisk/stegvis") {
            kunSkrivetilgang {
                val kjoringVariant = call.receive<MigreringKjoringVariant>()
                logger.info("Håndterer behandling $behandlingId med kjøringsvariant ${kjoringVariant.name}")
                val nyttVedtak =
                    withSerializableRetry {
                        inTransaction {
                            runBlocking {
                                automatiskVedtakBehandlingService.vedtakStegvis(behandlingId, sakId, brukerTokenInfo, kjoringVariant)
                            }
                        }
                    }
                call.respond(nyttVedtak)
            }
        }
    }
}
