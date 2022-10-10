package barnepensjon.kommerbarnettilgode

import GrunnlagTestData
import adresseDanmarkPdl
import adresserNorgePdl
import grunnlag.kilde
import no.nav.etterlatte.libs.common.grunnlag.Opplysning
import no.nav.etterlatte.libs.common.grunnlag.PeriodisertOpplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.libs.common.vikaar.VurderingsResultat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import vilkaar.barnepensjon.barnOgAvdoedSammeBostedsadresse
import java.time.YearMonth
import java.util.*

class BarnGjenlevendeSammeAdresseTest {

    @Test
    fun vuderBarnGjenlevndeSammeBostedsadresse() {
        val testDataUlikAdresse = GrunnlagTestData(
            opplysningsmapGjenlevendeOverrides = mapOf(
                Opplysningstyper.BOSTEDSADRESSE to Opplysning.Periodisert(
                    adresseDanmarkPdl().map {
                        PeriodisertOpplysning(
                            UUID.randomUUID(),
                            kilde,
                            it.toJsonNode(),
                            it.gyldigFraOgMed!!.let { YearMonth.of(it.year, it.month) },
                            it.gyldigTilOgMed?.let { YearMonth.of(it.year, it.month) }
                        )
                    }
                )
            ),
            opplysningsmapSoekerOverrides = mapOf(
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
                )
            )
        )

        val testDataLikAdresse = GrunnlagTestData(
            opplysningsmapGjenlevendeOverrides = mapOf(
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
                )
            ),
            opplysningsmapSoekerOverrides = mapOf(
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
                )
            )
        )

        val sammeAdresse = barnOgAvdoedSammeBostedsadresse(
            testDataLikAdresse.hentOpplysningsgrunnlag().soeker,
            testDataLikAdresse.hentOpplysningsgrunnlag().hentGjenlevende()
        )

        val ulikeAdresse = barnOgAvdoedSammeBostedsadresse(
            testDataUlikAdresse.hentOpplysningsgrunnlag().soeker,
            testDataUlikAdresse.hentOpplysningsgrunnlag().hentGjenlevende()
        )
        Assertions.assertEquals(VurderingsResultat.OPPFYLT, sammeAdresse.resultat)
        Assertions.assertEquals(VurderingsResultat.IKKE_OPPFYLT, ulikeAdresse.resultat)
    }
}