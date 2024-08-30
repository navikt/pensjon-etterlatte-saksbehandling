package no.nav.etterlatte.avkorting

import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.virkningstidspunkt
import no.nav.etterlatte.libs.common.beregning.AvkortingGrunnlagLagreDto
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeTillattException
import java.time.Month

object AvkortingValider {
    fun validerInntekt(
        nyInntekt: AvkortingGrunnlagLagreDto,
        avkorting: Avkorting,
        behandling: DetaljertBehandling,
    ) {
        skalIkkeKunneLeggeTilEllerEndreAarsinntektTidligereEnnForrigeAarsinntekt(behandling, avkorting)
        skalIkkeLeggeTilFratrekkInnAarHvisDetErEtFulltaar(avkorting, nyInntekt, behandling)

        // TODO valider at virk tidligere enn forrige innvilgelse ikke støttes enda
    }

    private fun skalIkkeKunneLeggeTilEllerEndreAarsinntektTidligereEnnForrigeAarsinntekt(
        behandling: DetaljertBehandling,
        avkorting: Avkorting,
    ) {
        val virkningstidspunkt = behandling.virkningstidspunkt().dato
        val nyligsteInntekt =
            avkorting.aarsoppgjoer
                .singleOrNull { it.aar == virkningstidspunkt.year }
                ?.inntektsavkorting
                ?.lastOrNull()
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
        val virkningstidspunkt = behandling.virkningstidspunkt().dato

        val fratrekkLagtTil = nyInntekt.fratrekkInnAar > 0 || nyInntekt.fratrekkInnAarUtland > 0
        if (!fratrekkLagtTil) {
            return
        }

        if (behandling.behandlingType == BehandlingType.FØRSTEGANGSBEHANDLING) {
            if (virkningstidspunkt.month == Month.JANUARY) {
                throw ErFulltAar()
            }
        } else {
            val nyligsteAarsoppgjoer = avkorting.aarsoppgjoer.maxBy { it.aar }
            val nyligsteInntekt = nyligsteAarsoppgjoer.inntektsavkorting.lastOrNull()?.grunnlag

            if (nyligsteInntekt != null) {
                val revurderingINyttAar = nyligsteInntekt.periode.fom.year < virkningstidspunkt.year
                if (revurderingINyttAar) {
                    throw ErFulltAar()
                }

                val revurderingIFulltAar = nyligsteAarsoppgjoer.forventaInnvilgaMaaneder == 12
                if (revurderingIFulltAar) {
                    throw ErFulltAar()
                }
            }
        }
    }
}

class ErFulltAar :
    IkkeTillattException(
        code = "NY_INNTEKT_FRATREKK_INN_AAR_FULLT_AAR",
        detail = "Kan ikke legge til fratrekk inn år i år med 12 innvilgede måneder.",
    )
