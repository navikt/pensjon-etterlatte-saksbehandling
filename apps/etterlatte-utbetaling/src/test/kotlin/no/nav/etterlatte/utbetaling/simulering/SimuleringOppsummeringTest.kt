package no.nav.etterlatte.utbetaling.simulering

import io.kotest.matchers.shouldBe
import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.utbetaling.common.KlasseType
import no.nav.etterlatte.utbetaling.common.SimulertBeregningsperiode
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class SimuleringOppsummeringTest {
    @Test
    fun `summerer simulering per aar og for hele perioden`() {
        val etterbetalingPerioder =
            listOf(
                periode("2024-01-01", "100", KlasseType.YTEL),
                periode("2024-01-01", "-20", KlasseType.SKAT),
                periode("2024-01-01", "-5", KlasseType.TREK),
                periode("2024-02-01", "-150", KlasseType.YTEL, tilbakefoering = true),
                periode("2025-01-01", "200", KlasseType.YTEL),
                periode("2025-01-01", "-40", KlasseType.SKAT),
                periode("2025-01-01", "10", KlasseType.JUST),
                periode("2025-02-01", "-250", KlasseType.YTEL, tilbakefoering = true),
            )
        val tilbakekrevingPerioder =
            listOf(
                periode("2024-02-01", "20", KlasseType.FEIL),
                periode("2025-02-01", "30", KlasseType.FEIL),
            )

        val oppsummeringer = oppsummerSimulering(etterbetalingPerioder, tilbakekrevingPerioder)

        oppsummeringer.perAar.map { it.aarstall } shouldBe listOf(2025, 2024)
        with(oppsummeringer.perAar.first().oppsummering) {
            with(etterbetaling) {
                brutto shouldBe BigDecimal("-50")
                skatt shouldBe BigDecimal("-40")
                andreTrekk shouldBe BigDecimal("10")
                netto shouldBe BigDecimal("-80")
            }
            with(feilutbetaling!!) {
                brutto shouldBe BigDecimal("250")
                beloepBrukerenSkulleHatt shouldBe BigDecimal("220")
                netto shouldBe BigDecimal("30")
            }
        }
        with(oppsummeringer.perAar.last().oppsummering) {
            with(etterbetaling) {
                brutto shouldBe BigDecimal("-50")
                skatt shouldBe BigDecimal("-20")
                andreTrekk shouldBe BigDecimal("-5")
                netto shouldBe BigDecimal("-75")
            }
            with(feilutbetaling!!) {
                brutto shouldBe BigDecimal("150")
                beloepBrukerenSkulleHatt shouldBe BigDecimal("130")
                netto shouldBe BigDecimal("20")
            }
        }
        with(oppsummeringer.forPerioden) {
            with(etterbetaling) {
                brutto shouldBe BigDecimal("-100")
                skatt shouldBe BigDecimal("-60")
                andreTrekk shouldBe BigDecimal("5")
                netto shouldBe BigDecimal("-155")
            }
            with(feilutbetaling!!) {
                brutto shouldBe BigDecimal("400")
                beloepBrukerenSkulleHatt shouldBe BigDecimal("350")
                netto shouldBe BigDecimal("50")
            }
        }
    }

    @Test
    fun `avgrenser tilbakefoeringer til feilperioder uten aa telle dem flere ganger`() {
        val tilbakefoering = periode("2024-04-01", "-150", KlasseType.YTEL, tilbakefoering = true)
        val etterbetalingPerioder =
            listOf(
                tilbakefoering,
                tilbakefoering.copy(tom = LocalDate.parse("2024-04-15"), beloep = BigDecimal("-100")),
            )
        val tilbakekrevingPerioder =
            listOf(
                periode("2024-04-01", "30", KlasseType.FEIL),
                periode("2024-04-01", "20", KlasseType.FEIL),
            )

        val oppsummeringer = oppsummerSimulering(etterbetalingPerioder, tilbakekrevingPerioder)

        with(requireNotNull(oppsummeringer.forPerioden.feilutbetaling)) {
            brutto shouldBe BigDecimal("150")
            beloepBrukerenSkulleHatt shouldBe BigDecimal("100")
            netto shouldBe BigDecimal("50")
        }
        oppsummeringer.perAar.single().oppsummering shouldBe oppsummeringer.forPerioden
    }

    private fun periode(
        fom: String,
        beloep: String,
        klasseType: KlasseType,
        tilbakefoering: Boolean = false,
    ) = LocalDate.parse(fom).let { start ->
        SimulertBeregningsperiode(
            fom = start,
            tom = start.withDayOfMonth(start.lengthOfMonth()),
            gjelderId = Folkeregisteridentifikator.of("17111357722"),
            utbetalesTilId = "17111357722",
            forfall = start,
            feilkonto = false,
            kodeFaggruppe = "PEN",
            enhet = Enhetsnummer("4819"),
            konto = "4134010",
            behandlingskode = "1",
            beloep = BigDecimal(beloep),
            tilbakefoering = tilbakefoering,
            klassekode = klasseType.name,
            klassekodeBeskrivelse = klasseType.beskrivelse,
            klasseType = klasseType,
        )
    }
}
