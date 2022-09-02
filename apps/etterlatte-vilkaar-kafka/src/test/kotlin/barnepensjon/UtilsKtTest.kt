package barnepensjon

import adresseDanmarkPdl
import adresseUtlandFoerFemAar
import adresserNorgePdl
import no.nav.etterlatte.barnepensjon.Periode
import no.nav.etterlatte.barnepensjon.harKunNorskeAdresserEtterDato
import no.nav.etterlatte.barnepensjon.harKunNorskePdlAdresserEtterDato
import no.nav.etterlatte.barnepensjon.hentAdresseperioderINorge
import no.nav.etterlatte.barnepensjon.hentGaps
import no.nav.etterlatte.barnepensjon.kombinerPerioder
import no.nav.etterlatte.barnepensjon.setVilkaarVurderingFraKriterier
import no.nav.etterlatte.barnepensjon.setVilkaarVurderingFraVilkaar
import no.nav.etterlatte.barnepensjon.setVurderingFraKommerBarnetTilGode
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Adresser
import no.nav.etterlatte.libs.common.vikaar.Kriterie
import no.nav.etterlatte.libs.common.vikaar.Kriterietyper
import no.nav.etterlatte.libs.common.vikaar.Vilkaartyper
import no.nav.etterlatte.libs.common.vikaar.VurderingsResultat
import no.nav.etterlatte.libs.common.vikaar.VurdertVilkaar
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

internal class UtilsKtTest {

    companion object {
        val oppfyltVilkarDoedsfall = VurdertVilkaar(
            Vilkaartyper.DOEDSFALL_ER_REGISTRERT,
            VurderingsResultat.OPPFYLT,
            null,
            emptyList(),
            LocalDateTime.now()
        )
        val oppfyltVilkarAlder = VurdertVilkaar(
            Vilkaartyper.SOEKER_ER_UNDER_20,
            VurderingsResultat.OPPFYLT,
            null,
            emptyList(),
            LocalDateTime.now()
        )
        val oppfyltVilkarBarnetsMedlemskap = VurdertVilkaar(
            Vilkaartyper.BARNETS_MEDLEMSKAP,
            VurderingsResultat.OPPFYLT,
            null,
            emptyList(),
            LocalDateTime.now()
        )
        val ikkeOppfyltVilkaarBarnetsMedlemskap = VurdertVilkaar(
            Vilkaartyper.BARNETS_MEDLEMSKAP,
            VurderingsResultat.IKKE_OPPFYLT,
            null,
            emptyList(),
            LocalDateTime.now()
        )
        val manglerInfoVilkaarBarnetsMedlemskap = VurdertVilkaar(
            Vilkaartyper.BARNETS_MEDLEMSKAP,
            VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING,
            null,
            emptyList(),
            LocalDateTime.now()
        )
        val ikkeOppfyltVilkaarAvdodesMedlemskap = VurdertVilkaar(
            Vilkaartyper.AVDOEDES_FORUTGAAENDE_MEDLEMSKAP,
            VurderingsResultat.IKKE_OPPFYLT,
            null,
            emptyList(),
            LocalDateTime.now()
        )
        val manglerInfoVilkaarAvdodesMedlemskap = VurdertVilkaar(
            Vilkaartyper.AVDOEDES_FORUTGAAENDE_MEDLEMSKAP,
            VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING,
            null,
            emptyList(),
            LocalDateTime.now()
        )

        val oppfyltGjenlevendeBarnSammeAdresse = VurdertVilkaar(
            Vilkaartyper.GJENLEVENDE_OG_BARN_SAMME_BOSTEDADRESSE,
            VurderingsResultat.OPPFYLT,
            null,
            emptyList(),
            LocalDateTime.now()
        )
        val ikkeOppfyltGjenlevendeBarnSammeAdresse = VurdertVilkaar(
            Vilkaartyper.GJENLEVENDE_OG_BARN_SAMME_BOSTEDADRESSE,
            VurderingsResultat.IKKE_OPPFYLT,
            null,
            emptyList(),
            LocalDateTime.now()
        )
        val manglerInfoGjenlevendeBarnSammeAdresse = VurdertVilkaar(
            Vilkaartyper.GJENLEVENDE_OG_BARN_SAMME_BOSTEDADRESSE,
            VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING,
            null,
            emptyList(),
            LocalDateTime.now()
        )
        val oppfyltBarnIngenUtlandsadresse = VurdertVilkaar(
            Vilkaartyper.BARN_INGEN_OPPGITT_UTLANDSADRESSE,
            VurderingsResultat.OPPFYLT,
            null,
            emptyList(),
            LocalDateTime.now()
        )
        val ikkeOppfyltBarnIngenUtlandsadresse = VurdertVilkaar(
            Vilkaartyper.BARN_INGEN_OPPGITT_UTLANDSADRESSE,
            VurderingsResultat.IKKE_OPPFYLT,
            null,
            emptyList(),
            LocalDateTime.now()
        )
        val oppfyltBarnAvdoedSammeAdresse = VurdertVilkaar(
            Vilkaartyper.BARN_BOR_PAA_AVDOEDES_ADRESSE,
            VurderingsResultat.OPPFYLT,
            null,
            emptyList(),
            LocalDateTime.now()
        )
        val ikkeOppfyltBarnAvdoedSammeAdresse = VurdertVilkaar(
            Vilkaartyper.BARN_BOR_PAA_AVDOEDES_ADRESSE,
            VurderingsResultat.IKKE_OPPFYLT,
            null,
            emptyList(),
            LocalDateTime.now()
        )
        val oppfyltSaksbehandlerResultat = VurdertVilkaar(
            Vilkaartyper.SAKSBEHANDLER_RESULTAT,
            VurderingsResultat.OPPFYLT,
            null,
            emptyList(),
            LocalDateTime.now()
        )
        val ikkeOppfyltSaksbehandlerResultat = VurdertVilkaar(
            Vilkaartyper.SAKSBEHANDLER_RESULTAT,
            VurderingsResultat.IKKE_OPPFYLT,
            null,
            emptyList(),
            LocalDateTime.now()
        )
    }

