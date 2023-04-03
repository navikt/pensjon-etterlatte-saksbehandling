package no.nav.etterlatte.gyldigsoeknad.barnepensjon

import io.mockk.mockk
import no.nav.etterlatte.gyldigsoeknad.client.PdlClient
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsTyper
import no.nav.etterlatte.libs.common.gyldigSoeknad.VurderingsResultat
import no.nav.etterlatte.libs.common.gyldigSoeknad.gyldighetsgrunnlag.InnsenderHarForeldreansvarGrunnlag
import no.nav.etterlatte.libs.common.gyldigSoeknad.gyldighetsgrunnlag.PersonInfoGyldighet
import no.nav.etterlatte.libs.common.gyldigSoeknad.gyldighetsgrunnlagTyper.InnsenderErForelderGrunnlag
import no.nav.etterlatte.libs.common.innsendtsoeknad.barnepensjon.Barnepensjon
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.pdlhendelse.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.common.person.FamilieRelasjon
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.person.VergeEllerFullmektig
import no.nav.etterlatte.libs.common.person.VergemaalEllerFremtidsfullmakt
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.FileNotFoundException
import java.time.LocalDate

internal class GyldigSoeknadServiceTest {

    private val pdlClient = mockk<PdlClient>()
    private val gyldigSoeknadService = GyldigSoeknadService(pdlClient)

    @Test
    fun `skal hente persongalleri fra søknad`() {
        val persongalleri = gyldigSoeknadService.hentPersongalleriFraSoeknad(soeknad)

        assertEquals("12101376212", persongalleri.soeker)
        assertEquals(listOf("03108718357"), persongalleri.gjenlevende)
        assertEquals("03108718357", persongalleri.innsender)
        assertEquals(emptyList<String>(), persongalleri.soesken)
        assertEquals(listOf("22128202440"), persongalleri.avdoed)
    }

    @Test
    fun vurderInnsenderErForelder() {
        val innsenderErForelder = gyldigSoeknadService.innsenderErForelder(
            GyldighetsTyper.INNSENDER_ER_FORELDER,
            gjenlevende,
            innsender,
            FamilieRelasjon(listOf(), foreldreFnrMedGjenlevende, null)
        )

        val innsenderErIkkeForelder = gyldigSoeknadService.innsenderErForelder(
            GyldighetsTyper.INNSENDER_ER_FORELDER,
            gjenlevende,
            innsender,
            FamilieRelasjon(listOf(), foreldreFnrUtenGjenlevende, null)
        )

        val foreldreMangler = gyldigSoeknadService.innsenderErForelder(
            GyldighetsTyper.INNSENDER_ER_FORELDER,
            gjenlevende,
            innsender,
            null
        )

        assertEquals(VurderingsResultat.OPPFYLT, innsenderErForelder.resultat)
        assertEquals(
            InnsenderErForelderGrunnlag(
                FamilieRelasjon(listOf(), foreldreFnrMedGjenlevende, null),
                innsender,
                gjenlevende
            ),
            innsenderErForelder.basertPaaOpplysninger
        )
        assertEquals(VurderingsResultat.IKKE_OPPFYLT, innsenderErIkkeForelder.resultat)
        assertEquals(VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING, foreldreMangler.resultat)
    }

    @Test
    fun vurderInnsenderHarForeldreansvar() {
        val innsenderErForelder = GyldigSoeknadService(pdlClient).innsenderHarForeldreansvar(
            GyldighetsTyper.INNSENDER_ER_FORELDER,
            innsender,
            FamilieRelasjon(foreldreFnrMedGjenlevende, foreldreFnrMedGjenlevende, null)
        )

        val innsenderErIkkeForelder = GyldigSoeknadService(pdlClient).innsenderHarForeldreansvar(
            GyldighetsTyper.INNSENDER_ER_FORELDER,
            innsender,
            FamilieRelasjon(foreldreFnrUtenGjenlevende, foreldreFnrUtenGjenlevende, null)
        )

        val foreldreMangler = GyldigSoeknadService(pdlClient).innsenderHarForeldreansvar(
            GyldighetsTyper.INNSENDER_ER_FORELDER,
            innsender,
            null
        )

        assertEquals(VurderingsResultat.OPPFYLT, innsenderErForelder.resultat)
        assertEquals(
            InnsenderHarForeldreansvarGrunnlag(
                FamilieRelasjon(foreldreFnrMedGjenlevende, foreldreFnrMedGjenlevende, null),
                innsender
            ),
            innsenderErForelder.basertPaaOpplysninger
        )
        assertEquals(VurderingsResultat.IKKE_OPPFYLT, innsenderErIkkeForelder.resultat)
        assertEquals(VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING, foreldreMangler.resultat)
    }

    @Test
    fun vurderIngenAnnenVergeEnnForelder() {
        val soekerIngenVerge = mockPerson(null)
        val soekerHarVerge = mockPerson(
            listOf(
                VergemaalEllerFremtidsfullmakt(
                    "embete",
                    "type",
                    VergeEllerFullmektig(null, null, null, true)
                )
            )
        )

        val harIngenVerge = GyldigSoeknadService(pdlClient).ingenAnnenVergeEnnForelder(
            GyldighetsTyper.INGEN_ANNEN_VERGE_ENN_FORELDER,
            soekerIngenVerge
        )

        val harVerge = GyldigSoeknadService(pdlClient).ingenAnnenVergeEnnForelder(
            GyldighetsTyper.INGEN_ANNEN_VERGE_ENN_FORELDER,
            soekerHarVerge
        )
        assertEquals(VurderingsResultat.IKKE_OPPFYLT, harVerge.resultat)
        assertEquals(VurderingsResultat.OPPFYLT, harIngenVerge.resultat)
    }

    companion object {
        private val skjemaInfo = objectMapper.writeValueAsString(
            objectMapper.readTree(readFile("/fordeltmelding.json")).get("@skjema_info")
        )
        val soeknad = objectMapper.readValue(skjemaInfo, Barnepensjon::class.java)

        val gjenlevendeFnr = "03108718357"
        val foreldreFnrMedGjenlevende = listOf(
            Folkeregisteridentifikator.of(gjenlevendeFnr),
            Folkeregisteridentifikator.of("22128202440")
        )
        val foreldreFnrUtenGjenlevende = listOf(Folkeregisteridentifikator.of("22128202440"))
        val gjenlevende = listOf(PersonInfoGyldighet("navn navnulfsen", gjenlevendeFnr))
        val innsender = PersonInfoGyldighet("innsendernavn", "03108718357")

        fun readFile(file: String) = Companion::class.java.getResource(file)?.readText()
            ?: throw FileNotFoundException("Fant ikke filen $file")
    }
}

private fun mockPerson(
    vergemaalEllerFremtidsfullmakt: List<VergemaalEllerFremtidsfullmakt>? = null
) = Person(
    fornavn = "Test",
    etternavn = "Testulfsen",
    foedselsnummer = Folkeregisteridentifikator.of("19078504903"),
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
    familieRelasjon = FamilieRelasjon(null, null, null),
    avdoedesBarn = null,
    vergemaalEllerFremtidsfullmakt = vergemaalEllerFremtidsfullmakt
)