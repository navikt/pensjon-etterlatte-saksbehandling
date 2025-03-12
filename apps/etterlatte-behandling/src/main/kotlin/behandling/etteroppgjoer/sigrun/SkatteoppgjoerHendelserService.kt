package no.nav.etterlatte.behandling.etteroppgjoer.sigrun

import no.nav.etterlatte.inTransaction

class SkatteoppgjoerHendelserService(
    private val dao: SkatteoppgjoerHendelserDao,
    private val sigrunKlient: SigrunKlient,
) {
    suspend fun startHendelsesKjoering(request: HendelseKjoeringRequest) {
        val sisteKjoering = dao.hentSisteKjoering()
        val hendelsesListe = sigrunKlient.hentHendelsesliste(request.antall, sisteKjoering.sisteSekvensnummer)

        inTransaction {
            // TODO: sjekke opp mot etteroppgjoer tabell = skal ha etteroppgjoer
            // TODO: .....
            // TODO: lagre ny kjoering med oppdatert sekvensnummer
        }
    }
}

data class HendelseKjoeringRequest(
    val antall: Int,
)
