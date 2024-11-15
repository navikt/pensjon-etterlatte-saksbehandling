package no.nav.etterlatte.pdl.mapper

import io.ktor.http.HttpStatusCode
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.person.Sivilstatus
import no.nav.etterlatte.libs.testdata.grunnlag.GJENLEVENDE_FOEDSELSNUMMER
import no.nav.etterlatte.libs.testdata.grunnlag.SOEKER_FOEDSELSNUMMER
import no.nav.etterlatte.pdl.ParallelleSannheterException
import no.nav.etterlatte.pdl.ParallelleSannheterKlient
import no.nav.etterlatte.pdl.PdlHentPerson
import no.nav.etterlatte.pdl.PdlKlient
import no.nav.etterlatte.pdl.PdlSivilstand
import no.nav.etterlatte.pdl.PdlSivilstandstype
import no.nav.etterlatte.pdl.PdlStatsborgerskap
import no.nav.etterlatte.pdlHentPerson
import no.nav.etterlatte.pdlMetadata
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.Month

internal class ParallelleSannheterServiceTest {
    private val pdlKlient = mockk<PdlKlient>()
    private val ppsKlient = mockk<ParallelleSannheterKlient>()

    private val parallelleSannheterService = ParallelleSannheterService(ppsKlient, pdlKlient, mockk())

    @Test
    fun `ved feil fra pps avklar statsborgerskap tar vi med pdl-dataene`() {
        val fomNorge = LocalDate.of(2020, 1, 1)
        val tomNorge = LocalDate.of(2030, 1, 1)
        val statNorge =
            PdlStatsborgerskap(
                land = "Norge",
                gyldigFraOgMed = fomNorge,
                gyldigTilOgMed = tomNorge,
                metadata = pdlMetadata(),
            )

        val fomSverige = LocalDate.of(2000, 1, 1)
        val tomSverige = null
        val statSverige = PdlStatsborgerskap("Sverige", fomSverige, tomSverige, pdlMetadata())

        val statsborgerskapPdl: List<PdlStatsborgerskap> = listOf(statNorge, statSverige)
        val pdlHentPerson = pdlHentPerson(statsborgerskap = statsborgerskapPdl)

        ppsKlient.setupMockToPickFirst(pdlHentPerson)
        coEvery { ppsKlient.avklarAdressebeskyttelse(pdlHentPerson.adressebeskyttelse) } returns null
        coEvery { ppsKlient.avklarDoedsfall(pdlHentPerson.doedsfall) } returns null
        coEvery { ppsKlient.avklarStatsborgerskap(statsborgerskapPdl) } throws Exception("Whoops")

        val person =
            parallelleSannheterService.mapPerson(
                fnr = SOEKER_FOEDSELSNUMMER,
                personRolle = PersonRolle.BARN,
                hentPerson = pdlHentPerson,
                saktyper = listOf(SakType.BARNEPENSJON),
            )
        Assertions.assertNull(person.statsborgerskap)
        Assertions.assertEquals(2, person.pdlStatsborgerskap?.size)
        val mappetStatsborgerskapNorge = person.pdlStatsborgerskap?.find { it.land == "Norge" }!!
        val mappetStatsborgerskapSverige = person.pdlStatsborgerskap?.find { it.land == "Sverige" }!!

        Assertions.assertEquals(statNorge.gyldigFraOgMed, mappetStatsborgerskapNorge.gyldigFraOgMed)
        Assertions.assertEquals(statNorge.gyldigTilOgMed, mappetStatsborgerskapNorge.gyldigTilOgMed)
        Assertions.assertEquals(statSverige.gyldigFraOgMed, mappetStatsborgerskapSverige.gyldigFraOgMed)
        Assertions.assertEquals(statSverige.gyldigTilOgMed, mappetStatsborgerskapSverige.gyldigTilOgMed)
    }

