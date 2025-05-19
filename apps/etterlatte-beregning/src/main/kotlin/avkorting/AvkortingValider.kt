package no.nav.etterlatte.avkorting

import no.nav.etterlatte.beregning.Beregning
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.beregning.AvkortingGrunnlagLagreDto
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.feilhaandtering.krevIkkeNull
import java.time.Month
import java.time.Year
import java.time.YearMonth

val MAANED_FOR_INNTEKT_NESTE_AAR = Month.OCTOBER

object AvkortingValider {
    fun paakrevdeInntekterForBeregningAvAvkorting(
        avkorting: Avkorting,
        beregning: Beregning,
        behandlingType: BehandlingType,
    ): List<Int> {
        val sortertePerioder = beregning.beregningsperioder.sortedBy { it.datoFOM }

        // Vi trenger inntekter fram til der behandlingen løper, eller i år og potensielt neste i førstegangsbehandlinger
        val foersteAar = (avkorting.aarsoppgjoer.map { it.aar } + listOf(sortertePerioder.first().datoFOM.year)).min()
        val sisteAar =
            when (val sisteAarIBeregning = sortertePerioder.last().datoTOM?.year) {
                null ->
                    if (YearMonth.now().month >= MAANED_FOR_INNTEKT_NESTE_AAR && behandlingType == BehandlingType.FØRSTEGANGSBEHANDLING) {
                        Year
                            .now()
                            .plusYears(1)
                            .value
                    } else {
                        Year.now().value
                    }

                else -> sisteAarIBeregning
            }
        val aarViMaaHaInntekterFor = (foersteAar..sisteAar).toList()
        return aarViMaaHaInntekterFor
    }

    fun validerInntekter(
        behandling: DetaljertBehandling,
        beregning: Beregning,
        eksisterendeAvkorting: Avkorting,
        nyeGrunnlag: List<AvkortingGrunnlagLagreDto>,
    ) {
        val inntekterViHar =
            eksisterendeAvkorting.aarsoppgjoer.map { it.aar }.toSet() + nyeGrunnlag.map { it.fom.year }.toSet()
        val inntekterViTrenger =
            paakrevdeInntekterForBeregningAvAvkorting(
                eksisterendeAvkorting,
                beregning,
                behandling.behandlingType,
            ).toSet()
        if (!inntekterViHar.containsAll(inntekterViTrenger)) {
            throw UgyldigForespoerselException(
                "MANGLER_INNTEKTER_FOR_AVKORTING",
                "Mangler inntektsgrunnlag for år(ene) ${inntekterViTrenger - inntekterViHar}",
            )
        }
        val virk =
            krevIkkeNull(behandling.virkningstidspunkt?.dato) {
                "Behandling mangler virkningstidspunkt, kan ikke legge inn nye inntekter. " +
                    "Sak: ${behandling.sak.sakId}, id:${behandling.id}"
            }
        val virkFoerstegangsbehandling =
            virk.takeIf { behandling.behandlingType == BehandlingType.FØRSTEGANGSBEHANDLING }

        // Nye år:
        val nyeAarMedInntekt =
            nyeGrunnlag.map { it.fom.year }.toSet() - eksisterendeAvkorting.aarsoppgjoer.map { it.aar }.toSet()

        val alleNyeAarHarInntektFraStart =
            nyeAarMedInntekt.all { aar ->
                // Start på et år er enten 1. januar eller virk i førstegangsbehandlinger
                nyeGrunnlag.find {
                    it.fom ==
                        YearMonth.of(
                            aar,
                            Month.JANUARY,
                        ) ||
                        it.fom == virkFoerstegangsbehandling
                } != null
            }
        if (!alleNyeAarHarInntektFraStart) {
            // kast feil vi må ha kontinuerlige inntekter
            throw UgyldigForespoerselException(
                "NYTT_INNTEKTSAAR_IKKE_FRA_START",
                "Alle nye år med inntekt må ha inntekt fra januar",
            )
        }

        if (behandling.behandlingType == BehandlingType.REVURDERING && nyeAarMedInntekt.isNotEmpty()) {
            if (YearMonth.of(nyeAarMedInntekt.min(), Month.JANUARY) < virk) {
                throw UgyldigForespoerselException(
                    "NYTT_INNTEKTSAAR_IKKE_FRA_START",
                    "Revurdering med ny inntekt må ha virkningstidspunkt før det nye inntektsåret",
                )
            }
        }

        if (nyeGrunnlag.map { it.fom }.toSet().size < nyeGrunnlag.size) {
            throw UgyldigForespoerselException("DUPLIKAT_INNTEKT", "Kan ikke registrere flere inntekter med samme fra-dato")
        }
    }

