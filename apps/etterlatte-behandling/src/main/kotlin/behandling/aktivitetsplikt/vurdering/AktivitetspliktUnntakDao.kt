package no.nav.etterlatte.behandling.aktivitetsplikt.vurdering

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.behandling.aktivitetsplikt.AktivitetspliktVurderingOpprettetDato
import no.nav.etterlatte.behandling.hendelse.getUUID
import no.nav.etterlatte.behandling.objectMapper
import no.nav.etterlatte.common.ConnectionAutoclosing
import no.nav.etterlatte.libs.common.aktivitetsplikt.UnntakFraAktivitetDto
import no.nav.etterlatte.libs.common.aktivitetsplikt.UnntakFraAktivitetsplikt
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.database.singleOrNull
import no.nav.etterlatte.libs.database.toList
import java.sql.Date
import java.sql.ResultSet
import java.time.LocalDate
import java.util.UUID

class AktivitetspliktUnntakDao(
    private val connectionAutoclosing: ConnectionAutoclosing,
) {
    fun opprettUnntak(
        unntak: LagreAktivitetspliktUnntak,
        sakId: no.nav.etterlatte.libs.common.sak.SakId,
        kilde: Grunnlagsopplysning.Kilde,
        oppgaveId: UUID? = null,
        behandlingId: UUID? = null,
    ) = connectionAutoclosing.hentConnection {
        with(it) {
            val stmt =
                prepareStatement(
                    """
                        INSERT INTO aktivitetsplikt_unntak(id, sak_id, behandling_id, oppgave_id, unntak, fom, tom, opprettet, endret, beskrivelse) 
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """.trimMargin(),
                )
            stmt.setObject(1, UUID.randomUUID())
            stmt.setLong(2, sakId)
            stmt.setObject(3, behandlingId)
            stmt.setObject(4, oppgaveId)
            stmt.setString(5, unntak.unntak.name)
            stmt.setDate(6, unntak.fom?.let { tom -> Date.valueOf(tom) })
            stmt.setDate(7, unntak.tom?.let { tom -> Date.valueOf(tom) })
            stmt.setString(8, kilde.toJson())
            stmt.setString(9, kilde.toJson())
            stmt.setString(10, unntak.beskrivelse)

            stmt.executeUpdate()
        }
    }

    fun oppdaterUnntak(
        unntak: LagreAktivitetspliktUnntak,
        kilde: Grunnlagsopplysning.Kilde,
        behandlingId: UUID,
    ) = connectionAutoclosing.hentConnection {
        with(it) {
            val stmt =
                prepareStatement(
                    """
                        UPDATE aktivitetsplikt_unntak
                        SET  unntak = ?, fom = ?, tom = ?, endret = ?, beskrivelse = ? 
                        WHERE id = ? AND behandling_id = ?
                    """.trimMargin(),
                )
            stmt.setString(1, unntak.unntak.name)
            stmt.setDate(2, unntak.fom?.let { tom -> Date.valueOf(tom) })
            stmt.setDate(3, unntak.tom?.let { tom -> Date.valueOf(tom) })
            stmt.setString(4, kilde.toJson())
            stmt.setString(5, unntak.beskrivelse)
            stmt.setObject(6, requireNotNull(unntak.id))
            stmt.setObject(7, behandlingId)

            stmt.executeUpdate()
        }
    }

    fun slettUnntak(
        unntakId: UUID,
        behandlingId: UUID,
    ) = connectionAutoclosing.hentConnection {
        with(it) {
            val stmt =
                prepareStatement(
                    """
                        DELETE FROM aktivitetsplikt_unntak
                        WHERE id = ? AND behandling_id = ?
                    """.trimMargin(),
                )
            stmt.setObject(1, unntakId)
            stmt.setObject(2, behandlingId)

            stmt.executeUpdate()
        }
    }

    fun hentUnntakForOppgave(oppgaveId: UUID): List<AktivitetspliktUnntak> =
        connectionAutoclosing.hentConnection {
            with(it) {
                val stmt =
                    prepareStatement(
                        """
                        SELECT id, sak_id, behandling_id, oppgave_id, unntak, fom, tom, opprettet, endret, beskrivelse
                        FROM aktivitetsplikt_unntak
                        WHERE oppgave_id = ?
                        """.trimMargin(),
                    )
                stmt.setObject(1, oppgaveId)

                stmt.executeQuery().toList { toUnntak() }
            }
        }

    fun hentNyesteUnntak(sakId: no.nav.etterlatte.libs.common.sak.SakId): List<AktivitetspliktUnntak> =
        connectionAutoclosing.hentConnection {
            with(it) {
                val stmt =
                    prepareStatement(
                        """
                        SELECT behandling_id, oppgave_id FROM aktivitetsplikt_unntak
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
                    hentUnntakForBehandling(UUID.fromString(behandlingId))
                } else {
                    requireNotNull(oppgaveId) {
                        "Har en vurdering av aktivitet som ikke er knyttet til en oppgave eller en behandling"
                    }
                    hentUnntakForOppgave(UUID.fromString(oppgaveId))
                }
            }
        }

    fun hentUnntakForBehandling(behandlingId: UUID): List<AktivitetspliktUnntak> =
        connectionAutoclosing.hentConnection {
            with(it) {
                val stmt =
                    prepareStatement(
                        """
                        SELECT id, sak_id, behandling_id, oppgave_id, unntak, fom, tom, opprettet, endret, beskrivelse
                        FROM aktivitetsplikt_unntak
                        WHERE behandling_id = ?
                        """.trimMargin(),
                    )
                stmt.setObject(1, behandlingId)

                stmt.executeQuery().toList { toUnntak() }
            }
        }

    fun kopierUnntak(
        unntakId: UUID,
        behandlingId: UUID,
    ) = connectionAutoclosing.hentConnection {
        with(it) {
            val stmt =
                prepareStatement(
                    """
                    INSERT INTO aktivitetsplikt_unntak(id, sak_id, behandling_id, unntak, fom, tom, opprettet, endret, beskrivelse)
                    SELECT gen_random_uuid(), sak_id, ?, unntak, fom, tom, opprettet, endret, beskrivelse
                    FROM aktivitetsplikt_unntak
                    WHERE id = ?
                    """.trimMargin(),
                )
            stmt.setObject(1, behandlingId)
            stmt.setObject(2, unntakId)

            stmt.executeUpdate()
        }
    }

    private fun ResultSet.toUnntak() =
        AktivitetspliktUnntak(
            id = getUUID("id"),
            sakId = getLong("sak_id"),
            behandlingId = getString("behandling_id")?.let { UUID.fromString(it) },
            oppgaveId = getString("oppgave_id")?.let { UUID.fromString(it) },
            unntak = AktivitetspliktUnntakType.valueOf(getString("unntak")),
            fom = getDate("fom")?.toLocalDate(),
            tom = getDate("tom")?.toLocalDate(),
            opprettet = objectMapper.readValue(getString("opprettet")),
            endret = objectMapper.readValue(getString("endret")),
            beskrivelse = getString("beskrivelse"),
        )
}

data class AktivitetspliktUnntak(
    val id: UUID,
    val sakId: no.nav.etterlatte.libs.common.sak.SakId,
    val behandlingId: UUID? = null,
    val oppgaveId: UUID? = null,
    val unntak: AktivitetspliktUnntakType,
    val fom: LocalDate?,
    val tom: LocalDate?,
    override val opprettet: Grunnlagsopplysning.Kilde,
    val endret: Grunnlagsopplysning.Kilde?,
    val beskrivelse: String,
) : AktivitetspliktVurderingOpprettetDato {
    fun toDto(): UnntakFraAktivitetDto =
        UnntakFraAktivitetDto(
            unntak =
                when (this.unntak) {
                    AktivitetspliktUnntakType.OMSORG_BARN_UNDER_ETT_AAR ->
                        UnntakFraAktivitetsplikt.OMSORG_BARN_UNDER_ETT_AAR

                    AktivitetspliktUnntakType.OMSORG_BARN_SYKDOM ->
                        UnntakFraAktivitetsplikt.OMSORG_BARN_SYKDOM

                    AktivitetspliktUnntakType.MANGLENDE_TILSYNSORDNING_SYKDOM ->
                        UnntakFraAktivitetsplikt.MANGLENDE_TILSYNSORDNING_SYKDOM

                    AktivitetspliktUnntakType.SYKDOM_ELLER_REDUSERT_ARBEIDSEVNE ->
                        UnntakFraAktivitetsplikt.SYKDOM_ELLER_REDUSERT_ARBEIDSEVNE

                    AktivitetspliktUnntakType.GRADERT_UFOERETRYGD ->
                        UnntakFraAktivitetsplikt.GRADERT_UFOERETRYGD

                    AktivitetspliktUnntakType.MIDLERTIDIG_SYKDOM ->
                        UnntakFraAktivitetsplikt.MIDLERTIDIG_SYKDOM

                    AktivitetspliktUnntakType.FOEDT_1963_ELLER_TIDLIGERE_OG_LAV_INNTEKT ->
                        UnntakFraAktivitetsplikt.FOEDT_1963_ELLER_TIDLIGERE_OG_LAV_INNTEKT
                },
            fom = fom,
            tom = tom,
        )
}

data class LagreAktivitetspliktUnntak(
    val id: UUID? = null,
    val unntak: AktivitetspliktUnntakType,
    val fom: LocalDate?,
    val tom: LocalDate?,
    val beskrivelse: String,
)

enum class AktivitetspliktUnntakType {
    OMSORG_BARN_UNDER_ETT_AAR,
    OMSORG_BARN_SYKDOM,
    MANGLENDE_TILSYNSORDNING_SYKDOM,
    SYKDOM_ELLER_REDUSERT_ARBEIDSEVNE,
    GRADERT_UFOERETRYGD,
    MIDLERTIDIG_SYKDOM,
    FOEDT_1963_ELLER_TIDLIGERE_OG_LAV_INNTEKT,
}
