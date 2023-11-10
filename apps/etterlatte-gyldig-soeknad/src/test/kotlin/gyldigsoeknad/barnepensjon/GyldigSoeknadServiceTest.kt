package no.nav.etterlatte.gyldigsoeknad.barnepensjon

import no.nav.etterlatte.libs.common.innsendtsoeknad.barnepensjon.Barnepensjon
import no.nav.etterlatte.libs.common.objectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.FileNotFoundException

internal class GyldigSoeknadServiceTest {
    private val gyldigSoeknadService = GyldigSoeknadService()

    @Test
    fun `skal hente persongalleri fra søknad`() {
        val persongalleri = gyldigSoeknadService.hentPersongalleriFraSoeknad(soeknad)

        assertEquals("25478323363", persongalleri.soeker)
        assertEquals(listOf("01498344336"), persongalleri.gjenlevende)
        assertEquals("01498344336", persongalleri.innsender)
        assertEquals(emptyList<String>(), persongalleri.soesken)
        assertEquals(listOf("08498224343"), persongalleri.avdoed)
    }

    companion object {
        private val skjemaInfo =
            objectMapper.writeValueAsString(
                objectMapper.readTree(readFile("/fordeltmelding.json")).get("@skjema_info"),
            )
        val soeknad = objectMapper.readValue(skjemaInfo, Barnepensjon::class.java)

        fun readFile(file: String) =
            Companion::class.java.getResource(file)?.readText()
                ?: throw FileNotFoundException("Fant ikke filen $file")
    }
}

private fun mockPerson(
    vergemaalEllerFremtidsfullmakt: List<VergemaalEllerFremtidsfullmakt>? = null,
    familieRelasjon: FamilieRelasjon? = FamilieRelasjon(null, null, null),
) = Person(
    fornavn = "Test",
    etternavn = "Testulfsen",
    foedselsnummer = SOEKER_FOEDSELSNUMMER,
    foedselsdato = LocalDate.parse("2020-06-10"),
    foedselsaar = 1985,
    foedeland = null,
    doedsdato = null,
    adressebeskyttelse = AdressebeskyttelseGradering.UGRADERT,
    bostedsadresse = null,
    deltBostedsadresse = null,
    kontaktadresse = null,
    oppholdsadresse = null,
    sivilstatus = null,
    sivilstand = null,
    statsborgerskap = null,
    utland = null,
    familieRelasjon = familieRelasjon,
    avdoedesBarn = null,
    avdoedesBarnUtenIdent = null,
    vergemaalEllerFremtidsfullmakt = vergemaalEllerFremtidsfullmakt,
)
