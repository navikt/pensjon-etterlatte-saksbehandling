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

class AktivitetspliktUnntakDao(private val connectionAutoclosing: ConnectionAutoclosing) {
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
                        INSERT INTO aktivitetsplikt_unntak(id, sak_id, behandling_id, oppgave_id, unntak, tom, opprettet, endret, beskrivelse) 
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """.trimMargin(),
                )
            stmt.setObject(1, UUID.randomUUID())
            stmt.setLong(2, sakId)
            stmt.setObject(3, behandlingId)
            stmt.setObject(4, oppgaveId)
            stmt.setString(5, unntak.unntak.name)
            stmt.setDate(6, unntak.tom?.let { tom -> Date.valueOf(tom) })
            stmt.setString(7, kilde.toJson())
            stmt.setString(8, kilde.toJson())
            stmt.setString(9, unntak.beskrivelse)

            stmt.executeUpdate()
        }
    }

    fun hentUnntak(oppgaveId: UUID): AktivitetspliktUnntak? =
        connectionAutoclosing.hentConnection {
            with(it) {
                val stmt =
                    prepareStatement(
                        """
                        SELECT id, sak_id, behandling_id, oppgave_id, unntak, tom, opprettet, endret, beskrivelse
                        FROM aktivitetsplikt_unntak
                        WHERE oppgave_id = ?
                        """.trimMargin(),
                    )
                stmt.setObject(1, oppgaveId)

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
    val tom: LocalDate?,
    val opprettet: Grunnlagsopplysning.Kilde,
    val endret: Grunnlagsopplysning.Kilde?,
    val beskrivelse: String,
)

data class LagreAktivitetspliktUnntak(
    val id: UUID? = null,
    val unntak: AktivitetspliktUnntakType,
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
