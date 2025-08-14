package behandling.jobs.uttrekk

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
import java.time.LocalDate
import java.time.YearMonth

enum class UttrekkToggles(
    private val key: String,
) : FeatureToggle {
    UTTREKK_LOEPENDE_YTELSE_ETTER_67("uttrekk-loepende-ytelse-etter-67"),
    ;

    override fun key(): String = key
}

data class LoependeSak(
    val sakId: SakId,
    val foedselMnd: YearMonth,
    val fyller67Mnd: YearMonth,
    val opphoerDato: LocalDate,
)

class UttrekkLoependeYtelseEtter67JobService(
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
        if (!featureToggleService.isEnabled(UttrekkToggles.UTTREKK_LOEPENDE_YTELSE_ETTER_67, false)) {
            logger.info("Jobb for UttrekkLoependeYtelseEtter67 er deakativert")
            return
        }

        val sakerSomSkulleVaertOpphoert = mutableListOf<LoependeSak>()

        val trekkeIFraAar = 67L
        val start = YearMonth.of(2025, 1).minusYears(trekkeIFraAar) // Reformtidspunkt minus 67 år
        val end = YearMonth.from(LocalDate.now().minusYears(trekkeIFraAar)) // Gjeldende måned minus 67 år

        val foedselsmaanederSoekereFylt67 =
            generateSequence(start) { it.plusMonths(1) }
                .takeWhile { it <= end }
                .toList()

        foedselsmaanederSoekereFylt67
            .forEach { foedselsmaaned ->
                logger.info("Sjekker saker hvor bruker fylte 67 år i måned: $foedselsmaaned")
                val sakIder = aldersovergangService.hentSoekereFoedtIEnGittMaaned(foedselsmaaned).map { SakId(it.toLong()) }

                sakIder
                    // .filterNot { sakId -> inTransaction { vilkaarsvurderingDao.hentMigrertYrkesskadefordel(sakId) } }
                    .forEach { sakId ->
                        try {
                            val sak = inTransaction { sakService.finnSak(sakId) }

                            // Ser kun på OMS
                            if (sak?.sakType == SakType.OMSTILLINGSSTOENAD) {
                                val fyller67mnd = foedselsmaaned.plusYears(trekkeIFraAar)
                                val opphoerMnd = fyller67mnd.plusMonths(1).atDay(1)

                                // Sjekker om sak er løpende måneden etter søker ble 67 år
                                val ytelse =
                                    runBlocking {
                                        vedtakKlient.sakHarLopendeVedtakPaaDato(
                                            sakId,
                                            opphoerMnd,
                                            HardkodaSystembruker.uttrekk,
                                        )
                                    }

                                if (ytelse.erLoepende) {
                                    sakerSomSkulleVaertOpphoert.add(LoependeSak(sakId, foedselsmaaned, fyller67mnd, opphoerMnd))
                                }
                            }
                        } catch (e: Exception) {
                            logger.info("Sjekk av riktig opphørt ytelse etter 67 år feilet i sak $sakId", e)
                        }
                    }
            }

        if (sakerSomSkulleVaertOpphoert.isNotEmpty()) {
            sakerSomSkulleVaertOpphoert.forEach { loependeSak ->
                logger.info(
                    "${loependeSak.sakId} hadde løpende OMS pr ${loependeSak.opphoerDato}, men søker har fylt " +
                        "67 år i måned ${loependeSak.fyller67Mnd} og ytelse burde vært opphørt.",
                )
            }
        } else {
            logger.info("Fant ingen saker hvor søker har løpende ytelse etter 67 år")
        }
    }
}
