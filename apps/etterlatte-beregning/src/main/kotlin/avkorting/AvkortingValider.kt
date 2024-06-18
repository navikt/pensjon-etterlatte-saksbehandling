package no.nav.etterlatte.avkorting

import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.virkningstidspunkt
import no.nav.etterlatte.libs.common.beregning.AvkortingGrunnlagLagreDto
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeTillattException
import java.time.Month
import java.time.Year

fun validerInnekt(
    nyInntekt: AvkortingGrunnlagLagreDto,
    avkorting: Avkorting,
    behandling: DetaljertBehandling,
) {
    skalIkkeKunneLeggeTilEllerEndreAarsinntektTidligereAar(behandling)

    skalIkkeKunneLeggeTilEllerEndreAarsinntektTidligereEnnForrigeAarsinntekt(behandling, avkorting)

    skalIkkeLeggeTilFratrekkInnAarHvisDetErEtFulltaar(avkorting, nyInntekt, behandling)
}

private fun skalIkkeKunneLeggeTilEllerEndreAarsinntektTidligereAar(behandling: DetaljertBehandling) {
    if (behandling.behandlingType == BehandlingType.REVURDERING && behandling.virkningstidspunkt().dato.year < Year.now().value) {
        throw IkkeTillattException(
            code = "NY_INNTEKT_TIDLIGERE_AAR",
            detail = "Kan ikke legge til eller endre årsinntekt for tidligere år",
        )
    }
}

private fun skalIkkeKunneLeggeTilEllerEndreAarsinntektTidligereEnnForrigeAarsinntekt(
    behandling: DetaljertBehandling,
    avkorting: Avkorting,
) {
    val virkningstidspunkt = behandling.virkningstidspunkt().dato
    val nyligsteInntekt =
        avkorting.aarsoppgjoer
            .single { it.aar == virkningstidspunkt.year }
            .inntektsavkorting
            .lastOrNull()
    if (nyligsteInntekt != null && nyligsteInntekt.grunnlag.periode.fom > virkningstidspunkt) {
        throw IkkeTillattException(
            code = "NY_INNTEKT_KUN_NY_ELLER_NYLIGSTE",
            detail = "Kan ikke legge til eller endre årsinntekt som er tidligere enn forrige angitte årsinntekt.",
        )
    }
}

private fun skalIkkeLeggeTilFratrekkInnAarHvisDetErEtFulltaar(
    avkorting: Avkorting,
    nyInntekt: AvkortingGrunnlagLagreDto,
    behandling: DetaljertBehandling,
) {
    class ErFulltAar :
        IkkeTillattException(
            code = "NY_INNTEKT_FRATREKK_INN_AAR_FULLT_AAR",
            detail = "Kan ikke legge til fratrekk inn år i år med 12 innvilgede måneder.",
        )

    val virkningstidspunkt = behandling.virkningstidspunkt().dato

    val fratrekkLagtTil = nyInntekt.fratrekkInnAar > 0 || nyInntekt.fratrekkInnAarUtland > 0
    if (!fratrekkLagtTil) {
        return
    }

    val innvilgelseFraJanuar =
        behandling.behandlingType == BehandlingType.FØRSTEGANGSBEHANDLING && virkningstidspunkt.month == Month.JANUARY
    if (innvilgelseFraJanuar) {
        throw ErFulltAar()
    }

    val aarsoppgjoer = avkorting.aarsoppgjoer.single { it.aar == behandling.virkningstidspunkt().dato.year }
    val nyligsteInntekt = aarsoppgjoer.inntektsavkorting.lastOrNull()?.grunnlag

    if (nyligsteInntekt != null) {
        val revurderingINyttAar = nyligsteInntekt.periode.fom.year < virkningstidspunkt.year
        if (revurderingINyttAar) {
            throw ErFulltAar()
        }

        val revurderingIFulltAar = aarsoppgjoer.forventaInnvilgaMaaneder == 12
        if (revurderingIFulltAar) {
            throw ErFulltAar()
        }
    }
}
