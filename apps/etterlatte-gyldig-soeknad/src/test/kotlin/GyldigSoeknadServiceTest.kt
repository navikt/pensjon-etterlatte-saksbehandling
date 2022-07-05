package behandlingfrasoknad

import Pdl
import com.fasterxml.jackson.module.kotlin.treeToValue
import io.mockk.mockk
import model.GyldigSoeknadService
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsTyper
import no.nav.etterlatte.libs.common.gyldigSoeknad.gyldighetsgrunnlag.InnsenderHarForeldreansvarGrunnlag
import no.nav.etterlatte.libs.common.gyldigSoeknad.gyldighetsgrunnlag.PersonInfoGyldighet
import no.nav.etterlatte.libs.common.gyldigSoeknad.gyldighetsgrunnlagTyper.InnsenderErForelderGrunnlag
import org.junit.jupiter.api.Test
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.Adresse
import no.nav.etterlatte.libs.common.person.Adressebeskyttelse
import no.nav.etterlatte.libs.common.person.FamilieRelasjon
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.person.VergeEllerFullmektig
import no.nav.etterlatte.libs.common.person.VergemaalEllerFremtidsfullmakt
import no.nav.etterlatte.libs.common.vikaar.VurderingsResultat
import org.junit.jupiter.api.Assertions.*
import java.time.LocalDate


internal class GyldigSoeknadServiceTest {

    companion object {
        val pdl = mockk<Pdl>()

        val persongalleri = GyldigSoeknadService(pdl).hentPersongalleriFraSoeknad(
            objectMapper.treeToValue(
                objectMapper.readTree(
                    javaClass.getResource("/fordeltmelding.json")!!.readText()
                )!!["@skjema_info"]
            )!!
        )

        val gjenlevendeFnr = "03108718357"
        val foreldreFnrMedGjenlevende = listOf(Foedselsnummer.of(gjenlevendeFnr), Foedselsnummer.of("22128202440"))
        val foreldreFnrUtenGjenlevende = listOf(Foedselsnummer.of("22128202440"))
        val gjenlevende = listOf(PersonInfoGyldighet("navn navnulfsen", gjenlevendeFnr))
        val innsender = PersonInfoGyldighet("innsendernavn", "03108718357")

    }

    @Test
    fun hentPersongalleriFraSoeknad() {
        assertEquals("12101376212", persongalleri.soeker)
        assertEquals(listOf("03108718357"), persongalleri.gjenlevende)
        assertEquals("03108718357", persongalleri.innsender)
        assertEquals(emptyList<String>(), persongalleri.soesken)
        assertEquals(listOf("22128202440"), persongalleri.avdoed)
    }

    @Test
    fun vurderInnsenderErForelder() {
        val innsenderErForelder = GyldigSoeknadService(pdl).innsenderErForelder(
            GyldighetsTyper.INNSENDER_ER_FORELDER,
            gjenlevende,
            innsender,
            FamilieRelasjon(listOf(), foreldreFnrMedGjenlevende, null)
        )

        val innsenderErIkkeForelder = GyldigSoeknadService(pdl).innsenderErForelder(
            GyldighetsTyper.INNSENDER_ER_FORELDER,
            gjenlevende,
            innsender,
            FamilieRelasjon(listOf(), foreldreFnrUtenGjenlevende, null)
        )

        val foreldreMangler = GyldigSoeknadService(pdl).innsenderErForelder(
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
            ), innsenderErForelder.basertPaaOpplysninger
        )
        assertEquals(VurderingsResultat.IKKE_OPPFYLT, innsenderErIkkeForelder.resultat)
        assertEquals(VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING, foreldreMangler.resultat)

    }

    @Test
    fun vurderInnsenderHarForeldreansvar() {
        val innsenderErForelder = GyldigSoeknadService(pdl).innsenderHarForeldreansvar(
            GyldighetsTyper.INNSENDER_ER_FORELDER,
            innsender,
            FamilieRelasjon(foreldreFnrMedGjenlevende, foreldreFnrMedGjenlevende, null)
        )

        val innsenderErIkkeForelder = GyldigSoeknadService(pdl).innsenderHarForeldreansvar(
            GyldighetsTyper.INNSENDER_ER_FORELDER,
            innsender,
            FamilieRelasjon(foreldreFnrUtenGjenlevende, foreldreFnrUtenGjenlevende, null)
        )

        val foreldreMangler = GyldigSoeknadService(pdl).innsenderHarForeldreansvar(
            GyldighetsTyper.INNSENDER_ER_FORELDER,
            innsender,
            null
        )

        assertEquals(VurderingsResultat.OPPFYLT, innsenderErForelder.resultat)
        assertEquals(
            InnsenderHarForeldreansvarGrunnlag(
                FamilieRelasjon(foreldreFnrMedGjenlevende, foreldreFnrMedGjenlevende, null),
                innsender,
            ), innsenderErForelder.basertPaaOpplysninger
        )
        assertEquals(VurderingsResultat.IKKE_OPPFYLT, innsenderErIkkeForelder.resultat)
        assertEquals(VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING, foreldreMangler.resultat)
    }

    @Test
    fun vurderIngenAnnenVergeEnnForelder() {
        val soekerIngenVerge = lagMockPersonPdl(null)
        val soekerHarVerge = lagMockPersonPdl(
            listOf(
                VergemaalEllerFremtidsfullmakt(
                    "embete",
                    "type",
                    VergeEllerFullmektig(null, null, null, true)
                )
            )
        )

        val harIngenVerge = GyldigSoeknadService(pdl).ingenAnnenVergeEnnForelder(
            GyldighetsTyper.INGEN_ANNEN_VERGE_ENN_FORELDER,
            soekerIngenVerge,
        )

        val harVerge = GyldigSoeknadService(pdl).ingenAnnenVergeEnnForelder(
            GyldighetsTyper.INGEN_ANNEN_VERGE_ENN_FORELDER,
            soekerHarVerge,
        )
        assertEquals(VurderingsResultat.IKKE_OPPFYLT, harVerge.resultat)
        assertEquals(VurderingsResultat.OPPFYLT, harIngenVerge.resultat)
    }

}

fun lagMockPersonPdl(
    vergemaalEllerFremtidsfullmakt: List<VergemaalEllerFremtidsfullmakt>?
) = Person(
    fornavn = "Test",
    etternavn = "Testulfsen",
    foedselsnummer = Foedselsnummer.of("19078504903"),
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
    statsborgerskap = null,
    utland = null,
    familieRelasjon = FamilieRelasjon(null, null, null),
    avdoedesBarn = null,
    vergemaalEllerFremtidsfullmakt = vergemaalEllerFremtidsfullmakt
)