package no.nav.etterlatte.pdl.mapper

import io.mockk.coEvery
import io.mockk.mockk
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.testdata.grunnlag.SOEKER_FOEDSELSNUMMER
import no.nav.etterlatte.pdl.ParallelleSannheterKlient
import no.nav.etterlatte.pdl.PdlFoedsel
import no.nav.etterlatte.pdl.PdlHentPerson
import no.nav.etterlatte.pdl.PdlKlient
import no.nav.etterlatte.pdl.PdlMetadata
import no.nav.etterlatte.pdl.PdlNavn
import no.nav.etterlatte.pdl.PdlStatsborgerskap
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate

class PersonMapperTest {
    @Test
    fun `ved feil fra pps avklar statsborgerskap tar vi med pdl-dataene`() {
        val fomNorge = LocalDate.of(2020, 1, 1)
        val tomNorge = LocalDate.of(2030, 1, 1)
        val statNorge =
            PdlStatsborgerskap(
                land = "Norge",
                gyldigFraOgMed = fomNorge,
                gyldigTilOgMed = tomNorge,
                metadata = pdlmetadata(),
            )

        val fomSverige = LocalDate.of(2000, 1, 1)
        val tomSverige = null
        val statSverige = PdlStatsborgerskap("Sverige", fomSverige, tomSverige, pdlmetadata())

        val statsborgerskapPdl: List<PdlStatsborgerskap> = listOf(statNorge, statSverige)
        val pdlHentPerson = pdlHentPerson(statsborgerskap = statsborgerskapPdl)

        val ppsKlient =
            mockk<ParallelleSannheterKlient> {
                coEvery { avklarNavn(pdlHentPerson.navn) } returns pdlHentPerson.navn.first()
                coEvery { avklarAdressebeskyttelse(pdlHentPerson.adressebeskyttelse) } returns null
                coEvery { avklarStatsborgerskap(statsborgerskapPdl) } throws Exception("Whoops")
                coEvery { avklarFoedsel(pdlHentPerson.foedsel) } returns pdlHentPerson.foedsel.first()
                coEvery { avklarDoedsfall(pdlHentPerson.doedsfall) } returns null
            }
        val pdlKlient = mockk<PdlKlient>()

        val person =
            PersonMapper.mapPerson(
                ppsKlient = ppsKlient,
                pdlKlient = pdlKlient,
                fnr = SOEKER_FOEDSELSNUMMER,
                personRolle = PersonRolle.BARN,
                hentPerson = pdlHentPerson(statsborgerskap = statsborgerskapPdl),
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
                    metadata = pdlmetadata(),
                ),
                PdlStatsborgerskap("Sverige", fomSverige, tomSverige, pdlmetadata()),
            )
        val pdlHentPerson = pdlHentPerson(statsborgerskap = statsborgerskapPdl)

        val ppsKlient =
            mockk<ParallelleSannheterKlient> {
                coEvery { avklarNavn(pdlHentPerson.navn) } returns pdlHentPerson.navn.first()
                coEvery { avklarAdressebeskyttelse(pdlHentPerson.adressebeskyttelse) } returns null
                coEvery { avklarStatsborgerskap(statsborgerskapPdl) } returns statsborgerskapPdl.first()
                coEvery { avklarFoedsel(pdlHentPerson.foedsel) } returns pdlHentPerson.foedsel.first()
                coEvery { avklarDoedsfall(pdlHentPerson.doedsfall) } returns null
            }
        val pdlKlient = mockk<PdlKlient>()

        val person =
            PersonMapper.mapPerson(
                ppsKlient = ppsKlient,
                pdlKlient = pdlKlient,
                fnr = SOEKER_FOEDSELSNUMMER,
                personRolle = PersonRolle.BARN,
                hentPerson = pdlHentPerson(statsborgerskap = statsborgerskapPdl),
                saktyper = listOf(SakType.BARNEPENSJON),
            )
        Assertions.assertEquals("Norge", person.statsborgerskap)
        Assertions.assertNull(person.pdlStatsborgerskap)
    }

    @Test
    fun `hvis statsborgerskap er null fra PDL får vi null i begge`() {
        val statsborgerskapPdl: List<PdlStatsborgerskap>? = null
        val pdlHentPerson = pdlHentPerson(statsborgerskap = statsborgerskapPdl)

        val ppsKlient =
            mockk<ParallelleSannheterKlient> {
                coEvery { avklarNavn(pdlHentPerson.navn) } returns pdlHentPerson.navn.first()
                coEvery { avklarAdressebeskyttelse(pdlHentPerson.adressebeskyttelse) } returns null
                coEvery { avklarFoedsel(pdlHentPerson.foedsel) } returns pdlHentPerson.foedsel.first()
                coEvery { avklarDoedsfall(pdlHentPerson.doedsfall) } returns null
            }
        val pdlKlient = mockk<PdlKlient>()

        val person =
            PersonMapper.mapPerson(
                ppsKlient = ppsKlient,
                pdlKlient = pdlKlient,
                fnr = SOEKER_FOEDSELSNUMMER,
                personRolle = PersonRolle.BARN,
                hentPerson = pdlHentPerson(statsborgerskap = statsborgerskapPdl),
                saktyper = listOf(SakType.BARNEPENSJON),
            )
        Assertions.assertNull(person.pdlStatsborgerskap)
        Assertions.assertNull(person.statsborgerskap)
    }
}

fun pdlmetadata(): PdlMetadata = PdlMetadata(endringer = listOf(), historisk = false, master = "", opplysningsId = "")

fun pdlHentPerson(
    navn: List<PdlNavn> = listOf(PdlNavn("fornavn", null, "etternavn", metadata = pdlmetadata())),
    foedsel: List<PdlFoedsel> =
        listOf(
            PdlFoedsel(
                foedselsaar = 1990,
                metadata = pdlmetadata(),
            ),
        ),
    statsborgerskap: List<PdlStatsborgerskap>? = null,
): PdlHentPerson =
    PdlHentPerson(
        adressebeskyttelse = listOf(),
        navn = navn,
        foedsel = foedsel,
        sivilstand = null,
        doedsfall = listOf(),
        bostedsadresse = null,
        deltBostedsadresse = null,
        kontaktadresse = null,
        oppholdsadresse = null,
        innflyttingTilNorge = null,
        statsborgerskap = statsborgerskap,
        utflyttingFraNorge = null,
        foreldreansvar = null,
        forelderBarnRelasjon = null,
        vergemaalEllerFremtidsfullmakt = null,
    )
