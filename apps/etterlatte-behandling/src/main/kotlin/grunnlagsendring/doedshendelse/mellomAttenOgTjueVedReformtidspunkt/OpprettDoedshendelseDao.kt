package no.nav.etterlatte.grunnlagsendring.doedshendelse.mellom18og20PaaReformtidspunkt

import no.nav.etterlatte.common.ConnectionAutoclosing
import no.nav.etterlatte.libs.database.toList

enum class OpprettDoedshendelseStatus {
    NY,
    STARTET,
    OPPRETTET,
    FEILET,
}

class OpprettDoedshendelseDao(
    private val connectionAutoclosing: ConnectionAutoclosing,
) {
    fun hentAvdoede(status: List<OpprettDoedshendelseStatus>): List<String> =
        connectionAutoclosing.hentConnection {
            with(it) {
                prepareStatement(
                    """
                    SELECT fnr, status
                    FROM mellom_atten_og_tjue_ved_reformtidspunkt
                    WHERE status = any(?)
                    """.trimIndent(),
                ).apply {
                    setArray(1, createArrayOf("text", status.toTypedArray()))
                }.executeQuery()
                    .toList { getString("fnr") }
            }
        }

    fun oppdater(
        fnr: String,
        status: OpprettDoedshendelseStatus,
        feilmelding: String? = null,
    ) = connectionAutoclosing.hentConnection {
        with(it) {
            prepareStatement(
                """
                UPDATE mellom_atten_og_tjue_ved_reformtidspunkt 
                SET status = ?, endret = now(), feilmelding = ?
                WHERE fnr = ?
                """.trimIndent(),
            ).apply {
                setString(1, status.name)
                setString(2, feilmelding)
                setString(3, fnr)
            }.executeUpdate()
        }
    }
}
