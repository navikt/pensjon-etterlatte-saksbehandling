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

class AktivitetspliktUnntakDao(
    private val connectionAutoclosing: ConnectionAutoclosing,
) {
    fun opprettUnntak(
        unntak: LagreAktivitetspliktUnntak,
        sakId: Long,
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

    fun hentUnntakForOppgave(oppgaveId: UUID): AktivitetspliktUnntak? =
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

                stmt.executeQuery().singleOrNull { toUnntak() }
            }
        }

    fun hentNyesteUnntak(sakId: Long): AktivitetspliktUnntak? =
        connectionAutoclosing.hentConnection {
            with(it) {
                val stmt =
                    prepareStatement(
                        """
                        SELECT id, sak_id, behandling_id, oppgave_id, unntak, fom, tom, opprettet, endret, beskrivelse
                        FROM aktivitetsplikt_unntak
                        WHERE sak_id = ?
                        ORDER BY endret::jsonb->>'tidspunkt' DESC
                        LIMIT 1
                        """.trimMargin(),
                    )
                stmt.setLong(1, sakId)
                stmt.executeQuery().singleOrNull { toUnntak() }
            }
        }

    fun hentUnntakForBehandling(behandlingId: UUID): AktivitetspliktUnntak? =
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

                stmt.executeQuery().singleOrNull { toUnntak() }
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
    val sakId: Long,
    val behandlingId: UUID? = null,
    val oppgaveId: UUID? = null,
    val unntak: AktivitetspliktUnntakType,
    val fom: LocalDate?,
    val tom: LocalDate?,
    override val opprettet: Grunnlagsopplysning.Kilde,
    val endret: Grunnlagsopplysning.Kilde?,
    val beskrivelse: String,
) : AktivitetspliktVurderingOpprettetDato

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
