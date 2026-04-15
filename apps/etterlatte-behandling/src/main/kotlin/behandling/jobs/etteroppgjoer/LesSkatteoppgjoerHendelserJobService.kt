package no.nav.etterlatte.behandling.jobs.etteroppgjoer

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerService
import no.nav.etterlatte.behandling.etteroppgjoer.HendelseslisteFraSkatt
import no.nav.etterlatte.behandling.etteroppgjoer.SkatteoppgjoerHendelse
import no.nav.etterlatte.behandling.etteroppgjoer.sigrun.HendelseKjoeringRequest
import no.nav.etterlatte.behandling.etteroppgjoer.sigrun.HendelserKjoering
import no.nav.etterlatte.behandling.etteroppgjoer.sigrun.SigrunKlient
import no.nav.etterlatte.behandling.etteroppgjoer.sigrun.SkatteoppgjoerHendelserDao
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.ktor.token.HardkodaSystembruker
import no.nav.etterlatte.sak.SakService
import no.nav.etterlatte.sikkerLogg
import org.slf4j.LoggerFactory
import kotlin.time.DurationUnit
import kotlin.time.measureTimedValue

class LesSkatteoppgjoerHendelserJobService(
    private val dao: SkatteoppgjoerHendelserDao,
    private val sigrunKlient: SigrunKlient,
    private val etteroppgjoerService: EtteroppgjoerService,
    private val sakService: SakService,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun lesOgBehandleHendelser(request: HendelseKjoeringRequest): Int =
        inTransaction {
            val hendelsesliste = lesHendelsesliste(request)

            if (hendelsesliste.hendelser.isNotEmpty()) {
                behandleHendelseListe(hendelsesliste.hendelser)
            }

            hendelsesliste.hendelser.size
        }

    private fun behandleHendelseListe(hendelsesListe: List<SkatteoppgjoerHendelse>) {
        logger.info("Starter å behandle ${hendelsesListe.size} hendelser fra skatt")
        val (antallRelevante, varighet) =
            measureTimedValue {
                val antallRelevanteHendelser =
                    hendelsesListe.count {
                        behandleHendelse(it)
                    }

                dao.lagreKjoering(
                    HendelserKjoering(
                        sisteSekvensnummer = hendelsesListe.last().sekvensnummer,
                        antallHendelser = hendelsesListe.size,
                        antallRelevante = antallRelevanteHendelser,
                        sisteRegistreringstidspunkt = hendelsesListe.last().registreringstidspunkt,
                    ),
                )

                antallRelevanteHendelser
            }

        logger.info(
            "Ferdig å behandle ${hendelsesListe.size} hendelse fra skatt ($antallRelevante relevante) " +
                "tok ${varighet.toString(DurationUnit.SECONDS, 2)}",
        )
    }

    private fun behandleHendelse(hendelse: SkatteoppgjoerHendelse): Boolean {
        logger.info("Behandler hendelse ${hendelse.sekvensnummer}")

        val inntektsaar =
            hendelse.gjelderPeriode?.toInt() ?: run {
                logger.info("${hendelse.sekvensnummer}: har ikke gyldig gjelderPeriode, hopper over")
                return false
            }

        val ident = hendelse.identifikator
        val sak =
            sakService.finnSak(ident, SakType.OMSTILLINGSSTOENAD) ?: run {
                logger.info("${hendelse.sekvensnummer}: fant ingen sak for ident, hopper over")
                return false
            }

        val innvilgetAar = etteroppgjoerService.finnInnvilgedeAarForSak(sak.id, HardkodaSystembruker.etteroppgjoer)
        if (inntektsaar !in innvilgetAar) {
            logger.info(
                "${hendelse.sekvensnummer}: fant ingen innvilgede perioder ($innvilgetAar) i sak for periode $inntektsaar, hopper over",
            )
            return false
        }

        sikkerLogg.info(
            "Behandler hendelse sekvensnummer=${hendelse.sekvensnummer}, ident=$ident, sakId=${sak.id}. " +
                "Hendelse=${hendelse.toJson()}",
        )

        return try {
            opprettEllerOppdaterEtteroppgjoer(hendelse, sak, inntektsaar)
            true
        } catch (e: Exception) {
            throw InternfeilException(
                "Feilet i behandling av hendelse med sekvensnummer: ${hendelse.sekvensnummer}",
                e,
            )
        }
    }

    /**
     * Oppretter eller oppdaterer etteroppgjør for en hendelse som er bekreftet relevant.
     */
    private fun opprettEllerOppdaterEtteroppgjoer(
        hendelse: SkatteoppgjoerHendelse,
        sak: Sak,
        inntektsaar: Int,
    ) {
        val etteroppgjoer =
            etteroppgjoerService.finnEtteroppgjoerForInntektsaar(sak.id, inntektsaar)
                ?: run {
                    logger.info("${hendelse.sekvensnummer}: fant ingen etteroppgjør for sak og inntektsår, oppretter nytt etteroppgjør")
                    etteroppgjoerService.opprettNyttEtteroppgjoer(sak.id, inntektsaar)
                }

        etteroppgjoerService.haandterSkatteoppgjoerMottatt(etteroppgjoer, sak)
    }

    private fun lesHendelsesliste(request: HendelseKjoeringRequest): HendelseslisteFraSkatt {
        val sisteKjoering = dao.hentSisteKjoering()
        return runBlocking {
            sigrunKlient.hentHendelsesliste(
                request.antallHendelser,
                sisteKjoering.nesteSekvensnummer(),
            )
        }
    }
}
