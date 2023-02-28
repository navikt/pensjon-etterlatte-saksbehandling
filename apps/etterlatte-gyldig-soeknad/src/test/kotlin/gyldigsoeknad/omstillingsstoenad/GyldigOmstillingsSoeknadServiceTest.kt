package gyldigsoeknad.omstillingsstoenad

import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.gyldigsoeknad.client.PdlClient
import no.nav.etterlatte.gyldigsoeknad.omstillingsstoenad.GyldigOmstillingsSoeknadService
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsResultat
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsTyper
import no.nav.etterlatte.libs.common.gyldigSoeknad.VurderingsResultat
import no.nav.etterlatte.libs.common.gyldigSoeknad.VurdertGyldighet
import no.nav.etterlatte.libs.common.gyldigSoeknad.gyldighetsgrunnlag.InnsenderErGjenlevende
import no.nav.etterlatte.libs.common.gyldigSoeknad.gyldighetsgrunnlag.PersonInfoGyldighet
import no.nav.etterlatte.libs.common.gyldigSoeknad.gyldighetsgrunnlag.PersonInfoMedSiviltilstand
import no.nav.etterlatte.libs.common.person.Adressebeskyttelse
import no.nav.etterlatte.libs.common.person.FamilieRelasjon
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.person.Sivilstatus
import no.nav.etterlatte.libs.common.person.Siviltilstand
import no.nav.etterlatte.libs.common.tidspunkt.standardTidssoneUTC
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GyldigOmstillingsSoeknadServiceTest {

    private val pdlClient = mockk<PdlClient>()
    private val clock = Clock.fixed(Instant.now(), standardTidssoneUTC)
    private val service = GyldigOmstillingsSoeknadService(pdlClient, clock)

    @BeforeAll
    fun setup() {
        every { pdlClient.hentPerson(innsenderFnr, PersonRolle.GJENLEVENDE) } returns innsender
        every { pdlClient.hentPerson(avdoedFnr, PersonRolle.AVDOED) } returns avdoed
    }

    @Test
    fun `Skal lage vurdering med gyldig hvis avdoed og innsender har tidligere sivilstatus sammen fra PDL`() {
        val vurdering = service.vurderGyldighet(innsenderFnr, listOf(avdoedFnr))

        val forventaVurdering = VurdertGyldighet(
            navn = GyldighetsTyper.INNSENDER_ER_GJENLEVENDE,
            resultat = VurderingsResultat.OPPFYLT,
            basertPaaOpplysninger = InnsenderErGjenlevende(
                innsender = PersonInfoGyldighet(navn = "Linda Lee", fnr = innsenderFnr),
                avdoed = listOf(
                    PersonInfoMedSiviltilstand(
                        personInfo = PersonInfoGyldighet(navn = "Bruce Lee", fnr = avdoedFnr),
                        siviltilstand = listOf(
                            Siviltilstand(
                                Sivilstatus.GIFT,
                                relatertVedSiviltilstand = Foedselsnummer.of(innsenderFnr)
                            )
                        )
                    )
                )
            )
        )
        val forventetResult = GyldighetsResultat(
            resultat = VurderingsResultat.OPPFYLT,
            vurderinger = listOf(forventaVurdering),
            vurdertDato = LocalDateTime.ofInstant(clock.instant(), standardTidssoneUTC)
        )
        assertEquals(forventetResult, vurdering)
    }

    @Test
    fun `Skal lage vurdering med ikke oppfylt hvis avdoed og innsender ikke har en tidligere sivilstatus sammen`() {
        every { pdlClient.hentPerson(fremmedFnr, PersonRolle.GJENLEVENDE) } returns fremmed

        val vurdering = service.vurderGyldighet(fremmedFnr, listOf(avdoedFnr))

        val forventaVurdering = VurdertGyldighet(
            navn = GyldighetsTyper.INNSENDER_ER_GJENLEVENDE,
            resultat = VurderingsResultat.IKKE_OPPFYLT,
            basertPaaOpplysninger = InnsenderErGjenlevende(
                innsender = PersonInfoGyldighet(navn = "Fremmed Lee", fnr = fremmedFnr),
                avdoed = listOf(
                    PersonInfoMedSiviltilstand(
                        personInfo = PersonInfoGyldighet(navn = "Bruce Lee", fnr = avdoedFnr),
                        siviltilstand = listOf(
                            Siviltilstand(
                                Sivilstatus.GIFT,
                                relatertVedSiviltilstand = Foedselsnummer.of(innsenderFnr)
                            )
                        )
                    )
                )
            )
        )
        val forventetResult = GyldighetsResultat(
            resultat = VurderingsResultat.IKKE_OPPFYLT,
            vurderinger = listOf(forventaVurdering),
            vurdertDato = LocalDateTime.ofInstant(clock.instant(), standardTidssoneUTC)
        )
        assertEquals(forventetResult, vurdering)
    }

    @Test
    fun `Skal lage vurdering med manglende opplysning hvis avdoed mangler opplysninger`() {
        val vurdering = service.vurderGyldighet(innsenderFnr, listOf())

        val forventaVurdering = VurdertGyldighet(
            navn = GyldighetsTyper.INNSENDER_ER_GJENLEVENDE,
            resultat = VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING,
            basertPaaOpplysninger = InnsenderErGjenlevende(
                innsender = PersonInfoGyldighet(navn = "Linda Lee", fnr = innsenderFnr),
                avdoed = listOf()
            )
        )
        val forventetResult = GyldighetsResultat(
            resultat = VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING,
            vurderinger = listOf(forventaVurdering),
            vurdertDato = LocalDateTime.ofInstant(clock.instant(), standardTidssoneUTC)
        )
        assertEquals(forventetResult, vurdering)
    }

    companion object {
        const val innsenderFnr = "19078504903"
        const val avdoedFnr = "26058411891"
        val innsender = mockPerson("Linda", "Lee", innsenderFnr)
        val avdoed = mockPerson(
            "Bruce",
            "Lee",
            avdoedFnr,
            listOf(Siviltilstand(Sivilstatus.GIFT, relatertVedSiviltilstand = Foedselsnummer.of(innsenderFnr)))
        )
        const val fremmedFnr = "19040550081"
        val fremmed = mockPerson("Fremmed", "Lee", fremmedFnr)
    }
}

private fun mockPerson(
    fornavn: String,
    etternavn: String,
    foedselsnummer: String,
    siviltilstand: List<Siviltilstand>? = null
) = Person(
    fornavn = fornavn,
    etternavn = etternavn,
    foedselsnummer = Foedselsnummer.of(foedselsnummer),
    foedselsdato = LocalDate.parse("2020-06-10"),
    foedselsaar = 1985,
    foedeland = null,
    doedsdato = null,
    adressebeskyttelse = Adressebeskyttelse.UGRADERT,
    bostedsadresse = null,
    deltBostedsadresse = null,
    kontaktadresse = null,
    oppholdsadresse = null,
    sivilstatus = null,
    siviltilstand = siviltilstand,
    statsborgerskap = null,
    utland = null,
    familieRelasjon = FamilieRelasjon(null, null, null),
    avdoedesBarn = null,
    vergemaalEllerFremtidsfullmakt = null
)