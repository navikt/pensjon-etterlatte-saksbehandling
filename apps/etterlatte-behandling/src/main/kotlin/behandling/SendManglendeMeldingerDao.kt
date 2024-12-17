package no.nav.etterlatte.behandling

import no.nav.etterlatte.behandling.hendelse.getUUID
import no.nav.etterlatte.behandling.jobs.BehandlingSomIkkeErAvbruttIStatistikk
import no.nav.etterlatte.common.ConnectionAutoclosing
import no.nav.etterlatte.libs.common.feilhaandtering.krev
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.database.toList
import java.util.UUID

class SendManglendeMeldingerDao(
    private val connectionAutoclosing: ConnectionAutoclosing,
) {
    fun hentManglendeAvslagBehandling(): List<BehandlingSomIkkeErAvbruttIStatistikk> =
        connectionAutoclosing.hentConnection {
            val statement =
                it.prepareStatement(
                    """
                    SELECT behandling_id, sak_id, mangler_hendelse from behandling_mangler_avbrudd_statistikk where sendt_melding = false limit 100
                    """.trimIndent(),
                )
            return@hentConnection statement
                .executeQuery()
                .toList {
                    BehandlingSomIkkeErAvbruttIStatistikk(
                        behandlingId = getUUID("behandling_id"),
                        sakId = SakId(getLong("sak_id")),
                        manglerHendelse = getBoolean("mangler_hendelse"),
                    )
                }
        }

    fun oppdaterSendtMelding(behandlingId: UUID) {
        connectionAutoclosing.hentConnection {
            val statement =
                it.prepareStatement(
                    """
                    UPDATE behandling_mangler_avbrudd_statistikk SET sendt_melding = true WHERE behandling_id = ?
                    """.trimIndent(),
                )
            statement.setObject(1, behandlingId)
            krev(statement.executeUpdate() == 1) {
                "Fikk ikke oppdatert raden for behandling har sendt melding for behandling $behandlingId"
            }
        }
    }
}