    fun validerInntekt(
        nyInntekt: AvkortingGrunnlagLagreDto,
        avkorting: Avkorting,
        erFoerstegangsbehandling: Boolean,
        naa: YearMonth = YearMonth.now(),
    ) {
        skalIkkeKunneEndreInntektITidligereAarHvisAarsoppgjoerErEtteroppgjoer(
            erFoerstegangsbehandling,
            nyInntekt.fom,
            avkorting,
            naa,
        )

        foersteRevurderingIAareneEtterInnvilgelsesaarMaaStarteIJanuar(
            nyInntekt,
            avkorting,
            erFoerstegangsbehandling,
        )

        skalIkkeLeggeTilFratrekkInnAarHvisDetErEtFulltaar(
            nyInntekt,
            nyInntekt.fom,
        )

        // TODO valider at virk tidligere enn forrige innvilgelse ikke støttes enda
    }

    private fun skalIkkeKunneEndreInntektITidligereAarHvisAarsoppgjoerErEtteroppgjoer(
        erFoerstegangsbehandling: Boolean,
        nyInntektFom: YearMonth,
        avkorting: Avkorting,
        naa: YearMonth,
    ) {
        if (!erFoerstegangsbehandling && nyInntektFom.year < naa.year) {
            val gjeldendeAar = avkorting.aarsoppgjoer.single { it.aar == nyInntektFom.year }
            if (gjeldendeAar is Etteroppgjoer) {
                throw InntektForTidligereAar()
            }
        }
    }

    private fun foersteRevurderingIAareneEtterInnvilgelsesaarMaaStarteIJanuar(
        nyInntekt: AvkortingGrunnlagLagreDto,
        avkorting: Avkorting,
        erFoerstegangsbehandling: Boolean,
    ) {
        if (!erFoerstegangsbehandling) {
            if (avkorting.aarsoppgjoer.none { it.aar == nyInntekt.fom.year }) {
                if (nyInntekt.fom.month != Month.JANUARY) {
                    throw FoersteRevurderingSenereEnnJanuar()
                }
            }
        }
    }

    private fun skalIkkeLeggeTilFratrekkInnAarHvisDetErEtFulltaar(
        nyInntekt: AvkortingGrunnlagLagreDto,
        fom: YearMonth,
    ) {
        val fratrekkLagtTil = nyInntekt.fratrekkInnAar > 0 || nyInntekt.fratrekkInnAarUtland > 0
        if (fratrekkLagtTil && fom.month == Month.JANUARY) {
            throw HarFratrekkInnAarForFulltAar()
        }
    }
}

class FoersteRevurderingSenereEnnJanuar :
    UgyldigForespoerselException(
        code = "FOERSTE_REVURDERING_I_NYTT_AAR_SENERE_ENN_JANUAR",
        detail = "Første revurdering i årene etter innvilgelsesår må være fom januar.",
    )

class HarFratrekkInnAarForFulltAar :
    UgyldigForespoerselException(
        code = "NY_INNTEKT_FRATREKK_INN_AAR_FULLT_AAR",
        detail = "Kan ikke legge til fratrekk inn år når det er innvilga måned fra og med januar",
    )

class InntektForTidligereAar :
    UgyldigForespoerselException(
        "ENDRE_INNTEKT_TIDLIGERE_AAR",
        "Det er ikke mulig å endre inntekt for tidligere år",
    )
