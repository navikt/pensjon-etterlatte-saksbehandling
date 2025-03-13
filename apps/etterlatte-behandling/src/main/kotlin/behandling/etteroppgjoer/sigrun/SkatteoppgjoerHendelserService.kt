package no.nav.etterlatte.behandling.etteroppgjoer.sigrun

import no.nav.etterlatte.inTransaction

class SkatteoppgjoerHendelserService(
    private val dao: SkatteoppgjoerHendelserDao,
    private val sigrunKlient: SigrunKlient,
) {
    suspend fun startHendelsesKjoering(request: HendelseKjoeringRequest) {
        val sisteKjoering = inTransaction { dao.hentSisteKjoering() }
        val hendelsesListe = sigrunKlient.hentHendelsesliste(request.antall, sisteKjoering.nesteSekvensnummer())

        inTransaction {
            val nyKjoering =
                HendelserKjoering(
                    hendelsesListe.hendelser.last().sekvensnummer,
                    hendelsesListe.hendelser.size,
                    0,
                )

            for (hendelse in hendelsesListe.hendelser) {
                // TODO: sjekke opp mot etteroppgjoer tabell = skal ha etteroppgjoer
                // TODO: ..... opprette forbehandling
                // TODO: ??

                // TODO: bump for hver forbehandling som opprettes
                nyKjoering.antallRelevante++
            }

            dao.lagreKjoering(nyKjoering)
        }
    }
}

data class HendelseKjoeringRequest(
    val antall: Int,
)
