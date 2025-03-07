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
import no.nav.etterlatte.vilkaarsvurdering.dao.VilkaarsvurderingDao
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.YearMonth

enum class UttrekkToggles(
    private val key: String,
) : FeatureToggle {
    UTTREKK_LOEPENDE_YTELSE_ETTER_20("uttrekk-loepende-ytelse-etter-20"),
    ;

    override fun key(): String = key
}

data class LoependeSak(
    val sakId: SakId,
    val foedselMnd: YearMonth,
    val fyller20Mnd: YearMonth,
    val opphoerDato: LocalDate,
)

class UttrekkLoependeYtelseEtter20JobService(
    private val vedtakKlient: VedtakKlient,
    private val sakService: SakService,
    private val aldersovergangService: AldersovergangService,
    private val vilkaarsvurderingDao: VilkaarsvurderingDao,
    private val featureToggleService: FeatureToggleService,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun setupKontekstAndRun(context: Context) {
        Kontekst.set(context)
        run()
    }

    private fun run() {
        if (featureToggleService.isEnabled(UttrekkToggles.UTTREKK_LOEPENDE_YTELSE_ETTER_20, false)) {
            // Henter ut alle fødselsmåneder fra reformtidspunkt og frem til nå, hvor personer har blitt 20 år
            val foedselsmaanederSoekereFylt20 = hentMaanederSoekereFylt20()
            val sakerSomSkulleVaertOpphoert = mutableListOf<LoependeSak>()

            foedselsmaanederSoekereFylt20
                .forEach { foedselsmaaned ->
                    logger.info("Sjekker de som har blitt 20 år og ble født innenfor følgende måned: $foedselsmaaned")
                    val sakIder = aldersovergangService.hentSoekereFoedtIEnGittMaaned(foedselsmaaned).map { SakId(it.toLong()) }

                    sakIder
                        // Ta bort de med yrkesskade-fordel. De skal opphøres først når de er 21 år
                        .filterNot { sakId -> inTransaction { vilkaarsvurderingDao.hentMigrertYrkesskadefordel(sakId) } }
                        .forEach { sakId ->
                            try {
                                val sak = inTransaction { sakService.finnSak(sakId) }

                                // Ser kun på Barnepensjon
                                if (sak?.sakType == SakType.BARNEPENSJON) {
                                    val fyller20Mnd = foedselsmaaned.plusYears(20)
                                    val opphoerMnd = fyller20Mnd.plusMonths(1).atDay(1)

                                    // Sjekker om sak er løpende måneden etter søker ble 20 år
                                    val ytelse =
                                        runBlocking {
                                            vedtakKlient.sakHarLopendeVedtakPaaDato(
                                                sakId,
                                                opphoerMnd,
                                                HardkodaSystembruker.uttrekk,
                                            )
                                        }

                                    if (ytelse.erLoepende) {
                                        sakerSomSkulleVaertOpphoert.add(LoependeSak(sakId, foedselsmaaned, fyller20Mnd, opphoerMnd))
                                    }
                                }
                            } catch (e: Exception) {
                                logger.info("Sjekk av riktig opphørt ytelse etter 20 år feilet i sak $sakId", e)
                            }
                        }
                }

            if (sakerSomSkulleVaertOpphoert.size > 0) {
                sakerSomSkulleVaertOpphoert.forEach { loependeSak ->
                    logger.info(
                        "${loependeSak.sakId} hadde løpende BP pr ${loependeSak.opphoerDato}, men søker har fylt " +
                            "20 år i måned ${loependeSak.fyller20Mnd} og ytelse burde vært opphørt.",
                    )
                }
            } else {
                logger.info("Fant ingen saker hvor søker har løpende ytelse etter 20 år")
            }
        }
    }

    private fun hentMaanederSoekereFylt20(): List<YearMonth> {
        val start = YearMonth.of(2024, 1).minusYears(20) // Reformtidspunkt minus 20 år
        val end = YearMonth.from(LocalDate.now().minusYears(20)) // Gjeldende måned minus 20 år

        return generateSequence(start) { it.plusMonths(1) }
            .takeWhile { it <= end }
            .toList()
    }
}
