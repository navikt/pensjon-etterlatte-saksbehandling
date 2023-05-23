package no.nav.etterlatte.fordeler

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.FNR_1
import no.nav.etterlatte.FNR_2
import no.nav.etterlatte.FNR_3
import no.nav.etterlatte.behandling.BehandlingKlient
import no.nav.etterlatte.fordeler.FordelerKriterie.AVDOED_ER_IKKE_REGISTRERT_SOM_DOED
import no.nav.etterlatte.libs.common.person.FamilieRelasjon
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.mockNorskAdresse
import no.nav.etterlatte.mockPerson
import no.nav.etterlatte.pdltjenester.PdlTjenesterKlient
import no.nav.etterlatte.pdltjenester.PersonFinnesIkkeException
import no.nav.etterlatte.readSoknad
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.OffsetDateTime

internal class FordelerServiceTest {

    private val pdlTjenesterKlient = mockk<PdlTjenesterKlient>()
    private val fordelerRepo = mockk<FordelerRepository>()
    private val behandlingKlient = mockk<BehandlingKlient>()
    private val fordelerService = FordelerService(
        FordelerKriterier(),
        pdlTjenesterKlient,
        fordelerRepo,
        maxFordelingTilGjenny = Long.MAX_VALUE,
        behandlingKlient = behandlingKlient
    )

