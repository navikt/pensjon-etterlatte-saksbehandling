package no.nav.etterlatte.behandling.etteroppgjoer.sigrun

import no.nav.etterlatte.inTransaction

class SkatteoppgjoerHendelserService(
    private val dao: SkatteoppgjoerHendelserDao,
) {
    fun hentogBehandleSkatteoppgjoerHendelser() {
        inTransaction {
            val sisteKjoering = dao.hentSisteKjoering()

            // TODO: gjør rest kall til Sigrun Skatteoppgjør hendelser

            // TODO: lagre ny kjoering med oppdatert sekvensnummer
        }
    }
}

data class HendelserKjoering(
    val sisteSekvensnummer: Int,
    val antallHendelser: Int, // antall vi har etterspurt
    val antallBehandlet: Int, // antall vi har sjekket
    val antallRelevante: Int, // antall vi er interessert i (opprettet forbehandling)
)

data class SkatteoppgjoerHendelser(
    val gjelderPeriode: String,
    val hendelsetype: String,
    val identifikator: String,
    val sekvensnummer: Int,
    val somAktoerid: Boolean,
)
