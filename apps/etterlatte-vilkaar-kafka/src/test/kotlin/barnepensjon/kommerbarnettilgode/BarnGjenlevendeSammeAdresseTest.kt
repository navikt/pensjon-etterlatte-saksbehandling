package barnepensjon.kommerbarnettilgode

import GrunnlagTestData
import adresseDanmarkPdl
import adresserNorgePdl
import grunnlag.kilde
import no.nav.etterlatte.libs.common.grunnlag.Opplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.libs.common.vikaar.VurderingsResultat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import vilkaar.barnepensjon.barnOgAvdoedSammeBostedsadresse
import java.util.*

class BarnGjenlevendeSammeAdresseTest {

    @Test
    fun vuderBarnGjenlevndeSammeBostedsadresse() {
        val testDataUlikAdresse = GrunnlagTestData(
            opplysningsmapGjenlevendeOverrides = mapOf(
                Opplysningstyper.BOSTEDSADRESSE to Opplysning.Konstant(
                    UUID.randomUUID(),
                    kilde,
                    adresseDanmarkPdl().toJsonNode()
                )
            ),
            opplysningsmapSøkerOverrides = mapOf(
                Opplysningstyper.BOSTEDSADRESSE to Opplysning.Konstant(
                    UUID.randomUUID(),
                    kilde,
                    adresserNorgePdl().toJsonNode()
                )
            )
        )

        val testDataLikAdresse = GrunnlagTestData(
            opplysningsmapGjenlevendeOverrides = mapOf(
                Opplysningstyper.BOSTEDSADRESSE to Opplysning.Konstant(
                    UUID.randomUUID(),
                    kilde,
                    adresserNorgePdl().toJsonNode()
                )
            ),
            opplysningsmapSøkerOverrides = mapOf(
                Opplysningstyper.BOSTEDSADRESSE to Opplysning.Konstant(
                    UUID.randomUUID(),
                    kilde,
                    adresserNorgePdl().toJsonNode()
                )
            )
        )

        val sammeAdresse = barnOgAvdoedSammeBostedsadresse(
            testDataLikAdresse.hentOpplysningsgrunnlag().søker,
            testDataLikAdresse.hentOpplysningsgrunnlag().hentGjenlevende()
        )

        val ulikeAdresse = barnOgAvdoedSammeBostedsadresse(
            testDataUlikAdresse.hentOpplysningsgrunnlag().søker,
            testDataUlikAdresse.hentOpplysningsgrunnlag().hentGjenlevende()
        )
        Assertions.assertEquals(VurderingsResultat.OPPFYLT, sammeAdresse.resultat)
        Assertions.assertEquals(VurderingsResultat.IKKE_OPPFYLT, ulikeAdresse.resultat)
    }
}