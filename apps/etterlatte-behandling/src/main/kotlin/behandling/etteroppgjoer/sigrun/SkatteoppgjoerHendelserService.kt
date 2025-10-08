package no.nav.etterlatte.behandling.etteroppgjoer.sigrun

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Semaphore
import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.etterlatte.Context
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.behandling.etteroppgjoer.Etteroppgjoer
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerService
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerStatus
import no.nav.etterlatte.behandling.etteroppgjoer.HendelseslisteFraSkatt
import no.nav.etterlatte.behandling.etteroppgjoer.SkatteoppgjoerHendelse
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.feilhaandtering.krevIkkeNull
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.toJson
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
    private val lock = Semaphore(1, 0)

    fun setupKontekstAndRun(
        request: HendelseKjoeringRequest,
        context: Context,
    ) {
        if (lock.tryAcquire()) {
            Kontekst.set(context)

            lesOgBehandleHendelser(request)
            lock.release()
        } else {
            logger.info("Jobben kjører allerede, vi gidder ikke starte enda en kjøring")
        }
    }

    fun lesOgBehandleHendelser(request: HendelseKjoeringRequest) {
        logger.info("Starter med å be om ${request.antallHendelser} hendelser fra skatt - ${request.antallKjoeringer} ganger")
        try {
            repeat(request.antallKjoeringer) {
                inTransaction {
                    val hendelsesliste = lesHendelsesliste(request)

                    if (!hendelsesliste.hendelser.isEmpty()) {
                        behandleHendelser(hendelsesliste.hendelser, request)
                    }
                }
                if (request.venteMellomKjoeringer) {
                    Thread.sleep(2000)
                }
            }
            logger.info("Ferdig med å lese ${request.antallKjoeringer} * ${request.antallHendelser} hendelser fra skatt")
        } catch (e: Exception) {
            logger.error("Feilet under aggresiv lesing av hendelser fra skatt", e)
        }
    }

    fun setupContextAndSettSekvensnummerForLesingFraDato(
        dato: LocalDate,
        context: Context,
    ) {
        Kontekst.set(context)
        settSekvensnummerForLesingFraDato(dato)
    }

    fun settSekvensnummerForLesingFraDato(dato: LocalDate) {
        val sekvensnummer = runBlocking { sigrunKlient.hentSekvensnummerForLesingFraDato(dato) }

        inTransaction {
            dao.lagreKjoering(HendelserKjoering(sekvensnummer, 0, 0, null))
        }
    }

    private fun behandleHendelser(
        hendelsesListe: List<SkatteoppgjoerHendelse>,
        request: HendelseKjoeringRequest,
    ) {
        measureTimedValue {
            hendelsesListe
                .count { hendelse ->
                    logger.info("Behandler hendelse ${hendelse.sekvensnummer}")
                    if (hendelse.gjelderPeriode == null) {
                        logger.error("Hendelse med sekvensnummer ${hendelse.sekvensnummer} mangler periode")
                        return@count false
                    }
                    if (hendelse.gjelderPeriode.toInt() != request.etteroppgjoerAar) {
                        logger.info("Hendelse med sekvensnummer ${hendelse.sekvensnummer} har ikke relevant periode")
                        return@count false
                    }
                    try {
                        return@count behandleHendelse(hendelse)
                    } catch (e: Exception) {
                        throw InternfeilException(
                            "Feilet i behandling av hendelse med sekvensnummer: ${hendelse.sekvensnummer}",
                            e,
                        )
                    }
                }.also { antallRelevante ->
                    dao.lagreKjoering(
                        HendelserKjoering(
                            sisteSekvensnummer = hendelsesListe.last().sekvensnummer,
                            antallHendelser = hendelsesListe.size,
                            antallRelevante = antallRelevante,
                            sisteRegistreringstidspunkt = hendelsesListe.last().registreringstidspunkt,
                        ),
                    )
                }
        }.let { (antallRelevante, varighet) ->
            logger.info(
                "Behandling av ${hendelsesListe.size} ($antallRelevante relevante) " +
                    "tok ${varighet.toString(DurationUnit.SECONDS, 2)}",
            )
        }
    }

    /**
     * Behandler hendelse, det vil si oppdaterer status på etteroppgjøret.
     *
     * @return true hvis hendelsen er relevant, dvs. at saken skal ha etteroppgjør.
     */
    private fun behandleHendelse(hendelse: SkatteoppgjoerHendelse): Boolean {
        val inntektsaar = krevIkkeNull(hendelse.gjelderPeriode?.toInt(), { "Mangler inntektsår" })
        val ident = hendelse.identifikator

        val sak = sakService.finnSak(ident, SakType.OMSTILLINGSSTOENAD)
        val etteroppgjoer: Etteroppgjoer? =
            sak?.let { etteroppgjoerService.hentEtteroppgjoerForInntektsaar(it.id, inntektsaar) }

        if (etteroppgjoer != null) {
            if (hendelse.hendelsetype == null || hendelse.hendelsetype == SigrunKlient.HENDELSETYPE_NY) {
                logger.info("Oppdaterer etteroppgjør for sak ${sak.id}, år $inntektsaar")

                oppdaterEtteroppgjoerStatus(etteroppgjoer, hendelse, sak)
            } else {
                logger.warn(
                    """
                    Mottok hendelse av type ${hendelse.hendelsetype} på sak ${sak.id}, 
                    som skal ha etteroppgjør. Sekvensnummer: ${hendelse.sekvensnummer}, 
                    inntektsår: $inntektsaar
                    """.trimIndent(),
                )
            }
        }

        return etteroppgjoer != null
    }

    private fun oppdaterEtteroppgjoerStatus(
        etteroppgjoer: Etteroppgjoer,
        hendelse: SkatteoppgjoerHendelse,
        sak: Sak,
    ) {
        if (etteroppgjoer.status in
            listOf(
                EtteroppgjoerStatus.VENTER_PAA_SKATTEOPPGJOER,
                EtteroppgjoerStatus.MOTTATT_SKATTEOPPGJOER,
            )
        ) {
            logger.info(
                "Vi har mottatt hendelse ${hendelse.hendelsetype} fra skatt med sekvensnummer=" +
                    "${hendelse.sekvensnummer} om tilgjengelig skatteoppgjør " +
                    "for ${hendelse.gjelderPeriode?.toInt()}, sakId=${sak.id}. " +
                    "Oppdaterer etteroppgjoer med status ${etteroppgjoer.status}.",
            )
            if (etteroppgjoer.status == EtteroppgjoerStatus.MOTTATT_SKATTEOPPGJOER) {
                logger.warn(
                    "Vi fikk ny hendelse (type=${hendelse.hendelsetype}) om skatteoppgjør i sak ${sak.id}, " +
                        "sekvensnummer: ${hendelse.sekvensnummer}, etter at vi allerede har oppdatert status til " +
                        "MOTTATT_SKATTEOPPJOER. Se sikkerlogg for full hendelse fra skatt",
                )
                sikkerLogg.info("Full hendelse for dobbel oppdatering fra skatt: ", kv("hendelse", hendelse.toJson()))
            }

            etteroppgjoerService.oppdaterEtteroppgjoerStatus(
                sak.id,
                etteroppgjoer.inntektsaar,
                EtteroppgjoerStatus.MOTTATT_SKATTEOPPGJOER,
            )
        } else {
            logger.error(
                "Vi har mottatt hendelse fra skatt om nytt skatteoppgjør for sakId=${sak.id}, men det er allerede " +
                    "opprettet et etteroppgjør med status ${etteroppgjoer.status}. Se sikkerlogg for mer informasjon.",
            )
            sikkerLogg.error(
                "Person med fnr=${hendelse.identifikator} har mottatt ny hendelse fra skatt om nytt skatteoppgjør, " +
                    "men det er allerede opprettet et etteroppgjør med status ${etteroppgjoer.status}.",
            )
        }
    }

    private fun lesHendelsesliste(request: HendelseKjoeringRequest): HendelseslisteFraSkatt {
        val sisteKjoering = dao.hentSisteKjoering()

        return runBlocking { sigrunKlient.hentHendelsesliste(request.antallHendelser, sisteKjoering.nesteSekvensnummer()) }
    }
}

data class HendelseKjoeringRequest(
    val antallHendelser: Int,
    val etteroppgjoerAar: Int,
    val venteMellomKjoeringer: Boolean,
    val antallKjoeringer: Int,
)

data class HendelserSettSekvensnummerRequest(
    val startdato: LocalDate,
)
