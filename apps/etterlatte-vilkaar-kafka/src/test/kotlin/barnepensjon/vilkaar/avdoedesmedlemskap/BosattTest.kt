package barnepensjon.vilkaar.avdoedesmedlemskap

import GrunnlagTestData
import adresseDanmarkPdl
import adresseUtlandFoerFemAar
import adresserNorgePdl
import grunnlag.kilde
import no.nav.etterlatte.barnepensjon.toYearMonth
import no.nav.etterlatte.libs.common.grunnlag.Opplysning
import no.nav.etterlatte.libs.common.grunnlag.PeriodisertOpplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper.BOSTEDSADRESSE
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper.KONTAKTADRESSE
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper.OPPHOLDSADRESSE
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Utenlandsopphold
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.UtenlandsoppholdOpplysninger
import no.nav.etterlatte.libs.common.person.InnflyttingTilNorge
import no.nav.etterlatte.libs.common.person.UtflyttingFraNorge
import no.nav.etterlatte.libs.common.person.Utland
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.JaNeiVetIkke
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.OppholdUtlandType
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.libs.common.vikaar.Kriterietyper
import no.nav.etterlatte.libs.common.vikaar.Kriterietyper.AVDOED_NORSK_STATSBORGER
import no.nav.etterlatte.libs.common.vikaar.VurderingsResultat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

class BosattTest {

    @Test
    fun vurderNorskStatsborgerskap() {
        val testdataNorsk = GrunnlagTestData().hentOpplysningsgrunnlag().hentAvdoed()
        val testdataDansk = testdataNorsk + mapOf(
            Opplysningstyper.STATSBORGERSKAP to Opplysning.Konstant(UUID.randomUUID(), kilde, "DAN".toJsonNode())
        )
        val testdataIngenStatsborgerskap = testdataNorsk - Opplysningstyper.STATSBORGERSKAP

        val norsk =
            kriterieNorskStatsborger(testdataNorsk, AVDOED_NORSK_STATSBORGER)
        val dansk =
            kriterieNorskStatsborger(testdataDansk, AVDOED_NORSK_STATSBORGER)
        val mangler =
            kriterieNorskStatsborger(testdataIngenStatsborgerskap, AVDOED_NORSK_STATSBORGER)

        assertEquals(VurderingsResultat.OPPFYLT, norsk.resultat)
        assertEquals(VurderingsResultat.IKKE_OPPFYLT, dansk.resultat)
        assertEquals(VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING, mangler.resultat)
    }

