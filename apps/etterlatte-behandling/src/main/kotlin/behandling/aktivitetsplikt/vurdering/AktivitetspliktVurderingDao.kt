package no.nav.etterlatte.behandling.aktivitetsplikt.vurdering

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.behandling.hendelse.getUUID
import no.nav.etterlatte.behandling.objectMapper
import no.nav.etterlatte.common.ConnectionAutoclosing
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.database.single
import java.sql.Date
import java.sql.ResultSet
import java.time.LocalDate
import java.util.UUID

class AktivitetspliktVurderingDao(private val connectionAutoclosing: ConnectionAutoclosing) {
    fun opprettVurdering(
        vurdering: LagreAktivitetspliktVurdering,
        sakId: Long,
        kilde: Grunnlagsopplysning.Kilde,
        oppgaveId: UUID? = null,
        behandlingId: UUID? = null,
    ) = connectionAutoclosing.hentConnection {
        with(it) {
            val stmt =
                prepareStatement(
                    """
                        INSERT INTO aktivitetsplikt_vurdering(id, sak_id, behandling_id, oppgave_id, vurdering, fom, opprettet, endret, beskrivelse) 
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """.trimMargin(),
                )
            stmt.setObject(1, UUID.randomUUID())
            stmt.setLong(2, sakId)
            stmt.setObject(3, behandlingId)
            stmt.setObject(4, oppgaveId)
            stmt.setString(5, vurdering.vurdering.name)
            stmt.setDate(6, Date.valueOf(vurdering.fom))
            stmt.setString(7, kilde.toJson())
            stmt.setString(8, kilde.toJson())
            stmt.setString(9, vurdering.beskrivelse)

            stmt.executeUpdate()
        }
    }

    fun hentVurdering(oppgaveId: UUID): AktivitetspliktVurdering =
        connectionAutoclosing.hentConnection {
            with(it) {
                val stmt =
                    prepareStatement(
                        """
                        SELECT id, sak_id, behandling_id, oppgave_id, vurdering, fom, opprettet, endret, beskrivelse
                        FROM aktivitetsplikt_vurdering
                        WHERE oppgave_id = ?
                        """.trimMargin(),
                    )
                stmt.setObject(1, oppgaveId)

                stmt.executeQuery().single { toVurdering() }
            }
        }

    private fun ResultSet.toVurdering() =
        AktivitetspliktVurdering(
            id = getUUID("id"),
            sakId = getLong("sak_id"),
            behandlingId = getString("behandling_id")?.let { UUID.fromString(it) },
            oppgaveId = getString("oppgave_id")?.let { UUID.fromString(it) },
            vurdering = AktivitetspliktVurderingType.valueOf(getString("vurdering")),
            fom = getDate("fom").toLocalDate(),
            opprettet = objectMapper.readValue(getString("opprettet")),
            endret = objectMapper.readValue(getString("endret")),
            beskrivelse = getString("beskrivelse"),
        )
}

data class AktivitetspliktVurdering(
    val id: UUID,
    val sakId: Long,
    val behandlingId: UUID? = null,
    val oppgaveId: UUID? = null,
    val vurdering: AktivitetspliktVurderingType,
    val fom: LocalDate,
    val opprettet: Grunnlagsopplysning.Kilde,
    val endret: Grunnlagsopplysning.Kilde?,
    val beskrivelse: String,
)

data class LagreAktivitetspliktVurdering(
    val id: UUID? = null,
    val vurdering: AktivitetspliktVurderingType,
    val fom: LocalDate = LocalDate.now(),
    val beskrivelse: String,
)

enum class AktivitetspliktVurderingType {
    AKTIVITET_UNDER_50,
    AKTIVITET_OVER_50,
    AKTIVITET_100,
}
