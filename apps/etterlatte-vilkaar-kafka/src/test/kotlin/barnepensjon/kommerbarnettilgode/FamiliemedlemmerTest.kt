package barnepensjon.kommerbarnettilgode

import LesVilkaarsmelding
import no.nav.etterlatte.barnepensjon.model.VilkaarService
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.Adresse
import no.nav.etterlatte.libs.common.person.AdresseType
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.vikaar.KommerSoekerTilgode
import no.nav.etterlatte.libs.common.vikaar.PersoninfoAvdoed
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

internal class FamiliemedlemmerTest {
    private val rapid = TestRapid()
        .also { LesVilkaarsmelding(it, VilkaarService()) }
        .apply { start() }

    @BeforeEach
    fun leggMeldingPåKø() {
        rapid.sendTestMessage(lesInnTestMelding())
    }

    @Test
    fun `ny melding på køn inneholder mappet familierelasjoner for den avdøde`() {
        val nyMelding = rapid.inspektør.message(0)
        val kommerSoekerTilgode =
            objectMapper.readValue(nyMelding["kommerSoekerTilGode"].toJson(), KommerSoekerTilgode::class.java)

        val expected = PersoninfoAvdoed(
            navn = "VAKKER LAPP",
            fnr = Foedselsnummer.of("22128202440"),
            rolle = PersonRolle.AVDOED,
            bostedadresser = listOf(
                Adresse(
                    type = AdresseType.VEGADRESSE,
                    aktiv = true,
                    coAdresseNavn = null,
                    adresseLinje1 = "Bøveien 937",
                    adresseLinje2 = null,
                    adresseLinje3 = null,
                    postnr = "8475",
                    poststed = null,
                    land = null,
                    kilde = "FREG",
                    gyldigFraOgMed = LocalDateTime.parse("1999-01-01T00:00:00"),
                    gyldigTilOgMed = null
                )
            ),
            doedsdato = LocalDate.parse("2022-02-10"),
            barn = listOf(Foedselsnummer.of("12101376212"))
        )

        assertEquals(expected, kommerSoekerTilgode.familieforhold.avdoed)
    }

    private fun lesInnTestMelding(): String {
        return FamiliemedlemmerTest::class.java.getResource("/melding.json")?.readText()!!
    }
}