    @Test
    fun vilkarsvurderingOppfyltOmAlleVilkårOppfylt() {
        assertEquals(
            VurderingsResultat.OPPFYLT,
            setVilkaarVurderingFraVilkaar(
                listOf(
                    oppfyltVilkarAlder,
                    oppfyltVilkarDoedsfall,
                    oppfyltVilkarBarnetsMedlemskap
                )
            )
        )
    }

    @Test
    fun vilkarsvurderingIkkeOppfyltOmMinstEtVilkårIkkeOppfylt() {
        assertEquals(
            VurderingsResultat.IKKE_OPPFYLT,
            setVilkaarVurderingFraVilkaar(
                listOf(
                    oppfyltVilkarAlder,
                    oppfyltVilkarDoedsfall,
                    ikkeOppfyltVilkaarBarnetsMedlemskap
                )
            )
        )
    }

    @Test
    fun vilkarsvurderingKanIkkeVurderesOmMinstEtVilkårIkkeKanVurderes() {
        assertEquals(
            VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING,
            setVilkaarVurderingFraVilkaar(
                listOf(
                    oppfyltVilkarAlder,
                    oppfyltVilkarDoedsfall,
                    manglerInfoVilkaarBarnetsMedlemskap
                )
            )
        )
        assertEquals(
            VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING,
            setVilkaarVurderingFraVilkaar(
                listOf(
                    oppfyltVilkarAlder,
                    oppfyltVilkarDoedsfall,
                    ikkeOppfyltVilkaarAvdodesMedlemskap,
                    manglerInfoVilkaarBarnetsMedlemskap
                )
            )
        )
    }

    @Test
    fun vilkaarOmAvdodesMedlemskapSkalIkkeEndreTotalvurdering() {
        assertEquals(
            VurderingsResultat.OPPFYLT,
            setVilkaarVurderingFraVilkaar(
                listOf(
                    oppfyltVilkarAlder,
                    oppfyltVilkarDoedsfall,
                    oppfyltVilkarBarnetsMedlemskap,
                    manglerInfoVilkaarAvdodesMedlemskap
                )
            )
        )
        assertEquals(
            VurderingsResultat.OPPFYLT,
            setVilkaarVurderingFraVilkaar(
                listOf(
                    oppfyltVilkarAlder,
                    oppfyltVilkarDoedsfall,
                    oppfyltVilkarBarnetsMedlemskap,
                    ikkeOppfyltVilkaarAvdodesMedlemskap
                )
            )
        )
    }

