package barnepensjon.vilkaar

import GrunnlagTestData
import grunnlag.GJENLEVENDE_FØDSELSNUMMER
import grunnlag.kilde
import no.nav.etterlatte.barnepensjon.vilkaarDoedsfallErRegistrert
import no.nav.etterlatte.libs.common.grunnlag.Opplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper
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
            opplysningsmapSøkerOverrides = mapOf(
                Opplysningstyper.FAMILIERELASJON to Opplysning.Konstant(
                    UUID.randomUUID(),
                    kilde,
                    FamilieRelasjon(
                        ansvarligeForeldre = listOf(GJENLEVENDE_FØDSELSNUMMER),
                        foreldre = listOf(GJENLEVENDE_FØDSELSNUMMER),
                        barn = null
                    ).toJsonNode()
                )
            )
        ).hentOpplysningsgrunnlag()

        val avdoedIngenDoedsdato = testData.hentAvdoed() - Opplysningstyper.DOEDSDATO

        val doedsdatoIkkeIPdl =
            vilkaarDoedsfallErRegistrert(
                avdoedIngenDoedsdato,
                testData.søker
            )

        val avdoedErForelder =
            vilkaarDoedsfallErRegistrert(
                testData.hentAvdoed(),
                testData.søker
            )

        val avdoedIkkeForelder =
            vilkaarDoedsfallErRegistrert(
                avdoedErIkkeForelder.hentAvdoed(),
                avdoedErIkkeForelder.søker
            )

        assertEquals(VurderingsResultat.IKKE_OPPFYLT, doedsdatoIkkeIPdl.resultat)
        assertEquals(VurderingsResultat.OPPFYLT, avdoedErForelder.resultat)
        assertEquals(VurderingsResultat.IKKE_OPPFYLT, avdoedIkkeForelder.resultat)
    }
}