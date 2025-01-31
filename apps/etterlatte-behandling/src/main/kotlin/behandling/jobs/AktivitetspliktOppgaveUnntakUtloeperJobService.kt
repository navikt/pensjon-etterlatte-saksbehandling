package no.nav.etterlatte.behandling.jobs

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.aktivitetsplikt.AktivitetspliktDao
import no.nav.etterlatte.behandling.aktivitetsplikt.AktivitetspliktOppgaveToggles
import no.nav.etterlatte.behandling.aktivitetsplikt.AktivitetspliktService
import no.nav.etterlatte.behandling.klienter.VedtakKlient
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.tidspunkt.toTidspunkt
import no.nav.etterlatte.libs.ktor.token.HardkodaSystembruker
import no.nav.etterlatte.oppgave.OppgaveService
import org.slf4j.LoggerFactory
import java.time.LocalDate

class AktivitetspliktOppgaveUnntakUtloeperJobService(
    private val aktivitetspliktDao: AktivitetspliktDao,
    private val aktivitetspliktService: AktivitetspliktService,
    private val oppgaveService: OppgaveService,
    private val vedtakKlient: VedtakKlient,
    private val featureToggleService: FeatureToggleService,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    @OptIn(DelicateCoroutinesApi::class)
    fun run() {
        if (featureToggleService.isEnabled(AktivitetspliktOppgaveToggles.UNNTAK_MED_FRIST, false)) {
            logger.info("Starter jobb for å lage oppfølgingsoppgaver for unntak med frist")
            newSingleThreadContext("aktivitetspliktOppgaveUnntakUtloeperJob").use { ctx ->
                Runtime.getRuntime().addShutdownHook(Thread { ctx.close() })
                runBlocking(ctx) {
                    opprettOppfoelgingsOppgaveForAktivitetspliktUnntakUtloeper()
                }
            }
        } else {
            logger.info("Jobb for å lage oppfølgingsoppgaver for unntak med frist er skrudd av")
        }
    }

    private suspend fun opprettOppfoelgingsOppgaveForAktivitetspliktUnntakUtloeper() {
        val tomDato = LocalDate.now().plusMonths(2)
        val sakIds = aktivitetspliktDao.finnSakerKlarForOppfoelgingsoppgaveVarigUnntakUtloeper(tomDato)
        logger.info("Fant ${sakIds.size} saker som skal sjekkes for oppfølgingsoppgaver unntak utløper")

        sakIds.forEach { sakId ->
            if (!vedtakKlient
                    .sakHarLopendeVedtakPaaDato(
                        sakId,
                        tomDato,
                        HardkodaSystembruker.oppgave,
                    ).erLoepende
            ) {
                logger.info("Lager ikke oppfølgingsoppgave for sak $sakId grunnen ikke løpende på dato $tomDato")
                return@forEach
            }

            val sak = aktivitetspliktService.hentVurderingForSak(sakId)
            val oppgaveReferanser =
                oppgaveService
                    .hentOppgaverForSak(sakId)
                    .filter { it.type == OppgaveType.OPPFOELGING }
                    .map { it.referanse }

            sak.unntak
                .filter { it.tom != null && it.tom <= tomDato }
                .forEach { unntak ->
                    val harOppave = oppgaveReferanser.any { unntak.id.toString() == it }
                    if (harOppave) {
                        return@forEach
                    } else {
                        logger.info("Oppretter oppfølgingsoppgave for sak $sakId for unntak ${unntak.id}")
                        oppgaveService.opprettOppgave(
                            referanse = unntak.id.toString(),
                            sakId = sakId,
                            kilde = OppgaveKilde.SAKSBEHANDLER,
                            type = OppgaveType.OPPFOELGING,
                            merknad = "Unntak ${unntak.unntak.lesbartNavn} utløper",
                            frist =
                                unntak.tom!!
                                    .minusMonths(1)
                                    .atStartOfDay()
                                    .toTidspunkt(),
                            saksbehandler = null,
                            gruppeId = null,
                        )
                    }
                }
        }
    }
}
