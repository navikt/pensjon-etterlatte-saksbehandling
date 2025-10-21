package no.nav.etterlatte.brev.brevbaker

import io.kotest.matchers.shouldBe
import io.mockk.mockk
import no.nav.etterlatte.behandling.randomSakId
import no.nav.etterlatte.brev.BrevData
import no.nav.etterlatte.brev.EtterlatteBrevKode
import no.nav.etterlatte.brev.adresse.Avsender
import no.nav.etterlatte.brev.behandling.Soeker
import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.UkjentVergemaal
import no.nav.etterlatte.libs.common.person.Vergemaal
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.pensjon.brevbaker.api.model.Foedselsnummer
import no.nav.pensjon.brevbaker.api.model.Telefonnummer
import org.junit.jupiter.api.Test

class BrevbakerRequestTest {
    @Test
    fun `skal ikke sette verges navn men barnets navn ved verge`() {
        val request =
            brevbakerRequest(
                soekerOgEventuellVerge =
                    SoekerOgEventuellVerge(
                        Soeker("Sverre", "Solli", "Sand", Foedselsnummer("08498224343"), null, true, true),
                        Vergemaal("Palle Poulsen", Folkeregisteridentifikator.of("09498230323")),
                    ),
            )
        request.felles.annenMottakerNavn shouldBe "Sverre Solli Sand ved verge"
    }

    @Test
    fun `skal ikke sette verges navn hvis verge ikke finner og det er barnepensjon over 18 år`() {
        val request =
            brevbakerRequest(
                sakType = SakType.BARNEPENSJON,
                soekerOgEventuellVerge =
                    SoekerOgEventuellVerge(
                        Soeker("Søker", null, "Søkersen", Foedselsnummer("08498224343"), false, false, false),
                        null,
                    ),
            )
        request.felles.annenMottakerNavn shouldBe null
    }

    @Test
    fun `skal sette verges navn til søkers navn ved verge hvis verge ikke finnes og det er barnepensjon der vi ikke er over 18`() {
        val request =
            brevbakerRequest(
                sakType = SakType.BARNEPENSJON,
                soekerOgEventuellVerge =
                    SoekerOgEventuellVerge(
                        Soeker("Søker", null, "Søkersen", Foedselsnummer("08498224343"), null, true, true),
                        null,
                    ),
            )
        request.felles.annenMottakerNavn shouldBe "Søker Søkersen ved verge"
    }

    @Test
    fun `skal sette verges navn til søkers navn ved verge hvis ukjent vergemaal`() {
        val request =
            brevbakerRequest(
                soekerOgEventuellVerge =
                    SoekerOgEventuellVerge(
                        Soeker("Terje", "André", "Vigen", Foedselsnummer("08498224343"), null, true, true),
                        UkjentVergemaal(),
                    ),
            )
        request.felles.annenMottakerNavn shouldBe "Terje André Vigen ved verge"
    }

    private fun brevbakerRequest(
        brevKode: EtterlatteBrevKode = mockk(),
        brevData: BrevData = mockk(),
        avsender: Avsender = Avsender("", Telefonnummer("123"), "", ""),
        soekerOgEventuellVerge: SoekerOgEventuellVerge = mockk(),
        sakId: SakId = randomSakId(),
        spraak: Spraak = Spraak.NB,
        sakType: SakType = SakType.BARNEPENSJON,
    ) = BrevbakerRequest.fra(
        brevKode = brevKode,
        brevData = brevData,
        avsender = avsender,
        soekerOgEventuellVerge = soekerOgEventuellVerge,
        sakId = sakId,
        spraak = spraak,
        sakType = sakType,
    )
}
