package no.nav.etterlatte.gyldigsoeknad.barnepensjon

import io.kotest.matchers.date.beInToday
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.gyldigsoeknad.client.PdlClient
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsTyper.HAR_FORELDREANSVAR_FOR_BARNET
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsTyper.INGEN_ANNEN_VERGE_ENN_FORELDER
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsTyper.INNSENDER_ER_FORELDER
import no.nav.etterlatte.libs.common.gyldigSoeknad.VurderingsResultat
import no.nav.etterlatte.libs.common.gyldigSoeknad.gyldighetsgrunnlag.InnsenderHarForeldreansvarGrunnlag
import no.nav.etterlatte.libs.common.gyldigSoeknad.gyldighetsgrunnlag.PersonInfoGyldighet
import no.nav.etterlatte.libs.common.gyldigSoeknad.gyldighetsgrunnlagTyper.InnsenderErForelderGrunnlag
import no.nav.etterlatte.libs.common.innsendtsoeknad.barnepensjon.Barnepensjon
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.common.person.FamilieRelasjon
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.person.VergeEllerFullmektig
import no.nav.etterlatte.libs.common.person.VergemaalEllerFremtidsfullmakt
import no.nav.etterlatte.libs.testdata.grunnlag.AVDOED_FOEDSELSNUMMER
import no.nav.etterlatte.libs.testdata.grunnlag.GJENLEVENDE_FOEDSELSNUMMER
import no.nav.etterlatte.libs.testdata.grunnlag.SOEKER_FOEDSELSNUMMER
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

        assertEquals("25478323363", persongalleri.soeker)
        assertEquals(listOf("01498344336"), persongalleri.gjenlevende)
        assertEquals("01498344336", persongalleri.innsender)
        assertEquals(emptyList<String>(), persongalleri.soesken)
        assertEquals(listOf("08498224343"), persongalleri.avdoed)
    }

    @Test
    fun vurderInnsenderErForelder() {
        val innsenderErForelder =
            gyldigSoeknadService.innsenderErForelder(
                INNSENDER_ER_FORELDER,
                gjenlevende,
                innsender,
                FamilieRelasjon(listOf(), foreldreFnrMedGjenlevende, null),
            )

        val innsenderErIkkeForelder =
            gyldigSoeknadService.innsenderErForelder(
                INNSENDER_ER_FORELDER,
                gjenlevende,
                innsender,
                FamilieRelasjon(listOf(), foreldreFnrUtenGjenlevende, null),
            )

        val foreldreMangler =
            gyldigSoeknadService.innsenderErForelder(
                INNSENDER_ER_FORELDER,
                gjenlevende,
                innsender,
                null,
            )

        assertEquals(VurderingsResultat.OPPFYLT, innsenderErForelder.resultat)
        assertEquals(
            InnsenderErForelderGrunnlag(
                FamilieRelasjon(listOf(), foreldreFnrMedGjenlevende, null),
                innsender,
                gjenlevende,
            ),
            innsenderErForelder.basertPaaOpplysninger,
        )
        assertEquals(VurderingsResultat.IKKE_OPPFYLT, innsenderErIkkeForelder.resultat)
        assertEquals(VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING, foreldreMangler.resultat)
    }

    @Test
    fun vurderInnsenderHarForeldreansvar() {
        val innsenderErForelder =
            GyldigSoeknadService(pdlClient).innsenderHarForeldreansvar(
                INNSENDER_ER_FORELDER,
                innsender,
                FamilieRelasjon(foreldreFnrMedGjenlevende, foreldreFnrMedGjenlevende, null),
            )

        val innsenderErIkkeForelder =
            GyldigSoeknadService(pdlClient).innsenderHarForeldreansvar(
                INNSENDER_ER_FORELDER,
                innsender,
                FamilieRelasjon(foreldreFnrUtenGjenlevende, foreldreFnrUtenGjenlevende, null),
            )

        val foreldreMangler =
            GyldigSoeknadService(pdlClient).innsenderHarForeldreansvar(
                INNSENDER_ER_FORELDER,
                innsender,
                null,
            )

        assertEquals(VurderingsResultat.OPPFYLT, innsenderErForelder.resultat)
        assertEquals(
            InnsenderHarForeldreansvarGrunnlag(
                FamilieRelasjon(foreldreFnrMedGjenlevende, foreldreFnrMedGjenlevende, null),
                innsender,
            ),
            innsenderErForelder.basertPaaOpplysninger,
        )
        assertEquals(VurderingsResultat.IKKE_OPPFYLT, innsenderErIkkeForelder.resultat)
        assertEquals(VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING, foreldreMangler.resultat)
    }

    @Test
    fun vurderIngenAnnenVergeEnnForelder() {
        val soekerIngenVerge = mockPerson(null)
        val soekerHarVerge =
            mockPerson(
                listOf(
                    VergemaalEllerFremtidsfullmakt(
                        "embete",
                        "type",
                        VergeEllerFullmektig(null, null, null, true),
                    ),
                ),
            )

        val harIngenVerge =
            GyldigSoeknadService(pdlClient).ingenAnnenVergeEnnForelder(
                INGEN_ANNEN_VERGE_ENN_FORELDER,
                soekerIngenVerge,
            )

        val harVerge =
            GyldigSoeknadService(pdlClient).ingenAnnenVergeEnnForelder(
                INGEN_ANNEN_VERGE_ENN_FORELDER,
                soekerHarVerge,
            )
        assertEquals(VurderingsResultat.IKKE_OPPFYLT, harVerge.resultat)
        assertEquals(VurderingsResultat.OPPFYLT, harIngenVerge.resultat)
    }

    @Test
    fun vurderGyldighet() {
        val persongalleri = gyldigSoeknadService.hentPersongalleriFraSoeknad(soeknad)
        val foreldre =
            persongalleri
                .let { listOf(it.avdoed[0], it.innsender) }
                .map { Folkeregisteridentifikator.of(it) }
        every {
            pdlClient.hentPerson(match { it == soeknad.soeker.foedselsnummer.svar.value }, any(), any())
        } returns
            mockPerson(
                familieRelasjon =
                    FamilieRelasjon(
                        ansvarligeForeldre = emptyList(),
                        foreldre = foreldre,
                        barn = emptyList(),
                    ),
            )
        every {
            pdlClient.hentPerson(match { it == soeknad.innsender.foedselsnummer.svar.value }, any(), any())
        } returns mockPerson()

        val gyldighet = gyldigSoeknadService.vurderGyldighet(persongalleri, SakType.BARNEPENSJON)

        gyldighet.resultat shouldBe VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING
        val vurderinger = gyldighet.vurderinger.associateBy { it.navn }
        vurderinger shouldHaveSize 3
        vurderinger[INNSENDER_ER_FORELDER]?.resultat shouldBe VurderingsResultat.OPPFYLT
        vurderinger[INGEN_ANNEN_VERGE_ENN_FORELDER]!!.resultat shouldBe VurderingsResultat.OPPFYLT
        vurderinger[HAR_FORELDREANSVAR_FOR_BARNET]!!.resultat shouldBe VurderingsResultat.IKKE_OPPFYLT
        gyldighet.vurdertDato should beInToday()
    }

    companion object {
        private val skjemaInfo =
            objectMapper.writeValueAsString(
                objectMapper.readTree(readFile("/fordeltmelding.json")).get("@skjema_info"),
            )
        val soeknad = objectMapper.readValue(skjemaInfo, Barnepensjon::class.java)

        val foreldreFnrMedGjenlevende =
            listOf(
                GJENLEVENDE_FOEDSELSNUMMER,
                AVDOED_FOEDSELSNUMMER,
            )
        val foreldreFnrUtenGjenlevende = listOf(AVDOED_FOEDSELSNUMMER)
        val gjenlevende = listOf(PersonInfoGyldighet("navn navnulfsen", GJENLEVENDE_FOEDSELSNUMMER.value))
        val innsender = PersonInfoGyldighet("innsendernavn", GJENLEVENDE_FOEDSELSNUMMER.value)

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
