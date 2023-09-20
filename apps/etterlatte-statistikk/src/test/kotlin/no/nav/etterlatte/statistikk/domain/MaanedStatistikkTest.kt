package no.nav.etterlatte.statistikk.domain

import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

class MaanedStatistikkTest {
    @Test
    fun `se at alle saker som har vedtak blir med i maanedsstatistikk`() {
        val rader: List<StoenadRad> =
            listOf(
                stoenadRad(sakId = 123),
                stoenadRad(sakId = 123, vedtakType = VedtakType.ENDRING),
                stoenadRad(sakId = 456),
            )

        val maaned = YearMonth.of(2023, 2)
        val statistikk = MaanedStatistikk(maaned, rader)

        Assertions.assertEquals(statistikk.rader.size, 2)
        Assertions.assertEquals(statistikk.rader.map { it.sakId }.toSet(), setOf(123L, 456L))
    }

    @Test
    fun `sak som er vedtatt og opphørt i samme statistikkmåned er ignorert`() {
        val rader: List<StoenadRad> =
            listOf(
                stoenadRad(sakId = 123, vedtakLoependeFom = LocalDate.of(2023, 2, 1)),
                stoenadRad(sakId = 123, vedtakType = VedtakType.OPPHOER, vedtakLoependeFom = LocalDate.of(2023, 2, 1)),
                stoenadRad(sakId = 456),
            )

        val maaned = YearMonth.of(2023, 2)
        val statistikk = MaanedStatistikk(maaned, rader)

        Assertions.assertEquals(statistikk.rader.size, 1)
        Assertions.assertEquals(statistikk.rader[0].sakId, 456L)
    }

    @Test
    fun `siste gjeldende vedtak er det som hentes data fra`() {
        val rader: List<StoenadRad> =
            listOf(
                stoenadRad(sakId = 123, vedtakLoependeFom = LocalDate.of(2022, 8, 1), nettoYtelse = "10"),
                stoenadRad(sakId = 123, vedtakLoependeFom = LocalDate.of(2023, 1, 1), nettoYtelse = "20"),
            )
        val maaned = YearMonth.of(2023, 2)
        val statistikk = MaanedStatistikk(maaned, rader)
        val statistikkRad = statistikk.rader[0]

        Assertions.assertEquals(statistikkRad.nettoYtelse, "20")
        // TODO nettoYtelse til skal utbedres?
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
    sakId: Long = -1L,
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
    )
