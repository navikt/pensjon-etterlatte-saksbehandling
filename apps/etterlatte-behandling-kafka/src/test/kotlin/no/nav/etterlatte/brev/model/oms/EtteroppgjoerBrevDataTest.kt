package no.nav.etterlatte.brev.model.oms

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.brev.BrevFastInnholdData
import no.nav.etterlatte.libs.common.beregning.EtteroppgjoerResultatType
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.toJson
import no.nav.pensjon.brevbaker.api.model.Kroner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertInstanceOf

class EtteroppgjoerBrevDataTest {
    @Test
    fun `tester serialisering og deserialisering av brevFastInnholdData`() {
        val brevData =
            EtteroppgjoerBrevData.Forhaandsvarsel(
                bosattUtland = false,
                norskInntekt = false,
                etteroppgjoersAar = 2024,
                rettsgebyrBeloep = Kroner(123),
                resultatType = EtteroppgjoerResultatType.ETTERBETALING,
                inntekt = Kroner(123),
                faktiskInntekt = Kroner(123),
                avviksBeloep = Kroner(123),
            )

        val json = brevData.toJson()
        val gjenskapt = objectMapper.readValue<BrevFastInnholdData>(json)
        assertInstanceOf<EtteroppgjoerBrevData.Forhaandsvarsel>(gjenskapt)
    }
}
