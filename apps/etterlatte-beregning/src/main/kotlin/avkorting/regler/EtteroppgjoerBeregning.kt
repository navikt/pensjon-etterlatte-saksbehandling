package no.nav.etterlatte.avkorting.regler

import no.nav.etterlatte.avkorting.Aarsoppgjoer
import no.nav.etterlatte.avkorting.AvkortetYtelse
import no.nav.etterlatte.avkorting.EtteroppgjoerResultatType
import no.nav.etterlatte.beregning.regler.omstillingstoenad.OMS_GYLDIG_FRA
import no.nav.etterlatte.libs.regler.FaktumNode
import no.nav.etterlatte.libs.regler.KonstantRegel
import no.nav.etterlatte.libs.regler.Regel
import no.nav.etterlatte.libs.regler.RegelMeta
import no.nav.etterlatte.libs.regler.RegelReferanse
import no.nav.etterlatte.libs.regler.benytter
import no.nav.etterlatte.libs.regler.finnFaktumIGrunnlag
import no.nav.etterlatte.libs.regler.med
import no.nav.etterlatte.libs.regler.og
import java.time.temporal.ChronoUnit

data class EtteroppgjoerDifferanseGrunnlag(
    val utbetaltStoenad: FaktumNode<Aarsoppgjoer>,
    val nyBruttoStoenad: FaktumNode<Aarsoppgjoer>,
)

val nyBruttoStoenad: Regel<EtteroppgjoerDifferanseGrunnlag, List<AvkortetYtelse>> =
    finnFaktumIGrunnlag(
        gjelderFra = OMS_GYLDIG_FRA,
        beskrivelse = "",
        finnFaktum = EtteroppgjoerDifferanseGrunnlag::nyBruttoStoenad,
        finnFelt = { it.avkortetYtelse },
    )

val utbetaltStoenad: Regel<EtteroppgjoerDifferanseGrunnlag, List<AvkortetYtelse>> =
    finnFaktumIGrunnlag(
        gjelderFra = OMS_GYLDIG_FRA,
        beskrivelse = "",
        finnFaktum = EtteroppgjoerDifferanseGrunnlag::utbetaltStoenad,
        finnFelt = { it.avkortetYtelse },
    )

val sumNyBruttoStoenad =
    RegelMeta(
        gjelderFra = OMS_GYLDIG_FRA,
        beskrivelse = "",
        regelReferanse = RegelReferanse(id = "", versjon = ""),
    ) benytter nyBruttoStoenad med { avkortetYtelse ->
        avkortetYtelse.sumOf {
            // TODO egen regel?
            (it.periode.fom.until(it.periode.tom, ChronoUnit.MONTHS) + 1) * it.ytelseEtterAvkorting
        }
    }

val sumUtbetaltStoenad =
    RegelMeta(
        gjelderFra = OMS_GYLDIG_FRA,
        beskrivelse = "",
        regelReferanse = RegelReferanse(id = "", versjon = ""),
    ) benytter utbetaltStoenad med { avkortetYtelse ->
        avkortetYtelse.sumOf {
            // TODO egen regel?
            (it.periode.fom.until(it.periode.tom, ChronoUnit.MONTHS) + 1) * it.ytelseEtterAvkorting
        }
    }

val differanse =
    RegelMeta(
        gjelderFra = OMS_GYLDIG_FRA,
        beskrivelse = "",
        regelReferanse = RegelReferanse(id = "", versjon = ""),
    ) benytter sumUtbetaltStoenad og sumNyBruttoStoenad med { sumUtbetalt, sumNyBrutto ->
        sumUtbetalt - sumNyBrutto
    }

val grense =
    KonstantRegel<EtteroppgjoerDifferanseGrunnlag, GrenseForEtteroppgjoer>(
        gjelderFra = OMS_GYLDIG_FRA,
        beskrivelse = "",
        regelReferanse = RegelReferanse(id = "", versjon = ""),
        verdi = GrenseForEtteroppgjoer(1000, 1000),
    )

data class GrenseForEtteroppgjoer(
    val tilbakekreving: Int,
    val etterbetaling: Int,
)

val etteroppgjoerRegel =
    RegelMeta(
        gjelderFra = OMS_GYLDIG_FRA,
        beskrivelse = "",
        regelReferanse = RegelReferanse(id = "", versjon = ""),
    ) benytter differanse og grense med { differanse, grenser ->
        when {
            differanse > grenser.tilbakekreving -> EtteroppgjoerResultatType.TILBAKREVING
            differanse * -1 > grenser.etterbetaling -> EtteroppgjoerResultatType.ETTERBETALING
            else -> EtteroppgjoerResultatType.IKKE_ETTEROPPGJOER
        }
    }
