package barnepensjon.vilkaar.avdoedesmedlemskap

import GrunnlagTestData
import adresseDanmarkPdl
import adresseUtlandFoerFemAar
import adresserNorgePdl
import grunnlag.kilde
import grunnlag.utenlandsopphold
import no.nav.etterlatte.barnepensjon.toYearMonth
import no.nav.etterlatte.libs.common.grunnlag.Opplysning
import no.nav.etterlatte.libs.common.grunnlag.PeriodisertOpplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper.BOSTEDSADRESSE
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper.KONTAKTADRESSE
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper.OPPHOLDSADRESSE
import no.nav.etterlatte.libs.common.person.InnflyttingTilNorge
import no.nav.etterlatte.libs.common.person.UtflyttingFraNorge
import no.nav.etterlatte.libs.common.person.Utland
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

        val norsk = kriterieNorskStatsborger(testdataNorsk, AVDOED_NORSK_STATSBORGER)
        val dansk = kriterieNorskStatsborger(testdataDansk, AVDOED_NORSK_STATSBORGER)
        val mangler = kriterieNorskStatsborger(testdataIngenStatsborgerskap, AVDOED_NORSK_STATSBORGER)

        assertEquals(VurderingsResultat.OPPFYLT, norsk.resultat)
        assertEquals(VurderingsResultat.IKKE_OPPFYLT, dansk.resultat)
        assertEquals(VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING, mangler.resultat)
    }

    @Test
    fun vurderInnOgUtvandring() {
        val avdoedTestdata = GrunnlagTestData(
            opplysningsmapAvdoedOverrides = mapOf(
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
        val avdoedIngenUtland = avdoedTestdata - Opplysningstyper.UTLAND
        val avdoedInnvandring = avdoedTestdata + mapOf(
            Opplysningstyper.UTLAND to Opplysning.Konstant(
                UUID.randomUUID(),
                kilde,
                Utland(
                    innflyttingTilNorge = listOf(InnflyttingTilNorge("DAN", LocalDate.now())),
                    utflyttingFraNorge = listOf(UtflyttingFraNorge("USA", LocalDate.now()))
                ).toJsonNode()
            )
        )
        val avdoedHarIkkeUtland = avdoedTestdata + mapOf(
            Opplysningstyper.UTLAND to Opplysning.Konstant(
                UUID.randomUUID(),
                kilde,
                Utland(
                    innflyttingTilNorge = emptyList(),
                    utflyttingFraNorge = emptyList()
                ).toJsonNode()
            )
        )

        val ingenUtland = kriterieIngenInnUtvandring(avdoedIngenUtland, AVDOED_NORSK_STATSBORGER)
        val ingenInnOgUtvandring = kriterieIngenInnUtvandring(avdoedHarIkkeUtland, AVDOED_NORSK_STATSBORGER)
        val harInnOgUtvandring = kriterieIngenInnUtvandring(avdoedInnvandring, AVDOED_NORSK_STATSBORGER)

        assertEquals(VurderingsResultat.OPPFYLT, ingenUtland.resultat)
        assertEquals(VurderingsResultat.OPPFYLT, ingenInnOgUtvandring.resultat)
        assertEquals(VurderingsResultat.IKKE_OPPFYLT, harInnOgUtvandring.resultat)
    }

    @Test
    fun vurderIngenUtelandsopphold() {
        val avdoedMedUtlandsopphold = GrunnlagTestData(
            opplysningsmapAvdoedOverrides = mapOf(
                Opplysningstyper.UTENLANDSOPPHOLD to Opplysning.Periodisert(utenlandsopphold)
            )
        ).hentOpplysningsgrunnlag().hentAvdoed()

        val avdoedUtenUtlandsopphold = GrunnlagTestData(
            opplysningsmapAvdoedOverrides = mapOf(
                Opplysningstyper.UTENLANDSOPPHOLD to Opplysning.Periodisert(
                    emptyList()
                )
            )
        ).hentOpplysningsgrunnlag().hentAvdoed()

        val utenlandsopphold =
            kriterieIngenUtenlandsoppholdFraSoeknad(
                avdoedMedUtlandsopphold,
                Kriterietyper.AVDOED_IKKE_OPPHOLD_UTLAND_FRA_SOEKNAD
            )

        val ingenUtenlandsopphold =
            kriterieIngenUtenlandsoppholdFraSoeknad(
                avdoedUtenUtlandsopphold,
                Kriterietyper.AVDOED_IKKE_OPPHOLD_UTLAND_FRA_SOEKNAD
            )

        assertEquals(VurderingsResultat.IKKE_OPPFYLT, utenlandsopphold.resultat)
        assertEquals(VurderingsResultat.OPPFYLT, ingenUtenlandsopphold.resultat)
    }

    @Test
    fun kunNorskeAdresserSisteFemAar() {
        val avdoedUtenUtlandsadresser = GrunnlagTestData(
            opplysningsmapAvdoedOverrides = mapOf(
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
        val avdoedUtenAdresse = avdoedUtenUtlandsadresser - BOSTEDSADRESSE - KONTAKTADRESSE - OPPHOLDSADRESSE
        val avdoedMedUtlandsadresser = avdoedUtenUtlandsadresser + mapOf(
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
        val avdoedMedUtlandsadresserFoerFemAar = avdoedUtenUtlandsadresser + mapOf(
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
                avdoedMedUtlandsadresser,
                Kriterietyper.AVDOED_KUN_NORSKE_BOSTEDSADRESSER
            )
        val utenlandsoppholdOpphold =
            kriterieKunNorskeOppholdsadresserSisteFemAar(
                avdoedMedUtlandsadresser,
                Kriterietyper.AVDOED_KUN_NORSKE_OPPHOLDSSADRESSER
            )

        val utenlandsoppholdKontakt =
            kriterieKunNorskeKontaktadresserSisteFemAar(
                avdoedMedUtlandsadresser,
                Kriterietyper.AVDOED_KUN_NORSKE_KONTAKTADRESSER
            )

        val ingenUtenlandsoppholdBosted =
            kriterieKunNorskeBostedsadresserSisteFemAar(
                avdoedUtenUtlandsadresser,
                Kriterietyper.AVDOED_KUN_NORSKE_BOSTEDSADRESSER
            )

        val ingenUtenlandsoppholdOpphold =
            kriterieKunNorskeOppholdsadresserSisteFemAar(
                avdoedUtenUtlandsadresser,
                Kriterietyper.AVDOED_KUN_NORSKE_OPPHOLDSSADRESSER
            )

        val ingenUtenlandsoppholdKontakt =
            kriterieKunNorskeKontaktadresserSisteFemAar(
                avdoedUtenUtlandsadresser,
                Kriterietyper.AVDOED_KUN_NORSKE_KONTAKTADRESSER
            )

        val utenlandsoppholdFoerFemAar =
            kriterieKunNorskeBostedsadresserSisteFemAar(
                avdoedMedUtlandsadresserFoerFemAar,
                Kriterietyper.AVDOED_KUN_NORSKE_BOSTEDSADRESSER
            )

        val ingenAdresser =
            kriterieKunNorskeBostedsadresserSisteFemAar(
                avdoedUtenAdresse,
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
        val avdoedUtenUtlandsadresser = GrunnlagTestData(
            opplysningsmapAvdoedOverrides = mapOf(
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
        val avdoedUtenAdresse = avdoedUtenUtlandsadresser - BOSTEDSADRESSE
        val avdoedMedUtlandsadresser = avdoedUtenUtlandsadresser + mapOf(
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
        val avdoedMedUtlandsadresserFoerFemAar = avdoedUtenUtlandsadresser + mapOf(
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
                avdoedMedUtlandsadresser,
                Kriterietyper.AVDOED_SAMMENHENGENDE_BOSTEDSADRESSE_NORGE_SISTE_FEM_AAR
            )

        val ingenUtenlandsopphold =
            kriterieSammenhengendeBostedsadresserINorgeSisteFemAar(
                avdoedUtenUtlandsadresser,
                Kriterietyper.AVDOED_SAMMENHENGENDE_BOSTEDSADRESSE_NORGE_SISTE_FEM_AAR
            )

        val utenlandsoppholdFoerFemAar =
            kriterieSammenhengendeBostedsadresserINorgeSisteFemAar(
                avdoedMedUtlandsadresserFoerFemAar,
                Kriterietyper.AVDOED_SAMMENHENGENDE_BOSTEDSADRESSE_NORGE_SISTE_FEM_AAR
            )

        val ingenAdresser =
            kriterieSammenhengendeBostedsadresserINorgeSisteFemAar(
                avdoedUtenAdresse,
                Kriterietyper.AVDOED_SAMMENHENGENDE_BOSTEDSADRESSE_NORGE_SISTE_FEM_AAR
            )

        assertEquals(VurderingsResultat.IKKE_OPPFYLT, utenlandsopphold.resultat)
        assertEquals(VurderingsResultat.OPPFYLT, ingenUtenlandsopphold.resultat)
        assertEquals(VurderingsResultat.OPPFYLT, utenlandsoppholdFoerFemAar.resultat)
        assertEquals(VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING, ingenAdresser.resultat)
    }
}