    @Test
    fun kommerBarnetTilGodeVurderingOppfylt() {
        assertEquals(
            VurderingsResultat.OPPFYLT,
            setVurderingFraKommerBarnetTilGode(
                listOf(
                    oppfyltGjenlevendeBarnSammeAdresse,
                    oppfyltBarnIngenUtlandsadresse,
                    ikkeOppfyltBarnAvdoedSammeAdresse
                )
            )
        )
    }

    @Test
    fun kommerBarnetTilGodeVurderingIkkeOppfylt() {
        assertEquals(
            VurderingsResultat.IKKE_OPPFYLT,
            setVurderingFraKommerBarnetTilGode(
                listOf(
                    ikkeOppfyltGjenlevendeBarnSammeAdresse,
                    oppfyltBarnIngenUtlandsadresse,
                    ikkeOppfyltBarnAvdoedSammeAdresse
                )
            )
        )
        assertEquals(
            VurderingsResultat.IKKE_OPPFYLT,
            setVurderingFraKommerBarnetTilGode(
                listOf(
                    ikkeOppfyltGjenlevendeBarnSammeAdresse,
                    oppfyltBarnIngenUtlandsadresse,
                    oppfyltBarnAvdoedSammeAdresse
                )
            )
        )
        assertEquals(
            VurderingsResultat.IKKE_OPPFYLT,
            setVurderingFraKommerBarnetTilGode(
                listOf(
                    oppfyltGjenlevendeBarnSammeAdresse,
                    ikkeOppfyltBarnIngenUtlandsadresse,
                    ikkeOppfyltBarnAvdoedSammeAdresse
                )
            )
        )
        assertEquals(
            VurderingsResultat.IKKE_OPPFYLT,
            setVurderingFraKommerBarnetTilGode(
                listOf(
                    ikkeOppfyltBarnAvdoedSammeAdresse,
                    ikkeOppfyltBarnIngenUtlandsadresse,
                    oppfyltBarnAvdoedSammeAdresse
                )
            )
        )
    }

    @Test
    fun saksbehandlerResultatSkalGjelde() {
        assertEquals(
            VurderingsResultat.IKKE_OPPFYLT,
            setVurderingFraKommerBarnetTilGode(
                listOf(
                    oppfyltGjenlevendeBarnSammeAdresse,
                    oppfyltBarnIngenUtlandsadresse,
                    ikkeOppfyltBarnAvdoedSammeAdresse,
                    ikkeOppfyltSaksbehandlerResultat
                )
            )
        )

        assertEquals(
            VurderingsResultat.OPPFYLT,
            setVurderingFraKommerBarnetTilGode(
                listOf(
                    ikkeOppfyltGjenlevendeBarnSammeAdresse,
                    oppfyltBarnIngenUtlandsadresse,
                    ikkeOppfyltBarnAvdoedSammeAdresse,
                    oppfyltSaksbehandlerResultat
                )
            )
        )

        assertEquals(
            VurderingsResultat.OPPFYLT,
            setVurderingFraKommerBarnetTilGode(
                listOf(
                    oppfyltGjenlevendeBarnSammeAdresse,
                    oppfyltBarnIngenUtlandsadresse,
                    ikkeOppfyltBarnAvdoedSammeAdresse
                )
            )
        )
    }

    @Test
    fun kommerBarnetTilGodeVurderingManglerInfo() {
        assertEquals(
            VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING,
            setVurderingFraKommerBarnetTilGode(
                listOf(
                    manglerInfoGjenlevendeBarnSammeAdresse,
                    oppfyltBarnIngenUtlandsadresse,
                    ikkeOppfyltBarnAvdoedSammeAdresse
                )
            )
        )
    }

