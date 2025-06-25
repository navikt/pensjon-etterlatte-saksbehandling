package no.nav.etterlatte.behandling.jobs.vedtak

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.klienter.VedtakKlient
import no.nav.etterlatte.libs.ktor.token.HardkodaSystembruker
import org.slf4j.LoggerFactory

@OptIn(DelicateCoroutinesApi::class)
class VedtakIverksettelseTidspunktMigreringService(
    private val behandlingService: BehandlingService,
    private val vedtakKlient: VedtakKlient,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun run() {
        newSingleThreadContext("${this::class.simpleName}-thread").use { ctx ->
            Runtime.getRuntime().addShutdownHook(Thread { ctx.close() })
            runBlocking(ctx) {
                logger.info("Starter periodiske jobber for etteroppgjoer")
                startMigreringIverksettelsesTidspunkt()
            }
        }
    }

    private suspend fun startMigreringIverksettelsesTidspunkt() {
        val vedtakListe = vedtakKlient.hentVedtakUtenIverksettelsesTidspunkt(HardkodaSystembruker.uttrekk)
        vedtakListe.forEach { vedtak ->
            logger.info("Migrerte vedtak $vedtak iverksettelsesTidspunkt")
            val hendelse = behandlingService.hentBehandlingHendelseForIverksattVedtak(vedtak.id)
            vedtakKlient.oppdaterIverksattDatoForVedtak(vedtak.id, hendelse.inntruffet!!, HardkodaSystembruker.uttrekk)
        }
    }
}
