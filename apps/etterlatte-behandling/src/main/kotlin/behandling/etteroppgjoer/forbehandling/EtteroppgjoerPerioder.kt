package no.nav.etterlatte.behandling.etteroppgjoer.forbehandling

import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.common.vedtak.InnvilgetPeriodeDto
import java.time.YearMonth

internal fun innvilgedePerioderIEtteroppgjoersAar(
    innvilgedePerioder: List<InnvilgetPeriodeDto>,
    inntektsaar: Int,
): List<Periode> {
    if (innvilgedePerioder.isEmpty()) {
        throw UgyldigForespoerselException(
            "MANGLER_INNVILGET_PERIODE",
            "Saken har ingen innvilget periode. Dobbeltsjekk at dette stemmer, hvis saken er opphørt fra første " +
                "virkiningstidspunkt er det ikke noe å behandle et etteroppgjør på. Hvis det ikke stemmer " +
                "må det meldes feil i porten.",
        )
    }
    val januar = YearMonth.of(inntektsaar, 1)
    val desember = YearMonth.of(inntektsaar, 12)
    val perioder =
        innvilgedePerioder
            .mapNotNull {
                val fom = maxOf(it.periode.fom, januar)
                val tom = minOf(it.periode.tom ?: desember, desember)
                if (fom <= tom) Periode(fom, tom) else null
            }.sortedBy { it.fom }
    if (perioder.isEmpty()) {
        throw InternfeilException(
            "Sak er ikke innvilget i året $inntektsaar, skal ikke kunne opprette etteroppgjør for året $inntektsaar",
        )
    }
    return perioder
}

internal fun oppgjoersPeriode(innvilgedePerioder: List<Periode>): Periode =
    Periode(
        fom = innvilgedePerioder.minOf { it.fom },
        tom = innvilgedePerioder.maxOf { requireNotNull(it.tom) },
    )
