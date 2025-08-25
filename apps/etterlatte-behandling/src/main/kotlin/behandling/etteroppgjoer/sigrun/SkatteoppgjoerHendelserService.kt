package no.nav.etterlatte.behandling.etteroppgjoer.sigrun

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerService
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerStatus
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.sak.SakService
import no.nav.etterlatte.sikkerLogg
import org.slf4j.LoggerFactory

class SkatteoppgjoerHendelserService(
    private val dao: SkatteoppgjoerHendelserDao,
    private val sigrunKlient: SigrunKlient,
    private val etteroppgjoerService: EtteroppgjoerService,
    private val sakService: SakService,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun startHendelsesKjoering(
        request: HendelseKjoeringRequest,
        trigger: String? = "manuelt",
    ) {
        logger.info("Starter å behandle ${request.antall} hendelser fra skatt (type: $trigger)")
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
                val ident = hendelse.identifikator
                val inntektsaar = hendelse.gjelderPeriode.toInt()

                val sak = sakService.finnSak(ident, SakType.OMSTILLINGSSTOENAD)
                val etteroppgjoer = sak?.let { etteroppgjoerService.hentEtteroppgjoerForInntektsaar(it.id, inntektsaar) }

                if (etteroppgjoer != null) {
                    if (etteroppgjoer.status in
                        listOf(EtteroppgjoerStatus.VENTER_PAA_SKATTEOPPGJOER, EtteroppgjoerStatus.MOTTATT_SKATTEOPPGJOER)
                    ) {
                        etteroppgjoerService.oppdaterEtteroppgjoerStatus(
                            sak.id,
                            etteroppgjoer.inntektsaar,
                            EtteroppgjoerStatus.MOTTATT_SKATTEOPPGJOER,
                        )
                    } else {
                        logger.error(
                            "Vi har mottatt hendelse fra skatt om nytt skatteoppgjør for sakId=${sak.id}, men det er allerede opprettet et etteroppgjør med status ${etteroppgjoer.status}. Se sikkerlogg for mer informasjon.",
                        )
                        sikkerLogg.error(
                            "Person med fnr=$ident har mottatt ny hendelse fra skatt om nytt skatteoppgjør, men det er allerede opprettet et etteroppgjør med status ${etteroppgjoer.status}. Se sikkerlogg for mer informasjon.",
                        )
                    }
                }

                etteroppgjoer != null // relevant / ikke relevant hendelse
            }

        dao.lagreKjoering(kjoering)
    }
}

data class HendelseKjoeringRequest(
    val antall: Int,
)
