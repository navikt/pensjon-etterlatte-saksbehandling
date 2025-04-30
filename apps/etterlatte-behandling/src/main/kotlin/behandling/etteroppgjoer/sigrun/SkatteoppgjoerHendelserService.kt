package no.nav.etterlatte.behandling.etteroppgjoer.sigrun

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerService
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerStatus
import org.slf4j.LoggerFactory

class SkatteoppgjoerHendelserService(
    private val dao: SkatteoppgjoerHendelserDao,
    private val sigrunKlient: SigrunKlient,
    private val etteroppgjoerService: EtteroppgjoerService,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun startHendelsesKjoering(request: HendelseKjoeringRequest) {
        logger.info("Starter å behandle ${request.antall} hendelser fra skatt")

        val sisteKjoering = dao.hentSisteKjoering()
        val hendelsesListe = runBlocking { sigrunKlient.hentHendelsesliste(request.antall, sisteKjoering.nesteSekvensnummer()) }

        val kjoering =
            HendelserKjoering(
                sisteSekvensnummer = hendelsesListe.hendelser.last().sekvensnummer,
                antallHendelser = hendelsesListe.hendelser.size,
                antallRelevante = 0,
            )

        kjoering.antallRelevante =
            hendelsesListe.hendelser.count { hendelse ->
                val resultat =
                    etteroppgjoerService.skalHaEtteroppgjoer(
                        hendelse.identifikator,
                        hendelse.gjelderPeriode.toInt(),
                    )

                if (resultat.skalHaEtteroppgjoer) {
                    val etteroppgjoer = resultat.etteroppgjoer!!

                    logger.info("Sak=${etteroppgjoer.sakId} skal ha etteroppgjør for inntektsår=${hendelse.gjelderPeriode}")
                    etteroppgjoerService.oppdaterEtteroppgjoerStatus(
                        etteroppgjoer.sakId,
                        etteroppgjoer.inntektsaar,
                        EtteroppgjoerStatus.MOTTATT_SKATTEOPPGJOER,
                    )
                }

                resultat.skalHaEtteroppgjoer
            }

        dao.lagreKjoering(kjoering)
    }
}

data class HendelseKjoeringRequest(
    val antall: Int,
)
