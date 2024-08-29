package no.nav.etterlatte.behandling.aktivitetsplikt.vurdering

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.behandling.aktivitetsplikt.AktivitetspliktVurderingOpprettetDato
import no.nav.etterlatte.behandling.hendelse.getUUID
import no.nav.etterlatte.behandling.objectMapper
import no.nav.etterlatte.common.ConnectionAutoclosing
import no.nav.etterlatte.libs.common.aktivitetsplikt.AktivitetspliktAktivitetsgradDto
import no.nav.etterlatte.libs.common.aktivitetsplikt.VurdertAktivitetsgrad
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.database.singleOrNull
import no.nav.etterlatte.libs.database.toList
import java.sql.Date
import java.sql.ResultSet
import java.time.LocalDate
import java.util.UUID

class AktivitetspliktAktivitetsgradDao(
    private val connectionAutoclosing: ConnectionAutoclosing,
) {
    fun opprettAktivitetsgrad(
        aktivitetsgrad: LagreAktivitetspliktAktivitetsgrad,
        sakId: SakId,
        kilde: Grunnlagsopplysning.Kilde,
        oppgaveId: UUID? = null,
        behandlingId: UUID? = null,
    ) = connectionAutoclosing.hentConnection {
        check(oppgaveId != null || behandlingId != null) {
            "Kan ikke opprette aktivitetsgrad som ikke er koblet på en behandling eller oppgave"
        }

        with(it) {
            val stmt =
                prepareStatement(
                    """
                        INSERT INTO aktivitetsplikt_aktivitetsgrad(id, sak_id, behandling_id, oppgave_id, aktivitetsgrad, fom, tom, opprettet, endret, beskrivelse) 
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """.trimMargin(),
                )
            stmt.setObject(1, UUID.randomUUID())
            stmt.setLong(2, sakId)
            stmt.setObject(3, behandlingId)
            stmt.setObject(4, oppgaveId)
            stmt.setString(5, aktivitetsgrad.aktivitetsgrad.name)
            stmt.setDate(6, Date.valueOf(aktivitetsgrad.fom))
            stmt.setDate(7, aktivitetsgrad.tom?.let { tom -> Date.valueOf(tom) })
            stmt.setString(8, kilde.toJson())
            stmt.setString(9, kilde.toJson())
            stmt.setString(10, aktivitetsgrad.beskrivelse)

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
                        SET  aktivitetsgrad = ?, fom = ?, tom = ?, endret = ?, beskrivelse = ? 
                        WHERE id = ? AND behandling_id = ?
                    """.trimMargin(),
                )

            stmt.setString(1, aktivitetsgrad.aktivitetsgrad.name)
            stmt.setDate(2, Date.valueOf(aktivitetsgrad.fom))
            stmt.setDate(3, aktivitetsgrad.tom?.let { tom -> Date.valueOf(tom) })
            stmt.setString(4, kilde.toJson())
            stmt.setString(5, aktivitetsgrad.beskrivelse)
            stmt.setObject(6, requireNotNull(aktivitetsgrad.id))
            stmt.setObject(7, behandlingId)

            stmt.executeUpdate()
        }
    }

    fun hentAktivitetsgradForOppgave(oppgaveId: UUID): List<AktivitetspliktAktivitetsgrad> =
        connectionAutoclosing.hentConnection {
            with(it) {
                val stmt =
                    prepareStatement(
                        """
                        SELECT id, sak_id, behandling_id, oppgave_id, aktivitetsgrad, fom, tom, opprettet, endret, beskrivelse
                        FROM aktivitetsplikt_aktivitetsgrad
                        WHERE oppgave_id = ?
                        ORDER BY fom ASC NULLS FIRST
                        """.trimMargin(),
                    )
                stmt.setObject(1, oppgaveId)

                stmt.executeQuery().toList { toAktivitetsgrad() }
            }
        }

    fun hentNyesteAktivitetsgrad(sakId: SakId): List<AktivitetspliktAktivitetsgrad> =
        connectionAutoclosing.hentConnection {
            with(it) {
                val stmt =
                    prepareStatement(
                        """
                        SELECT behandling_id, oppgave_id FROM aktivitetsplikt_aktivitetsgrad
                        WHERE sak_id = ?
                        ORDER BY endret::jsonb ->> 'tidspunkt' DESC
                        LIMIT 1
                        """.trimIndent(),
                    )
                stmt.setLong(1, sakId)

                val (behandlingId, oppgaveId) =
                    stmt.executeQuery().singleOrNull {
                        getString("behandling_id") to getString("oppgave_id")
                    } ?: return@hentConnection emptyList()

                if (behandlingId != null) {
                    hentAktivitetsgradForBehandling(UUID.fromString(behandlingId))
                } else {
                    requireNotNull(oppgaveId) {
                        "Har en vurdering av aktivitet som ikke er knyttet til en oppgave eller en behandling"
                    }
                    hentAktivitetsgradForOppgave(UUID.fromString(oppgaveId))
                }
            }
        }

    fun hentAktivitetsgradForBehandling(behandlingId: UUID): List<AktivitetspliktAktivitetsgrad> =
        connectionAutoclosing.hentConnection {
            with(it) {
                val stmt =
                    prepareStatement(
                        """
                        SELECT id, sak_id, behandling_id, oppgave_id, aktivitetsgrad, fom, tom, opprettet, endret, beskrivelse
                        FROM aktivitetsplikt_aktivitetsgrad
                        WHERE behandling_id = ?
                        ORDER BY fom ASC NULLS FIRST
                        """.trimMargin(),
                    )
                stmt.setObject(1, behandlingId)

                stmt.executeQuery().toList { toAktivitetsgrad() }
            }
        }

    fun kopierAktivitetsgrad(
        aktivitetId: UUID,
        behandlingId: UUID,
    ) = connectionAutoclosing.hentConnection {
        with(it) {
            val stmt =
                prepareStatement(
                    """
                        INSERT INTO aktivitetsplikt_aktivitetsgrad(id, sak_id, behandling_id, aktivitetsgrad, fom, tom, opprettet, endret, beskrivelse) 
                        SELECT gen_random_uuid(), sak_id, ?, aktivitetsgrad, fom, tom, opprettet, endret, beskrivelse
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
            tom = getDate("tom")?.toLocalDate(),
            opprettet = objectMapper.readValue(getString("opprettet")),
            endret = objectMapper.readValue(getString("endret")),
            beskrivelse = getString("beskrivelse"),
        )
}

data class AktivitetspliktAktivitetsgrad(
    val id: UUID,
    val sakId: SakId,
    val behandlingId: UUID? = null,
    val oppgaveId: UUID? = null,
    val aktivitetsgrad: AktivitetspliktAktivitetsgradType,
    val fom: LocalDate,
    val tom: LocalDate?,
    override val opprettet: Grunnlagsopplysning.Kilde,
    val endret: Grunnlagsopplysning.Kilde?,
    val beskrivelse: String,
) : AktivitetspliktVurderingOpprettetDato {
    fun toDto(): AktivitetspliktAktivitetsgradDto =
        AktivitetspliktAktivitetsgradDto(
            vurdering =
                when (this.aktivitetsgrad) {
                    AktivitetspliktAktivitetsgradType.AKTIVITET_UNDER_50 -> VurdertAktivitetsgrad.AKTIVITET_UNDER_50
                    AktivitetspliktAktivitetsgradType.AKTIVITET_OVER_50 -> VurdertAktivitetsgrad.AKTIVITET_OVER_50
                    AktivitetspliktAktivitetsgradType.AKTIVITET_100 -> VurdertAktivitetsgrad.AKTIVITET_100
                },
            fom = this.fom,
            tom = this.tom,
        )
}

data class LagreAktivitetspliktAktivitetsgrad(
    val id: UUID? = null,
    val aktivitetsgrad: AktivitetspliktAktivitetsgradType,
    val fom: LocalDate = LocalDate.now(),
    val tom: LocalDate? =  null,
    val beskrivelse: String,
)

enum class AktivitetspliktAktivitetsgradType {
    AKTIVITET_UNDER_50,
    AKTIVITET_OVER_50,
    AKTIVITET_100,
}
