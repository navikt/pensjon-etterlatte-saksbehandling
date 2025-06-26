package no.nav.etterlatte.behandling.jobs.vedtak

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.klienter.VedtakKlient
import no.nav.etterlatte.funksjonsbrytere.FeatureToggle
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.ktor.token.HardkodaSystembruker
import org.slf4j.LoggerFactory

@OptIn(DelicateCoroutinesApi::class)
class VedtakIverksettelseTidspunktMigreringService(
    private val behandlingService: BehandlingService,
    private val vedtakKlient: VedtakKlient,
    private val featureToggleService: FeatureToggleService,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun run() {
        newSingleThreadContext("${this::class.simpleName}-thread").use { ctx ->
            Runtime.getRuntime().addShutdownHook(Thread { ctx.close() })
            runBlocking(ctx) {
                if (featureToggleService.isEnabled(MigrereToggle.MIGRERE_IVERKSETTELSES_TIDSPUNKT, false)) {
                    logger.info("Starter jobb for å migrere vedtak iverksettelsesTidspunkt")
                    startMigreringIverksettelsesTidspunkt()
                } else {
                    logger.info("Jobb for å migrere vedtak iverksettelsesTidspunkt er deaktivert ")
                }
            }
        }
    }

    private suspend fun startMigreringIverksettelsesTidspunkt() {
        val vedtakListe = vedtakKlient.hentVedtakUtenIverksettelsesTidspunkt(HardkodaSystembruker.uttrekk)

        if (vedtakListe.isEmpty()) {
            logger.info("Fant ingen vedtak å migrere iverksettelsesTidspunkt for")
            return
        }

        vedtakListe.forEach { vedtakId ->
            try {
                logger.info("Starter migrering av iverksettelsesTidspunkt for vedtak $vedtakId")
                val hendelse = behandlingService.hentBehandlingHendelseForIverksattVedtak(vedtakId)
                vedtakKlient.oppdaterIverksattDatoForVedtak(vedtakId, hendelse.inntruffet!!, HardkodaSystembruker.uttrekk)
            } catch (e: Exception) {
                logger.info("Migrering av iverksettelsesTidspunkt for vedtak $vedtakId feilet", e)
            }
        }
    }
}

enum class MigrereToggle(
    private val toggle: String,
) : FeatureToggle {
    MIGRERE_IVERKSETTELSES_TIDSPUNKT("migrere_iverksettelses_tidspunkt"),
    ;

    override fun key(): String = toggle
}
