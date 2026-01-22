package no.nav.etterlatte.brev.model.oms

import no.nav.etterlatte.brev.Slate
import no.nav.etterlatte.brev.behandling.AvkortetBeregningsperiode
import no.nav.etterlatte.brev.behandling.Avkortingsinfo
import no.nav.etterlatte.brev.model.OmstillingsstoenadBeregning
import no.nav.etterlatte.brev.model.OmstillingsstoenadBeregningsperiode
import no.nav.etterlatte.brev.model.erYrkesskade
import no.nav.etterlatte.brev.model.fromDto
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.virkningstidspunkt
import no.nav.etterlatte.libs.common.beregning.BeregningOgAvkortingPeriodeDto
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.kodeverk.LandDto
import no.nav.etterlatte.libs.common.trygdetid.TrygdetidDto
import no.nav.etterlatte.regler.ANTALL_DESIMALER_INNTENKT
import no.nav.etterlatte.regler.roundingModeInntekt
import no.nav.pensjon.brevbaker.api.model.Kroner
import java.math.BigDecimal
import java.time.LocalDate

fun omsBeregning(
    behandling: DetaljertBehandling,
    trygdetid: TrygdetidDto,
    avkortingsinfo: Avkortingsinfo,
    landKodeverk: List<LandDto>,
    innhold: List<Slate.Element> = emptyList(),
): OmstillingsstoenadBeregning {
    val beregningsperioder =
        avkortingsinfo.beregningsperioder.map { it.tilOmstillingsstoenadBeregningsperiode() }
    val beregningsperioderOpphoer = utledBeregningsperioderOpphoer(behandling, beregningsperioder)
    return OmstillingsstoenadBeregning(
        innhold = innhold,
        virkningsdato = avkortingsinfo.virkningsdato,
        beregningsperioder = beregningsperioder,
        sisteBeregningsperiode = beregningsperioderOpphoer.sisteBeregningsperiode,
        sisteBeregningsperiodeNesteAar = beregningsperioderOpphoer.sisteBeregningsperiodeNesteAar,
        trygdetid =
            trygdetid.fromDto(
                beregningsMetodeFraGrunnlag = beregningsperioderOpphoer.sisteBeregningsperiode.beregningsMetodeFraGrunnlag,
                beregningsMetodeAnvendt = beregningsperioderOpphoer.sisteBeregningsperiode.beregningsMetodeAnvendt,
                navnAvdoed = null,
                landKodeverk = landKodeverk,
            ),
        oppphoersdato = beregningsperioderOpphoer.forventetOpphoerDato,
        opphoerNesteAar =
            beregningsperioderOpphoer.forventetOpphoerDato?.year == (behandling.virkningstidspunkt().dato.year + 1),
        erYrkesskade = trygdetid.erYrkesskade(),
    )
}

fun BeregningOgAvkortingPeriodeDto.toAvkortetBeregningsperiode(): AvkortetBeregningsperiode =
    AvkortetBeregningsperiode(
        datoFOM = periode.fom.atDay(1),
        datoTOM = periode.tom?.atEndOfMonth(),
        grunnbeloep = Kroner(grunnbelop),
        // (vises i brev) maanedsinntekt regel burde eksponert dette, krever omskrivning av regler som vi må bli enige om
        inntekt =
            Kroner(
                BigDecimal(oppgittInntekt - fratrekkInnAar)
                    .setScale(
                        ANTALL_DESIMALER_INNTENKT,
                        roundingModeInntekt,
                    ).toInt(),
            ),
        oppgittInntekt = Kroner(oppgittInntekt),
        fratrekkInnAar = Kroner(fratrekkInnAar),
        innvilgaMaaneder = innvilgaMaaneder,
        ytelseFoerAvkorting = Kroner(ytelseFoerAvkorting),
        restanse = Kroner(restanse),
        utbetaltBeloep = Kroner(ytelseEtterAvkorting),
        trygdetid = trygdetid,
        beregningsMetodeAnvendt =
            beregningsMetode
                ?: throw InternfeilException("OMS Brevdata krever anvendt beregningsmetode"),
        beregningsMetodeFraGrunnlag =
            beregningsMetodeFraGrunnlag
                ?: throw InternfeilException("OMS Brevdata krever valgt beregningsmetode fra beregningsgrunnlag"),
        // ved manuelt overstyrt beregning har vi ikke grunnlag
        sanksjon = sanksjon,
        institusjon = institusjonsopphold,
        erOverstyrtInnvilgaMaaneder = erOverstyrtInnvilgaMaaneder,
    )

fun utledBeregningsperioderOpphoer(
    behandling: DetaljertBehandling,
    beregningsperioder: List<OmstillingsstoenadBeregningsperiode>,
): BeregningsperioderFlereAarOpphoer {
    val sisteBeregningsperiode =
        beregningsperioder
            .filter {
                it.datoFOM.year == beregningsperioder.first().datoFOM.year
            }.maxBy { it.datoFOM }
    val sisteBeregningsperiodeNesteAar =
        beregningsperioder
            .filter {
                it.datoFOM.year == beregningsperioder.first().datoFOM.year + 1
            }.maxByOrNull { it.datoFOM }

    // Hvis antall innvilga måneder er overstyrt under beregning skal "forventa" opphørsdato vises selv uten opphørFom
    val forventaOpphoersDato =
        when (val opphoer = behandling.opphoerFraOgMed) {
            null -> {
                if (sisteBeregningsperiode.erOverstyrtInnvilgaMaaneder) {
                    val foersteFom = beregningsperioder.first().datoFOM
                    foersteFom.plusMonths(sisteBeregningsperiode.innvilgaMaaneder.toLong())
                } else if (sisteBeregningsperiodeNesteAar != null && sisteBeregningsperiodeNesteAar.erOverstyrtInnvilgaMaaneder) {
                    LocalDate
                        .of(sisteBeregningsperiodeNesteAar.datoFOM.year, 1, 1)
                        .plusMonths(sisteBeregningsperiodeNesteAar.innvilgaMaaneder.toLong())
                } else {
                    null
                }
            }

            else -> {
                opphoer.atDay(1)
            }
        }
    return BeregningsperioderFlereAarOpphoer(
        sisteBeregningsperiode = sisteBeregningsperiode,
        sisteBeregningsperiodeNesteAar = sisteBeregningsperiodeNesteAar,
        forventetOpphoerDato = forventaOpphoersDato,
    )
}

data class BeregningsperioderFlereAarOpphoer(
    val sisteBeregningsperiode: OmstillingsstoenadBeregningsperiode,
    val sisteBeregningsperiodeNesteAar: OmstillingsstoenadBeregningsperiode?,
    val forventetOpphoerDato: LocalDate?,
)