    @Test
    fun `ved 501 for sivilstand fra pps mappes sivilstatus til uavklart`() {
        val pdlSivilstand =
            listOf(
                PdlSivilstand(
                    type = PdlSivilstandstype.ENKE_ELLER_ENKEMANN,
                    gyldigFraOgMed = LocalDate.of(2023, Month.NOVEMBER, 7),
                    relatertVedSivilstand = null,
                    bekreftelsesdato = null,
                    metadata = pdlMetadata(),
                ),
                PdlSivilstand(
                    type = PdlSivilstandstype.UGIFT,
                    gyldigFraOgMed = null,
                    relatertVedSivilstand = null,
                    bekreftelsesdato = null,
                    metadata = pdlMetadata(),
                ),
            )
        val pdlHentPerson = pdlHentPerson(sivilstand = pdlSivilstand)

        ppsKlient.setupMockToPickFirst(pdlHentPerson)
        coEvery { ppsKlient.avklarAdressebeskyttelse(pdlHentPerson.adressebeskyttelse) } returns null
        coEvery { ppsKlient.avklarDoedsfall(pdlHentPerson.doedsfall) } returns null
        coEvery {
            ppsKlient.avklarSivilstand(
                pdlSivilstand,
                GJENLEVENDE_FOEDSELSNUMMER,
            )
        } throws ParallelleSannheterException("Kunne ikke avklare sivlstand", HttpStatusCode.NotImplemented)

        val person =
            parallelleSannheterService.mapPerson(
                fnr = GJENLEVENDE_FOEDSELSNUMMER,
                personRolle = PersonRolle.GJENLEVENDE,
                hentPerson = pdlHentPerson,
                saktyper = listOf(SakType.OMSTILLINGSSTOENAD),
            )

        Assertions.assertEquals(Sivilstatus.UAVKLART_PPS, person.sivilstatus)
    }

    @Test
    fun `ved avklaring av statsborgerskap fyller vi ut kun det`() {
        val fomNorge = LocalDate.of(2020, 1, 1)
        val tomNorge = LocalDate.of(2030, 1, 1)
        val fomSverige = LocalDate.of(2000, 1, 1)
        val tomSverige = null
        val statsborgerskapPdl: List<PdlStatsborgerskap> =
            listOf(
                PdlStatsborgerskap(
                    land = "Norge",
                    gyldigFraOgMed = fomNorge,
                    gyldigTilOgMed = tomNorge,
                    metadata = pdlMetadata(),
                ),
                PdlStatsborgerskap("Sverige", fomSverige, tomSverige, pdlMetadata()),
            )
        val pdlHentPerson = pdlHentPerson(statsborgerskap = statsborgerskapPdl)

        ppsKlient.setupMockToPickFirst(pdlHentPerson)
        coEvery { ppsKlient.avklarAdressebeskyttelse(pdlHentPerson.adressebeskyttelse) } returns null
        coEvery { ppsKlient.avklarStatsborgerskap(statsborgerskapPdl) } returns statsborgerskapPdl.first()
        coEvery { ppsKlient.avklarDoedsfall(pdlHentPerson.doedsfall) } returns null

        val person =
            parallelleSannheterService.mapPerson(
                fnr = SOEKER_FOEDSELSNUMMER,
                personRolle = PersonRolle.BARN,
                hentPerson = pdlHentPerson,
                saktyper = listOf(SakType.BARNEPENSJON),
            )
        Assertions.assertEquals("Norge", person.statsborgerskap)
        Assertions.assertNull(person.pdlStatsborgerskap)
    }

    @Test
    fun `hvis statsborgerskap er null fra PDL får vi null i begge`() {
        val statsborgerskapPdl: List<PdlStatsborgerskap>? = null
        val pdlHentPerson = pdlHentPerson(statsborgerskap = statsborgerskapPdl)

        ppsKlient.setupMockToPickFirst(pdlHentPerson)
        coEvery { ppsKlient.avklarAdressebeskyttelse(pdlHentPerson.adressebeskyttelse) } returns null
        coEvery { ppsKlient.avklarDoedsfall(pdlHentPerson.doedsfall) } returns null

        val person =
            parallelleSannheterService.mapPerson(
                fnr = SOEKER_FOEDSELSNUMMER,
                personRolle = PersonRolle.BARN,
                hentPerson = pdlHentPerson,
                saktyper = listOf(SakType.BARNEPENSJON),
            )
        Assertions.assertNull(person.pdlStatsborgerskap)
        Assertions.assertNull(person.statsborgerskap)
    }
}

private fun ParallelleSannheterKlient.setupMockToPickFirst(pdlHentPerson: PdlHentPerson) {
    coEvery { avklarNavn(pdlHentPerson.navn) } returns pdlHentPerson.navn.first()
    coEvery { avklarFoedselsdato(pdlHentPerson.foedselsdato) } returns pdlHentPerson.foedselsdato.first()
    coEvery { avklarFoedested(pdlHentPerson.foedested) } returns pdlHentPerson.foedested.firstOrNull()
}
