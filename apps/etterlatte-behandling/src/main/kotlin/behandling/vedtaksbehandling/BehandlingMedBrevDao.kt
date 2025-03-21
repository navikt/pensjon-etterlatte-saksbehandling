package no.nav.etterlatte.behandling.vedtaksbehandling

import no.nav.etterlatte.common.ConnectionAutoclosing
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.KlageStatus
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingStatus
import no.nav.etterlatte.libs.database.single
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.sql.ResultSet
import java.util.UUID

private val logger: Logger = LoggerFactory.getLogger("BehandlingMedBrev")

class BehandlingMedBrevDao(
    private val connectionAutoclosing: ConnectionAutoclosing,
) {
    fun erBehandlingRedigerbar(behandlingId: UUID): Boolean =
        connectionAutoclosing.hentConnection {
            with(it) {
                hentBehandlingMedBrev(behandlingId)
                    .erRedigerbar()
            }
        }

    fun hentBehandlingMedBrev(behandlingId: UUID): BehandlingMedBrev =
        connectionAutoclosing.hentConnection {
            with(it) {
                hentBehandlingMedBrev(behandlingId)
            }
        }

    private fun Connection.hentBehandlingMedBrev(behandlingId: UUID): BehandlingMedBrev {
        val statement =
            prepareStatement(
                """
                SELECT 'BEHANDLING', id, status FROM behandling WHERE id = ?
                union SELECT 'TILBAKEKREVING', id, status FROM tilbakekreving WHERE id = ?
                union SELECT 'KLAGE', id, status FROM klage WHERE id = ?
                union SELECT 'ETTEROPPGJOER', id, status FROM etteroppgjoer_behandling WHERE id = ?
                """.trimIndent(),
            )
        statement.setObject(1, behandlingId)
        statement.setObject(2, behandlingId)
        statement.setObject(3, behandlingId)
        statement.setObject(4, behandlingId)

        val vedtaksbehandling =
            statement
                .executeQuery()
                .single { toVedtaksbehandling() }
        return vedtaksbehandling
    }

    private fun ResultSet.toVedtaksbehandling(): BehandlingMedBrev {
        val type = BehandlingMedBrevType.valueOf(getString(1))
        val id = UUID.fromString(getString(2))
        val status = getString(3)

        return BehandlingMedBrev(id, type, status)
    }
}

data class BehandlingMedBrev(
    val id: UUID,
    val type: BehandlingMedBrevType,
    val status: String,
) {
    fun erRedigerbar(): Boolean {
        val redigerbar =
            when (type) {
                BehandlingMedBrevType.BEHANDLING -> BehandlingStatus.valueOf(status).kanEndres()
                BehandlingMedBrevType.TILBAKEKREVING -> TilbakekrevingStatus.valueOf(status).kanEndres()
                BehandlingMedBrevType.KLAGE -> {
                    val klageStatus = KlageStatus.valueOf(status)
                    // I konteksten av vedtak så er klager også mulig å endre når formkravene ikke er oppfylt,
                    // siden det er da man oppretter vedtak om avvist klage
                    KlageStatus.kanEndres(klageStatus) || klageStatus == KlageStatus.FORMKRAV_IKKE_OPPFYLT
                }
                BehandlingMedBrevType.ETTEROPPGJOER -> true // TODO få inn riktig redigerbarhet her
            }
        logger.info(
            "Fikk henvendelse om vedtak til behandling $id av type $type var redigerbar. " +
                "Svarte $redigerbar fordi statusen var $status",
        )
        return redigerbar
    }
}

enum class BehandlingMedBrevType(
    val harVedtaksbrev: Boolean,
) {
    BEHANDLING(true),
    TILBAKEKREVING(true),
    KLAGE(true),
    ETTEROPPGJOER(false),
}