    @Test
    fun `skal fordele gyldig soknad til behandling`() {
        val barnFnr = Folkeregisteridentifikator.of(FNR_1)
        val avdoedFnr = Folkeregisteridentifikator.of(FNR_2)
        val etterlattFnr = Folkeregisteridentifikator.of(FNR_3)
        every { fordelerRepo.finnFordeling(any()) } returns null
        every { fordelerRepo.lagreFordeling(any()) } returns Unit
        every { fordelerRepo.antallFordeltTil("GJENNY") } returns 0

        coEvery { pdlTjenesterKlient.hentPerson(match { it.foedselsnummer == barnFnr }) } returns mockPerson(
            bostedsadresse = mockNorskAdresse(),
            familieRelasjon = FamilieRelasjon(
                ansvarligeForeldre = listOf(etterlattFnr, avdoedFnr),
                foreldre = listOf(etterlattFnr, avdoedFnr),
                barn = null
            )
        )

        coEvery { pdlTjenesterKlient.hentPerson(match { it.foedselsnummer == avdoedFnr }) } returns mockPerson(
            doedsdato = LocalDate.parse("2023-01-01"),
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
        val barnFnr = Folkeregisteridentifikator.of(FNR_1)
        val avdoedFnr = Folkeregisteridentifikator.of(FNR_2)
        val etterlattFnr = Folkeregisteridentifikator.of(FNR_3)

        every { fordelerRepo.finnFordeling(any()) } returns null
        every { fordelerRepo.lagreFordeling(any()) } returns Unit

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

    @Test
    fun `Er gyldig for behandling om søknad tidligere er fordelt til Gjenny`() {
        every { fordelerRepo.finnFordeling(any()) } returns FordeltRecord(1, "GJENNY", Tidspunkt.now())
        every { fordelerRepo.finnKriterier(any()) } returns emptyList()

        val resultat = fordelerService.sjekkGyldighetForBehandling(fordelerEvent())

        assertTrue(
            resultat is FordelerResultat.GyldigForBehandling
        )
    }

    @Test
    fun `Er ikke gyldig for behandling om søknad tidligere er fordelt til Pesys`() {
        every { fordelerRepo.finnFordeling(any()) } returns FordeltRecord(1, "PESYS", Tidspunkt.now())
        every { fordelerRepo.finnKriterier(any()) } returns emptyList()

        val resultat = fordelerService.sjekkGyldighetForBehandling(fordelerEvent())

        assertTrue(
            resultat is FordelerResultat.IkkeGyldigForBehandling
        )
    }

    @Test
    fun `Skal ikke fordele søknader til GJENNY utover et maksimum antall`() {
        val fordelerService = FordelerService(
            FordelerKriterier(),
            pdlTjenesterKlient,
            fordelerRepo,
            maxFordelingTilGjenny = 10,
            behandlingKlient = behandlingKlient
        )

        val barnFnr = Folkeregisteridentifikator.of(FNR_1)
        val avdoedFnr = Folkeregisteridentifikator.of(FNR_2)
        val etterlattFnr = Folkeregisteridentifikator.of(FNR_3)
        every { fordelerRepo.finnFordeling(any()) } returns null
        every { fordelerRepo.lagreFordeling(any()) } returns Unit
        every { fordelerRepo.antallFordeltTil("GJENNY") } returns 10

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

        assertTrue(resultat is FordelerResultat.IkkeGyldigForBehandling)
    }

    @Test
    fun `returnerer UgyldigHendelse hvis en av barn, avdød, gjenlevende ikke finnes i PDL`() {
        val fordelerService = FordelerService(
            FordelerKriterier(),
            pdlTjenesterKlient,
            fordelerRepo,
            maxFordelingTilGjenny = 10,
            behandlingKlient = behandlingKlient
        )
        every { fordelerRepo.finnFordeling(any()) } returns null
        every { fordelerRepo.lagreFordeling(any()) } returns Unit

        val barnFnr = Folkeregisteridentifikator.of(FNR_1)
        val avdoedFnr = Folkeregisteridentifikator.of(FNR_2)
        val etterlattFnr = Folkeregisteridentifikator.of(FNR_3)

        coEvery { pdlTjenesterKlient.hentPerson(match { it.foedselsnummer == barnFnr }) } returns mockPerson(
            bostedsadresse = mockNorskAdresse(),
            familieRelasjon = FamilieRelasjon(
                ansvarligeForeldre = listOf(etterlattFnr, avdoedFnr),
                foreldre = listOf(etterlattFnr, avdoedFnr),
                barn = null
            )
        )

        coEvery {
            pdlTjenesterKlient.hentPerson(match { it.foedselsnummer == avdoedFnr })
        } throws PersonFinnesIkkeException(
            avdoedFnr
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
        assertTrue(resultat is FordelerResultat.UgyldigHendelse)
    }

    @Test
    fun `kaster feil hvis en av barn, avdød, gjenlevende gir en feilmedling som ikke er PersonFinnesIkkeException`() {
        val fordelerService = FordelerService(
            FordelerKriterier(),
            pdlTjenesterKlient,
            fordelerRepo,
            maxFordelingTilGjenny = 10,
            behandlingKlient = behandlingKlient
        )
        every { fordelerRepo.finnFordeling(any()) } returns null
        every { fordelerRepo.lagreFordeling(any()) } returns Unit

        val barnFnr = Folkeregisteridentifikator.of(FNR_1)
        val avdoedFnr = Folkeregisteridentifikator.of(FNR_2)
        val etterlattFnr = Folkeregisteridentifikator.of(FNR_3)

        coEvery { pdlTjenesterKlient.hentPerson(match { it.foedselsnummer == barnFnr }) } returns mockPerson(
            bostedsadresse = mockNorskAdresse(),
            familieRelasjon = FamilieRelasjon(
                ansvarligeForeldre = listOf(etterlattFnr, avdoedFnr),
                foreldre = listOf(etterlattFnr, avdoedFnr),
                barn = null
            )
        )

        coEvery {
            pdlTjenesterKlient.hentPerson(match { it.foedselsnummer == avdoedFnr })
        } throws IllegalArgumentException("Dette er ugyldig format")

        coEvery { pdlTjenesterKlient.hentPerson(match { it.foedselsnummer == etterlattFnr }) } returns mockPerson(
            bostedsadresse = mockNorskAdresse(),
            familieRelasjon = FamilieRelasjon(
                ansvarligeForeldre = listOf(etterlattFnr),
                foreldre = null,
                barn = listOf(barnFnr)
            )
        )
        assertThrows<Exception> { fordelerService.sjekkGyldighetForBehandling(fordelerEvent()) }
    }

    private fun fordelerEvent(hendelseGyldigTil: OffsetDateTime = OffsetDateTime.now().plusDays(1)) = FordelerEvent(
        soeknadId = 1,
        soeknad = GYLDIG_BARNEPENSJON_SOKNAD,
        hendelseGyldigTil = hendelseGyldigTil
    )

    companion object {
        val GYLDIG_BARNEPENSJON_SOKNAD = readSoknad("/fordeler/soknad_barnepensjon.json")
    }
}