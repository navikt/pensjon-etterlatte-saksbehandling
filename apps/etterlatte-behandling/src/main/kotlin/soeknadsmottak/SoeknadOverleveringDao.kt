package no.nav.etterlatte.soeknadsmottak

import no.nav.etterlatte.common.ConnectionAutoclosing
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.soeknadsmottak.MottattSoeknadType
import no.nav.etterlatte.libs.common.toJson
import tools.jackson.module.kotlin.readValue
import java.util.UUID

data class SoeknadOverlevering(
    val mottaksnoekkel: String,
    val soeknadType: MottattSoeknadType,
    val sakId: SakId,
    val journalpostId: String,
    val dokumentInfoIder: List<String>,
    val oppgaveId: UUID?,
)

/** Skriver på aktiv transaksjon når den kalles innenfor `inTransaction`. */
class SoeknadOverleveringDao(
    private val connectionAutoclosing: ConnectionAutoclosing,
) {
    /**
     * Reserverer nøkkelen. Returnerer false hvis den finnes. Et parallelt kall med samme nøkkel venter
     * på den andre transaksjonen, og får false hvis den committer.
     */
    fun reserver(overlevering: SoeknadOverlevering): Boolean =
        connectionAutoclosing.hentConnection { connection ->
            connection
                .prepareStatement(
                    """
                    INSERT INTO soeknad_overlevering (mottaksnoekkel, soeknad_type, sak_id, journalpost_id, dokument_info_ider)
                    VALUES (?, ?, ?, ?, ?::jsonb)
                    ON CONFLICT (mottaksnoekkel) DO NOTHING
                    """.trimIndent(),
                ).use {
                    it.setString(1, overlevering.mottaksnoekkel)
                    it.setString(2, overlevering.soeknadType.name)
                    it.setLong(3, overlevering.sakId.sakId)
                    it.setString(4, overlevering.journalpostId)
                    it.setString(5, overlevering.dokumentInfoIder.toJson())
                    it.executeUpdate() == 1
                }
        }

    fun lagreOppgave(
        mottaksnoekkel: String,
        oppgaveId: UUID,
    ) = connectionAutoclosing.hentConnection { connection ->
        connection
            .prepareStatement("UPDATE soeknad_overlevering SET oppgave_id = ? WHERE mottaksnoekkel = ? AND oppgave_id IS NULL")
            .use {
                it.setObject(1, oppgaveId)
                it.setString(2, mottaksnoekkel)
                check(it.executeUpdate() == 1) { "Overlevering $mottaksnoekkel har allerede oppgave" }
            }
    }

    fun hent(mottaksnoekkel: String): SoeknadOverlevering? =
        connectionAutoclosing.hentConnection { connection ->
            connection.prepareStatement("SELECT * FROM soeknad_overlevering WHERE mottaksnoekkel = ?").use {
                it.setString(1, mottaksnoekkel)
                it.executeQuery().use { rs ->
                    if (!rs.next()) {
                        null
                    } else {
                        SoeknadOverlevering(
                            mottaksnoekkel = rs.getString("mottaksnoekkel"),
                            soeknadType = MottattSoeknadType.valueOf(rs.getString("soeknad_type")),
                            sakId = SakId(rs.getLong("sak_id")),
                            journalpostId = rs.getString("journalpost_id"),
                            dokumentInfoIder = objectMapper.readValue(rs.getString("dokument_info_ider")),
                            oppgaveId = rs.getObject("oppgave_id", UUID::class.java),
                        )
                    }
                }
            }
        }
}
