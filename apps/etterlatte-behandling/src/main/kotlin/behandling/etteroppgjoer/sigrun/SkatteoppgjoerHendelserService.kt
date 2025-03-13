package no.nav.etterlatte.behandling.etteroppgjoer.sigrun

import no.nav.etterlatte.inTransaction

class SkatteoppgjoerHendelserService(
    private val dao: SkatteoppgjoerHendelserDao,
    private val sigrunKlient: SigrunKlient,
) {
    suspend fun startHendelsesKjoering(request: HendelseKjoeringRequest) {
        val sisteKjoering = inTransaction { dao.hentSisteKjoering() }
        val hendelsesListe = sigrunKlient.hentHendelsesliste(request.antall, sisteKjoering.sisteSekvensnummer).hendelser

        inTransaction {
            val nyKjoering =
                HendelserKjoering(
                    hendelsesListe.last().sekvensnummer,
                    hendelsesListe.size,
                    0,
                    0,
                )

            for (hendelse in hendelsesListe) {
                // TODO: sjekke opp mot etteroppgjoer tabell = skal ha etteroppgjoer
                // TODO: ..... opprette forbehandling
                // TODO: ??

                nyKjoering.antallBehandlet++
                nyKjoering.antallRelevante++
            }

            dao.lagreKjoering(nyKjoering)
        }
    }
}

data class HendelseKjoeringRequest(
    val antall: Int,
)
