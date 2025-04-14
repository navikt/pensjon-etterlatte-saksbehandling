package no.nav.etterlatte.behandling.jobs.sjekkloependeover20

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.Context
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.behandling.klienter.VedtakKlient
import no.nav.etterlatte.funksjonsbrytere.FeatureToggle
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.grunnlag.aldersovergang.AldersovergangService
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.ktor.token.HardkodaSystembruker
import no.nav.etterlatte.sak.SakService
import org.slf4j.LoggerFactory
import java.time.Month
import java.time.YearMonth

private enum class UttrekkFylt18Toggles(
    private val key: String,
) : FeatureToggle {
    UTTREKK_FYLT_18("uttrekk-fylt-18"),
    ;

    override fun key(): String = key
}

class UttrekkFylt18JobService(
    private val vedtakKlient: VedtakKlient,
    private val sakService: SakService,
    private val aldersovergangService: AldersovergangService,
    private val featureToggleService: FeatureToggleService,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun setupKontekstAndRun(context: Context) {
        Kontekst.set(context)
        run()
    }

    private fun run() {
        if (featureToggleService.isEnabled(UttrekkFylt18Toggles.UTTREKK_FYLT_18, false)) {
            // Henter ut alle fødselsmåneder fra reformtidspunkt og frem til nå, hvor personer har blitt 20 år
            val brukereFylt18 = hentMaaneder()
            val resultat = mutableMapOf<YearMonth, List<SakId>>()

            brukereFylt18
                .forEach { foedselsmaaned ->
                    logger.info("Sjekker de som har blitt 18 år og ble født innenfor følgende måned: $foedselsmaaned")
                    val sakIder =
                        aldersovergangService
                            .hentSoekereFoedtIEnGittMaaned(
                                foedselsmaaned,
                            ).map { SakId(it.toLong()) }
                    val aktuelleSaker = mutableListOf<SakId>()

                    sakIder
                        .forEach { sakId ->
                            try {
                                val sak = inTransaction { sakService.finnSak(sakId) }

                                // Ser kun på Barnepensjon
                                if (sak?.sakType == SakType.BARNEPENSJON) {
                                    val ytelse =
                                        runBlocking {
                                            vedtakKlient.sakHarLopendeVedtakPaaDato(
                                                sakId,
                                                foedselsmaaned.atDay(1),
                                                HardkodaSystembruker.uttrekk,
                                            )
                                        }

                                    if (ytelse.erLoepende) {
                                        aktuelleSaker.add(sakId)
                                    }
                                }
                            } catch (e: Exception) {
                                logger.info("Sjekk av om bruker har fylt 18 feilet i sak $sakId", e)
                            }
                        }

                    resultat.put(foedselsmaaned, aktuelleSaker)
                }

            if (resultat.isNotEmpty()) {
                logger.info("Fant følgende antall saker:")
                resultat.forEach {
                    logger.info("Måned: ${it.key} - Saker: ${it.value.joinToString(", ") { it.toString() }}")
                }
            } else {
                logger.info("Fant ingen saker hvor søker er over 18 år i de aktuelle månedene")
            }
        }
    }

    private fun hentMaaneder(): List<YearMonth> =
        listOf(
            YearMonth.of(2007, Month.JANUARY),
            YearMonth.of(2007, Month.FEBRUARY),
            YearMonth.of(2007, Month.MARCH),
            YearMonth.of(2007, Month.APRIL),
        )
}
