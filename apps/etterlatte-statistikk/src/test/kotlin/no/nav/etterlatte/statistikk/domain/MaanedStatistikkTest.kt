package no.nav.etterlatte.statistikk.domain

import io.kotest.matchers.shouldBe
import no.nav.etterlatte.behandling.randomSakId
import no.nav.etterlatte.behandling.sakId3
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.util.UUID

class MaanedStatistikkTest {
    @Test
    fun `se at alle saker som har vedtak blir med i maanedsstatistikk`() {
        val sak1 = randomSakId()
        val sak2 = randomSakId()
        val rader: List<StoenadRad> =
            listOf(
                stoenadRad(sakId = sak1),
                stoenadRad(sakId = sak1, vedtakType = VedtakType.ENDRING),
                stoenadRad(sakId = sak2),
            )

        val maaned = YearMonth.of(2023, 2)
        val statistikk = MaanedStatistikk(maaned, rader, emptyMap(), emptyMap())

        Assertions.assertEquals(statistikk.rader.size, 2)
        Assertions.assertEquals(statistikk.rader.map { it.sakId }.toSet(), setOf(sak1, sak2))
    }

    @Test
    fun `sak som er vedtatt og opphørt i samme statistikkmåned er ignorert`() {
        val sak1 = randomSakId()
        val sak2 = randomSakId()
        val rader: List<StoenadRad> =
            listOf(
                stoenadRad(sakId = sak1, vedtakLoependeFom = LocalDate.of(2023, 2, 1)),
                stoenadRad(sakId = sak1, vedtakType = VedtakType.OPPHOER, vedtakLoependeFom = LocalDate.of(2023, 2, 1)),
                stoenadRad(sakId = sak2),
            )

        val maaned = YearMonth.of(2023, 2)
        val statistikk = MaanedStatistikk(maaned, rader, emptyMap(), emptyMap())

        Assertions.assertEquals(statistikk.rader.size, 1)
        Assertions.assertEquals(statistikk.rader[0].sakId, sak2)
    }

    @Test
    fun `siste gjeldende vedtak er det som hentes data fra`() {
        val sak1 = randomSakId()
        val rader: List<StoenadRad> =
            listOf(
                stoenadRad(sakId = sak1, vedtakLoependeFom = LocalDate.of(2022, 8, 1), nettoYtelse = "10"),
                stoenadRad(sakId = sak1, vedtakLoependeFom = LocalDate.of(2023, 1, 1), nettoYtelse = "20"),
            )
        val maaned = YearMonth.of(2023, 2)
        val statistikk = MaanedStatistikk(maaned, rader, emptyMap(), emptyMap())
        val statistikkRad = statistikk.rader[0]

        Assertions.assertEquals(statistikkRad.nettoYtelse, "20")
    }

    @Test
    fun `opphørte vedtak skal ikke bli med i månedsstatistikken`() {
        val rader: List<StoenadRad> =
            listOf(
                stoenadRad(
                    123,
                    vedtakLoependeFom = LocalDate.of(2024, 1, 1),
                    vedtaksperioder =
                        listOf(
                            StoenadUtbetalingsperiode(
                                fraOgMed = YearMonth.of(2024, Month.JANUARY),
                                tilOgMed = null,
                                type = StoenadPeriodeType.UTBETALING,
                                beloep = BigDecimal("1000"),
                            ),
                        ),
                ),
                stoenadRad(
                    123,
                    vedtakLoependeFom = LocalDate.of(2024, 5, 1),
                    opphoerFom = YearMonth.of(2024, Month.JUNE),
                    vedtaksperioder =
                        listOf(
                            StoenadUtbetalingsperiode(
                                type = StoenadPeriodeType.UTBETALING,
                                beloep = BigDecimal("1100"),
                                fraOgMed = YearMonth.of(2024, Month.MAY),
                                tilOgMed = null,
                            ),
                            StoenadUtbetalingsperiode(
                                type = StoenadPeriodeType.OPPHOER,
                                beloep = null,
                                fraOgMed = YearMonth.of(2024, Month.JUNE),
                                tilOgMed = null,
                            ),
                        ),
                ),
            )

        val statistikkMai = MaanedStatistikk(YearMonth.of(2024, Month.MAY), rader, emptyMap(), emptyMap())
        val statisitkkJuni = MaanedStatistikk(YearMonth.of(2024, Month.JUNE), rader, emptyMap(), emptyMap())

        with(statistikkMai.rader) {
            size shouldBe 1
            val rad = get(0)
            rad.vedtakLoependeTom shouldBe YearMonth.of(2024, Month.MAY).atEndOfMonth()
        }
        statisitkkJuni.rader.size shouldBe 0
    }
}

fun stoenadRad(
    id: Long = 0L,
    fnrSoeker: String = "12312312312",
    fnrForeldre: List<String> = listOf("23123123123"),
    fnrSoesken: List<String> = emptyList(),
    anvendtTrygdetid: String = "40",
    nettoYtelse: String = "123",
    beregningType: String = "papir",
    anvendtSats: String = "123123",
    behandlingId: UUID = UUID.randomUUID(),
    sakId: SakId = sakId3,
    sakNummer: Long = -1L,
    tekniskTid: Tidspunkt = Tidspunkt.now(),
    sakYtelse: String = "",
    versjon: String = "",
    saksbehandler: String = "",
    attestant: String? = null,
    vedtakLoependeFom: LocalDate = LocalDate.of(2022, 8, 1),
    vedtakLoependeTom: LocalDate? = null,
    beregning: Beregning? = null,
    avkorting: Avkorting? = null,
    vedtakType: VedtakType = VedtakType.INNVILGELSE,
    sakUtland: SakUtland = SakUtland.NASJONAL,
    virknigstidspunkt: YearMonth = YearMonth.of(2023, 6),
    utbetalingsdato: LocalDate = LocalDate.of(2023, 7, 20),
    kilde: Vedtaksloesning = Vedtaksloesning.GJENNY,
    pesysId: Long = 123L,
    sakYtelsesgruppe: SakYtelsesgruppe? = SakYtelsesgruppe.EN_AVDOED_FORELDER,
    opphoerFom: YearMonth? = null,
    vedtaksperioder: List<StoenadUtbetalingsperiode>? = null,
): StoenadRad =
    StoenadRad(
        id = id,
        fnrSoeker = fnrSoeker,
        fnrForeldre = fnrForeldre,
        fnrSoesken = fnrSoesken,
        anvendtTrygdetid = anvendtTrygdetid,
        nettoYtelse = nettoYtelse,
        beregningType = beregningType,
        anvendtSats = anvendtSats,
        behandlingId = behandlingId,
        sakId = sakId,
        sakNummer = sakNummer,
        tekniskTid = tekniskTid,
        sakYtelse = sakYtelse,
        versjon = versjon,
        saksbehandler = saksbehandler,
        attestant = attestant,
        vedtakLoependeFom = vedtakLoependeFom,
        vedtakLoependeTom = vedtakLoependeTom,
        beregning = beregning,
        avkorting = avkorting,
        vedtakType = vedtakType,
        sakUtland = sakUtland,
        virkningstidspunkt = virknigstidspunkt,
        utbetalingsdato = utbetalingsdato,
        vedtaksloesning = kilde,
        pesysId = pesysId,
        sakYtelsesgruppe = sakYtelsesgruppe,
        opphoerFom = opphoerFom,
        vedtaksperioder = vedtaksperioder,
    )
