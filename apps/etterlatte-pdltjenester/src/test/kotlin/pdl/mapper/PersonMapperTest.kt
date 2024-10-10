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
import no.nav.etterlatte.pdl.PdlFoedested
import no.nav.etterlatte.pdl.PdlFoedselsdato
import no.nav.etterlatte.pdl.PdlHentPerson
import no.nav.etterlatte.pdl.PdlKlient
import no.nav.etterlatte.pdl.PdlMetadata
import no.nav.etterlatte.pdl.PdlNavn
import no.nav.etterlatte.pdl.PdlSivilstand
import no.nav.etterlatte.pdl.PdlSivilstandstype
import no.nav.etterlatte.pdl.PdlStatsborgerskap
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.Month

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
                setupMockToPickFirst(pdlHentPerson)
                coEvery { avklarAdressebeskyttelse(pdlHentPerson.adressebeskyttelse) } returns null
                coEvery { avklarDoedsfall(pdlHentPerson.doedsfall) } returns null
                coEvery { avklarStatsborgerskap(statsborgerskapPdl) } throws Exception("Whoops")
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
    fun `ved 501 for sivilstand fra pps mappes sivilstatus til uavklart`() {
        val pdlSivilstand =
            listOf(
                PdlSivilstand(
                    type = PdlSivilstandstype.ENKE_ELLER_ENKEMANN,
                    gyldigFraOgMed = LocalDate.of(2023, Month.NOVEMBER, 7),
                    relatertVedSivilstand = null,
                    bekreftelsesdato = null,
                    metadata =
                        PdlMetadata(
                            endringer = listOf(),
                            historisk = false,
                            master = "",
                            opplysningsId = "",
                        ),
                ),
                PdlSivilstand(
                    type = PdlSivilstandstype.UGIFT,
                    gyldigFraOgMed = null,
                    relatertVedSivilstand = null,
                    bekreftelsesdato = null,
                    metadata =
                        PdlMetadata(
                            endringer = listOf(),
                            historisk = false,
                            master = "",
                            opplysningsId = "",
                        ),
                ),
            )
        val pdlHentPerson = pdlHentPerson(sivilstand = pdlSivilstand)

        val pdlKlient = mockk<PdlKlient>()
        val ppsKlient =
            mockk<ParallelleSannheterKlient> {
                setupMockToPickFirst(pdlHentPerson)
                coEvery { avklarAdressebeskyttelse(pdlHentPerson.adressebeskyttelse) } returns null
                coEvery { avklarDoedsfall(pdlHentPerson.doedsfall) } returns null
                coEvery {
                    avklarSivilstand(
                        pdlSivilstand,
                        GJENLEVENDE_FOEDSELSNUMMER,
                    )
                } throws ParallelleSannheterException("Kunne ikke avklare sivlstand", HttpStatusCode.NotImplemented)
            }

        val person =
            PersonMapper.mapPerson(
                ppsKlient = ppsKlient,
                pdlKlient = pdlKlient,
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
                    metadata = pdlmetadata(),
                ),
                PdlStatsborgerskap("Sverige", fomSverige, tomSverige, pdlmetadata()),
            )
        val pdlHentPerson = pdlHentPerson(statsborgerskap = statsborgerskapPdl)

        val ppsKlient =
            mockk<ParallelleSannheterKlient> {
                setupMockToPickFirst(pdlHentPerson)
                coEvery { avklarAdressebeskyttelse(pdlHentPerson.adressebeskyttelse) } returns null
                coEvery { avklarStatsborgerskap(statsborgerskapPdl) } returns statsborgerskapPdl.first()
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
                setupMockToPickFirst(pdlHentPerson)
                coEvery { avklarAdressebeskyttelse(pdlHentPerson.adressebeskyttelse) } returns null
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
    foedsel: List<PdlFoedselsdato> =
        listOf(
            PdlFoedselsdato(
                foedselsdato = LocalDate.of(1990, 1, 1),
                foedselsaar = 1990,
                metadata = pdlmetadata(),
            ),
        ),
    foedested: List<PdlFoedested> = emptyList(),
    statsborgerskap: List<PdlStatsborgerskap>? = null,
    sivilstand: List<PdlSivilstand>? = null,
): PdlHentPerson =
    PdlHentPerson(
        adressebeskyttelse = listOf(),
        navn = navn,
        foedselsdato = foedsel,
        foedested = foedested,
        sivilstand = sivilstand,
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

private fun ParallelleSannheterKlient.setupMockToPickFirst(pdlHentPerson: PdlHentPerson) {
    coEvery { avklarNavn(pdlHentPerson.navn) } returns pdlHentPerson.navn.first()
    coEvery { avklarFoedselsdato(pdlHentPerson.foedselsdato) } returns pdlHentPerson.foedselsdato.first()
    coEvery { avklarFoedested(pdlHentPerson.foedested) } returns pdlHentPerson.foedested.firstOrNull()
}
