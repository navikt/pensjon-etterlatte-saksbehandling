package barnepensjon.vilkaar

import GrunnlagTestData
import adresseDanmarkPdl
import grunnlag.kilde
import no.nav.etterlatte.barnepensjon.toYearMonth
import no.nav.etterlatte.libs.common.grunnlag.Opplysning
import no.nav.etterlatte.libs.common.grunnlag.PeriodisertOpplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper.BOSTEDSADRESSE
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper.UTENLANDSADRESSE
import no.nav.etterlatte.libs.common.person.Utenlandsadresse
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.JaNeiVetIkke
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.libs.common.vikaar.VurderingsResultat
import no.nav.etterlatte.vilkaar.barnepensjon.vilkaarBarnetsMedlemskap
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.*

class BarnetsMedlemskapTest {
    @Test
    fun vuderBarnetsMedlemskap() {
        val testdataNorskAdresse = GrunnlagTestData(
            opplysningsmapSøkerOverrides = mapOf(
                UTENLANDSADRESSE to Opplysning.Konstant(
                    UUID.randomUUID(),
                    kilde,
                    Utenlandsadresse(JaNeiVetIkke.NEI, null, null).toJsonNode()
                )
            )
        ).hentOpplysningsgrunnlag()
        val testdataDanskAdresse = GrunnlagTestData(
            opplysningsmapSøkerOverrides = mapOf(
                BOSTEDSADRESSE to Opplysning.Periodisert(
                    adresseDanmarkPdl().map {
                        PeriodisertOpplysning(
                            id = UUID.randomUUID(),
                            kilde = kilde,
                            verdi = it.toJsonNode(),
                            fom = it.gyldigFraOgMed.toYearMonth()!!,
                            tom = it.gyldigTilOgMed.toYearMonth()
                        )
                    }
                ),
                UTENLANDSADRESSE to Opplysning.Konstant(
                    UUID.randomUUID(),
                    kilde,
                    Utenlandsadresse(JaNeiVetIkke.JA, null, null).toJsonNode()
                )
            ),
            opplysningsmapGjenlevendeOverrides = mapOf(
                BOSTEDSADRESSE to Opplysning.Periodisert(
                    adresseDanmarkPdl().map {
                        PeriodisertOpplysning(
                            id = UUID.randomUUID(),
                            kilde = kilde,
                            verdi = it.toJsonNode(),
                            fom = it.gyldigFraOgMed.toYearMonth()!!,
                            tom = it.gyldigTilOgMed.toYearMonth()
                        )
                    }
                )
            )
        ).hentOpplysningsgrunnlag()

        val ingenUtenlandsAdresser = vilkaarBarnetsMedlemskap(
            testdataNorskAdresse.søker,
            testdataNorskAdresse.hentGjenlevende(),
            testdataNorskAdresse.hentAvdoed()
        )

        val barnUtenlandsAdresserPdl = vilkaarBarnetsMedlemskap(
            testdataDanskAdresse.søker,
            testdataNorskAdresse.hentGjenlevende(),
            testdataNorskAdresse.hentAvdoed()
        )

        val barnUtenlandsAdresserSoeknad = vilkaarBarnetsMedlemskap(
            testdataNorskAdresse.søker + mapOf(
                UTENLANDSADRESSE to Opplysning.Konstant(
                    UUID.randomUUID(),
                    kilde,
                    Utenlandsadresse(JaNeiVetIkke.JA, null, null).toJsonNode()
                )
            ),
            testdataNorskAdresse.hentGjenlevende(),
            testdataNorskAdresse.hentAvdoed()
        )

        val gjenlevendeUtenlandsAdresserPdl = vilkaarBarnetsMedlemskap(
            testdataNorskAdresse.søker,
            testdataDanskAdresse.hentGjenlevende(),
            testdataNorskAdresse.hentAvdoed()
        )

        Assertions.assertEquals(VurderingsResultat.OPPFYLT, ingenUtenlandsAdresser.resultat)
        Assertions.assertEquals(VurderingsResultat.IKKE_OPPFYLT, barnUtenlandsAdresserPdl.resultat)
        Assertions.assertEquals(
            VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING,
            gjenlevendeUtenlandsAdresserPdl.resultat
        )
        Assertions.assertEquals(
            VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING,
            barnUtenlandsAdresserSoeknad.resultat
        )
    }
}