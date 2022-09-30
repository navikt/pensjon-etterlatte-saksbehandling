package barnepensjon.vilkaar

import GrunnlagTestData
import adresseDanmarkPdl
import grunnlag.kilde
import lagMockPersonSoekerSoeknad
import mapTilVilkaarstypeSoekerSoeknad
import no.nav.etterlatte.barnepensjon.toYearMonth
import no.nav.etterlatte.libs.common.grunnlag.Opplysning
import no.nav.etterlatte.libs.common.grunnlag.PeriodisertOpplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.UtenlandsadresseBarn
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
        val barnSoeknadNorge = lagMockPersonSoekerSoeknad(UtenlandsadresseBarn(JaNeiVetIkke.NEI, null, null))
        val barnSoeknadDanmark = lagMockPersonSoekerSoeknad(UtenlandsadresseBarn(JaNeiVetIkke.JA, null, null))

        val testdataNorskAdresse = GrunnlagTestData().hentOpplysningsgrunnlag()
        val testdataDanskAdresse = GrunnlagTestData(
            opplysningsmapSøkerOverrides = mapOf(
                Opplysningstyper.BOSTEDSADRESSE to Opplysning.Periodisert(
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
            ),
            opplysningsmapGjenlevendeOverrides = mapOf(
                Opplysningstyper.BOSTEDSADRESSE to Opplysning.Periodisert(
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
            ),
            opplysningsmapSøskenOverrides = mapOf(),
            opplysningsmapAvdødOverrides = mapOf(),
            opplysningsmapHalvsøskenOverrides = mapOf(),
            opplysningsmapSakOverrides = mapOf()
        ).hentOpplysningsgrunnlag()

        val ingenUtenlandsAdresser = vilkaarBarnetsMedlemskap(
            testdataNorskAdresse.søker,
            mapTilVilkaarstypeSoekerSoeknad(barnSoeknadNorge),
            testdataNorskAdresse.hentGjenlevende(),
            testdataNorskAdresse.hentAvdoed()
        )

        val barnUtenlandsAdresserPdl = vilkaarBarnetsMedlemskap(
            testdataDanskAdresse.søker,
            mapTilVilkaarstypeSoekerSoeknad(barnSoeknadNorge),
            testdataNorskAdresse.hentGjenlevende(),
            testdataNorskAdresse.hentAvdoed()
        )

        val barnUtenlandsAdresserSoeknad = vilkaarBarnetsMedlemskap(
            testdataNorskAdresse.søker,
            mapTilVilkaarstypeSoekerSoeknad(barnSoeknadDanmark),
            testdataNorskAdresse.hentGjenlevende(),
            testdataNorskAdresse.hentAvdoed()
        )

        val gjenlevendeUtenlandsAdresserPdl = vilkaarBarnetsMedlemskap(
            testdataNorskAdresse.søker,
            mapTilVilkaarstypeSoekerSoeknad(barnSoeknadNorge),
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