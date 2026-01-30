package no.nav.etterlatte.behandling.etteroppgjoer.sigrun

import kotlinx.coroutines.runBlocking
import net.logstash.logback.argument.StructuredArguments.kv
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
import kotlin.time.DurationUnit
import kotlin.time.measureTimedValue

class SkatteoppgjoerHendelserService(
    private val dao: SkatteoppgjoerHendelserDao,
    private val sigrunKlient: SigrunKlient,
    private val etteroppgjoerService: EtteroppgjoerService,
    private val sakService: SakService,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun lesOgBehandleHendelser(request: HendelseKjoeringRequest): Int {
        val antallLest =
            inTransaction {
                val hendelsesliste = lesHendelsesliste(request)

                if (!hendelsesliste.hendelser.isEmpty()) {
                    behandleHendelser(hendelsesliste.hendelser, request)
                }
                hendelsesliste.hendelser.size
            }

        return antallLest
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
                "Ferdig å behandle ${hendelsesListe.size} hendelse fra skatt ($antallRelevante relevante) " +
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

        sikkerLogg.info(
            "Behandler hendelse med sekvensnummer=${hendelse.sekvensnummer} for ident=$ident, sakId=${sak?.id}. Hendelse=${hendelse.toJson()}",
        )

        if (etteroppgjoer != null) {
            if (hendelse.hendelsetype == null || hendelse.hendelsetype == SigrunKlient.HENDELSETYPE_NY) {
                oppdaterEtteroppgjoerStatus(etteroppgjoer, hendelse, sak)
            } else {
                logger.warn(
                    """
                    Mottok hendelse av type ${hendelse.hendelsetype} på sak ${sak.id}, 
                    som skal ha etteroppgjør. Hendelsen blir ikke behandlet.
                    Sekvensnummer: ${hendelse.sekvensnummer}, inntektsår: $inntektsaar
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
        if (etteroppgjoer.venterPaaSkatteoppgjoer() || etteroppgjoer.mottattSkatteoppgjoer()) {
            logger.info(
                "Vi har mottatt hendelse ${hendelse.hendelsetype} fra skatt med sekvensnummer=" +
                    "${hendelse.sekvensnummer} om tilgjengelig skatteoppgjør " +
                    "for ${hendelse.gjelderPeriode?.toInt()}, sakId=${sak.id}. " +
                    "Oppdaterer etteroppgjoer med status ${etteroppgjoer.status}.",
            )

            /*
                Vi mottar en hendelse for hver ident, så hvis person har flere identer vil vi få flere hendelser for samme Etteroppgjør.
                Dette er ikke et problem hvis Etteroppgjøret fortsatt har status MOTTATT_SKATTEOPPGJOER
             */
            if (etteroppgjoer.mottattSkatteoppgjoer()) {
                logger.info(
                    "Vi fikk ny hendelse (type=${hendelse.hendelsetype}) om skatteoppgjør i sak ${sak.id}, " +
                        "sekvensnummer: ${hendelse.sekvensnummer}, etter at vi allerede har oppdatert status til " +
                        "MOTTATT_SKATTEOPPJOER. Se sikkerlogg for full hendelse fra skatt",
                )
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
)