    @Test
    fun vurderVilkaarsVurdering() {
        val kriterieOppfylt =
            Kriterie(Kriterietyper.SOEKER_ER_UNDER_20_PAA_VIRKNINGSDATO, VurderingsResultat.OPPFYLT, listOf())
        val kriterieIkkeOppfylt =
            Kriterie(
                Kriterietyper.SOEKER_ER_UNDER_20_PAA_VIRKNINGSDATO,
                VurderingsResultat.IKKE_OPPFYLT,
                listOf()
            )
        val kriterieKanIkkeVurdere = Kriterie(
            Kriterietyper.SOEKER_ER_UNDER_20_PAA_VIRKNINGSDATO,
            VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING,
            listOf()
        )

        val vilkaarKriterierOppfylt = setVilkaarVurderingFraKriterier(listOf(kriterieOppfylt, kriterieOppfylt))
        val vilkaarEtKriterieIkkeOppfylt =
            setVilkaarVurderingFraKriterier(listOf(kriterieOppfylt, kriterieIkkeOppfylt, kriterieKanIkkeVurdere))
        val vilkaarKriterierOppfyltOgKanIkkeHentesUt =
            setVilkaarVurderingFraKriterier(listOf(kriterieOppfylt, kriterieKanIkkeVurdere, kriterieOppfylt))

        assertEquals(VurderingsResultat.OPPFYLT, vilkaarKriterierOppfylt)
        assertEquals(VurderingsResultat.IKKE_OPPFYLT, vilkaarEtKriterieIkkeOppfylt)
        assertEquals(
            VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING,
            vilkaarKriterierOppfyltOgKanIkkeHentesUt
        )
    }

    @Test
    fun vurderHarKunNorskePdlAdresserEtterDato() {
        val doedsdato = LocalDate.parse("2022-03-25")
        val adresserKunNorske = Adresser(
            bostedadresse = adresserNorgePdl(),
            oppholdadresse = adresseUtlandFoerFemAar(),
            kontaktadresse = null
        )
        val adresserUtland = Adresser(
            bostedadresse = adresseDanmarkPdl(),
            oppholdadresse = adresseUtlandFoerFemAar(),
            kontaktadresse = null
        )

        val kunNorskeAdresserResultat = harKunNorskePdlAdresserEtterDato(adresserKunNorske, doedsdato)
        val utenlandskeAdresserResultat = harKunNorskePdlAdresserEtterDato(adresserUtland, doedsdato)

        assertEquals(VurderingsResultat.OPPFYLT, kunNorskeAdresserResultat)
        assertEquals(VurderingsResultat.IKKE_OPPFYLT, utenlandskeAdresserResultat)
    }

    @Test
    fun vurderHarKunNorskeAdresserEtterDato() {
        val doedsdato = LocalDate.parse("2022-03-25")

        val kunNorskeAdresserResultat = harKunNorskeAdresserEtterDato(adresserNorgePdl(), doedsdato)
        val utenlandskeAdresserResultat = harKunNorskeAdresserEtterDato(adresseDanmarkPdl(), doedsdato)
        val utenlandskeAdresserForFemAarResultat = harKunNorskeAdresserEtterDato(adresseUtlandFoerFemAar(), doedsdato)

        assertEquals(VurderingsResultat.OPPFYLT, kunNorskeAdresserResultat)
        assertEquals(VurderingsResultat.IKKE_OPPFYLT, utenlandskeAdresserResultat)
        assertEquals(VurderingsResultat.OPPFYLT, utenlandskeAdresserForFemAarResultat)
    }

    @Test
    fun vurderHentAdresseperioderINorge() {
        val doedsdato = LocalDate.parse("2022-03-25")
        val femAarFoerDoedsdato = doedsdato.minusYears(5)

        val kunNorskeAdressePerioder = hentAdresseperioderINorge(adresserNorgePdl(), doedsdato)
        val utenlandskeAdressePerioder = hentAdresseperioderINorge(adresseDanmarkPdl(), doedsdato)

        assertEquals(2, kunNorskeAdressePerioder?.size)
        assertEquals(femAarFoerDoedsdato, kunNorskeAdressePerioder?.first()?.gyldigFra)
        assertEquals(LocalDate.parse("2021-04-30"), kunNorskeAdressePerioder?.first()?.gyldigTil)
        assertEquals(LocalDate.parse("2020-01-26"), kunNorskeAdressePerioder?.get(1)?.gyldigFra)
        assertEquals(doedsdato, kunNorskeAdressePerioder?.get(1)?.gyldigTil)
        assertEquals(0, utenlandskeAdressePerioder?.size)
    }

