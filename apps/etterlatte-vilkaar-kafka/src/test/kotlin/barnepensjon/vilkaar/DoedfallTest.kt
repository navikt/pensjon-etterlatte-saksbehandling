package barnepensjon.vilkaar

import GrunnlagTestData
import grunnlag.GJENLEVENDE_FOEDSELSNUMMER
import grunnlag.kilde
import no.nav.etterlatte.barnepensjon.vilkaarDoedsfallErRegistrert
import no.nav.etterlatte.libs.common.grunnlag.Opplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.person.FamilieRelasjon
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.libs.common.vikaar.VurderingsResultat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.*

class DoedfallTest {

    @Test
    fun vurderDoedsdatoErRegistrert() {
        val testData = GrunnlagTestData().hentOpplysningsgrunnlag()
        val avdoedErIkkeForelder = GrunnlagTestData(
            opplysningsmapSoekerOverrides = mapOf(
                Opplysningstype.FAMILIERELASJON to Opplysning.Konstant(
                    UUID.randomUUID(),
                    kilde,
                    FamilieRelasjon(
                        ansvarligeForeldre = listOf(GJENLEVENDE_FOEDSELSNUMMER),
                        foreldre = listOf(GJENLEVENDE_FOEDSELSNUMMER),
                        barn = null
                    ).toJsonNode()
                )
            )
        ).hentOpplysningsgrunnlag()

        val avdoedIngenDoedsdato = testData.hentAvdoed() - Opplysningstype.DOEDSDATO

        val doedsdatoIkkeIPdl =
            vilkaarDoedsfallErRegistrert(
                avdoedIngenDoedsdato,
                testData.soeker
            )

        val avdoedErForelder =
            vilkaarDoedsfallErRegistrert(
                testData.hentAvdoed(),
                testData.soeker
            )

        val avdoedIkkeForelder =
            vilkaarDoedsfallErRegistrert(
                avdoedErIkkeForelder.hentAvdoed(),
                avdoedErIkkeForelder.soeker
            )

        assertEquals(VurderingsResultat.IKKE_OPPFYLT, doedsdatoIkkeIPdl.resultat)
        assertEquals(VurderingsResultat.OPPFYLT, avdoedErForelder.resultat)
        assertEquals(VurderingsResultat.IKKE_OPPFYLT, avdoedIkkeForelder.resultat)
    }
}