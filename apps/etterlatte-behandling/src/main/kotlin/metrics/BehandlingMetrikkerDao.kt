package no.nav.etterlatte.metrics

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.behandling.objectMapper
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.database.toList
import java.sql.ResultSet
import javax.sql.DataSource

class BehandlingMetrikkerDao(private val dataSource: DataSource) {
    fun hentBehandlingsstatusAntall(): Map<BehandlingMetrikkVariant, BehandlingAntall> {
        val behandlinger = hentAlleBehandlinger()
        val behandlingerGjenny = behandlinger.filter { it.kilde == Vedtaksloesning.GJENNY }
        val behandlingerPesys = behandlinger.filter { it.kilde == Vedtaksloesning.PESYS }
        val migrertAutomatisk = behandlingerPesys.filter { it.virkningstidspunkt?.kilde?.ident == "PESYS" }
        val migrertManuelt =
            behandlingerPesys.filter { it.virkningstidspunkt.let { virk -> virk == null || virk.kilde.ident != "PESYS" } }

        return mapOf(
            BehandlingMetrikkVariant.NY_GJENNY to BehandlingAntall.of(behandlingerGjenny),
            BehandlingMetrikkVariant.AUTOMATISK_FRA_PESYS to BehandlingAntall.of(migrertAutomatisk),
            BehandlingMetrikkVariant.MANUELT_FRA_PESYS to BehandlingAntall.of(migrertManuelt),
        )
    }

    private fun hentAlleBehandlinger(): List<BehandlingMedStatus> {
        dataSource.connection.use {
            val statement =
                it.prepareStatement(
                    """
                    SELECT status, behandlingstype, kilde, virkningstidspunkt
                    FROM behandling
                    """.trimIndent(),
                )
            return statement.executeQuery().toList {
                asBehandlingerMedStatus()
            }
        }
    }

    private fun ResultSet.asBehandlingerMedStatus(): BehandlingMedStatus {
        return BehandlingMedStatus(
            status = BehandlingStatus.valueOf(getString("status")),
            type = BehandlingType.valueOf(getString("behandlingstype")),
            kilde = Vedtaksloesning.valueOf("kilde"),
            virkningstidspunkt = getString("virkningstidspunkt ")?.let { objectMapper.readValue(it) },
        )
    }
}

data class BehandlingMedStatus(
    val status: BehandlingStatus,
    val type: BehandlingType,
    val kilde: Vedtaksloesning,
    val virkningstidspunkt: Virkningstidspunkt?,
)

data class BehandlingAntall(
    val underBehandling: Int,
    val fattet: Int,
    val attestert: Int,
    val iverksatt: Int,
) {
    companion object {
        fun of(behandlinger: List<BehandlingMedStatus>) =
            BehandlingAntall(
                underBehandling =
                    behandlinger.filter {
                        listOf(
                            BehandlingStatus.OPPRETTET,
                            BehandlingStatus.VILKAARSVURDERT,
                            BehandlingStatus.TRYGDETID_OPPDATERT,
                            BehandlingStatus.BEREGNET,
                            BehandlingStatus.AVKORTET,
                            BehandlingStatus.RETURNERT,
                            BehandlingStatus.FATTET_VEDTAK,
                        ).contains(it.status)
                    }.size,
                fattet = behandlinger.filter { it.status == BehandlingStatus.FATTET_VEDTAK }.size,
                attestert =
                    behandlinger.filter {
                        listOf(
                            BehandlingStatus.ATTESTERT,
                            BehandlingStatus.TIL_SAMORDNING,
                            BehandlingStatus.SAMORDNET,
                        ).contains(it.status)
                    }.size,
                iverksatt = behandlinger.filter { it.status == BehandlingStatus.IVERKSATT }.size,
            )
    }
}

enum class BehandlingMetrikkVariant {
    NY_GJENNY,
    MANUELT_FRA_PESYS,
    AUTOMATISK_FRA_PESYS,
}
