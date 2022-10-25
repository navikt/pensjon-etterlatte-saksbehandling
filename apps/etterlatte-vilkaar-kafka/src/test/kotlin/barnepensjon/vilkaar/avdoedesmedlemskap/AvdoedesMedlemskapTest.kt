package barnepensjon.vilkaar.avdoedesmedlemskap

import GrunnlagTestData
import adresserNorgePdl
import grunnlag.arbeidsforholdTestData
import grunnlag.inntektsopplysning
import grunnlag.kilde
import grunnlag.medlemskap
import no.nav.etterlatte.libs.common.grunnlag.Opplysning
import no.nav.etterlatte.libs.common.grunnlag.PeriodisertOpplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.AvdoedesMedlemskapVurdering
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.libs.common.vikaar.Kriterietyper
import no.nav.etterlatte.libs.common.vikaar.Utfall
import no.nav.etterlatte.libs.common.vikaar.VurderingsResultat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.time.YearMonth
import java.util.*

class AvdoedesMedlemskapTest {
    private val avdoedOpplysninger = mutableMapOf(
        Opplysningstype.KONTAKTADRESSE to Opplysning.Konstant(
            UUID.randomUUID(),
            kilde,
            adresserNorgePdl().toJsonNode()
        ),
        Opplysningstype.BOSTEDSADRESSE to Opplysning.Periodisert(
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
        Opplysningstype.OPPHOLDSADRESSE to Opplysning.Konstant(
            UUID.randomUUID(),
            kilde,
            adresserNorgePdl().toJsonNode()
        ),
        Opplysningstype.UTENLANDSOPPHOLD to Opplysning.Periodisert(
            emptyList()
        ),
        Opplysningstype.INNTEKT to Opplysning.Periodisert(inntektsopplysning),
        Opplysningstype.MEDLEMSKAPSPERIODE to Opplysning.Periodisert(medlemskap),
        Opplysningstype.ARBEIDSFORHOLD to Opplysning.Periodisert(arbeidsforholdTestData(100.0))
    )

    private val avdoedTestdata = GrunnlagTestData(
        opplysningsmapAvdoedOverrides = avdoedOpplysninger
    ).hentOpplysningsgrunnlag().hentAvdoed()

    @Test
    fun `Skal returnere med utfall oppfyllt dersom det ikke er gaps i de gyldige periodene`() {
        val vurdertVilkaar = vilkaarAvdoedesMedlemskap(avdoedTestdata)

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
            avdoedTestdata + mapOf(
                Opplysningstype.INNTEKT to Opplysning.Periodisert(inntektsopplysning)
            )
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
}