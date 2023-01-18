package no.nav.etterlatte.fordeler

import io.mockk.coEvery
import io.mockk.mockk
import no.nav.etterlatte.FNR_1
import no.nav.etterlatte.FNR_2
import no.nav.etterlatte.FNR_3
import no.nav.etterlatte.fordeler.FordelerKriterie.AVDOED_ER_IKKE_REGISTRERT_SOM_DOED
import no.nav.etterlatte.fordeler.digdirkrr.KontaktInfo
import no.nav.etterlatte.fordeler.digdirkrr.KontaktinfoKlient
import no.nav.etterlatte.libs.common.person.FamilieRelasjon
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.mockNorskAdresse
import no.nav.etterlatte.mockPerson
import no.nav.etterlatte.pdltjenester.PdlTjenesterKlient
import no.nav.etterlatte.readSoknad
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.OffsetDateTime

internal class FordelerServiceTest {

    private val pdlTjenesterKlient = mockk<PdlTjenesterKlient>()
    private val kontaktinfoKlient = mockk<KontaktinfoKlient>()
    private val fordelerService = FordelerService(FordelerKriterier(kontaktinfoKlient), pdlTjenesterKlient)

    @BeforeEach
    fun init() {
        coEvery { kontaktinfoKlient.hentSpraak(any()) } returns KontaktInfo(null)
    }

    @Test
    fun `skal fordele gyldig soknad til behandling`() {
        val barnFnr = Foedselsnummer.of(FNR_1)
        val avdoedFnr = Foedselsnummer.of(FNR_2)
        val etterlattFnr = Foedselsnummer.of(FNR_3)

        coEvery { pdlTjenesterKlient.hentPerson(match { it.foedselsnummer == barnFnr }) } returns mockPerson(
            bostedsadresse = mockNorskAdresse(),
            familieRelasjon = FamilieRelasjon(
                ansvarligeForeldre = listOf(etterlattFnr, avdoedFnr),
                foreldre = listOf(etterlattFnr, avdoedFnr),
                barn = null
            )
        )

        coEvery { pdlTjenesterKlient.hentPerson(match { it.foedselsnummer == avdoedFnr }) } returns mockPerson(
            doedsdato = LocalDate.parse("2022-01-01"),
            bostedsadresse = mockNorskAdresse()
        )

        coEvery { pdlTjenesterKlient.hentPerson(match { it.foedselsnummer == etterlattFnr }) } returns mockPerson(
            bostedsadresse = mockNorskAdresse(),
            familieRelasjon = FamilieRelasjon(
                ansvarligeForeldre = listOf(etterlattFnr),
                foreldre = null,
                barn = listOf(barnFnr)
            )
        )

        val resultat = fordelerService.sjekkGyldighetForBehandling(fordelerEvent())

        assertTrue(resultat is FordelerResultat.GyldigForBehandling)
    }

    @Test
    fun `skal ikke fordele hendelse som ikke lenger er gyldig`() {
        val resultat = fordelerService.sjekkGyldighetForBehandling(fordelerEvent(OffsetDateTime.now().minusHours(2)))

        assertTrue(resultat is FordelerResultat.UgyldigHendelse)
    }

    @Test
    fun `skal ikke fordele hendelse som ikke oppfyller alle kriterier`() {
        val barnFnr = Foedselsnummer.of(FNR_1)
        val avdoedFnr = Foedselsnummer.of(FNR_2)
        val etterlattFnr = Foedselsnummer.of(FNR_3)

        coEvery { pdlTjenesterKlient.hentPerson(match { it.foedselsnummer == barnFnr }) } returns mockPerson(
            bostedsadresse = mockNorskAdresse(),
            familieRelasjon = FamilieRelasjon(
                ansvarligeForeldre = listOf(etterlattFnr, avdoedFnr),
                foreldre = listOf(etterlattFnr, avdoedFnr),
                barn = null
            )
        )

        coEvery { pdlTjenesterKlient.hentPerson(match { it.foedselsnummer == avdoedFnr }) } returns mockPerson(
            bostedsadresse = mockNorskAdresse()
        )

        coEvery { pdlTjenesterKlient.hentPerson(match { it.foedselsnummer == etterlattFnr }) } returns mockPerson(
            bostedsadresse = mockNorskAdresse(),
            familieRelasjon = FamilieRelasjon(
                ansvarligeForeldre = listOf(etterlattFnr),
                foreldre = null,
                barn = listOf(barnFnr)
            )
        )

        val resultat = fordelerService.sjekkGyldighetForBehandling(fordelerEvent())

        assertTrue(
            resultat is FordelerResultat.IkkeGyldigForBehandling && resultat.ikkeOppfylteKriterier.contains(
                AVDOED_ER_IKKE_REGISTRERT_SOM_DOED
            )
        )
    }

    @Test
    fun `skal feile dersom kall mot pdltjenester feiler`() {
        coEvery { pdlTjenesterKlient.hentPerson(any()) } throws RuntimeException("Noe feilet")

        assertThrows<RuntimeException> { fordelerService.sjekkGyldighetForBehandling(fordelerEvent()) }
    }

    private fun fordelerEvent(hendelseGyldigTil: OffsetDateTime = OffsetDateTime.now().plusDays(1)) = FordelerEvent(
        soeknad = GYLDIG_BARNEPENSJON_SOKNAD,
        hendelseGyldigTil = hendelseGyldigTil
    )

    companion object {
        val GYLDIG_BARNEPENSJON_SOKNAD = readSoknad("/fordeler/soknad_barnepensjon.json")
    }
}