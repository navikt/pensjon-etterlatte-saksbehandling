package no.nav.etterlatte.behandling.etteroppgjoer.sigrun

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.Context
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerService
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerStatus
import no.nav.etterlatte.behandling.etteroppgjoer.HendelseslisteFraSkatt
import no.nav.etterlatte.behandling.etteroppgjoer.SkatteoppgjoerHendelse
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.sak.SakService
import no.nav.etterlatte.sikkerLogg
import org.slf4j.LoggerFactory
import java.time.LocalDate
import kotlin.time.DurationUnit
import kotlin.time.measureTimedValue

class SkatteoppgjoerHendelserService(
    private val dao: SkatteoppgjoerHendelserDao,
    private val sigrunKlient: SigrunKlient,
    private val etteroppgjoerService: EtteroppgjoerService,
    private val sakService: SakService,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun setupKontekstAndRun(
        request: HendelseKjoeringRequest,
        context: Context,
    ) {
        Kontekst.set(context)

        lesOgBehandleHendelser(request)
    }

    fun lesOgBehandleHendelser(request: HendelseKjoeringRequest) {
        logger.info("Starter med å be om ${request.antall} hendelser fra skatt")

        inTransaction {
            val hendelsesliste = lesHendelsesliste(request)

            if (!hendelsesliste.hendelser.isEmpty()) {
                behandleHendelser(hendelsesliste)
            }
        }
    }

    fun settSekvensnummerForLesingFraDato(dato: LocalDate) {
        val sekvensnummer = runBlocking { sigrunKlient.hentSekvensnummerForLesingFraDato(dato) }

        inTransaction {
            dao.lagreKjoering(HendelserKjoering(sekvensnummer, 0, 0))
        }
    }

    private fun behandleHendelser(hendelsesListe: HendelseslisteFraSkatt) {
        measureTimedValue {
            val antallRelevante =
                hendelsesListe.hendelser.count { hendelse ->
                    try {
                        behandleHendelse(hendelse)
                    } catch (e: Exception) {
                        throw InternfeilException("Feilet i behandling av hendelse med sekvensnummer: ${hendelse.sekvensnummer}", e)
                    }
                }

            dao.lagreKjoering(
                HendelserKjoering(
                    sisteSekvensnummer = hendelsesListe.hendelser.last().sekvensnummer,
                    antallHendelser = hendelsesListe.hendelser.size,
                    antallRelevante = antallRelevante,
                ),
            )
        }.let { (antallRelevante, varighet) ->
            logger.info(
                "Behandling av ${hendelsesListe.hendelser.size} ($antallRelevante relevante) " +
                    "tok ${varighet.toString(DurationUnit.SECONDS, 2)}",
            )
        }
    }

    private fun behandleHendelse(hendelse: SkatteoppgjoerHendelse): Boolean {
        val ident = hendelse.identifikator
        val inntektsaar = hendelse.gjelderPeriode.toInt()

        val sak = sakService.finnSak(ident, SakType.OMSTILLINGSSTOENAD)
        val etteroppgjoer =
            sak?.let { etteroppgjoerService.hentEtteroppgjoerForInntektsaar(it.id, inntektsaar) }

        if (etteroppgjoer != null) {
            if (etteroppgjoer.status in
                listOf(
                    EtteroppgjoerStatus.VENTER_PAA_SKATTEOPPGJOER,
                    EtteroppgjoerStatus.MOTTATT_SKATTEOPPGJOER,
                )
            ) {
                logger.info(
                    "Vi har mottatt hendelse fra skatt om tilgjengelig skatteoppgjør for $inntektsaar, sakId=${sak.id}. Oppdaterer etteroppgjoer med status ${etteroppgjoer.status}.",
                )
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
                    "Person med fnr=$ident har mottatt ny hendelse fra skatt om nytt skatteoppgjør, men det er allerede opprettet et etteroppgjør med status ${etteroppgjoer.status}.",
                )
            }
        }

        val relevantHendelse = etteroppgjoer != null
        return relevantHendelse
    }

    private fun lesHendelsesliste(request: HendelseKjoeringRequest): HendelseslisteFraSkatt {
        val sisteKjoering = dao.hentSisteKjoering()

        return runBlocking { sigrunKlient.hentHendelsesliste(request.antall, sisteKjoering.nesteSekvensnummer()) }
    }
}

data class HendelseKjoeringRequest(
    val antall: Int,
)

data class HendelserSettSekvensnummerRequest(
    val startdato: LocalDate,
)