    @Test
    fun vurderKombinerPerioder() {
        val doedsdato = LocalDate.parse("2022-03-25")
        val femAarFoerDoedsdato = doedsdato.minusYears(5)

        val perioderUtenGaps = listOf(
            Periode(
                gyldigFra = femAarFoerDoedsdato,
                gyldigTil = LocalDate.parse("2021-04-30")
            ),
            Periode(
                gyldigFra = LocalDate.parse("2020-01-26"),
                gyldigTil = doedsdato
            )
        )

        val etterfoelgendePerioder = listOf(
            Periode(
                gyldigFra = femAarFoerDoedsdato,
                gyldigTil = LocalDate.parse("2021-01-25")
            ),
            Periode(
                gyldigFra = LocalDate.parse("2021-01-26"),
                gyldigTil = doedsdato
            )
        )

        val perioderMedGaps = listOf(
            Periode(
                gyldigFra = femAarFoerDoedsdato,
                gyldigTil = LocalDate.parse("2020-04-30")
            ),
            Periode(
                gyldigFra = LocalDate.parse("2021-01-26"),
                gyldigTil = doedsdato
            )
        )

        val kombinertePerioder = kombinerPerioder(perioderUtenGaps)
        val kombinerteEtterfoelgendePerioder = kombinerPerioder(etterfoelgendePerioder)
        val kombinertePerioderMedGaps = kombinerPerioder(perioderMedGaps)

        assertEquals(1, kombinertePerioder?.size)
        assertEquals(femAarFoerDoedsdato, kombinertePerioder?.first()?.gyldigFra)
        assertEquals(doedsdato, kombinertePerioder?.first()?.gyldigTil)

        assertEquals(1, kombinerteEtterfoelgendePerioder?.size)
        assertEquals(femAarFoerDoedsdato, kombinerteEtterfoelgendePerioder?.first()?.gyldigFra)
        assertEquals(doedsdato, kombinerteEtterfoelgendePerioder?.first()?.gyldigTil)

        assertEquals(2, kombinertePerioderMedGaps?.size)
        assertEquals(femAarFoerDoedsdato, kombinertePerioderMedGaps?.first()?.gyldigFra)
        assertEquals(LocalDate.parse("2020-04-30"), kombinertePerioderMedGaps?.first()?.gyldigTil)
        assertEquals(LocalDate.parse("2021-01-26"), kombinertePerioderMedGaps?.get(1)?.gyldigFra)
        assertEquals(doedsdato, kombinertePerioderMedGaps?.get(1)?.gyldigTil)
    }

    @Test
    fun vurderHentGaps() {
        val doedsdato = LocalDate.parse("2022-03-25")
        val femAarFoerDoedsdato = doedsdato.minusYears(5)

        val perioderUtenGaps = listOf(
            Periode(
                gyldigFra = femAarFoerDoedsdato,
                gyldigTil = LocalDate.parse("2020-01-26")
            ),
            Periode(
                gyldigFra = LocalDate.parse("2020-01-26"),
                gyldigTil = doedsdato
            )
        )

        val perioderMedGaps = listOf(
            Periode(
                gyldigFra = femAarFoerDoedsdato.plusMonths(6),
                gyldigTil = LocalDate.parse("2020-04-30")
            ),
            Periode(
                gyldigFra = LocalDate.parse("2021-01-26"),
                gyldigTil = doedsdato.minusMonths(3)
            )
        )

        val stackUtenGaps = kombinerPerioder(perioderUtenGaps)
        val stackMedGaps = kombinerPerioder(perioderMedGaps)

        val ingenGaps = hentGaps(stackUtenGaps, femAarFoerDoedsdato, doedsdato)
        val gaps = hentGaps(stackMedGaps, femAarFoerDoedsdato, doedsdato)

        assertEquals(0, ingenGaps.size)

        assertEquals(3, gaps.size)
        assertEquals(femAarFoerDoedsdato, gaps.first().gyldigFra)
        assertEquals(femAarFoerDoedsdato.plusMonths(6).minusDays(1), gaps.first().gyldigTil)

        assertEquals(LocalDate.parse("2020-04-30").plusDays(1), gaps.get(1).gyldigFra)
        assertEquals(LocalDate.parse("2021-01-26").minusDays(1), gaps.get(1).gyldigTil)

        assertEquals(doedsdato.minusMonths(3).plusDays(1), gaps.get(2).gyldigFra)
        assertEquals(doedsdato, gaps.get(2).gyldigTil)
    }
}