package no.nav.etterlatte.behandling.jobs.etteroppgjoer

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerService
import no.nav.etterlatte.behandling.etteroppgjoer.HendelseslisteFraSkatt
import no.nav.etterlatte.behandling.etteroppgjoer.SkatteoppgjoerHendelse
import no.nav.etterlatte.behandling.etteroppgjoer.sigrun.HendelseKjoeringRequest
import no.nav.etterlatte.behandling.etteroppgjoer.sigrun.HendelserKjoering
import no.nav.etterlatte.behandling.etteroppgjoer.sigrun.SigrunKlient
import no.nav.etterlatte.behandling.etteroppgjoer.sigrun.SkatteoppgjoerHendelserDao
import no.nav.etterlatte.behandling.klienter.VedtakKlient
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.feilhaandtering.krevIkkeNull
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.ktor.token.HardkodaSystembruker
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
    private val vedtakKlient: VedtakKlient,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun lesOgBehandleHendelser(request: HendelseKjoeringRequest): Int {
        val antallLest =
            inTransaction {
                val hendelsesliste = lesHendelsesliste(request)

                if (hendelsesliste.hendelser.isNotEmpty()) {
                    behandleHendelseListe(hendelsesliste.hendelser)
                }

                hendelsesliste.hendelser.size
            }

        return antallLest
    }

    private fun behandleHendelseListe(hendelsesListe: List<SkatteoppgjoerHendelse>) {
        logger.info("Starter å behandle ${hendelsesListe.size} hendelser fra skatt")
        val (antallRelevante, varighet) =
            measureTimedValue {
                val antallRelevante =
                    hendelsesListe.count {
                        behandleHendelse(it)
                    }

                dao.lagreKjoering(
                    HendelserKjoering(
                        sisteSekvensnummer = hendelsesListe.last().sekvensnummer,
                        antallHendelser = hendelsesListe.size,
                        antallRelevante = antallRelevante,
                        sisteRegistreringstidspunkt = hendelsesListe.last().registreringstidspunkt,
                    ),
                )

                antallRelevante
            }

        logger.info(
            "Ferdig å behandle ${hendelsesListe.size} hendelse fra skatt ($antallRelevante relevante) " +
                "tok ${varighet.toString(DurationUnit.SECONDS, 2)}",
        )
    }

    private fun behandleHendelse(hendelse: SkatteoppgjoerHendelse): Boolean {
        logger.info("Behandler hendelse ${hendelse.sekvensnummer}")
        return try {
            opprettEllerOppdaterEtteroppgjoer(hendelse)
        } catch (e: Exception) {
            throw InternfeilException(
                "Feilet i behandling av hendelse med sekvensnummer: ${hendelse.sekvensnummer}",
                e,
            )
        }
    }

    /**
     * Behandler hendelse, det vil si oppdaterer status på etteroppgjøret.
     *
     * @return true hvis hendelsen er relevant, dvs. at saken skal ha etteroppgjør.
     */
    private fun opprettEllerOppdaterEtteroppgjoer(hendelse: SkatteoppgjoerHendelse): Boolean {
        val inntektsaar = krevIkkeNull(hendelse.gjelderPeriode?.toInt()) { "Mangler inntektsår" }
        val ident = hendelse.identifikator

        val sak = sakService.finnSak(ident, SakType.OMSTILLINGSSTOENAD) ?: return false

        sikkerLogg.info(
            "Behandler hendelse sekvensnummer=${hendelse.sekvensnummer}, ident=$ident, sakId=${sak.id}. " +
                "Hendelse=${hendelse.toJson()}",
        )

        val harUtbetalingIPeriode =
            runBlocking { vedtakKlient.harSakUtbetalingForInntektsaar(sak.id, inntektsaar, HardkodaSystembruker.etteroppgjoer) }
        if (!harUtbetalingIPeriode) return false

        val etteroppgjoer =
            try {
                etteroppgjoerService.hentEtteroppgjoerForInntektsaar(sak.id, inntektsaar)
            } catch (_: Exception) {
                null
            }

        when (etteroppgjoer) {
            null -> runBlocking { etteroppgjoerService.opprettNyttEtteroppgjoer(sak.id, inntektsaar) }
            else -> etteroppgjoerService.haandterSkatteoppgjoerMottatt(hendelse, etteroppgjoer, sak)
        }

        return true
    }

    private fun lesHendelsesliste(request: HendelseKjoeringRequest): HendelseslisteFraSkatt {
        val sisteKjoering = dao.hentSisteKjoering()
        return runBlocking { sigrunKlient.hentHendelsesliste(request.antallHendelser, sisteKjoering.nesteSekvensnummer()) }
    }
}
