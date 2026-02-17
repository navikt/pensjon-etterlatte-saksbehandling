package no.nav.etterlatte.avkorting

import no.nav.etterlatte.beregning.Beregning
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.beregning.AvkortingGrunnlagLagreDto
import no.nav.etterlatte.libs.common.beregning.Sanksjon
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.feilhaandtering.krevIkkeNull
import java.time.Month
import java.time.YearMonth

val MAANED_FOR_INNTEKT_NESTE_AAR = Month.OCTOBER

object AvkortingValider {
    fun paakrevdeInntekterForBeregningAvAvkorting(
        avkorting: Avkorting,
        beregning: Beregning,
        behandlingType: BehandlingType,
        sanksjoner: List<Sanksjon>,
        krevInntektForNesteAar: Boolean,
        naa: YearMonth = YearMonth.now(),
    ): List<Int> {
        val sortertePerioder = beregning.beregningsperioder.sortedBy { it.datoFOM }

        val alleAarViHarAvkortingEllerBeregning = avkorting.aarsoppgjoer.map { it.aar } + sortertePerioder.map { it.datoFOM.year }
        val foersteAar = alleAarViHarAvkortingEllerBeregning.min()
        val sisteAarFom = alleAarViHarAvkortingEllerBeregning.max()

        // TODO: trekke fra siste år hvis bare sansksjon

        // Vi trenger inntekter fram til der behandlingen løper, eller i år og potensielt neste i førstegangsbehandlinger
        val sisteAar =
            when (val tilOgMedAarBeregning = sortertePerioder.last().datoTOM?.year) {
                null -> {
                    if (naa.month >= MAANED_FOR_INNTEKT_NESTE_AAR && krevInntektForNesteAar &&
                        behandlingType == BehandlingType.FØRSTEGANGSBEHANDLING
                    ) {
                        maxOf(naa.year + 1, sisteAarFom)
                    } else {
                        // Hvis virkningstidspunkt er i framtiden (fra nå) er det viktig at siste år påkrevd er
                        // minst like stort som første år i beregning
                        maxOf(naa.year, sisteAarFom)
                    }
                }

                else -> {
                    tilOgMedAarBeregning
                }
            }
        val aarViMaaHaInntekterFor = (foersteAar..sisteAar).toList()
        return aarViMaaHaInntekterFor
    }

    fun validerInntekter(
        behandling: DetaljertBehandling,
        beregning: Beregning,
        eksisterendeAvkorting: Avkorting,
        nyeGrunnlag: List<AvkortingGrunnlagLagreDto>,
        sanksjoner: List<Sanksjon>,
        krevInntektForNesteAar: Boolean,
        naa: YearMonth = YearMonth.now(),
    ) {
        val inntekterViHar =
            eksisterendeAvkorting.aarsoppgjoer.map { it.aar }.toSet() + nyeGrunnlag.map { it.fom.year }.toSet()
        val inntekterViTrenger =
            paakrevdeInntekterForBeregningAvAvkorting(
                eksisterendeAvkorting,
                beregning,
                behandling.behandlingType,
                sanksjoner,
                krevInntektForNesteAar,
                naa,
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
            throw NyeAarMedInntektMaaStarteIJanuar()
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
            throw UgyldigForespoerselException(
                "DUPLIKAT_INNTEKT",
                "Kan ikke registrere flere inntekter med samme fra-dato",
            )
        }

        val fulleInntektsaarAvkorting =
            eksisterendeAvkorting.aarsoppgjoer
                .map { it.fom }
                .filter { it.month == Month.JANUARY }
                .map { it.year }
        val fulleInntektsaarNyeGrunnlag =
            nyeGrunnlag
                .map { it.fom }
                .filter { it.month == Month.JANUARY }
                .map { it.year }
        val fulleInntektsAar = fulleInntektsaarAvkorting + fulleInntektsaarNyeGrunnlag
        if (nyeGrunnlag.any { it.fom.year in fulleInntektsAar && (it.fratrekkInnAarUtland != 0 || it.fratrekkInnAar != 0) }) {
            throw HarFratrekkInnAarForFulltAar()
        }

        val etteroppgjorteAar =
            eksisterendeAvkorting.aarsoppgjoer
                .filterIsInstance<Etteroppgjoer>()
                .map { it.aar }
        if (nyeGrunnlag.any { it.fom.year in etteroppgjorteAar }) {
            throw InntektForTidligereAar()
        }
    }
}

class NyeAarMedInntektMaaStarteIJanuar :
    UgyldigForespoerselException(
        code = "INNTEKT_I_NYTT_AAR_SENERE_ENN_JANUAR",
        detail = "Inntekt i nytt år må starte i januar, med mindre det er første virkningstidspunkt i saken.",
    )

class HarFratrekkInnAarForFulltAar :
    UgyldigForespoerselException(
        code = "NY_INNTEKT_FRATREKK_INN_AAR_FULLT_AAR",
        detail = "Kan ikke legge til fratrekk inn år når det er innvilga måned fra og med januar",
    )

class InntektForTidligereAar :
    UgyldigForespoerselException(
        "ENDRE_INNTEKT_TIDLIGERE_AAR",
        "Det er ikke mulig å endre inntekt for tidligere år som er etteroppgjort",
    )
