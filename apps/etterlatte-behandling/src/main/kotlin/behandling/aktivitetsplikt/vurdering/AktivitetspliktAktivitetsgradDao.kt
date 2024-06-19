package no.nav.etterlatte.behandling.aktivitetsplikt.vurdering

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.behandling.aktivitetsplikt.AktivitetspliktVurderingOpprettetDato
import no.nav.etterlatte.behandling.hendelse.getUUID
import no.nav.etterlatte.behandling.objectMapper
import no.nav.etterlatte.common.ConnectionAutoclosing
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.database.singleOrNull
import java.sql.Date
import java.sql.ResultSet
import java.time.LocalDate
import java.util.UUID

class AktivitetspliktAktivitetsgradDao(
    private val connectionAutoclosing: ConnectionAutoclosing,
) {
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

    fun oppdaterAktivitetsgrad(
        aktivitetsgrad: LagreAktivitetspliktAktivitetsgrad,
        kilde: Grunnlagsopplysning.Kilde,
        behandlingId: UUID,
    ) = connectionAutoclosing.hentConnection {
        with(it) {
            val stmt =
                prepareStatement(
                    """
                        UPDATE aktivitetsplikt_aktivitetsgrad
                        SET  aktivitetsgrad = ?, fom = ?, endret = ?, beskrivelse = ? 
                        WHERE id = ? AND behandling_id = ?
                    """.trimMargin(),
                )

            stmt.setString(1, aktivitetsgrad.aktivitetsgrad.name)
            stmt.setDate(2, Date.valueOf(aktivitetsgrad.fom))
            stmt.setString(3, kilde.toJson())
            stmt.setString(4, aktivitetsgrad.beskrivelse)
            stmt.setObject(5, requireNotNull(aktivitetsgrad.id))
            stmt.setObject(6, behandlingId)

            stmt.executeUpdate()
        }
    }

    fun hentAktivitetsgradForOppgave(oppgaveId: UUID): AktivitetspliktAktivitetsgrad? =
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

    fun hentAktivitetsgradForBehandling(behandlingId: UUID): AktivitetspliktAktivitetsgrad? =
        connectionAutoclosing.hentConnection {
            with(it) {
                val stmt =
                    prepareStatement(
                        """
                        SELECT id, sak_id, behandling_id, oppgave_id, aktivitetsgrad, fom, opprettet, endret, beskrivelse
                        FROM aktivitetsplikt_aktivitetsgrad
                        WHERE behandling_id = ?
                        """.trimMargin(),
                    )
                stmt.setObject(1, behandlingId)

                stmt.executeQuery().singleOrNull { toAktivitetsgrad() }
            }
        }

    fun kopierAktivtetsgrad(
        aktivitetId: UUID,
        behandlingId: UUID,
    ) = connectionAutoclosing.hentConnection {
        with(it) {
            val stmt =
                prepareStatement(
                    """
                        INSERT INTO aktivitetsplikt_aktivitetsgrad(id, sak_id, behandling_id, aktivitetsgrad, fom, opprettet, endret, beskrivelse) 
                        SELECT gen_random_uuid(), sak_id, ?, aktivitetsgrad, fom, opprettet, endret, beskrivelse
                        FROM aktivitetsplikt_aktivitetsgrad
                        WHERE id = ?
                    """.trimMargin(),
                )
            stmt.setObject(1, behandlingId)
            stmt.setObject(2, aktivitetId)

            stmt.executeUpdate()
        }
    }

    fun slettAktivitetsgrad(
        aktivitetId: UUID,
        behandlingId: UUID,
    ) = connectionAutoclosing.hentConnection {
        with(it) {
            val stmt =
                prepareStatement(
                    """
                        DELETE FROM aktivitetsplikt_aktivitetsgrad
                        WHERE id = ? AND behandling_id = ?
                    """.trimMargin(),
                )
            stmt.setObject(1, aktivitetId)
            stmt.setObject(2, behandlingId)

            stmt.executeUpdate()
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
    override val opprettet: Grunnlagsopplysning.Kilde,
    val endret: Grunnlagsopplysning.Kilde?,
    val beskrivelse: String,
) : AktivitetspliktVurderingOpprettetDato

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