    @Test
    fun vurderInnOgUtvandring() {
        val avdødTestdata = GrunnlagTestData(
            opplysningsmapAvdødOverrides = mapOf(
                BOSTEDSADRESSE to Opplysning.Periodisert(
                    adresseUtlandFoerFemAar().map {
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
        ).hentOpplysningsgrunnlag().hentAvdoed()
        val avdødIngenUtland = avdødTestdata - Opplysningstyper.UTLAND
        val avdødInnvandring = avdødTestdata + mapOf(
            Opplysningstyper.UTLAND to Opplysning.Konstant(
                UUID.randomUUID(),
                kilde,
                Utland(
                    innflyttingTilNorge = listOf(InnflyttingTilNorge("DAN", LocalDate.now())),
                    utflyttingFraNorge = listOf(UtflyttingFraNorge("USA", LocalDate.now()))
                ).toJsonNode()
            )
        )
        val avdødHarIkkeUtland = avdødTestdata + mapOf(
            Opplysningstyper.UTLAND to Opplysning.Konstant(
                UUID.randomUUID(),
                kilde,
                Utland(
                    innflyttingTilNorge = emptyList(),
                    utflyttingFraNorge = emptyList()
                ).toJsonNode()
            )
        )

        val ingenUtland = kriterieIngenInnUtvandring(avdødIngenUtland, AVDOED_NORSK_STATSBORGER)
        val ingenInnOgUtvandring = kriterieIngenInnUtvandring(avdødHarIkkeUtland, AVDOED_NORSK_STATSBORGER)
        val harInnOgUtvandring = kriterieIngenInnUtvandring(avdødInnvandring, AVDOED_NORSK_STATSBORGER)

        assertEquals(VurderingsResultat.OPPFYLT, ingenUtland.resultat)
        assertEquals(VurderingsResultat.OPPFYLT, ingenInnOgUtvandring.resultat)
        assertEquals(VurderingsResultat.IKKE_OPPFYLT, harInnOgUtvandring.resultat)
    }

    @Test
    fun vurderIngenUtelandsopphold() {
        val avdødMedUtlandsopphold = GrunnlagTestData(
            opplysningsmapAvdødOverrides = mapOf(
                Opplysningstyper.UTENLANDSOPPHOLD to Opplysning.Konstant(
                    UUID.randomUUID(),
                    kilde,
                    no.nav.etterlatte.libs.common.person.Utenlandsopphold(JaNeiVetIkke.JA, null, null).toJsonNode()
                )
            )
        ).hentOpplysningsgrunnlag().hentAvdoed()

        val avdødUtenUtlandsopphold = GrunnlagTestData(
            opplysningsmapAvdødOverrides = mapOf(
                Opplysningstyper.UTENLANDSOPPHOLD to Opplysning.Konstant(
                    UUID.randomUUID(),
                    kilde,
                    no.nav.etterlatte.libs.common.person.Utenlandsopphold(JaNeiVetIkke.NEI, null, null).toJsonNode()
                )
            )
        ).hentOpplysningsgrunnlag().hentAvdoed()

        val utenlandsopphold =
            kriterieIngenUtenlandsoppholdFraSoeknad(
                avdødMedUtlandsopphold,
                Kriterietyper.AVDOED_IKKE_OPPHOLD_UTLAND_FRA_SOEKNAD
            )

        val ingenUtenlandsopphold =
            kriterieIngenUtenlandsoppholdFraSoeknad(
                avdødUtenUtlandsopphold,
                Kriterietyper.AVDOED_IKKE_OPPHOLD_UTLAND_FRA_SOEKNAD
            )

        assertEquals(VurderingsResultat.IKKE_OPPFYLT, utenlandsopphold.resultat)
        assertEquals(VurderingsResultat.OPPFYLT, ingenUtenlandsopphold.resultat)
    }

    @Test
    fun kunNorskeAdresserSisteFemAar() {
        val avdødUtenUtlandsadresser = GrunnlagTestData(
            opplysningsmapAvdødOverrides = mapOf(
                BOSTEDSADRESSE to Opplysning.Periodisert(
                    adresserNorgePdl().map {
                        PeriodisertOpplysning(
                            id = UUID.randomUUID(),
                            kilde = kilde,
                            verdi = it.toJsonNode(),
                            fom = it.gyldigFraOgMed.toYearMonth()!!,
                            tom = it.gyldigTilOgMed.toYearMonth()
                        )
                    }
                ),
                KONTAKTADRESSE to Opplysning.Konstant(UUID.randomUUID(), kilde, adresserNorgePdl().toJsonNode()),
                OPPHOLDSADRESSE to Opplysning.Konstant(UUID.randomUUID(), kilde, adresserNorgePdl().toJsonNode())
            )
        ).hentOpplysningsgrunnlag().hentAvdoed()
        val avdødUtenAdresse = avdødUtenUtlandsadresser - BOSTEDSADRESSE - KONTAKTADRESSE - OPPHOLDSADRESSE
        val avdødMedUtlandsadresser = avdødUtenUtlandsadresser + mapOf(
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
            KONTAKTADRESSE to Opplysning.Konstant(UUID.randomUUID(), kilde, adresseDanmarkPdl().toJsonNode()),
            OPPHOLDSADRESSE to Opplysning.Konstant(UUID.randomUUID(), kilde, adresseDanmarkPdl().toJsonNode())
        )
        val avdødMedUtlandsadresserFørFemÅr = avdødUtenUtlandsadresser + mapOf(
            BOSTEDSADRESSE to Opplysning.Periodisert(
                adresseUtlandFoerFemAar().map {
                    PeriodisertOpplysning(
                        id = UUID.randomUUID(),
                        kilde = kilde,
                        verdi = it.toJsonNode(),
                        fom = it.gyldigFraOgMed.toYearMonth()!!,
                        tom = it.gyldigTilOgMed.toYearMonth()
                    )
                }
            ),
            KONTAKTADRESSE to Opplysning.Konstant(UUID.randomUUID(), kilde, adresseUtlandFoerFemAar().toJsonNode()),
            OPPHOLDSADRESSE to Opplysning.Konstant(UUID.randomUUID(), kilde, adresseUtlandFoerFemAar().toJsonNode())
        )

        val utenlandsoppholdBosted =
            kriterieKunNorskeBostedsadresserSisteFemAar(
                avdødMedUtlandsadresser,
                Kriterietyper.AVDOED_KUN_NORSKE_BOSTEDSADRESSER
            )
        val utenlandsoppholdOpphold =
            kriterieKunNorskeOppholdsadresserSisteFemAar(
                avdødMedUtlandsadresser,
                Kriterietyper.AVDOED_KUN_NORSKE_OPPHOLDSSADRESSER
            )

        val utenlandsoppholdKontakt =
            kriterieKunNorskeKontaktadresserSisteFemAar(
                avdødMedUtlandsadresser,
                Kriterietyper.AVDOED_KUN_NORSKE_KONTAKTADRESSER
            )

        val ingenUtenlandsoppholdBosted =
            kriterieKunNorskeBostedsadresserSisteFemAar(
                avdødUtenUtlandsadresser,
                Kriterietyper.AVDOED_KUN_NORSKE_BOSTEDSADRESSER
            )

        val ingenUtenlandsoppholdOpphold =
            kriterieKunNorskeOppholdsadresserSisteFemAar(
                avdødUtenUtlandsadresser,
                Kriterietyper.AVDOED_KUN_NORSKE_OPPHOLDSSADRESSER
            )

        val ingenUtenlandsoppholdKontakt =
            kriterieKunNorskeKontaktadresserSisteFemAar(
                avdødUtenUtlandsadresser,
                Kriterietyper.AVDOED_KUN_NORSKE_KONTAKTADRESSER
            )

        val utenlandsoppholdFoerFemAar =
            kriterieKunNorskeBostedsadresserSisteFemAar(
                avdødMedUtlandsadresserFørFemÅr,
                Kriterietyper.AVDOED_KUN_NORSKE_BOSTEDSADRESSER
            )

        val ingenAdresser =
            kriterieKunNorskeBostedsadresserSisteFemAar(
                avdødUtenAdresse,
                Kriterietyper.AVDOED_KUN_NORSKE_BOSTEDSADRESSER
            )

        assertEquals(VurderingsResultat.OPPFYLT, ingenUtenlandsoppholdBosted.resultat)
        assertEquals(VurderingsResultat.OPPFYLT, ingenUtenlandsoppholdOpphold.resultat)
        assertEquals(VurderingsResultat.OPPFYLT, ingenUtenlandsoppholdKontakt.resultat)

        assertEquals(VurderingsResultat.IKKE_OPPFYLT, utenlandsoppholdBosted.resultat)
        assertEquals(VurderingsResultat.IKKE_OPPFYLT, utenlandsoppholdOpphold.resultat)
        assertEquals(VurderingsResultat.IKKE_OPPFYLT, utenlandsoppholdKontakt.resultat)

        assertEquals(VurderingsResultat.OPPFYLT, utenlandsoppholdFoerFemAar.resultat)
        assertEquals(VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING, ingenAdresser.resultat)
    }

    @Test
    fun vurderSammenhengendeAdresserINorgeSisteFemAar() {
        val avdødUtenUtlandsadresser = GrunnlagTestData(
            opplysningsmapAvdødOverrides = mapOf(
                BOSTEDSADRESSE to Opplysning.Periodisert(
                    adresserNorgePdl().map {
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
        ).hentOpplysningsgrunnlag().hentAvdoed()
        val avdødUtenAdresse = avdødUtenUtlandsadresser - BOSTEDSADRESSE
        val avdødMedUtlandsadresser = avdødUtenUtlandsadresser + mapOf(
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
        val avdødMedUtlandsadresserFørFemÅr = avdødUtenUtlandsadresser + mapOf(
            BOSTEDSADRESSE to Opplysning.Periodisert(
                adresseUtlandFoerFemAar().map {
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

        val utenlandsopphold =
            kriterieSammenhengendeBostedsadresserINorgeSisteFemAar(
                avdødMedUtlandsadresser,
                Kriterietyper.AVDOED_SAMMENHENGENDE_BOSTEDSADRESSE_NORGE_SISTE_FEM_AAR
            )

        val ingenUtenlandsopphold =
            kriterieSammenhengendeBostedsadresserINorgeSisteFemAar(
                avdødUtenUtlandsadresser,
                Kriterietyper.AVDOED_SAMMENHENGENDE_BOSTEDSADRESSE_NORGE_SISTE_FEM_AAR
            )

        val utenlandsoppholdFoerFemAar =
            kriterieSammenhengendeBostedsadresserINorgeSisteFemAar(
                avdødMedUtlandsadresserFørFemÅr,
                Kriterietyper.AVDOED_SAMMENHENGENDE_BOSTEDSADRESSE_NORGE_SISTE_FEM_AAR
            )

        val ingenAdresser =
            kriterieSammenhengendeBostedsadresserINorgeSisteFemAar(
                avdødUtenAdresse,
                Kriterietyper.AVDOED_SAMMENHENGENDE_BOSTEDSADRESSE_NORGE_SISTE_FEM_AAR
            )

        assertEquals(VurderingsResultat.IKKE_OPPFYLT, utenlandsopphold.resultat)
        assertEquals(VurderingsResultat.OPPFYLT, ingenUtenlandsopphold.resultat)
        assertEquals(VurderingsResultat.OPPFYLT, utenlandsoppholdFoerFemAar.resultat)
        assertEquals(VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING, ingenAdresser.resultat)
    }

    companion object {
        val utenlandsoppholdAvdoedSoeknad = Utenlandsopphold(
            JaNeiVetIkke.JA,
            listOf(
                UtenlandsoppholdOpplysninger(
                    "Danmark",
                    LocalDate.parse("2010-01-25"),
                    LocalDate.parse("2022-01-25"),
                    listOf(OppholdUtlandType.ARBEIDET),
                    JaNeiVetIkke.JA,
                    null
                ),
                UtenlandsoppholdOpplysninger(
                    "Costa Rica",
                    LocalDate.parse("2000-01-25"),
                    LocalDate.parse("2007-01-25"),
                    listOf(OppholdUtlandType.ARBEIDET),
                    JaNeiVetIkke.NEI,
                    null
                )
            )
        )

        val ingenUtenlandsoppholdAvdoedSoeknad = Utenlandsopphold(
            JaNeiVetIkke.NEI,
            listOf()
        )
    }
}