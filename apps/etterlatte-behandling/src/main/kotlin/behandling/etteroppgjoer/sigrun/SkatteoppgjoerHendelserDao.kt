package no.nav.etterlatte.behandling.etteroppgjoer.sigrun

import no.nav.etterlatte.common.ConnectionAutoclosing
import no.nav.etterlatte.libs.common.feilhaandtering.krev
import no.nav.etterlatte.libs.database.single

class SkatteoppgjoerHendelserDao(
    private val connectionAutoclosing: ConnectionAutoclosing,
) {
    fun lagreKjoering(kjoering: HendelserKjoering) =
        connectionAutoclosing.hentConnection {
            with(it) {
                val statement =
                    prepareStatement(
                        """
                        INSERT INTO skatteoppgjoer_hendelse_kjoringer(
                            siste_sekvensnummer, antall_hendelser, antall_behandlet, antall_relevante
                        ) 
                        VALUES (?, ?, ?, ?) 
                        """.trimIndent(),
                    )
                statement.setLong(1, kjoering.sisteSekvensnummer)
                statement.setInt(2, kjoering.antallHendelser)
                statement.setInt(3, kjoering.antallBehandlet)
                statement.setInt(4, kjoering.antallRelevante)
                statement.executeUpdate().also {
                    krev(it == 1) {
                        "Kunne ikke lagre kjoering for skatteoppgjoerHendelser=$kjoering"
                    }
                }
            }
        }

    fun hentSisteKjoering(): HendelserKjoering =
        connectionAutoclosing.hentConnection {
            with(it) {
                val statement =
                    prepareStatement(
                        """
                        SELECT *
                        FROM skatteoppgjoer_hendelse_kjoringer
                        ORDER BY opprettet DESC
                        LIMIT 1
                        """.trimIndent(),
                    )

                statement.executeQuery().single {
                    HendelserKjoering(
                        sisteSekvensnummer = getLong("siste_sekvensnummer"),
                        antallHendelser = getInt("antall_hendelser"),
                        antallBehandlet = getInt("antall_behandlet"),
                        antallRelevante = getInt("antall_relevante"),
                    )
                }
            }
        }
}

data class HendelserKjoering(
    val sisteSekvensnummer: Long,
    val antallHendelser: Int, // antall vi har etterspurt
    val antallBehandlet: Int, // antall vi har sjekket
    val antallRelevante: Int, // antall vi er interessert i (opprettet forbehandling)
)
