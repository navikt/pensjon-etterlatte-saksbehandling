package no.nav.etterlatte.brev.model

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.etterlatte.brev.behandling.Beregningsperiode
import no.nav.etterlatte.libs.common.beregning.BeregningsMetode
import no.nav.pensjon.brevbaker.api.model.Kroner
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class EtterbetalingBrevTest {
    @Test
    fun `skal kun returnere perioder som er innenfor etterbetalingsdatoer`() {
        val etterbetalingDto =
            EtterbetalingDTO(
                datoFom = LocalDate.of(2023, 1, 1),
                datoTom = LocalDate.of(2023, 3, 31),
            )

        val perioder =
            listOf(
                Beregningsperiode(
                    datoFOM = LocalDate.of(2023, 1, 1),
                    datoTOM = LocalDate.of(2023, 1, 31),
                    grunnbeloep = Kroner(120000),
                    antallBarn = 1,
                    utbetaltBeloep = Kroner(5000),
                    trygdetid = 40,
                    prorataBroek = null,
                    institusjon = false,
                    beregningsMetodeAnvendt = BeregningsMetode.NASJONAL,
                    beregningsMetodeFraGrunnlag = BeregningsMetode.BEST,
                ),
                Beregningsperiode(
                    datoFOM = LocalDate.of(2023, 2, 1),
                    datoTOM = null,
                    grunnbeloep = Kroner(120000),
                    antallBarn = 1,
                    utbetaltBeloep = Kroner(5000),
                    trygdetid = 40,
                    prorataBroek = null,
                    institusjon = false,
                    beregningsMetodeAnvendt = BeregningsMetode.NASJONAL,
                    beregningsMetodeFraGrunnlag = BeregningsMetode.BEST,
                ),
            )

        val etterbetalingBrev = EtterbetalingBrev.fra(etterbetalingDto, perioder)

        etterbetalingBrev shouldNotBe null
        etterbetalingBrev?.fraDato shouldBe etterbetalingDto.datoFom
        etterbetalingBrev?.tilDato shouldBe etterbetalingDto.datoTom
        etterbetalingBrev?.etterbetalingsperioder?.get(0)?.datoFOM shouldBe LocalDate.of(2023, 2, 1)
        etterbetalingBrev?.etterbetalingsperioder?.get(0)?.datoTOM shouldBe LocalDate.of(2023, 3, 31)
        etterbetalingBrev?.etterbetalingsperioder?.get(1)?.datoFOM shouldBe LocalDate.of(2023, 1, 1)
        etterbetalingBrev?.etterbetalingsperioder?.get(1)?.datoTOM shouldBe LocalDate.of(2023, 1, 31)
    }
}
