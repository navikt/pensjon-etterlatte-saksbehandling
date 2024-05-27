package no.nav.etterlatte.behandling.aktivitetsplikt.vurdering

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.behandling.hendelse.getUUID
import no.nav.etterlatte.behandling.objectMapper
import no.nav.etterlatte.common.ConnectionAutoclosing
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.database.singleOrNull
import java.sql.Date
import java.sql.ResultSet
import java.time.LocalDate
import java.util.UUID

class AktivitetspliktAktivitetsgradDao(private val connectionAutoclosing: ConnectionAutoclosing) {
    fun opprettAktivitetsgrad(
        aktivitetsgrad: LagreAktivitetspliktAktivitetsgrad,
        sakId: Long,
        kilde: Grunnlagsopplysning.Kilde,
        oppgaveId: UUID? = null,
        behandlingId: UUID? = null,
    ) = connectionAutoclosing.hentConnection {
        with(it) {
            val stmt =
                prepareStatement(
                    """
                        INSERT INTO aktivitetsplikt_aktivitetsgrad(id, sak_id, behandling_id, oppgave_id, aktivitetsgrad, fom, opprettet, endret, beskrivelse) 
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """.trimMargin(),
                )
            stmt.setObject(1, UUID.randomUUID())
            stmt.setLong(2, sakId)
            stmt.setObject(3, behandlingId)
            stmt.setObject(4, oppgaveId)
            stmt.setString(5, aktivitetsgrad.aktivitetsgrad.name)
            stmt.setDate(6, Date.valueOf(aktivitetsgrad.fom))
            stmt.setString(7, kilde.toJson())
            stmt.setString(8, kilde.toJson())
            stmt.setString(9, aktivitetsgrad.beskrivelse)

            stmt.executeUpdate()
        }
    }

    fun hentAktivitetsgrad(oppgaveId: UUID): AktivitetspliktAktivitetsgrad? =
        connectionAutoclosing.hentConnection {
            with(it) {
                val stmt =
                    prepareStatement(
                        """
                        SELECT id, sak_id, behandling_id, oppgave_id, aktivitetsgrad, fom, opprettet, endret, beskrivelse
                        FROM aktivitetsplikt_aktivitetsgrad
                        WHERE oppgave_id = ?
                        """.trimMargin(),
                    )
                stmt.setObject(1, oppgaveId)

                stmt.executeQuery().singleOrNull { toAktivitetsgrad() }
            }
        }

    fun hentNyesteAktivitetsgrad(sakId: Long): AktivitetspliktAktivitetsgrad? =
        connectionAutoclosing.hentConnection {
            with(it) {
                val stmt =
                    prepareStatement(
                        """
                        SELECT id, sak_id, behandling_id, oppgave_id, aktivitetsgrad, fom, opprettet, endret, beskrivelse
                        FROM aktivitetsplikt_aktivitetsgrad
                        WHERE sak_id = ?
                        ORDER BY endret::jsonb->>'tidspunkt' DESC
                        LIMIT 1
                        """.trimMargin(),
                    )
                stmt.setLong(1, sakId)

                stmt.executeQuery().singleOrNull { toAktivitetsgrad() }
            }
        }

    private fun ResultSet.toAktivitetsgrad() =
        AktivitetspliktAktivitetsgrad(
            id = getUUID("id"),
            sakId = getLong("sak_id"),
            behandlingId = getString("behandling_id")?.let { UUID.fromString(it) },
            oppgaveId = getString("oppgave_id")?.let { UUID.fromString(it) },
            aktivitetsgrad = AktivitetspliktAktivitetsgradType.valueOf(getString("aktivitetsgrad")),
            fom = getDate("fom").toLocalDate(),
            opprettet = objectMapper.readValue(getString("opprettet")),
            endret = objectMapper.readValue(getString("endret")),
            beskrivelse = getString("beskrivelse"),
        )
}

data class AktivitetspliktAktivitetsgrad(
    val id: UUID,
    val sakId: Long,
    val behandlingId: UUID? = null,
    val oppgaveId: UUID? = null,
    val aktivitetsgrad: AktivitetspliktAktivitetsgradType,
    val fom: LocalDate,
    val opprettet: Grunnlagsopplysning.Kilde,
    val endret: Grunnlagsopplysning.Kilde?,
    val beskrivelse: String,
)

data class LagreAktivitetspliktAktivitetsgrad(
    val id: UUID? = null,
    val aktivitetsgrad: AktivitetspliktAktivitetsgradType,
    val fom: LocalDate = LocalDate.now(),
    val beskrivelse: String,
)

enum class AktivitetspliktAktivitetsgradType {
    AKTIVITET_UNDER_50,
    AKTIVITET_OVER_50,
    AKTIVITET_100,
}
