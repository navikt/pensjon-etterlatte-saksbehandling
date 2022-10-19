package no.nav.etterlatte.vilkaarsvurdering

import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.grunnlag.hentDoedsdato
import java.time.LocalDate
import java.time.YearMonth

// TODO brukes kun midlertidig

object GrunnlagForAvdoedMangler : IllegalStateException("Kunne ikke hente ut grunnlagsinformasjon om avdød")

fun beregnVirkningstidspunktFoerstegangsbehandling(
    grunnlag: Grunnlag,
    soeknadMottattDato: LocalDate
): YearMonth {
    val avdoedDoedsdato = grunnlag.hentAvdoed().hentDoedsdato()

    return hentVirkningstidspunktFoerstegangssoeknad(avdoedDoedsdato?.verdi, soeknadMottattDato)
}

private fun hentVirkningstidspunktFoerstegangssoeknad(doedsdato: LocalDate?, mottattDato: LocalDate): YearMonth {
    if (doedsdato == null) {
        throw GrunnlagForAvdoedMangler
    }
    if (mottattDato.year - doedsdato.year > 3) {
        return YearMonth.of(mottattDato.year - 3, mottattDato.month)
    }
    return YearMonth.from(doedsdato).plusMonths(1)
}