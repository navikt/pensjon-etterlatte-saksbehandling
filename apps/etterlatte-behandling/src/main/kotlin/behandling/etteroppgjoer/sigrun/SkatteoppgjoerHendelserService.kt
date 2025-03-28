package no.nav.etterlatte.behandling.etteroppgjoer.sigrun

import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerService
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerStatus
import no.nav.etterlatte.inTransaction
import org.slf4j.LoggerFactory

class SkatteoppgjoerHendelserService(
    private val dao: SkatteoppgjoerHendelserDao,
    private val sigrunKlient: SigrunKlient,
    private val etteroppgjoerService: EtteroppgjoerService,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    suspend fun startHendelsesKjoering(request: HendelseKjoeringRequest) {
        logger.info("Starter kjøring for behandle hendelser fra skatt, sjekker ${request.antall} hendelser")

        val sisteKjoering = inTransaction { dao.hentSisteKjoering() }
        val hendelsesListe = sigrunKlient.hentHendelsesliste(request.antall, sisteKjoering.nesteSekvensnummer())
        var antallRelevanteHendelser = 0

        val kjoering =
            HendelserKjoering(
                hendelsesListe.hendelser.last().sekvensnummer,
                hendelsesListe.hendelser.size,
                antallRelevanteHendelser,
            )

        inTransaction {
            for (hendelse in hendelsesListe.hendelser) {
                val etteroppgjoerResultat =
                    etteroppgjoerService.skalHaEtteroppgjoer(
                        hendelse.identifikator,
                        hendelse.gjelderPeriode.toInt(),
                    )

                if (etteroppgjoerResultat.skalHaEtteroppgjoer) {
                    val etteroppgjoer = etteroppgjoerResultat.etteroppgjoer!!

                    // TODO: opprett forbehandling

                    etteroppgjoerService.oppdaterStatus(
                        etteroppgjoer.sakId,
                        etteroppgjoer.inntektsaar,
                        EtteroppgjoerStatus.MOTTATT_HENDELSE,
                    )
                    antallRelevanteHendelser++
                }

                // TODO legge til status evnt feil?
                dao.lagreKjoering(kjoering)
            }
        }
    }
}

data class HendelseKjoeringRequest(
    val antall: Int,
)
