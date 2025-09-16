package no.nav.etterlatte.behandling.etteroppgjoer.sigrun

import no.nav.etterlatte.common.ConnectionAutoclosing
import no.nav.etterlatte.libs.common.feilhaandtering.krev
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.getTidspunktOrNull
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
                        INSERT INTO etteroppgjoer_hendelse_kjoering(
                            siste_sekvensnummer, antall_hendelser, antall_relevante
                        ) 
                        VALUES (?, ?, ?) 
                        """.trimIndent(),
                    )
                statement.setLong(1, kjoering.sisteSekvensnummer)
                statement.setInt(2, kjoering.antallHendelser)
                statement.setInt(3, kjoering.antallRelevante)
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
                        FROM etteroppgjoer_hendelse_kjoering
                        ORDER BY opprettet DESC
                        LIMIT 1
                        """.trimIndent(),
                    )

                statement.executeQuery().single {
                    HendelserKjoering(
                        sisteSekvensnummer = getLong("siste_sekvensnummer"),
                        antallHendelser = getInt("antall_hendelser"),
                        antallRelevante = getInt("antall_relevante"),
                        sisteRegistreringstidspunkt = getTidspunktOrNull("siste_registreringstidspunkt"),
                    )
                }
            }
        }
}

data class HendelserKjoering(
    val sisteSekvensnummer: Long,
    val antallHendelser: Int, // antall vi har etterspurt
    val antallRelevante: Int, // antall vi er interessert i (opprettet etteroppgjoer)
    val sisteRegistreringstidspunkt: Tidspunkt?,
) {
    fun nesteSekvensnummer() = sisteSekvensnummer + 1
}
