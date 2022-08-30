package barnepensjon.vilkaar.avdoedesmedlemskap

import no.nav.etterlatte.barnepensjon.Periode
import no.nav.etterlatte.barnepensjon.hentDoedsdato
import no.nav.etterlatte.barnepensjon.hentGaps
import no.nav.etterlatte.barnepensjon.kombinerPerioder
import no.nav.etterlatte.libs.common.arbeidsforhold.AaregAnsettelsesdetaljer
import no.nav.etterlatte.libs.common.arbeidsforhold.AaregAnsettelsesperiode
import no.nav.etterlatte.libs.common.arbeidsforhold.ArbeidsforholdOpplysning
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.AvdoedSoeknad
import no.nav.etterlatte.libs.common.inntekt.Inntekt
import no.nav.etterlatte.libs.common.inntekt.InntektsOpplysning
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.vikaar.Kriterie
import no.nav.etterlatte.libs.common.vikaar.KriterieOpplysningsType
import no.nav.etterlatte.libs.common.vikaar.Kriteriegrunnlag
import no.nav.etterlatte.libs.common.vikaar.Kriterietyper
import no.nav.etterlatte.libs.common.vikaar.Utfall
import no.nav.etterlatte.libs.common.vikaar.VilkaarOpplysning
import no.nav.etterlatte.libs.common.vikaar.Vilkaartyper
import no.nav.etterlatte.libs.common.vikaar.VurderingsResultat
import no.nav.etterlatte.libs.common.vikaar.VurdertVilkaar
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters.firstDayOfNextMonth

fun vilkaarAvdoedesMedlemskap(
    avdoedSoeknad: VilkaarOpplysning<AvdoedSoeknad>?,
    avdoedPdl: VilkaarOpplysning<Person>?,
    inntektsOpplysning: VilkaarOpplysning<InntektsOpplysning>?,
    arbeidsforholdOpplysning: VilkaarOpplysning<ArbeidsforholdOpplysning>?
): VurdertVilkaar {
    if (listOf(avdoedSoeknad, avdoedPdl, inntektsOpplysning, arbeidsforholdOpplysning).any { it == null }) {
        return VurdertVilkaar(
            Vilkaartyper.AVDOEDES_FORUTGAAENDE_MEDLEMSKAP,
            VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING,
            Utfall.TRENGER_AVKLARING,
            emptyList(),
            LocalDateTime.now()
        )
    }

    val bosattNorge = metakriterieBosattNorge(avdoedSoeknad, avdoedPdl)

    val kriterier: List<Kriterie> = listOfNotNull(
        // kriterie A1-1: AP eller uføretrygd i hele 5-årsperioden
        kriterieHarMottattPensjonEllerTrygdSisteFemAar(avdoedPdl!!, inntektsOpplysning!!),
        // Kriterie A2-1 (todo: skal være 80%)
        kriterieHarHatt100prosentStillingSisteFemAar(arbeidsforholdOpplysning!!)
    )

    val vurderingsResultat = when (bosattNorge.utfall) {
        Utfall.OPPFYLT -> kriterier.minBy { it.resultat.ordinal }.resultat
        else -> bosattNorge.resultat
    }

    return VurdertVilkaar(
        Vilkaartyper.AVDOEDES_FORUTGAAENDE_MEDLEMSKAP,
        vurderingsResultat,
        vurderingsResultat.tilUtfall(),
        kriterier,
        LocalDateTime.now()
    )
}

private fun VurderingsResultat.tilUtfall() = when (this) {
    VurderingsResultat.OPPFYLT -> Utfall.OPPFYLT
    VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING -> Utfall.TRENGER_AVKLARING
    VurderingsResultat.IKKE_OPPFYLT -> Utfall.BEHANDLE_I_PSYS
}

data class DetaljerPeriode(
    val periode: AaregAnsettelsesperiode,
    val ansettelsesdetaljer: List<AaregAnsettelsesdetaljer>
)

fun kriterieHarHatt100prosentStillingSisteFemAar(
    arbeidsforholdOpplysning: VilkaarOpplysning<ArbeidsforholdOpplysning>
): Kriterie {
    val perioder = arbeidsforholdOpplysning.opplysning.arbeidsforhold.map {
        DetaljerPeriode(it.ansettelsesperiode, it.ansettelsesdetaljer)
    }

    val opplysningsGrunnlag = Kriteriegrunnlag(
        arbeidsforholdOpplysning.id,
        KriterieOpplysningsType.AVDOED_STILLINGSPROSENT,
        arbeidsforholdOpplysning.kilde,
        perioder
    )

    return Kriterie(
        Kriterietyper.AVDOED_HAR_HATT_100PROSENT_STILLING_SISTE_FEM_AAR,
        VurderingsResultat.OPPFYLT,
        listOf(opplysningsGrunnlag)
    )
}

fun kriterieHarMottattPensjonEllerTrygdSisteFemAar(
    avdoedPdl: VilkaarOpplysning<Person>,
    inntektsOpplysning: VilkaarOpplysning<InntektsOpplysning>
): Kriterie {
    val grunnlag = opprettGrunnlag(inntektsOpplysning)
    val doedsdato = hentDoedsdato(avdoedPdl)
    val gaps = finnGapsIUtbetalinger(grunnlag.first().opplysning, doedsdato.minusYears(5), doedsdato)

    return Kriterie(
        navn = Kriterietyper.AVDOED_HAR_MOTTATT_PENSJON_TRYGD_SISTE_FEM_AAR,
        resultat = if (gaps.isEmpty()) VurderingsResultat.OPPFYLT else VurderingsResultat.IKKE_OPPFYLT,
        basertPaaOpplysninger = grunnlag
    )
}

fun opprettGrunnlag(inntektsOpplysning: VilkaarOpplysning<InntektsOpplysning>) =
    inntektsOpplysning.opplysning.pensjonEllerTrygd.let { inntekter ->
        inntekter.filter { listOf("ufoeretrygd", "alderspensjon").contains(it.beskrivelse) }
            .sortedBy { YearMonth.parse(it.utbetaltIMaaned) }
            .let { utbetalinger ->
                listOf(
                    Kriteriegrunnlag(
                        inntektsOpplysning.id,
                        KriterieOpplysningsType.AVDOED_UFORE_PENSJON,
                        Grunnlagsopplysning.Inntektskomponenten("inntektskomponenten"),
                        utbetalinger
                    )
                )
            }
    }

fun finnGapsIUtbetalinger(utbetalinger: List<Inntekt>, fra: LocalDate, til: LocalDate): List<Periode> =
    utbetalinger.map {
        val utbetaltIMaaned = YearMonth.parse(it.utbetaltIMaaned)
        val gyldigFra = LocalDate.of(utbetaltIMaaned.year, utbetaltIMaaned.month, 1)

        Periode(gyldigFra, gyldigFra.with(firstDayOfNextMonth()))
    }.let {
        hentGaps(kombinerPerioder(it), fra, til)
    }