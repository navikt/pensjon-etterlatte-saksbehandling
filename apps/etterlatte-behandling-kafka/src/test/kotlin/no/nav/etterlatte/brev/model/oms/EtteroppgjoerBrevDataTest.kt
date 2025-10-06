package no.nav.etterlatte.brev.model.oms

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.brev.BrevFastInnholdData
import no.nav.etterlatte.brev.BrevVedleggRedigerbarNy
import no.nav.etterlatte.libs.common.beregning.EtteroppgjoerResultatType
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.toJson
import no.nav.pensjon.brevbaker.api.model.Kroner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertInstanceOf
import java.time.Month
import java.time.YearMonth

class EtteroppgjoerBrevDataTest {
    @Test
    fun `tester serialisering og deserialisering av brevFastInnholdData`() {
        val brevData =
            EtteroppgjoerBrevData.Forhaandsvarsel(
                bosattUtland = false,
                norskInntekt = false,
                etteroppgjoersAar = 2024,
                rettsgebyrBeloep = Kroner(1234),
                resultatType = EtteroppgjoerResultatType.ETTERBETALING,
                stoenad = Kroner(123),
                faktiskStoenad = Kroner(123),
                avviksBeloep = Kroner(123),
                grunnlag =
                    EtteroppgjoerBrevGrunnlag(
                        fom = YearMonth.of(2024, Month.JANUARY),
                        tom = YearMonth.of(2024, Month.DECEMBER),
                        innvilgedeMaaneder = 12,
                        loennsinntekt = Kroner(4),
                        naeringsinntekt = Kroner(40),
                        afp = Kroner(400),
                        utlandsinntekt = Kroner(4000),
                        inntekt = Kroner(4444),
                    ),
            )

        val json = brevData.toJson()
        val gjenskapt = objectMapper.readValue<BrevFastInnholdData>(json)
        assertInstanceOf<EtteroppgjoerBrevData.Forhaandsvarsel>(gjenskapt)
    }

    @Test
    fun `tester serialisering og deserialisering av brevVedleggData`() {
        val vedleggData = EtteroppgjoerBrevData.beregningsVedlegg(etteroppgjoersAar = 2024, erVedtak = true)
        val json = vedleggData.toJson()
        val gjenskapt = objectMapper.readValue<BrevVedleggRedigerbarNy>(json)
        assertInstanceOf<EtteroppgjoerBrevData.BeregningsVedleggInnhold>(gjenskapt.data)
    }
}
