package no.nav.etterlatte.behandling.jobs.sjekkloependeover20

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.Context
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.behandling.klienter.VedtakKlient
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

data class LoependeSak(
    val sakId: SakId,
    val fyller20Mnd: YearMonth,
    val paafoelgendeMnd: LocalDate,
)

class SjekkerLoependeYtelseEtter20AarJobService(
    private val vedtakKlient: VedtakKlient,
    private val sakService: SakService,
    private val aldersovergangService: AldersovergangService,
    private val vilkaarsvurderingDao: VilkaarsvurderingDao,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun setupKontekstAndRun(context: Context) {
        Kontekst.set(context)
        run()
    }

    private fun run() {
        val aktuelleMaaneder = hentAktuelleMaaneder()
        val fortsattLoepende = mutableListOf<LoependeSak>()

        aktuelleMaaneder.forEach { aktuellMnd ->
            logger.info("Henter ut de som er 20 år i $aktuellMnd")
            val sakIder = aldersovergangService.hentSoekereFoedtIEnGittMaaned(aktuellMnd).map { SakId(it.toLong()) }

            sakIder.forEach { sakId ->
                try {
                    val sak = inTransaction { sakService.finnSak(sakId) }
                    if (sak?.sakType == SakType.BARNEPENSJON) {
                        val paafoelgendeMnd = aktuellMnd.plusMonths(1).atDay(1)

                        val loependeYtelse =
                            runBlocking {
                                vedtakKlient.sakHarLopendeVedtakPaaDato(
                                    sakId,
                                    paafoelgendeMnd,
                                    HardkodaSystembruker.uttrekk,
                                )
                            }

                        if (loependeYtelse.erLoepende) {
                            fortsattLoepende.add(LoependeSak(sakId, aktuellMnd, paafoelgendeMnd))
                        }
                    }
                } catch (e: Exception) {
                    logger.info("Sjekk av riktig løpende ytelse etter 20 år feilet i sak $sakId", e)
                }
            }
        }

        fortsattLoepende.forEach { loependeSak ->
            val yrkesskadefordel = inTransaction { vilkaarsvurderingDao.hentMigrertYrkesskadefordel(loependeSak.sakId) }
            if (yrkesskadefordel) {
                logger.info(
                    "${loependeSak.sakId} var løpende pr ${loependeSak.paafoelgendeMnd}, men bruker har fylt 20 år i måned: ${loependeSak.fyller20Mnd}. Har migrert yrkesskadefordel.",
                )
            } else {
                logger.info(
                    "${loependeSak.sakId} var løpende pr ${loependeSak.paafoelgendeMnd}, men bruker har fylt 20 år i måned: ${loependeSak.fyller20Mnd}. Burde antageligvis vært opphørt.",
                )
            }
        }
    }

    // Hent alle perioder fra reformtidspunkt frem til nå
    private fun hentAktuelleMaaneder(): List<YearMonth> {
        val start = YearMonth.of(2004, 1) // Reformtidspunkt minus 20 år
        val end = YearMonth.from(LocalDate.now().minusYears(20)) // Gjeldende måned minus 20 år

        return generateSequence(start) { it.plusMonths(1) }
            .takeWhile { it <= end }
            .toList()
    }
}
