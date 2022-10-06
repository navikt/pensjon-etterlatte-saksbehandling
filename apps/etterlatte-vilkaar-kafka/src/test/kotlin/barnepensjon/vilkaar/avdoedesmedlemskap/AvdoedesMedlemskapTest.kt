package barnepensjon.vilkaar.avdoedesmedlemskap

import GrunnlagTestData
import LesVilkaarsmeldingTest.Companion.readFile
import adresserNorgePdl
import com.fasterxml.jackson.module.kotlin.readValue
import grunnlag.kilde
import no.nav.etterlatte.libs.common.arbeidsforhold.ArbeidsforholdOpplysning
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.Opplysning
import no.nav.etterlatte.libs.common.grunnlag.PeriodisertOpplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.AvdoedesMedlemskapVurdering
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.PeriodeType
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.SaksbehandlerMedlemskapsperiode
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.SaksbehandlerMedlemskapsperioder
import no.nav.etterlatte.libs.common.inntekt.InntektsOpplysning
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.libs.common.vikaar.Kriterietyper
import no.nav.etterlatte.libs.common.vikaar.Utfall
import no.nav.etterlatte.libs.common.vikaar.VilkaarOpplysning
import no.nav.etterlatte.libs.common.vikaar.VurderingsResultat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.util.*

class AvdoedesMedlemskapTest {
    private val testdata = GrunnlagTestData(
        opplysningsmapAvdødOverrides = mapOf(
            Opplysningstyper.KONTAKTADRESSE to Opplysning.Konstant(
                UUID.randomUUID(),
                kilde,
                adresserNorgePdl().toJsonNode()
            ),
            Opplysningstyper.BOSTEDSADRESSE to Opplysning.Periodisert(
                adresserNorgePdl().map {
                    PeriodisertOpplysning(
                        UUID.randomUUID(),
                        kilde,
                        it.toJsonNode(),
                        it.gyldigFraOgMed!!.let { YearMonth.of(it.year, it.month) },
                        it.gyldigTilOgMed?.let { YearMonth.of(it.year, it.month) }
                    )
                }
            ),
            Opplysningstyper.OPPHOLDSADRESSE to Opplysning.Konstant(
                UUID.randomUUID(),
                kilde,
                adresserNorgePdl().toJsonNode()
            ),
            Opplysningstyper.UTENLANDSOPPHOLD to Opplysning.Periodisert(
                emptyList()
            )
        )
    ).hentOpplysningsgrunnlag()

    @Test
    fun `Skal returnere med utfall oppfyllt dersom det ikke er gaps i de gyldige periodene`() {
        val vurdertVilkaar = vilkaarAvdoedesMedlemskap(
            testdata.hentAvdoed(),
            inntekt("inntektsopplysning.json"),
            arbeidsforhold("arbeidsforhold100.json"),
            saksbehandlerOpplysninger
        )

        assertEquals(VurderingsResultat.OPPFYLT, vurdertVilkaar.resultat)
        assertEquals(Utfall.OPPFYLT, vurdertVilkaar.utfall)
        assertEquals(8, vurdertVilkaar.kriterier.size)
        assertEquals(Kriterietyper.AVDOED_OPPFYLLER_MEDLEMSKAP, vurdertVilkaar.kriterier.last().navn)
        assertEquals(VurderingsResultat.OPPFYLT, vurdertVilkaar.kriterier.last().resultat)
        val opplysning =
            vurdertVilkaar.kriterier.last().basertPaaOpplysninger.first().opplysning as AvdoedesMedlemskapVurdering
        assertEquals(0, opplysning.gaps.size)
        assertTrue(opplysning.perioder.isNotEmpty())
        assertTrue(opplysning.gaps.isEmpty())
    }

    @Test()
    @Disabled("Kommentert ut for å komme videre i saksbehandling uten rett testbrukere.")
    fun `Skal returnere med utfall ikke oppfyllt dersom det er gaps i de gyldige periodene`() {
        val vurdertVilkaar = vilkaarAvdoedesMedlemskap(
            testdata.hentAvdoed(),
            inntekt("inntektsopplysningOpphold.json"),
            arbeidsforhold("arbeidsforhold75.json"),
            saksbehandlerOpplysninger
        )

        assertEquals(VurderingsResultat.IKKE_OPPFYLT, vurdertVilkaar.resultat)
        assertEquals(Utfall.BEHANDLE_I_PSYS, vurdertVilkaar.utfall)
        assertEquals(1, vurdertVilkaar.kriterier.size)
        vurdertVilkaar.kriterier.first().let {
            assertEquals(Kriterietyper.AVDOED_OPPFYLLER_MEDLEMSKAP, it.navn)
            assertEquals(VurderingsResultat.IKKE_OPPFYLT, it.resultat)
            val opplysning = it.basertPaaOpplysninger.first().opplysning as AvdoedesMedlemskapVurdering
            assertEquals(2, opplysning.gaps.size)
            assertTrue(opplysning.perioder.isNotEmpty())
        }
    }

    companion object {
        private fun inntekt(file: String) =
            objectMapper.readValue<VilkaarOpplysning<InntektsOpplysning>>(readFile(file))

        private fun arbeidsforhold(file: String) =
            objectMapper.readValue<VilkaarOpplysning<ArbeidsforholdOpplysning>>(readFile(file))

        private val saksbehandlerOpplysninger = VilkaarOpplysning(
            id = UUID.randomUUID(),
            kilde = Grunnlagsopplysning.Vilkaarskomponenten("vilkaar"),
            opplysningType = Opplysningstyper.SAKSBEHANDLER_AVDOED_MEDLEMSKAPS_PERIODE,
            opplysning = SaksbehandlerMedlemskapsperioder(
                listOf(
                    SaksbehandlerMedlemskapsperiode(
                        periodeType = PeriodeType.DAGPENGER,
                        id = UUID.randomUUID().toString(),
                        kilde = Grunnlagsopplysning.Saksbehandler("zid122", Instant.now()),
                        fraDato = LocalDate.of(2021, 1, 1),
                        tilDato = LocalDate.of(2022, 1, 1),
                        stillingsprosent = null,
                        arbeidsgiver = null,
                        begrunnelse = "Sykdom",
                        oppgittKilde = "NAV"
                    ),
                    SaksbehandlerMedlemskapsperiode(
                        periodeType = PeriodeType.ARBEIDSPERIODE,
                        id = UUID.randomUUID().toString(),
                        kilde = Grunnlagsopplysning.Saksbehandler("zid122", Instant.now()),
                        fraDato = LocalDate.of(2021, 1, 1),
                        tilDato = LocalDate.of(2022, 1, 1),
                        stillingsprosent = "70.0",
                        arbeidsgiver = null,
                        begrunnelse = "Annen jobb",
                        oppgittKilde = "NAV"
                    )
                )
            )
        )
    }
}